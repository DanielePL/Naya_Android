package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workout History - Summary of completed workouts
 */
@Serializable
data class WorkoutHistory(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String,

    // Program Context (optional)
    @SerialName("program_id") val programId: String? = null,
    @SerialName("program_week") val programWeek: Int? = null,
    @SerialName("program_day") val programDay: Int? = null,

    // Workout Reference
    @SerialName("workout_template_id") val workoutTemplateId: String? = null,
    @SerialName("workout_name") val workoutName: String,

    // Summary Stats
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("total_sets") val totalSets: Int = 0,
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("total_exercises") val totalExercises: Int = 0,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,

    // Performance Metrics
    @SerialName("avg_rpe") val avgRpe: Double? = null,
    @SerialName("fatigue_index") val fatigueIndex: Double? = null,
    @SerialName("performance_score") val performanceScore: Double? = null,

    // Records
    @SerialName("prs_achieved") val prsAchieved: Int = 0,
    @SerialName("pr_exercises") val prExercises: List<String>? = null,

    // VBT Summary
    @SerialName("avg_velocity") val avgVelocity: Double? = null,
    @SerialName("total_reps_tracked") val totalRepsTracked: Int = 0,

    // User Input
    val notes: String? = null,
    @SerialName("mood_rating") val moodRating: Int? = null,
    @SerialName("energy_rating") val energyRating: Int? = null,

    // Timestamps
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String,

    // Calendar helpers (generated columns in DB, but useful)
    @SerialName("completed_date") val completedDate: String? = null,
    @SerialName("completed_week") val completedWeek: Int? = null,
    @SerialName("completed_month") val completedMonth: Int? = null,
    @SerialName("completed_year") val completedYear: Int? = null,
    @SerialName("day_of_week") val dayOfWeek: Int? = null,

    @SerialName("created_at") val createdAt: String? = null
) {
    /**
     * Get formatted duration display
     */
    fun getDurationDisplay(): String {
        val minutes = durationMinutes ?: return "N/A"
        return if (minutes >= 60) {
            "${minutes / 60}h ${minutes % 60}min"
        } else {
            "${minutes}min"
        }
    }

    /**
     * Get formatted volume display
     */
    fun getVolumeDisplay(): String {
        return if (totalVolumeKg >= 1000) {
            String.format("%.1fk kg", totalVolumeKg / 1000)
        } else {
            "${totalVolumeKg.toInt()} kg"
        }
    }

    /**
     * Check if this workout has PRs
     */
    val hasPRs: Boolean
        get() = prsAchieved > 0

    /**
     * Get day name from dayOfWeek
     */
    fun getDayName(): String {
        return when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Unknown"
        }
    }
}

/**
 * User Training Summary - Overall statistics
 */
@Serializable
data class UserTrainingSummary(
    @SerialName("user_id") val userId: String,

    // Lifetime Stats
    @SerialName("total_workouts") val totalWorkouts: Int = 0,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("total_sets") val totalSets: Int = 0,
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("total_training_minutes") val totalTrainingMinutes: Int = 0,
    @SerialName("total_prs") val totalPRs: Int = 0,

    // Current Streak
    @SerialName("current_streak_days") val currentStreakDays: Int = 0,
    @SerialName("longest_streak_days") val longestStreakDays: Int = 0,
    @SerialName("last_workout_date") val lastWorkoutDate: String? = null,

    // This Week
    @SerialName("week_workouts") val weekWorkouts: Int = 0,
    @SerialName("week_volume_kg") val weekVolumeKg: Double = 0.0,
    @SerialName("week_started_at") val weekStartedAt: String? = null,

    // This Month
    @SerialName("month_workouts") val monthWorkouts: Int = 0,
    @SerialName("month_volume_kg") val monthVolumeKg: Double = 0.0,
    @SerialName("month_started_at") val monthStartedAt: String? = null,

    // This Year
    @SerialName("year_workouts") val yearWorkouts: Int = 0,
    @SerialName("year_volume_kg") val yearVolumeKg: Double = 0.0,
    @SerialName("year_prs") val yearPRs: Int = 0,

    // Averages
    @SerialName("avg_workout_duration_minutes") val avgWorkoutDurationMinutes: Double? = null,
    @SerialName("avg_workouts_per_week") val avgWorkoutsPerWeek: Double? = null,
    @SerialName("avg_volume_per_workout") val avgVolumePerWorkout: Double? = null,

    // Favorites
    @SerialName("favorite_exercises") val favoriteExercises: List<String>? = null,
    @SerialName("favorite_workout_template_id") val favoriteWorkoutTemplateId: String? = null,

    // Active Program
    @SerialName("active_program_id") val activeProgramId: String? = null,

    // Timestamps
    @SerialName("first_workout_at") val firstWorkoutAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    /**
     * Get total training time formatted
     */
    fun getTotalTrainingTimeDisplay(): String {
        val hours = totalTrainingMinutes / 60
        val minutes = totalTrainingMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    /**
     * Get total volume formatted
     */
    fun getTotalVolumeDisplay(): String {
        return when {
            totalVolumeKg >= 1_000_000 -> String.format("%.1fM kg", totalVolumeKg / 1_000_000)
            totalVolumeKg >= 1_000 -> String.format("%.1fk kg", totalVolumeKg / 1_000)
            else -> "${totalVolumeKg.toInt()} kg"
        }
    }
}

/**
 * Weekly volume data for charts
 */
data class WeeklyVolumeData(
    val year: Int,
    val week: Int,
    val workouts: Int,
    val volumeKg: Double,
    val sets: Int,
    val reps: Int,
    val avgDuration: Double?,
    val prs: Int
)

/**
 * Calendar day data for history view
 */
data class CalendarDayData(
    val date: String,
    val dayOfMonth: Int,
    val hasWorkout: Boolean,
    val workoutHistory: WorkoutHistory? = null,
    val isToday: Boolean = false,
    val isInCurrentMonth: Boolean = true
)

/**
 * Month calendar data
 */
data class MonthCalendarData(
    val year: Int,
    val month: Int,
    val monthName: String,
    val days: List<CalendarDayData>,
    val totalWorkouts: Int,
    val totalVolumeKg: Double
)
