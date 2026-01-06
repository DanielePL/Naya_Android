# 🎬 Form Analysis → Set Integration Flow

Kompletter Workflow: Video aufnehmen → Analysieren → Im Set speichern

---

## 📱 User Journey

### **Schritt 1: Video während Set aufnehmen**

```kotlin
// In ActiveWorkoutSessionScreen
var recordedVideoUri by remember { mutableStateOf<Uri?>(null) }

// Button: "Record Form Video"
Button(onClick = {
    // Launch camera to record video
    recordedVideoUri = cameraLauncher.launch()
}) {
    Text("Record Form Video")
}
```

### **Schritt 2: "Analyze Form" Button erscheint**

Nach Video-Aufnahme wird `FormAnalysisButton` sichtbar:

```kotlin
if (recordedVideoUri != null) {
    FormAnalysisButton(
        videoUri = recordedVideoUri,
        userId = currentUserId,
        setId = currentSetId,
        exerciseId = "squat-back-barbell",
        exerciseName = "Barbell Back Squat",
        onAnalysisComplete = { result ->
            // Show success message
            // Refresh set display with velocity metrics
        }
    )
}
```

### **Schritt 3: User klickt "Analyze Form & Save to Set"**

Button startet den kompletten Workflow:

```kotlin
val integration = FormAnalysisIntegration(context, supabase, videoStorageManager)

val result = integration.analyzeAndSaveToSet(
    videoUri = recordedVideoUri,
    userId = currentUserId,
    setId = currentSetId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    apiEndpoint = "http://192.168.1.34:8000/api/v1/analyze-form"
)

if (result.success) {
    // ✅ Video analyzed
    // ✅ Video uploaded (device/cloud)
    // ✅ Metrics saved to DB
    showSuccessDialog(result)
}
```

---

## 🔄 Backend Flow (FormAnalysisIntegration)

### **STEP 1: Backend Analysis**

```kotlin
// Send video to Python backend
POST http://192.168.1.34:8000/api/v1/analyze-form

FormData:
- video: <video_file>
- user_id: "uuid"
- set_id: "uuid"
- exercise_id: "squat-back-barbell"
- exercise_name: "Barbell Back Squat"

Response:
{
  "velocity_metrics": {
    "reps_detected": 5,
    "summary": {
      "avg_peak_velocity": 0.85,
      "velocity_drop": 12.3,
      "unit": "m/s"
    }
  },
  "calibration": {
    "tier": "relative",
    "method": "Relative Speed Index",
    "confidence": 0.8
  }
}
```

### **STEP 2: Check Coach Status**

```kotlin
val hasCoach = videoStorageManager.checkUserHasCoach(userId)

// Query: user_coach_relationships
// If active coach exists → Cloud Storage
// If no coach → Device Storage
```

### **STEP 3: Upload Video**

```kotlin
val uploadResult = videoStorageManager.saveVideo(
    videoUri = videoUri,
    userId = userId,
    setId = setId,
    hasActiveCoach = hasCoach
)

// DEVICE STORAGE:
// Path: /storage/emulated/0/Prometheus/videos/{setId}.mp4
// Cost: 0€

// CLOUD STORAGE:
// Upload to Supabase Storage: users/{userId}/sets/{setId}.mp4
// Cost: ~0.001€/video/month
// Returns signed URL for playback
```

### **STEP 4: Save to Database**

```kotlin
supabase.from("workout_sets").update(
    mapOf(
        "video_url" to videoUrl,
        "video_storage_type" to "cloud", // or "device"
        "video_uploaded_at" to now(),
        "velocity_metrics" to {
            "reps_detected": 5,
            "avg_peak_velocity": 0.85,
            "velocity_drop": 12.3,
            "unit": "m/s",
            "calibration_tier": "relative",
            "calibration_method": "Relative Speed Index",
            "calibration_confidence": 0.8
        }
    )
) {
    filter { eq("id", setId) }
}
```

---

## 🎨 UI Updates After Analysis

### **Set Card mit Video + Metrics**

```kotlin
WorkoutSetCard(
    setNumber = 1,
    exerciseName = "Barbell Back Squat",
    weight = 100.0,
    reps = 5,
    rpe = 8.0f,

    // ✅ Video verfügbar
    videoUrl = "https://zzluhirmmnkfkifriult.supabase.co/storage/...",

    // ✅ Velocity Metrics verfügbar
    velocityMetrics = VelocityMetrics(
        avgPeakVelocity = 0.85,
        velocityDrop = 12.3,
        repsDetected = 5,
        unit = "m/s",
        calibrationTier = "relative"
    ),

    hasCoachingBadge = hasCoach,

    onVideoClick = {
        // Open fullscreen video player
        showVideoPlayer = true
    }
)
```

### **Visual Result:**

```
┌─────────────────────────────────────┐
│ Set 1: Barbell Back Squat  🔥 Coach│
│ 100kg × 5 reps | RPE 8             │
│                                     │
│ ┌───────┐  📊 Velocity Metrics     │
│ │ ▶️     │  Peak: 0.85 m/s          │
│ │ Video │  Drop: 12.3%             │
│ └───────┘  Reps: 5                 │
│                                     │
│ (Tap video to play fullscreen)     │
└─────────────────────────────────────┘
```

