package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Exercise Statistics - Aggregated stats per exercise per user
 */
@Serializable
data class ExerciseStatistics(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String? = null,

    // Personal Records (Weight)
    @SerialName("pr_weight_kg") val prWeightKg: Double? = null,
    @SerialName("pr_weight_reps") val prWeightReps: Int? = null,
    @SerialName("pr_weight_date") val prWeightDate: String? = null,
    @SerialName("pr_weight_session_id") val prWeightSessionId: String? = null,

    // Personal Records (Reps)
    @SerialName("pr_reps") val prReps: Int? = null,
    @SerialName("pr_reps_weight_kg") val prRepsWeightKg: Double? = null,
    @SerialName("pr_reps_date") val prRepsDate: String? = null,
    @SerialName("pr_reps_session_id") val prRepsSessionId: String? = null,

    // Personal Records (Volume)
    @SerialName("pr_volume_kg") val prVolumeKg: Double? = null,
    @SerialName("pr_volume_date") val prVolumeDate: String? = null,
    @SerialName("pr_volume_session_id") val prVolumeSessionId: String? = null,

    // Personal Records (VBT)
    @SerialName("pr_velocity") val prVelocity: Double? = null,
    @SerialName("pr_velocity_date") val prVelocityDate: String? = null,
    @SerialName("pr_velocity_session_id") val prVelocitySessionId: String? = null,

    // Estimated 1RM
    @SerialName("estimated_1rm_kg") val estimated1rmKg: Double? = null,
    @SerialName("estimated_1rm_formula") val estimated1rmFormula: String? = "epley",
    @SerialName("estimated_1rm_date") val estimated1rmDate: String? = null,

    // Lifetime Aggregates
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("total_sets") val totalSets: Int = 0,
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("total_sessions") val totalSessions: Int = 0,

    // Averages
    @SerialName("avg_weight_kg") val avgWeightKg: Double? = null,
    @SerialName("avg_reps_per_set") val avgRepsPerSet: Double? = null,
    @SerialName("avg_sets_per_session") val avgSetsPerSession: Double? = null,
    @SerialName("avg_volume_per_session") val avgVolumePerSession: Double? = null,

    // Recent Performance (last 4 weeks)
    @SerialName("recent_avg_weight_kg") val recentAvgWeightKg: Double? = null,
    @SerialName("recent_avg_reps") val recentAvgReps: Double? = null,
    @SerialName("recent_total_volume_kg") val recentTotalVolumeKg: Double? = null,
    @SerialName("recent_sessions") val recentSessions: Int = 0,

    // Trend Analysis
    @SerialName("trend_direction") val trendDirection: TrendDirection? = null,
    @SerialName("trend_percentage") val trendPercentage: Double? = null,

    // Timestamps
    @SerialName("first_performed_at") val firstPerformedAt: String? = null,
    @SerialName("last_performed_at") val lastPerformedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    /**
     * Get formatted PR display string
     */
    fun getPrDisplay(): String {
        return if (prWeightKg != null && prWeightReps != null) {
            "${prWeightKg.toInt()}kg x $prWeightReps"
        } else {
            "No PR yet"
        }
    }

    /**
     * Check if this is a "new" PR compared to previous
     */
    fun isRecentPR(withinDays: Int = 7): Boolean {
        // TODO: Implement date comparison
        return prWeightDate != null
    }

    /**
     * Get display name - uses exerciseName if available, otherwise formats exerciseId
     */
    fun getDisplayName(): String {
        return exerciseName ?: exerciseId.replace("_", " ").replace("-", " ")
    }
}

@Serializable
enum class TrendDirection {
    @SerialName("improving") IMPROVING,
    @SerialName("stable") STABLE,
    @SerialName("declining") DECLINING
}

/**
 * Exercise statistics with exercise details for display
 */
data class ExerciseStatisticsWithDetails(
    val statistics: ExerciseStatistics,
    val exercise: Exercise? = null,
    val exerciseName: String = exercise?.name ?: "Unknown Exercise",
    val mainMuscle: String = exercise?.mainMuscle ?: ""
)

/**
 * PR (Personal Record) entry
 */
@Serializable
data class PRHistory(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String? = null, // For display - falls back to exerciseId if null
    @SerialName("session_id") val sessionId: String? = null,

    @SerialName("pr_type") val prType: PRType,

    @SerialName("weight_kg") val weightKg: Double? = null,
    val reps: Int? = null,
    @SerialName("volume_kg") val volumeKg: Double? = null,
    val velocity: Double? = null,

    @SerialName("previous_pr_value") val previousPrValue: Double? = null,
    @SerialName("improvement_percentage") val improvementPercentage: Double? = null,

    @SerialName("achieved_at") val achievedAt: String,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    /** Get display name - uses exerciseName if available, otherwise formats exerciseId */
    fun getDisplayName(): String {
        return exerciseName ?: exerciseId.replace("_", " ").replace("-", " ")
    }
}

@Serializable
enum class PRType {
    @SerialName("weight") WEIGHT,
    @SerialName("reps") REPS,
    @SerialName("volume") VOLUME,
    @SerialName("velocity") VELOCITY,
    @SerialName("1rm_estimated") ESTIMATED_1RM
}

/**
 * PR with exercise details for display
 */
data class PRWithDetails(
    val pr: PRHistory,
    val exerciseName: String,
    val displayValue: String,
    val displayUnit: String
) {
    companion object {
        fun fromPRHistory(pr: PRHistory, exerciseName: String): PRWithDetails {
            val (value, unit) = when (pr.prType) {
                PRType.WEIGHT -> Pair("${pr.weightKg?.toInt() ?: 0}kg x ${pr.reps ?: 0}", "")
                PRType.REPS -> Pair("${pr.reps ?: 0} reps", "@ ${pr.weightKg?.toInt() ?: 0}kg")
                PRType.VOLUME -> Pair("${pr.volumeKg?.toInt() ?: 0}", "kg volume")
                PRType.VELOCITY -> Pair("${pr.velocity ?: 0}", "m/s")
                PRType.ESTIMATED_1RM -> Pair("${pr.weightKg?.toInt() ?: 0}", "kg e1RM")
            }
            return PRWithDetails(pr, exerciseName, value, unit)
        }
    }
}
