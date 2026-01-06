package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val weight: Double? = null, // in kg (bodyweight)
    val height: Double? = null, // in cm
    val age: Int? = null,
    val gender: Gender? = null, // For TDEE calculation

    @SerialName("activity_level")
    val activityLevel: ActivityLevel? = null, // For TDEE calculation

    @SerialName("training_experience")
    val trainingExperience: Int? = null, // in years

    @SerialName("personal_records")
    val personalRecords: Map<String, PersonalRecord> = emptyMap(),

    @SerialName("medical_conditions")
    val medicalConditions: List<MedicalCondition> = emptyList(),

    val injuries: List<Injury> = emptyList(),
    val goals: List<String> = emptyList(),

    @SerialName("preferred_sports")
    val preferredSports: List<String> = emptyList(), // e.g., ["Powerlifting", "Weightlifting"]

    @SerialName("target_workout_duration")
    val targetWorkoutDuration: Int? = null, // Target workout duration in minutes (e.g., 60, 75, 90)

    @SerialName("profile_image_url")
    val profileImageUrl: String? = null, // URL to profile image in Supabase Storage

    @SerialName("has_coach")
    val hasCoach: Boolean = false, // True if user has a physical coach (stores meal photos in cloud)

    @SerialName("last_seen")
    val lastSeen: Long? = null, // Unix timestamp

    // ==================== Onboarding Data ====================

    @SerialName("goal_records")
    val goalRecords: Map<String, GoalRecord> = emptyMap(), // Target PRs from onboarding

    @SerialName("sessions_per_week")
    val sessionsPerWeek: Int? = null, // Training commitment: sessions per week

    @SerialName("effort_level")
    val effortLevel: Int? = null, // Training commitment: effort level 1-10

    @SerialName("experience_level")
    val experienceLevel: String? = null, // BEGINNER, INTERMEDIATE, EXPERIENCED, ELITE

    @SerialName("primary_goals")
    val primaryGoals: List<String> = emptyList(), // STRENGTH, PERFORMANCE, BODY_COMP, COMPLETE

    @SerialName("feature_interests")
    val featureInterests: List<String> = emptyList(), // VBT, NUTRITION, AI_COACH, TEMPLATES

    @SerialName("coach_situation")
    val coachSituation: String? = null, // HAS_COACH, USE_AI_COACH, SELF_COACHED

    @SerialName("onboarding_completed_at")
    val onboardingCompletedAt: Long? = null, // When onboarding was completed

    // ==================== Unified Sport System (135+ sports) ====================

    @SerialName("primary_sport")
    val primarySport: String? = null, // UnifiedSport.id - main sport selected during onboarding

    @SerialName("secondary_sport")
    val secondarySport: String? = null, // UnifiedSport.id - optional second sport

    @SerialName("tertiary_sport")
    val tertiarySport: String? = null, // UnifiedSport.id - optional third sport

    @SerialName("primary_sport_category")
    val primarySportCategory: String? = null, // SportCategory enum name (e.g., "STRENGTH", "BALL")

    @SerialName("training_focus")
    val trainingFocus: Map<String, Float>? = null, // 6 factors: kraft, schnelligkeit, ausdauer, beweglichkeit, geschicklichkeit, mindset

    // ==================== Dietary Preferences ====================

    @SerialName("dietary_preferences")
    val dietaryPreferences: List<String> = emptyList(), // Can select multiple: OMNIVORE, PESCATARIAN, VEGETARIAN, VEGAN, KETO, HALAL, KOSHER

    @SerialName("food_allergies")
    val foodAllergies: List<String> = emptyList(), // DAIRY, GLUTEN, NUTS, SOY, EGGS, SHELLFISH, OTHER

    @SerialName("food_dislikes")
    val foodDislikes: List<String> = emptyList(), // Free-form: "Mushrooms", "Liver", etc.

    @SerialName("custom_allergy_note")
    val customAllergyNote: String? = null, // For OTHER allergy specification

    // ==================== Menopause Wellness (NAYA) ====================

    @SerialName("menopause_stage")
    val menopauseStage: String? = null, // "perimenopause", "menopause", "postmenopause", "not_sure"

    @SerialName("primary_symptoms")
    val primarySymptoms: List<String> = emptyList(), // ["hot_flashes", "sleep_issues", "mood_changes", "fatigue", "weight_gain", "brain_fog"]

    @SerialName("wellness_goals")
    val wellnessGoals: List<String> = emptyList() // ["better_sleep", "manage_symptoms", "stay_active", "reduce_stress", "hormonal_balance"]
) {
    /**
     * Calculate TDEE (Total Daily Energy Expenditure) using Mifflin-St Jeor equation
     * Returns null if required fields are missing
     */
    fun calculateTDEE(): Int? {
        val w = weight ?: return null
        val h = height ?: return null
        val a = age ?: return null
        val g = gender ?: return null
        val act = activityLevel ?: return null

        // BMR using Mifflin-St Jeor equation
        val bmr = when (g) {
            Gender.MALE -> (10 * w) + (6.25 * h) - (5 * a) + 5
            Gender.FEMALE -> (10 * w) + (6.25 * h) - (5 * a) - 161
        }

        // TDEE = BMR × Activity Multiplier
        return (bmr * act.multiplier).toInt()
    }

    /**
     * Calculate suggested macros based on TDEE, goal type, and sport-specific training focus
     *
     * Sport-specific adjustments:
     * - High Kraft (Strength): More protein (2.2-2.5g/kg), moderate carbs
     * - High Ausdauer (Endurance): Higher carbs (50-60%), moderate protein
     * - High Schnelligkeit (Speed/Power): Balanced with emphasis on carbs for explosive energy
     * - Bodybuilding (high nutritionRelevance): Higher protein, precise macro timing
     */
    fun calculateSuggestedMacros(goalType: com.example.menotracker.data.models.GoalType): MacroSuggestion? {
        val tdee = calculateTDEE() ?: return null
        val bodyWeight = weight ?: 80.0

        // Get training focus from user profile (set during onboarding from SportDatabase)
        val kraft = trainingFocus?.get("kraft") ?: 0.5f
        val ausdauer = trainingFocus?.get("ausdauer") ?: 0.5f
        val schnelligkeit = trainingFocus?.get("schnelligkeit") ?: 0.5f

        // Adjust calories based on goal
        val targetCalories = when (goalType) {
            com.example.menotracker.data.models.GoalType.CUTTING -> (tdee * 0.8).toInt() // 20% deficit
            com.example.menotracker.data.models.GoalType.BULKING -> (tdee * 1.15).toInt() // 15% surplus
            com.example.menotracker.data.models.GoalType.MAINTENANCE -> tdee
            com.example.menotracker.data.models.GoalType.PERFORMANCE -> (tdee * 1.1).toInt() // 10% surplus
        }

        // ═══════════════════════════════════════════════════════════════
        // SPORT-SPECIFIC PROTEIN CALCULATION
        // ═══════════════════════════════════════════════════════════════
        // Base: 1.6g/kg (general athlete)
        // Strength sports: up to 2.4g/kg
        // Endurance sports: 1.4-1.8g/kg (less protein needed)
        val proteinMultiplier = when {
            kraft >= 0.8f -> 2.4  // Powerlifting, Bodybuilding, Strongman
            kraft >= 0.6f -> 2.2  // CrossFit, Wrestling, Rugby
            ausdauer >= 0.8f -> 1.6  // Marathon, Triathlon, Cycling
            ausdauer >= 0.6f -> 1.8  // Soccer, Basketball, Tennis
            else -> 2.0  // Default athletic
        }
        val proteinGrams = (bodyWeight * proteinMultiplier).toInt()
        val proteinCalories = proteinGrams * 4

        // ═══════════════════════════════════════════════════════════════
        // SPORT-SPECIFIC CARB/FAT RATIO
        // ═══════════════════════════════════════════════════════════════
        // Endurance athletes need more carbs (glycogen stores)
        // Strength athletes can have more fat (hormonal support)
        val fatPercentage = when {
            ausdauer >= 0.8f -> 0.20f  // Endurance: lower fat, more carbs
            ausdauer >= 0.6f -> 0.22f
            kraft >= 0.8f -> 0.28f  // Strength: higher fat for hormones
            kraft >= 0.6f -> 0.26f
            else -> 0.25f  // Default: 25%
        }

        val fatCalories = (targetCalories * fatPercentage).toInt()
        val fatGrams = fatCalories / 9

        // Carbs: remaining calories after protein and fat
        val carbCalories = targetCalories - proteinCalories - fatCalories
        val carbGrams = (carbCalories / 4).coerceAtLeast(50) // minimum 50g carbs

        return MacroSuggestion(
            calories = targetCalories,
            protein = proteinGrams,
            carbs = carbGrams,
            fat = fatGrams,
            sportOptimized = trainingFocus != null
        )
    }

    /**
     * Get the user's dietary profile for personalized nutrition tips
     */
    fun getDietaryProfile(): DietaryProfile {
        // Convert stored strings to DietaryPreference enums (can be multiple)
        val preferences = dietaryPreferences.mapNotNull { prefString ->
            DietaryPreference.entries.find { it.name == prefString.uppercase() }
        }.ifEmpty { listOf(DietaryPreference.OMNIVORE) }

        // Convert stored strings to FoodAllergy enums
        val allergies = foodAllergies.mapNotNull { allergyString ->
            FoodAllergy.entries.find { it.name == allergyString.uppercase() }
        }

        return DietaryProfile(
            preferences = preferences,
            allergies = allergies,
            dislikes = foodDislikes
        )
    }
}

