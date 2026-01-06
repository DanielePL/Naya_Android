-- ============================================================
-- MENOTRACKER - Menopause Schema Migration
-- ============================================================
-- Erstellt Tabellen für Symptom-Tracking, Schlaf, Hormon-Phasen
-- und Knochengesundheit
-- ============================================================

-- ============================================================
-- 1. SYMPTOM LOGS
-- Speichert individuelle Symptom-Einträge
-- ============================================================
CREATE TABLE IF NOT EXISTS symptom_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    symptom_type TEXT NOT NULL,
    intensity INTEGER CHECK (intensity BETWEEN 1 AND 10),
    duration_minutes INTEGER,
    triggers TEXT[],
    notes TEXT,
    logged_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index für schnelle Abfragen nach User und Datum
CREATE INDEX IF NOT EXISTS idx_symptom_logs_user_date
ON symptom_logs(user_id, logged_at DESC);

-- Index für Symptom-Typ Analysen
CREATE INDEX IF NOT EXISTS idx_symptom_logs_type
ON symptom_logs(user_id, symptom_type);

-- RLS aktivieren
ALTER TABLE symptom_logs ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view own symptom logs"
ON symptom_logs FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own symptom logs"
ON symptom_logs FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own symptom logs"
ON symptom_logs FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own symptom logs"
ON symptom_logs FOR DELETE
USING (auth.uid() = user_id);

-- ============================================================
-- 2. MENOPAUSE PROFILES
-- Speichert das Menopause-Stadium und Zyklus-Informationen
-- ============================================================
CREATE TABLE IF NOT EXISTS menopause_profiles (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE UNIQUE NOT NULL,
    stage TEXT DEFAULT 'premenopause' CHECK (stage IN (
        'premenopause',
        'early_perimenopause',
        'late_perimenopause',
        'menopause',
        'postmenopause'
    )),
    last_period_date DATE,
    average_cycle_length INTEGER,
    hrt_status TEXT DEFAULT 'none' CHECK (hrt_status IN (
        'none',
        'considering',
        'current',
        'past'
    )),
    primary_symptoms TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS aktivieren
ALTER TABLE menopause_profiles ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view own menopause profile"
ON menopause_profiles FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own menopause profile"
ON menopause_profiles FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own menopause profile"
ON menopause_profiles FOR UPDATE
USING (auth.uid() = user_id);

-- ============================================================
-- 3. SLEEP LOGS
-- Speichert tägliche Schlaf-Einträge
-- ============================================================
CREATE TABLE IF NOT EXISTS sleep_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    date DATE NOT NULL,
    bed_time TIME,
    wake_time TIME,
    total_hours FLOAT,
    quality_rating INTEGER CHECK (quality_rating BETWEEN 1 AND 5),
    interruptions INTEGER DEFAULT 0,
    interruption_reasons TEXT[],
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Unique constraint: Ein Eintrag pro User pro Tag
CREATE UNIQUE INDEX IF NOT EXISTS idx_sleep_logs_user_date_unique
ON sleep_logs(user_id, date);

-- Index für Zeitreihen-Abfragen
CREATE INDEX IF NOT EXISTS idx_sleep_logs_user_date
ON sleep_logs(user_id, date DESC);

-- RLS aktivieren
ALTER TABLE sleep_logs ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view own sleep logs"
ON sleep_logs FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own sleep logs"
ON sleep_logs FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own sleep logs"
ON sleep_logs FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own sleep logs"
ON sleep_logs FOR DELETE
USING (auth.uid() = user_id);

-- ============================================================
-- 4. BONE HEALTH LOGS
-- Speichert tägliche Knochengesundheits-Nährstoffe
-- ============================================================
CREATE TABLE IF NOT EXISTS bone_health_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    date DATE NOT NULL,
    calcium_mg FLOAT DEFAULT 0,
    vitamin_d_iu FLOAT DEFAULT 0,
    omega3_mg FLOAT DEFAULT 0,
    magnesium_mg FLOAT DEFAULT 0,
    strength_training_done BOOLEAN DEFAULT FALSE,
    weight_bearing_minutes INTEGER DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Unique constraint: Ein Eintrag pro User pro Tag
CREATE UNIQUE INDEX IF NOT EXISTS idx_bone_health_user_date_unique
ON bone_health_logs(user_id, date);

-- Index für Zeitreihen-Abfragen
CREATE INDEX IF NOT EXISTS idx_bone_health_user_date
ON bone_health_logs(user_id, date DESC);

-- RLS aktivieren
ALTER TABLE bone_health_logs ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view own bone health logs"
ON bone_health_logs FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own bone health logs"
ON bone_health_logs FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own bone health logs"
ON bone_health_logs FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own bone health logs"
ON bone_health_logs FOR DELETE
USING (auth.uid() = user_id);

-- ============================================================
-- 5. USER PROFILES ERWEITERUNG
-- Fügt Menopause-bezogene Felder hinzu
-- ============================================================
DO $$
BEGIN
    -- Menopause Stadium
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_profiles' AND column_name = 'menopause_stage'
    ) THEN
        ALTER TABLE user_profiles ADD COLUMN menopause_stage TEXT DEFAULT 'premenopause';
    END IF;

    -- Letzte Periode
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_profiles' AND column_name = 'last_period_date'
    ) THEN
        ALTER TABLE user_profiles ADD COLUMN last_period_date DATE;
    END IF;

    -- Knochengesundheits-Fokus
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_profiles' AND column_name = 'bone_health_focus'
    ) THEN
        ALTER TABLE user_profiles ADD COLUMN bone_health_focus BOOLEAN DEFAULT FALSE;
    END IF;

    -- Primäre Symptome
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_profiles' AND column_name = 'primary_symptoms'
    ) THEN
        ALTER TABLE user_profiles ADD COLUMN primary_symptoms TEXT[];
    END IF;

    -- Geburtsdatum (für Alter)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_profiles' AND column_name = 'birth_date'
    ) THEN
        ALTER TABLE user_profiles ADD COLUMN birth_date DATE;
    END IF;
