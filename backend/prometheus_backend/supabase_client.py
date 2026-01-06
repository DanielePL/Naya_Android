"""
Supabase Client for Form Analysis Backend
Handles database operations for workout sessions and velocity metrics
"""

import os
from typing import Dict, List, Optional
from supabase import create_client, Client
from datetime import datetime


class SupabaseFormAnalysisClient:
    """Client for saving form analysis data to Supabase"""

    def __init__(self, use_service_role: bool = False):
        """
        Initialize Supabase client

        Args:
            use_service_role: If True, uses SUPABASE_SERVICE_ROLE_KEY which bypasses RLS.
                              Required for backend storage uploads and admin operations.

        Requires environment variables:
        - SUPABASE_URL: Your Supabase project URL
        - SUPABASE_KEY: Your Supabase anon key (for client operations)
        - SUPABASE_SERVICE_ROLE_KEY: Your service role key (for backend operations)
        """
        supabase_url = os.environ.get("SUPABASE_URL")

        # Use service_role key for backend operations (bypasses RLS)
        if use_service_role:
            supabase_key = os.environ.get("SUPABASE_SERVICE_ROLE_KEY")
            if not supabase_key or supabase_key == "YOUR_SERVICE_ROLE_KEY_HERE":
                print("⚠️ SUPABASE_SERVICE_ROLE_KEY not configured, falling back to anon key")
                supabase_key = os.environ.get("SUPABASE_KEY")
        else:
            supabase_key = os.environ.get("SUPABASE_KEY")

        if not supabase_url or not supabase_key:
            raise ValueError(
                "SUPABASE_URL and SUPABASE_KEY environment variables are required. "
                "Set them in your environment or .env file."
            )

        self.client: Client = create_client(supabase_url, supabase_key)
        self._is_admin = use_service_role

    def save_form_analysis(
        self,
        user_id: str,
        set_id: str,
        session_id: Optional[str],
        exercise_id: str,
        exercise_name: str,
        velocity_metrics: Dict,
        video_url: Optional[str] = None
    ) -> Dict:
        """
        Save form analysis to workout_sets table with JSONB velocity_metrics

        Args:
            user_id: UUID of the user
            set_id: UUID of the workout set (used as primary key)
            exercise_id: Exercise ID (e.g., "squat-back-barbell")
            exercise_name: Exercise name (e.g., "Barbell Back Squat")
            velocity_metrics: Dict from MovementVelocityCalculator
            video_url: Optional URL to analyzed video

        Returns:
            Dict with saved set_id and metrics info
        """
        try:
            # STEP 1: Create session if provided and doesn't exist
            if session_id:
                try:
                    # Check if session exists
                    existing_session = self.client.table('workout_sessions') \
                        .select('id') \
                        .eq('id', session_id) \
                        .execute()

                    # Create session if it doesn't exist
                    if not existing_session.data or len(existing_session.data) == 0:
                        session_data = {
                            "id": session_id,
                            "user_id": user_id,
                            "workout_name": "Workout Session",
                            "started_at": datetime.utcnow().isoformat()
                        }
                        self.client.table('workout_sessions').insert(session_data).execute()
                        print(f"✅ Created new session: {session_id}")
                except Exception as e:
                    print(f"⚠️ Failed to create session: {str(e)}")
                    # Continue anyway - session_id will be None

            # STEP 2: Extract data from velocity_metrics
            calibration = velocity_metrics.get('calibration', {})
            summary = velocity_metrics.get('summary', {})
            rep_data = velocity_metrics.get('rep_data', [])
            reps_detected = velocity_metrics.get('reps_detected', 0)

            # Build JSONB velocity_metrics object
            velocity_metrics_jsonb = {
                "reps_detected": reps_detected,
                "fps": velocity_metrics.get('fps', 30.0),
                "tracked_landmark": velocity_metrics.get('tracked_landmark', 'hip'),
                "exercise_type": exercise_id,
                "unit": calibration.get('unit', 'speed_index'),

                # Calibration info
                "calibration_tier": calibration.get('tier', 'relative'),
                "calibration_method": calibration.get('method', 'Relative Speed Index'),

                # Summary metrics
                "avg_peak_velocity": summary.get('avg_peak_velocity') or summary.get('avg_speed_index', 0),
                "max_peak_velocity": summary.get('max_peak_velocity') or summary.get('max_speed_index', 0),
                "min_peak_velocity": summary.get('min_peak_velocity') or summary.get('min_speed_index', 0),
                "velocity_drop_percent": summary.get('velocity_drop_percent', 0),
                "avg_rom_m": summary.get('avg_rom_m', 0),

                # Per-rep data
                "rep_data": [
                    {
                        "rep_number": rep.get('rep_number', i + 1),
                        "peak_velocity": rep.get('peak_velocity') or rep.get('speed_index', 0),
                        "avg_velocity": rep.get('avg_velocity', 0),
                        "rom_m": rep.get('rom', 0),
                        "duration_s": rep.get('duration_s', 0),
                        "concentric_duration_s": rep.get('concentric_duration_s', 0),
                        "eccentric_duration_s": rep.get('eccentric_duration_s', 0)
                    }
                    for i, rep in enumerate(rep_data)
                ]
            }

            # Upsert to workout_sets table
            workout_set_data = {
                "id": set_id,
                "session_id": session_id,  # Can be None for standalone form checks
                "exercise_id": exercise_id,
                "set_number": 1,
                "reps": reps_detected,
                "video_url": video_url or "",
                "video_storage_type": "device",
                "video_uploaded_at": datetime.utcnow().isoformat(),
                "velocity_metrics": velocity_metrics_jsonb
            }

            response = self.client.table('workout_sets') \
                .upsert(workout_set_data, on_conflict='id') \
                .execute()

            if not response.data or len(response.data) == 0:
                raise Exception("Failed to insert workout_set")

            return {
                "success": True,
                "set_id": set_id,
                "reps_saved": reps_detected
            }

        except Exception as e:
            return {
                "success": False,
                "error": str(e)
            }

    def _build_velocity_metrics_data(
        self,
        form_analysis_id: str,
        summary: Dict,
        unit: str
    ) -> Dict:
        """Build velocity_metrics record from summary data"""

        data = {
            "form_analysis_id": form_analysis_id,
            "velocity_drop_percent": summary.get('velocity_drop_percent', 0),
            "unit": unit,
            "note": summary.get('note')
        }

        # Add tier-specific fields
        if unit == 'm/s':
            # TIER 1/2: Calibrated m/s values
            data.update({
                "avg_peak_velocity": summary.get('avg_peak_velocity'),
                "max_peak_velocity": summary.get('max_peak_velocity'),
                "min_peak_velocity": summary.get('min_peak_velocity'),
                "avg_mean_velocity": summary.get('avg_mean_velocity'),
                "avg_rom_m": summary.get('avg_rom_m'),
                "best_rep_velocity": summary.get('best_rep_velocity'),
                "last_rep_velocity": summary.get('last_rep_velocity')
            })
        else:
            # TIER 3: Relative speed index
            data.update({
                "avg_peak_velocity": summary.get('avg_speed_index'),
                "max_peak_velocity": summary.get('max_speed_index'),
                "min_peak_velocity": summary.get('min_speed_index'),
                "avg_consistency": summary.get('avg_consistency'),
                "avg_rom_relative": summary.get('avg_rom_relative')
            })

        return data

    def _build_rep_metrics_batch(
        self,
        form_analysis_id: str,
        velocity_metrics_id: str,
        rep_data: List[Dict],
        unit: str
    ) -> List[Dict]:
        """Build batch of rep_metrics records"""

        rep_metrics_batch = []

        for rep in rep_data:
            rep_record = {
                "velocity_metrics_id": velocity_metrics_id,
                "form_analysis_id": form_analysis_id,
                "rep_number": rep.get('rep_number', 0),
                "duration_seconds": rep.get('duration_s'),
                "unit": unit
            }

            # Add tier-specific fields
            if unit == 'm/s':
                # TIER 1/2: Calibrated values
                rep_record.update({
                    "peak_velocity": rep.get('peak_velocity'),
                    "avg_velocity": rep.get('avg_velocity'),
                    "rom": rep.get('rom'),
                    "concentric_duration_seconds": rep.get('concentric_duration_s'),
                    "eccentric_duration_seconds": rep.get('eccentric_duration_s')
                })
            else:
                # TIER 3: Relative values
                rep_record.update({
                    "peak_velocity": rep.get('speed_index'),
                    "consistency_score": rep.get('consistency_score'),
                    "rom": rep.get('rom_relative')
                })

            rep_metrics_batch.append(rep_record)

        return rep_metrics_batch

    def get_user_form_analyses(
        self,
        user_id: str,
        limit: int = 10,
        exercise_id: Optional[str] = None
    ) -> List[Dict]:
        """
        Get user's recent form analyses

        Args:
            user_id: User UUID
            limit: Number of results to return
            exercise_id: Optional filter by exercise

        Returns:
            List of form analysis records with velocity metrics
        """
        try:
            query = self.client.table('form_analysis') \
                .select('*, velocity_metrics(*)') \
                .eq('user_id', user_id) \
                .order('created_at', desc=True) \
                .limit(limit)

            if exercise_id:
                query = query.eq('exercise_id', exercise_id)

            response = query.execute()

            return response.data if response.data else []

        except Exception as e:
            print(f"Error fetching form analyses: {str(e)}")
            return []

    def get_rep_details(
        self,
        form_analysis_id: str
    ) -> List[Dict]:
        """
        Get detailed per-rep metrics for a form analysis

        Args:
            form_analysis_id: Form analysis UUID

        Returns:
            List of rep metrics ordered by rep number
        """
        try:
            response = self.client.table('rep_metrics') \
                .select('*') \
                .eq('form_analysis_id', form_analysis_id) \
                .order('rep_number') \
                .execute()

            return response.data if response.data else []

        except Exception as e:
            print(f"Error fetching rep details: {str(e)}")
            return []
