-- =====================================================
-- PROMETHEUS EXERCISES SCHEMA
-- =====================================================
-- This schema stores all exercises available in the app library
-- Based on the Exercise.kt data model

-- 1. EXERCISES TABLE
CREATE TABLE IF NOT EXISTS exercises (
    id TEXT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    name TEXT NOT NULL,
    main_muscle_group TEXT,
    secondary_muscle_groups TEXT[], -- Array of muscle groups
    equipment TEXT[], -- Array of equipment
    tempo TEXT,
    rest_time_seconds INTEGER,
    track_reps BOOLEAN DEFAULT true,
    track_sets BOOLEAN DEFAULT true,
    track_weight BOOLEAN DEFAULT true,
    track_rpe BOOLEAN DEFAULT false,
    track_duration BOOLEAN DEFAULT false,
    track_distance BOOLEAN DEFAULT false,
    video_url TEXT,
    tutorial TEXT,
    notes TEXT,
    supports_power_score BOOLEAN DEFAULT false,
    supports_technique_score BOOLEAN DEFAULT false,
    vbt_measurement_type TEXT, -- "average" or "peak"
    vbt_category TEXT, -- "Squat", "Deadlift", "Olympic", "Press", "Row", "Pull"
    sports TEXT[] -- Array of sport categories
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_exercises_name
    ON exercises(name);

CREATE INDEX IF NOT EXISTS idx_exercises_main_muscle
    ON exercises(main_muscle_group);

CREATE INDEX IF NOT EXISTS idx_exercises_created_at
    ON exercises(created_at DESC);

-- GIN indexes for array searches
CREATE INDEX IF NOT EXISTS idx_exercises_sports
    ON exercises USING GIN(sports);

CREATE INDEX IF NOT EXISTS idx_exercises_equipment
    ON exercises USING GIN(equipment);

-- =====================================================
-- ROW LEVEL SECURITY (RLS)
-- =====================================================

-- Enable RLS
ALTER TABLE exercises ENABLE ROW LEVEL SECURITY;

-- Allow everyone to view exercises (public library)
CREATE POLICY "Anyone can view exercises"
    ON exercises FOR SELECT
    USING (true);

-- Only allow authenticated users to insert/update/delete
CREATE POLICY "Authenticated users can insert exercises"
    ON exercises FOR INSERT
    WITH CHECK (true);

CREATE POLICY "Authenticated users can update exercises"
    ON exercises FOR UPDATE
    USING (true);

CREATE POLICY "Authenticated users can delete exercises"
    ON exercises FOR DELETE
    USING (true);