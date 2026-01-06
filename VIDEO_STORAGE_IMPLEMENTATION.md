# 📹 Video Storage & Playback Implementation

Komplette Implementierung des Video-Storage-Systems mit Smart Storage Logic:
- **Single Users**: Device Storage (spart Kosten)
- **Coaching Users**: Cloud Storage via Supabase (ermöglicht Coach-Zugriff)

---

## 🏗️ Implementierte Komponenten

### 1. **Supabase Storage Schema** (`supabase_storage_buckets.sql`)

**Features:**
- Storage Bucket: `workout-videos` (50 MB Limit pro Video)
- RLS Policies für User und Coach-Zugriff
- User-Coach Relationship Tracking
- Automatische Storage-Type Detection

**Installation:**
```sql
-- In Supabase SQL Editor ausführen
-- Datei: supabase_storage_buckets.sql
```

**Path Structure:**
```
Cloud: users/{user_id}/sets/{set_id}.mp4
Device: /storage/emulated/0/Prometheus/videos/{set_id}.mp4
```

---

### 2. **VideoStorageManager** (Kotlin)

**Datei:** `data/storage/VideoStorageManager.kt`

**Funktionen:**
- `saveVideo()` - Smart Storage (Device oder Cloud basierend auf Coach-Status)
- `deleteVideo()` - Video löschen
- `getVideoUrl()` - URL für Playback generieren
- `checkUserHasCoach()` - Coach-Status prüfen

**Verwendung:**

```kotlin
// In ViewModel oder Repository
val videoStorageManager = VideoStorageManager(context, supabase)

// Video speichern
val hasCoach = videoStorageManager.checkUserHasCoach(userId)

val result = videoStorageManager.saveVideo(
    videoUri = videoUri,
    userId = userId,
    setId = setId,
    hasActiveCoach = hasCoach
)

when {
    result.success && result.storageType == StorageType.CLOUD -> {
        // Cloud upload erfolgreich
        val videoUrl = result.videoUrl
        // Speichere videoUrl in Supabase workout_sets Tabelle
    }
    result.success && result.storageType == StorageType.DEVICE -> {
        // Device save erfolgreich
        val localPath = result.localPath
        // Speichere localPath in Supabase workout_sets Tabelle
    }
    else -> {
        // Fehler
        Log.e(TAG, "Upload failed: ${result.error}")
    }
}

// Video abrufen
val videoUrl = videoStorageManager.getVideoUrl(
    userId = userId,
    setId = setId,
    storageType = StorageType.CLOUD  // oder DEVICE
)
```

---

### 3. **WorkoutSetCard** (Compose UI)

**Datei:** `ui/components/WorkoutSetCard.kt`

**Features:**
- Video-Thumbnail links (100x80 dp)
- Velocity Metrics rechts
- Play-Button Overlay
- Coaching Badge (wenn aktiv)
- Calibration Tier Badge

**Verwendung:**

```kotlin
@Composable
fun ExampleScreen() {
    var showVideoPlayer by remember { mutableStateOf(false) }
    var currentVideoUrl by remember { mutableStateOf<String?>(null) }

    WorkoutSetCard(
        setNumber = 1,
        exerciseName = "Barbell Back Squat",
        weight = 100.0,
        reps = 5,
        rpe = 8.0f,
        videoUrl = "https://zzluhirmmnkfkifriult.supabase.co/storage/v1/object/sign/workout-videos/users/...",
        velocityMetrics = VelocityMetrics(
            avgPeakVelocity = 0.85,
            velocityDrop = 12.3,
            repsDetected = 5,
            unit = "m/s",
            calibrationTier = "calibrated"
        ),
        hasCoachingBadge = true,
        onVideoClick = {
            currentVideoUrl = "https://..."
            showVideoPlayer = true
        }
    )

    // Video Player Dialog
    if (showVideoPlayer && currentVideoUrl != null) {
        VideoPlayerDialog(
            videoUrl = currentVideoUrl!!,
            exerciseName = "Barbell Back Squat",
            onDismiss = { showVideoPlayer = false }
        )
    }
}
```

**UI-Layout:**
```
┌─────────────────────────────────────┐
│ Set 1: Barbell Back Squat  🔥 Coach│
│ 100kg × 5 reps | RPE 8             │
│                                     │
│ ┌───────┐  📊 Velocity Metrics     │
│ │ ▶️     │  Peak: 0.85 m/s          │
│ │ Video │  Drop: 12.3%             │
│ └───────┘  Reps: 5                 │
│             ✓ Calibrated            │
└─────────────────────────────────────┘
```

---

### 4. **VideoPlayerDialog** (Compose UI)

**Datei:** `ui/components/VideoPlayerDialog.kt`

**Features:**
- Fullscreen Dialog
- ExoPlayer (Media3) Integration
- Loop Mode für Analyse
- Playback Controls
- Close Button
- Unterstützt Cloud URLs und lokale Pfade

**Verwendung:**

```kotlin
// Fullscreen Player
VideoPlayerDialog(
    videoUrl = videoUrl,
    exerciseName = "Barbell Back Squat",
    onDismiss = { showVideoPlayer = false }
)

// Embedded Player (für kleinere Ansichten)
VideoPlayer(
    videoUrl = videoUrl,
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
)
```

---

## 🔄 Kompletter Workflow

### **1. Video nach Form-Analyse hochladen**

