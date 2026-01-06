-- =====================================================
-- COMPLETE FIX: Library zeigt keine Exercises/Workouts
-- =====================================================
-- Führe dieses komplette Script in Supabase SQL Editor aus
-- =====================================================

-- ============ STEP 1: RLS DEAKTIVIEREN ============
DO $$
BEGIN
    RAISE NOTICE '=== Deaktiviere RLS für alle Tabellen ===';
END $$;

ALTER TABLE IF EXISTS exercises DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS exercises_new DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS exercise_view DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS technique_guides DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS workout_templates DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS workout_template_exercises DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS exercise_sets DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS workouts DISABLE ROW LEVEL SECURITY;

-- ============ STEP 2: CHECK EXISTING DATA ============
DO $$
DECLARE
    exercises_count INT;
    templates_count INT;
BEGIN
    -- Check exercises_new
    SELECT COUNT(*) INTO exercises_count FROM exercises_new;
    RAISE NOTICE 'exercises_new hat % Einträge', exercises_count;

    -- Check workout_templates
    SELECT COUNT(*) INTO templates_count FROM workout_templates;
    RAISE NOTICE 'workout_templates hat % Einträge', templates_count;

    IF exercises_count = 0 THEN
        RAISE NOTICE '⚠️ exercises_new ist LEER - füge Seed-Daten ein';
    END IF;

    IF templates_count = 0 THEN
        RAISE NOTICE '⚠️ workout_templates ist LEER - füge Seed-Daten ein';
    END IF;
END $$;

-- ============ STEP 3: SEED EXERCISES (NUR WENN LEER) ============
-- Füge die wichtigsten 10 Exercises ein
INSERT INTO exercises_new (id, name, main_muscle_group, equipment, supports_power_score, supports_technique_score, vbt_category, sports, track_reps, track_sets, track_weight)
SELECT * FROM (VALUES
    ('1', 'Front Squat', 'Legs', ARRAY['Barbell'], true, true, 'Squat', ARRAY['Weightlifting', 'General Strength', 'CrossFit'], true, true, true),
    ('2', 'Romanian Deadlift', 'Legs', ARRAY['Barbell'], true, true, 'Deadlift', ARRAY['Powerlifting', 'General Strength'], true, true, true),
    ('6', 'Conventional Deadlift', 'Back', ARRAY['Barbell'], true, true, 'Deadlift', ARRAY['Powerlifting', 'General Strength'], true, true, true),
    ('11', 'Barbell Back Squat', 'Legs', ARRAY['Barbell'], true, true, 'Squat', ARRAY['Powerlifting', 'General Strength'], true, true, true),
    ('13', 'Overhead Press', 'Shoulders', ARRAY['Barbell'], true, true, 'Press', ARRAY['Powerlifting', 'General Strength'], true, true, true),
    ('19', 'Barbell Bench Press', 'Chest', ARRAY['Barbell'], true, true, 'Press', ARRAY['Powerlifting', 'General Strength'], true, true, true),
    ('20', 'Barbell Row', 'Back', ARRAY['Barbell'], true, false, 'Row', ARRAY['General Strength', 'Powerlifting'], true, true, true),
    ('8', 'Incline Bench Press', 'Chest', ARRAY['Barbell'], true, true, 'Press', ARRAY['Powerlifting', 'General Strength'], true, true, true),
    ('12', 'Bulgarian Split Squat', 'Legs', ARRAY['Dumbbell'], false, false, null, ARRAY['General Strength'], true, true, true),
    ('15', 'Lat Pulldown', 'Back', ARRAY['Machine'], false, false, null, ARRAY['General Strength'], true, true, true)
) AS v(id, name, main_muscle_group, equipment, supports_power_score, supports_technique_score, vbt_category, sports, track_reps, track_sets, track_weight)
WHERE NOT EXISTS (SELECT 1 FROM exercises_new WHERE exercises_new.id = v.id);

-- ============ STEP 4: SEED WORKOUT TEMPLATES (NUR WENN LEER) ============
-- Füge 3 Basic Templates ein
DO $$
DECLARE
    template1_id UUID;
    template2_id UUID;
    template3_id UUID;
    ex1_id UUID;
    ex2_id UUID;
    ex3_id UUID;
