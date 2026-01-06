-- =====================================================
-- UPDATE WORKOUT_SETS TABLE FOR VIDEO & METRICS
-- =====================================================
-- Add columns to store video and velocity metrics

-- Add video storage columns
ALTER TABLE workout_sets
ADD COLUMN IF NOT EXISTS video_url TEXT,
ADD COLUMN IF NOT EXISTS video_storage_type TEXT DEFAULT 'device'
    CHECK (video_storage_type IN ('device', 'cloud')),
ADD COLUMN IF NOT EXISTS video_thumbnail_url TEXT,
ADD COLUMN IF NOT EXISTS video_uploaded_at TIMESTAMPTZ;

-- Add velocity metrics column (JSONB for flexibility)
ALTER TABLE workout_sets
ADD COLUMN IF NOT EXISTS velocity_metrics JSONB;

-- Index for video queries
CREATE INDEX IF NOT EXISTS idx_workout_sets_video_url
ON workout_sets(video_url)
WHERE video_url IS NOT NULL;

-- Index for sets with velocity metrics
CREATE INDEX IF NOT EXISTS idx_workout_sets_velocity_metrics
ON workout_sets USING GIN (velocity_metrics)
WHERE velocity_metrics IS NOT NULL;

-- =====================================================
-- EXAMPLE VELOCITY_METRICS JSONB STRUCTURE
-- =====================================================

/*
{
  "reps_detected": 5,
  "avg_peak_velocity": 0.85,
  "velocity_drop": 12.3,
  "unit": "m/s",
  "calibration_tier": "relative",
  "calibration_method": "Relative Speed Index",
  "calibration_confidence": 0.8,

  // Optional: Store all rep data
  "rep_data": [
    {
      "rep_number": 1,
      "peak_velocity": 0.90,
      "avg_velocity": 0.75,
      "duration_s": 2.1
    },
    {
      "rep_number": 2,
      "peak_velocity": 0.88,
      "avg_velocity": 0.73,
      "duration_s": 2.3
    }
    // ...
  ]
}
*/

-- =====================================================
-- EXAMPLE QUERIES
-- =====================================================

-- Get sets with videos
SELECT
    ws.id,
    ws.exercise_id,
    ws.reps,
    ws.weight_kg,
    ws.video_url,
    ws.video_storage_type,
    ws.velocity_metrics->>'reps_detected' as reps_detected,
    ws.velocity_metrics->>'avg_peak_velocity' as peak_velocity,
    ws.velocity_metrics->>'velocity_drop' as velocity_drop,
    ws.velocity_metrics->>'unit' as unit
FROM workout_sets ws
WHERE ws.video_url IS NOT NULL;

-- Get sets with high velocity drop (fatigue indicator)
SELECT
    ws.id,
    ws.exercise_id,
    CAST(ws.velocity_metrics->>'velocity_drop' AS NUMERIC) as velocity_drop
FROM workout_sets ws
WHERE ws.velocity_metrics IS NOT NULL
  AND CAST(ws.velocity_metrics->>'velocity_drop' AS NUMERIC) > 20.0
ORDER BY velocity_drop DESC;

-- Get calibrated sets only
SELECT
    ws.id,
    ws.velocity_metrics->>'calibration_tier' as tier,
    ws.velocity_metrics->>'avg_peak_velocity' as peak_velocity
FROM workout_sets ws
WHERE ws.velocity_metrics->>'calibration_tier' IN ('pro', 'calibrated');

-- =====================================================
-- HELPER FUNCTION: Get velocity metrics summary
-- =====================================================

CREATE OR REPLACE FUNCTION get_velocity_summary(p_set_id UUID)
RETURNS TABLE (
    reps_detected INTEGER,
    avg_peak_velocity NUMERIC,
    velocity_drop NUMERIC,
    unit TEXT,
    calibration_tier TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        CAST(velocity_metrics->>'reps_detected' AS INTEGER),
        CAST(velocity_metrics->>'avg_peak_velocity' AS NUMERIC),
        CAST(velocity_metrics->>'velocity_drop' AS NUMERIC),
        velocity_metrics->>'unit',
        velocity_metrics->>'calibration_tier'
    FROM workout_sets
    WHERE id = p_set_id
      AND velocity_metrics IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

-- Usage:
-- SELECT * FROM get_velocity_summary('set-uuid-here');
