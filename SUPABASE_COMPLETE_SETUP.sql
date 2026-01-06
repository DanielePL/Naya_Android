-- ═══════════════════════════════════════════════════════════════════════════
-- MENOTRACKER - COMPLETE DATABASE SETUP
-- ═══════════════════════════════════════════════════════════════════════════
-- Run this entire script in Supabase SQL Editor to set up all tables
-- ═══════════════════════════════════════════════════════════════════════════

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ═══════════════════════════════════════════════════════════════════════════
-- 1. USER PROFILES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL DEFAULT 'Member',
    weight NUMERIC,
    height NUMERIC,
    age INTEGER,
    training_experience INTEGER,
    personal_records JSONB DEFAULT '{}'::jsonb,
    medical_conditions JSONB DEFAULT '[]'::jsonb,
    injuries JSONB DEFAULT '[]'::jsonb,
    goals TEXT[] DEFAULT '{}',
    preferred_sports TEXT[] DEFAULT '{}',
    target_workout_duration INTEGER,
    last_seen BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_id ON user_profiles(id);

ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own profile" ON user_profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users can create their own profile" ON user_profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Users can update their own profile" ON user_profiles FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Users can delete their own profile" ON user_profiles FOR DELETE USING (auth.uid() = id);

-- Auto-create profile on signup
CREATE OR REPLACE FUNCTION create_default_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_profiles (id, name)
    VALUES (NEW.id, 'Member')
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION create_default_user_profile();

-- ═══════════════════════════════════════════════════════════════════════════
-- 2. EXERCISES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS exercises (
    id TEXT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    name TEXT NOT NULL,
    main_muscle_group TEXT,
    secondary_muscle_groups TEXT[],
    equipment TEXT[],
    tempo TEXT,
    rest_time_seconds INTEGER,
    track_reps BOOLEAN DEFAULT true,
    track_sets BOOLEAN DEFAULT true,
    track_weight BOOLEAN DEFAULT true,
    track_rpe BOOLEAN DEFAULT false,
    track_duration BOOLEAN DEFAULT false,
    track_distance BOOLEAN DEFAULT false,
    video_url TEXT,
    tutorial TEXT,
    notes TEXT,
    supports_power_score BOOLEAN DEFAULT false,
    supports_technique_score BOOLEAN DEFAULT false,
    vbt_measurement_type TEXT,
    vbt_category TEXT,
    sports TEXT[]
);

CREATE INDEX IF NOT EXISTS idx_exercises_name ON exercises(name);
CREATE INDEX IF NOT EXISTS idx_exercises_main_muscle ON exercises(main_muscle_group);

ALTER TABLE exercises ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view exercises" ON exercises FOR SELECT USING (true);
CREATE POLICY "Authenticated users can insert exercises" ON exercises FOR INSERT WITH CHECK (true);
CREATE POLICY "Authenticated users can update exercises" ON exercises FOR UPDATE USING (true);
CREATE POLICY "Authenticated users can delete exercises" ON exercises FOR DELETE USING (true);

-- ═══════════════════════════════════════════════════════════════════════════
-- 3. WORKOUT SESSIONS & SETS
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS workout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    workout_name TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    duration_minutes INTEGER,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workout_sessions_user_id ON workout_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_started_at ON workout_sessions(started_at DESC);

