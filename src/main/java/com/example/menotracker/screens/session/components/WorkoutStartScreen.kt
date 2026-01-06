package com.example.menotracker.screens.session.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.viewmodels.WorkoutTemplate

// Design System colors
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)

/**
 * Start screen shown before workout begins
 * Shows workout overview and START button
 */
@Composable
fun WorkoutStartScreen(
    workout: WorkoutTemplate,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val totalSets = workout.exercises.sumOf { it.sets.size }
    val estimatedDuration = workout.exercises.sumOf { exercise ->
        exercise.sets.sumOf { set ->
            // Estimate ~45 seconds per set + rest time
            45 + set.restSeconds
        }
    } / 60 // Convert to minutes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section - Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Cancel",
                    tint = textGray
                )
            }
        }

        // Middle section - Workout info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.weight(1f),
        ) {
            Spacer(Modifier.height(32.dp))

            // Workout name
            Text(
                text = workout.name,
                color = textWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                // Exercises count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${workout.exercises.size}",
                        color = orangeGlow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Exercises",
                        color = textGray,
                        fontSize = 14.sp
                    )
                }

                // Sets count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalSets",
                        color = orangeGlow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sets",
                        color = textGray,
                        fontSize = 14.sp
                    )
                }

                // Duration estimate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "~$estimatedDuration",
                        color = orangeGlow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Minutes",
                        color = textGray,
                        fontSize = 14.sp
                    )
                }
            }

            // Exercise list preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(workout.exercises) { index, exercise ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Exercise number
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(orangePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = orangePrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Exercise name
                            Text(
                                text = exercise.exerciseName,
                                color = textWhite,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )

                            // Sets count
                            Text(
                                text = "${exercise.sets.size} sets",
                                color = textGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Bottom section - START button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            // START button
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "START WORKOUT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Cancel text button
            TextButton(onClick = onCancel) {
                Text(
                    text = "Cancel",
                    color = textGray,
                    fontSize = 16.sp
                )
            }
        }
    }
}
