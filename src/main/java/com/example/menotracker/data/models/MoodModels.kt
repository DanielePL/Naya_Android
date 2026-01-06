package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mood Journaling Datenmodelle
 * FREE: 3 Check-ins pro Woche
 * PREMIUM: Unbegrenzt
 */

// ============================================================
// MOOD TYPES
// ============================================================

/**
 * Stimmungstypen mit Emoji und Display-Namen
 */
enum class MoodType(
    val emoji: String,
    val displayName: String,
    val color: Long
) {
    HAPPY("😊", "Happy", 0xFF10B981),      // Green
    CALM("😌", "Calm", 0xFF60A5FA),         // Blue
    NEUTRAL("😐", "Neutral", 0xFF9CA3AF),   // Gray
    SAD("😢", "Sad", 0xFFA78BFA),            // Purple (aufgehellt)
    ANGRY("😤", "Irritated", 0xFFEF4444),   // Red
    ANXIOUS("😰", "Anxious", 0xFFF59E0B)    // Amber
}

/**
 * Stimmungs-Trigger / Auslöser
 */
enum class MoodTrigger(
    val displayName: String,
    val emoji: String
) {
    STRESS("Stress", "😓"),
    SLEEP("Sleep Issues", "😴"),
    HORMONES("Hormonal Changes", "🌡️"),
    DIET("Diet", "🍽️"),
    EXERCISE("Exercise", "🏃‍♀️"),
    WEATHER("Weather", "🌤️"),
    SOCIAL("Social", "👥"),
    WORK("Work", "💼"),
    HEALTH("Health", "🩺"),
    FAMILY("Family", "👨‍👩‍👧"),
    FINANCES("Finances", "💰"),
    MENOPAUSE("Menopause Symptoms", "🔥"),
    OTHER("Other", "📝")
}

/**
 * Tageszeit für Mood-Logging
 */
enum class TimeOfDay(
    val displayName: String,
    val emoji: String
) {
    MORNING("Morning", "🌅"),
    AFTERNOON("Afternoon", "☀️"),
    EVENING("Evening", "🌆"),
    NIGHT("Night", "🌙")
}

// ============================================================
// MOOD ENTRY
// ============================================================

/**
 * Mood-Eintrag aus Supabase
 */
@Serializable
data class MoodEntry(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("mood_type") val moodType: String,
    val intensity: Int, // 1-5
    @SerialName("time_of_day") val timeOfDay: String? = null,
    val triggers: List<String>? = null,
    @SerialName("journal_text") val journalText: String? = null,
    @SerialName("linked_symptom_ids") val linkedSymptomIds: List<String>? = null,
    @SerialName("logged_at") val loggedAt: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    val moodEnum: MoodType?
        get() = try {
            MoodType.valueOf(moodType)
        } catch (e: Exception) {
            null
        }

    val timeOfDayEnum: TimeOfDay?
        get() = try {
            timeOfDay?.let { TimeOfDay.valueOf(it) }
        } catch (e: Exception) {
            null
        }

    val triggerEnums: List<MoodTrigger>
        get() = triggers?.mapNotNull { trigger ->
            try {
                MoodTrigger.valueOf(trigger)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
}

/**
 * Mood-Eintrag für Insert
 */
@Serializable
data class MoodEntryInsert(
    val user_id: String,
    val mood_type: String,
    val intensity: Int,
    val time_of_day: String? = null,
    val triggers: List<String>? = null,
    val journal_text: String? = null,
    val linked_symptom_ids: List<String>? = null,
    val logged_at: String
)

// ============================================================
// STATISTICS
// ============================================================

/**
 * Mood-Statistiken für einen Zeitraum
 */
data class MoodStats(
    val moodType: MoodType,
    val occurrenceCount: Int,
    val avgIntensity: Float,
    val percentage: Float
)

/**
 * Wöchentliche Mood-Zusammenfassung
 */
data class WeeklyMoodSummary(
    val totalEntries: Int,
    val dominantMood: MoodType?,
    val averageIntensity: Float,
    val moodDistribution: Map<MoodType, MoodStats>,
    val mostCommonTriggers: List<Pair<MoodTrigger, Int>>,
    val entriesRemaining: Int, // Für Free Tier
    val canAddMore: Boolean
)

/**
 * Mood-Trend über Zeit
 */
data class MoodTrendPoint(
    val date: String,
    val averageMoodScore: Float, // -2 (sehr schlecht) bis +2 (sehr gut)
    val dominantMood: MoodType?
)

/**
 * Konvertiert MoodType zu numerischem Score für Trend-Analyse
 */
fun MoodType.toScore(): Int = when (this) {
    MoodType.HAPPY -> 2
    MoodType.CALM -> 1
    MoodType.NEUTRAL -> 0
    MoodType.SAD -> -1
    MoodType.ANXIOUS -> -1
    MoodType.ANGRY -> -2
}
