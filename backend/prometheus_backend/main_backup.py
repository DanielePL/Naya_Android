"""
Prometheus Backend - MediaPipe Form Analysis API
Provides endpoints for analyzing workout form from video uploads using MediaPipe Pose
"""

from fastapi import FastAPI, File, UploadFile, HTTPException, Form, BackgroundTasks
from fastapi.responses import JSONResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
import aiofiles
import os
import shutil
import subprocess
from pathlib import Path
from typing import Optional, Dict
import json
from datetime import datetime
from dotenv import load_dotenv
import uuid
import threading
import httpx
from pydantic import BaseModel

# Use relative imports for modules in the same package
from .pose_processor import PoseProcessor
from .supabase_client import SupabaseFormAnalysisClient

# AI Coach imports
try:
    from ai_coach_service.program_generator import ProgramGenerator
    from ai_coach_service.exercise_database import ExerciseDatabase
except ImportError:
    print("⚠️ AI Coach service not available - check ai_coach_service folder")

# VBT Service imports
try:
    from .vbt_service import VBTAnalyzer
    VBT_AVAILABLE = True
    print("✅ VBT Service loaded - barbell detection available")
except ImportError as e:
    VBT_AVAILABLE = False
    print(f"⚠️ VBT Service not available: {e}")

# Load environment variables from .env file
load_dotenv()

app = FastAPI(
    title="Prometheus Form Analysis API",
    description="2D pose estimation and kinematics analysis for workout videos",
    version="1.1.0"  # Added admin dashboard
)

# CORS middleware for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify your Android app's domain
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration
UPLOAD_DIR = Path("uploads")
OUTPUT_DIR = Path("outputs")
VBT_UPLOAD_DIR = Path("vbt_uploads")
UPLOAD_DIR.mkdir(exist_ok=True)
OUTPUT_DIR.mkdir(exist_ok=True)
VBT_UPLOAD_DIR.mkdir(exist_ok=True)

# VBT Processing Storage (in-memory for now, use Redis in production)
vbt_uploads: Dict[str, Dict] = {}  # upload_id -> {chunks: [], total_chunks: int}
vbt_tasks: Dict[str, Dict] = {}    # task_id -> {status, progress, results, error}
vbt_lock = threading.Lock()


@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "service": "Prometheus Form Analysis API",
        "status": "running",
        "version": "1.0.0"
    }


@app.post("/api/v1/analyze-form")
async def analyze_form(
    video: UploadFile = File(...),
    exercise_type: Optional[str] = Form(None),
    calibrated: bool = Form(False),
    user_id: Optional[str] = Form(None),
    set_id: Optional[str] = Form(None),
    session_id: Optional[str] = Form(None),
    exercise_id: Optional[str] = Form(None),
    exercise_name: Optional[str] = Form(None)
):
    """
    Analyze workout form from uploaded video

    Parameters:
    - video: MP4 or AVI video file
    - exercise_type: Type of exercise (squat, deadlift, bench_press, etc.)
    - calibrated: Whether to apply calibration for metric measurements
    - user_id: Optional - UUID of user (for Supabase save)
    - set_id: Optional - UUID of workout set (for Supabase save)
    - exercise_id: Optional - Exercise ID (for Supabase save)
    - exercise_name: Optional - Exercise name (for Supabase save)

    Returns:
    - analysis_id: Unique ID for this analysis
    - pose_data: Joint positions and angles
    - video_url: URL to download analyzed video with overlays
    - supabase_saved: Whether data was saved to Supabase
    """

    if not video.filename.lower().endswith(('.mp4', '.avi', '.mov')):
        raise HTTPException(
            status_code=400,
            detail="Invalid file format. Please upload MP4, AVI, or MOV file."
        )

    # Generate unique analysis ID
    analysis_id = f"{datetime.now().strftime('%Y%m%d_%H%M%S')}_{video.filename}"
    video_path = UPLOAD_DIR / analysis_id
    output_path = OUTPUT_DIR / analysis_id.replace('.', '_')
    output_path.mkdir(exist_ok=True)

    # Get absolute paths
    video_path_abs = video_path.absolute()
    output_path_abs = output_path.absolute()

    try:
        # Save uploaded video
        async with aiofiles.open(video_path, 'wb') as out_file:
            content = await video.read()
            await out_file.write(content)

        # Normalize video (fix rotation, codec compatibility)
        normalized_video = video_path.parent / f"normalized_{video_path.name}"
        normalize_result = subprocess.run(
            [
                "ffmpeg",
                "-noautorotate",  # Don't auto-rotate based on metadata
                "-i", str(video_path_abs),
                "-vf", "transpose=1",  # 90 degrees clockwise (fixes Android portrait)
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-pix_fmt", "yuv420p",
                "-metadata:s:v", "rotate=0",  # Clear rotation metadata
                "-y",  # Overwrite
                str(normalized_video.absolute())
            ],
            capture_output=True,
            text=True,
            timeout=60
        )

        if normalize_result.returncode != 0:
            print(f"⚠️ Video normalization failed, using original: {normalize_result.stderr}")
            normalized_video = video_path  # Use original if normalization fails
        else:
            print(f"✅ Video normalized successfully")
            video_path_abs = normalized_video.absolute()

        # Process with MediaPipe Pose
        print(f"🎯 Processing video with MediaPipe...")
        processor = PoseProcessor()

        try:
            result = processor.process_video(
                video_path=normalized_video.absolute(),
                output_dir=output_path_abs,
                save_video=True,
                save_pose_data=True,
                exercise_type=exercise_type or "general"
            )

            print(f"✅ MediaPipe processing complete: {result['frames_processed']} frames")

            pose_data = result['pose_data']
            analyzed_video = result['output_video']

        except Exception as e:
            print(f"❌ MediaPipe processing failed: {str(e)}")
            raise HTTPException(
                status_code=500,
                detail=f"Pose analysis failed: {str(e)}"
            )

        # Calculate form metrics based on exercise type
        form_metrics = calculate_form_metrics(pose_data, exercise_type)

        # Get velocity metrics with calibration info
        velocity_metrics = result.get('velocity_metrics', {})
        calibration_info = velocity_metrics.get('calibration', {})

        response = {
            "analysis_id": analysis_id,
            "exercise_type": exercise_type,
            "timestamp": datetime.now().isoformat(),
            "pose_data": pose_data,
            "vbt_metrics": velocity_metrics,  # Changed from velocity_metrics to match Android
            "calibration": calibration_info,
            "weight_detected": None,  # Future: YOLO weight plate detection
            "form_metrics": form_metrics,
            "video_available": analyzed_video is not None,
            "download_url": f"/api/v1/download/{analysis_id}" if analyzed_video else None
        }

        # Debug: Print velocity metrics in response
        summary = velocity_metrics.get('summary', {})
        unit = summary.get('unit', 'speed_index')
        if unit == 'm/s':
            avg_value = summary.get('avg_peak_velocity', 0)
            print(f"📤 API Response: reps={velocity_metrics.get('reps_detected', 0)}, avg_peak={avg_value:.3f} m/s [{calibration_info.get('tier', 'unknown')}]")
        else:
            avg_value = summary.get('avg_speed_index', 0)
            print(f"📤 API Response: reps={velocity_metrics.get('reps_detected', 0)}, avg_speed_index={avg_value:.1f} [{calibration_info.get('tier', 'relative')}]")

        # Save to Supabase if user_id and set_id are provided
        supabase_result = None
        if user_id and set_id:
            try:
                print(f"💾 Saving to Supabase: user={user_id}, set={set_id}, session={session_id}")
                supabase_client = SupabaseFormAnalysisClient()

                # Use provided names or fallback to exercise_type
                final_exercise_id = exercise_id or exercise_type or "general"
                final_exercise_name = exercise_name or exercise_type or "General Exercise"

                supabase_result = supabase_client.save_form_analysis(
                    user_id=user_id,
                    set_id=set_id,
                    session_id=session_id,
                    exercise_id=final_exercise_id,
                    exercise_name=final_exercise_name,
                    velocity_metrics=velocity_metrics,
                    video_url=response.get('download_url')
                )

                if supabase_result.get('success'):
                    print(f"✅ Supabase save successful: form_analysis_id={supabase_result.get('form_analysis_id')}")
                    response['supabase'] = {
                        'saved': True,
                        'form_analysis_id': supabase_result.get('form_analysis_id'),
                        'reps_saved': supabase_result.get('reps_saved')
                    }
                else:
                    print(f"⚠️ Supabase save failed: {supabase_result.get('error')}")
                    response['supabase'] = {
                        'saved': False,
                        'error': supabase_result.get('error')
                    }
            except Exception as e:
                print(f"❌ Supabase save error: {str(e)}")
                response['supabase'] = {
                    'saved': False,
                    'error': str(e)
                }
        else:
            print(f"ℹ️ Skipping Supabase save (user_id or set_id not provided)")
            response['supabase'] = {
                'saved': False,
                'reason': 'user_id and set_id required for saving'
            }

        return JSONResponse(content=response)

    except subprocess.TimeoutExpired:
        raise HTTPException(
            status_code=504,
            detail="Analysis timeout. Video may be too long or complex."
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Analysis error: {str(e)}"
        )


@app.get("/api/v1/download/{analysis_id}")
async def download_analyzed_video(analysis_id: str):
    """Download analyzed video with pose overlays"""
    output_path = OUTPUT_DIR / analysis_id.replace('.', '_')

    if not output_path.exists():
        raise HTTPException(status_code=404, detail="Analysis not found")

    video_file = find_output_video(output_path)

    if not video_file:
        raise HTTPException(status_code=404, detail="Analyzed video not found")

    return FileResponse(
        video_file,
        media_type="video/mp4",
        filename=f"analyzed_{analysis_id}"
    )


@app.delete("/api/v1/analysis/{analysis_id}")
async def delete_analysis(analysis_id: str):
    """Delete analysis data and free up storage"""
    video_path = UPLOAD_DIR / analysis_id
    output_path = OUTPUT_DIR / analysis_id.replace('.', '_')

    deleted = False

    if video_path.exists():
        video_path.unlink()
        deleted = True

    if output_path.exists():
        shutil.rmtree(output_path)
        deleted = True

    if not deleted:
        raise HTTPException(status_code=404, detail="Analysis not found")

    return {"message": "Analysis deleted successfully"}


def find_output_video(output_dir: Path) -> Optional[Path]:
    """Find the analyzed video file in output directory"""
    video_files = list(output_dir.glob("*.mp4")) + list(output_dir.glob("*.avi"))
    return video_files[0] if video_files else None


def calculate_form_metrics(pose_data: dict, exercise_type: Optional[str]) -> dict:
    """
    Calculate exercise-specific form metrics

    For example:
    - Squat: knee angle, hip angle, depth, bar path
    - Deadlift: bar path, hip hinge angle, back angle
    - Bench Press: bar path, elbow angle, shoulder angle
    """

    if not exercise_type:
        return {"message": "No exercise type specified"}

    # Placeholder for exercise-specific analysis
    # In production, this would analyze the pose_data to extract specific metrics

    metrics = {
        "exercise": exercise_type,
        "analysis_available": False,
        "message": "Exercise-specific analysis coming soon"
    }

    if exercise_type.lower() in ["squat", "back_squat", "front_squat"]:
        metrics["target_angles"] = {
            "knee": {"min": 80, "max": 130},
            "hip": {"min": 70, "max": 120},
        }
        metrics["notes"] = "Squat depth and form analysis"

    elif exercise_type.lower() in ["deadlift", "romanian_deadlift"]:
        metrics["target_angles"] = {
            "hip": {"min": 90, "max": 180},
            "knee": {"min": 160, "max": 180},
        }
        metrics["notes"] = "Hip hinge and bar path analysis"

    elif exercise_type.lower() in ["bench_press", "incline_bench"]:
        metrics["target_angles"] = {
            "elbow": {"min": 75, "max": 90},
            "shoulder": {"min": 45, "max": 75},
        }
        metrics["notes"] = "Bar path and shoulder safety analysis"

    return metrics


# ========================================================================
# VBT CLOUD PROCESSING - Process video from Supabase Storage
# ========================================================================

class VBTProcessRequest(BaseModel):
    """Request body for VBT cloud processing"""
    user_id: str
    set_id: str
    video_url: str  # Signed URL to video in Supabase Storage
    exercise_type: str = "squat"
    weight_kg: Optional[float] = None
    one_rm: Optional[float] = None


@app.post("/api/v1/process-vbt-cloud")
async def process_vbt_from_cloud(
    request: VBTProcessRequest,
    background_tasks: BackgroundTasks
):
    """
    Process VBT video from Supabase Storage (async background processing)

    Flow:
    1. Download video from Supabase Storage (via signed URL)
    2. Process with YOLO VBT Analyzer
    3. Upload processed video back to Supabase Storage
    4. Update workout_sets table with VBT metrics

    Parameters:
    - user_id: UUID of the user
    - set_id: UUID of the workout set
    - video_url: Signed URL to video in Supabase Storage
    - exercise_type: Type of exercise (squat, bench, deadlift)
    - weight_kg: Optional weight used (for power calculation)
    - one_rm: Optional 1RM (for load % calculation)

    Returns:
    - task_id: ID to check processing status
    - status: "processing"
    """
    if not VBT_AVAILABLE:
        raise HTTPException(
            status_code=503,
            detail="VBT processing not available - YOLO model not loaded"
        )

    # Generate task ID
    task_id = str(uuid.uuid4())

    # Initialize task status
    with vbt_lock:
        vbt_tasks[task_id] = {
            "status": "processing",
            "progress": 0,
            "user_id": request.user_id,
            "set_id": request.set_id,
            "started_at": datetime.now().isoformat()
        }

    # Start background processing
    background_tasks.add_task(
        _process_vbt_cloud_task,
        task_id,
        request.user_id,
        request.set_id,
        request.video_url,
        request.exercise_type,
        request.weight_kg,
        request.one_rm
    )

    return {
        "task_id": task_id,
        "status": "processing",
        "message": "VBT analysis started in background"
    }


@app.get("/api/v1/vbt-status/{task_id}")
async def get_vbt_status(task_id: str):
    """Get status of VBT processing task"""
    with vbt_lock:
        task = vbt_tasks.get(task_id)

    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    return task


