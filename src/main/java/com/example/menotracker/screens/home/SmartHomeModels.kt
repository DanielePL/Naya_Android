package com.example.menotracker.screens.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

// ═══════════════════════════════════════════════════════════════════════════════
// USER TIER SYSTEM
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * User subscription tiers determining available features
 */
enum class UserTier {
    WORKOUT_ONLY,   // Can track workouts
    NUTRITION_ONLY, // Can track nutrition
    FULL           // Can do both
}

/**
 * Check if user has access to workout features
 */
fun UserTier.hasWorkout(): Boolean = this == UserTier.WORKOUT_ONLY || this == UserTier.FULL

/**
 * Check if user has access to nutrition features
 */
fun UserTier.hasNutrition(): Boolean = this == UserTier.NUTRITION_ONLY || this == UserTier.FULL

// ═══════════════════════════════════════════════════════════════════════════════
// WORKOUT TIME PATTERNS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Time slots for workout pattern detection
 */
enum class WorkoutTimeSlot {
    MORNING,  // 5:00 - 10:00
    MIDDAY,   // 10:00 - 14:00
    EVENING;  // 16:00 - 21:00

    /**
     * Check if a given hour falls within this time slot
     */
    fun containsHour(hour: Int): Boolean = when (this) {
        MORNING -> hour in 5..10
        MIDDAY -> hour in 10..14
        EVENING -> hour in 16..21
    }

