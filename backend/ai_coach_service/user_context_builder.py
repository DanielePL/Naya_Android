"""
User Context Builder for AI Coach
Loads user data from Supabase to build rich, personalized prompts
"""

import os
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from supabase import create_client, Client


class UserContextBuilder:
    """Builds rich context for AI Coach from Supabase data"""

    def __init__(self):
        """Initialize Supabase client"""
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise ValueError("SUPABASE_URL and SUPABASE_KEY required")

        self.client: Client = create_client(supabase_url, supabase_key)

    def build_user_context(self, user_id: str) -> Dict:
        """
        Build complete user context for AI prompt

        Args:
            user_id: User's UUID

        Returns:
            Dict with user profile, PRs, exercises, nutrition, etc.
        """
        try:
            # Load all user data in parallel
            profile = self._get_user_profile(user_id)
            prs = self._get_user_prs(user_id)
            exercises = self._get_available_exercises(limit=100)
            workouts = self._get_user_workouts(user_id, limit=10)
            public_templates = self._get_public_workout_templates(limit=20)

            # Load nutrition data
            nutrition_goal = self._get_nutrition_goal(user_id)
            nutrition_today = self._get_nutrition_today(user_id)
            nutrition_weekly = self._get_nutrition_weekly_summary(user_id)

            # Load advanced nutrition data
            macro_quality = self._calculate_macro_quality(nutrition_today)
            anabolic_window = self._get_anabolic_window(user_id)

            return {
                "profile": profile,
                "prs": prs,
                "exercises": exercises,
                "workouts": workouts,
                "public_templates": public_templates,
                "nutrition_goal": nutrition_goal,
                "nutrition_today": nutrition_today,
                "nutrition_weekly": nutrition_weekly,
                "macro_quality": macro_quality,
                "anabolic_window": anabolic_window,
                "has_data": bool(profile or prs or exercises)
            }

        except Exception as e:
            print(f"❌ Error building user context: {str(e)}")
            return {
                "profile": None,
                "prs": [],
                "exercises": [],
                "workouts": [],
                "public_templates": [],
                "nutrition_goal": None,
                "nutrition_today": None,
                "nutrition_weekly": None,
                "has_data": False
            }

    def _get_user_profile(self, user_id: str) -> Optional[Dict]:
        """Get user profile from Supabase"""
        try:
            result = self.client.table("user_profiles")\
                .select("*")\
                .eq("id", user_id)\
                .single()\
                .execute()

            if result.data:
                print(f"✅ Loaded profile for user: {result.data.get('name', 'Unknown')}")
                return result.data
            return None

        except Exception as e:
            print(f"⚠️ No profile found: {str(e)}")
            return None

    def _get_user_prs(self, user_id: str) -> List[Dict]:
        """Get user's personal records"""
        try:
            result = self.client.table("user_profiles")\
                .select("personal_records")\
                .eq("id", user_id)\
                .single()\
                .execute()

            if result.data and result.data.get("personal_records"):
                prs = result.data["personal_records"]
                print(f"✅ Loaded {len(prs)} PRs")
                return prs
            return []

        except Exception as e:
            print(f"⚠️ No PRs found: {str(e)}")
            return []

    def _get_available_exercises(self, limit: int = 100) -> List[Dict]:
        """Get available exercises from database (exercises_new table)"""
        try:
            # Try exercises_new first (main library)
            result = self.client.table("exercises_new")\
                .select("id, name, category, equipment")\
                .limit(limit)\
                .execute()

            if result.data:
                print(f"✅ Loaded {len(result.data)} exercises from exercises_new")
                return result.data
            return []

        except Exception as e:
            print(f"⚠️ Error loading exercises: {str(e)}")
            return []

    def _get_user_workouts(self, user_id: str, limit: int = 10) -> List[Dict]:
        """Get user's own workout templates with exercises"""
        try:
            result = self.client.table("workout_templates")\
                .select("id, name, created_at")\
                .eq("user_id", user_id)\
                .order("created_at", desc=True)\
                .limit(limit)\
                .execute()

            if not result.data:
                print(f"⚠️ No user workout templates found for user {user_id}")
                return []

            print(f"✅ Found {len(result.data)} user workout templates")

            # For each template, load exercises (same as public templates)
            templates_with_exercises = []
            for template in result.data:
                try:
                    # Load exercises for this template
                    exercises_result = self.client.table("workout_template_exercises")\
                        .select("id, exercise_id, order_index")\
                        .eq("workout_template_id", template["id"])\
                        .order("order_index")\
                        .execute()

                    # Load exercise details for each exercise
                    exercises = []
                    for ex in exercises_result.data or []:
                        exercise_name = "Unknown Exercise"

                        # Load from exercises_new (main library)
                        try:
                            exercise_details = self.client.table("exercises_new")\
                                .select("name")\
                                .eq("id", ex["exercise_id"])\
                                .single()\
                                .execute()

                            if exercise_details.data:
                                exercise_name = exercise_details.data.get("name", "Unknown Exercise")
                        except Exception:
                            # Fallback to exercises table
                            try:
                                exercise_details = self.client.table("exercises")\
                                    .select("name")\
                                    .eq("id", ex["exercise_id"])\
                                    .single()\
                                    .execute()

                                if exercise_details.data:
                                    exercise_name = exercise_details.data.get("name", "Unknown Exercise")
                            except Exception:
                                pass

                        # Load sets
                        sets_result = self.client.table("exercise_sets")\
                            .select("target_reps, target_weight")\
                            .eq("workout_exercise_id", ex["id"])\
                            .execute()

                        exercises.append({
                            "name": exercise_name,
                            "order": ex["order_index"],
                            "sets_count": len(sets_result.data or []),
                            "sets": sets_result.data or []
                        })

                    templates_with_exercises.append({
                        "id": template["id"],
                        "name": template["name"],
                        "description": template.get("description", ""),
                        "exercises": exercises
                    })

                except Exception as e:
                    print(f"⚠️ Error loading exercises for user template {template['id']}: {str(e)}")
                    templates_with_exercises.append({
                        "id": template["id"],
                        "name": template["name"],
                        "description": template.get("description", ""),
                        "exercises": []
                    })

            print(f"✅ Loaded {len(templates_with_exercises)} user workout templates with exercises")
            return templates_with_exercises

        except Exception as e:
            print(f"⚠️ Error loading user workouts: {str(e)}")
            return []

    def _get_public_workout_templates(self, limit: int = 20) -> List[Dict]:
        """Get public workout templates with exercises"""
        try:
            # Load ALL templates first (filtering NULL in Supabase query is tricky)
            # Then filter in Python for user_id = None, same as Android app does
            all_templates_result = self.client.table("workout_templates")\
                .select("id, name, sports, user_id")\
                .execute()

            if not all_templates_result.data:
                print("⚠️ No templates found in database at all")
                return []

            # Filter for public templates (user_id IS NULL) in Python
            public_templates = [t for t in all_templates_result.data if t.get("user_id") is None]
            print(f"🔍 Found {len(public_templates)} public templates out of {len(all_templates_result.data)} total templates")

            # Limit after filtering
            public_templates = public_templates[:limit]

            if not public_templates:
                print("⚠️ No public templates found after filtering")
                return []

            # For each template, load exercises
            templates_with_exercises = []
            for template in public_templates:
                try:
                    # Load exercises for this template
                    exercises_result = self.client.table("workout_template_exercises")\
                        .select("id, exercise_id, order_index")\
                        .eq("workout_template_id", template["id"])\
                        .order("order_index")\
                        .execute()

                    # Load sets and exercise details for each exercise
                    exercises = []
                    for ex in exercises_result.data or []:
                        exercise_name = "Unknown Exercise"

                        # Load from exercises_new (main library)
                        try:
                            exercise_details = self.client.table("exercises_new")\
                                .select("name")\
                                .eq("id", ex["exercise_id"])\
                                .single()\
                                .execute()

                            if exercise_details.data:
                                exercise_name = exercise_details.data.get("name", "Unknown Exercise")
                        except Exception as e:
                            # Fallback to exercises table
                            try:
                                exercise_details = self.client.table("exercises")\
                                    .select("name")\
                                    .eq("id", ex["exercise_id"])\
                                    .single()\
                                    .execute()

                                if exercise_details.data:
                                    exercise_name = exercise_details.data.get("name", "Unknown Exercise")
                            except Exception:
                                print(f"⚠️ Could not load exercise for id {ex['exercise_id']}: {e}")

                        # Load sets using workout_exercise_id
                        sets_result = self.client.table("exercise_sets")\
                            .select("target_reps, target_weight")\
                            .eq("workout_exercise_id", ex["id"])\
                            .execute()

                        exercises.append({
                            "name": exercise_name,
                            "order": ex["order_index"],
                            "sets_count": len(sets_result.data or []),
                            "sets": sets_result.data or []
                        })

                    templates_with_exercises.append({
                        "id": template["id"],
                        "name": template["name"],
                        "description": template.get("description", ""),
                        "sports": template.get("sports", []),
                        "exercises": exercises
                    })

                except Exception as e:
                    print(f"⚠️ Error loading exercises for template {template['id']}: {str(e)}")
                    # Still add the template even if exercises failed to load
                    templates_with_exercises.append({
                        "id": template["id"],
                        "name": template["name"],
                        "description": template.get("description", ""),
                        "sports": template.get("sports", []),
                        "exercises": []
                    })
                    continue

            print(f"✅ Loaded {len(templates_with_exercises)} public workout templates with exercises")
            return templates_with_exercises

        except Exception as e:
            print(f"⚠️ Error loading public templates: {str(e)}")
            return []

    # ═══════════════════════════════════════════════════════════════
    # NUTRITION DATA METHODS
    # ═══════════════════════════════════════════════════════════════

    def _get_nutrition_goal(self, user_id: str) -> Optional[Dict]:
        """Get user's active nutrition goal"""
        try:
            result = self.client.table("nutrition_goals")\
                .select("*")\
                .eq("user_id", user_id)\
                .eq("is_active", True)\
                .limit(1)\
                .execute()

            if result.data and len(result.data) > 0:
                goal = result.data[0]
                print(f"✅ Loaded nutrition goal: {goal.get('goal_type', 'Unknown')}")
                return goal
            return None

        except Exception as e:
            print(f"⚠️ No nutrition goal found: {str(e)}")
            return None

    def _get_nutrition_today(self, user_id: str) -> Optional[Dict]:
        """Get today's nutrition log with meals"""
        try:
            today = datetime.now().strftime("%Y-%m-%d")

            # Get nutrition log for today
            result = self.client.table("nutrition_logs")\
                .select("id, date, target_calories, target_protein, target_carbs, target_fat")\
                .eq("user_id", user_id)\
                .eq("date", today)\
                .limit(1)\
                .execute()

            if not result.data or len(result.data) == 0:
                print(f"ℹ️ No nutrition log for today")
                return None

            log = result.data[0]
            log_id = log["id"]

            # Get meals for this log
            meals_result = self.client.table("meals")\
                .select("id, meal_type, meal_name, time")\
                .eq("nutrition_log_id", log_id)\
                .execute()

            meals = []
            total_calories = 0
            total_protein = 0
            total_carbs = 0
            total_fat = 0

            for meal in meals_result.data or []:
                # Get meal items
                items_result = self.client.table("meal_items")\
                    .select("item_name, calories, protein, carbs, fat, quantity, quantity_unit")\
                    .eq("meal_id", meal["id"])\
                    .execute()

                meal_calories = sum(item.get("calories", 0) for item in items_result.data or [])
                meal_protein = sum(item.get("protein", 0) for item in items_result.data or [])
                meal_carbs = sum(item.get("carbs", 0) for item in items_result.data or [])
                meal_fat = sum(item.get("fat", 0) for item in items_result.data or [])

                total_calories += meal_calories
                total_protein += meal_protein
                total_carbs += meal_carbs
                total_fat += meal_fat

                meals.append({
                    "meal_type": meal.get("meal_type", "unknown"),
                    "meal_name": meal.get("meal_name", "Unnamed"),
                    "time": meal.get("time", ""),
                    "calories": meal_calories,
                    "protein": meal_protein,
                    "carbs": meal_carbs,
                    "fat": meal_fat,
                    "items": [item.get("item_name", "") for item in items_result.data or []]
                })

            nutrition_today = {
                "date": today,
                "target_calories": log.get("target_calories", 2500),
                "target_protein": log.get("target_protein", 180),
                "target_carbs": log.get("target_carbs", 280),
                "target_fat": log.get("target_fat", 80),
                "consumed_calories": total_calories,
                "consumed_protein": total_protein,
                "consumed_carbs": total_carbs,
                "consumed_fat": total_fat,
                "meals": meals,
                "meals_count": len(meals)
            }

            print(f"✅ Loaded today's nutrition: {total_calories} kcal, {len(meals)} meals")
            return nutrition_today

        except Exception as e:
            print(f"⚠️ Error loading today's nutrition: {str(e)}")
            return None

    def _get_nutrition_weekly_summary(self, user_id: str) -> Optional[Dict]:
        """Get nutrition summary for the past 7 days"""
        try:
            today = datetime.now()
            week_ago = today - timedelta(days=7)
            today_str = today.strftime("%Y-%m-%d")
            week_ago_str = week_ago.strftime("%Y-%m-%d")

            # Get nutrition logs for the past week
            result = self.client.table("nutrition_logs")\
                .select("id, date")\
                .eq("user_id", user_id)\
                .gte("date", week_ago_str)\
                .lte("date", today_str)\
                .execute()

            if not result.data:
                print(f"ℹ️ No nutrition logs for the past week")
                return None

            days_with_data = []
            total_calories_week = 0
            total_protein_week = 0

            for log in result.data:
                log_id = log["id"]

                # Get meals for this log
                meals_result = self.client.table("meals")\
                    .select("id")\
                    .eq("nutrition_log_id", log_id)\
                    .execute()

                if meals_result.data and len(meals_result.data) > 0:
                    day_calories = 0
                    day_protein = 0

                    for meal in meals_result.data:
                        items_result = self.client.table("meal_items")\
                            .select("calories, protein")\
                            .eq("meal_id", meal["id"])\
                            .execute()

                        day_calories += sum(item.get("calories", 0) for item in items_result.data or [])
                        day_protein += sum(item.get("protein", 0) for item in items_result.data or [])

                    if day_calories > 0:  # Only count days with actual food logged
                        days_with_data.append({
                            "date": log["date"],
                            "calories": day_calories,
                            "protein": day_protein
                        })
                        total_calories_week += day_calories
                        total_protein_week += day_protein

            if not days_with_data:
                return None

            weekly_summary = {
                "days_logged": len(days_with_data),
                "avg_daily_calories": total_calories_week / len(days_with_data) if days_with_data else 0,
                "avg_daily_protein": total_protein_week / len(days_with_data) if days_with_data else 0,
                "total_calories_week": total_calories_week,
                "total_protein_week": total_protein_week,
                "daily_breakdown": days_with_data
            }

            print(f"✅ Weekly nutrition summary: {len(days_with_data)} days, avg {weekly_summary['avg_daily_calories']:.0f} kcal/day")
            return weekly_summary

        except Exception as e:
            print(f"⚠️ Error loading weekly nutrition: {str(e)}")
            return None

    def _calculate_macro_quality(self, nutrition_today: Optional[Dict]) -> Optional[Dict]:
        """
        Calculate macro quality scores based on food items
        Mirrors the MacroQuality.kt logic from Android app
        """
        if not nutrition_today:
            return None

        meals = nutrition_today.get("meals", [])
        if not meals:
            return None

        # ═══════════════════════════════════════════════════════════════
        # PROTEIN QUALITY - Based on DIAAS (Digestible Indispensable Amino Acid Score)
        # ═══════════════════════════════════════════════════════════════

        # DIAAS scores for common protein sources (matching MacroQuality.kt)
        diaas_scores = {
            # High quality animal proteins
            "whey": 1.09, "egg": 1.13, "egg white": 1.08, "beef": 1.01, "steak": 1.01,
            "chicken": 1.08, "hähnchen": 1.08, "poulet": 1.08, "fish": 1.00, "fisch": 1.00,
            "salmon": 1.00, "lachs": 1.00, "tuna": 1.00, "thunfisch": 1.00,
            "greek yogurt": 1.08, "skyr": 1.08, "quark": 1.08, "milk": 1.14, "milch": 1.14,
            "casein": 1.00,

            # Plant proteins
            "soy": 0.90, "soja": 0.90, "tofu": 0.85, "tempeh": 0.88,
            "pea protein": 0.82, "erbsenprotein": 0.82, "rice protein": 0.60, "reisprotein": 0.60,
            "hemp": 0.63, "hanf": 0.63, "lentil": 0.70, "linsen": 0.70,
            "chickpea": 0.68, "kichererbse": 0.68, "bean": 0.65, "bohne": 0.65, "quinoa": 0.81,

            # Grains (low, limiting in lysine)
            "bread": 0.40, "brot": 0.40, "pasta": 0.45, "nudel": 0.45, "spaghetti": 0.45,
            "rice": 0.60, "reis": 0.60, "oat": 0.54, "hafer": 0.54, "wheat": 0.45, "weizen": 0.45,

            # Mixed
            "pizza": 0.55, "burger": 0.95
        }

        def get_diaas_score(food_name: str) -> float:
            name = food_name.lower()
            for keyword, score in diaas_scores.items():
                if keyword in name:
                    return score
            return 0.70  # Default

        # ═══════════════════════════════════════════════════════════════
        # GLYCEMIC INDEX for Carb Quality
        # ═══════════════════════════════════════════════════════════════

        gi_scores = {
            # Low GI (< 55) - Green
            "vegetable": 15, "gemüse": 15, "salad": 15, "salat": 15, "broccoli": 15, "spinach": 15,
            "lentil": 30, "linsen": 30, "bean": 30, "bohne": 30, "chickpea": 30,
            "apple": 40, "apfel": 40, "berry": 40, "beere": 40, "orange": 40,
            "oat": 55, "hafer": 55, "quinoa": 53, "sweet potato": 54, "süsskartoffel": 54,
            "brown rice": 50, "vollkornreis": 50,

            # Medium GI (56-69) - Orange
            "banana": 62, "banane": 62, "honey": 61, "honig": 61, "whole wheat": 69, "vollkorn": 69,

            # High GI (> 70) - Red
            "white bread": 75, "weissbrot": 75, "toast": 75, "potato": 78, "kartoffel": 78,
            "rice cake": 82, "reiswaffel": 82, "corn flakes": 81, "cornflakes": 81,
            "candy": 80, "süssigkeit": 80, "sugar": 65, "zucker": 65,
            "sports drink": 78, "gatorade": 78, "white rice": 65
        }

        def get_gi_score(food_name: str) -> int:
            name = food_name.lower()
            for keyword, gi in gi_scores.items():
                if keyword in name:
                    return gi
            return 55  # Default medium

        # ═══════════════════════════════════════════════════════════════
        # CALCULATE QUALITY SCORES
        # ═══════════════════════════════════════════════════════════════

        total_weighted_diaas = 0.0
        total_protein = 0.0
        high_quality_protein = 0.0

        total_weighted_gi = 0.0
        total_carbs = 0.0
        low_gi_carbs = 0.0

        protein_sources = []
        carb_sources = []

        for meal in meals:
            items = meal.get("items_detail", [])  # Full item details
            if not items:
                # Fallback to simple items list
                items = [{"name": name, "protein": 0, "carbs": 0} for name in meal.get("items", [])]

            for item in items:
                item_name = item.get("name", item.get("item_name", ""))
                protein = item.get("protein", 0) or 0
                carbs = item.get("carbs", 0) or 0

                # Protein quality
                if protein > 0:
                    diaas = get_diaas_score(item_name)
                    total_weighted_diaas += diaas * protein
                    total_protein += protein
                    if diaas >= 0.9:
                        high_quality_protein += protein
                    protein_sources.append({
                        "name": item_name,
                        "protein": protein,
                        "diaas": diaas,
                        "quality": "high" if diaas >= 0.9 else "medium" if diaas >= 0.7 else "low"
                    })

                # Carb quality
                if carbs > 0:
                    gi = get_gi_score(item_name)
                    total_weighted_gi += gi * carbs
                    total_carbs += carbs
                    if gi < 55:
                        low_gi_carbs += carbs
                    carb_sources.append({
                        "name": item_name,
                        "carbs": carbs,
                        "gi": gi,
                        "quality": "low_gi" if gi < 55 else "medium_gi" if gi < 70 else "high_gi"
                    })

        # Calculate final scores
        weighted_diaas = total_weighted_diaas / total_protein if total_protein > 0 else 0.7
        avg_gi = total_weighted_gi / total_carbs if total_carbs > 0 else 55
        high_quality_percent = (high_quality_protein / total_protein * 100) if total_protein > 0 else 0
        low_gi_percent = (low_gi_carbs / total_carbs * 100) if total_carbs > 0 else 50

        # Quality levels
        def get_quality_level(score: float) -> str:
            if score >= 0.85:
                return "EXCELLENT"
            elif score >= 0.65:
                return "GOOD"
            elif score >= 0.45:
                return "MODERATE"
            else:
                return "POOR"

        protein_quality_level = get_quality_level(weighted_diaas)
        carb_quality_score = (100 - avg_gi) / 100  # Lower GI = better
        carb_quality_level = get_quality_level(carb_quality_score)

        # Generate suggestions
        protein_suggestion = None
        if weighted_diaas < 0.6:
            protein_suggestion = "Add complete proteins (eggs, dairy, fish) to improve amino acid profile"
        elif weighted_diaas < 0.8:
            protein_suggestion = "Consider adding Greek yogurt or eggs for better protein quality"
        elif high_quality_percent < 50:
            protein_suggestion = "Good mix! More complete proteins would optimize muscle protein synthesis"

        carb_suggestion = None
        if avg_gi > 70:
            carb_suggestion = "High glycemic load - add vegetables or legumes to slow glucose absorption"
        elif avg_gi > 60:
            carb_suggestion = "Moderate GI - consider swapping refined carbs for whole grains"

        print(f"✅ Calculated macro quality: Protein DIAAS={weighted_diaas:.2f}, Avg GI={avg_gi:.0f}")

        return {
            "protein_quality": {
                "weighted_diaas": round(weighted_diaas, 2),
                "quality_level": protein_quality_level,
                "total_protein": round(total_protein, 1),
                "high_quality_protein": round(high_quality_protein, 1),
                "high_quality_percent": round(high_quality_percent, 1),
                "suggestion": protein_suggestion,
                "top_sources": sorted(protein_sources, key=lambda x: x["protein"], reverse=True)[:5]
            },
            "carb_quality": {
                "average_gi": round(avg_gi, 0),
                "quality_level": carb_quality_level,
                "low_gi_percent": round(low_gi_percent, 1),
                "suggestion": carb_suggestion,
                "top_sources": sorted(carb_sources, key=lambda x: x["carbs"], reverse=True)[:5]
            },
            "overall_quality": get_quality_level((weighted_diaas + carb_quality_score) / 2)
        }

    def _get_anabolic_window(self, user_id: str) -> Optional[Dict]:
        """
        Check if user has an active anabolic window from a recent workout
        Uses completed_at instead of end_time (matches actual DB schema)
        """
        try:
            # Check for recent workout sessions (within last 6 hours)
            six_hours_ago = datetime.now() - timedelta(hours=6)
            six_hours_ago_str = six_hours_ago.isoformat()

            result = self.client.table("workout_sessions")\
                .select("id, completed_at, workout_type, is_fasted")\
                .eq("user_id", user_id)\
                .eq("status", "completed")\
                .gte("completed_at", six_hours_ago_str)\
                .order("completed_at", desc=True)\
                .limit(1)\
                .execute()

            if not result.data or len(result.data) == 0:
                return None

            session = result.data[0]
            end_time_str = session.get("completed_at")

            if not end_time_str:
                return None

            # Parse end time and calculate window
            from datetime import datetime as dt
            try:
                end_time = dt.fromisoformat(end_time_str.replace("Z", "+00:00"))
            except:
                return None

            now = datetime.now(end_time.tzinfo) if end_time.tzinfo else datetime.now()
            elapsed_minutes = (now - end_time).total_seconds() / 60

            is_fasted = session.get("is_fasted", False)
            window_duration = 30 if is_fasted else 120  # minutes
            extended_window = 360  # 6 hours

            remaining = max(0, window_duration - elapsed_minutes)
            extended_remaining = max(0, extended_window - elapsed_minutes)

            if extended_remaining <= 0:
                return None  # Window completely closed

            # Determine urgency
            if remaining > 0:
                if remaining <= 10:
                    urgency = "CRITICAL"
                elif remaining <= 30:
                    urgency = "HIGH"
                else:
                    urgency = "MODERATE"
            else:
                urgency = "EXTENDED"  # Primary window closed, extended still open

            print(f"✅ Anabolic window active: {remaining:.0f}min remaining (urgency: {urgency})")

            return {
                "is_active": remaining > 0,
                "is_extended_active": extended_remaining > 0,
                "minutes_remaining": round(remaining),
                "extended_minutes_remaining": round(extended_remaining),
                "urgency": urgency,
                "was_fasted": is_fasted,
                "workout_type": session.get("workout_type", "strength"),
                "protein_recommendation": "30-40g complete protein within the window"
            }

        except Exception as e:
            print(f"⚠️ Error checking anabolic window: {str(e)}")
            return None

    def format_system_prompt(self, user_context: Dict) -> str:
        """
        Build rich system prompt with user context

        Args:
            user_context: Dict from build_user_context()

        Returns:
            Complete system prompt string
        """
        profile = user_context.get("profile")
        prs = user_context.get("prs", [])
        exercises = user_context.get("exercises", [])
        public_templates = user_context.get("public_templates", [])
        user_workouts = user_context.get("workouts", [])  # User's own workout templates
        nutrition_goal = user_context.get("nutrition_goal")
        nutrition_today = user_context.get("nutrition_today")
        nutrition_weekly = user_context.get("nutrition_weekly")
        macro_quality = user_context.get("macro_quality")
        anabolic_window = user_context.get("anabolic_window")

        # DETAILED DEBUG LOGGING
        print(f"\n{'='*60}")
        print(f"🔍 BUILDING SYSTEM PROMPT - DEBUG INFO")
        print(f"{'='*60}")
        print(f"Profile exists: {profile is not None}")
        print(f"PRs count: {len(prs) if isinstance(prs, list) else 'N/A'}")
        print(f"Exercises count: {len(exercises)}")
        print(f"User's own templates count: {len(user_workouts) if user_workouts else 0}")
        if user_workouts and len(user_workouts) > 0:
            print(f"User's templates: {[t.get('name', 'NO NAME') for t in user_workouts[:5]]}")
        print(f"Public templates count: {len(public_templates) if public_templates else 0}")
        if public_templates and len(public_templates) > 0:
            print(f"First public template: {public_templates[0].get('name', 'NO NAME')}")
        print(f"{'='*60}\n")

        # Basic coach style - Menopause Wellness Coach
        base_prompt = """Du bist eine erfahrene Menopause-Wellness-Beraterin, die mit ihrer Klientin spricht.

Deine Expertise:
- Perimenopause und Menopause Symptom-Management
- Hormon-angepasste Trainingsempfehlungen
- Knochengesundheit und Osteoporose-Prävention
- Ernährung für hormonelle Balance (Phytoöstrogene, Calcium, Vitamin D)
- Schlafoptimierung in der Menopause
- Stress-Management und Stimmungsregulation

Dein Coaching-Stil:
- Sprich natürlich und einfühlsam, nicht wie ein Lehrbuch
- Sei unterstützend, verständnisvoll und ermutigend
- Verwende Umgangssprache, vermeide formale Listen
- KEIN Markdown-Format (kein **, ###, -, etc.) - nur Fliesstext
- Halte Antworten kurz und fokussiert (2-4 Sätze für Begrüssungen, 4-8 Sätze für Gesundheitsfragen)
- Antworte IMMER auf Deutsch"""

        # Add user-specific context if available
        if not profile:
            return base_prompt

        user_name = profile.get("name", "athlete")
        experience = profile.get("training_experience", 1)
        training_days = profile.get("training_days_per_week", 3)
        goals = profile.get("goals", [])
        injuries = profile.get("injuries", [])
        equipment = profile.get("equipment_access", [])
        preferred_sports = profile.get("preferred_sports", [])

        # Build rich context
        context_parts = [base_prompt, f"\nATHLETE PROFILE:"]
        context_parts.append(f"- Name: {user_name}")
        context_parts.append(f"- Training experience: {experience} years")
        context_parts.append(f"- Training frequency: {training_days} days/week")

        if goals:
            context_parts.append(f"- Primary goals: {', '.join(goals)}")

        if preferred_sports:
            context_parts.append(f"- Preferred sports: {', '.join(preferred_sports)}")

        # Add PRs if available
        if prs:
            context_parts.append("\nPERSONAL RECORDS:")
            for exercise_name, pr_data in prs.items():
                weight = pr_data.get("weight", 0)
                reps = pr_data.get("reps", 1)
                context_parts.append(f"- {exercise_name}: {weight}kg x {reps} reps")

        # Add injuries/limitations
        if injuries:
            context_parts.append("\nINJURIES & LIMITATIONS:")
            for injury in injuries:
                injury_type = injury.get("type", "Unknown")
                notes = injury.get("notes", "")
                context_parts.append(f"- {injury_type}: {notes}")

        # Add equipment access
        if equipment:
            context_parts.append(f"\nAVAILABLE EQUIPMENT: {', '.join(equipment)}")

        # Add exercise library (sample)
        if exercises:
            context_parts.append(f"\nAVAILABLE EXERCISES (you can recommend these):")
            # Group by category
            by_category = {}
            for ex in exercises[:50]:  # Limit to prevent token overflow
                cat = ex.get("category", "Other")
                if cat not in by_category:
                    by_category[cat] = []
                by_category[cat].append(ex)

            for category, exs in list(by_category.items())[:6]:  # Max 6 categories
                context_parts.append(f"\n{category.upper()}:")
                for ex in exs[:8]:  # Max 8 per category
                    eq = ', '.join(ex.get("equipment", [])) if ex.get("equipment") else "Bodyweight"
                    context_parts.append(f"  - {ex['name']} ({eq})")

        # Add USER'S OWN workout templates (My Workout Templates)
        print(f"🔍 DEBUG: user_workouts type={type(user_workouts)}, len={len(user_workouts) if user_workouts else 0}")
        if user_workouts and len(user_workouts) > 0:
            print(f"✅ Adding {len(user_workouts)} USER templates to system prompt")
            context_parts.append(f"\nATHLETE'S OWN WORKOUT TEMPLATES ({len(user_workouts)} templates):")
            context_parts.append("These are workouts the athlete has saved or created. You should prioritize these when they ask about 'my workouts' or 'my templates':")

            for template in user_workouts[:10]:  # Show max 10 user templates
                context_parts.append(f"\n• {template['name']}")
                if template.get('description'):
                    context_parts.append(f"  Description: {template['description']}")

                # Show exercises
                template_exercises = template.get('exercises', [])
                for ex in template_exercises[:5]:
                    sets_info = f"{ex['sets_count']} sets"
                    context_parts.append(f"  - {ex['name']}: {sets_info}")

                if len(template_exercises) > 5:
                    context_parts.append(f"  ... and {len(template_exercises) - 5} more exercises")

            context_parts.append("\nWhen the athlete asks about 'my workouts', 'my templates', or similar, refer to these templates.")
        else:
            print(f"⚠️ No user workout templates available")

        # Add public workout templates
        print(f"🔍 DEBUG: public_templates type={type(public_templates)}, len={len(public_templates) if public_templates else 0}")
        if public_templates and len(public_templates) > 0:
            print(f"✅ Adding {len(public_templates)} templates to system prompt")
            context_parts.append(f"\nPUBLIC WORKOUT TEMPLATES LIBRARY ({len(public_templates)} templates available):")
            context_parts.append("You can recommend these pre-built workouts to the athlete:")

            for template in public_templates[:15]:  # Show max 15 templates
                sports_str = f" ({', '.join(template.get('sports', []))})" if template.get('sports') else ""
                template_id = template.get('id', '')
                context_parts.append(f"\n• {template['name']}{sports_str} [ID: {template_id}]")

                # Show first 5 exercises
                exercises = template.get('exercises', [])
                for ex in exercises[:5]:
                    sets_info = f"{ex['sets_count']} sets"
                    context_parts.append(f"  - {ex['name']}: {sets_info}")

                if len(exercises) > 5:
                    context_parts.append(f"  ... and {len(exercises) - 5} more exercises")

            if len(public_templates) > 15:
                context_parts.append(f"\n... and {len(public_templates) - 15} more templates available")

            context_parts.append("\n🎯 YOU CAN RECOMMEND WORKOUTS! When the athlete asks for a workout, you MUST:")
            context_parts.append("1. Look at the templates above and pick one that matches their goals/sports")
            context_parts.append("2. Tell them about the workout (what it targets, exercises included)")
            context_parts.append("3. Add the special marker at the END of your message so they can start it directly")

            context_parts.append("\n⚠️ CRITICAL - TEMPLATE RECOMMENDATION FORMAT:")
            context_parts.append("When you recommend ANY template, you MUST add this marker at the END of your message:")
            context_parts.append("[RECOMMEND_TEMPLATE:template_id:Template Name]")
            context_parts.append("")
            context_parts.append("EXAMPLE: If recommending 'Push Day A' with ID 'abc-123-def', your message should end with:")
            context_parts.append("[RECOMMEND_TEMPLATE:abc-123-def:Push Day A]")
            context_parts.append("")
            context_parts.append("The app will show a clickable card with a 'Start Workout' button.")
            context_parts.append("You HAVE access to workout templates - you are NOT just a text assistant!")
            context_parts.append("Always recommend a template when the user asks for a workout or exercise routine.")
        else:
            print(f"⚠️ No public templates available to add to system prompt")

        # ═══════════════════════════════════════════════════════════════
        # NUTRITION SECTION
        # ═══════════════════════════════════════════════════════════════
        nutrition_section_added = False

        if nutrition_goal or nutrition_today or nutrition_weekly:
            context_parts.append("\n" + "="*50)
            context_parts.append("NUTRITION DATA")
            context_parts.append("="*50)
            nutrition_section_added = True

        # Nutrition Goal
        if nutrition_goal:
            goal_type = nutrition_goal.get("goal_type", "maintenance") or "maintenance"
            target_cal = nutrition_goal.get("target_calories") or 2500
            target_pro = nutrition_goal.get("target_protein") or 180
            target_carbs = nutrition_goal.get("target_carbs") or 280
            target_fat = nutrition_goal.get("target_fat") or 80

            context_parts.append(f"\nNUTRITION GOAL: {goal_type.upper()}")
            context_parts.append(f"- Daily targets: {int(target_cal)} kcal | {int(target_pro)}g protein | {int(target_carbs)}g carbs | {int(target_fat)}g fat")

        # Today's Nutrition
        if nutrition_today:
            consumed_cal = nutrition_today.get("consumed_calories") or 0
            consumed_pro = nutrition_today.get("consumed_protein") or 0
            consumed_carbs = nutrition_today.get("consumed_carbs") or 0
            consumed_fat = nutrition_today.get("consumed_fat") or 0
            target_cal = nutrition_today.get("target_calories") or 2500
            target_pro = nutrition_today.get("target_protein") or 180
            meals = nutrition_today.get("meals", [])

            context_parts.append(f"\nTODAY'S NUTRITION STATUS:")
            context_parts.append(f"- Calories: {int(consumed_cal)}/{int(target_cal)} kcal ({int(target_cal - consumed_cal)} remaining)")
            context_parts.append(f"- Protein: {int(consumed_pro)}/{int(target_pro)}g ({int(target_pro - consumed_pro)}g remaining)")
            context_parts.append(f"- Carbs: {int(consumed_carbs)}g | Fat: {int(consumed_fat)}g")
            context_parts.append(f"- Meals logged today: {len(meals)}")

            if meals:
                context_parts.append("\nMeals eaten today:")
                for meal in meals:
                    meal_type = (meal.get("meal_type") or "meal").capitalize()
                    meal_name = meal.get("meal_name") or "Unknown"
                    meal_cal = meal.get("calories") or 0
                    meal_pro = meal.get("protein") or 0
                    items = meal.get("items", [])
                    items_str = ", ".join(items[:3]) if items else "various items"
                    if len(items) > 3:
                        items_str += f" +{len(items)-3} more"
                    context_parts.append(f"  • {meal_type}: {meal_name} ({int(meal_cal)} kcal, {int(meal_pro)}g protein) - {items_str}")

        # Weekly Summary
        if nutrition_weekly:
            days_logged = nutrition_weekly.get("days_logged") or 0
            avg_cal = nutrition_weekly.get("avg_daily_calories") or 0
            avg_pro = nutrition_weekly.get("avg_daily_protein") or 0

            context_parts.append(f"\nWEEKLY NUTRITION SUMMARY (last 7 days):")
            context_parts.append(f"- Days with food logged: {days_logged}/7")
            context_parts.append(f"- Average daily intake: {int(avg_cal)} kcal, {int(avg_pro)}g protein")

            # Consistency feedback
            if days_logged >= 5:
                context_parts.append("- Tracking consistency: EXCELLENT")
            elif days_logged >= 3:
                context_parts.append("- Tracking consistency: GOOD")
            else:
                context_parts.append("- Tracking consistency: NEEDS IMPROVEMENT")

        # ═══════════════════════════════════════════════════════════════
        # MACRO QUALITY SECTION (DIAAS, Glycemic Index)
        # ═══════════════════════════════════════════════════════════════
        if macro_quality:
            context_parts.append("\nMACRO QUALITY ANALYSIS:")

            # Protein Quality
            pq = macro_quality.get("protein_quality", {})
            if pq:
                diaas = pq.get("weighted_diaas", 0)
                pq_level = pq.get("quality_level", "MODERATE")
                hq_percent = pq.get("high_quality_percent", 0)
                pq_suggestion = pq.get("suggestion")

                context_parts.append(f"\nPROTEIN QUALITY: {pq_level}")
                context_parts.append(f"- DIAAS Score: {diaas:.2f} (1.0+ = excellent bioavailability)")
                context_parts.append(f"- High-quality protein: {hq_percent:.0f}% of total")
                if pq_suggestion:
                    context_parts.append(f"- Suggestion: {pq_suggestion}")

                # Top protein sources
                top_sources = pq.get("top_sources", [])
                if top_sources:
                    sources_str = ", ".join([f"{s['name']} ({s['quality']})" for s in top_sources[:3]])
                    context_parts.append(f"- Top sources today: {sources_str}")

            # Carb Quality
            cq = macro_quality.get("carb_quality", {})
            if cq:
                avg_gi = cq.get("average_gi", 55)
                cq_level = cq.get("quality_level", "MODERATE")
                low_gi_percent = cq.get("low_gi_percent", 50)
                cq_suggestion = cq.get("suggestion")

                context_parts.append(f"\nCARB QUALITY: {cq_level}")
                context_parts.append(f"- Average Glycemic Index: {avg_gi:.0f} (<55=low, 55-70=medium, >70=high)")
                context_parts.append(f"- Low-GI carbs: {low_gi_percent:.0f}% of total")
                if cq_suggestion:
                    context_parts.append(f"- Suggestion: {cq_suggestion}")

            overall = macro_quality.get("overall_quality", "MODERATE")
            context_parts.append(f"\nOVERALL NUTRITION QUALITY: {overall}")

        # ═══════════════════════════════════════════════════════════════
        # ANABOLIC WINDOW SECTION
        # ═══════════════════════════════════════════════════════════════
        if anabolic_window:
            urgency = anabolic_window.get("urgency", "MODERATE")
            remaining = anabolic_window.get("minutes_remaining", 0)
            extended_remaining = anabolic_window.get("extended_minutes_remaining", 0)
            was_fasted = anabolic_window.get("was_fasted", False)
            workout_type = anabolic_window.get("workout_type", "strength")

            context_parts.append("\n" + "!"*50)
            context_parts.append("ANABOLIC WINDOW ACTIVE!")
            context_parts.append("!"*50)

            if remaining > 0:
                context_parts.append(f"⏰ PRIMARY WINDOW: {remaining} minutes remaining")
                context_parts.append(f"Urgency: {urgency}")
            else:
                context_parts.append(f"⏰ EXTENDED WINDOW: {extended_remaining} minutes remaining")
                context_parts.append("Primary window closed, but protein intake still beneficial")

            if was_fasted:
                context_parts.append("⚠️ FASTED WORKOUT - protein intake is CRITICAL within 30min!")

            context_parts.append(f"Workout type: {workout_type}")
            context_parts.append("Recommendation: 30-40g complete protein (whey, eggs, chicken)")

            # Urgency-specific messaging
            if urgency == "CRITICAL":
                context_parts.append("\n🚨 CRITICAL: Less than 10 minutes left! Prioritize immediate protein intake!")
            elif urgency == "HIGH":
                context_parts.append("\n⚡ HIGH PRIORITY: Window closing soon. Suggest quick protein options.")

        # Add nutrition coaching guidelines - Menopause-focused
        if nutrition_section_added:
            context_parts.append("""
MENOPAUSE-ERNÄHRUNGSBERATUNG:
- Beziehe dich auf die tatsächliche Nahrungsaufnahme
- Sei spezifisch: "Ich sehe, du hast heute {x}mg Calcium bekommen, du brauchst noch etwa {y}mg"
- Schlage praktische Mahlzeiten vor, die auf Menopause-Bedürfnisse abgestimmt sind
- Betone diese Schlüsselnährstoffe:
  - CALCIUM: 1200mg täglich für Knochengesundheit
  - VITAMIN D: 800-1000 IE für Calcium-Aufnahme
  - PROTEIN: 1.2-1.4g/kg für Muskelerhalt
  - OMEGA-3: Für Herz- und Gehirngesundheit
  - PHYTOÖSTROGENE: Soja, Leinsamen, Hülsenfrüchte

MENOPAUSE-SPEZIFISCHE ERNÄHRUNGSTIPPS:
- Bei Hitzewallungen: Scharfes Essen, Alkohol und Koffein meiden
- Für Knochengesundheit: Milchprodukte, grünes Blattgemüse, Sardinen
- Gegen Gewichtszunahme: Proteinreich essen, verarbeitete Lebensmittel meiden
- Für besseren Schlaf: Leichte Abendmahlzeiten, Magnesium-reiche Lebensmittel
- Bei Brain Fog: Omega-3 (Lachs, Walnüsse), Beeren für Antioxidantien

PHYTOÖSTROGEN-QUELLEN empfehlen:
- Soja: Tofu, Tempeh, Edamame, Sojamilch
- Leinsamen: 1-2 EL täglich
- Hülsenfrüchte: Kichererbsen, Linsen, Bohnen
- Vollkornprodukte: Hafer, Gerste

Verbinde Ernährung mit Symptomen: "Wenn du mehr Leinsamen isst, kann das bei Hitzewallungen helfen."
Sei ermutigend, nicht belehrend - gib praktische, umsetzbare Tipps.""")

        # Add post-workout nutrition guidelines if active
        if anabolic_window and anabolic_window.get("urgency") in ["CRITICAL", "HIGH"]:
            context_parts.append("""
⚠️ POST-WORKOUT ERNÄHRUNG PRIORITÄT:
Wenn die Klientin dir schreibt, erwähne SOFORT das Ernährungsfenster nach dem Training!
Beispiel: "Hey [Name], ich sehe du hast gerade trainiert - jetzt ist der perfekte Zeitpunkt für eine proteinreiche Mahlzeit. Hast du schon etwas gegessen? Ein Quark mit Beeren oder Eier wären jetzt ideal für Muskelerhalt und Knochengesundheit."
Für Frauen in der Menopause ist Post-Workout Protein besonders wichtig für den Muskelerhalt!""")

        # Add coaching guidelines - Menopause-focused
        context_parts.append("""

COACHING-RICHTLINIEN FÜR MENOPAUSE-WELLNESS:
- Du KENNST diese Klientin persönlich - verwende ihren Namen und ihre Geschichte
- Sei einfühlsam bei Symptom-Gesprächen: "Ich verstehe, Hitzewallungen können sehr belastend sein..."
- Berücksichtige das Menopause-Stadium bei allen Empfehlungen
- Betone Recovery und Stress-Management - nicht nur Leistung
- Verbinde Training mit Symptom-Management
- Bei Trainingsplänen, berücksichtige:
  - Knochengesundheit (Krafttraining wichtig!)
  - Gelenkschonung
  - Energielevel-Schwankungen
  - Schlafqualität

SYMPTOM-BASIERTE EMPFEHLUNGEN:
- Bei Hitzewallungen: Kühle Umgebung, lockere Kleidung, Trigger vermeiden
- Bei Schlafproblemen: Abend-Routine, Schlafhygiene, Entspannungstechniken
- Bei Stimmungsschwankungen: Bewegung, soziale Kontakte, Selbstfürsorge
- Bei Brain Fog: Ausreichend Schlaf, Hydration, Omega-3
- Bei Gelenkschmerzen: Sanfte Bewegung, Anti-entzündliche Ernährung

KEINE generischen Ratschläge - alles muss personalisiert sein für {user_name}.""")

        final_prompt = "\n".join(context_parts)

        # FINAL DEBUG LOGGING
        print(f"\n{'='*60}")
        print(f"✅ SYSTEM PROMPT COMPLETED")
        print(f"{'='*60}")
        print(f"Total prompt length: {len(final_prompt)} characters")
        print(f"Contains 'PUBLIC WORKOUT TEMPLATES': {'PUBLIC WORKOUT TEMPLATES' in final_prompt}")
        print(f"Number of template mentions: {final_prompt.count('WORKOUT:')}")
        if 'PUBLIC WORKOUT TEMPLATES' in final_prompt:
            # Show a snippet of the templates section
            start_idx = final_prompt.find('PUBLIC WORKOUT TEMPLATES')
            snippet = final_prompt[start_idx:start_idx+500]
            print(f"Template section preview:\n{snippet}...")
        print(f"{'='*60}\n")

        return final_prompt
