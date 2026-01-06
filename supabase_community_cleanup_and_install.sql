-- ═══════════════════════════════════════════════════════════════════════════
-- PROMETHEUS COMMUNITY - CLEAN INSTALL SCRIPT
-- Drops everything first, then creates fresh
-- ═══════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 1: DROP ALL FUNCTIONS FIRST
-- ═══════════════════════════════════════════════════════════════════════════

DROP FUNCTION IF EXISTS get_community_feed(INTEGER, INTEGER);
DROP FUNCTION IF EXISTS get_discover_feed(INTEGER, INTEGER);
DROP FUNCTION IF EXISTS get_exercise_leaderboard(TEXT, INTEGER, BOOLEAN);
DROP FUNCTION IF EXISTS get_current_max_out_friday();
DROP FUNCTION IF EXISTS award_xp(UUID, INTEGER, TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS check_and_award_badge(UUID, TEXT);
DROP FUNCTION IF EXISTS update_user_streak(UUID);
DROP FUNCTION IF EXISTS get_activity_feed(INTEGER, INTEGER);
DROP FUNCTION IF EXISTS get_user_gamification_stats(UUID);
DROP FUNCTION IF EXISTS create_next_max_out_friday();
DROP FUNCTION IF EXISTS get_max_out_friday_winners(INTEGER);

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 2: DROP TRIGGERS
-- ═══════════════════════════════════════════════════════════════════════════

DROP TRIGGER IF EXISTS trigger_update_likes_count ON community_likes;
DROP TRIGGER IF EXISTS trigger_update_comments_count ON community_comments;
DROP TRIGGER IF EXISTS trigger_update_participants_count ON community_challenge_entries;
DROP TRIGGER IF EXISTS trigger_community_profiles_updated ON community_profiles;
DROP TRIGGER IF EXISTS trigger_community_posts_updated ON community_posts;
DROP TRIGGER IF EXISTS trigger_community_comments_updated ON community_comments;

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 3: DROP TRIGGER FUNCTIONS (except shared ones)
-- ═══════════════════════════════════════════════════════════════════════════

DROP FUNCTION IF EXISTS update_post_likes_count();
DROP FUNCTION IF EXISTS update_post_comments_count();
DROP FUNCTION IF EXISTS update_challenge_participants_count();
-- NOTE: update_updated_at_column() is shared with other tables, don't drop it

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 4: CREATE TABLES (IF NOT EXISTS)
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

CREATE TABLE IF NOT EXISTS community_follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'pending', 'blocked')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_follow UNIQUE(follower_id, following_id),
    CONSTRAINT no_self_follow CHECK (follower_id != following_id)
);

CREATE TABLE IF NOT EXISTS community_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    workout_history_id UUID REFERENCES workout_history(id) ON DELETE SET NULL,
    workout_name TEXT NOT NULL,
    total_volume_kg DOUBLE PRECISION DEFAULT 0,
    total_sets INTEGER DEFAULT 0,
    total_reps INTEGER DEFAULT 0,
    duration_minutes INTEGER,
    prs_achieved INTEGER DEFAULT 0,
    pr_exercises TEXT[],
    caption TEXT,
    visibility TEXT DEFAULT 'followers' CHECK (visibility IN ('public', 'followers', 'private')),
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_like UNIQUE(post_id, user_id)
);

CREATE TABLE IF NOT EXISTS community_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    parent_comment_id UUID REFERENCES community_comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK (length(content) > 0 AND length(content) <= 1000),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_leaderboard (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    exercise_id TEXT NOT NULL,
    exercise_name TEXT NOT NULL,
    pr_weight_kg DOUBLE PRECISION,
    pr_reps INTEGER,
    estimated_1rm_kg DOUBLE PRECISION,
    user_bodyweight_kg DOUBLE PRECISION,
    wilks_score DOUBLE PRECISION,
    dots_score DOUBLE PRECISION,
    achieved_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_leaderboard_entry UNIQUE(user_id, exercise_id)
);

