// app/src/main/java/com/example/myapplicationtest/screens/workoutbuilder/WorkoutBuilderScreen.kt

package com.example.menotracker.screens.workoutbuilder

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.menotracker.data.ExerciseSet
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.viewmodels.WorkoutBuilderViewModel
import com.example.menotracker.viewmodels.WorkoutTemplate
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip

// Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)
private val cardBorder = orangeGlow.copy(alpha = 0.5f)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF2a1f1a), Color(0xFF0f0f0f), Color(0xFF1a1410))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderScreen(
    viewModel: WorkoutBuilderViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToExerciseSelection: () -> Unit,
    onSaveAndStartSession: ((WorkoutTemplate) -> Unit)? = null
) {
    val context = LocalContext.current
    val workoutName by viewModel.currentWorkoutName.collectAsStateWithLifecycle()
    val selectedExercises by viewModel.selectedExercises.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val isTemplateCustomizationMode by viewModel.isTemplateCustomizationMode.collectAsStateWithLifecycle()
    val isILBTestMode by viewModel.isILBTestMode.collectAsStateWithLifecycle()

    // Debug: Log save button state
    val canSave = workoutName.isNotBlank() && selectedExercises.isNotEmpty()
    android.util.Log.d("WorkoutBuilder", "📝 Save check: name='$workoutName' (${workoutName.isNotBlank()}), exercises=${selectedExercises.size} (${selectedExercises.isNotEmpty()}), canSave=$canSave")

    var showDiscardDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(if (isEditMode) "Edit Workout" else "Create Workout")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (workoutName.isNotBlank() || selectedExercises.isNotEmpty()) {
                                showDiscardDialog = true
                            } else {
                                viewModel.clearCurrentWorkout()
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textWhite
                            )
                        }
                    },
                    actions = {
                        // Debug: Show state in TopBar subtitle
                        Text(
                            text = "${selectedExercises.size}ex",
                            color = if (canSave) Color.Green else Color.Red,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        TextButton(
                            onClick = {
                                android.util.Log.d("WorkoutBuilder", "💾 SAVE clicked - attempting to save")
                                if (viewModel.saveWorkout()) {
                                    Toast.makeText(
                                        context,
                                        "✓ Workout saved successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to save workout. Make sure all fields are filled.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = canSave
                        ) {
                            Text(
                                text = "SAVE",
                                color = if (canSave) orangeGlow else textGray,
                                fontWeight = FontWeight.Bold
                            )
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
                // Workout Name Input
                item {
                    WorkoutNameCard(
                        workoutName = workoutName,
                        onNameChange = { viewModel.updateWorkoutName(it) }
                    )
                }

                // ILB Test Mode Toggle
                item {
                    ILBTestModeCard(
                        isEnabled = isILBTestMode,
                        onToggle = { viewModel.setILBTestMode(it) }
                    )
                }

                // Exercises List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXERCISES (${selectedExercises.size})",
                            color = textGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Selected Exercises
                if (selectedExercises.isEmpty()) {
                    item {
                        EmptyExercisesCard()
                    }
                } else {
                    items(selectedExercises, key = { ex: ExerciseWithSets -> ex.exerciseId }) { exercise: ExerciseWithSets ->
                        ExerciseItemCard(
                            exercise = exercise,
                            onRemove = { viewModel.removeExercise(exercise.exerciseId) },
                            onMoveUp = {
                                val index = selectedExercises.indexOf(exercise)
                                if (index > 0) {
                                    viewModel.moveExercise(index, index - 1)
                                }
                            },
                            onMoveDown = {
                                val index = selectedExercises.indexOf(exercise)
                                if (index < selectedExercises.size - 1) {
                                    viewModel.moveExercise(index, index + 1)
                                }
                            },
                            canMoveUp = selectedExercises.indexOf(exercise) > 0,
                            canMoveDown = selectedExercises.indexOf(exercise) < selectedExercises.size - 1,
                            onSetUpdate = { updatedSets ->
                                viewModel.updateExerciseSets(exercise.exerciseId, updatedSets)
                            },
                            onAddSet = { viewModel.addSetToExercise(exercise.exerciseId) }
                        )
                    }
                }

                // Add Exercise Button
                item {
                    AddExerciseButton(
                        onClick = onNavigateToExerciseSelection
                    )
                }

                // Save & Start Session Button (only when in template customization mode)
                if (isTemplateCustomizationMode && onSaveAndStartSession != null) {
                    item {
                        Button(
                            onClick = {
                                val workout = viewModel.saveWorkoutAndGetTemplate()
                                if (workout != null) {
                                    Toast.makeText(
                                        context,
                                        "✓ Workout saved!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSaveAndStartSession(workout)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to save workout. Make sure all fields are filled.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = workoutName.isNotBlank() && selectedExercises.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = orangePrimary,
                                contentColor = Color.White,
                                disabledContainerColor = textGray.copy(alpha = 0.3f),
                                disabledContentColor = textGray
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "SAVE & START SESSION",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // Discard Changes Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCurrentWorkout()
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = orangeGlow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel", color = textGray)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = textWhite,
            textContentColor = textGray
        )
    }
}

@Composable
private fun WorkoutNameCard(
    workoutName: String,
    onNameChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.5.dp, cardBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "WORKOUT NAME",
                color = textGray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = workoutName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Legday, Chest & Triceps...", color = textGray.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textWhite,
                    unfocusedTextColor = textWhite,
                    focusedBorderColor = orangeGlow,
                    unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                    cursorColor = orangeGlow
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun EmptyExercisesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.5.dp, cardBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = textGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No exercises yet",
                    color = textGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Add exercises to your workout",
                    color = textGray.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * ExerciseItemCard - Matches the SetCard design from Active Workout Screen
 * Features inline BasicTextFields for direct weight/reps input
 */
@Composable
private fun ExerciseItemCard(
    exercise: ExerciseWithSets,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSetUpdate: (List<ExerciseSet>) -> Unit,
    onAddSet: () -> Unit
) {
    // Dropdown menu state
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Local state for set values - initialize from exercise.sets
    // Simplified: only weight and reps (rest timer handled in active workout)
    val localSets = remember(exercise.exerciseId, exercise.sets.size) {
        exercise.sets.map { set ->
            mutableStateOf(
                SetInputState(
                    weight = if (set.targetWeight > 0) set.targetWeight.toInt().toString() else "",
                    reps = if (set.targetReps > 0) set.targetReps.toString() else ""
                )
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Exercise Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Exercise number badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(orangePrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = orangePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Exercise name
                Text(
                    text = exercise.exerciseName,
                    color = textWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Sets count badge
                Text(
                    text = "${exercise.sets.size} sets",
                    color = orangeGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                // Options menu button
                Box {
                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = textGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(Color(0xFF2A2A2A))
                    ) {
                        // Add Set
                        DropdownMenuItem(
                            text = { Text("Add Set", color = textWhite) },
                            onClick = {
                                showOptionsMenu = false
                                onAddSet()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Green)
                            }
                        )

                        // Move Up
                        if (canMoveUp) {
                            DropdownMenuItem(
                                text = { Text("Move Up", color = textWhite) },
                                onClick = {
                                    showOptionsMenu = false
                                    onMoveUp()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = textGray)
                                }
                            )
                        }

                        // Move Down
                        if (canMoveDown) {
                            DropdownMenuItem(
                                text = { Text("Move Down", color = textWhite) },
                                onClick = {
                                    showOptionsMenu = false
                                    onMoveDown()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = textGray)
                                }
                            )
                        }

                        HorizontalDivider(color = textGray.copy(alpha = 0.3f))

                        // Remove Exercise
                        DropdownMenuItem(
                            text = { Text("Remove Exercise", color = Color.Red) },
                            onClick = {
                                showOptionsMenu = false
                                onRemove()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            }
                        )
                    }
                }
            }

            // Sets Table Header - Evenly distributed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SET", color = textGray, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                Text("KG", color = textGray, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("REPS", color = textGray, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }

            // Sets List - Inline inputs like SetCard
            exercise.sets.forEachIndexed { index, set ->
                val setInputState = localSets.getOrNull(index)?.value ?: SetInputState("", "")

                BuilderSetRow(
                    setNumber = index + 1,
                    weight = setInputState.weight,
                    reps = setInputState.reps,
                    isFirstRow = index == 0,
                    onWeightChange = { newWeight ->
                        localSets.getOrNull(index)?.value = setInputState.copy(weight = newWeight)

                        // Auto-fill: If first row, copy to all empty rows below
                        if (index == 0 && newWeight.isNotEmpty()) {
                            localSets.forEachIndexed { i, state ->
                                if (i > 0 && state.value.weight.isEmpty()) {
                                    state.value = state.value.copy(weight = newWeight)
                                }
                            }
                        }

                        // Update all exercise sets
                        val updatedSets = exercise.sets.toMutableList()
                        localSets.forEachIndexed { i, state ->
                            if (i < updatedSets.size) {
                                updatedSets[i] = updatedSets[i].copy(
                                    targetWeight = state.value.weight.toDoubleOrNull() ?: 0.0
                                )
                            }
                        }
                        onSetUpdate(updatedSets)
                    },
                    onRepsChange = { newReps ->
                        localSets.getOrNull(index)?.value = setInputState.copy(reps = newReps)

                        // Auto-fill: If first row, copy to all empty rows below
                        if (index == 0 && newReps.isNotEmpty()) {
                            localSets.forEachIndexed { i, state ->
                                if (i > 0 && state.value.reps.isEmpty()) {
                                    state.value = state.value.copy(reps = newReps)
                                }
                            }
                        }

                        // Update all exercise sets
                        val updatedSets = exercise.sets.toMutableList()
                        localSets.forEachIndexed { i, state ->
                            if (i < updatedSets.size) {
                                updatedSets[i] = updatedSets[i].copy(
                                    targetReps = state.value.reps.toIntOrNull() ?: 0
                                )
                            }
                        }
                        onSetUpdate(updatedSets)
                    }
                )
            }

            // Add Set Button - Compact
            TextButton(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Add Set",
                    color = orangeGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Data class to hold local input state for a set
 * Simplified: only weight and reps (rest timer is handled separately in active workout)
 */
private data class SetInputState(
    val weight: String,
    val reps: String
)

/**
 * BuilderSetRow - Inline row with BasicTextFields for weight/reps
 * Evenly distributed layout with auto-fill from first row
 */
@Composable
private fun BuilderSetRow(
    setNumber: Int,
    weight: String,
    reps: String,
    isFirstRow: Boolean = false,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // SET number badge
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFirstRow) orangeGlow.copy(alpha = 0.15f) else textGray.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$setNumber",
                color = if (isFirstRow) orangeGlow else textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // KG input - centered in flex space
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = weight,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    val decimalCount = filtered.count { it == '.' }
                    if (decimalCount <= 1 && filtered.length <= 5) {
                        val normalized = when {
                            filtered.isEmpty() -> filtered
                            filtered == "0" -> filtered
                            filtered.startsWith("0.") -> filtered
                            filtered.startsWith("0") -> filtered.dropWhile { it == '0' }.ifEmpty { "0" }
                            else -> filtered
                        }
                        val numValue = normalized.toDoubleOrNull()
                        if (numValue == null || numValue <= 999) {
                            onWeightChange(normalized)
                        }
                    }
                },
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(textGray.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                textStyle = TextStyle(
                    color = orangeGlow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (weight.isEmpty()) {
                            Text(
                                text = if (isFirstRow) "kg" else "–",
                                color = textGray.copy(alpha = 0.4f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // REPS input - centered in flex space
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = reps,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    if (filtered.length <= 3) {
                        val numValue = filtered.toIntOrNull()
                        if (numValue == null || numValue <= 999) {
                            onRepsChange(filtered)
                        }
                    }
                },
                modifier = Modifier
                    .width(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(textGray.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                textStyle = TextStyle(
                    color = orangeGlow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (reps.isEmpty()) {
                            Text(
                                text = if (isFirstRow) "reps" else "–",
                                color = textGray.copy(alpha = 0.4f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun AddExerciseButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = orangeGlow.copy(alpha = 0.2f),
            contentColor = orangeGlow
        ),
        border = BorderStroke(1.5.dp, orangeGlow)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "ADD EXERCISE",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * ILB Test Mode Card - Toggle for strength testing mode
 * When enabled, compound exercises will be converted to AMRAP tests
 * and new 1RM values will be calculated after completion
 */
@Composable
private fun ILBTestModeCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) orangeGlow.copy(alpha = 0.15f) else cardBackground
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled) orangeGlow else cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = if (isEnabled) orangeGlow else textGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ILB STRENGTH TEST",
                        color = if (isEnabled) orangeGlow else textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isEnabled)
                        "Compound exercises → AMRAP • New 1RM calculated"
                    else
                        "Enable to test your max strength via AMRAP sets",
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = orangePrimary,
                    checkedTrackColor = orangeGlow.copy(alpha = 0.3f),
                    uncheckedThumbColor = textGray,
                    uncheckedTrackColor = textGray.copy(alpha = 0.2f)
                )
            )
        }
    }
}