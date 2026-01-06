package com.example.menotracker.network

import com.example.menotracker.config.BackendConfig
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * NeiroFitness Backend API Service
 *
 * Provides VBT analysis via the NeiroFitness Python backend.
 * Used for post-recording analysis with more accurate metrics.
 *
 * Flow:
 * 1. Upload video chunks → /api/upload-chunk
 * 2. Start processing → /api/process-video
 * 3. Poll for results → /api/status/{task_id}
 * 4. Get final results → /api/results/{task_id}
 *
 * @author Naya VBT Integration
 */
interface NeiroFitnessApiService {

    companion object {
        /**
         * Get VBT backend URL from centralized config
         * URL changes based on environment (Production, Emulator, Physical Device)
         */
        val DEFAULT_BASE_URL: String
            get() = BackendConfig.VBT_BASE_URL.let {
                if (it.endsWith("/")) it else "$it/"
            }
    }

    // ═══════════════════════════════════════════════════════════════
    // VIDEO UPLOAD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Upload video chunk for processing
     *
     * @param chunk Video file chunk (multipart)
     * @param chunkNumber Current chunk number (0-indexed)
     * @param totalChunks Total number of chunks
     * @param uploadId Unique upload identifier
     * @return Upload progress response
     */
    @Multipart
    @POST("api/upload-chunk")
    suspend fun uploadChunk(
        @Part chunk: MultipartBody.Part,
        @Part("chunk_number") chunkNumber: RequestBody,
        @Part("total_chunks") totalChunks: RequestBody,
        @Part("upload_id") uploadId: RequestBody
    ): Response<ChunkUploadResponse>

    // ═══════════════════════════════════════════════════════════════
    // VIDEO PROCESSING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start video processing after upload completes
     *
     * @param request Processing parameters
     * @return Task ID for status polling
     */
    @POST("api/process-video")
    suspend fun startProcessing(
        @Body request: ProcessVideoRequest
    ): Response<ProcessVideoResponse>

    /**
     * Get processing status
     *
     * @param taskId Task identifier from startProcessing
     * @return Current processing status
     */
    @GET("api/status/{task_id}")
    suspend fun getProcessingStatus(
        @Path("task_id") taskId: String
    ): Response<ProcessingStatusResponse>

    // ═══════════════════════════════════════════════════════════════
    // RESULTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get final VBT analysis results
     *
     * @param taskId Task identifier
     * @return Full VBT metrics and analysis
     */
    @GET("api/results/{task_id}")
    suspend fun getResults(
        @Path("task_id") taskId: String
    ): Response<VBTAnalysisResultResponse>

    // ═══════════════════════════════════════════════════════════════
    // HEALTH CHECK
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if backend is reachable
     */
    @GET("api/health")
    suspend fun healthCheck(): Response<HealthCheckResponse>
}

// ═══════════════════════════════════════════════════════════════
// REQUEST/RESPONSE DATA CLASSES
// ═══════════════════════════════════════════════════════════════

/**
 * Response from chunk upload
 */
data class ChunkUploadResponse(
    val success: Boolean,
    val message: String,
    val chunksReceived: Int,
    val totalChunks: Int,
    val uploadComplete: Boolean
)

/**
 * Request to start video processing
 */
data class ProcessVideoRequest(
    val uploadId: String,
    val exerciseType: String = "squat",
    val weightKg: Float? = null,
    val oneRM: Float? = null,
    val userHeightCm: Float = 175f
)

/**
 * Response from process-video endpoint
 */
data class ProcessVideoResponse(
    val success: Boolean,
    val taskId: String,
    val message: String
)

/**
 * Processing status response
 */
data class ProcessingStatusResponse(
    val taskId: String,
    val status: String,  // "pending", "processing", "completed", "failed"
    val progress: Int,   // 0-100
    val currentStep: String?,
    val error: String?
)

/**
 * Full VBT analysis results
 */
data class VBTAnalysisResultResponse(
    val taskId: String,
    val success: Boolean,
    val exerciseType: String,
    val totalReps: Int,
    val reps: List<RepAnalysis>,
    val summary: SetSummary,
    val warnings: List<String>,
    val processedAt: String
)

/**
 * Per-rep analysis data
 */
data class RepAnalysis(
    val repNumber: Int,
    val peakVelocity: Float,
    val avgVelocity: Float,
    val concentricTime: Float,
    val eccentricTime: Float,
    val totalTime: Float,
    val rangeOfMotion: Float,
    val pathAccuracy: Float,
    val techniqueScore: Int,
    val force: Float?,
    val power: Float?,
    val warnings: List<String>
)

/**
 * Set summary statistics
 */
data class SetSummary(
    val avgVelocity: Float,
    val peakVelocity: Float,
    val velocityDrop: Float,
    val avgTechniqueScore: Int,
    val totalTUT: Float,
    val estimatedOneRM: Float?,
    val loadPercent: Float?,
    val fatigueIndex: Float,
    val overallGrade: String  // "A", "B", "C", "D", "F"
)

/**
 * Health check response
 */
data class HealthCheckResponse(
    val status: String,
    val version: String,
    val modelLoaded: Boolean
)
