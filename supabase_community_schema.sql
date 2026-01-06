-- ═══════════════════════════════════════════════════════════════════════════
-- PROMETHEUS COMMUNITY FEATURE - DATABASE SCHEMA
-- Version: 1.0
-- Date: 2024-12-09
--
-- This migration creates all tables for the Community feature.
-- All tables are NEW - no modifications to existing tables.
-- ═══════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. COMMUNITY PROFILES (extends user_profiles without modifying)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_profiles (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    is_public BOOLEAN DEFAULT true,
    display_name TEXT,
    bio TEXT,
    show_in_leaderboard BOOLEAN DEFAULT true,
    allow_follow_requests BOOLEAN DEFAULT true,
    auto_share_workouts BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE community_profiles IS 'Community-specific profile settings, extends user_profiles';

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. FOLLOW RELATIONSHIPS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'pending', 'blocked')),
    created_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT unique_follow UNIQUE(follower_id, following_id),
    CONSTRAINT no_self_follow CHECK (follower_id != following_id)
);

CREATE INDEX IF NOT EXISTS idx_follows_follower ON community_follows(follower_id) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_follows_following ON community_follows(following_id) WHERE status = 'active';

COMMENT ON TABLE community_follows IS 'User follow relationships';

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. COMMUNITY POSTS (Shared Workouts)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    workout_history_id UUID REFERENCES workout_history(id) ON DELETE SET NULL,

    -- Denormalized workout data for fast feed loading
    workout_name TEXT NOT NULL,
    total_volume_kg DOUBLE PRECISION DEFAULT 0,
    total_sets INTEGER DEFAULT 0,
    total_reps INTEGER DEFAULT 0,
    duration_minutes INTEGER,
    prs_achieved INTEGER DEFAULT 0,
    pr_exercises TEXT[],  -- Array of exercise names with PRs

    -- Post content
    caption TEXT,
    visibility TEXT DEFAULT 'followers' CHECK (visibility IN ('public', 'followers', 'private')),

    -- Engagement counts (denormalized for performance)
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_posts_user ON community_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_created ON community_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_visibility ON community_posts(visibility);
CREATE INDEX IF NOT EXISTS idx_posts_workout ON community_posts(workout_history_id);

COMMENT ON TABLE community_posts IS 'Shared workout posts for the community feed';