ALTER TABLE workout_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own workout sessions" ON workout_sessions FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can create their own workout sessions" ON workout_sessions FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own workout sessions" ON workout_sessions FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own workout sessions" ON workout_sessions FOR DELETE USING (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS workout_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id TEXT NOT NULL,
    set_number INTEGER NOT NULL,
    reps INTEGER,
    weight_kg NUMERIC,
    duration_seconds INTEGER,
    distance_meters NUMERIC,
    rpe NUMERIC,
    rest_seconds INTEGER,
    video_url TEXT,
    video_storage_type TEXT DEFAULT 'device',
    video_thumbnail_url TEXT,
    video_uploaded_at TIMESTAMPTZ,
    velocity_metrics JSONB,
    completed_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workout_sets_session_id ON workout_sets(session_id);
CREATE INDEX IF NOT EXISTS idx_workout_sets_exercise_id ON workout_sets(exercise_id);

ALTER TABLE workout_sets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own workout sets" ON workout_sets FOR SELECT
USING (EXISTS (SELECT 1 FROM workout_sessions WHERE workout_sessions.id = workout_sets.session_id AND workout_sessions.user_id = auth.uid()));

CREATE POLICY "Users can create their own workout sets" ON workout_sets FOR INSERT
WITH CHECK (EXISTS (SELECT 1 FROM workout_sessions WHERE workout_sessions.id = workout_sets.session_id AND workout_sessions.user_id = auth.uid()));

CREATE POLICY "Users can update their own workout sets" ON workout_sets FOR UPDATE
USING (EXISTS (SELECT 1 FROM workout_sessions WHERE workout_sessions.id = workout_sets.session_id AND workout_sessions.user_id = auth.uid()));

CREATE POLICY "Users can delete their own workout sets" ON workout_sets FOR DELETE
USING (EXISTS (SELECT 1 FROM workout_sessions WHERE workout_sessions.id = workout_sets.session_id AND workout_sessions.user_id = auth.uid()));

-- ═══════════════════════════════════════════════════════════════════════════
-- 4. WORKOUT TEMPLATES
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS workout_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_workout_templates_user_id ON workout_templates(user_id);

ALTER TABLE workout_templates ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own and public workout templates" ON workout_templates FOR SELECT
USING (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Users can insert their own workout templates" ON workout_templates FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own workout templates" ON workout_templates FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own workout templates" ON workout_templates FOR DELETE
USING (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- 5. NUTRITION
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS foods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users,
    name TEXT NOT NULL,
    brand TEXT,
    serving_size FLOAT NOT NULL,
    serving_unit TEXT DEFAULT 'g',
    calories FLOAT NOT NULL,
    protein FLOAT NOT NULL,
    carbs FLOAT NOT NULL,
    fat FLOAT NOT NULL,
    fiber FLOAT DEFAULT 0,
    sugar FLOAT DEFAULT 0,
    category TEXT,
    is_public BOOLEAN DEFAULT false,
    is_favorite BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE foods ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view public and own foods" ON foods FOR SELECT USING (is_public = true OR auth.uid() = user_id);
CREATE POLICY "Users can insert their own foods" ON foods FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own foods" ON foods FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own foods" ON foods FOR DELETE USING (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS nutrition_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users ON DELETE CASCADE,
    date DATE NOT NULL,
    target_calories FLOAT,
    target_protein FLOAT,
    target_carbs FLOAT,
    target_fat FLOAT,
    workout_session_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, date)
);

ALTER TABLE nutrition_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own nutrition logs" ON nutrition_logs FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own nutrition logs" ON nutrition_logs FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own nutrition logs" ON nutrition_logs FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own nutrition logs" ON nutrition_logs FOR DELETE USING (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS meals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nutrition_log_id UUID NOT NULL REFERENCES nutrition_logs ON DELETE CASCADE,
    meal_type TEXT NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack')),
    meal_name TEXT,
    time TIMESTAMPTZ DEFAULT NOW(),
    photo_url TEXT,
    ai_analysis_id TEXT,
    ai_confidence FLOAT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE meals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own meals" ON meals FOR SELECT
USING (EXISTS (SELECT 1 FROM nutrition_logs WHERE nutrition_logs.id = meals.nutrition_log_id AND nutrition_logs.user_id = auth.uid()));

CREATE POLICY "Users can insert own meals" ON meals FOR INSERT
WITH CHECK (EXISTS (SELECT 1 FROM nutrition_logs WHERE nutrition_logs.id = meals.nutrition_log_id AND nutrition_logs.user_id = auth.uid()));

CREATE POLICY "Users can update own meals" ON meals FOR UPDATE
USING (EXISTS (SELECT 1 FROM nutrition_logs WHERE nutrition_logs.id = meals.nutrition_log_id AND nutrition_logs.user_id = auth.uid()));

CREATE POLICY "Users can delete own meals" ON meals FOR DELETE
USING (EXISTS (SELECT 1 FROM nutrition_logs WHERE nutrition_logs.id = meals.nutrition_log_id AND nutrition_logs.user_id = auth.uid()));

CREATE TABLE IF NOT EXISTS meal_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    meal_id UUID NOT NULL REFERENCES meals ON DELETE CASCADE,
    food_id UUID REFERENCES foods,
    item_name TEXT NOT NULL,
    quantity FLOAT NOT NULL,
    quantity_unit TEXT DEFAULT 'g',
    calories FLOAT NOT NULL,
    protein FLOAT NOT NULL,
    carbs FLOAT NOT NULL,
    fat FLOAT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE meal_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own meal items" ON meal_items FOR SELECT
USING (EXISTS (SELECT 1 FROM meals JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id WHERE meals.id = meal_items.meal_id AND nutrition_logs.user_id = auth.uid()));

CREATE POLICY "Users can insert own meal items" ON meal_items FOR INSERT
WITH CHECK (EXISTS (SELECT 1 FROM meals JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id WHERE meals.id = meal_items.meal_id AND nutrition_logs.user_id = auth.uid()));

CREATE POLICY "Users can update own meal items" ON meal_items FOR UPDATE
USING (EXISTS (SELECT 1 FROM meals JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id WHERE meals.id = meal_items.meal_id AND nutrition_logs.user_id = auth.uid()));

CREATE POLICY "Users can delete own meal items" ON meal_items FOR DELETE
USING (EXISTS (SELECT 1 FROM meals JOIN nutrition_logs ON nutrition_logs.id = meals.nutrition_log_id WHERE meals.id = meal_items.meal_id AND nutrition_logs.user_id = auth.uid()));

CREATE TABLE IF NOT EXISTS nutrition_goals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users ON DELETE CASCADE,
    goal_type TEXT NOT NULL CHECK (goal_type IN ('cutting', 'bulking', 'maintenance', 'performance')),
    target_calories FLOAT NOT NULL,
    target_protein FLOAT NOT NULL,
    target_carbs FLOAT NOT NULL,
    target_fat FLOAT NOT NULL,
    meals_per_day INT DEFAULT 3,
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE nutrition_goals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own nutrition goals" ON nutrition_goals FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own nutrition goals" ON nutrition_goals FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own nutrition goals" ON nutrition_goals FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own nutrition goals" ON nutrition_goals FOR DELETE USING (auth.uid() = user_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- 6. STORAGE BUCKETS
-- ═══════════════════════════════════════════════════════════════════════════

-- Profile Images Bucket
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('profile-images', 'profile-images', true, 5242880, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO NOTHING;

-- Workout Videos Bucket
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('workout-videos', 'workout-videos', false, 52428800, ARRAY['video/mp4', 'video/quicktime'])
ON CONFLICT (id) DO NOTHING;

-- Meal Photos Bucket
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('meal-photos', 'meal-photos', false, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO NOTHING;

-- Storage Policies
CREATE POLICY "Users can upload profile images" ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'profile-images' AND auth.uid()::text = (storage.foldername(name))[1]);

CREATE POLICY "Anyone can view profile images" ON storage.objects FOR SELECT
USING (bucket_id = 'profile-images');

CREATE POLICY "Users can upload workout videos" ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'workout-videos' AND auth.uid()::text = (storage.foldername(name))[1]);

CREATE POLICY "Users can view own workout videos" ON storage.objects FOR SELECT
USING (bucket_id = 'workout-videos' AND auth.uid()::text = (storage.foldername(name))[1]);

CREATE POLICY "Users can delete own workout videos" ON storage.objects FOR DELETE
USING (bucket_id = 'workout-videos' AND auth.uid()::text = (storage.foldername(name))[1]);

CREATE POLICY "Users can upload meal photos" ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'meal-photos' AND auth.uid()::text = (storage.foldername(name))[1]);

CREATE POLICY "Users can view own meal photos" ON storage.objects FOR SELECT
USING (bucket_id = 'meal-photos' AND auth.uid()::text = (storage.foldername(name))[1]);

-- ═══════════════════════════════════════════════════════════════════════════
-- 7. SAMPLE EXERCISES (Basic Library)
-- ═══════════════════════════════════════════════════════════════════════════

INSERT INTO exercises (id, name, main_muscle_group, secondary_muscle_groups, equipment, supports_power_score, supports_technique_score)
VALUES
    ('squat-back-barbell', 'Barbell Back Squat', 'Quadriceps', ARRAY['Glutes', 'Hamstrings'], ARRAY['Barbell'], true, true),
    ('bench-press-barbell', 'Barbell Bench Press', 'Chest', ARRAY['Triceps', 'Shoulders'], ARRAY['Barbell'], true, true),
    ('deadlift-conventional', 'Conventional Deadlift', 'Back', ARRAY['Glutes', 'Hamstrings'], ARRAY['Barbell'], true, true),
    ('row-barbell-bent', 'Bent Over Barbell Row', 'Back', ARRAY['Biceps'], ARRAY['Barbell'], true, true),
    ('press-overhead-barbell', 'Barbell Overhead Press', 'Shoulders', ARRAY['Triceps'], ARRAY['Barbell'], true, true),
    ('lunge-dumbbell', 'Dumbbell Lunges', 'Quadriceps', ARRAY['Glutes', 'Hamstrings'], ARRAY['Dumbbells'], false, false),
    ('curl-bicep-dumbbell', 'Dumbbell Bicep Curl', 'Biceps', ARRAY[]::TEXT[], ARRAY['Dumbbells'], false, false),
    ('extension-tricep-cable', 'Cable Tricep Extension', 'Triceps', ARRAY[]::TEXT[], ARRAY['Cable'], false, false),
    ('plank', 'Plank', 'Core', ARRAY['Shoulders'], ARRAY['Bodyweight'], false, false),
    ('pull-up', 'Pull Up', 'Back', ARRAY['Biceps'], ARRAY['Bodyweight'], false, true)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════
-- DONE!
-- ═══════════════════════════════════════════════════════════════════════════

DO $$
BEGIN
    RAISE NOTICE '✅ Menotracker database setup complete!';
    RAISE NOTICE '📊 Tables created: user_profiles, exercises, workout_sessions, workout_sets, workout_templates, nutrition tables';
    RAISE NOTICE '🗂️ Storage buckets: profile-images, workout-videos, meal-photos';
    RAISE NOTICE '🔒 Row Level Security enabled on all tables';
END $$;
