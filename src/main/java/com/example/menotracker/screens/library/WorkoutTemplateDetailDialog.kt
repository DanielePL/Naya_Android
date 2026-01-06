package com.example.menotracker.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.menotracker.viewmodels.WorkoutTemplate
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaBackground

// Design System - Matching TrainingScreen Glassmorphism Style
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)
private val cardBorder = orangeGlow.copy(alpha = 0.5f)
private val dialogBackground = Color(0xFF1a1410).copy(alpha = 0.95f)

@Composable
fun WorkoutTemplateDetailDialog(
    template: WorkoutTemplate,
    onDismiss: () -> Unit,
    onStartWorkout: (WorkoutTemplate) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = dialogBackground),
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textGray
                        )
                    }
                }

                HorizontalDivider(color = cardBorder)

                // Exercise List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${template.exercises.size} Exercises",
                                color = textGray,
                                fontSize = 14.sp
                            )
                            val totalSets = template.exercises.sumOf { it.sets.size }
                            Text(
                                text = "$totalSets Total Sets",
                                color = orangeGlow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(template.exercises.sortedBy { it.order }) { exerciseWithSets ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Exercise Name
                                Text(
                                    text = exerciseWithSets.exerciseName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = textWhite
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Muscle Group & Equipment
                                Text(
                                    text = "${exerciseWithSets.muscleGroup} • ${exerciseWithSets.equipment}",
                                    color = orangeGlow,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Sets Details
                                exerciseWithSets.sets.sortedBy { it.setNumber }.forEach { set ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Set ${set.setNumber}:",
                                            color = textWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(
                                                text = "${set.targetReps} reps",
                                                color = textGray,
                                                fontSize = 14.sp
                                            )
                                            if (set.targetWeight > 0) {
                                                Text(
                                                    text = "@ ${if (set.targetWeight % 1.0 == 0.0) set.targetWeight.toInt() else set.targetWeight}kg",
                                                    color = orangeGlow,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "${set.restSeconds}s rest",
                                                color = textGray.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Start Workout Button
                HorizontalDivider(color = cardBorder)

                Button(
                    onClick = { onStartWorkout(template) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
                        text = "Start This Workout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
