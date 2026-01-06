package com.example.menotracker.community.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// COMMUNITY PROFILE
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class CommunityProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("show_in_leaderboard") val showInLeaderboard: Boolean = true,
    @SerialName("allow_follow_requests") val allowFollowRequests: Boolean = true,
    @SerialName("auto_share_workouts") val autoShareWorkouts: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// FOLLOW SYSTEM
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class FollowStatus {
    @SerialName("active") ACTIVE,
    @SerialName("pending") PENDING,
    @SerialName("blocked") BLOCKED
}

@Serializable
data class Follow(
    val id: String,
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    val status: FollowStatus = FollowStatus.ACTIVE,
    @SerialName("created_at") val createdAt: String? = null
) {
    // Placeholder properties when user data isn't joined
    val userDisplayName: String? get() = null
    val userAvatarUrl: String? get() = null
}

@Serializable
data class FollowWithUser(
    val id: String,
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    val status: FollowStatus = FollowStatus.ACTIVE,
    @SerialName("created_at") val createdAt: String? = null,
    // Joined user data
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null
) {
    // Alias properties for consistent UI access
    val userDisplayName: String? get() = userName
    val userAvatarUrl: String? get() = userAvatar
}

// ═══════════════════════════════════════════════════════════════════════════
// POSTS
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class PostVisibility {
    @SerialName("public") PUBLIC,
    @SerialName("followers") FOLLOWERS,
    @SerialName("private") PRIVATE
}

@Serializable
data class CommunityPost(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("workout_history_id") val workoutHistoryId: String? = null,
    @SerialName("workout_name") val workoutName: String,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("total_sets") val totalSets: Int = 0,
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("prs_achieved") val prsAchieved: Int = 0,
    @SerialName("pr_exercises") val prExercises: List<String>? = null,
    val caption: String? = null,
    @SerialName("image_urls") val imageUrls: List<String>? = null,
    @SerialName("video_urls") val videoUrls: List<String>? = null,
    val visibility: PostVisibility = PostVisibility.FOLLOWERS,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Post with joined user data for feed display
 */
@Serializable
data class FeedPost(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null,
    @SerialName("workout_name") val workoutName: String,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("total_sets") val totalSets: Int = 0,
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("prs_achieved") val prsAchieved: Int = 0,
    @SerialName("pr_exercises") val prExercises: List<String>? = null,
    val caption: String? = null,
    @SerialName("image_urls") val imageUrls: List<String>? = null,
    @SerialName("video_urls") val videoUrls: List<String>? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_liked") val isLiked: Boolean = false
) {
    // Alias properties for consistent UI access
    val userDisplayName: String? get() = userName
    val userAvatarUrl: String? get() = userAvatar

    // Combined media for carousel
    val hasMedia: Boolean get() = !imageUrls.isNullOrEmpty() || !videoUrls.isNullOrEmpty()
}

// ═══════════════════════════════════════════════════════════════════════════
// LIKES & COMMENTS
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class CommunityLike(
    val id: String = "",
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CommunityComment(
    val id: String = "",
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    // Joined user data
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null
) {
    // Alias properties for consistent UI access
    val userDisplayName: String? get() = userName
    val userAvatarUrl: String? get() = userAvatar
}

// ═══════════════════════════════════════════════════════════════════════════
// LEADERBOARD
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class LeaderboardEntry(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("pr_weight_kg") val prWeightKg: Double? = null,
    @SerialName("pr_reps") val prReps: Int? = null,
    @SerialName("estimated_1rm_kg") val estimated1rmKg: Double? = null,
    @SerialName("user_bodyweight_kg") val userBodyweightKg: Double? = null,
    @SerialName("wilks_score") val wilksScore: Double? = null,
    @SerialName("dots_score") val dotsScore: Double? = null,
    @SerialName("achieved_at") val achievedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    // Joined user data
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null,
    // UI state (calculated)
    var rank: Int = 0,
    @SerialName("is_current_user") val isCurrentUser: Boolean = false,
    @SerialName("is_pr") val isPr: Boolean = false
) {
    // Alias properties for consistent UI access
    val userDisplayName: String? get() = userName
    val userAvatarUrl: String? get() = userAvatar
}

/**
 * Predefined exercises for leaderboards
 */
object LeaderboardExercises {
    val EXERCISES = listOf(
        LeaderboardExercise("bench_press", "Bench Press"),
        LeaderboardExercise("squat", "Squat"),
        LeaderboardExercise("deadlift", "Deadlift"),
        LeaderboardExercise("overhead_press", "Overhead Press"),
        LeaderboardExercise("barbell_row", "Barbell Row"),
        LeaderboardExercise("weighted_pullup", "Weighted Pull-Up")
    )

    fun getExerciseName(id: String): String {
        return EXERCISES.find { it.id == id }?.name ?: id
    }
}

data class LeaderboardExercise(
    val id: String,
    val name: String
)

// ═══════════════════════════════════════════════════════════════════════════
// USER COMMUNITY STATS
// ═══════════════════════════════════════════════════════════════════════════

data class UserCommunityStats(
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val isPendingFollow: Boolean = false
)

data class CommunityUserProfile(
    val userId: String,
    val name: String,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val isPublic: Boolean = true,
    val stats: UserCommunityStats = UserCommunityStats(),
    // Training stats (from user_training_summary)
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val currentStreak: Int = 0,
    val totalPRs: Int = 0
) {
    // Alias properties for consistent UI access
    val displayName: String? get() = name
    val avatarUrl: String? get() = profileImageUrl
    val followersCount: Int get() = stats.followersCount
    val followingCount: Int get() = stats.followingCount
    val postsCount: Int get() = stats.postsCount
    val isFollowing: Boolean get() = stats.isFollowing
}

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE WRAPPERS
// ═══════════════════════════════════════════════════════════════════════════

data class FeedState(
    val posts: List<FeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val currentTab: FeedTab = FeedTab.FOLLOWING
)

enum class FeedTab {
    FOLLOWING,
    DISCOVER
}

data class LeaderboardState(
    val entries: List<LeaderboardEntry> = emptyList(),
    val selectedExercise: String = "bench_press",
    val friendsOnly: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserRank: Int? = null
)

data class PostDetailState(
    val post: FeedPost? = null,
    val comments: List<CommunityComment> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingComments: Boolean = false,
    val error: String? = null
)

data class UserProfileState(
    val profile: CommunityUserProfile? = null,
    val posts: List<FeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingPosts: Boolean = false,
    val isCurrentUser: Boolean = false,
    val error: String? = null
)