package com.example.menotracker.billing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "SubscriptionManager"
private const val PREFS_NAME = "subscription_prefs"
private const val KEY_TRIAL_END_DATE = "trial_end_date"
private const val KEY_DEV_MODE = "dev_mode"
private const val KEY_TIER = "subscription_tier"
private const val KEY_PREMIUM_CHOICE = "premium_feature_choice"

/**
 * Subscription Tiers for Naya
 *
 * FREE: Library, 3 Workouts, Community Posts
 * PREMIUM ($59/year): + Bar Speed OR Nutrition (user chooses one)
 * ELITE ($99/year): Everything including AI Coach & Physical Coach
 */
enum class SubscriptionTier {
    FREE,
    PREMIUM,
    ELITE;

    val displayName: String
        get() = when (this) {
            FREE -> "Free"
            PREMIUM -> "Premium"
            ELITE -> "Elite"
        }

    val yearlyPrice: String
        get() = when (this) {
            FREE -> "$0"
            PREMIUM -> "$59"
            ELITE -> "$99"
        }

    val monthlyPrice: String
        get() = when (this) {
            FREE -> "$0"
            PREMIUM -> "$5.90"
            ELITE -> "$9.90"
        }
}

/**
 * Premium Feature Choice - Premium users choose ONE
 */
enum class PremiumFeatureChoice {
    VBT,
    NUTRITION;

    val displayName: String
        get() = when (this) {
            VBT -> "Velocity Based Training"
            NUTRITION -> "Nutrition Tracker"
        }
}

/**
 * Features that can be gated by subscription
 */
enum class Feature {
    // FREE features
    LIBRARY,
    WORKOUT_TRACKING,
    COMMUNITY_POSTS,
    MOOD_JOURNALING,      // Basic mood tracking (limited)
    BASIC_BREATHING,      // 1 free breathing exercise
    BASIC_MEDITATION,     // 2 free meditations
    BASIC_SOUNDS,         // 3 free ambient sounds

    // PREMIUM features (choose one)
    BAR_SPEED,  // Previously VBT - Bar Speed tracker
    NUTRITION,

    // PREMIUM features (always included)
    UNLIMITED_MOOD_ENTRIES,  // Unlimited mood journal entries
    BREATHING_LIBRARY,       // Full breathing exercise library
    MEDITATION_LIBRARY,      // Full meditation library
    PREMIUM_SOUNDS,          // All ambient sounds
    SOUNDSCAPE_MIXER,        // Multi-layer sound mixing

    // ELITE only
    AI_COACH,
    PHYSICAL_COACH,
    WOD_SCANNER,  // Smart WOD Scanner with OCR/Excel/PDF

    // Limits
    UNLIMITED_WORKOUTS
}

/**
 * Subscription Manager - Central place for checking subscription status
 *
 * IMPORTANT: Call initialize(context) on app startup to restore persisted state
 */
object SubscriptionManager {
    private val _currentTier = MutableStateFlow(SubscriptionTier.FREE)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    private val _premiumFeatureChoice = MutableStateFlow<PremiumFeatureChoice?>(null)
    val premiumFeatureChoice: StateFlow<PremiumFeatureChoice?> = _premiumFeatureChoice.asStateFlow()

    // Dev mode - bypasses all tier restrictions (connected to red dot)
    // DEV: Default ON for testing - set to false for production
    private val _devModeEnabled = MutableStateFlow(true)
    val devModeEnabled: StateFlow<Boolean> = _devModeEnabled.asStateFlow()

    // Trial system
    private val _trialEndDate = MutableStateFlow<Long?>(null) // Epoch millis when trial ends
    val trialEndDate: StateFlow<Long?> = _trialEndDate.asStateFlow()

    // Trial duration in days
    const val TRIAL_DURATION_DAYS = 10

    // Free tier limits
    const val FREE_WORKOUT_LIMIT = 3
    const val FREE_MOOD_ENTRIES_PER_WEEK = 3  // Mood entries per week for free tier

    // SharedPreferences instance for persistence
    private var prefs: SharedPreferences? = null

