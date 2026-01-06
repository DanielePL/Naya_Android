package com.example.menotracker.data

import android.util.Log
import com.example.menotracker.data.models.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository for Training Programs (multi-week workout cycles)
 */
class TrainingProgramRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TAG = "TrainingProgramRepo"
    }

    // ========================================
    // PROGRAM CRUD
    // ========================================

    /**
     * Create a new training program
     */
    suspend fun createProgram(
        userId: String,
        name: String,
        description: String? = null,
        durationWeeks: Int = 4,
        workoutsPerWeek: Int = 3,
        goal: ProgramGoal? = null,
        programType: ProgramType? = null,
        difficultyLevel: DifficultyLevel? = null,
        sports: List<String>? = null
    ): Result<TrainingProgram> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating program: $name")

            val programId = UUID.randomUUID().toString()
            val now = java.time.Instant.now().toString()

            val data = buildMap<String, Any?> {
                put("id", programId)
                put("user_id", userId)
                put("name", name)
                put("duration_weeks", durationWeeks)
                put("workouts_per_week", workoutsPerWeek)
                put("total_sessions_planned", durationWeeks * workoutsPerWeek)
                put("created_at", now)
                put("updated_at", now)

                description?.let { put("description", it) }
                goal?.let { put("goal", it.name.lowercase()) }
                programType?.let { put("program_type", it.name.lowercase()) }
                difficultyLevel?.let { put("difficulty_level", it.name.lowercase()) }
                sports?.let { put("sports", it) }
            }

            supabase.from("training_programs").insert(data)

            // Fetch the created program
            val program = supabase.from("training_programs")
                .select {
                    filter {
                        eq("id", programId)
                    }
                }
                .decodeSingle<TrainingProgram>()

            Log.d(TAG, "Program created: $programId")
            Result.success(program)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating program: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all programs for user
     */
    suspend fun getPrograms(userId: String): Result<List<TrainingProgram>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading programs for user")

                val result = supabase.from("training_programs")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<TrainingProgram>()

                Log.d(TAG, "Loaded ${result.size} programs")
                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading programs: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Get active program for user
     */
    suspend fun getActiveProgram(userId: String): Result<TrainingProgram?> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading active program")

                val result = supabase.from("training_programs")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("is_active", true)
                        }
                    }
                    .decodeSingleOrNull<TrainingProgram>()

                Log.d(TAG, "Active program: ${result?.name ?: "None"}")
                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading active program: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Get program by ID with all workouts
     */
    suspend fun getProgramWithWorkouts(programId: String): Result<ProgramWithWorkouts?> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading program with workouts: $programId")

                // Get program
                val program = supabase.from("training_programs")
                    .select {
                        filter {
                            eq("id", programId)
                        }
                    }
                    .decodeSingleOrNull<TrainingProgram>()

                if (program == null) {
                    Log.w(TAG, "Program not found: $programId")
                    return@withContext Result.success(null)
                }

                // Get program workouts
                val workouts = supabase.from("program_workouts")
                    .select {
                        filter {
                            eq("program_id", programId)
                        }
                        order("order_in_program", Order.ASCENDING)
                    }
                    .decodeList<ProgramWorkout>()

                // Get workout templates for these workouts
                val templateIds = workouts.map { it.workoutTemplateId }.distinct()
                val templates = if (templateIds.isNotEmpty()) {
                    supabase.from("workout_templates")
                        .select {
                            filter {
                                isIn("id", templateIds)
                            }
                        }
                        .decodeList<WorkoutTemplateDto>()
                        .associateBy { it.id }
                } else {
                    emptyMap()
                }

                // Combine workouts with templates
                val workoutsWithTemplates = workouts.map { pw ->
                    ProgramWorkoutWithTemplate(
                        programWorkout = pw,
                        workoutTemplate = templates[pw.workoutTemplateId]
                    )
                }

                // Find current workout
                val currentWorkout = workoutsWithTemplates.firstOrNull {
                    !it.programWorkout.isCompleted &&
                    it.programWorkout.weekNumber == program.currentWeek
                }

                val result = ProgramWithWorkouts(
                    program = program,
                    workouts = workoutsWithTemplates,
                    currentWorkout = currentWorkout
                )

                Log.d(TAG, "Loaded program with ${workouts.size} workouts")
                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading program with workouts: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Activate a program (deactivates others)
     */
    suspend fun activateProgram(userId: String, programId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Activating program: $programId")

                val now = java.time.Instant.now().toString()

                // Deactivate all other programs
                supabase.from("training_programs").update(
                    mapOf(
                        "is_active" to false,
                        "updated_at" to now
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("is_active", true)
                    }
                }

                // Activate this program
                supabase.from("training_programs").update(
                    mapOf(
                        "is_active" to true,
                        "status" to "in_progress",
                        "started_at" to now,
                        "updated_at" to now
                    )
                ) {
                    filter {
                        eq("id", programId)
                    }
                }

                Log.d(TAG, "Program activated")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error activating program: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Pause program
     */
    suspend fun pauseProgram(programId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = java.time.Instant.now().toString()

            supabase.from("training_programs").update(
                mapOf(
                    "status" to "paused",
                    "paused_at" to now,
                    "updated_at" to now
                )
            ) {
                filter {
                    eq("id", programId)
                }
            }

            Log.d(TAG, "Program paused")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing program: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Complete program
     */
    suspend fun completeProgram(programId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = java.time.Instant.now().toString()

            supabase.from("training_programs").update(
                mapOf(
                    "status" to "completed",
                    "is_active" to false,
                    "completed_at" to now,
                    "updated_at" to now
                )
            ) {
                filter {
                    eq("id", programId)
                }
            }

            Log.d(TAG, "Program completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing program: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete program
     */
    suspend fun deleteProgram(programId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("training_programs").delete {
                filter {
                    eq("id", programId)
                }
            }

            Log.d(TAG, "Program deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting program: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update program name
     */
    suspend fun updateProgramName(programId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("training_programs").update({
                set("name", newName)
                set("updated_at", java.time.Instant.now().toString())
            }) {
                filter {
                    eq("id", programId)
                }
            }

            Log.d(TAG, "Program name updated to: $newName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating program name: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ========================================
    // PROGRAM WORKOUTS
    // ========================================

    /**
     * Add workout to program
     */
    suspend fun addWorkoutToProgram(
        programId: String,
        workoutTemplateId: String,
        weekNumber: Int,
        dayNumber: Int,
        orderInProgram: Int,
        dayOfWeek: Int? = null,
        progressionNotes: String? = null,
        intensityModifier: Double = 1.0
    ): Result<ProgramWorkout> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding workout to program: Week $weekNumber, Day $dayNumber")

            val id = UUID.randomUUID().toString()
            val now = java.time.Instant.now().toString()

            val data = buildMap<String, Any?> {
                put("id", id)
                put("program_id", programId)
                put("workout_template_id", workoutTemplateId)
                put("week_number", weekNumber)
                put("day_number", dayNumber)
                put("order_in_program", orderInProgram)
                put("created_at", now)

                dayOfWeek?.let { put("day_of_week", it) }
                progressionNotes?.let { put("progression_notes", it) }
                if (intensityModifier != 1.0) put("intensity_modifier", intensityModifier)
            }

            supabase.from("program_workouts").insert(data)

            val result = supabase.from("program_workouts")
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<ProgramWorkout>()

            Log.d(TAG, "Workout added to program")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding workout to program: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mark program workout as completed
     */
    suspend fun completeProgramWorkout(
        programWorkoutId: String,
        sessionId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = java.time.Instant.now().toString()

            supabase.from("program_workouts").update(
                mapOf(
                    "is_completed" to true,
                    "completed_at" to now,
                    "session_id" to sessionId
                )
            ) {
                filter {
                    eq("id", programWorkoutId)
                }
            }

            Log.d(TAG, "Program workout completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing program workout: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get next workout in program
     */
    suspend fun getNextProgramWorkout(programId: String): Result<ProgramWorkoutWithTemplate?> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting next workout for program")

                val workout = supabase.from("program_workouts")
                    .select {
                        filter {
                            eq("program_id", programId)
                            eq("is_completed", false)
                        }
                        order("order_in_program", Order.ASCENDING)
                        limit(1)
                    }
                    .decodeSingleOrNull<ProgramWorkout>()

                if (workout == null) {
                    return@withContext Result.success(null)
                }

                // Get template
                val template = supabase.from("workout_templates")
                    .select {
                        filter {
                            eq("id", workout.workoutTemplateId)
                        }
                    }
                    .decodeSingleOrNull<WorkoutTemplateDto>()

                Result.success(ProgramWorkoutWithTemplate(workout, template))
            } catch (e: Exception) {
                Log.e(TAG, "Error getting next workout: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Update program progress after completing a workout
     */
    suspend fun updateProgramProgress(
        programId: String,
        volumeKg: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating program progress")

            // Get current program
            val program = supabase.from("training_programs")
                .select {
                    filter {
                        eq("id", programId)
                    }
                }
                .decodeSingle<TrainingProgram>()

            val now = java.time.Instant.now().toString()
            val newCompleted = program.totalSessionsCompleted + 1
            val newVolume = program.totalVolumeKg + volumeKg

            // Calculate new week/day
            val workoutsPerWeek = program.workoutsPerWeek
            val completedInCurrentWeek = newCompleted % workoutsPerWeek
            val newWeek = if (completedInCurrentWeek == 0 && newCompleted > 0) {
                minOf(program.currentWeek + 1, program.durationWeeks)
            } else {
                program.currentWeek
            }

            val updateData = mutableMapOf<String, Any>(
                "total_sessions_completed" to newCompleted,
                "total_volume_kg" to newVolume,
                "current_week" to newWeek,
                "updated_at" to now
            )

            // Check if program is completed
            if (newCompleted >= program.totalSessionsPlanned) {
                updateData["status"] = "completed"
                updateData["is_active"] = false
                updateData["completed_at"] = now
            }

            supabase.from("training_programs").update(updateData) {
                filter {
                    eq("id", programId)
                }
            }

            Log.d(TAG, "Program progress updated: $newCompleted/${program.totalSessionsPlanned}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating program progress: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get workouts for a specific week
     */
    suspend fun getWorkoutsForWeek(
        programId: String,
        weekNumber: Int
    ): Result<List<ProgramWorkoutWithTemplate>> = withContext(Dispatchers.IO) {
        try {
            val workouts = supabase.from("program_workouts")
                .select {
                    filter {
                        eq("program_id", programId)
                        eq("week_number", weekNumber)
                    }
                    order("day_number", Order.ASCENDING)
                }
                .decodeList<ProgramWorkout>()

            val templateIds = workouts.map { it.workoutTemplateId }.distinct()
            val templates = if (templateIds.isNotEmpty()) {
                supabase.from("workout_templates")
                    .select {
                        filter {
                            isIn("id", templateIds)
                        }
                    }
                    .decodeList<WorkoutTemplateDto>()
                    .associateBy { it.id }
            } else {
                emptyMap()
            }

            val result = workouts.map { pw ->
                ProgramWorkoutWithTemplate(pw, templates[pw.workoutTemplateId])
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting week workouts: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ========================================
    // PROGRAM CREATION HELPERS
    // ========================================

    /**
     * Create a program from workout templates
     * Automatically schedules workouts across weeks
     */
    suspend fun createProgramFromTemplates(
        userId: String,
        name: String,
        description: String?,
        durationWeeks: Int,
        workoutTemplateIds: List<String>,
        goal: ProgramGoal? = null
    ): Result<TrainingProgram> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating program from ${workoutTemplateIds.size} templates")

            // Create the program
            val programResult = createProgram(
                userId = userId,
                name = name,
                description = description,
                durationWeeks = durationWeeks,
                workoutsPerWeek = workoutTemplateIds.size,
                goal = goal
            )

            if (programResult.isFailure) {
                return@withContext programResult
            }

            val program = programResult.getOrThrow()

            // Schedule workouts for each week
            var orderInProgram = 1
            for (week in 1..durationWeeks) {
                workoutTemplateIds.forEachIndexed { index, templateId ->
                    addWorkoutToProgram(
                        programId = program.id,
                        workoutTemplateId = templateId,
                        weekNumber = week,
                        dayNumber = index + 1,
                        orderInProgram = orderInProgram++
                    )
                }
            }

            Log.d(TAG, "Program created with ${durationWeeks * workoutTemplateIds.size} scheduled workouts")
            Result.success(program)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating program from templates: ${e.message}", e)
            Result.failure(e)
        }
    }
}
