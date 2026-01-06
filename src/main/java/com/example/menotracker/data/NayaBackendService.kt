package com.example.menotracker.data

import com.example.menotracker.config.BackendConfig
import com.example.menotracker.data.models.*
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * NAYA AI Coach Backend Service
 * Verbindet mit dem Python FastAPI Backend für AI Coaching
 */

// Request Model
data class CoachingRequest(
    val exercise: String,
    val context: String? = null
)

// Response Model
data class CoachingResponse(
    val exercise: String,
    val cues: String
)

// Health Check Response
data class BackendHealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val ollama: OllamaStatus
)

data class OllamaStatus(
    val status: String,
    val model: String,
    @SerializedName("api_url") val apiUrl: String
)

// ============================================================
// VBT CLOUD PROCESSING DATA CLASSES
// ============================================================

data class VbtProcessRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("session_id") val sessionId: String,  // Actual session UUID for DB lookup
    @SerializedName("set_id") val setId: String,  // Composite key for file naming
    @SerializedName("video_url") val videoUrl: String,
    @SerializedName("exercise_type") val exerciseType: String = "squat",
    @SerializedName("weight_kg") val weightKg: Float? = null,
    @SerializedName("one_rm") val oneRm: Float? = null
)

data class VbtProcessResponse(
    @SerializedName("task_id") val taskId: String,
    val status: String,
    val message: String
)

data class VbtStatusResponse(
    val status: String,  // "processing", "completed", "failed"
    val progress: Int,
    val message: String? = null,
    val results: VbtResults? = null,
    val error: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("set_id") val setId: String? = null,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null
)

data class VbtResults(
    val success: Boolean,
    @SerializedName("exerciseType") val exerciseType: String? = null,
    @SerializedName("totalReps") val totalReps: Int = 0,
    val reps: List<VbtRepData>? = null,
    val summary: VbtSummary? = null,
    val warnings: List<String>? = null,
    @SerializedName("processedAt") val processedAt: String? = null,
    @SerializedName("annotatedVideoUrl") val annotatedVideoUrl: String? = null
)

data class VbtRepData(
    @SerializedName("repNumber") val repNumber: Int,
    @SerializedName("peakVelocity") val peakVelocity: Float,
    @SerializedName("avgVelocity") val avgVelocity: Float,
    @SerializedName("concentricTime") val concentricTime: Float,
    @SerializedName("eccentricTime") val eccentricTime: Float,
    @SerializedName("totalTime") val totalTime: Float,
    @SerializedName("pathAccuracy") val pathAccuracy: Float,
    @SerializedName("techniqueScore") val techniqueScore: Int,
    val force: Float? = null,
    val power: Float? = null,
    val warnings: List<String>? = null
)

data class VbtSummary(
    @SerializedName("avgVelocity") val avgVelocity: Float,
    @SerializedName("peakVelocity") val peakVelocity: Float,
    @SerializedName("velocityDrop") val velocityDrop: Float,
    @SerializedName("avgTechniqueScore") val avgTechniqueScore: Int,
    @SerializedName("totalTUT") val totalTut: Float,
    @SerializedName("estimatedOneRM") val estimatedOneRm: Float? = null,
    @SerializedName("loadPercent") val loadPercent: Float? = null,
    @SerializedName("fatigueIndex") val fatigueIndex: Float,
    @SerializedName("overallGrade") val overallGrade: String
)

// ============================================================
// WOD SCANNER DATA CLASSES
// ============================================================

data class WodScanRequest(
    @SerializedName("user_id") val userId: String?,
    @SerializedName("image_base64") val imageBase64: String,
    @SerializedName("image_type") val imageType: String = "image/jpeg",
    @SerializedName("box_name") val boxName: String? = null,
    @SerializedName("save_to_database") val saveToDatabase: Boolean = true,
    @SerializedName("is_public") val isPublic: Boolean = false
)

data class WodLogResultRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("score_type") val scoreType: String,
    @SerializedName("rounds_completed") val roundsCompleted: Int? = null,
    @SerializedName("reps_completed") val repsCompleted: Int? = null,
    @SerializedName("time_seconds") val timeSeconds: Int? = null,
    @SerializedName("weight_kg") val weightKg: Double? = null,
    @SerializedName("total_reps") val totalReps: Int? = null,
    @SerializedName("scaling_level") val scalingLevel: String = "rx",
    @SerializedName("completed_within_cap") val completedWithinCap: Boolean = true,
    val notes: String? = null,
    @SerializedName("video_url") val videoUrl: String? = null
)

