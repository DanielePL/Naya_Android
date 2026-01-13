// screens/wod/WodDetailScreen.kt
package com.example.menotracker.screens.wod

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.AICreatedExercise
import com.example.menotracker.data.ExerciseRepository
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.data.NayaBackendRepository
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaSurface
import com.example.menotracker.viewmodels.WorkoutTemplate
import kotlinx.coroutines.launch

/**
 * WOD Detail Screen - View workout details and log scores
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WodDetailScreen(
    wodId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    onStartWorkout: (WorkoutTemplate) -> Unit
) {
    val scope = rememberCoroutineScope()

    var wodWithMovements by remember { mutableStateOf<WodWithMovements?>(null) }
    var results by remember { mutableStateOf<List<WodResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Score logging dialog
    var showLogScoreDialog by remember { mutableStateOf(false) }

    // Scaling/gender selection dialog for starting workout
    var showStartWorkoutDialog by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) }

    // Unknown exercises dialog state
    var showUnknownExercisesDialog by remember { mutableStateOf(false) }
    var unknownExercises by remember { mutableStateOf<List<ExerciseWithSets>>(emptyList()) }
    var pendingWorkoutTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var isCreatingExercises by remember { mutableStateOf(false) }
    var creationProgress by remember { mutableStateOf("") }

    // Load WOD details and results
    LaunchedEffect(wodId) {
        isLoading = true
        error = null

        // Load WOD details
        val wodResult = NayaBackendRepository.getWodDetail(wodId)
        wodResult.onSuccess { wod ->
            wodWithMovements = wod
        }.onFailure { e ->
            error = e.message
        }

        // Load user's results for this WOD
        // TODO: Add getWodResults to NayaBackendService
        // For now, results will be empty

        isLoading = false
    }

    val wod = wodWithMovements?.wod
    val movements = wodWithMovements?.movements ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(wod?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (wodWithMovements != null && !isConverting) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Log Score button (secondary)
                        OutlinedButton(
                            onClick = { showLogScoreDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NayaPrimary
                            )
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Log Score")
                        }

                        // Start Workout button (primary)
                        Button(
                            onClick = { showStartWorkoutDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NayaPrimary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start Workout")
                        }
                    }
                }
            } else if (isConverting) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NayaPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Creating workout...")
                    }
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NayaPrimary)
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Failed to load WOD")
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
            }

            wod != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // WOD Header Card
                    item {
                        WodHeaderCard(wod = wod)
                    }

                    // Movements section
                    item {
                        Text(
                            "Movements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    itemsIndexed(movements) { index, movement ->
                        MovementDetailCard(
                            index = index + 1,
                            movement = movement,
                            isMale = true // TODO: Get from user profile
                        )
                    }

                    // Scaling options (if available)
                    wodWithMovements?.scaling?.takeIf { it.isNotEmpty() }?.let { scaling ->
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Scaling Options",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        scaling.forEach { (level, options) ->
                            item {
                                ScalingCard(
                                    scalingLevel = level,
                                    options = options
                                )
                            }
                        }
                    }

                    // User's Results
                    if (results.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Your Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(results) { result ->
                            ResultCard(result = result)
                        }
                    }

                    // Spacer for FAB
                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Log Score Dialog
    if (showLogScoreDialog && wod != null) {
        LogScoreDialog(
            wod = wod,
            onDismiss = { showLogScoreDialog = false },
            onSubmit = { scoreType, rounds, reps, timeSeconds, weightKg, scalingLevel, notes ->
                scope.launch {
                    val result = NayaBackendRepository.logWodResult(
                        wodId = wodId,
                        userId = userId,
                        scoreType = scoreType,
                        roundsCompleted = rounds,
                        repsCompleted = reps,
                        timeSeconds = timeSeconds,
                        weightKg = weightKg,
                        scalingLevel = scalingLevel,
                        notes = notes
                    )

                    result.onSuccess {
                        showLogScoreDialog = false
                        // Refresh results
                        // TODO: Reload results
                    }.onFailure { e ->
                        // Show error toast
                    }
                }
            }
        )
    }

    // Start Workout Dialog - Choose scaling and gender
    if (showStartWorkoutDialog && wodWithMovements != null) {
        StartWorkoutDialog(
            wod = wodWithMovements!!.wod,
            hasScalingOptions = wodWithMovements!!.scaling.isNotEmpty(),
            onDismiss = { showStartWorkoutDialog = false },
            onStart = { scalingLevel, isMale ->
                showStartWorkoutDialog = false
                isConverting = true

                try {
                    // Convert WOD to WorkoutTemplate (synchronous)
                    val workoutTemplate = WodToWorkoutConverter.convertToWorkout(
                        wodWithMovements = wodWithMovements!!,
                        scalingLevel = scalingLevel,
                        isMale = isMale
                    )

                    // Check for unknown exercises (IDs starting with "wod_")
                    val unknown = workoutTemplate.exercises.filter {
                        it.exerciseId.startsWith("wod_")
                    }

                    if (unknown.isNotEmpty()) {
                        // Show AI Coach dialog for unknown exercises
                        unknownExercises = unknown
                        pendingWorkoutTemplate = workoutTemplate
                        isConverting = false
                        showUnknownExercisesDialog = true
                    } else {
                        isConverting = false
                        // All exercises found - start workout
                        onStartWorkout(workoutTemplate)
                    }
                } catch (e: Exception) {
                    isConverting = false
                    android.util.Log.e("WodDetailScreen", "Failed to convert WOD", e)
                    // TODO: Show error toast
                }
            }
        )
    }

    // Unknown Exercises Dialog - Ask AI Coach to create them
    if (showUnknownExercisesDialog && unknownExercises.isNotEmpty()) {
        UnknownExercisesDialog(
            unknownExercises = unknownExercises,
            isCreating = isCreatingExercises,
            progress = creationProgress,
            onDismiss = {
                showUnknownExercisesDialog = false
                unknownExercises = emptyList()
                pendingWorkoutTemplate = null
            },
            onSkipAndStart = {
                // Start workout with placeholder exercises anyway
                showUnknownExercisesDialog = false
                pendingWorkoutTemplate?.let { onStartWorkout(it) }
                unknownExercises = emptyList()
                pendingWorkoutTemplate = null
            },
            onCreateWithAI = {
                // Use AI Coach to create the exercises
                isCreatingExercises = true

                scope.launch {
                    try {
                        val movementNames = unknownExercises.map { it.exerciseName }
                        creationProgress = "Creating ${movementNames.size} exercises..."

                        val result = NayaBackendRepository.createExercisesBatchFromAI(
                            movements = movementNames,
                            userId = userId,
                            context = "CrossFit WOD: ${wodWithMovements?.wod?.name}"
                        )

                        result.onSuccess { response ->
                            if (response.createdCount > 0) {
                                creationProgress = "Created ${response.createdCount} exercises!"

                                // Add new exercises to local repository cache
                                response.created.forEach { aiExercise ->
                                    val exercise = aiExercise.toExercise()
                                    ExerciseRepository.addExerciseToCache(exercise)
                                }

                                // Update the workout template with real exercise IDs
                                val updatedTemplate = updateWorkoutWithNewExercises(
                                    pendingWorkoutTemplate!!,
                                    response.created
                                )

                                // Small delay so user sees success message
                                kotlinx.coroutines.delay(800)

                                isCreatingExercises = false
                                showUnknownExercisesDialog = false

                                // Start workout with updated template
                                onStartWorkout(updatedTemplate)

                                unknownExercises = emptyList()
                                pendingWorkoutTemplate = null
                            } else {
                                creationProgress = "Failed to create exercises"
                                kotlinx.coroutines.delay(1500)
                                isCreatingExercises = false
                            }
                        }.onFailure { e ->
                            creationProgress = "Error: ${e.message}"
                            kotlinx.coroutines.delay(1500)
                            isCreatingExercises = false
                        }
                    } catch (e: Exception) {
                        creationProgress = "Error: ${e.message}"
                        kotlinx.coroutines.delay(1500)
                        isCreatingExercises = false
                    }
                }
            }
        )
    }
}

/**
 * Update workout template with newly created exercise IDs
 */
