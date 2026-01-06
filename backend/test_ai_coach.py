"""
Quick test of AI Coach functionality
"""

import os
import sys
from dotenv import load_dotenv

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Load environment variables
load_dotenv()

# Import AI Coach components
from ai_coach_service.exercise_database import ExerciseDatabase
from ai_coach_service.ai_coach_client import AICoachClient

def test_exercise_database():
    """Test exercise database connection"""
    print("\n" + "="*60)
    print("TEST 1: Exercise Database")
    print("="*60)

    try:
        db = ExerciseDatabase()
        stats = db.get_exercise_stats()

        print(f"✅ Connected to exercise database")
        print(f"   Total exercises: {stats.get('total_exercises', 0)}")
        print(f"   Muscle groups: {len(stats.get('muscle_groups', {}))}")
        print(f"   Equipment types: {len(stats.get('equipment_types', {}))}")

        # Get sample exercises
        exercises = db.get_exercises(limit=5)
        print(f"\n   Sample exercises:")
        for ex in exercises[:5]:
            print(f"   - {ex['name']} ({ex.get('category', 'N/A')})")

        return True
    except Exception as e:
        print(f"❌ Exercise database test failed: {str(e)}")
        return False


def test_ai_coach_simple():
    """Test AI Coach with minimal request"""
    print("\n" + "="*60)
    print("TEST 2: AI Coach - Simple Program Generation")
    print("="*60)

    try:
        # Check API key
        api_key = os.environ.get("ANTHROPIC_API_KEY")
        if not api_key:
            print("❌ ANTHROPIC_API_KEY not set in environment")
            return False

        print(f"✅ API Key configured: {api_key[:20]}...")

        # Create AI Coach client
        coach = AICoachClient()
        print(f"✅ AI Coach initialized (model: {coach.model})")

        # Simple test profile
        test_profile = {
            "training_experience": 2,
            "training_days_per_week": 3,
            "session_duration_minutes": 60,
            "goals": ["strength"],
            "personal_records": {
                "back_squat": {"weight": 100, "reps": 1},
                "bench_press": {"weight": 80, "reps": 1}
            },
            "equipment_access": ["barbell", "dumbbells", "rack"],
            "injuries": []
        }

        # Minimal exercise set for testing
        test_exercises = [
            {"id": "squat-back-barbell", "name": "Barbell Back Squat", "category": "Quadriceps", "equipment": ["Barbell"]},
            {"id": "bench-press-barbell", "name": "Barbell Bench Press", "category": "Chest", "equipment": ["Barbell"]},
            {"id": "deadlift-barbell", "name": "Barbell Deadlift", "category": "Lower Back", "equipment": ["Barbell"]}
        ]

        print("\n🤖 Calling Claude API...")
        print(f"   Profile: {test_profile['training_days_per_week']} days/week, {test_profile['training_experience']} years exp")
        print(f"   Exercises available: {len(test_exercises)}")

        result = coach.generate_training_program(
            user_profile=test_profile,
            available_exercises=test_exercises,
            program_duration_weeks=4,
            num_workouts=2
        )

        if result.get('success'):
            program = result['program']
            metadata = result.get('metadata', {})

            print(f"\n✅ Program generated successfully!")
            print(f"   Tokens: {metadata.get('tokens_input', 0)} in, {metadata.get('tokens_output', 0)} out")
            print(f"   Duration: {metadata.get('duration_ms', 0)}ms")
            print(f"   Summary: {program.get('program_summary', 'N/A')[:100]}...")
            print(f"   Workouts created: {len(program.get('workouts', []))}")

            # Show first workout
            workouts = program.get('workouts', [])
            if workouts:
                first_workout = workouts[0]
                print(f"\n   First workout: {first_workout.get('name', 'N/A')}")
                print(f"   Exercises: {len(first_workout.get('exercises', []))}")

                for ex in first_workout.get('exercises', [])[:2]:
                    print(f"     - {ex.get('exercise_id')}: {len(ex.get('sets', []))} sets")

            return True
        else:
            print(f"❌ Program generation failed: {result.get('error')}")
            return False

    except Exception as e:
        print(f"❌ AI Coach test failed: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


def main():
    print("\n" + "="*60)
    print("PROMETHEUS AI COACH - INTEGRATION TEST")
    print("="*60)

    # Test 1: Exercise Database
    db_ok = test_exercise_database()

    # Test 2: AI Coach
    ai_ok = test_ai_coach_simple()

    # Summary
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    print(f"Exercise Database: {'✅ PASS' if db_ok else '❌ FAIL'}")
    print(f"AI Coach:          {'✅ PASS' if ai_ok else '❌ FAIL'}")
    print(f"Overall:           {'✅ ALL TESTS PASSED' if (db_ok and ai_ok) else '❌ SOME TESTS FAILED'}")
    print("="*60 + "\n")

    sys.exit(0 if (db_ok and ai_ok) else 1)


if __name__ == "__main__":
    main()