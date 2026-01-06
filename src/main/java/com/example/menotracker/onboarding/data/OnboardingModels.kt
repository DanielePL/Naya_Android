package com.example.menotracker.onboarding.data

import com.example.menotracker.data.models.DietaryPreference
import com.example.menotracker.data.models.FoodAllergy

/**
 * Onboarding Data Models for Naya
 * Handles user persona calculation and feature visibility
 */

// Primary goal selection - can select multiple
// Simplified for mass appeal: Aesthetics, Strength, Sport Performance
enum class PrimaryGoal(val displayName: String, val description: String, val iconName: String) {
    BODY_COMP("Aesthetics", "Look better, feel better", "person"),
    STRENGTH("Strength", "Get stronger, lift more", "fitness_center"),
    PERFORMANCE("Sport Performance", "Compete at your best", "emoji_events"),
    COMPLETE("All of the Above", "The complete package", "bolt")
}

// ═══════════════════════════════════════════════════════════════════════════
// MENOPAUSE-SPECIFIC ENUMS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Menopause stage selection for personalized recommendations
 */
enum class OnboardingMenopauseStage(
    val displayName: String,
    val description: String,
    val iconName: String
) {
    PREMENOPAUSE(
        "Premenopause",
        "Regular cycles, but noticing first changes",
        "calendar_today"
    ),
    EARLY_PERIMENOPAUSE(
        "Early Perimenopause",
        "Cycles becoming irregular, first symptoms",
        "schedule"
    ),
    LATE_PERIMENOPAUSE(
        "Late Perimenopause",
        "60+ days without period, stronger symptoms",
        "hourglass_bottom"
    ),
    MENOPAUSE(
        "Menopause",
        "12 months without period",
        "check_circle"
    ),
    POSTMENOPAUSE(
        "Postmenopause",
        "More than 12 months after last period",
        "verified"
    ),
    UNSURE(
        "Unsure",
        "I'm not quite sure",
        "help"
    )
}

/**
 * Menopause-specific wellness goals
 */
enum class MenopauseGoal(
    val displayName: String,
    val description: String,
    val iconName: String
) {
    SYMPTOM_RELIEF(
        "Symptom Relief",
        "Hot flashes, sleep issues, mood",
        "healing"
    ),
    BONE_HEALTH(
        "Bone Health",
        "Osteoporosis prevention through nutrition & exercise",
        "accessibility_new"
    ),
    WEIGHT_MANAGEMENT(
        "Weight Management",
        "Adapt metabolism, maintain weight",
        "monitor_weight"
    ),
    ENERGY_VITALITY(
        "Energy & Vitality",
        "Fight fatigue, stay active",
        "bolt"
    ),
    SLEEP_QUALITY(
        "Sleep Quality",
        "Fall asleep and stay asleep better",
        "bedtime"
    ),
    STRESS_MOOD(
        "Stress & Mood",
        "Emotional balance, reduce anxiety",
        "self_improvement"
    ),
    HORMONE_BALANCE(
        "Hormone Balance",
        "Natural ways to support hormones",
        "balance"
    )
}

/**
 * Primary symptoms the user experiences
 */
enum class OnboardingSymptom(
    val displayName: String,
    val description: String,
    val iconName: String
) {
    HOT_FLASHES(
        "Hot Flashes",
        "Sudden waves of heat",
        "whatshot"
    ),
    NIGHT_SWEATS(
        "Night Sweats",
        "Sweating during sleep",
        "nights_stay"
    ),
    SLEEP_ISSUES(
        "Sleep Problems",
        "Difficulty falling or staying asleep",
        "bedtime"
    ),
    MOOD_SWINGS(
        "Mood Swings",
        "Emotional ups and downs",
        "mood"
    ),
    FATIGUE(
        "Fatigue",
        "Exhaustion and lack of energy",
        "battery_low"
    ),
    BRAIN_FOG(
        "Brain Fog",
        "Concentration issues, forgetfulness",
        "psychology"
    ),
    WEIGHT_GAIN(
        "Weight Gain",
        "Especially around the belly",
        "monitor_weight"
    ),
    JOINT_PAIN(
        "Joint Pain",
        "Stiffness and aches",
        "accessibility"
    ),
    ANXIETY(
        "Anxiety",
        "Nervousness and restlessness",
        "sentiment_stressed"
    ),
    LOW_LIBIDO(
        "Libido Changes",
        "Changes in sexual desire",
        "favorite_border"
    ),
    NONE(
        "None/Other",
        "None of the above symptoms",
        "check"
    )
}

