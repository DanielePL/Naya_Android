package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Program Template from database (public templates for users to start)
 */
@Serializable
data class ProgramTemplate(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("sport_type_id") val sportTypeId: String? = null,
    val difficulty: String? = null,
    @SerialName("duration_weeks") val durationWeeks: Int,
    @SerialName("days_per_week") val daysPerWeek: Int? = null,
    @SerialName("periodization_type") val periodizationType: String? = null,
    @SerialName("goal_tags") val goalTags: List<String>? = null,
    @SerialName("equipment_required") val equipmentRequired: List<String>? = null,
    @SerialName("is_public") val isPublic: Boolean = true
) {
    // Display helpers
    val displayDifficulty: String
        get() = difficulty?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All Levels"

    val displaySport: String
        get() = sportTypeId?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "General"

    val displayGoals: String
        get() = goalTags?.joinToString(", ") {
            it.lowercase().replace("_", " ").replaceFirstChar { c -> c.uppercase() }
        } ?: ""
}

/**
 * Program Template Phase (mesocycle/block within a program template)
 */
@Serializable
data class ProgramTemplatePhase(
    val id: String,
    @SerialName("program_template_id") val programTemplateId: String,
    val name: String,
    @SerialName("training_phase_id") val trainingPhaseId: String? = null,
    @SerialName("sport_specific_phase_id") val sportSpecificPhaseId: String? = null,
    @SerialName("block_type_id") val blockTypeId: String? = null,
    @SerialName("start_week") val startWeek: Int,
    @SerialName("duration_weeks") val durationWeeks: Int,
    @SerialName("volume_modifier") val volumeModifier: Double? = null,
    @SerialName("intensity_modifier") val intensityModifier: Double? = null,
    val description: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
) {
    // Week range display (e.g., "Week 1-4")
    val weekRangeDisplay: String
        get() = if (durationWeeks == 1) {
            "Week $startWeek"
        } else {
            "Week $startWeek-${startWeek + durationWeeks - 1}"
        }

    // Volume/Intensity display
    val volumeDisplay: String
        get() = volumeModifier?.let {
            when {
                it >= 1.1 -> "High Volume"
                it <= 0.7 -> "Low Volume"
                else -> "Moderate Volume"
            }
        } ?: "Standard Volume"

    val intensityDisplay: String
        get() = intensityModifier?.let {
            when {
                it >= 0.9 -> "High Intensity"
                it <= 0.7 -> "Low Intensity"
                else -> "Moderate Intensity"
            }
        } ?: "Standard Intensity"
}

@Serializable
data class Program(
    val id: String,
    val name: String,
    val description: String = "",
    val durationInWeeks: Int = 4,
    val workoutsPerWeek: Int = 4,
    val difficulty: String = "Intermediate",
    val weeks: List<ProgramWeek> = emptyList()
)

@Serializable
data class ProgramWeek(
    val weekNumber: Int,
    val workouts: List<LegacyProgramWorkout>
)

/**
 * Legacy program workout format (simple display only)
 * For database-backed workouts, use ProgramWorkout from TrainingProgram.kt
 */
@Serializable
data class LegacyProgramWorkout(
    val id: String,
    val name: String,
    val day: String,
    val duration: String
)

/**
 * Program Template Week - defines a week within a program template
 */
@Serializable
data class ProgramTemplateWeek(
    val id: String,
    @SerialName("program_template_id") val programTemplateId: String,
    @SerialName("week_number") val weekNumber: Int,
    @SerialName("phase_id") val phaseId: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("is_deload") val isDeload: Boolean = false,
    @SerialName("volume_modifier") val volumeModifier: Double? = null,
    @SerialName("intensity_modifier") val intensityModifier: Double? = null
)

/**
 * Program Template Day - a training day within a week, links to workout template
 */
@Serializable
data class ProgramTemplateDay(
    val id: String,
    @SerialName("program_template_week_id") val programTemplateWeekId: String,
    @SerialName("workout_template_id") val workoutTemplateId: String? = null,
    @SerialName("day_of_week") val dayOfWeek: Int? = null, // 1=Monday, 7=Sunday
    @SerialName("day_name") val dayName: String? = null,
    val notes: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
) {
    val displayName: String
        get() = dayName ?: "Day ${sortOrder ?: dayOfWeek ?: 1}"
}

/**
 * User Program Progress - tracks workout completion within a user's program
 */
@Serializable
data class UserProgramProgress(
    val id: String,
    @SerialName("user_program_id") val userProgramId: String,
    @SerialName("program_template_day_id") val programTemplateDayId: String,
    @SerialName("week_number") val weekNumber: Int,
    @SerialName("completed_at") val completedAt: String? = null,
    val skipped: Boolean = false,
    @SerialName("workout_session_id") val workoutSessionId: String? = null,
    val notes: String? = null
)

/**
 * User's own program (saved to user_programs table)
 * Created when user starts a program from a template
 */
@Serializable
data class UserProgram(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("program_template_id") val programTemplateId: String? = null,
    val name: String,
    val description: String? = null,
    @SerialName("sport_type_id") val sportTypeId: String? = null,
    val difficulty: String? = null,
    @SerialName("duration_weeks") val durationWeeks: Int,
    @SerialName("days_per_week") val daysPerWeek: Int? = null,
    @SerialName("periodization_type") val periodizationType: String? = null,
    @SerialName("current_week") val currentWeek: Int = 1,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val status: String = "active" // active, completed, paused
) {
    companion object {
        fun fromTemplate(template: ProgramTemplate, userId: String, newId: String): UserProgram {
            return UserProgram(
                id = newId,
                userId = userId,
                programTemplateId = template.id,
                name = template.name,
                description = template.description,
                sportTypeId = template.sportTypeId,
                difficulty = template.difficulty,
                durationWeeks = template.durationWeeks,
                daysPerWeek = template.daysPerWeek,
                periodizationType = template.periodizationType,
                currentWeek = 1,
                status = "active"
            )
        }
    }
}