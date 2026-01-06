"""
Exercise Database Query Service
Fetches and filters exercises from Supabase for AI Coach
"""

import os
from typing import List, Dict, Optional
from supabase import create_client, Client


class ExerciseDatabase:
    """Query and filter exercises from Supabase exercises_new table"""

    def __init__(self):
        """Initialize Supabase client"""
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise ValueError("SUPABASE_URL and SUPABASE_KEY required")

        self.client: Client = create_client(supabase_url, supabase_key)

    def get_exercises(
        self,
        equipment: Optional[List[str]] = None,
        muscle_groups: Optional[List[str]] = None,
        difficulty: Optional[List[str]] = None,
        limit: int = 200
    ) -> List[Dict]:
        """
        Query exercises from Supabase with filters

        Args:
            equipment: Filter by equipment (e.g., ["barbell", "dumbbells"])
            muscle_groups: Filter by main_muscle (e.g., ["chest", "legs"])
            difficulty: Filter by difficulty (e.g., ["beginner", "intermediate"])
            limit: Max number of exercises to return

        Returns:
            List of exercise dictionaries with id, name, equipment, muscle_group
        """
        try:
            print(f"📚 Querying exercises from Supabase...")

            # Build query
            query = self.client.table('exercises_new').select(
                'id, name, category, equipment, notes'
            )

            # Apply filters if provided
            # Note: Supabase filtering on array fields is tricky, so we fetch all
            # and filter in Python for better flexibility

            response = query.limit(limit).execute()

            if not response.data:
                print("⚠️ No exercises found")
                return []

            exercises = response.data

            # Post-query filtering
            if equipment:
                exercises = [
                    ex for ex in exercises
                    if any(eq.lower() in str(ex.get('equipment') or '').lower() for eq in equipment)
                ]

            if muscle_groups:
                exercises = [
                    ex for ex in exercises
                    if (ex.get('category') or '').lower() in [mg.lower() for mg in muscle_groups]
                ]

            # Note: 'level' column doesn't exist in current schema
            # Skip difficulty filtering for now
            # if difficulty:
            #     exercises = [
            #         ex for ex in exercises
            #         if (ex.get('level') or '').lower() in [d.lower() for d in difficulty]
            #     ]

            print(f"✅ Found {len(exercises)} exercises matching criteria")
            return exercises

        except Exception as e:
            print(f"❌ Error querying exercises: {str(e)}")
            return []

    def get_all_exercises(self, limit: int = 810) -> List[Dict]:
        """Get all exercises from database"""
        return self.get_exercises(limit=limit)

    def get_exercises_by_ids(self, exercise_ids: List[str]) -> List[Dict]:
        """
        Get specific exercises by their IDs

        Args:
            exercise_ids: List of exercise IDs

        Returns:
            List of exercise dictionaries
        """
        try:
            response = self.client.table('exercises_new') \
                .select('id, name, category, equipment') \
                .in_('id', exercise_ids) \
                .execute()

            return response.data if response.data else []

        except Exception as e:
            print(f"❌ Error fetching exercises by IDs: {str(e)}")
            return []

    def search_exercises(self, search_term: str, limit: int = 50) -> List[Dict]:
        """
        Search exercises by name

        Args:
            search_term: Search string
            limit: Max results

        Returns:
            List of matching exercises
        """
        try:
            response = self.client.table('exercises_new') \
                .select('id, name, category, equipment') \
                .ilike('name', f'%{search_term}%') \
                .limit(limit) \
                .execute()

            return response.data if response.data else []

        except Exception as e:
            print(f"❌ Error searching exercises: {str(e)}")
            return []

    def get_exercise_stats(self) -> Dict:
        """Get statistics about the exercise database"""
        try:
            # Get total count
            response = self.client.table('exercises_new').select('*', count='exact').execute()
            total_count = response.count if hasattr(response, 'count') else len(response.data)

            # Get sample for analysis
            sample = response.data[:100] if response.data else []

            # Analyze muscle groups
            muscle_groups = {}
            equipment_types = {}

            for ex in sample:
                muscle = ex.get('category', 'Unknown')
                muscle_groups[muscle] = muscle_groups.get(muscle, 0) + 1

                equipment_list = ex.get('equipment', [])
                if isinstance(equipment_list, list) and equipment_list:
                    for eq in equipment_list:
                        equipment_types[eq] = equipment_types.get(eq, 0) + 1
                else:
                    equipment_types['Unknown'] = equipment_types.get('Unknown', 0) + 1

            return {
                "total_exercises": total_count,
                "muscle_groups": muscle_groups,
                "equipment_types": equipment_types
            }

        except Exception as e:
            print(f"❌ Error getting exercise stats: {str(e)}")
            return {"total_exercises": 0}