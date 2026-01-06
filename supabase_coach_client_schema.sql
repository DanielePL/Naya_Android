-- =====================================================
-- COACH-CLIENT RELATIONSHIPS SCHEMA
-- =====================================================
-- Ermöglicht Coaches Zugriff auf Client-Profile (nur bei akzeptierter Verbindung)
-- Injuries sind sichtbar (rechtliche Absicherung für Coaches)

-- =====================================================
-- 1. COACH_CLIENT_CONNECTIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS coach_client_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coach_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Status der Verbindung
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'rejected', 'removed')),

    -- Wer hat die Einladung gesendet?
    invited_by TEXT NOT NULL CHECK (invited_by IN ('coach', 'client')),

    -- Optional: Notizen vom Coach zum Client
    coach_notes TEXT,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    accepted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    -- Constraints
    CONSTRAINT unique_coach_client UNIQUE(coach_id, client_id),
    CONSTRAINT no_self_connection CHECK (coach_id != client_id)
);

-- Indexes für Performance
CREATE INDEX IF NOT EXISTS idx_coach_client_coach_id ON coach_client_connections(coach_id);
CREATE INDEX IF NOT EXISTS idx_coach_client_client_id ON coach_client_connections(client_id);
CREATE INDEX IF NOT EXISTS idx_coach_client_status ON coach_client_connections(status);

-- =====================================================
-- 2. VIEW FÜR COACHES (Client-Profile)
-- =====================================================
-- Coaches sehen: Name, Body Stats, PRs, Goals, Injuries
-- Coaches sehen NICHT: medical_conditions (später Anamnese-Bogen)

CREATE OR REPLACE VIEW v_client_profiles_for_coach AS
SELECT
    up.id,
    up.name,
    up.weight,
    up.height,
    up.age,
    up.training_experience,
    up.personal_records,
    up.goals,
    up.preferred_sports,
    up.injuries,              -- Sichtbar für rechtliche Absicherung
    up.target_workout_duration,
    up.last_seen,
    up.created_at,
    up.updated_at
    -- NICHT: medical_conditions (später mit Anamnese-Bogen)
FROM user_profiles up;

-- =====================================================
-- 3. RLS FÜR COACH_CLIENT_CONNECTIONS
-- =====================================================
ALTER TABLE coach_client_connections ENABLE ROW LEVEL SECURITY;

-- SELECT: Beide Parteien können ihre Verbindungen sehen
CREATE POLICY "Users can view their connections"
ON coach_client_connections FOR SELECT
USING (auth.uid() = coach_id OR auth.uid() = client_id);

-- INSERT: Coaches können Clients einladen
CREATE POLICY "Coaches can invite clients"
ON coach_client_connections FOR INSERT
WITH CHECK (auth.uid() = coach_id AND invited_by = 'coach');

-- INSERT: Clients können Coach-Anfragen senden
CREATE POLICY "Clients can request coaches"
ON coach_client_connections FOR INSERT
WITH CHECK (auth.uid() = client_id AND invited_by = 'client');

-- UPDATE: Beide können Status ändern (accept/reject)
CREATE POLICY "Users can update their connections"
ON coach_client_connections FOR UPDATE
USING (auth.uid() = coach_id OR auth.uid() = client_id);

-- DELETE: Beide können Verbindung entfernen
CREATE POLICY "Users can delete their connections"
ON coach_client_connections FOR DELETE
USING (auth.uid() = coach_id OR auth.uid() = client_id);

-- =====================================================
-- 4. ERWEITERTE RLS FÜR USER_PROFILES
-- =====================================================
-- Coaches können Profile ihrer akzeptierten Clients lesen

-- Zuerst: Alte Policy droppen falls vorhanden
DROP POLICY IF EXISTS "Users can view their own profile" ON user_profiles;

-- Neue Policy: Eigenes Profil ODER akzeptierter Coach
CREATE POLICY "Users and coaches can view profiles"
ON user_profiles FOR SELECT
USING (
    -- Eigenes Profil
    auth.uid() = id
    OR
    -- Coach mit akzeptierter Verbindung
    EXISTS (
        SELECT 1 FROM coach_client_connections
        WHERE coach_id = auth.uid()
        AND client_id = user_profiles.id
        AND status = 'accepted'
    )
);

-- =====================================================
-- 5. TRIGGER FÜR UPDATED_AT
-- =====================================================
CREATE OR REPLACE FUNCTION update_coach_client_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    -- Setze accepted_at wenn Status auf 'accepted' wechselt
    IF NEW.status = 'accepted' AND (OLD.status IS NULL OR OLD.status != 'accepted') THEN
        NEW.accepted_at = NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS coach_client_updated_at_trigger ON coach_client_connections;
CREATE TRIGGER coach_client_updated_at_trigger
    BEFORE UPDATE ON coach_client_connections
    FOR EACH ROW
    EXECUTE FUNCTION update_coach_client_updated_at();

-- =====================================================
-- 6. HELPER FUNCTION: Get Coach's Clients
-- =====================================================
CREATE OR REPLACE FUNCTION get_coach_clients(p_coach_id UUID)
RETURNS TABLE (
    client_id UUID,
    client_name TEXT,
    status TEXT,
    connected_since TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        cc.client_id,
        up.name,
        cc.status,
        cc.accepted_at
    FROM coach_client_connections cc
    JOIN user_profiles up ON up.id = cc.client_id
    WHERE cc.coach_id = p_coach_id
    AND cc.status = 'accepted'
    ORDER BY cc.accepted_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 7. HELPER FUNCTION: Get Client's Coaches
-- =====================================================
CREATE OR REPLACE FUNCTION get_client_coaches(p_client_id UUID)
RETURNS TABLE (
    coach_id UUID,
    coach_name TEXT,
    status TEXT,
    connected_since TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        cc.coach_id,
        up.name,
        cc.status,
        cc.accepted_at
    FROM coach_client_connections cc
    JOIN user_profiles up ON up.id = cc.coach_id
    WHERE cc.client_id = p_client_id
    AND cc.status = 'accepted'
    ORDER BY cc.accepted_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- SUCCESS MESSAGE
-- =====================================================
DO $$
BEGIN
    RAISE NOTICE '✅ Coach-Client Schema erstellt!';
    RAISE NOTICE '📊 Tabelle: coach_client_connections';
    RAISE NOTICE '👁️ View: v_client_profiles_for_coach';
    RAISE NOTICE '🔒 RLS Policies aktiv';
    RAISE NOTICE '⚠️ Injuries sichtbar für Coaches (rechtliche Absicherung)';
    RAISE NOTICE '📋 medical_conditions ausgeblendet (später Anamnese-Bogen)';
END $$;