private fun updateWorkoutWithNewExercises(
    template: WorkoutTemplate,
    createdExercises: List<AICreatedExercise>
): WorkoutTemplate {
    val updatedExercises = template.exercises.map { exerciseWithSets ->
        if (exerciseWithSets.exerciseId.startsWith("wod_")) {
            // Find matching created exercise by name
            val createdExercise = createdExercises.find {
                it.name.equals(exerciseWithSets.exerciseName, ignoreCase = true) ||
                exerciseWithSets.exerciseName.contains(it.name, ignoreCase = true) ||
                it.name.contains(exerciseWithSets.exerciseName, ignoreCase = true)
            }

            if (createdExercise != null) {
                exerciseWithSets.copy(
                    exerciseId = createdExercise.id,
                    muscleGroup = createdExercise.category ?: exerciseWithSets.muscleGroup,
                    equipment = createdExercise.equipment?.firstOrNull() ?: exerciseWithSets.equipment
                )
            } else {
                exerciseWithSets
            }
        } else {
            exerciseWithSets
        }
    }

    return template.copy(exercises = updatedExercises)
}

/**
 * Convert AICreatedExercise to Exercise model
 */
private fun AICreatedExercise.toExercise(): Exercise {
    return Exercise(
        id = this.id,
        name = this.name,
        category = this.category,
        secondaryMuscles = this.secondaryMuscleGroups,
        equipment = this.equipment,
        level = this.level,
        visibility = this.visibility,
        sports = this.sports,
        trackReps = this.trackReps,
        trackSets = this.trackSets,
        trackWeight = this.trackWeight,
        trackDuration = this.trackDuration,
        trackDistance = this.trackDistance,
        tutorial = this.tutorial,
        notes = this.notes
    )
}

