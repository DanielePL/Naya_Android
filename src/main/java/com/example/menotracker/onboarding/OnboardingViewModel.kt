package com.example.menotracker.onboarding

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.onboarding.data.*
import com.example.menotracker.data.AuthRepository
import com.example.menotracker.data.UserProfileRepository
import com.example.menotracker.data.repository.MenopauseProfileRepository
import com.example.menotracker.data.models.UserProfile
import com.example.menotracker.data.models.MenopauseStage
import com.example.menotracker.data.models.MenopauseSymptomType
import com.example.menotracker.data.models.HRTStatus
import com.example.menotracker.data.models.PersonalRecord
import com.example.menotracker.data.models.GoalRecord
import com.example.menotracker.data.models.ActivityLevel
import com.example.menotracker.data.models.DietaryPreference
import com.example.menotracker.data.models.FoodAllergy
import com.example.menotracker.billing.SubscriptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// DataStore for onboarding preferences
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")

// Auth DataStore for guest mode (same as in AuthRepository)
private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")
private val GUEST_MODE_KEY = stringPreferencesKey("guest_mode")

class OnboardingViewModel : ViewModel() {

    companion object {
        private const val TAG = "OnboardingViewModel"

        // Preferences keys
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_PRIMARY_GOAL = stringPreferencesKey("primary_goal")
        private val KEY_SPORTS = stringPreferencesKey("sports")
        private val KEY_EXPERIENCE_LEVEL = stringPreferencesKey("experience_level")
        private val KEY_COACH_SITUATION = stringPreferencesKey("coach_situation")
        private val KEY_PERSONA_TYPE = stringPreferencesKey("persona_type")
        private val KEY_NUTRITION_ENABLED = booleanPreferencesKey("nutrition_enabled")
        private val KEY_FEATURE_INTERESTS = stringPreferencesKey("feature_interests")
        private val KEY_PENDING_CLOUD_SYNC = booleanPreferencesKey("pending_cloud_sync")
        private val KEY_PENDING_USER_NAME = stringPreferencesKey("pending_user_name")
    }

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _isOnboardingComplete = MutableStateFlow(false)
    val isOnboardingComplete: StateFlow<Boolean> = _isOnboardingComplete.asStateFlow()

    // ✅ FIX: Track wenn initialize() wirklich fertig ist (nicht nur gestartet)
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var context: Context? = null
    private var authRepository: AuthRepository? = null

    /**
     * Initialize with context and check if onboarding is already complete
     * IMPORTANT: We check BOTH local DataStore AND user's cloud profile to ensure
     * new users on the same device still see onboarding
     */
    fun initialize(context: Context) {
        // ✅ FIX: Reset isInitialized bei jedem Aufruf - wichtig für Deep Link re-initialization!
        _isInitialized.value = false

        this.context = context
        this.authRepository = AuthRepository(context)
        UserProfileRepository.initialize(context)
        viewModelScope.launch {
            val prefs = context.onboardingDataStore.data.first()
            val localComplete = prefs[KEY_ONBOARDING_COMPLETE] ?: false

            // Get current user ID DIRECTLY from Supabase (not from Flow which might be stale)
            val currentUserId = authRepository?.getCurrentUser()?.id
            Log.d(TAG, "🔑 initialize() - currentUserId from getCurrentUser(): $currentUserId")

            // Check if THIS user has completed onboarding (cloud profile check)
            val userHasCompletedOnboarding = if (currentUserId != null) {
                try {
                    val profileResult = UserProfileRepository.getCurrentProfile(currentUserId)
                    val profile = profileResult.getOrNull()
                    // User has completed onboarding if onboardingCompletedAt is set
                    val cloudComplete = profile?.onboardingCompletedAt != null
                    Log.d(TAG, "User $currentUserId cloud onboarding status: $cloudComplete (onboardingCompletedAt: ${profile?.onboardingCompletedAt})")
                    cloudComplete
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check user profile: ${e.message}")
                    // Fallback to local status if cloud check fails
                    localComplete
                }
            } else {
                // ✅ FIX: No user ID = can't verify identity = require fresh onboarding
                // Don't trust stale local DataStore when no user is logged in
                Log.d(TAG, "No user ID available - cannot verify identity, requiring fresh onboarding")
                false
            }

            // Check if we have pending onboarding data that needs to sync to cloud
            val pendingCloudSync = prefs[KEY_PENDING_CLOUD_SYNC] ?: false
            val pendingUserName = prefs[KEY_PENDING_USER_NAME]

            if (userHasCompletedOnboarding) {
                // Already complete in cloud - load saved persona
                _isOnboardingComplete.value = true
                loadSavedPersona(prefs)
            } else if (localComplete && pendingCloudSync && currentUserId != null) {
                // User just logged in after email verification - sync pending onboarding data to cloud
                Log.d(TAG, "📤 User logged in with pending onboarding data - syncing to cloud...")

                // Load saved answers and persona from DataStore
                val savedPersona = loadSavedPersonaForSync(prefs)
                val savedAnswers = loadSavedAnswersForSync(prefs, pendingUserName)

                // Update state with loaded data
                _state.value = _state.value.copy(
                    answers = savedAnswers,
                    persona = savedPersona
                )

                // Sync to cloud
                saveOnboardingToUserProfile(savedAnswers, savedPersona)

                // Clear pending sync flag
                context.onboardingDataStore.edit { editPrefs ->
                    editPrefs[KEY_PENDING_CLOUD_SYNC] = false
                    editPrefs.remove(KEY_PENDING_USER_NAME)
                }

                // Start trial if not started
                if (!SubscriptionManager.hasTrialBeenStarted()) {
                    SubscriptionManager.startTrial(context)
                    Log.d(TAG, "🎁 Started 10-day trial for verified user - ELITE access enabled!")
                }

                // ✅ FIX: Set BOTH isComplete flags so OnboardingFlow properly completes
                _state.value = _state.value.copy(isComplete = true)
                _isOnboardingComplete.value = true
                Log.d(TAG, "✅ Onboarding data synced to cloud for user $currentUserId")
            } else if (localComplete && !userHasCompletedOnboarding && currentUserId != null) {
                // Local says complete but cloud says no and no pending sync - different user on same device
                Log.d(TAG, "Different user detected - resetting local onboarding status")
                context.onboardingDataStore.edit { it.clear() }
                _isOnboardingComplete.value = false
            } else {
                _isOnboardingComplete.value = userHasCompletedOnboarding
            }

            Log.d(TAG, "Onboarding initialized - complete: ${_isOnboardingComplete.value} (local: $localComplete, pending: $pendingCloudSync)")

            // ✅ FIX: Erst JETZT ist initialize wirklich fertig - UI kann jetzt rendern
            _isInitialized.value = true
        }
    }

