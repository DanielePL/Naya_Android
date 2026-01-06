// app/src/main/java/com/example/myapplicationtest/viewmodels/WorkoutSessionViewModel.kt

package com.example.menotracker.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.menotracker.debug.DebugLogger

/**
 * Shared ViewModel for active workout session
 * Manages state that survives camera/activity lifecycle changes
 */
class WorkoutSessionViewModel : ViewModel() {

    // ✅ ACTIVE WORKOUT STATE (survives camera Activity lifecycle!)
    private val _activeWorkout = MutableStateFlow<WorkoutTemplate?>(null)
    val activeWorkout: StateFlow<WorkoutTemplate?> = _activeWorkout.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun setActiveWorkout(workout: WorkoutTemplate?) {
        _activeWorkout.value = workout
        android.util.Log.d("WorkoutViewModel", "📋 Active workout set: ${workout?.name}")
    }

    fun setActiveSessionId(sessionId: String?) {
        _activeSessionId.value = sessionId
        android.util.Log.d("WorkoutViewModel", "📋 Active session ID set: $sessionId")
    }

    // Session data
    private val _sessionId = MutableSharedFlow<String>(replay = 1)
    val sessionId = _sessionId.asSharedFlow()

    // ✅ PERSISTENT SET DATA STORAGE (survives recomposition!)
    private val _setDataMap = MutableStateFlow<Map<String, SetData>>(emptyMap())
    val setDataMap: StateFlow<Map<String, SetData>> = _setDataMap.asStateFlow()

    // Video save callback: setId -> (videoPath, velocity, repsDetected, analysisId)
    // ✅ replay = 1 means the last event is cached and delivered to new collectors
    private val _videoSaved = MutableSharedFlow<VideoSaveData>(replay = 1)
    val videoSaved = _videoSaved.asSharedFlow()

    // ✅ Temporary storage for video playback metrics (for VideoPlayerScreen)
    private val _videoPlaybackMetrics = MutableStateFlow<VelocityMetricsData?>(null)
    val videoPlaybackMetrics: StateFlow<VelocityMetricsData?> = _videoPlaybackMetrics.asStateFlow()

    /**
     * Store metrics temporarily for VideoPlayerScreen to display
     * Called when navigating from SetCard to VideoPlayerScreen
     */
    fun setVideoMetricsForPlayback(metrics: VelocityMetricsData) {
        _videoPlaybackMetrics.value = metrics
        android.util.Log.d("WorkoutViewModel", "📊 Video playback metrics set: peak=${metrics.peakVelocity} m/s, reps=${metrics.totalReps}")
    }

    /**
     * Clear metrics after VideoPlayerScreen is done
     */
    fun clearVideoPlaybackMetrics() {
        _videoPlaybackMetrics.value = null
    }

    suspend fun setSessionId(id: String) {
        _sessionId.emit(id)
    }

    suspend fun emitVideoSaved(data: VideoSaveData) {
        android.util.Log.d("WorkoutViewModel", "videoSaved event: setId=${data.setId}, velocity=${data.vbtVelocity}")

        DebugLogger.logVideoPathUpdate("WorkoutViewModel.emitVideoSaved - BEFORE emit", data.videoPath)

        // ✅ Store in persistent map (survives recomposition!)
        val currentMap = _setDataMap.value.toMutableMap()
        val existingData = currentMap[data.setId]
        currentMap[data.setId] = SetData(
            videoPath = data.videoPath,
            vbtVelocity = data.vbtVelocity,
            vbtRepsDetected = data.vbtRepsDetected,
            velocityMetrics = existingData?.velocityMetrics,  // Preserve VBT metrics
            analysisId = data.analysisId,
            isCompleted = existingData?.isCompleted ?: false,
            completedReps = existingData?.completedReps,
            completedWeight = existingData?.completedWeight,
            currentReps = existingData?.currentReps ?: "",
            currentWeight = existingData?.currentWeight ?: ""
        )
        _setDataMap.value = currentMap
        android.util.Log.d("WorkoutViewModel", "Stored in setDataMap, total entries: ${currentMap.size}")

        // Emit event for immediate UI update
        _videoSaved.emit(data)

        DebugLogger.logVideoPathUpdate("WorkoutViewModel.emitVideoSaved - AFTER emit", data.videoPath)
    }

