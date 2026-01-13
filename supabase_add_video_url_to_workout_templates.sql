-- Migration: Add video_url to workout_templates
-- Run this in your Supabase SQL Editor

-- =====================================================
-- STEP 1: Add video_url column to workout_templates table
-- =====================================================
ALTER TABLE workout_templates
ADD COLUMN IF NOT EXISTS video_url TEXT DEFAULT NULL;

-- =====================================================
-- STEP 2: Add index for faster queries on templates with videos
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_workout_templates_video_url
ON workout_templates(video_url)
WHERE video_url IS NOT NULL;

-- =====================================================
-- STEP 3: Create storage bucket (MANUAL - in Supabase Dashboard)
-- =====================================================
-- 1. Go to Storage in Supabase Dashboard
-- 2. Click "New Bucket"
-- 3. Name: workout-templates
-- 4. Check "Public bucket" (required for video playback)
-- 5. Save

-- =====================================================
-- STEP 4: Storage policies (run AFTER creating bucket)
-- =====================================================
-- These policies allow:
-- - Anyone can READ (public videos for workout previews)
-- - Authenticated users can UPLOAD (admin check in app code)

-- Policy: Allow public read access to all files
CREATE POLICY "Public read access"
ON storage.objects FOR SELECT
USING (bucket_id = 'workout-templates');

-- Policy: Allow authenticated users to upload
CREATE POLICY "Authenticated users can upload"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'workout-templates');

-- Policy: Allow authenticated users to update their uploads
CREATE POLICY "Authenticated users can update"
ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'workout-templates');

-- Policy: Allow authenticated users to delete
CREATE POLICY "Authenticated users can delete"
ON storage.objects FOR DELETE
TO authenticated
USING (bucket_id = 'workout-templates');

-- =====================================================
-- Verify the column was added:
-- =====================================================
-- SELECT column_name, data_type FROM information_schema.columns
-- WHERE table_name = 'workout_templates' AND column_name = 'video_url';
