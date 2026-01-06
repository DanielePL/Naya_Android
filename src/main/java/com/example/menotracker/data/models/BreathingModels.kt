package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Breathing Exercises Datenmodelle
 * FREE: 1 Übung (4-7-8 Relaxation)
 * PREMIUM: Volle Library (5 Übungen)
 */

// ============================================================
// BREATHING PHASES
// ============================================================

/**
 * Phasen einer Atemübung
 */
enum class BreathingPhase(
    val displayName: String,
    val instruction: String
) {
    INHALE("Inhale", "Breathe in slowly"),
    HOLD_IN("Hold", "Hold your breath"),
    EXHALE("Exhale", "Breathe out slowly"),
    HOLD_OUT("Hold", "Pause before next breath"),
    REST("Rest", "Relax naturally")
}

// ============================================================
// BREATHING PATTERN
// ============================================================

/**
 * Definiert das Timing einer Atemübung
 */
data class BreathingPattern(
    val inhaleSeconds: Int,
    val holdInSeconds: Int,
    val exhaleSeconds: Int,
    val holdOutSeconds: Int = 0,
    val cycleDurationSeconds: Int = inhaleSeconds + holdInSeconds + exhaleSeconds + holdOutSeconds
)

// ============================================================
// EXERCISE TYPES
// ============================================================

/**
 * Atemübungs-Typen mit ihren Eigenschaften
 */
enum class BreathingExerciseType(
    val displayName: String,
    val description: String,
    val pattern: BreathingPattern,
    val targetSymptoms: List<String>,
    val difficulty: ExerciseDifficulty,
    val isFree: Boolean,
    val defaultCycles: Int,
    val emoji: String
) {
    RELAXATION_478(
        displayName = "4-7-8 Relaxation",
        description = "Deep relaxation technique for better sleep and reduced anxiety",
        pattern = BreathingPattern(
            inhaleSeconds = 4,
            holdInSeconds = 7,
            exhaleSeconds = 8
        ),
        targetSymptoms = listOf("Sleep Issues", "Anxiety", "Stress"),
        difficulty = ExerciseDifficulty.BEGINNER,
        isFree = true,
        defaultCycles = 4,
        emoji = "😴"
    ),

    BOX_BREATHING(
        displayName = "Box Breathing",
        description = "Navy SEAL technique for stress relief and mental clarity",
        pattern = BreathingPattern(
            inhaleSeconds = 4,
            holdInSeconds = 4,
            exhaleSeconds = 4,
            holdOutSeconds = 4
        ),
        targetSymptoms = listOf("Stress", "Mood Swings", "Anxiety"),
        difficulty = ExerciseDifficulty.INTERMEDIATE,
        isFree = false,
        defaultCycles = 6,
        emoji = "📦"
    ),

    DEEP_BELLY(
        displayName = "Deep Belly Breathing",
        description = "Diaphragmatic breathing for deep relaxation and energy",
        pattern = BreathingPattern(
            inhaleSeconds = 5,
            holdInSeconds = 2,
            exhaleSeconds = 6
        ),
        targetSymptoms = listOf("Anxiety", "Fatigue", "Tension"),
        difficulty = ExerciseDifficulty.BEGINNER,
        isFree = false,
        defaultCycles = 8,
        emoji = "🫁"
    ),

    COOLING_BREATH(
        displayName = "Cooling Breath",
        description = "Sitali Pranayama - cooling technique for hot flashes",
        pattern = BreathingPattern(
            inhaleSeconds = 4,
            holdInSeconds = 2,
            exhaleSeconds = 6
        ),
        targetSymptoms = listOf("Hot Flashes", "Night Sweats", "Irritability"),
        difficulty = ExerciseDifficulty.INTERMEDIATE,
        isFree = false,
        defaultCycles = 10,
        emoji = "❄️"
    ),

    ENERGIZING_BREATH(
        displayName = "Energizing Breath",
        description = "Kapalabhati - quick breaths to boost energy and mental clarity",
        pattern = BreathingPattern(
            inhaleSeconds = 1,
            holdInSeconds = 0,
            exhaleSeconds = 1
        ),
        targetSymptoms = listOf("Fatigue", "Brain Fog", "Low Energy"),
        difficulty = ExerciseDifficulty.ADVANCED,
        isFree = false,
        defaultCycles = 30,
        emoji = "⚡"
    );

    val totalDurationSeconds: Int
        get() = pattern.cycleDurationSeconds * defaultCycles
}