// Sport selection with strength/nutrition relevance scores
enum class Sport(
    val displayName: String,
    val iconName: String,
    val strengthScore: Float,
    val nutritionScore: Float
) {
    WEIGHTLIFTING("Weightlifting", "fitness_center", 1.0f, 0.5f),
    POWERLIFTING("Powerlifting", "fitness_center", 1.0f, 0.4f),
    STRONGMAN("Strongman", "fitness_center", 0.8f, 0.6f),
    CROSSFIT("CrossFit", "bolt", 0.7f, 0.7f),
    HYROX("Hyrox", "directions_run", 0.5f, 0.8f),
    BODYBUILDING("Bodybuilding", "fitness_center", 0.3f, 1.0f),
    ENDURANCE("Endurance", "directions_run", 0.2f, 0.7f),
    TEAM_SPORTS("Team Sports", "sports_soccer", 0.5f, 0.5f),
    GENERAL("General Fitness", "self_improvement", 0.4f, 0.5f)
}

// Experience level
enum class ExperienceLevel(val displayName: String, val description: String) {
    BEGINNER("Beginner", "Less than 1 year"),
    INTERMEDIATE("Intermediate", "1-3 years experience"),
    EXPERIENCED("Experienced", "3+ years, know my numbers"),
    ELITE("Elite / Competitive", "Competitions, coaching")
}

// Gender for strength standards calculation
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female")
}

/**
 * Sport-specific PR (Personal Record) types
 * Each lift has a display name and the sports it's relevant to
 */
enum class LiftType(
    val displayName: String,
    val shortName: String,
    val relevantSports: List<Sport>
) {
    // Powerlifting (SBD)
    SQUAT("Back Squat", "Squat", listOf(Sport.POWERLIFTING, Sport.STRONGMAN, Sport.CROSSFIT, Sport.BODYBUILDING, Sport.GENERAL)),
    BENCH_PRESS("Bench Press", "Bench", listOf(Sport.POWERLIFTING, Sport.BODYBUILDING, Sport.GENERAL)),
    DEADLIFT("Deadlift", "Deadlift", listOf(Sport.POWERLIFTING, Sport.STRONGMAN, Sport.CROSSFIT, Sport.BODYBUILDING, Sport.GENERAL)),

    // Weightlifting
    SNATCH("Snatch", "Snatch", listOf(Sport.WEIGHTLIFTING, Sport.CROSSFIT)),
    CLEAN_AND_JERK("Clean & Jerk", "C&J", listOf(Sport.WEIGHTLIFTING, Sport.CROSSFIT)),

    // Strongman specific
    LOG_LIFT("Log Lift", "Log", listOf(Sport.STRONGMAN)),
    AXLE_CLEAN_PRESS("Axle Clean & Press", "Axle", listOf(Sport.STRONGMAN)),
    YOKE_CARRY("Yoke Carry", "Yoke", listOf(Sport.STRONGMAN)),
    FARMERS_WALK("Farmer's Walk", "Farmers", listOf(Sport.STRONGMAN)),
    ATLAS_STONES("Atlas Stones", "Stones", listOf(Sport.STRONGMAN)),

    // CrossFit benchmarks (optional)
    FRONT_SQUAT("Front Squat", "FS", listOf(Sport.CROSSFIT, Sport.WEIGHTLIFTING)),
    OVERHEAD_SQUAT("Overhead Squat", "OHS", listOf(Sport.CROSSFIT, Sport.WEIGHTLIFTING)),
    THRUSTER("Thruster", "Thruster", listOf(Sport.CROSSFIT)),

    // General/Bodybuilding
    OVERHEAD_PRESS("Overhead Press", "OHP", listOf(Sport.BODYBUILDING, Sport.GENERAL, Sport.STRONGMAN));

    companion object {
        /**
         * Get all lift types relevant for the given sports
         */
        fun getLiftsForSports(sports: List<Sport>): List<LiftType> {
            return entries.filter { lift ->
                lift.relevantSports.any { it in sports }
            }
        }

        /**
         * Get primary lifts for a sport (the most important ones)
         */
        fun getPrimaryLiftsForSport(sport: Sport): List<LiftType> {
            return when (sport) {
                Sport.POWERLIFTING -> listOf(SQUAT, BENCH_PRESS, DEADLIFT)
                Sport.WEIGHTLIFTING -> listOf(SNATCH, CLEAN_AND_JERK)
                Sport.CROSSFIT -> listOf(SNATCH, CLEAN_AND_JERK, SQUAT, DEADLIFT)
                Sport.STRONGMAN -> listOf(DEADLIFT, LOG_LIFT, AXLE_CLEAN_PRESS, YOKE_CARRY, SQUAT)
                Sport.BODYBUILDING -> listOf(SQUAT, BENCH_PRESS, DEADLIFT, OVERHEAD_PRESS)
                Sport.GENERAL -> listOf(SQUAT, BENCH_PRESS, DEADLIFT)
                else -> emptyList() // Endurance, Hyrox, Team Sports - no strength PRs
            }
        }
    }
}

/**
 * A single PR entry with current and goal values
 */
data class PREntry(
    val liftType: LiftType,
    val currentKg: Float? = null,
    val goalKg: Float? = null
) {
    val hasCurrentPR: Boolean get() = currentKg != null && currentKg > 0
    val hasGoalPR: Boolean get() = goalKg != null && goalKg > 0
    val kgToGain: Float? get() = if (currentKg != null && goalKg != null) goalKg - currentKg else null
}

