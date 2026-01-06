-- ═══════════════════════════════════════════════════════════════════════════
-- PROMETHEUS COMMUNITY FEATURE - EXTENDED SCHEMA
-- Version: 2.0
-- Date: 2024-12-11
--
-- Extensions for:
-- - Gamification (Badges, XP, Levels, Streaks)
-- - User-Created Challenges
-- - Challenge Invites
-- - Activity Feed
-- - Enhanced Max Out Friday
-- ═══════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. BADGES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_badges (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('workout', 'strength', 'consistency', 'community', 'challenge', 'special')),
    rarity TEXT NOT NULL CHECK (rarity IN ('common', 'rare', 'epic', 'legendary')),
    xp_reward INTEGER DEFAULT 0,
    requirement_type TEXT,  -- e.g., 'workout_count', 'pr_count', 'streak_days'
    requirement_value INTEGER,
    is_hidden BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE community_badges IS 'Badge definitions for gamification';

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. USER BADGES (Earned badges)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    badge_id TEXT NOT NULL REFERENCES community_badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT unique_user_badge UNIQUE(user_id, badge_id)
);

CREATE INDEX IF NOT EXISTS idx_user_badges_user ON community_user_badges(user_id);
CREATE INDEX IF NOT EXISTS idx_user_badges_badge ON community_user_badges(badge_id);