data class MacroSuggestion(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val sportOptimized: Boolean = false // True if macros were adjusted based on user's sport/training focus
)

@Serializable
enum class Gender {
    @SerialName("male") MALE,
    @SerialName("female") FEMALE;

    val displayName: String
        get() = when (this) {
            MALE -> "Male"
            FEMALE -> "Female"
        }
}

@Serializable
enum class ActivityLevel {
    @SerialName("sedentary") SEDENTARY,
    @SerialName("light") LIGHT,
    @SerialName("moderate") MODERATE,
    @SerialName("active") ACTIVE,
    @SerialName("very_active") VERY_ACTIVE;

    val displayName: String
        get() = when (this) {
            SEDENTARY -> "Sedentary"
            LIGHT -> "Lightly Active"
            MODERATE -> "Moderately Active"
            ACTIVE -> "Active"
            VERY_ACTIVE -> "Very Active"
        }

    val description: String
        get() = when (this) {
            SEDENTARY -> "Little or no exercise"
            LIGHT -> "Light exercise 1-3 days/week"
            MODERATE -> "Moderate exercise 3-5 days/week"
            ACTIVE -> "Hard exercise 6-7 days/week"
            VERY_ACTIVE -> "Very hard exercise, physical job"
        }

    val multiplier: Double
        get() = when (this) {
            SEDENTARY -> 1.2
            LIGHT -> 1.375
            MODERATE -> 1.55
            ACTIVE -> 1.725
            VERY_ACTIVE -> 1.9
        }
}

