-- =====================================================
-- PROMETHEUS TDEE PROFILE MIGRATION
-- =====================================================
-- Adds gender and activity_level columns to user_profiles table
-- for TDEE (Total Daily Energy Expenditure) calculation
-- Run this in Supabase SQL Editor
-- =====================================================

-- =====================================================
-- 1. ADD GENDER COLUMN
-- =====================================================
-- Gender is used for BMR calculation (Mifflin-St Jeor equation)
-- Values: 'male' or 'female'

ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS gender TEXT DEFAULT NULL;

-- Add check constraint for valid gender values
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'user_profiles_gender_check'
    ) THEN
        ALTER TABLE user_profiles
        ADD CONSTRAINT user_profiles_gender_check
        CHECK (gender IS NULL OR gender IN ('male', 'female'));
    END IF;
END $$;

-- =====================================================
-- 2. ADD ACTIVITY LEVEL COLUMN
-- =====================================================
-- Activity level multiplier for TDEE calculation
-- Values: 'sedentary', 'light', 'moderate', 'active', 'very_active'
--
-- Multipliers:
--   sedentary   = 1.2   (Little or no exercise)
--   light       = 1.375 (Light exercise 1-3 days/week)
--   moderate    = 1.55  (Moderate exercise 3-5 days/week)
--   active      = 1.725 (Hard exercise 6-7 days/week)
--   very_active = 1.9   (Very hard exercise, physical job)

ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS activity_level TEXT DEFAULT NULL;

-- Add check constraint for valid activity level values
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'user_profiles_activity_level_check'
    ) THEN
        ALTER TABLE user_profiles
        ADD CONSTRAINT user_profiles_activity_level_check
        CHECK (activity_level IS NULL OR activity_level IN ('sedentary', 'light', 'moderate', 'active', 'very_active'));
    END IF;
END $$;

-- =====================================================
-- 3. VERIFICATION QUERY
-- =====================================================

-- Run this to verify columns were added:
-- SELECT column_name, data_type, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'user_profiles'
-- AND column_name IN ('gender', 'activity_level');

-- =====================================================
-- TDEE CALCULATION REFERENCE
-- =====================================================
--
-- BMR (Basal Metabolic Rate) - Mifflin-St Jeor Equation:
--   Male:   BMR = (10 x weight_kg) + (6.25 x height_cm) - (5 x age_years) + 5
--   Female: BMR = (10 x weight_kg) + (6.25 x height_cm) - (5 x age_years) - 161
--
-- TDEE = BMR x Activity Multiplier
--
-- Goal-Based Adjustments:
--   CUTTING:     TDEE - 20% (caloric deficit for fat loss)
--   MAINTENANCE: TDEE (maintain current weight)
--   BULKING:     TDEE + 15% (caloric surplus for muscle gain)
--   PERFORMANCE: TDEE + 10% (slight surplus for athletes)
--
-- Macro Suggestions (from TDEE):
--   Protein: 2g per kg body weight
--   Fat: 25% of total calories / 9 (kcal per gram)
--   Carbs: Remaining calories / 4 (kcal per gram)
--