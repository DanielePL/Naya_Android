package com.example.menotracker.data

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * UsageTracker - Tracks all cost-generating API calls for admin analytics.
 *
 * This singleton logs usage events to Supabase for cost calculation.
 * Users cannot see this data - it's only accessible via admin panel.
 *
 * Supported event types:
 * - openai_vision: Meal photo analysis (GPT-4o Vision)
 * - openai_chat: OpenAI chat completions
 * - claude_chat: Claude API calls
 * - vbt_analysis: Form/VBT video analysis
 * - ai_coach_chat: AI Coach backend messages
 * - storage_upload: Supabase storage uploads
 * - storage_download: Supabase storage downloads
 */
object UsageTracker {

    private const val TAG = "UsageTracker"

    // Background scope for fire-and-forget logging
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Supabase client reference
    private val supabase = SupabaseClient.client

    // JSON serializer
    private val json = Json { ignoreUnknownKeys = true }

    // ═══════════════════════════════════════════════════════════════
    // EVENT TYPES
    // ═══════════════════════════════════════════════════════════════

    object EventType {
        const val OPENAI_VISION = "openai_vision"
        const val OPENAI_CHAT = "openai_chat"
        const val CLAUDE_CHAT = "claude_chat"
        const val VBT_ANALYSIS = "vbt_analysis"
        const val AI_COACH_CHAT = "ai_coach_chat"
        const val STORAGE_UPLOAD = "storage_upload"
        const val STORAGE_DOWNLOAD = "storage_download"
    }

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════

    @Serializable
    data class UsageLogEntry(
        val user_id: String?,
        val event_type: String,
        val input_tokens: Int = 0,
        val output_tokens: Int = 0,
        val total_tokens: Int = 0,
        val estimated_cost: Double = 0.0,
        val metadata: JsonObject = buildJsonObject {},
        val request_duration_ms: Int? = null,
        val success: Boolean = true,
        val error_message: String? = null
    )

    // ═══════════════════════════════════════════════════════════════
    // COST CALCULATION (Approximations - actual costs from DB)
    // ═══════════════════════════════════════════════════════════════

    private object CostRates {
        // GPT-4o pricing (as of 2024)
        const val GPT4O_INPUT_PER_TOKEN = 0.0000025    // $2.50 per 1M tokens
        const val GPT4O_OUTPUT_PER_TOKEN = 0.00001    // $10 per 1M tokens

        // Claude 3 Haiku pricing
        const val CLAUDE_HAIKU_INPUT_PER_TOKEN = 0.00000025   // $0.25 per 1M
        const val CLAUDE_HAIKU_OUTPUT_PER_TOKEN = 0.00000125  // $1.25 per 1M

        // Fixed costs (estimated)
        const val VBT_ANALYSIS_BASE = 0.001   // ~$0.001 per video analysis
        const val AI_COACH_CHAT_BASE = 0.002  // ~$0.002 per message

        // Storage costs
        const val STORAGE_PER_BYTE = 0.0000000005  // ~$0.50 per GB
    }