    companion object {
        /**
         * Get the time slot for a given hour, or null if outside all slots
         */
        fun fromHour(hour: Int): WorkoutTimeSlot? = when (hour) {
            in 5..10 -> MORNING
            in 10..14 -> MIDDAY
            in 16..21 -> EVENING
            else -> null
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HERO CARD TYPES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Types of hero cards that can be displayed
 */
enum class HeroCardType {
    // Workout types
    START_WORKOUT,      // Main CTA to start a workout session

    // Nutrition types (meal-specific)
    LOG_BREAKFAST,
    LOG_LUNCH,
    LOG_DINNER,
    QUICK_LOG           // Fallback nutrition card outside meal times
}

/**
 * Get display info for a hero card type
 */
fun HeroCardType.getDisplayInfo(): HeroCardDisplayInfo = when (this) {
    HeroCardType.START_WORKOUT -> HeroCardDisplayInfo(
        title = "Ready to Train?",
        subtitle = "Start your workout session",
        ctaText = "START WORKOUT",
        icon = HeroCardIcon.WORKOUT
    )
    HeroCardType.LOG_BREAKFAST -> HeroCardDisplayInfo(
        title = "Good Morning!",
        subtitle = "Log your breakfast to stay on track",
        ctaText = "LOG BREAKFAST",
        icon = HeroCardIcon.BREAKFAST
    )
    HeroCardType.LOG_LUNCH -> HeroCardDisplayInfo(
        title = "Lunch Time!",
        subtitle = "Keep your nutrition on point",
        ctaText = "LOG LUNCH",
        icon = HeroCardIcon.LUNCH
    )
    HeroCardType.LOG_DINNER -> HeroCardDisplayInfo(
        title = "Dinner Time!",
        subtitle = "Finish strong with your last meal",
        ctaText = "LOG DINNER",
        icon = HeroCardIcon.DINNER
    )
    HeroCardType.QUICK_LOG -> HeroCardDisplayInfo(
        title = "Track Your Nutrition",
        subtitle = "Log meals to hit your macros",
        ctaText = "QUICK LOG",
        icon = HeroCardIcon.NUTRITION
    )
}

data class HeroCardDisplayInfo(
    val title: String,
    val subtitle: String,
    val ctaText: String,
    val icon: HeroCardIcon
)

enum class HeroCardIcon {
    WORKOUT,
    BREAKFAST,
    LUNCH,
    DINNER,
    NUTRITION
}

// ═══════════════════════════════════════════════════════════════════════════════
// DUAL STREAKS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * User streak data for workout and nutrition
 */
data class UserStreaks(
    val workoutStreak: Int = 0,        // Days with at least 1 workout
    val nutritionStreak: Int = 0,      // Days with at least 1 meal logged
    val lastWorkoutDate: LocalDate? = null,
    val lastNutritionDate: LocalDate? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// SMART HOME PREFERENCES (stored in UserProfile / Supabase)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Home screen preferences stored per user
 * These extend the UserProfile with home screen specific data
 */
@Serializable
data class SmartHomePreferences(
    @SerialName("workout_time_pattern")
    val workoutTimePattern: String? = null, // MORNING, MIDDAY, EVENING, or null

    @SerialName("workout_time_slots")
    val workoutTimeSlots: List<String> = emptyList(), // Last 10 workout time slots

    @SerialName("workout_promo_impressions")
    val workoutPromoImpressions: Int = 0,

    @SerialName("nutrition_promo_impressions")
    val nutritionPromoImpressions: Int = 0,

    @SerialName("preferred_feed_type")
    val preferredFeedType: String = "GLOBAL", // GLOBAL or FOLLOWING

    @SerialName("soft_prompt_answered")
    val softPromptAnswered: Boolean = false,

    @SerialName("soft_prompt_shown_at")
    val softPromptShownAt: Long? = null,

    // Dual streaks
    @SerialName("workout_streak")
    val workoutStreak: Int = 0,

    @SerialName("nutrition_streak")
    val nutritionStreak: Int = 0,

    @SerialName("last_workout_date")
    val lastWorkoutDate: String? = null, // ISO date string

    @SerialName("last_nutrition_date")
    val lastNutritionDate: String? = null // ISO date string
) {
    /**
     * Get the detected workout time pattern as enum
     */
    fun getWorkoutPattern(): WorkoutTimeSlot? {
        return workoutTimePattern?.let {
            try {
                WorkoutTimeSlot.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Get workout time slots as enums
     */
    fun getWorkoutSlots(): List<WorkoutTimeSlot> {
        return workoutTimeSlots.mapNotNull {
            try {
                WorkoutTimeSlot.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SOFT PROMPT (Quick Question Card)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Options for the soft prompt time selection
 */
data class TimeSlotOption(
    val emoji: String,
    val label: String,
    val timeSlot: WorkoutTimeSlot
)

/**
 * Data for the quick question card shown after 7 days if pattern is unclear
 */
data class QuickQuestionCard(
    val question: String = "When do you usually train?",
    val options: List<TimeSlotOption> = listOf(
        TimeSlotOption("🌅", "Morning", WorkoutTimeSlot.MORNING),
        TimeSlotOption("☀️", "Midday", WorkoutTimeSlot.MIDDAY),
        TimeSlotOption("🌙", "Evening", WorkoutTimeSlot.EVENING)
    ),
    val skipOption: String = "It varies",
    val dismissable: Boolean = true
)

// ═══════════════════════════════════════════════════════════════════════════════
// TIME-BASED NUTRITION THEME COLORS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Time-based theme for Nutrition cards
 * Changes color based on time of day to feel "alive" with the user's rhythm
 */
data class TimeBasedNutritionTheme(
    val emoji: String,
    val gradientStartHex: Long,
    val gradientEndHex: Long,
    val iconColorHex: Long
)

/**
 * Get the nutrition theme based on current time
 * All colors stay within the Naya violet branding
 *
 * | Time        | Theme          | Colors                         |
 * |-------------|----------------|--------------------------------|
 * | 6-9         | Morning        | Soft lavender (calm start)     |
 * | 9-11        | Late Morning   | Light violet                   |
 * | 11-14       | Lunch          | Bright violet                  |
 * | 14-17       | Afternoon      | Medium violet                  |
 * | 17-21       | Dinner         | Deeper violet                  |
 * | 21-6        | Night          | Deep purple/indigo             |
 */
fun getNutritionThemeForHour(hour: Int): TimeBasedNutritionTheme {
    return when (hour) {
        // Morning (6-9): Soft Lavender - Calm start
        in 6..8 -> TimeBasedNutritionTheme(
            emoji = "🌅",
            gradientStartHex = 0xFFC4B5FD,  // Violet 300 (NayaVioletBright)
            gradientEndHex = 0xFFB89EFC,    // Violet 400 (NayaVioletGlow)
            iconColorHex = 0xFFEDE9FE       // Violet 100
        )

        // Late Morning (9-11): Light Violet
        in 9..10 -> TimeBasedNutritionTheme(
            emoji = "☀️",
            gradientStartHex = 0xFFB89EFC,  // Violet 400 (NayaVioletGlow)
            gradientEndHex = 0xFFA78BFA,    // Violet 500 (NayaPrimary)
            iconColorHex = 0xFFEDE9FE       // Violet 100
        )

        // Lunch (11-14): Bright Violet - Peak energy
        in 11..13 -> TimeBasedNutritionTheme(
            emoji = "☀️",
            gradientStartHex = 0xFFA78BFA,  // Violet 500 (NayaPrimary)
            gradientEndHex = 0xFF8B5CF6,    // Violet 600 (NayaVioletDark)
            iconColorHex = 0xFFEDE9FE       // Violet 100
        )

        // Afternoon (14-17): Medium Violet
        in 14..16 -> TimeBasedNutritionTheme(
            emoji = "🌤️",
            gradientStartHex = 0xFF8B5CF6,  // Violet 600 (NayaVioletDark)
            gradientEndHex = 0xFF7C3AED,    // Violet 700
            iconColorHex = 0xFFEDE9FE       // Violet 100
        )

        // Dinner (17-21): Deeper Violet - Winding down
        in 17..20 -> TimeBasedNutritionTheme(
            emoji = "🌅",
            gradientStartHex = 0xFF7C3AED,  // Violet 700
            gradientEndHex = 0xFF6D28D9,    // Violet 800
            iconColorHex = 0xFFDDD6FE       // Violet 200
        )

        // Night (21-6): Deep Purple/Indigo - Rest time
        else -> TimeBasedNutritionTheme(
            emoji = "🌙",
            gradientStartHex = 0xFF6D28D9,  // Violet 800
            gradientEndHex = 0xFF5B21B6,    // Violet 900
            iconColorHex = 0xFFDDD6FE       // Violet 200
        )
    }
}

/**
 * Meal types for nutrition tracking
 */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}