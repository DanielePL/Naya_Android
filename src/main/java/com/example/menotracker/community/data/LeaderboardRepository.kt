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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Repository for leaderboard operations.
 * Handles leaderboard queries, updates, and ranking.
 */
object LeaderboardRepository {
    private const val TAG = "LeaderboardRepository"

    // Table name
    private const val LEADERBOARD_TABLE = "community_leaderboard"

    // Default limit
    private const val DEFAULT_LIMIT = 50

    // ═══════════════════════════════════════════════════════════════════════
    // LEADERBOARD QUERIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get leaderboard for a specific exercise
     * Uses RPC function for optimized query with user data
     */
    suspend fun getLeaderboard(
        exerciseId: String,
        friendsOnly: Boolean = false,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<LeaderboardEntry>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.LEADERBOARD_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_exercise_id", JsonPrimitive(exerciseId))
                    put("p_limit", JsonPrimitive(limit))
                    put("p_friends_only", JsonPrimitive(friendsOnly))
                }

                val entries = SupabaseClient.client.postgrest
                    .rpc("get_exercise_leaderboard", params)
                    .decodeList<LeaderboardEntry>()

                // Add rank numbers
                entries.forEachIndexed { index, entry ->
                    entry.rank = index + 1
                }

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${entries.size} leaderboard entries for $exerciseId")
                }
                Result.success(entries)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading leaderboard: ${e.message}", e)

            // Fallback to direct table query
            try {
                val fallbackEntries = getLeaderboardDirect(exerciseId, limit)
                Result.success(fallbackEntries)
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
        }
    }

    /**
     * Direct table query fallback if RPC fails
     */
    private suspend fun getLeaderboardDirect(
        exerciseId: String,
        limit: Int
    ): List<LeaderboardEntry> {
        return withContext(Dispatchers.IO) {
            val entries = SupabaseClient.client
                .from(LEADERBOARD_TABLE)
                .select {
                    filter {
                        eq("exercise_id", exerciseId)
                    }
                    order("estimated_1rm_kg", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<LeaderboardEntry>()

            entries.forEachIndexed { index, entry ->
                entry.rank = index + 1
            }

            entries
        }
    }

    /**
     * Get current user's rank for an exercise
     */
    suspend fun getUserRank(userId: String, exerciseId: String): Result<Int?> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.LEADERBOARD_ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                // Get all entries sorted by 1RM
                val allEntries = SupabaseClient.client
                    .from(LEADERBOARD_TABLE)
                    .select {
                        filter {
                            eq("exercise_id", exerciseId)
                        }
                        order("estimated_1rm_kg", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<LeaderboardEntry>()

                val userIndex = allEntries.indexOfFirst { it.userId == userId }
                if (userIndex >= 0) {
                    Result.success(userIndex + 1)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user rank: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's entry for an exercise
     */
    suspend fun getUserEntry(userId: String, exerciseId: String): Result<LeaderboardEntry?> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.LEADERBOARD_ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val entries = SupabaseClient.client
                    .from(LEADERBOARD_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("exercise_id", exerciseId)
                        }
                    }
                    .decodeList<LeaderboardEntry>()

                Result.success(entries.firstOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LEADERBOARD UPDATES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update or create leaderboard entry for a user
     * Called when user sets a new PR
     */
    suspend fun updateLeaderboardEntry(
        userId: String,
        exerciseId: String,
        exerciseName: String,
        prWeightKg: Double,
        prReps: Int,
        estimated1rmKg: Double,
        userBodyweightKg: Double? = null
    ): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.LEADERBOARD_ENABLED) {
            return Result.success(Unit)  // Silently succeed if disabled
        }

        return try {
            withContext(Dispatchers.IO) {
                // Calculate Wilks score if bodyweight available
                val wilksScore = if (userBodyweightKg != null && userBodyweightKg > 0) {
                    calculateWilksScore(estimated1rmKg, userBodyweightKg)
                } else null

                val entry = LeaderboardEntry(
                    userId = userId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    prWeightKg = prWeightKg,
                    prReps = prReps,
                    estimated1rmKg = estimated1rmKg,
                    userBodyweightKg = userBodyweightKg,
                    wilksScore = wilksScore,
                    achievedAt = java.time.Instant.now().toString()
                )

                SupabaseClient.client
                    .from(LEADERBOARD_TABLE)
                    .upsert(entry)

                Log.d(TAG, "✅ Updated leaderboard entry for $exerciseId: ${estimated1rmKg}kg")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating leaderboard entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Remove user from leaderboard (opt-out)
     */
    suspend fun removeFromLeaderboard(userId: String): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(Unit)
        }

        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from(LEADERBOARD_TABLE)
                    .delete {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                Log.d(TAG, "✅ Removed user $userId from all leaderboards")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error removing from leaderboard: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Calculate Wilks score for relative strength comparison
     * Uses the Wilks formula for male lifters (simplified)
     */
    private fun calculateWilksScore(totalKg: Double, bodyweightKg: Double): Double {
        // Wilks coefficients for males
        val a = -216.0475144
        val b = 16.2606339
        val c = -0.002388645
        val d = -0.00113732
        val e = 7.01863E-06
        val f = -1.291E-08

        val x = bodyweightKg
        val coefficient = 500.0 / (a + b * x + c * x * x + d * x * x * x + e * x * x * x * x + f * x * x * x * x * x)

        return totalKg * coefficient
    }

    /**
     * Get leaderboard summary for user (their best ranks)
     */
    suspend fun getUserLeaderboardSummary(userId: String): Result<Map<String, Int>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.LEADERBOARD_ENABLED) {
            return Result.success(emptyMap())
        }

        return try {
            withContext(Dispatchers.IO) {
                val ranks = mutableMapOf<String, Int>()

                for (exercise in LeaderboardExercises.EXERCISES) {
                    val rank = getUserRank(userId, exercise.id).getOrNull()
                    if (rank != null) {
                        ranks[exercise.id] = rank
                    }
                }

                Result.success(ranks)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting leaderboard summary: ${e.message}", e)
            Result.failure(e)
        }
    }
}
