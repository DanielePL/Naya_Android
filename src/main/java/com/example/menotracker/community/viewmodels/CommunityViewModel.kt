package com.example.menotracker.community.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.community.data.CommunityRepository
import com.example.menotracker.community.data.FeedRepository
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.data.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel for Community functionality.
 * Handles user profiles, follows, and community navigation.
 */
class CommunityViewModel : ViewModel() {
    companion object {
        private const val TAG = "CommunityViewModel"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _userProfileState = MutableStateFlow(UserProfileState())
    val userProfileState: StateFlow<UserProfileState> = _userProfileState.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<CommunityProfile?>(null)
    val currentUserProfile: StateFlow<CommunityProfile?> = _currentUserProfile.asStateFlow()

    private val _followers = MutableStateFlow<List<Follow>>(emptyList())
    val followers: StateFlow<List<Follow>> = _followers.asStateFlow()

    private val _following = MutableStateFlow<List<Follow>>(emptyList())
    val following: StateFlow<List<Follow>> = _following.asStateFlow()

    private val _isLoadingFollows = MutableStateFlow(false)
    val isLoadingFollows: StateFlow<Boolean> = _isLoadingFollows.asStateFlow()

    private val _selectedTab = MutableStateFlow(CommunityTab.FEED)
    val selectedTab: StateFlow<CommunityTab> = _selectedTab.asStateFlow()

    private var currentUserId: String? = null

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    fun initialize(userId: String) {
        if (!CommunityFeatureFlag.ENABLED) {
            return
        }

        currentUserId = userId
        loadCurrentUserProfile()
    }

    private fun loadCurrentUserProfile() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            CommunityRepository.getOrCreateCommunityProfile(userId).onSuccess { profile ->
                _currentUserProfile.value = profile

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "✅ Loaded current user profile: ${profile.userId}")
                }
            }.onFailure { error ->
                Log.e(TAG, "❌ Error loading current user profile: ${error.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TAB NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════

    fun selectTab(tab: CommunityTab) {
        _selectedTab.value = tab
    }

    // ═══════════════════════════════════════════════════════════════════════
    // USER PROFILE
    // ═══════════════════════════════════════════════════════════════════════

    fun loadUserProfile(userId: String) {
        _userProfileState.value = UserProfileState(isLoading = true)

        viewModelScope.launch {
            try {
                // Load profile
                val profileResult = CommunityRepository.getCommunityProfile(userId)

                profileResult.onSuccess { profile ->
                    if (profile == null) {
                        _userProfileState.value = _userProfileState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                        return@onSuccess
                    }

                    // Load stats including follow status
                    val currentId = currentUserId ?: ""
                    val stats = if (currentId.isNotEmpty()) {
                        CommunityRepository.getUserCommunityStats(currentId, userId).getOrNull()
                    } else {
                        null
                    }

                    val isFollowing = stats?.isFollowing ?: false

                    // Load posts count
                    val postsCount = FeedRepository.getPostsCountForUser(userId).getOrNull() ?: 0

                    // Fetch profile image URL from user_profiles table
                    val profileImageUrl = UserProfileRepository.getProfileImageUrl(userId)
                    if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                        Log.d(TAG, "🖼️ Profile image URL for $userId: $profileImageUrl")
                    }

                    _userProfileState.value = UserProfileState(
                        profile = CommunityUserProfile(
                            userId = profile.userId,
                            name = profile.displayName ?: "User",
                            profileImageUrl = profileImageUrl,
                            bio = profile.bio,
                            isPublic = profile.isPublic,
                            stats = UserCommunityStats(
                                followersCount = stats?.followersCount ?: 0,
                                followingCount = stats?.followingCount ?: 0,
                                postsCount = postsCount,
                                isFollowing = isFollowing
                            )
                        ),
                        isLoading = false,
                        isCurrentUser = currentUserId == userId
                    )

                    // Load posts if profile is public or current user
                    if (profile.isPublic || currentUserId == userId || isFollowing) {
                        loadUserPosts(userId)
                    }

                    if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                        Log.d(TAG, "✅ Loaded user profile: $userId")
                    }
                }.onFailure { error ->
                    _userProfileState.value = _userProfileState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            } catch (e: Exception) {
                _userProfileState.value = _userProfileState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadUserPosts(userId: String) {
        _userProfileState.value = _userProfileState.value.copy(isLoadingPosts = true)

        viewModelScope.launch {
            FeedRepository.getPostsForUser(userId).onSuccess { posts ->
                _userProfileState.value = _userProfileState.value.copy(
                    posts = posts,
                    isLoadingPosts = false
                )
            }.onFailure {
                _userProfileState.value = _userProfileState.value.copy(isLoadingPosts = false)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FOLLOWS
    // ═══════════════════════════════════════════════════════════════════════

    fun followUser(targetUserId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            CommunityRepository.followUser(userId, targetUserId).onSuccess {
                Log.d(TAG, "✅ Followed user: $targetUserId")

                // Update profile state if viewing this user
                _userProfileState.value.profile?.let { profile ->
                    if (profile.userId == targetUserId) {
                        val updatedStats = profile.stats.copy(
                            isFollowing = true,
                            followersCount = profile.stats.followersCount + 1
                        )
                        _userProfileState.value = _userProfileState.value.copy(
                            profile = profile.copy(stats = updatedStats)
                        )
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "❌ Error following user: ${error.message}")
            }
        }
    }

    fun unfollowUser(targetUserId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            CommunityRepository.unfollowUser(userId, targetUserId).onSuccess {
                Log.d(TAG, "✅ Unfollowed user: $targetUserId")

                // Update profile state if viewing this user
                _userProfileState.value.profile?.let { profile ->
                    if (profile.userId == targetUserId) {
                        val updatedStats = profile.stats.copy(
                            isFollowing = false,
                            followersCount = (profile.stats.followersCount - 1).coerceAtLeast(0)
                        )
                        _userProfileState.value = _userProfileState.value.copy(
                            profile = profile.copy(stats = updatedStats)
                        )
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "❌ Error unfollowing user: ${error.message}")
            }
        }
    }

    fun loadFollowers(userId: String) {
        _isLoadingFollows.value = true

        viewModelScope.launch {
            CommunityRepository.getFollowers(userId).onSuccess { list ->
                _followers.value = list
                _isLoadingFollows.value = false

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "✅ Loaded ${list.size} followers")
                }
            }.onFailure {
                _isLoadingFollows.value = false
            }
        }
    }

    fun loadFollowing(userId: String) {
        _isLoadingFollows.value = true

        viewModelScope.launch {
            CommunityRepository.getFollowing(userId).onSuccess { list ->
                _following.value = list
                _isLoadingFollows.value = false

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "✅ Loaded ${list.size} following")
                }
            }.onFailure {
                _isLoadingFollows.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROFILE SETTINGS
    // ═══════════════════════════════════════════════════════════════════════

    fun updateBio(bio: String) {
        val userId = currentUserId ?: return
        val currentProfile = _currentUserProfile.value ?: return

        viewModelScope.launch {
            val updatedProfile = currentProfile.copy(bio = bio)
            CommunityRepository.updateCommunityProfile(updatedProfile).onSuccess {
                _currentUserProfile.value = updatedProfile
                Log.d(TAG, "✅ Bio updated")
            }.onFailure { error ->
                Log.e(TAG, "❌ Error updating bio: ${error.message}")
            }
        }
    }

    fun setProfilePublic(isPublic: Boolean) {
        val userId = currentUserId ?: return
        val currentProfile = _currentUserProfile.value ?: return

        viewModelScope.launch {
            val updatedProfile = currentProfile.copy(isPublic = isPublic)
            CommunityRepository.updateCommunityProfile(updatedProfile).onSuccess {
                _currentUserProfile.value = updatedProfile
                Log.d(TAG, "✅ Profile visibility updated: ${if (isPublic) "public" else "private"}")
            }.onFailure { error ->
                Log.e(TAG, "❌ Error updating profile visibility: ${error.message}")
            }
        }
    }

    fun setLeaderboardOptIn(optIn: Boolean) {
        val userId = currentUserId ?: return
        val currentProfile = _currentUserProfile.value ?: return

        viewModelScope.launch {
            val updatedProfile = currentProfile.copy(showInLeaderboard = optIn)
            CommunityRepository.updateCommunityProfile(updatedProfile).onSuccess {
                _currentUserProfile.value = updatedProfile
                Log.d(TAG, "✅ Leaderboard opt-in updated: $optIn")
            }.onFailure { error ->
                Log.e(TAG, "❌ Error updating leaderboard opt-in: ${error.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    fun clearUserProfile() {
        _userProfileState.value = UserProfileState()
    }

    fun clearFollows() {
        _followers.value = emptyList()
        _following.value = emptyList()
    }
}

/**
 * Community navigation tabs
 */
enum class CommunityTab {
    FEED,
    LEADERBOARD,
    CHALLENGES,
    ACTIVITY
}
