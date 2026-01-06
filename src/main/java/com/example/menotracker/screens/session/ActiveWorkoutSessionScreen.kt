// File: android/app/src/main/java/com/example/myapplicationtest/screens/session/ActiveWorkoutSessionScreen.kt

package com.example.menotracker.screens.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.ExerciseSet
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.viewmodels.WorkoutTemplate
import com.example.menotracker.viewmodels.WorkoutSessionViewModel
import com.example.menotracker.viewmodels.WorkoutTimerViewModel
import com.example.menotracker.viewmodels.WorkoutTimerData
import com.example.menotracker.viewmodels.RestTimerViewModel
import com.example.menotracker.viewmodels.TimerData
import com.example.menotracker.viewmodels.TimerState
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.WorkoutSessionRepository
import com.example.menotracker.data.StatisticsRepository
import com.example.menotracker.data.WorkoutPatternRepository
import com.example.menotracker.data.PreWorkoutNutritionService
import com.example.menotracker.data.models.PRType
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.menotracker.debug.DebugLogger
import com.example.menotracker.community.components.EnhancedShareWorkoutDialog
import com.example.menotracker.community.components.ShareableVideo
import com.example.menotracker.community.components.shouldShowShareDialog
import java.util.Locale
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaBackground
import com.example.menotracker.data.models.UserStrengthProfile
import com.example.menotracker.data.models.getWeightRecommendation
import com.example.menotracker.data.models.WeightRecommendation
import com.example.menotracker.screens.session.components.ExerciseSection
import com.example.menotracker.screens.session.components.RestTimerCard
import com.example.menotracker.screens.session.components.WorkoutPausedCard
import com.example.menotracker.screens.session.components.WorkoutStartScreen
import com.example.menotracker.screens.session.components.WorkoutCompletionScreen
import com.example.menotracker.screens.session.components.ExercisePickerDialog
import com.example.menotracker.screens.session.components.SaveTemplateDialog
import com.example.menotracker.screens.session.components.ExerciseInfo
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.menotracker.billing.Feature
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.screens.session.components.WodTimerCard
import com.example.menotracker.screens.session.components.TimerSelectionCard
import com.example.menotracker.screens.session.components.ILBTestWeekBanner
import com.example.menotracker.screens.session.components.ILBSessionSummary
import com.example.menotracker.data.ILBService
import com.example.menotracker.data.models.ILBTestResult
import com.example.menotracker.data.models.ILBCalculator
import com.example.menotracker.viewmodels.SetType
import com.example.menotracker.data.SettingsDataStore
import com.example.menotracker.data.AppReviewManager
import com.example.menotracker.ui.composables.AppRatingDialog
import android.app.Activity

