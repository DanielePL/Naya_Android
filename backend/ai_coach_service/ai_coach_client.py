"""
OpenAI API Client for AI Coach
Handles LLM interactions for program generation
"""

import os
import json
from typing import Dict, List, Optional
from openai import OpenAI
from datetime import datetime


class AICoachClient:
    """Client for interacting with OpenAI API"""

    def __init__(self):
        """
        Initialize OpenAI client

        Requires environment variable:
        - OPENAI_API_KEY: Your OpenAI API key
        """
        api_key = os.environ.get("OPENAI_API_KEY")

        if not api_key:
            raise ValueError(
                "OPENAI_API_KEY environment variable is required. "
                "Set it in your .env file."
            )

        self.client = OpenAI(api_key=api_key)
        # Use GPT-4 Turbo for best quality, or gpt-3.5-turbo for faster/cheaper
        self.model = "gpt-4-turbo-preview"  # or "gpt-3.5-turbo"

    def generate_training_program(
        self,
        user_profile: Dict,
        available_exercises: List[Dict],
        program_duration_weeks: int = 8,
        num_workouts: int = 4
    ) -> Dict:
        """
        Generate a complete training program using OpenAI

        Args:
            user_profile: User assessment data (PRs, goals, constraints)
            available_exercises: List of exercises from database
            program_duration_weeks: Length of training cycle
            num_workouts: Number of distinct workout templates

        Returns:
            Dict with program_summary and workouts list
        """
        try:
            # Build the prompt
            system_prompt = self._build_system_prompt()
            user_prompt = self._build_user_prompt(
                user_profile=user_profile,
                available_exercises=available_exercises,
                program_duration_weeks=program_duration_weeks,
                num_workouts=num_workouts
            )

            print(f"🤖 Calling OpenAI API (model: {self.model})...")
            start_time = datetime.now()

            # Call OpenAI API
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.7,
                max_tokens=4096
            )

            end_time = datetime.now()
            duration_ms = int((end_time - start_time).total_seconds() * 1000)

            # Parse response
            response_text = response.choices[0].message.content
            print(f"✅ OpenAI response received ({duration_ms}ms, {response.usage.prompt_tokens} in, {response.usage.completion_tokens} out)")

            # Extract JSON from response
            program_data = self._parse_json_response(response_text)

            return {
                "success": True,
                "program": program_data,
                "metadata": {
                    "model": self.model,
                    "tokens_input": response.usage.prompt_tokens,
                    "tokens_output": response.usage.completion_tokens,
                    "duration_ms": duration_ms
                }
            }

        except Exception as e:
            print(f"❌ AI Coach generation failed: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def _build_system_prompt(self) -> str:
        """Build the system prompt for OpenAI"""
        return """You are an expert strength & conditioning coach with 20+ years of experience in program design.

You specialize in evidence-based training for:
- Strength development (powerlifting, general strength)
- Hypertrophy (muscle building)
- Athletic performance
- Functional fitness

Your expertise includes:
- Periodization (linear, undulating, block periodization)
- Load prescription (% 1RM, RPE, RIR)
- Exercise selection based on biomechanics
- Injury prevention and movement quality
- Progressive overload strategies

You have access to a comprehensive exercise database with detailed metadata (muscle groups, equipment requirements, difficulty levels).

Your task is to create personalized, scientifically-sound training programs based on the user's profile, goals, and constraints.

CRITICAL REQUIREMENTS:
1. Only use exercises from the provided database (by exercise_id)
2. Return valid JSON matching the exact schema provided
3. Be conservative with load prescriptions for safety
4. Account for injury history and limitations
5. Ensure proper exercise sequencing (compound → isolation)
6. Include appropriate rest periods based on intensity
7. Plan progressive overload across the program"""

    def _build_user_prompt(
        self,
        user_profile: Dict,
        available_exercises: List[Dict],
        program_duration_weeks: int,
        num_workouts: int
    ) -> str:
        """Build the user prompt with all context"""

        # Format PRs
        prs = user_profile.get('personal_records', {})
        pr_text = "\n".join([
            f"- {ex_name}: {pr['weight']}kg x {pr['reps']} reps"
            for ex_name, pr in prs.items()
        ]) if prs else "No PRs provided - use moderate loads and auto-regulation"

        # Format injuries/limitations
        injuries = user_profile.get('injuries', [])
        injury_text = "\n".join([
            f"- {inj.get('type', 'Unknown')}: {inj.get('notes', 'No details')}"
            for inj in injuries
        ]) if injuries else "None reported"

        # Format goals
        goals = user_profile.get('goals', [])
        goals_text = ", ".join(goals) if goals else "General fitness"

        # Format equipment
        equipment = user_profile.get('equipment_access', [])
        equipment_text = ", ".join(equipment) if equipment else "Full gym access"

        # Format available exercises (limit to prevent token overflow)
        exercise_groups = {}
        for ex in available_exercises[:200]:
            muscle = ex.get('category', 'Other')
            if muscle not in exercise_groups:
                exercise_groups[muscle] = []
            exercise_groups[muscle].append(ex)

        exercises_text = ""
        for muscle, exercises in exercise_groups.items():
            exercises_text += f"\n{muscle.upper()}:\n"
            for ex in exercises[:15]:
                equipment_list = str(ex.get('equipment', 'Unknown'))
                exercises_text += f"  - {ex['id']}: {ex['name']} ({equipment_list})\n"

        prompt = f"""Create a {program_duration_weeks}-week training program for this user:

USER PROFILE:
- Training experience: {user_profile.get('training_experience', 1)} years
- Training frequency: {user_profile.get('training_days_per_week', 3)} days/week
- Session duration: {user_profile.get('session_duration_minutes', 60)} minutes
- Primary goals: {goals_text}

PERSONAL RECORDS:
{pr_text}

CONSTRAINTS:
- Equipment available: {equipment_text}
- Injuries/limitations: {injury_text}

AVAILABLE EXERCISES (by muscle group):
{exercises_text}

REQUIREMENTS:
1. Create {num_workouts} distinct workout templates
2. Each workout should target different muscle groups or movement patterns
3. Use only exercises from the database above (use exact exercise IDs)
4. For each exercise, specify:
   - order_index (0-based sequencing)
   - sets: array of set objects with:
     * set_number (1-based)
     * target_reps
     * target_weight (in kg - calculate from PRs if available, or use RPE-based estimation)
     * rest_seconds (120-300 for heavy, 60-90 for accessories)
5. Structure with proper periodization principles
6. If PRs are available, use percentage-based loading
7. If no PRs, use RPE 7-8 as baseline and include notes for auto-regulation

Return ONLY valid JSON (no markdown, no explanations) matching this schema:

{{
  "program_summary": "2-3 sentence overview of the program approach and key focus areas",
  "training_split": "e.g., Upper/Lower, Push/Pull/Legs, Full Body",
  "periodization_model": "e.g., Linear, Block, Daily Undulating",
  "workouts": [
    {{
      "name": "Descriptive workout name (e.g., Upper Body Strength A)",
      "description": "Brief focus of this workout",
      "target_duration_minutes": {user_profile.get('session_duration_minutes', 60)},
      "exercises": [
        {{
          "exercise_id": "exact-id-from-database",
          "order_index": 0,
          "notes": "Optional coaching cues or progression notes",
          "sets": [
            {{
              "set_number": 1,
              "target_reps": 5,
              "target_weight": 100.0,
              "rest_seconds": 180
            }}
          ]
        }}
      ]
    }}
  ]
}}

IMPORTANT: Return ONLY the JSON object, no additional text."""

        return prompt

    def _parse_json_response(self, response_text: str) -> Dict:
        """
        Parse JSON from OpenAI's response
        Handles markdown code blocks and extracts pure JSON
        """
        try:
            # Try direct JSON parse first
            return json.loads(response_text)
        except json.JSONDecodeError:
            # Try to extract JSON from markdown code block
            if "```json" in response_text:
                json_start = response_text.find("```json") + 7
                json_end = response_text.find("```", json_start)
                json_text = response_text[json_start:json_end].strip()
                return json.loads(json_text)
            elif "```" in response_text:
                json_start = response_text.find("```") + 3
                json_end = response_text.find("```", json_start)
                json_text = response_text[json_start:json_end].strip()
                return json.loads(json_text)
            else:
                # Try to find JSON object in text
                start = response_text.find("{")
                end = response_text.rfind("}") + 1
                if start != -1 and end > start:
                    json_text = response_text[start:end]
                    return json.loads(json_text)
                else:
                    raise ValueError("No valid JSON found in response")