    // ✅ Update set completion status
    fun updateSetCompletion(setId: String, isCompleted: Boolean, reps: Int?, weight: Double?, isFailed: Boolean = false) {
        val currentMap = _setDataMap.value.toMutableMap()
        val existingData = currentMap[setId]
        currentMap[setId] = SetData(
            videoPath = existingData?.videoPath,
            vbtVelocity = existingData?.vbtVelocity,
            vbtRepsDetected = existingData?.vbtRepsDetected,
            velocityMetrics = existingData?.velocityMetrics,  // Preserve VBT metrics
            analysisId = existingData?.analysisId,
            isCompleted = isCompleted,
            isFailed = isFailed,
            completedReps = reps,
            completedWeight = weight,
            currentReps = existingData?.currentReps ?: "",
            currentWeight = existingData?.currentWeight ?: ""
        )
        _setDataMap.value = currentMap
    }

    // ✅ Mark set as failed (long-press action)
    fun markSetAsFailed(setId: String, reps: Int?, weight: Double?) {
        updateSetCompletion(setId, isCompleted = true, reps = reps, weight = weight, isFailed = true)
    }

    // ✅ Update current input values (work-in-progress)
    fun updateCurrentInputs(setId: String, reps: String, weight: String) {
        val currentMap = _setDataMap.value.toMutableMap()
        val existingData = currentMap[setId]
        currentMap[setId] = SetData(
            videoPath = existingData?.videoPath,
            vbtVelocity = existingData?.vbtVelocity,
            vbtRepsDetected = existingData?.vbtRepsDetected,
            velocityMetrics = existingData?.velocityMetrics,  // Preserve VBT metrics
            analysisId = existingData?.analysisId,
            isCompleted = existingData?.isCompleted ?: false,
            isFailed = existingData?.isFailed ?: false,
            completedReps = existingData?.completedReps,
            completedWeight = existingData?.completedWeight,
            currentReps = reps,
            currentWeight = weight
        )
        _setDataMap.value = currentMap
    }

    // ✅ Update VBT velocity metrics from backend analysis
    fun updateVelocityMetrics(setId: String, metrics: VelocityMetricsData) {
        val currentMap = _setDataMap.value.toMutableMap()
        val existingData = currentMap[setId]
        currentMap[setId] = SetData(
            videoPath = existingData?.videoPath,
            vbtVelocity = metrics.peakVelocity,  // Also update legacy field
            vbtRepsDetected = metrics.totalReps,
            velocityMetrics = metrics,
            analysisId = existingData?.analysisId,
            isCompleted = existingData?.isCompleted ?: false,
            isFailed = existingData?.isFailed ?: false,
            completedReps = existingData?.completedReps,
            completedWeight = existingData?.completedWeight,
            currentReps = existingData?.currentReps ?: "",
            currentWeight = existingData?.currentWeight ?: ""
        )
        _setDataMap.value = currentMap
    }

    // ✅ Get set data for a specific set
    fun getSetData(setId: String): SetData? {
        return _setDataMap.value[setId]
    }

    // ✅ Clear all set data (e.g., when workout ends)
    fun clearSetData() {
        _setDataMap.value = emptyMap()
    }

