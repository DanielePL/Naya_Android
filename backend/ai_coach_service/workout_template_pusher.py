"""
Workout Template Pusher
Pushes AI-generated workouts to Supabase workout_templates table
"""

import os
from typing import Dict, List, Optional
from datetime import datetime
from supabase import create_client, Client


class WorkoutTemplatePusher:
    """Push workout templates to Supabase"""

    def __init__(self):
        """Initialize Supabase client"""
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise ValueError("SUPABASE_URL and SUPABASE_KEY required")

        self.client: Client = create_client(supabase_url, supabase_key)

    def push_program(
        self,
        user_id: str,
        program_data: Dict,
        session_id: Optional[str] = None
    ) -> Dict:
        """
        Push complete AI-generated program to database

        Args:
            user_id: User UUID
            program_data: AI-generated program with workouts
            session_id: Optional AI coaching session ID

        Returns:
            Dict with created template IDs and success status
        """
        try:
            workouts = program_data.get('workouts', [])
            if not workouts:
                return {"success": False, "error": "No workouts in program"}

            print(f"💾 Pushing {len(workouts)} workouts to Supabase for user {user_id}...")

            created_template_ids = []

            for workout in workouts:
                template_id = self._create_workout_template(
                    user_id=user_id,
                    workout=workout
                )

                if template_id:
                    created_template_ids.append(template_id)
                else:
                    print(f"⚠️ Failed to create template for: {workout.get('name')}")

            if len(created_template_ids) == len(workouts):
                print(f"✅ Successfully created all {len(created_template_ids)} workout templates")
            else:
                print(f"⚠️ Created {len(created_template_ids)}/{len(workouts)} templates")

            return {
                "success": True,
                "template_ids": created_template_ids,
                "workouts_created": len(created_template_ids),
                "program_summary": program_data.get('program_summary', '')
            }

        except Exception as e:
            print(f"❌ Error pushing program: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def _create_workout_template(
        self,
        user_id: str,
        workout: Dict
    ) -> Optional[str]:
        """
        Create a single workout template with exercises and sets

        Args:
            user_id: User UUID
            workout: Workout data from AI

        Returns:
            Created template UUID or None if failed
        """
        try:
            # 1. Create workout_template
            template_data = {
                "name": workout.get('name', 'Untitled Workout'),
                "user_id": user_id
            }

            template_response = self.client.table('workout_templates') \
                .insert(template_data) \
                .execute()

            if not template_response.data or len(template_response.data) == 0:
                print(f"❌ Failed to create template")
                return None

            template_id = template_response.data[0]['id']
            print(f"✅ Created template: {template_data['name']} ({template_id})")

            # 2. Create exercises for this template
            exercises = workout.get('exercises', [])

            for exercise in exercises:
                self._create_template_exercise(
                    template_id=template_id,
                    exercise=exercise
                )

            return template_id

        except Exception as e:
            print(f"❌ Error creating workout template: {str(e)}")
            return None

    def _create_template_exercise(
        self,
        template_id: str,
        exercise: Dict
    ) -> Optional[str]:
        """
        Create a workout_template_exercise with sets

        Args:
            template_id: Workout template UUID
            exercise: Exercise data with sets

        Returns:
            Created exercise UUID or None
        """
        try:
            # 1. Create workout_template_exercise
            exercise_data = {
                "workout_template_id": template_id,
                "exercise_id": exercise.get('exercise_id'),
                "order_index": exercise.get('order_index', 0)
            }

            exercise_response = self.client.table('workout_template_exercises') \
                .insert(exercise_data) \
                .execute()

            if not exercise_response.data or len(exercise_response.data) == 0:
                print(f"❌ Failed to create exercise: {exercise.get('exercise_id')}")
                return None

            workout_exercise_id = exercise_response.data[0]['id']

            # 2. Create sets for this exercise
            sets = exercise.get('sets', [])
            set_records = []

            for set_data in sets:
                set_record = {
                    "workout_exercise_id": workout_exercise_id,
                    "set_number": set_data.get('set_number', 1),
                    "target_reps": set_data.get('target_reps', 10),
                    "target_weight": set_data.get('target_weight', 0.0),
                    "rest_seconds": set_data.get('rest_seconds', 90)
                }
                set_records.append(set_record)

            if set_records:
                self.client.table('exercise_sets').insert(set_records).execute()

            print(f"  ✅ Added exercise: {exercise.get('exercise_id')} ({len(sets)} sets)")

            return workout_exercise_id

        except Exception as e:
            print(f"❌ Error creating template exercise: {str(e)}")
            return None

    def delete_user_templates(self, user_id: str) -> bool:
        """
        Delete all workout templates for a user
        (Useful for regenerating programs)

        Args:
            user_id: User UUID

        Returns:
            Success status
        """
        try:
            self.client.table('workout_templates') \
                .delete() \
                .eq('user_id', user_id) \
                .execute()

            print(f"🗑️ Deleted all templates for user {user_id}")
            return True

        except Exception as e:
            print(f"❌ Error deleting templates: {str(e)}")
            return False