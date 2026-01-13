-- Migration: Add video_url to workout_templates
-- Run this in your Supabase SQL Editor

-- Step 1: Add video_url column to workout_templates table
ALTER TABLE workout_templates
ADD COLUMN IF NOT EXISTS video_url TEXT DEFAULT NULL;

-- Step 2: Add index for faster queries on templates with videos
CREATE INDEX IF NOT EXISTS idx_workout_templates_video_url
ON workout_templates(video_url)
WHERE video_url IS NOT NULL;

-- Step 3: Create storage bucket for workout template videos (if not exists)
-- Note: Run this in Supabase Dashboard > Storage > Create Bucket
-- Bucket name: workout-templates
-- Public bucket: YES (videos need to be accessible to all users)

-- Step 4: Storage policy for workout-templates bucket (run in SQL editor)
-- Allow public read access
INSERT INTO storage.policies (name, bucket_id, definition, check_expression)
SELECT
    'Public read access',
    'workout-templates',
    '{"condition": "true"}'::jsonb,
    '{"condition": "true"}'::jsonb
WHERE NOT EXISTS (
    SELECT 1 FROM storage.policies
    WHERE bucket_id = 'workout-templates' AND name = 'Public read access'
);

-- Allow authenticated users to upload (admin check done in app)
INSERT INTO storage.policies (name, bucket_id, definition, check_expression, operation)
SELECT
    'Authenticated upload',
    'workout-templates',
    '{"condition": "auth.role() = ''authenticated''"}'::jsonb,
    '{"condition": "true"}'::jsonb,
    'INSERT'
WHERE NOT EXISTS (
    SELECT 1 FROM storage.policies
    WHERE bucket_id = 'workout-templates' AND name = 'Authenticated upload'
);

-- =====================================================
-- IMPORTANT: Manual steps in Supabase Dashboard
-- =====================================================
-- 1. Go to Storage in Supabase Dashboard
-- 2. Click "New Bucket"
-- 3. Name: workout-templates
-- 4. Check "Public bucket" (required for video playback)
-- 5. Save

-- Verify the column was added:
-- SELECT column_name, data_type FROM information_schema.columns
-- WHERE table_name = 'workout_templates' AND column_name = 'video_url';
