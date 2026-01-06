-- =====================================================
-- PROMETHEUS EXERCISES - COMPLETE SETUP
-- =====================================================
-- This script creates the exercises table, populates it with initial data,
-- and disables RLS for testing. Run this in your Supabase SQL Editor.
--
-- INSTRUCTIONS:
-- 1. Go to your Supabase project dashboard
-- 2. Navigate to the SQL Editor
-- 3. Copy and paste this entire script
-- 4. Click "Run" to execute
-- =====================================================

-- =====================================================
-- STEP 1: CREATE EXERCISES TABLE SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS exercises (
    id TEXT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    name TEXT NOT NULL,
    main_muscle_group TEXT,
    secondary_muscle_groups TEXT[], -- Array of muscle groups
    equipment TEXT[], -- Array of equipment
    tempo TEXT,
    rest_time_seconds INTEGER,
    track_reps BOOLEAN DEFAULT true,
    track_sets BOOLEAN DEFAULT true,
    track_weight BOOLEAN DEFAULT true,
    track_rpe BOOLEAN DEFAULT false,
    track_duration BOOLEAN DEFAULT false,
    track_distance BOOLEAN DEFAULT false,
    video_url TEXT,
    tutorial TEXT,
    notes TEXT,
    supports_power_score BOOLEAN DEFAULT false,
    supports_technique_score BOOLEAN DEFAULT false,
    vbt_measurement_type TEXT, -- "average" or "peak"
    vbt_category TEXT, -- "Squat", "Deadlift", "Olympic", "Press", "Row", "Pull"
    sports TEXT[] -- Array of sport categories
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_exercises_name ON exercises(name);
CREATE INDEX IF NOT EXISTS idx_exercises_main_muscle ON exercises(main_muscle_group);
CREATE INDEX IF NOT EXISTS idx_exercises_created_at ON exercises(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_exercises_sports ON exercises USING GIN(sports);
CREATE INDEX IF NOT EXISTS idx_exercises_equipment ON exercises USING GIN(equipment);

-- =====================================================
-- STEP 2: POPULATE WITH INITIAL EXERCISES
-- =====================================================

-- Clear existing data
TRUNCATE TABLE exercises CASCADE;

-- Insert exercises
INSERT INTO exercises (id, name, main_muscle_group, equipment, supports_power_score, supports_technique_score, vbt_category, sports, track_reps, track_sets, track_weight) VALUES
-- Legs
('1', 'Front Squat', 'Legs', ARRAY['Barbell'], true, true, 'Squat', ARRAY['Weightlifting', 'General Strength', 'CrossFit'], true, true, true),
('2', 'Romanian Deadlift', 'Legs', ARRAY['Barbell'], true, true, 'Deadlift', ARRAY['Powerlifting', 'General Strength'], true, true, true),
('4', 'Calf Raises', 'Legs', ARRAY['Machine'], false, false, null, ARRAY['General Strength'], true, true, true),
('5', 'Lunges', 'Legs', ARRAY['Bodyweight'], false, false, null, ARRAY['General Strength', 'CrossFit', 'Hyrox'], true, true, false),
('11', 'Barbell Back Squat', 'Legs', ARRAY['Barbell'], true, true, 'Squat', ARRAY['Powerlifting', 'General Strength'], true, true, true),
('12', 'Bulgarian Split Squat', 'Legs', ARRAY['Dumbbell'], false, false, null, ARRAY['General Strength'], true, true, true),
('18', 'Leg Extension', 'Legs', ARRAY['Machine'], false, false, null, ARRAY['General Strength'], true, true, true),

-- Shoulders
('3', 'Dumbbell Shoulder Press', 'Shoulders', ARRAY['Dumbbell'], false, false, null, ARRAY['General Strength'], true, true, true),
('13', 'Overhead Press', 'Shoulders', ARRAY['Barbell'], true, true, 'Press', ARRAY['Powerlifting', 'General Strength'], true, true, true),
('24', 'Lateral Raises', 'Shoulders', ARRAY['Dumbbell'], false, false, null, ARRAY['General Strength'], true, true, true),

-- Back
('6', 'Conventional Deadlift', 'Back', ARRAY['Barbell'], true, true, 'Deadlift', ARRAY['Powerlifting', 'General Strength'], true, true, true),
('10', 'Cable Row', 'Back', ARRAY['Cable'], false, false, null, ARRAY['General Strength'], true, true, true),
('15', 'Lat Pulldown', 'Back', ARRAY['Machine'], false, false, null, ARRAY['General Strength'], true, true, true),
('20', 'Barbell Row', 'Back', ARRAY['Barbell'], true, false, 'Row', ARRAY['General Strength', 'Powerlifting'], true, true, true),
('23', 'Hyperextensions', 'Back', ARRAY['Bodyweight'], false, false, null, ARRAY['General Strength'], true, true, false),

-- Core
('7', 'Plank', 'Core', ARRAY['Bodyweight'], false, false, null, ARRAY['General Strength', 'CrossFit', 'Hyrox'], false, false, false),
('22', 'Russian Twists', 'Core', ARRAY['Bodyweight'], false, false, null, ARRAY['General Strength', 'CrossFit'], true, true, false),

-- Chest
('8', 'Incline Bench Press', 'Chest', ARRAY['Barbell'], true, true, 'Press', ARRAY['Powerlifting', 'General Strength'], true, true, true),
('14', 'Dips', 'Chest', ARRAY['Bodyweight'], false, false, null, ARRAY['General Strength', 'CrossFit'], true, true, false),
('17', 'Chest Flyes', 'Chest', ARRAY['Dumbbell'], false, false, null, ARRAY['General Strength'], true, true, true),
('19', 'Barbell Bench Press', 'Chest', ARRAY['Barbell'], true, true, 'Press', ARRAY['Powerlifting', 'General Strength'], true, true, true),

-- Arms
('9', 'Tricep Pushdowns', 'Arms', ARRAY['Cable'], false, false, null, ARRAY['General Strength'], true, true, true),
('21', 'Close-Grip Bench Press', 'Arms', ARRAY['Barbell'], true, true, 'Press', ARRAY['Powerlifting', 'General Strength'], true, true, true),

-- Olympic
('16', 'Power Clean', 'Olympic', ARRAY['Barbell'], true, false, 'Olympic', ARRAY['Weightlifting', 'CrossFit'], true, true, true);

-- =====================================================
-- STEP 3: DISABLE ROW LEVEL SECURITY (for testing)
-- =====================================================

ALTER TABLE exercises DISABLE ROW LEVEL SECURITY;

-- =====================================================
-- VERIFICATION
-- =====================================================

-- Count total exercises
SELECT '✅ Total exercises inserted:' as status, COUNT(*) as count FROM exercises;

-- Show exercises by muscle group
SELECT
    main_muscle_group as muscle_group,
    COUNT(*) as exercise_count
FROM exercises
GROUP BY main_muscle_group
ORDER BY exercise_count DESC;

-- =====================================================
-- ✅ SETUP COMPLETE
-- =====================================================
-- Your exercises table is ready!
-- The app should now be able to load exercises from Supabase.