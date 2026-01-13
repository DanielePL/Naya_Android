package com.example.menotracker.screens.ai_coach

import com.example.menotracker.data.models.*
import com.example.menotracker.data.repository.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * LocalUserContextBuilder - Builds comprehensive system prompts for the AI Coach
 *
 * Aggregates all user health data to create rich, personalized context
 * that enables the AI to provide empathetic, informed menopause support.
 */
object LocalUserContextBuilder {

    /**
     * Build the complete menopause wellness context string for the AI system prompt
     */
    suspend fun buildContext(
        userId: String,
        userProfile: UserProfile?
    ): MenopauseWellnessContext {
        // Fetch all relevant data in parallel
        val symptoms = SymptomRepository.getSymptomHistory(userId, 7).getOrNull() ?: emptyList()
        val symptomStats = SymptomRepository.weeklyStats.value
        val sleepLogs = SleepRepository.getSleepHistory(userId, 7).getOrNull() ?: emptyList()
        val sleepStats = SleepRepository.getWeeklyStats(userId).getOrNull()
        val moods = MoodRepository.getMoodHistory(userId, 7).getOrNull() ?: emptyList()
        val moodStats = MoodRepository.weeklyStats.value
        val boneHealthLogs = BoneHealthRepository.getHistory(userId, 7).getOrNull() ?: emptyList()
        val todayBoneHealth = BoneHealthRepository.todayLog.value

        return MenopauseWellnessContext(
            userProfile = userProfile,
            recentSymptoms = symptoms,
            symptomStats = symptomStats,
            sleepLogs = sleepLogs,
            sleepStats = sleepStats,
            recentMoods = moods,
            moodStats = moodStats,
            boneHealthLogs = boneHealthLogs,
            todayBoneHealth = todayBoneHealth
        )
    }

