# AI Coach - Design Specification

## Overview
Automated training program generation based on user profile, PRs, and individual performance analysis (ILB/ILM).

## User Flow

### Phase 1: Data Collection
```
┌─────────────────────────────────────┐
│  User Profile Assessment            │
├─────────────────────────────────────┤
│ 1. Basic Info                       │
│    - Training experience (years)    │
│    - Available training days/week   │
│    - Session duration preference    │
│    - Primary goals                  │
│                                     │
│ 2. Personal Records (PRs)           │
│    Option A: Enter known PRs        │
│    Option B: "I don't know" → ILB   │
│                                     │
│    Common PRs:                      │
│    - Back Squat                     │
│    - Bench Press                    │
│    - Deadlift                       │
│    - Front Squat                    │
│    - Overhead Press                 │
│    - Pull-ups (max reps)            │
│                                     │
│ 3. Individual Leistungsbild (ILB)   │
│    If user doesn't know PRs:        │
│    - Guided strength tests          │
│    - Movement quality screens       │
│    - Video-based form assessment    │
│                                     │
│ 4. Constraints & Preferences        │
│    - Injuries/medical conditions    │
│    - Equipment access               │
│    - Preferred exercises            │
│    - Exercise exclusions            │
└─────────────────────────────────────┘
```

### Phase 2: AI Analysis & Program Generation
```
┌─────────────────────────────────────┐
│  AI Coach Processing                │
├─────────────────────────────────────┤
│ 1. Profile Analysis                 │
│    - Parse user goals               │
│    - Analyze PR ratios              │
│    - Identify weaknesses            │
│    - Check injury history           │
│                                     │
│ 2. Exercise Selection               │
│    - Query exercises_new (810 ex)   │
│    - Filter by equipment            │
│    - Match to muscle groups         │
│    - Avoid contraindicated moves    │
│                                     │
│ 3. Load Prescription                │
│    - Calculate training loads       │
│    - Based on %1RM or RPE           │
│    - Progressive overload path      │
│    - Deload weeks planned           │
│                                     │
│ 4. Periodization                    │
│    - Cycle structure (4-12 weeks)   │
│    - Accumulation phase             │
│    - Intensification phase          │
│    - Realization/peak phase         │
│                                     │
│ 5. Workout Template Creation        │
│    - Generate workout_templates     │
│    - Create exercise sequences      │
│    - Define sets/reps/rest          │
│    - Add progression notes          │
└─────────────────────────────────────┘
```

### Phase 3: Auto-Push to User
```
┌─────────────────────────────────────┐
│  Template Push to TrainingScreen    │
├─────────────────────────────────────┤
│ 1. Create in Database               │
│    - workout_templates              │
│    - workout_template_exercises     │
│    - exercise_sets                  │
│                                     │
│ 2. Real-time Update                 │
│    - Supabase real-time triggers    │
│    - Android receives updates       │
│    - User sees new workouts         │
│                                     │
│ 3. Program Summary                  │
│    - Training cycle overview        │
│    - Expected progress              │
│    - Key focus areas                │
│    - Adjustment schedule            │
└─────────────────────────────────────┘
```

## Data Model

### Assessment Data (stored in user_profiles)
```json
{
  "training_experience": 3,
  "training_days_per_week": 4,
  "session_duration_minutes": 75,
  "goals": ["strength", "hypertrophy"],
  "personal_records": {
    "back_squat": {"weight": 140, "reps": 1, "date": "2025-01-15"},
    "bench_press": {"weight": 100, "reps": 1, "date": "2025-01-10"},
    "deadlift": {"weight": 180, "reps": 1, "date": "2025-01-12"}
  },
  "preferred_sports": ["powerlifting", "bodybuilding"],
  "equipment_access": ["barbell", "dumbbells", "rack", "bench"],
  "injuries": [
    {"type": "shoulder", "severity": "mild", "notes": "Avoid overhead"}
  ]
}
```

