package com.example.menotracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val IS_METRIC_KEY = booleanPreferencesKey("is_metric")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val LANGUAGE_KEY = stringPreferencesKey("app_language") // "system", "en", "de", "fr"

        // SharedPreferences key for synchronous language read (for attachBaseContext)
        private const val LANGUAGE_PREFS_NAME = "language_prefs"
        private const val LANGUAGE_PREFS_KEY = "app_language"

        /**
         * Synchronously get the saved language code.
         * Uses SharedPreferences for fast, synchronous access needed in attachBaseContext.
         */
        fun getLanguageSync(context: Context): String {
            val prefs = context.getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(LANGUAGE_PREFS_KEY, "system") ?: "system"
        }

        /**
         * Synchronously save language (for backup to SharedPreferences)
         */
        fun saveLanguageSync(context: Context, languageCode: String) {
            val prefs = context.getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(LANGUAGE_PREFS_KEY, languageCode).apply()
        }

        // ═══════════════════════════════════════════════════════════════
        // FORM ANALYSIS / POSE OVERLAY SETTINGS
        // ═══════════════════════════════════════════════════════════════
        private val FRAME_SKIP_RATE_KEY = intPreferencesKey("frame_skip_rate")
        private val GRID_OVERLAY_ENABLED_KEY = booleanPreferencesKey("grid_overlay_enabled")
        private val GRID_SPACING_CM_KEY = intPreferencesKey("grid_spacing_cm")
        private val GRID_LINE_THICKNESS_KEY = floatPreferencesKey("grid_line_thickness")
        private val SKELETON_LINE_THICKNESS_KEY = floatPreferencesKey("skeleton_line_thickness")
        private val SHOW_JOINT_ANGLES_KEY = booleanPreferencesKey("show_joint_angles")

        // ═══════════════════════════════════════════════════════════════
        // APP REVIEW / RATING SETTINGS
        // ═══════════════════════════════════════════════════════════════
        private val HAS_RATED_APP_KEY = booleanPreferencesKey("has_rated_app")
        private val COMPLETED_WORKOUTS_COUNT_KEY = intPreferencesKey("completed_workouts_count")
        private val LAST_REVIEW_PROMPT_TIMESTAMP_KEY = longPreferencesKey("last_review_prompt_timestamp")
        private val REVIEW_PROMPT_DISMISSED_COUNT_KEY = intPreferencesKey("review_prompt_dismissed_count")
    }

    // ========== IS METRIC (Units: Metric or Imperial) ==========
    val isMetric: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[IS_METRIC_KEY] ?: true // Default: Metric
    }

    suspend fun setIsMetric(isMetric: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[IS_METRIC_KEY] = isMetric
        }
    }

    // ========== NOTIFICATIONS ENABLED ==========
    val notificationsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true // Default: Enabled
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    // ========== LANGUAGE ==========
    val language: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "system" // Default: System language
    }

    suspend fun setLanguage(languageCode: String) {
        // Save to SharedPreferences for synchronous access in attachBaseContext
        Companion.saveLanguageSync(context, languageCode)
        // Save to DataStore for reactive updates
        context.settingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FORM ANALYSIS SETTINGS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Frame Skip Rate: 1 = no skip (30 FPS), 2 = every 2nd (15 FPS), 4 = every 4th (7.5 FPS)
     * Default: 2 (good balance for most exercises)
     * Snatch/Clean: Use 1-2
     * Squat/Bench/Deadlift: Can use up to 4
     */
    val frameSkipRate: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[FRAME_SKIP_RATE_KEY] ?: 2 // Default: every 2nd frame
    }

    suspend fun setFrameSkipRate(rate: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[FRAME_SKIP_RATE_KEY] = rate.coerceIn(1, 5)
        }
    }

    /**
     * Grid Overlay: Show alignment grid on camera preview
     */
    val gridOverlayEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[GRID_OVERLAY_ENABLED_KEY] ?: false // Default: disabled
    }

    suspend fun setGridOverlayEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[GRID_OVERLAY_ENABLED_KEY] = enabled
        }
    }

    /**
     * Grid Spacing in cm: 5, 10, 15, 20, 25, 30
     * Default: 15cm (good balance)
     */
    val gridSpacingCm: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[GRID_SPACING_CM_KEY] ?: 15 // Default: 15cm
    }

    suspend fun setGridSpacingCm(spacingCm: Int) {
        context.settingsDataStore.edit { preferences ->
            // Ensure it's in 5cm steps between 5 and 30
            val validSpacing = ((spacingCm / 5) * 5).coerceIn(5, 30)
            preferences[GRID_SPACING_CM_KEY] = validSpacing
        }
    }

    /**
     * Grid Line Thickness: 1f (thin) to 6f (thick)
     * Default: 2f
     */
    val gridLineThickness: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[GRID_LINE_THICKNESS_KEY] ?: 2f // Default: medium
    }

    suspend fun setGridLineThickness(thickness: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[GRID_LINE_THICKNESS_KEY] = thickness.coerceIn(1f, 6f)
        }
    }

    /**
     * Skeleton Line Thickness: 4f (thin), 8f (medium/default), 12f (thick)
     */
    val skeletonLineThickness: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[SKELETON_LINE_THICKNESS_KEY] ?: 8f // Default: medium
    }

    suspend fun setSkeletonLineThickness(thickness: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[SKELETON_LINE_THICKNESS_KEY] = thickness.coerceIn(4f, 16f)
        }
    }

    /**
     * Show Joint Angles: Display angle values on tracked joints
     */
    val showJointAngles: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SHOW_JOINT_ANGLES_KEY] ?: true // Default: enabled
    }

    suspend fun setShowJointAngles(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_JOINT_ANGLES_KEY] = enabled
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // APP REVIEW / RATING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Has the user already rated the app?
     */
    val hasRatedApp: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[HAS_RATED_APP_KEY] ?: false
    }

    suspend fun setHasRatedApp(hasRated: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[HAS_RATED_APP_KEY] = hasRated
        }
    }

    /**
     * Number of completed workouts (for triggering review prompt)
     */
    val completedWorkoutsCount: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[COMPLETED_WORKOUTS_COUNT_KEY] ?: 0
    }

    suspend fun incrementCompletedWorkouts() {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[COMPLETED_WORKOUTS_COUNT_KEY] ?: 0
            preferences[COMPLETED_WORKOUTS_COUNT_KEY] = current + 1
        }
    }

    /**
     * Timestamp of last review prompt (to avoid spamming)
     */
    val lastReviewPromptTimestamp: Flow<Long> = context.settingsDataStore.data.map { preferences ->
        preferences[LAST_REVIEW_PROMPT_TIMESTAMP_KEY] ?: 0L
    }

    suspend fun setLastReviewPromptTimestamp(timestamp: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[LAST_REVIEW_PROMPT_TIMESTAMP_KEY] = timestamp
        }
    }

    /**
     * How many times has the user dismissed the review prompt?
     */
    val reviewPromptDismissedCount: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[REVIEW_PROMPT_DISMISSED_COUNT_KEY] ?: 0
    }

    suspend fun incrementReviewPromptDismissed() {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[REVIEW_PROMPT_DISMISSED_COUNT_KEY] ?: 0
            preferences[REVIEW_PROMPT_DISMISSED_COUNT_KEY] = current + 1
        }
    }

    /**
     * DEBUG: Reset review state for testing - sets counter to 4 so next workout triggers dialog
     */
    suspend fun resetReviewStateForTesting() {
        context.settingsDataStore.edit { preferences ->
            preferences[HAS_RATED_APP_KEY] = false
            preferences[COMPLETED_WORKOUTS_COUNT_KEY] = 4
            preferences[LAST_REVIEW_PROMPT_TIMESTAMP_KEY] = 0L
            preferences[REVIEW_PROMPT_DISMISSED_COUNT_KEY] = 0
        }
        android.util.Log.d("SettingsDataStore", "🔄 Review state reset - next workout will show rating dialog!")
    }

    /**
     * Combined Flow for review state
     */
    val reviewState: Flow<ReviewState> = context.settingsDataStore.data.map { preferences ->
        ReviewState(
            hasRated = preferences[HAS_RATED_APP_KEY] ?: false,
            completedWorkouts = preferences[COMPLETED_WORKOUTS_COUNT_KEY] ?: 0,
            lastPromptTimestamp = preferences[LAST_REVIEW_PROMPT_TIMESTAMP_KEY] ?: 0L,
            dismissedCount = preferences[REVIEW_PROMPT_DISMISSED_COUNT_KEY] ?: 0
        )
    }
}

