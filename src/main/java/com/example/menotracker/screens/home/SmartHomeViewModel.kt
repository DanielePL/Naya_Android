package com.example.menotracker.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * ViewModel for the Smart Home Screen
 * Handles Hero Card selection, pattern detection, and streaks
 */
class SmartHomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SmartHomeViewModel"
    }

    private val repository = SmartHomeRepository.getInstance(application)

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════

    data class SmartHomeState(
        val heroCardType: HeroCardType = HeroCardType.START_WORKOUT,
        val userTier: UserTier = UserTier.FULL,
        val streaks: UserStreaks = UserStreaks(),
        val preferences: SmartHomePreferences = SmartHomePreferences(),
        val showSoftPrompt: Boolean = false,
        val isLoading: Boolean = false
    )

    private val _state = MutableStateFlow(SmartHomeState())
    val state: StateFlow<SmartHomeState> = _state.asStateFlow()

    init {
        observePreferences()
        refreshHeroCard()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                repository.preferences,
                repository.userTier
            ) { prefs, tier ->
                prefs to tier
            }.collect { (prefs, tier) ->
                val streaks = repository.getCurrentStreaks()
                val heroCard = getHeroCard(tier, prefs, LocalTime.now())
                val showPrompt = repository.shouldShowSoftPrompt()

                _state.value = _state.value.copy(
                    heroCardType = heroCard,
                    userTier = tier,
                    streaks = streaks,
                    preferences = prefs,
                    showSoftPrompt = showPrompt
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HERO CARD SELECTION LOGIC
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Determine which hero card to show based on user tier, pattern, and time of day
     *
     * Priority:
     * 1. If user has workout pattern and is in workout window -> START_WORKOUT
     * 2. If user has nutrition tier -> Time-based meal card
     * 3. Fallback for workout-only user -> START_WORKOUT
     */
    fun getHeroCard(
        tier: UserTier,
        preferences: SmartHomePreferences,
        currentTime: LocalTime
    ): HeroCardType {
        val workoutPattern = preferences.getWorkoutPattern()
        val currentHour = currentTime.hour

        Log.d(TAG, "getHeroCard: tier=$tier, pattern=$workoutPattern, hour=$currentHour")

        // 1. Check if user has workout pattern and is in the workout window
        if (workoutPattern != null && tier.hasWorkout()) {
            val isWorkoutWindow = workoutPattern.containsHour(currentHour)
            Log.d(TAG, "Workout pattern check: isWindow=$isWorkoutWindow")

            if (isWorkoutWindow) {
                return HeroCardType.START_WORKOUT
            }
        }

        // 2. Fallback: Time-based Nutrition (if user has nutrition tier)
        if (tier.hasNutrition()) {
            return when (currentHour) {
                in 6..10 -> HeroCardType.LOG_BREAKFAST
                in 11..14 -> HeroCardType.LOG_LUNCH
                in 17..21 -> HeroCardType.LOG_DINNER
                else -> HeroCardType.QUICK_LOG
            }
        }

        // 3. Fallback for workout-only user outside pattern
        return HeroCardType.START_WORKOUT
    }

    /**
     * Refresh the hero card based on current time
     * Call this when the screen becomes visible or time changes significantly
     */
    fun refreshHeroCard() {
        val currentState = _state.value
        val newHeroCard = getHeroCard(
            currentState.userTier,
            currentState.preferences,
            LocalTime.now()
        )

        if (newHeroCard != currentState.heroCardType) {
            _state.value = currentState.copy(heroCardType = newHeroCard)
            Log.d(TAG, "Hero card refreshed to: $newHeroCard")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PATTERN DETECTION ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Record that a workout was completed at the current time
     * This updates the pattern detection
     */
    fun onWorkoutCompleted() {
        viewModelScope.launch {
            val currentTime = LocalTime.now()
            repository.recordWorkoutTime(currentTime)
            repository.updateWorkoutStreak()
        }
    }

    /**
     * Record that a meal was logged
     */
    fun onMealLogged() {
        viewModelScope.launch {
            repository.updateNutritionStreak()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SOFT PROMPT ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * User selected a preferred workout time from the soft prompt
     */
    fun onWorkoutTimeSelected(timeSlot: WorkoutTimeSlot) {
        viewModelScope.launch {
            repository.setPreferredWorkoutTime(timeSlot)
            _state.value = _state.value.copy(showSoftPrompt = false)
        }
    }

    /**
     * User dismissed the soft prompt without selecting
     */
    fun onSoftPromptDismissed() {
        viewModelScope.launch {
            repository.dismissSoftPrompt()
            _state.value = _state.value.copy(showSoftPrompt = false)
        }
    }

    /**
     * Mark that the soft prompt was shown (for tracking)
     */
    fun onSoftPromptShown() {
        viewModelScope.launch {
            repository.markSoftPromptShown()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROMO CARD ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Track that a workout promo card was shown
     */
    fun onWorkoutPromoShown() {
        viewModelScope.launch {
            repository.incrementWorkoutPromoImpression()
        }
    }

    /**
     * Track that a nutrition promo card was shown
     */
    fun onNutritionPromoShown() {
        viewModelScope.launch {
            repository.incrementNutritionPromoImpression()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if user should see workout promo card
     */
    fun shouldShowWorkoutPromo(): Boolean {
        val tier = _state.value.userTier
        val impressions = _state.value.preferences.workoutPromoImpressions

        // Only show if user doesn't have workout tier
        if (tier.hasWorkout()) return false

        // Decay logic: hide after 35 impressions
        return impressions < 35
    }

    /**
     * Check if user should see nutrition promo card
     */
    fun shouldShowNutritionPromo(): Boolean {
        val tier = _state.value.userTier
        val impressions = _state.value.preferences.nutritionPromoImpressions

        // Only show if user doesn't have nutrition tier
        if (tier.hasNutrition()) return false

        // Decay logic: hide after 35 impressions
        return impressions < 35
    }

    /**
     * Get promo card size based on impressions
     * Full size for first 2 weeks, mini after that
     */
    fun getPromoCardSize(isWorkout: Boolean): PromoCardSize {
        val impressions = if (isWorkout) {
            _state.value.preferences.workoutPromoImpressions
        } else {
            _state.value.preferences.nutritionPromoImpressions
        }

        return when {
            impressions < 14 -> PromoCardSize.FULL
            impressions < 35 -> PromoCardSize.MINI
            else -> PromoCardSize.HIDDEN
        }
    }
}

enum class PromoCardSize {
    FULL,
    MINI,
    HIDDEN
}