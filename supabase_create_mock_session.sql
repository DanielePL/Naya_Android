-- =====================================================
-- CREATE MOCK WORKOUT SESSION
-- =====================================================
-- Creates a dummy session for form-check-only workflow
-- This is needed because workout_sets requires a session_id

-- Create a dummy user if not exists (for testing without auth)
-- In production, this would be replaced with real authenticated users

INSERT INTO workout_sessions (id, user_id, workout_name, started_at)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    '00000000-0000-0000-0000-000000000001', -- Mock user ID
    'Form Check Only',
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- Note: If you get an auth.users foreign key error, you need to either:
-- 1. Temporarily disable the foreign key constraint, OR
-- 2. Remove the REFERENCES auth.users constraint from workout_sessions

-- Option 1: Disable FK temporarily (run this if needed)
ALTER TABLE workout_sessions
DROP CONSTRAINT IF EXISTS workout_sessions_user_id_fkey;

-- Re-create the mock session without FK constraint
INSERT INTO workout_sessions (id, user_id, workout_name, started_at)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    '00000000-0000-0000-0000-000000000001',
    'Form Check Only',
    NOW()
)
ON CONFLICT (id) DO UPDATE SET workout_name = 'Form Check Only';

DO $$
BEGIN
    RAISE NOTICE '✅ Mock workout session created!';
    RAISE NOTICE '   Session ID: 00000000-0000-0000-0000-000000000000';
    RAISE NOTICE '   This session is used for form-check-only workflows';
END $$;
