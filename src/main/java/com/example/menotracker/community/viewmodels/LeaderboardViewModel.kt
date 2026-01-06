package com.example.menotracker.community.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.community.data.LeaderboardRepository
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Leaderboard functionality.
 * Handles leaderboard loading, filtering, and user ranking.
 */
class LeaderboardViewModel : ViewModel() {
    companion object {
        private const val TAG = "LeaderboardViewModel"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow(LeaderboardState())
    val state: StateFlow<LeaderboardState> = _state.asStateFlow()

    private var currentUserId: String? = null

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    fun initialize(userId: String) {
        currentUserId = userId
        loadLeaderboard()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LEADERBOARD LOADING
    // ═══════════════════════════════════════════════════════════════════════

    fun loadLeaderboard() {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.LEADERBOARD_ENABLED) {
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = LeaderboardRepository.getLeaderboard(
                    exerciseId = _state.value.selectedExercise,
                    friendsOnly = _state.value.friendsOnly
                )

                result.onSuccess { entries ->
                    // Find current user's rank
                    val userRank = entries.find { it.userId == currentUserId }?.rank

                    _state.value = _state.value.copy(
                        entries = entries,
                        isLoading = false,
                        error = null,
                        currentUserRank = userRank
                    )

                    if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                        Log.d(TAG, "✅ Loaded ${entries.size} leaderboard entries")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error loading leaderboard: ${error.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception loading leaderboard: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════════════════════════════

    fun selectExercise(exerciseId: String) {
        if (_state.value.selectedExercise != exerciseId) {
            _state.value = _state.value.copy(
                selectedExercise = exerciseId,
                entries = emptyList()
            )
            loadLeaderboard()
        }
    }

    fun toggleFriendsOnly() {
        _state.value = _state.value.copy(
            friendsOnly = !_state.value.friendsOnly,
            entries = emptyList()
        )
        loadLeaderboard()
    }

    fun setFriendsOnly(friendsOnly: Boolean) {
        if (_state.value.friendsOnly != friendsOnly) {
            _state.value = _state.value.copy(
                friendsOnly = friendsOnly,
                entries = emptyList()
            )
            loadLeaderboard()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════════════

    fun refresh() {
        loadLeaderboard()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    fun getExerciseName(): String {
        return LeaderboardExercises.getExerciseName(_state.value.selectedExercise)
    }

    fun getAvailableExercises(): List<LeaderboardExercise> {
        return LeaderboardExercises.EXERCISES
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
