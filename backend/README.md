# Prometheus Backend - MediaPipe Form Analysis Service

This backend service provides 2D pose estimation and kinematics analysis for workout videos using MediaPipe Pose.

## Features

- **Video Upload**: Accept workout videos from Android app
- **Pose Detection**: Extract 2D joint positions using MediaPipe Pose
- **VBT Analysis**: Velocity-based training metrics with movement tracking
- **Form Analysis**: Calculate exercise-specific metrics (angles, depth, bar path)
- **Video Overlay**: Return analyzed video with pose overlays
- **REST API**: Easy integration with Android app
- **Supabase Integration**: Automatic saving of form analysis results

## Requirements

- Python 3.10, 3.11, or 3.12
- 4GB+ RAM recommended
- GPU optional (CPU inference works fine for most videos)

## Installation

### 1. Create Python Virtual Environment

```bash
cd backend
python3.11 -m venv venv
source venv/bin/activate  # On macOS/Linux
# OR
venv\Scripts\activate  # On Windows
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

This will install:
- FastAPI (web framework)
- MediaPipe (pose estimation)
- OpenCV (video processing)
- SciPy (VBT signal processing)
- Supabase (database integration)
- And all dependencies

### 3. Test Installation

```bash
python -c "import mediapipe; print('MediaPipe installed:', mediapipe.__version__)"
```

## Running the Server

### Development Mode

```bash
cd prometheus_backend
python main.py
```

The server will start at `http://localhost:8000`

### Production Mode

```bash
cd prometheus_backend
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

## API Endpoints

### 1. Health Check

```bash
GET /
```

Returns service status.

### 2. Analyze Form

```bash
POST /api/v1/analyze-form
Content-Type: multipart/form-data

Parameters:
- video: Video file (MP4, AVI, MOV)
- exercise_type: Optional (squat, deadlift, bench_press, etc.)
- calibrated: Optional boolean

Response:
{
  "analysis_id": "20241119_143022_squat.mp4",
  "exercise_type": "squat",
  "timestamp": "2024-11-19T14:30:22",
  "pose_data": { ... },
  "form_metrics": {
    "target_angles": {
      "knee": {"min": 80, "max": 130},
      "hip": {"min": 70, "max": 120}
    }
  },
  "video_available": true,
  "download_url": "/api/v1/download/20241119_143022_squat.mp4"
}
```

### 3. Download Analyzed Video

```bash
GET /api/v1/download/{analysis_id}
```

Returns video file with pose overlays.

### 4. Delete Analysis

```bash
DELETE /api/v1/analysis/{analysis_id}
```

Removes analysis data to free storage.

### 5. Get Supported Exercises

```bash
GET /api/v1/exercises
```

Returns list of exercises with analysis support.

## Testing with cURL

### Upload a video for analysis:

```bash
curl -X POST "http://localhost:8000/api/v1/analyze-form" \
  -F "video=@/path/to/squat_video.mp4" \
  -F "exercise_type=squat"
```

### Download analyzed video:

```bash
curl "http://localhost:8000/api/v1/download/20241119_143022_squat.mp4" \
  --output analyzed_squat.mp4
