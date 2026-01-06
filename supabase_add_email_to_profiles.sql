-- =====================================================
-- ADD EMAIL TO USER_PROFILES
-- =====================================================
-- Makes it easier for admin to contact users

-- 1. Add email column
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS email TEXT;

-- 2. Backfill existing users with their emails from auth.users
UPDATE user_profiles
SET email = au.email
FROM auth.users au
WHERE user_profiles.id = au.id
AND user_profiles.email IS NULL;

-- 3. Update the trigger function to include email on signup
CREATE OR REPLACE FUNCTION create_default_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_profiles (id, name, email)
    VALUES (NEW.id, 'Champion', NEW.email)
    ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Create index for email lookups
CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON user_profiles(email);

-- Verify
SELECT id, name, email, created_at FROM user_profiles ORDER BY created_at DESC LIMIT 10;
