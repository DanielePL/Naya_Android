-- ═══════════════════════════════════════════════════════════════
-- COMPLETE FIX: Profile Image Upload
-- Führe dieses Script KOMPLETT in Supabase SQL Editor aus
-- ═══════════════════════════════════════════════════════════════

-- STEP 1: Add missing column to user_profiles table
-- ----------------------------------------------------------------
ALTER TABLE public.user_profiles
ADD COLUMN IF NOT EXISTS profile_image_url TEXT;

-- Verify column was added
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'user_profiles'
AND column_name = 'profile_image_url';


-- STEP 2: Create profile-images bucket (if not exists)
-- ----------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public)
VALUES ('profile-images', 'profile-images', true)
ON CONFLICT (id) DO NOTHING;


-- STEP 3: Drop ALL existing policies (clean slate)
-- ----------------------------------------------------------------
DROP POLICY IF EXISTS "Public Access" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can upload profile images" ON storage.objects;
DROP POLICY IF EXISTS "Users can update own profile image" ON storage.objects;
DROP POLICY IF EXISTS "Users can delete own profile image" ON storage.objects;
DROP POLICY IF EXISTS "profile-images-public-read" ON storage.objects;
DROP POLICY IF EXISTS "profile-images-auth-insert" ON storage.objects;
DROP POLICY IF EXISTS "profile-images-auth-update" ON storage.objects;
DROP POLICY IF EXISTS "profile-images-auth-delete" ON storage.objects;


-- STEP 4: Create SIMPLE policies (no filename checks)
-- ----------------------------------------------------------------

-- Allow EVERYONE to read profile images (public bucket)
CREATE POLICY "profile-images-public-read"
ON storage.objects FOR SELECT
TO public
USING ( bucket_id = 'profile-images' );

-- Allow ANY authenticated user to INSERT files
CREATE POLICY "profile-images-auth-insert"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK ( bucket_id = 'profile-images' );

-- Allow ANY authenticated user to UPDATE files
CREATE POLICY "profile-images-auth-update"
ON storage.objects FOR UPDATE
TO authenticated
USING ( bucket_id = 'profile-images' );

-- Allow ANY authenticated user to DELETE files
CREATE POLICY "profile-images-auth-delete"
ON storage.objects FOR DELETE
TO authenticated
USING ( bucket_id = 'profile-images' );


-- STEP 5: Verify everything is set up correctly
-- ----------------------------------------------------------------

-- Check bucket exists
SELECT id, name, public
FROM storage.buckets
WHERE id = 'profile-images';

-- Check all policies are active
SELECT schemaname, tablename, policyname, permissive, roles, cmd
FROM pg_policies
WHERE tablename = 'objects'
AND policyname LIKE 'profile-images%'
ORDER BY policyname;

-- Check user_profiles has the new column
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'user_profiles'
AND column_name = 'profile_image_url';