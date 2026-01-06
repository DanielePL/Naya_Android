-- =====================================================
-- TEMPORARILY DISABLE RLS FOR USER PROFILES (FOR TESTING)
-- =====================================================
-- This allows authenticated users to insert/update their profiles
-- WARNING: Only use this for development/testing!

-- Option 1: Completely disable RLS (easiest for testing)
ALTER TABLE user_profiles DISABLE ROW LEVEL SECURITY;

-- Verify RLS is disabled
SELECT tablename, rowsecurity
FROM pg_tables
WHERE tablename = 'user_profiles';

-- After testing, you can re-enable it with:
-- ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