### AI Coach Session
```sql
CREATE TABLE ai_coaching_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES user_profiles(id),

    -- Input data
    assessment_data JSONB NOT NULL,

    -- AI Generation
    ai_analysis JSONB,
    generated_program JSONB,

    -- Generated templates
    template_ids UUID[] DEFAULT '{}',

    -- Metadata
    model_used TEXT,
    tokens_used INTEGER,
    generation_time_ms INTEGER,

    status TEXT DEFAULT 'pending', -- pending, completed, failed
    error_message TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

## API Endpoints

### 1. Create AI Coaching Session
```
POST /api/v1/ai-coach/create-session
Body: {
  "user_id": "uuid",
  "assessment_data": { ... }
}
Response: {
  "session_id": "uuid",
  "status": "processing"
}
```

### 2. Generate Training Program
```
POST /api/v1/ai-coach/generate-program
Body: {
  "session_id": "uuid"
}
Response: {
  "program_summary": "...",
  "workouts_created": 12,
  "cycle_duration_weeks": 8,
  "template_ids": ["uuid1", "uuid2", ...]
}
```

### 3. Get Session Status
```
GET /api/v1/ai-coach/session/{session_id}
Response: {
  "status": "completed"
  "program_summary": "...",
  "workouts": [ ... ]
}
```

## AI Prompt Structure

### System Prompt
```
You are an expert strength & conditioning coach with 20+ years experience in
program design. You specialize in evidence-based training for strength,
hypertrophy, and athletic performance.

You have access to a database of 810 exercises with detailed metadata
(muscle groups, equipment, difficulty).

Your task is to create personalized training programs based on:
- User's training experience and goals
- Personal records (PRs) and strength ratios
- Available equipment and time constraints
- Injury history and movement limitations
```

### User Prompt Template
```
Create a {duration}-week training program for:

USER PROFILE:
- Experience: {training_experience} years
- Training frequency: {days_per_week} days/week
- Session duration: {session_duration} minutes
- Primary goals: {goals}

PERSONAL RECORDS:
{format_prs()}

CONSTRAINTS:
- Equipment: {equipment_access}
- Injuries/limitations: {injuries}
- Avoid: {excluded_exercises}

EXERCISE DATABASE:
{format_available_exercises()}

REQUIREMENTS:
1. Create {num_workouts} distinct workout templates
2. Use only exercises from the provided database
3. Include exercise_id for each exercise
4. Specify sets, reps, weight (%1RM or RPE), rest periods
5. Structure with proper periodization
6. Return as valid JSON matching the schema below

RESPONSE SCHEMA:
{
  "program_summary": "Brief overview of the program approach",
  "workouts": [
    {
      "name": "Upper Body Strength A",
      "description": "Focus on...",
      "exercises": [
        {
          "exercise_id": "bench-press-barbell",
          "order_index": 0,
          "sets": [
            {"set_number": 1, "target_reps": 5, "target_weight_percent": 80, "rest_seconds": 180},
            ...
          ]
        }
      ]
    }
  ]
}
```

## Progressive Enhancement

### MVP (Week 1-2)
- ✅ PR input screen
- ✅ Basic AI program generation (Claude API)
- ✅ Auto-push to workout_templates
- ✅ Single mesocycle (4-6 weeks)

### V2 (Week 3-4)
- ✅ Full ILB assessment flow
- ✅ Video-based strength testing
- ✅ Advanced periodization (12+ weeks)
- ✅ Auto-adjustment based on performance

### V3 (Week 5+)
- ✅ Real-time program adjustments
- ✅ Fatigue management & auto-deload
- ✅ Movement quality scoring
- ✅ Predictive injury prevention

## Success Metrics
- Time to first workout: < 5 minutes (with known PRs)
- Program quality: Validated by sports science principles
- User satisfaction: Can start training immediately
- Automation: 80%+ of decisions made by AI