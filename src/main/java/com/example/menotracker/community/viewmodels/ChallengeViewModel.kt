package com.example.menotracker.community.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.community.data.ChallengeRepository
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Challenge functionality.
 * Handles challenges, Max Out Friday, and user entries.
 */
class ChallengeViewModel : ViewModel() {
    companion object {
        private const val TAG = "ChallengeViewModel"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _challengesState = MutableStateFlow(ChallengesState())
    val challengesState: StateFlow<ChallengesState> = _challengesState.asStateFlow()

    private val _detailState = MutableStateFlow(ChallengeDetailState())
    val detailState: StateFlow<ChallengeDetailState> = _detailState.asStateFlow()

    private val _maxOutFridayState = MutableStateFlow(MaxOutFridayState())
    val maxOutFridayState: StateFlow<MaxOutFridayState> = _maxOutFridayState.asStateFlow()

    private var currentUserId: String? = null

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    fun initialize(userId: String) {
        currentUserId = userId
        loadChallenges()
        loadMaxOutFriday()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHALLENGES LOADING
    // ═══════════════════════════════════════════════════════════════════════

    fun loadChallenges() {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return
        }

        _challengesState.value = _challengesState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Load active and upcoming challenges in parallel
                val activeResult = ChallengeRepository.getActiveChallenges()
                val upcomingResult = ChallengeRepository.getUpcomingChallenges()

                activeResult.onSuccess { active ->
                    upcomingResult.onSuccess { upcoming ->
                        _challengesState.value = _challengesState.value.copy(
                            activeChallenges = active,
                            upcomingChallenges = upcoming,
                            isLoading = false,
                            error = null
                        )

                        if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                            Log.d(TAG, "✅ Loaded ${active.size} active, ${upcoming.size} upcoming challenges")
                        }
                    }.onFailure { error ->
                        handleChallengesError(error)
                    }
                }.onFailure { error ->
                    handleChallengesError(error)
                }
            } catch (e: Exception) {
                handleChallengesError(e)
            }
        }
    }

    private fun handleChallengesError(error: Throwable) {
        Log.e(TAG, "❌ Error loading challenges: ${error.message}")
        _challengesState.value = _challengesState.value.copy(
            isLoading = false,
            error = error.message
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAX OUT FRIDAY
    // ═══════════════════════════════════════════════════════════════════════

    fun loadMaxOutFriday() {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.MAX_OUT_FRIDAY_ENABLED) {
            return
        }

        _maxOutFridayState.value = _maxOutFridayState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = ChallengeRepository.getCurrentMaxOutFriday()

                result.onSuccess { info ->
                    _maxOutFridayState.value = _maxOutFridayState.value.copy(
                        info = info,
                        isLoading = false,
                        error = null
                    )

                    // Load entries if challenge exists
                    info?.let { loadMaxOutFridayEntries(it.id) }

                    if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                        Log.d(TAG, "✅ Loaded Max Out Friday: ${info?.exerciseName}")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error loading Max Out Friday: ${error.message}")
                    _maxOutFridayState.value = _maxOutFridayState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception loading Max Out Friday: ${e.message}")
                _maxOutFridayState.value = _maxOutFridayState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadMaxOutFridayEntries(challengeId: String) {
        viewModelScope.launch {
            ChallengeRepository.getChallengeEntries(challengeId, limit = 10).onSuccess { entries ->
                _maxOutFridayState.value = _maxOutFridayState.value.copy(
                    topEntries = entries
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHALLENGE DETAIL
    // ═══════════════════════════════════════════════════════════════════════

    fun loadChallengeDetail(challengeId: String) {
        _detailState.value = ChallengeDetailState(isLoading = true)

        viewModelScope.launch {
            try {
                val challengeResult = ChallengeRepository.getChallenge(challengeId)

                challengeResult.onSuccess { challenge ->
                    if (challenge == null) {
                        _detailState.value = _detailState.value.copy(
                            isLoading = false,
                            error = "Challenge not found"
                        )
                        return@onSuccess
                    }

                    _detailState.value = _detailState.value.copy(
                        challenge = challenge,
                        isLoading = false
                    )

                    // Load entries
                    loadChallengeEntries(challengeId)

                    // Load user's entry
                    currentUserId?.let { userId ->
                        loadUserEntry(challengeId, userId)
                    }
                }.onFailure { error ->
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadChallengeEntries(challengeId: String) {
        _detailState.value = _detailState.value.copy(isLoadingEntries = true)

        viewModelScope.launch {
            ChallengeRepository.getChallengeEntries(challengeId).onSuccess { entries ->
                _detailState.value = _detailState.value.copy(
                    entries = entries,
                    isLoadingEntries = false
                )
            }.onFailure {
                _detailState.value = _detailState.value.copy(isLoadingEntries = false)
            }
        }
    }

    private fun loadUserEntry(challengeId: String, userId: String) {
        viewModelScope.launch {
            ChallengeRepository.getUserEntry(challengeId, userId).onSuccess { entry ->
                _detailState.value = _detailState.value.copy(
                    userEntry = entry,
                    hasParticipated = entry != null
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHALLENGE PARTICIPATION
    // ═══════════════════════════════════════════════════════════════════════

    fun submitEntry(
        challengeId: String,
        valueKg: Double? = null,
        valueReps: Int? = null,
        workoutHistoryId: String? = null,
        isPr: Boolean = false,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            ChallengeRepository.submitChallengeEntry(
                challengeId = challengeId,
                userId = userId,
                valueKg = valueKg,
                valueReps = valueReps,
                workoutHistoryId = workoutHistoryId,
                isPr = isPr
            ).onSuccess { entry ->
                Log.d(TAG, "✅ Entry submitted: ${entry.id}")

                // Update state
                _detailState.value = _detailState.value.copy(
                    userEntry = entry,
                    hasParticipated = true
                )

                // Reload entries to show updated leaderboard
                loadChallengeEntries(challengeId)

                onSuccess()
            }.onFailure { error ->
                Log.e(TAG, "❌ Error submitting entry: ${error.message}")
                onError(error.message ?: "Unknown error")
            }
        }
    }

    fun submitMaxOutFridayEntry(
        valueKg: Double,
        workoutHistoryId: String? = null,
        isPr: Boolean = false,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val challengeId = _maxOutFridayState.value.info?.id ?: return

        submitEntry(
            challengeId = challengeId,
            valueKg = valueKg,
            workoutHistoryId = workoutHistoryId,
            isPr = isPr,
            onSuccess = {
                // Reload Max Out Friday state
                loadMaxOutFriday()
                onSuccess()
            },
            onError = onError
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════════════

    fun refresh() {
        loadChallenges()
        loadMaxOutFriday()
    }

    fun refreshChallengeDetail() {
        _detailState.value.challenge?.id?.let { loadChallengeDetail(it) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    fun clearError() {
        _challengesState.value = _challengesState.value.copy(error = null)
    }

    fun clearDetailError() {
        _detailState.value = _detailState.value.copy(error = null)
    }

    fun clearChallengeDetail() {
        _detailState.value = ChallengeDetailState()
    }
}
