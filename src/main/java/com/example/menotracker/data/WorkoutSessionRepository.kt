// app/src/main/java/com/example/myapplicationtest/data/WorkoutSessionRepository.kt

package com.example.menotracker.data

import android.util.Log
import com.example.menotracker.community.data.GamificationRepository
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.viewmodels.VelocityMetricsData
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for active workout session data
 * Handles persistent storage of workout sessions, reps, weights, and video data
 */
class WorkoutSessionRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TAG = "WorkoutSessionRepo"
    }

    /**
     * Create a new workout session in the database
     * @param sessionId Unique ID for the session (e.g., workout template ID)
     * @param workoutName Name of the workout
     * @param userId User ID from auth
     * @return Result with the session ID if successful
     */
    suspend fun createWorkoutSession(
        sessionId: String,
        workoutName: String,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔵 createWorkoutSession START: sessionId=$sessionId, userId=$userId, workoutName=$workoutName")

            val sessionData = WorkoutSessionInsert(
                id = sessionId,
                user_id = userId,
                workout_name = workoutName,
                started_at = java.time.Instant.now().toString()
            )
            Log.d(TAG, "🔵 Session data: $sessionData")

            // Use upsert to handle re-starting the same workout template
            // (will insert if ID doesn't exist, update if it does)
            supabase.from("workout_sessions").upsert(sessionData)

            Log.d(TAG, "✅ Workout session created/updated successfully: $sessionId")
            Result.success(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating workout session: ${e.message}", e)
            Log.e(TAG, "❌ Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }

    /**
     * Complete a workout session
     * @param sessionId Session ID to complete
     * @param durationMinutes Total workout duration in minutes
     * @param notes Optional notes about the workout
     * @param userId User ID for gamification tracking
     * @return Result indicating success or failure
     */
    suspend fun completeWorkoutSession(
        sessionId: String,
        durationMinutes: Int,
        notes: String? = null,
        userId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "✅ Completing workout session: $sessionId (duration: ${durationMinutes}min)")

            val updateData = WorkoutSessionUpdate(
                completed_at = java.time.Instant.now().toString(),
                duration_minutes = durationMinutes,
                notes = notes
            )

            supabase.from("workout_sessions").update(updateData) {
                filter {
                    eq("id", sessionId)
                }
            }

            Log.d(TAG, "✅ Workout session completed successfully")

            // Award gamification XP and update streak
            if (CommunityFeatureFlag.ENABLED && userId != null) {
                try {
                    Log.d(TAG, "🎮 Awarding gamification rewards...")
                    GamificationRepository.awardWorkoutXp(userId, sessionId)
                    GamificationRepository.updateStreak(userId)
                    GamificationRepository.checkAndAwardBadge(userId, "first_workout")
                    Log.d(TAG, "🎮 Gamification rewards awarded successfully")
                } catch (e: Exception) {
                    // Don't fail the whole operation if gamification fails
                    Log.e(TAG, "⚠️ Gamification error (non-fatal): ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error completing workout session: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update reps and weight for a workout set
     */
    suspend fun updateSetRepsWeight(
        setId: String,
        reps: Int?,
        weightKg: Double?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 Updating set $setId: reps=$reps, weight=$weightKg")

            supabase.from("workout_sets").update(
                buildMap<String, Any> {
                    if (reps != null) put("actual_reps", reps)
                    if (weightKg != null) put("actual_weight_kg", weightKg)
                }
            ) {
                filter {
                    eq("id", setId)
                }
            }

            Log.d(TAG, "✅ Set data updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating set data: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse setId to extract session_id, exercise_id, set_number
     * Format: "sessionId_exerciseId_setN"
     * Note: exerciseId may contain underscores (e.g., "bench_press")
     */
    private fun parseSetId(setId: String): Triple<String, String, Int>? {
        return try {
            Log.d(TAG, "🔍 parseSetId INPUT: $setId")

            // Find the last "_setN" pattern
            val setPattern = Regex("_set(\\d+)$")
            val setMatch = setPattern.find(setId)

            if (setMatch == null) {
                Log.e(TAG, "❌ No _setN pattern found in: $setId")
                return null
            }

            val setNumber = setMatch.groupValues[1].toInt()
            val withoutSet = setId.substring(0, setMatch.range.first)

            // Find first underscore to separate sessionId from exerciseId
            val firstUnderscore = withoutSet.indexOf('_')
            if (firstUnderscore == -1) {
                Log.e(TAG, "❌ No underscore found in: $withoutSet")
                return null
            }

            val sessionId = withoutSet.substring(0, firstUnderscore)
            val exerciseId = withoutSet.substring(firstUnderscore + 1)

            Log.d(TAG, "✅ parseSetId OUTPUT: sessionId=$sessionId, exerciseId=$exerciseId, setNumber=$setNumber")
            Triple(sessionId, exerciseId, setNumber)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse setId: $setId", e)
            null
        }
    }

    /**
     * Update current input values (work-in-progress)
     * Uses SELECT → UPDATE/INSERT pattern since unique constraint may not exist
     */
    suspend fun updateSetCurrentInputs(
        setId: String,
        currentReps: String,
        currentWeight: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 Updating current inputs for set $setId")

            val parsed = parseSetId(setId)
            if (parsed == null) {
                Log.e(TAG, "Invalid setId format: $setId")
                return@withContext Result.failure(Exception("Invalid setId format"))
            }

            val (sessionId, exerciseId, setNumber) = parsed

            // Check if row exists
            val existing = supabase.from("workout_sets")
                .select() {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
                .decodeList<WorkoutSetRow>()

            if (existing.isNotEmpty()) {
                // UPDATE existing row
                supabase.from("workout_sets").update(
                    mapOf(
                        "current_reps_input" to currentReps,
                        "current_weight_input" to currentWeight
                    )
                ) {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
            } else {
                // INSERT new row
                val setData = WorkoutSetInsert(
                    session_id = sessionId,
                    exercise_id = exerciseId,
                    set_number = setNumber,
                    current_reps_input = currentReps,
                    current_weight_input = currentWeight
                )
                supabase.from("workout_sets").insert(setData)
            }

            Log.d(TAG, "✅ Current inputs updated (session=$sessionId, exercise=$exerciseId, set=$setNumber)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating current inputs: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mark set as completed
     * Uses SELECT → UPDATE/INSERT pattern since unique constraint may not exist
     * @param isFailed If true, marks this set as a failed attempt (missed lift, etc.)
     */
    suspend fun completeSet(
        setId: String,
        reps: Int,
        weightKg: Double,
        isFailed: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val statusEmoji = if (isFailed) "❌" else "✅"
            Log.d(TAG, "$statusEmoji completeSet START: setId=$setId, reps=$reps, weight=$weightKg, failed=$isFailed")

            val parsed = parseSetId(setId)
            if (parsed == null) {
                Log.e(TAG, "❌ Invalid setId format: $setId")
                return@withContext Result.failure(Exception("Invalid setId format"))
            }

            val (sessionId, exerciseId, setNumber) = parsed
            Log.d(TAG, "🔵 Parsed: sessionId=$sessionId, exerciseId=$exerciseId, setNumber=$setNumber")

            val now = java.time.Instant.now().toString()

            // Check if row exists
            val existing = supabase.from("workout_sets")
                .select() {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
                .decodeList<WorkoutSetRow>()

            if (existing.isNotEmpty()) {
                // UPDATE existing row
                Log.d(TAG, "🔵 Updating existing row...")
                supabase.from("workout_sets").update(
                    mapOf(
                        "reps" to reps,
                        "weight_kg" to weightKg,
                        "completed_at" to now,
                        "is_failed" to isFailed
                    )
                ) {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
            } else {
                // INSERT new row
                Log.d(TAG, "🔵 Inserting new row...")
                val setData = WorkoutSetInsert(
                    session_id = sessionId,
                    exercise_id = exerciseId,
                    set_number = setNumber,
                    reps = reps,
                    weight_kg = weightKg,
                    completed_at = now,
                    is_failed = isFailed
                )
                supabase.from("workout_sets").insert(setData)
            }

            val statusText = if (isFailed) "FAILED" else "completed"
            Log.d(TAG, "$statusEmoji Set $statusText successfully (session=$sessionId, exercise=$exerciseId, set=$setNumber)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error completing set: ${e.message}", e)
            Log.e(TAG, "❌ Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }

    /**
     * Save video URL for a set
     * Called when video is recorded and saved (to device or cloud)
     * Uses SELECT → UPDATE/INSERT pattern since unique constraint may not exist
     */
    suspend fun saveVideoUrl(
        setId: String,
        videoUrl: String,
        storageType: String  // "device" or "cloud"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📹 Saving video URL for set $setId")
            Log.d(TAG, "   URL: $videoUrl")
            Log.d(TAG, "   Storage Type: $storageType")

            val parsed = parseSetId(setId)
            if (parsed == null) {
                Log.e(TAG, "❌ Invalid setId format: $setId")
                return@withContext Result.failure(Exception("Invalid setId format"))
            }

            val (sessionId, exerciseId, setNumber) = parsed

            // Check if row exists
            val existing = supabase.from("workout_sets")
                .select() {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
                .decodeList<WorkoutSetRow>()

            if (existing.isNotEmpty()) {
                // UPDATE existing row
                Log.d(TAG, "   Updating existing row...")
                supabase.from("workout_sets").update(
                    mapOf(
                        "video_url" to videoUrl,
                        "video_storage_type" to storageType
                    )
                ) {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
            } else {
                // INSERT new row
                Log.d(TAG, "   Inserting new row...")
                val setData = WorkoutSetInsert(
                    session_id = sessionId,
                    exercise_id = exerciseId,
                    set_number = setNumber,
                    video_url = videoUrl,
                    video_storage_type = storageType
                )
                supabase.from("workout_sets").insert(setData)
            }

            Log.d(TAG, "✅ Video URL saved for set (session=$sessionId, exercise=$exerciseId, set=$setNumber)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving video URL: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Save velocity metrics from VBT analysis to workout_sets
     * Called after backend VBT processing completes
     */
    suspend fun saveVelocityMetrics(
        setId: String,
        velocityMetrics: Map<String, Any>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Saving velocity metrics for set $setId")
            Log.d(TAG, "   Metrics: $velocityMetrics")

            val parsed = parseSetId(setId)
            if (parsed == null) {
                Log.e(TAG, "❌ Invalid setId format: $setId")
                return@withContext Result.failure(Exception("Invalid setId format"))
            }

            val (sessionId, exerciseId, setNumber) = parsed

            // Check if row exists
            val existing = supabase.from("workout_sets")
                .select() {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
                .decodeList<WorkoutSetRow>()

            if (existing.isNotEmpty()) {
                // UPDATE existing row with velocity_metrics
                Log.d(TAG, "   Updating existing row with velocity_metrics...")
                supabase.from("workout_sets").update(
                    mapOf("velocity_metrics" to velocityMetrics)
                ) {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
            } else {
                // INSERT new row (should rarely happen - video should already exist)
                Log.d(TAG, "   Inserting new row with velocity_metrics...")
                val setData = WorkoutSetInsert(
                    session_id = sessionId,
                    exercise_id = exerciseId,
                    set_number = setNumber
                )
                supabase.from("workout_sets").insert(setData)
                // Then update with velocity_metrics
                supabase.from("workout_sets").update(
                    mapOf("velocity_metrics" to velocityMetrics)
                ) {
                    filter {
                        eq("session_id", sessionId)
                        eq("exercise_id", exerciseId)
                        eq("set_number", setNumber)
                    }
                }
            }

            Log.d(TAG, "✅ Velocity metrics saved for set (session=$sessionId, exercise=$exerciseId, set=$setNumber)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving velocity metrics: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get set data (reps, weight, video info)
     */
    suspend fun getSetData(setId: String): Result<SetDataDTO> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📖 Loading set data for: $setId")

            val result = supabase.from("workout_sets")
                .select() {
                    filter {
                        eq("id", setId)
                    }
                }

            // TODO: Parse result to SetDataDTO
            // For now, return empty data
            Log.d(TAG, "✅ Set data loaded")
            Result.success(SetDataDTO())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading set data: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get multiple sets data for a workout session
     * @param setIds List of setIds in format "workoutId_exerciseId_setN"
     * @param sessionId The actual session UUID to query (if known)
     * @param workoutId The workout template ID (used for key mapping in response)
     */
    suspend fun getWorkoutSetsData(
        setIds: List<String>,
        sessionId: String? = null,
        workoutId: String? = null
    ): Result<Map<String, SetDataDTO>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📖 Loading data for ${setIds.size} sets, sessionId=$sessionId")

            // If no set IDs, return empty map
            if (setIds.isEmpty()) {
                Log.d(TAG, "No sets to load")
                return@withContext Result.success(emptyMap())
            }

            // If sessionId is provided, query by that (correct approach)
            // Otherwise fall back to parsing from setIds (legacy)
            val querySessionId = sessionId ?: run {
                val parsed = setIds.mapNotNull { parseSetId(it) }
                if (parsed.isEmpty()) {
                    Log.e(TAG, "No valid setIds to parse")
                    return@withContext Result.success(emptyMap())
                }
                parsed.first().first
            }

            Log.d(TAG, "🔍 Querying workout_sets by session_id: $querySessionId")

            // Query by session_id
            val result = supabase.from("workout_sets")
                .select() {
                    filter {
                        eq("session_id", querySessionId)
                    }
                }
                .decodeList<WorkoutSetRow>()

            val dataMap = result.associate { row ->
                // Reconstruct the setId - use workoutId for UI keys if provided, otherwise use session_id
                val keyPrefix = workoutId ?: row.session_id
                val reconstructedSetId = "${keyPrefix}_${row.exercise_id}_set${row.set_number}"
                Log.d(TAG, "📦 Mapping DB row to key: $reconstructedSetId (video=${row.video_url != null}, metrics=${row.velocity_metrics != null})")

                // Extract velocity metrics from JSONB
                val velocityMetrics = row.velocity_metrics?.let { metrics ->
                    try {
                        fun getFloat(key: String): Float? =
                            (metrics[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull()
                        fun getInt(key: String): Int? =
                            (metrics[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
                        fun getString(key: String): String? =
                            (metrics[key] as? kotlinx.serialization.json.JsonPrimitive)?.content

                        VelocityMetricsData(
                            source = getString("source") ?: "yolo_backend",
                            totalReps = getInt("total_reps") ?: 0,
                            avgVelocity = getFloat("avg_velocity") ?: 0f,
                            peakVelocity = getFloat("peak_velocity") ?: 0f,
                            velocityDrop = getFloat("velocity_drop") ?: 0f,
                            techniqueScore = getFloat("technique_score") ?: 0f,
                            totalTUT = getFloat("total_tut") ?: 0f,
                            estimatedOneRM = getFloat("estimated_1rm"),
                            loadPercent = getFloat("load_percent"),
                            fatigueIndex = getFloat("fatigue_index") ?: 0f,
                            overallGrade = getString("overall_grade") ?: "N/A"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing velocity_metrics: ${e.message}")
                        null
                    }
                }

                // Legacy: Extract single velocity value for backwards compatibility
                val vbtVelocity = velocityMetrics?.peakVelocity
                    ?: row.velocity_metrics?.get("avg_peak_velocity")?.let {
                        try {
                            (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull()
                        } catch (e: Exception) {
                            null
                        }
                    }

                reconstructedSetId to SetDataDTO(
                    actualReps = row.reps,
                    actualWeightKg = row.weight_kg,
                    currentRepsInput = row.current_reps_input ?: "",
                    currentWeightInput = row.current_weight_input ?: "",
                    videoUrl = row.video_url,
                    videoStorageType = row.video_storage_type,
                    vbtVelocity = vbtVelocity,
                    velocityMetrics = velocityMetrics,
                    completedAt = row.completed_at,
                    isFailed = row.is_failed ?: false
                )
            }

            Log.d(TAG, "✅ Loaded ${dataMap.size} sets from database")
            Result.success(dataMap)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading sets data: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Data Transfer Object for set data
 */
data class SetDataDTO(
    val actualReps: Int? = null,
    val actualWeightKg: Double? = null,
    val currentRepsInput: String? = null,
    val currentWeightInput: String? = null,
    val videoUrl: String? = null,
    val videoStorageType: String? = null,
    val vbtVelocity: Float? = null,
    val velocityMetrics: VelocityMetricsData? = null,  // Full VBT metrics from backend
    val completedAt: String? = null,
    val isFailed: Boolean = false  // ✅ NEW: Track failed attempts
)

/**
 * Database row structure for workout_sets table
 */
@kotlinx.serialization.Serializable
data class WorkoutSetRow(
    val id: String? = null,
    val session_id: String? = null,
    val exercise_id: String,
    val set_number: Int,
    val reps: Int? = null,
    val weight_kg: Double? = null,
    val current_reps_input: String? = null,
    val current_weight_input: String? = null,
    val video_url: String? = null,
    val video_storage_type: String? = null,
    val velocity_metrics: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    val completed_at: String? = null,
    val is_failed: Boolean? = null  // ✅ NEW: Track failed attempts
)

// Serializable data classes for Supabase upsert/insert operations
@kotlinx.serialization.Serializable
data class WorkoutSessionInsert(
    val id: String,
    val user_id: String,
    val workout_name: String,
    val started_at: String
)

@kotlinx.serialization.Serializable
data class WorkoutSessionUpdate(
    val completed_at: String,
    val duration_minutes: Int,
    val notes: String? = null
)

@kotlinx.serialization.Serializable
data class WorkoutSetInsert(
    val session_id: String,
    val exercise_id: String,
    val set_number: Int,
    val reps: Int? = null,
    val weight_kg: Double? = null,
    val current_reps_input: String? = null,
    val current_weight_input: String? = null,
    val video_url: String? = null,
    val video_storage_type: String? = null,
    val completed_at: String? = null,
    val is_failed: Boolean? = null
)
