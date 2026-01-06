package com.example.menotracker.config

/**
 * Centralized Backend Configuration
 *
 * Configure your Menotracker Backend URL here
 */
object BackendConfig {

    // ========== PRODUCTION (Render.com) ==========
    // Main Menotracker backend (AI Coach, WOD Scanner, etc.)
    // TODO: Update to new Menotracker backend URL when deployed
    private const val RENDER_BASE_URL = "https://naya-backend-69ht.onrender.com"

    // ========== DEVELOPMENT (Local) ==========
    // For Android Emulator (10.0.2.2 = localhost on host machine)
    private const val EMULATOR_BASE_URL = "http://10.0.2.2:8000"

    // For Physical Device - ngrok tunnel (works from anywhere!)
    private const val PHYSICAL_DEVICE_BASE_URL = "https://destroyable-kristan-nonrevoltingly.ngrok-free.dev"

    // ========== VBT BACKEND (NeiroFitnessApp) ==========
    // VBT always uses ngrok - no Render.com for VBT!
    private const val VBT_NGROK_URL = "https://destroyable-kristan-nonrevoltingly.ngrok-free.dev"

    // ========== ENVIRONMENT SELECTION ==========
    /**
     * Switch between environments:
     * - PRODUCTION: Use Render.com deployments
     * - EMULATOR: Use local backend on emulator
     * - PHYSICAL_DEVICE: Use local backend on physical device
     */
    enum class Environment {
        PRODUCTION,
        EMULATOR,
        PHYSICAL_DEVICE
    }

    // 🔧 CHANGE THIS to switch environments
    private val CURRENT_ENVIRONMENT = Environment.PRODUCTION

    // ========== ACTIVE CONFIGURATION ==========
    val BASE_URL: String
        get() = when (CURRENT_ENVIRONMENT) {
            Environment.PRODUCTION -> RENDER_BASE_URL
            Environment.EMULATOR -> EMULATOR_BASE_URL
            Environment.PHYSICAL_DEVICE -> PHYSICAL_DEVICE_BASE_URL
        }

    /**
     * VBT Analysis Backend URL (NeiroFitnessApp)
     *
     * This is a SEPARATE backend from the main Menotracker backend!
     * - Uses chunked video upload
     * - YOLO-based barbell detection
     * - Returns processed video with VBT overlay
     *
     * NOTE: VBT always uses ngrok - never Render.com!
     * The NeiroFitnessApp backend runs locally with ngrok tunnel.
     */
    val VBT_BASE_URL: String
        get() = VBT_NGROK_URL

    // ========== MAIN BACKEND API ENDPOINTS ==========
    /**
     * Form Analysis API (OLD - deprecated)
     * Endpoint: POST /api/v1/analyze-form
     */
    val FORM_ANALYSIS_ENDPOINT: String
        get() = "$BASE_URL/api/v1/analyze-form"

    /**
     * Health Check API
     * Endpoint: GET /
     */
    val HEALTH_CHECK_ENDPOINT: String
        get() = BASE_URL

    /**
     * Download Analyzed Video (OLD - deprecated)
     * Endpoint: GET /api/v1/download/{analysis_id}
     */
    fun getDownloadVideoEndpoint(analysisId: String): String {
        return "$BASE_URL/api/v1/download/$analysisId"
    }

    /**
     * Supported Exercises API
     * Endpoint: GET /api/v1/exercises
     */
    val EXERCISES_ENDPOINT: String
        get() = "$BASE_URL/api/v1/exercises"

    // ========== VBT BACKEND (NeiroFitnessApp) ENDPOINTS ==========
    /**
     * VBT Health Check
     * Endpoint: GET /api/health
     */
    val VBT_HEALTH_ENDPOINT: String
        get() = "$VBT_BASE_URL/api/health"

    /**
     * VBT Chunk Upload
     * Endpoint: POST /api/upload-chunk
     * Multipart: chunk, chunk_number, total_chunks, upload_id
     */
    val VBT_UPLOAD_CHUNK_ENDPOINT: String
        get() = "$VBT_BASE_URL/api/upload-chunk"

    /**
     * VBT Start Processing
     * Endpoint: POST /api/process-video
     * JSON: uploadId, exerciseType, weightKg, oneRM, userHeightCm
     */
    val VBT_PROCESS_VIDEO_ENDPOINT: String
        get() = "$VBT_BASE_URL/api/process-video"

    /**
     * VBT Processing Status
     * Endpoint: GET /api/status/{taskId}
     */
    fun getVbtStatusEndpoint(taskId: String): String {
        return "$VBT_BASE_URL/api/status/$taskId"
    }

    /**
     * VBT Analysis Results
     * Endpoint: GET /api/results/{taskId}
     */
    fun getVbtResultsEndpoint(taskId: String): String {
        return "$VBT_BASE_URL/api/results/$taskId"
    }

    /**
     * VBT Download Processed Video (with overlay)
     * Endpoint: GET /api/download/{taskId}
     */
    fun getVbtDownloadEndpoint(taskId: String): String {
        return "$VBT_BASE_URL/api/download/$taskId"
    }

    // ========== TIMEOUT CONFIGURATION ==========
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 120L  // Video processing can take time
    const val WRITE_TIMEOUT_SECONDS = 120L  // Video upload can take time

    // ========== LOGGING ==========
    const val ENABLE_LOGGING = true

    /**
     * Log current configuration
     */
    fun logConfiguration() {
        if (ENABLE_LOGGING) {
            android.util.Log.d("BackendConfig", """
                🔧 Backend Configuration:
                ├─ Environment: $CURRENT_ENVIRONMENT
                ├─ Base URL: $BASE_URL
                ├─ VBT URL: $VBT_BASE_URL
                ├─ Form Analysis: $FORM_ANALYSIS_ENDPOINT
                └─ Health Check: $HEALTH_CHECK_ENDPOINT
            """.trimIndent())
        }
    }
}
