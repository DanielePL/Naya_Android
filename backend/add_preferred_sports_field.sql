-- Add preferred_sports field to user_profiles table

ALTER TABLE user_profiles
ADD COLUMN IF NOT EXISTS preferred_sports TEXT[] DEFAULT '{}';

-- Update existing users to have empty array
UPDATE user_profiles
SET preferred_sports = '{}'
WHERE preferred_sports IS NULL;

-- Verify the column was added
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'user_profiles'
  AND column_name = 'preferred_sports';