-- ═══════════════════════════════════════════════════════════════
-- FREQUENT MEALS MIGRATION
-- Creates the frequent_meals table and upsert function
-- ═══════════════════════════════════════════════════════════════

-- 1. CREATE FREQUENT MEALS TABLE
CREATE TABLE IF NOT EXISTS frequent_meals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users ON DELETE CASCADE,

    -- Meal info
    name TEXT NOT NULL,
    meal_type TEXT CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack')),

    -- Items stored as JSONB array
    items JSONB NOT NULL DEFAULT '[]'::jsonb,

    -- Cached totals for quick display
    total_calories FLOAT DEFAULT 0,
    total_protein FLOAT DEFAULT 0,
    total_carbs FLOAT DEFAULT 0,
    total_fat FLOAT DEFAULT 0,

    -- Usage tracking
    usage_count INT DEFAULT 1,
    last_used_at TIMESTAMPTZ DEFAULT NOW(),

    -- Favorites
    is_favorite BOOLEAN DEFAULT false,
    is_custom BOOLEAN DEFAULT false,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),

    -- Unique constraint: one meal name per user
    UNIQUE(user_id, name)
);

-- 2. CREATE INDEX FOR FAST LOOKUPS
CREATE INDEX IF NOT EXISTS idx_frequent_meals_user_id ON frequent_meals(user_id);
CREATE INDEX IF NOT EXISTS idx_frequent_meals_is_favorite ON frequent_meals(user_id, is_favorite);
CREATE INDEX IF NOT EXISTS idx_frequent_meals_usage ON frequent_meals(user_id, usage_count DESC);

-- 3. CREATE FREQUENT ADDONS TABLE (for tracking meal combinations)
CREATE TABLE IF NOT EXISTS frequent_addons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users ON DELETE CASCADE,

    -- The base meal this addon goes with
    base_meal_name TEXT NOT NULL,

    -- Addon details
    addon_name TEXT NOT NULL,
    addon_calories FLOAT DEFAULT 0,
    addon_protein FLOAT DEFAULT 0,
    addon_carbs FLOAT DEFAULT 0,
    addon_fat FLOAT DEFAULT 0,

    -- How often this combination is used
    combination_count INT DEFAULT 1,
    last_used_at TIMESTAMPTZ DEFAULT NOW(),

    created_at TIMESTAMPTZ DEFAULT NOW(),

    -- Unique per user + base meal + addon
    UNIQUE(user_id, base_meal_name, addon_name)
);

CREATE INDEX IF NOT EXISTS idx_frequent_addons_user_base ON frequent_addons(user_id, base_meal_name);

-- 4. DROP EXISTING FUNCTION (if exists with different signature)
DROP FUNCTION IF EXISTS upsert_frequent_meal(UUID, TEXT, TEXT, JSONB, FLOAT, FLOAT, FLOAT, FLOAT);
DROP FUNCTION IF EXISTS upsert_frequent_meal(UUID, TEXT, TEXT, JSONB, FLOAT, FLOAT, FLOAT, FLOAT, BOOLEAN);

-- 4. UPSERT FUNCTION - Creates or updates a frequent meal
CREATE OR REPLACE FUNCTION upsert_frequent_meal(
    p_user_id UUID,
    p_name TEXT,
    p_meal_type TEXT,
    p_items JSONB,
    p_total_calories FLOAT,
    p_total_protein FLOAT,
    p_total_carbs FLOAT,
    p_total_fat FLOAT
) RETURNS UUID AS $$
DECLARE
    v_meal_id UUID;
BEGIN
    -- Try to find existing meal
    SELECT id INTO v_meal_id
    FROM frequent_meals
    WHERE user_id = p_user_id AND name = p_name;

    IF v_meal_id IS NOT NULL THEN
        -- Update existing meal
        UPDATE frequent_meals
        SET
            items = p_items,
            total_calories = p_total_calories,
            total_protein = p_total_protein,
            total_carbs = p_total_carbs,
            total_fat = p_total_fat,
            usage_count = usage_count + 1,
            last_used_at = NOW()
        WHERE id = v_meal_id;
    ELSE
        -- Insert new meal
        INSERT INTO frequent_meals (
            user_id, name, meal_type, items,
            total_calories, total_protein, total_carbs, total_fat,
            usage_count, last_used_at
        ) VALUES (
            p_user_id, p_name, p_meal_type, p_items,
            p_total_calories, p_total_protein, p_total_carbs, p_total_fat,
            1, NOW()
        )
        RETURNING id INTO v_meal_id;
    END IF;

    RETURN v_meal_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. DISABLE RLS FOR NOW (can enable later with proper policies)
ALTER TABLE frequent_meals DISABLE ROW LEVEL SECURITY;
ALTER TABLE frequent_addons DISABLE ROW LEVEL SECURITY;

-- 6. GRANT PERMISSIONS
GRANT ALL ON frequent_meals TO authenticated;
GRANT ALL ON frequent_addons TO authenticated;
GRANT EXECUTE ON FUNCTION upsert_frequent_meal TO authenticated;

-- Done!
-- Run this migration in Supabase SQL Editor
