-- ═══════════════════════════════════════════════════════════════════════════
-- COMMUNITY POSTS MEDIA MIGRATION
-- Adds image_urls and video_urls columns for photo/video posts
-- ═══════════════════════════════════════════════════════════════════════════

-- Add image_urls column (array of image URLs from Supabase Storage)
ALTER TABLE community_posts
ADD COLUMN IF NOT EXISTS image_urls TEXT[] DEFAULT '{}';

-- Add video_urls column (array of video URLs from Supabase Storage)
ALTER TABLE community_posts
ADD COLUMN IF NOT EXISTS video_urls TEXT[] DEFAULT '{}';

-- Add comments for documentation
COMMENT ON COLUMN community_posts.image_urls IS 'Array of image URLs for post media (stored in Supabase Storage)';
COMMENT ON COLUMN community_posts.video_urls IS 'Array of video URLs for post media (stored in Supabase Storage)';

-- ═══════════════════════════════════════════════════════════════════════════
-- STORAGE BUCKET FOR POST MEDIA
-- ═══════════════════════════════════════════════════════════════════════════

-- Create storage bucket for post media (if not exists)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'post-media',
    'post-media',
    true,  -- Public bucket so images/videos can be viewed
    52428800,  -- 50MB max file size
    ARRAY['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'video/mp4', 'video/quicktime', 'video/webm']
)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════
-- STORAGE POLICIES
-- ═══════════════════════════════════════════════════════════════════════════

-- Policy: Anyone can view post media (public bucket)
DROP POLICY IF EXISTS "Public can view post media" ON storage.objects;
CREATE POLICY "Public can view post media" ON storage.objects
    FOR SELECT
    USING (bucket_id = 'post-media');

-- Policy: Authenticated users can upload their own media
DROP POLICY IF EXISTS "Users can upload post media" ON storage.objects;
CREATE POLICY "Users can upload post media" ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'post-media'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Policy: Users can update their own media
DROP POLICY IF EXISTS "Users can update own post media" ON storage.objects;
CREATE POLICY "Users can update own post media" ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'post-media'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Policy: Users can delete their own media
DROP POLICY IF EXISTS "Users can delete own post media" ON storage.objects;
CREATE POLICY "Users can delete own post media" ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'post-media'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- ═══════════════════════════════════════════════════════════════════════════
-- VERIFICATION QUERY
-- ═══════════════════════════════════════════════════════════════════════════

-- Run this to verify the columns were added:
-- SELECT column_name, data_type, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'community_posts'
-- AND column_name IN ('image_urls', 'video_urls');
