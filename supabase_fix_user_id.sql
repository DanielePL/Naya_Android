-- =====================================================
-- FIX: Make user_id nullable for testing
-- =====================================================

-- 1. Drop the foreign key constraint
ALTER TABLE workout_templates
DROP CONSTRAINT IF EXISTS workout_templates_user_id_fkey;

-- 2. Make user_id nullable
ALTER TABLE workout_templates
ALTER COLUMN user_id DROP NOT NULL;

-- ✅ Now the app can save workouts without a valid user_id