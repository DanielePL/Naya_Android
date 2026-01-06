-- =====================================================
-- USER PROFILES TABLE MIGRATION
-- =====================================================
-- Updates existing user_profiles table or creates it if not exists

-- Check if table exists, if not create it
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_profiles') THEN
        CREATE TABLE user_profiles (
            id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
            name TEXT NOT NULL DEFAULT 'Champion',
            weight NUMERIC,
            height NUMERIC,
            age INTEGER,
            training_experience INTEGER,
            personal_records JSONB DEFAULT '{}'::jsonb,
            medical_conditions JSONB DEFAULT '[]'::jsonb,
            injuries JSONB DEFAULT '[]'::jsonb,
            goals TEXT[] DEFAULT '{}',
            preferred_sports TEXT[] DEFAULT '{}',
            target_workout_duration INTEGER,
            last_seen BIGINT,
            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;
END $$;

-- Add missing columns if they don't exist
DO $$
BEGIN
    -- Add last_seen column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'last_seen') THEN
        ALTER TABLE user_profiles ADD COLUMN last_seen BIGINT;
    END IF;

    -- Add name column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'name') THEN
        ALTER TABLE user_profiles ADD COLUMN name TEXT NOT NULL DEFAULT 'Champion';
    END IF;

    -- Add weight column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'weight') THEN
        ALTER TABLE user_profiles ADD COLUMN weight NUMERIC;
    END IF;

    -- Add height column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'height') THEN
        ALTER TABLE user_profiles ADD COLUMN height NUMERIC;
    END IF;

    -- Add age column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'age') THEN
        ALTER TABLE user_profiles ADD COLUMN age INTEGER;
    END IF;

    -- Add training_experience column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'training_experience') THEN
        ALTER TABLE user_profiles ADD COLUMN training_experience INTEGER;
    END IF;

    -- Add personal_records column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'personal_records') THEN
        ALTER TABLE user_profiles ADD COLUMN personal_records JSONB DEFAULT '{}'::jsonb;
    END IF;

    -- Add medical_conditions column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'medical_conditions') THEN
        ALTER TABLE user_profiles ADD COLUMN medical_conditions JSONB DEFAULT '[]'::jsonb;
    END IF;

    -- Add injuries column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'injuries') THEN
        ALTER TABLE user_profiles ADD COLUMN injuries JSONB DEFAULT '[]'::jsonb;
    END IF;

    -- Add goals column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'goals') THEN
        ALTER TABLE user_profiles ADD COLUMN goals TEXT[] DEFAULT '{}';
    END IF;

    -- Add preferred_sports column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'preferred_sports') THEN
        ALTER TABLE user_profiles ADD COLUMN preferred_sports TEXT[] DEFAULT '{}';
    END IF;

    -- Add target_workout_duration column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'target_workout_duration') THEN
        ALTER TABLE user_profiles ADD COLUMN target_workout_duration INTEGER;
    END IF;

    -- Add created_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'created_at') THEN
        ALTER TABLE user_profiles ADD COLUMN created_at TIMESTAMPTZ DEFAULT NOW();
    END IF;

    -- Add updated_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'user_profiles' AND column_name = 'updated_at') THEN
        ALTER TABLE user_profiles ADD COLUMN updated_at TIMESTAMPTZ DEFAULT NOW();
    END IF;
END $$;

-- Create indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_user_profiles_id ON user_profiles(id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_last_seen ON user_profiles(last_seen DESC);

-- Enable RLS
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view their own profile" ON user_profiles;
DROP POLICY IF EXISTS "Users can create their own profile" ON user_profiles;
DROP POLICY IF EXISTS "Users can update their own profile" ON user_profiles;
DROP POLICY IF EXISTS "Users can delete their own profile" ON user_profiles;

-- Create RLS Policies
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

-- Success message
DO $$
BEGIN
    RAISE NOTICE '✅ User profiles table migrated successfully!';
END $$;