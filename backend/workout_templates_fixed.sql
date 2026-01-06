-- ============================================
-- PROMETHEUS - EXAMPLE WORKOUT TEMPLATES (FIXED FOR ARRAY FIELDS)
-- ============================================
-- This script creates example workout templates for different training styles
-- Compatible with Supabase SQL Editor
-- FIXED: equipment field is text[] array, using array_to_string() for ILIKE queries

-- ============================================
-- 1. POWERLIFTING WORKOUTS
-- ============================================

-- Powerlifting - Day 1: Squat Focus
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    -- Create workout template
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Powerlifting - Squat Day', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    -- Add Back Squat
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%squat%'
      AND name NOT ILIKE '%bulgarian%'
      AND name NOT ILIKE '%front%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Back Squat', 'Legs', 'Barbell', 1, NOW());
    END IF;

    -- Add Leg Press
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%leg press%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Leg Press', 'Legs', 'Machine', 2, NOW());
    END IF;

    -- Add Leg Curl
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%leg curl%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Leg Curl', 'Legs', 'Machine', 3, NOW());
    END IF;

    RAISE NOTICE 'Created: Powerlifting - Squat Day';
END $$;

-- Powerlifting - Day 2: Bench Press Focus
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Powerlifting - Bench Day', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%bench press%'
      AND name NOT ILIKE '%incline%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Bench Press', 'Chest', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%incline%bench%'
       OR name ILIKE '%incline%press%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Incline Bench Press', 'Chest', 'Barbell', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%dip%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Tricep Dips', 'Triceps', 'Bodyweight', 3, NOW());
    END IF;

    RAISE NOTICE 'Created: Powerlifting - Bench Day';
END $$;

-- Powerlifting - Day 3: Deadlift Focus
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Powerlifting - Deadlift Day', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%deadlift%'
      AND name NOT ILIKE '%romanian%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Deadlift', 'Back', 'Barbell', 1, NOW());
    END IF;

    -- FIXED: Using array_to_string for equipment array
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%barbell%row%'
       OR (name ILIKE '%row%' AND array_to_string(equipment, ' ') ILIKE '%barbell%')
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Barbell Row', 'Back', 'Barbell', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%pull%up%'
       OR name ILIKE '%pullup%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Pull-ups', 'Back', 'Bodyweight', 3, NOW());
    END IF;

    RAISE NOTICE 'Created: Powerlifting - Deadlift Day';
END $$;

-- ============================================
-- 2. OLYMPIC WEIGHTLIFTING WORKOUTS
-- ============================================

-- Weightlifting - Snatch Day
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Weightlifting - Snatch Day', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%snatch%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Power Snatch', 'Full Body', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%overhead%squat%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Overhead Squat', 'Legs', 'Barbell', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%overhead%press%'
       OR name ILIKE '%shoulder%press%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Overhead Press', 'Shoulders', 'Barbell', 3, NOW());
    END IF;

    RAISE NOTICE 'Created: Weightlifting - Snatch Day';
END $$;

-- Weightlifting - Clean & Jerk Day
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Weightlifting - Clean & Jerk Day', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%clean%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Power Clean', 'Full Body', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%front%squat%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Front Squat', 'Legs', 'Barbell', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%push%press%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Push Press', 'Shoulders', 'Barbell', 3, NOW());
    END IF;

    RAISE NOTICE 'Created: Weightlifting - Clean & Jerk Day';
END $$;

-- ============================================
-- 3. GENERAL STRENGTH WORKOUTS
-- ============================================

-- General Strength - Full Body A
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'General Strength - Full Body A', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%squat%'
      AND name NOT ILIKE '%bulgarian%'
      AND name NOT ILIKE '%front%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Back Squat', 'Legs', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%bench%press%'
      AND name NOT ILIKE '%incline%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Bench Press', 'Chest', 'Barbell', 2, NOW());
    END IF;

    -- FIXED: Using array_to_string for equipment array
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%row%'
      AND array_to_string(equipment, ' ') ILIKE '%barbell%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Barbell Row', 'Back', 'Barbell', 3, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%plank%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Plank', 'Core', 'Bodyweight', 4, NOW());
    END IF;

    RAISE NOTICE 'Created: General Strength - Full Body A';