// 🎨 Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)
private val cardBorder = orangeGlow.copy(alpha = 0.5f)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF1E1E1E), NayaBackground, Color(0xFF1a1410))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutSessionScreen(
    workout: WorkoutTemplate,
    workoutSessionViewModel: com.example.menotracker.viewmodels.WorkoutSessionViewModel,
    workoutTimerViewModel: WorkoutTimerViewModel,
    restTimerViewModel: RestTimerViewModel,
    accountViewModel: com.example.menotracker.viewmodels.AccountViewModel,
    onNavigateBack: (totalTimeSeconds: Int) -> Unit,
    onNavigateToVBT: (String, Int) -> Unit, // exerciseId, setNumber
    onNavigateToVideoPlayer: (String, com.example.menotracker.viewmodels.VelocityMetricsData?) -> Unit, // videoPath, metrics
    onNavigateToPaywall: () -> Unit = {} // For upgrade prompts
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val workoutSessionRepository = remember { WorkoutSessionRepository(SupabaseClient.client) }

    // Workout pattern learning for smart nutrition timing
    val workoutPatternRepository = remember { WorkoutPatternRepository.getInstance(context) }
    val preWorkoutNutritionService = remember { PreWorkoutNutritionService.getInstance(context) }

    var currentExerciseIndex by remember { mutableStateOf(0) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }

    // Workout start state - timer only starts after user clicks START in TopAppBar
    var hasStarted by remember { mutableStateOf(false) }

    // ✅ UNIQUE SESSION ID - Use ViewModel's session ID if available (survives navigation)
    // This prevents clearing data when returning from camera
    val activeSessionId by workoutSessionViewModel.activeSessionId.collectAsState()
    val uniqueSessionId = remember(activeSessionId) {
        activeSessionId ?: "${workout.id}_${System.currentTimeMillis()}"
    }

    // 🔄 MUTABLE EXERCISES - Allow modifications during workout
    val mutableExercises = remember { mutableStateListOf<ExerciseWithSets>().apply { addAll(workout.exercises) } }
    var workoutModified by remember { mutableStateOf(false) } // Track if user made changes
    var showSwapExerciseDialog by remember { mutableStateOf<Int?>(null) } // Index of exercise to swap
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var finalWorkoutTimeSeconds by remember { mutableStateOf(0) } // Store final time for navigation after save dialog
    var showShareDialog by remember { mutableStateOf(false) } // Community share dialog after workout completion
    var isSharingToCommunity by remember { mutableStateOf(false) } // Loading state for community share
    var showRatingDialog by remember { mutableStateOf(false) } // App rating dialog after workout

    // ⭐ APP RATING - Check if we should show rating prompt after workout
    val settingsDataStore = remember { SettingsDataStore(context) }
    val reviewState by settingsDataStore.reviewState.collectAsState(initial = com.example.menotracker.data.ReviewState())
    val appReviewManager = remember(context) {
        (context as? Activity)?.let { AppReviewManager(it, settingsDataStore) }
    }

    // ⏱️ TIMER SELECTION - User can add timer to ANY workout (not just WODs)
    var selectedTimerConfig by remember { mutableStateOf(workout.wodTimerConfig) }

    // 📊 ILB (Individuelles Leistungsbild) - Periodized strength testing
    val ilbService = remember { ILBService.getInstance(context) }
    var isILBTestWeek by remember { mutableStateOf(workout.isILBTestWeek) }
    var showILBBanner by remember { mutableStateOf(workout.isILBTestWeek) }
    var showILBSummary by remember { mutableStateOf(false) }
    val ilbTestResults = remember { mutableStateListOf<ILBTestResult>() }

    // Helper function to handle post-workout flow (rating → share → navigate)
    val finishWorkoutFlow: () -> Unit = {
        // Increment compeleted workouts counter for rating trigger
        coroutineScope.launch {
            appReviewManager?.onWorkoutCompleted()

            // Get fresh review state AFTER incrementing (use first() to get current value)
            val currentReviewState = settingsDataStore.reviewState.first()
            android.util.Log.d("AppReview", "⭐ Review check - workouts: ${currentReviewState.completedWorkouts}, hasRated: ${currentReviewState.hasRated}, shouldShow: ${currentReviewState.shouldShowReviewPrompt()}")

            // Check if we should show rating dialog
            if (currentReviewState.shouldShowReviewPrompt()) {
                android.util.Log.d("AppReview", "✅ Showing rating dialog!")
                showRatingDialog = true
            } else if (shouldShowShareDialog()) {
                showShareDialog = true
            } else {
                onNavigateBack(finalWorkoutTimeSeconds)
            }
        }
    }

    // Statistics tracking
    var totalPRsAchieved by remember { mutableStateOf(0) }
    var prExerciseNames by remember { mutableStateOf(listOf<String>()) }
    var workoutStartTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // ✅ SINGLE SOURCE OF TRUTH: Only setDataMap from ViewModel
    val setDataMap by workoutSessionViewModel.setDataMap.collectAsState()
    val workoutTimer by workoutTimerViewModel.workoutTimer.collectAsStateWithLifecycle()
    val timerData by restTimerViewModel.timerData.collectAsStateWithLifecycle()
    val userProfile by accountViewModel.userProfile.collectAsState()
    val strengthProfile by accountViewModel.strengthProfile.collectAsState()

    // Debug: Log setDataMap changes to track video path updates
    LaunchedEffect(setDataMap) {
        android.util.Log.d("ActiveWorkoutScreen", "📊 setDataMap changed! Total entries: ${setDataMap.size}")
        setDataMap.forEach { (setId, data) ->
            android.util.Log.d("ActiveWorkoutScreen", "  📹 $setId -> videoPath=${data.videoPath}, completed=${data.isCompleted}")
        }
    }

    // Load strength profile for weight recommendations
    LaunchedEffect(Unit) {
        accountViewModel.loadStrengthProfile()
    }

    // 🔧 DEBUG: Reset review state for testing - REMOVE AFTER TESTING!
    LaunchedEffect(Unit) {
        settingsDataStore.resetReviewStateForTesting()
        android.util.Log.d("AppReview", "🔧 DEBUG: Review state reset - will show after this workout!")
    }

    // Log screen load (but don't start timer yet)
    LaunchedEffect(Unit) {
        android.util.Log.d("ActiveWorkoutScreen", "🚀🚀🚀 ACTIVE_WORKOUT_SESSION_SCREEN LOADED 🚀🚀🚀")
        android.util.Log.d("ActiveWorkoutScreen", "📁 File: screens/session/ActiveWorkoutSessionScreen.kt")
        android.util.Log.d("ActiveWorkoutScreen", "🏋️ Workout: ${workout.name} (ID: ${workout.id})")
        android.util.Log.d("ActiveWorkoutScreen", "⏸️ Waiting for user to press START...")
    }

    // Start workout timer and create session ONLY when user clicks START
    LaunchedEffect(hasStarted) {
        if (hasStarted) {
            android.util.Log.d("ActiveWorkoutScreen", "▶️ USER PRESSED START - Beginning workout!")

            val targetDuration = userProfile?.targetWorkoutDuration
            workoutTimerViewModel.startWorkoutTimer(targetDuration)

            // Create workout session in database
            val userId = userProfile?.id ?: "00000000-0000-0000-0000-000000000001" // Fallback to temp user
            workoutSessionRepository.createWorkoutSession(
                sessionId = workout.id,
                workoutName = workout.name,
                userId = userId
            ).onSuccess {
                android.util.Log.d("WorkoutSession", "✅ Workout session created: ${workout.name}")
            }.onFailure { error ->
                android.util.Log.e("WorkoutSession", "❌ Failed to create session: ${error.message}", error)
            }

            // Track workout start for pattern learning (smart nutrition timing)
            workoutPatternRepository.startWorkout(
                workoutType = "STRENGTH",
                wasFasted = false, // Could check last meal time if available
                preWorkoutMealTimeMillis = null
            )
            android.util.Log.d("WorkoutPattern", "📊 Workout started - tracking for pattern learning")
        }
    }

    // ✅ Setup vibration callbacks for rest timer
    LaunchedEffect(Unit) {
        restTimerViewModel.onTimerComplete = {
            vibratePattern(vibrator, longArrayOf(0, 500, 200, 500))
        }
        restTimerViewModel.onTick = { remaining ->
            if (remaining in 1..3) {
                vibratePattern(vibrator, longArrayOf(0, 100))
            }
        }
    }

    // ✅ FRESH START - Clear data only when switching to a DIFFERENT workout
    // This prevents clearing when returning from camera (same workout)
    // Track with rememberSaveable to persist across navigation (survives back-stack changes)
    var lastClearedWorkoutId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(workout.id) {
        // Only clear if this is a DIFFERENT workout than last time we cleared
        if (lastClearedWorkoutId != workout.id) {
            android.util.Log.d("WorkoutSession", "🆕 Starting FRESH workout: ${workout.id} (previous: $lastClearedWorkoutId)")

            // Clear ViewModel to ensure fresh state for NEW workout
            workoutSessionViewModel.clearSetData()
            lastClearedWorkoutId = workout.id

            android.util.Log.d("WorkoutSession", "✅ Ready for new workout - all sets unchecked!")
        } else {
            android.util.Log.d("WorkoutSession", "📷 Same workout ${workout.id} - preserving all data")
        }
    }

    // ✅ Load LAST USED WEIGHTS from previous workouts and pre-fill
    LaunchedEffect(workout.id) {
        val userId = userProfile?.id ?: return@LaunchedEffect
        android.util.Log.d("WorkoutSession", "📊 Loading last used weights for exercises...")

        // Get all exercise IDs in this workout
        val exerciseIds = workout.exercises.map { it.exerciseId }

        // Try to load from workout_sets first (most recent actual performance)
        StatisticsRepository.getLastUsedWeights(userId, exerciseIds)
            .onSuccess { lastWeights ->
                android.util.Log.d("WorkoutSession", "✅ Found last weights for ${lastWeights.size} exercises")

                // Update mutableExercises with last used weights
                lastWeights.forEach { (exerciseId, performance) ->
                    val exerciseIndex = mutableExercises.indexOfFirst { it.exerciseId == exerciseId }
                    if (exerciseIndex >= 0) {
                        val exercise = mutableExercises[exerciseIndex]
                        // Update all sets to use the last weight
                        val updatedSets = exercise.sets.map { set ->
                            set.copy(targetWeight = performance.lastWeightKg)
                        }
                        mutableExercises[exerciseIndex] = exercise.copy(sets = updatedSets)
                        android.util.Log.d("WorkoutSession", "📝 Pre-filled ${exercise.exerciseName}: ${performance.lastWeightKg}kg")
                    }
                }
            }
            .onFailure { error ->
                android.util.Log.w("WorkoutSession", "⚠️ Could not load last weights: ${error.message}")

                // Fallback: Try to load from exercise_statistics (PR data)
                StatisticsRepository.getLastPerformanceFromStats(userId, exerciseIds)
                    .onSuccess { prWeights ->
                        android.util.Log.d("WorkoutSession", "✅ Fallback: Found PR weights for ${prWeights.size} exercises")
                        prWeights.forEach { (exerciseId, performance) ->
                            val exerciseIndex = mutableExercises.indexOfFirst { it.exerciseId == exerciseId }
                            if (exerciseIndex >= 0) {
                                val exercise = mutableExercises[exerciseIndex]
                                val updatedSets = exercise.sets.map { set ->
                                    set.copy(targetWeight = performance.lastWeightKg)
                                }
                                mutableExercises[exerciseIndex] = exercise.copy(sets = updatedSets)
                                android.util.Log.d("WorkoutSession", "📝 Pre-filled from PR ${exercise.exerciseName}: ${performance.lastWeightKg}kg")
                            }
                        }
                    }
            }
    }

    // 🔥 DEC24: Guard against empty exercises list
    if (workout.exercises.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No exercises in workout")
        }
        return
    }

    val currentExercise = workout.exercises[currentExerciseIndex.coerceIn(0, workout.exercises.lastIndex)]

    // ✅ Calculate currentSetIndex based on setDataMap (persistent!)
    val currentSetIndex = currentExercise.sets.indices.firstOrNull { index ->
        val setId = "${workout.id}_${currentExercise.exerciseId}_set${index + 1}"
        val setData = setDataMap[setId]
        !(setData?.isCompleted ?: false)
    } ?: -1

    val isWorkoutComplete = currentExercise.sets.indices.all { index ->
        val setId = "${workout.id}_${currentExercise.exerciseId}_set${index + 1}"
        val setData = setDataMap[setId]
        setData?.isCompleted ?: false
    }

    // ✅ LOAD video URLs and VBT metrics from database
    // First load immediately, then poll every 15 seconds for updates
    LaunchedEffect(activeSessionId) {
        // Wait for activeSessionId to be set
        val sessionId = activeSessionId
        if (sessionId == null) {
            android.util.Log.d("WorkoutSession", "⏳ Waiting for activeSessionId before loading...")
            return@LaunchedEffect
        }

        // Generate set IDs for reference (using workout.id for UI keys)
        val allSetIds = mutableExercises.flatMap { exercise ->
            exercise.sets.indices.map { setIndex ->
                "${workout.id}_${exercise.exerciseId}_set${setIndex + 1}"
            }
        }

        android.util.Log.d("WorkoutSession", "📊 Loading VBT data - sessionId=$sessionId, workoutId=${workout.id}")

        // Helper function to load set data
        suspend fun loadSetData(isInitial: Boolean = false) {
            try {
                // Pass sessionId for DB query and workoutId for key mapping
                val setsData = workoutSessionRepository.getWorkoutSetsData(
                    setIds = allSetIds,
                    sessionId = sessionId,
                    workoutId = workout.id
                )
                setsData.onSuccess { dataMap ->
                    var dataLoaded = 0
                    dataMap.forEach { (setId, dto) ->
                        // Keys are already mapped to workout.id by repository
                        val existing = setDataMap[setId]
                        val hasNewVideo = dto.videoUrl != null && existing?.videoPath == null
                        val hasNewMetrics = dto.velocityMetrics != null && existing?.velocityMetrics == null
                        if (hasNewVideo || hasNewMetrics) {
                            workoutSessionViewModel.loadSetDataFromDTO(setId, dto)
                            dataLoaded++
                        }
                    }
                    if (dataLoaded > 0) {
                        val prefix = if (isInitial) "🚀 Initial load:" else "🔄 Poll:"
                        android.util.Log.d("WorkoutSession", "$prefix Loaded data for $dataLoaded sets (videos + VBT metrics)")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("WorkoutSession", "⚠️ Failed to load set data: ${e.message}")
            }
        }

        // IMMEDIATE load on screen open - don't wait for first poll!
        android.util.Log.d("WorkoutSession", "🚀 Loading saved videos immediately...")
        loadSetData(isInitial = true)

        // Then poll every 15 seconds for VBT analysis updates
        while (true) {
            kotlinx.coroutines.delay(15000)
            loadSetData()
        }
    }

    // Pause timer when finish dialog shows (celebration screen)
    LaunchedEffect(showFinishDialog) {
        if (showFinishDialog) {
            workoutTimerViewModel.pauseWorkoutTimer()
            android.util.Log.d("WorkoutSession", "⏸️ Timer paused for celebration screen")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Always show workout - START button in TopAppBar controls timer start
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            workout.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        if (hasStarted) {
                            // WORKOUT TIMER - Dominant, turns red when exceeding target
                            Text(
                                text = workoutTimer.formatTime(),
                                fontSize = 24.sp,
                                color = if (workoutTimer.isExceedingTarget()) Color.Red else orangeGlow,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // PLAY/PAUSE Button
                            IconButton(onClick = {
                                if (workoutTimer.isPaused) {
                                    workoutTimerViewModel.resumeWorkoutTimer()
                                } else {
                                    workoutTimerViewModel.pauseWorkoutTimer()
                                }
                            }) {
                                Icon(
                                    imageVector = if (workoutTimer.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (workoutTimer.isPaused) "Resume" else "Pause",
                                    tint = if (workoutTimer.isPaused) orangeGlow else textWhite
                                )
                            }

                            // FINISH Button
                            TextButton(
                                onClick = {
                                    if (currentExerciseIndex < workout.exercises.size - 1) {
                                        currentExerciseIndex++
                                    } else {
                                        showFinishDialog = true
                                    }
                                }
                            ) {
                                Text(
                                    if (currentExerciseIndex < workout.exercises.size - 1) "SKIP" else "FINISH",
                                    color = orangeGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = textWhite
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout paused overlay
                if (workoutTimer.isPaused) {
                    item {
                        WorkoutPausedCard(
                            onResume = { workoutTimerViewModel.resumeWorkoutTimer() }
                        )
                    }
                }

                // ⏱️ Smart Start Card - Timer Selection + START Button combined
                // Shows before workout starts, disappears after
                item(key = "timer_selection") {
                    TimerSelectionCard(
                        currentTimerConfig = selectedTimerConfig,
                        isWorkoutStarted = hasStarted,
                        onTimerSelected = { config ->
                            selectedTimerConfig = config
                            android.util.Log.d("TimerSelection", "⏱️ Timer selected: ${config?.wodType ?: "none"}")
                        },
                        onStartWorkout = {
                            hasStarted = true
                            // ILB: Start test session if this is a test week
                            if (isILBTestWeek) {
                                ilbService.startTestSession()
                                android.util.Log.d("ILB", "📊 ILB Test session started")
                            }
                            android.util.Log.d("TimerSelection", "▶️ START clicked from TimerSelectionCard")
                        }
                    )
                }

                // 📊 ILB Test Week Banner - Shows at start of test week workouts
                if (isILBTestWeek && showILBBanner && hasStarted) {
                    item(key = "ilb_banner") {
                        ILBTestWeekBanner(
                            weekNumber = 1, // TODO: Get actual week number from program
                            onDismiss = { showILBBanner = false }
                        )
                    }
                }

                // WOD Timer Card - Show when ANY timer is selected (not just WODs!)
                if (selectedTimerConfig != null && hasStarted) {
                    item(key = "wod_timer") {
                        WodTimerCard(
                            timerConfig = selectedTimerConfig!!,
                            onTimerComplete = {
                                android.util.Log.d("WodTimer", "Timer completed!")
                                // Could auto-show finish dialog here
                            },
                            onRoundComplete = { round ->
                                android.util.Log.d("WodTimer", "Round $round completed")
                                // Vibrate on round completion is handled in WodTimerCard
                            }
                        )
                    }
                }

                // Rest Timer Card (only show when timer is active) - COMPACT
                if (timerData.state != TimerState.IDLE) {
                    item {
                        RestTimerCard(
                            timerData = timerData,
                            onSkip = { restTimerViewModel.skipTimer() },
                            onAddTime = { seconds -> restTimerViewModel.addTime(seconds) }
                        )
                    }
                }

                // ===== ALL EXERCISES - Vertical List with Frames =====
                mutableExercises.forEachIndexed { exerciseIndex, exercise ->
                    // Check if this exercise is completed
                    val exerciseCompleted = exercise.sets.indices.all { setIdx ->
                        val setId = "${workout.id}_${exercise.exerciseId}_set${setIdx + 1}"
                        setDataMap[setId]?.isCompleted ?: false
                    }

                    // Find current set index for this exercise
                    val exerciseCurrentSetIndex = exercise.sets.indices.firstOrNull { setIdx ->
                        val setId = "${workout.id}_${exercise.exerciseId}_set${setIdx + 1}"
                        setDataMap[setId]?.isCompleted != true
                    } ?: -1

                    // Exercise Card with Frame
                    item(key = "exercise_${exercise.exerciseId}_${exerciseIndex}") {
                        ExerciseSection(
                            exercise = exercise,
                            exerciseIndex = exerciseIndex,
                            isCompleted = exerciseCompleted,
                            currentSetIndex = exerciseCurrentSetIndex,
                            setDataMap = setDataMap,
                            workoutId = workout.id,
                            userId = userProfile?.id,  // For VBT baseline comparison
                            workoutSessionViewModel = workoutSessionViewModel,
                            workoutSessionRepository = workoutSessionRepository,
                            coroutineScope = coroutineScope,
                            restTimerViewModel = restTimerViewModel,
                            strengthProfile = strengthProfile, // For PR-based weight recommendations
                            onVBTClick = { setNumber ->
                                // Check VBT access before navigating
                                if (SubscriptionManager.hasAccess(Feature.BAR_SPEED)) {
                                    onNavigateToVBT(exercise.exerciseId, setNumber)
                                } else {
                                    onNavigateToPaywall()
                                }
                            },
                            onVideoClick = { videoPath, velocityMetrics ->
                                onNavigateToVideoPlayer(videoPath, velocityMetrics)
                            },
                            onExerciseCompleted = {
                                // Check if whole workout is complete
                                val workoutComplete = mutableExercises.all { ex ->
                                    ex.sets.indices.all { setIdx ->
                                        val sid = "${workout.id}_${ex.exerciseId}_set${setIdx + 1}"
                                        setDataMap[sid]?.isCompleted ?: false
                                    }
                                }
                                if (workoutComplete) {
                                    showFinishDialog = true
                                }
                            },
                            onSwapExercise = {
                                showSwapExerciseDialog = exerciseIndex
                                workoutModified = true
                            },
                            onRemoveExercise = {
                                if (mutableExercises.size > 1) {
                                    mutableExercises.removeAt(exerciseIndex)
                                    workoutModified = true
                                }
                            },
                            onAddSet = {
                                val lastSet = exercise.sets.lastOrNull()
                                val newSet = ExerciseSet(
                                    setNumber = exercise.sets.size + 1,
                                    targetReps = lastSet?.targetReps ?: 0,  // Empty - user must set reps
                                    targetWeight = lastSet?.targetWeight ?: 0.0,
                                    restSeconds = lastSet?.restSeconds ?: 90
                                )
                                val updatedExercise = exercise.copy(
                                    sets = exercise.sets + newSet
                                )
                                mutableExercises[exerciseIndex] = updatedExercise
                                workoutModified = true
                            },
                            onRemoveSet = { setIndex ->
                                if (exercise.sets.size > 1) {
                                    val updatedSets = exercise.sets.toMutableList().apply {
                                        removeAt(setIndex)
                                    }.mapIndexed { idx, set ->
                                        set.copy(setNumber = idx + 1)
                                    }
                                    mutableExercises[exerciseIndex] = exercise.copy(sets = updatedSets)
                                    workoutModified = true
                                }
                            },
                            // ILB: Periodized strength testing
                            isILBTestMode = isILBTestWeek,
                            ilbTestResults = ilbTestResults.associateBy { it.exerciseId }
                                .mapKeys { (exerciseId, _) ->
                                    // Map to setId format for lookup
                                    "${workout.id}_${exerciseId}_set1"
                                },
                            onAMRAPComplete = { exerciseId, exerciseName, setId, reps, weight ->
                                // Process AMRAP result and calculate new 1RM
                                coroutineScope.launch {
                                    userProfile?.id?.let { userId ->
                                        val result = ilbService.processAMRAPResult(
                                            userId = userId,
                                            exerciseId = exerciseId,
                                            exerciseName = exerciseName,
                                            testWeight = weight.toFloat(),
                                            amrapReps = reps
                                        )
                                        ilbTestResults.add(result)
                                        android.util.Log.d("ILB", "📊 AMRAP processed: ${result.exerciseName} → ${result.new1RM}kg (${result.getDisplayMessage()})")
                                    }
                                }
                            }
                        )
                    }
                }

                // Add Exercise Button
                item {
                    OutlinedButton(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = orangeGlow),
                        border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Exercise", fontWeight = FontWeight.Medium)
                    }
                }

                // FINISH WORKOUT Button - Always visible at bottom
                item {
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50) // Green for finish
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "FINISH WORKOUT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    // Pause Dialog
    if (showPauseDialog) {
        AlertDialog(
            onDismissRequest = { showPauseDialog = false },
            title = { Text("Pause Workout?", color = textWhite, fontWeight = FontWeight.Bold) },
            text = { Text("You can resume where you left off.", color = textGray) },
            confirmButton = {
                TextButton(onClick = {
                    workoutTimerViewModel.pauseWorkoutTimer()
                    showPauseDialog = false
                }) {
                    Text("Pause", color = orangeGlow, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val finalTime = workoutTimerViewModel.stopWorkoutTimer()

                    // Complete workout session in database (exit from pause)
                    coroutineScope.launch {
                        val durationMinutes = (finalTime.totalSeconds / 60)
                        workoutSessionRepository.completeWorkoutSession(
                            sessionId = workout.id,
                            durationMinutes = durationMinutes,
                            notes = "Exited during pause"
                        ).onSuccess {
                            android.util.Log.d("WorkoutSession", "✅ Workout exited from pause: ${workout.name}")
                        }.onFailure { error ->
                            android.util.Log.e("WorkoutSession", "❌ Failed to complete session: ${error.message}", error)
                        }
                    }

                    showPauseDialog = false
                    onNavigateBack(finalTime.totalSeconds)
                }) {
                    Text("Exit Workout", color = Color.Red)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = textWhite,
            textContentColor = textGray
        )
    }

    // 📊 ILB Session Summary - Show before finish dialog if test results exist
    if (showILBSummary && ilbTestResults.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showILBSummary = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ILBSessionSummary(
                    results = ilbTestResults.toList(),
                    onClose = {
                        showILBSummary = false
                        // Continue to finish dialog after dismissing ILB summary
                    }
                )
            }
        }
    }

    // Finish Workout Celebration Screen
    if (showFinishDialog) {
        // ILB: End test session and show summary first if there are results
        LaunchedEffect(Unit) {
            if (isILBTestWeek && ilbTestResults.isNotEmpty() && !showILBSummary) {
                val sessionResults = ilbService.endTestSession()
                android.util.Log.d("ILB", "📊 ILB Test session ended with ${sessionResults.size} results")
                showILBSummary = true
            }
        }

        // Calculate workout summary stats
        val totalSetsCompleted = setDataMap.values.count { it.isCompleted }
        val totalVolumeKg = setDataMap.values
            .filter { it.isCompleted && it.completedWeight != null && it.completedReps != null }
            .sumOf { (it.completedWeight ?: 0.0) * (it.completedReps ?: 0) }
        val totalReps = setDataMap.values
            .filter { it.isCompleted && it.completedReps != null }
            .sumOf { it.completedReps ?: 0 }

        // Full-screen celebration dialog
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFinishDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            WorkoutCompletionScreen(
                workoutName = workout.name,
                totalTime = workoutTimer.formatTime(),
                totalSetsCompleted = totalSetsCompleted,
                totalVolumeKg = totalVolumeKg,
                totalReps = totalReps,
                totalExercises = mutableExercises.size,
                prsAchieved = totalPRsAchieved,
                prExerciseNames = prExerciseNames,
                exercises = mutableExercises,
                setDataMap = setDataMap,
                workoutId = workout.id,
                onComplete = {
                    val finalTime = workoutTimerViewModel.stopWorkoutTimer()
                    val userId = userProfile?.id ?: "00000000-0000-0000-0000-000000000001"

                    // Use NonCancellable to ensure database operations complete even when navigating away
                    coroutineScope.launch(kotlinx.coroutines.NonCancellable) {
                        val durationMinutes = (finalTime.totalSeconds / 60)

                        android.util.Log.d("WorkoutStats", "🔵 Starting workout completion save (NonCancellable)")

                        // 1. Update exercise statistics for each completed set
                        var localPRCount = 0
                        val localPRExercises = mutableListOf<String>()

                        for (exercise in workout.exercises) {
                            for (setIdx in exercise.sets.indices) {
                                val setId = "${workout.id}_${exercise.exerciseId}_set${setIdx + 1}"
                                val setData = setDataMap[setId]

                                if (setData?.isCompleted == true && setData.completedWeight != null && setData.completedReps != null) {
                                    val prResult = StatisticsRepository.updateExerciseStatisticsForSet(
                                        userId = userId,
                                        exerciseId = exercise.exerciseId,
                                        exerciseName = exercise.exerciseName,
                                        sessionId = workout.id,
                                        weightKg = setData.completedWeight,
                                        reps = setData.completedReps,
                                        velocity = setData.vbtVelocity?.toDouble()
                                    )
                                    prResult.onSuccess { prsAchieved ->
                                        if (prsAchieved.isNotEmpty()) {
                                            localPRCount += prsAchieved.size
                                            if (exercise.exerciseName !in localPRExercises) {
                                                localPRExercises.add(exercise.exerciseName)
                                            }
                                            android.util.Log.d("WorkoutStats", "🏆 PRs for ${exercise.exerciseName}: $prsAchieved")
                                        }
                                    }.onFailure { error ->
                                        android.util.Log.e("WorkoutStats", "❌ Failed to update stats for ${exercise.exerciseName}: ${error.message}")
                                    }
                                }
                            }
                        }

                        // 2. Save workout to history
                        val completedAt = java.time.Instant.now().toString()
                        StatisticsRepository.saveWorkoutToHistory(
                            sessionId = workout.id,
                            userId = userId,
                            workoutName = workout.name,
                            workoutTemplateId = null,
                            totalVolumeKg = totalVolumeKg,
                            totalSets = totalSetsCompleted,
                            totalReps = totalReps,
                            totalExercises = workout.exercises.size,
                            durationMinutes = durationMinutes,
                            prsAchieved = localPRCount,
                            prExercises = localPRExercises,
                            startedAt = null,
                            completedAt = completedAt
                        ).onSuccess {
                            android.util.Log.d("WorkoutStats", "✅ Workout history saved")
                        }.onFailure { error ->
                            android.util.Log.e("WorkoutStats", "❌ Failed to save workout history: ${error.message}")
                        }

                        // 3. Update user training summary
                        StatisticsRepository.updateUserTrainingSummary(userId)

                        // 4. Complete workout session (with gamification)
                        workoutSessionRepository.completeWorkoutSession(
                            sessionId = workout.id,
                            durationMinutes = durationMinutes,
                            userId = userId
                        ).onSuccess {
                            android.util.Log.d("WorkoutSession", "✅ Workout completed: ${workout.name} (${durationMinutes}min) with $localPRCount PRs")
                        }.onFailure { error ->
                            android.util.Log.e("WorkoutSession", "❌ Failed to complete session: ${error.message}", error)
                        }

                        // 5. End workout pattern tracking and start post-workout nutrition window
                        val completedRecord = workoutPatternRepository.endWorkout()
                        if (completedRecord != null) {
                            android.util.Log.d("WorkoutPattern", "📊 Workout ended - pattern updated, duration: ${completedRecord.durationMinutes}min")

                            // Start post-workout nutrition tracking (anabolic window)
                            val wasFasted = completedRecord.wasFasted
                            preWorkoutNutritionService.startPostWorkoutTracking(
                                workoutEndTime = java.time.Instant.now(),
                                wasFasted = wasFasted
                            )
                            android.util.Log.d("WorkoutPattern", "🍽️ Post-workout nutrition window started (fasted: $wasFasted)")
                        }

                        android.util.Log.d("WorkoutStats", "🔵 Workout completion save finished")
                    }

                    // Store final time for navigation
                    finalWorkoutTimeSeconds = finalTime.totalSeconds
                    showFinishDialog = false

                    // Check if workout was modified and show save template dialog
                    if (workoutModified) {
                        showSaveTemplateDialog = true
                    } else {
                        // Trigger rating → share → navigate flow
                        finishWorkoutFlow()
                    }
                },
                onDismiss = { showFinishDialog = false }
            )
        }
    }

    // Community Share Dialog (shown after workout completion)
    if (showShareDialog) {
        val totalSetsCompleted = setDataMap.values.count { it.isCompleted }
        val totalVolumeForShare = setDataMap.values
            .filter { it.isCompleted && it.completedWeight != null && it.completedReps != null }
            .sumOf { (it.completedWeight ?: 0.0) * (it.completedReps ?: 0) }
        val totalRepsForShare = setDataMap.values
            .filter { it.isCompleted }
            .sumOf { it.completedReps ?: 0 }
        val durationMinutes = (finalWorkoutTimeSeconds / 60)

        // Collect available videos from completed sets
        val availableVideos = remember(setDataMap) {
            val videos = mutableListOf<ShareableVideo>()
            var setCounter = 0
            workout.exercises.forEach { exercise ->
                exercise.sets.forEachIndexed { setIndex, set ->
                    // Use the correct setId format: workoutId_exerciseId_setN
                    val setId = "${workout.id}_${exercise.exerciseId}_set${setIndex + 1}"
                    val setData = setDataMap[setId]
                    android.util.Log.d("CommunityShare", "🔍 Checking setId: $setId -> videoPath=${setData?.videoPath}, completed=${setData?.isCompleted}")
                    if (setData?.videoPath != null && setData.isCompleted) {
                        setCounter++
                        videos.add(ShareableVideo(
                            path = setData.videoPath!!,
                            exerciseName = exercise.exerciseName,
                            setNumber = setCounter
                        ))
                    }
                }
            }
            android.util.Log.d("CommunityShare", "📹 Found ${videos.size} videos to share")
            videos.toList()
        }

        EnhancedShareWorkoutDialog(
            workoutName = workout.name,
            totalVolumeKg = totalVolumeForShare,
            totalSets = totalSetsCompleted,
            totalReps = totalRepsForShare,
            durationMinutes = durationMinutes,
            prsAchieved = totalPRsAchieved,
            prExercises = prExerciseNames,
            availableVideos = availableVideos,
            isSharing = isSharingToCommunity,
            onShareToCommunity = { caption, selectedVideoPaths ->
                // Post to community feed
                val userId = userProfile?.id
                android.util.Log.d("CommunityShare", "🔄 Starting share - userId: $userId, videos: ${selectedVideoPaths.size}")
                if (userId == null) {
                    android.util.Log.e("CommunityShare", "❌ No user profile ID available!")
                    showShareDialog = false
                    onNavigateBack(finalWorkoutTimeSeconds)
                    return@EnhancedShareWorkoutDialog
                }

                // Show loading state and upload in lifecycle-aware scope
                isSharingToCommunity = true
                coroutineScope.launch(kotlinx.coroutines.NonCancellable + kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        // First upload videos if any are selected
                        var uploadedVideoUrls: List<String>? = null
                        if (selectedVideoPaths.isNotEmpty()) {
                            android.util.Log.d("CommunityShare", "📹 Uploading ${selectedVideoPaths.size} videos...")
                            val uploadResult = com.example.menotracker.community.data.FeedRepository.uploadCommunityVideos(
                                userId = userId,
                                videoPaths = selectedVideoPaths
                            )
                            uploadResult.onSuccess { urls ->
                                uploadedVideoUrls = urls.ifEmpty { null }
                                android.util.Log.d("CommunityShare", "✅ Uploaded ${urls.size} videos")
                            }.onFailure { error ->
                                android.util.Log.e("CommunityShare", "⚠️ Video upload failed: ${error.message}")
                                // Continue without videos
                            }
                        }

                        // Create post (with or without videos)
                        val result = com.example.menotracker.community.data.FeedRepository.createPost(
                            userId = userId,
                            workoutHistoryId = null,
                            workoutName = workout.name,
                            totalVolumeKg = totalVolumeForShare,
                            totalSets = totalSetsCompleted,
                            totalReps = totalRepsForShare,
                            durationMinutes = durationMinutes,
                            prsAchieved = totalPRsAchieved,
                            prExercises = prExerciseNames.ifEmpty { null },
                            caption = caption.ifBlank { null },
                            videoUrls = uploadedVideoUrls
                        )
                        result.onSuccess { post ->
                            android.util.Log.d("CommunityShare", "✅ Workout shared! Post ID: ${post.id}, videos: ${uploadedVideoUrls?.size ?: 0}")
                        }.onFailure { error ->
                            android.util.Log.e("CommunityShare", "❌ Failed to share: ${error.message}", error)
                        }
                    } finally {
                        // Navigate after upload completes (on main thread)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isSharingToCommunity = false
                            showShareDialog = false
                            onNavigateBack(finalWorkoutTimeSeconds)
                        }
                    }
                }
            },
            onSkip = {
                showShareDialog = false
                onNavigateBack(finalWorkoutTimeSeconds)
            }
        )
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Workout?", color = textWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Your progress will be saved.", color = textGray) },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTime = workoutTimerViewModel.stopWorkoutTimer()

                        // Complete workout session in database (early exit)
                        coroutineScope.launch {
                            val durationMinutes = (finalTime.totalSeconds / 60)
                            workoutSessionRepository.completeWorkoutSession(
                                sessionId = workout.id,
                                durationMinutes = durationMinutes,
                                notes = "Exited early"
                            ).onSuccess {
                                android.util.Log.d("WorkoutSession", "✅ Workout exited early: ${workout.name} (${durationMinutes}min)")
                            }.onFailure { error ->
                                android.util.Log.e("WorkoutSession", "❌ Failed to complete session: ${error.message}", error)
                            }
                        }

                        onNavigateBack(finalTime.totalSeconds)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("EXIT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = textGray)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = textWhite,
            textContentColor = textGray
        )
    }

    // Swap Exercise Dialog
    showSwapExerciseDialog?.let { exerciseIndexToSwap ->
        ExercisePickerDialog(
            title = "Swap Exercise",
            onDismiss = { showSwapExerciseDialog = null },
            onExerciseSelected = { selectedExercise ->
                // Create new ExerciseWithSets from selected exercise
                val currentExercise = mutableExercises[exerciseIndexToSwap]
                val newExercise = ExerciseWithSets(
                    exerciseId = selectedExercise.id,
                    exerciseName = selectedExercise.name,
                     muscleGroup = selectedExercise.muscleGroup,
                    equipment = selectedExercise.equipment,
                    order = currentExercise.order, // Keep same order
                    sets = currentExercise.sets.map { it.copy() } // Keep same sets
                )
                mutableExercises[exerciseIndexToSwap] = newExercise
                showSwapExerciseDialog = null
                workoutModified = true
            }
        )
    }

    // Add Exercise Dialog
    if (showAddExerciseDialog) {
        ExercisePickerDialog(
            title = "Add Exercise",
            onDismiss = { showAddExerciseDialog = false },
            onExerciseSelected = { selectedExercise ->
                // Create new ExerciseWithSets with empty default sets
                // User must enter their own reps and weight
                val newExercise = ExerciseWithSets(
                    exerciseId = selectedExercise.id,
                    exerciseName = selectedExercise.name,
                    muscleGroup = selectedExercise.muscleGroup,
                    equipment = selectedExercise.equipment,
                    order = mutableExercises.size + 1, // Add at end
                    sets = listOf(
                        ExerciseSet(setNumber = 1, targetReps = 0, targetWeight = 0.0, restSeconds = 90),
                        ExerciseSet(setNumber = 2, targetReps = 0, targetWeight = 0.0, restSeconds = 90),
                        ExerciseSet(setNumber = 3, targetReps = 0, targetWeight = 0.0, restSeconds = 90)
                    )
                )
                mutableExercises.add(newExercise)
                showAddExerciseDialog = false
                workoutModified = true
            }
        )
    }

    // Save Template Dialog (shown when workout is modified and finished)
    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            workoutName = workout.name,
            onDismiss = {
                showSaveTemplateDialog = false
                finishWorkoutFlow()
            },
            onKeepOriginal = {
                showSaveTemplateDialog = false
                // Continue with finish flow without saving template changes
                finishWorkoutFlow()
            },
            onSaveAsNew = { newName ->
                showSaveTemplateDialog = false
                val userId = userProfile?.id ?: "00000000-0000-0000-0000-000000000001"

                // Create new template from modified exercises
                coroutineScope.launch {
                    android.util.Log.d("WorkoutSession", "💾 Saving new template: $newName")

                    // Convert mutableExercises to WorkoutTemplate
                    val newTemplate = WorkoutTemplate(
                        id = java.util.UUID.randomUUID().toString(),
                        name = newName,
                        exercises = mutableExercises.toList(),
                        createdAt = System.currentTimeMillis()
                    )

                    com.example.menotracker.data.WorkoutTemplateRepository
                        .saveWorkoutTemplate(newTemplate, userId)
                        .onSuccess { templateId ->
                            android.util.Log.d("WorkoutSession", "✅ New template saved: $templateId")
                        }
                        .onFailure { error ->
                            android.util.Log.e("WorkoutSession", "❌ Failed to save template: ${error.message}", error)
                        }
                }
                finishWorkoutFlow()
            },
            onUpdateExisting = {
                showSaveTemplateDialog = false
                val userId = userProfile?.id ?: "00000000-0000-0000-0000-000000000001"

                // Update existing template with modified exercises
                coroutineScope.launch {
                    android.util.Log.d("WorkoutSession", "📝 Updating existing template: ${workout.name}")

                    // Create updated template keeping same ID
                    val updatedTemplate = WorkoutTemplate(
                        id = workout.id,
                        name = workout.name,
                        exercises = mutableExercises.toList(),
                        createdAt = workout.createdAt
                    )

                    com.example.menotracker.data.WorkoutTemplateRepository
                        .updateWorkoutTemplate(updatedTemplate, userId)
                        .onSuccess {
                            android.util.Log.d("WorkoutSession", "✅ Template updated successfully")
                        }
                        .onFailure { error ->
                            android.util.Log.e("WorkoutSession", "❌ Failed to update template: ${error.message}", error)
                        }
                }
                finishWorkoutFlow()
            }
        )
    }

    // ⭐ App Rating Dialog (shown after workout completion, before share)
    if (showRatingDialog) {
        AppRatingDialog(
            onDismiss = {
                // User dismissed - record and continue flow
                coroutineScope.launch {
                    appReviewManager?.onPromptDismissed()
                }
                showRatingDialog = false
                // Continue to share dialog or navigate
                if (shouldShowShareDialog()) {
                    showShareDialog = true
                } else {
                    onNavigateBack(finalWorkoutTimeSeconds)
                }
            },
            onPositive = {
                // User is happy - launch In-App Review
                coroutineScope.launch {
                    appReviewManager?.onPromptShown()
                    appReviewManager?.launchInAppReview()
                }
                showRatingDialog = false
                // Continue to share dialog or navigate
                if (shouldShowShareDialog()) {
                    showShareDialog = true
                } else {
                    onNavigateBack(finalWorkoutTimeSeconds)
                }
            },
            onNegative = {
                // User is unhappy - they already submitted feedback
                coroutineScope.launch {
                    appReviewManager?.onNegativeFeedback()
                }
                showRatingDialog = false
                // Continue to share dialog or navigate
                if (shouldShowShareDialog()) {
                    showShareDialog = true
                } else {
                    onNavigateBack(finalWorkoutTimeSeconds)
                }
            },
            onFeedbackSubmit = { feedback ->
                // Log the feedback (could send to backend later)
                android.util.Log.d("AppReview", "📝 User feedback: $feedback")
                // TODO: Send feedback to backend/analytics
            }
        )
    }
}

