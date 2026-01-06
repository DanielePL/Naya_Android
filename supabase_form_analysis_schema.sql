-- =====================================================
-- PROMETHEUS FORM ANALYSIS & VELOCITY METRICS SCHEMA
-- =====================================================
-- This schema stores workout sessions, sets, and form analysis
-- with the new honest 3-tier velocity measurement system

-- =====================================================
-- 1. WORKOUT SESSIONS TABLE
-- =====================================================
-- Stores completed workout sessions
CREATE TABLE IF NOT EXISTS workout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    -- Workout info
    workout_template_id UUID REFERENCES workout_templates(id),
    workout_name TEXT NOT NULL,
    notes TEXT,

    -- Session stats
    total_sets INTEGER DEFAULT 0,
    total_reps INTEGER DEFAULT 0,
    total_volume_kg NUMERIC(10,2) DEFAULT 0,
    duration_seconds INTEGER,

    -- Session quality
    avg_rpe NUMERIC(3,1),
    perceived_difficulty TEXT, -- "easy", "moderate", "hard", "maximal"

    -- Metadata
    location TEXT,
    tags TEXT[]
);

-- =====================================================
-- 2. WORKOUT SETS TABLE
-- =====================================================
-- Stores individual sets within a workout session
CREATE TABLE IF NOT EXISTS workout_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Exercise info
    exercise_id TEXT NOT NULL REFERENCES exercises(id),
    set_number INTEGER NOT NULL,

    -- Set data
    reps INTEGER,
    weight_kg NUMERIC(10,2),
    rpe NUMERIC(3,1), -- Rate of Perceived Exertion (1-10)
    duration_seconds INTEGER,
    distance_meters NUMERIC(10,2),

    -- Rest time
    rest_time_seconds INTEGER,

    -- Set quality
    completed BOOLEAN DEFAULT true,
    technique_rating INTEGER CHECK (technique_rating >= 1 AND technique_rating <= 10),
    notes TEXT,

    CONSTRAINT unique_session_exercise_set UNIQUE(session_id, exercise_id, set_number)
);

-- =====================================================
-- 3. FORM ANALYSIS TABLE
-- =====================================================
-- Stores video-based form analysis results
CREATE TABLE IF NOT EXISTS form_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    set_id UUID NOT NULL REFERENCES workout_sets(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Video info
    video_url TEXT,
    video_duration_seconds NUMERIC(10,2),
    analysis_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Exercise context
    exercise_id TEXT NOT NULL REFERENCES exercises(id),
    exercise_name TEXT NOT NULL,

    -- Calibration info (from CalibrationManager)
    calibration_tier TEXT NOT NULL CHECK (calibration_tier IN ('pro', 'calibrated', 'relative')),
    calibration_method TEXT NOT NULL, -- "LiDAR Depth Tracking", "Reference Object (weight_plate_45)", "Relative Speed Index"
    calibration_confidence NUMERIC(3,2) NOT NULL CHECK (calibration_confidence >= 0 AND calibration_confidence <= 1),
    calibration_unit TEXT NOT NULL CHECK (calibration_unit IN ('m/s', 'speed_index')),

    -- Rep detection
    reps_detected INTEGER NOT NULL DEFAULT 0,
    fps NUMERIC(10,2),
    frames_processed INTEGER,
    tracked_landmark TEXT, -- "hip", "wrist", "shoulder", etc.

    -- Overall quality scores


    form_score NUMERIC(4,1), -- Overall technique score (0-10)
    power_score NUMERIC(4,1), -- Power/explosiveness score (0-10)
    consistency_score NUMERIC(4,1), -- Movement consistency (0-10)

    -- Notes and feedback
    form_feedback JSONB, -- Detailed feedback per rep
    ai_recommendations TEXT[],

    -- Metadata
    analysis_status TEXT DEFAULT 'completed' CHECK (analysis_status IN ('pending', 'processing', 'completed', 'failed')),
    error_message TEXT
);

