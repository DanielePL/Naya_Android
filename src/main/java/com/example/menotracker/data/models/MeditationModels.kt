package com.example.menotracker.data.models

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Meditation Models for Mindfulness Feature
 *
 * FREE: 2 meditations (Body Scan, Gratitude)
 * PREMIUM: Full library + Soundscape Mixer
 */

// ============================================================
// MEDITATION TYPES
// ============================================================

/**
 * Available meditation types with their configurations
 */
enum class MeditationType(
    val displayName: String,
    val description: String,
    val longDescription: String,
    val defaultDurationMinutes: Int,
    val availableDurations: List<Int>,
    val isFree: Boolean,
    val emoji: String,
    val category: MeditationCategory,
    val targetSymptoms: List<String>
) {
    // FREE meditations
    BODY_SCAN(
        displayName = "Body Scan",
        description = "Progressive relaxation",
        longDescription = "Systematically focus on different parts of your body to release tension and promote deep relaxation.",
        defaultDurationMinutes = 10,
        availableDurations = listOf(5, 10, 15),
        isFree = true,
        emoji = "\uD83E\uDDD8",
        category = MeditationCategory.RELAXATION,
        targetSymptoms = listOf("Stress", "Muscle tension", "Sleep issues")
    ),
    GRATITUDE(
        displayName = "Gratitude",
        description = "Focus on thankfulness",
        longDescription = "Cultivate positive emotions by reflecting on the things you're grateful for in your life.",
        defaultDurationMinutes = 5,
        availableDurations = listOf(3, 5, 10),
        isFree = true,
        emoji = "\uD83D\uDE4F",
        category = MeditationCategory.MINDFULNESS,
        targetSymptoms = listOf("Mood swings", "Anxiety", "Negative thoughts")
    ),

    // PREMIUM meditations
    STRESS_RELIEF(
        displayName = "Stress Relief",
        description = "Release tension",
        longDescription = "A guided practice to help you identify and release stress from your body and mind.",
        defaultDurationMinutes = 15,
        availableDurations = listOf(10, 15, 20),
        isFree = false,
        emoji = "\uD83D\uDE0C",
        category = MeditationCategory.RELAXATION,
        targetSymptoms = listOf("Stress", "Anxiety", "Overwhelm")
    ),
    SLEEP_PREP(
        displayName = "Sleep Preparation",
        description = "Wind down for sleep",
        longDescription = "Gentle guidance to help you transition from wakefulness to restful sleep.",
        defaultDurationMinutes = 20,
        availableDurations = listOf(15, 20, 30),
        isFree = false,
        emoji = "\uD83D\uDE34",
        category = MeditationCategory.SLEEP,
        targetSymptoms = listOf("Insomnia", "Night sweats", "Racing thoughts")
    ),
    MENOPAUSE_CALM(
        displayName = "Menopause Calm",
        description = "Hot flash relief",
        longDescription = "Specialized meditation to help manage hot flashes and menopause symptoms through cooling visualizations.",
        defaultDurationMinutes = 10,
        availableDurations = listOf(5, 10, 15),
        isFree = false,
        emoji = "\uD83C\uDF38",
        category = MeditationCategory.MENOPAUSE,
        targetSymptoms = listOf("Hot flashes", "Night sweats", "Anxiety")
    ),
    MORNING_ENERGY(
        displayName = "Morning Energy",
        description = "Energizing start",
        longDescription = "Begin your day with intention and positive energy through this uplifting meditation.",
        defaultDurationMinutes = 10,
        availableDurations = listOf(5, 10, 15),
        isFree = false,
        emoji = "\u2600\uFE0F",
        category = MeditationCategory.ENERGY,
        targetSymptoms = listOf("Fatigue", "Low mood", "Lack of motivation")
    ),
    LOVING_KINDNESS(
        displayName = "Loving Kindness",
        description = "Compassion practice",
        longDescription = "Cultivate feelings of love and compassion for yourself and others.",
        defaultDurationMinutes = 15,
        availableDurations = listOf(10, 15, 20),
        isFree = false,
        emoji = "\u2764\uFE0F",
        category = MeditationCategory.MINDFULNESS,
        targetSymptoms = listOf("Self-criticism", "Relationship stress", "Isolation")
    ),
    FOCUS(
        displayName = "Focus & Clarity",
        description = "Sharpen your mind",
        longDescription = "Improve concentration and mental clarity through mindful attention training.",
        defaultDurationMinutes = 10,
        availableDurations = listOf(5, 10, 15),
        isFree = false,
        emoji = "\uD83C\uDFAF",
        category = MeditationCategory.FOCUS,
        targetSymptoms = listOf("Brain fog", "Concentration issues", "Mental fatigue")
    );

    companion object {
        fun getFreeMeditations() = entries.filter { it.isFree }
        fun getPremiumMeditations() = entries.filter { !it.isFree }
        fun getByCategory(category: MeditationCategory) = entries.filter { it.category == category }
    }
}