    private fun loadSavedPersona(prefs: Preferences) {
        try {
            val personaType = prefs[KEY_PERSONA_TYPE]?.let { PersonaType.valueOf(it) }
                ?: PersonaType.COMPLETE_ATHLETE

            val nutritionEnabled = prefs[KEY_NUTRITION_ENABLED] ?: true

            _state.value = _state.value.copy(
                isComplete = true,
                persona = UserPersona(
                    type = personaType,
                    nutritionEnabled = nutritionEnabled,
                    nutritionScore = if (nutritionEnabled) 0.8f else 0.2f,
                    showAdvancedFeatures = true,
                    coachMode = CoachMode.AI,
                    featureVideos = PersonaEngine.getFeatureVideos(personaType),
                    paywallBenefits = PersonaEngine.getPaywallBenefits(personaType)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load saved persona: ${e.message}")
        }
    }

    /**
     * Load saved persona from DataStore for cloud sync
     */
    private fun loadSavedPersonaForSync(prefs: Preferences): UserPersona {
        return try {
            val personaType = prefs[KEY_PERSONA_TYPE]?.let { PersonaType.valueOf(it) }
                ?: PersonaType.COMPLETE_ATHLETE

            val nutritionEnabled = prefs[KEY_NUTRITION_ENABLED] ?: true

            UserPersona(
                type = personaType,
                nutritionEnabled = nutritionEnabled,
                nutritionScore = if (nutritionEnabled) 0.8f else 0.2f,
                showAdvancedFeatures = true,
                coachMode = CoachMode.AI,
                featureVideos = PersonaEngine.getFeatureVideos(personaType),
                paywallBenefits = PersonaEngine.getPaywallBenefits(personaType)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persona for sync: ${e.message}")
            PersonaEngine.createDefaultPersona()
        }
    }

    /**
     * Load saved answers from DataStore for cloud sync
     */
    private fun loadSavedAnswersForSync(prefs: Preferences, userName: String?): OnboardingAnswers {
        return try {
            val primaryGoals = prefs[KEY_PRIMARY_GOAL]?.split(",")?.mapNotNull {
                try { PrimaryGoal.valueOf(it) } catch (e: Exception) { null }
            } ?: emptyList()

            val experienceLevel = prefs[KEY_EXPERIENCE_LEVEL]?.let {
                try { ExperienceLevel.valueOf(it) } catch (e: Exception) { null }
            }

            val coachSituation = prefs[KEY_COACH_SITUATION]?.let {
                try { CoachSituation.valueOf(it) } catch (e: Exception) { null }
            }

            val featureInterests = prefs[KEY_FEATURE_INTERESTS]?.split(",")?.mapNotNull {
                try { FeatureInterest.valueOf(it) } catch (e: Exception) { null }
            }?.toList() ?: emptyList()

            val sports = prefs[KEY_SPORTS]?.split(",")?.mapNotNull {
                try { Sport.valueOf(it) } catch (e: Exception) { null }
            } ?: emptyList()

            OnboardingAnswers(
                userName = userName,
                primaryGoals = primaryGoals,
                experienceLevel = experienceLevel,
                coachSituation = coachSituation,
                featureInterests = featureInterests,
                sports = sports,
                trainingCommitment = TrainingCommitment(sessionsPerWeek = 3, effortLevel = 7) // Default
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load answers for sync: ${e.message}")
            OnboardingAnswers()
        }
    }

    // ==================== Navigation ====================

    /**
     * Check if we should show the strength goal flow (PRs, commitment, etc.)
     * Based on VBT relevance of selected sport or strength-related goals
     */
    private fun shouldShowStrengthGoalFlow(): Boolean {
        val answers = _state.value.answers

        // Check new unified sport system first (VBT relevance >= 0.5 = strength-focused)
        val unifiedSportRequiresStrength = answers.unifiedSport?.let { sport ->
            sport.vbtRelevance >= 0.5f || sport.trainingFocus.kraft >= 0.6f
        } ?: false

        // Legacy check for old sport enum
        val strengthSports = listOf(Sport.POWERLIFTING, Sport.WEIGHTLIFTING, Sport.STRONGMAN, Sport.CROSSFIT)
        val hasLegacyStrengthSport = answers.sports.any { it in strengthSports }

        // Check goals
        val hasStrengthGoal = answers.primaryGoals.any {
            it in listOf(PrimaryGoal.STRENGTH, PrimaryGoal.PERFORMANCE, PrimaryGoal.COMPLETE)
        }

        return unifiedSportRequiresStrength || hasLegacyStrengthSport || hasStrengthGoal
    }

    fun goToNextStep() {
        val current = _state.value.currentStep
        val answers = _state.value.answers

        val nextStep =
            when (current) {
            // ==================== MENOTRACKER FLOW ====================
            OnboardingStep.WELCOME -> OnboardingStep.MENOPAUSE_STAGE
            OnboardingStep.MENOPAUSE_STAGE -> OnboardingStep.PRIMARY_SYMPTOMS
            OnboardingStep.PRIMARY_SYMPTOMS -> OnboardingStep.MENOPAUSE_GOALS
            OnboardingStep.MENOPAUSE_GOALS -> OnboardingStep.DIETARY_PREFERENCES

            // ==================== LEGACY STEPS (auto-skip for Menotracker) ====================
            OnboardingStep.GOAL_SELECTION -> OnboardingStep.SPORT_SELECTION
            OnboardingStep.SPORT_SELECTION -> OnboardingStep.EXPERIENCE_LEVEL
            OnboardingStep.EXPERIENCE_LEVEL -> OnboardingStep.TRAINING_COMMITMENT

            // Training Commitment → Feature Interest (simplified) or Promise (detailed)
            OnboardingStep.TRAINING_COMMITMENT -> {
                // If user entered detailed benchmarks, show promise commitment
                if (answers.currentBenchmarks.isNotEmpty()) {
                    OnboardingStep.PROMISE_COMMITMENT
                } else {
                    OnboardingStep.FEATURE_INTEREST
                }
            }

            // ==================== DETAILED FLOW (Optional - via "Add details" link) ====================
            OnboardingStep.GENDER_SELECTION -> OnboardingStep.AGE_EXPERIENCE
            OnboardingStep.AGE_EXPERIENCE -> OnboardingStep.SPORT_BENCHMARKS
            OnboardingStep.SPORT_BENCHMARKS -> OnboardingStep.GOAL_BENCHMARKS
            OnboardingStep.GOAL_BENCHMARKS -> OnboardingStep.TRAINING_COMMITMENT

            // Legacy steps (auto-skip)
            OnboardingStep.CURRENT_PRS -> OnboardingStep.SPORT_BENCHMARKS
            OnboardingStep.GOAL_PRS -> OnboardingStep.GOAL_BENCHMARKS

            OnboardingStep.PROMISE_COMMITMENT -> {
                // Auto-calculate experience level from PRs
                val level = _state.value.answers.experienceLevel
                if (level in listOf(ExperienceLevel.EXPERIENCED, ExperienceLevel.ELITE)) {
                    OnboardingStep.COACH_SITUATION
                } else {
                    OnboardingStep.FEATURE_INTEREST
                }
            }

            OnboardingStep.FEATURE_INTEREST -> {
                // Show DIETARY_PREFERENCES if user wants Nutrition OR has Body Comp goal
                val wantsNutrition = FeatureInterest.NUTRITION in answers.featureInterests
                val hasBodyCompGoal = PrimaryGoal.BODY_COMP in answers.primaryGoals

                if (wantsNutrition || hasBodyCompGoal) {
                    OnboardingStep.DIETARY_PREFERENCES
                } else if (answers.experienceLevel in listOf(ExperienceLevel.EXPERIENCED, ExperienceLevel.ELITE)) {
                    OnboardingStep.COACH_SITUATION
                } else {
                    calculateAndSetPersona()
                    OnboardingStep.FEATURE_PROMO
                }
            }

            // Dietary Preferences → Feature Promo (Menotracker simplified flow)
            OnboardingStep.DIETARY_PREFERENCES -> {
                calculateAndSetPersona()
                OnboardingStep.FEATURE_PROMO
            }

            OnboardingStep.COACH_SITUATION -> {
                calculateAndSetPersona()
                OnboardingStep.FEATURE_PROMO
            }

            OnboardingStep.FEATURE_PROMO -> OnboardingStep.PAYWALL
            OnboardingStep.PAYWALL -> OnboardingStep.REGISTRATION
            OnboardingStep.REGISTRATION -> OnboardingStep.NOTIFICATIONS
            OnboardingStep.NOTIFICATIONS -> OnboardingStep.COMPLETE
            OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
        }

        _state.value = _state.value.copy(currentStep = nextStep)

        if (nextStep == OnboardingStep.COMPLETE) {
            completeOnboarding()
        }
    }

    fun goToPreviousStep() {
        val current = _state.value.currentStep
        val answers = _state.value.answers

        val prevStep = when (current) {
            // ==================== MENOTRACKER FLOW ====================
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.MENOPAUSE_STAGE -> OnboardingStep.WELCOME
            OnboardingStep.PRIMARY_SYMPTOMS -> OnboardingStep.MENOPAUSE_STAGE
            OnboardingStep.MENOPAUSE_GOALS -> OnboardingStep.PRIMARY_SYMPTOMS
            OnboardingStep.DIETARY_PREFERENCES -> OnboardingStep.MENOPAUSE_GOALS

            // ==================== LEGACY STEPS ====================
            OnboardingStep.GOAL_SELECTION -> OnboardingStep.WELCOME
            OnboardingStep.SPORT_SELECTION -> OnboardingStep.GOAL_SELECTION
            OnboardingStep.EXPERIENCE_LEVEL -> OnboardingStep.SPORT_SELECTION

            // Training Commitment: Back to Experience Level (simplified) or Goal Benchmarks (detailed)
            OnboardingStep.TRAINING_COMMITMENT -> {
                if (answers.currentBenchmarks.isNotEmpty()) {
                    OnboardingStep.GOAL_BENCHMARKS
                } else {
                    OnboardingStep.EXPERIENCE_LEVEL
                }
            }

            // ==================== DETAILED FLOW (via "Add details" link) ====================
            OnboardingStep.GENDER_SELECTION -> OnboardingStep.EXPERIENCE_LEVEL  // Back to fitness level
            OnboardingStep.AGE_EXPERIENCE -> OnboardingStep.GENDER_SELECTION
            OnboardingStep.SPORT_BENCHMARKS -> OnboardingStep.AGE_EXPERIENCE
            OnboardingStep.GOAL_BENCHMARKS -> OnboardingStep.SPORT_BENCHMARKS

            // Legacy steps
            OnboardingStep.CURRENT_PRS -> OnboardingStep.AGE_EXPERIENCE
            OnboardingStep.GOAL_PRS -> OnboardingStep.CURRENT_PRS

            OnboardingStep.PROMISE_COMMITMENT -> OnboardingStep.TRAINING_COMMITMENT

            OnboardingStep.FEATURE_INTEREST -> {
                // If user entered detailed benchmarks, go back to promise
                if (answers.currentBenchmarks.isNotEmpty()) {
                    OnboardingStep.PROMISE_COMMITMENT
                } else {
                    OnboardingStep.TRAINING_COMMITMENT
                }
            }

            OnboardingStep.COACH_SITUATION -> OnboardingStep.FEATURE_INTEREST

            OnboardingStep.FEATURE_PROMO -> OnboardingStep.DIETARY_PREFERENCES
            OnboardingStep.PAYWALL -> OnboardingStep.FEATURE_PROMO
            OnboardingStep.REGISTRATION -> OnboardingStep.PAYWALL
            OnboardingStep.NOTIFICATIONS -> OnboardingStep.REGISTRATION
            OnboardingStep.COMPLETE -> OnboardingStep.NOTIFICATIONS
        }

        _state.value = _state.value.copy(currentStep = prevStep)
    }

    // ==================== Answer Setters ====================

    fun toggleGoal(goal: PrimaryGoal) {
        val currentGoals = _state.value.answers.primaryGoals.toMutableList()
        if (currentGoals.contains(goal)) {
            currentGoals.remove(goal)
        } else {
            currentGoals.add(goal)
        }
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(primaryGoals = currentGoals)
        )
    }

    fun confirmGoals() {
        goToNextStep()
    }

    fun setSports(sports: List<Sport>) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(sports = sports)
        )
    }

    fun toggleSport(sport: Sport) {
        val currentSports = _state.value.answers.sports.toMutableList()
        if (currentSports.contains(sport)) {
            currentSports.remove(sport)
        } else {
            currentSports.add(sport)
        }
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(sports = currentSports)
        )
    }

