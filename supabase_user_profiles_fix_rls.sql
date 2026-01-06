-- =====================================================
-- FIX RLS POLICIES FOR USER PROFILES
-- =====================================================

-- First, let's check current policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'user_profiles';

-- Drop all existing policies
DROP POLICY IF EXISTS "Users can view their own profile" ON user_profiles;
DROP POLICY IF EXISTS "Users can create their own profile" ON user_profiles;
DROP POLICY IF EXISTS "Users can update their own profile" ON user_profiles;
DROP POLICY IF EXISTS "Users can delete their own profile" ON user_profiles;

-- Create more permissive policies for authenticated users
CREATE POLICY "Enable read for authenticated users"
ON user_profiles FOR SELECT
TO authenticated
USING (true);

CREATE POLICY "Enable insert for authenticated users"
ON user_profiles FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = id);

CREATE POLICY "Enable update for authenticated users"
ON user_profiles FOR UPDATE
TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

CREATE POLICY "Enable delete for own profile"
ON user_profiles FOR DELETE
TO authenticated
USING (auth.uid() = id);

-- Alternative: If above doesn't work, temporarily disable RLS for testing
-- (Don't use this in production!)
-- ALTER TABLE user_profiles DISABLE ROW LEVEL SECURITY;

-- Verify the policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd
FROM pg_policies
WHERE tablename = 'user_profiles';

-- Test: Try to see if you can query your profile
-- SELECT * FROM user_profiles WHERE id = auth.uid();