/**
 * Meditation categories for grouping
 */
enum class MeditationCategory(val displayName: String, val emoji: String) {
    RELAXATION("Relaxation", "\uD83E\uDDD8"),
    MINDFULNESS("Mindfulness", "\uD83E\uDDD0"),
    SLEEP("Sleep", "\uD83C\uDF19"),
    MENOPAUSE("Menopause", "\uD83C\uDF38"),
    ENERGY("Energy", "\u26A1"),
    FOCUS("Focus", "\uD83C\uDFAF")
}

// ============================================================
// SESSION DATA
// ============================================================

/**
 * Recorded meditation session
 */
data class MeditationSession(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val meditationType: MeditationType,
    val durationSeconds: Int,
    val soundsUsed: List<AmbientSound> = emptyList(),
    val musicUsed: BackgroundMusic? = null,
    val moodBefore: Int? = null,
    val moodAfter: Int? = null,
    val completedAt: OffsetDateTime = OffsetDateTime.now(),
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

/**
 * Insert DTO for Supabase
 */
data class MeditationSessionInsert(
    val user_id: String,
    val meditation_type: String,
    val duration_seconds: Int,
    val sounds_used: String? = null, // JSON array of sound names
    val music_used: String? = null,
    val mood_before: Int? = null,
    val mood_after: Int? = null,
    val completed_at: String
)

// ============================================================
// STATS
// ============================================================

/**
 * Aggregated meditation statistics
 */
data class MeditationStats(
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageSessionMinutes: Float = 0f,
    val favoriteType: MeditationType? = null,
    val averageMoodImprovement: Float = 0f,
    val sessionsThisWeek: Int = 0,
    val sessionsThisMonth: Int = 0
)

// ============================================================
// SESSION STATE
// ============================================================

/**
 * Active meditation session state
 */
data class MeditationSessionState(
    val meditationType: MeditationType,
    val totalDurationSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val currentPhase: MeditationPhase = MeditationPhase.INTRO
) {
    val elapsedSeconds: Int get() = totalDurationSeconds - remainingSeconds
    val isComplete: Boolean get() = remainingSeconds <= 0
}

/**
 * Phases within a meditation session
 */
enum class MeditationPhase(val displayName: String) {
    INTRO("Getting Started"),
    MAIN("Meditation"),
    CLOSING("Closing"),
    COMPLETE("Complete")
}

/**
 * Create initial session state for a meditation type
 */
fun MeditationType.createSessionState(durationMinutes: Int = this.defaultDurationMinutes): MeditationSessionState {
    val totalSeconds = durationMinutes * 60
    return MeditationSessionState(
        meditationType = this,
        totalDurationSeconds = totalSeconds,
        remainingSeconds = totalSeconds,
        isRunning = false,
        isPaused = false,
        progress = 0f,
        currentPhase = MeditationPhase.INTRO
    )
}
