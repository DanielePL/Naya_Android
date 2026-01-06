-- =====================================================
-- Prometheus User Strength Profile Schema
-- The "red thread" through the app - PR-based training system
-- =====================================================

-- User Strength Profiles Table
-- Core table for storing current PRs, goal PRs, and training commitment
CREATE TABLE IF NOT EXISTS user_strength_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Identity
    gender TEXT NOT NULL CHECK (gender IN ('male', 'female')),
    bodyweight_kg REAL NOT NULL,
    experience_level TEXT NOT NULL CHECK (experience_level IN ('beginner', 'intermediate', 'experienced', 'elite')),

    -- Current PRs (IST-Zustand)
    current_squat_kg REAL NOT NULL DEFAULT 0,
    current_bench_kg REAL NOT NULL DEFAULT 0,
    current_deadlift_kg REAL NOT NULL DEFAULT 0,
    current_overhead_kg REAL, -- Optional

    -- Goal PRs (SOLL-Zustand)
    goal_squat_kg REAL NOT NULL DEFAULT 0,
    goal_bench_kg REAL NOT NULL DEFAULT 0,
    goal_deadlift_kg REAL NOT NULL DEFAULT 0,
    goal_overhead_kg REAL, -- Optional

    -- Timeline
    target_date DATE,
    estimated_weeks INTEGER,

    -- Commitment
    sessions_per_week INTEGER NOT NULL DEFAULT 4 CHECK (sessions_per_week BETWEEN 1 AND 7),
    effort_level INTEGER NOT NULL DEFAULT 7 CHECK (effort_level BETWEEN 1 AND 10),

    -- Computed columns (stored for query performance)
    current_total_kg REAL GENERATED ALWAYS AS (current_squat_kg + current_bench_kg + current_deadlift_kg) STORED,
    goal_total_kg REAL GENERATED ALWAYS AS (goal_squat_kg + goal_bench_kg + goal_deadlift_kg) STORED,

    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    -- One profile per user
    UNIQUE(user_id)
);

-- PR History Table
-- Track all PR attempts over time
CREATE TABLE IF NOT EXISTS pr_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- PR Details
    lift TEXT NOT NULL CHECK (lift IN ('squat', 'bench', 'deadlift', 'overhead')),
    weight_kg REAL NOT NULL,
    reps INTEGER NOT NULL DEFAULT 1,
    estimated_1rm REAL NOT NULL, -- Calculated e1RM if reps > 1

    -- VBT Data (optional)
    velocity_ms REAL,
    mean_velocity_ms REAL,
    peak_velocity_ms REAL,

    -- Media
    video_url TEXT,

    -- Context
    session_id UUID, -- Link to workout session
    notes TEXT,

    -- Metadata
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),

    -- Indexes for common queries
    CONSTRAINT pr_history_positive_weight CHECK (weight_kg > 0),
    CONSTRAINT pr_history_positive_reps CHECK (reps > 0)
);

-- Strength Milestones Table
-- Track progress milestones toward goals
CREATE TABLE IF NOT EXISTS strength_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Milestone Details
    week INTEGER NOT NULL,
    expected_total REAL NOT NULL,
    message TEXT NOT NULL,

    -- Status
    is_reached BOOLEAN DEFAULT FALSE,
    reached_at TIMESTAMPTZ,
    actual_total REAL, -- What total they actually had when milestone was checked

    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),

    -- Unique milestone per user per week
    UNIQUE(user_id, week)
);

-- =====================================================
-- INDEXES
-- =====================================================

-- User strength profile lookups
CREATE INDEX IF NOT EXISTS idx_user_strength_profiles_user_id
    ON user_strength_profiles(user_id);

-- PR history queries
CREATE INDEX IF NOT EXISTS idx_pr_history_user_lift
    ON pr_history(user_id, lift, date DESC);

CREATE INDEX IF NOT EXISTS idx_pr_history_user_date
    ON pr_history(user_id, date DESC);

-- Milestone queries
CREATE INDEX IF NOT EXISTS idx_strength_milestones_user
    ON strength_milestones(user_id, week);

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Function to calculate estimated 1RM (Epley formula)
CREATE OR REPLACE FUNCTION calculate_e1rm(weight REAL, reps INTEGER)
RETURNS REAL AS $$
BEGIN
    IF reps = 1 THEN
        RETURN weight;
    ELSE
        RETURN weight * (1 + reps::REAL / 30.0);
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to update strength profile totals automatically
CREATE OR REPLACE FUNCTION update_strength_profile_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for auto-updating timestamp
DROP TRIGGER IF EXISTS trigger_update_strength_profile_timestamp ON user_strength_profiles;
CREATE TRIGGER trigger_update_strength_profile_timestamp
    BEFORE UPDATE ON user_strength_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_strength_profile_timestamp();

-- Function to check and update PR after workout
CREATE OR REPLACE FUNCTION check_and_update_pr(
    p_user_id UUID,
    p_lift TEXT,
    p_weight REAL,
    p_reps INTEGER,
    p_velocity REAL DEFAULT NULL,
    p_session_id UUID DEFAULT NULL
)
RETURNS TABLE (
    is_new_pr BOOLEAN,
    old_pr REAL,
    new_pr REAL,
    improvement REAL
) AS $$
DECLARE
    v_e1rm REAL;
    v_current_pr REAL;
