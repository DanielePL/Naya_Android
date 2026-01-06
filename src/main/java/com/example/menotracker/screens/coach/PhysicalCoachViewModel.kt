package com.example.menotracker.screens.coach

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.data.CoachRepository
import com.example.menotracker.data.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Physical Coach chat screen
 * Handles Supabase connection for coach-client messaging
 */
class PhysicalCoachViewModel : ViewModel() {

    companion object {
        private const val TAG = "PhysicalCoachVM"
        private const val POLL_INTERVAL_MS = 5000L // Poll every 5 seconds
    }

    // UI State
    sealed class CoachState {
        object Loading : CoachState()
        object NoCoachConnected : CoachState()
        data class PendingConnection(
            val coachName: String?
        ) : CoachState()
        data class Ready(
            val coachId: String,
            val coachName: String,
            val coachAvatarUrl: String?,
            val conversationId: String
        ) : CoachState()
        data class Error(val message: String) : CoachState()
    }

    private val _state = MutableStateFlow<CoachState>(CoachState.Loading)
    val state: StateFlow<CoachState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<CoachRepository.CoachMessage>>(emptyList())
    val messages: StateFlow<List<CoachRepository.CoachMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _connectionSuccess = MutableStateFlow(false)
    val connectionSuccess: StateFlow<Boolean> = _connectionSuccess.asStateFlow()

    private var currentUserId: String? = null
    private var currentConversationId: String? = null
    private var isPolling = false

    init {
        loadCoachConnection()
    }

    private fun getCurrentUserId(): String? {
        return try {
            SupabaseClient.client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user: ${e.message}")
            null
        }
    }

    private fun loadCoachConnection() {
        viewModelScope.launch {
            _state.value = CoachState.Loading

            val userId = getCurrentUserId()
            Log.e(TAG, "🔍 Current User ID: $userId")

            if (userId == null) {
                Log.e(TAG, "❌ User ID is NULL - not logged in!")
                _state.value = CoachState.Error("Not logged in")
                return@launch
            }

            currentUserId = userId
            Log.e(TAG, "🔍 Loading coach connection for user: $userId")

            // Get coach connection
            val connectionResult = CoachRepository.getCoachConnection(userId)

            connectionResult.onSuccess { connection ->
                if (connection == null) {
                    // Check for pending connection
                    val pendingResult = CoachRepository.getPendingConnection(userId)
                    pendingResult.onSuccess { pending ->
                        if (pending != null) {
                            Log.d(TAG, "Found pending connection to coach: ${pending.coachId}")
                            val profileResult = CoachRepository.getCoachProfile(pending.coachId)
                            val coachName = profileResult.getOrNull()?.fullName
                            _state.value = CoachState.PendingConnection(coachName = coachName)
                        } else {
                            Log.d(TAG, "No coach connected")
                            _state.value = CoachState.NoCoachConnected
                        }
                    }
                    pendingResult.onFailure {
                        Log.d(TAG, "No coach connected")
                        _state.value = CoachState.NoCoachConnected
                    }
                    return@launch
                }

                Log.d(TAG, "Found coach connection: ${connection.coachId}")

                // Get coach profile from profiles table
                val profileResult = CoachRepository.getCoachProfile(connection.coachId)
                val coachName = profileResult.getOrNull()?.fullName ?: "Your Coach"
                val coachAvatarUrl = profileResult.getOrNull()?.avatarUrl

                // Get or create conversation
                val conversationResult = CoachRepository.getOrCreateConversation(
                    clientId = userId,
                    coachId = connection.coachId
                )

                conversationResult.onSuccess { conversation ->
                    currentConversationId = conversation.id
                    Log.d(TAG, "Conversation ready: ${conversation.id}")

                    _state.value = CoachState.Ready(
                        coachId = connection.coachId,
                        coachName = coachName,
                        coachAvatarUrl = coachAvatarUrl,
                        conversationId = conversation.id
                    )

                    // Load messages
                    loadMessages()

                    // Start polling for new messages
                    startPolling()
                }

                conversationResult.onFailure { error ->
                    Log.e(TAG, "Failed to get/create conversation: ${error.message}")
                    _state.value = CoachState.Error("Failed to load chat: ${error.message}")
                }
            }

            connectionResult.onFailure { error ->
                Log.e(TAG, "Failed to get coach connection: ${error.message}")
                _state.value = CoachState.Error("Failed to connect: ${error.message}")
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            val conversationId = currentConversationId ?: return@launch

            val result = CoachRepository.getMessages(conversationId)

            result.onSuccess { messages ->
                _messages.value = messages
                Log.d(TAG, "Loaded ${messages.size} messages")

                // Mark as read
                currentUserId?.let { userId ->
                    CoachRepository.markMessagesAsRead(conversationId, userId)
                }
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to load messages: ${error.message}")
            }
        }
    }

    private fun startPolling() {
        if (isPolling) return
        isPolling = true

        viewModelScope.launch {
            while (isPolling) {
                delay(POLL_INTERVAL_MS)
                if (currentConversationId != null) {
                    loadMessages()
                }
            }
        }
    }

    fun stopPolling() {
        isPolling = false
    }

    fun sendMessage(content: String) {
        val conversationId = currentConversationId ?: return
        val userId = currentUserId ?: return

        if (content.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true

            val result = CoachRepository.sendMessage(
                conversationId = conversationId,
                senderId = userId,
                content = content.trim()
            )

            result.onSuccess { message ->
                Log.d(TAG, "Message sent: ${message.id}")
                // Add to local list immediately
                _messages.value = _messages.value + message
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to send message: ${error.message}")
            }

            _isSending.value = false
        }
    }

    fun sendMessageWithFile(
        context: Context,
        content: String,
        fileUri: Uri,
        fileName: String,
        fileType: String
    ) {
        val conversationId = currentConversationId ?: return
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _isSending.value = true

            val result = CoachRepository.sendMessageWithFile(
                context = context,
                conversationId = conversationId,
                senderId = userId,
                content = content,
                fileUri = fileUri,
                fileName = fileName,
                fileType = fileType
            )

            result.onSuccess { message ->
                Log.d(TAG, "Message with file sent: ${message.id}")
                _messages.value = _messages.value + message
            }

            result.onFailure { error ->
                Log.e(TAG, "Failed to send message with file: ${error.message}")
            }

            _isSending.value = false
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadMessages()
            _isRefreshing.value = false
        }
    }

    fun retry() {
        loadCoachConnection()
    }

    /**
     * Connect to a coach using their invite code
     */
    fun connectWithCode(code: String) {
        val userId = currentUserId ?: getCurrentUserId() ?: return

        viewModelScope.launch {
            _isConnecting.value = true
            _connectionError.value = null
            _connectionSuccess.value = false

            val result = CoachRepository.connectWithCoachCode(code, userId)

            result.onSuccess {
                Log.d(TAG, "✅ Connection request sent successfully")
                _connectionSuccess.value = true
                // Show pending state
                _state.value = CoachState.PendingConnection(coachName = null)
            }

            result.onFailure { error ->
                Log.e(TAG, "❌ Failed to connect with code: ${error.message}")
                _connectionError.value = error.message ?: "Failed to connect"
            }

            _isConnecting.value = false
        }
    }

    fun clearConnectionError() {
        _connectionError.value = null
    }

    fun clearConnectionSuccess() {
        _connectionSuccess.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}