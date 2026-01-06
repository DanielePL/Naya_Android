"""
Workout Parser
Extracts structured workout data from AI Coach responses
"""

import re
import json
from typing import Dict, List, Optional


class WorkoutParser:
    """Parses AI Coach responses to extract workout recommendations"""

    def __init__(self):
        pass

    def detect_workout(self, message: str) -> bool:
        """
        Detect if message contains a workout recommendation

        Args:
            message: AI response text

        Returns:
            True if workout detected
        """
        workout_indicators = [
            r"WORKOUT:",
            r"I'll create a workout",
            r"Here's (?:a|the) (?:workout|program)",
            r"Training plan:",
            r"\d+\s*x\s*\d+",  # Pattern like "3x8"
            r"Sets?\s*x\s*Reps?",
        ]

        for pattern in workout_indicators:
            if re.search(pattern, message, re.IGNORECASE):
                return True

        return False

    def parse_workout(self, message: str) -> Optional[Dict]:
        """
        Parse workout from AI response

        Args:
            message: AI response with workout

        Returns:
            Dict with workout structure or None
        """
        if not self.detect_workout(message):
            return None

        try:
            # Try to extract workout name
            workout_name = self._extract_workout_name(message)

            # Extract exercises
            exercises = self._extract_exercises(message)

            if not exercises:
                return None

            return {
                "name": workout_name,
                "description": self._extract_description(message),
                "exercises": exercises
            }

        except Exception as e:
            print(f"❌ Error parsing workout: {str(e)}")
            return None

    def _extract_workout_name(self, message: str) -> str:
        """Extract workout name from message"""
        # Try to find "WORKOUT: Name" pattern
        match = re.search(r"WORKOUT:\s*(.+?)(?:\n|$)", message, re.IGNORECASE)
        if match:
            return match.group(1).strip()

        # Try to find name after "create"
        match = re.search(r"(?:create|build)\s+(?:a|the)\s+(.+?)\s+workout", message, re.IGNORECASE)
        if match:
            return match.group(1).strip().title() + " Workout"

        # Default
        return "AI Coach Workout"

    def _extract_description(self, message: str) -> str:
        """Extract workout description"""
        # Try to find description after workout
        match = re.search(r"(?:This|It)\s+targets?\s+(.+?)(?:\.|$)", message, re.IGNORECASE)
        if match:
            return match.group(1).strip()

        # Use first sentence as fallback
        sentences = message.split('.')
        if sentences:
            return sentences[0].strip()

        return "AI-generated workout"

    def _extract_exercises(self, message: str) -> List[Dict]:
        """
        Extract exercises from message

        Looks for patterns like:
        - 1. Squat - 5x5 @ 80%
        - Bench Press: 4 x 6 @ 75%
        - Deadlift 3x8
        """
        exercises = []

        # Pattern 1: Numbered list with sets x reps
        # Example: "1. Squat - 5x5 @ 80%"
        pattern1 = r"(\d+)\.\s*([A-Za-z\s&'-]+?)\s*[-:]\s*(\d+)\s*x\s*(\d+)(?:\s*@\s*(.+?))?"
        matches = re.finditer(pattern1, message, re.IGNORECASE)

        for match in matches:
            order = int(match.group(1))
            name = match.group(2).strip()
            sets = int(match.group(3))
            reps = int(match.group(4))
            intensity = match.group(5).strip() if match.group(5) else None

            exercises.append({
                "order_index": order - 1,
                "exercise_name": name,
                "sets": sets,
                "reps": reps,
                "intensity": intensity,
                "notes": None
            })

        # Pattern 2: Name with sets x reps (no number)
        # Example: "Squat: 5x5 @ 80%"
        if not exercises:
            pattern2 = r"([A-Za-z\s&'-]+?):\s*(\d+)\s*x\s*(\d+)(?:\s*@\s*(.+?))?\n"
            matches = re.finditer(pattern2, message, re.IGNORECASE)

            for idx, match in enumerate(matches):
                name = match.group(1).strip()
                sets = int(match.group(2))
                reps = int(match.group(3))
                intensity = match.group(4).strip() if match.group(4) else None

                exercises.append({
                    "order_index": idx,
                    "exercise_name": name,
                    "sets": sets,
                    "reps": reps,
                    "intensity": intensity,
                    "notes": None
                })

        return exercises

    def format_workout_for_save(self, workout: Dict, user_id: str) -> Dict:
        """
        Format workout for saving to Supabase

        Args:
            workout: Parsed workout dict
            user_id: User's UUID

        Returns:
            Formatted workout template
        """
        return {
            "user_id": user_id,
            "name": workout["name"],
            "description": workout["description"],
            "exercises": workout["exercises"],
            "created_by": "ai_coach",
            "is_public": False
        }


# Example usage for testing
if __name__ == "__main__":
    parser = WorkoutParser()

    test_message = """
    I'll create a workout for you. Here's the plan:

    WORKOUT: Upper Body Strength

    1. Bench Press - 5x5 @ 80%
    2. Overhead Press - 4x6 @ 75%
    3. Barbell Row - 4x8 @ 70%
    4. Pull-ups - 3x10 @ Bodyweight
    5. Tricep Dips - 3x12 @ Bodyweight

    This targets upper body strength based on your powerlifting goals.
    """

    if parser.detect_workout(test_message):
        workout = parser.parse_workout(test_message)
        print(json.dumps(workout, indent=2))
