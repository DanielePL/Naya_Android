-- =====================================================
-- ADD WORKOUT TRACKING TABLES (workout_sessions & workout_sets)
-- =====================================================
-- Assumes 'exercises' table already exists
-- Only creates workout tracking tables with video & VBT support

-- =====================================================
-- 1. WORKOUT_SESSIONS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS workout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    workout_name TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    duration_minutes INTEGER,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for workout sessions
CREATE INDEX IF NOT EXISTS idx_workout_sessions_user_id ON workout_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_started_at ON workout_sessions(started_at DESC);

-- RLS for workout_sessions
ALTER TABLE workout_sessions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their own workout sessions" ON workout_sessions;
CREATE POLICY "Users can view their own workout sessions"
ON workout_sessions FOR SELECT
USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can create their own workout sessions" ON workout_sessions;
CREATE POLICY "Users can create their own workout sessions"
ON workout_sessions FOR INSERT
WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update their own workout sessions" ON workout_sessions;
CREATE POLICY "Users can update their own workout sessions"
ON workout_sessions FOR UPDATE
USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete their own workout sessions" ON workout_sessions;
CREATE POLICY "Users can delete their own workout sessions"
ON workout_sessions FOR DELETE
USING (auth.uid() = user_id);

-- =====================================================
-- 2. WORKOUT_SETS TABLE (with video & metrics support)
-- =====================================================

CREATE TABLE IF NOT EXISTS workout_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id TEXT NOT NULL,  -- References exercises table (no FK constraint to allow flexibility)
    set_number INTEGER NOT NULL,

    -- Performance data
    reps INTEGER,
    weight_kg NUMERIC,
    duration_seconds INTEGER,
    distance_meters NUMERIC,
    rpe NUMERIC,  -- Rate of Perceived Exertion (1-10)
    rest_seconds INTEGER,

    -- Video storage
    video_url TEXT,
    video_storage_type TEXT DEFAULT 'device' CHECK (video_storage_type IN ('device', 'cloud')),
    video_thumbnail_url TEXT,
    video_uploaded_at TIMESTAMPTZ,

    -- Velocity metrics (JSONB for flexibility)
    velocity_metrics JSONB,

    -- Timestamps
    completed_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    -- Ensure set numbers are sequential within a session
    CONSTRAINT unique_session_exercise_set UNIQUE(session_id, exercise_id, set_number)
);

-- Indexes for workout_sets
CREATE INDEX IF NOT EXISTS idx_workout_sets_session_id ON workout_sets(session_id);
CREATE INDEX IF NOT EXISTS idx_workout_sets_exercise_id ON workout_sets(exercise_id);
CREATE INDEX IF NOT EXISTS idx_workout_sets_video_url ON workout_sets(video_url) WHERE video_url IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_workout_sets_velocity_metrics ON workout_sets USING GIN (velocity_metrics) WHERE velocity_metrics IS NOT NULL;

-- RLS for workout_sets
ALTER TABLE workout_sets ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their own workout sets" ON workout_sets;
CREATE POLICY "Users can view their own workout sets"
ON workout_sets FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM workout_sessions
        WHERE workout_sessions.id = workout_sets.session_id
        AND workout_sessions.user_id = auth.uid()
    )
);

DROP POLICY IF EXISTS "Users can create their own workout sets" ON workout_sets;
CREATE POLICY "Users can create their own workout sets"
ON workout_sets FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM workout_sessions
        WHERE workout_sessions.id = workout_sets.session_id
        AND workout_sessions.user_id = auth.uid()
    )
);

DROP POLICY IF EXISTS "Users can update their own workout sets" ON workout_sets;
CREATE POLICY "Users can update their own workout sets"
ON workout_sets FOR UPDATE
USING (
    EXISTS (
        SELECT 1 FROM workout_sessions
        WHERE workout_sessions.id = workout_sets.session_id
        AND workout_sessions.user_id = auth.uid()
    )
);

DROP POLICY IF EXISTS "Users can delete their own workout sets" ON workout_sets;
CREATE POLICY "Users can delete their own workout sets"
ON workout_sets FOR DELETE
USING (
    EXISTS (
        SELECT 1 FROM workout_sessions
        WHERE workout_sessions.id = workout_sets.session_id
        AND workout_sessions.user_id = auth.uid()
    )
);