    private fun calculateCost(
        eventType: String,
        inputTokens: Int = 0,
        outputTokens: Int = 0,
        fileSizeBytes: Long = 0
    ): Double {
        return when (eventType) {
            EventType.OPENAI_VISION, EventType.OPENAI_CHAT -> {
                (inputTokens * CostRates.GPT4O_INPUT_PER_TOKEN) +
                        (outputTokens * CostRates.GPT4O_OUTPUT_PER_TOKEN)
            }
            EventType.CLAUDE_CHAT -> {
                (inputTokens * CostRates.CLAUDE_HAIKU_INPUT_PER_TOKEN) +
                        (outputTokens * CostRates.CLAUDE_HAIKU_OUTPUT_PER_TOKEN)
            }
            EventType.VBT_ANALYSIS -> CostRates.VBT_ANALYSIS_BASE
            EventType.AI_COACH_CHAT -> CostRates.AI_COACH_CHAT_BASE
            EventType.STORAGE_UPLOAD, EventType.STORAGE_DOWNLOAD -> {
                fileSizeBytes * CostRates.STORAGE_PER_BYTE
            }
            else -> 0.0
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC LOGGING METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Log an OpenAI Vision API call (meal photo analysis)
     */
    fun logOpenAIVision(
        userId: String?,
        inputTokens: Int,
        outputTokens: Int,
        model: String = "gpt-4o",
        imageSizeKb: Int? = null,
        durationMs: Int? = null,
        success: Boolean = true,
        errorMessage: String? = null
    ) {
        val metadata = buildJsonObject {
            put("model", model)
            imageSizeKb?.let { put("image_size_kb", it) }
        }

        logEvent(
            userId = userId,
            eventType = EventType.OPENAI_VISION,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            metadata = metadata,
            durationMs = durationMs,
            success = success,
            errorMessage = errorMessage
        )
    }

    /**
     * Log a VBT/Form Analysis request
     */
    fun logVBTAnalysis(
        userId: String?,
        exerciseType: String,
        videoDurationSec: Int? = null,
        durationMs: Int? = null,
        success: Boolean = true,
        errorMessage: String? = null
    ) {
        val metadata = buildJsonObject {
            put("exercise_type", exerciseType)
            videoDurationSec?.let { put("video_duration_sec", it) }
        }

        logEvent(
            userId = userId,
            eventType = EventType.VBT_ANALYSIS,
            metadata = metadata,
            durationMs = durationMs,
            success = success,
            errorMessage = errorMessage
        )
    }

    /**
     * Log an AI Coach chat message
     */
    fun logAICoachChat(
        userId: String?,
        messageLength: Int? = null,
        conversationId: String? = null,
        durationMs: Int? = null,
        success: Boolean = true,
        errorMessage: String? = null
    ) {
        val metadata = buildJsonObject {
            messageLength?.let { put("message_length", it) }
            conversationId?.let { put("conversation_id", it) }
        }

        logEvent(
            userId = userId,
            eventType = EventType.AI_COACH_CHAT,
            metadata = metadata,
            durationMs = durationMs,
            success = success,
            errorMessage = errorMessage
        )
    }

    /**
     * Log a storage upload
     */
    fun logStorageUpload(
        userId: String?,
        bucket: String,
        fileSizeBytes: Long,
        fileType: String? = null,
        success: Boolean = true,
        errorMessage: String? = null
    ) {
        val metadata = buildJsonObject {
            put("bucket", bucket)
            put("file_size_bytes", fileSizeBytes)
            fileType?.let { put("file_type", it) }
        }

        logEvent(
            userId = userId,
            eventType = EventType.STORAGE_UPLOAD,
            metadata = metadata,
            fileSizeBytes = fileSizeBytes,
            success = success,
            errorMessage = errorMessage
        )
    }

    /**
     * Log a Claude API call (if enabled)
     */
    fun logClaudeChat(
        userId: String?,
        inputTokens: Int,
        outputTokens: Int,
        model: String = "claude-3-haiku-20240307",
        durationMs: Int? = null,
        success: Boolean = true,
        errorMessage: String? = null
    ) {
        val metadata = buildJsonObject {
            put("model", model)
        }

        logEvent(
            userId = userId,
            eventType = EventType.CLAUDE_CHAT,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            metadata = metadata,
            durationMs = durationMs,
            success = success,
            errorMessage = errorMessage
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERNAL LOGGING
    // ═══════════════════════════════════════════════════════════════

    private fun logEvent(
        userId: String?,
        eventType: String,
        inputTokens: Int = 0,
        outputTokens: Int = 0,
        metadata: JsonObject = buildJsonObject {},
        durationMs: Int? = null,
        fileSizeBytes: Long = 0,
        success: Boolean = true,
        errorMessage: String? = null
    ) {
        // Fire-and-forget: don't block the calling code
        scope.launch {
            try {
                val estimatedCost = calculateCost(
                    eventType = eventType,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    fileSizeBytes = fileSizeBytes
                )

                val entry = UsageLogEntry(
                    user_id = userId,
                    event_type = eventType,
                    input_tokens = inputTokens,
                    output_tokens = outputTokens,
                    total_tokens = inputTokens + outputTokens,
                    estimated_cost = estimatedCost,
                    metadata = metadata,
                    request_duration_ms = durationMs,
                    success = success,
                    error_message = errorMessage
                )

                supabase.postgrest["usage_logs"].insert(entry)

                Log.d(TAG, "📊 Logged usage: $eventType | " +
                        "tokens: ${inputTokens + outputTokens} | " +
                        "cost: $${"%.6f".format(estimatedCost)}")

            } catch (e: Exception) {
                // Silent fail - don't impact user experience
                Log.e(TAG, "⚠️ Failed to log usage event: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get current user ID from Supabase auth (helper for callers)
     */
    suspend fun getCurrentUserId(): String? {
        return try {
            supabase.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }
}