    /**
     * Convert wellness context to a formatted system prompt string
     */
    fun formatContextForPrompt(context: MenopauseWellnessContext): String {
        val sb = StringBuilder()

        sb.appendLine("═══════════════════════════════════════════════════════════════")
        sb.appendLine("MENOPAUSE WELLNESS CONTEXT")
        sb.appendLine("═══════════════════════════════════════════════════════════════")
        sb.appendLine()

        // User Profile Section
        context.userProfile?.let { profile ->
            sb.appendLine("## User Profile")
            profile.name.let { sb.appendLine("- Name: ${it.split(" ").firstOrNull() ?: "User"}") }
            profile.age?.let { sb.appendLine("- Age: $it") }
            profile.menopauseStage?.let { sb.appendLine("- Menopause Stage: ${formatMenopauseStage(it)}") }

            if (profile.primarySymptoms.isNotEmpty()) {
                sb.appendLine("- Primary Concerns: ${profile.primarySymptoms.joinToString(", ") { formatSymptomName(it) }}")
            }

            if (profile.wellnessGoals.isNotEmpty()) {
                sb.appendLine("- Wellness Goals: ${profile.wellnessGoals.joinToString(", ") { formatGoalName(it) }}")
            }

            // Dietary info
            if (profile.dietaryPreferences.isNotEmpty()) {
                sb.appendLine("- Diet: ${profile.dietaryPreferences.joinToString(", ")}")
            }
            if (profile.foodAllergies.isNotEmpty()) {
                sb.appendLine("- Allergies: ${profile.foodAllergies.joinToString(", ")}")
            }

            sb.appendLine()
        }

        // Recent Symptoms Section
        if (context.recentSymptoms.isNotEmpty()) {
            sb.appendLine("## Recent Symptoms (Last 7 Days)")

            // Group symptoms by type and calculate stats
            val symptomGroups = context.recentSymptoms.groupBy { it.symptomEnum }

            symptomGroups.forEach { (symptomType, logs) ->
                symptomType?.let { type ->
                    val avgIntensity = logs.map { it.intensity }.average()
                    val count = logs.size
                    val maxIntensity = logs.maxOfOrNull { it.intensity } ?: 0

                    sb.appendLine("- ${type.displayName}: $count episodes, avg intensity ${String.format("%.1f", avgIntensity)}/10, max $maxIntensity/10")

                    // Add common triggers if present
                    val triggers = logs.flatMap { it.triggers ?: emptyList() }
                        .groupingBy { it }
                        .eachCount()
                        .toList()
                        .sortedByDescending { it.second }
                        .take(3)

                    if (triggers.isNotEmpty()) {
                        sb.appendLine("  Triggers: ${triggers.joinToString(", ") { "${it.first} (${it.second}x)" }}")
                    }
                }
            }
            sb.appendLine()
        }

        // Sleep Section
        context.sleepStats?.let { stats ->
            if (stats.daysLogged > 0) {
                sb.appendLine("## Sleep Quality (Last 7 Days)")
                sb.appendLine("- Average Hours: ${String.format("%.1f", stats.averageHours)} hrs/night")
                sb.appendLine("- Average Quality: ${String.format("%.1f", stats.averageQuality)}/5")
                sb.appendLine("- Total Interruptions: ${stats.totalInterruptions}")
                stats.mostCommonInterruption?.let {
                    sb.appendLine("- Most Common Interruption: ${it.displayName}")
                }

                // Sleep assessment
                val sleepAssessment = when {
                    stats.averageHours < 5f -> "CRITICAL - severe sleep deficit"
                    stats.averageHours < 6f -> "POOR - significant sleep deficit"
                    stats.averageHours < 7f -> "FAIR - slightly below recommended"
                    stats.averageHours <= 9f -> "GOOD - within recommended range"
                    else -> "EXCESSIVE - may indicate other issues"
                }
                sb.appendLine("- Assessment: $sleepAssessment")
                sb.appendLine()
            }
        }

        // Mood Section
        context.moodStats?.let { stats ->
            if (stats.totalEntries > 0) {
                sb.appendLine("## Mood Patterns (Last 7 Days)")
                sb.appendLine("- Entries Logged: ${stats.totalEntries}")
                stats.dominantMood?.let {
                    sb.appendLine("- Dominant Mood: ${it.displayName}")
                }
                sb.appendLine("- Average Intensity: ${String.format("%.1f", stats.averageIntensity)}/5")

                // Mood distribution
                if (stats.moodDistribution.isNotEmpty()) {
                    sb.appendLine("- Distribution:")
                    stats.moodDistribution
                        .toList()
                        .sortedByDescending { it.second.percentage }
                        .take(3)
                        .forEach { (mood, moodStats) ->
                            sb.appendLine("  • ${mood.displayName}: ${(moodStats.percentage * 100).toInt()}%")
                        }
                }

                // Common triggers
                if (stats.mostCommonTriggers.isNotEmpty()) {
                    sb.appendLine("- Common Triggers: ${stats.mostCommonTriggers.take(3).joinToString(", ") { "${it.first.displayName}" }}")
                }
                sb.appendLine()
            }
        }

        // Bone Health Section
        if (context.boneHealthLogs.isNotEmpty()) {
            sb.appendLine("## Bone Health (Last 7 Days)")

            val avgCalcium = context.boneHealthLogs.map { it.calciumMg }.average()
            val avgVitD = context.boneHealthLogs.map { it.vitaminDIu }.average()
            val strengthDays = context.boneHealthLogs.count { it.strengthTrainingDone }

            sb.appendLine("- Avg Daily Calcium: ${avgCalcium.toInt()}mg (Goal: ${BoneHealthLog.CALCIUM_GOAL.toInt()}mg)")
            sb.appendLine("- Avg Daily Vitamin D: ${avgVitD.toInt()} IU (Goal: ${BoneHealthLog.VITAMIN_D_GOAL.toInt()} IU)")
            sb.appendLine("- Strength Training Days: $strengthDays/7")

            // Assessment
            val calciumAssessment = when {
                avgCalcium < BoneHealthLog.CALCIUM_GOAL * 0.5 -> "LOW - needs improvement"
                avgCalcium < BoneHealthLog.CALCIUM_GOAL * 0.8 -> "FAIR - approaching goal"
                else -> "GOOD - meeting goals"
            }
            sb.appendLine("- Calcium Assessment: $calciumAssessment")
            sb.appendLine()
        }

        // Today's Status
        sb.appendLine("## Today's Status")
        sb.appendLine("- Date: ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))}")

        val todaySymptoms = context.recentSymptoms.filter {
            it.loggedAt.startsWith(LocalDate.now().toString())
        }
        if (todaySymptoms.isNotEmpty()) {
            sb.appendLine("- Symptoms Today: ${todaySymptoms.map { it.symptomEnum?.displayName ?: it.symptomType }.joinToString(", ")}")
        } else {
            sb.appendLine("- No symptoms logged today")
        }

        context.todayBoneHealth?.let { bone ->
            sb.appendLine("- Calcium Today: ${bone.calciumMg.toInt()}mg / ${BoneHealthLog.CALCIUM_GOAL.toInt()}mg")
            sb.appendLine("- Vitamin D Today: ${bone.vitaminDIu.toInt()} IU / ${BoneHealthLog.VITAMIN_D_GOAL.toInt()} IU")
        }

        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════════════════════════════")

        return sb.toString()
    }

    /**
     * Generate a short context summary for quick reference
     */
    fun generateQuickSummary(context: MenopauseWellnessContext): String {
        val parts = mutableListOf<String>()

        context.userProfile?.let { profile ->
            profile.menopauseStage?.let { parts.add("Stage: ${formatMenopauseStage(it)}") }
        }

        // Most pressing symptoms
        val topSymptoms = context.symptomStats
            .toList()
            .sortedByDescending { it.second.occurrenceCount }
            .take(2)
            .map { it.first.displayName }

        if (topSymptoms.isNotEmpty()) {
            parts.add("Top symptoms: ${topSymptoms.joinToString(", ")}")
        }

        // Sleep status
        context.sleepStats?.let { stats ->
            if (stats.averageHours < 6f) {
                parts.add("Sleep: ${String.format("%.1f", stats.averageHours)}h (low)")
            }
        }

        // Mood status
        context.moodStats?.dominantMood?.let { mood ->
            if (mood in listOf(MoodType.SAD, MoodType.ANXIOUS, MoodType.ANGRY)) {
                parts.add("Mood: ${mood.displayName}")
            }
        }

        return if (parts.isNotEmpty()) parts.joinToString(" | ") else "No recent health data"
    }

