package com.example.menotracker.data.models

import com.google.gson.annotations.SerializedName
import java.time.Instant

/**
 * AI Coach Conversation Thread
 */
data class Conversation(
    val id: String,
    val title: String,
    @SerializedName("message_count")
    val messageCount: Int,
    @SerializedName("last_message")
    val lastMessage: String?,
    @SerializedName("last_message_role")
    val lastMessageRole: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val archived: Boolean = false
) {
    /**
     * Get formatted timestamp for display
     */
    fun getFormattedTime(): String {
        return try {
            val instant = Instant.parse(updatedAt)
            val now = Instant.now()
            val diff = java.time.Duration.between(instant, now)

            when {
                diff.toMinutes() < 1 -> "Just now"
                diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
                diff.toHours() < 24 -> "${diff.toHours()}h ago"
                diff.toDays() < 7 -> "${diff.toDays()}d ago"
                else -> {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    java.time.ZonedDateTime.parse(updatedAt).format(formatter)
                }
            }
        } catch (e: Exception) {
            updatedAt
        }
    }
}

/**
 * Request to send a chat message
 */
data class ChatRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("conversation_id")
    val conversationId: String?,
    val message: String,
    val context: ChatContext = ChatContext(),
    val attachments: List<ChatAttachment>? = null
)

/**
 * Attachment data for chat messages (base64 encoded)
 */
data class ChatAttachment(
    val type: String,  // "image", "pdf"
    val data: String,  // base64 encoded content
    @SerializedName("file_name")
    val fileName: String,
    @SerializedName("mime_type")
    val mimeType: String
)

/**
 * Context for chat message - includes menopause wellness data
 */
data class ChatContext(
    @SerializedName("current_screen")
    val currentScreen: String = "ai_coach",
    @SerializedName("selected_exercise_id")
    val selectedExerciseId: String? = null,
    @SerializedName("selected_workout_id")
    val selectedWorkoutId: String? = null,

    // Menopause-specific context for NAYA Coach
    @SerializedName("menopause_stage")
    val menopauseStage: String? = null,  // "perimenopause", "menopause", "postmenopause"
    @SerializedName("primary_symptoms")
    val primarySymptoms: List<String>? = null,  // ["hot_flashes", "sleep_issues", "mood_changes"]
    @SerializedName("wellness_goals")
    val wellnessGoals: List<String>? = null,  // ["better_sleep", "manage_symptoms", "stay_active"]
    @SerializedName("recent_symptoms")
    val recentSymptoms: List<RecentSymptom>? = null,  // Last 7 days symptom log
    @SerializedName("user_age")
    val userAge: Int? = null
)

/**
 * Recent symptom entry for AI context
 */
data class RecentSymptom(
    val type: String,  // "hot_flash", "mood", "sleep", "energy"
    val severity: Int,  // 1-5
    val date: String,  // ISO date
    val notes: String? = null
)

/**
 * Response from chat endpoint
 */
data class ChatResponse(
    @SerializedName("conversation_id")
    val conversationId: String,
    val message: String,
    val actions: List<ChatAction> = emptyList()
)

/**
 * Action suggested by AI
 */
data class ChatAction(
    val type: String, // "workout_created", "recommend_template", "recommend_exercise", etc.
    val data: Map<String, Any>
)

/**
 * Template recommendation from AI - for displaying clickable cards
 */
data class TemplateRecommendation(
    @SerializedName("template_id")
    val templateId: String,
    val name: String,
    val description: String? = null,
    val sports: List<String>? = null,
    @SerializedName("exercise_count")
    val exerciseCount: Int = 0,
    @SerializedName("exercise_names")
    val exerciseNames: List<String> = emptyList()
)

/**
 * Workout recommendation from AI
 */
data class WorkoutRecommendation(
    val name: String,
    val description: String,
    val exercises: List<ExerciseRecommendation>
)

/**
 * Exercise in a workout recommendation (API response model)
 * Note: Different from DbExerciseRecommendation - this is for AI chat responses
 */
data class ExerciseRecommendation(
    @SerializedName("exercise_name")
    val exerciseName: String,
    @SerializedName("order_index")
    val orderIndex: Int,
    val sets: Int,
    val reps: Int,
    val intensity: String? = null,
    val notes: String? = null
)
