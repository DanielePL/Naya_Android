-- ═══════════════════════════════════════════════════════════════
-- PROMETHEUS NUTRITION TRACKER - DATABASE SCHEMA
-- ═══════════════════════════════════════════════════════════════

-- 1. FOODS TABLE (Food Database)
CREATE TABLE IF NOT EXISTS foods (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users,

  -- Basic Info
  name TEXT NOT NULL,
  brand TEXT,

  -- Serving Size
  serving_size FLOAT NOT NULL,
  serving_unit TEXT DEFAULT 'g',

  -- Macros (per serving)
  calories FLOAT NOT NULL,
  protein FLOAT NOT NULL,
  carbs FLOAT NOT NULL,
  fat FLOAT NOT NULL,
  fiber FLOAT DEFAULT 0,
  sugar FLOAT DEFAULT 0,

  -- Meta
  category TEXT,
  is_public BOOLEAN DEFAULT false,
  is_favorite BOOLEAN DEFAULT false,

  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. NUTRITION LOGS TABLE (Daily Log)
CREATE TABLE IF NOT EXISTS nutrition_logs (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES auth.users ON DELETE CASCADE,
  date DATE NOT NULL,

  -- Daily Targets
  target_calories FLOAT,
  target_protein FLOAT,
  target_carbs FLOAT,
  target_fat FLOAT,

  -- Link to workout (optional)
  workout_session_id UUID,

  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),

  -- Unique constraint: one log per user per day
  UNIQUE(user_id, date)
);