/**
 * Schwierigkeitsgrad
 */
enum class ExerciseDifficulty(
    val displayName: String,
    val color: Long
) {
    BEGINNER("Beginner", 0xFF10B981),      // Green
    INTERMEDIATE("Intermediate", 0xFFF59E0B), // Amber
    ADVANCED("Advanced", 0xFFEF4444)        // Red
}

// ============================================================
// SESSION DATA
// ============================================================

/**
 * Breathing Session aus Supabase
 */
@Serializable
data class BreathingSession(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("exercise_type") val exerciseType: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("cycles_completed") val cyclesCompleted: Int,
    @SerialName("mood_before") val moodBefore: Int? = null, // 1-5
    @SerialName("mood_after") val moodAfter: Int? = null,   // 1-5
    @SerialName("completed_at") val completedAt: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    val exerciseEnum: BreathingExerciseType?
        get() = try {
            BreathingExerciseType.valueOf(exerciseType)
        } catch (e: Exception) {
            null
        }

    val moodImprovement: Int?
        get() = if (moodBefore != null && moodAfter != null) {
            moodAfter - moodBefore
        } else null
}

/**
 * Breathing Session für Insert
 */
@Serializable
data class BreathingSessionInsert(
    val user_id: String,
    val exercise_type: String,
    val duration_seconds: Int,
    val cycles_completed: Int,
    val mood_before: Int? = null,
    val mood_after: Int? = null,
    val completed_at: String
)

// ============================================================
// STATISTICS
// ============================================================

/**
 * Breathing-Statistiken
 */
data class BreathingStats(
    val totalSessions: Int,
    val totalMinutes: Int,
    val favoriteExercise: BreathingExerciseType?,
    val averageMoodImprovement: Float,
    val weeklySessionCount: Int,
    val currentStreak: Int
)

/**
 * Wöchentliche Breathing-Zusammenfassung
 */
data class WeeklyBreathingSummary(
    val sessionsCompleted: Int,
    val totalMinutes: Int,
    val exerciseDistribution: Map<BreathingExerciseType, Int>,
    val averageMoodBefore: Float,
    val averageMoodAfter: Float,
    val bestDay: String?, // Tag mit meisten Sessions
    val streak: Int
)

// ============================================================
// SESSION STATE
// ============================================================

/**
 * Aktueller Zustand einer laufenden Session
 */
data class BreathingSessionState(
    val exerciseType: BreathingExerciseType,
    val currentPhase: BreathingPhase,
    val currentCycle: Int,
    val totalCycles: Int,
    val phaseSecondsRemaining: Int,
    val totalSecondsRemaining: Int,
    val isRunning: Boolean,
    val isPaused: Boolean,
    val progress: Float // 0.0 - 1.0 für Animation
)

/**
 * Initiale Session State erstellen
 */
fun BreathingExerciseType.createInitialState(cycles: Int = defaultCycles): BreathingSessionState {
    return BreathingSessionState(
        exerciseType = this,
        currentPhase = BreathingPhase.INHALE,
        currentCycle = 1,
        totalCycles = cycles,
        phaseSecondsRemaining = pattern.inhaleSeconds,
        totalSecondsRemaining = pattern.cycleDurationSeconds * cycles,
        isRunning = false,
        isPaused = false,
        progress = 0f
    )
}