/**
 * Current PRs data - sport-specific with backward compatibility
 */
data class CurrentPRs(
    // Legacy powerlifting fields (for backward compatibility)
    val squatKg: Float? = null,
    val benchKg: Float? = null,
    val deadliftKg: Float? = null,
    val bodyweightKg: Float? = null,

    // New sport-specific PRs map
    val prMap: Map<LiftType, Float> = emptyMap()
) {
    // Powerlifting total (SBD)
    val powerliftingTotalKg: Float?
        get() {
            val squat = prMap[LiftType.SQUAT] ?: squatKg
            val bench = prMap[LiftType.BENCH_PRESS] ?: benchKg
            val deadlift = prMap[LiftType.DEADLIFT] ?: deadliftKg
            return if (squat != null && bench != null && deadlift != null) {
                squat + bench + deadlift
            } else null
        }

    // Weightlifting total (Snatch + C&J)
    val weightliftingTotalKg: Float?
        get() {
            val snatch = prMap[LiftType.SNATCH]
            val cj = prMap[LiftType.CLEAN_AND_JERK]
            return if (snatch != null && cj != null) {
                snatch + cj
            } else null
        }

    // Legacy compatibility - totalKg returns powerlifting total
    val totalKg: Float? get() = powerliftingTotalKg

    /**
     * Get PR for a specific lift
     */
    fun getPR(liftType: LiftType): Float? {
        return prMap[liftType] ?: when (liftType) {
            LiftType.SQUAT -> squatKg
            LiftType.BENCH_PRESS -> benchKg
            LiftType.DEADLIFT -> deadliftKg
            else -> null
        }
    }

    /**
     * Create a new CurrentPRs with an updated PR value
     */
    fun withPR(liftType: LiftType, value: Float?): CurrentPRs {
        val newMap = prMap.toMutableMap()
        if (value != null && value > 0) {
            newMap[liftType] = value
        } else {
            newMap.remove(liftType)
        }

        // Also update legacy fields for backward compatibility
        return when (liftType) {
            LiftType.SQUAT -> copy(prMap = newMap, squatKg = value)
            LiftType.BENCH_PRESS -> copy(prMap = newMap, benchKg = value)
            LiftType.DEADLIFT -> copy(prMap = newMap, deadliftKg = value)
            else -> copy(prMap = newMap)
        }
    }

    /**
     * Check if any PRs are entered
     */
    fun hasAnyPRs(): Boolean {
        return prMap.values.any { it > 0 } ||
               squatKg != null || benchKg != null || deadliftKg != null
    }

    /**
     * Get count of entered PRs
     */
    fun prCount(): Int {
        val legacyCount = listOfNotNull(squatKg, benchKg, deadliftKg)
            .filter { it > 0 }
            .count()
        val mapCount = prMap.values.count { it > 0 }
        // Avoid double counting
        return maxOf(legacyCount, mapCount)
    }
}

/**
 * Goal PRs data - sport-specific with backward compatibility
 */
data class GoalPRs(
    // Legacy powerlifting fields
    val squatKg: Float? = null,
    val benchKg: Float? = null,
    val deadliftKg: Float? = null,

    // New sport-specific goals map
    val goalMap: Map<LiftType, Float> = emptyMap()
) {
    val powerliftingTotalKg: Float?
        get() {
            val squat = goalMap[LiftType.SQUAT] ?: squatKg
            val bench = goalMap[LiftType.BENCH_PRESS] ?: benchKg
            val deadlift = goalMap[LiftType.DEADLIFT] ?: deadliftKg
            return if (squat != null && bench != null && deadlift != null) {
                squat + bench + deadlift
            } else null
        }

    val weightliftingTotalKg: Float?
        get() {
            val snatch = goalMap[LiftType.SNATCH]
            val cj = goalMap[LiftType.CLEAN_AND_JERK]
            return if (snatch != null && cj != null) {
                snatch + cj
            } else null
        }

    // Legacy compatibility
    val totalKg: Float? get() = powerliftingTotalKg

    fun getGoal(liftType: LiftType): Float? {
        return goalMap[liftType] ?: when (liftType) {
            LiftType.SQUAT -> squatKg
            LiftType.BENCH_PRESS -> benchKg
            LiftType.DEADLIFT -> deadliftKg
            else -> null
        }
    }

    fun withGoal(liftType: LiftType, value: Float?): GoalPRs {
        val newMap = goalMap.toMutableMap()
        if (value != null && value > 0) {
            newMap[liftType] = value
        } else {
            newMap.remove(liftType)
        }

        return when (liftType) {
            LiftType.SQUAT -> copy(goalMap = newMap, squatKg = value)
            LiftType.BENCH_PRESS -> copy(goalMap = newMap, benchKg = value)
            LiftType.DEADLIFT -> copy(goalMap = newMap, deadliftKg = value)
            else -> copy(goalMap = newMap)
        }
    }

    fun hasAnyGoals(): Boolean {
        return goalMap.values.any { it > 0 } ||
               squatKg != null || benchKg != null || deadliftKg != null
    }
}