// ============================================================
// AI EXERCISE CREATOR DATA CLASSES
// ============================================================

data class AICreateExerciseRequest(
    @SerializedName("user_id") val userId: String?,
    @SerializedName("movement_name") val movementName: String,
    val context: String? = null
)

data class AICreateExerciseResponse(
    val success: Boolean,
    val exercise: AICreatedExercise? = null,
    @SerializedName("exercise_id") val exerciseId: String? = null,
    val error: String? = null
)

data class AICreatedExercise(
    val id: String,
    val name: String,
    val category: String?,
    @SerializedName("secondary_muscle_groups") val secondaryMuscleGroups: List<String>?,
    val equipment: List<String>?,
    val level: String?,
    val visibility: String?,
    @SerializedName("owner_id") val ownerId: String?,
    val sports: List<String>?,
    @SerializedName("track_reps") val trackReps: Boolean,
    @SerializedName("track_sets") val trackSets: Boolean,
    @SerializedName("track_weight") val trackWeight: Boolean,
    @SerializedName("track_duration") val trackDuration: Boolean,
    @SerializedName("track_distance") val trackDistance: Boolean,
    val tutorial: String?,
    val notes: String?
)

data class AICreateExercisesBatchRequest(
    @SerializedName("user_id") val userId: String?,
    val movements: List<String>,
    val context: String? = null
)

data class AICreateExercisesBatchResponse(
    val success: Boolean,
    val created: List<AICreatedExercise>,
    val failed: List<FailedExerciseCreation>,
    @SerializedName("created_count") val createdCount: Int,
    @SerializedName("failed_count") val failedCount: Int
)

data class FailedExerciseCreation(
    val name: String,
    val error: String
)

// Retrofit Service Interface
interface NayaBackendService {

    @GET("/")
    suspend fun healthCheck(): BackendHealthResponse

    @POST("/ai-coach")
    suspend fun getCoachingCues(
        @Body request: CoachingRequest
    ): CoachingResponse

    // ============================================================
    // AI COACH - PERSISTENT CHAT ENDPOINTS
    // ============================================================

    /**
     * Send a chat message with conversation history
     */
    @POST("/api/v1/ai-coach/chat")
    suspend fun sendChatMessage(
        @Body request: ChatRequest
    ): ChatResponse

    /**
     * Get all conversations for a user
     */
    @GET("/api/v1/ai-coach/conversations")
    suspend fun getUserConversations(
        @Query("user_id") userId: String
    ): List<Conversation>

    /**
     * Get all messages in a conversation
     */
    @GET("/api/v1/ai-coach/conversations/{conversation_id}/messages")
    suspend fun getConversationMessages(
        @Path("conversation_id") conversationId: String
    ): List<ChatMessage>

    /**
     * Archive a conversation (soft delete)
     */
    @DELETE("/api/v1/ai-coach/conversations/{conversation_id}")
    suspend fun archiveConversation(
        @Path("conversation_id") conversationId: String
    ): Map<String, Any>

    /**
     * Save AI-generated workout to My Workouts
     */
    @POST("/api/v1/ai-coach/save-workout")
    suspend fun saveAIWorkout(
        @Body request: Map<String, Any>
    ): Map<String, Any>

    // ============================================================
    // VBT CLOUD PROCESSING ENDPOINTS
    // ============================================================

    /**
     * Start VBT cloud processing for a video uploaded to Supabase Storage
     */
    @POST("/api/v1/process-vbt-cloud")
    suspend fun processVbtCloud(
        @Body request: VbtProcessRequest
    ): VbtProcessResponse

    /**
     * Check status of VBT processing task
     */
    @GET("/api/v1/vbt-status/{task_id}")
    suspend fun getVbtStatus(
        @Path("task_id") taskId: String
    ): VbtStatusResponse

    // ============================================================
    // CROSSFIT WOD SCANNER ENDPOINTS
    // ============================================================

    /**
     * Scan a CrossFit WOD from whiteboard photo using Vision AI
     */
    @POST("/api/v1/ai-coach/scan-wod")
    suspend fun scanWod(
        @Body request: WodScanRequest
    ): WodScanResponse

    /**
     * Get list of WODs with optional filters
     */
    @GET("/api/v1/ai-coach/wods")
    suspend fun getWods(
        @Query("user_id") userId: String? = null,
        @Query("wod_type") wodType: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("max_duration") maxDuration: Int? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): WodListResponse

    /**
     * Get a single WOD with all movements
     */
    @GET("/api/v1/ai-coach/wods/{wod_id}")
    suspend fun getWodDetail(
        @Path("wod_id") wodId: String
    ): WodWithMovements

