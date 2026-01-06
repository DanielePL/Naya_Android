-- =====================================================
-- CROSSFIT WOD DATABASE SCHEMA
-- Prometheus Fitness App - CrossFit Platform Extension
-- =====================================================
-- Run this in Supabase SQL Editor to create WOD tables

-- =====================================================
-- 1. WOD TYPE DEFINITIONS
-- =====================================================
-- Stores all possible WOD types (AMRAP, EMOM, ForTime, etc.)

CREATE TABLE IF NOT EXISTS wod_types (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    scoring_format TEXT NOT NULL DEFAULT 'rounds_reps', -- 'rounds_reps', 'time', 'weight', 'reps', 'pass_fail'
    has_time_cap BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Insert standard WOD types
INSERT INTO wod_types (id, name, description, scoring_format, has_time_cap) VALUES
    ('amrap', 'AMRAP', 'As Many Rounds/Reps As Possible in given time', 'rounds_reps', true),
    ('emom', 'EMOM', 'Every Minute On the Minute', 'pass_fail', true),
    ('for_time', 'For Time', 'Complete workout as fast as possible', 'time', true),
    ('rft', 'Rounds For Time', 'Complete X rounds as fast as possible', 'time', true),
    ('chipper', 'Chipper', 'Complete all movements in sequence', 'time', true),
    ('ladder', 'Ladder', 'Ascending/Descending rep scheme', 'time', true),
    ('tabata', 'Tabata', '20 sec work / 10 sec rest intervals', 'reps', true),
    ('death_by', 'Death By', 'Add 1 rep each minute until failure', 'reps', false),
    ('max_effort', 'Max Effort', 'Find maximum weight/reps', 'weight', false),
    ('partner', 'Partner WOD', 'Workout with partner, alternating work', 'rounds_reps', true),
    ('team', 'Team WOD', 'Team-based workout', 'rounds_reps', true),
    ('hero', 'Hero WOD', 'Named WOD honoring fallen heroes', 'time', true),
    ('girl', 'Girl WOD', 'Classic CrossFit benchmark (Fran, Grace, etc.)', 'time', true),
    ('open', 'Open WOD', 'CrossFit Open style workout', 'rounds_reps', true),
    ('custom', 'Custom', 'Custom WOD format', 'custom', false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 2. WOD TEMPLATES TABLE
-- =====================================================
-- Main WOD storage - similar to workout_templates but WOD-specific

CREATE TABLE IF NOT EXISTS wod_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Basic Info
    name TEXT NOT NULL,
    description TEXT,

    -- WOD Configuration
    wod_type TEXT NOT NULL REFERENCES wod_types(id) DEFAULT 'amrap',
    time_cap_seconds INTEGER,  -- Time cap in seconds (null = no cap)
    target_rounds INTEGER,     -- For RFT: how many rounds to complete

    -- Rep Scheme (for complex patterns like 21-15-9)
    rep_scheme TEXT,           -- JSON array like [21, 15, 9] or null for fixed
    rep_scheme_type TEXT,      -- 'fixed', 'descending', 'ascending', 'pyramid', 'custom'

    -- Scoring
    scoring_type TEXT NOT NULL DEFAULT 'rounds_reps',

    -- Source & Attribution
    source TEXT,               -- 'whiteboard_scan', 'manual', 'crossfit_main', 'box_programming'
    source_box_name TEXT,      -- Name of CrossFit box if from a specific gym
    source_date DATE,          -- Date the WOD was programmed
    source_image_url TEXT,     -- URL to original whiteboard photo

    -- User & Visibility
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    is_public BOOLEAN DEFAULT false,
    is_verified BOOLEAN DEFAULT false,  -- Admin verified content

    -- Categorization
    difficulty TEXT CHECK (difficulty IN ('beginner', 'intermediate', 'advanced', 'elite')),
    estimated_duration_minutes INTEGER,
    primary_focus TEXT[],      -- ['cardio', 'strength', 'gymnastics', 'weightlifting']
    equipment_needed TEXT[],   -- ['barbell', 'pullup_bar', 'box', 'rower']

    -- Metadata
    tags TEXT[] DEFAULT '{}',
    likes_count INTEGER DEFAULT 0,
    completions_count INTEGER DEFAULT 0,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- 3. WOD MOVEMENTS TABLE
-- =====================================================
-- Individual movements within a WOD

CREATE TABLE IF NOT EXISTS wod_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wod_template_id UUID NOT NULL REFERENCES wod_templates(id) ON DELETE CASCADE,

    -- Exercise Reference (nullable for custom/unknown movements)
    exercise_id UUID REFERENCES exercises_new(id) ON DELETE SET NULL,

    -- Movement Details (used if no exercise_id or to override)
    movement_name TEXT NOT NULL,
    movement_description TEXT,

    -- Ordering
    order_index INTEGER NOT NULL DEFAULT 0,
    segment INTEGER DEFAULT 1,  -- For multi-part WODs (Part A, Part B)

    -- Reps Configuration
    rep_type TEXT NOT NULL DEFAULT 'fixed', -- 'fixed', 'calories', 'distance', 'time', 'max'
    reps INTEGER,              -- Fixed rep count
    reps_male INTEGER,         -- Male Rx reps (if different)
    reps_female INTEGER,       -- Female Rx reps (if different)

    -- Distance/Calories (alternative to reps)
    distance_meters INTEGER,
    calories INTEGER,
    time_seconds INTEGER,      -- For movements like "1 min plank hold"

    -- Weight Configuration
    weight_type TEXT DEFAULT 'bodyweight', -- 'bodyweight', 'fixed', 'percentage', 'ascending'
    weight_kg_male DECIMAL(10,2),
    weight_kg_female DECIMAL(10,2),
    weight_percentage INTEGER, -- % of 1RM

    -- For EMOM: which minute(s) this movement is on
    emom_minutes INTEGER[],    -- e.g., [1, 3, 5] for odd minutes

    -- Notes
    notes TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- 4. WOD SCALING OPTIONS
-- =====================================================
-- Rx, Scaled, and Foundations variations

CREATE TABLE IF NOT EXISTS wod_scaling (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wod_template_id UUID NOT NULL REFERENCES wod_templates(id) ON DELETE CASCADE,
    wod_movement_id UUID REFERENCES wod_movements(id) ON DELETE CASCADE,

    -- Scaling Level
    scaling_level TEXT NOT NULL CHECK (scaling_level IN ('rx', 'scaled', 'foundations', 'masters')),

    -- Override Values
    reps INTEGER,
    reps_male INTEGER,
    reps_female INTEGER,
    weight_kg_male DECIMAL(10,2),
    weight_kg_female DECIMAL(10,2),

    -- Alternative Movement
    alternative_exercise_id UUID REFERENCES exercises_new(id),
    alternative_movement_name TEXT,
    alternative_description TEXT,  -- e.g., "Ring rows instead of pull-ups"

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- 5. WOD RESULTS / SCORES
-- =====================================================
-- User performances on WODs

CREATE TABLE IF NOT EXISTS wod_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    wod_template_id UUID NOT NULL REFERENCES wod_templates(id) ON DELETE CASCADE,

    -- When completed
    completed_at TIMESTAMPTZ DEFAULT NOW(),

    -- Score (flexible based on WOD type)
    score_type TEXT NOT NULL,  -- 'rounds_reps', 'time', 'weight', 'reps', 'pass_fail'

    -- For rounds + reps scoring
    rounds_completed INTEGER,
    reps_completed INTEGER,    -- Extra reps beyond full rounds

    -- For time scoring (in seconds)
    time_seconds INTEGER,

    -- For weight scoring
    weight_kg DECIMAL(10,2),

    -- For reps scoring
    total_reps INTEGER,

    -- Scaling used
    scaling_level TEXT DEFAULT 'rx',

    -- Was it completed within time cap?
    completed_within_cap BOOLEAN DEFAULT true,

    -- Notes & Media
    notes TEXT,
    video_url TEXT,

    -- Verification
    is_verified BOOLEAN DEFAULT false,  -- Coach/judge verified

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- 6. FAMOUS/BENCHMARK WODS
-- =====================================================
-- Store classic CrossFit benchmark WODs

CREATE TABLE IF NOT EXISTS benchmark_wods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wod_template_id UUID NOT NULL REFERENCES wod_templates(id) ON DELETE CASCADE,

    -- Benchmark Info
    benchmark_name TEXT NOT NULL UNIQUE,  -- 'Fran', 'Murph', 'Grace'
    benchmark_category TEXT NOT NULL,     -- 'girl', 'hero', 'open', 'games'

    -- History
    origin_story TEXT,
    created_year INTEGER,
    named_after TEXT,  -- For Hero WODs

    -- Standards
    official_standards TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_wod_templates_user_id ON wod_templates(user_id);
CREATE INDEX IF NOT EXISTS idx_wod_templates_wod_type ON wod_templates(wod_type);
CREATE INDEX IF NOT EXISTS idx_wod_templates_is_public ON wod_templates(is_public);
CREATE INDEX IF NOT EXISTS idx_wod_templates_created_at ON wod_templates(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wod_templates_source ON wod_templates(source);
CREATE INDEX IF NOT EXISTS idx_wod_templates_difficulty ON wod_templates(difficulty);

CREATE INDEX IF NOT EXISTS idx_wod_movements_wod_template ON wod_movements(wod_template_id);
CREATE INDEX IF NOT EXISTS idx_wod_movements_order ON wod_movements(wod_template_id, order_index);
CREATE INDEX IF NOT EXISTS idx_wod_movements_exercise ON wod_movements(exercise_id);

CREATE INDEX IF NOT EXISTS idx_wod_scaling_wod_template ON wod_scaling(wod_template_id);
CREATE INDEX IF NOT EXISTS idx_wod_scaling_movement ON wod_scaling(wod_movement_id);
CREATE INDEX IF NOT EXISTS idx_wod_scaling_level ON wod_scaling(scaling_level);

CREATE INDEX IF NOT EXISTS idx_wod_results_user ON wod_results(user_id);
CREATE INDEX IF NOT EXISTS idx_wod_results_wod ON wod_results(wod_template_id);
CREATE INDEX IF NOT EXISTS idx_wod_results_completed ON wod_results(user_id, completed_at DESC);
CREATE INDEX IF NOT EXISTS idx_wod_results_score ON wod_results(wod_template_id, score_type, rounds_completed DESC, reps_completed DESC);

-- =====================================================
-- ROW LEVEL SECURITY
-- =====================================================

ALTER TABLE wod_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE wod_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE wod_scaling ENABLE ROW LEVEL SECURITY;
ALTER TABLE wod_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE benchmark_wods ENABLE ROW LEVEL SECURITY;

-- WOD Templates: View own + public, CRUD own
CREATE POLICY "View own and public WODs"
    ON wod_templates FOR SELECT
    USING (user_id = auth.uid() OR is_public = true OR user_id IS NULL);

CREATE POLICY "Insert own WODs"
    ON wod_templates FOR INSERT
    WITH CHECK (user_id = auth.uid() OR user_id IS NULL);

CREATE POLICY "Update own WODs"
    ON wod_templates FOR UPDATE
    USING (user_id = auth.uid());

CREATE POLICY "Delete own WODs"
    ON wod_templates FOR DELETE
    USING (user_id = auth.uid());

-- WOD Movements: Follow parent WOD permissions
CREATE POLICY "View movements of accessible WODs"
    ON wod_movements FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM wod_templates wt
            WHERE wt.id = wod_movements.wod_template_id
            AND (wt.user_id = auth.uid() OR wt.is_public = true OR wt.user_id IS NULL)
        )
    );

CREATE POLICY "Insert movements to own WODs"
    ON wod_movements FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM wod_templates wt
            WHERE wt.id = wod_movements.wod_template_id
            AND (wt.user_id = auth.uid() OR wt.user_id IS NULL)
        )
    );

