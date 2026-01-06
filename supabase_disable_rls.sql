-- =====================================================
-- DISABLE ROW LEVEL SECURITY FOR TESTING
-- =====================================================
-- This allows the app to insert/update/delete without authentication
-- Re-enable RLS when you implement proper authentication!

ALTER TABLE workout_templates DISABLE ROW LEVEL SECURITY;
ALTER TABLE workout_template_exercises DISABLE ROW LEVEL SECURITY;
ALTER TABLE exercise_sets DISABLE ROW LEVEL SECURITY;

-- ✅ RLS disabled - the app can now save workouts without authentication