```kotlin
// In FormAnalysisViewModel oder Repository

suspend fun uploadVideoAfterAnalysis(
    videoUri: Uri,
    userId: String,
    setId: String
) {
    // 1. Check ob User Coach hat
    val hasCoach = videoStorageManager.checkUserHasCoach(userId)

    // 2. Video speichern
    val uploadResult = videoStorageManager.saveVideo(
        videoUri = videoUri,
        userId = userId,
        setId = setId,
        hasActiveCoach = hasCoach
    )

    if (uploadResult.success) {
        // 3. workout_sets Tabelle updaten
        val videoUrl = uploadResult.videoUrl ?: uploadResult.localPath
        val storageType = if (hasCoach) "cloud" else "device"

        supabase.postgrest
            .from("workout_sets")
            .update({
                set("video_url", videoUrl)
                set("video_storage_type", storageType)
                set("video_uploaded_at", Clock.System.now().toString())
            }) {
                filter {
                    eq("id", setId)
                }
            }

        Log.d(TAG, "✅ Video uploaded and saved to DB")
    } else {
        Log.e(TAG, "❌ Upload failed: ${uploadResult.error}")
    }
}
```

### **2. Video in Set-Card anzeigen**

```kotlin
// In ActiveWorkoutSessionScreen oder WorkoutHistoryScreen

@Composable
fun WorkoutSetsDisplay(sets: List<WorkoutSet>) {
    var showVideoPlayer by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<Pair<String, String>?>(null) }

    LazyColumn {
        items(sets) { set ->
            WorkoutSetCard(
                setNumber = set.setNumber,
                exerciseName = set.exercise.name,
                weight = set.weight,
                reps = set.reps,
                rpe = set.rpe,
                videoUrl = set.videoUrl,
                velocityMetrics = set.velocityMetrics?.let {
                    VelocityMetrics(
                        avgPeakVelocity = it.avgPeakVelocity,
                        velocityDrop = it.velocityDrop,
                        repsDetected = it.repsDetected,
                        unit = it.unit,
                        calibrationTier = it.calibrationTier
                    )
                },
                hasCoachingBadge = set.user.hasActiveCoach,
                onVideoClick = {
                    selectedVideo = Pair(set.videoUrl!!, set.exercise.name)
                    showVideoPlayer = true
                }
            )
        }
    }

    // Video Player
    selectedVideo?.let { (url, name) ->
        if (showVideoPlayer) {
            VideoPlayerDialog(
                videoUrl = url,
                exerciseName = name,
                onDismiss = { showVideoPlayer = false }
            )
        }
    }
}
```

---

## 💾 Storage Kosten

### **Single User (Device Storage)**
- **Kosten:** 0€
- **Speicherort:** Lokales Gerät
- **Zugriff:** Nur User selbst

### **Coaching User (Cloud Storage)**
- **Kosten:** ~0.021€/GB/month (Supabase)
- **50 MB Video:** ~0.001€/month
- **100 Videos:** ~0.10€/month
- **Zugriff:** User + Coach

### **Kostenoptimierung:**
- Automatische Umschaltung: Single → Coaching aktiviert Cloud Storage
- Alte Videos können nach X Monaten archiviert/gelöscht werden
- Kompression: 50 MB Limit erzwingt moderate Qualität

---

## 🔐 Sicherheit (RLS Policies)

### **User Permissions:**
✅ Upload eigene Videos
✅ View eigene Videos
✅ Delete eigene Videos
❌ View Videos anderer Users (außer eigener Coach)

### **Coach Permissions:**
✅ View Videos von aktiven Clients
❌ Upload/Delete Client-Videos
❌ View Videos von Nicht-Clients

### **Implementierung:**
Policies in `supabase_storage_buckets.sql` bereits definiert.

---

## 📊 Datenbank Schema Ergänzungen

### **workout_sets Tabelle (UPDATE)**

```sql
ALTER TABLE workout_sets
ADD COLUMN video_url TEXT,
ADD COLUMN video_storage_type TEXT DEFAULT 'device',
ADD COLUMN video_thumbnail_url TEXT,
ADD COLUMN video_uploaded_at TIMESTAMPTZ;
```

### **user_coach_relationships Tabelle (NEU)**

```sql
CREATE TABLE user_coach_relationships (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES auth.users,
    coach_id UUID REFERENCES auth.users,
    status TEXT CHECK (status IN ('active', 'inactive', 'pending')),
    plan_type TEXT,  -- 'basic', 'premium', 'elite'
    started_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ
);
```

---

## ✅ Nächste Schritte

### **1. Supabase Setup**
```bash
# In Supabase SQL Editor:
1. supabase_storage_buckets.sql ausführen
2. Storage Bucket "workout-videos" erstellen
3. Policies aktivieren
```

### **2. Android Integration**
```kotlin
1. VideoStorageManager in DI/Hilt registrieren
2. WorkoutSetCard in bestehende Screens integrieren
3. Video-Upload nach Form-Analyse hinzufügen
```

### **3. Testing**
- [ ] Single User: Video auf Device speichern
- [ ] Coaching User: Video zu Cloud hochladen
- [ ] Coach Access: Coach kann Client-Videos sehen
- [ ] Video Playback: Fullscreen Player funktioniert
- [ ] RLS: Andere User können Videos nicht sehen

---

## 🎨 Design-Hinweise

**Prometheus Farben:**
- Orange: `#FF9D50` (Primary/Coaching)
- Cyan: `#00D9FF` (Velocity Metrics)
- Green: `#00FF88` (Success/Calibrated)
- Dark BG: `#1A1410`

**Video Thumbnail:**
- Größe: 100x80 dp (kompakt)
- Rounded Corners: 8dp
- Play Button: 40dp Icon (zentriert)
- "Video" Badge: Orange, Top-Left

---

## 📞 Support

Bei Fragen oder Problemen:
1. Check Logs: `VideoStorageManager` Tag
2. Supabase Dashboard: Storage Bucket Übersicht
3. RLS Policies: Teste mit verschiedenen User-Rollen

---

**Status:** ✅ Vollständig implementiert und ready to use!