/**
 * Data class for app review state
 */
data class ReviewState(
    val hasRated: Boolean = false,
    val completedWorkouts: Int = 0,
    val lastPromptTimestamp: Long = 0L,
    val dismissedCount: Int = 0
) {
    companion object {
        private const val WORKOUTS_BEFORE_FIRST_PROMPT = 5
        private const val WORKOUTS_BETWEEN_PROMPTS = 10
        private const val MIN_DAYS_BETWEEN_PROMPTS = 7
        private const val MAX_DISMISSALS = 3
    }

    /**
     * Should we show the review prompt?
     */
    fun shouldShowReviewPrompt(): Boolean {
        if (hasRated) return false
        if (dismissedCount >= MAX_DISMISSALS) return false

        // Check minimum workouts
        val requiredWorkouts = if (dismissedCount == 0) {
            WORKOUTS_BEFORE_FIRST_PROMPT
        } else {
            WORKOUTS_BEFORE_FIRST_PROMPT + (dismissedCount * WORKOUTS_BETWEEN_PROMPTS)
        }
        if (completedWorkouts < requiredWorkouts) return false

        // Check time since last prompt
        if (lastPromptTimestamp > 0) {
            val daysSinceLastPrompt = (System.currentTimeMillis() - lastPromptTimestamp) / (1000 * 60 * 60 * 24)
            if (daysSinceLastPrompt < MIN_DAYS_BETWEEN_PROMPTS) return false
        }

        return true
    }
}