CREATE POLICY "Update movements in own WODs"
    ON wod_movements FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM wod_templates wt
            WHERE wt.id = wod_movements.wod_template_id
            AND wt.user_id = auth.uid()
        )
    );

CREATE POLICY "Delete movements from own WODs"
    ON wod_movements FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM wod_templates wt
            WHERE wt.id = wod_movements.wod_template_id
            AND wt.user_id = auth.uid()
        )
    );

-- WOD Scaling: Follow parent WOD permissions
CREATE POLICY "View scaling of accessible WODs"
    ON wod_scaling FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM wod_templates wt
            WHERE wt.id = wod_scaling.wod_template_id
            AND (wt.user_id = auth.uid() OR wt.is_public = true OR wt.user_id IS NULL)
        )
    );

CREATE POLICY "Manage scaling of own WODs"
    ON wod_scaling FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM wod_templates wt
            WHERE wt.id = wod_scaling.wod_template_id
            AND wt.user_id = auth.uid()
        )
    );

-- WOD Results: Only own results
CREATE POLICY "View own results"
    ON wod_results FOR SELECT
    USING (user_id = auth.uid());

CREATE POLICY "Insert own results"
    ON wod_results FOR INSERT
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Update own results"
    ON wod_results FOR UPDATE
    USING (user_id = auth.uid());

