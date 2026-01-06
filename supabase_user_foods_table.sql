-- ═══════════════════════════════════════════════════════════════════════════════
-- USER FOODS TABLE
-- Personal food library with modifications (e.g., "Matcha Latte Oat Milk No Sugar")
-- ═══════════════════════════════════════════════════════════════════════════════

-- Create the user_foods table
CREATE TABLE IF NOT EXISTS user_foods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,

    -- Origin tracking
    base_food_id TEXT,                    -- Original food ID from search (usda_123, rest_456)
    base_food_source TEXT,                -- "USDA", "RESTAURANT", "OPEN_FOOD_FACTS", etc.

    -- Display info
    display_name TEXT NOT NULL,           -- Full name with modifications: "Starbucks Matcha Latte Oat"
    base_name TEXT NOT NULL,              -- Original name: "Matcha Latte"
    brand TEXT,                           -- "Starbucks", "McDonald's", etc.
    emoji TEXT,                           -- For quick recognition
    category TEXT,                        -- "Coffee", "Fast Food", "Meal Prep"

    -- Modifications (JSON array)
    modifications JSONB DEFAULT '[]',     -- [{category: "milk", value: "oat", displayLabel: "Oat Milk"}]

    -- Nutritional info (per 100g or per serving)
    is_per_serving BOOLEAN DEFAULT FALSE, -- true = values are per serving
    calories REAL NOT NULL,
    protein REAL NOT NULL,
    carbs REAL NOT NULL,
    fat REAL NOT NULL,
    fiber REAL DEFAULT 0,
    sugar REAL DEFAULT 0,
    saturated_fat REAL DEFAULT 0,
    unsaturated_fat REAL DEFAULT 0,
    trans_fat REAL DEFAULT 0,
    omega_3 REAL DEFAULT 0,
    omega_6 REAL DEFAULT 0,
    cholesterol REAL DEFAULT 0,
    sodium REAL DEFAULT 0,
    potassium REAL DEFAULT 0,
    calcium REAL DEFAULT 0,
    iron REAL DEFAULT 0,
    magnesium REAL DEFAULT 0,

    -- Serving info
    default_serving_size REAL DEFAULT 100,
    default_serving_unit TEXT DEFAULT 'g',
    serving_description TEXT,             -- "1 Grande (473ml)", "1 Portion"
    portions JSONB DEFAULT '[]',          -- [{name: "Grande", amount: 473, unit: "ml"}]

    -- Usage tracking
    use_count INT DEFAULT 1,              -- How many times this was tracked
    last_used_at BIGINT,                  -- Unix timestamp
    is_favorite BOOLEAN DEFAULT FALSE,

    -- NOVA classification
    nova_classification INT,              -- 1-4 food processing score

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes for fast queries
CREATE INDEX IF NOT EXISTS idx_user_foods_user_id ON user_foods(user_id);
CREATE INDEX IF NOT EXISTS idx_user_foods_last_used ON user_foods(user_id, last_used_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_foods_use_count ON user_foods(user_id, use_count DESC);
CREATE INDEX IF NOT EXISTS idx_user_foods_favorites ON user_foods(user_id, is_favorite) WHERE is_favorite = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_foods_display_name ON user_foods(display_name);
CREATE INDEX IF NOT EXISTS idx_user_foods_base_name ON user_foods(base_name);

-- Full text search index
CREATE INDEX IF NOT EXISTS idx_user_foods_search ON user_foods
    USING gin(to_tsvector('english', coalesce(display_name, '') || ' ' || coalesce(base_name, '') || ' ' || coalesce(brand, '')));

-- Row Level Security
ALTER TABLE user_foods ENABLE ROW LEVEL SECURITY;

-- Users can only see/modify their own foods
CREATE POLICY "Users can view own foods" ON user_foods
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own foods" ON user_foods
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own foods" ON user_foods
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own foods" ON user_foods
    FOR DELETE USING (auth.uid() = user_id);

-- Function to increment use_count atomically
CREATE OR REPLACE FUNCTION increment_food_use_count(food_id UUID)
RETURNS void AS $$
BEGIN
    UPDATE user_foods
    SET use_count = use_count + 1,
        last_used_at = EXTRACT(EPOCH FROM NOW()) * 1000,
        updated_at = NOW()
    WHERE id = food_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to update updated_at
CREATE OR REPLACE FUNCTION update_user_foods_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_foods_updated_at
    BEFORE UPDATE ON user_foods
    FOR EACH ROW
    EXECUTE FUNCTION update_user_foods_updated_at();

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON user_foods TO authenticated;
GRANT EXECUTE ON FUNCTION increment_food_use_count TO authenticated;

-- ═══════════════════════════════════════════════════════════════════════════════
-- EXAMPLE MODIFICATIONS JSON FORMAT
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- modifications: [
--   {
--     "category": "milk",
--     "value": "oat",
--     "display_label": "Oat Milk",
--     "short_label": "Oat",
--     "calorie_adjustment": 20,
--     "protein_adjustment": 0,
--     "carb_adjustment": 4,
--     "fat_adjustment": 0
--   },
--   {
--     "category": "sweetener",
--     "value": "none",
--     "display_label": "No Sugar",
--     "short_label": "No Sugar",
--     "calorie_adjustment": -40,
--     "protein_adjustment": 0,
--     "carb_adjustment": -10,
--     "fat_adjustment": 0
--   }
-- ]
--
-- ═══════════════════════════════════════════════════════════════════════════════
