package com.example.menotracker.screens.home

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

/**
 * Repository for Smart Home screen data including:
 * - Workout time pattern detection
 * - Dual streak tracking
 * - Home preferences persistence
 */
class SmartHomeRepository(
    private val context: Context,
    private val supabase: SupabaseClient? = null
) {
    companion object {
        private const val TAG = "SmartHomeRepository"
        private const val PREFS_NAME = "smart_home_prefs"
        private const val KEY_PREFERENCES = "preferences"
        private const val KEY_USER_TIER = "user_tier"
        private const val MIN_WORKOUTS_FOR_PATTERN = 3
        private const val MAX_WORKOUT_SLOTS_STORED = 10
        private const val DAYS_BEFORE_SOFT_PROMPT = 7

        @Volatile
        private var INSTANCE: SmartHomeRepository? = null

        fun getInstance(context: Context, supabase: SupabaseClient? = null): SmartHomeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartHomeRepository(context.applicationContext, supabase).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // State flows for reactive updates
    private val _preferences = MutableStateFlow(loadPreferencesFromCache())
    val preferences: StateFlow<SmartHomePreferences> = _preferences.asStateFlow()

    private val _userTier = MutableStateFlow(loadUserTierFromCache())
    val userTier: StateFlow<UserTier> = _userTier.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════════
    // PATTERN DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Record a workout completion time and update the pattern detection
     * Called after each workout is completed
     */
    suspend fun recordWorkoutTime(workoutTime: LocalTime): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Recording workout time: $workoutTime")

            val currentPrefs = _preferences.value
            val timeSlot = WorkoutTimeSlot.fromHour(workoutTime.hour)

            if (timeSlot != null) {
                // Add to the list of workout time slots (keep last 10)
                val updatedSlots = (currentPrefs.getWorkoutSlots() + timeSlot)
                    .takeLast(MAX_WORKOUT_SLOTS_STORED)
                    .map { it.name }

                // Evaluate pattern if we have enough data
                val newPattern = evaluatePattern(updatedSlots.mapNotNull {
                    try { WorkoutTimeSlot.valueOf(it) } catch (e: Exception) { null }
                })

                val updatedPrefs = currentPrefs.copy(
                    workoutTimeSlots = updatedSlots,
                    workoutTimePattern = newPattern?.name
                )

                savePreferences(updatedPrefs)
                Log.d(TAG, "Pattern updated: $newPattern from ${updatedSlots.size} workouts")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording workout time", e)
            Result.failure(e)
        }
    }

    /**
     * Evaluate the dominant workout time pattern from recent workouts
     */
    private fun evaluatePattern(slots: List<WorkoutTimeSlot>): WorkoutTimeSlot? {
        if (slots.size < MIN_WORKOUTS_FOR_PATTERN) {
            return null
        }

        // Count occurrences of each time slot
        val counts = slots.groupingBy { it }.eachCount()
        val dominant = counts.maxByOrNull { it.value }

        // Need at least 2 workouts in the dominant slot
        return if (dominant != null && dominant.value >= 2) {
            dominant.key
        } else {
            null
        }
    }

    /**
     * Manually set the user's preferred workout time (from soft prompt)
     */
    suspend fun setPreferredWorkoutTime(timeSlot: WorkoutTimeSlot): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "User manually set workout time: $timeSlot")

            val updatedPrefs = _preferences.value.copy(
                workoutTimePattern = timeSlot.name,
                softPromptAnswered = true
            )
            savePreferences(updatedPrefs)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting preferred workout time", e)
            Result.failure(e)
        }
    }

    /**
     * Mark soft prompt as dismissed without setting a pattern
     */
    suspend fun dismissSoftPrompt(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatedPrefs = _preferences.value.copy(
                softPromptAnswered = true
            )
            savePreferences(updatedPrefs)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing soft prompt", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STREAK TRACKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Update workout streak after completing a workout
     */
    suspend fun updateWorkoutStreak(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val currentPrefs = _preferences.value
            val lastWorkout = currentPrefs.lastWorkoutDate?.let { LocalDate.parse(it) }

            val newStreak = when {
                // First workout ever
                lastWorkout == null -> 1
                // Already worked out today
                lastWorkout == today -> currentPrefs.workoutStreak
                // Consecutive day
                lastWorkout == today.minusDays(1) -> currentPrefs.workoutStreak + 1
                // Streak broken
                else -> 1
            }

            val updatedPrefs = currentPrefs.copy(
                workoutStreak = newStreak,
                lastWorkoutDate = today.toString()
            )
            savePreferences(updatedPrefs)

            Log.d(TAG, "Workout streak updated: $newStreak")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating workout streak", e)
            Result.failure(e)
        }
    }

    /**
     * Update nutrition streak after logging a meal
     */
    suspend fun updateNutritionStreak(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val currentPrefs = _preferences.value
            val lastNutrition = currentPrefs.lastNutritionDate?.let { LocalDate.parse(it) }

            val newStreak = when {
                // First meal logged ever
                lastNutrition == null -> 1
                // Already logged today
                lastNutrition == today -> currentPrefs.nutritionStreak
                // Consecutive day
                lastNutrition == today.minusDays(1) -> currentPrefs.nutritionStreak + 1
                // Streak broken
                else -> 1
            }

            val updatedPrefs = currentPrefs.copy(
                nutritionStreak = newStreak,
                lastNutritionDate = today.toString()
            )
            savePreferences(updatedPrefs)

            Log.d(TAG, "Nutrition streak updated: $newStreak")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating nutrition streak", e)
            Result.failure(e)
        }
    }

    /**
     * Get current streaks (checking for streak breaks)
     */
    fun getCurrentStreaks(): UserStreaks {
        val prefs = _preferences.value
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val lastWorkout = prefs.lastWorkoutDate?.let { LocalDate.parse(it) }
        val lastNutrition = prefs.lastNutritionDate?.let { LocalDate.parse(it) }

        // Check if streaks are still valid (last activity was today or yesterday)
        val workoutStreak = if (lastWorkout == today || lastWorkout == yesterday) {
            prefs.workoutStreak
        } else {
            0
        }

        val nutritionStreak = if (lastNutrition == today || lastNutrition == yesterday) {
            prefs.nutritionStreak
        } else {
            0
        }

        return UserStreaks(
            workoutStreak = workoutStreak,
            nutritionStreak = nutritionStreak,
            lastWorkoutDate = lastWorkout,
            lastNutritionDate = lastNutrition
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SOFT PROMPT LOGIC
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if we should show the soft prompt to ask about workout time preference
     */
    fun shouldShowSoftPrompt(appInstallDate: LocalDate? = null): Boolean {
        val prefs = _preferences.value

        // Already answered
        if (prefs.softPromptAnswered) return false

        // Already has a detected pattern
        if (prefs.getWorkoutPattern() != null) return false

        // Check if user has had the app for at least 7 days
        val installDate = appInstallDate ?: getAppInstallDate()
        val daysSinceInstall = java.time.temporal.ChronoUnit.DAYS.between(installDate, LocalDate.now())

        return daysSinceInstall >= DAYS_BEFORE_SOFT_PROMPT
    }

    /**
     * Mark that the soft prompt was shown
     */
    suspend fun markSoftPromptShown(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatedPrefs = _preferences.value.copy(
                softPromptShownAt = System.currentTimeMillis()
            )
            savePreferences(updatedPrefs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROMO IMPRESSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Increment workout promo impression count
     */
    suspend fun incrementWorkoutPromoImpression(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatedPrefs = _preferences.value.copy(
                workoutPromoImpressions = _preferences.value.workoutPromoImpressions + 1
            )
            savePreferences(updatedPrefs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Increment nutrition promo impression count
     */
    suspend fun incrementNutritionPromoImpression(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatedPrefs = _preferences.value.copy(
                nutritionPromoImpressions = _preferences.value.nutritionPromoImpressions + 1
            )
            savePreferences(updatedPrefs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER TIER
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set the user's tier (called when subscription status changes)
     */
    fun setUserTier(tier: UserTier) {
        prefs.edit().putString(KEY_USER_TIER, tier.name).apply()
        _userTier.value = tier
        Log.d(TAG, "User tier set to: $tier")
    }

    /**
     * Get current user tier
     * For now, defaults to FULL since we don't have separate tiers yet
     */
    private fun loadUserTierFromCache(): UserTier {
        val tierName = prefs.getString(KEY_USER_TIER, UserTier.FULL.name)
        return try {
            UserTier.valueOf(tierName ?: UserTier.FULL.name)
        } catch (e: Exception) {
            UserTier.FULL
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════════════════════

    private fun loadPreferencesFromCache(): SmartHomePreferences {
        val prefsJson = prefs.getString(KEY_PREFERENCES, null)
        return if (prefsJson != null) {
            try {
                json.decodeFromString(prefsJson)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading preferences", e)
                SmartHomePreferences()
            }
        } else {
            SmartHomePreferences()
        }
    }

    private suspend fun savePreferences(preferences: SmartHomePreferences) {
        try {
            val prefsJson = json.encodeToString(preferences)
            prefs.edit().putString(KEY_PREFERENCES, prefsJson).apply()
            _preferences.value = preferences

            // Also sync to Supabase if available
            syncToSupabase(preferences)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving preferences", e)
        }
    }

    private suspend fun syncToSupabase(preferences: SmartHomePreferences) {
        // TODO: Implement Supabase sync when ready
        // This will update the profiles table with the new fields
    }

    private fun getAppInstallDate(): LocalDate {
        // Try to get install date from package manager
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installTime = packageInfo.firstInstallTime
            java.time.Instant.ofEpochMilli(installTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } catch (e: Exception) {
            // Fallback to today if can't determine
            LocalDate.now()
        }
    }

    /**
     * Load preferences from Supabase (called on app start)
     */
    suspend fun loadFromSupabase(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        // TODO: Implement loading from Supabase profiles table
        // For now, just use local cache
        Result.success(Unit)
    }
}