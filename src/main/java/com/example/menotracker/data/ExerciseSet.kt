// app/src/main/java/com/example/myapplicationtest/data/ExerciseSet.kt

package com.example.menotracker.data

import java.util.UUID

/**
 * Represents a single set in a workout
 */
data class ExerciseSet(
    val id: String = UUID.randomUUID().toString(),
    val setNumber: Int,
    val targetReps: Int,
    val targetWeight: Double,
    val actualReps: Int? = null,  // null when planning, filled during workout
    val actualWeight: Double? = null,  // null when planning, filled during workout
    val completed: Boolean = false,
    val restSeconds: Int = 90,  // Default 90 seconds rest

    // 🎥 Video & VBT Tracking
    val videoPath: String? = null,  // Local path to analyzed video with pose overlay
    val vbtVelocity: Float? = null,  // Average peak velocity in m/s
    val vbtRepsDetected: Int? = null,  // Reps detected by VBT analysis
    val analysisId: String? = null  // Backend analysis ID for reference
)

/**
 * Exercise with its sets in a workout template
 */
data class ExerciseWithSets(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String,
    val order: Int,
    val sets: List<ExerciseSet>,
    val notes: String = ""
) {
    fun getTotalSets(): Int = sets.size

    fun getSetsSummary(): String {
        if (sets.isEmpty()) return "No sets"
        val firstSet = sets.first()
        val allSame = sets.all {
            it.targetReps == firstSet.targetReps &&
                    it.targetWeight == firstSet.targetWeight
        }

        return if (allSame) {
            "${sets.size} × ${firstSet.targetReps} @ ${formatWeight(firstSet.targetWeight)}"
        } else {
            "${sets.size} sets"
        }
    }

    private fun formatWeight(weight: Double): String {
        return if (weight % 1.0 == 0.0) {
            "${weight.toInt()}kg"
        } else {
            "${weight}kg"
        }
    }

    fun getRestTimeSummary(): String {
        if (sets.isEmpty()) return ""
        val restTime = sets.first().restSeconds
        return when {
            restTime < 60 -> "${restTime}s rest"
            restTime % 60 == 0 -> "${restTime / 60}min rest"
            else -> "${restTime}s rest"
        }
    }
}

/**
 * Predefined rest time presets
 */
object RestTimePresets {
    val SHORT = 30
    val MEDIUM = 60
    val STANDARD = 90
    val LONG = 120
    val VERY_LONG = 180
    val STRENGTH = 240

    fun getPresets(): List<Pair<String, Int>> = listOf(
        "30s (Cardio)" to SHORT,
        "1min (Hypertrophy)" to MEDIUM,
        "1.5min (Standard)" to STANDARD,
        "2min (Strength)" to LONG,
        "3min (Heavy)" to VERY_LONG,
        "4min (Max Strength)" to STRENGTH
    )
}