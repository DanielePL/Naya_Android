-- ═══════════════════════════════════════════════════════════════════════════════
-- PROMETHEUS RESTAURANT SCHEMA - MISSING TABLES & FUNCTIONS
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Existing tables (already in Supabase):
--   ✅ restaurant_chains
--   ✅ restaurants
--   ✅ restaurant_meals
--
-- This file creates:
--   • Helper functions (set_restaurant_location, update_updated_at, find_nearby)
--   • user_restaurant_history
--   • meal_submissions
--   • user_food_contribution_stats
--   • RLS policies
--
-- ═══════════════════════════════════════════════════════════════════════════════


-- ═══════════════════════════════════════════════════════════════════════════════
-- HELPER FUNCTIONS
-- ═══════════════════════════════════════════════════════════════════════════════

-- Enable PostGIS if not already enabled
CREATE EXTENSION IF NOT EXISTS postgis;

-- Enable pg_trgm for fuzzy text search (used by existing indexes)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Function: Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function: Auto-set geography location from lat/lng
CREATE OR REPLACE FUNCTION set_restaurant_location()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
    NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function: Find nearby restaurants
DROP FUNCTION IF EXISTS find_nearby_restaurants(double precision, double precision, integer, integer);
CREATE OR REPLACE FUNCTION find_nearby_restaurants(
  p_latitude DOUBLE PRECISION,
  p_longitude DOUBLE PRECISION,
  p_radius_meters INTEGER DEFAULT 5000,
  p_limit INTEGER DEFAULT 20
)
RETURNS TABLE(
  id UUID,
  name TEXT,
  chain_name TEXT,
  chain_logo TEXT,
  chain_cuisine_type TEXT,
  distance_meters DOUBLE PRECISION,
  total_meals INTEGER,
  is_verified BOOLEAN,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION
)
LANGUAGE sql
STABLE
AS $$
  SELECT
    r.id,
    r.name,
    c.name as chain_name,
    c.logo_url as chain_logo,
    c.cuisine_type as chain_cuisine_type,
    ST_Distance(
      r.location,
      ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography
    ) as distance_meters,
    r.total_meals,
    r.is_verified,
    r.latitude,
    r.longitude
  FROM restaurants r
  LEFT JOIN restaurant_chains c ON c.id = r.chain_id
  WHERE r.is_active = true
    AND ST_DWithin(
      r.location,
      ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography,
      p_radius_meters
    )
  ORDER BY distance_meters ASC
  LIMIT p_limit;
$$;