CREATE POLICY "Delete own results"
    ON wod_results FOR DELETE
    USING (user_id = auth.uid());

-- Benchmark WODs: Public read
CREATE POLICY "Anyone can view benchmark WODs"
    ON benchmark_wods FOR SELECT
    USING (true);

-- =====================================================
-- TRIGGER: Update timestamps
-- =====================================================

CREATE OR REPLACE FUNCTION update_wod_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_wod_templates_updated_at'
    ) THEN
        CREATE TRIGGER trigger_wod_templates_updated_at
            BEFORE UPDATE ON wod_templates
            FOR EACH ROW
            EXECUTE FUNCTION update_wod_updated_at();
    END IF;
END $$;

-- =====================================================
-- TRIGGER: Update completion count
-- =====================================================

CREATE OR REPLACE FUNCTION update_wod_completion_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE wod_templates
        SET completions_count = completions_count + 1
        WHERE id = NEW.wod_template_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE wod_templates
        SET completions_count = GREATEST(0, completions_count - 1)
        WHERE id = OLD.wod_template_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trigger_wod_completion_count'
    ) THEN
        CREATE TRIGGER trigger_wod_completion_count
            AFTER INSERT OR DELETE ON wod_results
            FOR EACH ROW
            EXECUTE FUNCTION update_wod_completion_count();
    END IF;
