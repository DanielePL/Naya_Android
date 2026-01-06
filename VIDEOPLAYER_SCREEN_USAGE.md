# 📹 VideoPlayerScreen Usage Guide

## Changes Made

### ✅ Button Updates
1. **"Back" Button → "Save to Set" Button**
   - Primary orange button
   - Icon: Save
   - Text: "Save to Set"
   - Shows loading spinner während save

2. **"New Analysis" Button**
   - Secondary outlined button
   - Beide Buttons disabled während save operation

---

## 🔧 How to Use

### **Basic Usage**

```kotlin
@Composable
fun MyScreen() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // State
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var currentSetId by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    // TODO: Get from DI/ViewModel
    // val videoStorageManager: VideoStorageManager
    // val formAnalysisIntegration: FormAnalysisIntegration
    // val supabase: SupabaseClient

    // Navigate to VideoPlayerScreen
    if (videoUri != null) {
        VideoPlayerScreen(
            videoUri = videoUri!!,
            onNavigateBack = {
                navController.popBackStack()
            },
            onAnalyzeAnother = {
                // Reset and allow new video recording
                videoUri = null
                // Navigate back to camera/recording screen
                navController.navigate("camera")
            },
            onSaveToSet = {
                scope.launch {
                    // STEP 1: Get analysis result from ViewModel
                    val analysisResult = viewModel.analysisResult.value

                    // STEP 2: Save video to storage (device/cloud)
                    val userId = "current-user-id"
                    val setId = currentSetId ?: "current-set-id"

                    val hasCoach = videoStorageManager.checkUserHasCoach(userId)

                    val uploadResult = videoStorageManager.saveVideo(
                        videoUri = videoUri!!,
                        userId = userId,
                        setId = setId,
                        hasActiveCoach = hasCoach
                    )

                    if (uploadResult.success) {
                        // STEP 3: Save metrics to database
                        val videoUrl = uploadResult.videoUrl ?: uploadResult.localPath

                        supabase.from("workout_sets").update(
                            mapOf(
                                "video_url" to videoUrl,
                                "video_storage_type" to if (hasCoach) "cloud" else "device",
                                "video_uploaded_at" to Clock.System.now().toString(),
                                "velocity_metrics" to analysisResult?.vbt_metrics
                            )
                        ) {
                            filter { eq("id", setId) }
                        }

                        // Success!
                        showSuccess = true
                        delay(1000)
                        navController.popBackStack()
                    } else {
                        // Show error
                        Log.e("Save", "Failed: ${uploadResult.error}")
                    }
                }
            }
        )
    }

    // Success Dialog
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            title = { Text("✅ Saved to Set!") },
            text = { Text("Video and velocity metrics saved successfully.") },
            confirmButton = {
                TextButton(onClick = { showSuccess = false }) {
                    Text("OK")
                }
            }
        )
    }
}
```

---

## 🎯 Complete Integration Example

### **With FormAnalysisIntegration**

```kotlin
@Composable
fun VideoPlayerScreenWithIntegration(
    videoUri: Uri,
    userId: String,
    setId: String,
    exerciseId: String,
    exerciseName: String,
    navController: NavController,
    viewModel: FormAnalysisViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var saveError by remember { mutableStateOf<String?>(null) }

    // TODO: Inject via DI
    // val supabase = remember { SupabaseClient(...) }
    // val videoStorageManager = remember { VideoStorageManager(context, supabase) }
    // val integration = remember { FormAnalysisIntegration(context, supabase, videoStorageManager) }

    VideoPlayerScreen(
        videoUri = videoUri,
        onNavigateBack = {
            navController.popBackStack()
        },
        onAnalyzeAnother = {
            // Clear current analysis
            viewModel.clearAnalysis()
            // Navigate back to camera
            navController.navigate("camera") {
                popUpTo("workout_session") { inclusive = false }
            }
        },
        onSaveToSet = {
            scope.launch {
                try {
                    // Use FormAnalysisIntegration for complete workflow
                    val result = integration.analyzeAndSaveToSet(
                        videoUri = videoUri,
                        userId = userId,
                        setId = setId,
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        apiEndpoint = "http://192.168.1.34:8000/api/v1/analyze-form"
                    )

                    if (result.success) {
                        // Show success and navigate back
                        Toast.makeText(
                            context,
                            "✅ Video and metrics saved to set!",
                            Toast.LENGTH_SHORT
                        ).show()

                        delay(500)
                        navController.popBackStack()
                    } else {
                        saveError = result.error
                    }
                } catch (e: Exception) {
                    saveError = e.message
                }
            }
        },
        viewModel = viewModel
    )

    // Error Dialog
    if (saveError != null) {
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text("❌ Save Failed") },
            text = { Text(saveError ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { saveError = null }) {
                    Text("OK")
                }
            }
        )
    }
}
```

---

## 🔄 Navigation Setup

### **In NavHost**

```kotlin
composable("video_player/{videoUri}/{setId}") { backStackEntry ->
    val videoUriString = backStackEntry.arguments?.getString("videoUri")
    val setId = backStackEntry.arguments?.getString("setId")
    val videoUri = Uri.parse(videoUriString)

    VideoPlayerScreenWithIntegration(
        videoUri = videoUri,
        userId = currentUserId,
        setId = setId ?: "",
        exerciseId = currentExerciseId,
        exerciseName = currentExerciseName,
        navController = navController,
        viewModel = viewModel()
    )
}
```

---

## 📊 Data Flow

```
1. User finishes workout set
   ↓
2. Records video
   ↓
3. Video sent to backend for analysis
   ↓
4. VideoPlayerScreen shows results
   ↓
5. User clicks "Save to Set"
   ↓
6. onSaveToSet callback triggered:
   - Video uploaded (device/cloud)
   - Metrics saved to database
   - Success notification
   ↓
7. Navigate back to workout session
   ↓
8. Set now shows video thumbnail + metrics
```

---

## ✅ Updated Button Behavior

### **Save to Set Button**
- **State**: `enabled` by default
- **On Click**:
  - Shows loading spinner
  - Disables both buttons
  - Calls `onSaveToSet()`
  - After completion: navigates back
- **Visual**: Orange primary button, full width

### **New Analysis Button**
- **State**: `enabled` by default, disabled during save
- **On Click**: Calls `onAnalyzeAnother()`
- **Visual**: White outlined button, full width

---

## 🚀 Implementation Checklist

- [ ] Update navigation to pass `videoUri`, `setId` parameters
- [ ] Inject `VideoStorageManager` via DI
- [ ] Inject `FormAnalysisIntegration` via DI
- [ ] Implement `onSaveToSet` logic in calling screen
- [ ] Test save to device storage (single user)
- [ ] Test save to cloud storage (coaching user)
- [ ] Test "New Analysis" flow
- [ ] Add success/error notifications

---

## 🎨 Visual Changes

**Before:**
```
[New Analysis] [Back]
```

**After:**
```
[Save to Set]        ← Primary orange button, full width
[New Analysis]       ← Secondary outlined button, full width
```

---

## 💡 Tips

1. **Loading State**: Button shows spinner während save operation
2. **Error Handling**: Zeige AlertDialog bei save errors
3. **Success Feedback**: Toast notification bei erfolg
4. **Navigation**: `popBackStack()` nach erfolgreichem save
5. **Disabled State**: Beide buttons disabled während save