@Serializable
data class PersonalRecord(
    @SerialName("exercise_name")
    val exerciseName: String,

    val value: Double,
    val unit: String, // "kg", "reps", "min:sec", "pts"

    @SerialName("achieved_at")
    val achievedAt: Long = System.currentTimeMillis(),

    val sport: String // "Olympic Weightlifting", "Powerlifting", etc.
)

@Serializable
data class MedicalCondition(
    val id: String,
    val name: String,
    val description: String,

    @SerialName("diagnosed_at")
    val diagnosedAt: Long,

    val restrictions: List<String> = emptyList(), // e.g., "No overhead pressing", "No deep squats"

    @SerialName("document_uri")
    val documentUri: String? = null
)

@Serializable
data class Injury(
    val id: String,

    @SerialName("body_part")
    val bodyPart: String, // "Shoulder", "Lower Back", "Knee", etc.

    val severity: InjurySeverity,
    val description: String,

    @SerialName("occurred_at")
    val occurredAt: Long,

    @SerialName("restricted_movements")
    val restrictedMovements: List<String> = emptyList(),

    @SerialName("recommended_alternatives")
    val recommendedAlternatives: List<String> = emptyList()
)

@Serializable
enum class InjurySeverity {
    MILD,      // Can train with modifications
    MODERATE,  // Significant restrictions needed
    SEVERE     // May need complete rest or medical clearance
}

/**
 * Goal record for target PRs from onboarding
 */
@Serializable
data class GoalRecord(
    @SerialName("exercise_name")
    val exerciseName: String,

    @SerialName("target_value")
    val targetValue: Double, // Target PR value in kg

    val unit: String = "kg",

    @SerialName("set_at")
    val setAt: Long = System.currentTimeMillis(),

    val sport: String // "Powerlifting", "Weightlifting", etc.
)
