// app/src/main/java/com/example/myapplicationtest/screens/workout/WorkoutDetailScreen.kt

package com.example.menotracker.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.ExerciseSet
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.viewmodels.WorkoutTemplate
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

// Design Colors
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)

@Composable
fun WorkoutDetailScreen(
    workout: WorkoutTemplate?,
    onBackClick: () -> Unit = {},
    onStartWorkout: (WorkoutTemplate) -> Unit = {},
    onSwapExercise: (exerciseId: String) -> Unit = {},
    onUpdateExercise: (exerciseId: String, sets: List<ExerciseSet>) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()

    // Edit mode state
    var editingExerciseId by remember { mutableStateOf<String?>(null) }

    val backgroundBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF2a1f1a),
            Color(0xFF0f0f0f),
            Color(0xFF1a1410)
        )
    )

    if (workout == null) {
        // Loading or error state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = orangePrimary)
        }
        return
    }

    // Calculate total duration estimate (sets * rest time + work time)
    val totalMinutes = remember(workout) {
        val totalSets = workout.exercises.sumOf { it.sets.size }
        val avgRestSeconds = workout.exercises.flatMap { it.sets }.map { it.restSeconds }.average().toInt()
        val workTimePerSet = 45 // Estimated seconds per set
        ((totalSets * (avgRestSeconds + workTimePerSet)) / 60)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 140.dp)
        ) {
            // Header
            WorkoutHeader(
                workoutName = workout.name,
                exerciseCount = workout.exercises.size,
                onBackClick = onBackClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Overview
            WorkoutStatsRow(
                totalMinutes = totalMinutes,
                exerciseCount = workout.exercises.size
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Exercises List
            ExercisesList(
                exercises = workout.exercises,
                editingExerciseId = editingExerciseId,
                onEditClick = { exerciseId ->
                    editingExerciseId = if (editingExerciseId == exerciseId) null else exerciseId
                },
                onSwapClick = onSwapExercise,
                onUpdateSets = { exerciseId, sets ->
                    onUpdateExercise(exerciseId, sets)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Start Workout Button (Fixed at bottom)
        StartWorkoutButton(
            onStartClick = { onStartWorkout(workout) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 100.dp)
        )
    }
}

@Composable
private fun WorkoutHeader(
    workoutName: String,
    exerciseCount: Int,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = orangeGlow
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = workoutName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = "$exerciseCount exercises from your library",
            fontSize = 16.sp,
            color = textGray,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun WorkoutStatsRow(
    totalMinutes: Int,
    exerciseCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Duration
        StatCard(
            icon = Icons.Outlined.Schedule,
            value = "~$totalMinutes min",
            label = "Duration",
            modifier = Modifier.weight(1f)
        )

        // Exercises
        StatCard(
            icon = Icons.Outlined.FitnessCenter,
            value = "$exerciseCount",
            label = "Exercises",
            modifier = Modifier.weight(1f)
        )

        // Edit hint
        StatCard(
            icon = Icons.Outlined.Edit,
            value = "Tap",
            label = "To Edit",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    orangeGlow.copy(alpha = 0.1f),
                    RoundedCornerShape(16.dp)
                )
                .blur(20.dp)
        )

        // Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground, RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = orangeGlow.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = textGray
                )
            }
        }
    }
}

