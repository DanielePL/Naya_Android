"""
Program Generator - Main Orchestration
Coordinates AI Coach, Exercise Database, and Template Pusher
"""

from typing import Dict, List, Optional
from datetime import datetime

# Try absolute imports first (Railway deployment), fallback to relative imports (local dev)
try:
    from ai_coach_service.ai_coach_client import AICoachClient
    from ai_coach_service.exercise_database import ExerciseDatabase
    from ai_coach_service.workout_template_pusher import WorkoutTemplatePusher
except ImportError:
    from .ai_coach_client import AICoachClient
    from .exercise_database import ExerciseDatabase
    from .workout_template_pusher import WorkoutTemplatePusher


class ProgramGenerator:
    """
    Main orchestrator for AI Coach program generation

    Flow:
    1. Receive user assessment data
    2. Query relevant exercises from database
    3. Call AI Coach (Claude) to generate program
    4. Push workout templates to Supabase
    5. Return program summary and template IDs
    """

    def __init__(self):
        """Initialize all service components"""
        self.ai_coach = AICoachClient()
        self.exercise_db = ExerciseDatabase()
        self.template_pusher = WorkoutTemplatePusher()

    def generate_program(
        self,
        user_id: str,
        assessment_data: Dict,
        program_duration_weeks: int = 8,
        num_workouts: int = 4
    ) -> Dict:
        """
        Generate complete training program

        Args:
            user_id: User UUID
            assessment_data: User profile with PRs, goals, constraints
            program_duration_weeks: Training cycle length
            num_workouts: Number of distinct workout templates

        Returns:
            Dict with:
            - success: bool
            - program_summary: str
            - template_ids: List[str]
            - workouts_created: int
            - metadata: Dict (tokens, duration, etc.)
        """
        try:
            print(f"🎯 Starting program generation for user {user_id}")
            print(f"   Duration: {program_duration_weeks} weeks")
            print(f"   Workouts: {num_workouts}")

            # STEP 1: Query exercises from database
            print("\n📚 STEP 1: Fetching exercises from database...")
            available_exercises = self._get_filtered_exercises(assessment_data)

            if not available_exercises:
                return {
                    "success": False,
                    "error": "No exercises available in database"
                }

            print(f"   Found {len(available_exercises)} exercises")

            # STEP 2: Generate program with AI
            print("\n🤖 STEP 2: Generating program with AI Coach...")
            ai_result = self.ai_coach.generate_training_program(
                user_profile=assessment_data,
                available_exercises=available_exercises,
                program_duration_weeks=program_duration_weeks,
                num_workouts=num_workouts
            )

            if not ai_result.get('success'):
                return {
                    "success": False,
                    "error": ai_result.get('error', 'AI generation failed')
                }

            program_data = ai_result['program']
            print(f"   ✅ AI generated {len(program_data.get('workouts', []))} workouts")
            print(f"   Summary: {program_data.get('program_summary', 'N/A')[:100]}...")

            # STEP 3: Push to Supabase
            print("\n💾 STEP 3: Pushing workout templates to database...")
            push_result = self.template_pusher.push_program(
                user_id=user_id,
                program_data=program_data
            )

            if not push_result.get('success'):
                return {
                    "success": False,
                    "error": f"Failed to save templates: {push_result.get('error')}"
                }

            # STEP 4: Build response
            print("\n✅ PROGRAM GENERATION COMPLETE!")
            print(f"   Templates created: {len(push_result['template_ids'])}")

            return {
                "success": True,
                "program_summary": program_data.get('program_summary', ''),
                "training_split": program_data.get('training_split', 'Custom'),
                "periodization_model": program_data.get('periodization_model', 'Progressive'),
                "template_ids": push_result['template_ids'],
                "workouts_created": len(push_result['template_ids']),
                "program_duration_weeks": program_duration_weeks,
                "metadata": {
                    **ai_result.get('metadata', {}),
                    "exercises_considered": len(available_exercises),
                    "generation_timestamp": datetime.utcnow().isoformat()
                }
            }

        except Exception as e:
            print(f"❌ Program generation error: {str(e)}")
            import traceback
            traceback.print_exc()
            return {
                "success": False,
                "error": str(e)
            }

    def _get_filtered_exercises(self, assessment_data: Dict) -> List[Dict]:
        """
        Filter exercises based on user constraints

        Args:
            assessment_data: User profile with equipment, injuries, etc.

        Returns:
            List of suitable exercises
        """
        try:
            # Get user constraints
            equipment = assessment_data.get('equipment_access', [])
            injuries = assessment_data.get('injuries', [])
            excluded_exercises = assessment_data.get('excluded_exercises', [])

            # Get all exercises (or filter by equipment if specified)
            if equipment:
                print(f"   Filtering by equipment: {', '.join(equipment)}")
                exercises = self.exercise_db.get_exercises(
                    equipment=equipment,
                    limit=300
                )
            else:
                print("   Fetching all available exercises...")
                exercises = self.exercise_db.get_all_exercises(limit=300)

            # Filter out excluded exercises
            if excluded_exercises:
                exercises = [
                    ex for ex in exercises
                    if ex['id'] not in excluded_exercises
                ]

            # TODO: Advanced filtering based on injuries
            # For now, we'll pass all exercises and let the AI handle it

            return exercises

        except Exception as e:
            print(f"❌ Error filtering exercises: {str(e)}")
            return []

    def get_exercise_stats(self) -> Dict:
        """Get statistics about available exercises"""
        return self.exercise_db.get_exercise_stats()

    def test_ai_connection(self) -> bool:
        """Test if AI Coach is properly configured"""
        try:
            # Simple test with minimal data
            test_profile = {
                "training_experience": 1,
                "training_days_per_week": 3,
                "session_duration_minutes": 60,
                "goals": ["strength"],
                "personal_records": {}
            }

            test_exercises = [
                {"id": "squat-back-barbell", "name": "Barbell Back Squat", "main_muscle": "Quadriceps"},
                {"id": "bench-press-barbell", "name": "Barbell Bench Press", "main_muscle": "Chest"}
            ]

            result = self.ai_coach.generate_training_program(
                user_profile=test_profile,
                available_exercises=test_exercises,
                program_duration_weeks=4,
                num_workouts=2
            )

            return result.get('success', False)

        except Exception as e:
            print(f"❌ AI connection test failed: {str(e)}")
            return False