-- 3. MEALS TABLE (Breakfast, Lunch, Dinner, Snacks)
CREATE TABLE IF NOT EXISTS meals (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nutrition_log_id UUID NOT NULL REFERENCES nutrition_logs ON DELETE CASCADE,

  -- Meal Info
  meal_type TEXT NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack')),
  meal_name TEXT,
  time TIMESTAMPTZ DEFAULT NOW(),

  -- Photo (optional)
  photo_url TEXT,

  -- AI Analysis (if from photo)
  ai_analysis_id TEXT,
  ai_confidence FLOAT,

  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. MEAL ITEMS TABLE (Individual foods in a meal)
CREATE TABLE IF NOT EXISTS meal_items (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  meal_id UUID NOT NULL REFERENCES meals ON DELETE CASCADE,

  -- Food reference (optional - can be AI-estimated without food_id)
  food_id UUID REFERENCES foods,

  -- Item Details
  item_name TEXT NOT NULL,
  quantity FLOAT NOT NULL,
  quantity_unit TEXT DEFAULT 'g',

  -- Nutritional Values (denormalized for performance)
  calories FLOAT NOT NULL,
  protein FLOAT NOT NULL,
  carbs FLOAT NOT NULL,
  fat FLOAT NOT NULL,

  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. MEAL TEMPLATES TABLE (Saved frequent meals)
CREATE TABLE IF NOT EXISTS meal_templates (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES auth.users ON DELETE CASCADE,

  name TEXT NOT NULL,
  meal_type TEXT CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack')),
  is_favorite BOOLEAN DEFAULT false,

  -- Cached totals
  total_calories FLOAT,
  total_protein FLOAT,
  total_carbs FLOAT,
  total_fat FLOAT,

  -- Template items (JSON array)
  items JSONB NOT NULL,

  photo_url TEXT,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. NUTRITION GOALS TABLE (User targets)
CREATE TABLE IF NOT EXISTS nutrition_goals (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES auth.users ON DELETE CASCADE,

  -- Goal Type
  goal_type TEXT NOT NULL CHECK (goal_type IN ('cutting', 'bulking', 'maintenance', 'performance')),

  -- Daily Targets
  target_calories FLOAT NOT NULL,
  target_protein FLOAT NOT NULL,
  target_carbs FLOAT NOT NULL,
  target_fat FLOAT NOT NULL,

  -- Settings
  meals_per_day INT DEFAULT 3,

  -- Active Period
  start_date DATE,
  end_date DATE,
  is_active BOOLEAN DEFAULT true,

  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ═══════════════════════════════════════════════════════════════
-- INDEXES (for performance)
-- ═══════════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_foods_user_id ON foods(user_id);
CREATE INDEX IF NOT EXISTS idx_foods_name ON foods(name);
CREATE INDEX IF NOT EXISTS idx_foods_is_favorite ON foods(user_id, is_favorite) WHERE is_favorite = true;

CREATE INDEX IF NOT EXISTS idx_nutrition_logs_user_date ON nutrition_logs(user_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_nutrition_logs_date ON nutrition_logs(date DESC);

CREATE INDEX IF NOT EXISTS idx_meals_log_id ON meals(nutrition_log_id);
CREATE INDEX IF NOT EXISTS idx_meals_type ON meals(meal_type);

CREATE INDEX IF NOT EXISTS idx_meal_items_meal_id ON meal_items(meal_id);

CREATE INDEX IF NOT EXISTS idx_meal_templates_user_id ON meal_templates(user_id);
CREATE INDEX IF NOT EXISTS idx_meal_templates_favorite ON meal_templates(user_id, is_favorite) WHERE is_favorite = true;

CREATE INDEX IF NOT EXISTS idx_nutrition_goals_user_active ON nutrition_goals(user_id, is_active) WHERE is_active = true;

-- Partial unique index: Only one active goal per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_nutrition_goals_user_active_unique ON nutrition_goals(user_id) WHERE is_active = true;

-- ═══════════════════════════════════════════════════════════════
-- ROW LEVEL SECURITY (RLS)
-- ═══════════════════════════════════════════════════════════════

-- Enable RLS on all tables
ALTER TABLE foods ENABLE ROW LEVEL SECURITY;
ALTER TABLE nutrition_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE meals ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE nutrition_goals ENABLE ROW LEVEL SECURITY;

-- FOODS: Users can see public foods + their own
DROP POLICY IF EXISTS "Users can view public and own foods" ON foods;
CREATE POLICY "Users can view public and own foods" ON foods
  FOR SELECT USING (is_public = true OR auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert their own foods" ON foods;
CREATE POLICY "Users can insert their own foods" ON foods
  FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update their own foods" ON foods;
CREATE POLICY "Users can update their own foods" ON foods
  FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete their own foods" ON foods;
CREATE POLICY "Users can delete their own foods" ON foods
  FOR DELETE USING (auth.uid() = user_id);

-- NUTRITION LOGS: Users can only see their own
DROP POLICY IF EXISTS "Users can view own nutrition logs" ON nutrition_logs;
CREATE POLICY "Users can view own nutrition logs" ON nutrition_logs
  FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own nutrition logs" ON nutrition_logs;
CREATE POLICY "Users can insert own nutrition logs" ON nutrition_logs
  FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own nutrition logs" ON nutrition_logs;
CREATE POLICY "Users can update own nutrition logs" ON nutrition_logs
  FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own nutrition logs" ON nutrition_logs;
CREATE POLICY "Users can delete own nutrition logs" ON nutrition_logs
  FOR DELETE USING (auth.uid() = user_id);

-- MEALS: Users can only see meals from their own logs
DROP POLICY IF EXISTS "Users can view own meals" ON meals;
CREATE POLICY "Users can view own meals" ON meals
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM nutrition_logs
      WHERE nutrition_logs.id = meals.nutrition_log_id
      AND nutrition_logs.user_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS "Users can insert own meals" ON meals;
CREATE POLICY "Users can insert own meals" ON meals
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM nutrition_logs
      WHERE nutrition_logs.id = meals.nutrition_log_id
      AND nutrition_logs.user_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS "Users can update own meals" ON meals;
CREATE POLICY "Users can update own meals" ON meals
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM nutrition_logs
      WHERE nutrition_logs.id = meals.nutrition_log_id
      AND nutrition_logs.user_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS "Users can delete own meals" ON meals;
CREATE POLICY "Users can delete own meals" ON meals
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM nutrition_logs
      WHERE nutrition_logs.id = meals.nutrition_log_id
      AND nutrition_logs.user_id = auth.uid()
    )
  );

-- MEAL ITEMS: Users can only see items from their own meals
DROP POLICY IF EXISTS "Users can view own meal items" ON meal_items;
CREATE POLICY "Users can view own meal items" ON meal_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM meals
      JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
      WHERE meals.id = meal_items.meal_id
      AND nutrition_logs.user_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS "Users can insert own meal items" ON meal_items;
CREATE POLICY "Users can insert own meal items" ON meal_items
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM meals
      JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
      WHERE meals.id = meal_items.meal_id
      AND nutrition_logs.user_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS "Users can update own meal items" ON meal_items;
CREATE POLICY "Users can update own meal items" ON meal_items
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM meals
      JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
      WHERE meals.id = meal_items.meal_id
      AND nutrition_logs.user_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS "Users can delete own meal items" ON meal_items;
CREATE POLICY "Users can delete own meal items" ON meal_items
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM meals
      JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id
      WHERE meals.id = meal_items.meal_id
      AND nutrition_logs.user_id = auth.uid()
    )
  );

-- MEAL TEMPLATES: Users can only see their own
DROP POLICY IF EXISTS "Users can view own meal templates" ON meal_templates;
CREATE POLICY "Users can view own meal templates" ON meal_templates
  FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own meal templates" ON meal_templates;
CREATE POLICY "Users can insert own meal templates" ON meal_templates
  FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own meal templates" ON meal_templates;
CREATE POLICY "Users can update own meal templates" ON meal_templates
  FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own meal templates" ON meal_templates;
CREATE POLICY "Users can delete own meal templates" ON meal_templates
  FOR DELETE USING (auth.uid() = user_id);

-- NUTRITION GOALS: Users can only see their own
DROP POLICY IF EXISTS "Users can view own nutrition goals" ON nutrition_goals;
CREATE POLICY "Users can view own nutrition goals" ON nutrition_goals
  FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own nutrition goals" ON nutrition_goals;
CREATE POLICY "Users can insert own nutrition goals" ON nutrition_goals
  FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own nutrition goals" ON nutrition_goals;
CREATE POLICY "Users can update own nutrition goals" ON nutrition_goals
  FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own nutrition goals" ON nutrition_goals;
CREATE POLICY "Users can delete own nutrition goals" ON nutrition_goals
  FOR DELETE USING (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════
-- HELPER FUNCTIONS
-- ═══════════════════════════════════════════════════════════════

-- Function: Get or create today's nutrition log
CREATE OR REPLACE FUNCTION get_or_create_nutrition_log(p_user_id UUID, p_date DATE)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_log_id UUID;
  v_goal_calories FLOAT;
  v_goal_protein FLOAT;
  v_goal_carbs FLOAT;
  v_goal_fat FLOAT;
BEGIN
  -- Try to get existing log
  SELECT id INTO v_log_id
  FROM nutrition_logs
  WHERE user_id = p_user_id AND date = p_date;

  -- If not found, create new
  IF v_log_id IS NULL THEN
    -- Get user's active nutrition goals
    SELECT target_calories, target_protein, target_carbs, target_fat
    INTO v_goal_calories, v_goal_protein, v_goal_carbs, v_goal_fat
    FROM nutrition_goals
    WHERE user_id = p_user_id AND is_active = true
    LIMIT 1;

    -- Create log with goals
    INSERT INTO nutrition_logs (user_id, date, target_calories, target_protein, target_carbs, target_fat)
    VALUES (p_user_id, p_date, v_goal_calories, v_goal_protein, v_goal_carbs, v_goal_fat)
    RETURNING id INTO v_log_id;
  END IF;

  RETURN v_log_id;
END;
$$;

-- Function: Calculate daily totals for a nutrition log
CREATE OR REPLACE FUNCTION calculate_nutrition_log_totals(p_log_id UUID)
RETURNS TABLE(
  total_calories FLOAT,
  total_protein FLOAT,
  total_carbs FLOAT,
  total_fat FLOAT
)
LANGUAGE sql
STABLE
AS $$
  SELECT
    COALESCE(SUM(mi.calories), 0) as total_calories,
    COALESCE(SUM(mi.protein), 0) as total_protein,
    COALESCE(SUM(mi.carbs), 0) as total_carbs,
    COALESCE(SUM(mi.fat), 0) as total_fat
  FROM meal_items mi
  JOIN meals m ON m.id = mi.meal_id
  WHERE m.nutrition_log_id = p_log_id;
$$;

-- ═══════════════════════════════════════════════════════════════
-- DONE! Run this SQL in Supabase SQL Editor
-- ═══════════════════════════════════════════════════════════════