/**
 * Compact exercise info header with progress
 */
@Composable
private fun ExerciseInfoCard(
    exercise: ExerciseWithSets,
    currentSetIndex: Int,
    totalSets: Int
) {
    // Compact exercise header - just name + equipment + progress in one row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Exercise name
        Text(
            text = exercise.exerciseName,
            color = textWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // Equipment chip (compact)
        Surface(
            color = textGray.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = exercise.equipment,
                color = textGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Set progress text
        Text(
            text = "Set ${currentSetIndex + 1}/$totalSets",
            color = orangeGlow,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ExerciseSection moved to components/ExerciseSection.kt

@Composable
private fun InfoChip(icon: ImageVector, text: String, backgroundColor: Color) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textWhite,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                color = textWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Header row for the sets table (Hevy-style)
 */
@Composable
private fun SetTableHeader(showPrevious: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SET column
        Text(
            text = "SET",
            color = textGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(36.dp)
        )

        // PREVIOUS column (optional)
        if (showPrevious) {
            Text(
                text = "PREVIOUS",
                color = textGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        // KG column
        Text(
            text = "KG",
            color = textGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(70.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // REPS column
        Text(
            text = "REPS",
            color = textGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(70.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Camera icon column
        Spacer(modifier = Modifier.width(40.dp))

        // Checkbox column
        Spacer(modifier = Modifier.width(40.dp))
    }
}

private fun vibratePattern(vibrator: Vibrator, pattern: LongArray) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern, -1)
    }
}