// Training commitment data
data class TrainingCommitment(
    val sessionsPerWeek: Int = 4,
    val effortLevel: Int = 7 // 1-10 scale
)

// Coach situation
enum class CoachSituation(val displayName: String, val description: String) {
    HAS_COACH("Yes, I have a coach", "I want to connect them"),
    USE_AI_COACH("No, use AI Coach", "Naya AI coaches me"),
    SELF_COACHED("No, self-coached", "No coaching needed")
}

// Feature interests (priming question)
enum class FeatureInterest(val displayName: String, val description: String) {
    NUTRITION("Log meals & hit macros", "Nutrition tracking"),
    AI_COACH("Get training guidance", "AI-powered coaching"),
    TEMPLATES("Follow structured programs", "Templates & library")
}

// Persona type (calculated)
enum class PersonaType {
    STRENGTH_FOCUSED,
    PERFORMANCE_FOCUSED,
    NUTRITION_FOCUSED,
    COMPLETE_ATHLETE
}

// Coach mode for UI
enum class CoachMode {
    PHYSICAL,
    AI,
    NONE
}

// Feature video data for promo screen
data class FeatureVideo(
    val id: String,
    val title: String,
    val description: String,
    val iconResName: String = "ic_fitness" // Default icon
)

// Combo video for single-video promo screen
data class ComboVideo(
    val id: String,
    val title: String,
    val subtitle: String,
    val features: List<FeatureInterest>,
    val videoUrl: String = "" // Will be filled when videos are ready
)

// Onboarding answers collected during flow
data class OnboardingAnswers(
    // ═══════════════════════════════════════════════════════════════
    // MENOPAUSE-SPECIFIC FIELDS (new for Menotracker)
    // ═══════════════════════════════════════════════════════════════
    val menopauseStage: OnboardingMenopauseStage? = null,
    val primarySymptoms: List<OnboardingSymptom> = emptyList(),
    val menopauseGoals: List<MenopauseGoal> = emptyList(),
    // ═══════════════════════════════════════════════════════════════
    // LEGACY FIELDS (kept for backwards compatibility)
    // ═══════════════════════════════════════════════════════════════
    val primaryGoals: List<PrimaryGoal> = emptyList(),
    val sports: List<Sport> = emptyList(),
    val experienceLevel: ExperienceLevel? = null,
    val featureInterests: List<FeatureInterest> = emptyList(),
    val coachSituation: CoachSituation? = null,
    // New goal-setting fields
    val gender: Gender? = null,
    val age: Int? = null,                    // User's age for McCulloch coefficient
    val trainingYears: Int? = null,          // Years of training experience
    val currentPRs: CurrentPRs = CurrentPRs(),
    val goalPRs: GoalPRs = GoalPRs(),
    val trainingCommitment: TrainingCommitment = TrainingCommitment(),
    // Unified Sport selection (new 135+ sports system) - up to 3 sports
    val unifiedSport: UnifiedSport? = null,
    val secondaryUnifiedSport: UnifiedSport? = null,
    val tertiaryUnifiedSport: UnifiedSport? = null,
    // Universal Benchmarks (replaces sport-specific PRs for non-strength sports)
    val currentBenchmarks: Map<String, String> = emptyMap(),  // benchmark_id -> value
    val goalBenchmarks: Map<String, String> = emptyMap(),     // benchmark_id -> goal value
    // Dietary preferences for personalized nutrition (can select multiple, e.g. Vegan + Halal)
    val dietaryPreferences: List<DietaryPreference> = emptyList(),
    val foodAllergies: List<FoodAllergy> = emptyList(),
    val foodDislikes: List<String> = emptyList(),             // Free-form food dislikes
    val customAllergyNote: String? = null,                    // For FoodAllergy.OTHER
    // Registration info (collected after paywall)
    val userName: String? = null
)

// Registration state for the registration screen
data class RegistrationState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val needsEmailConfirmation: Boolean = false,
    val error: String? = null
)

// Calculated user persona
data class UserPersona(
    val type: PersonaType,
    val nutritionEnabled: Boolean,
    val nutritionScore: Float,
    val showAdvancedFeatures: Boolean,
    val coachMode: CoachMode,
    val featureVideos: List<FeatureVideo>,
    val paywallBenefits: List<String>,
    val comboVideo: ComboVideo? = null,
    val selectedFeatures: List<FeatureInterest> = emptyList()
)