---

## 💾 Datenbank Schema

### **workout_sets Tabelle**

```sql
CREATE TABLE workout_sets (
    id UUID PRIMARY KEY,
    session_id UUID REFERENCES workout_sessions,
    exercise_id TEXT,
    set_number INTEGER,

    -- Performance data
    reps INTEGER,
    weight_kg NUMERIC,
    rpe NUMERIC,

    -- ✅ Video storage
    video_url TEXT,
    video_storage_type TEXT CHECK (video_storage_type IN ('device', 'cloud')),
    video_uploaded_at TIMESTAMPTZ,

    -- ✅ Velocity metrics (JSONB)
    velocity_metrics JSONB
);
```

### **velocity_metrics JSONB Format**

```json
{
  "reps_detected": 5,
  "avg_peak_velocity": 0.85,
  "velocity_drop": 12.3,
  "unit": "m/s",
  "calibration_tier": "relative",
  "calibration_method": "Relative Speed Index",
  "calibration_confidence": 0.8,

  "rep_data": [
    {
      "rep_number": 1,
      "peak_velocity": 0.90,
      "avg_velocity": 0.75,
      "duration_s": 2.1
    },
    {
      "rep_number": 2,
      "peak_velocity": 0.88,
      "avg_velocity": 0.73,
      "duration_s": 2.3
    }
  ]
}
```

---

## 📊 Queries

### **Get Sets mit Videos**

```kotlin
val setsWithVideos = supabase.from("workout_sets")
    .select()
    .neq("video_url", null)
    .decodeList<WorkoutSet>()
```

### **Get Velocity Drop für Fatigue Analysis**

```sql
SELECT
    exercise_id,
    CAST(velocity_metrics->>'velocity_drop' AS NUMERIC) as velocity_drop
FROM workout_sets
WHERE velocity_metrics IS NOT NULL
  AND CAST(velocity_metrics->>'velocity_drop' AS NUMERIC) > 20.0
ORDER BY velocity_drop DESC;
```

---

## 🔧 Implementation Checklist

### **Backend (bereits erledigt ✅)**
- [x] Form Analysis API (`main.py`)
- [x] Supabase Integration im Backend
- [x] Velocity Metrics Berechnung

### **Android (zu implementieren)**
- [ ] `FormAnalysisIntegration` in DI registrieren (Hilt/Dagger)
- [ ] Actual API call implementieren (Retrofit/OkHttp)
- [ ] `FormAnalysisButton` in `ActiveWorkoutSessionScreen` integrieren
- [ ] Camera Integration für Video-Aufnahme
- [ ] Success Dialog mit Metrics-Anzeige
- [ ] `WorkoutSetCard` in Screens verwenden

### **Database (zu implementieren)**
- [ ] SQL in Supabase ausführen: `supabase_workout_sets_video_update.sql`
- [ ] Test: Set mit Video speichern
- [ ] Test: Velocity Metrics abrufen

---

## 🎯 User Experience

**Optimaler Flow:**

1. User macht Set (Squat, 100kg x 5 reps)
2. User tippt "Record Form Video"
3. Camera öffnet sich, User nimmt Set auf
4. Nach Aufnahme erscheint "Analyze Form & Save to Set" Button
5. User tippt Button → Loading Spinner (2-5 Sekunden)
6. Success Dialog:
   ```
   ✅ Analysis Complete!

   • Reps Detected: 5
   • Peak Velocity: 0.85 m/s
   • Velocity Drop: 12.3%
   • Calibration: Relative Speed Index

   Video saved to set!
   ```
7. Set-Card zeigt jetzt Video-Thumbnail + Metrics
8. User kann Video antippen → Fullscreen Player

**Vorteile:**
- Kein manuelles Speichern nötig
- Metrics automatisch zum Set hinzugefügt
- Video direkt beim Set verfügbar
- Coach (falls vorhanden) kann Videos sehen

---

## 🚀 Next Steps

1. **Implement API Call** in `FormAnalysisIntegration.kt`
   - Verwende Retrofit oder OkHttp
   - Multipart FormData für Video-Upload

2. **Camera Integration**
   - CameraX für Video-Aufnahme
   - Temporäre Datei speichern
   - URI an FormAnalysisButton übergeben

3. **DI Setup**
   - `VideoStorageManager` in Hilt Module
   - `FormAnalysisIntegration` als Singleton

4. **Testing**
   - Unit Tests für Upload Logic
   - Integration Tests für kompletten Flow
   - UI Tests für Button States

---

## 📝 Notizen

- **Storage Decision**: Automatisch basierend auf Coach-Status
- **Error Handling**: Zeige klare Fehlermeldungen bei Upload/Analysis Fehler
- **Progress Indication**: Loading Spinner während Analyse
- **Offline Support**: Video lokal speichern, später hochladen wenn wieder online
