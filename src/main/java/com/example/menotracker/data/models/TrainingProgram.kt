package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Training Program - Multi-week training cycles
 */
@Serializable
data class TrainingProgram(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String? = null,

    // Program Structure
    @SerialName("duration_weeks") val durationWeeks: Int = 4,
    @SerialName("workouts_per_week") val workoutsPerWeek: Int = 3,

    // Program Type & Goal
    val goal: ProgramGoal? = null,
    @SerialName("program_type") val programType: ProgramType? = null,
    @SerialName("difficulty_level") val difficultyLevel: DifficultyLevel? = null,

    // Sports Association
    val sports: List<String>? = null,

    // Status & Progress
    @SerialName("is_active") val isActive: Boolean = false,
    val status: ProgramStatus = ProgramStatus.NOT_STARTED,
    @SerialName("current_week") val currentWeek: Int = 1,
    @SerialName("current_day") val currentDay: Int = 1,

    // Dates
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("paused_at") val pausedAt: String? = null,

    // Stats (denormalized)
    @SerialName("total_sessions_planned") val totalSessionsPlanned: Int = 0,
    @SerialName("total_sessions_completed") val totalSessionsCompleted: Int = 0,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,

    // Metadata
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,

    // ILB (Individuelles Leistungsbild) Settings
    @SerialName("ilb_mode") val ilbMode: ILBMode = ILBMode.OFF,
    @SerialName("ilb_interval_weeks") val ilbIntervalWeeks: Int = 6,  // Test alle X Wochen
    @SerialName("last_ilb_test_week") val lastILBTestWeek: Int? = null  // Letzte Test-Woche
) {
    // ILB: Nächste Test-Woche berechnen
    val nextILBTestWeek: Int?
        get() = when (ilbMode) {
            ILBMode.OFF -> null
            ILBMode.MANUAL -> null  // User entscheidet selbst
            ILBMode.AUTO -> {
                val lastTest = lastILBTestWeek ?: 0
                val nextTest = lastTest + ilbIntervalWeeks
                if (nextTest <= durationWeeks) nextTest else null
            }
        }

    // ILB: Ist die aktuelle Woche eine Test-Woche?
    val isCurrentWeekILBTest: Boolean
        get() = ilbMode == ILBMode.AUTO && currentWeek == nextILBTestWeek
    val progressPercentage: Float
        get() = if (totalSessionsPlanned > 0) {
            (totalSessionsCompleted.toFloat() / totalSessionsPlanned.toFloat()) * 100f
        } else 0f

    val weekProgressPercentage: Float
        get() = if (durationWeeks > 0) {
            ((currentWeek - 1).toFloat() / durationWeeks.toFloat()) * 100f
        } else 0f

    val isCompleted: Boolean
        get() = status == ProgramStatus.COMPLETED

    val isInProgress: Boolean
        get() = status == ProgramStatus.IN_PROGRESS
}

@Serializable
enum class ProgramGoal {
    @SerialName("strength") STRENGTH,
    @SerialName("hypertrophy") HYPERTROPHY,
    @SerialName("endurance") ENDURANCE,
    @SerialName("power") POWER,
    @SerialName("general") GENERAL,
    @SerialName("sport_specific") SPORT_SPECIFIC
}

@Serializable
enum class ProgramType {
    @SerialName("linear") LINEAR,
    @SerialName("undulating") UNDULATING,
    @SerialName("block") BLOCK,
    @SerialName("custom") CUSTOM
}

@Serializable
enum class DifficultyLevel {
    @SerialName("beginner") BEGINNER,
    @SerialName("intermediate") INTERMEDIATE,
    @SerialName("advanced") ADVANCED,
    @SerialName("elite") ELITE
}

@Serializable
enum class ProgramStatus {
    @SerialName("not_started") NOT_STARTED,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
    @SerialName("paused") PAUSED,
    @SerialName("abandoned") ABANDONED
}

@Serializable
enum class ILBMode {
    @SerialName("off") OFF,
    @SerialName("manual") MANUAL,
    @SerialName("auto") AUTO
}

/**
 * Program Workout - Links workout templates to programs
 */
@Serializable
data class ProgramWorkout(
    val id: String,
    @SerialName("program_id") val programId: String,
    @SerialName("workout_template_id") val workoutTemplateId: String,

    // Scheduling
    @SerialName("week_number") val weekNumber: Int,
    @SerialName("day_number") val dayNumber: Int,
    @SerialName("day_of_week") val dayOfWeek: Int? = null,

    // Ordering
    @SerialName("order_in_program") val orderInProgram: Int,

    // Progression
    @SerialName("progression_notes") val progressionNotes: String? = null,
    @SerialName("intensity_modifier") val intensityModifier: Double = 1.0,

    // Completion
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("session_id") val sessionId: String? = null,

    // Metadata
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Program with all its workouts loaded
 */
data class ProgramWithWorkouts(
    val program: TrainingProgram,
    val workouts: List<ProgramWorkoutWithTemplate>,
    val currentWorkout: ProgramWorkoutWithTemplate? = null
)

/**
 * Program workout with the template details
 */
data class ProgramWorkoutWithTemplate(
    val programWorkout: ProgramWorkout,
    val workoutTemplate: WorkoutTemplateDto? = null,
    val workoutName: String = workoutTemplate?.name ?: "Unknown Workout"
)

/**
 * Summary of a training program for display
 */
data class ProgramSummary(
    val program: TrainingProgram,
    val nextWorkout: ProgramWorkoutWithTemplate? = null,
    val completedThisWeek: Int = 0,
    val remainingThisWeek: Int = 0,
    val recentPRs: Int = 0
)
