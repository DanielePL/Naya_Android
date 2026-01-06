package com.example.menotracker.data

import android.util.Log
import com.example.menotracker.data.models.*
import com.example.menotracker.onboarding.data.CurrentPRs
import com.example.menotracker.onboarding.data.GoalPRs
import com.example.menotracker.onboarding.data.TrainingCommitment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Repository for User Strength Profile data
 * The "red thread" - connects onboarding PRs to workouts and programs
 */
class StrengthProfileRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TAG = "StrengthProfileRepo"
        private const val TABLE_PROFILES = "user_strength_profiles"
        private const val TABLE_PR_HISTORY = "pr_history"
        private const val TABLE_MILESTONES = "strength_milestones"
    }

    // =====================================================
    // STRENGTH PROFILE CRUD
    // =====================================================

    /**
     * Get user's strength profile
     */
    suspend fun getProfile(userId: String): Result<UserStrengthProfile?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Fetching strength profile for user: $userId")

            val result = supabase.from(TABLE_PROFILES)
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<StrengthProfileDto>()

            val profile = result?.toUserStrengthProfile()
            Log.d(TAG, "✅ Profile fetched: ${profile?.currentTotal}kg total")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Create or update strength profile from onboarding data
     */
    suspend fun saveProfileFromOnboarding(
        userId: String,
        gender: com.example.menotracker.onboarding.data.Gender,
        currentPRs: CurrentPRs,
        goalPRs: GoalPRs,
        commitment: TrainingCommitment,
        experienceLevel: com.example.menotracker.onboarding.data.ExperienceLevel?,
        estimatedWeeks: Int? = null
    ): Result<UserStrengthProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 Saving strength profile from onboarding for user: $userId")

            // Calculate target date
            val targetDate = estimatedWeeks?.let {
                LocalDate.now().plusWeeks(it.toLong()).format(DateTimeFormatter.ISO_DATE)
            }

            val dto = StrengthProfileDto(
                id = UUID.randomUUID().toString(),
                userId = userId,
                gender = when (gender) {
                    com.example.menotracker.onboarding.data.Gender.MALE -> "male"
                    com.example.menotracker.onboarding.data.Gender.FEMALE -> "female"
                },
                bodyweightKg = currentPRs.bodyweightKg ?: 80f,
                experienceLevel = when (experienceLevel) {
                    com.example.menotracker.onboarding.data.ExperienceLevel.BEGINNER -> "beginner"
                    com.example.menotracker.onboarding.data.ExperienceLevel.INTERMEDIATE -> "intermediate"
                    com.example.menotracker.onboarding.data.ExperienceLevel.EXPERIENCED -> "experienced"
                    com.example.menotracker.onboarding.data.ExperienceLevel.ELITE -> "elite"
                    null -> "intermediate"
                },
                currentSquatKg = currentPRs.squatKg ?: 0f,
                currentBenchKg = currentPRs.benchKg ?: 0f,
                currentDeadliftKg = currentPRs.deadliftKg ?: 0f,
                goalSquatKg = goalPRs.squatKg ?: 0f,
                goalBenchKg = goalPRs.benchKg ?: 0f,
                goalDeadliftKg = goalPRs.deadliftKg ?: 0f,
                targetDate = targetDate,
                estimatedWeeks = estimatedWeeks,
                sessionsPerWeek = commitment.sessionsPerWeek,
                effortLevel = commitment.effortLevel
            )

            supabase.from(TABLE_PROFILES).upsert(dto)

            val profile = dto.toUserStrengthProfile()
            Log.d(TAG, "✅ Profile saved: ${profile.currentTotal}kg → ${profile.goalTotal}kg")

            // Generate milestones
            generateMilestones(userId, profile)

            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update current PRs
     */
    suspend fun updateCurrentPRs(
        userId: String,
        squatKg: Float? = null,
        benchKg: Float? = null,
        deadliftKg: Float? = null,
        overheadKg: Float? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Updating current PRs for user: $userId")

            val updates = buildMap<String, Any> {
                squatKg?.let { put("current_squat_kg", it) }
                benchKg?.let { put("current_bench_kg", it) }
                deadliftKg?.let { put("current_deadlift_kg", it) }
                overheadKg?.let { put("current_overhead_kg", it) }
            }

            if (updates.isNotEmpty()) {
                supabase.from(TABLE_PROFILES).update(updates) {
                    filter { eq("user_id", userId) }
                }
            }

            Log.d(TAG, "✅ PRs updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating PRs: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update goal PRs
     */
    suspend fun updateGoalPRs(
        userId: String,
        squatKg: Float? = null,
        benchKg: Float? = null,
        deadliftKg: Float? = null,
        overheadKg: Float? = null,
        targetDate: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎯 Updating goal PRs for user: $userId")

            val updates = buildMap<String, Any?> {
                squatKg?.let { put("goal_squat_kg", it) }
                benchKg?.let { put("goal_bench_kg", it) }
                deadliftKg?.let { put("goal_deadlift_kg", it) }
                overheadKg?.let { put("goal_overhead_kg", it) }
                targetDate?.let { put("target_date", it) }
            }

            if (updates.isNotEmpty()) {
                supabase.from(TABLE_PROFILES).update(updates) {
                    filter { eq("user_id", userId) }
                }
            }

            Log.d(TAG, "✅ Goal PRs updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating goal PRs: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update bodyweight
     */
    suspend fun updateBodyweight(userId: String, bodyweightKg: Float): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(TABLE_PROFILES).update(
                mapOf("bodyweight_kg" to bodyweightKg)
            ) {
                filter { eq("user_id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating bodyweight: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =====================================================
    // PR HISTORY
    // =====================================================

    /**
     * Record a PR attempt and check if it's a new PR
     */
    suspend fun recordPRAttempt(
        userId: String,
        lift: LiftType,
        weightKg: Float,
        reps: Int,
        velocityMs: Float? = null,
        sessionId: String? = null
    ): Result<PRCheckResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Recording PR attempt: ${lift.name} ${weightKg}kg x $reps")

            // Calculate e1RM
            val e1RM = PRHistoryEntry.calculateE1RM(weightKg, reps)

            // Get current PR
            val profile = getProfile(userId).getOrNull()
            val currentPR = when (lift) {
                LiftType.SQUAT -> profile?.currentSquatKg ?: 0f
                LiftType.BENCH -> profile?.currentBenchKg ?: 0f
                LiftType.DEADLIFT -> profile?.currentDeadliftKg ?: 0f
                LiftType.OVERHEAD -> profile?.currentOverheadKg ?: 0f
            }

            // Insert PR history entry
            val historyDto = PRHistoryDto(
                id = UUID.randomUUID().toString(),
                userId = userId,
                lift = lift.name.lowercase(),
                weightKg = weightKg,
                reps = reps,
                estimated1rm = e1RM,
                velocityMs = velocityMs,
                sessionId = sessionId,
                date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            )

            supabase.from(TABLE_PR_HISTORY).insert(historyDto)

            // Check if this is a new PR
            val isNewPR = (reps == 1 && weightKg > currentPR) ||
                    (reps > 1 && e1RM > currentPR * 1.02f)

            if (isNewPR) {
                // Update profile with new PR
                when (lift) {
                    LiftType.SQUAT -> updateCurrentPRs(userId, squatKg = e1RM)
                    LiftType.BENCH -> updateCurrentPRs(userId, benchKg = e1RM)
                    LiftType.DEADLIFT -> updateCurrentPRs(userId, deadliftKg = e1RM)
                    LiftType.OVERHEAD -> updateCurrentPRs(userId, overheadKg = e1RM)
                }

                Log.d(TAG, "🎉 NEW PR! ${lift.name}: ${currentPR}kg → ${e1RM}kg")
                Result.success(PRCheckResult.NewPR(e1RM, currentPR, e1RM - currentPR))
            } else {
                Log.d(TAG, "📊 PR attempt recorded (not a new PR)")
                Result.success(PRCheckResult.NoPR(e1RM))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error recording PR: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get PR history for a specific lift
     */
    suspend fun getPRHistory(
        userId: String,
        lift: LiftType? = null,
        limit: Int = 50
    ): Result<List<PRHistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from(TABLE_PR_HISTORY)
                .select {
                    filter {
                        eq("user_id", userId)
                        lift?.let { eq("lift", it.name.lowercase()) }
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<PRHistoryDto>()

            Result.success(result.map { it.toPRHistoryEntry() })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching PR history: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =====================================================
    // MILESTONES
    // =====================================================

    /**
     * Generate milestones for the user's journey
     */
    private suspend fun generateMilestones(userId: String, profile: UserStrengthProfile) {
        try {
            val totalToGain = profile.totalToGain
            val weeks = profile.estimatedWeeks ?: 20

            if (totalToGain <= 0 || weeks <= 0) return

            val milestones = listOf(
                MilestoneDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    week = weeks / 4,
                    expectedTotal = profile.currentTotal + (totalToGain * 0.25f),
                    message = "First checkpoint - stay consistent!"
                ),
                MilestoneDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    week = weeks / 2,
                    expectedTotal = profile.currentTotal + (totalToGain * 0.5f),
                    message = "Halfway there! You're on track."
                ),
                MilestoneDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    week = (weeks * 0.75f).toInt(),
                    expectedTotal = profile.currentTotal + (totalToGain * 0.75f),
                    message = "Final stretch - time to peak!"
                ),
                MilestoneDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    week = weeks,
                    expectedTotal = profile.goalTotal,
                    message = "Goal reached! Time for new PRs!"
                )
            )

            // Delete existing milestones and insert new ones
            supabase.from(TABLE_MILESTONES).delete {
                filter { eq("user_id", userId) }
            }

            supabase.from(TABLE_MILESTONES).insert(milestones)
            Log.d(TAG, "✅ Generated ${milestones.size} milestones")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error generating milestones (non-fatal): ${e.message}")
        }
    }

    /**
     * Get milestones for user
     */
    suspend fun getMilestones(userId: String): Result<List<StrengthMilestone>> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from(TABLE_MILESTONES)
                .select {
                    filter { eq("user_id", userId) }
                    order("week", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<MilestoneDto>()

            Result.success(result.map { it.toStrengthMilestone() })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching milestones: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check and update milestones based on current progress
     */
    suspend fun checkMilestones(userId: String): Result<List<StrengthMilestone>> = withContext(Dispatchers.IO) {
        try {
            val profile = getProfile(userId).getOrNull() ?: return@withContext Result.success(emptyList())
            val milestones = getMilestones(userId).getOrNull() ?: return@withContext Result.success(emptyList())

            val updatedMilestones = milestones.map { milestone ->
                if (!milestone.isReached && profile.currentTotal >= milestone.expectedTotal) {
                    // Mark milestone as reached
                    supabase.from(TABLE_MILESTONES).update(
                        mapOf(
                            "is_reached" to true,
                            "reached_at" to java.time.Instant.now().toString(),
                            "actual_total" to profile.currentTotal
                        )
                    ) {
                        filter { eq("id", milestone.id) }
                    }
                    milestone.copy(isReached = true)
                } else {
                    milestone
                }
            }

            Result.success(updatedMilestones)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking milestones: ${e.message}", e)
            Result.failure(e)
        }
    }

    // =====================================================
    // PERCENTAGE CALCULATIONS (for workouts)
    // =====================================================

    /**
     * Calculate working weight for a percentage-based set
     */
    suspend fun calculateWorkingWeight(
        userId: String,
        lift: LiftType,
        percentage: Float,
        roundTo: Float = 2.5f
    ): Result<Float> = withContext(Dispatchers.IO) {
        try {
            val profile = getProfile(userId).getOrNull()
                ?: return@withContext Result.failure(Exception("No strength profile found"))

            val weight = profile.calculateWorkingWeight(lift, percentage, roundTo)
            Result.success(weight)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get VBT auto-regulation suggestion
     */
    suspend fun getVBTSuggestion(
        userId: String,
        targetPercentage: Float,
        measuredVelocity: Float
    ): Result<WeightSuggestion> = withContext(Dispatchers.IO) {
        try {
            val profile = getProfile(userId).getOrNull()
                ?: return@withContext Result.failure(Exception("No strength profile found"))

            val suggestion = VBTAutoRegulation.suggestAdjustment(
                targetPercentage = targetPercentage,
                measuredVelocity = measuredVelocity,
                profile = profile
            )
            Result.success(suggestion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// =====================================================
// DTOs for Supabase
// =====================================================

@Serializable
data class StrengthProfileDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val gender: String,
    @SerialName("bodyweight_kg") val bodyweightKg: Float,
    @SerialName("experience_level") val experienceLevel: String,
    @SerialName("current_squat_kg") val currentSquatKg: Float,
    @SerialName("current_bench_kg") val currentBenchKg: Float,
    @SerialName("current_deadlift_kg") val currentDeadliftKg: Float,
    @SerialName("current_overhead_kg") val currentOverheadKg: Float? = null,
    @SerialName("goal_squat_kg") val goalSquatKg: Float,
    @SerialName("goal_bench_kg") val goalBenchKg: Float,
    @SerialName("goal_deadlift_kg") val goalDeadliftKg: Float,
    @SerialName("goal_overhead_kg") val goalOverheadKg: Float? = null,
    @SerialName("target_date") val targetDate: String? = null,
    @SerialName("estimated_weeks") val estimatedWeeks: Int? = null,
    @SerialName("sessions_per_week") val sessionsPerWeek: Int = 4,
    @SerialName("effort_level") val effortLevel: Int = 7,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toUserStrengthProfile(): UserStrengthProfile = UserStrengthProfile(
        id = id,
        userId = userId,
        gender = if (gender == "male") StrengthGender.MALE else StrengthGender.FEMALE,
        bodyweightKg = bodyweightKg,
        experienceLevel = when (experienceLevel) {
            "beginner" -> StrengthExperienceLevel.BEGINNER
            "intermediate" -> StrengthExperienceLevel.INTERMEDIATE
            "experienced" -> StrengthExperienceLevel.EXPERIENCED
            "elite" -> StrengthExperienceLevel.ELITE
            else -> StrengthExperienceLevel.INTERMEDIATE
        },
        currentSquatKg = currentSquatKg,
        currentBenchKg = currentBenchKg,
        currentDeadliftKg = currentDeadliftKg,
        currentOverheadKg = currentOverheadKg,
        goalSquatKg = goalSquatKg,
        goalBenchKg = goalBenchKg,
        goalDeadliftKg = goalDeadliftKg,
        goalOverheadKg = goalOverheadKg,
        targetDate = targetDate,
        estimatedWeeks = estimatedWeeks,
        sessionsPerWeek = sessionsPerWeek,
        effortLevel = effortLevel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Serializable
data class PRHistoryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val lift: String,
    @SerialName("weight_kg") val weightKg: Float,
    val reps: Int = 1,
    @SerialName("estimated_1rm") val estimated1rm: Float,
    @SerialName("velocity_ms") val velocityMs: Float? = null,
    @SerialName("session_id") val sessionId: String? = null,
    val date: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toPRHistoryEntry(): PRHistoryEntry = PRHistoryEntry(
        id = id,
        userId = userId,
        date = date,
        lift = LiftType.valueOf(lift.uppercase()),
        weightKg = weightKg,
        reps = reps,
        estimated1RM = estimated1rm,
        velocityMs = velocityMs,
        createdAt = createdAt
    )
}

@Serializable
data class MilestoneDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val week: Int,
    @SerialName("expected_total") val expectedTotal: Float,
    val message: String,
    @SerialName("is_reached") val isReached: Boolean = false,
    @SerialName("reached_at") val reachedAt: String? = null,
    @SerialName("actual_total") val actualTotal: Float? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toStrengthMilestone(): StrengthMilestone = StrengthMilestone(
        id = id,
        userId = userId,
        week = week,
        expectedTotal = expectedTotal,
        message = message,
        isReached = isReached,
        reachedAt = reachedAt
    )
}

/**
 * Result of checking for a new PR
 */
sealed class PRCheckResult {
    data class NewPR(
        val newPR: Float,
        val oldPR: Float,
        val improvement: Float
    ) : PRCheckResult()

    data class NoPR(
        val estimated1RM: Float
    ) : PRCheckResult()
}