    /**
     * Set the unified sport selection (new 135+ sports system)
     */
    fun setUnifiedSport(sport: UnifiedSport?) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(unifiedSport = sport)
        )
    }

    /**
     * Set secondary unified sport (optional)
     */
    fun setSecondaryUnifiedSport(sport: UnifiedSport?) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(secondaryUnifiedSport = sport)
        )
    }

    /**
     * Set tertiary unified sport (optional)
     */
    fun setTertiaryUnifiedSport(sport: UnifiedSport?) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(tertiaryUnifiedSport = sport)
        )
    }

    fun setExperienceLevel(level: ExperienceLevel) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(experienceLevel = level)
        )
        goToNextStep()
    }

    /**
     * Set experience level without auto-navigating (for simplified flow)
     * Used when user selects level on FitnessLevelScreen
     */
    fun setExperienceLevelSimple(level: ExperienceLevel) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(experienceLevel = level)
        )
    }

    /**
     * Navigate to detailed flow (Gender → Age → Benchmarks)
     * Called when user clicks "Add specific numbers" on FitnessLevelScreen
     */
    fun goToDetailedFlow() {
        Log.d(TAG, "User wants to add detailed benchmarks - going to detailed flow")
        _state.value = _state.value.copy(currentStep = OnboardingStep.GENDER_SELECTION)
    }

    fun setCoachSituation(situation: CoachSituation) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(coachSituation = situation)
        )
        goToNextStep()
    }

    fun toggleFeatureInterest(interest: FeatureInterest) {
        val currentInterests = _state.value.answers.featureInterests.toMutableList()
        if (currentInterests.contains(interest)) {
            currentInterests.remove(interest)
        } else {
            currentInterests.add(interest)
        }
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(featureInterests = currentInterests)
        )
    }

    fun confirmFeatureInterests() {
        goToNextStep()
    }

    // ==================== Menopause-Specific Functions ====================

    /**
     * Set menopause stage (from MenopauseStageScreen)
     */
    fun setMenopauseStage(stage: OnboardingMenopauseStage) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(menopauseStage = stage)
        )
        Log.d(TAG, "Set menopause stage: ${stage.name}")
    }

    /**
     * Toggle symptom selection (from PrimarySymptomsScreen)
     * If NONE is selected, clear all other symptoms
     * If another symptom is selected while NONE is active, clear NONE
     */
    fun toggleSymptom(symptom: OnboardingSymptom) {
        val currentSymptoms = _state.value.answers.primarySymptoms.toMutableList()

        if (symptom == OnboardingSymptom.NONE) {
            // "None" is exclusive - clear everything and set only NONE
            _state.value = _state.value.copy(
                answers = _state.value.answers.copy(
                    primarySymptoms = if (currentSymptoms.contains(OnboardingSymptom.NONE)) {
                        emptyList()
                    } else {
                        listOf(OnboardingSymptom.NONE)
                    }
                )
            )
        } else {
            // Regular symptom - remove NONE if present
            currentSymptoms.remove(OnboardingSymptom.NONE)

            if (currentSymptoms.contains(symptom)) {
                currentSymptoms.remove(symptom)
            } else {
                currentSymptoms.add(symptom)
            }

            _state.value = _state.value.copy(
                answers = _state.value.answers.copy(primarySymptoms = currentSymptoms)
            )
        }
    }

    /**
     * Toggle menopause goal selection (from MenopauseGoalsScreen)
     * Max 3 goals allowed
     */
    fun toggleMenopauseGoal(goal: MenopauseGoal) {
        val currentGoals = _state.value.answers.menopauseGoals.toMutableList()

        if (currentGoals.contains(goal)) {
            currentGoals.remove(goal)
        } else if (currentGoals.size < 3) {
            currentGoals.add(goal)
        }

        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(menopauseGoals = currentGoals)
        )
    }

    // ==================== New Goal Setting Functions ====================

    fun setGender(gender: Gender) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(gender = gender)
        )
        goToNextStep()
    }

    fun setAgeAndTrainingYears(age: Int, trainingYears: Int) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(
                age = age,
                trainingYears = trainingYears
            )
        )
        goToNextStep()
    }

    fun setCurrentPRs(prs: CurrentPRs) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(currentPRs = prs)
        )
    }

    fun skipCurrentPRs() {
        // Set default PRs based on experience level and gender for estimation
        goToNextStep()
    }

    fun setGoalPRs(prs: GoalPRs) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(goalPRs = prs)
        )
    }

    fun skipGoalPRs() {
        // User chose to skip goal setting, continue with flow
        goToNextStep()
    }

    // ==================== NEW: Universal Benchmarks Functions ====================

    /**
     * Set current benchmarks (universal system - replaces sport-specific PRs)
     */
    fun setCurrentBenchmarks(benchmarks: Map<String, String>) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(currentBenchmarks = benchmarks)
        )
        Log.d(TAG, "Set current benchmarks: $benchmarks")
    }

    /**
     * Skip current benchmarks (user doesn't know their numbers yet)
     */
    fun skipBenchmarks() {
        Log.d(TAG, "User skipped benchmarks")
        goToNextStep()
    }

    /**
     * Set goal benchmarks (universal system - targets for user's sport)
     */
    fun setGoalBenchmarks(goalBenchmarks: Map<String, String>) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(goalBenchmarks = goalBenchmarks)
        )
        Log.d(TAG, "Set goal benchmarks: $goalBenchmarks")
    }

    /**
     * Skip goal benchmarks (user doesn't want to set goals yet)
     */
    fun skipGoalBenchmarks() {
        Log.d(TAG, "User skipped goal benchmarks")
        goToNextStep()
    }

    fun setTrainingCommitment(commitment: TrainingCommitment) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(trainingCommitment = commitment)
        )
    }

    // ==================== Dietary Preferences Functions ====================

    /**
     * Toggle dietary preference (can select multiple, e.g. Vegan + Halal)
     */
    fun toggleDietaryPreference(preference: DietaryPreference) {
        val currentPreferences = _state.value.answers.dietaryPreferences.toMutableList()
        if (currentPreferences.contains(preference)) {
            currentPreferences.remove(preference)
        } else {
            currentPreferences.add(preference)
        }
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(dietaryPreferences = currentPreferences)
        )
        Log.d(TAG, "Toggled dietary preference: ${preference.name}, now: $currentPreferences")
    }

    /**
     * Toggle food allergy selection
     */
    fun toggleFoodAllergy(allergy: FoodAllergy) {
        val currentAllergies = _state.value.answers.foodAllergies.toMutableList()
        if (currentAllergies.contains(allergy)) {
            currentAllergies.remove(allergy)
        } else {
            currentAllergies.add(allergy)
        }
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(foodAllergies = currentAllergies)
        )
    }

    /**
     * Update food dislikes list
     */
    fun updateFoodDislikes(dislikes: List<String>) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(foodDislikes = dislikes)
        )
    }

    /**
     * Update custom allergy note (for "Other" selection)
     */
    fun updateCustomAllergyNote(note: String?) {
        _state.value = _state.value.copy(
            answers = _state.value.answers.copy(customAllergyNote = note)
        )
    }

    /**
     * Skip dietary preferences (use defaults)
     */
    fun skipDietaryPreferences() {
        Log.d(TAG, "User skipped dietary preferences")
        goToNextStep()
    }

    fun confirmPromise() {
        // Auto-calculate experience level from PRs - respects what user has achieved
        val calculatedLevel = calculateExperienceLevelFromPRs()
        if (calculatedLevel != null) {
            _state.value = _state.value.copy(
                answers = _state.value.answers.copy(experienceLevel = calculatedLevel)
            )
            Log.d(TAG, "Auto-calculated experience level: $calculatedLevel")
        }
        goToNextStep()
    }

    /**
     * Calculate experience level based on PRs, gender, and bodyweight.
     * Supports multiple sports: Powerlifting, Weightlifting, Strongman, CrossFit.
     * Uses the highest level from any sport the user has PRs for.
     */
    private fun calculateExperienceLevelFromPRs(): ExperienceLevel? {
        val currentPRs = _state.value.answers.currentPRs
        val gender = _state.value.answers.gender ?: return null
        val bodyweight = currentPRs.bodyweightKg
        val sports = _state.value.answers.sports

        val levels = mutableListOf<ExperienceLevel>()

        // Check Powerlifting Total (SBD)
        currentPRs.powerliftingTotalKg?.let { total ->
            levels.add(calculatePowerliftingLevel(total, gender, bodyweight))
        }

        // Check Weightlifting Total (Snatch + C&J)
        currentPRs.weightliftingTotalKg?.let { total ->
            levels.add(calculateWeightliftingLevel(total, gender, bodyweight))
        }

        // Check individual Strongman lifts
        if (Sport.STRONGMAN in sports) {
            calculateStrongmanLevel(currentPRs, gender, bodyweight)?.let { levels.add(it) }
        }

        // Return the highest level achieved, or null if no PRs
        return levels.maxByOrNull { it.ordinal }
    }

    /**
     * Powerlifting experience level (SBD Total)
     */
    private fun calculatePowerliftingLevel(total: Float, gender: Gender, bodyweight: Float?): ExperienceLevel {
        val (beginnerMax, intermediateMax, experiencedMax) = when (gender) {
            Gender.MALE -> when {
                bodyweight == null -> Triple(450f, 550f, 650f)
                bodyweight <= 66f -> Triple(350f, 450f, 550f)
                bodyweight <= 74f -> Triple(400f, 500f, 600f)
                bodyweight <= 83f -> Triple(450f, 550f, 650f)
                bodyweight <= 93f -> Triple(475f, 575f, 675f)
                bodyweight <= 105f -> Triple(500f, 600f, 700f)
                bodyweight <= 120f -> Triple(525f, 625f, 725f)
                else -> Triple(550f, 650f, 750f)
            }
            Gender.FEMALE -> when {
                bodyweight == null -> Triple(220f, 280f, 340f)
                bodyweight <= 52f -> Triple(180f, 240f, 300f)
                bodyweight <= 57f -> Triple(200f, 260f, 320f)
                bodyweight <= 63f -> Triple(220f, 280f, 340f)
                bodyweight <= 72f -> Triple(240f, 300f, 360f)
                bodyweight <= 76f -> Triple(260f, 320f, 380f)
                bodyweight <= 84f -> Triple(280f, 340f, 400f)
                else -> Triple(300f, 360f, 420f)
            }
        }

        return when {
            total >= experiencedMax -> ExperienceLevel.ELITE
            total >= intermediateMax -> ExperienceLevel.EXPERIENCED
            total >= beginnerMax -> ExperienceLevel.INTERMEDIATE
            else -> ExperienceLevel.BEGINNER
        }
    }

    /**
     * Weightlifting experience level (Snatch + C&J Total)
     */
    private fun calculateWeightliftingLevel(total: Float, gender: Gender, bodyweight: Float?): ExperienceLevel {
        val (beginnerMax, intermediateMax, experiencedMax) = when (gender) {
            Gender.MALE -> when {
                bodyweight == null -> Triple(140f, 200f, 280f)
                bodyweight <= 61f -> Triple(100f, 150f, 210f)
                bodyweight <= 73f -> Triple(120f, 180f, 250f)
                bodyweight <= 81f -> Triple(140f, 200f, 280f)
                bodyweight <= 89f -> Triple(150f, 220f, 300f)
                bodyweight <= 102f -> Triple(160f, 240f, 330f)
                else -> Triple(170f, 260f, 360f)
            }
            Gender.FEMALE -> when {
                bodyweight == null -> Triple(90f, 130f, 175f)
                bodyweight <= 49f -> Triple(65f, 95f, 130f)
                bodyweight <= 55f -> Triple(75f, 110f, 150f)
                bodyweight <= 59f -> Triple(85f, 125f, 165f)
                bodyweight <= 64f -> Triple(90f, 130f, 175f)
                bodyweight <= 71f -> Triple(95f, 140f, 190f)
                bodyweight <= 81f -> Triple(100f, 150f, 205f)
                else -> Triple(110f, 160f, 220f)
            }
        }

        return when {
            total >= experiencedMax -> ExperienceLevel.ELITE
            total >= intermediateMax -> ExperienceLevel.EXPERIENCED
            total >= beginnerMax -> ExperienceLevel.INTERMEDIATE
            else -> ExperienceLevel.BEGINNER
        }
    }

    /**
     * Strongman experience level (based on best lift relative to bodyweight)
     */
    private fun calculateStrongmanLevel(currentPRs: CurrentPRs, gender: Gender, bodyweight: Float?): ExperienceLevel? {
        val bw = bodyweight ?: 90f // Default bodyweight for strongman

        // Check deadlift (most universal strongman lift)
        val deadlift = currentPRs.getPR(LiftType.DEADLIFT)
        val logLift = currentPRs.getPR(LiftType.LOG_LIFT)

        // Use deadlift-to-bodyweight ratio as primary metric
        if (deadlift != null) {
            val ratio = deadlift / bw
            val thresholds = when (gender) {
                Gender.MALE -> Triple(2.0f, 2.5f, 3.0f) // 2x, 2.5x, 3x BW
                Gender.FEMALE -> Triple(1.5f, 2.0f, 2.5f)
            }
            return when {
                ratio >= thresholds.third -> ExperienceLevel.ELITE
                ratio >= thresholds.second -> ExperienceLevel.EXPERIENCED
                ratio >= thresholds.first -> ExperienceLevel.INTERMEDIATE
                else -> ExperienceLevel.BEGINNER
            }
        }

        // Fallback to log lift if no deadlift
        if (logLift != null) {
            val thresholds = when (gender) {
                Gender.MALE -> Triple(80f, 110f, 140f)
                Gender.FEMALE -> Triple(45f, 65f, 85f)
            }
            return when {
                logLift >= thresholds.third -> ExperienceLevel.ELITE
                logLift >= thresholds.second -> ExperienceLevel.EXPERIENCED
                logLift >= thresholds.first -> ExperienceLevel.INTERMEDIATE
                else -> ExperienceLevel.BEGINNER
            }
        }

        return null
    }

    // ==================== Persona Calculation ====================

    private fun calculateAndSetPersona() {
        val persona = PersonaEngine.calculatePersona(_state.value.answers)
        _state.value = _state.value.copy(persona = persona)
        Log.d(TAG, "Calculated persona: ${persona.type}")
    }

    // ==================== Completion ====================

    private fun completeOnboarding() {
        viewModelScope.launch {
            val ctx = context ?: return@launch
            val persona = _state.value.persona ?: PersonaEngine.createDefaultPersona()
            val answers = _state.value.answers

            // 1. Save to DataStore (quick local persistence)
            ctx.onboardingDataStore.edit { prefs ->
                prefs[KEY_ONBOARDING_COMPLETE] = true
                prefs[KEY_PERSONA_TYPE] = persona.type.name
                prefs[KEY_NUTRITION_ENABLED] = persona.nutritionEnabled

                if (answers.primaryGoals.isNotEmpty()) {
                    prefs[KEY_PRIMARY_GOAL] = answers.primaryGoals.joinToString(",") { it.name }
                }
                answers.experienceLevel?.let { prefs[KEY_EXPERIENCE_LEVEL] = it.name }
                answers.coachSituation?.let { prefs[KEY_COACH_SITUATION] = it.name }

                if (answers.sports.isNotEmpty()) {
                    prefs[KEY_SPORTS] = answers.sports.joinToString(",") { it.name }
                }

                if (answers.featureInterests.isNotEmpty()) {
                    prefs[KEY_FEATURE_INTERESTS] = answers.featureInterests.joinToString(",") { it.name }
                }
            }

            // 2. Save to UserProfile (syncs with Supabase)
            saveOnboardingToUserProfile(answers, persona)

            // 3. Start 10-day trial period (only if not already started)
            // This gives new users ELITE access for 10 days to try all features
            if (!SubscriptionManager.hasTrialBeenStarted()) {
                SubscriptionManager.startTrial(ctx)
                Log.d(TAG, "🎁 Started 10-day trial for new user - ELITE access enabled!")
            }

            _state.value = _state.value.copy(isComplete = true)
            _isOnboardingComplete.value = true

            Log.d(TAG, "Onboarding completed and saved")
        }
    }

    /**
     * Save onboarding data to UserProfile for cloud sync
     */
    private suspend fun saveOnboardingToUserProfile(answers: OnboardingAnswers, persona: UserPersona) {
        try {
            val userId = authRepository?.currentUserId?.first() ?: return
            Log.d(TAG, "Saving onboarding data to UserProfile for user: $userId")

            // Get existing profile or create new one
            val existingProfileResult = UserProfileRepository.getCurrentProfile(userId)
            val existingProfile = existingProfileResult.getOrNull()

            // Convert current PRs to PersonalRecord map (includes universal benchmarks)
            val personalRecords = convertCurrentPRsToPersonalRecords(answers)

            // Convert goal PRs to GoalRecord map (includes universal benchmarks)
            val goalRecords = convertGoalPRsToGoalRecords(answers)

            // Convert onboarding Gender to UserProfile Gender
            val profileGender = when (answers.gender) {
                Gender.MALE -> com.example.menotracker.data.models.Gender.MALE
                Gender.FEMALE -> com.example.menotracker.data.models.Gender.FEMALE
                null -> existingProfile?.gender
            }

            // Calculate activity level from training sessions
            val activityLevel = when (answers.trainingCommitment.sessionsPerWeek) {
                1, 2 -> ActivityLevel.LIGHT
                3 -> ActivityLevel.MODERATE
                4, 5 -> ActivityLevel.ACTIVE
                else -> ActivityLevel.VERY_ACTIVE
            }

            // Convert unified sport to profile fields
            val unifiedSport = answers.unifiedSport
            val trainingFocusMap = unifiedSport?.trainingFocus?.let { focus ->
                mapOf(
                    "kraft" to focus.kraft,
                    "schnelligkeit" to focus.schnelligkeit,
                    "ausdauer" to focus.ausdauer,
                    "beweglichkeit" to focus.beweglichkeit,
                    "geschicklichkeit" to focus.geschicklichkeit,
                    "mindset" to focus.mindset
                )
            }

            // Combine preferred sports: legacy sports + unified sport name
            val allPreferredSports = buildList {
                addAll(answers.sports.map { it.displayName })
                unifiedSport?.name?.let { add(it) }
            }.distinct()

            // Create updated profile
            val updatedProfile = UserProfile(
                id = userId,
                name = answers.userName ?: existingProfile?.name ?: "Champion",
                weight = answers.currentPRs.bodyweightKg?.toDouble() ?: existingProfile?.weight,
                height = existingProfile?.height,
                age = answers.age ?: existingProfile?.age,
                gender = profileGender,
                activityLevel = activityLevel,
                trainingExperience = answers.trainingYears ?: existingProfile?.trainingExperience,
                personalRecords = personalRecords,
                medicalConditions = existingProfile?.medicalConditions ?: emptyList(),
                injuries = existingProfile?.injuries ?: emptyList(),
                goals = answers.primaryGoals.map { it.name },
                preferredSports = allPreferredSports,
                targetWorkoutDuration = existingProfile?.targetWorkoutDuration,
                profileImageUrl = existingProfile?.profileImageUrl,
                hasCoach = answers.coachSituation == CoachSituation.HAS_COACH,
                lastSeen = System.currentTimeMillis(),
                // New onboarding fields
                goalRecords = goalRecords,
                sessionsPerWeek = answers.trainingCommitment.sessionsPerWeek,
                effortLevel = answers.trainingCommitment.effortLevel,
                experienceLevel = answers.experienceLevel?.name,
                primaryGoals = answers.primaryGoals.map { it.name },
                featureInterests = answers.featureInterests.map { it.name },
                coachSituation = answers.coachSituation?.name,
                onboardingCompletedAt = System.currentTimeMillis(),
                // Unified Sport System fields (up to 3 sports)
                primarySport = unifiedSport?.id,
                secondarySport = answers.secondaryUnifiedSport?.id,
                tertiarySport = answers.tertiaryUnifiedSport?.id,
                primarySportCategory = unifiedSport?.category?.name,
                trainingFocus = trainingFocusMap,
                // Dietary preferences (can be multiple, e.g. Vegan + Halal)
                dietaryPreferences = answers.dietaryPreferences.map { it.name },
                foodAllergies = answers.foodAllergies.map { it.name },
                foodDislikes = answers.foodDislikes,
                customAllergyNote = answers.customAllergyNote,
                // Menopause wellness fields from onboarding
                menopauseStage = answers.menopauseStage?.name?.lowercase(),
                primarySymptoms = answers.primarySymptoms.map { it.name.lowercase() },
                wellnessGoals = answers.menopauseGoals.map { it.name.lowercase() }
            )

            val result = UserProfileRepository.updateProfile(updatedProfile)
            if (result.isSuccess) {
                Log.d(TAG, "✅ Onboarding data saved to UserProfile successfully")

                // Also create MenopauseProfile if menopause data was collected
                if (answers.menopauseStage != null || answers.primarySymptoms.isNotEmpty()) {
                    try {
                        // Map OnboardingMenopauseStage to MenopauseStage enum
                        val stage = when (answers.menopauseStage) {
                            OnboardingMenopauseStage.PREMENOPAUSE -> MenopauseStage.PREMENOPAUSE
                            OnboardingMenopauseStage.EARLY_PERIMENOPAUSE -> MenopauseStage.EARLY_PERIMENOPAUSE
                            OnboardingMenopauseStage.LATE_PERIMENOPAUSE -> MenopauseStage.LATE_PERIMENOPAUSE
                            OnboardingMenopauseStage.MENOPAUSE -> MenopauseStage.MENOPAUSE
                            OnboardingMenopauseStage.POSTMENOPAUSE -> MenopauseStage.POSTMENOPAUSE
                            OnboardingMenopauseStage.UNSURE, null -> MenopauseStage.PREMENOPAUSE
                        }

                        // Map OnboardingSymptom to MenopauseSymptomType
                        val symptoms = answers.primarySymptoms.mapNotNull { symptom ->
                            when (symptom) {
                                OnboardingSymptom.HOT_FLASHES -> MenopauseSymptomType.HOT_FLASH
                                OnboardingSymptom.NIGHT_SWEATS -> MenopauseSymptomType.NIGHT_SWEAT
                                OnboardingSymptom.SLEEP_ISSUES -> MenopauseSymptomType.SLEEP_ISSUE
                                OnboardingSymptom.MOOD_SWINGS -> MenopauseSymptomType.MOOD_SWING
                                OnboardingSymptom.FATIGUE -> MenopauseSymptomType.FATIGUE
                                OnboardingSymptom.BRAIN_FOG -> MenopauseSymptomType.BRAIN_FOG
                                OnboardingSymptom.WEIGHT_GAIN -> MenopauseSymptomType.WEIGHT_GAIN
                                OnboardingSymptom.JOINT_PAIN -> MenopauseSymptomType.JOINT_PAIN
                                OnboardingSymptom.ANXIETY -> MenopauseSymptomType.ANXIETY
                                OnboardingSymptom.LOW_LIBIDO -> MenopauseSymptomType.LOW_LIBIDO
                                OnboardingSymptom.NONE -> null
                            }
                        }

                        MenopauseProfileRepository.saveProfile(
                            userId = userId,
                            stage = stage,
                            lastPeriodDate = null,
                            hrtStatus = HRTStatus.NONE,
                            primarySymptoms = symptoms.ifEmpty { null }
                        )
                        Log.d(TAG, "✅ MenopauseProfile created from onboarding: stage=$stage, symptoms=$symptoms")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to create MenopauseProfile: ${e.message}")
                    }
                }
            } else {
                Log.e(TAG, "❌ Failed to save onboarding data: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving onboarding to UserProfile: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Convert CurrentPRs to PersonalRecord map for UserProfile
     * Now includes universal benchmarks from the new onboarding flow
     */
    private fun convertCurrentPRsToPersonalRecords(answers: OnboardingAnswers): Map<String, PersonalRecord> {
        val records = mutableMapOf<String, PersonalRecord>()
        val currentPRs = answers.currentPRs
        val now = System.currentTimeMillis()

        // Helper to add PR if present (legacy system)
        fun addPR(liftType: LiftType, sport: String) {
            currentPRs.getPR(liftType)?.let { value ->
                records[liftType.displayName] = PersonalRecord(
                    exerciseName = liftType.displayName,
                    value = value.toDouble(),
                    unit = "kg",
                    achievedAt = now,
                    sport = sport
                )
            }
        }

        // Legacy Powerlifting PRs
        addPR(LiftType.SQUAT, "Powerlifting")
        addPR(LiftType.BENCH_PRESS, "Powerlifting")
        addPR(LiftType.DEADLIFT, "Powerlifting")

        // Weightlifting
        addPR(LiftType.SNATCH, "Olympic Weightlifting")
        addPR(LiftType.CLEAN_AND_JERK, "Olympic Weightlifting")

        // Strongman
        addPR(LiftType.LOG_LIFT, "Strongman")
        addPR(LiftType.AXLE_CLEAN_PRESS, "Strongman")
        addPR(LiftType.YOKE_CARRY, "Strongman")
        addPR(LiftType.FARMERS_WALK, "Strongman")
        addPR(LiftType.ATLAS_STONES, "Strongman")

        // CrossFit / General
        addPR(LiftType.FRONT_SQUAT, "CrossFit")
        addPR(LiftType.OVERHEAD_SQUAT, "CrossFit")
        addPR(LiftType.THRUSTER, "CrossFit")
        addPR(LiftType.OVERHEAD_PRESS, "General")

        // NEW: Universal benchmarks from new onboarding flow
        val sportName = answers.unifiedSport?.name ?: "General"
        answers.currentBenchmarks.forEach { (benchmarkId, valueStr) ->
            val valueDouble = valueStr.toDoubleOrNull()
            if (valueDouble != null) {
                // Get benchmark definition to determine unit
                val benchmarkDef = answers.unifiedSport?.let { sport ->
                    SportBenchmarks.getBenchmarksForSport(sport).find { it.id == benchmarkId }
                }
                val unit = benchmarkDef?.unit ?: "kg"
                val name = benchmarkDef?.name ?: benchmarkId.replace("_", " ").replaceFirstChar { it.uppercase() }

                records[name] = PersonalRecord(
                    exerciseName = name,
                    value = valueDouble,
                    unit = unit,
                    achievedAt = now,
                    sport = sportName
                )
            }
        }

        return records
    }

    /**
     * Convert GoalPRs to GoalRecord map for UserProfile
     * Now includes universal goal benchmarks from the new onboarding flow
     */
    private fun convertGoalPRsToGoalRecords(answers: OnboardingAnswers): Map<String, GoalRecord> {
        val records = mutableMapOf<String, GoalRecord>()
        val goalPRs = answers.goalPRs
        val now = System.currentTimeMillis()

        // Helper to add goal if present (legacy system)
        fun addGoal(liftType: LiftType, sport: String) {
            goalPRs.getGoal(liftType)?.let { value ->
                records[liftType.displayName] = GoalRecord(
                    exerciseName = liftType.displayName,
                    targetValue = value.toDouble(),
                    unit = "kg",
                    setAt = now,
                    sport = sport
                )
            }
        }

        // Legacy Powerlifting goals
        addGoal(LiftType.SQUAT, "Powerlifting")
        addGoal(LiftType.BENCH_PRESS, "Powerlifting")
        addGoal(LiftType.DEADLIFT, "Powerlifting")

        // Weightlifting
        addGoal(LiftType.SNATCH, "Olympic Weightlifting")
        addGoal(LiftType.CLEAN_AND_JERK, "Olympic Weightlifting")

        // Strongman
        addGoal(LiftType.LOG_LIFT, "Strongman")
        addGoal(LiftType.AXLE_CLEAN_PRESS, "Strongman")

        // CrossFit / General
        addGoal(LiftType.FRONT_SQUAT, "CrossFit")
        addGoal(LiftType.OVERHEAD_PRESS, "General")

        // NEW: Universal goal benchmarks from new onboarding flow
        val sportName = answers.unifiedSport?.name ?: "General"
        answers.goalBenchmarks.forEach { (benchmarkId, valueStr) ->
            val valueDouble = valueStr.toDoubleOrNull()
            if (valueDouble != null) {
                // Get benchmark definition to determine unit
                val benchmarkDef = answers.unifiedSport?.let { sport ->
                    SportBenchmarks.getBenchmarksForSport(sport).find { it.id == benchmarkId }
                }
                val unit = benchmarkDef?.unit ?: "kg"
                val name = benchmarkDef?.name ?: benchmarkId.replace("_", " ").replaceFirstChar { it.uppercase() }

                records[name] = GoalRecord(
                    exerciseName = name,
                    targetValue = valueDouble,
                    unit = unit,
                    setAt = now,
                    sport = sportName
                )
            }
        }

        return records
    }

    /**
     * DEV MODE: Skip onboarding entirely with default persona
     */
    fun devSkipOnboarding() {
        Log.d(TAG, "DEV MODE: Skipping onboarding")

        val defaultPersona = PersonaEngine.createDefaultPersona()

        // Set isOnboardingComplete IMMEDIATELY (before async) so UI updates right away
        _isOnboardingComplete.value = true

        _state.value = _state.value.copy(
            currentStep = OnboardingStep.COMPLETE,
            persona = defaultPersona,
            isComplete = true,
            skippedViaDevMode = true
        )

        // Save to DataStore async (doesn't block UI)
        viewModelScope.launch {
            val ctx = context ?: return@launch

            // Enable guest mode so isLoggedIn returns true
            ctx.authDataStore.edit { prefs ->
                prefs[GUEST_MODE_KEY] = "true"
            }
            Log.d(TAG, "DEV MODE: Guest mode enabled")

            ctx.onboardingDataStore.edit { prefs ->
                prefs[KEY_ONBOARDING_COMPLETE] = true
                prefs[KEY_PERSONA_TYPE] = PersonaType.COMPLETE_ATHLETE.name
                prefs[KEY_NUTRITION_ENABLED] = true
            }
            Log.d(TAG, "DEV MODE: Onboarding skipped and saved to DataStore")
        }
    }

    /**
     * Reset onboarding (for testing)
     */
    fun resetOnboarding() {
        viewModelScope.launch {
            val ctx = context ?: return@launch
            ctx.onboardingDataStore.edit { prefs ->
                prefs.clear()
            }

            _state.value = OnboardingState()
            _isOnboardingComplete.value = false

            Log.d(TAG, "Onboarding reset")
        }
    }

    /**
     * Skip paywall (continue with free tier) - goes to registration
     */
    fun skipPaywall() {
        goToNextStep() // Goes to REGISTRATION
    }

    // ==================== Registration ====================

    private val _registrationState = MutableStateFlow(RegistrationState())
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    /**
     * Register a new user with name, email, and password
     */
    fun registerUser(name: String, email: String, password: String) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState(isLoading = true)

            try {
                val result = authRepository?.signUpWithEmail(email, password)

                if (result?.isSuccess == true) {
                    // Save the user's name to answers for profile creation
                    _state.value = _state.value.copy(
                        answers = _state.value.answers.copy(userName = name)
                    )

                    // Update user profile with name
                    val userId = authRepository?.currentUserId?.first()
                    if (userId != null) {
                        UserProfileRepository.updateProfile(
                            com.example.menotracker.data.models.UserProfile(
                                id = userId,
                                name = name
                            )
                        )
                    }

                    _registrationState.value = RegistrationState(isSuccess = true)
                    Log.d(TAG, "✅ User registered successfully: $email")
                    goToNextStep() // Go to NOTIFICATIONS
                } else {
                    val error = result?.exceptionOrNull()?.message ?: "Registration failed"

                    // Check if account was created but needs email confirmation
                    if (error.contains("Account created")) {
                        Log.d(TAG, "📧 Account created - email confirmation required")

                        // Save onboarding data locally for later sync after email verification
                        savePendingOnboardingData(name)

                        _registrationState.value = RegistrationState(
                            isSuccess = true,
                            needsEmailConfirmation = true
                        )
                    } else {
                        _registrationState.value = RegistrationState(error = error)
                        Log.e(TAG, "❌ Registration failed: $error")
                    }
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState(error = e.message ?: "Registration failed")
                Log.e(TAG, "❌ Registration exception: ${e.message}")
            }
        }
    }

    /**
     * Clear registration error
     */
    fun clearRegistrationError() {
        _registrationState.value = _registrationState.value.copy(error = null)
    }

    /**
     * Save onboarding data locally when email confirmation is required.
     * This data will be synced to cloud after user verifies email and logs in.
     */
    private suspend fun savePendingOnboardingData(userName: String) {
        val ctx = context ?: return
        val persona = _state.value.persona ?: PersonaEngine.calculatePersona(_state.value.answers)
        val answers = _state.value.answers

        Log.d(TAG, "💾 Saving pending onboarding data for user: $userName")

        ctx.onboardingDataStore.edit { prefs ->
            // Mark as complete locally but pending cloud sync
            prefs[KEY_ONBOARDING_COMPLETE] = true
            prefs[KEY_PENDING_CLOUD_SYNC] = true
            prefs[KEY_PENDING_USER_NAME] = userName

            // Save persona
            prefs[KEY_PERSONA_TYPE] = persona.type.name
            prefs[KEY_NUTRITION_ENABLED] = persona.nutritionEnabled

            // Save answers
            if (answers.primaryGoals.isNotEmpty()) {
                prefs[KEY_PRIMARY_GOAL] = answers.primaryGoals.joinToString(",") { it.name }
            }
            answers.experienceLevel?.let { prefs[KEY_EXPERIENCE_LEVEL] = it.name }
            answers.coachSituation?.let { prefs[KEY_COACH_SITUATION] = it.name }

            if (answers.sports.isNotEmpty()) {
                prefs[KEY_SPORTS] = answers.sports.joinToString(",") { it.name }
            }

            if (answers.featureInterests.isNotEmpty()) {
                prefs[KEY_FEATURE_INTERESTS] = answers.featureInterests.joinToString(",") { it.name }
            }
        }

        Log.d(TAG, "✅ Pending onboarding data saved - will sync after email verification")
    }

    /**
     * Skip notifications setup
     */
    fun skipNotifications() {
        goToNextStep()
    }

    /**
     * Check if user entered detailed flow (via "Add details" link)
     */
    private fun isDetailedFlow(): Boolean {
        return _state.value.answers.currentBenchmarks.isNotEmpty() ||
               _state.value.answers.gender != null
    }

    /**
     * Get the total number of steps for progress indicator
     * MENOTRACKER FLOW: 4 steps (MenopauseStage, Symptoms, Goals, Dietary)
     */
    fun getTotalSteps(): Int {
        return 4 // Menopause Stage, Symptoms, Goals, Dietary
    }

    /**
     * Get current step number (1-based) for progress indicator
     * MENOTRACKER FLOW: Welcome → Stage → Symptoms → Goals → Dietary → Paywall flow
     */
    fun getCurrentStepNumber(): Int {
        return when (_state.value.currentStep) {
            OnboardingStep.WELCOME -> 0
            OnboardingStep.MENOPAUSE_STAGE -> 1
            OnboardingStep.PRIMARY_SYMPTOMS -> 2
            OnboardingStep.MENOPAUSE_GOALS -> 3
            OnboardingStep.DIETARY_PREFERENCES -> 4
            // End flow screens (no progress indicator)
            else -> 4
        }
    }
}
