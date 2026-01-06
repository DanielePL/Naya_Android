-- =====================================================
-- PROMETHEUS MEAL PHOTOS STORAGE BUCKET & POLICIES
-- =====================================================
-- Storage for AI-analyzed meal photos (Nutrition Tracker)
-- Run this in Supabase SQL Editor

-- =====================================================
-- 1. CREATE STORAGE BUCKET
-- =====================================================

-- Create bucket for meal photos
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'meal-photos',
    'meal-photos',
    true,  -- Public bucket (photos can be viewed without auth)
    5242880,  -- 5 MB limit per photo
    ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE SET
    public = true,
    file_size_limit = 5242880,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp'];

-- =====================================================
-- 2. STORAGE POLICIES (RLS)
-- =====================================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "users_can_upload_meal_photos" ON storage.objects;
DROP POLICY IF EXISTS "users_can_read_own_meal_photos" ON storage.objects;
DROP POLICY IF EXISTS "users_can_update_own_meal_photos" ON storage.objects;
DROP POLICY IF EXISTS "users_can_delete_own_meal_photos" ON storage.objects;
DROP POLICY IF EXISTS "public_can_read_meal_photos" ON storage.objects;

-- Policy: Authenticated users can upload their own meal photos
-- Path format: <user_id>/meal_<user_id>_<timestamp>.jpg
CREATE POLICY "users_can_upload_meal_photos"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'meal-photos'
    AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Policy: Anyone can read meal photos (public bucket)
CREATE POLICY "public_can_read_meal_photos"
ON storage.objects FOR SELECT
TO public
USING (
    bucket_id = 'meal-photos'
);

-- Policy: Users can update their own meal photos
CREATE POLICY "users_can_update_own_meal_photos"
ON storage.objects FOR UPDATE
TO authenticated
USING (
    bucket_id = 'meal-photos'
    AND (storage.foldername(name))[1] = auth.uid()::text
)
WITH CHECK (
    bucket_id = 'meal-photos'
    AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Policy: Users can delete their own meal photos
CREATE POLICY "users_can_delete_own_meal_photos"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'meal-photos'
    AND (storage.foldername(name))[1] = auth.uid()::text
);

-- =====================================================
-- 3. VERIFICATION QUERIES (run after setup)
-- =====================================================

-- Check if bucket exists:
-- SELECT * FROM storage.buckets WHERE id = 'meal-photos';

-- Check policies:
-- SELECT polname, polcmd, pg_get_expr(polqual, polrelid) AS using, pg_get_expr(polwithcheck, polrelid) AS with_check
-- FROM pg_policy
-- WHERE polrelid = 'storage.objects'::regclass
-- AND polname LIKE '%meal%';

-- =====================================================
-- NOTES
-- =====================================================
--
-- Upload path: <user_id>/meal_<user_id>_<timestamp>.jpg
--
-- The policy uses (storage.foldername(name))[1] to extract
-- the first folder from the path, which must match auth.uid()
--
-- Example: If user_id is "abc-123", valid path is:
--   abc-123/meal_abc-123_1234567890.jpg
--