END $$;

-- =====================================================
-- HELPER FUNCTIONS
-- =====================================================

-- Function to get WOD with all movements
CREATE OR REPLACE FUNCTION get_wod_with_movements(wod_id UUID)
RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    SELECT json_build_object(
        'wod', row_to_json(wt.*),
        'movements', (
            SELECT json_agg(
                json_build_object(
                    'movement', row_to_json(wm.*),
                    'exercise', row_to_json(e.*)
                )
                ORDER BY wm.segment, wm.order_index
            )
            FROM wod_movements wm
            LEFT JOIN exercises_new e ON e.id = wm.exercise_id
            WHERE wm.wod_template_id = wod_id
        ),
        'scaling', (
            SELECT json_agg(row_to_json(ws.*))
            FROM wod_scaling ws
            WHERE ws.wod_template_id = wod_id
        )
    ) INTO result
    FROM wod_templates wt
    WHERE wt.id = wod_id;

    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to search WODs
CREATE OR REPLACE FUNCTION search_wods(
    search_query TEXT DEFAULT NULL,
    wod_type_filter TEXT DEFAULT NULL,
    difficulty_filter TEXT DEFAULT NULL,
    max_duration INTEGER DEFAULT NULL,
    limit_count INTEGER DEFAULT 20,
    offset_count INTEGER DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    name TEXT,
    description TEXT,
    wod_type TEXT,
    time_cap_seconds INTEGER,
    difficulty TEXT,
    estimated_duration_minutes INTEGER,
    completions_count INTEGER,
    created_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        wt.id,
        wt.name,
        wt.description,
        wt.wod_type,
        wt.time_cap_seconds,
        wt.difficulty,
        wt.estimated_duration_minutes,
        wt.completions_count,
        wt.created_at
    FROM wod_templates wt
    WHERE
        (wt.is_public = true OR wt.user_id = auth.uid() OR wt.user_id IS NULL)
        AND (search_query IS NULL OR wt.name ILIKE '%' || search_query || '%')
        AND (wod_type_filter IS NULL OR wt.wod_type = wod_type_filter)
        AND (difficulty_filter IS NULL OR wt.difficulty = difficulty_filter)
        AND (max_duration IS NULL OR wt.estimated_duration_minutes <= max_duration)
    ORDER BY wt.completions_count DESC, wt.created_at DESC
    LIMIT limit_count
    OFFSET offset_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- SAMPLE DATA: Classic Benchmark WODs
-- =====================================================

-- Note: Run this separately after creating the tables
-- These are example benchmark WODs to populate the database

/*
-- Fran (21-15-9 Thrusters and Pull-ups)
INSERT INTO wod_templates (name, description, wod_type, time_cap_seconds, rep_scheme, rep_scheme_type, scoring_type, source, is_public, is_verified, difficulty, estimated_duration_minutes, primary_focus, equipment_needed)
VALUES (
    'Fran',
    '21-15-9 Thrusters and Pull-ups. One of the most famous CrossFit benchmark workouts.',
    'for_time',
    600, -- 10 min cap
    '[21, 15, 9]',
    'descending',
    'time',
    'crossfit_main',
    true,
    true,
    'intermediate',
    5,
    ARRAY['cardio', 'strength'],
    ARRAY['barbell', 'pullup_bar']
);

-- Then add movements for Fran
-- (Would need to look up exercise_id for Thrusters and Pull-ups)
*/

COMMENT ON TABLE wod_templates IS 'CrossFit WOD templates - stores workout configurations';
COMMENT ON TABLE wod_movements IS 'Individual movements within a WOD';
COMMENT ON TABLE wod_scaling IS 'Scaling options (Rx, Scaled, Foundations) for WOD movements';
COMMENT ON TABLE wod_results IS 'User performances/scores on WODs';
COMMENT ON TABLE benchmark_wods IS 'Links WOD templates to official benchmark names (Fran, Murph, etc.)';