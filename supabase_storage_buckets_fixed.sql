-- =====================================================
-- PROMETHEUS VIDEO STORAGE BUCKETS & POLICIES
-- =====================================================
-- Storage für Workout-Videos (Coaching Users)
-- Single Users speichern lokal auf dem Device

-- =====================================================
-- 1. CREATE USER COACH RELATIONSHIP TABLE FIRST
-- =====================================================
-- This must be created BEFORE storage policies that reference it

CREATE TABLE IF NOT EXISTS user_coach_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    coach_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'pending')),

    -- Coaching plan details
    plan_type TEXT,  -- 'basic', 'premium', 'elite'
    started_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,

    -- Unique constraint: user can only have one active coach
    CONSTRAINT unique_active_coach UNIQUE(user_id, coach_id)
);

-- Index for coach lookups
CREATE INDEX IF NOT EXISTS idx_user_coach_relationships_user_id ON user_coach_relationships(user_id);
CREATE INDEX IF NOT EXISTS idx_user_coach_relationships_coach_id ON user_coach_relationships(coach_id);
CREATE INDEX IF NOT EXISTS idx_user_coach_relationships_status ON user_coach_relationships(status);

-- RLS for user_coach_relationships
ALTER TABLE user_coach_relationships ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own coaching relationships"
ON user_coach_relationships FOR SELECT
USING (auth.uid() = user_id OR auth.uid() = coach_id);

CREATE POLICY "Users can create coaching relationships"
ON user_coach_relationships FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- =====================================================
-- 2. CREATE STORAGE BUCKET
-- =====================================================

-- Create bucket for workout videos
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'workout-videos',
    'workout-videos',
    false,  -- Private bucket (requires authentication)
    52428800,  -- 50 MB limit per video
    ARRAY['video/mp4', 'video/quicktime', 'video/x-msvideo']  -- MP4, MOV, AVI
)
ON CONFLICT (id) DO NOTHING;  -- Don't error if bucket already exists

-- =====================================================
-- 3. STORAGE POLICIES (RLS)
-- =====================================================

-- Policy: Users can upload their own videos
CREATE POLICY "Users can upload their own workout videos"
ON storage.objects FOR INSERT
WITH CHECK (
    bucket_id = 'workout-videos'
    AND auth.uid()::text = (storage.foldername(name))[1]
    -- Path format: users/{user_id}/sets/{set_id}.mp4
);

-- Policy: Users can view their own videos
CREATE POLICY "Users can view their own workout videos"
ON storage.objects FOR SELECT
USING (
    bucket_id = 'workout-videos'
    AND auth.uid()::text = (storage.foldername(name))[1]
);

-- Policy: Coaches can view videos of their clients
CREATE POLICY "Coaches can view client workout videos"
ON storage.objects FOR SELECT
USING (
    bucket_id = 'workout-videos'
    AND EXISTS (
        SELECT 1 FROM user_coach_relationships
        WHERE user_coach_relationships.user_id::text = (storage.foldername(name))[1]
        AND user_coach_relationships.coach_id = auth.uid()
        AND user_coach_relationships.status = 'active'
    )
);

-- Policy: Users can delete their own videos
CREATE POLICY "Users can delete their own workout videos"
ON storage.objects FOR DELETE
USING (
    bucket_id = 'workout-videos'
    AND auth.uid()::text = (storage.foldername(name))[1]
);

-- =====================================================
-- 4. HELPER FUNCTIONS
-- =====================================================

-- Function to check if user has active coach
CREATE OR REPLACE FUNCTION user_has_active_coach(p_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM user_coach_relationships
        WHERE user_id = p_user_id
        AND status = 'active'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get user's storage type
CREATE OR REPLACE FUNCTION get_user_storage_type(p_user_id UUID)
RETURNS TEXT AS $$
DECLARE
    v_has_coach BOOLEAN;
BEGIN
    v_has_coach := user_has_active_coach(p_user_id);

    IF v_has_coach THEN
        RETURN 'cloud';
    ELSE
        RETURN 'device';
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- USAGE NOTES
-- =====================================================

/*
1. Video Upload Path Structure:
   - Cloud: users/{user_id}/sets/{set_id}.mp4
   - Device: file:///storage/emulated/0/Prometheus/videos/{set_id}.mp4

2. Storage Decision Logic (Android):
   - Check: user_has_active_coach(user_id)
   - If TRUE → Upload to Supabase Storage
   - If FALSE → Save to device storage only

3. Coach Access:
   - Coaches can view all videos from their active clients
   - Implemented via RLS policy using user_coach_relationships

4. Storage Costs:
   - Single users: 0€ (device storage)
   - Coaching users: ~0.021€/GB/month (Supabase pricing)
   - 50MB limit per video keeps costs manageable

5. Example: Add coach to user
   INSERT INTO user_coach_relationships (user_id, coach_id, status, plan_type)
   VALUES ('user-uuid', 'coach-uuid', 'active', 'premium');
*/
