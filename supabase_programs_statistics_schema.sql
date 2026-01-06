-- ============================================================
-- PROMETHEUS FITNESS APP - PROGRAMS & STATISTICS SCHEMA
-- ============================================================
-- This schema adds:
-- 1. Training Programs (multi-week training cycles)
-- 2. Program-Workout relationships
-- 3. Exercise Statistics (PRs, aggregates, trends)
-- 4. Workout History (quick access to completed sessions)
-- 5. PR History (track all personal records over time)
-- ============================================================

-- ============================================================
-- 1. TRAINING PROGRAMS
-- Multi-week training cycles that contain multiple workouts
-- ============================================================
CREATE TABLE IF NOT EXISTS training_programs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,

    -- Program Structure
    duration_weeks INT NOT NULL DEFAULT 4,
    workouts_per_week INT NOT NULL DEFAULT 3,

    -- Program Type & Goal
    goal TEXT CHECK (goal IN ('strength', 'hypertrophy', 'endurance', 'power', 'general', 'sport_specific')),
    program_type TEXT CHECK (program_type IN ('linear', 'undulating', 'block', 'custom')),
    difficulty_level TEXT CHECK (difficulty_level IN ('beginner', 'intermediate', 'advanced', 'elite')),

    -- Sports Association (for sport-specific programs)
    sports TEXT[] DEFAULT '{}',

    -- Status & Progress
    is_active BOOLEAN DEFAULT false,
    status TEXT DEFAULT 'not_started' CHECK (status IN ('not_started', 'in_progress', 'completed', 'paused', 'abandoned')),
    current_week INT DEFAULT 1,
    current_day INT DEFAULT 1,

    -- Dates
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    paused_at TIMESTAMPTZ,

    -- Stats (denormalized for quick access)
    total_sessions_planned INT DEFAULT 0,
    total_sessions_completed INT DEFAULT 0,
    total_volume_kg NUMERIC DEFAULT 0,

    -- Metadata
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes for training_programs
CREATE INDEX IF NOT EXISTS idx_programs_user_id ON training_programs(user_id);
CREATE INDEX IF NOT EXISTS idx_programs_user_active ON training_programs(user_id, is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_programs_status ON training_programs(user_id, status);

-- RLS Policies for training_programs
ALTER TABLE training_programs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own programs" ON training_programs
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own programs" ON training_programs
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own programs" ON training_programs
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own programs" ON training_programs
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 2. PROGRAM WORKOUTS
-- Links workout templates to programs with scheduling info
-- ============================================================
CREATE TABLE IF NOT EXISTS program_workouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id UUID NOT NULL REFERENCES training_programs(id) ON DELETE CASCADE,
    workout_template_id UUID NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,

    -- Scheduling
    week_number INT NOT NULL,                    -- Which week (1, 2, 3...)
    day_number INT NOT NULL,                     -- Day within week (1-7, or just order)
    day_of_week INT,                             -- Optional: specific weekday (1=Mon...7=Sun)

    -- Ordering
    order_in_program INT NOT NULL,               -- Global order (1, 2, 3... across all weeks)

    -- Progression Notes
    progression_notes TEXT,                      -- e.g., "Add 2.5kg to compound lifts"
    intensity_modifier NUMERIC DEFAULT 1.0,      -- Multiplier for weights (e.g., 0.9 for deload)

    -- Completion Status
    is_completed BOOLEAN DEFAULT false,
    completed_at TIMESTAMPTZ,
    session_id UUID REFERENCES workout_sessions(id), -- Link to actual completed session

    -- Metadata
    created_at TIMESTAMPTZ DEFAULT now(),

    UNIQUE(program_id, week_number, day_number)
);

-- Indexes for program_workouts
CREATE INDEX IF NOT EXISTS idx_program_workouts_program ON program_workouts(program_id);
CREATE INDEX IF NOT EXISTS idx_program_workouts_template ON program_workouts(workout_template_id);
CREATE INDEX IF NOT EXISTS idx_program_workouts_order ON program_workouts(program_id, order_in_program);
CREATE INDEX IF NOT EXISTS idx_program_workouts_week ON program_workouts(program_id, week_number);

-- RLS Policies for program_workouts
ALTER TABLE program_workouts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view program workouts" ON program_workouts
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM training_programs WHERE id = program_id AND user_id = auth.uid())
    );