async def _process_vbt_cloud_task(
    task_id: str,
    user_id: str,
    set_id: str,
    video_url: str,
    exercise_type: str,
    weight_kg: Optional[float],
    one_rm: Optional[float]
):
    """Background task for VBT cloud processing"""
    try:
        print(f"[VBT] Starting cloud processing: task={task_id}, set={set_id}")

        # Update progress
        def update_progress(progress: int, message: str):
            with vbt_lock:
                if task_id in vbt_tasks:
                    vbt_tasks[task_id]["progress"] = progress
                    vbt_tasks[task_id]["message"] = message
            print(f"[VBT] {message} ({progress}%)")

        update_progress(5, "Downloading video from cloud...")

        # Step 1: Download video from Supabase Storage
        local_video_path = VBT_UPLOAD_DIR / f"{set_id}.mp4"

        async with httpx.AsyncClient(timeout=120.0) as client:
            response = await client.get(video_url)
            if response.status_code != 200:
                raise Exception(f"Failed to download video: HTTP {response.status_code}")

            with open(local_video_path, 'wb') as f:
                f.write(response.content)

        print(f"[VBT] Video downloaded: {local_video_path} ({local_video_path.stat().st_size / 1024 / 1024:.1f} MB)")
        update_progress(20, "Video downloaded, starting YOLO analysis...")

        # Step 2: Normalize video rotation (fix 90-degree rotation from Android)
        normalized_video = VBT_UPLOAD_DIR / f"{set_id}_normalized.mp4"
        normalize_result = subprocess.run(
            [
                "ffmpeg",
                "-noautorotate",
                "-i", str(local_video_path),
                "-vf", "transpose=1",  # 90 degrees clockwise
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-pix_fmt", "yuv420p",
                "-metadata:s:v", "rotate=0",
                "-y",
                str(normalized_video)
            ],
            capture_output=True,
            text=True,
            timeout=120
        )

        if normalize_result.returncode == 0:
            print(f"[VBT] Video normalized successfully")
            video_to_process = normalized_video
        else:
            print(f"[VBT] Normalization failed, using original: {normalize_result.stderr}")
            video_to_process = local_video_path

        update_progress(30, "Processing with YOLO barbell detection...")

        # Step 3: Process with VBT Analyzer
        analyzer = VBTAnalyzer()
        vbt_result = analyzer.analyze_video(
            video_path=str(video_to_process),
            exercise_type=exercise_type,
            weight_kg=weight_kg,
            one_rm=one_rm,
            progress_callback=lambda p, m: update_progress(30 + int(p * 0.5), m)
        )

        if not vbt_result.get("success"):
            raise Exception(vbt_result.get("error", "VBT analysis failed"))

        print(f"[VBT] Analysis complete: {vbt_result.get('totalReps')} reps detected")
        update_progress(80, "Uploading processed video...")

        # Step 4: Upload processed video back to Supabase (if needed)
        # For now, we'll use the original video URL and just update metrics
        # TODO: Add video with overlays and upload to Supabase Storage

        update_progress(90, "Saving VBT metrics to database...")

        # Step 5: Update workout_sets with VBT metrics
        try:
            supabase_client = SupabaseFormAnalysisClient()

            # Build velocity_metrics JSONB for workout_sets
            summary = vbt_result.get("summary", {})
            reps = vbt_result.get("reps", [])

            velocity_metrics = {
                "source": "yolo_backend",
                "exercise_type": exercise_type,
                "total_reps": vbt_result.get("totalReps", 0),
                "avg_velocity": summary.get("avgVelocity", 0),
                "peak_velocity": summary.get("peakVelocity", 0),
                "velocity_drop": summary.get("velocityDrop", 0),
                "technique_score": summary.get("avgTechniqueScore", 0),
                "total_tut": summary.get("totalTUT", 0),
                "estimated_1rm": summary.get("estimatedOneRM"),
                "load_percent": summary.get("loadPercent"),
                "fatigue_index": summary.get("fatigueIndex", 0),
                "overall_grade": summary.get("overallGrade", "N/A"),
                "warnings": vbt_result.get("warnings", []),
                "rep_data": [
                    {
                        "rep_number": r.get("repNumber"),
                        "peak_velocity": r.get("peakVelocity"),
                        "avg_velocity": r.get("avgVelocity"),
                        "concentric_time": r.get("concentricTime"),
                        "eccentric_time": r.get("eccentricTime"),
                        "total_time": r.get("totalTime"),
                        "path_accuracy": r.get("pathAccuracy"),
                        "technique_score": r.get("techniqueScore"),
                        "force": r.get("force"),
                        "power": r.get("power")
                    }
                    for r in reps
                ],
                "processed_at": datetime.now().isoformat()
            }

            # Update workout_sets table
            supabase_client.client.table('workout_sets') \
                .update({
                    "velocity_metrics": velocity_metrics,
                    "reps": vbt_result.get("totalReps", 0),
                    "vbt_processed": True,
                    "vbt_processed_at": datetime.now().isoformat()
                }) \
                .eq('id', set_id) \
                .execute()

            print(f"[VBT] Metrics saved to workout_sets: set_id={set_id}")

        except Exception as db_error:
            print(f"[VBT] Failed to save to Supabase: {db_error}")
            # Continue anyway - metrics are still available in response

        # Cleanup temp files
        if local_video_path.exists():
            local_video_path.unlink()
        if normalized_video.exists():
            normalized_video.unlink()

        update_progress(100, "Complete!")

        # Update task status to complete
        with vbt_lock:
            if task_id in vbt_tasks:
                vbt_tasks[task_id].update({
                    "status": "completed",
                    "progress": 100,
                    "results": vbt_result,
                    "completed_at": datetime.now().isoformat()
                })

        print(f"[VBT] Task {task_id} completed successfully")

    except Exception as e:
        print(f"[VBT] Task {task_id} failed: {str(e)}")
        import traceback
        traceback.print_exc()

        with vbt_lock:
            if task_id in vbt_tasks:
                vbt_tasks[task_id].update({
                    "status": "failed",
                    "error": str(e),
                    "failed_at": datetime.now().isoformat()
                })


@app.get("/api/v1/exercises")
async def get_supported_exercises():
    """Get list of exercises with form analysis support"""
    return {
        "supported_exercises": [
            {"id": "squat", "name": "Back Squat", "analysis_level": "full"},
            {"id": "front_squat", "name": "Front Squat", "analysis_level": "full"},
            {"id": "deadlift", "name": "Deadlift", "analysis_level": "full"},
            {"id": "bench_press", "name": "Bench Press", "analysis_level": "full"},
            {"id": "overhead_press", "name": "Overhead Press", "analysis_level": "basic"},
            {"id": "pull_up", "name": "Pull-up", "analysis_level": "basic"},
            {"id": "push_up", "name": "Push-up", "analysis_level": "basic"},
        ]
    }


# ============================================================================
# AI COACH ENDPOINTS
# ============================================================================

@app.post("/api/v1/ai-coach/generate-program")
async def generate_ai_program(request_data: dict):
    """
    Generate a complete training program using AI Coach

    Request Body:
    {
        "user_id": "uuid",
        "assessment_data": {
            "training_experience": 3,
            "training_days_per_week": 4,
            "session_duration_minutes": 75,
            "goals": ["strength", "hypertrophy"],
            "personal_records": {
                "back_squat": {"weight": 140, "reps": 1},
                "bench_press": {"weight": 100, "reps": 1}
            },
            "equipment_access": ["barbell", "dumbbells", "rack"],
            "injuries": []
        },
        "program_duration_weeks": 8,
        "num_workouts": 4
    }

    Returns:
    {
        "success": true,
        "program_summary": "...",
        "template_ids": ["uuid1", "uuid2"],
        "workouts_created": 4
    }
    """
    try:
        user_id = request_data.get('user_id')
        assessment_data = request_data.get('assessment_data')
        program_duration_weeks = request_data.get('program_duration_weeks', 8)
        num_workouts = request_data.get('num_workouts', 4)

        if not user_id or not assessment_data:
            raise HTTPException(
                status_code=400,
                detail="user_id and assessment_data are required"
            )

        # Initialize ProgramGenerator
        generator = ProgramGenerator()

        # Generate program
        result = generator.generate_program(
            user_id=user_id,
            assessment_data=assessment_data,
            program_duration_weeks=program_duration_weeks,
            num_workouts=num_workouts
        )

        if not result.get('success'):
            raise HTTPException(
                status_code=500,
                detail=result.get('error', 'Program generation failed')
            )

        return JSONResponse(content=result)

    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ AI Coach endpoint error: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"Internal server error: {str(e)}"
        )


@app.get("/api/v1/ai-coach/exercises/stats")
async def get_exercise_database_stats():
    """Get statistics about the exercise database"""
    try:
        exercise_db = ExerciseDatabase()
        stats = exercise_db.get_exercise_stats()
        return JSONResponse(content=stats)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to get exercise stats: {str(e)}"
        )


@app.post("/ai-coach")
async def get_coaching_cues(request_data: dict):
    """
    Chat endpoint for AI Coach

    Request:
    {
        "exercise": "Back Squat",
        "context": "Beginner with knee pain"
    }

    Response:
    {
        "exercise": "Back Squat",
        "cues": "AI coach response..."
    }
    """
    try:
        from ai_coach_service.ai_coach_client import AICoachClient

        exercise = request_data.get('exercise', '')
        context = request_data.get('context', '')

        # Build prompt for AI Coach
        system_prompt = """You are an experienced strength and conditioning coach having a conversation with your athlete.

Your coaching style:
- Speak naturally like a real coach, not like a textbook
- Be motivating and supportive, but direct and honest
- Use conversational language, avoid formal lists or bullet points
- NO markdown formatting (no **, ###, -, etc.) - just plain text
- Keep responses short and focused (2-4 sentences for greetings, 4-8 sentences for exercise questions)

When the athlete asks about exercises or training:
- Give specific, actionable cues they can use immediately
- Mention common mistakes you see people make
- Add safety tips if relevant

When the athlete greets you or makes small talk:
- Respond warmly and ask how their training is going
- Build rapport like a real coach would"""

        # Check if this is a general greeting/chat or exercise-specific
        user_message = exercise if exercise else "Hello"

        # If there's context, it's probably exercise-related
        if context:
            user_prompt = f"I'm working on {exercise}. {context}"
        elif exercise and len(exercise) > 50:
            # Long message is probably a chat message, not just exercise name
            user_prompt = exercise
        elif exercise and any(word in exercise.lower() for word in ['squat', 'bench', 'deadlift', 'press', 'pull', 'push', 'row', 'curl', 'lunge', 'raise', 'extension', 'flexion']):
            # Looks like an exercise name
            user_prompt = f"Can you give me some coaching tips for {exercise}?"
        else:
            # General chat/greeting
            user_prompt = exercise if exercise else "Hello coach!"

        # Use OpenAI Client
        ai_client = AICoachClient()
        result = ai_client.client.chat.completions.create(
            model=ai_client.model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            max_tokens=500,  # Shorter for coach-style responses
            temperature=0.8  # Higher for more natural, varied responses
        )

        response_text = result.choices[0].message.content

        return {
            "exercise": exercise,
            "cues": response_text
        }

    except Exception as e:
        print(f"❌ Coaching cues error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error generating coaching cues: {str(e)}"
        )


@app.get("/api/v1/ai-coach/test")
async def test_ai_coach():
    """Test AI Coach configuration and connection"""
    try:
        generator = ProgramGenerator()
        is_working = generator.test_ai_connection()

        return {
            "ai_coach_available": is_working,
            "openai_api_configured": os.environ.get("OPENAI_API_KEY") is not None,
            "supabase_configured": os.environ.get("SUPABASE_URL") is not None
        }
    except Exception as e:
        return {
            "ai_coach_available": False,
            "error": str(e)
        }


# ============================================================
# AI COACH - PERSISTENT CHAT ENDPOINTS
# ============================================================

@app.post("/api/v1/ai-coach/chat")
async def chat_with_coach(request_data: dict):
    """
    Chat with AI Coach with persistent conversation history

    Request:
    {
        "user_id": "uuid",
        "conversation_id": "uuid" or null (creates new conversation),
        "message": "I want to improve my squat",
        "context": {
            "current_screen": "home",
            "selected_exercise_id": null
        }
    }

    Response:
    {
        "conversation_id": "uuid",
        "message": "AI response...",
        "actions": []  // Future: workout_created, exercise_added, etc.
    }
    """
    try:
        from ai_coach_service.ai_coach_client import AICoachClient
        from ai_coach_service.conversation_manager import ConversationManager

        user_id = request_data.get('user_id')
        conversation_id = request_data.get('conversation_id')
        message = request_data.get('message', '')
        context = request_data.get('context', {})

        if not user_id:
            raise HTTPException(status_code=400, detail="user_id is required")
        if not message:
            raise HTTPException(status_code=400, detail="message is required")

        conv_manager = ConversationManager()

        # Create new conversation if needed
        if not conversation_id:
            conv_result = conv_manager.create_conversation(
                user_id=user_id,
                title=None  # Will be auto-generated from first message
            )
            if not conv_result.get('success'):
                raise HTTPException(
                    status_code=500,
                    detail=f"Failed to create conversation: {conv_result.get('error')}"
                )
            conversation_id = conv_result['conversation_id']
            print(f"✨ Created new conversation: {conversation_id}")

        # Get conversation history (last 20 messages for context)
        history = conv_manager.get_recent_messages(conversation_id, count=20)

        # Build rich system prompt with user context from Supabase
        from ai_coach_service.user_context_builder import UserContextBuilder

        context_builder = UserContextBuilder()
        user_context = context_builder.build_user_context(user_id)
        system_prompt = context_builder.format_system_prompt(user_context)

        print(f"📊 User context loaded: Profile={user_context.get('profile') is not None}, "
              f"PRs={len(user_context.get('prs', []))}, "
              f"Exercises={len(user_context.get('exercises', []))}, "
              f"UserWorkouts={len(user_context.get('workouts', []))}, "
              f"PublicTemplates={len(user_context.get('public_templates', []))}")

        # Build messages array for OpenAI
        messages = [{"role": "system", "content": system_prompt}]

        # Add conversation history
        for msg in history:
            messages.append({
                "role": msg["role"],
                "content": msg["content"]
            })

        # Add new user message
        messages.append({"role": "user", "content": message})

        # Save user message to database
        conv_manager.add_message(
            conversation_id=conversation_id,
            role="user",
            content=message,
            metadata=context
        )

        # Call OpenAI API
        ai_client = AICoachClient()
        result = ai_client.client.chat.completions.create(
            model=ai_client.model,
            messages=messages,
            max_tokens=500,
            temperature=0.8
        )

        response_text = result.choices[0].message.content

        # Save assistant response to database
        conv_manager.add_message(
            conversation_id=conversation_id,
            role="assistant",
            content=response_text,
            metadata={
                "model": ai_client.model,
                "tokens_input": result.usage.prompt_tokens,
                "tokens_output": result.usage.completion_tokens
            }
        )

        # Update conversation title if it's the first exchange
        if len(history) == 0:
            title = message[:50] + "..." if len(message) > 50 else message
            conv_manager.update_conversation_title(conversation_id, title)

        print(f"💬 Chat: {len(history)+1} messages in conversation {conversation_id}")

        # Detect and parse workout recommendations
        from ai_coach_service.workout_parser import WorkoutParser
        import re

        workout_parser = WorkoutParser()
        actions = []

        if workout_parser.detect_workout(response_text):
            parsed_workout = workout_parser.parse_workout(response_text)
            if parsed_workout:
                print(f"🏋️ Detected workout: {parsed_workout['name']} with {len(parsed_workout['exercises'])} exercises")
                actions.append({
                    "type": "workout_created",
                    "data": {"workout": parsed_workout}
                })

        # Detect template recommendations [RECOMMEND_TEMPLATE:template_id:Template Name]
        template_pattern = r'\[RECOMMEND_TEMPLATE:([^:]+):([^\]]+)\]'
        template_matches = re.findall(template_pattern, response_text)

        for template_id, template_name in template_matches:
            template_id = template_id.strip()
            template_name = template_name.strip()

            # Get template details from context
            template_data = None
            for template in user_context.get('public_templates', []):
                if template.get('id') == template_id:
                    template_data = template
                    break

            if template_data:
                print(f"📋 Detected template recommendation: {template_name} (ID: {template_id})")
                actions.append({
                    "type": "recommend_template",
                    "data": {
                        "template": {
                            "template_id": template_id,
                            "name": template_name,
                            "description": template_data.get('description', ''),
                            "sports": template_data.get('sports', []),
                            "exercise_count": len(template_data.get('exercises', [])),
                            "exercise_names": [ex.get('name', '') for ex in template_data.get('exercises', [])[:5]]
                        }
                    }
                })
            else:
                print(f"⚠️ Template ID not found in context: {template_id}")

        # Clean the response text by removing the recommendation markers
        clean_response = re.sub(template_pattern, '', response_text).strip()

        return {
            "conversation_id": conversation_id,
            "message": clean_response,
            "actions": actions
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ Chat error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error processing chat: {str(e)}"
        )


@app.get("/api/v1/ai-coach/conversations")
async def get_user_conversations(user_id: str):
    """
    Get all conversations for a user

    Query params:
        user_id: User's UUID

    Response:
    [
        {
            "id": "uuid",
            "title": "Squat program discussion",
            "message_count": 15,
            "last_message": "Great progress!",
            "last_message_role": "assistant",
            "created_at": "2024-01-01T00:00:00",
            "updated_at": "2024-01-01T12:00:00"
        }
    ]
    """
    try:
        from ai_coach_service.conversation_manager import ConversationManager

        if not user_id:
            raise HTTPException(status_code=400, detail="user_id is required")

        conv_manager = ConversationManager()
        conversations = conv_manager.get_user_conversations(
            user_id=user_id,
            include_archived=False,
            limit=50
        )

        return conversations

    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ Error getting conversations: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving conversations: {str(e)}"
        )


