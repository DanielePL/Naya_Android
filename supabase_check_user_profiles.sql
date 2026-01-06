-- =====================================================
-- CHECK USER PROFILES DATA
-- =====================================================

-- 1. See all user profiles
SELECT
    id,
    name,
    weight,
    height,
    training_experience,
    preferred_sports,
    target_workout_duration,
    created_at,
    updated_at
FROM user_profiles
ORDER BY updated_at DESC;

-- 2. See personal records (if any)
SELECT
    id,
    name,
    personal_records
FROM user_profiles
WHERE personal_records IS NOT NULL
  AND personal_records != '{}'::jsonb;

-- 3. Count total profiles
SELECT COUNT(*) as total_profiles FROM user_profiles;

-- 4. See your specific profile (replace with your user ID)
-- Get your user ID first:
SELECT
    id as user_id,
    email,
    created_at as signed_up_at
FROM auth.users
ORDER BY created_at DESC
LIMIT 5;

-- 5. Then check your profile data:
-- SELECT * FROM user_profiles WHERE id = 'YOUR_USER_ID_HERE';