package com.example.menotracker.data

import android.util.Log
import com.example.menotracker.data.models.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository for workout statistics, history, and PRs
 */
object StatisticsRepository {
    private const val TAG = "StatisticsRepo"

    // Use the shared Supabase client (full path to avoid import conflict)
    private val supabase get() = com.example.menotracker.data.SupabaseClient.client

    // ========================================
    // WORKOUT HISTORY
    // ========================================

    /**
     * Save completed workout to history
     */
    suspend fun saveWorkoutToHistory(
        sessionId: String,
        userId: String,
        workoutName: String,
        workoutTemplateId: String? = null,
        programId: String? = null,
        programWeek: Int? = null,
        programDay: Int? = null,
        totalVolumeKg: Double,
        totalSets: Int,
        totalReps: Int,
        totalExercises: Int,
        durationMinutes: Int?,
        prsAchieved: Int = 0,
        prExercises: List<String> = emptyList(),
        avgVelocity: Double? = null,
        notes: String? = null,
        startedAt: String?,
        completedAt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔵 saveWorkoutToHistory START")
            Log.d(TAG, "🔵 sessionId=$sessionId, userId=$userId")
            Log.d(TAG, "🔵 workoutName=$workoutName, volume=$totalVolumeKg, sets=$totalSets, reps=$totalReps")

            val historyId = UUID.randomUUID().toString()
            val data = WorkoutHistoryInsert(
                id = historyId,
                session_id = sessionId,
                user_id = userId,
                workout_name = workoutName,
                workout_template_id = workoutTemplateId,
                program_id = programId,
                program_week = programWeek,
                program_day = programDay,
                total_volume_kg = totalVolumeKg,
                total_sets = totalSets,
                total_reps = totalReps,
                total_exercises = totalExercises,
                duration_minutes = durationMinutes,
                prs_achieved = prsAchieved,
                pr_exercises = prExercises.ifEmpty { null },
                avg_velocity = avgVelocity,
                notes = notes,
                started_at = startedAt,
                completed_at = completedAt
            )

            Log.d(TAG, "🔵 Inserting workout history data: $data")
            supabase.from("workout_history").insert(data)

            Log.d(TAG, "✅ Workout history saved: $historyId")
            Result.success(historyId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving workout history: ${e.message}", e)
            Log.e(TAG, "❌ Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }

    /**
     * Get workout history for user (paginated)
     */
    suspend fun getWorkoutHistory(
        userId: String,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<WorkoutHistory>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading workout history for user: $userId")

            val result = supabase.from("workout_history")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("completed_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                    // Note: offset would need range() in newer versions
                }
                .decodeList<WorkoutHistory>()

            Log.d(TAG, "Loaded ${result.size} workout history entries")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading workout history: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get workouts for a specific date range (for calendar view)
     */
    suspend fun getWorkoutHistoryForMonth(
        userId: String,
        year: Int,
        month: Int
    ): Result<List<WorkoutHistory>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading workouts for $year-$month")

            val result = supabase.from("workout_history")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("completed_year", year)
                        eq("completed_month", month)
                    }
                    order("completed_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<WorkoutHistory>()

            Log.d(TAG, "Loaded ${result.size} workouts for $year-$month")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading month workouts: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ========================================
    // EXERCISE STATISTICS
    // ========================================

    /**
     * Get statistics for all exercises (for overview)
     */
    suspend fun getAllExerciseStatistics(userId: String): Result<List<ExerciseStatistics>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading all exercise statistics")

                val result = supabase.from("exercise_statistics")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("total_volume_kg", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<ExerciseStatistics>()

                Log.d(TAG, "Loaded ${result.size} exercise statistics")
                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading exercise statistics: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Get statistics for a specific exercise
     */
    suspend fun getExerciseStatistics(
        userId: String,
        exerciseId: String
    ): Result<ExerciseStatistics?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading statistics for exercise: $exerciseId")

            val result = supabase.from("exercise_statistics")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("exercise_id", exerciseId)
                    }
                }
                .decodeSingleOrNull<ExerciseStatistics>()

            Log.d(TAG, "Exercise statistics loaded: ${result != null}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading exercise statistics: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update exercise statistics after completing a set
     * Returns list of PRs achieved
     */
    suspend fun updateExerciseStatisticsForSet(
        userId: String,
        exerciseId: String,
        exerciseName: String? = null,
        sessionId: String,
        weightKg: Double,
        reps: Int,
        velocity: Double? = null
    ): Result<List<PRType>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating stats for $exerciseId: ${weightKg}kg x $reps")

            val prsAchieved = mutableListOf<PRType>()
            val volume = weightKg * reps
            val estimated1rm = calculate1RM(weightKg, reps)
            val now = java.time.Instant.now().toString()

            // Get existing stats
            val existing = supabase.from("exercise_statistics")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("exercise_id", exerciseId)
                    }
                }
                .decodeSingleOrNull<ExerciseStatistics>()

            // Check for PRs
            val isWeightPR = existing == null || weightKg > (existing.prWeightKg ?: 0.0)
            val isRepsPR = existing == null || reps > (existing.prReps ?: 0)
            val isVolumePR = existing == null || volume > (existing.prVolumeKg ?: 0.0)
            val isVelocityPR = velocity != null && (existing == null || velocity > (existing.prVelocity ?: 0.0))

            if (isWeightPR) prsAchieved.add(PRType.WEIGHT)
            if (isVolumePR) prsAchieved.add(PRType.VOLUME)
            if (isVelocityPR) prsAchieved.add(PRType.VELOCITY)

            // Calculate aggregates
            val newTotalVolume = (existing?.totalVolumeKg ?: 0.0) + volume
            val newTotalSets = (existing?.totalSets ?: 0) + 1
            val newTotalReps = (existing?.totalReps ?: 0) + reps
            val is1rmPR = existing == null || estimated1rm > (existing.estimated1rmKg ?: 0.0)

            // Build update data using serializable class
            val data = ExerciseStatisticsUpsert(
                user_id = userId,
                exercise_id = exerciseId,
                exercise_name = exerciseName,
                updated_at = now,
                // PRs
                pr_weight_kg = if (isWeightPR) weightKg else existing?.prWeightKg,
                pr_weight_reps = if (isWeightPR) reps else existing?.prWeightReps,
                pr_weight_date = if (isWeightPR) now else existing?.prWeightDate,
                pr_weight_session_id = if (isWeightPR) sessionId else existing?.prWeightSessionId,
                pr_reps = if (isRepsPR) reps else existing?.prReps,
                pr_reps_weight_kg = if (isRepsPR) weightKg else existing?.prRepsWeightKg,
                pr_reps_date = if (isRepsPR) now else existing?.prRepsDate,
                pr_reps_session_id = if (isRepsPR) sessionId else existing?.prRepsSessionId,
                pr_volume_kg = if (isVolumePR) volume else existing?.prVolumeKg,
                pr_volume_date = if (isVolumePR) now else existing?.prVolumeDate,
                pr_volume_session_id = if (isVolumePR) sessionId else existing?.prVolumeSessionId,
                pr_velocity = if (isVelocityPR && velocity != null) velocity else existing?.prVelocity,
                pr_velocity_date = if (isVelocityPR && velocity != null) now else existing?.prVelocityDate,
                pr_velocity_session_id = if (isVelocityPR && velocity != null) sessionId else existing?.prVelocitySessionId,
                // Estimated 1RM
                estimated_1rm_kg = if (is1rmPR) estimated1rm else existing?.estimated1rmKg,
                estimated_1rm_date = if (is1rmPR) now else existing?.estimated1rmDate,
                // Aggregates
                total_volume_kg = newTotalVolume,
                total_sets = newTotalSets,
                total_reps = newTotalReps,
                last_performed_at = now,
                first_performed_at = existing?.firstPerformedAt ?: now,
                total_sessions = existing?.totalSessions ?: 1
            )

            // Upsert statistics - use onConflict to specify which columns to match
            supabase.from("exercise_statistics").upsert(data, onConflict = "user_id,exercise_id")

            // Record PRs in history
            for (prType in prsAchieved) {
                recordPR(
                    userId = userId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    sessionId = sessionId,
                    prType = prType,
                    weightKg = weightKg,
                    reps = reps,
                    volumeKg = volume,
                    velocity = velocity,
                    previousValue = when (prType) {
                        PRType.WEIGHT -> existing?.prWeightKg
                        PRType.VOLUME -> existing?.prVolumeKg
                        PRType.VELOCITY -> existing?.prVelocity
                        else -> null
                    }
                )
            }

            Log.d(TAG, "Statistics updated. PRs achieved: ${prsAchieved.size}")
            Result.success(prsAchieved)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating exercise statistics: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ========================================
    // PR HISTORY
    // ========================================

    /**
     * Record a new PR
     */
    private suspend fun recordPR(
        userId: String,
        exerciseId: String,
        exerciseName: String?,
        sessionId: String,
        prType: PRType,
        weightKg: Double?,
        reps: Int?,
        volumeKg: Double?,
        velocity: Double?,
        previousValue: Double?
    ) {
        try {
            val now = java.time.Instant.now().toString()
            val currentValue = when (prType) {
                PRType.WEIGHT -> weightKg
                PRType.VOLUME -> volumeKg
                PRType.VELOCITY -> velocity
                else -> null
            }
            val improvement = if (previousValue != null && previousValue > 0 && currentValue != null) {
                ((currentValue - previousValue) / previousValue) * 100
            } else null

            val data = PRHistoryInsert(
                id = UUID.randomUUID().toString(),
                user_id = userId,
                exercise_id = exerciseId,
                exercise_name = exerciseName,
                session_id = sessionId,
                pr_type = prType.name.lowercase(),
                achieved_at = now,
                weight_kg = weightKg,
                reps = reps,
                volume_kg = volumeKg,
                velocity = velocity,
                previous_pr_value = previousValue,
                improvement_percentage = improvement
            )

            supabase.from("pr_history").insert(data)
            Log.d(TAG, "PR recorded: $prType for ${exerciseName ?: exerciseId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording PR: ${e.message}", e)
        }
    }

    /**
     * Get recent PRs for user
     */
    suspend fun getRecentPRs(
        userId: String,
        limit: Int = 10
    ): Result<List<PRHistory>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading recent PRs")

            val result = supabase.from("pr_history")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("achieved_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<PRHistory>()

            Log.d(TAG, "Loaded ${result.size} recent PRs")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent PRs: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get PRs for a specific exercise
     */
    suspend fun getPRsForExercise(
        userId: String,
        exerciseId: String
    ): Result<List<PRHistory>> = withContext(Dispatchers.IO) {
        try {
            val result = supabase.from("pr_history")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("exercise_id", exerciseId)
                    }
                    order("achieved_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<PRHistory>()

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading exercise PRs: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ========================================
    // USER TRAINING SUMMARY
    // ========================================

    /**
     * Get user training summary
     */
    suspend fun getUserTrainingSummary(userId: String): Result<UserTrainingSummary?> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading user training summary")

                val result = supabase.from("user_training_summary")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserTrainingSummary>()

                Log.d(TAG, "Training summary loaded: ${result != null}")
                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading training summary: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Update user training summary (call after each workout)
     */
    suspend fun updateUserTrainingSummary(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating user training summary")

                // Call the database function
                supabase.from("user_training_summary").select {
                    filter {
                        eq("user_id", userId)
                    }
                }

                // The actual update is done by the SQL function
                // For now, we'll do a simple upsert with basic data
                val now = java.time.Instant.now().toString()

                val data = UserTrainingSummaryUpsert(
                    user_id = userId,
                    updated_at = now
                )
                supabase.from("user_training_summary").upsert(data)

                Log.d(TAG, "Training summary updated")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating training summary: ${e.message}", e)
                Result.failure(e)
            }
        }

    // ========================================
    // HELPER FUNCTIONS
    // ========================================

    /**
     * Calculate estimated 1RM using Epley formula
     */
    private fun calculate1RM(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        return weight * (1 + reps.toDouble() / 30)
    }

    /**
     * Get weekly volume data for charts
     */
    suspend fun getWeeklyVolumeData(
        userId: String,
        weeks: Int = 12
    ): Result<List<WeeklyVolumeData>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading weekly volume data")

            // This would ideally use the view, but we'll aggregate from history
            val history = supabase.from("workout_history")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("completed_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit((weeks * 7).toLong()) // Rough limit
                }
                .decodeList<WorkoutHistory>()

            // Group by week
            val weeklyData = history
                .filter { it.completedYear != null && it.completedWeek != null }
                .groupBy { Pair(it.completedYear!!, it.completedWeek!!) }
                .map { (yearWeek, workouts) ->
                    WeeklyVolumeData(
                        year = yearWeek.first,
                        week = yearWeek.second,
                        workouts = workouts.size,
                        volumeKg = workouts.sumOf { it.totalVolumeKg },
                        sets = workouts.sumOf { it.totalSets },
                        reps = workouts.sumOf { it.totalReps },
                        avgDuration = workouts.mapNotNull { it.durationMinutes }.average().takeIf { !it.isNaN() },
                        prs = workouts.sumOf { it.prsAchieved }
                    )
                }
                .sortedByDescending { it.year * 100 + it.week }
                .take(weeks)

            Log.d(TAG, "Loaded ${weeklyData.size} weeks of volume data")
            Result.success(weeklyData)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading weekly volume: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ========================================
    // LAST USED WEIGHTS (for pre-filling workouts)
    // ========================================

    /**
     * Data class for last performance on an exercise
     */
    data class LastExercisePerformance(
        val exerciseId: String,
        val lastWeightKg: Double,
        val lastReps: Int,
        val performedAt: String
    )

    /**
     * Get last used weights for a list of exercises
     * Returns the most recent completed set for each exercise
     */
    suspend fun getLastUsedWeights(
        userId: String,
        exerciseIds: List<String>
    ): Result<Map<String, LastExercisePerformance>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading last used weights for ${exerciseIds.size} exercises (user: $userId)")

            if (exerciseIds.isEmpty()) {
                return@withContext Result.success(emptyMap())
            }

            // Step 1: Get all session IDs for this user
            val userSessions = supabase.from("workout_sessions")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<UserSessionRecord>()
                .map { it.id }

            Log.d(TAG, "Found ${userSessions.size} sessions for user")

            if (userSessions.isEmpty()) {
                Log.d(TAG, "No sessions found for user, returning empty")
                return@withContext Result.success(emptyMap())
            }

            val resultMap = mutableMapOf<String, LastExercisePerformance>()

            // Step 2: Query workout_sets for the most recent completed set per exercise
            // Filter by user's session IDs
            for (exerciseId in exerciseIds) {
                try {
                    val sets = supabase.from("workout_sets")
                        .select {
                            filter {
                                eq("exercise_id", exerciseId)
                                isIn("session_id", userSessions)
                                neq("weight_kg", 0) // Only sets with actual weight
                            }
                            order("completed_at", Order.DESCENDING)
                            limit(1)
                        }
                        .decodeList<WorkoutSetRecord>()

                    sets.firstOrNull()?.let { set ->
                        if (set.weightKg != null && set.reps != null && set.completedAt != null) {
                            resultMap[exerciseId] = LastExercisePerformance(
                                exerciseId = exerciseId,
                                lastWeightKg = set.weightKg,
                                lastReps = set.reps,
                                performedAt = set.completedAt
                            )
                            Log.d(TAG, "Found last weight for $exerciseId: ${set.weightKg}kg x ${set.reps}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not load last weight for $exerciseId: ${e.message}")
                }
            }

            Log.d(TAG, "Loaded last weights for ${resultMap.size}/${exerciseIds.size} exercises")
            Result.success(resultMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading last used weights: ${e.message}", e)
            Result.failure(e)
        }
    }

    @kotlinx.serialization.Serializable
    private data class UserSessionRecord(
        val id: String
    )

    /**
     * Get last performance from exercise_statistics (PR data)
     * Fallback if workout_sets doesn't have data
     */
    suspend fun getLastPerformanceFromStats(
        userId: String,
        exerciseIds: List<String>
    ): Result<Map<String, LastExercisePerformance>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading last performance from stats for ${exerciseIds.size} exercises")

            val resultMap = mutableMapOf<String, LastExercisePerformance>()

            for (exerciseId in exerciseIds) {
                try {
                    val stats = supabase.from("exercise_statistics")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("exercise_id", exerciseId)
                            }
                        }
                        .decodeSingleOrNull<ExerciseStatistics>()

                    stats?.let {
                        if (it.prWeightKg != null && it.prWeightReps != null) {
                            resultMap[exerciseId] = LastExercisePerformance(
                                exerciseId = exerciseId,
                                lastWeightKg = it.prWeightKg,
                                lastReps = it.prWeightReps,
                                performedAt = it.lastPerformedAt ?: ""
                            )
                            Log.d(TAG, "Found PR weight for $exerciseId: ${it.prWeightKg}kg x ${it.prWeightReps}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not load stats for $exerciseId: ${e.message}")
                }
            }

            Log.d(TAG, "Loaded stats for ${resultMap.size}/${exerciseIds.size} exercises")
            Result.success(resultMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading exercise stats: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Database row structure for workout_sets query
 */
@kotlinx.serialization.Serializable
private data class WorkoutSetRecord(
    @kotlinx.serialization.SerialName("exercise_id")
    val exerciseId: String,
    @kotlinx.serialization.SerialName("weight_kg")
    val weightKg: Double? = null,
    val reps: Int? = null,
    @kotlinx.serialization.SerialName("completed_at")
    val completedAt: String? = null
)

// ========================================
// SERIALIZABLE DATA CLASSES FOR INSERT/UPSERT
// ========================================

@kotlinx.serialization.Serializable
data class WorkoutHistoryInsert(
    val id: String,
    val session_id: String,
    val user_id: String,
    val workout_name: String,
    val workout_template_id: String? = null,
    val program_id: String? = null,
    val program_week: Int? = null,
    val program_day: Int? = null,
    val total_volume_kg: Double,
    val total_sets: Int,
    val total_reps: Int,
    val total_exercises: Int,
    val duration_minutes: Int? = null,
    val prs_achieved: Int = 0,
    val pr_exercises: List<String>? = null,
    val avg_velocity: Double? = null,
    val notes: String? = null,
    val started_at: String? = null,
    val completed_at: String
)

@kotlinx.serialization.Serializable
data class ExerciseStatisticsUpsert(
    val user_id: String,
    val exercise_id: String,
    val exercise_name: String? = null,
    val updated_at: String,
    val pr_weight_kg: Double? = null,
    val pr_weight_reps: Int? = null,
    val pr_weight_date: String? = null,
    val pr_weight_session_id: String? = null,
    val pr_reps: Int? = null,
    val pr_reps_weight_kg: Double? = null,
    val pr_reps_date: String? = null,
    val pr_reps_session_id: String? = null,
    val pr_volume_kg: Double? = null,
    val pr_volume_date: String? = null,
    val pr_volume_session_id: String? = null,
    val pr_velocity: Double? = null,
    val pr_velocity_date: String? = null,
    val pr_velocity_session_id: String? = null,
    val estimated_1rm_kg: Double? = null,
    val estimated_1rm_date: String? = null,
    val total_volume_kg: Double? = null,
    val total_sets: Int? = null,
    val total_reps: Int? = null,
    val last_performed_at: String? = null,
    val first_performed_at: String? = null,
    val total_sessions: Int? = null
)

@kotlinx.serialization.Serializable
data class PRHistoryInsert(
    val id: String,
    val user_id: String,
    val exercise_id: String,
    val exercise_name: String? = null,
    val session_id: String,
    val pr_type: String,
    val achieved_at: String,
    val weight_kg: Double? = null,
    val reps: Int? = null,
    val volume_kg: Double? = null,
    val velocity: Double? = null,
    val previous_pr_value: Double? = null,
    val improvement_percentage: Double? = null
)

@kotlinx.serialization.Serializable
data class UserTrainingSummaryUpsert(
    val user_id: String,
    val updated_at: String
)