@app.get("/api/v1/ai-coach/conversations/{conversation_id}/messages")
async def get_conversation_messages(conversation_id: str):
    """
    Get all messages in a conversation

    Path params:
        conversation_id: Conversation UUID

    Response:
    [
        {
            "id": "uuid",
            "role": "user",
            "content": "I want to improve my squat",
            "metadata": {},
            "created_at": "2024-01-01T00:00:00"
        }
    ]
    """
    try:
        from ai_coach_service.conversation_manager import ConversationManager

        conv_manager = ConversationManager()
        messages = conv_manager.get_conversation_messages(
            conversation_id=conversation_id,
            limit=200
        )

        return messages

    except Exception as e:
        print(f"❌ Error getting messages: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error retrieving messages: {str(e)}"
        )


@app.delete("/api/v1/ai-coach/conversations/{conversation_id}")
async def delete_conversation(conversation_id: str):
    """
    Archive a conversation (soft delete)

    Path params:
        conversation_id: Conversation UUID

    Response:
    {
        "success": true,
        "conversation_id": "uuid"
    }
    """
    try:
        from ai_coach_service.conversation_manager import ConversationManager

        conv_manager = ConversationManager()
        result = conv_manager.archive_conversation(conversation_id)

        if not result.get('success'):
            raise HTTPException(
                status_code=500,
                detail=result.get('error', 'Failed to archive conversation')
            )

        return result

    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ Error archiving conversation: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error archiving conversation: {str(e)}"
        )


@app.post("/api/v1/ai-coach/save-workout")
async def save_ai_workout(request_data: dict):
    """
    Save AI-generated workout to user's workout templates

    Request:
    {
        "user_id": "uuid",
        "workout": {
            "name": "Upper Body Strength",
            "description": "...",
            "exercises": [...]
        }
    }

    Response:
    {
        "success": true,
        "workout_template_id": "uuid"
    }
    """
    try:
        from ai_coach_service.user_context_builder import UserContextBuilder

        user_id = request_data.get('user_id')
        workout = request_data.get('workout')

        if not user_id or not workout:
            raise HTTPException(status_code=400, detail="user_id and workout required")

        # Get Supabase client
        context_builder = UserContextBuilder()
        supabase = context_builder.client

        # Prepare workout template data
        workout_data = {
            "user_id": user_id,
            "name": workout.get("name", "AI Coach Workout"),
            "description": workout.get("description", "Generated by AI Coach"),
            "created_by": "ai_coach",
            "is_public": False
        }

        # Insert workout template
        result = supabase.table("workout_templates")\
            .insert(workout_data)\
            .execute()

        if not result.data or len(result.data) == 0:
            raise Exception("Failed to create workout template")

        workout_template_id = result.data[0]["id"]
        print(f"✅ Created workout template: {workout_template_id}")

        # Insert exercises
        exercises_data = []
        for exercise in workout.get("exercises", []):
            # Find matching exercise in database
            exercise_search = supabase.table("exercises")\
                .select("id")\
                .ilike("name", f"%{exercise['exercise_name']}%")\
                .limit(1)\
                .execute()

            if exercise_search.data and len(exercise_search.data) > 0:
                exercise_id = exercise_search.data[0]["id"]

                exercises_data.append({
                    "workout_template_id": workout_template_id,
                    "exercise_id": exercise_id,
                    "order_index": exercise.get("order_index", 0),
                    "target_sets": exercise.get("sets", 3),
                    "target_reps": exercise.get("reps", 10),
                    "notes": exercise.get("notes") or exercise.get("intensity")
                })

        # Bulk insert exercises
        if exercises_data:
            supabase.table("workout_template_exercises")\
                .insert(exercises_data)\
                .execute()

            print(f"✅ Added {len(exercises_data)} exercises to workout")

        return {
            "success": True,
            "workout_template_id": workout_template_id,
            "exercises_added": len(exercises_data)
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ Error saving workout: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error saving workout: {str(e)}"
        )


# ============================================================
# ADMIN DASHBOARD - COMPREHENSIVE COST TRACKING & ANALYTICS
# ============================================================

ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD", "prometheus_admin_2024")


def verify_admin(password: str) -> bool:
    """Verify admin password"""
    return password == ADMIN_PASSWORD


