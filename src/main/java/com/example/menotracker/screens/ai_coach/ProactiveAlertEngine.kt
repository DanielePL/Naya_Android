package com.example.menotracker.screens.ai_coach

import com.example.menotracker.data.models.*
import com.example.menotracker.data.repository.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * ProactiveAlertEngine - Generates automatic health alerts and recommendations
 *
 * Analyzes user health data to identify patterns and generate proactive
 * notifications that can help women in menopause stay ahead of symptoms.
 */
object ProactiveAlertEngine {

    /**
     * Check all alert conditions and return active alerts
     */
    suspend fun checkAlerts(userId: String, context: MenopauseWellnessContext): List<ProactiveAlert> {
        val alerts = mutableListOf<ProactiveAlert>()

        // Check each alert condition
        checkSleepCrisis(context)?.let { alerts.add(it) }
        checkSleepDeficit(context)?.let { alerts.add(it) }
        checkSymptomSpike(context)?.let { alerts.add(it) }
        checkMoodTrend(context)?.let { alerts.add(it) }
        checkCalciumReminder(context)?.let { alerts.add(it) }
        checkInactivityAlert(context)?.let { alerts.add(it) }
        checkHydrationRisk(context)?.let { alerts.add(it) }
        checkPositiveProgress(context)?.let { alerts.add(it) }

        return alerts.sortedByDescending { it.priority.ordinal }
    }

    /**
     * Get the most important alert (if any) for proactive chat start
     */
    suspend fun getTopAlert(userId: String, context: MenopauseWellnessContext): ProactiveAlert? {
        val alerts = checkAlerts(userId, context)
        return alerts.firstOrNull { it.priority == AlertPriority.HIGH }
            ?: alerts.firstOrNull { it.priority == AlertPriority.MEDIUM }
    }

    // ═══════════════════════════════════════════════════════════════
    // ALERT RULES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Rule: Sleep Crisis
     * Trigger: Less than 5 hours sleep for 3+ consecutive nights
     */
    private fun checkSleepCrisis(context: MenopauseWellnessContext): ProactiveAlert? {
        val stats = context.sleepStats ?: return null
        if (stats.daysLogged < 3) return null

        // Check for severe sleep deficit
        if (stats.averageHours < 5f && stats.daysLogged >= 3) {
            return ProactiveAlert(
                id = "sleep_crisis",
                type = AlertType.SLEEP_CRISIS,
                priority = AlertPriority.HIGH,
                title = "Sleep Alert",
                message = "You've been averaging only ${String.format("%.1f", stats.averageHours)} hours of sleep. This can significantly impact your menopause symptoms and overall wellbeing.",
                suggestion = "Would you like to talk about sleep strategies that might help?",
                emoji = "😴",
                actionText = "Get Sleep Help"
            )
        }
        return null
    }

    /**
     * Rule: Sleep Deficit
     * Trigger: Less than 6 hours average sleep
     */
    private fun checkSleepDeficit(context: MenopauseWellnessContext): ProactiveAlert? {
        val stats = context.sleepStats ?: return null
        if (stats.daysLogged < 3) return null

        if (stats.averageHours in 5f..6f) {
            return ProactiveAlert(
                id = "sleep_deficit",
                type = AlertType.SLEEP_DEFICIT,
                priority = AlertPriority.MEDIUM,
                title = "Sleep Below Target",
                message = "Your sleep has been ${String.format("%.1f", stats.averageHours)} hours on average. For menopause wellness, 7-8 hours is ideal.",
                suggestion = "Small changes like earlier bedtime or reducing caffeine after noon can help.",
                emoji = "🌙",
                actionText = "Sleep Tips"
            )
        }
        return null
    }

    /**
     * Rule: Symptom Spike
     * Trigger: >50% increase in any symptom compared to baseline, or high-intensity symptoms
     */
    private fun checkSymptomSpike(context: MenopauseWellnessContext): ProactiveAlert? {
        // Check for high-intensity symptoms
        val severeSymptoms = context.symptomStats.filter { (_, stats) ->
            stats.avgIntensity >= 7 && stats.occurrenceCount >= 3
        }

        if (severeSymptoms.isNotEmpty()) {
            val worstSymptom = severeSymptoms.maxByOrNull { it.value.avgIntensity }!!
            return ProactiveAlert(
                id = "symptom_spike_${worstSymptom.key.name}",
                type = AlertType.SYMPTOM_SPIKE,
                priority = AlertPriority.HIGH,
                title = "${worstSymptom.key.displayName} Alert",
                message = "You've logged ${worstSymptom.value.occurrenceCount} ${worstSymptom.key.displayName.lowercase()} episodes this week with high intensity (${String.format("%.1f", worstSymptom.value.avgIntensity)}/10).",
                suggestion = "Let's explore some relief strategies together.",
                emoji = getSymptomEmoji(worstSymptom.key),
                actionText = "Get Relief Tips"
            )
        }

        // Check for frequent symptoms (even if not severe)
        val frequentSymptoms = context.symptomStats.filter { (_, stats) ->
            stats.occurrenceCount >= 5
        }

        if (frequentSymptoms.isNotEmpty()) {
            val mostFrequent = frequentSymptoms.maxByOrNull { it.value.occurrenceCount }!!
            return ProactiveAlert(
                id = "symptom_frequent_${mostFrequent.key.name}",
                type = AlertType.SYMPTOM_SPIKE,
                priority = AlertPriority.MEDIUM,
                title = "Pattern Detected",
                message = "${mostFrequent.key.displayName} has occurred ${mostFrequent.value.occurrenceCount} times this week.",
                suggestion = "I've noticed a pattern. Would you like to discuss potential triggers?",
                emoji = getSymptomEmoji(mostFrequent.key),
                actionText = "Discuss Pattern"
            )
        }

        return null
    }

