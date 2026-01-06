"""
WOD Vision Parser - Extract CrossFit WODs from Whiteboard Photos
Uses OpenAI Vision API to read and parse WOD images into structured data
"""

import os
import json
import base64
import re
from typing import Dict, List, Optional, Tuple
from datetime import datetime
from openai import OpenAI


class WodVisionParser:
    """
    Parses CrossFit WOD images (whiteboard photos) using OpenAI Vision API
    Extracts structured WOD data ready for database storage
    """

    def __init__(self):
        api_key = os.environ.get("OPENAI_API_KEY")
        if not api_key:
            raise ValueError("OPENAI_API_KEY environment variable is required")

        self.client = OpenAI(api_key=api_key)
        self.model = "gpt-4o"  # Vision-capable model

    def parse_wod_image(
        self,
        image_base64: str,
        image_type: str = "image/jpeg",
        additional_context: Optional[str] = None
    ) -> Dict:
        """
        Parse a WOD image and extract structured data

        Args:
            image_base64: Base64 encoded image data
            image_type: MIME type (image/jpeg, image/png, etc.)
            additional_context: Optional context about the image

        Returns:
            Dict with parsed WOD data or error
        """
        try:
            print(f"🔍 Parsing WOD image ({len(image_base64)} bytes base64)")

            # Build the vision prompt
            system_prompt = self._build_system_prompt()
            user_prompt = self._build_user_prompt(additional_context)

            # Create the image content
            image_url = f"data:{image_type};base64,{image_base64}"

            # Call Vision API
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": user_prompt},
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": image_url,
                                    "detail": "high"  # High detail for text recognition
                                }
                            }
                        ]
                    }
                ],
                max_tokens=2000,
                temperature=0.1  # Low temperature for accuracy
            )

            response_text = response.choices[0].message.content
            print(f"✅ Vision API response received")

            # Parse the JSON response
            parsed_wod = self._extract_json_from_response(response_text)

            if parsed_wod:
                # Validate and enhance the parsed data
                validated_wod = self._validate_and_enhance(parsed_wod)
                print(f"✅ WOD parsed: {validated_wod.get('name', 'Unnamed')} ({validated_wod.get('wod_type', 'unknown')})")
                return {
                    "success": True,
                    "wod": validated_wod,
                    "raw_response": response_text,
                    "tokens_used": {
                        "input": response.usage.prompt_tokens,
                        "output": response.usage.completion_tokens
                    }
                }
            else:
                return {
                    "success": False,
                    "error": "Could not extract WOD data from image",
                    "raw_response": response_text
                }

        except Exception as e:
            print(f"❌ WOD Vision parsing error: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def _build_system_prompt(self) -> str:
        """Build the system prompt for WOD extraction"""
        return """You are a CrossFit WOD parser. Your job is to analyze whiteboard photos from CrossFit boxes and extract the workout information into structured JSON format.

You understand all CrossFit terminology:
- AMRAP: As Many Rounds/Reps As Possible
- EMOM: Every Minute On the Minute
- For Time / RFT: Complete as fast as possible
- Chipper: Complete all movements in sequence
- Tabata: 20 sec work / 10 sec rest
- Rx: As prescribed (standard weights/movements)
- Scaled: Modified version for less experienced athletes

Common movements you know:
- Thrusters, Wall Balls, Box Jumps, Burpees
- Pull-ups, Toes-to-Bar, Muscle-ups, Kipping
- Deadlifts, Cleans, Snatches, Jerks
- Rowing (calories/meters), Bike, Run
- Double-unders, Singles, Jump rope
- HSPU (Handstand Push-ups), Pistols
- KB Swings, Turkish Get-ups

Weight notations:
- 95/65 means 95 lbs for men, 65 lbs for women
- 43/30 kg means 43 kg for men, 30 kg for women
- BW or bodyweight means no additional weight

Rep schemes:
- 21-15-9: Do 21 reps, then 15, then 9 of each movement
- 10-9-8-7-6-5-4-3-2-1: Descending ladder
- 1-2-3-4-5-6-7-8-9-10: Ascending ladder

ALWAYS respond with valid JSON matching this exact schema:
{
    "name": "WOD name or date",
    "description": "Brief description of the workout",
    "wod_type": "amrap|emom|for_time|rft|chipper|ladder|tabata|death_by|max_effort|custom",
    "time_cap_seconds": null or number,
    "target_rounds": null or number (for RFT),
    "rep_scheme": null or array like [21, 15, 9],
    "rep_scheme_type": "fixed|descending|ascending|pyramid|custom",
    "difficulty": "beginner|intermediate|advanced|elite",
    "estimated_duration_minutes": number,
    "primary_focus": ["cardio", "strength", "gymnastics", "weightlifting"],
    "equipment_needed": ["barbell", "pullup_bar", "box", "kettlebell", etc],
    "movements": [
        {
            "order_index": 0,
            "movement_name": "Exercise name",
            "rep_type": "fixed|calories|distance|time|max",
            "reps": number or null,
            "reps_male": number or null (if different),
            "reps_female": number or null (if different),
            "distance_meters": number or null,
            "calories": number or null,
            "time_seconds": number or null,
            "weight_type": "bodyweight|fixed|percentage",
            "weight_kg_male": number or null,
            "weight_kg_female": number or null,
            "notes": "any special instructions"
        }
    ],
    "scaling": {
        "scaled": [
            {
                "movement_name": "Original movement",
                "alternative_name": "Scaled alternative",
                "reps": number or null,
                "weight_kg_male": number or null,
                "weight_kg_female": number or null,
                "notes": "scaling instructions"
            }
        ],
        "foundations": [...]
    },
    "notes": "Any additional notes from the whiteboard",
    "confidence": 0.0 to 1.0 (your confidence in the parsing accuracy)
}

If you cannot read part of the image clearly, set confidence lower and add notes about unclear parts.
Convert all weights to kilograms (1 lb = 0.453592 kg).
If the image doesn't contain a WOD, return {"error": "No WOD found in image", "description": "what you see instead"}."""

    def _build_user_prompt(self, additional_context: Optional[str]) -> str:
        """Build the user prompt"""
        base_prompt = """Analyze this CrossFit whiteboard photo and extract the WOD (Workout of the Day) information.

Return ONLY valid JSON with the workout structure. Do not include any explanation text outside the JSON."""

        if additional_context:
            base_prompt += f"\n\nAdditional context: {additional_context}"

        return base_prompt

    def _extract_json_from_response(self, response_text: str) -> Optional[Dict]:
        """Extract JSON from the response text"""
        try:
            # Try direct JSON parse
            return json.loads(response_text)
        except json.JSONDecodeError:
            pass

        # Try to find JSON in markdown code block
        json_patterns = [
            r'```json\s*([\s\S]*?)\s*```',
            r'```\s*([\s\S]*?)\s*```',
            r'\{[\s\S]*\}'
        ]

        for pattern in json_patterns:
            match = re.search(pattern, response_text)
            if match:
                try:
                    json_str = match.group(1) if '```' in pattern else match.group(0)
                    return json.loads(json_str)
                except (json.JSONDecodeError, IndexError):
                    continue

        return None

    def _validate_and_enhance(self, wod_data: Dict) -> Dict:
        """Validate and enhance the parsed WOD data"""

        # Ensure required fields
        if "name" not in wod_data or not wod_data["name"]:
            wod_data["name"] = f"WOD {datetime.now().strftime('%Y-%m-%d')}"

        if "wod_type" not in wod_data:
            wod_data["wod_type"] = "custom"

        # Normalize wod_type
        wod_type_map = {
            "amrap": "amrap",
            "as many rounds as possible": "amrap",
            "emom": "emom",
            "every minute on the minute": "emom",
            "for time": "for_time",
            "fortime": "for_time",
            "rft": "rft",
            "rounds for time": "rft",
            "chipper": "chipper",
            "ladder": "ladder",
            "tabata": "tabata",
            "death by": "death_by",
            "deathby": "death_by",
            "max": "max_effort",
            "max effort": "max_effort",
            "1rm": "max_effort"
        }
        wod_type_lower = wod_data.get("wod_type", "").lower().strip()
        wod_data["wod_type"] = wod_type_map.get(wod_type_lower, wod_type_lower)

        # Ensure movements list exists
        if "movements" not in wod_data:
            wod_data["movements"] = []

        # Validate and fix movements
        for i, movement in enumerate(wod_data.get("movements", [])):
            # Ensure order_index
            if "order_index" not in movement:
                movement["order_index"] = i

            # Ensure movement_name
            if "movement_name" not in movement or not movement["movement_name"]:
                movement["movement_name"] = f"Movement {i+1}"

            # Default rep_type
            if "rep_type" not in movement:
                if movement.get("calories"):
                    movement["rep_type"] = "calories"
                elif movement.get("distance_meters"):
                    movement["rep_type"] = "distance"
                elif movement.get("time_seconds"):
                    movement["rep_type"] = "time"
                else:
                    movement["rep_type"] = "fixed"

            # Default weight_type
            if "weight_type" not in movement:
                if movement.get("weight_kg_male") or movement.get("weight_kg_female"):
                    movement["weight_type"] = "fixed"
                else:
                    movement["weight_type"] = "bodyweight"

        # Set default difficulty if not present
        if "difficulty" not in wod_data:
            # Estimate based on movements and time
            time_cap = wod_data.get("time_cap_seconds", 0) or 0
            num_movements = len(wod_data.get("movements", []))

            if time_cap > 1200 or num_movements > 6:  # > 20 min or > 6 movements
                wod_data["difficulty"] = "advanced"
            elif time_cap > 600 or num_movements > 4:  # > 10 min or > 4 movements
                wod_data["difficulty"] = "intermediate"
            else:
                wod_data["difficulty"] = "beginner"

        # Set default scoring_type based on wod_type
        scoring_map = {
            "amrap": "rounds_reps",
            "emom": "pass_fail",
            "for_time": "time",
            "rft": "time",
            "chipper": "time",
            "ladder": "time",
            "tabata": "reps",
            "death_by": "reps",
            "max_effort": "weight"
        }
        if "scoring_type" not in wod_data:
            wod_data["scoring_type"] = scoring_map.get(wod_data["wod_type"], "custom")

        # Add metadata
        wod_data["parsed_at"] = datetime.now().isoformat()
        wod_data["source"] = "whiteboard_scan"

        return wod_data

    def parse_wod_from_url(self, image_url: str, additional_context: Optional[str] = None) -> Dict:
        """
        Parse a WOD from an image URL (e.g., Supabase Storage URL)

        Args:
            image_url: URL to the image
            additional_context: Optional context

        Returns:
            Dict with parsed WOD data
        """
        try:
            print(f"🔍 Parsing WOD from URL: {image_url[:50]}...")

            system_prompt = self._build_system_prompt()
            user_prompt = self._build_user_prompt(additional_context)

            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": user_prompt},
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": image_url,
                                    "detail": "high"
                                }
                            }
                        ]
                    }
                ],
                max_tokens=2000,
                temperature=0.1
            )

            response_text = response.choices[0].message.content
            parsed_wod = self._extract_json_from_response(response_text)

            if parsed_wod:
                validated_wod = self._validate_and_enhance(parsed_wod)
                return {
                    "success": True,
                    "wod": validated_wod,
                    "tokens_used": {
                        "input": response.usage.prompt_tokens,
                        "output": response.usage.completion_tokens
                    }
                }
            else:
                return {
                    "success": False,
                    "error": "Could not extract WOD data from image",
                    "raw_response": response_text
                }

        except Exception as e:
            print(f"❌ WOD URL parsing error: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }


class WodDatabaseService:
    """
    Service to save parsed WODs to Supabase database
    """

    def __init__(self):
        from supabase import create_client

        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise ValueError("SUPABASE_URL and SUPABASE_KEY required")

        self.client = create_client(supabase_url, supabase_key)

    def save_wod(
        self,
        wod_data: Dict,
        user_id: Optional[str] = None,
        source_image_url: Optional[str] = None,
        source_box_name: Optional[str] = None,
        is_public: bool = False
    ) -> Dict:
        """
        Save a parsed WOD to the database

        Args:
            wod_data: Parsed WOD data from WodVisionParser
            user_id: User who scanned the WOD
            source_image_url: URL to the original whiteboard image
            source_box_name: Name of the CrossFit box
            is_public: Whether to make the WOD public

        Returns:
            Dict with success status and WOD ID
        """
        try:
            print(f"💾 Saving WOD: {wod_data.get('name', 'Unnamed')}")

            # 1. Insert WOD template
            wod_template_data = {
                "name": wod_data.get("name"),
                "description": wod_data.get("description"),
                "wod_type": wod_data.get("wod_type", "custom"),
                "time_cap_seconds": wod_data.get("time_cap_seconds"),
                "target_rounds": wod_data.get("target_rounds"),
                "rep_scheme": json.dumps(wod_data.get("rep_scheme")) if wod_data.get("rep_scheme") else None,
                "rep_scheme_type": wod_data.get("rep_scheme_type"),
                "scoring_type": wod_data.get("scoring_type", "custom"),
                "source": wod_data.get("source", "whiteboard_scan"),
                "source_box_name": source_box_name,
                "source_date": datetime.now().date().isoformat(),
                "source_image_url": source_image_url,
                "user_id": user_id,
                "is_public": is_public,
                "difficulty": wod_data.get("difficulty"),
                "estimated_duration_minutes": wod_data.get("estimated_duration_minutes"),
                "primary_focus": wod_data.get("primary_focus", []),
                "equipment_needed": wod_data.get("equipment_needed", []),
                "tags": wod_data.get("tags", ["whiteboard_scan"])
            }

            result = self.client.table("wod_templates").insert(wod_template_data).execute()

            if not result.data:
                raise Exception("Failed to insert WOD template")

            wod_template_id = result.data[0]["id"]
            print(f"✅ WOD template created: {wod_template_id}")

            # 2. Insert movements
            movements = wod_data.get("movements", [])
            movements_inserted = 0

            for movement in movements:
                # Try to find matching exercise in exercises_new
                exercise_id = self._find_exercise_id(movement.get("movement_name", ""))

                movement_data = {
                    "wod_template_id": wod_template_id,
                    "exercise_id": exercise_id,
                    "movement_name": movement.get("movement_name"),
                    "movement_description": movement.get("notes"),
                    "order_index": movement.get("order_index", 0),
                    "segment": movement.get("segment", 1),
                    "rep_type": movement.get("rep_type", "fixed"),
                    "reps": movement.get("reps"),
                    "reps_male": movement.get("reps_male"),
                    "reps_female": movement.get("reps_female"),
                    "distance_meters": movement.get("distance_meters"),
                    "calories": movement.get("calories"),
                    "time_seconds": movement.get("time_seconds"),
                    "weight_type": movement.get("weight_type", "bodyweight"),
                    "weight_kg_male": movement.get("weight_kg_male"),
                    "weight_kg_female": movement.get("weight_kg_female"),
                    "notes": movement.get("notes")
                }

                self.client.table("wod_movements").insert(movement_data).execute()
                movements_inserted += 1

            print(f"✅ Inserted {movements_inserted} movements")

            # 3. Insert scaling options if present
            scaling = wod_data.get("scaling", {})
            scaling_inserted = 0

            for level, options in scaling.items():
                if not isinstance(options, list):
                    continue

                for option in options:
                    # Find the original movement to link
                    original_movement = next(
                        (m for m in movements if m.get("movement_name") == option.get("movement_name")),
                        None
                    )

                    scaling_data = {
                        "wod_template_id": wod_template_id,
                        "scaling_level": level,
                        "alternative_movement_name": option.get("alternative_name"),
                        "alternative_description": option.get("notes"),
                        "reps": option.get("reps"),
                        "weight_kg_male": option.get("weight_kg_male"),
                        "weight_kg_female": option.get("weight_kg_female")
                    }

                    self.client.table("wod_scaling").insert(scaling_data).execute()
                    scaling_inserted += 1

            if scaling_inserted > 0:
                print(f"✅ Inserted {scaling_inserted} scaling options")

            return {
                "success": True,
                "wod_template_id": wod_template_id,
                "movements_count": movements_inserted,
                "scaling_count": scaling_inserted
            }

        except Exception as e:
            print(f"❌ Error saving WOD: {str(e)}")
            return {
                "success": False,
                "error": str(e)
            }

    def _find_exercise_id(self, movement_name: str) -> Optional[str]:
        """Try to find a matching exercise in the database"""
        if not movement_name:
            return None

        try:
            # Clean the movement name
            clean_name = movement_name.lower().strip()

            # Common CrossFit movement name mappings
            name_mappings = {
                "pull-up": "pull up",
                "pullup": "pull up",
                "pull ups": "pull up",
                "pushup": "push up",
                "push-up": "push up",
                "push ups": "push up",
                "kb swing": "kettlebell swing",
                "kb swings": "kettlebell swing",
                "box jump": "box jump",
                "wall ball": "wall ball",
                "wallball": "wall ball",
                "t2b": "toes to bar",
                "toes-to-bar": "toes to bar",
                "hspu": "handstand push up",
                "du": "double under",
                "double-under": "double under",
                "c2b": "chest to bar",
                "chest-to-bar": "chest to bar pull up"
            }

            search_name = name_mappings.get(clean_name, clean_name)

            # Search in exercises_new
            result = self.client.table("exercises_new")\
                .select("id")\
                .ilike("name", f"%{search_name}%")\
                .limit(1)\
                .execute()

            if result.data and len(result.data) > 0:
                return result.data[0]["id"]

            return None

        except Exception as e:
            print(f"⚠️ Could not find exercise for '{movement_name}': {e}")
            return None

    def get_wod_with_movements(self, wod_template_id: str) -> Optional[Dict]:
        """Get a complete WOD with all movements"""
        try:
            # Use the helper function if available, otherwise manual query
            result = self.client.rpc(
                "get_wod_with_movements",
                {"wod_id": wod_template_id}
            ).execute()

            if result.data:
                return result.data

            return None

        except Exception as e:
            print(f"⚠️ Error getting WOD: {e}")
            return None

    def search_wods(
        self,
        query: Optional[str] = None,
        wod_type: Optional[str] = None,
        difficulty: Optional[str] = None,
        max_duration: Optional[int] = None,
        limit: int = 20,
        offset: int = 0
    ) -> List[Dict]:
        """Search WODs with filters"""
        try:
            result = self.client.rpc(
                "search_wods",
                {
                    "search_query": query,
                    "wod_type_filter": wod_type,
                    "difficulty_filter": difficulty,
                    "max_duration": max_duration,
                    "limit_count": limit,
                    "offset_count": offset
                }
            ).execute()

            return result.data or []

        except Exception as e:
            print(f"⚠️ Error searching WODs: {e}")
            return []


# Standalone test function
if __name__ == "__main__":
    import sys

    # Test with a sample image
    parser = WodVisionParser()

    # Test with base64 image
    if len(sys.argv) > 1:
        image_path = sys.argv[1]
        with open(image_path, "rb") as f:
            image_data = base64.b64encode(f.read()).decode("utf-8")

        result = parser.parse_wod_image(image_data)
        print(json.dumps(result, indent=2))
    else:
        print("Usage: python wod_vision_parser.py <image_path>")