END $$;

-- General Strength - Full Body B
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'General Strength - Full Body B', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%deadlift%'
      AND name NOT ILIKE '%romanian%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Deadlift', 'Back', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%overhead%press%'
       OR name ILIKE '%shoulder%press%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Overhead Press', 'Shoulders', 'Barbell', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%pull%up%'
       OR name ILIKE '%pullup%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Pull-ups', 'Back', 'Bodyweight', 3, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%lunge%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Walking Lunges', 'Legs', 'Bodyweight', 4, NOW());
    END IF;

    RAISE NOTICE 'Created: General Strength - Full Body B';
END $$;

-- ============================================
-- 4. HYROX WORKOUTS
-- ============================================

-- Hyrox - Running & Strength
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Hyrox - Running & Strength', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%run%'
       OR name ILIKE '%treadmill%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, '1km Run', 'Cardio', 'None', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%burpee%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Burpees', 'Full Body', 'Bodyweight', 2, NOW());
    END IF;

    -- FIXED: Using array_to_string for equipment array
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%row%'
      AND array_to_string(equipment, ' ') ILIKE '%machine%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Rowing Machine', 'Cardio', 'Machine', 3, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%wall%ball%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Wall Balls', 'Legs', 'Medicine Ball', 4, NOW());
    END IF;

    RAISE NOTICE 'Created: Hyrox - Running & Strength';
END $$;

-- Hyrox - Sled & Carries
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Hyrox - Sled & Carries', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%sled%push%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Sled Push', 'Legs', 'Sled', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%sled%pull%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Sled Pull', 'Full Body', 'Sled', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%farmer%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Farmers Carry', 'Full Body', 'Dumbbells', 3, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%sandbag%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Sandbag Lunges', 'Legs', 'Sandbag', 4, NOW());
    END IF;

    RAISE NOTICE 'Created: Hyrox - Sled & Carries';
END $$;

-- ============================================
-- 5. CROSSFIT WORKOUTS
-- ============================================

-- CrossFit - Fran
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'CrossFit - Fran', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%thruster%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Thruster', 'Full Body', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%pull%up%'
       OR name ILIKE '%pullup%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Pull-ups', 'Back', 'Bodyweight', 2, NOW());
    END IF;

    RAISE NOTICE 'Created: CrossFit - Fran';
END $$;

-- CrossFit - Cindy
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'CrossFit - Cindy', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%pull%up%'
       OR name ILIKE '%pullup%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Pull-ups', 'Back', 'Bodyweight', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%push%up%'
       OR name ILIKE '%pushup%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Push-ups', 'Chest', 'Bodyweight', 2, NOW());
    END IF;

    -- FIXED: Using array_to_string for equipment array
    SELECT id INTO v_exercise_id FROM exercises
    WHERE (name ILIKE '%air%squat%'
       OR (name ILIKE '%squat%' AND array_to_string(equipment, ' ') ILIKE '%bodyweight%'))
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Air Squats', 'Legs', 'Bodyweight', 3, NOW());
    END IF;

    RAISE NOTICE 'Created: CrossFit - Cindy';
END $$;

-- CrossFit - Heavy Metcon
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'CrossFit - Heavy Metcon', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%clean%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Power Clean', 'Full Body', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%burpee%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Burpee Box Jump Overs', 'Full Body', 'Bodyweight', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%box%jump%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Box Jumps', 'Legs', 'Plyo Box', 3, NOW());
    END IF;

    -- FIXED: Using array_to_string for equipment array
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%row%'
      AND array_to_string(equipment, ' ') ILIKE '%machine%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Rowing', 'Cardio', 'Machine', 4, NOW());
    END IF;

    RAISE NOTICE 'Created: CrossFit - Heavy Metcon';
END $$;

-- ============================================
-- 6. BODYBUILDING WORKOUTS
-- ============================================