@Composable
private fun ExercisesList(
    exercises: List<ExerciseWithSets>,
    editingExerciseId: String?,
    onEditClick: (String) -> Unit,
    onSwapClick: (String) -> Unit,
    onUpdateSets: (String, List<ExerciseSet>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Exercises",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        exercises.forEachIndexed { index, exercise ->
            ExerciseCard(
                exercise = exercise,
                number = index + 1,
                isEditing = editingExerciseId == exercise.exerciseId,
                onEditClick = { onEditClick(exercise.exerciseId) },
                onSwapClick = { onSwapClick(exercise.exerciseId) },
                onUpdateSets = { sets -> onUpdateSets(exercise.exerciseId, sets) }
            )

            if (index < exercises.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseWithSets,
    number: Int,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onSwapClick: () -> Unit,
    onUpdateSets: (List<ExerciseSet>) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Local state for editing
    var editedSets by remember(exercise.sets) { mutableStateOf(exercise.sets) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onEditClick
            )
    ) {
        // Glow
        if (isPressed || isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isEditing) 200.dp else 120.dp)
                    .background(
                        orangeGlow.copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    )
                    .blur(25.dp)
            )
        }

        // Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground, RoundedCornerShape(20.dp))
                .border(
                    width = if (isEditing) 2.dp else 1.dp,
                    color = orangeGlow.copy(alpha = if (isEditing) 0.7f else 0.3f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Number Circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(orangePrimary, orangeGlow)
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Exercise Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = exercise.exerciseName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Muscle Group & Equipment
                        Text(
                            text = "${exercise.muscleGroup} • ${exercise.equipment}",
                            fontSize = 12.sp,
                            color = textGray
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Sets summary
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FitnessCenter,
                                    contentDescription = null,
                                    tint = orangeGlow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${exercise.sets.size} sets",
                                    fontSize = 14.sp,
                                    color = textGray
                                )
                            }

                            // Average rest time
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = orangeGlow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${exercise.sets.firstOrNull()?.restSeconds ?: 90}s rest",
                                    fontSize = 14.sp,
                                    color = textGray
                                )
                            }
                        }
                    }

                    // Action buttons
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Swap button
                        IconButton(
                            onClick = onSwapClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SwapHoriz,
                                contentDescription = "Swap Exercise",
                                tint = orangeGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Edit indicator
                        Icon(
                            imageVector = if (isEditing) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = textGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Expanded edit section
                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = orangeGlow.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sets editor
                    Text(
                        text = "Edit Sets",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = orangeGlow
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    editedSets.forEachIndexed { index, set ->
                        SetEditRow(
                            setNumber = index + 1,
                            set = set,
                            onSetChange = { updatedSet ->
                                editedSets = editedSets.toMutableList().apply {
                                    this[index] = updatedSet
                                }
                                onUpdateSets(editedSets)
                            }
                        )
                        if (index < editedSets.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add/Remove set buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (editedSets.size > 1) {
                                    editedSets = editedSets.dropLast(1)
                                    onUpdateSets(editedSets)
                                }
                            },
                            enabled = editedSets.size > 1,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = orangeGlow
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, orangeGlow.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove Set", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val lastSet = editedSets.lastOrNull()
                                val newSet = ExerciseSet(
                                    setNumber = editedSets.size + 1,
                                    targetReps = lastSet?.targetReps ?: 10,
                                    targetWeight = lastSet?.targetWeight ?: 0.0,
                                    restSeconds = lastSet?.restSeconds ?: 90
                                )
                                editedSets = editedSets + newSet
                                onUpdateSets(editedSets)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = orangeGlow
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, orangeGlow.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Set", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetEditRow(
    setNumber: Int,
    set: ExerciseSet,
    onSetChange: (ExerciseSet) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Set number
        Text(
            text = "Set $setNumber",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textWhite,
            modifier = Modifier.width(50.dp)
        )

        // Reps input
        OutlinedTextField(
            value = set.targetReps.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.let { reps ->
                    onSetChange(set.copy(targetReps = reps))
                }
            },
            label = { Text("Reps", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = orangeGlow,
                unfocusedBorderColor = orangeGlow.copy(alpha = 0.3f),
                focusedTextColor = textWhite,
                unfocusedTextColor = textWhite,
                focusedLabelColor = orangeGlow,
                unfocusedLabelColor = textGray
            )
        )

        // Weight input
        OutlinedTextField(
            value = set.targetWeight.toString(),
            onValueChange = { value ->
                value.toDoubleOrNull()?.let { weight ->
                    onSetChange(set.copy(targetWeight = weight))
                }
            },
            label = { Text("kg", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = orangeGlow,
                unfocusedBorderColor = orangeGlow.copy(alpha = 0.3f),
                focusedTextColor = textWhite,
                unfocusedTextColor = textWhite,
                focusedLabelColor = orangeGlow,
                unfocusedLabelColor = textGray
            )
        )

        // Rest input
        OutlinedTextField(
            value = set.restSeconds.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.let { rest ->
                    onSetChange(set.copy(restSeconds = rest))
                }
            },
            label = { Text("Rest", fontSize = 10.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = orangeGlow,
                unfocusedBorderColor = orangeGlow.copy(alpha = 0.3f),
                focusedTextColor = textWhite,
                unfocusedTextColor = textWhite,
                focusedLabelColor = orangeGlow,
                unfocusedLabelColor = textGray
            )
        )
    }
}

@Composable
private fun StartWorkoutButton(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(modifier = modifier.fillMaxWidth()) {
        // Big Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    orangeGlow.copy(alpha = if (isPressed) 0.4f else 0.3f),
                    RoundedCornerShape(20.dp)
                )
                .blur(if (isPressed) 40.dp else 30.dp)
        )

        // Button
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPressed) orangeGlow else orangePrimary
            ),
            shape = RoundedCornerShape(20.dp),
            interactionSource = interactionSource
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "START WORKOUT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}