    /**
     * Rule: Mood Trend
     * Trigger: Negative mood (sad, anxious, angry) dominant for 5+ days
     */
    private fun checkMoodTrend(context: MenopauseWellnessContext): ProactiveAlert? {
        val stats = context.moodStats ?: return null
        if (stats.totalEntries < 3) return null

        val negativeMoods = listOf(MoodType.SAD, MoodType.ANXIOUS, MoodType.ANGRY)

        val negativePercentage = stats.moodDistribution
            .filter { it.key in negativeMoods }
            .values
            .sumOf { it.percentage.toDouble() }

        if (negativePercentage > 0.6 && stats.totalEntries >= 3) {
            val dominantMood = stats.dominantMood
            return ProactiveAlert(
                id = "mood_trend",
                type = AlertType.MOOD_PATTERN,
                priority = AlertPriority.MEDIUM,
                title = "Checking In",
                message = "Your mood has been leaning towards ${dominantMood?.displayName?.lowercase() ?: "challenging"} lately. Mood changes are very common during menopause.",
                suggestion = "Would you like to talk about what's been on your mind?",
                emoji = dominantMood?.emoji ?: "💜",
                actionText = "Talk About It"
            )
        }
        return null
    }

    /**
     * Rule: Calcium Reminder
     * Trigger: No calcium logged for 3+ days (bone health)
     */
    private fun checkCalciumReminder(context: MenopauseWellnessContext): ProactiveAlert? {
        if (context.boneHealthLogs.isEmpty()) {
            // No bone health tracking at all - gentle reminder
            return ProactiveAlert(
                id = "calcium_intro",
                type = AlertType.NUTRITION_GAP,
                priority = AlertPriority.LOW,
                title = "Bone Health Tip",
                message = "Did you know calcium is especially important during menopause? Tracking your intake can help maintain strong bones.",
                suggestion = "I can share some easy calcium-rich food ideas.",
                emoji = "🦴",
                actionText = "Learn More"
            )
        }

        // Check recent logs
        val avgCalcium = context.boneHealthLogs.map { it.calciumMg }.average()
        if (avgCalcium < BoneHealthLog.CALCIUM_GOAL * 0.5) {
            return ProactiveAlert(
                id = "calcium_low",
                type = AlertType.NUTRITION_GAP,
                priority = AlertPriority.MEDIUM,
                title = "Calcium Check",
                message = "Your calcium intake has been averaging ${avgCalcium.toInt()}mg (goal: ${BoneHealthLog.CALCIUM_GOAL.toInt()}mg). Bone health is especially important now.",
                suggestion = "Would you like some practical tips to boost your calcium?",
                emoji = "🥛",
                actionText = "Get Tips"
            )
        }
        return null
    }

    /**
     * Rule: Inactivity Alert
     * Trigger: No workouts logged for 5+ days
     */
    private fun checkInactivityAlert(context: MenopauseWellnessContext): ProactiveAlert? {
        val strengthDays = context.boneHealthLogs.count { it.strengthTrainingDone }

        if (context.boneHealthLogs.size >= 5 && strengthDays == 0) {
            return ProactiveAlert(
                id = "inactivity",
                type = AlertType.INACTIVITY,
                priority = AlertPriority.LOW,
                title = "Movement Reminder",
                message = "Regular movement helps with menopause symptoms, especially sleep and mood. Even a 15-minute walk makes a difference!",
                suggestion = "Want some menopause-friendly exercise ideas?",
                emoji = "🚶‍♀️",
                actionText = "Show Exercises"
            )
        }
        return null
    }

