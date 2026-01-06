package com.example.menotracker.community.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// CHALLENGES
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class ChallengeType {
    @SerialName("max_out_friday") MAX_OUT_FRIDAY,
    @SerialName("volume") VOLUME,
    @SerialName("streak") STREAK,
    @SerialName("custom") CUSTOM
}

@Serializable
enum class ChallengeStatus {
    @SerialName("upcoming") UPCOMING,
    @SerialName("active") ACTIVE,
    @SerialName("completed") COMPLETED,
    @SerialName("cancelled") CANCELLED
}

@Serializable
data class Challenge(
    val id: String = "",
    val title: String,
    val description: String? = null,
    @SerialName("challenge_type") val challengeType: ChallengeType,

    // For Max Out Friday
    @SerialName("exercise_id") val exerciseId: String? = null,
    @SerialName("exercise_name") val exerciseName: String? = null,

    // For Volume challenges
    @SerialName("target_volume_kg") val targetVolumeKg: Double? = null,

    // For Streak challenges
    @SerialName("target_streak_days") val targetStreakDays: Int? = null,

    // Timing
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    @SerialName("recurrence_pattern") val recurrencePattern: String? = null,

    // Status
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,
    @SerialName("participants_count") val participantsCount: Int = 0,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// CHALLENGE ENTRIES
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class ChallengeEntry(
    val id: String = "",
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("user_id") val userId: String,

    // Entry data
    @SerialName("value_kg") val valueKg: Double? = null,
    @SerialName("value_reps") val valueReps: Int? = null,
    @SerialName("streak_count") val streakCount: Int? = null,

    // Proof
    @SerialName("workout_history_id") val workoutHistoryId: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,

    // Status
    val rank: Int? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_pr") val isPr: Boolean = false,

    @SerialName("submitted_at") val submittedAt: String? = null,
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
// MAX OUT FRIDAY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Response from get_current_max_out_friday() RPC
 */
@Serializable
data class MaxOutFridayInfo(
    val id: String,
    val title: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("participants_count") val participantsCount: Int,
    @SerialName("user_entry_kg") val userEntryKg: Double? = null,
    @SerialName("user_rank") val userRank: Int? = null
)

/**
 * Exercise rotation for Max Out Friday
 */
object MaxOutFridayRotation {
    val EXERCISES = listOf(
        "bench_press" to "Bench Press",
        "squat" to "Squat",
        "deadlift" to "Deadlift",
        "overhead_press" to "Overhead Press",
        "barbell_row" to "Barbell Row",
        "weighted_pullup" to "Weighted Pull-Up"
    )

    /**
     * Get the exercise for a given week number (1-indexed from start)
     */
    fun getExerciseForWeek(weekNumber: Int): Pair<String, String> {
        val index = (weekNumber - 1) % EXERCISES.size
        return EXERCISES[index]
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE WRAPPERS
// ═══════════════════════════════════════════════════════════════════════════

data class ChallengesState(
    val activeChallenges: List<Challenge> = emptyList(),
    val upcomingChallenges: List<Challenge> = emptyList(),
    val completedChallenges: List<Challenge> = emptyList(),
    val maxOutFriday: MaxOutFridayInfo? = null,
    val currentChallenge: Challenge? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChallengeDetailState(
    val challenge: Challenge? = null,
    val entries: List<ChallengeEntry> = emptyList(),
    val userEntry: ChallengeEntry? = null,
    val isLoading: Boolean = false,
    val isLoadingEntries: Boolean = false,
    val isSubmitting: Boolean = false,
    val hasParticipated: Boolean = false,
    val error: String? = null
)

data class MaxOutFridayState(
    val info: MaxOutFridayInfo? = null,
    val topEntries: List<ChallengeEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val timeRemaining: String = "",  // "2d 14h 32m"
    val timeRemainingMillis: Long = 0,
    val nextExercise: Pair<String, String>? = null,  // Preview of next week's exercise
    val previousWinners: List<PreviousWinner> = emptyList()
)

// ═══════════════════════════════════════════════════════════════════════════
// EXTENDED MAX OUT FRIDAY
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class PreviousWinner(
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("winner_id") val winnerId: String,
    @SerialName("winner_name") val winnerName: String,
    @SerialName("winner_avatar") val winnerAvatar: String? = null,
    @SerialName("winning_weight_kg") val winningWeightKg: Double,
    @SerialName("week_date") val weekDate: String
)

@Serializable
data class MaxOutFridayHistory(
    val id: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("week_date") val weekDate: String,
    @SerialName("user_entry_kg") val userEntryKg: Double? = null,
    @SerialName("user_rank") val userRank: Int? = null,
    @SerialName("total_participants") val totalParticipants: Int,
    @SerialName("winner_name") val winnerName: String? = null,
    @SerialName("winning_weight_kg") val winningWeightKg: Double? = null,
    @SerialName("was_pr") val wasPr: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════
// USER-CREATED CHALLENGES
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class CreateChallengeRequest(
    val title: String,
    val description: String? = null,
    @SerialName("challenge_type") val challengeType: ChallengeType,
    // For specific exercise challenges
    @SerialName("exercise_id") val exerciseId: String? = null,
    @SerialName("exercise_name") val exerciseName: String? = null,
    // For volume challenges
    @SerialName("target_volume_kg") val targetVolumeKg: Double? = null,
    // For streak challenges
    @SerialName("target_streak_days") val targetStreakDays: Int? = null,
    // Timing
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    // Visibility
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("invited_user_ids") val invitedUserIds: List<String>? = null
)

@Serializable
data class ChallengeInvite(
    val id: String = "",
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("inviter_id") val inviterId: String,
    @SerialName("invitee_id") val inviteeId: String,
    val status: InviteStatus = InviteStatus.PENDING,
    @SerialName("created_at") val createdAt: String? = null,
    // Joined data
    @SerialName("challenge_title") val challengeTitle: String? = null,
    @SerialName("inviter_name") val inviterName: String? = null
)

@Serializable
enum class InviteStatus {
    @SerialName("pending") PENDING,
    @SerialName("accepted") ACCEPTED,
    @SerialName("declined") DECLINED
}

// ═══════════════════════════════════════════════════════════════════════════
// CHALLENGE VIDEO PROOF
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class ChallengeVideo(
    val id: String = "",
    @SerialName("entry_id") val entryId: String,
    @SerialName("video_url") val videoUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE EXTENSIONS
// ═══════════════════════════════════════════════════════════════════════════

data class CreateChallengeState(
    val title: String = "",
    val description: String = "",
    val challengeType: ChallengeType = ChallengeType.CUSTOM,
    val selectedExercise: Pair<String, String>? = null,  // id, name
    val targetVolume: Double? = null,
    val targetStreakDays: Int? = null,
    val startDate: String = "",
    val endDate: String = "",
    val isPublic: Boolean = true,
    val invitedUsers: List<String> = emptyList(),
    val isCreating: Boolean = false,
    val error: String? = null,
    val createdChallengeId: String? = null  // Set on success
)

data class ChallengeInvitesState(
    val pendingInvites: List<ChallengeInvite> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)