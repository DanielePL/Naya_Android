package com.example.menotracker.community.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.community.data.GamificationRepository
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for gamification features: XP, Levels, Badges, Streaks, Activity Feed.
 */
class GamificationViewModel : ViewModel() {
    companion object {
        private const val TAG = "GamificationViewModel"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _userLevel = MutableStateFlow<UserLevel?>(null)
    val userLevel: StateFlow<UserLevel?> = _userLevel.asStateFlow()

    private val _userStreak = MutableStateFlow<UserStreak?>(null)
    val userStreak: StateFlow<UserStreak?> = _userStreak.asStateFlow()

    private val _userBadges = MutableStateFlow<List<UserBadge>>(emptyList())
    val userBadges: StateFlow<List<UserBadge>> = _userBadges.asStateFlow()

    private val _xpHistory = MutableStateFlow<List<XpTransaction>>(emptyList())
    val xpHistory: StateFlow<List<XpTransaction>> = _xpHistory.asStateFlow()

    private val _activityFeedState = MutableStateFlow(ActivityFeedState())
    val activityFeedState: StateFlow<ActivityFeedState> = _activityFeedState.asStateFlow()

    private val _xpGainEvent = MutableStateFlow<XpGainEvent?>(null)
    val xpGainEvent: StateFlow<XpGainEvent?> = _xpGainEvent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentUserId: String? = null

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    fun initialize(userId: String) {
        if (!CommunityFeatureFlag.ENABLED) {
            return
        }

        currentUserId = userId
        loadGamificationData()
        loadActivityFeed()
    }

    private fun loadGamificationData() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _isLoading.value = true

            // Load all gamification data in parallel
            launch {
                GamificationRepository.getUserLevel(userId).onSuccess { level ->
                    _userLevel.value = level
                }
            }

            launch {
                GamificationRepository.getUserStreak(userId).onSuccess { streak ->
                    _userStreak.value = streak
                }
            }

            launch {
                GamificationRepository.getUserBadges(userId).onSuccess { badges ->
                    _userBadges.value = badges
                }
            }

            launch {
                GamificationRepository.getXpHistory(userId, 10).onSuccess { history ->
                    _xpHistory.value = history
                }
            }

            _isLoading.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTIVITY FEED
    // ═══════════════════════════════════════════════════════════════════════

    private fun loadActivityFeed() {
        viewModelScope.launch {
            _activityFeedState.value = _activityFeedState.value.copy(isLoading = true)

            GamificationRepository.getActivityFeed().onSuccess { activities ->
                _activityFeedState.value = ActivityFeedState(
                    activities = activities,
                    isLoading = false
                )

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${activities.size} activity items")
                }
            }.onFailure { error ->
                _activityFeedState.value = _activityFeedState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }

    fun refreshActivityFeed() {
        loadActivityFeed()
    }

    fun loadMoreActivities() {
        val currentState = _activityFeedState.value
        if (currentState.isLoading || !currentState.hasMore) return

        viewModelScope.launch {
            _activityFeedState.value = currentState.copy(isLoading = true)

            GamificationRepository.getActivityFeed(
                limit = 20,
                offset = currentState.activities.size
            ).onSuccess { newActivities ->
                _activityFeedState.value = currentState.copy(
                    activities = currentState.activities + newActivities,
                    isLoading = false,
                    hasMore = newActivities.size == 20
                )
            }.onFailure { error ->
                _activityFeedState.value = currentState.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // XP AWARDING (Called from workout completion, etc.)
    // ═══════════════════════════════════════════════════════════════════════

    fun awardWorkoutXp(workoutId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            GamificationRepository.awardWorkoutXp(userId, workoutId)

            // Also update streak
            GamificationRepository.updateStreak(userId).onSuccess { streak ->
                _userStreak.value = streak
            }

            // Refresh level
            GamificationRepository.getUserLevel(userId).onSuccess { level ->
                val oldLevel = _userLevel.value?.level ?: 1
                _userLevel.value = level

                // Show XP popup
                _xpGainEvent.value = XpGainEvent(
                    amount = XpRewards.COMPLETE_WORKOUT,
                    source = "Workout completed",
                    newLevel = if (level?.level ?: 1 > oldLevel) level?.level else null
                )
            }

            // Check for first workout badge
            GamificationRepository.checkAndAwardBadge(userId, "first_workout")
        }
    }

    fun awardPrXp(exerciseId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            GamificationRepository.awardPrXp(userId, exerciseId)

            // Show XP popup
            _xpGainEvent.value = XpGainEvent(
                amount = XpRewards.ACHIEVE_PR,
                source = "New personal record!"
            )

            // Check for first PR badge
            GamificationRepository.checkAndAwardBadge(userId, "first_pr")

            // Refresh level
            GamificationRepository.getUserLevel(userId).onSuccess { level ->
                _userLevel.value = level
            }
        }
    }

    fun awardShareXp(postId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            GamificationRepository.awardShareXp(userId, postId)

            _xpGainEvent.value = XpGainEvent(
                amount = XpRewards.SHARE_WORKOUT,
                source = "Workout shared"
            )

            // Check for first share badge
            GamificationRepository.checkAndAwardBadge(userId, "first_share")

            // Refresh level
            GamificationRepository.getUserLevel(userId).onSuccess { level ->
                _userLevel.value = level
            }
        }
    }

    fun awardChallengeParticipationXp(challengeId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            GamificationRepository.awardChallengeParticipationXp(userId, challengeId)

            _xpGainEvent.value = XpGainEvent(
                amount = XpRewards.CHALLENGE_PARTICIPATION,
                source = "Joined challenge"
            )

            // Check for first challenge badge
            GamificationRepository.checkAndAwardBadge(userId, "challenge_first")

            // Refresh level
            GamificationRepository.getUserLevel(userId).onSuccess { level ->
                _userLevel.value = level
            }
        }
    }

    fun awardChallengeWinXp(challengeId: String, position: Int) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            GamificationRepository.awardChallengeWinXp(userId, challengeId, position)

            val xp = when (position) {
                1 -> XpRewards.CHALLENGE_WINNER
                2, 3 -> XpRewards.CHALLENGE_TOP_3
                in 4..10 -> XpRewards.CHALLENGE_TOP_10
                else -> 0
            }

            if (xp > 0) {
                _xpGainEvent.value = XpGainEvent(
                    amount = xp,
                    source = when (position) {
                        1 -> "Challenge Winner!"
                        2, 3 -> "Top 3 Finish!"
                        else -> "Top 10 Finish!"
                    }
                )

                if (position == 1) {
                    GamificationRepository.checkAndAwardBadge(userId, "challenge_winner")
                }
            }

            // Refresh level
            GamificationRepository.getUserLevel(userId).onSuccess { level ->
                _userLevel.value = level
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STREAK MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════

    fun useStreakFreeze() {
        val userId = currentUserId ?: return
        val streak = _userStreak.value ?: return

        if (streak.freezeTokens <= 0) return

        viewModelScope.launch {
            GamificationRepository.useStreakFreeze(userId).onSuccess {
                // Refresh streak
                GamificationRepository.getUserStreak(userId).onSuccess { updatedStreak ->
                    _userStreak.value = updatedStreak
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EVENT HANDLING
    // ═══════════════════════════════════════════════════════════════════════

    fun dismissXpGainEvent() {
        _xpGainEvent.value = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════════════

    fun refresh() {
        loadGamificationData()
        loadActivityFeed()
    }
}

/**
 * State for activity feed
 */
data class ActivityFeedState(
    val activities: List<ActivityItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)

/**
 * Event for XP gain popup
 */
data class XpGainEvent(
    val amount: Int,
    val source: String,
    val newLevel: Int? = null
)