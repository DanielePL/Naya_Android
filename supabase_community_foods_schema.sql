-- ═══════════════════════════════════════════════════════════════
-- PROMETHEUS COMMUNITY FOODS - DATABASE SCHEMA
-- ═══════════════════════════════════════════════════════════════
-- Crowdsourced nutrition data from barcode scans
-- Users can contribute products not found in OpenFoodFacts/USDA

-- 1. COMMUNITY FOODS TABLE (Crowdsourced Product Database)
CREATE TABLE IF NOT EXISTS community_foods (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  -- Barcode (unique identifier)
  barcode TEXT NOT NULL UNIQUE,

  -- Basic Info
  name TEXT NOT NULL,
  brand TEXT,

  -- Serving Info (always per 100g for consistency)
  serving_size FLOAT DEFAULT 100,
  serving_unit TEXT DEFAULT 'g',

  -- Core Macros (per 100g)
  calories FLOAT NOT NULL,
  protein FLOAT NOT NULL,
  carbs FLOAT NOT NULL,
  fat FLOAT NOT NULL,

  -- Extended Macros (optional)
  fiber FLOAT,
  sugar FLOAT,
  saturated_fat FLOAT,
  sodium FLOAT,

  -- Micronutrients (optional, stored as JSONB for flexibility)
  micronutrients JSONB DEFAULT '{}',

  -- Product Images
  label_image_url TEXT,      -- Photo of nutrition label
  product_image_url TEXT,    -- Photo of product front

  -- Contributor Info
  contributed_by UUID REFERENCES auth.users ON DELETE SET NULL,

  -- Trust & Verification
  verification_count INT DEFAULT 1,           -- How many users confirmed this data
  verified_by UUID[] DEFAULT '{}',            -- Array of user IDs who verified
  is_verified BOOLEAN DEFAULT false,          -- True when verification_count >= 3
  confidence_score FLOAT DEFAULT 0.5,         -- 0.0 to 1.0, increases with verifications

  -- OCR Metadata
  ocr_raw_text TEXT,                          -- Raw text from label scan
  extraction_method TEXT DEFAULT 'ocr',       -- 'ocr', 'manual', 'ai_assisted'

  -- Status
  is_flagged BOOLEAN DEFAULT false,           -- Flagged for review
  flag_reason TEXT,

  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. COMMUNITY FOOD VERIFICATIONS TABLE (Track individual verifications)
CREATE TABLE IF NOT EXISTS community_food_verifications (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  community_food_id UUID NOT NULL REFERENCES community_foods ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users ON DELETE CASCADE,

  -- Verification Details
  action TEXT NOT NULL CHECK (action IN ('confirm', 'correct', 'flag')),

  -- If corrected, store the proposed corrections
  corrections JSONB,

  -- Flag reason if flagged
  flag_reason TEXT,

  created_at TIMESTAMPTZ DEFAULT NOW(),

  -- One verification per user per food
  UNIQUE(community_food_id, user_id)
);

-- ═══════════════════════════════════════════════════════════════
-- INDEXES
-- ═══════════════════════════════════════════════════════════════

-- Fast barcode lookup (most important!)
CREATE UNIQUE INDEX IF NOT EXISTS idx_community_foods_barcode ON community_foods(barcode);

-- Find unverified foods for moderation
CREATE INDEX IF NOT EXISTS idx_community_foods_unverified ON community_foods(is_verified, created_at DESC) WHERE is_verified = false;

-- Find flagged foods for review
CREATE INDEX IF NOT EXISTS idx_community_foods_flagged ON community_foods(is_flagged, created_at DESC) WHERE is_flagged = true;

-- User contributions
CREATE INDEX IF NOT EXISTS idx_community_foods_contributor ON community_foods(contributed_by);

-- Verification lookups
CREATE INDEX IF NOT EXISTS idx_verifications_food ON community_food_verifications(community_food_id);
CREATE INDEX IF NOT EXISTS idx_verifications_user ON community_food_verifications(user_id);

-- ═══════════════════════════════════════════════════════════════
-- ROW LEVEL SECURITY (RLS)
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE community_foods ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_food_verifications ENABLE ROW LEVEL SECURITY;

-- COMMUNITY FOODS: Everyone can read, authenticated users can insert
DROP POLICY IF EXISTS "Anyone can view community foods" ON community_foods;
CREATE POLICY "Anyone can view community foods" ON community_foods
  FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated users can add community foods" ON community_foods;
CREATE POLICY "Authenticated users can add community foods" ON community_foods
  FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- Only contributor or admin can update their own submissions
DROP POLICY IF EXISTS "Contributors can update their own foods" ON community_foods;
CREATE POLICY "Contributors can update their own foods" ON community_foods
  FOR UPDATE USING (auth.uid() = contributed_by);

-- VERIFICATIONS: Users can see all verifications, add their own
DROP POLICY IF EXISTS "Anyone can view verifications" ON community_food_verifications;
CREATE POLICY "Anyone can view verifications" ON community_food_verifications
  FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated users can add verifications" ON community_food_verifications;
CREATE POLICY "Authenticated users can add verifications" ON community_food_verifications
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════
-- FUNCTIONS
-- ═══════════════════════════════════════════════════════════════

-- Function: Get community food by barcode
CREATE OR REPLACE FUNCTION get_community_food_by_barcode(p_barcode TEXT)
RETURNS TABLE(
  id UUID,
  barcode TEXT,
  name TEXT,
  brand TEXT,
  calories FLOAT,
  protein FLOAT,
  carbs FLOAT,
  fat FLOAT,
  fiber FLOAT,
  sugar FLOAT,
  saturated_fat FLOAT,
  sodium FLOAT,
  micronutrients JSONB,
  is_verified BOOLEAN,
  confidence_score FLOAT,
  verification_count INT
)
LANGUAGE sql
STABLE
AS $$
  SELECT
    cf.id,
    cf.barcode,
    cf.name,
    cf.brand,
    cf.calories,
    cf.protein,
    cf.carbs,
    cf.fat,
    cf.fiber,
    cf.sugar,
    cf.saturated_fat,
    cf.sodium,
    cf.micronutrients,
    cf.is_verified,
    cf.confidence_score,
    cf.verification_count
  FROM community_foods cf
  WHERE cf.barcode = p_barcode
    AND cf.is_flagged = false;
$$;

-- Function: Add verification and update food status
CREATE OR REPLACE FUNCTION verify_community_food(
  p_food_id UUID,
  p_user_id UUID,
  p_action TEXT,
  p_corrections JSONB DEFAULT NULL,
  p_flag_reason TEXT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_current_count INT;
BEGIN
  -- Insert verification record
  INSERT INTO community_food_verifications (community_food_id, user_id, action, corrections, flag_reason)
  VALUES (p_food_id, p_user_id, p_action, p_corrections, p_flag_reason)
  ON CONFLICT (community_food_id, user_id) DO UPDATE
  SET action = p_action, corrections = p_corrections, flag_reason = p_flag_reason;

  -- Handle based on action
  IF p_action = 'confirm' THEN
    -- Increment verification count
    UPDATE community_foods
    SET
      verification_count = verification_count + 1,
      verified_by = array_append(verified_by, p_user_id),
      confidence_score = LEAST(1.0, confidence_score + 0.15),
      is_verified = (verification_count + 1) >= 3,
      updated_at = NOW()
    WHERE id = p_food_id
      AND NOT (p_user_id = ANY(verified_by));  -- Don't count same user twice

  ELSIF p_action = 'flag' THEN
    -- Flag for review
    UPDATE community_foods
    SET
      is_flagged = true,
      flag_reason = p_flag_reason,
      updated_at = NOW()
    WHERE id = p_food_id;
  END IF;
END;
$$;

-- ═══════════════════════════════════════════════════════════════
-- TRIGGERS
-- ═══════════════════════════════════════════════════════════════

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_community_foods_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_community_foods_timestamp ON community_foods;
CREATE TRIGGER trigger_update_community_foods_timestamp
  BEFORE UPDATE ON community_foods
  FOR EACH ROW
  EXECUTE FUNCTION update_community_foods_timestamp();

-- ═══════════════════════════════════════════════════════════════
-- STORAGE BUCKET FOR LABEL IMAGES
-- ═══════════════════════════════════════════════════════════════
-- Run this in Supabase Dashboard -> Storage

-- INSERT INTO storage.buckets (id, name, public)
-- VALUES ('nutrition-labels', 'nutrition-labels', true);

-- Policy: Anyone can view images
-- CREATE POLICY "Public read access" ON storage.objects
--   FOR SELECT USING (bucket_id = 'nutrition-labels');

-- Policy: Authenticated users can upload
-- CREATE POLICY "Authenticated users can upload" ON storage.objects
--   FOR INSERT WITH CHECK (
--     bucket_id = 'nutrition-labels'
--     AND auth.uid() IS NOT NULL
--   );

-- ═══════════════════════════════════════════════════════════════
-- DONE! Run this SQL in Supabase SQL Editor
-- ═══════════════════════════════════════════════════════════════