// Onboarding step enum
enum class OnboardingStep {
    WELCOME,
    // ═══════════════════════════════════════════════════════════════
    // MENOPAUSE-SPECIFIC STEPS (new flow for Menotracker)
    // ═══════════════════════════════════════════════════════════════
    MENOPAUSE_STAGE,       // Welches Menopause-Stadium?
    PRIMARY_SYMPTOMS,      // Welche Hauptsymptome?
    MENOPAUSE_GOALS,       // Welche Ziele? (Symptome lindern, Knochen, etc.)
    // ═══════════════════════════════════════════════════════════════
    // LEGACY STEPS (kept for potential future use)
    // ═══════════════════════════════════════════════════════════════
    GOAL_SELECTION,
    SPORT_SELECTION,
    GENDER_SELECTION,      // Gender for strength standards
    AGE_EXPERIENCE,        // Age + training years for realistic timelines
    CURRENT_PRS,           // Current PRs input (legacy - for backward compat)
    SPORT_BENCHMARKS,      // NEW: Universal sport-specific benchmarks
    GOAL_PRS,              // Goal PRs input (legacy - for backward compat)
    GOAL_BENCHMARKS,       // NEW: Universal goal benchmarks
    TRAINING_COMMITMENT,   // Sessions/week + effort
    PROMISE_COMMITMENT,    // Timeline + promise to self
    EXPERIENCE_LEVEL,
    DIETARY_PREFERENCES,   // NEW: Dietary preference, allergies, dislikes
    FEATURE_INTEREST,      // Priming question: "What do you want to improve?"
    COACH_SITUATION,
    FEATURE_PROMO,
    PAYWALL,
    REGISTRATION,          // NEW: Name, Email, Password after paywall
    NOTIFICATIONS,
    COMPLETE
}

/**
 * Timeline Calculator - estimates time to reach PR goals
 * Based on training frequency, effort, age, training years, and current level
 *
 * Research sources:
 * - McCulloch Age Coefficients (USA Powerlifting)
 * - Long-Term Strength Adaptation: 15-Year Analysis (PMC7448836)
 * - Stronger by Science progression models
 */
object TimelineCalculator {

    /**
     * McCulloch Age Coefficients for masters lifters (40+)
     * These represent how much harder it is to progress compared to age 40
     * Source: USA Powerlifting / IPF Masters Coefficients
     */
    private fun getMcCullochCoefficient(age: Int): Float {
        return when {
            age < 40 -> 1.0f
            age == 40 -> 1.000f
            age == 41 -> 1.010f
            age == 42 -> 1.020f
            age == 43 -> 1.031f
            age == 44 -> 1.043f
            age == 45 -> 1.055f
            age == 46 -> 1.068f
            age == 47 -> 1.082f
            age == 48 -> 1.097f
            age == 49 -> 1.113f
            age == 50 -> 1.130f
            age == 51 -> 1.147f
            age == 52 -> 1.165f
            age == 53 -> 1.184f
            age == 54 -> 1.204f
            age == 55 -> 1.225f
            age == 56 -> 1.246f
            age == 57 -> 1.268f
            age == 58 -> 1.291f
            age == 59 -> 1.315f
            age == 60 -> 1.340f
            age == 61 -> 1.366f
            age == 62 -> 1.393f
            age == 63 -> 1.421f
            age == 64 -> 1.450f
            age == 65 -> 1.480f
            age == 66 -> 1.511f
            age == 67 -> 1.543f
            age == 68 -> 1.576f
            age == 69 -> 1.610f
            age >= 70 -> 1.645f + ((age - 70) * 0.04f) // Extrapolate for 70+
            else -> 1.0f
        }
    }

    /**
     * Training years progression multiplier
     * Based on research: ~10-12% gains in year 1, down to 0.27-0.32% by year 10
     * Source: PMC7448836, Stronger by Science
     *
     * This represents the fraction of "novice progression" still available
     */
    private fun getTrainingYearsMultiplier(years: Int): Float {
        return when {
            years <= 0 -> 1.0f      // True novice - full progression
            years == 1 -> 0.50f     // 50% of novice gains
            years == 2 -> 0.30f     // 30%
            years == 3 -> 0.20f     // 20%
            years == 4 -> 0.12f     // 12%
            years == 5 -> 0.08f     // 8%
            years in 6..10 -> 0.05f // 5% - approaching plateau
            years in 11..15 -> 0.025f // 2.5%
            years in 16..20 -> 0.015f // 1.5%
            years in 21..25 -> 0.010f // 1%
            years > 25 -> 0.005f    // 0.5% - maintenance territory
            else -> 0.05f
        }
    }