```

## Android Integration

### Add Retrofit Dependency

In `Prometheus_V1/build.gradle.kts`:

```kotlin
dependencies {
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
```

### Create API Service

```kotlin
interface FormAnalysisApiService {
    @Multipart
    @POST("api/v1/analyze-form")
    suspend fun analyzeForm(
        @Part video: MultipartBody.Part,
        @Part("exercise_type") exerciseType: RequestBody? = null
    ): FormAnalysisResponse

    @GET("api/v1/download/{analysisId}")
    suspend fun downloadAnalyzedVideo(
        @Path("analysisId") analysisId: String
    ): ResponseBody
}
```

### Usage in ViewModel

```kotlin
class FormAnalysisViewModel : ViewModel() {
    suspend fun analyzeWorkoutForm(videoUri: Uri, exerciseType: String) {
        val videoPart = prepareFilePart("video", videoUri)
        val exerciseTypePart = exerciseType.toRequestBody()

        val result = apiService.analyzeForm(videoPart, exerciseTypePart)
        // Handle result
    }
}
```

## Deployment Options

### Local Network (Development)

Run on your computer, access from Android emulator or physical device on same network:

```bash
# Find your local IP
ifconfig | grep "inet " | grep -v 127.0.0.1

# Run server
uvicorn main:app --host 0.0.0.0 --port 8000

# In Android app, use: http://YOUR_LOCAL_IP:8000
```

### Cloud Deployment

#### Railway (Recommended)

1. Create `Procfile`:
```
web: uvicorn prometheus_backend.main:app --host 0.0.0.0 --port $PORT
```

2. Push to GitHub and deploy on Railway.app

#### Docker

```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt

COPY prometheus_backend/ ./prometheus_backend/
CMD ["uvicorn", "prometheus_backend.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

Build and run:
```bash
docker build -t prometheus-backend .
docker run -p 8000:8000 prometheus-backend
```

## Exercise-Specific Metrics

### Squat Analysis
- **Depth**: Hip crease below knee joint
- **Knee tracking**: Knees aligned with toes
- **Bar path**: Vertical line over mid-foot
- **Angles**: Knee 80-130°, Hip 70-120°

### Deadlift Analysis
- **Bar path**: Vertical, close to shins/thighs
- **Hip hinge**: Proper hip/knee ratio
- **Back angle**: Neutral spine throughout
- **Angles**: Hip 90-180°, Knee 160-180°

### Bench Press Analysis
- **Bar path**: Slight arc from shoulders to chest
- **Elbow angle**: 75-90° at bottom
- **Shoulder safety**: 45-75° abduction
- **Wrist alignment**: Stacked over elbows

## Storage Management

Videos and analysis data accumulate in `uploads/` and `outputs/` directories. Set up automatic cleanup:

```python
# Add to main.py for automatic cleanup after 24 hours
import time
from apscheduler.schedulers.background import BackgroundScheduler

def cleanup_old_files():
    current_time = time.time()
    for directory in [UPLOAD_DIR, OUTPUT_DIR]:
        for item in directory.iterdir():
            if current_time - item.stat().st_mtime > 86400:  # 24 hours
                if item.is_file():
                    item.unlink()
                else:
                    shutil.rmtree(item)

scheduler = BackgroundScheduler()
scheduler.add_job(cleanup_old_files, 'interval', hours=6)
scheduler.start()
```

## Troubleshooting

### MediaPipe not found
```bash
pip install --upgrade mediapipe
```

### FFmpeg errors
Video normalization requires FFmpeg:
```bash
# macOS
brew install ffmpeg

# Ubuntu/Debian
sudo apt-get install ffmpeg

# Windows
# Download from https://ffmpeg.org/download.html
```

### Port already in use
```bash
# Kill process on port 8000
lsof -ti:8000 | xargs kill -9
```

### CUDA/GPU errors
MediaPipe works fine on CPU. GPU is not required for pose estimation.

## Performance Optimization

- **Video preprocessing**: Resize large videos before analysis
- **Batch processing**: Queue multiple videos
- **Caching**: Store common analysis results
- **Worker processes**: Use Gunicorn with multiple workers

## Security Considerations

- **File size limits**: Add max upload size (e.g., 100MB)
- **File validation**: Check video format and content
- **Rate limiting**: Prevent API abuse
- **Authentication**: Add API keys for production
- **HTTPS**: Use SSL certificates in production

## Future Enhancements

- [ ] Real-time webcam analysis
- [ ] YOLO weight plate detection
- [ ] Machine learning form scoring
- [ ] Form comparison with ideal technique
- [ ] Progressive overload tracking
- [ ] Injury risk assessment

## Support

For MediaPipe-specific issues, see: https://google.github.io/mediapipe/

For Prometheus integration issues, check the main app documentation.
