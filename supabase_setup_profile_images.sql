-- ═══════════════════════════════════════════════════════════════
-- SUPABASE STORAGE: PROFILE IMAGES BUCKET SETUP
-- Führe dieses Script in der Supabase SQL Editor aus
-- ═══════════════════════════════════════════════════════════════

-- 1. Create profile-images bucket (if it doesn't exist)
INSERT INTO storage.buckets (id, name, public)
VALUES ('profile-images', 'profile-images', true)
ON CONFLICT (id) DO NOTHING;

-- 1.5. Drop existing policies (if re-running this script)
DROP POLICY IF EXISTS "Public Access" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can upload profile images" ON storage.objects;
DROP POLICY IF EXISTS "Users can update own profile image" ON storage.objects;
DROP POLICY IF EXISTS "Users can delete own profile image" ON storage.objects;

-- 2. Allow public READ access (anyone can view profile images)
CREATE POLICY "Public Access"
ON storage.objects FOR SELECT
USING ( bucket_id = 'profile-images' );

-- 3. Allow authenticated users to UPLOAD their own profile image
CREATE POLICY "Authenticated users can upload profile images"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'profile-images'
    AND name LIKE 'profile_' || auth.uid()::text || '%'
);

-- 4. Allow authenticated users to UPDATE their own profile image
CREATE POLICY "Users can update own profile image"
ON storage.objects FOR UPDATE
TO authenticated
USING (
    bucket_id = 'profile-images'
    AND name LIKE 'profile_' || auth.uid()::text || '%'
);

-- 5. Allow authenticated users to DELETE their own profile image
CREATE POLICY "Users can delete own profile image"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'profile-images'
    AND name LIKE 'profile_' || auth.uid()::text || '%'
);

-- ═══════════════════════════════════════════════════════════════
-- VERIFY SETUP
-- ═══════════════════════════════════════════════════════════════

-- Check if bucket was created
SELECT * FROM storage.buckets WHERE id = 'profile-images';

-- Check policies
SELECT * FROM pg_policies WHERE tablename = 'objects' AND policyname LIKE '%profile%';