-- ═══════════════════════════════════════════════════════════════════════════════
-- USER RESTAURANT HISTORY
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS user_restaurant_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
  meal_id UUID REFERENCES restaurant_meals(id) ON DELETE SET NULL,
  nutrition_log_id UUID REFERENCES nutrition_logs(id) ON DELETE SET NULL,

  -- Usage tracking
  times_visited INTEGER DEFAULT 1,
  last_visited_at TIMESTAMPTZ DEFAULT NOW(),

  -- Favorites (JSONB array of meal IDs)
  favorite_meal_ids JSONB DEFAULT '[]'::jsonb,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  -- Unique constraint: one history entry per user per restaurant
  UNIQUE(user_id, restaurant_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_user_restaurant_history_user
  ON user_restaurant_history(user_id);
CREATE INDEX IF NOT EXISTS idx_user_restaurant_history_restaurant
  ON user_restaurant_history(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_user_restaurant_history_last_visited
  ON user_restaurant_history(user_id, last_visited_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_restaurant_history_frequent
  ON user_restaurant_history(user_id, times_visited DESC)
  WHERE times_visited >= 3;

-- Trigger for updated_at
DROP TRIGGER IF EXISTS trg_user_restaurant_history_updated_at ON user_restaurant_history;
CREATE TRIGGER trg_user_restaurant_history_updated_at
  BEFORE UPDATE ON user_restaurant_history
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();


-- ═══════════════════════════════════════════════════════════════════════════════
-- MEAL SUBMISSIONS (Crowdsourced)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS meal_submissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Relationships
  restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
  meal_id UUID REFERENCES restaurant_meals(id) ON DELETE SET NULL,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

  -- Submission data
  meal_name TEXT NOT NULL,

  -- Nutrition values
  calories DOUBLE PRECISION NOT NULL,
  protein DOUBLE PRECISION NOT NULL,
  carbs DOUBLE PRECISION NOT NULL,
  fat DOUBLE PRECISION NOT NULL,

  -- Media & Analysis
  photo_url TEXT,
  ai_analysis_json JSONB,
  notes TEXT,

  -- Location verification
  submission_latitude DOUBLE PRECISION,
  submission_longitude DOUBLE PRECISION,
  distance_from_restaurant DOUBLE PRECISION,

  -- Review status
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected', 'merged')),
  reviewed_by UUID REFERENCES auth.users(id),
  reviewed_at TIMESTAMPTZ,
  rejection_reason TEXT,

  -- Community voting
  upvotes INTEGER DEFAULT 0,
  downvotes INTEGER DEFAULT 0,
  trust_score DOUBLE PRECISION,

  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_meal_submissions_restaurant
  ON meal_submissions(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_meal_submissions_user
  ON meal_submissions(user_id);
CREATE INDEX IF NOT EXISTS idx_meal_submissions_status
  ON meal_submissions(status);
CREATE INDEX IF NOT EXISTS idx_meal_submissions_pending
  ON meal_submissions(created_at DESC)
  WHERE status = 'pending';


-- ═══════════════════════════════════════════════════════════════════════════════
-- USER FOOD CONTRIBUTION STATS (Gamification)
-- ═══════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS user_food_contribution_stats (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,

  -- Submission counts
  total_submissions INTEGER DEFAULT 0,
  approved_submissions INTEGER DEFAULT 0,
  rejected_submissions INTEGER DEFAULT 0,

  -- Verification activity
  total_verifications INTEGER DEFAULT 0,

  -- Trust & Level
  trust_score DOUBLE PRECISION DEFAULT 0.5,
  contribution_level TEXT DEFAULT 'newcomer'
    CHECK (contribution_level IN ('newcomer', 'contributor', 'trusted', 'expert')),

  -- Badges (JSONB array of badge IDs)
  badges JSONB DEFAULT '[]'::jsonb,

  -- Geographic contributions
  countries_contributed JSONB DEFAULT '[]'::jsonb,
  cities_contributed JSONB DEFAULT '[]'::jsonb,

  -- Streaks
  current_streak INTEGER DEFAULT 0,
  longest_streak INTEGER DEFAULT 0,
  last_contribution_at TIMESTAMPTZ,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_contribution_stats_level
  ON user_food_contribution_stats(contribution_level);
CREATE INDEX IF NOT EXISTS idx_contribution_stats_trust
  ON user_food_contribution_stats(trust_score DESC);
CREATE INDEX IF NOT EXISTS idx_contribution_stats_streak
  ON user_food_contribution_stats(current_streak DESC)
  WHERE current_streak > 0;

-- Trigger for updated_at
DROP TRIGGER IF EXISTS trg_contribution_stats_updated_at ON user_food_contribution_stats;
CREATE TRIGGER trg_contribution_stats_updated_at
  BEFORE UPDATE ON user_food_contribution_stats
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();


-- ═══════════════════════════════════════════════════════════════════════════════
-- ROW LEVEL SECURITY (RLS)
-- ═══════════════════════════════════════════════════════════════════════════════

-- Enable RLS
ALTER TABLE user_restaurant_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_food_contribution_stats ENABLE ROW LEVEL SECURITY;

-- ─────────────────────────────────────────────────────────────────────────────
-- USER RESTAURANT HISTORY POLICIES
-- ─────────────────────────────────────────────────────────────────────────────

DROP POLICY IF EXISTS "Users can view own restaurant history" ON user_restaurant_history;
CREATE POLICY "Users can view own restaurant history" ON user_restaurant_history
  FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own restaurant history" ON user_restaurant_history;
CREATE POLICY "Users can insert own restaurant history" ON user_restaurant_history
  FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own restaurant history" ON user_restaurant_history;
CREATE POLICY "Users can update own restaurant history" ON user_restaurant_history
  FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own restaurant history" ON user_restaurant_history;
CREATE POLICY "Users can delete own restaurant history" ON user_restaurant_history
  FOR DELETE USING (auth.uid() = user_id);

-- Coach can view client's restaurant history
DROP POLICY IF EXISTS "Coaches can view client restaurant history" ON user_restaurant_history;
CREATE POLICY "Coaches can view client restaurant history" ON user_restaurant_history
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM coach_client_connections ccc
      WHERE ccc.client_id = user_restaurant_history.user_id
        AND ccc.coach_id = auth.uid()
        AND ccc.status = 'accepted'
    )
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- MEAL SUBMISSIONS POLICIES
-- ─────────────────────────────────────────────────────────────────────────────

-- Anyone can view approved submissions
DROP POLICY IF EXISTS "Anyone can view approved submissions" ON meal_submissions;
CREATE POLICY "Anyone can view approved submissions" ON meal_submissions
  FOR SELECT USING (status = 'approved');

-- Users can view their own submissions (any status)
DROP POLICY IF EXISTS "Users can view own submissions" ON meal_submissions;
CREATE POLICY "Users can view own submissions" ON meal_submissions
  FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert submissions" ON meal_submissions;
CREATE POLICY "Users can insert submissions" ON meal_submissions
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Users can update their own pending submissions
DROP POLICY IF EXISTS "Users can update own pending submissions" ON meal_submissions;
CREATE POLICY "Users can update own pending submissions" ON meal_submissions
  FOR UPDATE USING (auth.uid() = user_id AND status = 'pending');

-- Users can delete their own pending submissions
DROP POLICY IF EXISTS "Users can delete own pending submissions" ON meal_submissions;
CREATE POLICY "Users can delete own pending submissions" ON meal_submissions
  FOR DELETE USING (auth.uid() = user_id AND status = 'pending');

-- ─────────────────────────────────────────────────────────────────────────────
-- USER FOOD CONTRIBUTION STATS POLICIES
-- ─────────────────────────────────────────────────────────────────────────────

-- Anyone can view contribution stats (leaderboard)
DROP POLICY IF EXISTS "Anyone can view contribution stats" ON user_food_contribution_stats;
CREATE POLICY "Anyone can view contribution stats" ON user_food_contribution_stats
  FOR SELECT USING (true);

-- Only system can insert/update (via triggers/functions)
DROP POLICY IF EXISTS "Users can insert own stats" ON user_food_contribution_stats;
CREATE POLICY "Users can insert own stats" ON user_food_contribution_stats
  FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own stats" ON user_food_contribution_stats;
CREATE POLICY "Users can update own stats" ON user_food_contribution_stats
  FOR UPDATE USING (auth.uid() = user_id);


-- ═══════════════════════════════════════════════════════════════════════════════
-- HELPER FUNCTIONS FOR RESTAURANT TRACKING
-- ═══════════════════════════════════════════════════════════════════════════════

-- Function: Record restaurant visit (upsert history)
CREATE OR REPLACE FUNCTION record_restaurant_visit(
  p_user_id UUID,
  p_restaurant_id UUID,
  p_meal_id UUID DEFAULT NULL,
  p_nutrition_log_id UUID DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_history_id UUID;
BEGIN
  INSERT INTO user_restaurant_history (
    user_id, restaurant_id, meal_id, nutrition_log_id, times_visited, last_visited_at
  )
  VALUES (
    p_user_id, p_restaurant_id, p_meal_id, p_nutrition_log_id, 1, NOW()
  )
  ON CONFLICT (user_id, restaurant_id) DO UPDATE SET
    times_visited = user_restaurant_history.times_visited + 1,
    last_visited_at = NOW(),
    meal_id = COALESCE(EXCLUDED.meal_id, user_restaurant_history.meal_id),
    nutrition_log_id = COALESCE(EXCLUDED.nutrition_log_id, user_restaurant_history.nutrition_log_id),
    updated_at = NOW()
  RETURNING id INTO v_history_id;

  RETURN v_history_id;
END;
$$;

-- Function: Get user's frequent restaurants ("Your Usuals")
DROP FUNCTION IF EXISTS get_user_usual_restaurants(uuid, integer, integer);
CREATE OR REPLACE FUNCTION get_user_usual_restaurants(
  p_user_id UUID,
  p_min_visits INTEGER DEFAULT 3,
  p_limit INTEGER DEFAULT 10
)
RETURNS TABLE(
  history_id UUID,
  restaurant_id UUID,
  restaurant_name TEXT,
  chain_name TEXT,
  chain_logo TEXT,
  times_visited INTEGER,
  last_visited_at TIMESTAMPTZ,
  favorite_meals JSONB
)
LANGUAGE sql
STABLE
AS $$
  SELECT
    h.id as history_id,
    r.id as restaurant_id,
    r.name as restaurant_name,
    c.name as chain_name,
    c.logo_url as chain_logo,
    h.times_visited,
    h.last_visited_at,
    h.favorite_meal_ids as favorite_meals
  FROM user_restaurant_history h
  JOIN restaurants r ON r.id = h.restaurant_id
  LEFT JOIN restaurant_chains c ON c.id = r.chain_id
  WHERE h.user_id = p_user_id
    AND h.times_visited >= p_min_visits
  ORDER BY h.times_visited DESC, h.last_visited_at DESC
  LIMIT p_limit;
$$;

-- Function: Update contribution stats after submission
CREATE OR REPLACE FUNCTION update_contribution_stats_on_submission()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  -- Ensure stats record exists
  INSERT INTO user_food_contribution_stats (user_id)
  VALUES (NEW.user_id)
  ON CONFLICT (user_id) DO NOTHING;

  -- Update total submissions
  UPDATE user_food_contribution_stats
  SET
    total_submissions = total_submissions + 1,
    last_contribution_at = NOW(),
    current_streak = CASE
      WHEN last_contribution_at IS NULL OR
           last_contribution_at < NOW() - INTERVAL '2 days'
      THEN 1
      ELSE current_streak + 1
    END,
    longest_streak = GREATEST(
      longest_streak,
      CASE
        WHEN last_contribution_at IS NULL OR
             last_contribution_at < NOW() - INTERVAL '2 days'
        THEN 1
        ELSE current_streak + 1
      END
    ),
    updated_at = NOW()
  WHERE user_id = NEW.user_id;

  RETURN NEW;
END;
$$;

-- Trigger: Auto-update stats on new submission
DROP TRIGGER IF EXISTS trg_submission_update_stats ON meal_submissions;
CREATE TRIGGER trg_submission_update_stats
  AFTER INSERT ON meal_submissions
  FOR EACH ROW EXECUTE FUNCTION update_contribution_stats_on_submission();

-- Function: Update stats when submission is reviewed
CREATE OR REPLACE FUNCTION update_contribution_stats_on_review()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  IF OLD.status = 'pending' AND NEW.status IN ('approved', 'rejected') THEN
    UPDATE user_food_contribution_stats
    SET
      approved_submissions = approved_submissions + CASE WHEN NEW.status = 'approved' THEN 1 ELSE 0 END,
      rejected_submissions = rejected_submissions + CASE WHEN NEW.status = 'rejected' THEN 1 ELSE 0 END,
      trust_score = CASE
        WHEN total_submissions > 0 THEN
          (approved_submissions + CASE WHEN NEW.status = 'approved' THEN 1 ELSE 0 END)::DOUBLE PRECISION /
          total_submissions::DOUBLE PRECISION
        ELSE 0.5
      END,
      contribution_level = CASE
        WHEN approved_submissions >= 50 AND trust_score >= 0.9 THEN 'expert'
        WHEN approved_submissions >= 20 AND trust_score >= 0.8 THEN 'trusted'
        WHEN approved_submissions >= 5 THEN 'contributor'
        ELSE 'newcomer'
      END,
      updated_at = NOW()
    WHERE user_id = NEW.user_id;
  END IF;

  RETURN NEW;
END;
$$;

-- Trigger: Auto-update stats on submission review
DROP TRIGGER IF EXISTS trg_submission_review_stats ON meal_submissions;
CREATE TRIGGER trg_submission_review_stats
  AFTER UPDATE ON meal_submissions
  FOR EACH ROW EXECUTE FUNCTION update_contribution_stats_on_review();


-- ═══════════════════════════════════════════════════════════════════════════════
-- SEARCH FUNCTIONS
-- ═══════════════════════════════════════════════════════════════════════════════

-- Function: Search restaurant meals by name (fuzzy)
CREATE OR REPLACE FUNCTION search_restaurant_meals(
  p_query TEXT,
  p_chain_id UUID DEFAULT NULL,
  p_region TEXT DEFAULT NULL,
  p_limit INTEGER DEFAULT 20
)
RETURNS TABLE(
  id UUID,
  name TEXT,
  chain_id UUID,
  chain_name TEXT,
  chain_logo TEXT,
  category TEXT,
  calories DOUBLE PRECISION,
  protein DOUBLE PRECISION,
  carbs DOUBLE PRECISION,
  fat DOUBLE PRECISION,
  is_verified BOOLEAN,
  is_popular BOOLEAN,
  similarity REAL
)
LANGUAGE sql
STABLE
AS $$
  SELECT
    m.id,
    m.name,
    m.chain_id,
    c.name as chain_name,
    c.logo_url as chain_logo,
    m.category,
    m.calories,
    m.protein,
    m.carbs,
    m.fat,
    m.is_verified,
    m.is_popular,
    similarity(m.name, p_query) as similarity
  FROM restaurant_meals m
  LEFT JOIN restaurant_chains c ON c.id = m.chain_id
  WHERE
    m.name % p_query
    AND (p_chain_id IS NULL OR m.chain_id = p_chain_id)
    AND (p_region IS NULL OR m.region = p_region OR m.region = 'GLOBAL')
  ORDER BY
    m.is_popular DESC,
    similarity(m.name, p_query) DESC,
    m.popularity_score DESC
  LIMIT p_limit;
$$;


-- ═══════════════════════════════════════════════════════════════════════════════
-- DONE! Run this SQL in Supabase SQL Editor
-- ═══════════════════════════════════════════════════════════════════════════════