/**
 * Dialog shown when unknown exercises are detected in a WOD
 * Offers to create them via AI Coach
 */
@Composable
private fun UnknownExercisesDialog(
    unknownExercises: List<ExerciseWithSets>,
    isCreating: Boolean,
    progress: String,
    onDismiss: () -> Unit,
    onSkipAndStart: () -> Unit,
    onCreateWithAI: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Psychology,
                    null,
                    tint = NayaPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text("Unknown Exercises")
                    Text(
                        "${unknownExercises.size} not in library",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isCreating) {
                    // Creating state
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NayaPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            progress,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "AI Coach is analyzing and creating exercise definitions...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Not creating - show exercises list
                    Text(
                        "These movements from the WOD are not in our exercise library:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        color = Color(0xFFFF9800).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            unknownExercises.forEach { exercise ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        exercise.exerciseName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // AI Coach explanation
                    Surface(
                        color = NayaPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                null,
                                tint = NayaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "AI Coach can help!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NayaPrimary
                                )
                                Text(
                                    "Create these exercises automatically with proper muscle groups, equipment, and tracking settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NayaPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isCreating) {
                Button(
                    onClick = onCreateWithAI,
                    colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Create with AI")
                }
            }
        },
        dismissButton = {
            if (!isCreating) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(onClick = onSkipAndStart) {
                        Text("Skip & Start")
                    }
                }
            }
        }
    )
}