-- =====================================================
-- 4. VELOCITY METRICS TABLE (TIER-SPECIFIC)
-- =====================================================
-- Stores velocity-based training metrics per form analysis
CREATE TABLE IF NOT EXISTS velocity_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_analysis_id UUID NOT NULL REFERENCES form_analysis(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Summary metrics (tier-dependent)
    -- TIER 1/2 (Calibrated): Uses m/s values
    -- TIER 3 (Relative): Uses speed_index (0-100)

    avg_peak_velocity NUMERIC(10,3), -- m/s or speed_index depending on tier
    max_peak_velocity NUMERIC(10,3),
    min_peak_velocity NUMERIC(10,3),
    avg_mean_velocity NUMERIC(10,3),

    -- Velocity drop (works for all tiers)
    velocity_drop_percent NUMERIC(5,1) NOT NULL,

    -- ROM (Range of Motion)
    avg_rom_m NUMERIC(10,3), -- Only for calibrated tiers
    avg_rom_relative NUMERIC(5,1), -- Percentage for relative tier

    -- Consistency metrics
    avg_consistency NUMERIC(5,1), -- Only for relative tier

    -- Best/Last rep comparison
    best_rep_velocity NUMERIC(10,3),
    last_rep_velocity NUMERIC(10,3),

    -- Unit tracking (from calibration)
    unit TEXT NOT NULL CHECK (unit IN ('m/s', 'speed_index')),

    -- Notes
    note TEXT -- E.g., "Speed index is relative - first rep = 100"
);

-- =====================================================
-- 5. REP METRICS TABLE (PER-REP DATA)
-- =====================================================
-- Stores detailed metrics for each individual rep
CREATE TABLE IF NOT EXISTS rep_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    velocity_metrics_id UUID NOT NULL REFERENCES velocity_metrics(id) ON DELETE CASCADE,
    form_analysis_id UUID NOT NULL REFERENCES form_analysis(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Rep info
    rep_number INTEGER NOT NULL,
    start_time_seconds NUMERIC(10,3),
    end_time_seconds NUMERIC(10,3),
    duration_seconds NUMERIC(10,3),

    -- Velocity data (tier-dependent)
    peak_velocity NUMERIC(10,3), -- m/s or speed_index
    avg_velocity NUMERIC(10,3),

    -- ROM data
    rom NUMERIC(10,3), -- meters (calibrated) or relative %

    -- Consistency
    consistency_score NUMERIC(5,1), -- Only for relative tier

    -- Phase breakdown
    concentric_duration_seconds NUMERIC(10,3),
    eccentric_duration_seconds NUMERIC(10,3),

    -- Rep quality
    technique_score NUMERIC(4,1), -- Per-rep technique (0-10)
    form_issues TEXT[], -- Array of detected form issues

    -- Unit
    unit TEXT NOT NULL CHECK (unit IN ('m/s', 'speed_index')),

    CONSTRAINT unique_rep_per_analysis UNIQUE(form_analysis_id, rep_number)
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Workout Sessions
CREATE INDEX idx_workout_sessions_user_id ON workout_sessions(user_id);
CREATE INDEX idx_workout_sessions_created_at ON workout_sessions(created_at DESC);
CREATE INDEX idx_workout_sessions_completed_at ON workout_sessions(completed_at DESC);

-- Workout Sets
CREATE INDEX idx_workout_sets_session_id ON workout_sets(session_id);
CREATE INDEX idx_workout_sets_exercise_id ON workout_sets(exercise_id);
CREATE INDEX idx_workout_sets_created_at ON workout_sets(created_at DESC);

-- Form Analysis
CREATE INDEX idx_form_analysis_set_id ON form_analysis(set_id);
CREATE INDEX idx_form_analysis_user_id ON form_analysis(user_id);
CREATE INDEX idx_form_analysis_exercise_id ON form_analysis(exercise_id);
CREATE INDEX idx_form_analysis_calibration_tier ON form_analysis(calibration_tier);
CREATE INDEX idx_form_analysis_created_at ON form_analysis(created_at DESC);

-- Velocity Metrics
CREATE INDEX idx_velocity_metrics_form_analysis_id ON velocity_metrics(form_analysis_id);
CREATE INDEX idx_velocity_metrics_unit ON velocity_metrics(unit);

-- Rep Metrics
CREATE INDEX idx_rep_metrics_velocity_metrics_id ON rep_metrics(velocity_metrics_id);
CREATE INDEX idx_rep_metrics_form_analysis_id ON rep_metrics(form_analysis_id);
CREATE INDEX idx_rep_metrics_rep_number ON rep_metrics(rep_number);

-- =====================================================
-- ROW LEVEL SECURITY (RLS)
-- =====================================================

-- Workout Sessions
ALTER TABLE workout_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own workout sessions"
    ON workout_sessions FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own workout sessions"
    ON workout_sessions FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own workout sessions"
    ON workout_sessions FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own workout sessions"
    ON workout_sessions FOR DELETE
    USING (auth.uid() = user_id);

-- Workout Sets
ALTER TABLE workout_sets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own workout sets"
    ON workout_sets FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM workout_sessions
        WHERE workout_sessions.id = workout_sets.session_id
        AND workout_sessions.user_id = auth.uid()
    ));

