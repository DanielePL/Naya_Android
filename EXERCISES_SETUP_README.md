# Fix: Library Connection to Supabase - No Exercises Showing

## Problem
The app's library screen is showing no exercises because the `exercises` table either doesn't exist or has an incorrect schema in your Supabase database.

## Solution

### Quick Fix (Recommended) ⚠️ THIS WILL DELETE EXISTING DATA
Run the RESET script to recreate the table with correct schema:

1. **Open Supabase Dashboard**
   - Go to https://zzluhirmmnkfkifriult.supabase.co
   - Log in to your project

2. **Navigate to SQL Editor**
   - Click on "SQL Editor" in the left sidebar
   - Click "New Query"

3. **Run the RESET Script** ⭐ **USE THIS ONE**
   - Open the file: `supabase_exercises_RESET_AND_SETUP.sql`
   - Copy the entire contents
   - Paste into the SQL Editor
   - Click **Run** button

   **Note:** This script will:
   - DROP the existing `exercises` table (if it exists)
   - Create a new table with the correct schema
   - Populate it with 24 initial exercises

4. **Verify**
   - You should see a success message showing the number of exercises inserted
   - The exercises table will be created, populated, and ready to use

5. **Restart Your App**
   - Close and reopen the Prometheus app
   - Navigate to the Library screen
   - You should now see 24 exercises organized by sport categories

---

## Alternative: Step-by-Step Setup

If you prefer to run each step separately:

### Step 1: Create the Table
Run `supabase_exercises_schema.sql` in the SQL Editor

### Step 2: Add Exercises
Run `supabase_exercises_seed.sql` in the SQL Editor

### Step 3: Disable RLS (for testing)
Run `supabase_exercises_disable_rls.sql` in the SQL Editor

---

## What This Does

### Creates the `exercises` Table
The table stores all exercises with:
- Basic info (name, muscle groups, equipment)
- Tracking options (reps, sets, weight, RPE, duration, distance)
- VBT (Velocity-Based Training) support
- Sport categorization (Powerlifting, Weightlifting, CrossFit, Hyrox, etc.)

### Populates with 24 Initial Exercises
Organized by muscle groups:
- **Legs** (7 exercises): Front Squat, Back Squat, Romanian Deadlift, etc.
- **Shoulders** (3 exercises): Dumbbell Press, Overhead Press, Lateral Raises
- **Back** (5 exercises): Deadlift, Rows, Pulldowns, etc.
- **Chest** (4 exercises): Bench Press, Incline Press, Dips, Flyes
- **Arms** (2 exercises): Tricep Pushdowns, Close-Grip Bench Press
- **Core** (2 exercises): Plank, Russian Twists
- **Olympic** (1 exercise): Power Clean

### Disables Row Level Security
Allows the app to access exercises without authentication (temporary for testing)

---

## Troubleshooting

### Still Not Seeing Exercises?

1. **Check the table exists:**
   ```sql
   SELECT * FROM exercises LIMIT 5;
   ```
   Run this in Supabase SQL Editor. You should see 5 exercises.

2. **Verify RLS is disabled:**
   ```sql
   SELECT tablename, rowsecurity
   FROM pg_tables
   WHERE tablename = 'exercises';
   ```
   The `rowsecurity` column should be `false`.

3. **Check app logs:**
   - Run the app in Android Studio
   - Open Logcat
   - Filter by tag: `ExerciseRepository`
   - Look for error messages

4. **VSupabase credentials:**
   - Check `Prometheus_V1/build.gradle.kts`
   - Ensure `SUPABASE_URL` and `SUPABASE_KEY` match your project

### Common Errors

**"relation 'exercises' does not exist"**
→ Run the schema creation script

**"No exercises loaded"**
→ Run the seed data script

**"Permission denied"**
→ Run the disable RLS script

---

## Next Steps

After exercises are loading:
1. You can add more exercises through the app using the "+" button
2. Exercises are automatically saved to Supabase
3. Filter exercises by sport category using the filter chips
4. Create custom workouts and programs using these exercises

---

## Re-enabling Security (Production)

When you implement user authentication:

1. Enable RLS:
   ```sql
   ALTER TABLE exercises ENABLE ROW LEVEL SECURITY;
   ```

2. Create appropriate policies based on your auth requirements