    /**
     * Calculate estimated weeks to reach goal PRs
     * Returns a range (min weeks, max weeks) and isVeteran flag
     */
    fun calculateTimeline(
        currentPRs: CurrentPRs,
        goalPRs: GoalPRs,
        commitment: TrainingCommitment,
        gender: Gender,
        experienceLevel: ExperienceLevel?,
        age: Int? = null,
        trainingYears: Int? = null
    ): TimelineResult {
        val currentTotal = currentPRs.totalKg ?: return TimelineResult(12, 24, false, null)
        val goalTotal = goalPRs.totalKg ?: return TimelineResult(12, 24, false, null)

        if (goalTotal <= currentTotal) return TimelineResult(0, 0, false, null)

        val totalToGain = goalTotal - currentTotal
        val percentageGain = (totalToGain / currentTotal) * 100

        // Base weekly progress for a novice (kg per week for total)
        val noviceWeeklyProgress = when (gender) {
            Gender.MALE -> 2.5f    // ~130kg/year for novice male
            Gender.FEMALE -> 1.75f // ~90kg/year for novice female
        }

        // Training years factor (biggest impact!)
        val trainingYearsMultiplier = trainingYears?.let { getTrainingYearsMultiplier(it) } ?: when (experienceLevel) {
            ExperienceLevel.BEGINNER -> 1.0f
            ExperienceLevel.INTERMEDIATE -> 0.30f
            ExperienceLevel.EXPERIENCED -> 0.08f
            ExperienceLevel.ELITE -> 0.03f
            null -> 0.30f
        }

        // Age factor (smaller impact, but real)
        val ageMultiplier = age?.let { 1.0f / getMcCullochCoefficient(it) } ?: 1.0f

        // Training frequency multiplier (optimal is 4-5x/week)
        val frequencyMultiplier = when (commitment.sessionsPerWeek) {
            1 -> 0.4f
            2 -> 0.6f
            3 -> 0.8f
            4 -> 1.0f
            5 -> 1.1f
            6 -> 1.05f // Diminishing returns + recovery issues
            else -> 1.0f
        }

        // Effort multiplier (1-10 scale)
        val effortMultiplier = 0.7f + (commitment.effortLevel * 0.04f) // 0.74 to 1.1

        // Calculate adjusted weekly progress
        val adjustedWeeklyProgress = noviceWeeklyProgress *
            trainingYearsMultiplier *
            ageMultiplier *
            frequencyMultiplier *
            effortMultiplier

        // Minimum progress floor (even veterans can gain something with perfect conditions)
        val weeklyProgress = adjustedWeeklyProgress.coerceAtLeast(0.02f)

        // Calculate weeks needed
        val baseWeeks = (totalToGain / weeklyProgress).toInt()

        // Add variance for realistic range
        val minWeeks = (baseWeeks * 0.85f).toInt().coerceAtLeast(4)
        val maxWeeks = (baseWeeks * 1.25f).toInt().coerceAtLeast(minWeeks + 4)

        // Determine if this is a veteran lifter (for messaging)
        val isVeteran = (trainingYears ?: 0) >= 10 || experienceLevel == ExperienceLevel.ELITE

        // Calculate realistic goal suggestion for veterans
        val realisticGoalSuggestion = if (isVeteran && baseWeeks > 260) { // More than 5 years
            // Suggest a more achievable goal
            val achievableIn52Weeks = currentTotal + (weeklyProgress * 52)
            achievableIn52Weeks
        } else null

        return TimelineResult(
            minWeeks = minWeeks,
            maxWeeks = maxWeeks,
            isVeteran = isVeteran,
            realisticGoalSuggestion = realisticGoalSuggestion
        )
    }

    // Backward compatible overload
    fun calculateTimeline(
        currentPRs: CurrentPRs,
        goalPRs: GoalPRs,
        commitment: TrainingCommitment,
        gender: Gender,
        experienceLevel: ExperienceLevel?
    ): Pair<Int, Int> {
        val result = calculateTimeline(currentPRs, goalPRs, commitment, gender, experienceLevel, null, null)
        return Pair(result.minWeeks, result.maxWeeks)
    }

    /**
     * Format timeline for display
     */
    fun formatTimeline(weeks: Pair<Int, Int>): String {
        return formatTimeline(weeks.first, weeks.second)
    }

    fun formatTimeline(minWeeks: Int, maxWeeks: Int): String {
        return when {
            minWeeks == 0 && maxWeeks == 0 -> "You're already there!"
            minWeeks < 8 -> "${minWeeks}-${maxWeeks} weeks"
            minWeeks < 52 -> {
                val minMonths = (minWeeks / 4.33f).toInt()
                val maxMonths = (maxWeeks / 4.33f).toInt()
                "$minMonths-$maxMonths months"
            }
            minWeeks < 260 -> { // Less than 5 years
                val minYears = minWeeks / 52f
                val maxYears = maxWeeks / 52f
                "%.1f-%.1f years".format(minYears, maxYears)
            }
            else -> {
                val minYears = (minWeeks / 52f).toInt()
                val maxYears = (maxWeeks / 52f).toInt()
                "$minYears-$maxYears+ years"
            }
        }
    }