-- Bodybuilding - Chest & Triceps
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Bodybuilding - Chest & Triceps', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%bench%press%'
      AND name NOT ILIKE '%incline%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Bench Press', 'Chest', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%incline%'
      AND (name ILIKE '%press%' OR name ILIKE '%bench%')
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Incline Dumbbell Press', 'Chest', 'Dumbbells', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE (name ILIKE '%fly%' OR name ILIKE '%flye%')
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Cable Flyes', 'Chest', 'Cable', 3, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%tricep%'
      AND name ILIKE '%pushdown%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Tricep Pushdown', 'Triceps', 'Cable', 4, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%skull%'
       OR name ILIKE '%lying%tricep%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Skull Crushers', 'Triceps', 'Barbell', 5, NOW());
    END IF;

    RAISE NOTICE 'Created: Bodybuilding - Chest & Triceps';
END $$;

-- Bodybuilding - Back & Biceps
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Bodybuilding - Back & Biceps', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%deadlift%'
      AND name NOT ILIKE '%romanian%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Deadlift', 'Back', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%pull%up%'
       OR name ILIKE '%pullup%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Weighted Pull-ups', 'Back', 'Bodyweight', 2, NOW());
    END IF;

    -- FIXED: Using array_to_string for equipment array
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%row%'
      AND array_to_string(equipment, ' ') ILIKE '%barbell%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Barbell Row', 'Back', 'Barbell', 3, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%lat%pulldown%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Lat Pulldown', 'Back', 'Cable', 4, NOW());
    END IF;

    -- FIXED: Using array_to_string for equipment array
    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%curl%'
      AND array_to_string(equipment, ' ') ILIKE '%barbell%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Barbell Curl', 'Biceps', 'Barbell', 5, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%hammer%curl%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Hammer Curls', 'Biceps', 'Dumbbells', 6, NOW());
    END IF;

    RAISE NOTICE 'Created: Bodybuilding - Back & Biceps';
END $$;

-- Bodybuilding - Leg Day
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Bodybuilding - Leg Day', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%squat%'
      AND name NOT ILIKE '%bulgarian%'
      AND name NOT ILIKE '%front%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Back Squat', 'Legs', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%leg%press%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Leg Press', 'Legs', 'Machine', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%romanian%deadlift%'
       OR name ILIKE '%rdl%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Romanian Deadlift', 'Legs', 'Barbell', 3, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%leg%curl%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Leg Curl', 'Legs', 'Machine', 4, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%leg%extension%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Leg Extension', 'Legs', 'Machine', 5, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%calf%raise%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Calf Raises', 'Legs', 'Machine', 6, NOW());
    END IF;

    RAISE NOTICE 'Created: Bodybuilding - Leg Day';
END $$;

-- Bodybuilding - Shoulders & Abs
DO $$
DECLARE
    v_workout_id UUID;
    v_exercise_id UUID;
BEGIN
    INSERT INTO workout_templates (id, name, user_id, created_at, updated_at)
    VALUES (gen_random_uuid(), 'Bodybuilding - Shoulders & Abs', NULL, NOW(), NOW())
    RETURNING id INTO v_workout_id;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%overhead%press%'
       OR name ILIKE '%shoulder%press%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Overhead Press', 'Shoulders', 'Barbell', 1, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%lateral%raise%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Lateral Raises', 'Shoulders', 'Dumbbells', 2, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%rear%delt%'
       OR name ILIKE '%reverse%fly%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Rear Delt Flyes', 'Shoulders', 'Dumbbells', 3, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%shrug%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Barbell Shrugs', 'Traps', 'Barbell', 4, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%crunch%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Cable Crunches', 'Core', 'Cable', 5, NOW());
    END IF;

    SELECT id INTO v_exercise_id FROM exercises
    WHERE name ILIKE '%plank%'
    LIMIT 1;
    IF v_exercise_id IS NOT NULL THEN
        INSERT INTO workout_template_exercises (id, workout_template_id, exercise_id, exercise_name, muscle_group, equipment, order_index, created_at)
        VALUES (gen_random_uuid(), v_workout_id, v_exercise_id, 'Plank', 'Core', 'Bodyweight', 6, NOW());
    END IF;

    RAISE NOTICE 'Created: Bodybuilding - Shoulders & Abs';
END $$;

-- ============================================
-- COMPLETION
-- ============================================

SELECT 'Successfully created example workout templates!' AS status,
       COUNT(*) AS total_workouts
FROM workout_templates
WHERE user_id IS NULL;