    /**
     * Log a user's result/score for a WOD
     */
    @POST("/api/v1/ai-coach/wods/{wod_id}/log-result")
    suspend fun logWodResult(
        @Path("wod_id") wodId: String,
        @Body request: WodLogResultRequest
    ): Map<String, Any>

    /**
     * Get results/scores for a WOD
     */
    @GET("/api/v1/ai-coach/wods/{wod_id}/results")
    suspend fun getWodResults(
        @Path("wod_id") wodId: String,
        @Query("user_id") userId: String? = null,
        @Query("limit") limit: Int = 50
    ): Map<String, Any>

    // ============================================================
    // AI EXERCISE CREATOR ENDPOINTS
    // ============================================================

    /**
     * Create a new exercise using AI from unknown movement name
     */
    @POST("/api/v1/ai-coach/create-exercise")
    suspend fun createExerciseFromAI(
        @Body request: AICreateExerciseRequest
    ): AICreateExerciseResponse

    /**
     * Create multiple exercises from unknown movement names (batch)
     */
    @POST("/api/v1/ai-coach/create-exercises-batch")
    suspend fun createExercisesBatchFromAI(
        @Body request: AICreateExercisesBatchRequest
    ): AICreateExercisesBatchResponse

    companion object {
        // Timeout Configuration (Backend kann bis zu 90s+ brauchen für VBT Analyse)
        private const val CONNECT_TIMEOUT = 15L  // Sekunden
        private const val READ_TIMEOUT = 180L    // 3 Minuten für VBT/YOLO Analyse
        private const val WRITE_TIMEOUT = 60L    // Sekunden für Video Upload

        /**
         * Erstellt eine Instanz des NayaBackendService
         * Verwendet BackendConfig.BASE_URL als zentrale Konfiguration
         *
         * @param baseUrl Optional: Custom Base URL (überschreibt BackendConfig)
         * @return NayaBackendService Instanz
         */
        fun create(baseUrl: String = BackendConfig.BASE_URL): NayaBackendService {
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request()
                    android.util.Log.d("NayaBackend", "🌐 Request: ${request.method} ${request.url}")

                    val startTime = System.currentTimeMillis()
                    val response = chain.proceed(request)
                    val elapsed = System.currentTimeMillis() - startTime

                    android.util.Log.d("NayaBackend", "✅ Response: ${response.code} in ${elapsed}ms")

                    if (!response.isSuccessful) {
                        val errorBody = response.peekBody(2048).string()
                        android.util.Log.e("NayaBackend", "❌ Error: $errorBody")
                    }

                    response
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(NayaBackendService::class.java)
        }

    }
}

/**
 * Repository für Backend-Kommunikation
 * Wrapper um den Service mit Error Handling
 */
object NayaBackendRepository {

    private var service: NayaBackendService = NayaBackendService.create()

    /**
     * Konfiguriert das Backend Repository mit einer custom URL
     * Nützlich für physische Geräte
     */
    fun configure(baseUrl: String) {
        service = NayaBackendService.create(baseUrl)
        android.util.Log.d("NayaBackend", "📡 Configured with base URL: $baseUrl")
    }