CREATE POLICY "Users can insert their own workout sets"
    ON workout_sets FOR INSERT
    WITH CHECK (EXISTS (
        SELECT 1 FROM workout_sessions
        WHERE workout_sessions.id = workout_sets.session_id
        AND workout_sessions.user_id = auth.uid()
    ));

CREATE POLICY "Users can update their own workout sets"
    ON workout_sets FOR UPDATE
    USING (EXISTS (
        SELECT 1 FROM workout_sessions
        WHERE workout_sessions.id = workout_sets.session_id
        AND workout_sessions.user_id = auth.uid()
    ));

CREATE POLICY "Users can delete their own workout sets"
    ON workout_sets FOR DELETE
    USING (EXISTS (
        SELECT 1 FROM workout_sessions
        WHERE workout_sessions.id = workout_sets.session_id
        AND workout_sessions.user_id = auth.uid()
    ));

-- Form Analysis
ALTER TABLE form_analysis ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own form analysis"
    ON form_analysis FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own form analysis"
    ON form_analysis FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own form analysis"
    ON form_analysis FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own form analysis"
    ON form_analysis FOR DELETE
    USING (auth.uid() = user_id);

-- Velocity Metrics
ALTER TABLE velocity_metrics ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own velocity metrics"
    ON velocity_metrics FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM form_analysis
        WHERE form_analysis.id = velocity_metrics.form_analysis_id
        AND form_analysis.user_id = auth.uid()
    ));

CREATE POLICY "Users can insert their own velocity metrics"
    ON velocity_metrics FOR INSERT
    WITH CHECK (EXISTS (
        SELECT 1 FROM form_analysis
        WHERE form_analysis.id = velocity_metrics.form_analysis_id
        AND form_analysis.user_id = auth.uid()
    ));

-- Rep Metrics
ALTER TABLE rep_metrics ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own rep metrics"
    ON rep_metrics FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM form_analysis
        WHERE form_analysis.id = rep_metrics.form_analysis_id
        AND form_analysis.user_id = auth.uid()
    ));

CREATE POLICY "Users can insert their own rep metrics"
    ON rep_metrics FOR INSERT
    WITH CHECK (EXISTS (
        SELECT 1 FROM form_analysis
        WHERE form_analysis.id = rep_metrics.form_analysis_id
        AND form_analysis.user_id = auth.uid()
    ));

-- =====================================================
-- HELPFUL VIEWS
-- =====================================================

-- View: Latest form analyses with velocity summary
CREATE OR REPLACE VIEW latest_form_analyses AS
SELECT
    fa.id,
    fa.user_id,
    fa.created_at,
    fa.exercise_name,
    fa.calibration_tier,
    fa.calibration_unit,
    fa.reps_detected,
    fa.form_score,
    vm.avg_peak_velocity,
    vm.velocity_drop_percent,
    vm.unit
FROM form_analysis fa
LEFT JOIN velocity_metrics vm ON fa.id = vm.form_analysis_id
ORDER BY fa.created_at DESC;

-- View: Session summary with total volume
CREATE OR REPLACE VIEW session_summaries AS
SELECT
    ws.id,
    ws.user_id,
    ws.workout_name,
    ws.started_at,
    ws.completed_at,
    ws.total_sets,
    ws.total_reps,
    ws.total_volume_kg,
    ws.duration_seconds,
    COUNT(DISTINCT fa.id) as analyses_count,
    AVG(fa.form_score) as avg_form_score
FROM workout_sessions ws
LEFT JOIN workout_sets wset ON ws.id = wset.session_id
LEFT JOIN form_analysis fa ON wset.id = fa.set_id
GROUP BY ws.id
ORDER BY ws.created_at DESC;
