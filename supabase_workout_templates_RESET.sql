-- =====================================================
-- RESET & RECREATE WORKOUT TEMPLATES SCHEMA
-- =====================================================
-- This script drops all existing tables and policies, then recreates them

-- =====================================================
-- 1. DROP ALL POLICIES
-- =====================================================

DROP POLICY IF EXISTS "Users can view their own workout templates" ON workout_templates;
DROP POLICY IF EXISTS "Users can insert their own workout templates" ON workout_templates;
DROP POLICY IF EXISTS "Users can update their own workout templates" ON workout_templates;
DROP POLICY IF EXISTS "Users can delete their own workout templates" ON workout_templates;

DROP POLICY IF EXISTS "Users can view exercises in their workout templates" ON workout_template_exercises;
DROP POLICY IF EXISTS "Users can insert exercises in their workout templates" ON workout_template_exercises;
DROP POLICY IF EXISTS "Users can update exercises in their workout templates" ON workout_template_exercises;
DROP POLICY IF EXISTS "Users can delete exercises in their workout templates" ON workout_template_exercises;

DROP POLICY IF EXISTS "Users can view sets in their workout exercises" ON exercise_sets;
DROP POLICY IF EXISTS "Users can insert sets in their workout exercises" ON exercise_sets;
DROP POLICY IF EXISTS "Users can update sets in their workout exercises" ON exercise_sets;
DROP POLICY IF EXISTS "Users can delete sets in their workout exercises" ON exercise_sets;

-- =====================================================
-- 2. DROP ALL TABLES (CASCADE will drop foreign keys)
-- =====================================================

DROP TABLE IF EXISTS exercise_sets CASCADE;
DROP TABLE IF EXISTS workout_template_exercises CASCADE;
DROP TABLE IF EXISTS workout_templates CASCADE;

-- =====================================================
-- 3. DROP FUNCTIONS
-- =====================================================

DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;

-- =====================================================
-- 4. CREATE TABLES
-- =====================================================

-- Workout Templates Table
CREATE TABLE workout_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE
);

-- Workout Template Exercises Table
CREATE TABLE workout_template_exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_template_id UUID NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    exercise_id TEXT NOT NULL,
    exercise_name TEXT NOT NULL,
    muscle_group TEXT NOT NULL,
    equipment TEXT NOT NULL,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Exercise Sets Table
CREATE TABLE exercise_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_exercise_id UUID NOT NULL REFERENCES workout_template_exercises(id) ON DELETE CASCADE,
    set_number INTEGER NOT NULL,
    target_reps INTEGER NOT NULL,
    target_weight DECIMAL(10, 2) NOT NULL,
    rest_seconds INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- 5. CREATE INDEXES
-- =====================================================

CREATE INDEX idx_workout_templates_user_id ON workout_templates(user_id);
CREATE INDEX idx_workout_templates_created_at ON workout_templates(created_at DESC);
CREATE INDEX idx_workout_template_exercises_workout_id ON workout_template_exercises(workout_template_id);
CREATE INDEX idx_workout_template_exercises_order ON workout_template_exercises(workout_template_id, order_index);
CREATE INDEX idx_exercise_sets_workout_exercise_id ON exercise_sets(workout_exercise_id);
CREATE INDEX idx_exercise_sets_set_number ON exercise_sets(workout_exercise_id, set_number);

-- =====================================================
-- 6. ENABLE ROW LEVEL SECURITY
-- =====================================================

ALTER TABLE workout_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE workout_template_exercises ENABLE ROW LEVEL SECURITY;
ALTER TABLE exercise_sets ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- 7. CREATE POLICIES FOR WORKOUT_TEMPLATES
-- =====================================================

CREATE POLICY "Users can view their own workout templates"
    ON workout_templates FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own workout templates"
    ON workout_templates FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own workout templates"
    ON workout_templates FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own workout templates"
    ON workout_templates FOR DELETE
    USING (auth.uid() = user_id);

-- =====================================================
-- 8. CREATE POLICIES FOR WORKOUT_TEMPLATE_EXERCISES
-- =====================================================

CREATE POLICY "Users can view exercises in their workout templates"
    ON workout_template_exercises FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM workout_templates
            WHERE workout_templates.id = workout_template_exercises.workout_template_id
            AND workout_templates.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert exercises in their workout templates"
    ON workout_template_exercises FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM workout_templates
            WHERE workout_templates.id = workout_template_exercises.workout_template_id
            AND workout_templates.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update exercises in their workout templates"
    ON workout_template_exercises FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM workout_templates
            WHERE workout_templates.id = workout_template_exercises.workout_template_id
            AND workout_templates.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete exercises in their workout templates"
    ON workout_template_exercises FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM workout_templates
            WHERE workout_templates.id = workout_template_exercises.workout_template_id
            AND workout_templates.user_id = auth.uid()
        )
    );

-- =====================================================
-- 9. CREATE POLICIES FOR EXERCISE_SETS
-- =====================================================

CREATE POLICY "Users can view sets in their workout exercises"
    ON exercise_sets FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM workout_template_exercises
            JOIN workout_templates ON workout_templates.id = workout_template_exercises.workout_template_id
            WHERE workout_template_exercises.id = exercise_sets.workout_exercise_id
            AND workout_templates.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert sets in their workout exercises"
    ON exercise_sets FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM workout_template_exercises
            JOIN workout_templates ON workout_templates.id = workout_template_exercises.workout_template_id
            WHERE workout_template_exercises.id = exercise_sets.workout_exercise_id
            AND workout_templates.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update sets in their workout exercises"
    ON exercise_sets FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM workout_template_exercises
            JOIN workout_templates ON workout_templates.id = workout_template_exercises.workout_template_id
            WHERE workout_template_exercises.id = exercise_sets.workout_exercise_id
            AND workout_templates.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete sets in their workout exercises"
    ON exercise_sets FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM workout_template_exercises
            JOIN workout_templates ON workout_templates.id = workout_template_exercises.workout_template_id
            WHERE workout_template_exercises.id = exercise_sets.workout_exercise_id
            AND workout_templates.user_id = auth.uid()
        )
    );

-- =====================================================
-- 10. CREATE FUNCTIONS & TRIGGERS
-- =====================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_workout_templates_updated_at
    BEFORE UPDATE ON workout_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- ✅ DONE! Schema created successfully
-- =====================================================