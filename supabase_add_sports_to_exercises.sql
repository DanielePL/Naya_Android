-- =====================================================
-- ADD SPORTS COLUMN TO EXERCISES TABLE
-- =====================================================

-- Add sports column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'exercises' AND column_name = 'sports') THEN
        ALTER TABLE exercises ADD COLUMN sports TEXT[] DEFAULT '{}';
        RAISE NOTICE '✅ Added sports column to exercises table';
    ELSE
        RAISE NOTICE 'ℹ️ Sports column already exists';
    END IF;
END $$;

-- Create index for sports queries
CREATE INDEX IF NOT EXISTS idx_exercises_sports ON exercises USING GIN(sports);

-- =====================================================
-- POPULATE SPORTS FOR EXERCISES
-- =====================================================

-- Olympic Weightlifting
UPDATE exercises SET sports = ARRAY['Weightlifting', 'General Strength']
WHERE name IN (
    'Snatch', 'Power Snatch', 'Hang Snatch', 'Muscle Snatch',
    'Clean', 'Power Clean', 'Hang Clean', 'Muscle Clean',
    'Clean and Jerk', 'Push Jerk', 'Split Jerk', 'Power Jerk',
    'Overhead Squat', 'Snatch Balance', 'Snatch Pull', 'Clean Pull',
    'Front Squat'  -- Used heavily in Weightlifting
);

-- Powerlifting (Big 3 + variations)
UPDATE exercises SET sports = ARRAY['Powerlifting', 'General Strength']
WHERE name ILIKE ANY(ARRAY[
    '%Squat%',
    '%Bench Press%',
    '%Deadlift%'
]) AND name NOT ILIKE '%Overhead Squat%' AND name NOT ILIKE '%Front Squat%';

-- Strongman
UPDATE exercises SET sports = ARRAY['Strongman', 'General Strength']
WHERE name ILIKE ANY(ARRAY[
    '%Atlas Stone%',
    '%Farmer%Walk%',
    '%Yoke%',
    '%Log Press%',
    '%Axle%',
    '%Tire Flip%',
    '%Sled%'
]);

-- CrossFit (WODs + specific movements)
UPDATE exercises SET sports = ARRAY['CrossFit', 'General Strength']
WHERE name IN (
    'Wall Balls', 'Burpees', 'Box Jumps', 'Thruster',
    'Strict Pull-ups', 'Kipping Pull-ups', 'Chest to Bar Pull-ups',
    'Handstand Push-ups', 'Double Unders', 'Toes to Bar',
    'Rope Climb', 'Muscle-up'
);

-- Hyrox (Event-specific)
UPDATE exercises SET sports = ARRAY['Hyrox', 'General Strength']
WHERE name ILIKE ANY(ARRAY[
    '%SkiErg%',
    '%Row%1km%',
    '%Sled Push%',
    '%Sled Pull%',
    '%Burpee Broad Jump%',
    '%Sandbag Lunges%',
    '%Farmers Carry%',
    '%Wall Balls%'
]);

-- General Strength - All compound movements
UPDATE exercises SET sports = array_append(sports, 'General Strength')
WHERE name ILIKE ANY(ARRAY[
    '%Press%',
    '%Row%',
    '%Pull%',
    '%Squat%',
    '%Lunge%',
    '%Deadlift%'
]) AND 'General Strength' != ALL(sports);

-- Isolation movements - Only General Strength
UPDATE exercises SET sports = ARRAY['General Strength']
WHERE name ILIKE ANY(ARRAY[
    '%Curl%',
    '%Extension%',
    '%Raise%',
    '%Fly%',
    '%Shrug%'
]);

-- Bodybuilding - General Strength
UPDATE exercises SET sports = ARRAY['Bodybuilding', 'General Strength']
WHERE name ILIKE ANY(ARRAY[
    '%Cable%',
    '%Machine%',
    '%Dumbbell%'
]) AND 'General Strength' != ALL(sports);

-- Verify the updates
SELECT
    sports,
    COUNT(*) as exercise_count
FROM exercises
WHERE sports IS NOT NULL AND array_length(sports, 1) > 0
GROUP BY sports
ORDER BY exercise_count DESC;

-- Show examples
SELECT name, sports
FROM exercises
WHERE sports IS NOT NULL AND array_length(sports, 1) > 0
ORDER BY name
LIMIT 20;

-- Count exercises without sports
SELECT COUNT(*) as exercises_without_sports
FROM exercises
WHERE sports IS NULL OR array_length(sports, 1) = 0;