@app.get("/admin")
async def admin_dashboard(password: Optional[str] = None):
    """
    Admin Dashboard - Comprehensive cost tracking and analytics

    Access: /admin?password=YOUR_ADMIN_PASSWORD
    """
    if not password or not verify_admin(password):
        return HTMLResponse(content="""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Prometheus Admin - Login</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                       display: flex; justify-content: center; align-items: center;
                       min-height: 100vh; margin: 0; background: #0f0f0f; color: #fff; }
                .login-box { background: #1a1a1a; padding: 40px; border-radius: 12px;
                             box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
                h1 { color: #ff6b35; margin-bottom: 30px; }
                input { padding: 12px 16px; font-size: 16px; border: 1px solid #333;
                        border-radius: 8px; background: #2a2a2a; color: #fff; width: 250px; }
                button { padding: 12px 24px; font-size: 16px; background: #ff6b35;
                         color: #fff; border: none; border-radius: 8px; cursor: pointer;
                         margin-top: 15px; }
                button:hover { background: #ff8555; }
            </style>
        </head>
        <body>
            <div class="login-box">
                <h1>🔒 Prometheus Admin</h1>
                <form method="get">
                    <input type="password" name="password" placeholder="Admin Password" required>
                    <br>
                    <button type="submit">Login</button>
                </form>
            </div>
        </body>
        </html>
        """, status_code=200)

    # Authenticated - show comprehensive dashboard
    return HTMLResponse(content=f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Prometheus Admin Dashboard</title>
        <style>
            * {{ box-sizing: border-box; }}
            body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                   margin: 0; padding: 20px; background: #0f0f0f; color: #e0e0e0; }}
            .header {{ display: flex; justify-content: space-between; align-items: center;
                      margin-bottom: 30px; padding-bottom: 20px; border-bottom: 1px solid #333; }}
            h1 {{ color: #ff6b35; margin: 0; }}
            .refresh-btn {{ background: #333; color: #fff; border: none; padding: 10px 20px;
                           border-radius: 8px; cursor: pointer; }}
            .refresh-btn:hover {{ background: #444; }}
            .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                    gap: 15px; margin-bottom: 25px; }}
            .card {{ background: #1a1a1a; border-radius: 12px; padding: 20px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.2); }}
            .card.highlight {{ border: 2px solid #ff6b35; }}
            .card.green {{ border-left: 4px solid #4ade80; }}
            .card.red {{ border-left: 4px solid #ef4444; }}
            .card.blue {{ border-left: 4px solid #60a5fa; }}
            .card.orange {{ border-left: 4px solid #fb923c; }}
            .card h2 {{ color: #ff6b35; margin-top: 0; font-size: 14px; text-transform: uppercase; }}
            .card h3 {{ color: #888; margin: 15px 0 10px 0; font-size: 13px; }}
            .stat {{ font-size: 28px; font-weight: bold; color: #fff; }}
            .stat.small {{ font-size: 22px; }}
            .stat-label {{ color: #666; font-size: 12px; margin-top: 4px; }}
            .profit {{ color: #4ade80; }}
            .loss {{ color: #ef4444; }}
            .table {{ width: 100%; border-collapse: collapse; font-size: 14px; }}
            .table th, .table td {{ padding: 10px 12px; text-align: left; border-bottom: 1px solid #333; }}
            .table th {{ color: #ff6b35; font-weight: 600; font-size: 12px; text-transform: uppercase; }}
            .table tr:hover {{ background: #222; }}
            .cost {{ color: #4ade80; }}
            .tabs {{ display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }}
            .tab {{ padding: 8px 16px; background: #222; border: none; color: #888;
                   border-radius: 8px; cursor: pointer; font-size: 13px; }}
            .tab.active {{ background: #ff6b35; color: #fff; }}
            .section {{ margin-bottom: 30px; }}
            .section-title {{ color: #ff6b35; font-size: 18px; margin-bottom: 15px;
                             padding-bottom: 10px; border-bottom: 1px solid #333; }}
            .form-row {{ display: flex; gap: 10px; margin-bottom: 10px; flex-wrap: wrap; }}
            .form-row input, .form-row select {{ padding: 10px 12px; background: #222; border: 1px solid #333;
                                                 border-radius: 6px; color: #fff; font-size: 14px; }}
            .form-row input {{ flex: 1; min-width: 150px; }}
            .form-row select {{ min-width: 140px; }}
            .btn {{ padding: 10px 20px; background: #ff6b35; color: #fff; border: none;
                   border-radius: 6px; cursor: pointer; font-size: 14px; }}
            .btn:hover {{ background: #ff8555; }}
            .btn.secondary {{ background: #333; }}
            .btn.danger {{ background: #ef4444; }}
            .comparison {{ display: flex; gap: 20px; align-items: center; margin-top: 15px; }}
            .comparison-item {{ text-align: center; }}
            .comparison-vs {{ color: #666; font-size: 24px; }}
            .badge {{ display: inline-block; padding: 4px 10px; border-radius: 12px;
                     font-size: 11px; font-weight: 600; }}
            .badge.below {{ background: #064e3b; color: #4ade80; }}
            .badge.average {{ background: #3f3f00; color: #fbbf24; }}
            .badge.high {{ background: #450a0a; color: #ef4444; }}
            .modal {{ display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                     background: rgba(0,0,0,0.8); z-index: 1000; justify-content: center; align-items: center; }}
            .modal.active {{ display: flex; }}
            .modal-content {{ background: #1a1a1a; padding: 30px; border-radius: 12px;
                             max-width: 500px; width: 90%; }}
            .modal-title {{ color: #ff6b35; margin-top: 0; }}
            .close-btn {{ position: absolute; top: 15px; right: 20px; color: #888;
                         font-size: 24px; cursor: pointer; }}
        </style>
    </head>
    <body>
        <div class="header">
            <h1>📊 Prometheus Cost Analytics</h1>
            <button class="refresh-btn" onclick="loadAllData()">🔄 Refresh All</button>
        </div>

        <!-- ═══════════════════════════════════════════════════════════════ -->
        <!-- COMPREHENSIVE SUMMARY -->
        <!-- ═══════════════════════════════════════════════════════════════ -->
        <div class="section">
            <div class="section-title">💰 Comprehensive Monthly Summary</div>
            <div class="grid">
                <div class="card highlight">
                    <h2>Total Monthly Cost</h2>
                    <div class="stat" id="total-monthly-cost">$0.00</div>
                    <div class="stat-label">Variable + Fixed Costs</div>
                </div>
                <div class="card green">
                    <h2>Variable Costs (API)</h2>
                    <div class="stat small" id="variable-costs">$0.00</div>
                    <div class="stat-label">OpenAI, VBT, Storage</div>
                </div>
                <div class="card orange">
                    <h2>Fixed Costs</h2>
                    <div class="stat small" id="fixed-costs">$0.00</div>
                    <div class="stat-label">Labor, Subscriptions, Infra</div>
                </div>
                <div class="card blue">
                    <h2>Active Users</h2>
                    <div class="stat small" id="active-users">0</div>
                    <div class="stat-label">This month</div>
                </div>
                <div class="card highlight">
                    <h2>Cost Per User</h2>
                    <div class="stat" id="cost-per-user">$0.00</div>
                    <div class="stat-label">Total cost / active users</div>
                </div>
                <div class="card">
                    <h2>Industry Comparison</h2>
                    <div class="stat small" id="industry-avg">$0.70</div>
                    <div class="stat-label">Avg fitness app cost/user</div>
                    <div style="margin-top: 10px;">
                        <span class="badge below" id="cost-badge">Calculating...</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- ═══════════════════════════════════════════════════════════════ -->
        <!-- REVENUE & APP STORE COMMISSIONS -->
        <!-- ═══════════════════════════════════════════════════════════════ -->
        <div class="section">
            <div class="section-title">📱 Revenue & App Store Commissions</div>
            <div class="grid">
                <div class="card green">
                    <h2>Gross Revenue</h2>
                    <div class="stat small" id="gross-revenue">$0.00</div>
                    <div class="stat-label">Before app store cut</div>
                </div>
                <div class="card red">
                    <h2>App Store Commission</h2>
                    <div class="stat small" id="app-store-commission">$0.00</div>
                    <div class="stat-label">Small Biz Program: 15%</div>
                </div>
                <div class="card green">
                    <h2>Net Revenue</h2>
                    <div class="stat small" id="net-revenue">$0.00</div>
                    <div class="stat-label">After commission</div>
                </div>
                <div class="card" id="profit-card">
                    <h2>Monthly Profit/Loss</h2>
                    <div class="stat small profit" id="monthly-profit">$0.00</div>
                    <div class="stat-label">Net revenue - total costs</div>
                </div>
            </div>
        </div>

        <!-- ═══════════════════════════════════════════════════════════════ -->
        <!-- BREAK-EVEN ANALYSIS -->
        <!-- ═══════════════════════════════════════════════════════════════ -->
        <div class="section">
            <div class="section-title">🎯 Break-Even Analysis</div>
            <div class="grid">
                <div class="card highlight" style="grid-column: span 2;">
                    <h2>Subscribers Needed for Break-Even</h2>
                    <div style="display: flex; gap: 30px; margin-top: 15px;">
                        <div style="flex: 1; text-align: center; padding: 15px; background: #222; border-radius: 8px;">
                            <div style="color: #888; font-size: 12px; margin-bottom: 5px;">MONTHLY SUBS ONLY</div>
                            <div class="stat" id="be-monthly-users" style="color: #60a5fa;">0</div>
                            <div class="stat-label">@ $<span id="be-monthly-price">9.99</span>/mo net</div>
                            <div class="stat-label" style="color: #888;">LTV: $<span id="be-monthly-ltv">0</span></div>
                        </div>
                        <div style="flex: 1; text-align: center; padding: 15px; background: #222; border-radius: 8px;">
                            <div style="color: #888; font-size: 12px; margin-bottom: 5px;">YEARLY SUBS ONLY</div>
                            <div class="stat" id="be-yearly-users" style="color: #4ade80;">0</div>
                            <div class="stat-label">@ $<span id="be-yearly-price">59.99</span>/yr net</div>
                            <div class="stat-label" style="color: #888;">LTV: $<span id="be-yearly-ltv">0</span></div>
                        </div>
                        <div style="flex: 1; text-align: center; padding: 15px; background: #333; border-radius: 8px; border: 1px solid #ff6b35;">
                            <div style="color: #ff6b35; font-size: 12px; margin-bottom: 5px;">REALISTIC MIX (65/35)</div>
                            <div class="stat" id="be-mixed-users" style="color: #ff6b35;">0</div>
                            <div class="stat-label">65% monthly / 35% yearly</div>
                            <div class="stat-label" style="color: #888;">Blended LTV: $<span id="be-mixed-ltv">0</span></div>
                        </div>
                    </div>
                </div>
                <div class="card">
                    <h2>Subscription Metrics</h2>
                    <div style="margin-top: 10px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span class="stat-label">Monthly Churn Rate:</span>
                            <span style="color: #ef4444;"><span id="be-churn">12</span>%</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span class="stat-label">Yearly Renewal Rate:</span>
                            <span style="color: #4ade80;"><span id="be-renewal">45</span>%</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span class="stat-label">Avg Monthly Lifetime:</span>
                            <span><span id="be-monthly-lifetime">4.5</span> months</span>
                        </div>
                        <div style="display: flex; justify-content: space-between;">
                            <span class="stat-label">Avg Yearly Renewals:</span>
                            <span><span id="be-yearly-lifetime">1.8</span> years</span>
                        </div>
                    </div>
                    <div class="stat-label" style="margin-top: 15px; font-style: italic;">
                        Based on fitness app industry benchmarks
                    </div>
                </div>
                <div class="card">
                    <h2>Monthly Costs to Cover</h2>
                    <div style="margin-top: 10px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span class="stat-label">Fixed Costs:</span>
                            <span>$<span id="be-fixed">0</span></span>
                        </div>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                            <span class="stat-label">Variable Costs/User:</span>
                            <span>$<span id="be-variable">0.05</span></span>
                        </div>
                        <div style="display: flex; justify-content: space-between; padding-top: 8px; border-top: 1px solid #333;">
                            <span style="font-weight: bold;">Total Monthly:</span>
                            <span style="font-weight: bold; color: #ff6b35;">$<span id="be-total-monthly">0</span></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- ═══════════════════════════════════════════════════════════════ -->
        <!-- TABS FOR DETAILED VIEWS -->
        <!-- ═══════════════════════════════════════════════════════════════ -->
        <div class="tabs">
            <button class="tab active" onclick="showTab('daily')">📅 Daily Costs</button>
            <button class="tab" onclick="showTab('users')">👤 Per-User Costs</button>
            <button class="tab" onclick="showTab('events')">📊 Event Types</button>
            <button class="tab" onclick="showTab('fixed')">📋 Fixed Costs</button>
            <button class="tab" onclick="showTab('revenue')">💵 Revenue</button>
        </div>

        <div class="card">
            <!-- Daily Costs Tab -->
            <div id="daily-tab">
                <h2>Daily Variable Cost Breakdown</h2>
                <table class="table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>OpenAI Vision</th>
                            <th>VBT Analysis</th>
                            <th>AI Coach</th>
                            <th>Storage</th>
                            <th>Total Cost</th>
                        </tr>
                    </thead>
                    <tbody id="daily-table">
                        <tr><td colspan="6">Loading data...</td></tr>
                    </tbody>
                </table>
            </div>

            <!-- Per-User Costs Tab -->
            <div id="users-tab" style="display:none;">
                <h2>Per-User Costs (Lifetime)</h2>
                <table class="table">
                    <thead>
                        <tr>
                            <th>User ID</th>
                            <th>First Activity</th>
                            <th>Active Days</th>
                            <th>Total Events</th>
                            <th>Total Cost</th>
                        </tr>
                    </thead>
                    <tbody id="users-table">
                        <tr><td colspan="5">Loading...</td></tr>
                    </tbody>
                </table>
            </div>

            <!-- Event Types Tab -->
            <div id="events-tab" style="display:none;">
                <h2>Event Type Breakdown (This Month)</h2>
                <table class="table">
                    <thead>
                        <tr>
                            <th>Event Type</th>
                            <th>Count</th>
                            <th>Avg Tokens</th>
                            <th>Total Cost</th>
                        </tr>
                    </thead>
                    <tbody id="events-table">
                        <tr><td colspan="4">Loading...</td></tr>
                    </tbody>
                </table>
            </div>

            <!-- Fixed Costs Tab -->
            <div id="fixed-tab" style="display:none;">
                <h2>Fixed Monthly Costs</h2>
                <div class="form-row" style="margin-bottom: 20px;">
                    <input type="text" id="fc-name" placeholder="Name (e.g., Claude Max)">
                    <select id="fc-category">
                        <option value="subscription">Subscription</option>
                        <option value="labor">Labor</option>
                        <option value="infrastructure">Infrastructure</option>
                        <option value="marketing">Marketing</option>
                        <option value="app_store_fees">App Store Fees</option>
                        <option value="other">Other</option>
                    </select>
                    <input type="number" id="fc-amount" placeholder="Amount ($)" step="0.01">
                    <select id="fc-recurrence">
                        <option value="monthly">Monthly</option>
                        <option value="yearly">Yearly</option>
                        <option value="one_time">One-time</option>
                    </select>
                    <button class="btn" onclick="addFixedCost()">+ Add Cost</button>
                </div>
                <table class="table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Category</th>
                            <th>Amount</th>
                            <th>Recurrence</th>
                            <th>Monthly Equiv.</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="fixed-table">
                        <tr><td colspan="6">Loading...</td></tr>
                    </tbody>
                </table>
            </div>

            <!-- Revenue Tab -->
            <div id="revenue-tab" style="display:none;">
                <h2>Revenue Tracking</h2>

                <!-- Month Filter -->
                <div style="display: flex; gap: 15px; align-items: center; margin-bottom: 20px; padding: 15px; background: #222; border-radius: 8px;">
                    <label style="color: #888;">Filter by Month:</label>
                    <input type="month" id="rev-month-filter" style="padding: 8px 12px; background: #333; border: 1px solid #444; border-radius: 6px; color: #fff;">
                    <button class="btn secondary" onclick="loadRevenue()" style="padding: 8px 16px;">Show All</button>
                    <button class="btn" onclick="filterRevenueByMonth()" style="padding: 8px 16px;">Filter</button>
                </div>

                <!-- Monthly Summary -->
                <div id="monthly-summary" style="display: none; margin-bottom: 20px; padding: 20px; background: linear-gradient(135deg, #1a3a1a 0%, #1a1a1a 100%); border-radius: 8px; border: 1px solid #2a5a2a;">
                    <h3 style="color: #4ade80; margin: 0 0 15px 0;">📅 Monthly Summary: <span id="summary-month">-</span></h3>
                    <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px;">
                        <div style="text-align: center;">
                            <div style="color: #888; font-size: 12px;">GROSS REVENUE</div>
                            <div style="color: #fff; font-size: 24px; font-weight: bold;" id="summary-gross">$0.00</div>
                        </div>
                        <div style="text-align: center;">
                            <div style="color: #888; font-size: 12px;">APP STORE FEE</div>
                            <div style="color: #ef4444; font-size: 24px; font-weight: bold;" id="summary-commission">-$0.00</div>
                        </div>
                        <div style="text-align: center;">
                            <div style="color: #888; font-size: 12px;">NET REVENUE</div>
                            <div style="color: #4ade80; font-size: 24px; font-weight: bold;" id="summary-net">$0.00</div>
                        </div>
                        <div style="text-align: center;">
                            <div style="color: #888; font-size: 12px;">TRANSACTIONS</div>
                            <div style="color: #60a5fa; font-size: 24px; font-weight: bold;" id="summary-count">0</div>
                        </div>
                    </div>
                </div>

                <!-- Add Revenue Form -->
                <div style="margin-bottom: 20px; padding: 15px; background: #1a1a1a; border-radius: 8px; border: 1px solid #333;">
                    <div style="color: #ff6b35; font-size: 14px; margin-bottom: 10px;">➕ Add New Revenue Entry</div>
                    <div class="form-row">
                        <select id="rev-platform">
                            <option value="ios">iOS (Apple)</option>
                            <option value="android">Android (Google)</option>
                            <option value="web">Web (No Commission)</option>
                        </select>
                        <select id="rev-type">
                            <option value="subscription">Subscription</option>
                            <option value="one_time_purchase">One-time Purchase</option>
                            <option value="ad_revenue">Ad Revenue</option>
                        </select>
                        <input type="number" id="rev-amount" placeholder="Gross Revenue ($)" step="0.01">
                        <input type="date" id="rev-date" title="Revenue Date">
                        <button class="btn" onclick="addRevenue()">+ Add Revenue</button>
                    </div>
                </div>

                <table class="table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Platform</th>
                            <th>Type</th>
                            <th>Gross Revenue</th>
                            <th>Commission (15%)</th>
                            <th>Net Revenue</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="revenue-table">
                        <tr><td colspan="7">Loading...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- ═══════════════════════════════════════════════════════════════ -->
        <!-- QUICK REFERENCE: COST RATES -->
        <!-- ═══════════════════════════════════════════════════════════════ -->
        <div class="card" style="margin-top: 20px;">
            <h2>📚 Current Cost Rates</h2>
            <div class="grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
                <div>
                    <h3>OpenAI GPT-4o Vision</h3>
                    <div class="stat-label">$2.50/1M input tokens</div>
                    <div class="stat-label">$10/1M output tokens</div>
                    <div class="stat-label">~$0.01-0.03 per meal photo</div>
                </div>
                <div>
                    <h3>Claude 3 Haiku</h3>
                    <div class="stat-label">$0.25/1M input tokens</div>
                    <div class="stat-label">$1.25/1M output tokens</div>
                </div>
                <div>
                    <h3>App Store Commissions</h3>
                    <div class="stat-label">Apple: 30% (15% small biz)</div>
                    <div class="stat-label">Google: 30% (15% small biz)</div>
                </div>
                <div>
                    <h3>Industry Benchmarks</h3>
                    <div class="stat-label">Avg Infra: $0.50/user/mo</div>
                    <div class="stat-label">Avg Support: $0.20/user/mo</div>
                </div>
            </div>
        </div>

        <script>
            const password = '{password}';

            // ════════════════════════════════════════════════════════════
            // DATA LOADING
            // ════════════════════════════════════════════════════════════

            async function loadAllData() {{
                await Promise.all([
                    loadSummary(),
                    loadComprehensiveSummary(),
                    loadBreakEven(),
                    loadDailyData(),
                    loadUsersData(),
                    loadEventsData(),
                    loadFixedCosts(),
                    loadRevenue()
                ]);
            }}

            async function loadBreakEven() {{
                try {{
                    const res = await fetch(`/api/v1/admin/break-even?password=${{password}}`);
                    const data = await res.json();

                    // Update break-even cards
                    document.getElementById('be-monthly-users').textContent = data.monthly_subs_needed || 0;
                    document.getElementById('be-yearly-users').textContent = data.yearly_subs_needed || 0;
                    document.getElementById('be-mixed-users').textContent = data.mixed_subs_needed || 0;

                    // Update prices and LTVs
                    document.getElementById('be-monthly-price').textContent = (data.monthly_net_price || 6.99).toFixed(2);
                    document.getElementById('be-yearly-price').textContent = (data.yearly_net_price || 41.99).toFixed(2);
                    document.getElementById('be-monthly-ltv').textContent = (data.monthly_ltv || 0).toFixed(2);
                    document.getElementById('be-yearly-ltv').textContent = (data.yearly_ltv || 0).toFixed(2);
                    document.getElementById('be-mixed-ltv').textContent = (data.blended_ltv || 0).toFixed(2);

                    // Update metrics
                    document.getElementById('be-churn').textContent = ((data.monthly_churn_rate || 0.12) * 100).toFixed(0);
                    document.getElementById('be-renewal').textContent = ((data.yearly_renewal_rate || 0.45) * 100).toFixed(0);
                    document.getElementById('be-monthly-lifetime').textContent = (data.avg_monthly_lifetime || 4.5).toFixed(1);
                    document.getElementById('be-yearly-lifetime').textContent = (data.avg_yearly_renewals || 1.8).toFixed(1);

                    // Update costs
                    document.getElementById('be-fixed').textContent = (data.monthly_fixed_cost || 0).toFixed(2);
                    document.getElementById('be-variable').textContent = (data.variable_cost_per_user || 0.05).toFixed(2);
                    document.getElementById('be-total-monthly').textContent = (data.total_monthly_cost || 0).toFixed(2);

                }} catch (e) {{
                    console.error('Break-even error:', e);
                }}
            }}

            async function loadSummary() {{
                try {{
                    const res = await fetch(`/api/v1/admin/summary?password=${{password}}`);
                    const data = await res.json();

                    document.getElementById('variable-costs').textContent = '$' + (data.total_cost || 0).toFixed(2);
                    document.getElementById('active-users').textContent = data.unique_users || 0;
                }} catch (e) {{ console.error('Summary error:', e); }}
            }}

            async function loadComprehensiveSummary() {{
                try {{
                    const res = await fetch(`/api/v1/admin/comprehensive?password=${{password}}`);
                    const data = await res.json();

                    document.getElementById('total-monthly-cost').textContent = '$' + (data.total_monthly_cost || 0).toFixed(2);
                    document.getElementById('fixed-costs').textContent = '$' + (data.monthly_fixed_cost || 0).toFixed(2);
                    document.getElementById('cost-per-user').textContent = '$' + (data.cost_per_active_user || 0).toFixed(2);
                    document.getElementById('gross-revenue').textContent = '$' + (data.total_gross_revenue || 0).toFixed(2);
                    document.getElementById('app-store-commission').textContent = '$' + (data.app_store_commission || 0).toFixed(2);
                    document.getElementById('net-revenue').textContent = '$' + (data.total_net_revenue || 0).toFixed(2);

                    const profit = data.monthly_profit || 0;
                    const profitEl = document.getElementById('monthly-profit');
                    profitEl.textContent = (profit >= 0 ? '+$' : '-$') + Math.abs(profit).toFixed(2);
                    profitEl.className = 'stat small ' + (profit >= 0 ? 'profit' : 'loss');

                    // Industry comparison
                    const industryAvg = 0.70;  // $0.50 infra + $0.20 support
                    const yourCost = data.cost_per_active_user || 0;
                    const badge = document.getElementById('cost-badge');

                    if (yourCost < industryAvg) {{
                        badge.textContent = 'Below Average ✓';
                        badge.className = 'badge below';
                    }} else if (yourCost > industryAvg * 1.5) {{
                        badge.textContent = 'High Cost ⚠';
                        badge.className = 'badge high';
                    }} else {{
                        badge.textContent = 'Average';
                        badge.className = 'badge average';
                    }}
                }} catch (e) {{
                    console.error('Comprehensive summary error:', e);
                }}
            }}

            async function loadDailyData() {{
                try {{
                    const res = await fetch(`/api/v1/admin/daily?password=${{password}}`);
                    const daily = await res.json();

                    const html = daily.map(d => `
                        <tr>
                            <td>${{d.date}}</td>
                            <td>${{d.openai_vision_calls || 0}}</td>
                            <td>${{d.vbt_analysis_calls || 0}}</td>
                            <td>${{d.ai_coach_messages || 0}}</td>
                            <td>${{d.storage_uploads || 0}}</td>
                            <td class="cost">${{d.total_estimated_cost ? '$' + d.total_estimated_cost.toFixed(4) : '$0.0000'}}</td>
                        </tr>
                    `).join('') || '<tr><td colspan="6">No data yet</td></tr>';

                    document.getElementById('daily-table').innerHTML = html;
                }} catch (e) {{ console.error('Daily error:', e); }}
            }}

            async function loadUsersData() {{
                try {{
                    const res = await fetch(`/api/v1/admin/users?password=${{password}}`);
                    const users = await res.json();

                    const html = users.map(u => `
                        <tr>
                            <td title="${{u.user_id}}">${{u.user_id ? u.user_id.substring(0, 8) + '...' : 'Unknown'}}</td>
                            <td>${{u.first_activity ? u.first_activity.split('T')[0] : '-'}}</td>
                            <td>${{u.active_days || 0}}</td>
                            <td>${{u.total_events || 0}}</td>
                            <td class="cost">${{u.total_estimated_cost ? '$' + u.total_estimated_cost.toFixed(4) : '$0.0000'}}</td>
                        </tr>
                    `).join('') || '<tr><td colspan="5">No data yet</td></tr>';

                    document.getElementById('users-table').innerHTML = html;
                }} catch (e) {{ console.error('Users error:', e); }}
            }}

            async function loadEventsData() {{
                try {{
                    const res = await fetch(`/api/v1/admin/events?password=${{password}}`);
                    const events = await res.json();

                    const html = events.map(e => `
                        <tr>
                            <td>${{e.event_type}}</td>
                            <td>${{e.count || 0}}</td>
                            <td>${{e.avg_tokens ? e.avg_tokens.toFixed(0) : '-'}}</td>
                            <td class="cost">${{e.total_cost ? '$' + e.total_cost.toFixed(4) : '$0.0000'}}</td>
                        </tr>
                    `).join('') || '<tr><td colspan="4">No data yet</td></tr>';

                    document.getElementById('events-table').innerHTML = html;
                }} catch (e) {{ console.error('Events error:', e); }}
            }}

            async function loadFixedCosts() {{
                try {{
                    const res = await fetch(`/api/v1/admin/fixed-costs?password=${{password}}`);
                    const costs = await res.json();

                    const html = costs.map(c => `
                        <tr>
                            <td>${{c.name}}</td>
                            <td>${{c.category}}</td>
                            <td>${{c.amount ? '$' + parseFloat(c.amount).toFixed(2) : '$0.00'}}</td>
                            <td>${{c.recurrence_type}}</td>
                            <td class="cost">${{c.monthly_equivalent ? '$' + parseFloat(c.monthly_equivalent).toFixed(2) : '-'}}</td>
                            <td>
                                <button class="btn secondary" onclick="editFixedCost('${{c.id}}', '${{c.name}}', '${{c.category}}', ${{c.amount}}, '${{c.recurrence_type}}')" style="padding: 4px 10px; font-size: 12px; margin-right: 5px;">Edit</button>
                                <button class="btn danger" onclick="deleteFixedCost('${{c.id}}')" style="padding: 4px 10px; font-size: 12px;">Delete</button>
                            </td>
                        </tr>
                    `).join('') || '<tr><td colspan="6">No fixed costs added yet. Add your first cost above!</td></tr>';

                    document.getElementById('fixed-table').innerHTML = html;
                }} catch (e) {{ console.error('Fixed costs error:', e); }}
            }}

            async function loadRevenue() {{
                try {{
                    const res = await fetch(`/api/v1/admin/revenue?password=${{password}}`);
                    const revenue = await res.json();

                    const html = revenue.map(r => `
                        <tr>
                            <td>${{r.period_start}}</td>
                            <td>${{r.platform}}</td>
                            <td>${{r.revenue_type}}</td>
                            <td>${{r.gross_revenue ? '$' + parseFloat(r.gross_revenue).toFixed(2) : '$0.00'}}</td>
                            <td class="loss">${{r.app_store_commission ? '$' + parseFloat(r.app_store_commission).toFixed(2) : '$0.00'}}</td>
                            <td class="cost">${{r.net_revenue ? '$' + parseFloat(r.net_revenue).toFixed(2) : '$0.00'}}</td>
                            <td>
                                <button class="btn secondary" onclick="editRevenue('${{r.id}}', '${{r.platform}}', '${{r.revenue_type}}', ${{r.gross_revenue}}, '${{r.period_start}}')" style="padding: 4px 10px; font-size: 12px; margin-right: 5px;">Edit</button>
                                <button class="btn danger" onclick="deleteRevenue('${{r.id}}')" style="padding: 4px 10px; font-size: 12px;">Delete</button>
                            </td>
                        </tr>
                    `).join('') || '<tr><td colspan="7">No revenue recorded yet</td></tr>';

                    document.getElementById('revenue-table').innerHTML = html;
                }} catch (e) {{ console.error('Revenue error:', e); }}
            }}

            // ════════════════════════════════════════════════════════════
            // ACTIONS
            // ════════════════════════════════════════════════════════════

            async function addFixedCost() {{
                const name = document.getElementById('fc-name').value;
                const category = document.getElementById('fc-category').value;
                const amount = parseFloat(document.getElementById('fc-amount').value);
                const recurrence = document.getElementById('fc-recurrence').value;

                if (!name || !amount) {{
                    alert('Please fill in name and amount');
                    return;
                }}

                try {{
                    const res = await fetch(`/api/v1/admin/fixed-costs?password=${{password}}`, {{
                        method: 'POST',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{ name, category, amount, recurrence_type: recurrence }})
                    }});

                    if (res.ok) {{
                        document.getElementById('fc-name').value = '';
                        document.getElementById('fc-amount').value = '';
                        loadFixedCosts();
                        loadComprehensiveSummary();
                    }} else {{
                        alert('Failed to add cost');
                    }}
                }} catch (e) {{
                    alert('Error: ' + e.message);
                }}
            }}

            async function deleteFixedCost(id) {{
                if (!confirm('Delete this cost?')) return;

                try {{
                    const res = await fetch(`/api/v1/admin/fixed-costs/${{id}}?password=${{password}}`, {{
                        method: 'DELETE'
                    }});

                    if (res.ok) {{
                        loadFixedCosts();
                        loadComprehensiveSummary();
                    }}
                }} catch (e) {{
                    alert('Error: ' + e.message);
                }}
            }}

            async function addRevenue() {{
                const platform = document.getElementById('rev-platform').value;
                const type = document.getElementById('rev-type').value;
                const amount = parseFloat(document.getElementById('rev-amount').value);
                const date = document.getElementById('rev-date').value;

                if (!amount || !date) {{
                    alert('Please fill in amount and date');
                    return;
                }}

                try {{
                    const res = await fetch(`/api/v1/admin/revenue?password=${{password}}`, {{
                        method: 'POST',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{
                            platform,
                            revenue_type: type,
                            gross_revenue: amount,
                            period_start: date,
                            period_end: date
                        }})
                    }});

                    if (res.ok) {{
                        document.getElementById('rev-amount').value = '';
                        loadRevenue();
                        loadComprehensiveSummary();
                    }} else {{
                        alert('Failed to add revenue');
                    }}
                }} catch (e) {{
                    alert('Error: ' + e.message);
                }}
            }}

            async function editFixedCost(id, name, category, amount, recurrence) {{
                const newName = prompt('Name:', name);
                if (newName === null) return;

                const newAmount = prompt('Amount ($):', amount);
                if (newAmount === null) return;

                const newCategory = prompt('Category (subscription/labor/infrastructure/marketing/app_store_fees/other):', category);
                if (newCategory === null) return;

                const newRecurrence = prompt('Recurrence (monthly/yearly/one_time):', recurrence);
                if (newRecurrence === null) return;

                try {{
                    const res = await fetch(`/api/v1/admin/fixed-costs/${{id}}?password=${{password}}`, {{
                        method: 'PUT',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{
                            name: newName,
                            category: newCategory,
                            amount: parseFloat(newAmount),
                            recurrence_type: newRecurrence
                        }})
                    }});

                    if (res.ok) {{
                        loadFixedCosts();
                        loadComprehensiveSummary();
                        loadBreakEven();
                    }} else {{
                        alert('Failed to update cost');
                    }}
                }} catch (e) {{
                    alert('Error: ' + e.message);
                }}
            }}

            async function deleteRevenue(id) {{
                if (!confirm('Delete this revenue entry?')) return;

                try {{
                    const res = await fetch(`/api/v1/admin/revenue/${{id}}?password=${{password}}`, {{
                        method: 'DELETE'
                    }});

                    if (res.ok) {{
                        loadRevenue();
                        loadComprehensiveSummary();
                    }}
                }} catch (e) {{
                    alert('Error: ' + e.message);
                }}
            }}

            async function editRevenue(id, platform, type, amount, date) {{
                const newPlatform = prompt('Platform (ios/android/web):', platform);
                if (newPlatform === null) return;

                const newType = prompt('Type (subscription/one_time_purchase/ad_revenue):', type);
                if (newType === null) return;

                const newAmount = prompt('Gross Revenue ($):', amount);
                if (newAmount === null) return;

                const newDate = prompt('Date (YYYY-MM-DD):', date);
                if (newDate === null) return;

                try {{
                    const res = await fetch(`/api/v1/admin/revenue/${{id}}?password=${{password}}`, {{
                        method: 'PUT',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{
                            platform: newPlatform,
                            revenue_type: newType,
                            gross_revenue: parseFloat(newAmount),
                            period_start: newDate,
                            period_end: newDate
                        }})
                    }});

                    if (res.ok) {{
                        loadRevenue();
                        loadComprehensiveSummary();
                    }} else {{
                        alert('Failed to update revenue');
                    }}
                }} catch (e) {{
                    alert('Error: ' + e.message);
                }}
            }}

            function showTab(tab) {{
                document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
                document.querySelectorAll('[id$="-tab"]').forEach(t => t.style.display = 'none');

                event.target.classList.add('active');
                document.getElementById(tab + '-tab').style.display = 'block';
            }}

            async function filterRevenueByMonth() {{
                const monthInput = document.getElementById('rev-month-filter').value;
                if (!monthInput) {{
                    alert('Please select a month');
                    return;
                }}

                try {{
                    const res = await fetch(`/api/v1/admin/revenue?password=${{password}}&month=${{monthInput}}`);
                    const allRevenue = await res.json();

                    // Filter by selected month (YYYY-MM format)
                    const filtered = allRevenue.filter(r => {{
                        return r.period_start && r.period_start.startsWith(monthInput);
                    }});

                    // Calculate monthly summary
                    let totalGross = 0;
                    let totalCommission = 0;
                    let totalNet = 0;

                    filtered.forEach(r => {{
                        totalGross += parseFloat(r.gross_revenue || 0);
                        totalCommission += parseFloat(r.app_store_commission || 0);
                        totalNet += parseFloat(r.net_revenue || 0);
                    }});

                    // Show monthly summary
                    const summaryDiv = document.getElementById('monthly-summary');
                    summaryDiv.style.display = 'block';

                    // Format month for display (e.g., "November 2024")
                    const [year, month] = monthInput.split('-');
                    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
                                       'July', 'August', 'September', 'October', 'November', 'December'];
                    document.getElementById('summary-month').textContent = `${{monthNames[parseInt(month) - 1]}} ${{year}}`;
                    document.getElementById('summary-gross').textContent = '$' + totalGross.toFixed(2);
                    document.getElementById('summary-commission').textContent = '-$' + totalCommission.toFixed(2);
                    document.getElementById('summary-net').textContent = '$' + totalNet.toFixed(2);
                    document.getElementById('summary-count').textContent = filtered.length;

                    // Update table
                    const html = filtered.map(r => `
                        <tr>
                            <td>${{r.period_start}}</td>
                            <td>${{r.platform}}</td>
                            <td>${{r.revenue_type}}</td>
                            <td>${{r.gross_revenue ? '$' + parseFloat(r.gross_revenue).toFixed(2) : '$0.00'}}</td>
                            <td class="loss">${{r.app_store_commission ? '$' + parseFloat(r.app_store_commission).toFixed(2) : '$0.00'}}</td>
                            <td class="cost">${{r.net_revenue ? '$' + parseFloat(r.net_revenue).toFixed(2) : '$0.00'}}</td>
                            <td>
                                <button class="btn secondary" onclick="editRevenue('${{r.id}}', '${{r.platform}}', '${{r.revenue_type}}', ${{r.gross_revenue}}, '${{r.period_start}}')" style="padding: 4px 10px; font-size: 12px; margin-right: 5px;">Edit</button>
                                <button class="btn danger" onclick="deleteRevenue('${{r.id}}')" style="padding: 4px 10px; font-size: 12px;">Delete</button>
                            </td>
                        </tr>
                    `).join('') || `<tr><td colspan="7">No revenue for ${{monthNames[parseInt(month) - 1]}} ${{year}}</td></tr>`;

                    document.getElementById('revenue-table').innerHTML = html;
                }} catch (e) {{
                    console.error('Filter error:', e);
                    alert('Error filtering: ' + e.message);
                }}
            }}

            // Set default date to today
            document.getElementById('rev-date').valueAsDate = new Date();

            // Set default month filter to current month
            const now = new Date();
            document.getElementById('rev-month-filter').value = now.toISOString().slice(0, 7);

            // Load all data on page load
            loadAllData();
        </script>
    </body>
    </html>
    """)


from fastapi.responses import HTMLResponse


@app.get("/api/v1/admin/summary")
async def admin_get_summary(password: str):
    """Get monthly cost summary"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Query monthly_cost_summary view
        result = client.table('monthly_cost_summary').select('*').limit(1).execute()

        if result.data and len(result.data) > 0:
            data = result.data[0]
            return {
                "total_cost": float(data.get('total_estimated_cost') or 0),
                "unique_users": data.get('unique_users', 0),
                "total_events": data.get('total_events', 0),
                "avg_cost_per_user": float(data.get('avg_cost_per_user') or 0),
                "openai_vision_calls": data.get('openai_vision_calls', 0),
                "vbt_analysis_calls": data.get('vbt_analysis_calls', 0),
                "ai_coach_messages": data.get('ai_coach_messages', 0)
            }

        return {
            "total_cost": 0,
            "unique_users": 0,
            "total_events": 0,
            "avg_cost_per_user": 0
        }

    except Exception as e:
        print(f"❌ Admin summary error: {str(e)}")
        return {"error": str(e), "total_cost": 0, "unique_users": 0, "total_events": 0}


@app.get("/api/v1/admin/daily")
async def admin_get_daily_costs(password: str, days: int = 30):
    """Get daily cost breakdown"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Query daily_cost_summary view
        result = client.table('daily_cost_summary')\
            .select('*')\
            .order('date', desc=True)\
            .limit(days)\
            .execute()

        return result.data or []

    except Exception as e:
        print(f"❌ Admin daily error: {str(e)}")
        return []


@app.get("/api/v1/admin/users")
async def admin_get_user_costs(password: str, limit: int = 50):
    """Get per-user cost breakdown"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Query user_cost_summary view
        result = client.table('user_cost_summary')\
            .select('*')\
            .order('total_estimated_cost', desc=True)\
            .limit(limit)\
            .execute()

        return result.data or []

    except Exception as e:
        print(f"❌ Admin users error: {str(e)}")
        return []


@app.get("/api/v1/admin/events")
async def admin_get_event_breakdown(password: str):
    """Get event type breakdown for current month"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        from datetime import datetime
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Get first day of current month
        now = datetime.now()
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

        # Query usage_logs with aggregation
        result = client.rpc('get_event_breakdown', {
            'start_date': month_start.isoformat()
        }).execute()

        if result.data:
            return result.data

        # Fallback: direct query without RPC
        result = client.table('usage_logs')\
            .select('event_type, total_tokens, estimated_cost')\
            .gte('created_at', month_start.isoformat())\
            .execute()

        # Aggregate manually
        event_stats = {}
        for row in result.data or []:
            et = row['event_type']
            if et not in event_stats:
                event_stats[et] = {'count': 0, 'total_tokens': 0, 'total_cost': 0}
            event_stats[et]['count'] += 1
            event_stats[et]['total_tokens'] += row.get('total_tokens') or 0
            event_stats[et]['total_cost'] += float(row.get('estimated_cost') or 0)

        return [
            {
                'event_type': et,
                'count': stats['count'],
                'avg_tokens': stats['total_tokens'] / stats['count'] if stats['count'] > 0 else 0,
                'total_cost': stats['total_cost']
            }
            for et, stats in event_stats.items()
        ]

    except Exception as e:
        print(f"❌ Admin events error: {str(e)}")
        return []


# ============================================================
# EXTENDED ADMIN ENDPOINTS - Fixed Costs, Revenue, Comprehensive
# ============================================================

@app.get("/api/v1/admin/comprehensive")
async def admin_get_comprehensive_summary(password: str):
    """Get comprehensive cost summary including fixed costs and revenue"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Try to query comprehensive_cost_summary view
        try:
            result = client.table('comprehensive_cost_summary').select('*').limit(1).execute()
            if result.data and len(result.data) > 0:
                data = result.data[0]
                return {
                    "total_variable_cost": float(data.get('total_variable_cost') or 0),
                    "monthly_fixed_cost": float(data.get('monthly_fixed_cost') or 0),
                    "total_monthly_cost": float(data.get('total_monthly_cost') or 0),
                    "active_users": data.get('active_users', 0),
                    "total_events": data.get('total_events', 0),
                    "cost_per_active_user": float(data.get('cost_per_active_user') or 0),
                    "total_gross_revenue": float(data.get('total_gross_revenue') or 0),
                    "app_store_commission": float(data.get('app_store_commission') or 0),
                    "total_net_revenue": float(data.get('total_net_revenue') or 0),
                    "monthly_profit": float(data.get('monthly_profit') or 0),
                    "revenue_per_active_user": float(data.get('revenue_per_active_user') or 0)
                }
        except Exception as view_error:
            print(f"⚠️ comprehensive_cost_summary view not found, calculating manually: {view_error}")

        # Fallback: Calculate manually if view doesn't exist
        from datetime import datetime
        now = datetime.now()
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)

        # Get variable costs from usage_logs
        usage_result = client.table('usage_logs')\
            .select('user_id, estimated_cost')\
            .gte('created_at', month_start.isoformat())\
            .execute()

        total_variable_cost = sum(float(r.get('estimated_cost') or 0) for r in (usage_result.data or []))
        active_users = len(set(r.get('user_id') for r in (usage_result.data or []) if r.get('user_id')))
        total_events = len(usage_result.data or [])

        # Get fixed costs
        monthly_fixed_cost = 0
        try:
            fixed_result = client.table('fixed_costs')\
                .select('amount, recurrence_type')\
                .eq('is_recurring', True)\
                .execute()

            for fc in (fixed_result.data or []):
                amount = float(fc.get('amount') or 0)
                if fc.get('recurrence_type') == 'monthly':
                    monthly_fixed_cost += amount
                elif fc.get('recurrence_type') == 'yearly':
                    monthly_fixed_cost += amount / 12
        except Exception as fc_error:
            print(f"⚠️ fixed_costs table not found: {fc_error}")

        # Get revenue
        total_gross_revenue = 0
        app_store_commission = 0
        total_net_revenue = 0
        try:
            revenue_result = client.table('app_revenue')\
                .select('gross_revenue, app_store_commission, net_revenue')\
                .gte('period_start', month_start.strftime('%Y-%m-%d'))\
                .execute()

            for rev in (revenue_result.data or []):
                total_gross_revenue += float(rev.get('gross_revenue') or 0)
                app_store_commission += float(rev.get('app_store_commission') or 0)
                total_net_revenue += float(rev.get('net_revenue') or 0)
        except Exception as rev_error:
            print(f"⚠️ app_revenue table not found: {rev_error}")

        total_monthly_cost = total_variable_cost + monthly_fixed_cost
        cost_per_user = total_monthly_cost / active_users if active_users > 0 else 0
        monthly_profit = total_net_revenue - total_monthly_cost

        return {
            "total_variable_cost": total_variable_cost,
            "monthly_fixed_cost": monthly_fixed_cost,
            "total_monthly_cost": total_monthly_cost,
            "active_users": active_users,
            "total_events": total_events,
            "cost_per_active_user": cost_per_user,
            "total_gross_revenue": total_gross_revenue,
            "app_store_commission": app_store_commission,
            "total_net_revenue": total_net_revenue,
            "monthly_profit": monthly_profit,
            "revenue_per_active_user": total_net_revenue / active_users if active_users > 0 else 0
        }

    except Exception as e:
        print(f"❌ Admin comprehensive error: {str(e)}")
        return {
            "total_variable_cost": 0, "monthly_fixed_cost": 0, "total_monthly_cost": 0,
            "active_users": 0, "cost_per_active_user": 0, "total_gross_revenue": 0,
            "app_store_commission": 0, "total_net_revenue": 0, "monthly_profit": 0
        }


@app.get("/api/v1/admin/fixed-costs")
async def admin_get_fixed_costs(password: str):
    """Get all fixed costs"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        result = client.table('fixed_costs')\
            .select('*')\
            .order('created_at', desc=True)\
            .execute()

        # Calculate monthly equivalent for each cost
        costs = []
        for fc in (result.data or []):
            amount = float(fc.get('amount') or 0)
            recurrence = fc.get('recurrence_type', 'monthly')

            if recurrence == 'monthly':
                monthly_eq = amount
            elif recurrence == 'yearly':
                monthly_eq = amount / 12
            else:  # one_time
                monthly_eq = None

            costs.append({
                **fc,
                'monthly_equivalent': monthly_eq
            })

        return costs

    except Exception as e:
        print(f"❌ Admin fixed costs GET error: {str(e)}")
        return []


@app.post("/api/v1/admin/fixed-costs")
async def admin_add_fixed_cost(password: str, request_data: dict):
    """Add a new fixed cost"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        data = {
            "name": request_data.get("name"),
            "category": request_data.get("category", "other"),
            "amount": float(request_data.get("amount", 0)),
            "recurrence_type": request_data.get("recurrence_type", "monthly"),
            "is_recurring": request_data.get("recurrence_type") != "one_time",
            "description": request_data.get("description")
        }

        result = client.table('fixed_costs').insert(data).execute()

        if result.data:
            print(f"✅ Added fixed cost: {data['name']} - ${data['amount']}")
            return {"success": True, "id": result.data[0].get("id")}

        return {"success": False, "error": "Insert failed"}

    except Exception as e:
        print(f"❌ Admin fixed costs POST error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/admin/fixed-costs/{cost_id}")
async def admin_delete_fixed_cost(cost_id: str, password: str):
    """Delete a fixed cost"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        result = client.table('fixed_costs')\
            .delete()\
            .eq('id', cost_id)\
            .execute()

        print(f"🗑️ Deleted fixed cost: {cost_id}")
        return {"success": True}

    except Exception as e:
        print(f"❌ Admin fixed costs DELETE error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.put("/api/v1/admin/fixed-costs/{cost_id}")
async def admin_update_fixed_cost(cost_id: str, password: str, request_data: dict):
    """Update a fixed cost"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        data = {
            "name": request_data.get("name"),
            "category": request_data.get("category", "other"),
            "amount": float(request_data.get("amount", 0)),
            "recurrence_type": request_data.get("recurrence_type", "monthly"),
            "is_recurring": request_data.get("recurrence_type") != "one_time",
            "description": request_data.get("description")
        }

        result = client.table('fixed_costs')\
            .update(data)\
            .eq('id', cost_id)\
            .execute()

        print(f"✏️ Updated fixed cost: {cost_id}")
        return {"success": True}

    except Exception as e:
        print(f"❌ Admin fixed costs PUT error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/admin/revenue")
async def admin_get_revenue(password: str):
    """Get all revenue entries"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        result = client.table('app_revenue')\
            .select('*')\
            .order('period_start', desc=True)\
            .limit(100)\
            .execute()

        return result.data or []

    except Exception as e:
        print(f"❌ Admin revenue GET error: {str(e)}")
        return []


@app.post("/api/v1/admin/revenue")
async def admin_add_revenue(password: str, request_data: dict):
    """Add a new revenue entry"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        platform = request_data.get("platform", "ios")
        revenue_type = request_data.get("revenue_type", "subscription")
        gross_revenue = float(request_data.get("gross_revenue", 0))

        # Calculate commission based on platform
        if platform == "web":
            commission_rate = 0
        else:
            commission_rate = 0.15  # Apple/Google Small Business Program (15% for <$1M revenue)

        # Only charge commission on subscriptions and IAP
        if revenue_type in ["subscription", "one_time_purchase"]:
            app_store_commission = gross_revenue * commission_rate
        else:
            app_store_commission = 0

        net_revenue = gross_revenue - app_store_commission

        data = {
            "platform": platform,
            "revenue_type": revenue_type,
            "gross_revenue": gross_revenue,
            "app_store_commission": app_store_commission,
            "net_revenue": net_revenue,
            "period_start": request_data.get("period_start"),
            "period_end": request_data.get("period_end"),
            "notes": request_data.get("notes")
        }

        result = client.table('app_revenue').insert(data).execute()

        if result.data:
            print(f"✅ Added revenue: {platform} ${gross_revenue} -> ${net_revenue} net")
            return {"success": True, "id": result.data[0].get("id")}

        return {"success": False, "error": "Insert failed"}

    except Exception as e:
        print(f"❌ Admin revenue POST error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/admin/revenue/{revenue_id}")
async def admin_delete_revenue(revenue_id: str, password: str):
    """Delete a revenue entry"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        result = client.table('app_revenue')\
            .delete()\
            .eq('id', revenue_id)\
            .execute()

        print(f"🗑️ Deleted revenue entry: {revenue_id}")
        return {"success": True}

    except Exception as e:
        print(f"❌ Admin revenue DELETE error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.put("/api/v1/admin/revenue/{revenue_id}")
async def admin_update_revenue(revenue_id: str, password: str, request_data: dict):
    """Update a revenue entry"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        platform = request_data.get("platform", "ios")
        revenue_type = request_data.get("revenue_type", "subscription")
        gross_revenue = float(request_data.get("gross_revenue", 0))

        # Calculate commission based on platform
        if platform == "web":
            commission_rate = 0
        else:
            commission_rate = 0.15  # Apple/Google Small Business Program

        # Only charge commission on subscriptions and IAP
        if revenue_type in ["subscription", "one_time_purchase"]:
            app_store_commission = gross_revenue * commission_rate
        else:
            app_store_commission = 0

        net_revenue = gross_revenue - app_store_commission

        data = {
            "platform": platform,
            "revenue_type": revenue_type,
            "gross_revenue": gross_revenue,
            "app_store_commission": app_store_commission,
            "net_revenue": net_revenue,
            "period_start": request_data.get("period_start"),
            "period_end": request_data.get("period_end"),
            "notes": request_data.get("notes")
        }

        result = client.table('app_revenue')\
            .update(data)\
            .eq('id', revenue_id)\
            .execute()

        print(f"✏️ Updated revenue: {revenue_id}")
        return {"success": True}

    except Exception as e:
        print(f"❌ Admin revenue PUT error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/admin/break-even")
async def admin_get_break_even_analysis(password: str):
    """
    Calculate break-even analysis for subscription business

    Uses industry benchmarks for fitness apps:
    - Monthly churn: ~12% (avg lifetime 4.5 months)
    - Yearly renewal: ~45%
    - Typical split: 65% monthly / 35% yearly subscribers
    """
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        from datetime import datetime
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Default values (industry benchmarks for fitness apps)
        monthly_gross_price = 9.99
        yearly_gross_price = 59.99
        commission_rate = 0.15  # Small Business Program (15% for <$1M revenue)
        monthly_churn_rate = 0.12
        yearly_renewal_rate = 0.45
        monthly_to_yearly_ratio = 0.65
        avg_monthly_lifetime = 4.5  # months
        avg_yearly_renewals = 1.8   # years total (initial + renewals)
        variable_cost_per_user = 0.05

        # Try to get custom values from business_metrics table
        try:
            metrics_result = client.table('business_metrics').select('*').execute()
            for metric in (metrics_result.data or []):
                key = metric.get('metric_key')
                value = float(metric.get('metric_value', 0))
                if key == 'monthly_sub_price':
                    monthly_gross_price = value
                elif key == 'yearly_sub_price':
                    yearly_gross_price = value
                elif key == 'apple_commission_rate':
                    commission_rate = value
                elif key == 'monthly_churn_rate':
                    monthly_churn_rate = value
                elif key == 'yearly_renewal_rate':
                    yearly_renewal_rate = value
                elif key == 'monthly_to_yearly_ratio':
                    monthly_to_yearly_ratio = value
                elif key == 'avg_monthly_ltv_months':
                    avg_monthly_lifetime = value
                elif key == 'avg_yearly_renewals':
                    avg_yearly_renewals = value
                elif key == 'variable_cost_per_active_user':
                    variable_cost_per_user = value
        except Exception as metrics_error:
            print(f"⚠️ business_metrics table not found, using defaults: {metrics_error}")

        # Calculate net prices (after app store commission)
        monthly_net_price = monthly_gross_price * (1 - commission_rate)
        yearly_net_price = yearly_gross_price * (1 - commission_rate)

        # Calculate LTV (Lifetime Value)
        # Monthly LTV = net monthly price * average lifetime in months
        monthly_ltv = monthly_net_price * avg_monthly_lifetime

        # Yearly LTV = net yearly price * average years
        yearly_ltv = yearly_net_price * avg_yearly_renewals

        # Blended LTV (65% monthly / 35% yearly)
        yearly_ratio = 1 - monthly_to_yearly_ratio
        blended_ltv = (monthly_to_yearly_ratio * monthly_ltv) + (yearly_ratio * yearly_ltv)

        # Get monthly fixed costs
        monthly_fixed_cost = 0
        try:
            fixed_result = client.table('fixed_costs')\
                .select('amount, recurrence_type')\
                .eq('is_recurring', True)\
                .execute()

            for fc in (fixed_result.data or []):
                amount = float(fc.get('amount') or 0)
                if fc.get('recurrence_type') == 'monthly':
                    monthly_fixed_cost += amount
                elif fc.get('recurrence_type') == 'yearly':
                    monthly_fixed_cost += amount / 12
        except Exception as fc_error:
            print(f"⚠️ fixed_costs table not found: {fc_error}")

        # Get current variable costs this month
        now = datetime.now()
        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        try:
            usage_result = client.table('usage_logs')\
                .select('estimated_cost')\
                .gte('created_at', month_start.isoformat())\
                .execute()
            total_variable_cost = sum(float(r.get('estimated_cost') or 0) for r in (usage_result.data or []))
        except:
            total_variable_cost = 0

        total_monthly_cost = monthly_fixed_cost + total_variable_cost

        # Calculate break-even subscribers
        # Break-even = Total Costs / (LTV - Variable Cost per User over Lifetime)

        # For monthly-only subscribers
        monthly_ltv_net = monthly_ltv - (variable_cost_per_user * avg_monthly_lifetime)
        if monthly_ltv_net > 0:
            monthly_subs_needed = int(total_monthly_cost / (monthly_ltv_net / avg_monthly_lifetime)) + 1
        else:
            monthly_subs_needed = 9999

        # For yearly-only subscribers (amortized monthly)
        yearly_ltv_net = yearly_ltv - (variable_cost_per_user * avg_yearly_renewals * 12)
        yearly_monthly_contribution = yearly_ltv_net / (avg_yearly_renewals * 12) if avg_yearly_renewals > 0 else 0
        if yearly_monthly_contribution > 0:
            yearly_subs_needed = int(total_monthly_cost / yearly_monthly_contribution) + 1
        else:
            yearly_subs_needed = 9999

        # For blended (realistic mix)
        blended_ltv_net = blended_ltv - (variable_cost_per_user * 6)  # assume 6 months average
        blended_monthly_contribution = blended_ltv_net / 6 if blended_ltv_net > 0 else 0
        if blended_monthly_contribution > 0:
            mixed_subs_needed = int(total_monthly_cost / blended_monthly_contribution) + 1
        else:
            mixed_subs_needed = 9999

        return {
            # Pricing
            "monthly_gross_price": monthly_gross_price,
            "yearly_gross_price": yearly_gross_price,
            "monthly_net_price": monthly_net_price,
            "yearly_net_price": yearly_net_price,
            "commission_rate": commission_rate,

            # LTV
            "monthly_ltv": monthly_ltv,
            "yearly_ltv": yearly_ltv,
            "blended_ltv": blended_ltv,

            # Benchmarks
            "monthly_churn_rate": monthly_churn_rate,
            "yearly_renewal_rate": yearly_renewal_rate,
            "avg_monthly_lifetime": avg_monthly_lifetime,
            "avg_yearly_renewals": avg_yearly_renewals,
            "monthly_to_yearly_ratio": monthly_to_yearly_ratio,

            # Costs
            "monthly_fixed_cost": monthly_fixed_cost,
            "variable_cost_per_user": variable_cost_per_user,
            "total_monthly_cost": total_monthly_cost,

            # Break-even
            "monthly_subs_needed": monthly_subs_needed,
            "yearly_subs_needed": yearly_subs_needed,
            "mixed_subs_needed": mixed_subs_needed
        }

    except Exception as e:
        print(f"❌ Admin break-even error: {str(e)}")
        return {
            "monthly_subs_needed": 0,
            "yearly_subs_needed": 0,
            "mixed_subs_needed": 0,
            "error": str(e)
        }


# ============================================================
# PARTNER PORTAL - Separate login for partners/influencers
# ============================================================

async def verify_partner(email: str, password: str):
    """Verify partner credentials and return partner data"""
    try:
        from supabase import create_client
        import hashlib
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Get partner by email
        result = client.table('partners').select('*').eq('email', email.lower()).eq('status', 'active').execute()

        if result.data and len(result.data) > 0:
            partner = result.data[0]
            # Simple password check (hash stored in payout_details.password_hash)
            stored_hash = partner.get('payout_details', {}).get('password_hash', '')
            input_hash = hashlib.sha256(password.encode()).hexdigest()

            if stored_hash == input_hash:
                return partner
        return None
    except Exception as e:
        print(f"Partner auth error: {e}")
        return None


async def get_partner_by_code(referral_code: str):
    """Get partner data by referral code (for session verification)"""
    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        result = client.table('partners').select('*').eq('referral_code', referral_code.upper()).eq('status', 'active').execute()

        if result.data and len(result.data) > 0:
            return result.data[0]
        return None
    except Exception as e:
        print(f"Partner lookup error: {e}")
        return None


@app.get("/partner")
async def partner_portal(code: Optional[str] = None):
    """
    Partner Portal - Dashboard for partners to view their earnings

    Access: /partner (login) or /partner?code=REFERRAL_CODE (authenticated)
    """

    # Check if authenticated via referral code
    partner = None
    if code:
        partner = await get_partner_by_code(code)

    if not partner:
        # Show login page
        return HTMLResponse(content="""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Prometheus Partner Portal</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                * { box-sizing: border-box; }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    display: flex; justify-content: center; align-items: center;
                    min-height: 100vh; margin: 0; background: #0f0f0f; color: #fff;
                }
                .login-box {
                    background: #1a1a1a; padding: 40px; border-radius: 16px;
                    box-shadow: 0 4px 30px rgba(0,0,0,0.4); width: 90%; max-width: 400px;
                }
                .logo { text-align: center; margin-bottom: 30px; }
                .logo h1 { color: #ff6b35; margin: 0; font-size: 28px; }
                .logo p { color: #666; margin: 10px 0 0 0; }
                .form-group { margin-bottom: 20px; }
                .form-group label { display: block; color: #888; margin-bottom: 8px; font-size: 14px; }
                input {
                    width: 100%; padding: 14px 16px; font-size: 16px;
                    border: 1px solid #333; border-radius: 10px;
                    background: #2a2a2a; color: #fff;
                }
                input:focus { outline: none; border-color: #ff6b35; }
                button {
                    width: 100%; padding: 14px 24px; font-size: 16px;
                    background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
                    color: #fff; border: none; border-radius: 10px;
                    cursor: pointer; font-weight: 600; margin-top: 10px;
                }
                button:hover { opacity: 0.9; }
                .error {
                    background: #3f1a1a; color: #ff6b6b; padding: 12px;
                    border-radius: 8px; margin-bottom: 20px; display: none;
                    font-size: 14px;
                }
                .help { text-align: center; margin-top: 25px; color: #666; font-size: 13px; }
                .help a { color: #ff6b35; text-decoration: none; }
            </style>
        </head>
        <body>
            <div class="login-box">
                <div class="logo">
                    <h1>🔥 Partner Portal</h1>
                    <p>Prometheus Affiliate Program</p>
                </div>

                <div class="error" id="error-msg"></div>

                <form id="login-form">
                    <div class="form-group">
                        <label>Referral Code</label>
                        <input type="text" id="referral-code" placeholder="e.g., ALEX15" required
                               style="text-transform: uppercase;">
                    </div>
                    <div class="form-group">
                        <label>Password</label>
                        <input type="password" id="password" placeholder="Your partner password" required>
                    </div>
                    <button type="submit">Login to Dashboard</button>
                </form>

                <div class="help">
                    Need help? Contact <a href="mailto:partners@prometheus.fitness">partners@prometheus.fitness</a>
                </div>
            </div>

            <script>
                document.getElementById('login-form').addEventListener('submit', async (e) => {
                    e.preventDefault();

                    const code = document.getElementById('referral-code').value.toUpperCase();
                    const password = document.getElementById('password').value;
                    const errorEl = document.getElementById('error-msg');

                    try {
                        const res = await fetch('/api/v1/partner/login', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ referral_code: code, password: password })
                        });

                        const data = await res.json();

                        if (data.success) {
                            window.location.href = '/partner?code=' + code;
                        } else {
                            errorEl.textContent = data.error || 'Invalid credentials';
                            errorEl.style.display = 'block';
                        }
                    } catch (err) {
                        errorEl.textContent = 'Connection error. Please try again.';
                        errorEl.style.display = 'block';
                    }
                });
            </script>
        </body>
        </html>
        """, status_code=200)

    # Authenticated - show partner dashboard
    partner_name = partner.get('name', 'Partner')
    partner_code = partner.get('referral_code', '')
    commission_percent = partner.get('commission_percent', 15)

    return HTMLResponse(content=f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Partner Dashboard - {partner_name}</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            * {{ box-sizing: border-box; }}
            body {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                margin: 0; padding: 15px; background: #0f0f0f; color: #e0e0e0;
            }}
            .header {{
                display: flex; justify-content: space-between; align-items: center;
                margin-bottom: 25px; padding-bottom: 20px; border-bottom: 1px solid #333;
                flex-wrap: wrap; gap: 15px;
            }}
            .header h1 {{ color: #ff6b35; margin: 0; font-size: 22px; }}
            .header-right {{ display: flex; align-items: center; gap: 15px; }}
            .code-badge {{
                background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
                padding: 8px 16px; border-radius: 20px; font-weight: bold;
                font-size: 14px;
            }}
            .logout {{
                color: #888; text-decoration: none; font-size: 14px;
                padding: 8px 16px; background: #222; border-radius: 8px;
            }}
            .logout:hover {{ background: #333; }}

            .grid {{
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
                gap: 12px; margin-bottom: 25px;
            }}
            .card {{
                background: #1a1a1a; border-radius: 12px; padding: 18px;
                box-shadow: 0 2px 10px rgba(0,0,0,0.2);
            }}
            .card.highlight {{
                background: linear-gradient(135deg, #1a2a1a 0%, #1a1a1a 100%);
                border: 1px solid #2a5a2a;
            }}
            .card.pending {{
                background: linear-gradient(135deg, #2a2a1a 0%, #1a1a1a 100%);
                border: 1px solid #5a5a2a;
            }}
            .card h2 {{
                color: #888; margin: 0 0 10px 0; font-size: 11px;
                text-transform: uppercase; letter-spacing: 0.5px;
            }}
            .stat {{ font-size: 26px; font-weight: bold; color: #fff; }}
            .stat.green {{ color: #4ade80; }}
            .stat.yellow {{ color: #fbbf24; }}
            .stat.small {{ font-size: 20px; }}
            .stat-sub {{ color: #666; font-size: 11px; margin-top: 4px; }}

            .section {{ margin-bottom: 30px; }}
            .section-title {{
                color: #ff6b35; font-size: 16px; margin-bottom: 15px;
                padding-bottom: 10px; border-bottom: 1px solid #333;
                display: flex; align-items: center; gap: 10px;
            }}

            .table-wrap {{ overflow-x: auto; }}
            .table {{
                width: 100%; border-collapse: collapse; font-size: 13px;
                min-width: 500px;
            }}
            .table th, .table td {{
                padding: 12px 10px; text-align: left;
                border-bottom: 1px solid #2a2a2a;
            }}
            .table th {{
                color: #888; font-weight: 600; font-size: 11px;
                text-transform: uppercase; background: #151515;
            }}
            .table tr:hover {{ background: #1f1f1f; }}

            .badge {{
                display: inline-block; padding: 4px 10px; border-radius: 12px;
                font-size: 10px; font-weight: 600; text-transform: uppercase;
            }}
            .badge.pending {{ background: #3f3f00; color: #fbbf24; }}
            .badge.confirmed {{ background: #064e3b; color: #4ade80; }}
            .badge.paid {{ background: #1e3a5f; color: #60a5fa; }}
            .badge.cancelled {{ background: #450a0a; color: #ef4444; }}

            .promo-box {{
                background: linear-gradient(135deg, #1a1a2e 0%, #1a1a1a 100%);
                border: 1px solid #333; border-radius: 12px; padding: 20px;
                margin-bottom: 25px;
            }}
            .promo-box h3 {{ color: #60a5fa; margin: 0 0 12px 0; font-size: 14px; }}
            .promo-link {{
                background: #222; padding: 12px 15px; border-radius: 8px;
                font-family: monospace; font-size: 13px; color: #4ade80;
                word-break: break-all; margin: 10px 0;
            }}
            .copy-btn {{
                background: #333; color: #fff; border: none; padding: 8px 16px;
                border-radius: 6px; cursor: pointer; font-size: 12px; margin-top: 10px;
            }}
            .copy-btn:hover {{ background: #444; }}

            .payout-info {{
                background: #1a1a1a; border-radius: 12px; padding: 20px;
                border: 1px solid #333;
            }}
            .payout-info h3 {{ color: #ff6b35; margin: 0 0 15px 0; }}
            .payout-row {{
                display: flex; justify-content: space-between;
                padding: 10px 0; border-bottom: 1px solid #2a2a2a;
            }}
            .payout-row:last-child {{ border-bottom: none; }}
            .payout-label {{ color: #888; }}
            .payout-value {{ color: #fff; font-weight: 500; }}

            .empty-state {{
                text-align: center; padding: 40px 20px; color: #666;
            }}
            .empty-state .icon {{ font-size: 48px; margin-bottom: 15px; }}

            @media (max-width: 600px) {{
                .header {{ flex-direction: column; align-items: flex-start; }}
                .grid {{ grid-template-columns: repeat(2, 1fr); }}
                .stat {{ font-size: 22px; }}
            }}
        </style>
    </head>
    <body>
        <div class="header">
            <h1>🔥 Partner Dashboard</h1>
            <div class="header-right">
                <div class="code-badge">{partner_code}</div>
                <a href="/partner" class="logout">Logout</a>
            </div>
        </div>

        <!-- Welcome & Stats -->
        <div class="section">
            <div class="grid">
                <div class="card highlight">
                    <h2>Total Earnings</h2>
                    <div class="stat green" id="total-earnings">$0.00</div>
                    <div class="stat-sub">Lifetime commissions</div>
                </div>
                <div class="card pending">
                    <h2>Pending Payout</h2>
                    <div class="stat yellow" id="pending-payout">$0.00</div>
                    <div class="stat-sub">Ready to be paid</div>
                </div>
                <div class="card">
                    <h2>Total Referrals</h2>
                    <div class="stat small" id="total-referrals">0</div>
                    <div class="stat-sub">All time</div>
                </div>
                <div class="card">
                    <h2>This Month</h2>
                    <div class="stat small" id="month-referrals">0</div>
                    <div class="stat-sub">New referrals</div>
                </div>
                <div class="card">
                    <h2>Commission Rate</h2>
                    <div class="stat small">{commission_percent}%</div>
                    <div class="stat-sub">Per subscription</div>
                </div>
                <div class="card">
                    <h2>Conversion Rate</h2>
                    <div class="stat small" id="conversion-rate">0%</div>
                    <div class="stat-sub">Code uses → subs</div>
                </div>
            </div>
        </div>

        <!-- Promo Link -->
        <div class="promo-box">
            <h3>📢 Your Referral Link</h3>
            <p style="color: #888; font-size: 13px; margin: 0 0 10px 0;">
                Share this link or tell your followers to use code <strong style="color: #ff6b35;">{partner_code}</strong> at checkout
            </p>
            <div class="promo-link" id="promo-link">https://prometheus.fitness/download?ref={partner_code}</div>
            <button class="copy-btn" onclick="copyLink()">📋 Copy Link</button>
            <button class="copy-btn" onclick="copyCode()" style="margin-left: 8px;">Copy Code: {partner_code}</button>
        </div>

        <!-- Recent Referrals -->
        <div class="section">
            <div class="section-title">📊 Recent Referrals</div>
            <div class="card">
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Subscription</th>
                                <th>Amount</th>
                                <th>Your Commission</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody id="referrals-table">
                            <tr><td colspan="5" class="empty-state">
                                <div class="icon">📭</div>
                                Loading referrals...
                            </td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Payout History -->
        <div class="section">
            <div class="section-title">💰 Payout History</div>
            <div class="card">
                <div class="table-wrap">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Period</th>
                                <th>Referrals</th>
                                <th>Amount</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody id="payouts-table">
                            <tr><td colspan="5" class="empty-state">
                                <div class="icon">💳</div>
                                No payouts yet
                            </td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Payout Info -->
        <div class="section">
            <div class="section-title">ℹ️ Payout Information</div>
            <div class="payout-info">
                <div class="payout-row">
                    <span class="payout-label">Minimum Payout</span>
                    <span class="payout-value">$50.00</span>
                </div>
                <div class="payout-row">
                    <span class="payout-label">Payout Schedule</span>
                    <span class="payout-value">Monthly (1st of month)</span>
                </div>
                <div class="payout-row">
                    <span class="payout-label">Commission Hold Period</span>
                    <span class="payout-value">14 days (for refunds)</span>
                </div>
                <div class="payout-row">
                    <span class="payout-label">Your Payout Method</span>
                    <span class="payout-value" id="payout-method">Not configured</span>
                </div>
            </div>
        </div>

        <script>
            const partnerCode = '{partner_code}';

            function copyLink() {{
                navigator.clipboard.writeText(document.getElementById('promo-link').textContent);
                alert('Link copied to clipboard!');
            }}

            function copyCode() {{
                navigator.clipboard.writeText(partnerCode);
                alert('Code copied: ' + partnerCode);
            }}

            async function loadPartnerData() {{
                try {{
                    const res = await fetch(`/api/v1/partner/stats?code=${{partnerCode}}`);
                    const data = await res.json();

                    // Update stats
                    document.getElementById('total-earnings').textContent = '$' + (data.total_earnings || 0).toFixed(2);
                    document.getElementById('pending-payout').textContent = '$' + (data.pending_payout || 0).toFixed(2);
                    document.getElementById('total-referrals').textContent = data.total_referrals || 0;
                    document.getElementById('month-referrals').textContent = data.month_referrals || 0;
                    document.getElementById('conversion-rate').textContent = (data.conversion_rate || 0).toFixed(1) + '%';
                    document.getElementById('payout-method').textContent = data.payout_method || 'Not configured';

                    // Update referrals table
                    if (data.recent_referrals && data.recent_referrals.length > 0) {{
                        document.getElementById('referrals-table').innerHTML = data.recent_referrals.map(r => `
                            <tr>
                                <td>${{r.date}}</td>
                                <td>${{r.subscription_type}}</td>
                                <td>${{r.amount}}</td>
                                <td style="color: #4ade80; font-weight: 600;">${{r.commission}}</td>
                                <td><span class="badge ${{r.status.toLowerCase()}}">${{r.status}}</span></td>
                            </tr>
                        `).join('');
                    }} else {{
                        document.getElementById('referrals-table').innerHTML = `
                            <tr><td colspan="5" class="empty-state">
                                <div class="icon">📭</div>
                                No referrals yet. Share your code to start earning!
                            </td></tr>
                        `;
                    }}

                    // Update payouts table
                    if (data.payouts && data.payouts.length > 0) {{
                        document.getElementById('payouts-table').innerHTML = data.payouts.map(p => `
                            <tr>
                                <td>${{p.date}}</td>
                                <td>${{p.period}}</td>
                                <td>${{p.referral_count}}</td>
                                <td style="color: #4ade80; font-weight: 600;">${{p.amount}}</td>
                                <td><span class="badge ${{p.status.toLowerCase()}}">${{p.status}}</span></td>
                            </tr>
                        `).join('');
                    }}

                }} catch (e) {{
                    console.error('Error loading partner data:', e);
                }}
            }}

            // Load data on page load
            loadPartnerData();

            // Refresh every 60 seconds
            setInterval(loadPartnerData, 60000);
        </script>
    </body>
    </html>
    """)


@app.post("/api/v1/partner/login")
async def partner_login(request: Request):
    """Partner login endpoint"""
    try:
        data = await request.json()
        referral_code = data.get('referral_code', '').upper()
        password = data.get('password', '')

        if not referral_code or not password:
            return {"success": False, "error": "Please enter code and password"}

        # Verify partner credentials
        from supabase import create_client
        import hashlib
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        result = client.table('partners').select('*').eq('referral_code', referral_code).execute()

        if not result.data or len(result.data) == 0:
            return {"success": False, "error": "Invalid referral code"}

        partner = result.data[0]

        if partner.get('status') != 'active':
            return {"success": False, "error": "Account is not active"}

        # Check password
        stored_hash = partner.get('payout_details', {}).get('password_hash', '')
        input_hash = hashlib.sha256(password.encode()).hexdigest()

        if stored_hash != input_hash:
            return {"success": False, "error": "Invalid password"}

        return {"success": True, "partner_code": referral_code}

    except Exception as e:
        print(f"Partner login error: {e}")
        return {"success": False, "error": "Login failed"}


@app.get("/api/v1/partner/stats")
async def get_partner_stats(code: str):
    """Get partner statistics for their dashboard"""
    try:
        from supabase import create_client
        from datetime import datetime, timedelta
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Get partner
        partner_result = client.table('partners').select('*').eq('referral_code', code.upper()).execute()

        if not partner_result.data:
            return {"error": "Partner not found"}

        partner = partner_result.data[0]
        partner_id = partner['id']

        # Get referrals
        referrals_result = client.table('partner_referrals').select('*').eq('partner_id', partner_id).order('created_at', desc=True).execute()
        referrals = referrals_result.data or []

        # Calculate stats
        total_earnings = sum(float(r.get('commission_amount', 0)) for r in referrals)
        pending_payout = sum(float(r.get('commission_amount', 0)) for r in referrals if r.get('commission_status') in ['pending', 'confirmed'])

        # This month's referrals
        month_start = datetime.now().replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        month_referrals = sum(1 for r in referrals if r.get('created_at') and r.get('created_at') >= month_start.isoformat())

        # Get code entries for conversion rate
        entries_result = client.table('referral_code_entries').select('*').eq('partner_id', partner_id).execute()
        total_entries = len(entries_result.data or [])
        conversion_rate = (len(referrals) / total_entries * 100) if total_entries > 0 else 0

        # Format recent referrals for display
        recent_referrals = []
        for r in referrals[:10]:  # Last 10
            recent_referrals.append({
                "date": r.get('created_at', '')[:10] if r.get('created_at') else '-',
                "subscription_type": r.get('subscription_type', 'subscription').replace('_', ' ').title(),
                "amount": f"${float(r.get('gross_amount', 0)):.2f}",
                "commission": f"${float(r.get('commission_amount', 0)):.2f}",
                "status": r.get('commission_status', 'pending').title()
            })

        # Get payouts
        payouts_result = client.table('partner_payouts').select('*').eq('partner_id', partner_id).order('created_at', desc=True).execute()
        payouts = []
        for p in (payouts_result.data or [])[:10]:
            payouts.append({
                "date": p.get('completed_at', p.get('created_at', ''))[:10] if p.get('completed_at') or p.get('created_at') else '-',
                "period": f"{p.get('period_start', '')} - {p.get('period_end', '')}",
                "referral_count": p.get('referral_count', 0),
                "amount": f"${float(p.get('amount', 0)):.2f}",
                "status": p.get('status', 'pending').title()
            })

        # Payout method
        payout_method = partner.get('payout_method', 'Not configured')
        if payout_method:
            payout_method = payout_method.replace('_', ' ').title()

        return {
            "total_earnings": total_earnings,
            "pending_payout": pending_payout,
            "total_referrals": len(referrals),
            "month_referrals": month_referrals,
            "conversion_rate": conversion_rate,
            "payout_method": payout_method,
            "recent_referrals": recent_referrals,
            "payouts": payouts
        }

    except Exception as e:
        print(f"Partner stats error: {e}")
        return {"error": str(e)}


# ============================================================
# ADMIN - PARTNER MANAGEMENT ENDPOINTS
# ============================================================

@app.get("/api/v1/admin/partners")
async def admin_get_partners(password: str):
    """Get all partners with their stats"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Try to get from partner_statistics view first
        try:
            result = client.table('partner_statistics').select('*').execute()
            return result.data or []
        except:
            # Fallback to partners table
            result = client.table('partners').select('*').order('created_at', desc=True).execute()
            return result.data or []

    except Exception as e:
        print(f"Admin partners error: {e}")
        return []


@app.post("/api/v1/admin/partners")
async def admin_create_partner(request: Request, password: str):
    """Create a new partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        import hashlib
        from supabase import create_client
        data = await request.json()

        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Generate password hash
        password_plain = data.get('password', 'prometheus2024')
        password_hash = hashlib.sha256(password_plain.encode()).hexdigest()

        # Generate referral code if not provided
        referral_code = data.get('referral_code', '').upper()
        if not referral_code:
            # Generate from name
            name = data.get('name', 'PARTNER')
            base_code = ''.join(c for c in name.upper() if c.isalnum())[:6]
            referral_code = base_code if base_code else 'PARTNER'

            # Check if exists and add number if needed
            existing = client.table('partners').select('referral_code').eq('referral_code', referral_code).execute()
            counter = 1
            while existing.data:
                referral_code = f"{base_code}{counter}"
                existing = client.table('partners').select('referral_code').eq('referral_code', referral_code).execute()
                counter += 1

        partner_data = {
            'name': data.get('name'),
            'email': data.get('email', '').lower(),
            'referral_code': referral_code,
            'partner_type': data.get('partner_type', 'affiliate'),
            'commission_percent': data.get('commission_percent', 15),
            'status': data.get('status', 'active'),
            'instagram_handle': data.get('instagram_handle'),
            'follower_count': data.get('follower_count'),
            'payout_method': data.get('payout_method'),
            'payout_details': {
                'password_hash': password_hash
            },
            'notes': data.get('notes')
        }

        result = client.table('partners').insert(partner_data).execute()

        return {
            "success": True,
            "partner": result.data[0] if result.data else None,
            "generated_password": password_plain,
            "referral_code": referral_code
        }

    except Exception as e:
        print(f"Admin create partner error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.put("/api/v1/admin/partners/{partner_id}")
async def admin_update_partner(partner_id: str, request: Request, password: str):
    """Update a partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        import hashlib
        data = await request.json()

        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        update_data = {}

        # Only update fields that are provided
        if 'name' in data:
            update_data['name'] = data['name']
        if 'email' in data:
            update_data['email'] = data['email'].lower()
        if 'partner_type' in data:
            update_data['partner_type'] = data['partner_type']
        if 'commission_percent' in data:
            update_data['commission_percent'] = data['commission_percent']
        if 'status' in data:
            update_data['status'] = data['status']
        if 'payout_method' in data:
            update_data['payout_method'] = data['payout_method']
        if 'notes' in data:
            update_data['notes'] = data['notes']
        if 'instagram_handle' in data:
            update_data['instagram_handle'] = data['instagram_handle']
        if 'follower_count' in data:
            update_data['follower_count'] = data['follower_count']

        # Handle password reset
        if 'new_password' in data and data['new_password']:
            password_hash = hashlib.sha256(data['new_password'].encode()).hexdigest()
            # Get existing payout_details and update
            existing = client.table('partners').select('payout_details').eq('id', partner_id).execute()
            payout_details = existing.data[0].get('payout_details', {}) if existing.data else {}
            payout_details['password_hash'] = password_hash
            update_data['payout_details'] = payout_details

        result = client.table('partners').update(update_data).eq('id', partner_id).execute()

        return {"success": True, "partner": result.data[0] if result.data else None}

    except Exception as e:
        print(f"Admin update partner error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/v1/admin/partners/{partner_id}")
async def admin_delete_partner(partner_id: str, password: str):
    """Delete a partner (soft delete - sets status to terminated)"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        # Soft delete - set status to terminated
        result = client.table('partners').update({'status': 'terminated'}).eq('id', partner_id).execute()

        return {"success": True}

    except Exception as e:
        print(f"Admin delete partner error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/admin/partner-referrals")
async def admin_get_all_referrals(password: str, partner_id: Optional[str] = None):
    """Get all referrals, optionally filtered by partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        query = client.table('partner_referrals').select('*, partners(name, referral_code)')

        if partner_id:
            query = query.eq('partner_id', partner_id)

        result = query.order('created_at', desc=True).limit(100).execute()

        return result.data or []

    except Exception as e:
        print(f"Admin referrals error: {e}")
        return []


@app.post("/api/v1/admin/partner-payouts")
async def admin_create_payout(request: Request, password: str):
    """Create a payout for a partner"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        from datetime import datetime
        data = await request.json()

        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        partner_id = data.get('partner_id')

        # Get confirmed referrals that haven't been paid
        referrals_result = client.table('partner_referrals')\
            .select('*')\
            .eq('partner_id', partner_id)\
            .eq('commission_status', 'confirmed')\
            .is_('payout_id', 'null')\
            .execute()

        referrals = referrals_result.data or []

        if not referrals:
            return {"success": False, "error": "No confirmed referrals to pay"}

        total_amount = sum(float(r.get('commission_amount', 0)) for r in referrals)

        # Create payout record
        payout_data = {
            'partner_id': partner_id,
            'amount': total_amount,
            'referral_count': len(referrals),
            'period_start': data.get('period_start', referrals[-1].get('created_at', '')[:10]),
            'period_end': data.get('period_end', referrals[0].get('created_at', '')[:10]),
            'status': 'pending'
        }

        payout_result = client.table('partner_payouts').insert(payout_data).execute()
        payout_id = payout_result.data[0]['id']

        # Update referrals with payout_id
        for r in referrals:
            client.table('partner_referrals').update({
                'payout_id': payout_id,
                'commission_status': 'paid',
                'paid_at': datetime.now().isoformat()
            }).eq('id', r['id']).execute()

        return {"success": True, "payout_id": payout_id, "amount": total_amount, "referral_count": len(referrals)}

    except Exception as e:
        print(f"Admin create payout error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.put("/api/v1/admin/partner-payouts/{payout_id}")
async def admin_update_payout(payout_id: str, request: Request, password: str):
    """Update payout status (mark as completed, etc.)"""
    if not verify_admin(password):
        raise HTTPException(status_code=401, detail="Unauthorized")

    try:
        from supabase import create_client
        from datetime import datetime
        data = await request.json()

        supabase_url = os.environ.get("SUPABASE_URL")
        supabase_key = os.environ.get("SUPABASE_KEY")
        client = create_client(supabase_url, supabase_key)

        update_data = {'status': data.get('status', 'completed')}

        if data.get('status') == 'completed':
            update_data['completed_at'] = datetime.now().isoformat()

        if 'payout_reference' in data:
            update_data['payout_reference'] = data['payout_reference']

        result = client.table('partner_payouts').update(update_data).eq('id', payout_id).execute()

        return {"success": True}

    except Exception as e:
        print(f"Admin update payout error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================
# VBT (VELOCITY BASED TRAINING) ENDPOINTS
# ============================================================

@app.get("/api/health")
async def vbt_health_check():
    """Health check endpoint for VBT service"""
    return {
        "status": "ok",
        "version": "1.0.0",
        "modelLoaded": VBT_AVAILABLE
    }


@app.post("/api/upload-chunk")
async def upload_vbt_chunk(
    chunk: UploadFile = File(...),
    chunk_number: str = Form(...),
    total_chunks: str = Form(...),
    upload_id: str = Form(...)
):
    """
    Upload video chunk for VBT analysis

    Supports chunked upload for large video files.
    """
    try:
        chunk_num = int(chunk_number)
        total = int(total_chunks)

        with vbt_lock:
            # Initialize upload tracking
            if upload_id not in vbt_uploads:
                vbt_uploads[upload_id] = {
                    "chunks": {},
                    "total_chunks": total,
                    "created_at": datetime.now().isoformat()
                }

            # Save chunk to disk
            chunk_path = VBT_UPLOAD_DIR / f"{upload_id}_chunk_{chunk_num}"
            content = await chunk.read()
            async with aiofiles.open(chunk_path, 'wb') as f:
                await f.write(content)

            vbt_uploads[upload_id]["chunks"][chunk_num] = str(chunk_path)

            chunks_received = len(vbt_uploads[upload_id]["chunks"])
            upload_complete = chunks_received >= total

        print(f"[VBT] Chunk {chunk_num + 1}/{total} received for {upload_id}")

        return {
            "success": True,
            "message": f"Chunk {chunk_num + 1} of {total} received",
            "chunksReceived": chunks_received,
            "totalChunks": total,
            "uploadComplete": upload_complete
        }

    except Exception as e:
        print(f"[VBT] Chunk upload error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/process-video")
async def start_vbt_processing(request_data: dict, background_tasks: BackgroundTasks):
    """
    Start VBT video processing

    Request:
    {
        "uploadId": "uuid",
        "exerciseType": "squat",
        "weightKg": 100.0,
        "oneRM": 140.0,
        "userHeightCm": 180.0
    }
    """
    if not VBT_AVAILABLE:
        raise HTTPException(status_code=503, detail="VBT service not available")

    upload_id = request_data.get("uploadId") or request_data.get("upload_id")
    exercise_type = request_data.get("exerciseType") or request_data.get("exercise_type", "squat")
    weight_kg = request_data.get("weightKg") or request_data.get("weight_kg")
    one_rm = request_data.get("oneRM") or request_data.get("one_rm")
    user_height_cm = request_data.get("userHeightCm") or request_data.get("user_height_cm", 175.0)

    if not upload_id or upload_id not in vbt_uploads:
        raise HTTPException(status_code=400, detail="Invalid or missing upload_id")

    # Check if all chunks received
    upload_info = vbt_uploads[upload_id]
    if len(upload_info["chunks"]) < upload_info["total_chunks"]:
        raise HTTPException(
            status_code=400,
            detail=f"Upload incomplete: {len(upload_info['chunks'])}/{upload_info['total_chunks']} chunks"
        )

    # Create task
    task_id = str(uuid.uuid4())
    with vbt_lock:
        vbt_tasks[task_id] = {
            "status": "pending",
            "progress": 0,
            "currentStep": "Initializing...",
            "results": None,
            "error": None,
            "created_at": datetime.now().isoformat()
        }

    # Start background processing
    background_tasks.add_task(
        process_vbt_video,
        task_id,
        upload_id,
        exercise_type,
        weight_kg,
        one_rm,
        user_height_cm
    )

    print(f"[VBT] Processing started: task={task_id}, upload={upload_id}")

    return {
        "success": True,
        "taskId": task_id,
        "message": "Processing started"
    }


def process_vbt_video(
    task_id: str,
    upload_id: str,
    exercise_type: str,
    weight_kg: Optional[float],
    one_rm: Optional[float],
    user_height_cm: float
):
    """Background task for VBT video processing"""
    try:
        with vbt_lock:
            vbt_tasks[task_id]["status"] = "processing"
            vbt_tasks[task_id]["progress"] = 5
            vbt_tasks[task_id]["currentStep"] = "Assembling video..."

        # Assemble chunks into single video file
        upload_info = vbt_uploads[upload_id]
        video_path = VBT_UPLOAD_DIR / f"{upload_id}_complete.mp4"

        with open(video_path, 'wb') as outfile:
            for i in range(upload_info["total_chunks"]):
                chunk_path = upload_info["chunks"][i]
                with open(chunk_path, 'rb') as chunk_file:
                    outfile.write(chunk_file.read())

        with vbt_lock:
            vbt_tasks[task_id]["progress"] = 15
            vbt_tasks[task_id]["currentStep"] = "Analyzing video..."

        # Run VBT analysis
        def progress_callback(progress: int, step: str):
            with vbt_lock:
                # Scale progress to 15-95 range
                scaled_progress = 15 + int(progress * 0.8)
                vbt_tasks[task_id]["progress"] = scaled_progress
                vbt_tasks[task_id]["currentStep"] = step

        analyzer = VBTAnalyzer(user_height_cm=user_height_cm)
        results = analyzer.analyze_video(
            video_path=str(video_path),
            exercise_type=exercise_type,
            weight_kg=weight_kg,
            one_rm=one_rm,
            progress_callback=progress_callback
        )

        # Add task_id to results
        results["taskId"] = task_id

        with vbt_lock:
            vbt_tasks[task_id]["status"] = "completed"
            vbt_tasks[task_id]["progress"] = 100
            vbt_tasks[task_id]["currentStep"] = "Complete"
            vbt_tasks[task_id]["results"] = results

        print(f"[VBT] Processing complete: {task_id}, reps={results.get('totalReps', 0)}")

        # Cleanup chunks
        for chunk_path in upload_info["chunks"].values():
            try:
                Path(chunk_path).unlink()
            except:
                pass

        # Keep video for potential re-analysis (cleanup after 1 hour in production)

    except Exception as e:
        print(f"[VBT] Processing error: {e}")
        with vbt_lock:
            vbt_tasks[task_id]["status"] = "failed"
            vbt_tasks[task_id]["error"] = str(e)
            vbt_tasks[task_id]["currentStep"] = "Failed"


@app.get("/api/status/{task_id}")
async def get_vbt_status(task_id: str):
    """Get VBT processing status"""
    if task_id not in vbt_tasks:
        raise HTTPException(status_code=404, detail="Task not found")

    task = vbt_tasks[task_id]

    return {
        "taskId": task_id,
        "status": task["status"],
        "progress": task["progress"],
        "currentStep": task["currentStep"],
        "error": task["error"]
    }


@app.get("/api/results/{task_id}")
async def get_vbt_results(task_id: str):
    """Get VBT analysis results"""
    if task_id not in vbt_tasks:
        raise HTTPException(status_code=404, detail="Task not found")

    task = vbt_tasks[task_id]

    if task["status"] != "completed":
        raise HTTPException(
            status_code=400,
            detail=f"Task not completed: {task['status']}"
        )

    return task["results"]


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
