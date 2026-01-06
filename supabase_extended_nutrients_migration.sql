-- =====================================================
-- PROMETHEUS EXTENDED NUTRIENTS MIGRATION
-- =====================================================
-- Adds micronutrient columns to meal_items table
-- Adds 'shake' to meal_type constraint
-- Run this in Supabase SQL Editor
-- =====================================================

-- =====================================================
-- 0. ADD 'shake' TO MEAL_TYPE CONSTRAINT
-- =====================================================

-- Drop old constraint and add new one with 'shake'
ALTER TABLE meals DROP CONSTRAINT IF EXISTS meals_meal_type_check;
ALTER TABLE meals ADD CONSTRAINT meals_meal_type_check
    CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack', 'shake'));

-- Also update meal_templates if it exists
ALTER TABLE meal_templates DROP CONSTRAINT IF EXISTS meal_templates_meal_type_check;
ALTER TABLE meal_templates ADD CONSTRAINT meal_templates_meal_type_check
    CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack', 'shake'));

-- =====================================================
-- 1. ADD CARB DETAIL COLUMNS
-- =====================================================

ALTER TABLE meal_items
ADD COLUMN IF NOT EXISTS fiber REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS sugar REAL DEFAULT 0;

-- =====================================================
-- 2. ADD FAT DETAIL COLUMNS
-- =====================================================

ALTER TABLE meal_items
ADD COLUMN IF NOT EXISTS saturated_fat REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS unsaturated_fat REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS trans_fat REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS omega3 REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS omega6 REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS cholesterol REAL DEFAULT 0;

-- =====================================================
-- 3. ADD MINERAL COLUMNS
-- =====================================================

ALTER TABLE meal_items
ADD COLUMN IF NOT EXISTS sodium REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS potassium REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS calcium REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS iron REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS magnesium REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS zinc REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS phosphorus REAL DEFAULT 0;

-- =====================================================
-- 4. ADD VITAMIN COLUMNS
-- =====================================================

ALTER TABLE meal_items
ADD COLUMN IF NOT EXISTS vitamin_a REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_c REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_d REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_e REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_k REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_b1 REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_b2 REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_b3 REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_b6 REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS vitamin_b12 REAL DEFAULT 0,
ADD COLUMN IF NOT EXISTS folate REAL DEFAULT 0;

-- =====================================================
-- 5. VERIFICATION QUERY
-- =====================================================

-- Run this to verify all columns were added:
-- SELECT column_name, data_type, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'meal_items'
-- ORDER BY ordinal_position;

-- =====================================================
-- COLUMN REFERENCE (Units)
-- =====================================================
--
-- Carb Details:
--   fiber: g
--   sugar: g
--
-- Fat Details:
--   saturated_fat: g
--   unsaturated_fat: g
--   trans_fat: g
--   omega3: mg
--   omega6: mg
--   cholesterol: mg
--
-- Minerals:
--   sodium: mg
--   potassium: mg
--   calcium: mg
--   iron: mg
--   magnesium: mg
--   zinc: mg
--   phosphorus: mg
--
-- Vitamins:
--   vitamin_a: mcg RAE
--   vitamin_c: mg
--   vitamin_d: mcg
--   vitamin_e: mg
--   vitamin_k: mcg
--   vitamin_b1: mg (Thiamin)
--   vitamin_b2: mg (Riboflavin)
--   vitamin_b3: mg (Niacin)
--   vitamin_b6: mg
--   vitamin_b12: mcg
--   folate: mcg DFE
--