    /**
     * Rule: Dehydration Risk
     * Trigger: Many hot flashes + no hydration tracking
     */
    private fun checkHydrationRisk(context: MenopauseWellnessContext): ProactiveAlert? {
        val hotFlashCount = context.symptomStats[MenopauseSymptomType.HOT_FLASH]?.occurrenceCount ?: 0
        val nightSweatCount = context.symptomStats[MenopauseSymptomType.NIGHT_SWEAT]?.occurrenceCount ?: 0

        if (hotFlashCount + nightSweatCount >= 5) {
            return ProactiveAlert(
                id = "hydration_risk",
                type = AlertType.HYDRATION_REMINDER,
                priority = AlertPriority.MEDIUM,
                title = "Stay Hydrated",
                message = "With ${hotFlashCount + nightSweatCount} hot flashes/night sweats this week, staying well-hydrated is extra important.",
                suggestion = "Aim for 8+ glasses of water daily. Cold water can also help during hot flashes!",
                emoji = "💧",
                actionText = "Hydration Tips"
            )
        }
        return null
    }

    /**
     * Rule: Positive Progress
     * Trigger: Improvement in symptoms or sleep
     */
    private fun checkPositiveProgress(context: MenopauseWellnessContext): ProactiveAlert? {
        val sleepStats = context.sleepStats ?: return null

        // Good sleep pattern
        if (sleepStats.averageHours >= 7f && sleepStats.averageQuality >= 3.5f && sleepStats.daysLogged >= 5) {
            return ProactiveAlert(
                id = "positive_sleep",
                type = AlertType.POSITIVE_PROGRESS,
                priority = AlertPriority.LOW,
                title = "Great Sleep!",
                message = "You've been getting ${String.format("%.1f", sleepStats.averageHours)} hours of quality sleep! Keep up these great habits.",
                suggestion = "Consistency is key - your body is thanking you!",
                emoji = "🌟",
                actionText = "See What's Working"
            )
        }

        // Low symptom week
        val totalSymptomOccurrences = context.symptomStats.values.sumOf { it.occurrenceCount }
        val avgIntensity = if (context.symptomStats.isNotEmpty()) {
            context.symptomStats.values.map { it.avgIntensity }.average()
        } else 0.0

        if (totalSymptomOccurrences <= 3 && context.recentSymptoms.isNotEmpty()) {
            return ProactiveAlert(
                id = "positive_symptoms",
                type = AlertType.POSITIVE_PROGRESS,
                priority = AlertPriority.LOW,
                title = "Low Symptom Week!",
                message = "Only $totalSymptomOccurrences symptom episodes this week. That's a win!",
                suggestion = "Let's note what's been working well for you.",
                emoji = "✨",
                actionText = "Track What Works"
            )
        }

        return null
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun getSymptomEmoji(symptom: MenopauseSymptomType): String {
        return when (symptom) {
            MenopauseSymptomType.HOT_FLASH -> "🔥"
            MenopauseSymptomType.NIGHT_SWEAT -> "😓"
            MenopauseSymptomType.MOOD_SWING -> "🎭"
            MenopauseSymptomType.ANXIETY -> "😰"
            MenopauseSymptomType.FATIGUE -> "😴"
            MenopauseSymptomType.BRAIN_FOG -> "🌫️"
            MenopauseSymptomType.SLEEP_ISSUE -> "🛏️"
            MenopauseSymptomType.JOINT_PAIN -> "🦴"
            MenopauseSymptomType.HEADACHE -> "🤕"
            MenopauseSymptomType.WEIGHT_GAIN -> "⚖️"
            MenopauseSymptomType.LOW_LIBIDO -> "💔"
            MenopauseSymptomType.VAGINAL_DRYNESS -> "💧"
            MenopauseSymptomType.HEART_PALPITATIONS -> "💓"
            MenopauseSymptomType.IRRITABILITY -> "😤"
        }
    }
}

/**
 * Proactive alert data class
 */
data class ProactiveAlert(
    val id: String,
    val type: AlertType,
    val priority: AlertPriority,
    val title: String,
    val message: String,
    val suggestion: String,
    val emoji: String,
    val actionText: String,
    val dismissedAt: Long? = null
) {
    /**
     * Create a message for the AI to start a proactive conversation
     */
    fun toConversationStarter(): String {
        return """
            |$emoji **$title**
            |
            |$message
            |
            |$suggestion
        """.trimMargin()
    }

    /**
     * Check if this alert should be shown based on priority
     */
    fun shouldShowBanner(): Boolean {
        return priority in listOf(AlertPriority.HIGH, AlertPriority.MEDIUM)
    }
}

enum class AlertType {
    SLEEP_CRISIS,
    SLEEP_DEFICIT,
    SYMPTOM_SPIKE,
    MOOD_PATTERN,
    NUTRITION_GAP,
    INACTIVITY,
    HYDRATION_REMINDER,
    POSITIVE_PROGRESS
}

enum class AlertPriority {
    LOW,
    MEDIUM,
    HIGH
}