CREATE POLICY "Users can insert program workouts" ON program_workouts
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM training_programs WHERE id = program_id AND user_id = auth.uid())
    );

CREATE POLICY "Users can update program workouts" ON program_workouts
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM training_programs WHERE id = program_id AND user_id = auth.uid())
    );

CREATE POLICY "Users can delete program workouts" ON program_workouts
    FOR DELETE USING (
        EXISTS (SELECT 1 FROM training_programs WHERE id = program_id AND user_id = auth.uid())
    );

-- ============================================================
-- 3. EXERCISE STATISTICS
-- Aggregated statistics per exercise per user
-- ============================================================
CREATE TABLE IF NOT EXISTS exercise_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    exercise_id TEXT NOT NULL,  -- References exercises_new but no FK for flexibility

    -- Personal Records (Weight)
    pr_weight_kg NUMERIC,
    pr_weight_reps INT,                          -- Reps at PR weight
    pr_weight_date TIMESTAMPTZ,
    pr_weight_session_id UUID REFERENCES workout_sessions(id),

    -- Personal Records (Reps - at any weight)
    pr_reps INT,
    pr_reps_weight_kg NUMERIC,                   -- Weight at PR reps
    pr_reps_date TIMESTAMPTZ,
    pr_reps_session_id UUID REFERENCES workout_sessions(id),

    -- Personal Records (Volume - single set)
    pr_volume_kg NUMERIC,                        -- Best weight x reps
    pr_volume_date TIMESTAMPTZ,
    pr_volume_session_id UUID REFERENCES workout_sessions(id),

    -- Personal Records (VBT)
    pr_velocity NUMERIC,                         -- Best peak velocity m/s
    pr_velocity_date TIMESTAMPTZ,
    pr_velocity_session_id UUID REFERENCES workout_sessions(id),

    -- Estimated 1RM (calculated)
    estimated_1rm_kg NUMERIC,
    estimated_1rm_formula TEXT DEFAULT 'epley',  -- 'epley', 'brzycki', 'lombardi'
    estimated_1rm_date TIMESTAMPTZ,

    -- Lifetime Aggregates
    total_volume_kg NUMERIC DEFAULT 0,           -- Sum of all (weight x reps)
    total_sets INT DEFAULT 0,
    total_reps INT DEFAULT 0,
    total_sessions INT DEFAULT 0,                -- How many sessions included this exercise

    -- Averages
    avg_weight_kg NUMERIC,
    avg_reps_per_set NUMERIC,
    avg_sets_per_session NUMERIC,
    avg_volume_per_session NUMERIC,

    -- Recent Performance (last 4 weeks)
    recent_avg_weight_kg NUMERIC,
    recent_avg_reps NUMERIC,
    recent_total_volume_kg NUMERIC,
    recent_sessions INT DEFAULT 0,

    -- Trend Analysis
    trend_direction TEXT CHECK (trend_direction IN ('improving', 'stable', 'declining')),
    trend_percentage NUMERIC,                    -- % change recent vs previous period

    -- Timestamps
    first_performed_at TIMESTAMPTZ,
    last_performed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT now(),

    UNIQUE(user_id, exercise_id)
);

-- Indexes for exercise_statistics
CREATE INDEX IF NOT EXISTS idx_exercise_stats_user ON exercise_statistics(user_id);
CREATE INDEX IF NOT EXISTS idx_exercise_stats_exercise ON exercise_statistics(exercise_id);
CREATE INDEX IF NOT EXISTS idx_exercise_stats_user_exercise ON exercise_statistics(user_id, exercise_id);
CREATE INDEX IF NOT EXISTS idx_exercise_stats_last_performed ON exercise_statistics(user_id, last_performed_at DESC);
CREATE INDEX IF NOT EXISTS idx_exercise_stats_pr_weight ON exercise_statistics(user_id, pr_weight_kg DESC NULLS LAST);

