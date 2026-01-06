package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

// ═══════════════════════════════════════════════════════════════
// WORKOUT PATTERN LEARNING MODELS
// Smart detection of user's workout habits for nutrition timing
// ═══════════════════════════════════════════════════════════════

/**
 * Represents a single recorded workout session for pattern learning
 */
@Serializable
data class WorkoutRecord(
    @SerialName("id") val id: String,
    @SerialName("start_time_millis") val startTimeMillis: Long,
    @SerialName("end_time_millis") val endTimeMillis: Long? = null,
    @SerialName("day_of_week") val dayOfWeek: Int,           // 1 = Monday, 7 = Sunday
    @SerialName("hour_of_day") val hourOfDay: Int,           // 0-23
    @SerialName("minute_of_hour") val minuteOfHour: Int,     // 0-59
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("workout_type") val workoutType: String? = null,
    @SerialName("was_fasted") val wasFasted: Boolean = false,
    @SerialName("pre_workout_meal_time_millis") val preWorkoutMealTimeMillis: Long? = null
) {
    val startTime: Instant get() = Instant.ofEpochMilli(startTimeMillis)
    val endTime: Instant? get() = endTimeMillis?.let { Instant.ofEpochMilli(it) }

    fun getHourAsFloat(): Float = hourOfDay + (minuteOfHour / 60f)
}

/**
 * Detected pattern for a specific day of the week
 */
@Serializable
data class DayPattern(
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("occurrence_count") val occurrenceCount: Int,
    @SerialName("average_hour") val averageHour: Float,      // e.g., 18.5 = 18:30
    @SerialName("std_deviation") val stdDeviation: Float,    // How consistent
    @SerialName("confidence") val confidence: Float          // 0.0 - 1.0
) {
    fun getAverageTimeFormatted(): String {
        val hour = averageHour.toInt()
        val minute = ((averageHour - hour) * 60).toInt()
        return String.format("%02d:%02d", hour, minute)
    }

    fun getTimeRange(): Pair<Float, Float> {
        val margin = (stdDeviation * 1.5f).coerceAtLeast(0.5f)
        return (averageHour - margin).coerceAtLeast(0f) to (averageHour + margin).coerceAtMost(23.99f)
    }
}

/**
 * Complete workout pattern for a user
 */
@Serializable
data class WorkoutPattern(
    @SerialName("user_id") val userId: String,
    @SerialName("day_patterns") val dayPatterns: List<DayPattern>,
    @SerialName("total_workouts_tracked") val totalWorkoutsTracked: Int,
    @SerialName("overall_confidence") val overallConfidence: Float,
    @SerialName("most_common_type") val mostCommonType: String? = null,
    @SerialName("average_duration_minutes") val averageDurationMinutes: Int? = null,
    @SerialName("last_updated_millis") val lastUpdatedMillis: Long
) {
    val lastUpdated: Instant get() = Instant.ofEpochMilli(lastUpdatedMillis)

    /**
     * Get the pattern for today, if any
     */
    fun getPatternForDay(dayOfWeek: Int): DayPattern? {
        return dayPatterns.find { it.dayOfWeek == dayOfWeek && it.confidence >= MIN_CONFIDENCE_THRESHOLD }
    }

    /**
     * Check if user likely works out today
     */
    fun isWorkoutLikelyToday(dayOfWeek: Int): Boolean {
        val pattern = getPatternForDay(dayOfWeek)
        return pattern != null && pattern.confidence >= MIN_CONFIDENCE_THRESHOLD
    }

    /**
     * Get predicted workout time for today (as hour float, e.g., 18.5)
     */
    fun getPredictedTimeToday(dayOfWeek: Int): Float? {
        return getPatternForDay(dayOfWeek)?.averageHour
    }

    companion object {
        const val MIN_CONFIDENCE_THRESHOLD = 0.5f
        const val MIN_WORKOUTS_FOR_PATTERN = 3
    }
}

// ═══════════════════════════════════════════════════════════════
// PRE-WORKOUT NUTRITION STATE
// ═══════════════════════════════════════════════════════════════

/**
 * Current state for pre-workout nutrition alerts
 */
data class PreWorkoutNutritionState(
    val predictedWorkoutTime: Instant?,
    val hoursUntilWorkout: Float?,
    val lastMealTime: Instant?,
    val hoursSinceLastMeal: Float?,
    val recommendedAction: PreWorkoutAction,
    val proteinRecommendation: IntRange,
    val carbRecommendation: IntRange,
    val fatRecommendation: IntRange,
    val confidence: Float,
    val message: String,
    val urgency: AlertUrgency
)

/**
 * Recommended pre-workout action based on timing
 */
enum class PreWorkoutAction {
    FULL_MEAL_NOW,        // 3-4h before workout - can eat complete meal
    LIGHT_MEAL_NOW,       // 1.5-2.5h before workout - lighter meal, less fat
    QUICK_CARBS_ONLY,     // 30min-1h before workout - fast carbs only
    TOO_LATE_TO_EAT,      // <30min - don't eat heavy, maybe small snack
    WELL_FUELED,          // Already ate recently, good to go
    NO_WORKOUT_PREDICTED, // No workout expected today
    WORKOUT_IN_PROGRESS,  // Currently working out
    POST_WORKOUT_WINDOW   // Post-workout nutrition phase
}

/**
 * Alert urgency level for UI styling
 */
enum class AlertUrgency {
    NONE,       // No action needed
    LOW,        // Informational
    MEDIUM,     // Should act soon
    HIGH,       // Act now
    CRITICAL    // Missed optimal window
}

