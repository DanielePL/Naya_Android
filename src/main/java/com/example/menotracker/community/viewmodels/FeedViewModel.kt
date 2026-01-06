package com.example.menotracker.community.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.community.data.FeedRepository
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Community Feed functionality.
 * Handles feed loading, pagination, likes, and comments.
 */
class FeedViewModel : ViewModel() {
    companion object {
        private const val TAG = "FeedViewModel"
        private const val PAGE_SIZE = 20
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _feedState = MutableStateFlow(FeedState())
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    private val _postDetailState = MutableStateFlow(PostDetailState())
    val postDetailState: StateFlow<PostDetailState> = _postDetailState.asStateFlow()

    private var currentUserId: String? = null
    private var currentOffset = 0

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    fun initialize(userId: String) {
        currentUserId = userId
        loadFeed(refresh = true)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FEED LOADING
    // ═══════════════════════════════════════════════════════════════════════

    fun loadFeed(refresh: Boolean = false) {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.FEED_ENABLED) {
            return
        }

        if (refresh) {
            currentOffset = 0
            _feedState.value = _feedState.value.copy(isLoading = true, error = null)
        } else {
            if (_feedState.value.isLoadingMore || !_feedState.value.hasMore) return
            _feedState.value = _feedState.value.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            try {
                val result = when (_feedState.value.currentTab) {
                    FeedTab.FOLLOWING -> FeedRepository.getFeed(PAGE_SIZE, currentOffset)
                    FeedTab.DISCOVER -> FeedRepository.getDiscoverFeed(PAGE_SIZE, currentOffset)
                }

                result.onSuccess { posts ->
                    val allPosts = if (refresh) {
                        posts
                    } else {
                        _feedState.value.posts + posts
                    }

                    currentOffset = allPosts.size

                    _feedState.value = _feedState.value.copy(
                        posts = allPosts,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        hasMore = posts.size >= PAGE_SIZE
                    )

                    if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                        Log.d(TAG, "✅ Loaded ${posts.size} posts, total: ${allPosts.size}")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error loading feed: ${error.message}")
                    _feedState.value = _feedState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = error.message
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception loading feed: ${e.message}")
                _feedState.value = _feedState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message
                )
            }
        }
    }

    fun loadMore() {
        loadFeed(refresh = false)
    }

    fun refresh() {
        loadFeed(refresh = true)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TAB SWITCHING
    // ═══════════════════════════════════════════════════════════════════════

    fun switchTab(tab: FeedTab) {
        if (_feedState.value.currentTab != tab) {
            _feedState.value = _feedState.value.copy(
                currentTab = tab,
                posts = emptyList(),
                hasMore = true
            )
            currentOffset = 0
            loadFeed(refresh = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIKES
    // ═══════════════════════════════════════════════════════════════════════

    fun toggleLike(postId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            // Find post and current like status
            val post = _feedState.value.posts.find { it.id == postId } ?: return@launch
            val isCurrentlyLiked = post.isLiked

            // Optimistic update
            updatePostInFeed(postId) { currentPost ->
                currentPost.copy(
                    isLiked = !isCurrentlyLiked,
                    likesCount = if (isCurrentlyLiked) currentPost.likesCount - 1 else currentPost.likesCount + 1
                )
            }

            // Make API call
            val result = if (isCurrentlyLiked) {
                FeedRepository.unlikePost(postId, userId)
            } else {
                FeedRepository.likePost(postId, userId)
            }

            // Revert on failure
            result.onFailure {
                Log.e(TAG, "❌ Error toggling like: ${it.message}")
                updatePostInFeed(postId) { currentPost ->
                    currentPost.copy(
                        isLiked = isCurrentlyLiked,
                        likesCount = if (isCurrentlyLiked) currentPost.likesCount + 1 else currentPost.likesCount - 1
                    )
                }
            }
        }
    }

    private fun updatePostInFeed(postId: String, transform: (FeedPost) -> FeedPost) {
        _feedState.value = _feedState.value.copy(
            posts = _feedState.value.posts.map { post ->
                if (post.id == postId) transform(post) else post
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST DETAIL
    // ═══════════════════════════════════════════════════════════════════════

    fun loadPostDetail(postId: String) {
        _postDetailState.value = PostDetailState(isLoading = true)

        viewModelScope.launch {
            try {
                // Load post
                val postResult = FeedRepository.getPost(postId)
                postResult.onSuccess { post ->
                    _postDetailState.value = _postDetailState.value.copy(
                        post = post,
                        isLoading = false
                    )

                    // Load comments
                    loadComments(postId)
                }.onFailure { error ->
                    _postDetailState.value = _postDetailState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            } catch (e: Exception) {
                _postDetailState.value = _postDetailState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadComments(postId: String) {
        _postDetailState.value = _postDetailState.value.copy(isLoadingComments = true)

        viewModelScope.launch {
            FeedRepository.getComments(postId).onSuccess { comments ->
                _postDetailState.value = _postDetailState.value.copy(
                    comments = comments,
                    isLoadingComments = false
                )
            }.onFailure { error ->
                Log.e(TAG, "❌ Error loading comments: ${error.message}")
                _postDetailState.value = _postDetailState.value.copy(
                    isLoadingComments = false
                )
            }
        }
    }

    fun addComment(postId: String, content: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            FeedRepository.addComment(postId, userId, content).onSuccess { comment ->
                // Add to comments list
                _postDetailState.value = _postDetailState.value.copy(
                    comments = _postDetailState.value.comments + comment
                )

                // Update comment count in feed
                updatePostInFeed(postId) { post ->
                    post.copy(commentsCount = post.commentsCount + 1)
                }

                Log.d(TAG, "✅ Comment added")
            }.onFailure { error ->
                Log.e(TAG, "❌ Error adding comment: ${error.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST CREATION
    // ═══════════════════════════════════════════════════════════════════════

    fun shareWorkout(
        workoutHistoryId: String?,
        workoutName: String,
        totalVolumeKg: Double,
        totalSets: Int,
        totalReps: Int,
        durationMinutes: Int?,
        prsAchieved: Int,
        prExercises: List<String>?,
        caption: String?,
        visibility: PostVisibility = PostVisibility.FOLLOWERS,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            FeedRepository.createPost(
                userId = userId,
                workoutHistoryId = workoutHistoryId,
                workoutName = workoutName,
                totalVolumeKg = totalVolumeKg,
                totalSets = totalSets,
                totalReps = totalReps,
                durationMinutes = durationMinutes,
                prsAchieved = prsAchieved,
                prExercises = prExercises,
                caption = caption,
                visibility = visibility
            ).onSuccess { post ->
                Log.d(TAG, "✅ Workout shared: ${post.id}")
                // Refresh feed to show new post
                refresh()
                onSuccess()
            }.onFailure { error ->
                Log.e(TAG, "❌ Error sharing workout: ${error.message}")
                onError(error.message ?: "Unknown error")
            }
        }
    }

    fun deletePost(postId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            FeedRepository.deletePost(postId, userId).onSuccess {
                // Remove from feed
                _feedState.value = _feedState.value.copy(
                    posts = _feedState.value.posts.filter { it.id != postId }
                )
                Log.d(TAG, "✅ Post deleted")
            }.onFailure { error ->
                Log.e(TAG, "❌ Error deleting post: ${error.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    fun clearError() {
        _feedState.value = _feedState.value.copy(error = null)
    }

    fun clearPostDetail() {
        _postDetailState.value = PostDetailState()
    }
}
