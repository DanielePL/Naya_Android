package com.example.menotracker.screens.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.data.WorkoutSessionRepository
import com.example.menotracker.data.models.UserStrengthProfile
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.viewmodels.RestTimerViewModel
import com.example.menotracker.viewmodels.SetData
import com.example.menotracker.viewmodels.WorkoutSessionViewModel
import com.example.menotracker.data.models.ILBTestResult
import com.example.menotracker.viewmodels.SetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Design System colors
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBorder = orangeGlow.copy(alpha = 0.5f)

/**
 * ExerciseSection - Exercise card with header and sets table
 *
 * Features:
 * - Exercise header with name and progress
 * - Sets table with SetCard rows
 * - Swap/Add/Remove exercise options
 * - Orange border when all sets completed
 * - PR-based weight recommendations (via SetCard)
 */
@Composable
fun ExerciseSection(
    exercise: ExerciseWithSets,
    exerciseIndex: Int,
    isCompleted: Boolean,
    currentSetIndex: Int,
    setDataMap: Map<String, SetData>,
    workoutId: String,
    userId: String?, // For VBT baseline comparison
    workoutSessionViewModel: WorkoutSessionViewModel,
    workoutSessionRepository: WorkoutSessionRepository,
    coroutineScope: CoroutineScope,
    restTimerViewModel: RestTimerViewModel,
    strengthProfile: UserStrengthProfile?, // For weight recommendations
    onVBTClick: (Int) -> Unit,
    onVideoClick: (String, com.example.menotracker.viewmodels.VelocityMetricsData?) -> Unit,
    onExerciseCompleted: () -> Unit,
    onSwapExercise: () -> Unit = {},
    onRemoveExercise: () -> Unit = {},
    onAddSet: () -> Unit = {},
    onRemoveSet: (Int) -> Unit = {},
    // ILB (Individuelles Leistungsbild) - Periodized strength testing
    isILBTestMode: Boolean = false,
    ilbTestResults: Map<String, ILBTestResult> = emptyMap(),  // setId -> result
    onAMRAPComplete: ((exerciseId: String, exerciseName: String, setId: String, reps: Int, weight: Double) -> Unit)? = null
) {
    // Frame color based on completion - orange glow when completed, subtle border otherwise
    val frameColor = if (isCompleted) orangeGlow else cardBorder.copy(alpha = 0.3f)
    val backgroundColor = Color.Transparent // Always black/transparent background

    // Dropdown menu state
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = if (isCompleted) 2.dp else 1.dp,
            color = frameColor
        ),
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
                // Completion checkmark or exercise number
                if (isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = orangeGlow,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(orangePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${exerciseIndex + 1}",
                            color = orangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Exercise name
                Text(
                    text = exercise.exerciseName,
                    color = textWhite, // Always white, frame indicates completion
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Progress indicator
                Text(
                    text = if (isCompleted) "Done" else "${currentSetIndex + 1}/${exercise.sets.size}",
                    color = orangeGlow, // Always orange
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
                        // Swap Exercise
                        DropdownMenuItem(
                            text = { Text("Swap Exercise", color = textWhite) },
                            onClick = {
                                showOptionsMenu = false
                                onSwapExercise()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = orangeGlow)
                            }
                        )

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

                        // Remove Last Set (only if more than 1 set)
                        if (exercise.sets.size > 1) {
                            DropdownMenuItem(
                                text = { Text("Remove Last Set", color = textWhite) },
                                onClick = {
                                    showOptionsMenu = false
                                    onRemoveSet(exercise.sets.size - 1)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color.Yellow)
                                }
                            )
                        }

                        HorizontalDivider(color = textGray.copy(alpha = 0.3f))

                        // Remove Exercise (only show if not the last exercise)
                        DropdownMenuItem(
                            text = { Text("Remove Exercise", color = Color.Red) },
                            onClick = {
                                showOptionsMenu = false
                                onRemoveExercise()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            }
                        )
                    }
                }
            }

            // Sets Table Header - aligned with SetCard row layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SET", color = textGray, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PREVIOUS", color = textGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("KG", color = textGray, fontSize = 10.sp, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(8.dp))
                Text("REPS", color = textGray, fontSize = 10.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(4.dp))
                Spacer(modifier = Modifier.width(40.dp)) // Camera
                Spacer(modifier = Modifier.width(40.dp)) // Checkbox
            }

            // Sets List
            exercise.sets.forEachIndexed { index, exerciseSet ->
                val setId = "${workoutId}_${exercise.exerciseId}_set${index + 1}"
                val setData = setDataMap[setId]

                // AUTO-FILL: Get previous set's completed values
                val previousSetData = if (index > 0) {
                    val prevSetId = "${workoutId}_${exercise.exerciseId}_set$index"
                    setDataMap[prevSetId]
                } else null

                val inheritedReps = setData?.currentReps?.ifEmpty {
                    previousSetData?.completedReps?.toString() ?: ""
                } ?: previousSetData?.completedReps?.toString() ?: ""

                val inheritedWeight = setData?.currentWeight?.ifEmpty {
                    previousSetData?.completedWeight?.toInt()?.toString() ?: ""
                } ?: previousSetData?.completedWeight?.toInt()?.toString() ?: ""

                // ILB: Determine set type - AMRAP for compound exercises in test mode
                val isCompoundExercise = com.example.menotracker.data.models.ILBCalculator.isCompoundExercise(exercise.exerciseName)
                val effectiveSetType = if (isILBTestMode && isCompoundExercise) SetType.AMRAP else SetType.STANDARD

                SetCard(
                    setId = setId,
                    setNumber = index + 1,
                    targetReps = exerciseSet.targetReps,
                    targetWeight = exerciseSet.targetWeight,
                    targetRestSeconds = exerciseSet.restSeconds,
                    isActive = index == currentSetIndex,
                    isCompleted = setData?.isCompleted ?: false,
                    isFailed = setData?.isFailed ?: false,
                    completedReps = setData?.completedReps,
                    completedWeight = setData?.completedWeight,
                    videoPath = setData?.videoPath,
                    vbtVelocity = setData?.vbtVelocity,
                    velocityMetrics = setData?.velocityMetrics,  // Full VBT metrics from backend
                    currentReps = inheritedReps,
                    currentWeight = inheritedWeight,
                    exerciseName = exercise.exerciseName, // For weight recommendations
                    strengthProfile = strengthProfile, // For weight recommendations
                    workoutSessionViewModel = workoutSessionViewModel,
                    workoutSessionRepository = workoutSessionRepository,
                    coroutineScope = coroutineScope,
                    // ILB: Pass set type and test result
                    setType = effectiveSetType,
                    ilbTestResult = ilbTestResults[setId],
                    onAMRAPComplete = if (isILBTestMode && isCompoundExercise && onAMRAPComplete != null) {
                        { reps, weight -> onAMRAPComplete(exercise.exerciseId, exercise.exerciseName, setId, reps, weight) }
                    } else null,
                    onComplete = { reps, weight ->
                        // Update ViewModel
                        workoutSessionViewModel.updateSetCompletion(
                            setId = setId,
                            isCompleted = true,
                            reps = reps,
                            weight = weight
                        )

                        // Save to database
                        coroutineScope.launch {
                            workoutSessionRepository.completeSet(setId, reps, weight)
                        }

                        // AUTO-FILL next sets
                        for (nextIdx in (index + 1) until exercise.sets.size) {
                            val nextSetId = "${workoutId}_${exercise.exerciseId}_set${nextIdx + 1}"
                            val nextSetData = setDataMap[nextSetId]
                            if (nextSetData?.isCompleted != true &&
                                nextSetData?.currentReps.isNullOrEmpty() &&
                                nextSetData?.currentWeight.isNullOrEmpty()) {
                                workoutSessionViewModel.updateCurrentInputs(
                                    nextSetId,
                                    reps.toString(),
                                    weight.toInt().toString()
                                )
                            }
                        }

                        // Start rest timer if not last set of this exercise
                        if (index < exercise.sets.size - 1) {
                            restTimerViewModel.startTimer(exerciseSet.restSeconds)
                        }

                        // Check if exercise is now completed
                        val allSetsNowCompleted = exercise.sets.indices.all { idx ->
                            val sid = "${workoutId}_${exercise.exerciseId}_set${idx + 1}"
                            if (idx == index) true else setDataMap[sid]?.isCompleted ?: false
                        }
                        if (allSetsNowCompleted) {
                            onExerciseCompleted()
                        }
                    },
                    onFail = { reps, weight ->
                        // Mark set as FAILED (long-press action)
                        workoutSessionViewModel.updateSetCompletion(
                            setId = setId,
                            isCompleted = true,
                            reps = reps,
                            weight = weight,
                            isFailed = true
                        )

                        // Save to database with failed flag
                        coroutineScope.launch {
                            workoutSessionRepository.completeSet(setId, reps, weight, isFailed = true)
                        }

                        // Start rest timer if not last set
                        if (index < exercise.sets.size - 1) {
                            restTimerViewModel.startTimer(exerciseSet.restSeconds)
                        }

                        // Check if exercise is now completed (failed sets count as completed)
                        val allSetsNowCompleted = exercise.sets.indices.all { idx ->
                            val sid = "${workoutId}_${exercise.exerciseId}_set${idx + 1}"
                            if (idx == index) true else setDataMap[sid]?.isCompleted ?: false
                        }
                        if (allSetsNowCompleted) {
                            onExerciseCompleted()
                        }
                    },
                    onUncomplete = {
                        // Mark set as not completed (allow user to correct mistakes)
                        workoutSessionViewModel.updateSetCompletion(
                            setId = setId,
                            isCompleted = false,
                            reps = null,
                            weight = null
                        )
                        // Note: We don't delete from DB, just update isCompleted state
                        // The user can re-enter correct values and complete again
                    },
                    onVBTClick = { onVBTClick(index + 1) },
                    onVideoClick = onVideoClick,
                    onAdjustNextSetWeight = if (index < exercise.sets.size - 1) { adjustmentKg ->
                        // VBT Auto-Regulation: Adjust NEXT set's weight
                        val nextSetIndex = index + 1
                        val nextSetId = "${workoutId}_${exercise.exerciseId}_set${nextSetIndex + 1}"
                        val nextSetData = setDataMap[nextSetId]

                        // Get base weight: current input, or this set's completed weight, or target
                        val baseWeight = nextSetData?.currentWeight?.toDoubleOrNull()
                            ?: setData?.completedWeight
                            ?: exerciseSet.targetWeight

                        // Calculate new weight with adjustment
                        val newWeight = (baseWeight + adjustmentKg).coerceAtLeast(0.0)

                        // Update the next set's weight
                        val currentReps = nextSetData?.currentReps ?: ""
                        workoutSessionViewModel.updateCurrentInputs(
                            nextSetId,
                            currentReps,
                            newWeight.toInt().toString()
                        )

                        // Also persist to database
                        coroutineScope.launch {
                            workoutSessionRepository.updateSetCurrentInputs(
                                nextSetId,
                                currentReps,
                                newWeight.toInt().toString()
                            )
                        }

                        android.util.Log.d("VBT_AUTOREGULATION",
                            "✅ Adjusted next set weight: $baseWeight → $newWeight (${if (adjustmentKg > 0) "+" else ""}${adjustmentKg.toInt()}kg)")
                    } else null  // No next set to adjust
                )
            }
        }
    }
}