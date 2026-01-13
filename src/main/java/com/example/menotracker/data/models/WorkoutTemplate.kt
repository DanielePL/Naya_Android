// app/src/main/java/com/example/myapplicationtest/data/models/WorkoutTemplate.kt

package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase model for workout_templates table
 */
@Serializable
data class WorkoutTemplateDto(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("sports")
    val sports: List<String>? = null,

    @SerialName("intensity")
    val intensity: String? = "AKTIV",

    @SerialName("video_url")
    val videoUrl: String? = null
)

/**
 * Supabase model for workout_template_exercises table
 * Uses live reference to exercises_new via exercise_id
 */
@Serializable
data class WorkoutTemplateExerciseDto(
    @SerialName("id")
    val id: String,

    @SerialName("workout_template_id")
    val workoutTemplateId: String,

    @SerialName("exercise_id")
    val exerciseId: String,

    @SerialName("order_index")
    val orderIndex: Int,

    @SerialName("created_at")
    val createdAt: String
)

/**
 * Extended DTO that includes exercise details from exercises_new via JOIN
 * Used when loading complete workout templates
 */
@Serializable
data class WorkoutTemplateExerciseWithDetails(
    @SerialName("id")
    val id: String,

    @SerialName("workout_template_id")
    val workoutTemplateId: String,

    @SerialName("exercise_id")
    val exerciseId: String,

    @SerialName("order_index")
    val orderIndex: Int,

    @SerialName("created_at")
    val createdAt: String,

    // Exercise details from exercises_new (via JOIN)
    @SerialName("exercise_name")
    val exerciseName: String,

    @SerialName("main_muscle")
    val muscleGroup: String,

    @SerialName("equipment_category")
    val equipment: String
)

/**
 * Supabase model for exercise_sets table
 */
@Serializable
data class ExerciseSetDto(
    @SerialName("id")
    val id: String,

    @SerialName("workout_exercise_id")
    val workoutExerciseId: String,

    @SerialName("set_number")
    val setNumber: Int,

    @SerialName("target_reps")
    val targetReps: Int,

    @SerialName("target_weight")
    val targetWeight: Double,

    @SerialName("rest_seconds")
    val restSeconds: Int,

    @SerialName("created_at")
    val createdAt: String
)

/**
 * Request model for creating a workout template with nested exercises and sets
 */
@Serializable
data class CreateWorkoutTemplateRequest(
    @SerialName("name")
    val name: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("intensity")
    val intensity: String? = "AKTIV"
)

@Serializable
data class CreateWorkoutExerciseRequest(
    @SerialName("workout_template_id")
    val workoutTemplateId: String,

    @SerialName("exercise_id")
    val exerciseId: String,

    @SerialName("order_index")
    val orderIndex: Int
)

@Serializable
data class CreateExerciseSetRequest(
    @SerialName("workout_exercise_id")
    val workoutExerciseId: String,

    @SerialName("set_number")
    val setNumber: Int,

    @SerialName("target_reps")
    val targetReps: Int,

    @SerialName("target_weight")
    val targetWeight: Double,

    @SerialName("rest_seconds")
    val restSeconds: Int
)