// ═══════════════════════════════════════════════════════════════
// POST-WORKOUT NUTRITION STATE (Enhanced)
// ═══════════════════════════════════════════════════════════════

/**
 * Enhanced post-workout state combining with pre-workout data
 */
data class PostWorkoutNutritionState(
    val workoutEndTime: Instant,
    val minutesSinceWorkout: Long,
    val wasFasted: Boolean,
    val proteinConsumedSince: Float,
    val carbsConsumedSince: Float,
    val proteinTarget: Float,
    val carbsTarget: Float,
    val windowPhase: PostWorkoutPhase,
    val urgency: AlertUrgency,
    val message: String
)

/**
 * Post-workout nutrition phases
 */
enum class PostWorkoutPhase {
    IMMEDIATE,      // 0-30min - Critical if fasted
    OPTIMAL,        // 30min-2h - Best for MPS
    EXTENDED,       // 2-6h - Still beneficial
    CLOSED          // >6h - Window closed
}

// ═══════════════════════════════════════════════════════════════
// NUTRITION TIMING RECOMMENDATIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Science-based nutrition timing recommendations
 */
object NutritionTimingRecommendations {

    // Pre-workout timing windows (hours before workout)
    const val FULL_MEAL_WINDOW_START = 4.0f
    const val FULL_MEAL_WINDOW_END = 2.5f
    const val LIGHT_MEAL_WINDOW_END = 1.5f
    const val QUICK_CARBS_WINDOW_END = 0.5f

    // Post-workout windows (minutes after workout)
    const val IMMEDIATE_WINDOW_MINUTES = 30
    const val OPTIMAL_WINDOW_MINUTES = 120
    const val EXTENDED_WINDOW_MINUTES = 360

    /**
     * Get pre-workout macros based on timing
     */
    fun getPreWorkoutMacros(hoursUntilWorkout: Float, bodyWeightKg: Float): PreWorkoutMacros {
        return when {
            hoursUntilWorkout >= FULL_MEAL_WINDOW_END -> PreWorkoutMacros(
                protein = (bodyWeightKg * 0.3f).toInt()..(bodyWeightKg * 0.5f).toInt(),
                carbs = (bodyWeightKg * 0.5f).toInt()..(bodyWeightKg * 0.8f).toInt(),
                fat = 10..20,
                description = "Full pre-workout meal"
            )
            hoursUntilWorkout >= LIGHT_MEAL_WINDOW_END -> PreWorkoutMacros(
                protein = (bodyWeightKg * 0.2f).toInt()..(bodyWeightKg * 0.3f).toInt(),
                carbs = (bodyWeightKg * 0.4f).toInt()..(bodyWeightKg * 0.6f).toInt(),
                fat = 5..10,
                description = "Light pre-workout meal"
            )
            hoursUntilWorkout >= QUICK_CARBS_WINDOW_END -> PreWorkoutMacros(
                protein = 0..10,
                carbs = 20..40,
                fat = 0..5,
                description = "Quick carbs only"
            )
            else -> PreWorkoutMacros(
                protein = 0..0,
                carbs = 10..20,
                fat = 0..0,
                description = "Small snack if needed"
            )
        }
    }

    /**
     * Get post-workout macros based on timing and workout type
     */
    fun getPostWorkoutMacros(
        minutesSinceWorkout: Long,
        bodyWeightKg: Float,
        wasFasted: Boolean
    ): PostWorkoutMacros {
        val urgencyMultiplier = if (wasFasted) 1.3f else 1.0f

        return when {
            minutesSinceWorkout <= IMMEDIATE_WINDOW_MINUTES -> PostWorkoutMacros(
                protein = ((bodyWeightKg * 0.3f) * urgencyMultiplier).toInt()..
                         ((bodyWeightKg * 0.5f) * urgencyMultiplier).toInt(),
                carbs = ((bodyWeightKg * 0.5f) * urgencyMultiplier).toInt()..
                       ((bodyWeightKg * 0.8f) * urgencyMultiplier).toInt(),
                priority = if (wasFasted) "CRITICAL" else "HIGH",
                description = if (wasFasted)
                    "Fasted workout - eat immediately!"
                    else "Optimal window - eat soon"
            )
            minutesSinceWorkout <= OPTIMAL_WINDOW_MINUTES -> PostWorkoutMacros(
                protein = (bodyWeightKg * 0.3f).toInt()..(bodyWeightKg * 0.4f).toInt(),
                carbs = (bodyWeightKg * 0.4f).toInt()..(bodyWeightKg * 0.6f).toInt(),
                priority = "MEDIUM",
                description = "Still in optimal window"
            )
            minutesSinceWorkout <= EXTENDED_WINDOW_MINUTES -> PostWorkoutMacros(
                protein = (bodyWeightKg * 0.25f).toInt()..(bodyWeightKg * 0.35f).toInt(),
                carbs = (bodyWeightKg * 0.3f).toInt()..(bodyWeightKg * 0.5f).toInt(),
                priority = "LOW",
                description = "Extended window - still beneficial"
            )
            else -> PostWorkoutMacros(
                protein = (bodyWeightKg * 0.2f).toInt()..(bodyWeightKg * 0.3f).toInt(),
                carbs = (bodyWeightKg * 0.3f).toInt()..(bodyWeightKg * 0.4f).toInt(),
                priority = "NONE",
                description = "Window closed - normal meal timing"
            )
        }
    }
}

data class PreWorkoutMacros(
    val protein: IntRange,
    val carbs: IntRange,
    val fat: IntRange,
    val description: String
)

data class PostWorkoutMacros(
    val protein: IntRange,
    val carbs: IntRange,
    val priority: String,
    val description: String
)