BEGIN
    -- Only insert if no templates exist
    IF (SELECT COUNT(*) FROM workout_templates WHERE user_id IS NULL) = 0 THEN
        RAISE NOTICE 'Füge Workout Templates ein...';

        -- Template 1: Powerlifting Basics
        INSERT INTO workout_templates (name, user_id, sports)
        VALUES ('Powerlifting Basics', NULL, ARRAY['Powerlifting'])
        RETURNING id INTO template1_id;

        -- Add exercises to template 1
        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template1_id, '11', 0)  -- Squat
        RETURNING id INTO ex1_id;

        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template1_id, '19', 1)  -- Bench
        RETURNING id INTO ex2_id;

        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template1_id, '6', 2)  -- Deadlift
        RETURNING id INTO ex3_id;

        -- Add sets
        INSERT INTO exercise_sets (workout_exercise_id, set_number, target_reps, target_weight, rest_seconds)
        VALUES
            (ex1_id, 1, 5, 100, 180),
            (ex1_id, 2, 5, 100, 180),
            (ex1_id, 3, 5, 100, 180),
            (ex2_id, 1, 5, 80, 180),
            (ex2_id, 2, 5, 80, 180),
            (ex2_id, 3, 5, 80, 180),
            (ex3_id, 1, 5, 120, 240),
            (ex3_id, 2, 5, 120, 240),
            (ex3_id, 3, 5, 120, 240);

        -- Template 2: Upper Body Strength
        INSERT INTO workout_templates (name, user_id, sports)
        VALUES ('Upper Body Strength', NULL, ARRAY['General Strength'])
        RETURNING id INTO template2_id;

        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template2_id, '19', 0)  -- Bench Press
        RETURNING id INTO ex1_id;

        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template2_id, '20', 1)  -- Barbell Row
        RETURNING id INTO ex2_id;

        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template2_id, '13', 2)  -- Overhead Press
        RETURNING id INTO ex3_id;

        INSERT INTO exercise_sets (workout_exercise_id, set_number, target_reps, target_weight, rest_seconds)
        VALUES
            (ex1_id, 1, 8, 60, 120),
            (ex1_id, 2, 8, 60, 120),
            (ex1_id, 3, 8, 60, 120),
            (ex2_id, 1, 8, 50, 90),
            (ex2_id, 2, 8, 50, 90),
            (ex2_id, 3, 8, 50, 90),
            (ex3_id, 1, 8, 40, 120),
            (ex3_id, 2, 8, 40, 120),
            (ex3_id, 3, 8, 40, 120);

        -- Template 3: Leg Day
        INSERT INTO workout_templates (name, user_id, sports)
        VALUES ('Leg Day', NULL, ARRAY['General Strength'])
        RETURNING id INTO template3_id;

        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template3_id, '11', 0)  -- Back Squat
        RETURNING id INTO ex1_id;

        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template3_id, '2', 1)  -- Romanian Deadlift
        RETURNING id INTO ex2_id;

        INSERT INTO workout_template_exercises (workout_template_id, exercise_id, order_index)
        VALUES (template3_id, '12', 2)  -- Bulgarian Split Squat
        RETURNING id INTO ex3_id;

        INSERT INTO exercise_sets (workout_exercise_id, set_number, target_reps, target_weight, rest_seconds)
        VALUES
            (ex1_id, 1, 10, 80, 120),
            (ex1_id, 2, 10, 80, 120),
            (ex1_id, 3, 10, 80, 120),
            (ex2_id, 1, 10, 60, 90),
            (ex2_id, 2, 10, 60, 90),
            (ex2_id, 3, 10, 60, 90),
            (ex3_id, 1, 12, 20, 90),
            (ex3_id, 2, 12, 20, 90),
            (ex3_id, 3, 12, 20, 90);

        RAISE NOTICE '✅ 3 Workout Templates eingefügt';
    END IF;
END $$;

-- ============ STEP 5: FINAL CHECK ============
SELECT 'FINAL RESULTS:' as info;

SELECT 'exercises_new' as table_name, COUNT(*) as row_count FROM exercises_new
UNION ALL
SELECT 'workout_templates', COUNT(*) FROM workout_templates
UNION ALL
SELECT 'workout_template_exercises', COUNT(*) FROM workout_template_exercises
UNION ALL
SELECT 'exercise_sets', COUNT(*) FROM exercise_sets;

-- Show samples
SELECT '=== SAMPLE EXERCISES ===' as info;
SELECT id, name, main_muscle_group, sports FROM exercises_new LIMIT 5;

SELECT '=== SAMPLE WORKOUT TEMPLATES ===' as info;
SELECT id, name, sports FROM workout_templates WHERE user_id IS NULL LIMIT 5;

-- ✅ FERTIG!
SELECT '✅ SETUP COMPLETE - Jetzt App neu starten!' as status;