    /**
     * Get motivational message based on timeline and experience
     */
    fun getMotivationalMessage(result: TimelineResult): String {
        val avgWeeks = (result.minWeeks + result.maxWeeks) / 2

        if (result.isVeteran) {
            return when {
                avgWeeks <= 52 -> "Your experience is your advantage. Execute and deliver."
                avgWeeks <= 156 -> "A veteran's journey. Small gains, big patience."
                result.realisticGoalSuggestion != null ->
                    "Consider ${result.realisticGoalSuggestion?.toInt()}kg as a 1-year target. Small wins compound."
                else -> "At your level, maintaining strength IS progress. Any gain is a victory."
            }
        }

        return when {
            avgWeeks <= 0 -> "Time to set a bigger goal!"
            avgWeeks <= 12 -> "A focused training block will get you there."
            avgWeeks <= 26 -> "Consistent effort over the next few months will make it happen."
            avgWeeks <= 52 -> "This is a serious goal. Stay committed and you'll crush it."
            avgWeeks <= 104 -> "A long-term project. Trust the process, track the journey."
            else -> "This is a multi-year transformation. Every rep counts."
        }
    }

    // Backward compatible overload
    fun getMotivationalMessage(weeks: Pair<Int, Int>): String {
        return getMotivationalMessage(TimelineResult(weeks.first, weeks.second, false, null))
    }
}

/**
 * Result from timeline calculation with additional context
 */
data class TimelineResult(
    val minWeeks: Int,
    val maxWeeks: Int,
    val isVeteran: Boolean,
    val realisticGoalSuggestion: Float? // Suggested achievable goal in 1 year (for veterans)
) {
    fun toPair(): Pair<Int, Int> = Pair(minWeeks, maxWeeks)
}

// Full onboarding state
data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val persona: UserPersona? = null,
    val isComplete: Boolean = false,
    val skippedViaDevMode: Boolean = false
)

/**
 * Persona Engine - calculates user persona from answers
 */
object PersonaEngine {

    fun calculatePersona(answers: OnboardingAnswers): UserPersona {
        // Base scores from goals (average if multiple selected)
        val goals = answers.primaryGoals.ifEmpty { listOf(PrimaryGoal.COMPLETE) }

        var strengthScore = goals.map { goal ->
            when (goal) {
                PrimaryGoal.STRENGTH -> 0.9f
                PrimaryGoal.PERFORMANCE -> 0.95f
                PrimaryGoal.BODY_COMP -> 0.3f
                PrimaryGoal.COMPLETE -> 0.8f
            }
        }.average().toFloat()

        var nutritionScore = goals.map { goal ->
            when (goal) {
                PrimaryGoal.STRENGTH -> 0.3f
                PrimaryGoal.PERFORMANCE -> 0.4f
                PrimaryGoal.BODY_COMP -> 0.95f
                PrimaryGoal.COMPLETE -> 0.8f
            }
        }.average().toFloat()

        // Adjust by sports average
        if (answers.sports.isNotEmpty()) {
            val sportStrength = answers.sports.map { it.strengthScore }.average().toFloat()
            val sportNutrition = answers.sports.map { it.nutritionScore }.average().toFloat()
            strengthScore = (strengthScore * 0.7f) + (sportStrength * 0.3f)
            nutritionScore = (nutritionScore * 0.7f) + (sportNutrition * 0.3f)
        }

        val type = when {
            strengthScore > 0.6f && nutritionScore > 0.6f -> PersonaType.COMPLETE_ATHLETE
            PrimaryGoal.PERFORMANCE in goals -> PersonaType.PERFORMANCE_FOCUSED
            strengthScore > 0.7f -> PersonaType.STRENGTH_FOCUSED
            nutritionScore > 0.7f -> PersonaType.NUTRITION_FOCUSED
            else -> PersonaType.COMPLETE_ATHLETE
        }

        val showAdvanced = answers.experienceLevel in listOf(
            ExperienceLevel.EXPERIENCED,
            ExperienceLevel.ELITE
        )

        val coachMode = when (answers.coachSituation) {
            CoachSituation.HAS_COACH -> CoachMode.PHYSICAL
            CoachSituation.USE_AI_COACH -> CoachMode.AI
            CoachSituation.SELF_COACHED -> CoachMode.NONE
            null -> CoachMode.AI
        }

        // Use feature interests directly from priming question
        val selectedFeatures = answers.featureInterests.ifEmpty {
            // Fallback to calculated interests if priming not answered
            listOfNotNull(
                if (nutritionScore > 0.5f) FeatureInterest.NUTRITION else null,
                if (coachMode == CoachMode.AI) FeatureInterest.AI_COACH else null,
                FeatureInterest.TEMPLATES // Everyone gets templates
            )
        }

        return UserPersona(
            type = type,
            nutritionEnabled = FeatureInterest.NUTRITION in selectedFeatures,
            nutritionScore = nutritionScore,
            showAdvancedFeatures = showAdvanced,
            coachMode = coachMode,
            featureVideos = getFeatureVideos(type),
            paywallBenefits = getPaywallBenefits(selectedFeatures),
            comboVideo = getComboVideo(selectedFeatures),
            selectedFeatures = selectedFeatures
        )
    }