CREATE TABLE IF NOT EXISTS community_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    challenge_type TEXT NOT NULL CHECK (challenge_type IN ('max_out_friday', 'volume', 'streak', 'custom')),
    exercise_id TEXT,
    exercise_name TEXT,
    target_volume_kg DOUBLE PRECISION,
    target_streak_days INTEGER,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_recurring BOOLEAN DEFAULT false,
    recurrence_pattern TEXT,
    status TEXT DEFAULT 'active' CHECK (status IN ('upcoming', 'active', 'completed', 'cancelled')),
    participants_count INTEGER DEFAULT 0,
    is_public BOOLEAN DEFAULT true,
    max_participants INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by UUID REFERENCES auth.users(id)
);

CREATE TABLE IF NOT EXISTS community_challenge_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID NOT NULL REFERENCES community_challenges(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    value_kg DOUBLE PRECISION,
    value_reps INTEGER,
    streak_count INTEGER,
    workout_history_id UUID REFERENCES workout_history(id),
    video_url TEXT,
    rank INTEGER,
    is_verified BOOLEAN DEFAULT false,
    is_pr BOOLEAN DEFAULT false,
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_challenge_entry UNIQUE(challenge_id, user_id)
);

-- Gamification tables
CREATE TABLE IF NOT EXISTS community_badges (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('workout', 'strength', 'consistency', 'community', 'challenge', 'special')),
    rarity TEXT NOT NULL CHECK (rarity IN ('common', 'rare', 'epic', 'legendary')),
    xp_reward INTEGER DEFAULT 0,
    requirement_type TEXT,
    requirement_value INTEGER,
    is_hidden BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    badge_id TEXT NOT NULL REFERENCES community_badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_badge UNIQUE(user_id, badge_id)
);