@Composable
private fun WodHeaderCard(wod: WodTemplate) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = NayaPrimary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Type and time cap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NayaPrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        wod.getWodTypeDisplay(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                wod.getTimeCapDisplay()?.let { timeCap ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = NayaPrimary
                        )
                        Text(
                            timeCap,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NayaPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // WOD Name
            Text(
                wod.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Description
            wod.description?.let { desc ->
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Rep scheme
            wod.getRepSchemeDisplay()?.let { scheme ->
                Surface(
                    color = NayaPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Numbers,
                            null,
                            tint = NayaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            scheme,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NayaPrimary
                        )
                    }
                }
            }

            // Target rounds
            wod.targetRounds?.let { rounds ->
                Text(
                    "$rounds Rounds",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                wod.difficulty?.let { difficulty ->
                    val color = Color(wod.getDifficultyColor())
                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            difficulty.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = color
                        )
                    }
                }

                wod.estimatedDurationMinutes?.let { duration ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "~$duration min",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        "${wod.completionsCount} completions",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }

            // Equipment needed
            wod.equipmentNeeded?.takeIf { it.isNotEmpty() }?.let { equipment ->
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Build,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        equipment.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Source box
            wod.sourceBoxName?.let { boxName ->
                Text(
                    "From: $boxName",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MovementDetailCard(
    index: Int,
    movement: WodMovement,
    isMale: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = NayaSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index badge
            Surface(
                color = NayaPrimary,
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "$index",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    movement.movementName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    movement.getFullDisplay(isMale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                movement.notes?.let { notes ->
                    Text(
                        notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScalingCard(
    scalingLevel: String,
    options: List<WodScaling>
) {
    val levelColor = when (scalingLevel) {
        "rx" -> NayaPrimary
        "scaled" -> Color(0xFFFF9800)
        "foundations" -> Color(0xFF4CAF50)
        "masters" -> Color(0xFF9C27B0)
        else -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = levelColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                scalingLevel.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = levelColor
            )

            Spacer(Modifier.height(8.dp))

            options.forEach { scaling ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("•", color = levelColor)
                    Text(
                        buildString {
                            scaling.alternativeMovementName?.let { append("$it: ") }
                            scaling.reps?.let { append("$it reps ") }
                            scaling.weightKgMale?.let { append("${it.toInt()}/${scaling.weightKgFemale?.toInt() ?: 0}kg") }
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: WodResult) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = NayaSurface
        ),
        shape = RoundedCornerShape(12.dp)
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
                    result.getScoreDisplay(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    result.getScalingDisplay(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            result.completedAt?.let { date ->
                Text(
                    date.take(10), // Just the date part
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogScoreDialog(
    wod: WodTemplate,
    onDismiss: () -> Unit,
    onSubmit: (
        scoreType: String,
        rounds: Int?,
        reps: Int?,
        timeSeconds: Int?,
        weightKg: Double?,
        scalingLevel: String,
        notes: String?
    ) -> Unit
) {
    var scalingLevel by remember { mutableStateOf("rx") }
    var rounds by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Determine score type based on WOD type
    val scoreType = when (wod.wodType) {
        "amrap" -> "rounds_reps"
        "for_time", "rft", "chipper" -> "time"
        "max_effort" -> "weight"
        else -> wod.scoringType
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Your Score") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scaling level selector
                Text("Scaling", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("rx" to "Rx", "scaled" to "Scaled").forEach { (value, label) ->
                        FilterChip(
                            selected = scalingLevel == value,
                            onClick = { scalingLevel = value },
                            label = { Text(label) }
                        )
                    }
                }

                HorizontalDivider()

                // Score input based on type
                when (scoreType) {
                    "rounds_reps" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = rounds,
                                onValueChange = { if (it.all { c -> c.isDigit() }) rounds = it },
                                label = { Text("Rounds") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = reps,
                                onValueChange = { if (it.all { c -> c.isDigit() }) reps = it },
                                label = { Text("+ Reps") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    "time" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = minutes,
                                onValueChange = { if (it.all { c -> c.isDigit() }) minutes = it },
                                label = { Text("Minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = seconds,
                                onValueChange = { if (it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) < 60) seconds = it },
                                label = { Text("Seconds") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    "weight" -> {
                        OutlinedTextField(
                            value = weightKg,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) weightKg = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalSeconds = if (scoreType == "time") {
                        ((minutes.toIntOrNull() ?: 0) * 60) + (seconds.toIntOrNull() ?: 0)
                    } else null

                    onSubmit(
                        scoreType,
                        rounds.toIntOrNull(),
                        reps.toIntOrNull(),
                        totalSeconds,
                        weightKg.toDoubleOrNull(),
                        scalingLevel,
                        notes.takeIf { it.isNotBlank() }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
            ) {
                Text("Save Score")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to configure workout before starting
 * Lets user choose scaling level and gender for weights
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartWorkoutDialog(
    wod: WodTemplate,
    hasScalingOptions: Boolean,
    onDismiss: () -> Unit,
    onStart: (scalingLevel: String, isMale: Boolean) -> Unit
) {
    var selectedScaling by remember { mutableStateOf("rx") }
    // Always use female weights for menopause app (isMale = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Start Workout")
                Text(
                    wod.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // WOD Type indicator
                Surface(
                    color = NayaPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                null,
                                tint = NayaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                wod.getWodTypeDisplay(),
                                fontWeight = FontWeight.Bold,
                                color = NayaPrimary
                            )
                        }

                        wod.getTimeCapDisplay()?.let { timeCap ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    null,
                                    tint = NayaPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    timeCap,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = NayaPrimary
                                )
                            }
                        }
                    }
                }

                // Scaling selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Scaling Level",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scalingOptions = if (hasScalingOptions) {
                            listOf("rx" to "Rx", "scaled" to "Scaled", "foundations" to "Foundations")
                        } else {
                            listOf("rx" to "Rx", "scaled" to "Scaled")
                        }

                        scalingOptions.forEach { (value, label) ->
                            val isSelected = selectedScaling == value
                            val color = when (value) {
                                "rx" -> NayaPrimary
                                "scaled" -> Color(0xFFFF9800)
                                "foundations" -> Color(0xFF4CAF50)
                                else -> Color.Gray
                            }

                            Surface(
                                onClick = { selectedScaling = value },
                                color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) color else Color.Gray
                                )
                            }
                        }
                    }
                }

                // Info text
                Surface(
                    color = Color(0xFF2196F3).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "The WOD will be converted to a workout template with the correct reps and weights for your selection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(selectedScaling, false) },  // Always female weights
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}