    /**
     * Get combo video based on selected feature interests
     */
    fun getComboVideo(features: List<FeatureInterest>): ComboVideo {
        val hasNutrition = FeatureInterest.NUTRITION in features
        val hasAiCoach = FeatureInterest.AI_COACH in features

        return when {
            hasNutrition && hasAiCoach -> ComboVideo(
                id = "combo_nutrition_coach",
                title = "Guided Nutrition",
                subtitle = "Nutrition + AI Coach",
                features = listOf(FeatureInterest.NUTRITION, FeatureInterest.AI_COACH)
            )
            hasNutrition -> ComboVideo(
                id = "combo_nutrition",
                title = "Nutrition Mastery",
                subtitle = "Hit your macros daily",
                features = listOf(FeatureInterest.NUTRITION)
            )
            hasAiCoach -> ComboVideo(
                id = "combo_coach",
                title = "AI-Powered Training",
                subtitle = "Your personal coach",
                features = listOf(FeatureInterest.AI_COACH)
            )
            else -> ComboVideo(
                id = "combo_templates",
                title = "Structured Programs",
                subtitle = "Follow proven templates",
                features = listOf(FeatureInterest.TEMPLATES)
            )
        }
    }

    fun getFeatureVideos(type: PersonaType): List<FeatureVideo> {
        return when (type) {
            PersonaType.STRENGTH_FOCUSED -> listOf(
                FeatureVideo("workout_promo", "Smart Workout Logging", "Track sets, reps and progress"),
                FeatureVideo("periodization_promo", "Smart Periodization", "Programs that adapt to you"),
                FeatureVideo("lab_promo", "Performance Lab", "Deep analytics and insights")
            )
            PersonaType.NUTRITION_FOCUSED -> listOf(
                FeatureVideo("meal_scan_promo", "AI Meal Scanner", "Snap a photo, get your macros"),
                FeatureVideo("nutrition_dashboard_promo", "Nutrition Dashboard", "Track protein, carbs, fats & more"),
                FeatureVideo("quick_add_promo", "Quick Add", "Log favorite meals in one tap")
            )
            PersonaType.COMPLETE_ATHLETE -> listOf(
                FeatureVideo("workout_promo", "Smart Workout Logging", "Track sets, reps and progress"),
                FeatureVideo("meal_scan_promo", "AI Meal Scanner", "Snap a photo, get your macros"),
                FeatureVideo("ai_coach_promo", "AI Coach", "Your personal training assistant")
            )
            PersonaType.PERFORMANCE_FOCUSED -> listOf(
                FeatureVideo("workout_promo", "Smart Workout Logging", "Track sets, reps and progress"),
                FeatureVideo("ai_coach_promo", "Naya Coach", "Your personal wellness companion"),
                FeatureVideo("load_lab_promo", "Load Management", "ACWR and recovery tracking")
            )
        }
    }

    fun getPaywallBenefits(features: List<FeatureInterest>): List<String> {
        val benefits = mutableListOf<String>()

        if (FeatureInterest.NUTRITION in features) {
            benefits.add("Unlimited Meal Scans")
            benefits.add("Full Macro Tracking")
        }
        if (FeatureInterest.AI_COACH in features) {
            benefits.add("Unlimited AI Coach")
        }
        if (FeatureInterest.TEMPLATES in features) {
            benefits.add("All Programs & Templates")
        }

        // Always add common benefits
        benefits.add("Full Lab Analytics")
        benefits.add("Priority Support")

        return benefits
    }

    // Overload for backward compatibility
    fun getPaywallBenefits(type: PersonaType): List<String> {
        return when (type) {
            PersonaType.STRENGTH_FOCUSED -> getPaywallBenefits(listOf(FeatureInterest.TEMPLATES))
            PersonaType.NUTRITION_FOCUSED -> getPaywallBenefits(listOf(FeatureInterest.NUTRITION, FeatureInterest.TEMPLATES))
            PersonaType.COMPLETE_ATHLETE -> getPaywallBenefits(listOf(FeatureInterest.NUTRITION, FeatureInterest.AI_COACH, FeatureInterest.TEMPLATES))
            PersonaType.PERFORMANCE_FOCUSED -> getPaywallBenefits(listOf(FeatureInterest.TEMPLATES))
        }
    }

    /**
     * Creates a default "complete athlete" persona for dev skip mode
     */
    fun createDefaultPersona(): UserPersona {
        return UserPersona(
            type = PersonaType.COMPLETE_ATHLETE,
            nutritionEnabled = true,
            nutritionScore = 0.8f,
            showAdvancedFeatures = true,
            coachMode = CoachMode.AI,
            featureVideos = getFeatureVideos(PersonaType.COMPLETE_ATHLETE),
            paywallBenefits = getPaywallBenefits(PersonaType.COMPLETE_ATHLETE)
        )
    }
}