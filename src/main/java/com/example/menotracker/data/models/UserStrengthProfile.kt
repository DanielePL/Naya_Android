package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * User Strength Profile - The "red thread" through the app
 *
 * This is the core data model that connects:
 * - Onboarding (where PRs are collected)
 * - Account screen (displays current/goal PRs)
 * - Workout system (calculates percentages from PRs)
 * - Programs (adapts templates to user's strength level)
 * - Progress tracking (monitors journey from current to goal)
 */
@Serializable
data class UserStrengthProfile(
    val id: String,
    @SerialName("user_id") val userId: String,

    // Identity
    val gender: StrengthGender,
    @SerialName("bodyweight_kg") val bodyweightKg: Float,
    @SerialName("experience_level") val experienceLevel: StrengthExperienceLevel,

    // Current PRs (IST-Zustand)
    @SerialName("current_squat_kg") val currentSquatKg: Float,
    @SerialName("current_bench_kg") val currentBenchKg: Float,
    @SerialName("current_deadlift_kg") val currentDeadliftKg: Float,
    @SerialName("current_overhead_kg") val currentOverheadKg: Float? = null,

    // Goal PRs (SOLL-Zustand)
    @SerialName("goal_squat_kg") val goalSquatKg: Float,
    @SerialName("goal_bench_kg") val goalBenchKg: Float,
    @SerialName("goal_deadlift_kg") val goalDeadliftKg: Float,
    @SerialName("goal_overhead_kg") val goalOverheadKg: Float? = null,

    // Timeline
    @SerialName("target_date") val targetDate: String? = null, // ISO date
    @SerialName("estimated_weeks") val estimatedWeeks: Int? = null,

    // Commitment
    @SerialName("sessions_per_week") val sessionsPerWeek: Int = 4,
    @SerialName("effort_level") val effortLevel: Int = 7, // 1-10

    // Tracking
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    // Calculated totals
    val currentTotal: Float
        get() = currentSquatKg + currentBenchKg + currentDeadliftKg

    val goalTotal: Float
        get() = goalSquatKg + goalBenchKg + goalDeadliftKg

    val totalToGain: Float
        get() = (goalTotal - currentTotal).coerceAtLeast(0f)

    val progressPercentage: Float
        get() = if (totalToGain > 0) {
            ((currentTotal - (goalTotal - totalToGain)) / totalToGain * 100).coerceIn(0f, 100f)
        } else 100f

    // Wilks coefficient for comparing across weight classes
    val wilksCoefficient: Float
        get() = calculateWilks(currentTotal, bodyweightKg, gender)

    /**
     * Calculate working weight for a given percentage and lift
     */
    fun calculateWorkingWeight(
        lift: LiftType,
        percentage: Float,
        roundTo: Float = 2.5f
    ): Float {
        val pr = when (lift) {
            LiftType.SQUAT -> currentSquatKg
            LiftType.BENCH -> currentBenchKg
            LiftType.DEADLIFT -> currentDeadliftKg
            LiftType.OVERHEAD -> currentOverheadKg ?: (currentBenchKg * 0.65f)
        }
        val raw = pr * percentage
        return (raw / roundTo).roundToInt() * roundTo
    }

    /**
     * Get expected velocity for a given percentage (for VBT integration)
     */
    fun getExpectedVelocity(percentage: Float): Float {
        return when {
            percentage <= 0.50f -> 1.30f
            percentage <= 0.60f -> 1.15f
            percentage <= 0.70f -> 0.95f
            percentage <= 0.75f -> 0.82f
            percentage <= 0.80f -> 0.70f
            percentage <= 0.85f -> 0.58f
            percentage <= 0.90f -> 0.47f
            percentage <= 0.95f -> 0.35f
            else -> 0.20f
        }
    }

    companion object {
        /**
         * Calculate Wilks coefficient for strength comparison across weight classes
         */
        fun calculateWilks(total: Float, bodyweight: Float, gender: StrengthGender): Float {
            val coefficients = if (gender == StrengthGender.MALE) {
                // Male Wilks coefficients
                listOf(-216.0475144, 16.2606339, -0.002388645, -0.00113732, 0.00000701863, -0.00000001291)
            } else {
                // Female Wilks coefficients
                listOf(594.31747775582, -27.23842536447, 0.82112226871, -0.00930733913, 0.00004731582, -0.00000009054)
            }

            val bw = bodyweight.toDouble()
            val denominator = coefficients[0] +
                    coefficients[1] * bw +
                    coefficients[2] * bw * bw +
                    coefficients[3] * bw * bw * bw +
                    coefficients[4] * bw * bw * bw * bw +
                    coefficients[5] * bw * bw * bw * bw * bw

            return (500 * total / denominator).toFloat()
        }

        /**
         * Create from onboarding data
         */
        fun fromOnboarding(
            userId: String,
            gender: com.example.menotracker.onboarding.data.Gender,
            currentPRs: com.example.menotracker.onboarding.data.CurrentPRs,
            goalPRs: com.example.menotracker.onboarding.data.GoalPRs,
            commitment: com.example.menotracker.onboarding.data.TrainingCommitment,
            experienceLevel: com.example.menotracker.onboarding.data.ExperienceLevel?,
            estimatedWeeks: Int? = null,
            targetDate: String? = null
        ): UserStrengthProfile {
            return UserStrengthProfile(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                gender = when (gender) {
                    com.example.menotracker.onboarding.data.Gender.MALE -> StrengthGender.MALE
                    com.example.menotracker.onboarding.data.Gender.FEMALE -> StrengthGender.FEMALE
                },
                bodyweightKg = currentPRs.bodyweightKg ?: 80f,
                experienceLevel = when (experienceLevel) {
                    com.example.menotracker.onboarding.data.ExperienceLevel.BEGINNER -> StrengthExperienceLevel.BEGINNER
                    com.example.menotracker.onboarding.data.ExperienceLevel.INTERMEDIATE -> StrengthExperienceLevel.INTERMEDIATE
                    com.example.menotracker.onboarding.data.ExperienceLevel.EXPERIENCED -> StrengthExperienceLevel.EXPERIENCED
                    com.example.menotracker.onboarding.data.ExperienceLevel.ELITE -> StrengthExperienceLevel.ELITE
                    null -> StrengthExperienceLevel.INTERMEDIATE
                },
                currentSquatKg = currentPRs.squatKg ?: 0f,
                currentBenchKg = currentPRs.benchKg ?: 0f,
                currentDeadliftKg = currentPRs.deadliftKg ?: 0f,
                goalSquatKg = goalPRs.squatKg ?: 0f,
                goalBenchKg = goalPRs.benchKg ?: 0f,
                goalDeadliftKg = goalPRs.deadliftKg ?: 0f,
                estimatedWeeks = estimatedWeeks,
                targetDate = targetDate,
                sessionsPerWeek = commitment.sessionsPerWeek,
                effortLevel = commitment.effortLevel
            )
        }
    }
}

@Serializable
enum class StrengthGender {
    @SerialName("male") MALE,
    @SerialName("female") FEMALE
}

@Serializable
enum class StrengthExperienceLevel {
    @SerialName("beginner") BEGINNER,
    @SerialName("intermediate") INTERMEDIATE,
    @SerialName("experienced") EXPERIENCED,
    @SerialName("elite") ELITE;

    val displayName: String
        get() = when (this) {
            BEGINNER -> "Beginner"
            INTERMEDIATE -> "Intermediate"
            EXPERIENCED -> "Experienced"
            ELITE -> "Elite"
        }

    // Expected weekly progress in kg (for total)
    val weeklyProgressRate: Float
        get() = when (this) {
            BEGINNER -> 2.5f
            INTERMEDIATE -> 1.5f
            EXPERIENCED -> 0.75f
            ELITE -> 0.35f
        }
}

@Serializable
enum class LiftType {
    @SerialName("squat") SQUAT,
    @SerialName("bench") BENCH,
    @SerialName("deadlift") DEADLIFT,
    @SerialName("overhead") OVERHEAD
}

/**
 * PR History Entry - Track progress over time
 */
@Serializable
data class PRHistoryEntry(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String, // ISO date
    val lift: LiftType,
    @SerialName("weight_kg") val weightKg: Float,
    val reps: Int = 1,
    @SerialName("estimated_1rm") val estimated1RM: Float, // e1RM if reps > 1
    @SerialName("velocity_ms") val velocityMs: Float? = null, // From VBT
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    companion object {
        /**
         * Calculate estimated 1RM from weight and reps using Epley formula
         */
        fun calculateE1RM(weight: Float, reps: Int): Float {
            return if (reps == 1) weight else weight * (1 + reps / 30f)
        }
    }
}

/**
 * Milestone for goal tracking
 */
@Serializable
data class StrengthMilestone(
    val id: String,
    @SerialName("user_id") val userId: String,
    val week: Int,
    @SerialName("expected_total") val expectedTotal: Float,
    val message: String,
    @SerialName("is_reached") val isReached: Boolean = false,
    @SerialName("reached_at") val reachedAt: String? = null
)

/**
 * Weight suggestion from VBT auto-regulation
 */
enum class WeightSuggestion {
    INCREASE_5KG,
    INCREASE_2_5KG,
    KEEP_WEIGHT,
    DECREASE_2_5KG,
    DECREASE_5KG;

    val adjustment: Float
        get() = when (this) {
            INCREASE_5KG -> 5f
            INCREASE_2_5KG -> 2.5f
            KEEP_WEIGHT -> 0f
            DECREASE_2_5KG -> -2.5f
            DECREASE_5KG -> -5f
        }

    val message: String
        get() = when (this) {
            INCREASE_5KG -> "Velocity high! Add 5kg"
            INCREASE_2_5KG -> "Feeling strong! Add 2.5kg"
            KEEP_WEIGHT -> "Weight is perfect"
            DECREASE_2_5KG -> "Velocity low. Drop 2.5kg"
            DECREASE_5KG -> "Too heavy. Drop 5kg"
        }
}

/**
 * Auto-regulation helper for VBT
 */
object VBTAutoRegulation {
    /**
     * Suggest weight adjustment based on velocity difference
     */
    fun suggestAdjustment(
        targetPercentage: Float,
        measuredVelocity: Float,
        profile: UserStrengthProfile
    ): WeightSuggestion {
        val expectedVelocity = profile.getExpectedVelocity(targetPercentage)
        val velocityDiff = measuredVelocity - expectedVelocity

        return when {
            velocityDiff > 0.08f -> WeightSuggestion.INCREASE_5KG
            velocityDiff > 0.04f -> WeightSuggestion.INCREASE_2_5KG
            velocityDiff < -0.08f -> WeightSuggestion.DECREASE_5KG
            velocityDiff < -0.04f -> WeightSuggestion.DECREASE_2_5KG
            else -> WeightSuggestion.KEEP_WEIGHT
        }
    }
}