    // ═══════════════════════════════════════════════════════════════
    // FORMATTING HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun formatMenopauseStage(stage: String): String {
        return when (stage.lowercase()) {
            "perimenopause" -> "Perimenopause (transition phase)"
            "menopause" -> "Menopause (12+ months without period)"
            "postmenopause" -> "Postmenopause"
            "premenopause" -> "Premenopause"
            "early_perimenopause" -> "Early Perimenopause"
            "late_perimenopause" -> "Late Perimenopause"
            "not_sure" -> "Not sure / Unspecified"
            else -> stage.replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatSymptomName(symptom: String): String {
        return when (symptom.lowercase()) {
            "hot_flashes" -> "Hot Flashes"
            "night_sweats" -> "Night Sweats"
            "sleep_issues" -> "Sleep Problems"
            "mood_changes" -> "Mood Changes"
            "anxiety" -> "Anxiety"
            "fatigue" -> "Fatigue"
            "weight_gain" -> "Weight Changes"
            "brain_fog" -> "Brain Fog"
            "joint_pain" -> "Joint Pain"
            "headaches" -> "Headaches"
            else -> symptom.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    private fun formatGoalName(goal: String): String {
        return when (goal.lowercase()) {
            "better_sleep" -> "Better Sleep"
            "manage_symptoms" -> "Manage Symptoms"
            "stay_active" -> "Stay Active"
            "reduce_stress" -> "Reduce Stress"
            "hormonal_balance" -> "Hormonal Balance"
            "bone_health" -> "Bone Health"
            "weight_management" -> "Weight Management"
            "energy_boost" -> "More Energy"
            else -> goal.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }
}

/**
 * Complete wellness context data class
 */
data class MenopauseWellnessContext(
    val userProfile: UserProfile?,
    val recentSymptoms: List<SymptomLog>,
    val symptomStats: Map<MenopauseSymptomType, SymptomStats>,
    val sleepLogs: List<SleepLog>,
    val sleepStats: WeeklySleepSummary?,
    val recentMoods: List<MoodEntry>,
    val moodStats: WeeklyMoodSummary?,
    val boneHealthLogs: List<BoneHealthLog>,
    val todayBoneHealth: BoneHealthLog?
) {
    /**
     * Check if user has logged any health data
     */
    val hasData: Boolean
        get() = recentSymptoms.isNotEmpty() ||
                sleepLogs.isNotEmpty() ||
                recentMoods.isNotEmpty() ||
                boneHealthLogs.isNotEmpty()

    /**
     * Get the most concerning health indicators
     */
    fun getMostConcerningIndicators(): List<HealthConcern> {
        val concerns = mutableListOf<HealthConcern>()

        // Check sleep
        sleepStats?.let { stats ->
            if (stats.averageHours < 5f && stats.daysLogged >= 3) {
                concerns.add(HealthConcern(
                    type = ConcernType.SLEEP_CRISIS,
                    severity = ConcernSeverity.HIGH,
                    message = "Only ${String.format("%.1f", stats.averageHours)} hours sleep average over ${stats.daysLogged} nights"
                ))
            } else if (stats.averageHours < 6f) {
                concerns.add(HealthConcern(
                    type = ConcernType.SLEEP_DEFICIT,
                    severity = ConcernSeverity.MEDIUM,
                    message = "Sleep below recommended (${String.format("%.1f", stats.averageHours)}h avg)"
                ))
            }
        }

        // Check symptom intensity
        symptomStats.forEach { (type, stats) ->
            if (stats.avgIntensity >= 7 && stats.occurrenceCount >= 3) {
                concerns.add(HealthConcern(
                    type = ConcernType.SEVERE_SYMPTOMS,
                    severity = ConcernSeverity.HIGH,
                    message = "${type.displayName}: ${stats.occurrenceCount} episodes at avg intensity ${String.format("%.1f", stats.avgIntensity)}/10"
                ))
            }
        }

        // Check mood
        moodStats?.let { stats ->
            if (stats.dominantMood in listOf(MoodType.SAD, MoodType.ANXIOUS) && stats.totalEntries >= 3) {
                concerns.add(HealthConcern(
                    type = ConcernType.MOOD_PATTERN,
                    severity = ConcernSeverity.MEDIUM,
                    message = "Dominant mood: ${stats.dominantMood?.displayName} over ${stats.totalEntries} entries"
                ))
            }
        }

        return concerns.sortedByDescending { it.severity.ordinal }
    }
}

/**
 * Health concern for proactive alerts
 */
data class HealthConcern(
    val type: ConcernType,
    val severity: ConcernSeverity,
    val message: String
)

enum class ConcernType {
    SLEEP_CRISIS,
    SLEEP_DEFICIT,
    SEVERE_SYMPTOMS,
    MOOD_PATTERN,
    NUTRITION_GAP,
    INACTIVITY
}

enum class ConcernSeverity {
    LOW,
    MEDIUM,
    HIGH
}