-- ═══════════════════════════════════════════════════════════════════════════
-- 4. LIKES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT unique_like UNIQUE(post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_likes_post ON community_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_likes_user ON community_likes(user_id);

COMMENT ON TABLE community_likes IS 'Likes on community posts';

-- ═══════════════════════════════════════════════════════════════════════════
-- 5. COMMENTS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    parent_comment_id UUID REFERENCES community_comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK (length(content) > 0 AND length(content) <= 1000),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comments_post ON community_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_user ON community_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent ON community_comments(parent_comment_id);

COMMENT ON TABLE community_comments IS 'Comments on community posts';

-- ═══════════════════════════════════════════════════════════════════════════
-- 6. LEADERBOARD (Denormalized for fast queries)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_leaderboard (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    exercise_id TEXT NOT NULL,
    exercise_name TEXT NOT NULL,

    -- PR Data
    pr_weight_kg DOUBLE PRECISION,
    pr_reps INTEGER,
    estimated_1rm_kg DOUBLE PRECISION,

    -- For relative strength (future)
    user_bodyweight_kg DOUBLE PRECISION,
    wilks_score DOUBLE PRECISION,
    dots_score DOUBLE PRECISION,

    -- Timestamps
    achieved_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT unique_leaderboard_entry UNIQUE(user_id, exercise_id)
);

CREATE INDEX IF NOT EXISTS idx_leaderboard_exercise ON community_leaderboard(exercise_id);
CREATE INDEX IF NOT EXISTS idx_leaderboard_1rm ON community_leaderboard(exercise_id, estimated_1rm_kg DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_leaderboard_weight ON community_leaderboard(exercise_id, pr_weight_kg DESC NULLS LAST);

COMMENT ON TABLE community_leaderboard IS 'Denormalized leaderboard entries for fast ranking queries';

-- ═══════════════════════════════════════════════════════════════════════════
-- 7. CHALLENGES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    challenge_type TEXT NOT NULL CHECK (challenge_type IN ('max_out_friday', 'volume', 'streak', 'custom')),

    -- For Max Out Friday
    exercise_id TEXT,
    exercise_name TEXT,

    -- For Volume challenges
    target_volume_kg DOUBLE PRECISION,

    -- For Streak challenges
    target_streak_days INTEGER,

    -- Timing
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_recurring BOOLEAN DEFAULT false,
    recurrence_pattern TEXT,  -- 'weekly_friday', 'monthly', etc.

    -- Status
    status TEXT DEFAULT 'active' CHECK (status IN ('upcoming', 'active', 'completed', 'cancelled')),

    -- Stats (denormalized)
    participants_count INTEGER DEFAULT 0,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by UUID REFERENCES auth.users(id)
);

CREATE INDEX IF NOT EXISTS idx_challenges_status ON community_challenges(status, start_date);
CREATE INDEX IF NOT EXISTS idx_challenges_type ON community_challenges(challenge_type);
CREATE INDEX IF NOT EXISTS idx_challenges_dates ON community_challenges(start_date, end_date);

COMMENT ON TABLE community_challenges IS 'Community challenges including Max Out Friday';

-- ═══════════════════════════════════════════════════════════════════════════
-- 8. CHALLENGE ENTRIES (Participants)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_challenge_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID NOT NULL REFERENCES community_challenges(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Entry data (depends on challenge type)
    value_kg DOUBLE PRECISION,      -- For max/volume challenges
    value_reps INTEGER,             -- For rep challenges
    streak_count INTEGER,           -- For streak challenges

    -- Proof
    workout_history_id UUID REFERENCES workout_history(id),
    video_url TEXT,

    -- Status
    rank INTEGER,                   -- Calculated position
    is_verified BOOLEAN DEFAULT false,
    is_pr BOOLEAN DEFAULT false,    -- Was this a PR?

    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT unique_challenge_entry UNIQUE(challenge_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_challenge_entries_challenge ON community_challenge_entries(challenge_id);
CREATE INDEX IF NOT EXISTS idx_challenge_entries_user ON community_challenge_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_challenge_entries_rank ON community_challenge_entries(challenge_id, rank);

COMMENT ON TABLE community_challenge_entries IS 'User participation in challenges';

-- ═══════════════════════════════════════════════════════════════════════════
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE community_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_leaderboard ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_challenge_entries ENABLE ROW LEVEL SECURITY;

-- Drop existing policies first to avoid conflicts
DROP POLICY IF EXISTS "Users can view public profiles or own profile" ON community_profiles;
DROP POLICY IF EXISTS "Users can manage own profile" ON community_profiles;
DROP POLICY IF EXISTS "Users can manage own follows" ON community_follows;
DROP POLICY IF EXISTS "Users can see follows involving them" ON community_follows;
DROP POLICY IF EXISTS "Public posts visible to all authenticated users" ON community_posts;
DROP POLICY IF EXISTS "Users can manage own posts" ON community_posts;
DROP POLICY IF EXISTS "Users can see all likes" ON community_likes;
DROP POLICY IF EXISTS "Users can manage own likes" ON community_likes;
DROP POLICY IF EXISTS "Users can see comments on visible posts" ON community_comments;
DROP POLICY IF EXISTS "Users can manage own comments" ON community_comments;
DROP POLICY IF EXISTS "Leaderboard visible for opted-in users" ON community_leaderboard;
DROP POLICY IF EXISTS "Users can manage own leaderboard entries" ON community_leaderboard;
DROP POLICY IF EXISTS "All authenticated users can view challenges" ON community_challenges;
DROP POLICY IF EXISTS "Users can view challenge entries" ON community_challenge_entries;
DROP POLICY IF EXISTS "Users can manage own challenge entries" ON community_challenge_entries;

-- Community Profiles
CREATE POLICY "Users can view public profiles or own profile" ON community_profiles
    FOR SELECT USING (is_public = true OR user_id = auth.uid());

CREATE POLICY "Users can manage own profile" ON community_profiles
    FOR ALL USING (user_id = auth.uid());

-- Follows
CREATE POLICY "Users can manage own follows" ON community_follows
    FOR ALL USING (follower_id = auth.uid());

CREATE POLICY "Users can see follows involving them" ON community_follows
    FOR SELECT USING (following_id = auth.uid() OR follower_id = auth.uid());

-- Posts (visibility-based)
CREATE POLICY "Public posts visible to all authenticated users" ON community_posts
    FOR SELECT USING (
        auth.uid() IS NOT NULL AND (
            visibility = 'public'
            OR user_id = auth.uid()
            OR (visibility = 'followers' AND EXISTS (
                SELECT 1 FROM community_follows
                WHERE follower_id = auth.uid()
                AND following_id = community_posts.user_id
                AND status = 'active'
            ))
        )
    );

CREATE POLICY "Users can manage own posts" ON community_posts
    FOR ALL USING (user_id = auth.uid());

-- Likes
CREATE POLICY "Users can see all likes" ON community_likes
    FOR SELECT USING (auth.uid() IS NOT NULL);

CREATE POLICY "Users can manage own likes" ON community_likes
    FOR ALL USING (user_id = auth.uid());

-- Comments
CREATE POLICY "Users can see comments on visible posts" ON community_comments
    FOR SELECT USING (
        auth.uid() IS NOT NULL AND EXISTS (
            SELECT 1 FROM community_posts
            WHERE id = community_comments.post_id
        )
    );

CREATE POLICY "Users can manage own comments" ON community_comments
    FOR ALL USING (user_id = auth.uid());

-- Leaderboard (only if user opted in)
CREATE POLICY "Leaderboard visible for opted-in users" ON community_leaderboard
    FOR SELECT USING (
        auth.uid() IS NOT NULL AND (
            user_id = auth.uid() OR
            EXISTS (
                SELECT 1 FROM community_profiles
                WHERE user_id = community_leaderboard.user_id
                AND show_in_leaderboard = true
            )
        )
    );

CREATE POLICY "Users can manage own leaderboard entries" ON community_leaderboard
    FOR ALL USING (user_id = auth.uid());

-- Challenges (public)
CREATE POLICY "All authenticated users can view challenges" ON community_challenges
    FOR SELECT USING (auth.uid() IS NOT NULL);

-- Challenge Entries
CREATE POLICY "Users can view challenge entries" ON community_challenge_entries
    FOR SELECT USING (auth.uid() IS NOT NULL);

CREATE POLICY "Users can manage own challenge entries" ON community_challenge_entries
    FOR ALL USING (user_id = auth.uid());

-- ═══════════════════════════════════════════════════════════════════════════
-- TRIGGER FUNCTIONS
-- ═══════════════════════════════════════════════════════════════════════════

-- Auto-update likes_count on community_posts
CREATE OR REPLACE FUNCTION update_post_likes_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE community_posts SET likes_count = likes_count + 1 WHERE id = NEW.post_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE community_posts SET likes_count = likes_count - 1 WHERE id = OLD.post_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_likes_count ON community_likes;
CREATE TRIGGER trigger_update_likes_count
    AFTER INSERT OR DELETE ON community_likes
    FOR EACH ROW EXECUTE FUNCTION update_post_likes_count();

-- Auto-update comments_count on community_posts
CREATE OR REPLACE FUNCTION update_post_comments_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE community_posts SET comments_count = comments_count + 1 WHERE id = NEW.post_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE community_posts SET comments_count = comments_count - 1 WHERE id = OLD.post_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_comments_count ON community_comments;
CREATE TRIGGER trigger_update_comments_count
    AFTER INSERT OR DELETE ON community_comments
    FOR EACH ROW EXECUTE FUNCTION update_post_comments_count();

-- Auto-update participants_count on community_challenges
CREATE OR REPLACE FUNCTION update_challenge_participants_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE community_challenges SET participants_count = participants_count + 1 WHERE id = NEW.challenge_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE community_challenges SET participants_count = participants_count - 1 WHERE id = OLD.challenge_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_participants_count ON community_challenge_entries;
CREATE TRIGGER trigger_update_participants_count
    AFTER INSERT OR DELETE ON community_challenge_entries
    FOR EACH ROW EXECUTE FUNCTION update_challenge_participants_count();

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_community_profiles_updated ON community_profiles;
CREATE TRIGGER trigger_community_profiles_updated
    BEFORE UPDATE ON community_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trigger_community_posts_updated ON community_posts;
CREATE TRIGGER trigger_community_posts_updated
    BEFORE UPDATE ON community_posts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS trigger_community_comments_updated ON community_comments;
CREATE TRIGGER trigger_community_comments_updated
    BEFORE UPDATE ON community_comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ═══════════════════════════════════════════════════════════════════════════
-- HELPER FUNCTIONS (RPC)
-- ═══════════════════════════════════════════════════════════════════════════

-- Get feed for current user (following + own posts)
CREATE OR REPLACE FUNCTION get_community_feed(
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    user_id UUID,
    user_name TEXT,
    user_avatar TEXT,
    workout_name TEXT,
    total_volume_kg DOUBLE PRECISION,
    total_sets INTEGER,
    total_reps INTEGER,
    duration_minutes INTEGER,
    prs_achieved INTEGER,
    pr_exercises TEXT[],
    caption TEXT,
    likes_count INTEGER,
    comments_count INTEGER,
    created_at TIMESTAMPTZ,
    is_liked BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id,
        p.user_id,
        up.name as user_name,
        up.profile_image_url as user_avatar,
        p.workout_name,
        p.total_volume_kg,
        p.total_sets,
        p.total_reps,
        p.duration_minutes,
        p.prs_achieved,
        p.pr_exercises,
        p.caption,
        p.likes_count,
        p.comments_count,
        p.created_at,
        EXISTS (
            SELECT 1 FROM community_likes l
            WHERE l.post_id = p.id AND l.user_id = auth.uid()
        ) as is_liked
    FROM community_posts p
    JOIN user_profiles up ON p.user_id = up.id
    WHERE
        p.user_id = auth.uid()
        OR (p.visibility = 'public')
        OR (p.visibility = 'followers' AND EXISTS (
            SELECT 1 FROM community_follows cf
            WHERE cf.follower_id = auth.uid()
            AND cf.following_id = p.user_id
            AND cf.status = 'active'
        ))
    ORDER BY p.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Get discover feed (public posts from non-followed users)
CREATE OR REPLACE FUNCTION get_discover_feed(
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    user_id UUID,
    user_name TEXT,
    user_avatar TEXT,
    workout_name TEXT,
    total_volume_kg DOUBLE PRECISION,
    total_sets INTEGER,
    total_reps INTEGER,
    duration_minutes INTEGER,
    prs_achieved INTEGER,
    pr_exercises TEXT[],
    caption TEXT,
    likes_count INTEGER,
    comments_count INTEGER,
    created_at TIMESTAMPTZ,
    is_liked BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.id,
        p.user_id,
        up.name as user_name,
        up.profile_image_url as user_avatar,
        p.workout_name,
        p.total_volume_kg,
        p.total_sets,
        p.total_reps,
        p.duration_minutes,
        p.prs_achieved,
        p.pr_exercises,
        p.caption,
        p.likes_count,
        p.comments_count,
        p.created_at,
        EXISTS (
            SELECT 1 FROM community_likes l
            WHERE l.post_id = p.id AND l.user_id = auth.uid()
        ) as is_liked
    FROM community_posts p
    JOIN user_profiles up ON p.user_id = up.id
    WHERE
        p.visibility = 'public'
        AND p.user_id != auth.uid()
        AND NOT EXISTS (
            SELECT 1 FROM community_follows cf
            WHERE cf.follower_id = auth.uid()
            AND cf.following_id = p.user_id
            AND cf.status = 'active'
        )
    ORDER BY p.likes_count DESC, p.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Get leaderboard for exercise
CREATE OR REPLACE FUNCTION get_exercise_leaderboard(
    p_exercise_id TEXT,
    p_limit INTEGER DEFAULT 50,
    p_friends_only BOOLEAN DEFAULT false
)
RETURNS TABLE (
    rank BIGINT,
    user_id UUID,
    user_name TEXT,
    user_avatar TEXT,
    pr_weight_kg DOUBLE PRECISION,
    estimated_1rm_kg DOUBLE PRECISION,
    achieved_at TIMESTAMPTZ,
    is_current_user BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        ROW_NUMBER() OVER (ORDER BY l.estimated_1rm_kg DESC NULLS LAST) as rank,
        l.user_id,
        up.name as user_name,
        up.profile_image_url as user_avatar,
        l.pr_weight_kg,
        l.estimated_1rm_kg,
        l.achieved_at,
        l.user_id = auth.uid() as is_current_user
    FROM community_leaderboard l
    JOIN user_profiles up ON l.user_id = up.id
    JOIN community_profiles cp ON l.user_id = cp.user_id
    WHERE
        l.exercise_id = p_exercise_id
        AND cp.show_in_leaderboard = true
        AND (
            NOT p_friends_only
            OR l.user_id = auth.uid()
            OR EXISTS (
                SELECT 1 FROM community_follows cf
                WHERE cf.follower_id = auth.uid()
                AND cf.following_id = l.user_id
                AND cf.status = 'active'
            )
        )
    ORDER BY l.estimated_1rm_kg DESC NULLS LAST
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Get current Max Out Friday challenge
CREATE OR REPLACE FUNCTION get_current_max_out_friday()
RETURNS TABLE (
    id UUID,
    title TEXT,
    exercise_id TEXT,
    exercise_name TEXT,
    start_date DATE,
    end_date DATE,
    participants_count INTEGER,
    user_entry_kg DOUBLE PRECISION,
    user_rank INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.title,
        c.exercise_id,
        c.exercise_name,
        c.start_date,
        c.end_date,
        c.participants_count,
        ce.value_kg as user_entry_kg,
        ce.rank as user_rank
    FROM community_challenges c
    LEFT JOIN community_challenge_entries ce ON c.id = ce.challenge_id AND ce.user_id = auth.uid()
    WHERE
        c.challenge_type = 'max_out_friday'
        AND c.status = 'active'
        AND CURRENT_DATE BETWEEN c.start_date AND c.end_date
    LIMIT 1;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════
-- SEED DATA: Create first Max Out Friday challenge
-- ═══════════════════════════════════════════════════════════════════════════

-- Insert initial Max Out Friday (Bench Press)
INSERT INTO community_challenges (
    title,
    description,
    challenge_type,
    exercise_id,
    exercise_name,
    start_date,
    end_date,
    is_recurring,
    recurrence_pattern,
    status
) VALUES (
    'Max Out Friday: Bench Press',
    'Show us your best bench press! Post your heaviest lift this week.',
    'max_out_friday',
    'bench_press',
    'Bench Press',
    date_trunc('week', CURRENT_DATE)::date + 4,  -- This Friday
    date_trunc('week', CURRENT_DATE)::date + 6,  -- Until Sunday
    true,
    'weekly_friday',
    'active'
) ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════
-- DONE
-- ═══════════════════════════════════════════════════════════════════════════

COMMENT ON SCHEMA public IS 'Community feature tables added: community_profiles, community_follows, community_posts, community_likes, community_comments, community_leaderboard, community_challenges, community_challenge_entries';