BEGIN
    -- Calculate e1RM
    v_e1rm := calculate_e1rm(p_weight, p_reps);

    -- Get current PR from profile
    SELECT
        CASE p_lift
            WHEN 'squat' THEN current_squat_kg
            WHEN 'bench' THEN current_bench_kg
            WHEN 'deadlift' THEN current_deadlift_kg
            WHEN 'overhead' THEN COALESCE(current_overhead_kg, 0)
        END
    INTO v_current_pr
    FROM user_strength_profiles
    WHERE user_id = p_user_id;

    -- Insert PR history entry
    INSERT INTO pr_history (user_id, lift, weight_kg, reps, estimated_1rm, velocity_ms, session_id)
    VALUES (p_user_id, p_lift, p_weight, p_reps, v_e1rm, p_velocity, p_session_id);

    -- Check if this is a new PR (more than 1kg improvement for actual 1RM, or >2% for e1RM)
    IF (p_reps = 1 AND p_weight > v_current_pr) OR
       (p_reps > 1 AND v_e1rm > v_current_pr * 1.02) THEN

        -- Update the profile with new PR
        UPDATE user_strength_profiles
        SET
            current_squat_kg = CASE WHEN p_lift = 'squat' THEN v_e1rm ELSE current_squat_kg END,
            current_bench_kg = CASE WHEN p_lift = 'bench' THEN v_e1rm ELSE current_bench_kg END,
            current_deadlift_kg = CASE WHEN p_lift = 'deadlift' THEN v_e1rm ELSE current_deadlift_kg END,
            current_overhead_kg = CASE WHEN p_lift = 'overhead' THEN v_e1rm ELSE current_overhead_kg END
        WHERE user_id = p_user_id;

        RETURN QUERY SELECT TRUE, v_current_pr, v_e1rm, v_e1rm - v_current_pr;
    ELSE
        RETURN QUERY SELECT FALSE, v_current_pr, v_e1rm, 0::REAL;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- ROW LEVEL SECURITY
-- =====================================================

-- Enable RLS
ALTER TABLE user_strength_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE pr_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE strength_milestones ENABLE ROW LEVEL SECURITY;

-- Policies for user_strength_profiles
CREATE POLICY "Users can view own strength profile"
    ON user_strength_profiles FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own strength profile"
    ON user_strength_profiles FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own strength profile"
    ON user_strength_profiles FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own strength profile"
    ON user_strength_profiles FOR DELETE
    USING (auth.uid() = user_id);

-- Policies for pr_history
CREATE POLICY "Users can view own PR history"
    ON pr_history FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own PR history"
    ON pr_history FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own PR history"
    ON pr_history FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own PR history"
    ON pr_history FOR DELETE
    USING (auth.uid() = user_id);

-- Policies for strength_milestones
CREATE POLICY "Users can view own milestones"
    ON strength_milestones FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own milestones"
    ON strength_milestones FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own milestones"
    ON strength_milestones FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own milestones"
    ON strength_milestones FOR DELETE
    USING (auth.uid() = user_id);

-- =====================================================
-- VIEWS
-- =====================================================

-- View for user's strength progress summary
CREATE OR REPLACE VIEW user_strength_summary AS
SELECT
    sp.user_id,
    sp.gender,
    sp.bodyweight_kg,
    sp.experience_level,
    sp.current_total_kg,
    sp.goal_total_kg,
    sp.goal_total_kg - sp.current_total_kg AS total_to_gain,
    CASE
        WHEN sp.goal_total_kg > sp.current_total_kg THEN
            ROUND(((sp.current_total_kg - (sp.goal_total_kg - (sp.goal_total_kg - sp.current_total_kg))) /
                   (sp.goal_total_kg - sp.current_total_kg) * 100)::numeric, 1)
        ELSE 100
    END AS progress_percentage,
    sp.target_date,
    sp.sessions_per_week,
    sp.effort_level,
    (SELECT COUNT(*) FROM pr_history WHERE user_id = sp.user_id AND date >= CURRENT_DATE - INTERVAL '30 days') AS prs_last_30_days,
    (SELECT MAX(date) FROM pr_history WHERE user_id = sp.user_id) AS last_pr_date,
    sp.updated_at
FROM user_strength_profiles sp;

-- =====================================================
-- SAMPLE DATA (for testing - remove in production)
-- =====================================================

-- Uncomment to insert test data:
/*
INSERT INTO user_strength_profiles (
    user_id, gender, bodyweight_kg, experience_level,
    current_squat_kg, current_bench_kg, current_deadlift_kg,
    goal_squat_kg, goal_bench_kg, goal_deadlift_kg,
    sessions_per_week, effort_level, estimated_weeks
) VALUES (
    'YOUR_USER_ID_HERE', 'male', 83,  'experienced',
    140, 100, 180,
    160, 115, 210,
    4, 7, 20
);
*/