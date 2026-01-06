-- =====================================================
-- CHECK CURRENT EXERCISES TABLE SCHEMA
-- =====================================================
-- Run this to see what columns exist in your current table

-- Show all columns in the exercises table
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'exercises'
ORDER BY ordinal_position;

-- Show sample data
SELECT * FROM exercises LIMIT 3;

-- Count exercises
SELECT COUNT(*) as total_exercises FROM exercises;