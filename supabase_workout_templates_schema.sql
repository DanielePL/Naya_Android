-- =====================================================
-- PROMETHEUS WORKOUT TEMPLATES SCHEMA
-- =====================================================
-- This schema stores workout templates created in the WorkoutBuilder
-- with detailed set-by-set configuration for each exercise

-- 1. WORKOUT TEMPLATES TABLE
-- Stores the workout metadata (name, creation time, etc.)
CREATE TABLE IF NOT EXISTS workout_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE
);

-- 2. WORKOUT TEMPLATE EXERCISES TABLE
-- Stores exercises within a workout with their order
-- ⚠️ MIGRATED TO LIVE REFERENCE: exercise_id now references exercises_new table
-- Exercise details (name, muscle_group, equipment) are loaded via JOIN
CREATE TABLE IF NOT EXISTS workout_template_exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_template_id UUID NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    exercise_id UUID NOT NULL REFERENCES exercises_new(id) ON DELETE CASCADE,  -- Live reference to exercises_new
    order_index INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. EXERCISE SETS TABLE
-- Stores individual sets for each exercise (e.g., Set 1: 10 reps @ 100kg)
CREATE TABLE IF NOT EXISTS exercise_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_exercise_id UUID NOT NULL REFERENCES workout_template_exercises(id) ON DELETE CASCADE,
    set_number INTEGER NOT NULL,
    target_reps INTEGER NOT NULL,
    target_weight DECIMAL(10, 2) NOT NULL,
    rest_seconds INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_workout_templates_user_id
    ON workout_templates(user_id);

CREATE INDEX IF NOT EXISTS idx_workout_templates_created_at
    ON workout_templates(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_workout_template_exercises_workout_id
    ON workout_template_exercises(workout_template_id);

CREATE INDEX IF NOT EXISTS idx_workout_template_exercises_order
    ON workout_template_exercises(workout_template_id, order_index);

CREATE INDEX IF NOT EXISTS idx_workout_template_exercises_exercise_id
    ON workout_template_exercises(exercise_id);

CREATE INDEX IF NOT EXISTS idx_exercise_sets_workout_exercise_id
    ON exercise_sets(workout_exercise_id);

CREATE INDEX IF NOT EXISTS idx_exercise_sets_set_number
    ON exercise_sets(workout_exercise_id, set_number);

-- =====================================================
-- ROW LEVEL SECURITY (RLS)
-- =====================================================

-- Enable RLS on all tables
ALTER TABLE workout_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE workout_template_exercises ENABLE ROW LEVEL SECURITY;
ALTER TABLE exercise_sets ENABLE ROW LEVEL SECURITY;

-- Policies for workout_templates
-- Allow viewing own templates AND public templates (user_id IS NULL)
CREATE POLICY "Users can view their own and public workout templates"
    ON workout_templates FOR SELECT
    USING (auth.uid() = user_id OR user_id IS NULL);

-- Allow anonymous users to view public templates
CREATE POLICY "Anonymous users can view public workout templates"
    ON workout_templates FOR SELECT TO anon
    USING (user_id IS NULL);

CREATE POLICY "Users can insert their own workout templates"
    ON workout_templates FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own workout templates"
    ON workout_templates FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own workout templates"
    ON workout_templates FOR DELETE
    USING (auth.uid() = user_id);

-- Policies for workout_template_exercises (access through workout_templates)
-- Allow viewing exercises in own templates AND public templates
CREATE POLICY "Users can view exercises in their own and public templates"
    ON workout_template_exercises FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM workout_templates
            WHERE workout_templates.id = workout_template_exercises.workout_template_id
            AND (workout_templates.user_id = auth.uid() OR workout_templates.user_id IS NULL)
        )
    );

-- Allow anonymous users to view exercises in public templates
CREATE POLICY "Anonymous users can view exercises in public templates"
    ON workout_template_exercises FOR SELECT TO anon
    USING (
        EXISTS (
            SELECT 1 FROM workout_templates
            WHERE workout_templates.id = workout_template_exercises.workout_template_id
            AND workout_templates.user_id IS NULL
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

-- Policies for exercise_sets (access through workout_template_exercises)
-- Allow viewing sets in own templates AND public templates
CREATE POLICY "Users can view sets in their own and public templates"
    ON exercise_sets FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM workout_template_exercises
            JOIN workout_templates ON workout_templates.id = workout_template_exercises.workout_template_id
            WHERE workout_template_exercises.id = exercise_sets.workout_exercise_id
            AND (workout_templates.user_id = auth.uid() OR workout_templates.user_id IS NULL)
        )
    );

-- Allow anonymous users to view sets in public templates
CREATE POLICY "Anonymous users can view sets in public templates"
    ON exercise_sets FOR SELECT TO anon
    USING (
        EXISTS (
            SELECT 1 FROM workout_template_exercises
            JOIN workout_templates ON workout_templates.id = workout_template_exercises.workout_template_id
            WHERE workout_template_exercises.id = exercise_sets.workout_exercise_id
            AND workout_templates.user_id IS NULL
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
-- FUNCTIONS & TRIGGERS
-- =====================================================

-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update updated_at on workout_templates
CREATE TRIGGER update_workout_templates_updated_at
    BEFORE UPDATE ON workout_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- SAMPLE DATA (Optional - for testing)
-- =====================================================

-- Uncomment to insert sample data:
-- INSERT INTO workout_templates (name, user_id)
-- VALUES ('Sample Leg Day', auth.uid());