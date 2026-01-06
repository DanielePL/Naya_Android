-- =====================================================
-- ADD SPORTS COLUMN TO WORKOUT_TEMPLATES TABLE
-- =====================================================

-- Add sports column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'workout_templates' AND column_name = 'sports') THEN
        ALTER TABLE workout_templates ADD COLUMN sports TEXT[] DEFAULT '{}';
        RAISE NOTICE '✅ Added sports column to workout_templates table';
    ELSE
        RAISE NOTICE 'ℹ️ Sports column already exists';
    END IF;
END $$;

-- Create index for sports queries
CREATE INDEX IF NOT EXISTS idx_workout_templates_sports ON workout_templates USING GIN(sports);

-- =====================================================
-- POPULATE SPORTS FOR WORKOUT TEMPLATES
-- =====================================================

-- Olympic Weightlifting Templates
UPDATE workout_templates SET sports = ARRAY['Weightlifting', 'General Strength']
WHERE name IN (
    'Bulgarian Method - Snatch Focus',
    'Olympic Lifting Technique Day',
    'Clean & Jerk Strength Builder'
);

-- Powerlifting Templates
UPDATE workout_templates SET sports = ARRAY['Powerlifting', 'General Strength']
WHERE name IN (
    'Texas Method - Squat Focus',
    '5/3/1 - Bench Press',
    'Westside Conjugate - Max Effort',
    'Smolov Jr - Bench Press'
);

-- Strongman Templates
UPDATE workout_templates SET sports = ARRAY['Strongman', 'General Strength']
WHERE name IN (
    'Strongman Event Day',
    'Farmer''s Walk & Yoke Training'
);

-- CrossFit Templates
UPDATE workout_templates SET sports = ARRAY['CrossFit', 'General Strength']
WHERE name IN (
    'CrossFit Benchmark - Fran',
    'CrossFit Benchmark - Grace',
    'CrossFit Benchmark - Murph',
    'EMOM Conditioning'
);

-- Hyrox Templates
UPDATE workout_templates SET sports = ARRAY['Hyrox', 'General Strength']
WHERE name IN (
    'Hyrox Simulation',
    'Hyrox Station Practice'
);

-- General Strength Templates
UPDATE workout_templates SET sports = ARRAY['General Strength']
WHERE name IN (
    'Upper/Lower Split - Upper',
    'Upper/Lower Split - Lower',
    'Push Pull Legs - Push',
    'Push Pull Legs - Pull',
    'Push Pull Legs - Legs',
    'Full Body Strength'
) OR sports IS NULL OR array_length(sports, 1) = 0;

-- Hypertrophy Templates
UPDATE workout_templates SET sports = ARRAY['Bodybuilding', 'General Strength']
WHERE name ILIKE '%hypertrophy%' OR name ILIKE '%volume%';

-- Verify the updates
SELECT
    sports,
    COUNT(*) as template_count,
    array_agg(name ORDER BY name) as template_names
FROM workout_templates
WHERE sports IS NOT NULL AND array_length(sports, 1) > 0
GROUP BY sports
ORDER BY template_count DESC;

-- Show all templates with their sports
SELECT name, sports
FROM workout_templates
ORDER BY name;

-- Count templates without sports
SELECT COUNT(*) as templates_without_sports
FROM workout_templates
WHERE sports IS NULL OR array_length(sports, 1) = 0;