    /**
     * Initialize SubscriptionManager with context - MUST be called on app startup
     * Restores persisted trial end date, dev mode, tier, and premium choice
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Restore trial end date
        val savedTrialEnd = prefs?.getLong(KEY_TRIAL_END_DATE, 0L) ?: 0L
        if (savedTrialEnd > 0) {
            _trialEndDate.value = savedTrialEnd
            Log.d(TAG, "Restored trial end date: $savedTrialEnd (${getRemainingTrialDays()} days remaining)")
        }

        // Restore dev mode (default TRUE for development builds)
        // TODO: Set default to false for production release
        val savedDevMode = prefs?.getBoolean(KEY_DEV_MODE, true) ?: true
        _devModeEnabled.value = savedDevMode
        Log.d(TAG, "🔓 Dev mode: $savedDevMode (all features unlocked)")

        // Restore subscription tier
        val savedTier = prefs?.getString(KEY_TIER, null)
        if (savedTier != null) {
            try {
                _currentTier.value = SubscriptionTier.valueOf(savedTier)
                Log.d(TAG, "Restored tier: $savedTier")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore tier: $savedTier")
            }
        }

        // Restore premium feature choice
        val savedChoice = prefs?.getString(KEY_PREMIUM_CHOICE, null)
        if (savedChoice != null) {
            try {
                _premiumFeatureChoice.value = PremiumFeatureChoice.valueOf(savedChoice)
                Log.d(TAG, "Restored premium choice: $savedChoice")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore premium choice: $savedChoice")
            }
        }

        Log.d(TAG, "SubscriptionManager initialized - Trial active: ${isInTrialPeriod()}, DevMode: $savedDevMode")
    }

    /**
     * Set subscription tier (called after purchase verification)
     */
    fun setTier(tier: SubscriptionTier) {
        _currentTier.value = tier
        prefs?.edit()?.putString(KEY_TIER, tier.name)?.apply()
        Log.d(TAG, "Set tier: $tier")
    }

    /**
     * Set premium feature choice (Bar Speed or Nutrition)
     */
    fun setPremiumFeatureChoice(choice: PremiumFeatureChoice) {
        _premiumFeatureChoice.value = choice
        prefs?.edit()?.putString(KEY_PREMIUM_CHOICE, choice.name)?.apply()
        Log.d(TAG, "Set premium choice: $choice")
    }

    /**
     * Toggle dev mode (connected to red dot in UI)
     * When enabled, all features are unlocked regardless of tier
     */
    fun toggleDevMode() {
        val newValue = !_devModeEnabled.value
        _devModeEnabled.value = newValue
        prefs?.edit()?.putBoolean(KEY_DEV_MODE, newValue)?.apply()
        Log.d(TAG, "Toggled dev mode: $newValue")
    }

