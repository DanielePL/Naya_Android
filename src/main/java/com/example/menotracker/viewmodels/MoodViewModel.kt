package com.example.menotracker.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.data.models.*
import com.example.menotracker.data.repository.MoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Mood Journaling
 * Handles mood tracking with tier-based limits
 */
class MoodViewModel : ViewModel() {
    private val TAG = "MoodViewModel"

    // ═══════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════

    private val _uiState = MutableStateFlow(MoodUiState())
    val uiState: StateFlow<MoodUiState> = _uiState.asStateFlow()

    private val _todayMoods = MutableStateFlow<List<MoodEntry>>(emptyList())
    val todayMoods: StateFlow<List<MoodEntry>> = _todayMoods.asStateFlow()

    private val _weeklyMoods = MutableStateFlow<List<MoodEntry>>(emptyList())
    val weeklyMoods: StateFlow<List<MoodEntry>> = _weeklyMoods.asStateFlow()

    private val _moodTrends = MutableStateFlow<List<MoodTrendPoint>>(emptyList())
    val moodTrends: StateFlow<List<MoodTrendPoint>> = _moodTrends.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showUpgradePrompt = MutableStateFlow(false)
    val showUpgradePrompt: StateFlow<Boolean> = _showUpgradePrompt.asStateFlow()

    // Entry sheet state
    private val _showEntrySheet = MutableStateFlow(false)
    val showEntrySheet: StateFlow<Boolean> = _showEntrySheet.asStateFlow()

    private val _selectedMoodForEdit = MutableStateFlow<MoodEntry?>(null)
    val selectedMoodForEdit: StateFlow<MoodEntry?> = _selectedMoodForEdit.asStateFlow()

    // Current user ID
    private var currentUserId: String? = null

    // ═══════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════

    fun initialize(userId: String) {
        currentUserId = userId
        loadData()
    }

    private fun loadData() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load today's moods
                MoodRepository.getTodayMoods(userId).onSuccess { moods ->
                    _todayMoods.value = moods
                }

                // Load weekly moods and stats
                MoodRepository.getWeeklyMoods(userId).onSuccess { moods ->
                    _weeklyMoods.value = moods
                }

                // Load trends
                MoodRepository.getMoodTrends(userId, 14).onSuccess { trends ->
                    _moodTrends.value = trends
                }

                updateUiState()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                _error.value = "Failed to load mood data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateUiState() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            val weeklyStats = MoodRepository.weeklyStats.value

            _uiState.value = MoodUiState(
                todayMoodCount = _todayMoods.value.size,
                latestMood = _todayMoods.value.firstOrNull()?.moodEnum,
                weeklyStats = weeklyStats,
                canAddMood = MoodRepository.canAddMoodEntry(userId),
                remainingSlots = MoodRepository.getRemainingSlots(userId),
                hasUnlimitedAccess = SubscriptionManager.hasAccess(
                    com.example.menotracker.billing.Feature.UNLIMITED_MOOD_ENTRIES
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ENTRY SHEET
    // ═══════════════════════════════════════════════════════════════

    fun openEntrySheet() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch

            if (!MoodRepository.canAddMoodEntry(userId)) {
                _showUpgradePrompt.value = true
                return@launch
            }

            _selectedMoodForEdit.value = null
            _showEntrySheet.value = true
        }
    }

    fun openEditSheet(mood: MoodEntry) {
        _selectedMoodForEdit.value = mood
        _showEntrySheet.value = true
    }

    fun closeEntrySheet() {
        _showEntrySheet.value = false
        _selectedMoodForEdit.value = null
    }

    fun dismissUpgradePrompt() {
        _showUpgradePrompt.value = false
    }

    // ═══════════════════════════════════════════════════════════════
    // LOG MOOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Log a new mood entry
     */
    fun logMood(
        moodType: MoodType,
        intensity: Int,
        timeOfDay: TimeOfDay? = null,
        triggers: List<MoodTrigger>? = null,
        journalText: String? = null
    ) {
        val userId = currentUserId ?: run {
            _error.value = "Not logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            MoodRepository.logMood(
                userId = userId,
                moodType = moodType,
                intensity = intensity,
                timeOfDay = timeOfDay,
                triggers = triggers,
                journalText = journalText
            ).onSuccess { entry ->
                Log.d(TAG, "Mood logged: ${entry.moodType}")
                _todayMoods.value = listOf(entry) + _todayMoods.value
                _weeklyMoods.value = listOf(entry) + _weeklyMoods.value
                closeEntrySheet()
                updateUiState()
            }.onFailure { e ->
                Log.e(TAG, "Failed to log mood", e)
                if (e.message?.contains("Weekly limit") == true) {
                    _showUpgradePrompt.value = true
                } else {
                    _error.value = e.message ?: "Failed to save mood"
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Quick mood log (just mood type and default intensity)
     */
    fun quickLogMood(moodType: MoodType) {
        val userId = currentUserId ?: run {
            _error.value = "Not logged in"
            return
        }

        viewModelScope.launch {
            if (!MoodRepository.canAddMoodEntry(userId)) {
                _showUpgradePrompt.value = true
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            MoodRepository.quickLogMood(userId, moodType).onSuccess { entry ->
                Log.d(TAG, "Quick mood logged: ${entry.moodType}")
                _todayMoods.value = listOf(entry) + _todayMoods.value
                _weeklyMoods.value = listOf(entry) + _weeklyMoods.value
                updateUiState()
            }.onFailure { e ->
                Log.e(TAG, "Failed to quick log mood", e)
                _error.value = "Failed to save mood"
            }

            _isLoading.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE / DELETE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Delete a mood entry
     */
    fun deleteMood(moodId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            MoodRepository.deleteMood(userId, moodId).onSuccess {
                _todayMoods.value = _todayMoods.value.filter { it.id != moodId }
                _weeklyMoods.value = _weeklyMoods.value.filter { it.id != moodId }
                updateUiState()
            }.onFailure { e ->
                _error.value = "Failed to delete mood"
            }
        }
    }

    /**
     * Update journal text
     */
    fun updateJournalText(moodId: String, newText: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            MoodRepository.updateJournalText(userId, moodId, newText).onSuccess { updated ->
                _todayMoods.value = _todayMoods.value.map {
                    if (it.id == moodId) updated else it
                }
                _weeklyMoods.value = _weeklyMoods.value.map {
                    if (it.id == moodId) updated else it
                }
                closeEntrySheet()
            }.onFailure { e ->
                _error.value = "Failed to update"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════

    fun refresh() {
        loadData()
    }

    fun clearError() {
        _error.value = null
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        MoodRepository.clearState()
    }
}

/**
 * UI State for Mood Journaling
 */
data class MoodUiState(
    val todayMoodCount: Int = 0,
    val latestMood: MoodType? = null,
    val weeklyStats: WeeklyMoodSummary? = null,
    val canAddMood: Boolean = true,
    val remainingSlots: Int = 3,
    val hasUnlimitedAccess: Boolean = false
)