    /**
     * Prüft ob das Backend erreichbar ist
     *
     * @return Result mit BackendHealthResponse oder Error
     */
    suspend fun checkHealth(): Result<BackendHealthResponse> {
        return try {
            val response = service.healthCheck()
            android.util.Log.d("NayaBackend", "✅ Health check successful: ${response.status}")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ Health check failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Holt Coaching Cues vom Backend
     *
     * @param exercise Name der Übung
     * @param context Optionaler Kontext (z.B. "Beginner with knee pain")
     * @return Result mit CoachingResponse oder Error
     */
    suspend fun getCoachingCues(
        exercise: String,
        context: String? = null
    ): Result<CoachingResponse> {
        return try {
            android.util.Log.d("NayaBackend", "🤖 Requesting cues for: $exercise")

            val request = CoachingRequest(
                exercise = exercise,
                context = context
            )

            val response = service.getCoachingCues(request)

            android.util.Log.d("NayaBackend", "✅ Got cues: ${response.cues.take(50)}...")

            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ Failed to get cues: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Holt Coaching Cues für eine Übung mit zusätzlichem User-Kontext
     * Nutzt User-Profil Informationen für bessere Empfehlungen
     *
     * @param exercise Name der Übung
     * @param userContext User-spezifischer Kontext aus dem Profil
     * @return Result mit CoachingResponse oder Error
     */
    suspend fun getPersonalizedCues(
        exercise: String,
        userContext: String? = null
    ): Result<CoachingResponse> {
        return getCoachingCues(exercise, userContext)
    }

    // ============================================================
    // VBT CLOUD PROCESSING METHODS
    // ============================================================

    /**
     * Start VBT cloud processing for a video in Supabase Storage
     *
     * @param userId User UUID
     * @param sessionId Session UUID for database lookup
     * @param setId Composite set ID for file naming (workoutId_exerciseId_setN)
     * @param videoUrl Signed URL to video in Supabase Storage
     * @param exerciseType Type of exercise (squat, bench, deadlift)
     * @param weightKg Optional weight used (for power calculation)
     * @param oneRm Optional 1RM (for load % calculation)
     * @return Result with task_id for status checking
     */
    suspend fun startVbtCloudProcessing(
        userId: String,
        sessionId: String,
        setId: String,
        videoUrl: String,
        exerciseType: String = "squat",
        weightKg: Float? = null,
        oneRm: Float? = null
    ): Result<VbtProcessResponse> {
        return try {
            android.util.Log.d("NayaBackend", "🎬 Starting VBT cloud processing: session=$sessionId, set=$setId")

            val request = VbtProcessRequest(
                userId = userId,
                sessionId = sessionId,
                setId = setId,
                videoUrl = videoUrl,
                exerciseType = exerciseType,
                weightKg = weightKg,
                oneRm = oneRm
            )

            val response = service.processVbtCloud(request)

            android.util.Log.d("NayaBackend", "✅ VBT processing started: task_id=${response.taskId}")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ VBT processing start failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check status of VBT processing task
     *
     * @param taskId Task ID from startVbtCloudProcessing
     * @return Result with processing status and results
     */
    suspend fun getVbtProcessingStatus(taskId: String): Result<VbtStatusResponse> {
        return try {
            android.util.Log.d("NayaBackend", "📊 Checking VBT status: task=$taskId")

            val response = service.getVbtStatus(taskId)

            android.util.Log.d("NayaBackend", "📊 VBT status: ${response.status} (${response.progress}%)")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ VBT status check failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Poll VBT processing status until complete
     *
     * @param taskId Task ID from startVbtCloudProcessing
     * @param onProgress Callback for progress updates
     * @param intervalMs Polling interval in milliseconds
     * @param timeoutMs Timeout in milliseconds
     * @return Result with final VBT results or error
     */
    suspend fun pollVbtProcessing(
        taskId: String,
        onProgress: ((Int, String?) -> Unit)? = null,
        intervalMs: Long = 2000,
        timeoutMs: Long = 300000  // 5 minutes
    ): Result<VbtResults> {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val statusResult = getVbtProcessingStatus(taskId)

            statusResult.onSuccess { status ->
                onProgress?.invoke(status.progress, status.message)

                when (status.status) {
                    "completed" -> {
                        return if (status.results != null) {
                            Result.success(status.results)
                        } else {
                            Result.failure(Exception("VBT processing completed but no results"))
                        }
                    }
                    "failed" -> {
                        return Result.failure(Exception(status.error ?: "VBT processing failed"))
                    }
                    "processing" -> {
                        // Continue polling
                    }
                }
            }.onFailure { error ->
                return Result.failure(error)
            }

            kotlinx.coroutines.delay(intervalMs)
        }

        return Result.failure(Exception("VBT processing timeout after ${timeoutMs / 1000}s"))
    }

    // ============================================================
    // WOD SCANNER METHODS
    // ============================================================

    /**
     * Scan a CrossFit WOD from a whiteboard photo
     *
     * @param imageBase64 Base64 encoded image data
     * @param userId User UUID (optional, for attribution)
     * @param boxName CrossFit box name (optional)
     * @param saveToDatabase Whether to save to database (default: true)
     * @param isPublic Whether to make public (default: false)
     * @return Result with scanned WOD data
     */
    suspend fun scanWod(
        imageBase64: String,
        userId: String? = null,
        boxName: String? = null,
        saveToDatabase: Boolean = true,
        isPublic: Boolean = false
    ): Result<WodScanResponse> {
        return try {
            android.util.Log.d("NayaBackend", "📸 Scanning WOD image...")

            val request = WodScanRequest(
                userId = userId,
                imageBase64 = imageBase64,
                imageType = "image/jpeg",
                boxName = boxName,
                saveToDatabase = saveToDatabase,
                isPublic = isPublic
            )

            val response = service.scanWod(request)

            if (response.success) {
                android.util.Log.d("NayaBackend", "✅ WOD scanned: ${response.wod?.name}")
            } else {
                android.util.Log.e("NayaBackend", "❌ WOD scan failed: ${response.error}")
            }

            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ WOD scan error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get list of WODs with filters
     */
    suspend fun getWods(
        userId: String? = null,
        wodType: String? = null,
        difficulty: String? = null,
        maxDuration: Int? = null,
        search: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<WodListResponse> {
        return try {
            android.util.Log.d("NayaBackend", "📋 Fetching WODs (limit=$limit, offset=$offset)")

            val response = service.getWods(
                userId = userId,
                wodType = wodType,
                difficulty = difficulty,
                maxDuration = maxDuration,
                search = search,
                limit = limit,
                offset = offset
            )

            android.util.Log.d("NayaBackend", "✅ Got ${response.count} WODs")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ Get WODs error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get single WOD with all movements
     */
    suspend fun getWodDetail(wodId: String): Result<WodWithMovements> {
        return try {
            android.util.Log.d("NayaBackend", "📋 Fetching WOD detail: $wodId")

            val response = service.getWodDetail(wodId)

            android.util.Log.d("NayaBackend", "✅ Got WOD: ${response.wod.name} with ${response.movements.size} movements")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ Get WOD detail error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Log a WOD result/score
     */
    suspend fun logWodResult(
        wodId: String,
        userId: String,
        scoreType: String,
        roundsCompleted: Int? = null,
        repsCompleted: Int? = null,
        timeSeconds: Int? = null,
        weightKg: Double? = null,
        totalReps: Int? = null,
        scalingLevel: String = "rx",
        completedWithinCap: Boolean = true,
        notes: String? = null,
        videoUrl: String? = null
    ): Result<Map<String, Any>> {
        return try {
            android.util.Log.d("NayaBackend", "📝 Logging WOD result for $wodId")

            val request = WodLogResultRequest(
                userId = userId,
                scoreType = scoreType,
                roundsCompleted = roundsCompleted,
                repsCompleted = repsCompleted,
                timeSeconds = timeSeconds,
                weightKg = weightKg,
                totalReps = totalReps,
                scalingLevel = scalingLevel,
                completedWithinCap = completedWithinCap,
                notes = notes,
                videoUrl = videoUrl
            )

            val response = service.logWodResult(wodId, request)

            android.util.Log.d("NayaBackend", "✅ WOD result logged")
            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ Log WOD result error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // AI EXERCISE CREATOR METHODS
    // ============================================================

    /**
     * Create a new exercise using AI from unknown movement name
     *
     * @param movementName The name of the unknown movement (e.g., "Turkish Get-Up")
     * @param userId Optional user ID for attribution
     * @param context Optional context (e.g., "CrossFit WOD")
     * @return Result with created exercise data
     */
    suspend fun createExerciseFromAI(
        movementName: String,
        userId: String? = null,
        context: String? = null
    ): Result<AICreateExerciseResponse> {
        return try {
            android.util.Log.d("NayaBackend", "🤖 AI creating exercise: $movementName")

            val request = AICreateExerciseRequest(
                userId = userId,
                movementName = movementName,
                context = context ?: "CrossFit/functional fitness movement"
            )

            val response = service.createExerciseFromAI(request)

            if (response.success) {
                android.util.Log.d("NayaBackend", "✅ AI created exercise: ${response.exercise?.name}")
            } else {
                android.util.Log.e("NayaBackend", "❌ AI exercise creation failed: ${response.error}")
            }

            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ AI exercise creation error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Create multiple exercises from a list of unknown movement names
     *
     * @param movements List of unknown movement names
     * @param userId Optional user ID for attribution
     * @param context Optional context (e.g., "CrossFit WOD")
     * @return Result with batch creation response
     */
    suspend fun createExercisesBatchFromAI(
        movements: List<String>,
        userId: String? = null,
        context: String? = null
    ): Result<AICreateExercisesBatchResponse> {
        return try {
            android.util.Log.d("NayaBackend", "🤖 AI creating ${movements.size} exercises in batch")

            val request = AICreateExercisesBatchRequest(
                userId = userId,
                movements = movements,
                context = context ?: "CrossFit/functional fitness movement"
            )

            val response = service.createExercisesBatchFromAI(request)

            android.util.Log.d("NayaBackend", "✅ AI batch result: ${response.createdCount} created, ${response.failedCount} failed")

            Result.success(response)
        } catch (e: Exception) {
            android.util.Log.e("NayaBackend", "❌ AI batch creation error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
