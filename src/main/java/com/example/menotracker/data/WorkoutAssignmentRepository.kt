// app/src/main/java/com/example/myapplicationtest/data/WorkoutAssignmentRepository.kt

package com.example.menotracker.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.menotracker.viewmodels.WorkoutTemplate
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.data.ExerciseSet as LocalExerciseSet

/**
 * Repository for Coach-assigned workouts
 * Loads workout_assignments from Supabase where client_id = current user
 */
object WorkoutAssignmentRepository {
    private const val TAG = "WorkoutAssignmentRepo"

    // ============ Data Classes (matching Supabase schema) ============

    @Serializable
    data class WorkoutAssignmentDto(
        val id: String,
        @SerialName("workout_template_id") val workoutTemplateId: String,
        @SerialName("coach_id") val coachId: String,
        @SerialName("client_id") val clientId: String,
        @SerialName("assigned_at") val assignedAt: String? = null,
        @SerialName("scheduled_date") val scheduledDate: String? = null,
        @SerialName("due_date") val dueDate: String? = null,
        val notes: String? = null,
        val status: String = "active",
        @SerialName("completed_at") val completedAt: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    data class WorkoutTemplateDto(
        val id: String,
        val name: String,
        val description: String? = null,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        val sports: List<String>? = null
    )

    @Serializable
    data class WorkoutTemplateExerciseDto(
        val id: String,
        @SerialName("workout_template_id") val workoutTemplateId: String,
        @SerialName("exercise_id") val exerciseId: String,
        @SerialName("order_index") val orderIndex: Int,
        val notes: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    data class ExerciseSetDto(
        val id: String,
        @SerialName("workout_exercise_id") val workoutExerciseId: String,
        @SerialName("set_number") val setNumber: Int,
        @SerialName("target_reps") val targetReps: Int? = null,
        @SerialName("target_weight") val targetWeight: Double? = null,
        @SerialName("rest_seconds") val restSeconds: Int? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    data class ExerciseDetailsDto(
        val id: String,
        val name: String,
        @SerialName("main_muscle_group") val mainMuscleGroup: String? = null,
        val category: String? = null,
        val equipment: List<String>? = null
    )

    @Serializable
    data class CoachProfileDto(
        val id: String,
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("avatar_url") val avatarUrl: String? = null
    )

    // ============ UI Data Classes ============

    data class AssignedWorkout(
        val assignmentId: String,
        val workoutTemplate: WorkoutTemplate,
        val coachName: String?,
        val coachAvatarUrl: String?,
        val assignedAt: String?,
        val scheduledDate: String?,
        val notes: String?,
        val status: String
    )

    // ============ State ============

    private val _assignedWorkouts = MutableStateFlow<List<AssignedWorkout>>(emptyList())
    val assignedWorkouts: StateFlow<List<AssignedWorkout>> = _assignedWorkouts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _latestAssignment = MutableStateFlow<AssignedWorkout?>(null)
    val latestAssignment: StateFlow<AssignedWorkout?> = _latestAssignment.asStateFlow()

    // ============ Methods ============

    /**
     * Load all active workout assignments for the current user
     */
    suspend fun loadAssignedWorkouts(clientId: String): Result<List<AssignedWorkout>> =
        withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                Log.d(TAG, "Loading assigned workouts for client: $clientId")

                // 1. Fetch workout_assignments for this client
                val assignments = SupabaseClient.client
                    .from("workout_assignments")
                    .select {
                        filter {
                            eq("client_id", clientId)
                            eq("status", "active")
                        }
                        order("assigned_at", Order.DESCENDING)
                    }
                    .decodeList<WorkoutAssignmentDto>()

                Log.d(TAG, "Found ${assignments.size} assignments")

                if (assignments.isEmpty()) {
                    _assignedWorkouts.value = emptyList()
                    _latestAssignment.value = null
                    return@withContext Result.success(emptyList())
                }

                // 2. Get unique workout template IDs
                val templateIds = assignments.map { it.workoutTemplateId }.distinct()

                // 3. Fetch workout templates
                val templates = SupabaseClient.client
                    .from("workout_templates")
                    .select()
                    .decodeList<WorkoutTemplateDto>()
                    .filter { it.id in templateIds }

                val templateMap = templates.associateBy { it.id }

                // 4. Fetch exercises for these templates
                val allExercises = SupabaseClient.client
                    .from("workout_template_exercises")
                    .select()
                    .decodeList<WorkoutTemplateExerciseDto>()
                    .filter { it.workoutTemplateId in templateIds }

                val exercisesByTemplate = allExercises.groupBy { it.workoutTemplateId }
                val exerciseIds = allExercises.map { it.id }
                val uniqueExerciseRefIds = allExercises.map { it.exerciseId }.distinct()

                // 5. Fetch sets for these exercises
                val allSets = SupabaseClient.client
                    .from("exercise_sets")
                    .select()
                    .decodeList<ExerciseSetDto>()
                    .filter { it.workoutExerciseId in exerciseIds }

                val setsByExercise = allSets.groupBy { it.workoutExerciseId }

                // 6. Fetch exercise details from exercises_new
                val exerciseDetailsMap = mutableMapOf<String, ExerciseDetailsDto>()
                if (uniqueExerciseRefIds.isNotEmpty()) {
                    try {
                        val details = SupabaseClient.client
                            .from("exercises_new")
                            .select()
                            .decodeList<ExerciseDetailsDto>()
                            .filter { it.id in uniqueExerciseRefIds }

                        details.forEach { exerciseDetailsMap[it.id] = it }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not fetch from exercises_new: ${e.message}")
                    }
                }

                // 7. Fetch coach profiles
                val coachIds = assignments.map { it.coachId }.distinct()
                val coachMap = mutableMapOf<String, CoachProfileDto>()
                if (coachIds.isNotEmpty()) {
                    try {
                        val coaches = SupabaseClient.client
                            .from("profiles")
                            .select()
                            .decodeList<CoachProfileDto>()
                            .filter { it.id in coachIds }

                        coaches.forEach { coachMap[it.id] = it }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not fetch coach profiles: ${e.message}")
                    }
                }

                // 8. Build AssignedWorkout objects
                val assignedWorkouts = assignments.mapNotNull { assignment ->
                    val template = templateMap[assignment.workoutTemplateId] ?: return@mapNotNull null
                    val templateExercises = exercisesByTemplate[template.id] ?: emptyList()
                    val coach = coachMap[assignment.coachId]

                    // Build exercises with sets
                    val exercisesWithSets = templateExercises.sortedBy { it.orderIndex }.mapNotNull { exercise ->
                        val details = exerciseDetailsMap[exercise.exerciseId]
                        val sets = setsByExercise[exercise.id] ?: emptyList()

                        ExerciseWithSets(
                            exerciseId = exercise.exerciseId,
                            exerciseName = details?.name ?: "Unknown Exercise",
                            muscleGroup = details?.mainMuscleGroup ?: details?.category ?: "Unknown",
                            equipment = details?.equipment?.joinToString(", ") ?: "None",
                            order = exercise.orderIndex,
                            sets = sets.sortedBy { it.setNumber }.map { set ->
                                LocalExerciseSet(
                                    id = set.id,
                                    setNumber = set.setNumber,
                                    targetReps = set.targetReps ?: 10,
                                    targetWeight = set.targetWeight ?: 0.0,
                                    restSeconds = set.restSeconds ?: 90
                                )
                            }
                        )
                    }

                    // Create WorkoutTemplate
                    val workoutTemplate = WorkoutTemplate(
                        id = template.id,
                        name = template.name,
                        exercises = exercisesWithSets,
                        createdAt = parseTimestamp(template.createdAt),
                        sports = template.sports
                    )

                    AssignedWorkout(
                        assignmentId = assignment.id,
                        workoutTemplate = workoutTemplate,
                        coachName = coach?.fullName,
                        coachAvatarUrl = coach?.avatarUrl,
                        assignedAt = assignment.assignedAt,
                        scheduledDate = assignment.scheduledDate,
                        notes = assignment.notes,
                        status = assignment.status
                    )
                }

                Log.d(TAG, "Built ${assignedWorkouts.size} assigned workouts")

                _assignedWorkouts.value = assignedWorkouts
                _latestAssignment.value = assignedWorkouts.firstOrNull()

                Result.success(assignedWorkouts)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading assigned workouts: ${e.message}", e)
                Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }

    /**
     * Mark an assignment as completed
     */
    suspend fun markAssignmentCompleted(assignmentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Marking assignment as completed: $assignmentId")

                SupabaseClient.client
                    .from("workout_assignments")
                    .update({
                        set("status", "completed")
                        set("completed_at", java.time.Instant.now().toString())
                    }) {
                        filter {
                            eq("id", assignmentId)
                        }
                    }

                // Remove from active list
                _assignedWorkouts.value = _assignedWorkouts.value.filter { it.assignmentId != assignmentId }
                if (_latestAssignment.value?.assignmentId == assignmentId) {
                    _latestAssignment.value = _assignedWorkouts.value.firstOrNull()
                }

                Log.d(TAG, "Assignment marked as completed")
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Error marking assignment completed: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Clear cached data (call on logout)
     */
    fun clearCache() {
        _assignedWorkouts.value = emptyList()
        _latestAssignment.value = null
    }

    /**
     * Parse ISO timestamp to milliseconds
     */
    private fun parseTimestamp(timestamp: String?): Long {
        return try {
            timestamp?.let {
                java.time.Instant.parse(it).toEpochMilli()
            } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