END $$;

-- ============================================================
-- 6. HILFSFUNKTIONEN
-- ============================================================

-- Funktion: Symptom-Statistiken der letzten 7 Tage
CREATE OR REPLACE FUNCTION get_weekly_symptom_stats(p_user_id UUID)
RETURNS TABLE (
    symptom_type TEXT,
    occurrence_count BIGINT,
    avg_intensity FLOAT,
    max_intensity INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        sl.symptom_type,
        COUNT(*)::BIGINT as occurrence_count,
        AVG(sl.intensity)::FLOAT as avg_intensity,
        MAX(sl.intensity) as max_intensity
    FROM symptom_logs sl
    WHERE sl.user_id = p_user_id
      AND sl.logged_at >= NOW() - INTERVAL '7 days'
    GROUP BY sl.symptom_type
    ORDER BY occurrence_count DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Funktion: Schlafqualität der letzten 7 Tage
CREATE OR REPLACE FUNCTION get_weekly_sleep_stats(p_user_id UUID)
RETURNS TABLE (
    avg_hours FLOAT,
    avg_quality FLOAT,
    total_interruptions BIGINT,
    days_logged BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        AVG(sl.total_hours)::FLOAT as avg_hours,
        AVG(sl.quality_rating)::FLOAT as avg_quality,
        SUM(sl.interruptions)::BIGINT as total_interruptions,
        COUNT(*)::BIGINT as days_logged
    FROM sleep_logs sl
    WHERE sl.user_id = p_user_id
      AND sl.date >= CURRENT_DATE - INTERVAL '7 days';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Funktion: Knochengesundheit Wochendurchschnitt
CREATE OR REPLACE FUNCTION get_weekly_bone_health_stats(p_user_id UUID)
RETURNS TABLE (
    avg_calcium FLOAT,
    avg_vitamin_d FLOAT,
    avg_omega3 FLOAT,
    strength_training_days BIGINT,
    days_logged BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        AVG(bh.calcium_mg)::FLOAT as avg_calcium,
        AVG(bh.vitamin_d_iu)::FLOAT as avg_vitamin_d,
        AVG(bh.omega3_mg)::FLOAT as avg_omega3,
        SUM(CASE WHEN bh.strength_training_done THEN 1 ELSE 0 END)::BIGINT as strength_training_days,
        COUNT(*)::BIGINT as days_logged
    FROM bone_health_logs bh
    WHERE bh.user_id = p_user_id
      AND bh.date >= CURRENT_DATE - INTERVAL '7 days';
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================
-- GRANT PERMISSIONS
-- ============================================================
GRANT EXECUTE ON FUNCTION get_weekly_symptom_stats TO authenticated;
GRANT EXECUTE ON FUNCTION get_weekly_sleep_stats TO authenticated;
GRANT EXECUTE ON FUNCTION get_weekly_bone_health_stats TO authenticated;

-- ============================================================
-- KOMMENTARE FÜR DOKUMENTATION
-- ============================================================
COMMENT ON TABLE symptom_logs IS 'Speichert Menopause-Symptom-Einträge mit Intensität und Triggern';
COMMENT ON TABLE menopause_profiles IS 'Speichert das Menopause-Stadium und HRT-Status pro User';
COMMENT ON TABLE sleep_logs IS 'Tägliche Schlaf-Logs mit Qualität und Unterbrechungen';
COMMENT ON TABLE bone_health_logs IS 'Tägliche Knochengesundheits-Nährstoff-Tracking';
