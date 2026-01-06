-- =====================================================
-- USER PROFILES TABLE
-- =====================================================
-- Stores user profile data including body stats, PRs, injuries, and preferences

CREATE TABLE IF NOT EXISTS user_profiles (
    -- Identity
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Basic Info
    name TEXT NOT NULL DEFAULT 'Champion',
    weight NUMERIC,  -- in kg
    height NUMERIC,  -- in cm
    age INTEGER,

    -- Training
    training_experience INTEGER,  -- years of training

    -- Personal Records (stored as JSONB)
    personal_records JSONB DEFAULT '{}'::jsonb,

    -- Medical & Health
    medical_conditions JSONB DEFAULT '[]'::jsonb,
    injuries JSONB DEFAULT '[]'::jsonb,

    -- Goals & Preferences
    goals TEXT[] DEFAULT '{}',
    preferred_sports TEXT[] DEFAULT '{}',
    target_workout_duration INTEGER,  -- minutes

    -- Timestamps
    last_seen BIGINT,  -- Unix timestamp in milliseconds
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_user_profiles_id ON user_profiles(id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_last_seen ON user_profiles(last_seen DESC);

-- Row Level Security (RLS)
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view their own profile"
ON user_profiles FOR SELECT
USING (auth.uid() = id);

CREATE POLICY "Users can create their own profile"
ON user_profiles FOR INSERT
WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
ON user_profiles FOR UPDATE
USING (auth.uid() = id);

CREATE POLICY "Users can delete their own profile"
ON user_profiles FOR DELETE
USING (auth.uid() = id);

-- Function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_user_profile_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for auto-updating updated_at
DROP TRIGGER IF EXISTS user_profiles_updated_at_trigger ON user_profiles;
CREATE TRIGGER user_profiles_updated_at_trigger
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_user_profile_updated_at();

-- Function to create default profile on user signup
CREATE OR REPLACE FUNCTION create_default_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_profiles (id, name)
    VALUES (NEW.id, 'Champion')
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to auto-create profile when user signs up
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION create_default_user_profile();