    // ✅ Load ALL set data from database into ViewModel (video URL + optional VBT metrics)
    // This MUST be called when loading workout to restore persistent video URLs!
    fun loadSetDataFromDTO(setId: String, dto: com.example.menotracker.data.SetDataDTO) {
        val currentMap = _setDataMap.value.toMutableMap()
        val existingData = currentMap[setId]

        // ALWAYS load video URL if present (even without VBT metrics)
        val videoUrl = dto.videoUrl ?: existingData?.videoPath

        currentMap[setId] = SetData(
            videoPath = videoUrl,
            vbtVelocity = dto.velocityMetrics?.peakVelocity ?: existingData?.vbtVelocity,
            vbtRepsDetected = dto.velocityMetrics?.totalReps ?: existingData?.vbtRepsDetected,
            velocityMetrics = dto.velocityMetrics ?: existingData?.velocityMetrics,
            analysisId = existingData?.analysisId,
            isCompleted = existingData?.isCompleted ?: (dto.completedAt != null),
            isFailed = existingData?.isFailed ?: dto.isFailed,
            completedReps = existingData?.completedReps ?: dto.actualReps,
            completedWeight = existingData?.completedWeight ?: dto.actualWeightKg,
            currentReps = existingData?.currentReps ?: dto.currentRepsInput ?: "",
            currentWeight = existingData?.currentWeight ?: dto.currentWeightInput ?: ""
        )
        _setDataMap.value = currentMap

        if (videoUrl != null) {
            android.util.Log.d("WorkoutViewModel", "📹 Loaded video URL for $setId: ${videoUrl.take(60)}...")
        }
        if (dto.velocityMetrics != null) {
            android.util.Log.d("WorkoutViewModel", "📊 Loaded VBT metrics for $setId: peak=${dto.velocityMetrics.peakVelocity}m/s")
        }
    }

    // ✅ Load velocity metrics from database into ViewModel (legacy - use loadSetDataFromDTO instead)
    @Deprecated("Use loadSetDataFromDTO instead", ReplaceWith("loadSetDataFromDTO(setId, dto)"))
    fun loadVelocityMetricsFromDTO(setId: String, dto: com.example.menotracker.data.SetDataDTO) {
        loadSetDataFromDTO(setId, dto)
    }
}

// ✅ Data for video save events (emitted immediately)
data class VideoSaveData(
    val setId: String,
    val videoPath: String,
    val vbtVelocity: Float,
    val vbtRepsDetected: Int,
    val analysisId: String? = null
)

// ✅ VBT Metrics from backend YOLO analysis
data class VelocityMetricsData(
    val source: String = "yolo_backend",  // "yolo_backend" or "on_device"
    val totalReps: Int = 0,
    val avgVelocity: Float = 0f,
    val peakVelocity: Float = 0f,
    val velocityDrop: Float = 0f,
    val techniqueScore: Float = 0f,
    val totalTUT: Float = 0f,           // Total time under tension (seconds)
    val estimatedOneRM: Float? = null,
    val loadPercent: Float? = null,
    val fatigueIndex: Float = 0f,
    val overallGrade: String = "N/A"    // A, B, C, D, F
)

// ✅ Set Type für verschiedene Set-Modi
enum class SetType {
    STANDARD,       // Normales Arbeitsset
    WARMUP,         // Aufwärmsatz
    AMRAP,          // As Many Reps As Possible (für ILB Tests)
    TOP_SET,        // Schwerstes Set des Tages
    BACK_OFF        // Leichtere Sets nach Top Set
}

// ✅ Complete set data (stored persistently in ViewModel)
data class SetData(
    val videoPath: String? = null,
    val vbtVelocity: Float? = null,
    val vbtRepsDetected: Int? = null,
    val velocityMetrics: VelocityMetricsData? = null,  // Full backend VBT metrics
    val analysisId: String? = null,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,  // ✅ NEW: Track failed attempts (e.g., missed snatch, failed max)
    val completedReps: Int? = null,
    val completedWeight: Double? = null,
    // ✅ Current input values (work-in-progress)
    val currentReps: String = "",
    val currentWeight: String = "",
    // ✅ ILB: Set type for AMRAP tests
    val setType: SetType = SetType.STANDARD,
    // ✅ ILB: Calculated 1RM from this set (if AMRAP)
    val estimatedOneRM: Float? = null
)