-- RLS Policies for exercise_statistics
ALTER TABLE exercise_statistics ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own exercise stats" ON exercise_statistics
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own exercise stats" ON exercise_statistics
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own exercise stats" ON exercise_statistics
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own exercise stats" ON exercise_statistics
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 4. WORKOUT HISTORY
-- Denormalized summary of completed workouts for quick access
-- ============================================================
CREATE TABLE IF NOT EXISTS workout_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Program Context (optional)
    program_id UUID REFERENCES training_programs(id) ON DELETE SET NULL,
    program_week INT,
    program_day INT,

    -- Workout Reference
    workout_template_id UUID REFERENCES workout_templates(id) ON DELETE SET NULL,
    workout_name TEXT NOT NULL,

    -- Summary Stats (denormalized for fast queries)
    total_volume_kg NUMERIC DEFAULT 0,
    total_sets INT DEFAULT 0,
    total_reps INT DEFAULT 0,
    total_exercises INT DEFAULT 0,
    duration_minutes INT,

    -- Performance Metrics
    avg_rpe NUMERIC,                             -- Average Rate of Perceived Exertion
    fatigue_index NUMERIC,                       -- Velocity drop % (VBT)
    performance_score NUMERIC,                   -- Calculated overall score

    -- Records
    prs_achieved INT DEFAULT 0,                  -- Count of PRs in this session
    pr_exercises TEXT[],                         -- List of exercise IDs with PRs

    -- VBT Summary (if applicable)
    avg_velocity NUMERIC,
    total_reps_tracked INT DEFAULT 0,            -- Reps with VBT data

    -- User Input
    notes TEXT,
    mood_rating INT CHECK (mood_rating BETWEEN 1 AND 5),
    energy_rating INT CHECK (energy_rating BETWEEN 1 AND 5),

    -- Timestamps
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ NOT NULL,

    -- Calendar helpers (computed on insert via trigger or application)
    completed_date DATE,
    completed_week INT,
    completed_month INT,
    completed_year INT,
    day_of_week INT,

    -- Metadata
    created_at TIMESTAMPTZ DEFAULT now(),

    UNIQUE(session_id)
);

-- Indexes for workout_history
CREATE INDEX IF NOT EXISTS idx_history_user ON workout_history(user_id);
CREATE INDEX IF NOT EXISTS idx_history_user_date ON workout_history(user_id, completed_at DESC);
CREATE INDEX IF NOT EXISTS idx_history_user_completed_date ON workout_history(user_id, completed_date DESC);
CREATE INDEX IF NOT EXISTS idx_history_program ON workout_history(program_id) WHERE program_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_history_template ON workout_history(workout_template_id);
CREATE INDEX IF NOT EXISTS idx_history_calendar ON workout_history(user_id, completed_year, completed_month);
CREATE INDEX IF NOT EXISTS idx_history_week ON workout_history(user_id, completed_year, completed_week);

-- RLS Policies for workout_history
ALTER TABLE workout_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own workout history" ON workout_history
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own workout history" ON workout_history
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own workout history" ON workout_history
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own workout history" ON workout_history
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 5. PR HISTORY
-- Track all personal records over time (not just current)
-- ============================================================
CREATE TABLE IF NOT EXISTS pr_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    exercise_id TEXT NOT NULL,
    session_id UUID REFERENCES workout_sessions(id) ON DELETE SET NULL,

    -- PR Type
    pr_type TEXT NOT NULL CHECK (pr_type IN ('weight', 'reps', 'volume', 'velocity', '1rm_estimated')),

    -- PR Values
    weight_kg NUMERIC,
    reps INT,
    volume_kg NUMERIC,                           -- weight x reps
    velocity NUMERIC,                            -- m/s for VBT

    -- Context
    previous_pr_value NUMERIC,                   -- What was the old PR
    improvement_percentage NUMERIC,              -- How much better

    -- Metadata
    achieved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    notes TEXT,

    created_at TIMESTAMPTZ DEFAULT now()
);

-- Indexes for pr_history
CREATE INDEX IF NOT EXISTS idx_pr_history_user ON pr_history(user_id);
CREATE INDEX IF NOT EXISTS idx_pr_history_exercise ON pr_history(user_id, exercise_id);
CREATE INDEX IF NOT EXISTS idx_pr_history_date ON pr_history(user_id, achieved_at DESC);
CREATE INDEX IF NOT EXISTS idx_pr_history_type ON pr_history(user_id, pr_type);
-- Note: Partial index with now() not possible - use application-side filtering for recent PRs
CREATE INDEX IF NOT EXISTS idx_pr_history_recent ON pr_history(user_id, achieved_at DESC);

