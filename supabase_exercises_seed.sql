-- =====================================================
-- PROMETHEUS EXERCISES - SEED DATA
-- =====================================================
-- Populate the exercises table with initial exercises
-- Run this after creating the exercises table schema

-- Clear existing data (optional - remove in production)
TRUNCATE TABLE exercises;

-- Insert exercises from the app's library
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
-- VERIFICATION
-- =====================================================

-- Count total exercises
SELECT COUNT(*) as total_exercises FROM exercises;

-- Show exercises by sport category
SELECT
    unnest(sports) as sport,
    COUNT(*) as exercise_count
FROM exercises
GROUP BY sport
ORDER BY exercise_count DESC;