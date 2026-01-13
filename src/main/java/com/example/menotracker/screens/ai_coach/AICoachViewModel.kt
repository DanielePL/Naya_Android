package com.example.menotracker.screens.ai_coach

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.config.BackendConfig
import com.example.menotracker.data.*
import com.example.menotracker.data.models.*
import com.example.menotracker.viewmodels.WorkoutBuilderViewModel
import io.github.jan.supabase.gotrue.auth
import com.example.menotracker.viewmodels.WorkoutTemplate
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AICoachViewModel(
    private val workoutBuilderViewModel: WorkoutBuilderViewModel
) : ViewModel() {

    companion object {
        private const val TAG = "AICoachViewModel"
    }

    /**
     * Get the current authenticated user ID from Supabase Auth
     * Returns null if not authenticated - AI Coach requires login
     */
    private fun getCurrentUserId(): String? {
        return try {
            SupabaseClient.client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Could not get current user ID: ${e.message}")
            null
        }
    }

    /**
     * Get user ID or throw error if not logged in
     * AI Coach requires authentication
     */
    private fun requireUserId(): String {
        return getCurrentUserId() ?: throw IllegalStateException("User must be logged in to use AI Coach")
    }

    // AI Service - Naya Backend (uses BackendConfig for URL)
    private val backendService = NayaBackendService.create(BackendConfig.BASE_URL)

    // State
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // User's workout templates (Library)
    private val _userWorkoutTemplates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val userWorkoutTemplates: StateFlow<List<WorkoutTemplate>> = _userWorkoutTemplates.asStateFlow()

    // Public workout templates (available to all users)
    private val _publicWorkoutTemplates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val publicWorkoutTemplates: StateFlow<List<WorkoutTemplate>> = _publicWorkoutTemplates.asStateFlow()

    // Conversation management
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    // Workout actions from AI responses
    private val _workoutActions = MutableStateFlow<Map<String, WorkoutRecommendation>>(emptyMap())
    val workoutActions: StateFlow<Map<String, WorkoutRecommendation>> = _workoutActions.asStateFlow()

    // Template recommendations from AI (existing templates from library)
    private val _templateRecommendations = MutableStateFlow<Map<String, TemplateRecommendation>>(emptyMap())
    val templateRecommendations: StateFlow<Map<String, TemplateRecommendation>> = _templateRecommendations.asStateFlow()

    // Conversation list for sidebar
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    // Menopause Wellness Context
    private val _wellnessContext = MutableStateFlow<MenopauseWellnessContext?>(null)
    val wellnessContext: StateFlow<MenopauseWellnessContext?> = _wellnessContext.asStateFlow()

    // Proactive Alerts
    private val _activeAlerts = MutableStateFlow<List<ProactiveAlert>>(emptyList())
    val activeAlerts: StateFlow<List<ProactiveAlert>> = _activeAlerts.asStateFlow()

    // Top alert for banner display
    private val _topAlert = MutableStateFlow<ProactiveAlert?>(null)
    val topAlert: StateFlow<ProactiveAlert?> = _topAlert.asStateFlow()

    // Temporary storage for message ID to link actions to messages
    private var _lastAiMessageId: String? = null

    // Temporary storage for template marker to append to message for persistence
    private var _pendingTemplateMarker: String? = null

    init {
        loadUserProfile()
        loadUserWorkoutTemplates()
        loadPublicWorkoutTemplates()
        loadConversations()
        loadWellnessContext()
        // Don't send welcome message yet - wait for conversations to load
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "⚠️ No user logged in - skipping profile load")
                return@launch
            }

            UserProfileRepository.getCurrentProfile(userId)
                .onSuccess { profile ->
                    _userProfile.value = profile
                    Log.d(TAG, "✅ Loaded user profile: ${profile.name}")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to load user profile: ${error.message}")
                }
        }
    }

    /**
     * Load comprehensive wellness context for AI-powered insights
     */
    private fun loadWellnessContext() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "⚠️ No user logged in - skipping wellness context load")
                return@launch
            }

            try {
                Log.d(TAG, "🔄 Loading wellness context for AI Coach...")

                // Build comprehensive context from all health data
                val context = LocalUserContextBuilder.buildContext(userId, _userProfile.value)
                _wellnessContext.value = context

                Log.d(TAG, "✅ Wellness context loaded: ${if (context.hasData) "has data" else "no data yet"}")

                // Check for proactive alerts
                checkProactiveAlerts(userId, context)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load wellness context: ${e.message}")
            }
        }
    }

    /**
     * Check for proactive alerts and update state
     */
    private suspend fun checkProactiveAlerts(userId: String, context: MenopauseWellnessContext) {
        try {
            val alerts = ProactiveAlertEngine.checkAlerts(userId, context)
            _activeAlerts.value = alerts

            // Get top alert for banner
            val topAlert = ProactiveAlertEngine.getTopAlert(userId, context)
            _topAlert.value = topAlert

            if (alerts.isNotEmpty()) {
                Log.d(TAG, "🔔 Found ${alerts.size} proactive alerts, top: ${topAlert?.title}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking proactive alerts: ${e.message}")
        }
    }

    /**
     * Refresh wellness context and alerts
     */
    fun refreshWellnessContext() {
        loadWellnessContext()
    }

    /**
     * Dismiss an alert (user clicked dismiss)
     */
    fun dismissAlert(alertId: String) {
        _activeAlerts.value = _activeAlerts.value.filter { it.id != alertId }
        if (_topAlert.value?.id == alertId) {
            _topAlert.value = _activeAlerts.value.firstOrNull { it.shouldShowBanner() }
        }
    }

    /**
     * Start a proactive conversation based on an alert
     */
    fun startProactiveChat(alert: ProactiveAlert) {
        viewModelScope.launch {
            // Create new conversation if needed
            if (_currentConversationId.value != null) {
                createNewChat()
            }

            // Add the alert as an AI message to start the conversation
            val proactiveMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = alert.toConversationStarter(),
                role = "assistant"
            )
            _messages.value = listOf(proactiveMessage)

            // Dismiss the alert
            dismissAlert(alert.id)

            Log.d(TAG, "💬 Started proactive chat: ${alert.title}")
        }
    }

    private fun loadUserWorkoutTemplates() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "⚠️ No user logged in - skipping workout templates load")
                return@launch
            }

            WorkoutTemplateRepository.loadWorkoutTemplates(userId)
                .onSuccess { templates ->
                    _userWorkoutTemplates.value = templates
                    Log.d(TAG, "✅ Loaded ${templates.size} workout templates from Library")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to load workout templates: ${error.message}")
                }
        }
    }

    private fun loadPublicWorkoutTemplates() {
        viewModelScope.launch {
            WorkoutTemplateRepository.loadPublicWorkoutTemplates()
                .onSuccess { templates ->
                    _publicWorkoutTemplates.value = templates
                    Log.d(TAG, "✅ Loaded ${templates.size} public workout templates")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to load public workout templates: ${error.message}")
                }
        }
    }

    private fun sendWelcomeMessage() {
        val profile = _userProfile.value
        val userName = profile?.name?.split(" ")?.firstOrNull() ?: "there"

        val welcomeMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = """
                Hi $userName! 💜 I'm Naya, your personal wellness companion.

                I specialize in:
                • Understanding & managing menopause symptoms
                • Hormonal changes & their effects
                • Optimizing sleep, mood & energy
                • Nutrition & movement during menopause
                • Stress management & emotional wellbeing

                I'm here to listen and support you - without judgment, with understanding.

                What's on your mind today? 🌸
            """.trimIndent(),
            role = "assistant"
        )

        _messages.value = listOf(welcomeMessage)
    }

    fun sendMessage(userMessage: String, attachments: List<MessageAttachment> = emptyList(), context: Context? = null) {
        if (userMessage.isBlank() && attachments.isEmpty()) return

        // Add user message to chat
        val userChatMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = userMessage,
            role = "user",
            attachments = attachments
        )
        _messages.value = _messages.value + userChatMessage

        // Get AI response
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = getAIResponse(userMessage, attachments, context)

                // Use the message ID that was set during action processing
                // This ensures the message ID matches the one used for workoutActions/templateRecommendations
                val messageId = _lastAiMessageId ?: UUID.randomUUID().toString()
                _lastAiMessageId = null // Reset for next message

                val aiChatMessage = ChatMessage(
                    id = messageId,
                    content = response,
                    role = "assistant"
                )
                _messages.value = _messages.value + aiChatMessage

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting AI response: ${e.message}", e)
                e.printStackTrace()
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "Sorry, I had a problem: ${e.message ?: "Unknown error"}. Please try again.",
                    role = "assistant"
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getAIResponse(userMessage: String, attachments: List<MessageAttachment>, appContext: Context?): String {
        try {
            Log.d(TAG, "🔄 Using NAYA Backend AI Coach Chat...")

            // Build comprehensive chat context with all wellness data
            val profile = _userProfile.value
            val wellness = _wellnessContext.value
            val alerts = _activeAlerts.value

            // Convert symptoms to RecentSymptom format
            val recentSymptomsList = wellness?.recentSymptoms?.take(20)?.map { symptom ->
                RecentSymptom(
                    type = symptom.symptomType,
                    severity = symptom.intensity,
                    date = symptom.loggedAt.substring(0, 10),
                    notes = symptom.notes
                )
            }

            // Build formatted context summary for system prompt
            val contextSummary = wellness?.let { LocalUserContextBuilder.formatContextForPrompt(it) }

            val chatContext = ChatContext(
                currentScreen = "ai_coach",
                selectedExerciseId = null,
                selectedWorkoutId = null,

                // User profile
                userName = profile?.name?.split(" ")?.firstOrNull(),
                userAge = profile?.age,

                // Menopause-specific context
                menopauseStage = profile?.menopauseStage,
                primarySymptoms = profile?.primarySymptoms,
                wellnessGoals = profile?.wellnessGoals,

                // Recent symptoms
                recentSymptoms = recentSymptomsList,

                // Sleep data
                avgSleepHours = wellness?.sleepStats?.averageHours,
                avgSleepQuality = wellness?.sleepStats?.averageQuality,
                sleepInterruptions = wellness?.sleepStats?.totalInterruptions,

                // Mood data
                dominantMood = wellness?.moodStats?.dominantMood?.displayName,
                moodTriggers = wellness?.moodStats?.mostCommonTriggers?.take(3)?.map { it.first.displayName },

                // Bone health
                avgCalciumMg = wellness?.boneHealthLogs?.map { it.calciumMg }?.average()?.toFloat(),
                avgVitaminDIu = wellness?.boneHealthLogs?.map { it.vitaminDIu }?.average()?.toFloat(),
                strengthTrainingDays = wellness?.boneHealthLogs?.count { it.strengthTrainingDone },

                // Dietary preferences
                dietaryPreferences = profile?.dietaryPreferences,
                foodAllergies = profile?.foodAllergies,

                // Active concerns from alerts
                activeConcerns = alerts.filter { it.priority == AlertPriority.HIGH || it.priority == AlertPriority.MEDIUM }
                    .map { "${it.title}: ${it.message}" },

                // Full context summary
                wellnessContextSummary = contextSummary
            )

            // Convert MessageAttachment to ChatAttachment with Base64 encoding
            val chatAttachments: List<ChatAttachment>? = if (attachments.isNotEmpty() && appContext != null) {
                attachments.mapNotNull { attachment ->
                    try {
                        val uri = Uri.parse(attachment.fileUri)
                        val inputStream = appContext.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val bytes = inputStream.readBytes()
                            inputStream.close()

                            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

                            // Determine MIME type based on AttachmentType enum
                            val typeString = when (attachment.type) {
                                AttachmentType.IMAGE, AttachmentType.DEXA_SCAN -> "image"
                                AttachmentType.PDF, AttachmentType.MEDICAL_DIAGNOSIS, AttachmentType.PHYSIO_REPORT -> "pdf"
                                else -> "other"
                            }
                            val mimeType = when (attachment.type) {
                                AttachmentType.IMAGE, AttachmentType.DEXA_SCAN -> appContext.contentResolver.getType(uri) ?: "image/jpeg"
                                AttachmentType.PDF, AttachmentType.MEDICAL_DIAGNOSIS, AttachmentType.PHYSIO_REPORT -> "application/pdf"
                                else -> "application/octet-stream"
                            }

                            Log.d(TAG, "📎 Encoded attachment: ${attachment.type}, ${bytes.size} bytes -> ${base64Data.length} chars base64")

                            ChatAttachment(
                                type = typeString,
                                data = base64Data,
                                fileName = attachment.fileName,
                                mimeType = mimeType
                            )
                        } else {
                            Log.w(TAG, "⚠️ Could not open input stream for ${attachment.fileUri}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error encoding attachment ${attachment.fileName}: ${e.message}")
                        null
                    }
                }.takeIf { it.isNotEmpty() }
            } else {
                null
            }

            Log.d(TAG, "📝 Sending to backend - Message: ${userMessage.take(50)}..., ConvID: ${_currentConversationId.value ?: "new"}, Attachments: ${chatAttachments?.size ?: 0}")

            // Call new persistent chat endpoint with attachments
            val result = AICoachRepository.sendMessage(
                userId = requireUserId(),
                conversationId = _currentConversationId.value,
                message = userMessage,
                context = chatContext,
                attachments = chatAttachments
            )

            result.onSuccess { response ->
                Log.d(TAG, "✅ Got chat response from backend")

                // Update conversation ID (for new conversations)
                val wasNewConversation = _currentConversationId.value == null
                _currentConversationId.value = response.conversationId

                // If this was a new conversation, refresh the conversations list
                if (wasNewConversation) {
                    Log.d(TAG, "🔄 New conversation created, refreshing list...")
                    refreshConversations()
                }

                // Generate message ID ONCE - this will be used for both the message and any actions
                val aiMessageId = UUID.randomUUID().toString()

                // Process any workout actions - use the same message ID
                response.actions.forEach { action ->
                    when (action.type) {
                        "workout_created" -> {
                            try {
                                // Extract workout data from action
                                @Suppress("UNCHECKED_CAST")
                                val workoutData = action.data["workout"] as? Map<String, Any>

                                if (workoutData != null) {
                                    val name = workoutData["name"] as? String ?: "Unnamed Workout"
                                    val description = workoutData["description"] as? String ?: ""
                                    val exercisesData = workoutData["exercises"] as? List<Map<String, Any>> ?: emptyList()

                                    val exercises = exercisesData.mapIndexed { index, ex ->
                                        ExerciseRecommendation(
                                            exerciseName = ex["exercise_name"] as? String ?: "",
                                            orderIndex = (ex["order_index"] as? Number)?.toInt() ?: index,
                                            sets = (ex["sets"] as? Number)?.toInt() ?: 3,
                                            reps = (ex["reps"] as? Number)?.toInt() ?: 10,
                                            intensity = ex["intensity"] as? String,
                                            notes = ex["notes"] as? String
                                        )
                                    }

                                    val workout = WorkoutRecommendation(
                                        name = name,
                                        description = description,
                                        exercises = exercises
                                    )

                                    // Store workout action with the SAME message ID
                                    _workoutActions.value = _workoutActions.value + (aiMessageId to workout)

                                    Log.d(TAG, "💪 Workout action detected: $name with ${exercises.size} exercises (msgId: $aiMessageId)")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error processing workout action: ${e.message}")
                            }
                        }

                        "recommend_template" -> {
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val templateData = action.data["template"] as? Map<String, Any>

                                if (templateData != null) {
                                    val templateId = templateData["template_id"] as? String ?: ""
                                    val name = templateData["name"] as? String ?: "Unnamed Template"
                                    val description = templateData["description"] as? String
                                    @Suppress("UNCHECKED_CAST")
                                    val sports = templateData["sports"] as? List<String>
                                    val exerciseCount = (templateData["exercise_count"] as? Number)?.toInt() ?: 0
                                    @Suppress("UNCHECKED_CAST")
                                    val exerciseNames = templateData["exercise_names"] as? List<String> ?: emptyList()

                                    val template = TemplateRecommendation(
                                        templateId = templateId,
                                        name = name,
                                        description = description,
                                        sports = sports,
                                        exerciseCount = exerciseCount,
                                        exerciseNames = exerciseNames
                                    )

                                    // Store template recommendation with the SAME message ID
                                    _templateRecommendations.value = _templateRecommendations.value + (aiMessageId to template)

                                    // Store template marker for message persistence
                                    _pendingTemplateMarker = "[TEMPLATE:$templateId:$name]"

                                    Log.d(TAG, "📋 Template recommendation: $name (ID: $templateId, msgId: $aiMessageId)")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error processing template recommendation: ${e.message}")
                            }
                        }
                    }
                }

                // Return both the message text AND the message ID so caller can use it
                // We'll handle this by storing the ID in a class-level variable temporarily
                _lastAiMessageId = aiMessageId
                return response.message
            }

            result.onFailure { error ->
                Log.e(TAG, "❌ Backend chat error: ${error.message}")
                throw error
            }

            return "I couldn't generate a response. Please check if the backend is running."

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in getBackendResponse: ${e.message}", e)

            // Provide helpful error message based on exception type
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true || e.message?.contains("Connection refused") == true ->
                    "Cannot connect to Naya Backend. Make sure it's running:\n\n" +
                    "Terminal: python3 naya_backend.py\n\n" +
                    "Backend URL: ${BackendConfig.BASE_URL}"

                e.message?.contains("timeout") == true ->
                    "Backend timeout. The server might be busy.\n\n" +
                    "Please try again in a moment."

                else -> "Backend error: ${e.message}\n\nCheck the backend logs for details."
            }

            throw Exception(errorMessage)
        }
    }

    /**
     * Save AI-generated workout to My Workouts
     */
    fun saveWorkout(workout: WorkoutRecommendation, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "💾 Saving workout: ${workout.name}")

                val result = AICoachRepository.saveWorkout(
                    userId = requireUserId(),
                    workout = workout
                )

                result.onSuccess { workoutTemplateId ->
                    Log.d(TAG, "✅ Workout saved successfully: $workoutTemplateId")

                    val successMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = "Workout '${workout.name}' saved to My Workouts! You can find it in your Library.",
                        role = "assistant"
                    )
                    _messages.value = _messages.value + successMessage

                    onSuccess()
                }

                result.onFailure { error ->
                    Log.e(TAG, "❌ Error saving workout: ${error.message}")
                    val errorMsg = error.message ?: "Unknown error"

                    val errorMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = "Sorry, I couldn't save the workout: $errorMsg. Please try again.",
                        role = "assistant"
                    )
                    _messages.value = _messages.value + errorMessage

                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception saving workout: ${e.message}")
                onError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load all conversations from backend
     * Automatically loads the most recent conversation
     */
    private fun loadConversations() {
        viewModelScope.launch {
            _isLoadingConversations.value = true

            try {
                val userId = getCurrentUserId()
                if (userId == null) {
                    Log.w(TAG, "⚠️ No user logged in - skipping conversations load")
                    sendWelcomeMessage()
                    return@launch
                }
                Log.d(TAG, "📂 Loading conversations for user: $userId")

                val result = AICoachRepository.getUserConversations(userId)

                result.onSuccess { conversations ->
                    _conversations.value = conversations.sortedByDescending { it.updatedAt }
                    Log.d(TAG, "✅ Loaded ${conversations.size} conversations")

                    // Auto-load most recent conversation if exists
                    if (conversations.isNotEmpty()) {
                        val mostRecent = conversations.first()
                        Log.d(TAG, "🔄 Auto-loading most recent conversation: ${mostRecent.id}")
                        loadConversation(mostRecent.id)
                    } else {
                        // No conversations yet - send welcome message
                        sendWelcomeMessage()
                    }
                }

                result.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load conversations: ${error.message}")
                    // If loading fails, just send welcome message
                    sendWelcomeMessage()
                }

            } finally {
                _isLoadingConversations.value = false
            }
        }
    }

    /**
     * Load a specific conversation and all its messages
     */
    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                Log.d(TAG, "📥 Loading conversation: $conversationId")

                val result = AICoachRepository.getConversationMessages(conversationId)

                result.onSuccess { messages ->
                    _currentConversationId.value = conversationId
                    _messages.value = messages

                    // Clear old actions and recommendations
                    _workoutActions.value = emptyMap()
                    _templateRecommendations.value = emptyMap()

                    // Parse template recommendations from message content
                    // Try multiple patterns:
                    // 1. [TEMPLATE:templateId:templateName] format (if backend adds it)
                    // 2. Match template names mentioned in the message text
                    val templatePattern = """\[TEMPLATE:([a-f0-9-]+):([^\]]+)\]""".toRegex()
                    val allTemplates = _publicWorkoutTemplates.value + _userWorkoutTemplates.value

                    messages.forEach { message ->
                        if (message.role == "assistant") {
                            // First try explicit [TEMPLATE:...] marker
                            val explicitMatch = templatePattern.find(message.content)
                            if (explicitMatch != null) {
                                val templateId = explicitMatch.groupValues[1]
                                val templateName = explicitMatch.groupValues[2]

                                val template = allTemplates.find { it.id == templateId }
                                if (template != null) {
                                    val recommendation = TemplateRecommendation(
                                        templateId = templateId,
                                        name = template.name,
                                        description = null, // WorkoutTemplate doesn't have description
                                        exerciseCount = template.exercises.size,
                                        exerciseNames = template.exercises.map { it.exerciseName }
                                    )
                                    _templateRecommendations.value = _templateRecommendations.value + (message.id to recommendation)
                                    Log.d(TAG, "📋 Restored template recommendation: ${template.name} for message ${message.id}")
                                } else {
                                    val recommendation = TemplateRecommendation(
                                        templateId = templateId,
                                        name = templateName,
                                        description = null,
                                        exerciseCount = 0,
                                        exerciseNames = emptyList()
                                    )
                                    _templateRecommendations.value = _templateRecommendations.value + (message.id to recommendation)
                                    Log.d(TAG, "📋 Created basic template recommendation: $templateName for message ${message.id}")
                                }
                            } else {
                                // Try to find template name mentioned in message (case-insensitive)
                                val lowerContent = message.content.lowercase()
                                val matchedTemplate = allTemplates.find { template ->
                                    // Check if template name appears in message
                                    // Use word boundaries to avoid partial matches
                                    val templateNameLower = template.name.lowercase()
                                    lowerContent.contains(templateNameLower) &&
                                    // Only match if it looks like a recommendation (contains keywords)
                                    (lowerContent.contains("recommend") ||
                                     lowerContent.contains("suggest") ||
                                     lowerContent.contains("try") ||
                                     lowerContent.contains("workout") ||
                                     lowerContent.contains("template"))
                                }

                                if (matchedTemplate != null) {
                                    val recommendation = TemplateRecommendation(
                                        templateId = matchedTemplate.id,
                                        name = matchedTemplate.name,
                                        description = null, // WorkoutTemplate doesn't have description
                                        exerciseCount = matchedTemplate.exercises.size,
                                        exerciseNames = matchedTemplate.exercises.map { it.exerciseName }
                                    )
                                    _templateRecommendations.value = _templateRecommendations.value + (message.id to recommendation)
                                    Log.d(TAG, "📋 Found template by name match: ${matchedTemplate.name} for message ${message.id}")
                                }
                            }
                        }
                    }

                    Log.d(TAG, "✅ Loaded ${messages.size} messages, restored ${_templateRecommendations.value.size} template recommendations")
                }

                result.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load conversation: ${error.message}")
                }

            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new chat conversation
     */
    fun createNewChat() {
        Log.d(TAG, "➕ Creating new chat")

        // Clear current state
        _messages.value = emptyList()
        _currentConversationId.value = null
        _workoutActions.value = emptyMap()
        _templateRecommendations.value = emptyMap()

        // Send welcome message
        sendWelcomeMessage()
    }

    /**
     * Delete (archive) a conversation
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Archiving conversation: $conversationId")

                val result = AICoachRepository.archiveConversation(conversationId)

                result.onSuccess {
                    Log.d(TAG, "✅ Conversation archived")

                    // Remove from local list
                    _conversations.value = _conversations.value.filter { it.id != conversationId }

                    // If we deleted the current conversation, create a new chat
                    if (_currentConversationId.value == conversationId) {
                        createNewChat()
                    }
                }

                result.onFailure { error ->
                    Log.e(TAG, "❌ Failed to archive conversation: ${error.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error archiving conversation: ${e.message}")
            }
        }
    }

    /**
     * Refresh conversations list
     */
    fun refreshConversations() {
        loadConversations()
    }

    fun clearChat() {
        createNewChat()
    }

    // ═══════════════════════════════════════════════════════════════
    // QUICK ACTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Quick action types for menopause wellness support
     */
    enum class QuickAction(
        val displayName: String,
        val emoji: String,
        val prompt: String
    ) {
        SYMPTOMS(
            "Discuss Symptoms",
            "🔥",
            "I'd like to talk about my current symptoms and get some relief strategies."
        ),
        SLEEP(
            "Sleep Help",
            "😴",
            "I'm having trouble sleeping. Can you help me with some strategies?"
        ),
        NUTRITION(
            "Nutrition Tips",
            "🥗",
            "What foods and supplements would help with my menopause symptoms?"
        ),
        RELAXATION(
            "Relaxation",
            "🧘‍♀️",
            "I'm feeling stressed. Can you guide me through a relaxation technique?"
        ),
        EXERCISE(
            "Exercise Ideas",
            "🏃‍♀️",
            "What exercises would be good for me during menopause?"
        ),
        MOOD(
            "Mood Support",
            "💜",
            "I've been feeling emotionally overwhelmed lately. Can we talk about it?"
        )
    }

    /**
     * Send a quick action message
     */
    fun sendQuickAction(action: QuickAction, context: Context? = null) {
        sendMessage(action.prompt, emptyList(), context)
    }

    /**
     * Get recommended quick actions based on current wellness context
     */
    fun getRecommendedQuickActions(): List<QuickAction> {
        val wellness = _wellnessContext.value ?: return listOf(
            QuickAction.SYMPTOMS,
            QuickAction.SLEEP,
            QuickAction.NUTRITION
        )

        val recommendations = mutableListOf<QuickAction>()

        // Prioritize based on current health data
        val sleepStats = wellness.sleepStats
        if (sleepStats != null && sleepStats.averageHours < 6f) {
            recommendations.add(QuickAction.SLEEP)
        }

        val moodStats = wellness.moodStats
        if (moodStats?.dominantMood in listOf(MoodType.SAD, MoodType.ANXIOUS, MoodType.ANGRY)) {
            recommendations.add(QuickAction.MOOD)
        }

        // Check for frequent symptoms
        if (wellness.symptomStats.isNotEmpty()) {
            recommendations.add(QuickAction.SYMPTOMS)
        }

        // Always include nutrition for menopause wellness
        recommendations.add(QuickAction.NUTRITION)

        // Add relaxation if stress indicators
        if (moodStats?.mostCommonTriggers?.any { it.first == MoodTrigger.STRESS } == true) {
            recommendations.add(QuickAction.RELAXATION)
        }

        // Exercise if inactive
        if (wellness.boneHealthLogs.none { it.strengthTrainingDone }) {
            recommendations.add(QuickAction.EXERCISE)
        }

        return recommendations.distinct().take(4)
    }

    /**
     * Get a quick summary of the user's wellness status
     */
    fun getWellnessSummary(): String {
        val wellness = _wellnessContext.value ?: return "Start tracking your wellness to get personalized insights!"
        return LocalUserContextBuilder.generateQuickSummary(wellness)
    }
}