-- RLS Policies for pr_history
ALTER TABLE pr_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own PR history" ON pr_history
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own PR history" ON pr_history
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own PR history" ON pr_history
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 6. USER TRAINING SUMMARY
-- Quick access to user's overall training statistics
-- ============================================================
CREATE TABLE IF NOT EXISTS user_training_summary (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Lifetime Stats
    total_workouts INT DEFAULT 0,
    total_volume_kg NUMERIC DEFAULT 0,
    total_sets INT DEFAULT 0,
    total_reps INT DEFAULT 0,
    total_training_minutes INT DEFAULT 0,
    total_prs INT DEFAULT 0,

    -- Current Streak
    current_streak_days INT DEFAULT 0,
    longest_streak_days INT DEFAULT 0,
    last_workout_date DATE,

    -- This Week
    week_workouts INT DEFAULT 0,
    week_volume_kg NUMERIC DEFAULT 0,
    week_started_at DATE,

    -- This Month
    month_workouts INT DEFAULT 0,
    month_volume_kg NUMERIC DEFAULT 0,
    month_started_at DATE,

    -- This Year
    year_workouts INT DEFAULT 0,
    year_volume_kg NUMERIC DEFAULT 0,
    year_prs INT DEFAULT 0,

    -- Averages
    avg_workout_duration_minutes NUMERIC,
    avg_workouts_per_week NUMERIC,
    avg_volume_per_workout NUMERIC,

    -- Favorites (most performed)
    favorite_exercises TEXT[],                   -- Top 5 exercise IDs
    favorite_workout_template_id UUID REFERENCES workout_templates(id),

    -- Active Program
    active_program_id UUID REFERENCES training_programs(id),

    -- Timestamps
    first_workout_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- RLS Policies for user_training_summary
ALTER TABLE user_training_summary ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own summary" ON user_training_summary
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own summary" ON user_training_summary
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own summary" ON user_training_summary
    FOR UPDATE USING (auth.uid() = user_id);

-- ============================================================
-- 7. HELPER FUNCTIONS
-- ============================================================

-- Trigger function to auto-populate calendar helper columns in workout_history
CREATE OR REPLACE FUNCTION populate_workout_history_calendar_fields()
RETURNS TRIGGER AS $$
BEGIN
    NEW.completed_date := NEW.completed_at::date;
    NEW.completed_week := EXTRACT(WEEK FROM NEW.completed_at)::int;
    NEW.completed_month := EXTRACT(MONTH FROM NEW.completed_at)::int;
    NEW.completed_year := EXTRACT(YEAR FROM NEW.completed_at)::int;
    NEW.day_of_week := EXTRACT(ISODOW FROM NEW.completed_at)::int;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for workout_history
DROP TRIGGER IF EXISTS trigger_workout_history_calendar ON workout_history;
CREATE TRIGGER trigger_workout_history_calendar
    BEFORE INSERT OR UPDATE ON workout_history
    FOR EACH ROW
    EXECUTE FUNCTION populate_workout_history_calendar_fields();

-- Function to calculate estimated 1RM
CREATE OR REPLACE FUNCTION calculate_1rm(weight NUMERIC, reps INT, formula TEXT DEFAULT 'epley')
RETURNS NUMERIC AS $$
BEGIN
    IF reps <= 0 OR weight <= 0 THEN
        RETURN NULL;
    END IF;

    IF reps = 1 THEN
        RETURN weight;
    END IF;

    CASE formula
        WHEN 'epley' THEN
            RETURN ROUND(weight * (1 + reps::numeric / 30), 1);
        WHEN 'brzycki' THEN
            RETURN ROUND(weight * (36 / (37 - reps::numeric)), 1);
        WHEN 'lombardi' THEN
            RETURN ROUND(weight * power(reps::numeric, 0.1), 1);
        ELSE
            RETURN ROUND(weight * (1 + reps::numeric / 30), 1); -- Default Epley
    END CASE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to update exercise statistics after a workout
CREATE OR REPLACE FUNCTION update_exercise_stats_after_workout(
    p_user_id UUID,
    p_exercise_id TEXT,
    p_session_id UUID,
    p_weight_kg NUMERIC,
    p_reps INT,
    p_velocity NUMERIC DEFAULT NULL
)
RETURNS void AS $$
DECLARE
    v_volume NUMERIC;
    v_est_1rm NUMERIC;
    v_existing RECORD;
    v_is_pr_weight BOOLEAN := false;
    v_is_pr_reps BOOLEAN := false;
    v_is_pr_volume BOOLEAN := false;
    v_is_pr_velocity BOOLEAN := false;
BEGIN
    v_volume := p_weight_kg * p_reps;
    v_est_1rm := calculate_1rm(p_weight_kg, p_reps);

    -- Get existing stats
    SELECT * INTO v_existing FROM exercise_statistics
    WHERE user_id = p_user_id AND exercise_id = p_exercise_id;

    -- Check for PRs
    IF v_existing IS NULL OR p_weight_kg > COALESCE(v_existing.pr_weight_kg, 0) THEN
        v_is_pr_weight := true;
    END IF;

    IF v_existing IS NULL OR p_reps > COALESCE(v_existing.pr_reps, 0) THEN
        v_is_pr_reps := true;
    END IF;

    IF v_existing IS NULL OR v_volume > COALESCE(v_existing.pr_volume_kg, 0) THEN
        v_is_pr_volume := true;
    END IF;

    IF p_velocity IS NOT NULL AND (v_existing IS NULL OR p_velocity > COALESCE(v_existing.pr_velocity, 0)) THEN
        v_is_pr_velocity := true;
    END IF;

    -- Upsert exercise_statistics
    INSERT INTO exercise_statistics (
        user_id, exercise_id,
        pr_weight_kg, pr_weight_reps, pr_weight_date, pr_weight_session_id,
        pr_reps, pr_reps_weight_kg, pr_reps_date, pr_reps_session_id,
        pr_volume_kg, pr_volume_date, pr_volume_session_id,
        pr_velocity, pr_velocity_date, pr_velocity_session_id,
        estimated_1rm_kg, estimated_1rm_date,
        total_volume_kg, total_sets, total_reps, total_sessions,
        first_performed_at, last_performed_at, updated_at
    )
    VALUES (
        p_user_id, p_exercise_id,
        CASE WHEN v_is_pr_weight THEN p_weight_kg ELSE NULL END,
        CASE WHEN v_is_pr_weight THEN p_reps ELSE NULL END,
        CASE WHEN v_is_pr_weight THEN now() ELSE NULL END,
        CASE WHEN v_is_pr_weight THEN p_session_id ELSE NULL END,
        CASE WHEN v_is_pr_reps THEN p_reps ELSE NULL END,
        CASE WHEN v_is_pr_reps THEN p_weight_kg ELSE NULL END,
        CASE WHEN v_is_pr_reps THEN now() ELSE NULL END,
        CASE WHEN v_is_pr_reps THEN p_session_id ELSE NULL END,
        CASE WHEN v_is_pr_volume THEN v_volume ELSE NULL END,
        CASE WHEN v_is_pr_volume THEN now() ELSE NULL END,
        CASE WHEN v_is_pr_volume THEN p_session_id ELSE NULL END,
        CASE WHEN v_is_pr_velocity THEN p_velocity ELSE NULL END,
        CASE WHEN v_is_pr_velocity THEN now() ELSE NULL END,
        CASE WHEN v_is_pr_velocity THEN p_session_id ELSE NULL END,
        v_est_1rm, now(),
        v_volume, 1, p_reps, 1,
        now(), now(), now()
    )
    ON CONFLICT (user_id, exercise_id) DO UPDATE SET
        pr_weight_kg = CASE WHEN v_is_pr_weight THEN p_weight_kg ELSE exercise_statistics.pr_weight_kg END,
        pr_weight_reps = CASE WHEN v_is_pr_weight THEN p_reps ELSE exercise_statistics.pr_weight_reps END,
        pr_weight_date = CASE WHEN v_is_pr_weight THEN now() ELSE exercise_statistics.pr_weight_date END,
        pr_weight_session_id = CASE WHEN v_is_pr_weight THEN p_session_id ELSE exercise_statistics.pr_weight_session_id END,
        pr_reps = CASE WHEN v_is_pr_reps THEN p_reps ELSE exercise_statistics.pr_reps END,
        pr_reps_weight_kg = CASE WHEN v_is_pr_reps THEN p_weight_kg ELSE exercise_statistics.pr_reps_weight_kg END,
        pr_reps_date = CASE WHEN v_is_pr_reps THEN now() ELSE exercise_statistics.pr_reps_date END,
        pr_reps_session_id = CASE WHEN v_is_pr_reps THEN p_session_id ELSE exercise_statistics.pr_reps_session_id END,
        pr_volume_kg = CASE WHEN v_is_pr_volume THEN v_volume ELSE exercise_statistics.pr_volume_kg END,
        pr_volume_date = CASE WHEN v_is_pr_volume THEN now() ELSE exercise_statistics.pr_volume_date END,
        pr_volume_session_id = CASE WHEN v_is_pr_volume THEN p_session_id ELSE exercise_statistics.pr_volume_session_id END,
        pr_velocity = CASE WHEN v_is_pr_velocity THEN p_velocity ELSE exercise_statistics.pr_velocity END,
        pr_velocity_date = CASE WHEN v_is_pr_velocity THEN now() ELSE exercise_statistics.pr_velocity_date END,
        pr_velocity_session_id = CASE WHEN v_is_pr_velocity THEN p_session_id ELSE exercise_statistics.pr_velocity_session_id END,
        estimated_1rm_kg = GREATEST(exercise_statistics.estimated_1rm_kg, v_est_1rm),
        estimated_1rm_date = CASE WHEN v_est_1rm > COALESCE(exercise_statistics.estimated_1rm_kg, 0) THEN now() ELSE exercise_statistics.estimated_1rm_date END,
        total_volume_kg = exercise_statistics.total_volume_kg + v_volume,
        total_sets = exercise_statistics.total_sets + 1,
        total_reps = exercise_statistics.total_reps + p_reps,
        last_performed_at = now(),
        updated_at = now();

    -- Record PRs in pr_history
    IF v_is_pr_weight THEN
        INSERT INTO pr_history (user_id, exercise_id, session_id, pr_type, weight_kg, reps, previous_pr_value, improvement_percentage, achieved_at)
        VALUES (p_user_id, p_exercise_id, p_session_id, 'weight', p_weight_kg, p_reps,
                v_existing.pr_weight_kg,
                CASE WHEN v_existing.pr_weight_kg > 0 THEN ((p_weight_kg - v_existing.pr_weight_kg) / v_existing.pr_weight_kg * 100) ELSE NULL END,
                now());
    END IF;

    IF v_is_pr_volume THEN
        INSERT INTO pr_history (user_id, exercise_id, session_id, pr_type, weight_kg, reps, volume_kg, previous_pr_value, improvement_percentage, achieved_at)
        VALUES (p_user_id, p_exercise_id, p_session_id, 'volume', p_weight_kg, p_reps, v_volume,
                v_existing.pr_volume_kg,
                CASE WHEN v_existing.pr_volume_kg > 0 THEN ((v_volume - v_existing.pr_volume_kg) / v_existing.pr_volume_kg * 100) ELSE NULL END,
                now());
    END IF;

    IF v_is_pr_velocity THEN
        INSERT INTO pr_history (user_id, exercise_id, session_id, pr_type, velocity, previous_pr_value, improvement_percentage, achieved_at)
        VALUES (p_user_id, p_exercise_id, p_session_id, 'velocity', p_velocity,
                v_existing.pr_velocity,
                CASE WHEN v_existing.pr_velocity > 0 THEN ((p_velocity - v_existing.pr_velocity) / v_existing.pr_velocity * 100) ELSE NULL END,
                now());
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function to update user training summary
CREATE OR REPLACE FUNCTION update_user_training_summary(p_user_id UUID)
RETURNS void AS $$
BEGIN
    INSERT INTO user_training_summary (user_id)
    VALUES (p_user_id)
    ON CONFLICT (user_id) DO UPDATE SET
        total_workouts = (SELECT COUNT(*) FROM workout_history WHERE user_id = p_user_id),
        total_volume_kg = (SELECT COALESCE(SUM(total_volume_kg), 0) FROM workout_history WHERE user_id = p_user_id),
        total_sets = (SELECT COALESCE(SUM(total_sets), 0) FROM workout_history WHERE user_id = p_user_id),
        total_reps = (SELECT COALESCE(SUM(total_reps), 0) FROM workout_history WHERE user_id = p_user_id),
        total_training_minutes = (SELECT COALESCE(SUM(duration_minutes), 0) FROM workout_history WHERE user_id = p_user_id),
        total_prs = (SELECT COUNT(*) FROM pr_history WHERE user_id = p_user_id),
        last_workout_date = (SELECT MAX(completed_date) FROM workout_history WHERE user_id = p_user_id),
        week_workouts = (SELECT COUNT(*) FROM workout_history WHERE user_id = p_user_id AND completed_at >= date_trunc('week', now())),
        week_volume_kg = (SELECT COALESCE(SUM(total_volume_kg), 0) FROM workout_history WHERE user_id = p_user_id AND completed_at >= date_trunc('week', now())),
        month_workouts = (SELECT COUNT(*) FROM workout_history WHERE user_id = p_user_id AND completed_at >= date_trunc('month', now())),
        month_volume_kg = (SELECT COALESCE(SUM(total_volume_kg), 0) FROM workout_history WHERE user_id = p_user_id AND completed_at >= date_trunc('month', now())),
        year_workouts = (SELECT COUNT(*) FROM workout_history WHERE user_id = p_user_id AND completed_at >= date_trunc('year', now())),
        year_volume_kg = (SELECT COALESCE(SUM(total_volume_kg), 0) FROM workout_history WHERE user_id = p_user_id AND completed_at >= date_trunc('year', now())),
        year_prs = (SELECT COUNT(*) FROM pr_history WHERE user_id = p_user_id AND achieved_at >= date_trunc('year', now())),
        updated_at = now();
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 8. VIEWS FOR QUICK ACCESS
-- ============================================================

-- View: Recent PRs (last 30 days)
CREATE OR REPLACE VIEW recent_prs AS
SELECT
    ph.user_id,
    ph.exercise_id,
    e.name as exercise_name,
    ph.pr_type,
    ph.weight_kg,
    ph.reps,
    ph.volume_kg,
    ph.velocity,
    ph.improvement_percentage,
    ph.achieved_at
FROM pr_history ph
LEFT JOIN exercises_new e ON e.id = ph.exercise_id
WHERE ph.achieved_at > (now() - interval '30 days')
ORDER BY ph.achieved_at DESC;

-- View: Weekly training volume
CREATE OR REPLACE VIEW weekly_volume AS
SELECT
    user_id,
    completed_year,
    completed_week,
    COUNT(*) as workouts,
    SUM(total_volume_kg) as volume_kg,
    SUM(total_sets) as sets,
    SUM(total_reps) as reps,
    AVG(duration_minutes) as avg_duration,
    SUM(prs_achieved) as prs
FROM workout_history
GROUP BY user_id, completed_year, completed_week
ORDER BY completed_year DESC, completed_week DESC;

-- View: Exercise leaderboard (top exercises by volume)
CREATE OR REPLACE VIEW exercise_volume_leaderboard AS
SELECT
    es.user_id,
    es.exercise_id,
    e.name as exercise_name,
    e.main_muscle,
    es.total_volume_kg,
    es.total_sessions,
    es.pr_weight_kg,
    es.last_performed_at
FROM exercise_statistics es
LEFT JOIN exercises_new e ON e.id = es.exercise_id
ORDER BY es.total_volume_kg DESC;

-- ============================================================
-- 9. ADD MISSING COLUMNS TO EXISTING TABLES
-- ============================================================

-- Add program reference to workout_sessions if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'workout_sessions' AND column_name = 'program_id') THEN
        ALTER TABLE workout_sessions ADD COLUMN program_id UUID REFERENCES training_programs(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'workout_sessions' AND column_name = 'workout_template_id') THEN
        ALTER TABLE workout_sessions ADD COLUMN workout_template_id UUID REFERENCES workout_templates(id) ON DELETE SET NULL;
    END IF;
END $$;

-- ============================================================
-- DONE!
-- ============================================================
-- Run this SQL in your Supabase SQL editor to create all tables
-- Then proceed to implement the Kotlin repositories and UI
-- ============================================================