-- =====================================================
-- 3. HELPER FUNCTIONS
-- =====================================================

-- Function to get velocity metrics summary
CREATE OR REPLACE FUNCTION get_velocity_summary(p_set_id UUID)
RETURNS TABLE (
    reps_detected INTEGER,
    avg_peak_velocity NUMERIC,
    velocity_drop NUMERIC,
    unit TEXT,
    exercise_type TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        CAST(velocity_metrics->>'reps_detected' AS INTEGER),
        CAST(velocity_metrics->>'avg_peak_velocity' AS NUMERIC),
        CAST(velocity_metrics->>'velocity_drop' AS NUMERIC),
        velocity_metrics->>'unit',
        velocity_metrics->>'exercise_type'
    FROM workout_sets
    WHERE id = p_set_id
      AND velocity_metrics IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 4. EXAMPLE VELOCITY_METRICS JSONB STRUCTURE
-- =====================================================

/*
Example velocity_metrics JSONB format:

{
  "reps_detected": 5,
  "avg_peak_velocity": 0.463,
  "velocity_drop": 13.4,
  "unit": "m/s",
  "exercise_type": "general",
  "tracked_landmark": "hip",
  "fps": 30.0,
  "rep_data": [
    {
      "rep_number": 1,
      "peak_velocity": 0.90,
      "avg_velocity": 0.75,
      "duration_s": 2.1,
      "rom_m": 0.82
    },
    {
      "rep_number": 2,
      "peak_velocity": 0.88,
      "avg_velocity": 0.73,
      "duration_s": 2.3,
      "rom_m": 0.80
    }
  ]
}
*/

-- =====================================================
-- 5. EXAMPLE QUERIES
-- =====================================================

/*
-- Get all sets with videos for current user
SELECT
    ws.id,
    ws.exercise_id,
    ws.reps,
    ws.weight_kg,
    ws.video_url,
    ws.velocity_metrics->>'avg_peak_velocity' as peak_velocity,
    ws.velocity_metrics->>'velocity_drop' as velocity_drop
FROM workout_sets ws
JOIN workout_sessions s ON s.id = ws.session_id
WHERE s.user_id = auth.uid()
  AND ws.video_url IS NOT NULL
ORDER BY ws.completed_at DESC;

-- Get sets with high velocity drop (fatigue indicator)
SELECT
    ws.id,
    ws.exercise_id,
    ws.set_number,
    CAST(ws.velocity_metrics->>'velocity_drop' AS NUMERIC) as velocity_drop
FROM workout_sets ws
JOIN workout_sessions s ON s.id = ws.session_id
WHERE s.user_id = auth.uid()
  AND ws.velocity_metrics IS NOT NULL
  AND CAST(ws.velocity_metrics->>'velocity_drop' AS NUMERIC) > 20.0
ORDER BY velocity_drop DESC;

-- Get velocity summary for a specific set
SELECT * FROM get_velocity_summary('set-uuid-here');

-- Count sets with videos per exercise
SELECT
    ws.exercise_id,
    COUNT(*) as sets_with_video,
    AVG(CAST(ws.velocity_metrics->>'avg_peak_velocity' AS NUMERIC)) as avg_velocity
FROM workout_sets ws
JOIN workout_sessions s ON s.id = ws.session_id
WHERE s.user_id = auth.uid()
  AND ws.video_url IS NOT NULL
  AND ws.velocity_metrics IS NOT NULL
GROUP BY ws.exercise_id
ORDER BY sets_with_video DESC;
*/

-- =====================================================
-- SUCCESS MESSAGE
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '✅ Workout tracking tables created successfully!';
    RAISE NOTICE '📊 Tables: workout_sessions, workout_sets';
    RAISE NOTICE '🎥 Video storage support: video_url, video_storage_type';
    RAISE NOTICE '⚡ VBT metrics support: velocity_metrics (JSONB)';
    RAISE NOTICE '🔒 Row Level Security policies applied';
    RAISE NOTICE '📝 Helper function: get_velocity_summary(set_id)';
END $$;
