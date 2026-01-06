package com.example.menotracker.data

import android.content.Context
import android.util.Log
import com.example.menotracker.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// ═══════════════════════════════════════════════════════════════
// PRE-WORKOUT NUTRITION SERVICE
// Smart alerts for optimal workout nutrition timing
// ═══════════════════════════════════════════════════════════════

class PreWorkoutNutritionService(
    private val context: Context,
    private val workoutPatternRepository: WorkoutPatternRepository,
    private val nutritionRepository: NutritionRepository? = null
) {

    companion object {
        private const val TAG = "PreWorkoutNutrition"

        // Default body weight if not set
        private const val DEFAULT_BODY_WEIGHT_KG = 75f

        // Alert timing thresholds
        private const val ALERT_FULL_MEAL_HOURS = 3.5f      // Show "eat full meal" alert
        private const val ALERT_LIGHT_MEAL_HOURS = 2.0f     // Show "eat light meal" alert
        private const val ALERT_QUICK_CARBS_HOURS = 0.75f   // Show "quick carbs" alert
        private const val TOO_LATE_HOURS = 0.3f             // Too late to eat much

        // Meal timing thresholds
        private const val RECENTLY_ATE_HOURS = 2.5f         // Considered well-fueled
        private const val SHOULD_EAT_HOURS = 3.5f           // Should consider eating

        @Volatile
        private var INSTANCE: PreWorkoutNutritionService? = null

        fun getInstance(context: Context): PreWorkoutNutritionService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreWorkoutNutritionService(
                    context.applicationContext,
                    WorkoutPatternRepository.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }

    private val _preWorkoutState = MutableStateFlow<PreWorkoutNutritionState?>(null)
    val preWorkoutState: Flow<PreWorkoutNutritionState?> = _preWorkoutState.asStateFlow()

    private val _postWorkoutState = MutableStateFlow<PostWorkoutNutritionState?>(null)
    val postWorkoutState: Flow<PostWorkoutNutritionState?> = _postWorkoutState.asStateFlow()

    // User settings (should be loaded from profile)
    private var userBodyWeightKg: Float = DEFAULT_BODY_WEIGHT_KG
    private var lastMealTimeMillis: Long? = null

    // ═══════════════════════════════════════════════════════════════
    // STATE CALCULATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Update the pre-workout nutrition state
     * Call this periodically (e.g., every 15 minutes) or when meal is logged
     */
    fun updatePreWorkoutState(
        lastMealTime: Instant? = null,
        bodyWeightKg: Float? = null
    ): PreWorkoutNutritionState {
        // Update cached values
        lastMealTime?.let { lastMealTimeMillis = it.toEpochMilli() }
        bodyWeightKg?.let { userBodyWeightKg = it }

        val state = calculatePreWorkoutState()
        _preWorkoutState.value = state

        Log.d(TAG, "Pre-workout state: ${state.recommendedAction}, urgency: ${state.urgency}")
        return state
    }

    /**
     * Calculate current pre-workout nutrition state
     */
    private fun calculatePreWorkoutState(): PreWorkoutNutritionState {
        val now = Instant.now()

        // Check if workout is in progress
        if (workoutPatternRepository.isWorkoutInProgress()) {
            return createWorkoutInProgressState()
        }

        // Get predicted workout time
        val predictedWorkoutTime = workoutPatternRepository.getPredictedWorkoutTimeToday()
        val hoursUntilWorkout = workoutPatternRepository.getHoursUntilPredictedWorkout()
        val confidence = workoutPatternRepository.getTodayPredictionConfidence()

        // If no workout predicted or already passed
        if (predictedWorkoutTime == null || hoursUntilWorkout == null || hoursUntilWorkout <= 0) {
            return createNoWorkoutPredictedState()
        }

        // Calculate hours since last meal
        val hoursSinceLastMeal = lastMealTimeMillis?.let {
            (now.toEpochMilli() - it) / (1000f * 60f * 60f)
        }

        // Determine recommended action
        val action = determinePreWorkoutAction(hoursUntilWorkout, hoursSinceLastMeal)
        val urgency = determineAlertUrgency(action, hoursUntilWorkout, hoursSinceLastMeal)
        val macros = NutritionTimingRecommendations.getPreWorkoutMacros(hoursUntilWorkout, userBodyWeightKg)
        val message = generatePreWorkoutMessage(action, hoursUntilWorkout, hoursSinceLastMeal)

        return PreWorkoutNutritionState(
            predictedWorkoutTime = predictedWorkoutTime,
            hoursUntilWorkout = hoursUntilWorkout,
            lastMealTime = lastMealTimeMillis?.let { Instant.ofEpochMilli(it) },
            hoursSinceLastMeal = hoursSinceLastMeal,
            recommendedAction = action,
            proteinRecommendation = macros.protein,
            carbRecommendation = macros.carbs,
            fatRecommendation = macros.fat,
            confidence = confidence,
            message = message,
            urgency = urgency
        )
    }

    /**
     * Determine what action the user should take
     */
    private fun determinePreWorkoutAction(
        hoursUntilWorkout: Float,
        hoursSinceLastMeal: Float?
    ): PreWorkoutAction {
        // Check if already well-fueled
        if (hoursSinceLastMeal != null && hoursSinceLastMeal < RECENTLY_ATE_HOURS) {
            // Check if the meal was at a good time relative to workout
            val mealToWorkoutGap = hoursUntilWorkout + (RECENTLY_ATE_HOURS - hoursSinceLastMeal)
            if (mealToWorkoutGap >= 1.5f && mealToWorkoutGap <= 4f) {
                return PreWorkoutAction.WELL_FUELED
            }
        }

        return when {
            hoursUntilWorkout > ALERT_FULL_MEAL_HOURS -> PreWorkoutAction.FULL_MEAL_NOW
            hoursUntilWorkout > ALERT_LIGHT_MEAL_HOURS -> PreWorkoutAction.FULL_MEAL_NOW
            hoursUntilWorkout > ALERT_QUICK_CARBS_HOURS -> PreWorkoutAction.LIGHT_MEAL_NOW
            hoursUntilWorkout > TOO_LATE_HOURS -> PreWorkoutAction.QUICK_CARBS_ONLY
            else -> PreWorkoutAction.TOO_LATE_TO_EAT
        }
    }

    /**
     * Determine alert urgency level
     */
    private fun determineAlertUrgency(
        action: PreWorkoutAction,
        hoursUntilWorkout: Float,
        hoursSinceLastMeal: Float?
    ): AlertUrgency {
        if (action == PreWorkoutAction.WELL_FUELED) return AlertUrgency.NONE

        val needsFood = hoursSinceLastMeal == null || hoursSinceLastMeal > SHOULD_EAT_HOURS

        return when {
            !needsFood -> AlertUrgency.LOW
            action == PreWorkoutAction.TOO_LATE_TO_EAT && needsFood -> AlertUrgency.CRITICAL
            action == PreWorkoutAction.QUICK_CARBS_ONLY && needsFood -> AlertUrgency.HIGH
            action == PreWorkoutAction.LIGHT_MEAL_NOW && needsFood -> AlertUrgency.MEDIUM
            action == PreWorkoutAction.FULL_MEAL_NOW && hoursUntilWorkout < 3f -> AlertUrgency.MEDIUM
            else -> AlertUrgency.LOW
        }
    }

    /**
     * Generate user-friendly message
     */
    private fun generatePreWorkoutMessage(
        action: PreWorkoutAction,
        hoursUntilWorkout: Float,
        hoursSinceLastMeal: Float?
    ): String {
        val timeStr = formatHoursToTime(hoursUntilWorkout)
        val lastMealStr = hoursSinceLastMeal?.let { formatHoursToTime(it) } ?: "unknown"

        return when (action) {
            PreWorkoutAction.FULL_MEAL_NOW -> {
                if (hoursSinceLastMeal != null && hoursSinceLastMeal > SHOULD_EAT_HOURS) {
                    "Workout in ~$timeStr. Last meal was $lastMealStr ago - eat a full meal now!"
                } else {
                    "Workout in ~$timeStr. Good time for a pre-workout meal."
                }
            }
            PreWorkoutAction.LIGHT_MEAL_NOW -> {
                "Workout in ~$timeStr. Time for a light meal - focus on protein & carbs, low fat."
            }
            PreWorkoutAction.QUICK_CARBS_ONLY -> {
                "Workout in ~$timeStr. Only quick carbs now (banana, dates, rice cakes)."
            }
            PreWorkoutAction.TOO_LATE_TO_EAT -> {
                "Workout starting soon. Skip heavy food - maybe a small snack if needed."
            }
            PreWorkoutAction.WELL_FUELED -> {
                "You're well-fueled for your workout!"
            }
            PreWorkoutAction.NO_WORKOUT_PREDICTED -> {
                "No workout predicted today based on your pattern."
            }
            PreWorkoutAction.WORKOUT_IN_PROGRESS -> {
                "Workout in progress. Focus on hydration!"
            }
            PreWorkoutAction.POST_WORKOUT_WINDOW -> {
                "Post-workout window active - prioritize protein intake!"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // POST-WORKOUT STATE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start post-workout tracking when workout ends
     */
    fun startPostWorkoutTracking(
        workoutEndTime: Instant,
        wasFasted: Boolean
    ): PostWorkoutNutritionState {
        val state = calculatePostWorkoutState(workoutEndTime, wasFasted, 0f, 0f)
        _postWorkoutState.value = state
        return state
    }

    /**
     * Update post-workout state when food is logged
     */
    fun updatePostWorkoutIntake(
        proteinConsumed: Float,
        carbsConsumed: Float
    ) {
        val currentState = _postWorkoutState.value ?: return

        val updatedState = calculatePostWorkoutState(
            currentState.workoutEndTime,
            currentState.wasFasted,
            proteinConsumed,
            carbsConsumed
        )

        _postWorkoutState.value = updatedState
    }

    /**
     * Calculate post-workout nutrition state
     */
    private fun calculatePostWorkoutState(
        workoutEndTime: Instant,
        wasFasted: Boolean,
        proteinConsumed: Float,
        carbsConsumed: Float
    ): PostWorkoutNutritionState {
        val now = Instant.now()
        val minutesSince = (now.toEpochMilli() - workoutEndTime.toEpochMilli()) / 60000

        val macros = NutritionTimingRecommendations.getPostWorkoutMacros(
            minutesSince,
            userBodyWeightKg,
            wasFasted
        )

        val proteinTarget = macros.protein.last.toFloat()
        val carbsTarget = macros.carbs.last.toFloat()

        val phase = when {
            minutesSince <= NutritionTimingRecommendations.IMMEDIATE_WINDOW_MINUTES ->
                PostWorkoutPhase.IMMEDIATE
            minutesSince <= NutritionTimingRecommendations.OPTIMAL_WINDOW_MINUTES ->
                PostWorkoutPhase.OPTIMAL
            minutesSince <= NutritionTimingRecommendations.EXTENDED_WINDOW_MINUTES ->
                PostWorkoutPhase.EXTENDED
            else -> PostWorkoutPhase.CLOSED
        }

        val proteinMet = proteinConsumed >= proteinTarget * 0.8f
        val urgency = when {
            phase == PostWorkoutPhase.CLOSED -> AlertUrgency.NONE
            proteinMet -> AlertUrgency.NONE
            phase == PostWorkoutPhase.IMMEDIATE && wasFasted -> AlertUrgency.CRITICAL
            phase == PostWorkoutPhase.IMMEDIATE -> AlertUrgency.HIGH
            phase == PostWorkoutPhase.OPTIMAL -> AlertUrgency.MEDIUM
            else -> AlertUrgency.LOW
        }

        val message = generatePostWorkoutMessage(phase, minutesSince, proteinConsumed, proteinTarget, wasFasted)

        return PostWorkoutNutritionState(
            workoutEndTime = workoutEndTime,
            minutesSinceWorkout = minutesSince,
            wasFasted = wasFasted,
            proteinConsumedSince = proteinConsumed,
            carbsConsumedSince = carbsConsumed,
            proteinTarget = proteinTarget,
            carbsTarget = carbsTarget,
            windowPhase = phase,
            urgency = urgency,
            message = message
        )
    }

    private fun generatePostWorkoutMessage(
        phase: PostWorkoutPhase,
        minutesSince: Long,
        proteinConsumed: Float,
        proteinTarget: Float,
        wasFasted: Boolean
    ): String {
        val remaining = (proteinTarget - proteinConsumed).coerceAtLeast(0f).toInt()

        return when {
            proteinConsumed >= proteinTarget * 0.8f -> {
                "Great job! You've hit your post-workout protein target."
            }
            phase == PostWorkoutPhase.IMMEDIATE && wasFasted -> {
                "⚡ Fasted workout! Eat ${remaining}g+ protein immediately for best recovery."
            }
            phase == PostWorkoutPhase.IMMEDIATE -> {
                "🎯 Optimal window! Get ${remaining}g+ protein in the next ${30 - minutesSince} min."
            }
            phase == PostWorkoutPhase.OPTIMAL -> {
                "Still in optimal window. Need ${remaining}g more protein."
            }
            phase == PostWorkoutPhase.EXTENDED -> {
                "Extended window. ${remaining}g protein still beneficial."
            }
            else -> {
                "Window closed. Eat normally and prepare for next workout."
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER STATES
    // ═══════════════════════════════════════════════════════════════

    private fun createNoWorkoutPredictedState(): PreWorkoutNutritionState {
        return PreWorkoutNutritionState(
            predictedWorkoutTime = null,
            hoursUntilWorkout = null,
            lastMealTime = lastMealTimeMillis?.let { Instant.ofEpochMilli(it) },
            hoursSinceLastMeal = lastMealTimeMillis?.let {
                (System.currentTimeMillis() - it) / (1000f * 60f * 60f)
            },
            recommendedAction = PreWorkoutAction.NO_WORKOUT_PREDICTED,
            proteinRecommendation = 0..0,
            carbRecommendation = 0..0,
            fatRecommendation = 0..0,
            confidence = 0f,
            message = "No workout predicted today based on your pattern.",
            urgency = AlertUrgency.NONE
        )
    }

    private fun createWorkoutInProgressState(): PreWorkoutNutritionState {
        return PreWorkoutNutritionState(
            predictedWorkoutTime = null,
            hoursUntilWorkout = 0f,
            lastMealTime = lastMealTimeMillis?.let { Instant.ofEpochMilli(it) },
            hoursSinceLastMeal = null,
            recommendedAction = PreWorkoutAction.WORKOUT_IN_PROGRESS,
            proteinRecommendation = 0..0,
            carbRecommendation = 0..0,
            fatRecommendation = 0..0,
            confidence = 1f,
            message = "Workout in progress. Stay hydrated!",
            urgency = AlertUrgency.NONE
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private fun formatHoursToTime(hours: Float): String {
        return when {
            hours < 1f -> "${(hours * 60).toInt()} min"
            hours < 2f -> "1h ${((hours - 1) * 60).toInt()}min"
            else -> "${hours.toInt()}h ${((hours - hours.toInt()) * 60).toInt()}min"
        }
    }

    /**
     * Clear post-workout state (e.g., after window closes or next day)
     */
    fun clearPostWorkoutState() {
        _postWorkoutState.value = null
    }

    /**
     * Set user's body weight for macro calculations
     */
    fun setUserBodyWeight(weightKg: Float) {
        userBodyWeightKg = weightKg
    }

    /**
     * Record a meal time
     */
    fun recordMealTime(mealTime: Instant = Instant.now()) {
        lastMealTimeMillis = mealTime.toEpochMilli()
        // Recalculate state
        updatePreWorkoutState()
    }
}