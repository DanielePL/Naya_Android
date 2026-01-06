-- ============================================================
-- Insert Test User for AI Coach Development
-- ============================================================
-- WICHTIG: Dieses Script muss als Supabase Admin ausgeführt werden
-- User ID: 00000000-0000-0000-0000-000000000000
-- Email: test@prometheus.app
-- ============================================================

-- Step 1: Upsert user
INSERT INTO auth.users (
  id,
  aud,
  role,
  email,
  encrypted_password,
  email_confirmed_at,
  created_at,
  updated_at,
  raw_app_meta_data,
  raw_user_meta_data,
  is_super_admin,
  last_sign_in_at
)
VALUES (
  '00000000-0000-0000-0000-000000000000'::uuid,
  'authenticated',
  'authenticated',
  'test@prometheus.app',
  '$2a$10$YourBcryptHashHere',
  NOW(),
  NOW(),
  NOW(),
  '{"provider":"email","providers":["email"]}'::jsonb,
  '{"name":"Test User","full_name":"Test User"}'::jsonb,
  FALSE,
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    updated_at = NOW();

-- Step 2: Upsert identity with provider_id
INSERT INTO auth.identities (
  id,
  user_id,
  identity_data,
  provider,
  provider_id,
  last_sign_in_at,
  created_at,
  updated_at
)
VALUES (
  '00000000-0000-0000-0000-000000000000'::uuid,
  '00000000-0000-0000-0000-000000000000'::uuid,
  '{"sub":"00000000-0000-0000-0000-000000000000","email":"test@prometheus.app"}'::jsonb,
  'email',
  'test@prometheus.app',
  NOW(),
  NOW(),
  NOW()
)
ON CONFLICT (id) DO UPDATE
SET updated_at = NOW();

-- Step 3: Verify
SELECT
  'auth.users' AS table_name,
  id,
  email,
  email_confirmed_at,
  created_at
FROM auth.users
WHERE id = '00000000-0000-0000-0000-000000000000'

UNION ALL

SELECT
  'auth.identities' AS table_name,
  id,
  provider AS email,
  last_sign_in_at AS email_confirmed_at,
  created_at
FROM auth.identities
WHERE user_id = '00000000-0000-0000-0000-000000000000';

-- ============================================================
-- Test: Conversation erstellen sollte jetzt funktionieren
-- ============================================================
/*
-- Test mit diesem curl command:
curl -X POST https://prometheus-v1-fcu3.onrender.com/api/v1/ai-coach/chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "00000000-0000-0000-0000-000000000000",
    "conversation_id": null,
    "message": "Hello, I want to improve my squat",
    "context": {"current_screen": "ai_coach"}
  }'
*/