CREATE TABLE IF NOT EXISTS community_user_levels (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    level INTEGER DEFAULT 1,
    current_xp INTEGER DEFAULT 0,
    total_xp INTEGER DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_xp_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    source TEXT NOT NULL,
    source_id TEXT,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_user_streaks (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    last_workout_date DATE,
    streak_protected_until DATE,
    freeze_tokens INTEGER DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_achievements (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    category TEXT NOT NULL,
    tiers JSONB NOT NULL,
    is_hidden BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_user_achievement_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    achievement_id TEXT NOT NULL REFERENCES community_achievements(id) ON DELETE CASCADE,
    current_value INTEGER DEFAULT 0,
    current_tier INTEGER DEFAULT 0,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_achievement UNIQUE(user_id, achievement_id)
);

CREATE TABLE IF NOT EXISTS community_activity_feed (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    activity_type TEXT NOT NULL CHECK (activity_type IN (
        'workout_shared', 'pr_achieved', 'challenge_joined', 'challenge_won',
        'badge_earned', 'level_up', 'follow', 'like', 'comment', 'streak_milestone'
    )),
    target_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    target_id TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS community_challenge_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID NOT NULL REFERENCES community_challenges(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    invitee_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_challenge_invite UNIQUE(challenge_id, invitee_id)
);

CREATE TABLE IF NOT EXISTS community_challenge_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id UUID NOT NULL REFERENCES community_challenge_entries(id) ON DELETE CASCADE,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    duration_seconds INTEGER,
    uploaded_at TIMESTAMPTZ DEFAULT NOW(),
    is_verified BOOLEAN DEFAULT false
);

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 5: CREATE INDEXES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_follows_follower ON community_follows(follower_id) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_follows_following ON community_follows(following_id) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_posts_user ON community_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_created ON community_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_likes_post ON community_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_post ON community_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_leaderboard_exercise ON community_leaderboard(exercise_id);
CREATE INDEX IF NOT EXISTS idx_challenges_status ON community_challenges(status, start_date);
CREATE INDEX IF NOT EXISTS idx_challenge_entries_challenge ON community_challenge_entries(challenge_id);
CREATE INDEX IF NOT EXISTS idx_user_badges_user ON community_user_badges(user_id);
CREATE INDEX IF NOT EXISTS idx_xp_transactions_user ON community_xp_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_user ON community_activity_feed(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_date ON community_activity_feed(created_at DESC);

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 6: ENABLE RLS AND DROP/CREATE POLICIES
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE community_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_leaderboard ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_challenge_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_user_badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_user_levels ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_xp_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_user_streaks ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_user_achievement_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_activity_feed ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_challenge_invites ENABLE ROW LEVEL SECURITY;
ALTER TABLE community_challenge_videos ENABLE ROW LEVEL SECURITY;

-- Drop ALL policies
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
DROP POLICY IF EXISTS "Badges are viewable by all authenticated users" ON community_badges;
DROP POLICY IF EXISTS "User badges are viewable by all" ON community_user_badges;
DROP POLICY IF EXISTS "Users can only receive their own badges" ON community_user_badges;
DROP POLICY IF EXISTS "User levels are viewable by all" ON community_user_levels;
DROP POLICY IF EXISTS "Users can only update own level" ON community_user_levels;
DROP POLICY IF EXISTS "Users can view own XP transactions" ON community_xp_transactions;
DROP POLICY IF EXISTS "User streaks are viewable by all" ON community_user_streaks;
DROP POLICY IF EXISTS "Users can manage own streak" ON community_user_streaks;
DROP POLICY IF EXISTS "Achievements are viewable by all authenticated users" ON community_achievements;
DROP POLICY IF EXISTS "Achievement progress is viewable by all" ON community_user_achievement_progress;
DROP POLICY IF EXISTS "Users can update own achievement progress" ON community_user_achievement_progress;
DROP POLICY IF EXISTS "Activity feed is viewable by followers or public" ON community_activity_feed;
DROP POLICY IF EXISTS "Users can view invites involving them" ON community_challenge_invites;
DROP POLICY IF EXISTS "Users can create invites" ON community_challenge_invites;
DROP POLICY IF EXISTS "Invitees can update invite status" ON community_challenge_invites;
DROP POLICY IF EXISTS "Challenge videos are viewable by all authenticated users" ON community_challenge_videos;
DROP POLICY IF EXISTS "Users can upload videos for own entries" ON community_challenge_videos;

-- Create policies
CREATE POLICY "Users can view public profiles or own profile" ON community_profiles FOR SELECT USING (is_public = true OR user_id = auth.uid());
CREATE POLICY "Users can manage own profile" ON community_profiles FOR ALL USING (user_id = auth.uid());
CREATE POLICY "Users can manage own follows" ON community_follows FOR ALL USING (follower_id = auth.uid());
CREATE POLICY "Users can see follows involving them" ON community_follows FOR SELECT USING (following_id = auth.uid() OR follower_id = auth.uid());
CREATE POLICY "Public posts visible to all authenticated users" ON community_posts FOR SELECT USING (auth.uid() IS NOT NULL AND (visibility = 'public' OR user_id = auth.uid() OR (visibility = 'followers' AND EXISTS (SELECT 1 FROM community_follows WHERE follower_id = auth.uid() AND following_id = community_posts.user_id AND status = 'active'))));
CREATE POLICY "Users can manage own posts" ON community_posts FOR ALL USING (user_id = auth.uid());
CREATE POLICY "Users can see all likes" ON community_likes FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can manage own likes" ON community_likes FOR ALL USING (user_id = auth.uid());
CREATE POLICY "Users can see comments on visible posts" ON community_comments FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can manage own comments" ON community_comments FOR ALL USING (user_id = auth.uid());
CREATE POLICY "Leaderboard visible for opted-in users" ON community_leaderboard FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can manage own leaderboard entries" ON community_leaderboard FOR ALL USING (user_id = auth.uid());
CREATE POLICY "All authenticated users can view challenges" ON community_challenges FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can view challenge entries" ON community_challenge_entries FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can manage own challenge entries" ON community_challenge_entries FOR ALL USING (user_id = auth.uid());
CREATE POLICY "Badges are viewable by all authenticated users" ON community_badges FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "User badges are viewable by all" ON community_user_badges FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can only receive their own badges" ON community_user_badges FOR INSERT WITH CHECK (user_id = auth.uid());
CREATE POLICY "User levels are viewable by all" ON community_user_levels FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can only update own level" ON community_user_levels FOR ALL USING (user_id = auth.uid());
CREATE POLICY "Users can view own XP transactions" ON community_xp_transactions FOR SELECT USING (user_id = auth.uid());
CREATE POLICY "User streaks are viewable by all" ON community_user_streaks FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can manage own streak" ON community_user_streaks FOR ALL USING (user_id = auth.uid());
CREATE POLICY "Achievements are viewable by all authenticated users" ON community_achievements FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Achievement progress is viewable by all" ON community_user_achievement_progress FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can update own achievement progress" ON community_user_achievement_progress FOR ALL USING (user_id = auth.uid());
CREATE POLICY "Activity feed is viewable by followers or public" ON community_activity_feed FOR SELECT USING (auth.uid() IS NOT NULL AND (user_id = auth.uid() OR EXISTS (SELECT 1 FROM community_follows WHERE follower_id = auth.uid() AND following_id = community_activity_feed.user_id AND status = 'active')));
CREATE POLICY "Users can view invites involving them" ON community_challenge_invites FOR SELECT USING (inviter_id = auth.uid() OR invitee_id = auth.uid());
CREATE POLICY "Users can create invites" ON community_challenge_invites FOR INSERT WITH CHECK (inviter_id = auth.uid());
CREATE POLICY "Invitees can update invite status" ON community_challenge_invites FOR UPDATE USING (invitee_id = auth.uid());
CREATE POLICY "Challenge videos are viewable by all authenticated users" ON community_challenge_videos FOR SELECT USING (auth.uid() IS NOT NULL);
CREATE POLICY "Users can upload videos for own entries" ON community_challenge_videos FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM community_challenge_entries WHERE id = entry_id AND user_id = auth.uid()));

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 7: CREATE TRIGGER FUNCTIONS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION update_post_likes_count() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE community_posts SET likes_count = likes_count + 1 WHERE id = NEW.post_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE community_posts SET likes_count = likes_count - 1 WHERE id = OLD.post_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_post_comments_count() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE community_posts SET comments_count = comments_count + 1 WHERE id = NEW.post_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE community_posts SET comments_count = comments_count - 1 WHERE id = OLD.post_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_challenge_participants_count() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE community_challenges SET participants_count = participants_count + 1 WHERE id = NEW.challenge_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE community_challenges SET participants_count = participants_count - 1 WHERE id = OLD.challenge_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- update_updated_at_column() already exists from other tables, reuse it
-- If it doesn't exist, create it:
CREATE OR REPLACE FUNCTION update_updated_at_column() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 8: CREATE TRIGGERS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TRIGGER trigger_update_likes_count AFTER INSERT OR DELETE ON community_likes FOR EACH ROW EXECUTE FUNCTION update_post_likes_count();
CREATE TRIGGER trigger_update_comments_count AFTER INSERT OR DELETE ON community_comments FOR EACH ROW EXECUTE FUNCTION update_post_comments_count();
CREATE TRIGGER trigger_update_participants_count AFTER INSERT OR DELETE ON community_challenge_entries FOR EACH ROW EXECUTE FUNCTION update_challenge_participants_count();
CREATE TRIGGER trigger_community_profiles_updated BEFORE UPDATE ON community_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_community_posts_updated BEFORE UPDATE ON community_posts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_community_comments_updated BEFORE UPDATE ON community_comments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 9: CREATE RPC FUNCTIONS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION get_community_feed(p_limit INTEGER DEFAULT 20, p_offset INTEGER DEFAULT 0)
RETURNS TABLE (
    id UUID, user_id UUID, user_name TEXT, user_avatar TEXT, workout_name TEXT,
    total_volume_kg DOUBLE PRECISION, total_sets INTEGER, total_reps INTEGER,
    duration_minutes INTEGER, prs_achieved INTEGER, pr_exercises TEXT[],
    caption TEXT, likes_count INTEGER, comments_count INTEGER,
    created_at TIMESTAMPTZ, is_liked BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT p.id, p.user_id, up.name, up.profile_image_url, p.workout_name,
           p.total_volume_kg, p.total_sets, p.total_reps, p.duration_minutes,
           p.prs_achieved, p.pr_exercises, p.caption, p.likes_count, p.comments_count,
           p.created_at, EXISTS (SELECT 1 FROM community_likes l WHERE l.post_id = p.id AND l.user_id = auth.uid())
    FROM community_posts p
    JOIN user_profiles up ON p.user_id = up.id
    WHERE p.user_id = auth.uid() OR p.visibility = 'public'
       OR (p.visibility = 'followers' AND EXISTS (SELECT 1 FROM community_follows cf WHERE cf.follower_id = auth.uid() AND cf.following_id = p.user_id AND cf.status = 'active'))
    ORDER BY p.created_at DESC LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION get_current_max_out_friday()
RETURNS TABLE (id UUID, title TEXT, exercise_id TEXT, exercise_name TEXT, start_date DATE, end_date DATE, participants_count INTEGER, user_entry_kg DOUBLE PRECISION, user_rank INTEGER) AS $$
BEGIN
    RETURN QUERY
    SELECT c.id, c.title, c.exercise_id, c.exercise_name, c.start_date, c.end_date, c.participants_count, ce.value_kg, ce.rank
    FROM community_challenges c
    LEFT JOIN community_challenge_entries ce ON c.id = ce.challenge_id AND ce.user_id = auth.uid()
    WHERE c.challenge_type = 'max_out_friday' AND c.status = 'active' AND CURRENT_DATE BETWEEN c.start_date AND c.end_date
    LIMIT 1;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION award_xp(p_user_id UUID, p_amount INTEGER, p_source TEXT, p_source_id TEXT DEFAULT NULL, p_description TEXT DEFAULT NULL)
RETURNS community_user_levels AS $$
DECLARE
    v_user_level community_user_levels;
    v_new_level INTEGER;
BEGIN
    INSERT INTO community_user_levels (user_id, level, current_xp, total_xp)
    VALUES (p_user_id, 1, p_amount, p_amount)
    ON CONFLICT (user_id) DO UPDATE SET
        total_xp = community_user_levels.total_xp + p_amount,
        current_xp = community_user_levels.current_xp + p_amount,
        updated_at = NOW()
    RETURNING * INTO v_user_level;

    v_new_level := GREATEST(1, FLOOR(SQRT(v_user_level.total_xp / 10.0))::INTEGER);

    IF v_new_level > v_user_level.level THEN
        UPDATE community_user_levels SET level = v_new_level WHERE user_id = p_user_id RETURNING * INTO v_user_level;
        INSERT INTO community_activity_feed (user_id, activity_type, metadata) VALUES (p_user_id, 'level_up', jsonb_build_object('new_level', v_new_level));
    END IF;

    INSERT INTO community_xp_transactions (user_id, amount, source, source_id, description) VALUES (p_user_id, p_amount, p_source, p_source_id, p_description);
    RETURN v_user_level;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION update_user_streak(p_user_id UUID)
RETURNS community_user_streaks AS $$
DECLARE
    v_streak community_user_streaks;
    v_today DATE := CURRENT_DATE;
BEGIN
    INSERT INTO community_user_streaks (user_id, current_streak, longest_streak, last_workout_date)
    VALUES (p_user_id, 1, 1, v_today)
    ON CONFLICT (user_id) DO UPDATE SET
        current_streak = CASE
            WHEN community_user_streaks.last_workout_date = v_today THEN community_user_streaks.current_streak
            WHEN community_user_streaks.last_workout_date = v_today - 1 THEN community_user_streaks.current_streak + 1
            ELSE 1
        END,
        longest_streak = GREATEST(community_user_streaks.longest_streak,
            CASE WHEN community_user_streaks.last_workout_date = v_today - 1 THEN community_user_streaks.current_streak + 1 ELSE 1 END),
        last_workout_date = v_today,
        updated_at = NOW()
    RETURNING * INTO v_streak;
    RETURN v_streak;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION check_and_award_badge(p_user_id UUID, p_badge_id TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    v_badge community_badges;
BEGIN
    SELECT * INTO v_badge FROM community_badges WHERE id = p_badge_id;
    IF NOT FOUND THEN RETURN FALSE; END IF;
    IF EXISTS (SELECT 1 FROM community_user_badges WHERE user_id = p_user_id AND badge_id = p_badge_id) THEN RETURN FALSE; END IF;

    INSERT INTO community_user_badges (user_id, badge_id) VALUES (p_user_id, p_badge_id);
    IF v_badge.xp_reward > 0 THEN PERFORM award_xp(p_user_id, v_badge.xp_reward, 'badge', p_badge_id); END IF;
    INSERT INTO community_activity_feed (user_id, activity_type, target_id, metadata) VALUES (p_user_id, 'badge_earned', p_badge_id, jsonb_build_object('badge_name', v_badge.name));
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 10: SEED BADGES
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO community_badges (id, name, description, icon_name, category, rarity, xp_reward, requirement_type, requirement_value) VALUES
('first_workout', 'First Steps', 'Complete your first workout', 'fitness_center', 'workout', 'common', 100, 'workout_count', 1),
('workout_10', 'Getting Started', 'Complete 10 workouts', 'fitness_center', 'workout', 'common', 200, 'workout_count', 10),
('workout_100', 'Century Club', 'Complete 100 workouts', 'military_tech', 'workout', 'rare', 500, 'workout_count', 100),
('first_pr', 'PR Hunter', 'Achieve your first personal record', 'emoji_events', 'strength', 'common', 100, 'pr_count', 1),
('streak_7', 'Week Warrior', 'Maintain a 7-day workout streak', 'local_fire_department', 'consistency', 'common', 150, 'streak_days', 7),
('streak_30', 'Monthly Grind', 'Maintain a 30-day workout streak', 'whatshot', 'consistency', 'rare', 500, 'streak_days', 30),
('first_share', 'Going Public', 'Share your first workout', 'share', 'community', 'common', 50, 'share_count', 1),
('challenge_first', 'Challenger', 'Participate in your first challenge', 'emoji_events', 'challenge', 'common', 100, 'challenge_participation', 1),
('challenge_winner', 'Champion', 'Win a community challenge', 'military_tech', 'challenge', 'epic', 500, 'challenge_wins', 1)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════
-- STEP 11: CREATE INITIAL MAX OUT FRIDAY
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO community_challenges (title, description, challenge_type, exercise_id, exercise_name, start_date, end_date, is_recurring, recurrence_pattern, status)
VALUES ('Max Out Friday: Bench Press', 'Show us your best bench press!', 'max_out_friday', 'bench_press', 'Bench Press',
        date_trunc('week', CURRENT_DATE)::date + 4, date_trunc('week', CURRENT_DATE)::date + 6, true, 'weekly_friday', 'active')
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════
-- DONE!
-- ═══════════════════════════════════════════════════════════════════════════