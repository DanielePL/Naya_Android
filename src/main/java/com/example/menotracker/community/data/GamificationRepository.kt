package com.example.menotracker.community.data

import android.util.Log
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.data.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repository for gamification features: XP, Levels, Badges, Streaks, Achievements.
 */
object GamificationRepository {
    private const val TAG = "GamificationRepository"

    // Table names
    private const val BADGES_TABLE = "community_badges"
    private const val USER_BADGES_TABLE = "community_user_badges"
    private const val USER_LEVELS_TABLE = "community_user_levels"
    private const val XP_TRANSACTIONS_TABLE = "community_xp_transactions"
    private const val USER_STREAKS_TABLE = "community_user_streaks"
    private const val ACTIVITY_FEED_TABLE = "community_activity_feed"

    // ═══════════════════════════════════════════════════════════════════════
    // USER LEVEL & XP
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get user's current level and XP
     */
    suspend fun getUserLevel(userId: String): Result<UserLevel?> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val results = SupabaseClient.client
                    .from(USER_LEVELS_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<UserLevel>()

                Result.success(results.firstOrNull() ?: UserLevel(userId = userId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user level: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Award XP to user (uses server function for level calculation)
     */
    suspend fun awardXp(
        userId: String,
        amount: Int,
        source: String,
        sourceId: String? = null,
        description: String? = null
    ): Result<UserLevel> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_amount", amount)
                    put("p_source", source)
                    sourceId?.let { put("p_source_id", it) }
                    description?.let { put("p_description", it) }
                }

                val result = SupabaseClient.client.postgrest
                    .rpc("award_xp", params)
                    .decodeSingle<UserLevel>()

                Log.d(TAG, "Awarded $amount XP to user $userId")
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error awarding XP: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get XP transaction history
     */
    suspend fun getXpHistory(userId: String, limit: Int = 20): Result<List<XpTransaction>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val transactions = SupabaseClient.client
                    .from(XP_TRANSACTIONS_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<XpTransaction>()

                Result.success(transactions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading XP history: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BADGES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get all available badges
     */
    suspend fun getAllBadges(): Result<List<Badge>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val badges = SupabaseClient.client
                    .from(BADGES_TABLE)
                    .select()
                    .decodeList<Badge>()

                Result.success(badges)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading badges: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's earned badges
     */
    suspend fun getUserBadges(userId: String): Result<List<UserBadge>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val badges = SupabaseClient.client
                    .from(USER_BADGES_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("earned_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<UserBadge>()

                Result.success(badges)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user badges: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check and award a badge (uses server function)
     */
    suspend fun checkAndAwardBadge(userId: String, badgeId: String): Result<Boolean> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(false)
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_badge_id", badgeId)
                }

                val awarded = SupabaseClient.client.postgrest
                    .rpc("check_and_award_badge", params)
                    .decodeSingle<Boolean>()

                if (awarded) {
                    Log.d(TAG, "Awarded badge $badgeId to user $userId")
                }
                Result.success(awarded)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/awarding badge: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STREAKS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get user's streak data
     */
    suspend fun getUserStreak(userId: String): Result<UserStreak?> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val results = SupabaseClient.client
                    .from(USER_STREAKS_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<UserStreak>()

                Result.success(results.firstOrNull() ?: UserStreak(userId = userId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user streak: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update streak on workout completion (uses server function)
     */
    suspend fun updateStreak(userId: String): Result<UserStreak> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_user_id", userId)
                }

                val streak = SupabaseClient.client.postgrest
                    .rpc("update_user_streak", params)
                    .decodeSingle<UserStreak>()

                Log.d(TAG, "Updated streak for user $userId: ${streak.currentStreak} days")
                Result.success(streak)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating streak: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Use a streak freeze token
     */
    suspend fun useStreakFreeze(userId: String): Result<Boolean> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(false)
        }

        return try {
            withContext(Dispatchers.IO) {
                val tomorrow = java.time.LocalDate.now().plusDays(1).toString()

                SupabaseClient.client
                    .from(USER_STREAKS_TABLE)
                    .update(
                        mapOf(
                            "streak_protected_until" to tomorrow,
                            "freeze_tokens" to "freeze_tokens - 1"
                        )
                    ) {
                        filter {
                            eq("user_id", userId)
                            gt("freeze_tokens", 0)
                        }
                    }

                Log.d(TAG, "Used streak freeze for user $userId")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error using streak freeze: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTIVITY FEED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get activity feed for current user (from followed users)
     */
    suspend fun getActivityFeed(limit: Int = 20, offset: Int = 0): Result<List<ActivityItem>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_limit", limit)
                    put("p_offset", offset)
                }

                val activities = SupabaseClient.client.postgrest
                    .rpc("get_activity_feed", params)
                    .decodeList<ActivityItem>()

                Result.success(activities)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading activity feed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Create an activity item
     */
    suspend fun createActivity(
        userId: String,
        activityType: ActivityType,
        targetUserId: String? = null,
        targetId: String? = null,
        metadata: Map<String, String>? = null
    ): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(Unit)
        }

        return try {
            withContext(Dispatchers.IO) {
                val activity = mapOf(
                    "user_id" to userId,
                    "activity_type" to activityType.name.lowercase(),
                    "target_user_id" to targetUserId,
                    "target_id" to targetId,
                    "metadata" to metadata
                ).filterValues { it != null }

                SupabaseClient.client
                    .from(ACTIVITY_FEED_TABLE)
                    .insert(activity)

                Log.d(TAG, "Created activity: $activityType for user $userId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating activity: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COMBINED STATS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Response from get_user_gamification_stats RPC
     */
    @Serializable
    data class GamificationStats(
        val level: Int,
        val total_xp: Int,
        val current_streak: Int,
        val longest_streak: Int,
        val badges_count: Long,
        val recent_badges: String // JSON array
    )

    /**
     * Get combined gamification stats for a user
     */
    suspend fun getGamificationStats(userId: String): Result<GamificationStats?> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_user_id", userId)
                }

                val stats = SupabaseClient.client.postgrest
                    .rpc("get_user_gamification_stats", params)
                    .decodeList<GamificationStats>()

                Result.success(stats.firstOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading gamification stats: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER: Award XP for common actions
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun awardWorkoutXp(userId: String, workoutId: String) {
        awardXp(userId, XpRewards.COMPLETE_WORKOUT, "workout", workoutId, "Completed workout")
    }

    suspend fun awardPrXp(userId: String, exerciseId: String) {
        awardXp(userId, XpRewards.ACHIEVE_PR, "pr", exerciseId, "New personal record!")
    }

    suspend fun awardShareXp(userId: String, postId: String) {
        awardXp(userId, XpRewards.SHARE_WORKOUT, "social", postId, "Shared workout")
    }

    suspend fun awardLikeReceivedXp(userId: String) {
        awardXp(userId, XpRewards.RECEIVE_LIKE, "social", null, "Received a like")
    }

    suspend fun awardChallengeParticipationXp(userId: String, challengeId: String) {
        awardXp(userId, XpRewards.CHALLENGE_PARTICIPATION, "challenge", challengeId, "Joined challenge")
    }

    suspend fun awardChallengeWinXp(userId: String, challengeId: String, position: Int) {
        val xp = when (position) {
            1 -> XpRewards.CHALLENGE_WINNER
            2, 3 -> XpRewards.CHALLENGE_TOP_3
            in 4..10 -> XpRewards.CHALLENGE_TOP_10
            else -> 0
        }
        if (xp > 0) {
            awardXp(userId, xp, "challenge", challengeId, "Challenge position #$position")
        }
    }
}