    /**
     * Set dev mode directly
     */
    fun setDevMode(enabled: Boolean) {
        _devModeEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_DEV_MODE, enabled)?.apply()
        Log.d(TAG, "Set dev mode: $enabled")
    }

    /**
     * Start a trial period (called after onboarding completion)
     * Trial gives ELITE access for 10 days
     */
    fun startTrial(context: Context? = null) {
        // Use provided context or existing prefs
        context?.let { prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

        val trialEnd = System.currentTimeMillis() + (TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000L)
        _trialEndDate.value = trialEnd
        prefs?.edit()?.putLong(KEY_TRIAL_END_DATE, trialEnd)?.apply()
        Log.d(TAG, "Started trial - ends at: $trialEnd ($TRIAL_DURATION_DAYS days)")
    }

    /**
     * Check if trial has already been started (to avoid restarting on each app launch)
     */
    fun hasTrialBeenStarted(): Boolean {
        return (_trialEndDate.value ?: 0L) > 0
    }

    /**
     * Check if user is in active trial period
     */
    fun isInTrialPeriod(): Boolean {
        val endDate = _trialEndDate.value ?: return false
        return System.currentTimeMillis() < endDate
    }

    /**
     * Get remaining trial days
     */
    fun getRemainingTrialDays(): Int {
        val endDate = _trialEndDate.value ?: return 0
        val remaining = endDate - System.currentTimeMillis()
        if (remaining <= 0) return 0
        return (remaining / (24 * 60 * 60 * 1000L)).toInt() + 1
    }

    /**
     * Check if a feature is available for the current subscription
     */
    fun hasAccess(feature: Feature): Boolean {
        // Dev mode bypasses ALL restrictions
        if (_devModeEnabled.value) return true

        // Trial period gives ELITE access
        if (isInTrialPeriod()) return true

        val tier = _currentTier.value
        val premiumChoice = _premiumFeatureChoice.value

        return when (feature) {
            // FREE features - everyone has access
            Feature.LIBRARY -> true
            Feature.WORKOUT_TRACKING -> true
            Feature.COMMUNITY_POSTS -> true
            Feature.MOOD_JOURNALING -> true      // Basic mood tracking for all
            Feature.BASIC_BREATHING -> true      // One free breathing exercise
            Feature.BASIC_MEDITATION -> true     // Two free meditations
            Feature.BASIC_SOUNDS -> true         // Three free ambient sounds

            // Bar Speed - Premium (if chosen) or Elite
            Feature.BAR_SPEED -> when (tier) {
                SubscriptionTier.ELITE -> true
                SubscriptionTier.PREMIUM -> premiumChoice == PremiumFeatureChoice.VBT
                SubscriptionTier.FREE -> false
            }

            // Nutrition - Premium (if chosen) or Elite
            Feature.NUTRITION -> when (tier) {
                SubscriptionTier.ELITE -> true
                SubscriptionTier.PREMIUM -> premiumChoice == PremiumFeatureChoice.NUTRITION
                SubscriptionTier.FREE -> false
            }

            // PREMIUM features (always included in Premium+)
            Feature.UNLIMITED_MOOD_ENTRIES -> tier != SubscriptionTier.FREE
            Feature.BREATHING_LIBRARY -> tier != SubscriptionTier.FREE
            Feature.MEDITATION_LIBRARY -> tier != SubscriptionTier.FREE
            Feature.PREMIUM_SOUNDS -> tier != SubscriptionTier.FREE
            Feature.SOUNDSCAPE_MIXER -> tier != SubscriptionTier.FREE

            // ELITE only features
            Feature.AI_COACH -> tier == SubscriptionTier.ELITE
            Feature.PHYSICAL_COACH -> tier == SubscriptionTier.ELITE
            Feature.WOD_SCANNER -> tier == SubscriptionTier.ELITE

            // Unlimited workouts - Premium or Elite
            Feature.UNLIMITED_WORKOUTS -> tier != SubscriptionTier.FREE
        }
    }

    /**
     * Check if user can create more workouts
     */
    fun canCreateWorkout(currentWorkoutCount: Int): Boolean {
        // Dev mode or trial bypasses limits
        if (_devModeEnabled.value || isInTrialPeriod()) return true
        if (_currentTier.value != SubscriptionTier.FREE) return true
        return currentWorkoutCount < FREE_WORKOUT_LIMIT
    }

    /**
     * Get remaining workout slots for free users
     */
    fun getRemainingWorkoutSlots(currentWorkoutCount: Int): Int {
        // Dev mode or trial = unlimited
        if (_devModeEnabled.value || isInTrialPeriod()) return Int.MAX_VALUE
        if (_currentTier.value != SubscriptionTier.FREE) return Int.MAX_VALUE
        return (FREE_WORKOUT_LIMIT - currentWorkoutCount).coerceAtLeast(0)
    }

    /**
     * Check if user can add more mood entries this week
     */
    fun canAddMoodEntry(currentWeeklyCount: Int): Boolean {
        // Dev mode or trial bypasses limits
        if (_devModeEnabled.value || isInTrialPeriod()) return true
        if (_currentTier.value != SubscriptionTier.FREE) return true
        return currentWeeklyCount < FREE_MOOD_ENTRIES_PER_WEEK
    }

    /**
     * Get remaining mood entry slots for free users this week
     */
    fun getRemainingMoodSlots(currentWeeklyCount: Int): Int {
        // Dev mode or trial = unlimited
        if (_devModeEnabled.value || isInTrialPeriod()) return Int.MAX_VALUE
        if (_currentTier.value != SubscriptionTier.FREE) return Int.MAX_VALUE
        return (FREE_MOOD_ENTRIES_PER_WEEK - currentWeeklyCount).coerceAtLeast(0)
    }

    /**
     * Check if a specific breathing exercise is available
     */
    fun canAccessBreathingExercise(isFreeExercise: Boolean): Boolean {
        // Dev mode or trial = all exercises
        if (_devModeEnabled.value || isInTrialPeriod()) return true
        // Free exercise is always available
        if (isFreeExercise) return true
        // Premium exercises require Premium+
        return _currentTier.value != SubscriptionTier.FREE
    }

    /**
     * Check if user needs to choose a premium feature
     */
    fun needsPremiumFeatureChoice(): Boolean {
        return _currentTier.value == SubscriptionTier.PREMIUM && _premiumFeatureChoice.value == null
    }

    /**
     * Get upgrade message for a locked feature
     */
    fun getUpgradeMessage(feature: Feature): String {
        return when (feature) {
            Feature.BAR_SPEED, Feature.NUTRITION -> "Upgrade to Premium ($59/year) to unlock this feature"
            Feature.AI_COACH, Feature.PHYSICAL_COACH, Feature.WOD_SCANNER -> "Upgrade to Elite ($99/year) for AI Coach, Physical Coach & WOD Scanner"
            Feature.UNLIMITED_WORKOUTS -> "Upgrade to Premium to create unlimited workouts"
            Feature.UNLIMITED_MOOD_ENTRIES -> "Upgrade to Premium for unlimited mood journal entries"
            Feature.BREATHING_LIBRARY -> "Upgrade to Premium to unlock all breathing exercises"
            Feature.MEDITATION_LIBRARY -> "Upgrade to Premium to unlock all meditations"
            Feature.PREMIUM_SOUNDS -> "Upgrade to Premium to unlock all ambient sounds"
            Feature.SOUNDSCAPE_MIXER -> "Upgrade to Premium to mix multiple sounds"
            else -> "This feature requires a subscription"
        }
    }

    /**
     * Get the required tier for a feature
     */
    fun getRequiredTier(feature: Feature): SubscriptionTier {
        return when (feature) {
            Feature.LIBRARY, Feature.WORKOUT_TRACKING, Feature.COMMUNITY_POSTS,
            Feature.MOOD_JOURNALING, Feature.BASIC_BREATHING, Feature.BASIC_MEDITATION,
            Feature.BASIC_SOUNDS -> SubscriptionTier.FREE
            Feature.BAR_SPEED, Feature.NUTRITION, Feature.UNLIMITED_WORKOUTS,
            Feature.UNLIMITED_MOOD_ENTRIES, Feature.BREATHING_LIBRARY,
            Feature.MEDITATION_LIBRARY, Feature.PREMIUM_SOUNDS,
            Feature.SOUNDSCAPE_MIXER -> SubscriptionTier.PREMIUM
            Feature.AI_COACH, Feature.PHYSICAL_COACH, Feature.WOD_SCANNER -> SubscriptionTier.ELITE
        }
    }

    /**
     * Check if a specific meditation is accessible
     */
    fun canAccessMeditation(isFreeSession: Boolean): Boolean {
        // Dev mode or trial = all sessions
        if (_devModeEnabled.value || isInTrialPeriod()) return true
        // Free session is always available
        if (isFreeSession) return true
        // Premium sessions require Premium+
        return _currentTier.value != SubscriptionTier.FREE
    }

    /**
     * Check if a specific sound is accessible
     */
    fun canAccessSound(isFreeSound: Boolean): Boolean {
        // Dev mode or trial = all sounds
        if (_devModeEnabled.value || isInTrialPeriod()) return true
        // Free sound is always available
        if (isFreeSound) return true
        // Premium sounds require Premium+
        return _currentTier.value != SubscriptionTier.FREE
    }

    /**
     * Check if user can use soundscape mixer
     */
    fun canUseSoundscapeMixer(): Boolean {
        return hasAccess(Feature.SOUNDSCAPE_MIXER)
    }

    /**
     * Check if user has premium access (PREMIUM or ELITE tier)
     */
    fun hasPremiumAccess(): Boolean {
        return _currentTier.value != SubscriptionTier.FREE
    }
}