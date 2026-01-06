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

/**
 * Repository for challenge operations.
 * Handles challenges, Max Out Friday, and challenge entries.
 */
object ChallengeRepository {
    private const val TAG = "ChallengeRepository"

    // Table names
    private const val CHALLENGES_TABLE = "community_challenges"
    private const val ENTRIES_TABLE = "community_challenge_entries"

    // ═══════════════════════════════════════════════════════════════════════
    // CHALLENGE QUERIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get all active challenges
     */
    suspend fun getActiveChallenges(): Result<List<Challenge>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val challenges = SupabaseClient.client
                    .from(CHALLENGES_TABLE)
                    .select {
                        filter {
                            eq("status", "active")
                        }
                        order("start_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<Challenge>()

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${challenges.size} active challenges")
                }
                Result.success(challenges)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading active challenges: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get upcoming challenges
     */
    suspend fun getUpcomingChallenges(): Result<List<Challenge>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val challenges = SupabaseClient.client
                    .from(CHALLENGES_TABLE)
                    .select {
                        filter {
                            eq("status", "upcoming")
                        }
                        order("start_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<Challenge>()

                Result.success(challenges)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading upcoming challenges: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get completed challenges (history)
     */
    suspend fun getCompletedChallenges(limit: Int = 10): Result<List<Challenge>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val challenges = SupabaseClient.client
                    .from(CHALLENGES_TABLE)
                    .select {
                        filter {
                            eq("status", "completed")
                        }
                        order("end_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<Challenge>()

                Result.success(challenges)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading completed challenges: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single challenge by ID
     */
    suspend fun getChallenge(challengeId: String): Result<Challenge?> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val challenges = SupabaseClient.client
                    .from(CHALLENGES_TABLE)
                    .select {
                        filter {
                            eq("id", challengeId)
                        }
                    }
                    .decodeList<Challenge>()

                Result.success(challenges.firstOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading challenge: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAX OUT FRIDAY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get current Max Out Friday challenge info
     * Uses RPC function for optimized query
     */
    suspend fun getCurrentMaxOutFriday(): Result<MaxOutFridayInfo?> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.MAX_OUT_FRIDAY_ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val results = SupabaseClient.client.postgrest
                    .rpc("get_current_max_out_friday")
                    .decodeList<MaxOutFridayInfo>()

                Result.success(results.firstOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading Max Out Friday: ${e.message}", e)

            // Fallback to direct query
            try {
                val fallback = getCurrentMaxOutFridayDirect()
                Result.success(fallback)
            } catch (fallbackError: Exception) {
                Result.failure(fallbackError)
            }
        }
    }

    /**
     * Direct query fallback for Max Out Friday
     */
    private suspend fun getCurrentMaxOutFridayDirect(): MaxOutFridayInfo? {
        return withContext(Dispatchers.IO) {
            val challenges = SupabaseClient.client
                .from(CHALLENGES_TABLE)
                .select {
                    filter {
                        eq("challenge_type", "max_out_friday")
                        eq("status", "active")
                    }
                }
                .decodeList<Challenge>()

            challenges.firstOrNull()?.let { challenge ->
                MaxOutFridayInfo(
                    id = challenge.id,
                    title = challenge.title,
                    exerciseId = challenge.exerciseId ?: "",
                    exerciseName = challenge.exerciseName ?: "",
                    startDate = challenge.startDate,
                    endDate = challenge.endDate,
                    participantsCount = challenge.participantsCount,
                    userEntryKg = null,
                    userRank = null
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHALLENGE ENTRIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get entries for a challenge (leaderboard)
     */
    suspend fun getChallengeEntries(
        challengeId: String,
        limit: Int = 50
    ): Result<List<ChallengeEntry>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val entries = SupabaseClient.client
                    .from(ENTRIES_TABLE)
                    .select {
                        filter {
                            eq("challenge_id", challengeId)
                        }
                        order("value_kg", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<ChallengeEntry>()

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${entries.size} entries for challenge $challengeId")
                }
                Result.success(entries)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading challenge entries: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's entry for a challenge
     */
    suspend fun getUserEntry(challengeId: String, userId: String): Result<ChallengeEntry?> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val entries = SupabaseClient.client
                    .from(ENTRIES_TABLE)
                    .select {
                        filter {
                            eq("challenge_id", challengeId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<ChallengeEntry>()

                Result.success(entries.firstOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading user entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Submit entry for a challenge (e.g., Max Out Friday)
     */
    suspend fun submitChallengeEntry(
        challengeId: String,
        userId: String,
        valueKg: Double? = null,
        valueReps: Int? = null,
        streakCount: Int? = null,
        workoutHistoryId: String? = null,
        videoUrl: String? = null,
        isPr: Boolean = false
    ): Result<ChallengeEntry> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val entry = ChallengeEntry(
                    challengeId = challengeId,
                    userId = userId,
                    valueKg = valueKg,
                    valueReps = valueReps,
                    streakCount = streakCount,
                    workoutHistoryId = workoutHistoryId,
                    videoUrl = videoUrl,
                    isPr = isPr
                )

                // Upsert to handle updates to existing entries
                val insertedEntries = SupabaseClient.client
                    .from(ENTRIES_TABLE)
                    .upsert(entry) {
                        select()
                    }
                    .decodeList<ChallengeEntry>()

                val insertedEntry = insertedEntries.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to submit entry"))

                Log.d(TAG, "✅ Submitted entry for challenge $challengeId: ${valueKg}kg")
                Result.success(insertedEntry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error submitting challenge entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update user's challenge entry (improve score)
     */
    suspend fun updateChallengeEntry(
        challengeId: String,
        userId: String,
        valueKg: Double? = null,
        valueReps: Int? = null,
        workoutHistoryId: String? = null,
        isPr: Boolean = false
    ): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val updates = mutableMapOf<String, Any?>()
                valueKg?.let { updates["value_kg"] = it }
                valueReps?.let { updates["value_reps"] = it }
                workoutHistoryId?.let { updates["workout_history_id"] = it }
                updates["is_pr"] = isPr
                updates["updated_at"] = java.time.Instant.now().toString()

                SupabaseClient.client
                    .from(ENTRIES_TABLE)
                    .update(updates) {
                        filter {
                            eq("challenge_id", challengeId)
                            eq("user_id", userId)
                        }
                    }

                Log.d(TAG, "✅ Updated entry for challenge $challengeId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating challenge entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user has participated in a challenge
     */
    suspend fun hasParticipated(challengeId: String, userId: String): Result<Boolean> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(false)
        }

        return try {
            withContext(Dispatchers.IO) {
                val entries = SupabaseClient.client
                    .from(ENTRIES_TABLE)
                    .select {
                        filter {
                            eq("challenge_id", challengeId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<ChallengeEntry>()

                Result.success(entries.isNotEmpty())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking participation: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // USER CHALLENGE HISTORY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get challenges user has participated in
     */
    suspend fun getUserChallengeHistory(userId: String, limit: Int = 20): Result<List<ChallengeEntry>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val entries = SupabaseClient.client
                    .from(ENTRIES_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("submitted_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<ChallengeEntry>()

                Result.success(entries)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading user challenge history: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // USER-CREATED CHALLENGES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Create a new user challenge
     */
    suspend fun createChallenge(
        request: CreateChallengeRequest,
        creatorId: String
    ): Result<Challenge> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val challengeData = mapOf(
                    "title" to request.title,
                    "description" to request.description,
                    "challenge_type" to request.challengeType.name.lowercase(),
                    "exercise_id" to request.exerciseId,
                    "exercise_name" to request.exerciseName,
                    "target_volume_kg" to request.targetVolumeKg,
                    "target_streak_days" to request.targetStreakDays,
                    "start_date" to request.startDate,
                    "end_date" to request.endDate,
                    "is_public" to request.isPublic,
                    "created_by" to creatorId,
                    "status" to "active"
                ).filterValues { it != null }

                val challenges = SupabaseClient.client
                    .from(CHALLENGES_TABLE)
                    .insert(challengeData) {
                        select()
                    }
                    .decodeList<Challenge>()

                val challenge = challenges.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to create challenge"))

                // Send invites if specified
                request.invitedUserIds?.forEach { inviteeId ->
                    sendChallengeInvite(challenge.id, creatorId, inviteeId)
                }

                Log.d(TAG, "✅ Created challenge: ${challenge.id}")
                Result.success(challenge)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating challenge: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a challenge (only by creator)
     */
    suspend fun deleteChallenge(challengeId: String, userId: String): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from(CHALLENGES_TABLE)
                    .delete {
                        filter {
                            eq("id", challengeId)
                            eq("created_by", userId)
                        }
                    }

                Log.d(TAG, "✅ Deleted challenge: $challengeId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting challenge: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHALLENGE INVITES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Send a challenge invite
     */
    suspend fun sendChallengeInvite(
        challengeId: String,
        inviterId: String,
        inviteeId: String
    ): Result<ChallengeInvite> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val invite = ChallengeInvite(
                    challengeId = challengeId,
                    inviterId = inviterId,
                    inviteeId = inviteeId,
                    status = InviteStatus.PENDING
                )

                val invites = SupabaseClient.client
                    .from("community_challenge_invites")
                    .insert(invite) {
                        select()
                    }
                    .decodeList<ChallengeInvite>()

                val createdInvite = invites.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to send invite"))

                Log.d(TAG, "✅ Sent invite to $inviteeId for challenge $challengeId")
                Result.success(createdInvite)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending invite: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get pending invites for a user
     */
    suspend fun getPendingInvites(userId: String): Result<List<ChallengeInvite>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val invites = SupabaseClient.client
                    .from("community_challenge_invites")
                    .select {
                        filter {
                            eq("invitee_id", userId)
                            eq("status", "pending")
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<ChallengeInvite>()

                Result.success(invites)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading pending invites: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Respond to a challenge invite
     */
    suspend fun respondToInvite(
        inviteId: String,
        userId: String,
        accept: Boolean
    ): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.CHALLENGES_ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val newStatus = if (accept) "accepted" else "declined"

                SupabaseClient.client
                    .from("community_challenge_invites")
                    .update(mapOf("status" to newStatus)) {
                        filter {
                            eq("id", inviteId)
                            eq("invitee_id", userId)
                        }
                    }

                Log.d(TAG, "✅ ${if (accept) "Accepted" else "Declined"} invite: $inviteId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error responding to invite: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAX OUT FRIDAY EXTENDED
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get previous Max Out Friday winners
     */
    suspend fun getMaxOutFridayWinners(limit: Int = 5): Result<List<PreviousWinner>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.MAX_OUT_FRIDAY_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = kotlinx.serialization.json.buildJsonObject {
                    put("p_limit", kotlinx.serialization.json.JsonPrimitive(limit))
                }

                val winners = SupabaseClient.client.postgrest
                    .rpc("get_max_out_friday_winners", params)
                    .decodeList<PreviousWinner>()

                Result.success(winners)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading MOF winners: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Calculate time remaining until challenge ends
     */
    fun calculateTimeRemaining(endDate: String): Long {
        return try {
            val end = java.time.LocalDate.parse(endDate.substring(0, 10))
                .atTime(23, 59, 59)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
            val now = java.time.Instant.now()
            java.time.Duration.between(now, end).toMillis().coerceAtLeast(0)
        } catch (e: Exception) {
            0L
        }
    }
}
