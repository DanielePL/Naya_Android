"""
Form Analysis Router - MediaPipe Pose Analysis Endpoints
"""

from fastapi import APIRouter, File, UploadFile, HTTPException, Form
from fastapi.responses import JSONResponse, FileResponse
import aiofiles
import subprocess
import shutil
from pathlib import Path
from typing import Optional
from datetime import datetime

from ..pose_processor import PoseProcessor
from ..supabase_client import SupabaseFormAnalysisClient

router = APIRouter(prefix="/api/v1", tags=["Form Analysis"])

# Configuration
UPLOAD_DIR = Path("uploads")
OUTPUT_DIR = Path("outputs")
UPLOAD_DIR.mkdir(exist_ok=True)
OUTPUT_DIR.mkdir(exist_ok=True)


def find_output_video(output_dir: Path) -> Optional[Path]:
    """Find the analyzed video file in output directory"""
    video_files = list(output_dir.glob("*.mp4")) + list(output_dir.glob("*.avi"))
    return video_files[0] if video_files else None


def calculate_form_metrics(pose_data: dict, exercise_type: Optional[str]) -> dict:
    """
    Calculate exercise-specific form metrics
    """
    if not exercise_type:
        return {"message": "No exercise type specified"}

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


@router.post("/analyze-form")
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

    video_path_abs = video_path.absolute()
    output_path_abs = output_path.absolute()

    try:
        # Save uploaded video
        async with aiofiles.open(video_path, 'wb') as out_file:
            content = await video.read()
            await out_file.write(content)

        # Check video rotation metadata before normalizing
        rotation = 0
        try:
            probe_result = subprocess.run(
                [
                    "ffprobe", "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream_tags=rotate:stream=width,height",
                    "-of", "csv=p=0",
                    str(video_path_abs)
                ],
                capture_output=True,
                text=True,
                timeout=10
            )
            if probe_result.returncode == 0:
                output = probe_result.stdout.strip()
                # Parse rotation from output
                if "90" in output:
                    rotation = 90
                elif "180" in output:
                    rotation = 180
                elif "270" in output:
                    rotation = 270
                print(f"Video rotation metadata: {rotation}°")
        except Exception as e:
            print(f"Could not probe video rotation: {e}")

        # Only apply rotation fix if video has rotation metadata
        normalized_video = video_path
        if rotation != 0:
            normalized_video = video_path.parent / f"normalized_{video_path.name}"

            # Determine transpose filter based on rotation
            if rotation == 90:
                transpose_filter = "transpose=1"  # 90° CW
            elif rotation == 180:
                transpose_filter = "transpose=1,transpose=1"  # 180°
            elif rotation == 270:
                transpose_filter = "transpose=2"  # 90° CCW
            else:
                transpose_filter = None

            if transpose_filter:
                normalize_result = subprocess.run(
                    [
                        "ffmpeg",
                        "-noautorotate",
                        "-i", str(video_path_abs),
                        "-vf", transpose_filter,
                        "-c:v", "libx264",
                        "-preset", "fast",
                        "-crf", "23",
                        "-pix_fmt", "yuv420p",
                        "-metadata:s:v", "rotate=0",
                        "-y",
                        str(normalized_video.absolute())
                    ],
                    capture_output=True,
                    text=True,
                    timeout=60
                )

                if normalize_result.returncode != 0:
                    print(f"Video normalization failed, using original: {normalize_result.stderr}")
                    normalized_video = video_path
                else:
                    print(f"Video normalized successfully (rotation: {rotation}° → 0°)")
                    video_path_abs = normalized_video.absolute()
        else:
            print(f"Video has no rotation metadata, skipping normalization")

        # Process with MediaPipe Pose
        print(f"Processing video with MediaPipe...")
        processor = PoseProcessor()

        try:
            result = processor.process_video(
                video_path=normalized_video.absolute(),
                output_dir=output_path_abs,
                save_video=True,
                save_pose_data=True,
                exercise_type=exercise_type or "general"
            )

            print(f"MediaPipe processing complete: {result['frames_processed']} frames")

            pose_data = result['pose_data']
            analyzed_video = result['output_video']

        except Exception as e:
            print(f"MediaPipe processing failed: {str(e)}")
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
            "vbt_metrics": velocity_metrics,
            "calibration": calibration_info,
            "weight_detected": None,
            "form_metrics": form_metrics,
            "video_available": analyzed_video is not None,
            "download_url": f"/api/v1/download/{analysis_id}" if analyzed_video else None
        }

        # Debug output
        summary = velocity_metrics.get('summary', {})
        unit = summary.get('unit', 'speed_index')
        if unit == 'm/s':
            avg_value = summary.get('avg_peak_velocity', 0)
            print(f"API Response: reps={velocity_metrics.get('reps_detected', 0)}, avg_peak={avg_value:.3f} m/s [{calibration_info.get('tier', 'unknown')}]")
        else:
            avg_value = summary.get('avg_speed_index', 0)
            print(f"API Response: reps={velocity_metrics.get('reps_detected', 0)}, avg_speed_index={avg_value:.1f} [{calibration_info.get('tier', 'relative')}]")

        # Save to Supabase if user_id and set_id are provided
        if user_id and set_id:
            try:
                print(f"Saving to Supabase: user={user_id}, set={set_id}, session={session_id}")
                supabase_client = SupabaseFormAnalysisClient()

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
                    print(f"Supabase save successful: form_analysis_id={supabase_result.get('form_analysis_id')}")
                    response['supabase'] = {
                        'saved': True,
                        'form_analysis_id': supabase_result.get('form_analysis_id'),
                        'reps_saved': supabase_result.get('reps_saved')
                    }
                else:
                    print(f"Supabase save failed: {supabase_result.get('error')}")
                    response['supabase'] = {
                        'saved': False,
                        'error': supabase_result.get('error')
                    }
            except Exception as e:
                print(f"Supabase save error: {str(e)}")
                response['supabase'] = {
                    'saved': False,
                    'error': str(e)
                }
        else:
            print(f"Skipping Supabase save (user_id or set_id not provided)")
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


@router.get("/download/{analysis_id}")
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


@router.delete("/analysis/{analysis_id}")
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


@router.get("/exercises")
async def get_supported_exercises():
    """Get list of supported exercises for form analysis"""
    return {
        "exercises": [
            {"id": "squat", "name": "Barbell Squat", "supported": True},
            {"id": "deadlift", "name": "Deadlift", "supported": True},
            {"id": "bench_press", "name": "Bench Press", "supported": True},
            {"id": "overhead_press", "name": "Overhead Press", "supported": True},
            {"id": "barbell_row", "name": "Barbell Row", "supported": True},
        ],
        "note": "Form metrics are exercise-specific. VBT works for all barbell exercises."
    }