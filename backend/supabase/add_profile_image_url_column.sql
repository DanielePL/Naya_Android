-- ═══════════════════════════════════════════════════════════════
-- ADD MISSING COLUMN: profile_image_url to user_profiles
-- Run this in Supabase SQL Editor
-- ═══════════════════════════════════════════════════════════════

-- Add the profile_image_url column if it doesn't exist
ALTER TABLE public.user_profiles
ADD COLUMN IF NOT EXISTS profile_image_url TEXT;

-- Verify the column was added
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'user_profiles'
AND column_name = 'profile_image_url';