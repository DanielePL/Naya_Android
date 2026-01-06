package com.example.menotracker.data

import android.util.Log
import com.example.menotracker.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository for AI Coach operations
 * Handles all API calls to the Naya Backend AI Coach endpoints
 */
object AICoachRepository {
    private const val TAG = "AICoachRepository"

    /**
     * Send a chat message and get response
     *
     * @param userId User's UUID
     * @param conversationId Optional conversation ID (null for new conversation)
     * @param message User's message
     * @param context Optional context (current screen, selected items)
     * @param attachments Optional list of base64-encoded attachments
     * @return Result with ChatResponse or error
     */
    suspend fun sendMessage(
        userId: String,
        conversationId: String?,
        message: String,
        context: ChatContext = ChatContext(),
        attachments: List<ChatAttachment>? = null
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val request = ChatRequest(
                userId = userId,
                conversationId = conversationId,
                message = message,
                context = context,
                attachments = attachments
            )

            Log.d(TAG, "📤 Sending chat message: ${message.take(50)}...")
            Log.d(TAG, "   Conversation ID: ${conversationId ?: "new"}")
            Log.d(TAG, "   Attachments: ${attachments?.size ?: 0}")

            val response = NayaBackendService.create().sendChatMessage(request)

            Log.d(TAG, "✅ Chat response received: ${response.message.take(50)}...")
            Log.d(TAG, "   Conversation ID: ${response.conversationId}")

            // Track AI Coach chat for cost analytics
            UsageTracker.logAICoachChat(
                userId = userId,
                messageLength = message.length,
                conversationId = response.conversationId,
                success = true
            )

            Result.success(response)

        } catch (e: HttpException) {
            Log.e(TAG, "❌ HTTP error sending message: ${e.code()} - ${e.message()}")

            // Track failed AI Coach chat
            UsageTracker.logAICoachChat(
                userId = userId,
                messageLength = message.length,
                conversationId = conversationId,
                success = false,
                errorMessage = "HTTP ${e.code()}"
            )

            Result.failure(IOException("Server error: ${e.code()}"))
        } catch (e: IOException) {
            Log.e(TAG, "❌ Network error sending message: ${e.message}")

            // Track failed AI Coach chat
            UsageTracker.logAICoachChat(
                userId = userId,
                messageLength = message.length,
                conversationId = conversationId,
                success = false,
                errorMessage = e.message
            )

            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending message: ${e.message}")

            // Track failed AI Coach chat
            UsageTracker.logAICoachChat(
                userId = userId,
                messageLength = message.length,
                conversationId = conversationId,
                success = false,
                errorMessage = e.message
            )

            Result.failure(e)
        }
    }

    /**
     * Get all conversations for a user
     *
     * @param userId User's UUID
     * @return Result with list of Conversations or error
     */
    suspend fun getUserConversations(userId: String): Result<List<Conversation>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📤 Fetching conversations for user: $userId")

                val conversations = NayaBackendService.create()
                    .getUserConversations(userId)

                Log.d(TAG, "✅ Fetched ${conversations.size} conversations")

                Result.success(conversations)

            } catch (e: HttpException) {
                Log.e(TAG, "❌ HTTP error fetching conversations: ${e.code()} - ${e.message()}")
                Result.failure(IOException("Server error: ${e.code()}"))
            } catch (e: IOException) {
                Log.e(TAG, "❌ Network error fetching conversations: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching conversations: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Get all messages in a conversation
     *
     * @param conversationId Conversation UUID
     * @return Result with list of ChatMessages or error
     */
    suspend fun getConversationMessages(conversationId: String): Result<List<ChatMessage>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📤 Fetching messages for conversation: $conversationId")

                val messages = NayaBackendService.create()
                    .getConversationMessages(conversationId)

                Log.d(TAG, "✅ Fetched ${messages.size} messages")

                Result.success(messages)

            } catch (e: HttpException) {
                Log.e(TAG, "❌ HTTP error fetching messages: ${e.code()} - ${e.message()}")
                Result.failure(IOException("Server error: ${e.code()}"))
            } catch (e: IOException) {
                Log.e(TAG, "❌ Network error fetching messages: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching messages: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Archive (soft delete) a conversation
     *
     * @param conversationId Conversation UUID
     * @return Result with success or error
     */
    suspend fun archiveConversation(conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📤 Archiving conversation: $conversationId")

                NayaBackendService.create()
                    .archiveConversation(conversationId)

                Log.d(TAG, "✅ Conversation archived")

                Result.success(Unit)

            } catch (e: HttpException) {
                Log.e(TAG, "❌ HTTP error archiving conversation: ${e.code()} - ${e.message()}")
                Result.failure(IOException("Server error: ${e.code()}"))
            } catch (e: IOException) {
                Log.e(TAG, "❌ Network error archiving conversation: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error archiving conversation: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Save AI-generated workout to user's workout library
     *
     * @param userId User's UUID
     * @param workout Workout recommendation from AI
     * @return Result with workout template ID or error
     */
    suspend fun saveWorkout(
        userId: String,
        workout: WorkoutRecommendation
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 Saving AI workout: ${workout.name}")

            val request = mapOf(
                "user_id" to userId,
                "workout" to mapOf(
                    "name" to workout.name,
                    "description" to workout.description,
                    "exercises" to workout.exercises.map { ex ->
                        mapOf(
                            "exercise_name" to ex.exerciseName,
                            "order_index" to ex.orderIndex,
                            "sets" to ex.sets,
                            "reps" to ex.reps,
                            "intensity" to ex.intensity,
                            "notes" to ex.notes
                        )
                    }
                )
            )

            val response = NayaBackendService.create()
                .saveAIWorkout(request)

            val workoutTemplateId = response["workout_template_id"] as? String
                ?: throw Exception("No workout_template_id in response")

            val exercisesAdded = (response["exercises_added"] as? Double)?.toInt() ?: 0

            Log.d(TAG, "✅ Workout saved: $workoutTemplateId ($exercisesAdded exercises)")

            Result.success(workoutTemplateId)

        } catch (e: HttpException) {
            Log.e(TAG, "❌ HTTP error saving workout: ${e.code()} - ${e.message()}")
            Result.failure(IOException("Server error: ${e.code()}"))
        } catch (e: IOException) {
            Log.e(TAG, "❌ Network error saving workout: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving workout: ${e.message}")
            Result.failure(e)
        }
    }
}
