// app/src/main/java/com/example/myapplicationtest/screens/library/LibraryScreen.kt

package com.example.menotracker.screens.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaBackground
import java.util.Locale
import com.example.menotracker.data.ExerciseRepository
import com.example.menotracker.data.ProgramRepository
import com.example.menotracker.data.WorkoutRepository
import com.example.menotracker.data.WorkoutAssignmentRepository
import com.example.menotracker.data.models.Exercise
import com.example.menotracker.data.models.Program
import com.example.menotracker.data.models.UserProgram
import com.example.menotracker.data.models.Workout
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

// 🎨 Design System - Matching TrainingScreen Glassmorphism Style
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)  // Glassmorphism for cards
private val cardBorder = orangeGlow.copy(alpha = 0.5f)
private val surfaceColor = Color(0xFF1a1410).copy(alpha = 0.4f)  // Glassmorphism for cards
private val dialogBackground = Color(0xFF1a1410).copy(alpha = 0.97f)  // More solid for dialogs (readable text)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF1E1E1E), NayaBackground, Color(0xFF1a1410))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    paddingValues: PaddingValues,
    initialTab: Int = 0,
    selectionMode: String? = null, // "exercises", "workouts", "programs", "calendar"
    workoutBuilderViewModel: com.example.menotracker.viewmodels.WorkoutBuilderViewModel? = null,
    accountViewModel: com.example.menotracker.viewmodels.AccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToCreateExercise: () -> Unit,
    onNavigateToCreateWorkout: () -> Unit,
    onNavigateToCreateProgram: () -> Unit,
    onNavigateToExerciseDetail: (Exercise) -> Unit,
    onNavigateToTraining: () -> Unit = {}, // Navigate to Training Screen
    onStartWorkout: (com.example.menotracker.viewmodels.WorkoutTemplate) -> Unit = {}, // Start workout session
    onItemSelected: ((String) -> Unit)? = null, // Callback when item is selected
    onCancelSelection: (() -> Unit)? = null // Callback to cancel selection
) {
    val scope = rememberCoroutineScope()

    // State for date picker
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedItemName by remember { mutableStateOf("") }

    // Get saved workouts from WorkoutBuilderViewModel (user's own workouts)
    val savedWorkoutTemplates by (workoutBuilderViewModel?.savedWorkouts?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    // Get public workout templates (16 example templates)
    val publicWorkoutTemplates by (workoutBuilderViewModel?.publicWorkouts?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    // Convert user's WorkoutTemplates to Workouts for display
    // Note: Using orEmpty() for null safety - Gson disk cache may deserialize null exercises
    val convertedWorkouts = remember(savedWorkoutTemplates) {
        savedWorkoutTemplates.map { template ->
            Workout(
                id = template.id,
                name = template.name,
                exercises = (template.exercises ?: emptyList()).map { exercise ->
                    com.example.menotracker.data.models.WorkoutExercise(
                        exercise = com.example.menotracker.data.models.Exercise(
                            id = exercise.exerciseId,
                            name = exercise.exerciseName,
                            mainMuscle = exercise.muscleGroup,
                            equipment = exercise.equipment.split(", ")
                        ),
                        sets = exercise.sets.firstOrNull()?.targetReps ?: 3,
                        reps = exercise.sets.firstOrNull()?.targetReps ?: 10,
                        weight = exercise.sets.firstOrNull()?.targetWeight?.toFloat() ?: 0f,
                        restTimeInSeconds = exercise.sets.firstOrNull()?.restSeconds ?: 90
                    )
                }
            )
        }
    }

    // Convert public WorkoutTemplates to Workouts for display
    val convertedPublicWorkouts = remember(publicWorkoutTemplates) {
        publicWorkoutTemplates.map { template ->
            Workout(
                id = template.id,
                name = template.name,
                exercises = (template.exercises ?: emptyList()).map { exercise ->
                    com.example.menotracker.data.models.WorkoutExercise(
                        exercise = com.example.menotracker.data.models.Exercise(
                            id = exercise.exerciseId,
                            name = exercise.exerciseName,
                            mainMuscle = exercise.muscleGroup,
                            equipment = exercise.equipment.split(", ")
                        ),
                        sets = exercise.sets.firstOrNull()?.targetReps ?: 3,
                        reps = exercise.sets.firstOrNull()?.targetReps ?: 10,
                        weight = exercise.sets.firstOrNull()?.targetWeight?.toFloat() ?: 0f,
                        restTimeInSeconds = exercise.sets.firstOrNull()?.restSeconds ?: 90
                    )
                }
            )
        }
    }

    // Combine user workouts with WorkoutRepository workouts
    val allWorkouts = remember(convertedWorkouts) {
        (WorkoutRepository.createdWorkouts + convertedWorkouts).distinctBy { it.id }
    }

    // Get user profile to access preferred sports (must be defined before use in LaunchedEffect)
    val userProfile by accountViewModel.userProfile.collectAsState()
    val preferredSports = userProfile?.preferredSports ?: emptyList()

    // Coach-assigned workouts
    val assignedWorkouts by WorkoutAssignmentRepository.assignedWorkouts.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch {
            // Load all repositories in PARALLEL for faster startup
            val exercisesDeferred = async { ExerciseRepository.initialize() }
            val workoutsDeferred = async { WorkoutRepository.initialize() }
            val programsDeferred = async { ProgramRepository.initialize() }
            val templatesDeferred = async { ProgramRepository.initializeTemplates() }

            // Wait for all to complete
            awaitAll(exercisesDeferred, workoutsDeferred, programsDeferred, templatesDeferred)
        }
    }

    // Initialize user programs and load assigned workouts when user profile is available
    LaunchedEffect(userProfile?.id) {
        val userId = userProfile?.id
        if (userId != null) {
            scope.launch {
                ProgramRepository.initializeUserPrograms(userId)
                WorkoutAssignmentRepository.loadAssignedWorkouts(userId)
            }
        }
    }

    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("Library", "Programs", "Workouts", "Exercises")

    // Exercise Sub-Tab state (0 = All Exercises, 1 = My Sports)
    var selectedExerciseSubTab by remember { mutableStateOf(0) }

    // Workout Sub-Tab state (0 = My Workouts, 1 = Templates)
    var selectedWorkoutSubTab by remember { mutableStateOf(0) }

    // Program Sub-Tab state (0 = My Programs, 1 = Templates)
    var selectedProgramSubTab by remember { mutableStateOf(0) }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var itemToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Workout detail views (separate for Workout and WorkoutTemplate)
    var selectedWorkout by remember { mutableStateOf<Workout?>(null) }
    var selectedWorkoutTemplate by remember { mutableStateOf<com.example.menotracker.viewmodels.WorkoutTemplate?>(null) }

    // Program template detail view
    var selectedProgramTemplate by remember { mutableStateOf<com.example.menotracker.data.models.ProgramTemplate?>(null) }

    // Start Program confirmation dialog
    var showStartProgramDialog by remember { mutableStateOf(false) }
    var programToStart by remember { mutableStateOf<com.example.menotracker.data.models.ProgramTemplate?>(null) }
    var isStartingProgram by remember { mutableStateOf(false) }

    // Get user's sports from SportDatabase for smart filtering (up to 3 sports)
    val userPrimarySport = remember(userProfile?.primarySport) {
        userProfile?.primarySport?.let { sportId ->
            com.example.menotracker.onboarding.data.SportDatabase.getById(sportId)
        }
    }
    val userSecondarySport = remember(userProfile?.secondarySport) {
        userProfile?.secondarySport?.let { sportId ->
            com.example.menotracker.onboarding.data.SportDatabase.getById(sportId)
        }
    }
    val userTertiarySport = remember(userProfile?.tertiarySport) {
        userProfile?.tertiarySport?.let { sportId ->
            com.example.menotracker.onboarding.data.SportDatabase.getById(sportId)
        }
    }

    // Combine all user sports for filtering (primary first, then secondary, then tertiary)
    val allUserSports = remember(userPrimarySport, userSecondarySport, userTertiarySport) {
        listOfNotNull(userPrimarySport, userSecondarySport, userTertiarySport)
    }

    // Configure LibrarySearchEngine with user's sport for personalized search ranking
    LaunchedEffect(userPrimarySport) {
        LibrarySearchEngine.setUserSport(userPrimarySport)
    }

    // Workout template category filter
    var selectedTemplateCategory by remember { mutableStateOf("All") }

    // Search state with debouncing for performance
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }

    // Debounce search query - only update after 300ms of no typing
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            // Instant clear when emptying search
            debouncedSearchQuery = ""
        } else {
            delay(300L) // Wait 300ms before searching
            debouncedSearchQuery = searchQuery
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SMART EXERCISE FILTERING - Based on User's Sport(s)
    // Supports up to 3 sports (primary, secondary, tertiary)
    // No manual filter buttons - everything is automatic!
    // ═══════════════════════════════════════════════════════════════
    val filteredExercises = remember(selectedExerciseSubTab, debouncedSearchQuery, allUserSports, preferredSports) {
        var exercises = when (selectedExerciseSubTab) {
            // "FOR YOU" Tab (0): Smart prioritization based on ALL user's sports
            0 -> {
                // Combine primaryExercises from all user sports
                val allPrimaryExerciseNames = allUserSports.flatMap { it.primaryExercises }.distinct()
                val allSportNames = allUserSports.map { it.name }

                if (allPrimaryExerciseNames.isEmpty() && allSportNames.isEmpty()) {
                    // No sports selected - show all sorted by importance
                    LibrarySearchEngine.sortByImportance(ExerciseRepository.createdExercises)
                } else {
                    // Smart sorting: Primary exercises first, then sport-matching, then others
                    val allExercises = ExerciseRepository.createdExercises

                    // 1. Primary exercises for ANY of user's sports (exact matches)
                    val primaryMatches = allExercises.filter { exercise ->
                        allPrimaryExerciseNames.any { primary ->
                            exercise.name.contains(primary, ignoreCase = true) ||
                            primary.contains(exercise.name, ignoreCase = true)
                        }
                    }

                    // 2. Exercises tagged with ANY of user's sports
                    val sportMatches = allExercises.filter { exercise ->
                        exercise.sports?.any { exerciseSport ->
                            allSportNames.any { userSport ->
                                exerciseSport.equals(userSport, ignoreCase = true)
                            }
                        } == true
                    }.filter { it !in primaryMatches }

                    // 3. All other exercises
                    val others = allExercises.filter { it !in primaryMatches && it !in sportMatches }

                    // Combine: Primary first, then sport matches, then others (each sorted by importance)
                    LibrarySearchEngine.sortByImportance(primaryMatches) +
                    LibrarySearchEngine.sortByImportance(sportMatches) +
                    LibrarySearchEngine.sortByImportance(others)
                }
            }
            // "ALL EXERCISES" Tab (1): Show everything sorted by importance
            else -> {
                LibrarySearchEngine.sortByImportance(ExerciseRepository.createdExercises)
            }
        }

        // Apply intelligent search with ranking using LibrarySearchEngine
        if (debouncedSearchQuery.isNotBlank()) {
            exercises = LibrarySearchEngine.searchExercises(exercises, debouncedSearchQuery)
        }

        exercises
    }

    // Filter user programs by search query
    val filteredUserPrograms = remember(debouncedSearchQuery, ProgramRepository.userPrograms) {
        if (debouncedSearchQuery.isBlank()) {
            ProgramRepository.userPrograms
        } else {
            ProgramRepository.userPrograms.filter {
                it.name.contains(debouncedSearchQuery, ignoreCase = true)
            }
        }
    }

    // Filter user's OWN exercises (where owner_id matches current user)
    val filteredUserExercises = remember(debouncedSearchQuery, userProfile) {
        val userId = userProfile?.id

        // Only show exercises created by THIS user (owner_id == userId)
        val userOwnedExercises = if (userId != null) {
            ExerciseRepository.createdExercises.filter { exercise ->
                exercise.ownerId == userId
            }
        } else {
            emptyList() // No user logged in = no exercises
        }

        // Apply intelligent search with ranking using LibrarySearchEngine
        if (debouncedSearchQuery.isBlank()) {
            userOwnedExercises
        } else {
            LibrarySearchEngine.searchExercises(userOwnedExercises, debouncedSearchQuery)
        }
    }

    // Determine if we're in selection mode
    val isSelectionMode = selectionMode != null

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Header with selection mode indicator
            if (isSelectionMode) {
                SelectionModeHeader(
                    selectionMode = selectionMode!!,
                    onCancelClick = { onCancelSelection?.invoke() }
                )
            } else {
                LibraryHeader()
            }

            LibraryTabs(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Search Bar
            LibrarySearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }
            )

            // Exercise Sub-Tabs (nur für Library/Exercises Tab)
            if (selectedTab == 0) {
                ExerciseSubTabs(
                    selectedSubTab = selectedExerciseSubTab,
                    onSubTabSelected = { selectedExerciseSubTab = it }
                )
            }

            // Program Sub-Tabs (nur für Programs Tab)
            if (selectedTab == 1) {
                ProgramSubTabs(
                    selectedSubTab = selectedProgramSubTab,
                    onSubTabSelected = { selectedProgramSubTab = it }
                )
            }

            // Workout Sub-Tabs (nur für Workouts Tab)
            if (selectedTab == 2) {
                WorkoutSubTabs(
                    selectedSubTab = selectedWorkoutSubTab,
                    onSubTabSelected = { selectedWorkoutSubTab = it },
                    assignedCount = assignedWorkouts.size
                )
            }

            // Smart personalization indicator for Exercise "FOR YOU" sub-tab
            if (selectedTab == 0 && selectedExerciseSubTab == 0 && allUserSports.isNotEmpty()) {
                PersonalizedForYouIndicator(
                    sportName = allUserSports.joinToString(", ") { it.name },
                    trainingFocusDescription = "${allUserSports.flatMap { it.primaryExercises }.distinct().size} exercises optimized for your sports"
                )
            }

            // Smart personalization indicator (nur für Templates Sub-Tab)
            if (selectedTab == 2 && selectedWorkoutSubTab == 1 && userPrimarySport != null) {
                PersonalizedForYouIndicator(
                    sportName = userPrimarySport.name,
                    trainingFocusDescription = when {
                        userPrimarySport.trainingFocus.kraft >= 0.5f -> "Strength-focused"
                        userPrimarySport.trainingFocus.ausdauer >= 0.5f -> "Endurance-focused"
                        userPrimarySport.trainingFocus.schnelligkeit >= 0.5f -> "Speed-focused"
                        else -> "Balanced training"
                    }
                )
            }

            when (selectedTab) {
                0 -> ExercisesGrid(
                    exercises = filteredExercises,
                    isSelectionMode = isSelectionMode,
                    onExerciseClick = if (isSelectionMode) {
                        { exercise ->
                            selectedItemName = exercise.name
                            if (selectionMode == "calendar") {
                                showDatePicker = true
                            } else {
                                onItemSelected?.invoke(exercise.name)
                            }
                        }
                    } else {
                        { exercise -> onNavigateToExerciseDetail(exercise) }
                    },
                    onDeleteClick = null,  // Library tab: No delete allowed (only user-created exercises)
                    showDeleteButton = false,
                    searchQuery = debouncedSearchQuery
                )
                1 -> {
                    if (selectedProgramSubTab == 0) {
                        // My Programs tab - show user's saved programs from templates
                        UserProgramsGrid(
                            programs = filteredUserPrograms,
                            isSelectionMode = isSelectionMode,
                            onProgramClick = if (isSelectionMode) {
                                { program ->
                                    selectedItemName = program.name
                                    if (selectionMode == "calendar") {
                                        showDatePicker = true
                                    } else {
                                        onItemSelected?.invoke(program.name)
                                    }
                                }
                            } else {
                                { program -> itemToDelete = "UserProgram" to program.id }
                            },
                            onDeleteClick = { itemToDelete = "UserProgram" to it }
                        )
                    } else {
                        // Templates tab - show program templates from database
                        var templatesToShow = ProgramRepository.programTemplates

                        // Apply intelligent search with ranking
                        if (debouncedSearchQuery.isNotBlank()) {
                            templatesToShow = LibrarySearchEngine.searchPrograms(templatesToShow, debouncedSearchQuery)
                        }

                        ProgramTemplatesGrid(
                            templates = templatesToShow,
                            isSelectionMode = isSelectionMode,
                            onTemplateClick = { template ->
                                if (isSelectionMode) {
                                    selectedItemName = template.name
                                    if (selectionMode == "calendar") {
                                        showDatePicker = true
                                    } else {
                                        onItemSelected?.invoke(template.name)
                                    }
                                } else {
                                    selectedProgramTemplate = template
                                }
                            }
                        )
                    }
                }
                2 -> {
                    when (selectedWorkoutSubTab) {
                        0 -> {
                            // My Workouts tab - show simple Workout objects
                            var myWorkouts = allWorkouts

                            // Apply search filter with debouncing
                            if (debouncedSearchQuery.isNotBlank()) {
                                myWorkouts = myWorkouts.filter { it.name.contains(debouncedSearchQuery, ignoreCase = true) }
                            }

                            WorkoutsGrid(
                                workouts = myWorkouts,
                                isSelectionMode = isSelectionMode,
                                onWorkoutClick = if (isSelectionMode) {
                                    { workout ->
                                        selectedItemName = workout.name
                                        if (selectionMode == "calendar") {
                                            showDatePicker = true
                                        } else {
                                            onItemSelected?.invoke(workout.name)
                                        }
                                    }
                                } else {
                                    { workout -> selectedWorkout = workout }
                                },
                                onDeleteClick = { itemToDelete = "Workout" to it }
                            )
                        }
                        1 -> {
                            // ═══════════════════════════════════════════════════════════════
                            // SMART WORKOUT TEMPLATE PRIORITIZATION
                            // Based on user's primary sport and training focus
                            // ═══════════════════════════════════════════════════════════════
                            val sportName = userPrimarySport?.name
                            val trainingFocus = userPrimarySport?.trainingFocus

                            var templatesToShow = if (sportName != null || trainingFocus != null) {
                                // Smart 3-tier prioritization:
                                // 1. Templates explicitly tagged with user's sport
                                val sportMatches = publicWorkoutTemplates.filter { template ->
                                    sportName != null && template.sports?.any {
                                        it.equals(sportName, ignoreCase = true)
                                    } == true
                                }

                                // 2. Templates matching training focus (Strength/Power workouts for kraft-heavy sports)
                                val focusMatches = publicWorkoutTemplates.filter { template ->
                                    template !in sportMatches && trainingFocus != null && run {
                                        val templateName = template.name.lowercase()
                                        val isStrengthTemplate = templateName.contains("strength") ||
                                            templateName.contains("power") || templateName.contains("hypertrophy")
                                        val isEnduranceTemplate = templateName.contains("endurance") ||
                                            templateName.contains("cardio") || templateName.contains("conditioning")
                                        val isSpeedTemplate = templateName.contains("explosive") ||
                                            templateName.contains("speed") || templateName.contains("plyometric")
                                        val isMobilityTemplate = templateName.contains("mobility") ||
                                            templateName.contains("flexibility") || templateName.contains("stretch")

                                        // Match based on training focus weights
                                        (trainingFocus.kraft >= 0.5f && isStrengthTemplate) ||
                                        (trainingFocus.ausdauer >= 0.5f && isEnduranceTemplate) ||
                                        (trainingFocus.schnelligkeit >= 0.5f && isSpeedTemplate) ||
                                        (trainingFocus.beweglichkeit >= 0.4f && isMobilityTemplate)
                                    }
                                }

                                // 3. All other templates
                                val others = publicWorkoutTemplates.filter {
                                    it !in sportMatches && it !in focusMatches
                                }

                                // Combine: Sport matches first, then focus matches, then others
                                sportMatches + focusMatches + others
                            } else {
                                publicWorkoutTemplates
                            }

                            // Then apply category filter
                            if (selectedTemplateCategory != "All") {
                                templatesToShow = templatesToShow.filter { template ->
                                    template.name.contains(selectedTemplateCategory, ignoreCase = true)
                                }
                            }

                            // Apply intelligent search with ranking using LibrarySearchEngine
                            if (debouncedSearchQuery.isNotBlank()) {
                                templatesToShow = LibrarySearchEngine.searchWorkouts(templatesToShow, debouncedSearchQuery)
                            }

                            WorkoutTemplatesGrid(
                                templates = templatesToShow,
                                isSelectionMode = isSelectionMode,
                                onTemplateClick = if (isSelectionMode) {
                                    { template ->
                                        selectedItemName = template.name
                                        if (selectionMode == "calendar") {
                                            showDatePicker = true
                                        } else {
                                            onItemSelected?.invoke(template.name)
                                        }
                                    }
                                } else {
                                    { template -> selectedWorkoutTemplate = template }
                                },
                                onDeleteClick = { itemToDelete = "Template" to it }
                            )
                        }
                        2 -> {
                            // ═══════════════════════════════════════════════════════════════
                            // COACH-ASSIGNED WORKOUTS
                            // Workouts assigned by a coach via the Coach Web App
                            // ═══════════════════════════════════════════════════════════════
                            if (assignedWorkouts.isEmpty()) {
                                // Empty state
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = textGray
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "No Assigned Workouts",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textWhite
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "When your coach assigns you a workout, it will appear here.",
                                            fontSize = 14.sp,
                                            color = textGray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // Filter assigned workouts by search
                                var filteredAssigned = assignedWorkouts
                                if (debouncedSearchQuery.isNotBlank()) {
                                    filteredAssigned = filteredAssigned.filter {
                                        it.workoutTemplate.name.contains(debouncedSearchQuery, ignoreCase = true)
                                    }
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredAssigned.size) { index ->
                                        val assignment = filteredAssigned[index]
                                        AssignedWorkoutCard(
                                            assignment = assignment,
                                            onClick = {
                                                // Open workout detail view
                                                selectedWorkoutTemplate = assignment.workoutTemplate
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> ExercisesGrid(
                    exercises = filteredUserExercises,
                    isSelectionMode = isSelectionMode,
                    onExerciseClick = if (isSelectionMode) {
                        { exercise ->
                            selectedItemName = exercise.name
                            if (selectionMode == "calendar") {
                                showDatePicker = true
                            } else {
                                onItemSelected?.invoke(exercise.name)
                            }
                        }
                    } else {
                        { exercise -> onNavigateToExerciseDetail(exercise) }
                    },
                    onDeleteClick = { itemToDelete = "Exercise" to it },
                    showDeleteButton = true,  // My Exercises tab: Allow delete for user-created exercises
                    searchQuery = debouncedSearchQuery
                )
            }
        }

        // FAB only shown in "My" tabs: My Programs, My Workouts, My Exercises
        val showFab = !isSelectionMode && (
            (selectedTab == 1 && selectedProgramSubTab == 0) ||  // My Programs
            (selectedTab == 2 && selectedWorkoutSubTab == 0) ||  // My Workouts
            selectedTab == 3  // My Exercises
        )

        if (showFab) {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = orangePrimary,
                contentColor = textWhite,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 140.dp)  // Clear the glassmorphism bottom nav bar
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(32.dp))
            }
        }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                // TODO: Add to calendar with selected date
                onItemSelected?.invoke(selectedItemName)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { (type, id) ->
        val name = when (type) {
            "Program" -> ProgramRepository.createdPrograms.find { it.id == id }?.name
            "UserProgram" -> ProgramRepository.userPrograms.find { it.id == id }?.name
            "Workout" -> WorkoutRepository.createdWorkouts.find { it.id == id }?.name
            "Exercise" -> ExerciseRepository.createdExercises.find { it.id == id }?.name
            else -> ""
        } ?: ""

        DeleteConfirmationDialog(
            itemName = name,
            onConfirm = {
                scope.launch {
                    when (type) {
                        "Program" -> ProgramRepository.deleteProgram(id)
                        "UserProgram" -> ProgramRepository.deleteUserProgram(id)
                        "Workout" -> WorkoutRepository.deleteWorkout(id)
                        "Exercise" -> ExerciseRepository.deleteExercise(id)
                    }
                    itemToDelete = null
                }
            },
            onDismiss = { itemToDelete = null }
        )
    }

    // Workout Detail Dialog (for simple user-created workouts)
    selectedWorkout?.let { workout ->
        WorkoutDetailDialog(
            workout = workout,
            onDismiss = { selectedWorkout = null },
            onDelete = {
                itemToDelete = "Workout" to workout.id
                selectedWorkout = null
            }
        )
    }

    // Workout Template Detail Dialog (for detailed templates with multiple sets)
    selectedWorkoutTemplate?.let { template ->
        WorkoutTemplateDetailDialog(
            template = template,
            onDismiss = { selectedWorkoutTemplate = null },
            onStartWorkout = { selectedTemplate ->
                selectedWorkoutTemplate = null
                onStartWorkout(selectedTemplate)
            }
        )
    }

    // Program Template Detail Dialog
    selectedProgramTemplate?.let { template ->
        ProgramTemplateDetailDialog(
            template = template,
            onDismiss = { selectedProgramTemplate = null },
            onStartProgram = { selectedTemplate ->
                programToStart = selectedTemplate
                showStartProgramDialog = true
                selectedProgramTemplate = null
            }
        )
    }

    // Start Program Confirmation Dialog
    if (showStartProgramDialog && programToStart != null) {
        StartProgramConfirmationDialog(
            programName = programToStart!!.name,
            isLoading = isStartingProgram,
            onConfirm = { loadToTraining ->
                val userId = userProfile?.id
                if (userId != null && programToStart != null) {
                    isStartingProgram = true
                    scope.launch {
                        val newProgramId = ProgramRepository.startProgramFromTemplate(
                            template = programToStart!!,
                            userId = userId
                        )
                        isStartingProgram = false
                        showStartProgramDialog = false

                        if (newProgramId != null) {
                            // Switch to My Programs tab to show the new program
                            selectedProgramSubTab = 0

                            if (loadToTraining) {
                                // Navigate to Training Screen
                                println("✅ Program started, navigating to Training Screen: $newProgramId")
                                onNavigateToTraining()
                            } else {
                                println("✅ Program saved to My Programs: $newProgramId")
                            }
                        }
                        programToStart = null
                    }
                }
            },
            onDismiss = {
                showStartProgramDialog = false
                programToStart = null
            }
        )
    }

    // Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = textWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Create New", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))

                ListItem(
                    headlineContent = { Text("Program", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Build a new multi-week training plan", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Default.CalendarViewWeek, contentDescription = null, tint = orangePrimary) },
                    modifier = Modifier.clickable(onClick = onNavigateToCreateProgram),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                ListItem(
                    headlineContent = { Text("Workout", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Assemble a new single-day workout", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = orangePrimary) },
                    modifier = Modifier.clickable(onClick = onNavigateToCreateWorkout),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                ListItem(
                    headlineContent = { Text("Exercise", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Define a new exercise from scratch", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = orangePrimary) },
                    modifier = Modifier.clickable(onClick = onNavigateToCreateExercise),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SELECTION MODE HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SelectionModeHeader(selectionMode: String, onCancelClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = orangePrimary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📅 SELECT TO ADD",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (selectionMode) {
                        "calendar" -> "Choose a workout for your calendar"
                        "exercises" -> "Choose an exercise to add"
                        "workouts" -> "Choose a workout to start"
                        "programs" -> "Choose a program to follow"
                        else -> "Select an item"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onCancelClick) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LibraryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocalLibrary,
            contentDescription = null,
            tint = orangeGlow,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "Library",
            color = textWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = {
            Text(
                text = "Search...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = orangePrimary
            )
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = surfaceColor,
            unfocusedContainerColor = surfaceColor,
            focusedBorderColor = orangePrimary,
            unfocusedBorderColor = orangePrimary.copy(alpha = 0.3f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = orangePrimary
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun LibraryTabs(tabs: List<String>, selectedTab: Int, onTabSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = orangePrimary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                height = 2.dp,
                color = orangeGlow
            )
        },
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                modifier = Modifier.padding(horizontal = 0.dp),
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == index) textWhite else textGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            )
        }
    }
}

@Composable
private fun ExercisesGrid(
    exercises: List<Exercise>,
    isSelectionMode: Boolean = false,
    onExerciseClick: (Exercise) -> Unit,
    onDeleteClick: ((String) -> Unit)? = null,
    showDeleteButton: Boolean = false,
    searchQuery: String = ""
) {
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    if (exercises.isEmpty()) {
        if (searchQuery.isNotBlank()) {
            SearchEmptyState(searchQuery = searchQuery, itemType = "exercises")
        } else {
            EmptyState(message = "No exercises found.\nTry adjusting your filters.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(exercises) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    isSelectionMode = isSelectionMode,
                    showDeleteButton = showDeleteButton,
                    onClick = {
                        if (isSelectionMode) {
                            onExerciseClick(exercise)
                        } else {
                            selectedExercise = exercise
                        }
                    },
                    onDeleteClick = if (showDeleteButton && onDeleteClick != null) {
                        { onDeleteClick(exercise.id) }
                    } else null
                )
            }
        }
    }

    // Exercise Detail Dialog
    selectedExercise?.let { exercise ->
        ExerciseDetailDialog(
            exercise = exercise,
            onDismiss = { selectedExercise = null },
            showDeleteButton = showDeleteButton,
            onDelete = if (showDeleteButton && onDeleteClick != null) {
                {
                    onDeleteClick(exercise.id)
                    selectedExercise = null
                }
            } else null
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    isSelectionMode: Boolean = false,
    showDeleteButton: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(
            width = if (isSelectionMode) 2.dp else 1.dp,
            color = if (isSelectionMode) orangePrimary else orangePrimary.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header - Name & VBT badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = exercise.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (exercise.supportsPowerScore) {
                            VbtBadge(icon = "⚡", tooltip = "Power Score")
                        }
                        if (exercise.supportsTechniqueScore) {
                            VbtBadge(icon = "✓", tooltip = "Technique Score")
                        }

                        if (showDeleteButton && !isSelectionMode && onDeleteClick != null) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Delete Exercise", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Main info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = orangeGlow,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = exercise.mainMuscle ?: "Unknown",
                            color = orangeGlow,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = exercise.equipment?.firstOrNull() ?: "No equipment",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // Sport tags & tracking
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!exercise.sports.isNullOrEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(exercise.sports.take(2).size) { index ->
                                val sport = exercise.sports[index]
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = orangePrimary.copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, orangePrimary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = sport,
                                        color = orangeGlow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            if (exercise.sports.size > 2) {
                                item {
                                    Text(
                                        text = "+${exercise.sports.size - 2}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (exercise.trackWeight) TrackingIndicator(icon = "kg", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (exercise.trackReps) TrackingIndicator(icon = "×", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (exercise.trackDuration) TrackingIndicator(icon = "⏱", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (exercise.trackDistance) TrackingIndicator(icon = "m", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (exercise.trackRpe) TrackingIndicator(icon = "RPE", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Selection indicator
            if (isSelectionMode) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Add",
                    tint = orangePrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun VbtBadge(icon: String, tooltip: String) {
    Surface(
        shape = CircleShape,
        color = orangePrimary,
        modifier = Modifier.size(20.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = icon,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TrackingIndicator(icon: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = icon,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ExerciseDetailSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                color = orangeGlow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        content()
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TrackingChip(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, textGray.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SearchEmptyState(
    searchQuery: String,
    itemType: String = "items"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Search icon with subtle animation feel
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = orangePrimary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No $itemType found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No results for \"$searchQuery\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Helpful suggestions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = surfaceColor
            ),
            border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Try:",
                    style = MaterialTheme.typography.labelLarge,
                    color = orangePrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SearchSuggestionItem("Check your spelling")
                SearchSuggestionItem("Use simpler keywords (e.g., \"squat\" instead of \"barbell back squat\")")
                SearchSuggestionItem("Search by muscle group (e.g., \"chest\", \"legs\")")
                SearchSuggestionItem("Try German terms: \"Kniebeuge\", \"Bankdrücken\"")
            }
        }
    }
}

@Composable
private fun SearchSuggestionItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = orangePrimary,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkoutsGrid(
    workouts: List<Workout>,
    isSelectionMode: Boolean = false,
    onWorkoutClick: (Workout) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    if (workouts.isEmpty()) {
        EmptyState(message = "You haven't created any workouts yet.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(workouts) { workout ->
                WorkoutCard(
                    workout = workout,
                    isSelectionMode = isSelectionMode,
                    onClick = { onWorkoutClick(workout) },
                    onDeleteClick = { onDeleteClick(workout.id) }
                )
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: Workout,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(
            width = if (isSelectionMode) 2.dp else 1.dp,
            color = if (isSelectionMode) orangePrimary else orangePrimary.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = workout.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSelectionMode) {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete Workout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(text = "${workout.exercises.size} exercises", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            if (isSelectionMode) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Add",
                    tint = orangePrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgramsGrid(
    programs: List<Program>,
    isSelectionMode: Boolean = false,
    onProgramClick: (Program) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    if (programs.isEmpty()) {
        EmptyState(message = "You haven't created any programs yet.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(programs) { program ->
                ProgramCard(
                    program = program,
                    isSelectionMode = isSelectionMode,
                    onClick = { onProgramClick(program) },
                    onDeleteClick = { onDeleteClick(program.id) }
                )
            }
        }
    }
}

@Composable
private fun ProgramCard(
    program: Program,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(
            width = if (isSelectionMode) 2.dp else 1.dp,
            color = if (isSelectionMode) orangePrimary else orangePrimary.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = program.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSelectionMode) {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete Program", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(text = "${program.durationInWeeks} weeks", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            if (isSelectionMode) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Add",
                    tint = orangePrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// USER PROGRAMS GRID (My Programs - saved from templates)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun UserProgramsGrid(
    programs: List<UserProgram>,
    isSelectionMode: Boolean = false,
    onProgramClick: (UserProgram) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    if (programs.isEmpty()) {
        EmptyState(message = "You haven't started any programs yet.\nBrowse Templates to find a program!")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(programs) { program ->
                UserProgramCard(
                    program = program,
                    isSelectionMode = isSelectionMode,
                    onClick = { onProgramClick(program) },
                    onDeleteClick = { onDeleteClick(program.id) }
                )
            }
        }
    }
}

@Composable
private fun UserProgramCard(
    program: UserProgram,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(
            width = if (isSelectionMode) 2.dp else 1.dp,
            color = if (isSelectionMode) orangePrimary else orangePrimary.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = program.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSelectionMode) {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete Program", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Duration & Days per week
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = orangePrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${program.durationWeeks} weeks",
                            color = orangeGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    program.daysPerWeek?.let { days ->
                        Text(
                            text = "${days}x/week",
                            color = textGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Current week progress
                Text(
                    text = "Week ${program.currentWeek} of ${program.durationWeeks}",
                    color = orangePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.weight(1f))

                // Status badge
                val statusColor = when (program.status) {
                    "active" -> Color(0xFF4CAF50)
                    "completed" -> orangePrimary
                    "paused" -> Color(0xFFFFC107)
                    else -> textGray
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = program.status.replaceFirstChar { it.uppercase() },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (isSelectionMode) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Add",
                    tint = orangePrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EXERCISE DETAIL DIALOG
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    showDeleteButton: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = dialogBackground,  // More solid background for readable text
        contentColor = textWhite,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (exercise.supportsPowerScore) {
                        VbtBadge(icon = "⚡", tooltip = "Power Score")
                    }
                    if (exercise.supportsTechniqueScore) {
                        VbtBadge(icon = "✓", tooltip = "Technique Score")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Muscles
                item {
                    ExerciseDetailSection(
                        title = "MUSCLES",
                        icon = Icons.Default.Accessibility
                    ) {
                        DetailItem(label = "Primary", value = exercise.mainMuscle ?: "Unknown", color = orangeGlow)
                        if (!exercise.secondaryMuscles.isNullOrEmpty()) {
                            DetailItem(
                                label = "Secondary",
                                value = exercise.secondaryMuscles.joinToString(", "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Equipment
                if (!exercise.equipment.isNullOrEmpty()) {
                    item {
                        ExerciseDetailSection(
                            title = "EQUIPMENT",
                            icon = Icons.Default.FitnessCenter
                        ) {
                            Text(
                                text = exercise.equipment.joinToString(", "),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Sports
                if (!exercise.sports.isNullOrEmpty()) {
                    item {
                        ExerciseDetailSection(
                            title = "SPORTS",
                            icon = Icons.Default.EmojiEvents
                        ) {
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(exercise.sports.size) { index ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = orangePrimary.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = exercise.sports[index],
                                            color = orangeGlow,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tracking
                item {
                    ExerciseDetailSection(
                        title = "TRACKING",
                        icon = Icons.Default.Analytics
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (exercise.trackWeight) TrackingChip("Weight")
                                if (exercise.trackReps) TrackingChip("Reps")
                                if (exercise.trackSets) TrackingChip("Sets")
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (exercise.trackDuration) TrackingChip("Duration")
                                if (exercise.trackDistance) TrackingChip("Distance")
                                if (exercise.trackRpe) TrackingChip("RPE")
                            }
                        }
                    }
                }

                // Parameters
                if (exercise.tempo != null || exercise.restTimeInSeconds != null) {
                    item {
                        ExerciseDetailSection(
                            title = "PARAMETERS",
                            icon = Icons.Default.Settings
                        ) {
                            exercise.tempo?.let {
                                DetailItem(label = "Tempo", value = it, color = MaterialTheme.colorScheme.onSurface)
                            }
                            exercise.restTimeInSeconds?.let {
                                DetailItem(label = "Rest Time", value = "${it}s", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // VBT Settings
                if (exercise.supportsPowerScore || exercise.supportsTechniqueScore) {
                    item {
                        ExerciseDetailSection(
                            title = "VBT SETTINGS",
                            icon = Icons.Default.Speed
                        ) {
                            if (exercise.supportsPowerScore) {
                                DetailItem(label = "Power Score", value = "Enabled ⚡", color = orangeGlow)
                            }
                            if (exercise.supportsTechniqueScore) {
                                DetailItem(label = "Technique Score", value = "Enabled ✓", color = orangeGlow)
                            }
                            exercise.vbtCategory?.let {
                                DetailItem(label = "VBT Category", value = it, color = MaterialTheme.colorScheme.onSurface)
                            }
                            exercise.vbtMeasurementType?.let {
                                DetailItem(label = "Measurement", value = it.replaceFirstChar { char -> char.uppercase() }, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Technique Guide
                if (!exercise.techniqueSections.isNullOrEmpty()) {
                    item {
                        ExerciseDetailSection(
                            title = "TECHNIQUE GUIDE",
                            icon = Icons.Default.MenuBook
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                exercise.techniqueSections.forEach { section ->
                                    if (section.bullets.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = section.title,
                                                color = orangeGlow,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            section.bullets.forEach { bullet ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "•",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = bullet,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp,
                                                        lineHeight = 18.sp,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes
                if (!exercise.notes.isNullOrEmpty()) {
                    item {
                        ExerciseDetailSection(
                            title = "NOTES",
                            icon = Icons.Default.Notes
                        ) {
                            Text(
                                text = exercise.notes,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Tutorial
                if (!exercise.tutorial.isNullOrEmpty()) {
                    item {
                        ExerciseDetailSection(
                            title = "TUTORIAL",
                            icon = Icons.Default.PlayCircle
                        ) {
                            Text(
                                text = exercise.tutorial,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Video URL
                if (!exercise.videoUrl.isNullOrEmpty()) {
                    item {
                        ExerciseDetailSection(
                            title = "VIDEO",
                            icon = Icons.Default.VideoLibrary
                        ) {
                            Text(
                                text = exercise.videoUrl,
                                color = orangeGlow,
                                fontSize = 12.sp,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        }
                    }
                }

                // Delete button (only show for user-created exercises in My Exercises tab)
                if (showDeleteButton && onDelete != null) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red.copy(alpha = 0.2f),
                                contentColor = Color.Red
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Exercise", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DATE PICKER DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = {
            Text(
                "Select Date",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Month Navigator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, "Previous Month", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, "Next Month", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Mini Calendar Grid
                MiniCalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDateSelected(selectedDate) },
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
            ) {
                Text("Add to ${selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun MiniCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Calendar days
        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value - 1
        val daysInMonth = currentMonth.lengthOfMonth()

        var dayCounter = 1
        for (week in 0..5) {
            if (dayCounter > daysInMonth) break

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayOfWeek in 0..6) {
                    val dayNumber = if (week == 0 && dayOfWeek < firstDayOfWeek) {
                        null
                    } else if (dayCounter <= daysInMonth) {
                        dayCounter++
                    } else {
                        null
                    }

                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNumber != null) {
                            val date = currentMonth.atDay(dayNumber)
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = when {
                                            isSelected -> orangePrimary
                                            isToday -> orangePrimary.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    color = when {
                                        isSelected -> textWhite
                                        isToday -> orangeGlow
                                        else -> textWhite
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = { Text("Delete $itemName?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = { Text("This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = orangePrimary)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// WORKOUT SUB-TABS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WorkoutSubTabs(selectedSubTab: Int, onSubTabSelected: (Int) -> Unit, assignedCount: Int = 0) {
    val subTabs = listOf("MY WORKOUTS", "TEMPLATES", if (assignedCount > 0) "COACH ($assignedCount)" else "COACH")

    TabRow(
        selectedTabIndex = selectedSubTab,
        containerColor = Color.Transparent,
        contentColor = orangePrimary,
        indicator = { tabPositions ->
            if (selectedSubTab < tabPositions.size) {
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    height = 2.dp,
                    color = orangeGlow
                )
            }
        },
        divider = {}
    ) {
        subTabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedSubTab == index,
                onClick = { onSubTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSubTab == index) textWhite else textGray,
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EXERCISE SUB-TABS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ExerciseSubTabs(selectedSubTab: Int, onSubTabSelected: (Int) -> Unit) {
    val subTabs = listOf("FOR YOU", "ALL EXERCISES")

    TabRow(
        selectedTabIndex = selectedSubTab,
        containerColor = Color.Transparent,
        contentColor = orangePrimary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                height = 2.dp,
                color = orangeGlow
            )
        },
        divider = {}
    ) {
        subTabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedSubTab == index,
                onClick = { onSubTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSubTab == index) textWhite else textGray,
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRAM SUB-TABS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgramSubTabs(selectedSubTab: Int, onSubTabSelected: (Int) -> Unit) {
    val subTabs = listOf("MY PROGRAMS", "TEMPLATES")

    TabRow(
        selectedTabIndex = selectedSubTab,
        containerColor = Color.Transparent,
        contentColor = orangePrimary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                height = 2.dp,
                color = orangeGlow
            )
        },
        divider = {}
    ) {
        subTabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedSubTab == index,
                onClick = { onSubTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedSubTab == index) textWhite else textGray,
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PERSONALIZED FOR YOU INDICATOR
// Shows user their content is personalized based on their sport
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PersonalizedForYouIndicator(
    sportName: String,
    trainingFocusDescription: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = orangePrimary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Personalized for $sportName",
                    color = orangeGlow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = trainingFocusDescription,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
@Composable
private fun WorkoutTemplatesGrid(
    templates: List<com.example.menotracker.viewmodels.WorkoutTemplate>,
    isSelectionMode: Boolean = false,
    onTemplateClick: (com.example.menotracker.viewmodels.WorkoutTemplate) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    if (templates.isEmpty()) {
        EmptyState(message = "No workout templates available.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                WorkoutTemplateCard(
                    template = template,
                    isSelectionMode = isSelectionMode,
                    onClick = { onTemplateClick(template) },
                    onDeleteClick = { onDeleteClick(template.id) }
                )
            }
        }
    }
}

@Composable
private fun WorkoutTemplateCard(
    template: com.example.menotracker.viewmodels.WorkoutTemplate,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(
            width = if (isSelectionMode) 2.dp else 1.dp,
            color = if (isSelectionMode) orangePrimary else orangePrimary.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = template.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Number of exercises (null safety for Gson deserialization)
                val exercises = template.exercises ?: emptyList()
                Text(
                    text = "${exercises.size} exercises",
                    color = orangePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Total sets count
                val totalSets = exercises.sumOf { it.sets.size }
                Text(
                    text = "$totalSets total sets",
                    color = textGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Preview first 2 exercises
                if (exercises.isNotEmpty()) {
                    val preview = exercises.take(2).joinToString(", ") { it.exerciseName }
                    Text(
                        text = preview,
                        color = textGray.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRAM TEMPLATES GRID
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgramTemplatesGrid(
    templates: List<com.example.menotracker.data.models.ProgramTemplate>,
    isSelectionMode: Boolean = false,
    onTemplateClick: (com.example.menotracker.data.models.ProgramTemplate) -> Unit
) {
    if (templates.isEmpty()) {
        EmptyState(message = "No program templates available.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                ProgramTemplateCard(
                    template = template,
                    isSelectionMode = isSelectionMode,
                    onClick = { onTemplateClick(template) }
                )
            }
        }
    }
}

@Composable
private fun ProgramTemplateCard(
    template: com.example.menotracker.data.models.ProgramTemplate,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(
            width = if (isSelectionMode) 2.dp else 1.dp,
            color = if (isSelectionMode) orangePrimary else orangePrimary.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.fillMaxSize()) {
                // Name
                Text(
                    text = template.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Duration & Days per week
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = orangePrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${template.durationWeeks} weeks",
                            color = orangeGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    template.daysPerWeek?.let { days ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = surfaceColor
                        ) {
                            Text(
                                text = "${days}x/week",
                                color = textGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sport & Difficulty
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = template.displaySport,
                        color = orangePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        color = textGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = template.displayDifficulty,
                        color = textGray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Description preview
                template.description?.let { desc ->
                    Text(
                        text = desc,
                        color = textGray.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }

            // Selection indicator
            if (isSelectionMode) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = "Add",
                    tint = orangePrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRAM TEMPLATE DETAIL DIALOG
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramTemplateDetailDialog(
    template: com.example.menotracker.data.models.ProgramTemplate,
    onDismiss: () -> Unit,
    onStartProgram: (com.example.menotracker.data.models.ProgramTemplate) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var phases by remember { mutableStateOf<List<com.example.menotracker.data.models.ProgramTemplatePhase>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch phases when dialog opens
    LaunchedEffect(template.id) {
        isLoading = true
        phases = ProgramRepository.fetchProgramPhases(template.id)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        contentColor = textWhite,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${template.displaySport} • ${template.displayDifficulty}",
                        color = orangeGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Duration badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = orangePrimary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = orangeGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${template.durationWeeks} Weeks",
                            color = orangeGlow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                template.daysPerWeek?.let { days ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, textGray.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = textGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$days Days/Week",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Description
            template.description?.let { desc ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DESCRIPTION",
                        color = orangeGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = desc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // PROGRAM PHASES (Training Blocks)
            // ═══════════════════════════════════════════════════════════════
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PROGRAM PHASES",
                    color = orangeGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = orangePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (phases.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, textGray.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "No phases defined for this program",
                            color = textGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    phases.forEachIndexed { index, phase ->
                        ProgramPhaseCard(
                            phase = phase,
                            phaseNumber = index + 1,
                            totalPhases = phases.size
                        )
                    }
                }
            }

            // Periodization Type
            template.periodizationType?.let { periodization ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PERIODIZATION TYPE",
                        color = orangeGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = orangePrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = periodization.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            color = orangeGlow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Goal Tags
            if (!template.goalTags.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "GOALS",
                        color = orangeGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(template.goalTags.size) { index ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = surfaceColor,
                                border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = template.goalTags[index].replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Equipment Required
            if (!template.equipmentRequired.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "EQUIPMENT NEEDED",
                        color = orangeGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(template.equipmentRequired.size) { index ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = textGray.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = template.equipmentRequired[index].replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Start Program Button
            Button(
                onClick = { onStartProgram(template) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary,
                    contentColor = textWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Start This Program",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRAM PHASE CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgramPhaseCard(
    phase: com.example.menotracker.data.models.ProgramTemplatePhase,
    phaseNumber: Int,
    totalPhases: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardBackground,
        border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Phase header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Phase number indicator
                    Surface(
                        shape = CircleShape,
                        color = orangePrimary
                    ) {
                        Text(
                            text = "$phaseNumber",
                            color = textWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = phase.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Week range badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = orangePrimary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = phase.weekRangeDisplay,
                        color = orangeGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Phase description
            phase.description?.let { desc ->
                Text(
                    text = desc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // Volume & Intensity indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Volume indicator
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when {
                        (phase.volumeModifier ?: 1.0) >= 1.1 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        (phase.volumeModifier ?: 1.0) <= 0.7 -> Color(0xFFF44336).copy(alpha = 0.2f)
                        else -> textGray.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = phase.volumeDisplay,
                        color = when {
                            (phase.volumeModifier ?: 1.0) >= 1.1 -> Color(0xFF4CAF50)
                            (phase.volumeModifier ?: 1.0) <= 0.7 -> Color(0xFFF44336)
                            else -> textGray
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Intensity indicator
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when {
                        (phase.intensityModifier ?: 1.0) >= 0.9 -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        (phase.intensityModifier ?: 1.0) <= 0.7 -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        else -> textGray.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = phase.intensityDisplay,
                        color = when {
                            (phase.intensityModifier ?: 1.0) >= 0.9 -> Color(0xFFFF9800)
                            (phase.intensityModifier ?: 1.0) <= 0.7 -> Color(0xFF2196F3)
                            else -> textGray
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Duration
                Text(
                    text = "${phase.durationWeeks} week${if (phase.durationWeeks > 1) "s" else ""}",
                    color = textGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// START PROGRAM CONFIRMATION DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StartProgramConfirmationDialog(
    programName: String,
    isLoading: Boolean,
    onConfirm: (loadToTraining: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = surfaceColor,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = orangePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Start Program",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "\"$programName\" will be added to My Programs.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = orangePrimary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Load to Training Screen?",
                            color = orangeGlow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Start training with this program immediately after saving.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = orangePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "No" - Just save to My Programs
                TextButton(
                    onClick = { onConfirm(false) },
                    enabled = !isLoading
                ) {
                    Text(
                        "Save Only",
                        color = if (isLoading) textGray else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                // "Yes" - Save and load to Training Screen
                Button(
                    onClick = { onConfirm(true) },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = orangePrimary,
                        contentColor = textWhite
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Start Training", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(
                    "Cancel",
                    color = if (isLoading) textGray else orangePrimary
                )
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// ASSIGNED WORKOUT CARD (Coach-assigned workouts)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AssignedWorkoutCard(
    assignment: WorkoutAssignmentRepository.AssignedWorkout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val template = assignment.workoutTemplate

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Coach badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(orangePrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = orangePrimary
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = assignment.coachName ?: "Coach",
                    fontSize = 11.sp,
                    color = orangePrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Workout name
            Text(
                text = template.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Exercise count (null safety for Gson deserialization)
            val templateExercises = template.exercises ?: emptyList()
            val exerciseCount = templateExercises.size
            val totalSets = templateExercises.sumOf { it.sets.size }
            Text(
                text = "$exerciseCount exercises • $totalSets sets",
                fontSize = 12.sp,
                color = textGray
            )

            // Coach notes (if any)
            assignment.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = notes,
                        fontSize = 11.sp,
                        color = textGray.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}
