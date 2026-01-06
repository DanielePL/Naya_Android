package com.example.menotracker.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.menotracker.ui.theme.NayaPrimary
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.onboarding.data.*
import com.example.menotracker.onboarding.screens.*
import com.example.menotracker.onboarding.data.Gender

/**
 * Menotracker Onboarding Flow
 *
 * MENOPAUSE-FOCUSED FLOW:
 * 1. Welcome
 * 2. Menopause Stage (Prämenopause → Postmenopause)
 * 3. Primary Symptoms (Hitzewallungen, Schlafprobleme, etc.)
 * 4. Menopause Goals (Symptome lindern, Knochengesundheit, etc.)
 * 5. Dietary Preferences (important for menopause nutrition)
 * 6. Feature Promo → Paywall → Registration → Done!
 */
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onLogin: () -> Unit
) {
    val viewModel: OnboardingViewModel = viewModel()
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Track if user wants to add detailed benchmarks
    var showDetailedBenchmarks by remember { mutableStateOf(false) }

    // Initialize ViewModel with context
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    // Check for completion
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            onComplete()
        }
    }

    // Animated content transitions
    AnimatedContent(
        targetState = state.currentStep,
        transitionSpec = {
            // Slide animation between screens
            if (targetState.ordinal > initialState.ordinal) {
                // Moving forward
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn() togetherWith slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                ) + fadeOut()
            } else {
                // Moving backward
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300)
                ) + fadeIn() togetherWith slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut()
            }
        },
        label = "OnboardingScreenTransition"
    ) { step ->
        when (step) {
            // ==================== MENOTRACKER CORE FLOW ====================

            OnboardingStep.WELCOME -> {
                WelcomeScreen(
                    onGetStarted = { viewModel.goToNextStep() },
                    onLogin = onLogin,
                    onDevSkip = {
                        viewModel.devSkipOnboarding()
                    }
                )
            }

            // ==================== MENOPAUSE-SPECIFIC SCREENS ====================

            OnboardingStep.MENOPAUSE_STAGE -> {
                MenopauseStageScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    selectedStage = state.answers.menopauseStage,
                    onStageSelected = { stage -> viewModel.setMenopauseStage(stage) },
                    onBack = { viewModel.goToPreviousStep() },
                    onContinue = { viewModel.goToNextStep() }
                )
            }

            OnboardingStep.PRIMARY_SYMPTOMS -> {
                PrimarySymptomsScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    selectedSymptoms = state.answers.primarySymptoms,
                    onSymptomToggled = { symptom -> viewModel.toggleSymptom(symptom) },
                    onBack = { viewModel.goToPreviousStep() },
                    onContinue = { viewModel.goToNextStep() }
                )
            }

            OnboardingStep.MENOPAUSE_GOALS -> {
                MenopauseGoalsScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    selectedGoals = state.answers.menopauseGoals,
                    onGoalToggled = { goal -> viewModel.toggleMenopauseGoal(goal) },
                    onBack = { viewModel.goToPreviousStep() },
                    onContinue = { viewModel.goToNextStep() }
                )
            }

            // ==================== LEGACY SCREENS (Auto-skip for Menotracker) ====================

            OnboardingStep.GOAL_SELECTION -> {
                // Skip - replaced by MENOPAUSE_GOALS
                LaunchedEffect(Unit) { viewModel.goToNextStep() }
            }

            OnboardingStep.SPORT_SELECTION -> {
                // Skip - not relevant for Menotracker
                LaunchedEffect(Unit) { viewModel.goToNextStep() }
            }

            OnboardingStep.EXPERIENCE_LEVEL -> {
                // Skip - not relevant for Menotracker
                LaunchedEffect(Unit) { viewModel.goToNextStep() }
            }

            // Dietary Preferences (personalized nutrition tips - multi-select)
            OnboardingStep.DIETARY_PREFERENCES -> {
                DietaryPreferencesScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    selectedDiets = state.answers.dietaryPreferences,
                    selectedAllergies = state.answers.foodAllergies,
                    foodDislikes = state.answers.foodDislikes,
                    customAllergyNote = state.answers.customAllergyNote,
                    onToggleDiet = { diet -> viewModel.toggleDietaryPreference(diet) },
                    onToggleAllergy = { allergy -> viewModel.toggleFoodAllergy(allergy) },
                    onUpdateDislikes = { dislikes -> viewModel.updateFoodDislikes(dislikes) },
                    onUpdateCustomAllergyNote = { note -> viewModel.updateCustomAllergyNote(note) },
                    onContinue = { viewModel.goToNextStep() },
                    onSkip = { viewModel.skipDietaryPreferences() },
                    onBack = { viewModel.goToPreviousStep() }
                )
            }

            // Training Commitment (simplified - just frequency)
            OnboardingStep.TRAINING_COMMITMENT -> {
                TrainingCommitmentScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    commitment = state.answers.trainingCommitment,
                    onUpdateCommitment = { commitment -> viewModel.setTrainingCommitment(commitment) },
                    onContinue = { viewModel.goToNextStep() },
                    onBack = { viewModel.goToPreviousStep() }
                )
            }

            // ==================== OPTIONAL DETAILED FLOW ====================
            // Only shown if user clicks "Add details" or selects Competitive level

            OnboardingStep.GENDER_SELECTION -> {
                GenderSelectionScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    onGenderSelected = { gender -> viewModel.setGender(gender) },
                    onBack = { viewModel.goToPreviousStep() }
                )
            }

            OnboardingStep.AGE_EXPERIENCE -> {
                AgeExperienceScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    initialAge = state.answers.age,
                    initialTrainingYears = state.answers.trainingYears,
                    onContinue = { age, trainingYears ->
                        viewModel.setAgeAndTrainingYears(age, trainingYears)
                    },
                    onBack = { viewModel.goToPreviousStep() }
                )
            }

            // Sport Benchmarks (optional - for power users)
            OnboardingStep.SPORT_BENCHMARKS -> {
                val selectedSport = state.answers.unifiedSport

                if (selectedSport != null && SportBenchmarks.shouldShowBenchmarks(selectedSport)) {
                    SportBenchmarksScreen(
                        currentStep = viewModel.getCurrentStepNumber(),
                        totalSteps = viewModel.getTotalSteps(),
                        selectedSport = selectedSport,
                        currentBenchmarks = state.answers.currentBenchmarks,
                        onUpdateBenchmarks = { benchmarks -> viewModel.setCurrentBenchmarks(benchmarks) },
                        onContinue = { viewModel.goToNextStep() },
                        onSkip = { viewModel.skipBenchmarks() },
                        onBack = { viewModel.goToPreviousStep() }
                    )
                } else {
                    LaunchedEffect(Unit) { viewModel.goToNextStep() }
                }
            }

            // Goal Benchmarks (optional - for power users)
            OnboardingStep.GOAL_BENCHMARKS -> {
                val selectedSport = state.answers.unifiedSport

                if (selectedSport != null && state.answers.currentBenchmarks.isNotEmpty()) {
                    GoalBenchmarksScreen(
                        currentStep = viewModel.getCurrentStepNumber(),
                        totalSteps = viewModel.getTotalSteps(),
                        selectedSport = selectedSport,
                        currentBenchmarks = state.answers.currentBenchmarks,
                        goalBenchmarks = state.answers.goalBenchmarks,
                        trainingYears = state.answers.trainingYears,
                        age = state.answers.age,
                        onUpdateGoalBenchmarks = { goals -> viewModel.setGoalBenchmarks(goals) },
                        onContinue = { viewModel.goToNextStep() },
                        onSkip = { viewModel.skipGoalBenchmarks() },
                        onBack = { viewModel.goToPreviousStep() }
                    )
                } else {
                    LaunchedEffect(Unit) { viewModel.goToNextStep() }
                }
            }

            // ==================== LEGACY STEPS (Auto-skip) ====================

            OnboardingStep.CURRENT_PRS -> {
                LaunchedEffect(Unit) { viewModel.goToNextStep() }
            }

            OnboardingStep.GOAL_PRS -> {
                LaunchedEffect(Unit) { viewModel.goToNextStep() }
            }

            OnboardingStep.PROMISE_COMMITMENT -> {
                // Skip promise commitment for simplified flow
                // Only show for competitive athletes who entered detailed PRs
                if (state.answers.currentBenchmarks.isNotEmpty() &&
                    state.answers.experienceLevel == ExperienceLevel.ELITE) {
                    PromiseCommitmentScreen(
                        currentPRs = state.answers.currentPRs,
                        goalPRs = state.answers.goalPRs,
                        commitment = state.answers.trainingCommitment,
                        gender = state.answers.gender ?: Gender.MALE,
                        experienceLevel = state.answers.experienceLevel,
                        age = state.answers.age,
                        trainingYears = state.answers.trainingYears,
                        onCommit = { viewModel.confirmPromise() },
                        onBack = { viewModel.goToPreviousStep() }
                    )
                } else {
                    LaunchedEffect(Unit) { viewModel.goToNextStep() }
                }
            }

            // ==================== END FLOW (Everyone) ====================

            OnboardingStep.FEATURE_INTEREST -> {
                FeatureInterestScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    selectedInterests = state.answers.featureInterests,
                    onToggleInterest = { interest -> viewModel.toggleFeatureInterest(interest) },
                    onContinue = { viewModel.confirmFeatureInterests() },
                    onBack = { viewModel.goToPreviousStep() }
                )
            }

            OnboardingStep.COACH_SITUATION -> {
                CoachSituationScreen(
                    currentStep = viewModel.getCurrentStepNumber(),
                    totalSteps = viewModel.getTotalSteps(),
                    onSituationSelected = { situation -> viewModel.setCoachSituation(situation) },
                    onBack = { viewModel.goToPreviousStep() }
                )
            }

            OnboardingStep.FEATURE_PROMO -> {
                val persona = state.persona ?: PersonaEngine.createDefaultPersona()
                FeaturePromoScreen(
                    featureVideos = persona.featureVideos,
                    personaType = persona.type,
                    onContinue = { viewModel.goToNextStep() },
                    onSkip = { viewModel.goToNextStep() },
                    comboVideo = persona.comboVideo,
                    selectedFeatures = persona.selectedFeatures
                )
            }

            OnboardingStep.PAYWALL -> {
                val persona = state.persona ?: PersonaEngine.createDefaultPersona()
                PaywallScreen(
                    personaType = persona.type,
                    benefits = persona.paywallBenefits,
                    onSubscribe = { viewModel.skipPaywall() },
                    onContinueFree = { viewModel.skipPaywall() },
                    onRestorePurchase = { viewModel.skipPaywall() }
                )
            }

            OnboardingStep.REGISTRATION -> {
                val registrationState by viewModel.registrationState.collectAsState()

                RegistrationScreen(
                    isLoading = registrationState.isLoading,
                    errorMessage = registrationState.error,
                    successMessage = if (registrationState.needsEmailConfirmation)
                        "Almost done!"
                        else null,
                    showLoginButton = false,  // Don't show button - email link will auto-login
                    onRegister = { name, email, password ->
                        viewModel.registerUser(name, email, password)
                    },
                    onBack = { viewModel.goToPreviousStep() },
                    onGoToLogin = onLogin
                )
            }

            OnboardingStep.NOTIFICATIONS -> {
                val persona = state.persona
                NotificationSetupScreen(
                    showNutritionReminder = persona?.nutritionEnabled ?: true,
                    onEnable = { _, _, _ ->
                        // TODO: Request notification permission and save preferences
                        viewModel.goToNextStep()
                    },
                    onSkip = { viewModel.skipNotifications() }
                )
            }

            OnboardingStep.COMPLETE -> {
                // This should trigger the LaunchedEffect above
                // Show loading while completing
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NayaPrimary)
                }
            }
        }
    }
}