COMMENT ON TABLE community_user_badges IS 'Badges earned by users';

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. USER LEVELS (XP System)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_user_levels (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    level INTEGER DEFAULT 1,
    current_xp INTEGER DEFAULT 0,
    total_xp INTEGER DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE community_user_levels IS 'User level and XP tracking';

-- ═══════════════════════════════════════════════════════════════════════════
-- 4. XP TRANSACTIONS (History)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_xp_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    source TEXT NOT NULL,  -- 'workout', 'pr', 'challenge', 'social', 'badge'
    source_id TEXT,        -- Related entity ID
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_xp_transactions_user ON community_xp_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_xp_transactions_date ON community_xp_transactions(created_at DESC);

COMMENT ON TABLE community_xp_transactions IS 'XP gain history';

-- ═══════════════════════════════════════════════════════════════════════════
-- 5. USER STREAKS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_user_streaks (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    last_workout_date DATE,
    streak_protected_until DATE,  -- Freeze protection
    freeze_tokens INTEGER DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE community_user_streaks IS 'User workout streak tracking';

-- ═══════════════════════════════════════════════════════════════════════════
-- 6. ACHIEVEMENTS (Multi-tier goals)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_achievements (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    category TEXT NOT NULL,
    tiers JSONB NOT NULL,  -- Array of {tier, name, requirement_value, xp_reward}
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

CREATE INDEX IF NOT EXISTS idx_achievement_progress_user ON community_user_achievement_progress(user_id);

COMMENT ON TABLE community_achievements IS 'Multi-tier achievement definitions';
COMMENT ON TABLE community_user_achievement_progress IS 'User progress on achievements';

-- ═══════════════════════════════════════════════════════════════════════════
-- 7. ACTIVITY FEED
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_activity_feed (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    activity_type TEXT NOT NULL CHECK (activity_type IN (
        'workout_shared', 'pr_achieved', 'challenge_joined', 'challenge_won',
        'badge_earned', 'level_up', 'follow', 'like', 'comment', 'streak_milestone'
    )),
    target_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    target_id TEXT,  -- Post ID, Challenge ID, etc.
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_activity_user ON community_activity_feed(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_date ON community_activity_feed(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_activity_type ON community_activity_feed(activity_type);

COMMENT ON TABLE community_activity_feed IS 'Activity feed for social engagement';

-- ═══════════════════════════════════════════════════════════════════════════
-- 8. CHALLENGE INVITES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_challenge_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID NOT NULL REFERENCES community_challenges(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    invitee_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
    created_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT unique_challenge_invite UNIQUE(challenge_id, invitee_id)
);

CREATE INDEX IF NOT EXISTS idx_invite_invitee ON community_challenge_invites(invitee_id) WHERE status = 'pending';
CREATE INDEX IF NOT EXISTS idx_invite_challenge ON community_challenge_invites(challenge_id);

COMMENT ON TABLE community_challenge_invites IS 'Invitations to participate in challenges';

-- ═══════════════════════════════════════════════════════════════════════════
-- 9. CHALLENGE VIDEOS (Proof)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS community_challenge_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id UUID NOT NULL REFERENCES community_challenge_entries(id) ON DELETE CASCADE,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    duration_seconds INTEGER,
    uploaded_at TIMESTAMPTZ DEFAULT NOW(),
    is_verified BOOLEAN DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_challenge_videos_entry ON community_challenge_videos(entry_id);

COMMENT ON TABLE community_challenge_videos IS 'Video proof for challenge entries';

-- ═══════════════════════════════════════════════════════════════════════════
-- 10. ADD is_public AND created_by TO CHALLENGES
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE community_challenges
    ADD COLUMN IF NOT EXISTS is_public BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS max_participants INTEGER;

-- ═══════════════════════════════════════════════════════════════════════════
-- ROW LEVEL SECURITY
-- ═══════════════════════════════════════════════════════════════════════════

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

-- Drop existing policies first to avoid conflicts
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

-- Badges (readable by all)
CREATE POLICY "Badges are viewable by all authenticated users" ON community_badges
    FOR SELECT USING (auth.uid() IS NOT NULL);

-- User Badges
CREATE POLICY "User badges are viewable by all" ON community_user_badges
    FOR SELECT USING (auth.uid() IS NOT NULL);

CREATE POLICY "Users can only receive their own badges" ON community_user_badges
    FOR INSERT WITH CHECK (user_id = auth.uid());

-- User Levels
CREATE POLICY "User levels are viewable by all" ON community_user_levels
    FOR SELECT USING (auth.uid() IS NOT NULL);

CREATE POLICY "Users can only update own level" ON community_user_levels
    FOR ALL USING (user_id = auth.uid());

-- XP Transactions
CREATE POLICY "Users can view own XP transactions" ON community_xp_transactions
    FOR SELECT USING (user_id = auth.uid());

-- User Streaks
CREATE POLICY "User streaks are viewable by all" ON community_user_streaks
    FOR SELECT USING (auth.uid() IS NOT NULL);

CREATE POLICY "Users can manage own streak" ON community_user_streaks
    FOR ALL USING (user_id = auth.uid());

-- Achievements
CREATE POLICY "Achievements are viewable by all authenticated users" ON community_achievements
    FOR SELECT USING (auth.uid() IS NOT NULL);

-- Achievement Progress
CREATE POLICY "Achievement progress is viewable by all" ON community_user_achievement_progress
    FOR SELECT USING (auth.uid() IS NOT NULL);

CREATE POLICY "Users can update own achievement progress" ON community_user_achievement_progress
    FOR ALL USING (user_id = auth.uid());

-- Activity Feed (visibility based on followed users or public activity)
CREATE POLICY "Activity feed is viewable by followers or public" ON community_activity_feed
    FOR SELECT USING (
        auth.uid() IS NOT NULL AND (
            user_id = auth.uid()
            OR EXISTS (
                SELECT 1 FROM community_follows
                WHERE follower_id = auth.uid()
                AND following_id = community_activity_feed.user_id
                AND status = 'active'
            )
        )
    );

-- Challenge Invites
CREATE POLICY "Users can view invites involving them" ON community_challenge_invites
    FOR SELECT USING (inviter_id = auth.uid() OR invitee_id = auth.uid());

CREATE POLICY "Users can create invites" ON community_challenge_invites
    FOR INSERT WITH CHECK (inviter_id = auth.uid());

CREATE POLICY "Invitees can update invite status" ON community_challenge_invites
    FOR UPDATE USING (invitee_id = auth.uid());

-- Challenge Videos
CREATE POLICY "Challenge videos are viewable by all authenticated users" ON community_challenge_videos
    FOR SELECT USING (auth.uid() IS NOT NULL);

CREATE POLICY "Users can upload videos for own entries" ON community_challenge_videos
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM community_challenge_entries
            WHERE id = entry_id AND user_id = auth.uid()
        )
    );

-- ═══════════════════════════════════════════════════════════════════════════
-- FUNCTIONS
-- ═══════════════════════════════════════════════════════════════════════════

-- Award XP to user
CREATE OR REPLACE FUNCTION award_xp(
    p_user_id UUID,
    p_amount INTEGER,
    p_source TEXT,
    p_source_id TEXT DEFAULT NULL,
    p_description TEXT DEFAULT NULL
)
RETURNS community_user_levels AS $$
DECLARE
    v_user_level community_user_levels;
    v_new_level INTEGER;
BEGIN
    -- Upsert user level
    INSERT INTO community_user_levels (user_id, level, current_xp, total_xp)
    VALUES (p_user_id, 1, p_amount, p_amount)
    ON CONFLICT (user_id) DO UPDATE SET
        total_xp = community_user_levels.total_xp + p_amount,
        current_xp = community_user_levels.current_xp + p_amount,
        updated_at = NOW()
    RETURNING * INTO v_user_level;

    -- Calculate new level (simple formula: level = sqrt(total_xp / 10))
    v_new_level := GREATEST(1, FLOOR(SQRT(v_user_level.total_xp / 10.0))::INTEGER);

    -- Update level if changed
    IF v_new_level > v_user_level.level THEN
        UPDATE community_user_levels
        SET level = v_new_level
        WHERE user_id = p_user_id
        RETURNING * INTO v_user_level;

        -- Create activity for level up
        INSERT INTO community_activity_feed (user_id, activity_type, metadata)
        VALUES (p_user_id, 'level_up', jsonb_build_object('new_level', v_new_level));
    END IF;

    -- Record transaction
    INSERT INTO community_xp_transactions (user_id, amount, source, source_id, description)
    VALUES (p_user_id, p_amount, p_source, p_source_id, p_description);

    RETURN v_user_level;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Check and award badges
CREATE OR REPLACE FUNCTION check_and_award_badge(
    p_user_id UUID,
    p_badge_id TEXT
)
RETURNS BOOLEAN AS $$
DECLARE
    v_badge community_badges;
    v_already_has BOOLEAN;
BEGIN
    -- Check if badge exists
    SELECT * INTO v_badge FROM community_badges WHERE id = p_badge_id;
    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    -- Check if already has badge
    SELECT EXISTS (
        SELECT 1 FROM community_user_badges
        WHERE user_id = p_user_id AND badge_id = p_badge_id
    ) INTO v_already_has;

    IF v_already_has THEN
        RETURN FALSE;
    END IF;

    -- Award badge
    INSERT INTO community_user_badges (user_id, badge_id)
    VALUES (p_user_id, p_badge_id);

    -- Award XP
    IF v_badge.xp_reward > 0 THEN
        PERFORM award_xp(p_user_id, v_badge.xp_reward, 'badge', p_badge_id, 'Earned badge: ' || v_badge.name);
    END IF;

    -- Create activity
    INSERT INTO community_activity_feed (user_id, activity_type, target_id, metadata)
    VALUES (p_user_id, 'badge_earned', p_badge_id, jsonb_build_object(
        'badge_name', v_badge.name,
        'badge_rarity', v_badge.rarity
    ));

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Update streak on workout completion
CREATE OR REPLACE FUNCTION update_user_streak(p_user_id UUID)
RETURNS community_user_streaks AS $$
DECLARE
    v_streak community_user_streaks;
    v_today DATE := CURRENT_DATE;
BEGIN
    -- Upsert streak record
    INSERT INTO community_user_streaks (user_id, current_streak, longest_streak, last_workout_date)
    VALUES (p_user_id, 1, 1, v_today)
    ON CONFLICT (user_id) DO UPDATE SET
        current_streak = CASE
            WHEN community_user_streaks.last_workout_date = v_today THEN community_user_streaks.current_streak
            WHEN community_user_streaks.last_workout_date = v_today - 1 THEN community_user_streaks.current_streak + 1
            WHEN community_user_streaks.streak_protected_until >= v_today THEN community_user_streaks.current_streak + 1
            ELSE 1
        END,
        longest_streak = GREATEST(
            community_user_streaks.longest_streak,
            CASE
                WHEN community_user_streaks.last_workout_date = v_today THEN community_user_streaks.current_streak
                WHEN community_user_streaks.last_workout_date = v_today - 1 THEN community_user_streaks.current_streak + 1
                ELSE 1
            END
        ),
        last_workout_date = v_today,
        streak_protected_until = NULL,
        updated_at = NOW()
    RETURNING * INTO v_streak;

    -- Check for streak milestones (7, 30, 100 days)
    IF v_streak.current_streak = 7 THEN
        PERFORM check_and_award_badge(p_user_id, 'streak_7');
        INSERT INTO community_activity_feed (user_id, activity_type, metadata)
        VALUES (p_user_id, 'streak_milestone', jsonb_build_object('days', 7));
    ELSIF v_streak.current_streak = 30 THEN
        PERFORM check_and_award_badge(p_user_id, 'streak_30');
        INSERT INTO community_activity_feed (user_id, activity_type, metadata)
        VALUES (p_user_id, 'streak_milestone', jsonb_build_object('days', 30));
    ELSIF v_streak.current_streak = 100 THEN
        PERFORM check_and_award_badge(p_user_id, 'streak_100');
        INSERT INTO community_activity_feed (user_id, activity_type, metadata)
        VALUES (p_user_id, 'streak_milestone', jsonb_build_object('days', 100));
    END IF;

    RETURN v_streak;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Get activity feed for user (from followed users)
CREATE OR REPLACE FUNCTION get_activity_feed(
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    user_id UUID,
    user_name TEXT,
    user_avatar TEXT,
    activity_type TEXT,
    target_user_id UUID,
    target_id TEXT,
    metadata JSONB,
    created_at TIMESTAMPTaZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        af.id,
        af.user_id,
        up.name as user_name,
        up.profile_image_url as user_avatar,
        af.activity_type,
        af.target_user_id,
        af.target_id,
        af.metadata,
        af.created_at
    FROM community_activity_feed af
    JOIN user_profiles up ON af.user_id = up.id
    WHERE
        af.user_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM community_follows cf
            WHERE cf.follower_id = auth.uid()
            AND cf.following_id = af.user_id
            AND cf.status = 'active'
        )
    ORDER BY af.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Get user gamification stats
CREATE OR REPLACE FUNCTION get_user_gamification_stats(p_user_id UUID)
RETURNS TABLE (
    level INTEGER,
    total_xp INTEGER,
    current_streak INTEGER,
    longest_streak INTEGER,
    badges_count BIGINT,
    recent_badges JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COALESCE(ul.level, 1),
        COALESCE(ul.total_xp, 0),
        COALESCE(us.current_streak, 0),
        COALESCE(us.longest_streak, 0),
        (SELECT COUNT(*) FROM community_user_badges WHERE user_id = p_user_id),
        (
            SELECT COALESCE(jsonb_agg(jsonb_build_object(
                'id', b.id,
                'name', b.name,
                'icon_name', b.icon_name,
                'rarity', b.rarity,
                'earned_at', ub.earned_at
            ) ORDER BY ub.earned_at DESC), '[]'::jsonb)
            FROM community_user_badges ub
            JOIN community_badges b ON ub.badge_id = b.id
            WHERE ub.user_id = p_user_id
            LIMIT 5
        )
    FROM (SELECT p_user_id as id) u
    LEFT JOIN community_user_levels ul ON ul.user_id = u.id
    LEFT JOIN community_user_streaks us ON us.user_id = u.id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create new challenge with auto-scheduling for Max Out Friday
CREATE OR REPLACE FUNCTION create_next_max_out_friday()
RETURNS community_challenges AS $$
DECLARE
    v_current_week INTEGER;
    v_exercise_index INTEGER;
    v_exercise_id TEXT;
    v_exercise_name TEXT;
    v_next_friday DATE;
    v_end_date DATE;
    v_challenge community_challenges;
BEGIN
    -- Calculate next Friday
    v_next_friday := date_trunc('week', CURRENT_DATE)::date + 4;
    IF v_next_friday <= CURRENT_DATE THEN
        v_next_friday := v_next_friday + 7;
    END IF;
    v_end_date := v_next_friday + 2;  -- Until Sunday

    -- Calculate week number since start (for rotation)
    v_current_week := EXTRACT(WEEK FROM v_next_friday)::INTEGER;

    -- Get exercise for this week (rotating through 6 exercises)
    v_exercise_index := (v_current_week - 1) % 6;
    v_exercise_id := (ARRAY['bench_press', 'squat', 'deadlift', 'overhead_press', 'barbell_row', 'weighted_pullup'])[v_exercise_index + 1];
    v_exercise_name := (ARRAY['Bench Press', 'Squat', 'Deadlift', 'Overhead Press', 'Barbell Row', 'Weighted Pull-Up'])[v_exercise_index + 1];

    -- Check if challenge already exists for this week
    IF EXISTS (
        SELECT 1 FROM community_challenges
        WHERE challenge_type = 'max_out_friday'
        AND start_date = v_next_friday
    ) THEN
        SELECT * INTO v_challenge FROM community_challenges
        WHERE challenge_type = 'max_out_friday'
        AND start_date = v_next_friday;
        RETURN v_challenge;
    END IF;

    -- Mark previous Max Out Friday as completed
    UPDATE community_challenges
    SET status = 'completed'
    WHERE challenge_type = 'max_out_friday'
    AND status = 'active'
    AND end_date < CURRENT_DATE;

    -- Create new challenge
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
        status,
        is_public
    ) VALUES (
        'Max Out Friday: ' || v_exercise_name,
        'Show us your best ' || v_exercise_name || '! Post your heaviest lift this week.',
        'max_out_friday',
        v_exercise_id,
        v_exercise_name,
        v_next_friday,
        v_end_date,
        true,
        'weekly_friday',
        CASE WHEN v_next_friday = CURRENT_DATE THEN 'active' ELSE 'upcoming' END,
        true
    )
    RETURNING * INTO v_challenge;

    RETURN v_challenge;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Get previous Max Out Friday winners
CREATE OR REPLACE FUNCTION get_max_out_friday_winners(p_limit INTEGER DEFAULT 5)
RETURNS TABLE (
    challenge_id UUID,
    exercise_name TEXT,
    winner_id UUID,
    winner_name TEXT,
    winner_avatar TEXT,
    winning_weight_kg DOUBLE PRECISION,
    week_date DATE
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT ON (c.id)
        c.id as challenge_id,
        c.exercise_name,
        e.user_id as winner_id,
        up.name as winner_name,
        up.profile_image_url as winner_avatar,
        e.value_kg as winning_weight_kg,
        c.start_date as week_date
    FROM community_challenges c
    JOIN community_challenge_entries e ON c.id = e.challenge_id
    JOIN user_profiles up ON e.user_id = up.id
    WHERE c.challenge_type = 'max_out_friday'
    AND c.status = 'completed'
    ORDER BY c.id, e.value_kg DESC NULLS LAST, e.submitted_at ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════
-- SEED BADGES
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO community_badges (id, name, description, icon_name, category, rarity, xp_reward, requirement_type, requirement_value) VALUES
-- Workout milestones
('first_workout', 'First Steps', 'Complete your first workout', 'fitness_center', 'workout', 'common', 100, 'workout_count', 1),
('workout_10', 'Getting Started', 'Complete 10 workouts', 'fitness_center', 'workout', 'common', 200, 'workout_count', 10),
('workout_100', 'Century Club', 'Complete 100 workouts', 'military_tech', 'workout', 'rare', 500, 'workout_count', 100),
('workout_500', 'Iron Veteran', 'Complete 500 workouts', 'workspace_premium', 'workout', 'epic', 1000, 'workout_count', 500),

-- Strength milestones
('first_pr', 'PR Hunter', 'Achieve your first personal record', 'emoji_events', 'strength', 'common', 100, 'pr_count', 1),
('pr_10', 'Progression', 'Achieve 10 personal records', 'trending_up', 'strength', 'rare', 300, 'pr_count', 10),
('pr_50', 'Record Breaker', 'Achieve 50 personal records', 'star', 'strength', 'epic', 750, 'pr_count', 50),

-- Streak milestones
('streak_7', 'Week Warrior', 'Maintain a 7-day workout streak', 'local_fire_department', 'consistency', 'common', 150, 'streak_days', 7),
('streak_30', 'Monthly Grind', 'Maintain a 30-day workout streak', 'whatshot', 'consistency', 'rare', 500, 'streak_days', 30),
('streak_100', 'Unstoppable', 'Maintain a 100-day workout streak', 'bolt', 'consistency', 'legendary', 2000, 'streak_days', 100),

-- Community badges
('first_share', 'Going Public', 'Share your first workout with the community', 'share', 'community', 'common', 50, 'share_count', 1),
('follower_10', 'Rising Star', 'Gain 10 followers', 'people', 'community', 'rare', 200, 'follower_count', 10),
('follower_100', 'Influencer', 'Gain 100 followers', 'groups', 'community', 'epic', 500, 'follower_count', 100),

-- Challenge badges
('challenge_first', 'Challenger', 'Participate in your first challenge', 'emoji_events', 'challenge', 'common', 100, 'challenge_participation', 1),
('challenge_winner', 'Champion', 'Win a community challenge', 'military_tech', 'challenge', 'epic', 500, 'challenge_wins', 1),
('max_out_friday_5', 'Friday Regular', 'Participate in 5 Max Out Friday challenges', 'local_fire_department', 'challenge', 'rare', 300, 'max_out_friday_count', 5),
('max_out_friday_pr', 'Friday PR', 'Set a personal record on Max Out Friday', 'star', 'challenge', 'rare', 250, 'max_out_friday_pr', 1)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════
-- SCHEDULE NEXT MAX OUT FRIDAY (Run this initially)
-- ═══════════════════════════════════════════════════════════════════════════

SELECT create_next_max_out_friday();

-- ═══════════════════════════════════════════════════════════════════════════
-- DONE
-- ═══════════════════════════════════════════════════════════════════════════

COMMENT ON SCHEMA public IS 'Community extended: gamification tables (badges, levels, streaks, achievements), activity feed, challenge invites';