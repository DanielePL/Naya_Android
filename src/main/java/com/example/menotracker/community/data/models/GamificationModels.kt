package com.example.menotracker.community.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// BADGES & ACHIEVEMENTS
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class BadgeCategory {
    @SerialName("workout") WORKOUT,           // Workout milestones
    @SerialName("strength") STRENGTH,         // PR achievements
    @SerialName("consistency") CONSISTENCY,   // Streaks
    @SerialName("community") COMMUNITY,       // Social engagement
    @SerialName("challenge") CHALLENGE,       // Challenge participation
    @SerialName("special") SPECIAL            // Special/seasonal badges
}

@Serializable
enum class BadgeRarity {
    @SerialName("common") COMMON,
    @SerialName("rare") RARE,
    @SerialName("epic") EPIC,
    @SerialName("legendary") LEGENDARY
}

@Serializable
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("icon_name") val iconName: String,
    val category: BadgeCategory,
    val rarity: BadgeRarity,
    @SerialName("xp_reward") val xpReward: Int = 0,
    // Requirement for unlocking
    @SerialName("requirement_type") val requirementType: String? = null,  // e.g., "workout_count", "pr_count", "streak_days"
    @SerialName("requirement_value") val requirementValue: Int? = null,   // e.g., 100 workouts
    @SerialName("is_hidden") val isHidden: Boolean = false,              // Hidden until unlocked
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UserBadge(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("badge_id") val badgeId: String,
    @SerialName("earned_at") val earnedAt: String? = null,
    // Joined badge data
    @SerialName("badge_name") val badgeName: String? = null,
    @SerialName("badge_icon") val badgeIcon: String? = null,
    @SerialName("badge_rarity") val badgeRarity: BadgeRarity? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// LEVEL SYSTEM
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class UserLevel(
    @SerialName("user_id") val userId: String,
    val level: Int = 1,
    @SerialName("current_xp") val currentXp: Int = 0,
    @SerialName("total_xp") val totalXp: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    // XP needed to level up (exponential curve)
    val xpForNextLevel: Int get() = calculateXpForLevel(level + 1)
    val xpProgress: Float get() {
        val currentLevelXp = calculateXpForLevel(level)
        val nextLevelXp = calculateXpForLevel(level + 1)
        val xpInCurrentLevel = totalXp - currentLevelXp
        val xpNeededForLevel = nextLevelXp - currentLevelXp
        return (xpInCurrentLevel.toFloat() / xpNeededForLevel).coerceIn(0f, 1f)
    }

    companion object {
        fun calculateXpForLevel(level: Int): Int {
            // Simple quadratic curve: level 2 = 100xp, level 10 = 1000xp, level 50 = 5000xp
            return (level * level * 10)
        }

        fun getLevelTitle(level: Int): String {
            return when {
                level >= 100 -> "Titan"
                level >= 75 -> "Legend"
                level >= 50 -> "Champion"
                level >= 35 -> "Master"
                level >= 25 -> "Expert"
                level >= 15 -> "Veteran"
                level >= 10 -> "Dedicated"
                level >= 5 -> "Regular"
                else -> "Newcomer"
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// XP SOURCES
// ═══════════════════════════════════════════════════════════════════════════

object XpRewards {
    const val COMPLETE_WORKOUT = 50
    const val ACHIEVE_PR = 100
    const val SHARE_WORKOUT = 25
    const val RECEIVE_LIKE = 5
    const val GIVE_LIKE = 2
    const val WRITE_COMMENT = 10
    const val STREAK_DAY_BONUS = 10          // Per day of streak
    const val CHALLENGE_PARTICIPATION = 50
    const val CHALLENGE_TOP_10 = 100
    const val CHALLENGE_TOP_3 = 200
    const val CHALLENGE_WINNER = 500
    const val MAX_OUT_FRIDAY_PR = 150        // Set a PR on Max Out Friday
    const val FIRST_FOLLOWER = 50
    const val MILESTONE_FOLLOWER_10 = 100
    const val MILESTONE_FOLLOWER_50 = 250
    const val MILESTONE_FOLLOWER_100 = 500
}

@Serializable
data class XpTransaction(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    val amount: Int,
    val source: String,  // e.g., "workout", "pr", "challenge", "social"
    @SerialName("source_id") val sourceId: String? = null,  // ID of related entity
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// STREAKS
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class UserStreak(
    @SerialName("user_id") val userId: String,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("last_workout_date") val lastWorkoutDate: String? = null,
    @SerialName("streak_protected_until") val streakProtectedUntil: String? = null,  // Freeze protection
    @SerialName("freeze_tokens") val freezeTokens: Int = 0,                          // Earned or purchased
    @SerialName("updated_at") val updatedAt: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// ACHIEVEMENTS (Complex multi-step goals)
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("icon_name") val iconName: String,
    val category: BadgeCategory,
    val tiers: List<AchievementTier> = emptyList(),
    @SerialName("is_hidden") val isHidden: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AchievementTier(
    val tier: Int,  // 1, 2, 3 (Bronze, Silver, Gold)
    val name: String,  // "Bronze", "Silver", "Gold"
    @SerialName("requirement_value") val requirementValue: Int,
    @SerialName("xp_reward") val xpReward: Int
)

@Serializable
data class UserAchievementProgress(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("achievement_id") val achievementId: String,
    @SerialName("current_value") val currentValue: Int = 0,
    @SerialName("current_tier") val currentTier: Int = 0,  // 0 = not started
    @SerialName("completed_at") val completedAt: String? = null,  // When highest tier reached
    @SerialName("updated_at") val updatedAt: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// ACTIVITY FEED
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class ActivityType {
    @SerialName("workout_shared") WORKOUT_SHARED,
    @SerialName("pr_achieved") PR_ACHIEVED,
    @SerialName("challenge_joined") CHALLENGE_JOINED,
    @SerialName("challenge_won") CHALLENGE_WON,
    @SerialName("badge_earned") BADGE_EARNED,
    @SerialName("level_up") LEVEL_UP,
    @SerialName("follow") FOLLOW,
    @SerialName("like") LIKE,
    @SerialName("comment") COMMENT,
    @SerialName("streak_milestone") STREAK_MILESTONE
}

@Serializable
data class ActivityItem(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("activity_type") val activityType: ActivityType,
    @SerialName("target_user_id") val targetUserId: String? = null,  // For follows, likes
    @SerialName("target_id") val targetId: String? = null,           // Post ID, Challenge ID, etc.
    val metadata: Map<String, String>? = null,                       // Additional data
    @SerialName("created_at") val createdAt: String? = null,
    // Joined user data
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════════════

data class GamificationState(
    val userLevel: UserLevel? = null,
    val badges: List<UserBadge> = emptyList(),
    val recentBadge: Badge? = null,  // For showing unlock animation
    val streak: UserStreak? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ActivityFeedState(
    val activities: List<ActivityItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// PREDEFINED BADGES
// ═══════════════════════════════════════════════════════════════════════════

object PredefinedBadges {
    // Workout milestones
    val FIRST_WORKOUT = Badge(
        id = "first_workout",
        name = "First Steps",
        description = "Complete your first workout",
        iconName = "fitness_center",
        category = BadgeCategory.WORKOUT,
        rarity = BadgeRarity.COMMON,
        xpReward = 100,
        requirementType = "workout_count",
        requirementValue = 1
    )

    val WORKOUT_10 = Badge(
        id = "workout_10",
        name = "Getting Started",
        description = "Complete 10 workouts",
        iconName = "fitness_center",
        category = BadgeCategory.WORKOUT,
        rarity = BadgeRarity.COMMON,
        xpReward = 200,
        requirementType = "workout_count",
        requirementValue = 10
    )

    val WORKOUT_100 = Badge(
        id = "workout_100",
        name = "Century Club",
        description = "Complete 100 workouts",
        iconName = "military_tech",
        category = BadgeCategory.WORKOUT,
        rarity = BadgeRarity.RARE,
        xpReward = 500,
        requirementType = "workout_count",
        requirementValue = 100
    )

    val WORKOUT_500 = Badge(
        id = "workout_500",
        name = "Iron Veteran",
        description = "Complete 500 workouts",
        iconName = "workspace_premium",
        category = BadgeCategory.WORKOUT,
        rarity = BadgeRarity.EPIC,
        xpReward = 1000,
        requirementType = "workout_count",
        requirementValue = 500
    )

    // Strength milestones
    val FIRST_PR = Badge(
        id = "first_pr",
        name = "PR Hunter",
        description = "Achieve your first personal record",
        iconName = "emoji_events",
        category = BadgeCategory.STRENGTH,
        rarity = BadgeRarity.COMMON,
        xpReward = 100,
        requirementType = "pr_count",
        requirementValue = 1
    )

    val PR_10 = Badge(
        id = "pr_10",
        name = "Progression",
        description = "Achieve 10 personal records",
        iconName = "trending_up",
        category = BadgeCategory.STRENGTH,
        rarity = BadgeRarity.RARE,
        xpReward = 300,
        requirementType = "pr_count",
        requirementValue = 10
    )

    val PR_50 = Badge(
        id = "pr_50",
        name = "Record Breaker",
        description = "Achieve 50 personal records",
        iconName = "star",
        category = BadgeCategory.STRENGTH,
        rarity = BadgeRarity.EPIC,
        xpReward = 750,
        requirementType = "pr_count",
        requirementValue = 50
    )

    // Streak milestones
    val STREAK_7 = Badge(
        id = "streak_7",
        name = "Week Warrior",
        description = "Maintain a 7-day workout streak",
        iconName = "local_fire_department",
        category = BadgeCategory.CONSISTENCY,
        rarity = BadgeRarity.COMMON,
        xpReward = 150,
        requirementType = "streak_days",
        requirementValue = 7
    )

    val STREAK_30 = Badge(
        id = "streak_30",
        name = "Monthly Grind",
        description = "Maintain a 30-day workout streak",
        iconName = "whatshot",
        category = BadgeCategory.CONSISTENCY,
        rarity = BadgeRarity.RARE,
        xpReward = 500,
        requirementType = "streak_days",
        requirementValue = 30
    )

    val STREAK_100 = Badge(
        id = "streak_100",
        name = "Unstoppable",
        description = "Maintain a 100-day workout streak",
        iconName = "bolt",
        category = BadgeCategory.CONSISTENCY,
        rarity = BadgeRarity.LEGENDARY,
        xpReward = 2000,
        requirementType = "streak_days",
        requirementValue = 100
    )

    // Community badges
    val FIRST_SHARE = Badge(
        id = "first_share",
        name = "Going Public",
        description = "Share your first workout with the community",
        iconName = "share",
        category = BadgeCategory.COMMUNITY,
        rarity = BadgeRarity.COMMON,
        xpReward = 50,
        requirementType = "share_count",
        requirementValue = 1
    )

    val FOLLOWER_10 = Badge(
        id = "follower_10",
        name = "Rising Star",
        description = "Gain 10 followers",
        iconName = "people",
        category = BadgeCategory.COMMUNITY,
        rarity = BadgeRarity.RARE,
        xpReward = 200,
        requirementType = "follower_count",
        requirementValue = 10
    )

    val FOLLOWER_100 = Badge(
        id = "follower_100",
        name = "Influencer",
        description = "Gain 100 followers",
        iconName = "groups",
        category = BadgeCategory.COMMUNITY,
        rarity = BadgeRarity.EPIC,
        xpReward = 500,
        requirementType = "follower_count",
        requirementValue = 100
    )

    // Challenge badges
    val CHALLENGE_FIRST = Badge(
        id = "challenge_first",
        name = "Challenger",
        description = "Participate in your first challenge",
        iconName = "emoji_events",
        category = BadgeCategory.CHALLENGE,
        rarity = BadgeRarity.COMMON,
        xpReward = 100,
        requirementType = "challenge_participation",
        requirementValue = 1
    )

    val CHALLENGE_WINNER = Badge(
        id = "challenge_winner",
        name = "Champion",
        description = "Win a community challenge",
        iconName = "military_tech",
        category = BadgeCategory.CHALLENGE,
        rarity = BadgeRarity.EPIC,
        xpReward = 500,
        requirementType = "challenge_wins",
        requirementValue = 1
    )

    val MAX_OUT_FRIDAY_5 = Badge(
        id = "max_out_friday_5",
        name = "Friday Regular",
        description = "Participate in 5 Max Out Friday challenges",
        iconName = "local_fire_department",
        category = BadgeCategory.CHALLENGE,
        rarity = BadgeRarity.RARE,
        xpReward = 300,
        requirementType = "max_out_friday_count",
        requirementValue = 5
    )

    val MAX_OUT_FRIDAY_PR = Badge(
        id = "max_out_friday_pr",
        name = "Friday PR",
        description = "Set a personal record on Max Out Friday",
        iconName = "star",
        category = BadgeCategory.CHALLENGE,
        rarity = BadgeRarity.RARE,
        xpReward = 250,
        requirementType = "max_out_friday_pr",
        requirementValue = 1
    )

    // All badges
    val ALL = listOf(
        FIRST_WORKOUT, WORKOUT_10, WORKOUT_100, WORKOUT_500,
        FIRST_PR, PR_10, PR_50,
        STREAK_7, STREAK_30, STREAK_100,
        FIRST_SHARE, FOLLOWER_10, FOLLOWER_100,
        CHALLENGE_FIRST, CHALLENGE_WINNER, MAX_OUT_FRIDAY_5, MAX_OUT_FRIDAY_PR
    )
}
