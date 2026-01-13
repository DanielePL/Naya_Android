-- Migration: Add intensity to workout_templates
-- Run this in your Supabase SQL Editor

-- Step 1: Add intensity column to workout_templates table
ALTER TABLE workout_templates
ADD COLUMN IF NOT EXISTS intensity TEXT DEFAULT 'AKTIV';

-- Step 2: Add index for faster queries by intensity
CREATE INDEX IF NOT EXISTS idx_workout_templates_intensity
ON workout_templates(intensity);

-- Step 3: Update existing templates with appropriate intensity levels
-- Adjust these based on your actual template names

-- Sanft (gentle) workouts
UPDATE workout_templates SET intensity = 'SANFT' WHERE name ILIKE '%stretch%';
UPDATE workout_templates SET intensity = 'SANFT' WHERE name ILIKE '%yoga%';
UPDATE workout_templates SET intensity = 'SANFT' WHERE name ILIKE '%mobility%';
UPDATE workout_templates SET intensity = 'SANFT' WHERE name ILIKE '%beckenboden%';
UPDATE workout_templates SET intensity = 'SANFT' WHERE name ILIKE '%entspannung%';
UPDATE workout_templates SET intensity = 'SANFT' WHERE name ILIKE '%morgen%';

-- Power (intense) workouts
UPDATE workout_templates SET intensity = 'POWER' WHERE name ILIKE '%hiit%';
UPDATE workout_templates SET intensity = 'POWER' WHERE name ILIKE '%power%';
UPDATE workout_templates SET intensity = 'POWER' WHERE name ILIKE '%crossfit%';
UPDATE workout_templates SET intensity = 'POWER' WHERE name ILIKE '%tabata%';
UPDATE workout_templates SET intensity = 'POWER' WHERE name ILIKE '%intensiv%';

-- Everything else stays AKTIV (default)

-- Verify the changes:
-- SELECT name, intensity FROM workout_templates ORDER BY intensity, name;
