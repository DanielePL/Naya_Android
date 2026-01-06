package com.example.menotracker.data

import android.util.Log
import android.content.Context
import android.net.Uri
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Repository for Physical Coach chat integration via Supabase
 * Uses shared RPC functions that work with both Android App and Coach Desktop
 */
object CoachRepository {
    private const val TAG = "CoachRepository"

    // ============ Data Classes ============

    // Matches actual DB schema: coach_client_connections
    // Fields: id, coach_id, client_id, status, invited_by, coach_notes, created_at, accepted_at, updated_at
    @Serializable
    data class CoachConnection(
        val id: String,
        @SerialName("coach_id") val coachId: String,
        @SerialName("client_id") val clientId: String,
        val status: String,
        @SerialName("invited_by") val invitedBy: String? = null,
        @SerialName("coach_notes") val coachNotes: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("accepted_at") val acceptedAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    // Matches DB: conversations (id, created_at, updated_at, title)
    @Serializable
    data class CoachConversation(
        val id: String,
        val title: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    // Matches DB: messages (id, conversation_id, sender_id, content, created_at, read_at, edited_at, file_url, file_type, file_name)
    @Serializable
    data class CoachMessage(
        val id: String? = null,
        @SerialName("conversation_id") val conversationId: String,
        @SerialName("sender_id") val senderId: String,
        val content: String,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("read_at") val readAt: String? = null,
        @SerialName("file_url") val fileUrl: String? = null,
        @SerialName("file_type") val fileType: String? = null,
        @SerialName("file_name") val fileName: String? = null
    )

    // Matches DB: conversation_participants (id, conversation_id, user_id, joined_at, last_read_at, role)
    // Note: role is ENUM 'participant_role' with values 'member', 'admin'
    @Serializable
    data class ConversationParticipant(
        @SerialName("conversation_id") val conversationId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("role") val role: String
    )

    @Serializable
    data class CoachProfile(
        val id: String,
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null,
        val email: String? = null,
        @SerialName("invite_code") val inviteCode: String? = null
    )

    // ============ Connection Methods ============

    /**
     * Find a coach by their invite code.
     * Looks up the invite_code field in the profiles table.
     * Code format: 6 uppercase alphanumeric characters (e.g., "A3B7X9")
     */
    private suspend fun findCoachByCode(code: String): String? {
        val normalizedCode = code.trim().uppercase()

        try {
            // Direct lookup by invite_code field
            val coaches = SupabaseClient.client
                .from("profiles")
                .select {
                    filter {
                        eq("invite_code", normalizedCode)
                        eq("role", "coach")
                    }
                }
                .decodeList<CoachProfile>()

            if (coaches.isNotEmpty()) {
                Log.d(TAG, "Found coach via invite_code: ${coaches.first().id}")
                return coaches.first().id
            }

            Log.d(TAG, "No coach found with invite_code: $normalizedCode")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding coach by code: ${e.message}")
            return null
        }
    }

    /**
     * Connect to a coach using their invite code.
     * Code format: 6 uppercase alphanumeric characters (e.g., "A3B7X9")
     * Creates a pending connection request that the coach must accept.
     */
    suspend fun connectWithCoachCode(code: String, clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to connect with coach code: $code")

            val normalizedCode = code.trim().uppercase()
            if (normalizedCode.length != 6) {
                return@withContext Result.failure(Exception("Invalid code format. Code must be 6 characters."))
            }

            // Look up coach by invite_code field
            val coachId = findCoachByCode(normalizedCode)

            if (coachId == null) {
                Log.e(TAG, "No coach found with code: $normalizedCode")
                return@withContext Result.failure(Exception("Invalid coach code. Please check and try again."))
            }

            Log.d(TAG, "Found coach: $coachId")

            // Check if connection already exists
            val existingConnections = SupabaseClient.client
                .from("coach_client_connections")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("client_id", clientId)
                    }
                }
                .decodeList<CoachConnection>()

            if (existingConnections.isNotEmpty()) {
                val existing = existingConnections.first()
                return@withContext when (existing.status) {
                    "accepted" -> Result.failure(Exception("You are already connected to this coach."))
                    "pending" -> Result.failure(Exception("Connection request already pending. Waiting for coach approval."))
                    else -> Result.failure(Exception("Connection already exists with status: ${existing.status}"))
                }
            }

            // Create new pending connection
            val newConnection = mapOf(
                "id" to java.util.UUID.randomUUID().toString(),
                "coach_id" to coachId,
                "client_id" to clientId,
                "status" to "pending",
                "invited_by" to "client"
            )

            SupabaseClient.client
                .from("coach_client_connections")
                .insert(newConnection)

            Log.d(TAG, "✅ Connection request created successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error connecting with coach code: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get any pending connection requests for this client
     */
    suspend fun getPendingConnection(clientId: String): Result<CoachConnection?> = withContext(Dispatchers.IO) {
        try {
            val connections = SupabaseClient.client
                .from("coach_client_connections")
                .select {
                    filter {
                        eq("client_id", clientId)
                        eq("status", "pending")
                    }
                }
                .decodeList<CoachConnection>()

            Result.success(connections.firstOrNull())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pending connection: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getCoachConnection(clientId: String): Result<CoachConnection?> = withContext(Dispatchers.IO) {
        try {
            Log.e(TAG, "🔍 Fetching coach connection for client_id: $clientId")

            val connections = SupabaseClient.client
                .from("coach_client_connections")
                .select {
                    filter {
                        eq("client_id", clientId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<CoachConnection>()

            Log.d(TAG, "🔍 Found ${connections.size} connections")
            val connection = connections.firstOrNull()
            if (connection != null) {
                Log.d(TAG, "✅ Coach found: coach_id=${connection.coachId}, status=${connection.status}")
            } else {
                Log.d(TAG, "❌ No coach connection found for client_id: $clientId")
            }
            Result.success(connection)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching coach connection: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getCoachProfile(coachId: String): Result<CoachProfile?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching coach profile: $coachId")

            val profile = SupabaseClient.client
                .from("profiles")
                .select {
                    filter {
                        eq("id", coachId)
                    }
                }
                .decodeSingleOrNull<CoachProfile>()

            Log.d(TAG, "Coach profile: ${profile?.fullName}")
            Result.success(profile)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching coach profile: ${e.message}")
            Result.failure(e)
        }
    }

    // ============ Conversation Methods ============

    /**
     * Uses the shared RPC function find_or_create_conversation
     * This bypasses RLS and works for both Android App and Coach Desktop
     */
    suspend fun getOrCreateConversation(clientId: String, coachId: String): Result<CoachConversation> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Getting/creating conversation via RPC: client=$clientId, coach=$coachId")

            // Use the shared RPC function - it handles everything server-side
            // Parameter name is "target_user_id" (consistent with Coach Desktop)
            val conversationId = SupabaseClient.client.postgrest
                .rpc(
                    function = "find_or_create_conversation",
                    parameters = JsonObject(mapOf("target_user_id" to JsonPrimitive(coachId)))
                )
                .decodeAs<String>()

            Log.d(TAG, "✅ RPC returned conversation_id: $conversationId")

            // Now fetch the full conversation object
            val conversation = SupabaseClient.client
                .from("conversations")
                .select {
                    filter {
                        eq("id", conversationId)
                    }
                }
                .decodeSingleOrNull<CoachConversation>()

            if (conversation != null) {
                Log.d(TAG, "✅ Conversation ready: ${conversation.id}")
                Result.success(conversation)
            } else {
                // Shouldn't happen, but create a minimal object if it does
                Log.w(TAG, "⚠️ Could not fetch conversation details, using ID only")
                Result.success(CoachConversation(id = conversationId))
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in getOrCreateConversation: ${e.message}")
            e.printStackTrace()

            // Fallback: try the old direct method (might work if RLS is permissive)
            Log.d(TAG, "🔄 Trying fallback method...")
            getOrCreateConversationFallback(clientId, coachId)
        }
    }

    /**
     * Fallback method using direct queries (in case RPC fails)
     */
    private suspend fun getOrCreateConversationFallback(clientId: String, coachId: String): Result<CoachConversation> {
        return try {
            // Try to find existing via find_shared_conversation RPC
            val existingId = try {
                SupabaseClient.client.postgrest
                    .rpc(
                        function = "find_shared_conversation",
                        parameters = JsonObject(mapOf("other_user_id" to JsonPrimitive(coachId)))
                    )
                    .decodeAsOrNull<String>()
            } catch (e: Exception) {
                null
            }

            if (existingId != null) {
                Log.d(TAG, "✅ Found existing via find_shared_conversation: $existingId")
                val conversation = SupabaseClient.client
                    .from("conversations")
                    .select { filter { eq("id", existingId) } }
                    .decodeSingleOrNull<CoachConversation>()

                return Result.success(conversation ?: CoachConversation(id = existingId))
            }

            // Last resort: create directly (might fail due to RLS)
            Log.d(TAG, "⚠️ Creating conversation directly (fallback)...")
            val newConversation = CoachConversation(
                id = java.util.UUID.randomUUID().toString(),
                title = "Coach Chat"
            )

            SupabaseClient.client
                .from("conversations")
                .insert(newConversation)

            val participants = listOf(
                ConversationParticipant(newConversation.id, clientId, "member"),
                ConversationParticipant(newConversation.id, coachId, "member")
            )

            SupabaseClient.client
                .from("conversation_participants")
                .insert(participants)

            Log.d(TAG, "✅ Created conversation (fallback): ${newConversation.id}")
            Result.success(newConversation)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fallback also failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ============ Message Methods ============

    suspend fun getMessages(conversationId: String): Result<List<CoachMessage>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching messages for conversation: $conversationId")

            val messages = SupabaseClient.client
                .from("messages")
                .select {
                    filter {
                        eq("conversation_id", conversationId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<CoachMessage>()

            Log.d(TAG, "Fetched ${messages.size} messages")
            Result.success(messages)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching messages: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String
    ): Result<CoachMessage> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message to conversation: $conversationId")

            val message = CoachMessage(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                content = content
            )

            SupabaseClient.client
                .from("messages")
                .insert(message)

            Log.d(TAG, "Message sent: ${message.id}")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Send a message with file attachment
     * Uploads file to Supabase Storage first, then creates message with file_url
     */
    suspend fun sendMessageWithFile(
        context: Context,
        conversationId: String,
        senderId: String,
        content: String,
        fileUri: Uri,
        fileName: String,
        fileType: String
    ): Result<CoachMessage> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending message with file: $fileName")

            // 1. Upload file to storage
            val fileBytes = context.contentResolver.openInputStream(fileUri)?.readBytes()
                ?: throw Exception("Could not read file")

            val storagePath = "$senderId/${System.currentTimeMillis()}_$fileName"

            SupabaseClient.client.storage
                .from("chat-files")
                .upload(storagePath, fileBytes, upsert = false)

            // 2. Get public URL
            val publicUrl = SupabaseClient.client.storage
                .from("chat-files")
                .publicUrl(storagePath)

            Log.d(TAG, "File uploaded: $publicUrl")

            // 3. Create message with file info
            val message = CoachMessage(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                content = content.ifBlank { fileName },
                fileUrl = publicUrl,
                fileType = fileType,
                fileName = fileName
            )

            SupabaseClient.client
                .from("messages")
                .insert(message)

            Log.d(TAG, "Message with file sent: ${message.id}")
            Result.success(message)

        } catch (e: Exception) {
            Log.e(TAG, "Error sending message with file: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(conversationId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Marking messages as read in: $conversationId")

            // DB uses read_at timestamp instead of is_read boolean
            SupabaseClient.client
                .from("messages")
                .update({
                    set("read_at", java.time.Instant.now().toString())
                }) {
                    filter {
                        eq("conversation_id", conversationId)
                        neq("sender_id", userId)
                        exact("read_at", null)
                    }
                }

            Log.d(TAG, "Messages marked as read")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: ${e.message}")
            Result.failure(e)
        }
    }

    // Real-time placeholder - returns empty flow
    // TODO: Implement real-time subscription with Supabase Realtime
    fun subscribeToMessages(conversationId: String): Flow<CoachMessage> = flow<CoachMessage> {
        Log.d(TAG, "Real-time subscription placeholder for: $conversationId")
        // This is a placeholder - polling is used instead in the ViewModel
    }.flowOn(Dispatchers.IO)
}