-- ═══════════════════════════════════════════════════════════════════════════
-- PROMETHEUS HYDRATION TRACKING SCHEMA
-- Track daily water intake for athletes
-- ═══════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════
-- HYDRATION LOGS TABLE
-- One row per user per day
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS hydration_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date DATE NOT NULL DEFAULT CURRENT_DATE,

    -- Current intake in milliliters
    water_intake_ml INTEGER NOT NULL DEFAULT 0,

    -- Daily target in milliliters (default ~2500ml for athletes)
    target_ml INTEGER NOT NULL DEFAULT 2500,

    -- Individual water entries for undo/history
    entries JSONB DEFAULT '[]'::jsonb,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    -- One log per user per day
    UNIQUE(user_id, date)
);

-- ═══════════════════════════════════════════════════════════════════════════
-- INDEXES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_hydration_logs_user_date
    ON hydration_logs(user_id, date DESC);

CREATE INDEX IF NOT EXISTS idx_hydration_logs_date
    ON hydration_logs(date);

-- ═══════════════════════════════════════════════════════════════════════════
-- ROW LEVEL SECURITY
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE hydration_logs ENABLE ROW LEVEL SECURITY;

-- Users can only access their own hydration logs
CREATE POLICY "Users can view own hydration logs"
    ON hydration_logs FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own hydration logs"
    ON hydration_logs FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own hydration logs"
    ON hydration_logs FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own hydration logs"
    ON hydration_logs FOR DELETE
    USING (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- FUNCTION: Get or create today's hydration log
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION get_or_create_hydration_log(
    p_user_id UUID,
    p_date DATE DEFAULT CURRENT_DATE,
    p_target_ml INTEGER DEFAULT 2500
)
RETURNS hydration_logs
LANGUAGE plpgsql
AS $$
DECLARE
    v_log hydration_logs;
BEGIN
    -- Try to get existing log
    SELECT * INTO v_log
    FROM hydration_logs
    WHERE user_id = p_user_id AND date = p_date;

    -- Create if not exists
    IF v_log IS NULL THEN
        INSERT INTO hydration_logs (user_id, date, target_ml)
        VALUES (p_user_id, p_date, p_target_ml)
        RETURNING * INTO v_log;
    END IF;

    RETURN v_log;
END;
$$;

-- ═══════════════════════════════════════════════════════════════════════════
-- FUNCTION: Add water intake
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION add_water_intake(
    p_user_id UUID,
    p_amount_ml INTEGER,
    p_date DATE DEFAULT CURRENT_DATE
)
RETURNS hydration_logs
LANGUAGE plpgsql
AS $$
DECLARE
    v_log hydration_logs;
    v_entry JSONB;
BEGIN
    -- Ensure log exists
    PERFORM get_or_create_hydration_log(p_user_id, p_date);

    -- Create entry record
    v_entry := jsonb_build_object(
        'amount_ml', p_amount_ml,
        'timestamp', NOW()
    );

    -- Update the log
    UPDATE hydration_logs
    SET
        water_intake_ml = water_intake_ml + p_amount_ml,
        entries = entries || v_entry,
        updated_at = NOW()
    WHERE user_id = p_user_id AND date = p_date
    RETURNING * INTO v_log;

    RETURN v_log;
END;
$$;

-- ═══════════════════════════════════════════════════════════════════════════
-- FUNCTION: Set water target based on body weight
-- Recommendation: 35-40ml per kg body weight for athletes
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION calculate_water_target(
    body_weight_kg NUMERIC,
    is_athlete BOOLEAN DEFAULT TRUE
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF is_athlete THEN
        -- Athletes: 40ml per kg
        RETURN ROUND(body_weight_kg * 40);
    ELSE
        -- General: 35ml per kg
        RETURN ROUND(body_weight_kg * 35);
    END IF;
END;
$$;

-- ═══════════════════════════════════════════════════════════════════════════
-- TRIGGER: Update updated_at on changes
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION update_hydration_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER hydration_logs_updated_at
    BEFORE UPDATE ON hydration_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_hydration_updated_at();