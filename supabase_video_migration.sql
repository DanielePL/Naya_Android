-- ═══════════════════════════════════════════════════════════════════════════════
-- VIDEO SUPPORT FOR COMMUNITY POSTS
-- ═══════════════════════════════════════════════════════════════════════════════

-- 1. Add video_urls column to community_posts
ALTER TABLE community_posts
ADD COLUMN IF NOT EXISTS video_urls TEXT[];

-- 2. Create community-videos storage bucket (if not exists)
-- Run in Supabase Dashboard > Storage > New Bucket:
-- Name: community-videos
-- Public: true (for easy video playback)

-- 3. Drop existing functions first (return type changed)
DROP FUNCTION IF EXISTS get_community_feed(INT, INT);
DROP FUNCTION IF EXISTS get_discover_feed(INT, INT);

-- 4. Recreate get_community_feed function with video_urls
CREATE OR REPLACE FUNCTION get_community_feed(p_limit INT DEFAULT 20, p_offset INT DEFAULT 0)
RETURNS TABLE (
    id UUID,
    user_id UUID,
    user_name TEXT,
    user_avatar TEXT,
    workout_name TEXT,
    total_volume_kg DOUBLE PRECISION,
    total_sets INT,
    total_reps INT,
    duration_minutes INT,
    prs_achieved INT,
    pr_exercises TEXT[],
    caption TEXT,
    video_urls TEXT[],
    likes_count INT,
    comments_count INT,
    created_at TIMESTAMPTZ,
    is_liked BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id,
        p.user_id,
        u.name AS user_name,
        u.profile_image_url AS user_avatar,
        p.workout_name,
        p.total_volume_kg,
        p.total_sets,
        p.total_reps,
        p.duration_minutes,
        p.prs_achieved,
        p.pr_exercises,
        p.caption,
        p.video_urls,
        p.likes_count,
        p.comments_count,
        p.created_at,
        EXISTS(
            SELECT 1 FROM community_likes l
            WHERE l.post_id = p.id AND l.user_id = auth.uid()
        ) AS is_liked
    FROM community_posts p
    JOIN user_profiles u ON p.user_id = u.id
    WHERE
        p.visibility IN ('public', 'followers')
        AND (
            p.user_id = auth.uid()
            OR EXISTS(
                SELECT 1 FROM community_follows f
                WHERE f.follower_id = auth.uid()
                AND f.following_id = p.user_id
                AND f.status = 'active'
            )
            OR p.visibility = 'public'
        )
    ORDER BY p.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Update get_discover_feed function to include video_urls
CREATE OR REPLACE FUNCTION get_discover_feed(p_limit INT DEFAULT 20, p_offset INT DEFAULT 0)
RETURNS TABLE (
    id UUID,
    user_id UUID,
    user_name TEXT,
    user_avatar TEXT,
    workout_name TEXT,
    total_volume_kg DOUBLE PRECISION,
    total_sets INT,
    total_reps INT,
    duration_minutes INT,
    prs_achieved INT,
    pr_exercises TEXT[],
    caption TEXT,
    video_urls TEXT[],
    likes_count INT,
    comments_count INT,
    created_at TIMESTAMPTZ,
    is_liked BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id,
        p.user_id,
        u.name AS user_name,
        u.profile_image_url AS user_avatar,
        p.workout_name,
        p.total_volume_kg,
        p.total_sets,
        p.total_reps,
        p.duration_minutes,
        p.prs_achieved,
        p.pr_exercises,
        p.caption,
        p.video_urls,
        p.likes_count,
        p.comments_count,
        p.created_at,
        EXISTS(
            SELECT 1 FROM community_likes l
            WHERE l.post_id = p.id AND l.user_id = auth.uid()
        ) AS is_liked
    FROM community_posts p
    JOIN user_profiles u ON p.user_id = u.id
    WHERE
        p.visibility = 'public'
        AND p.user_id != auth.uid()
        AND NOT EXISTS(
            SELECT 1 FROM community_follows f
            WHERE f.follower_id = auth.uid()
            AND f.following_id = p.user_id
            AND f.status = 'active'
        )
    ORDER BY
        p.likes_count DESC,
        p.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Storage policy for community-videos bucket
-- Run these after creating the bucket:

-- Allow authenticated users to upload videos to their own folder
-- INSERT policy: authenticated users can upload to posts/{user_id}/*
-- SELECT policy: anyone can read (public bucket)

-- Example policies (run in SQL editor after bucket creation):
/*
CREATE POLICY "Users can upload their own videos"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'community-videos'
    AND (storage.foldername(name))[1] = 'posts'
    AND (storage.foldername(name))[2] = auth.uid()::text
);

CREATE POLICY "Anyone can view community videos"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'community-videos');

CREATE POLICY "Users can delete their own videos"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'community-videos'
    AND (storage.foldername(name))[1] = 'posts'
    AND (storage.foldername(name))[2] = auth.uid()::text
);
*/

-- Done! Video support for community posts is now enabled.
