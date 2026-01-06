package com.example.menotracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.WorkoutIntensityLevel
import com.example.menotracker.ui.theme.NayaGlass
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaSurface
import com.example.menotracker.viewmodels.WorkoutTemplate

/**
 * Large workout preview card with placeholder/video support.
 * Designed for the simplified workout flow - shows workout at a glance.
 *
 * Phase A: Placeholder with gradient + exercise count
 * Phase B (later): Auto-play video when videoUrl is provided
 */
@Composable
fun WorkoutPreviewCard(
    workout: WorkoutTemplate,
    onClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    videoUrl: String? = null // For future video support
) {
    val intensity = WorkoutIntensityLevel.fromString(workout.intensity)
    val exerciseCount = workout.exercises.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = NayaSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Preview Area (16:9 aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                getIntensityGradientStart(intensity),
                                getIntensityGradientEnd(intensity)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$exerciseCount Übungen",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Intensity badge (top-left)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = intensity.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Info Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NayaGlass)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Workout name and exercise preview
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getExercisePreview(workout),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }

                // Start button
                FilledIconButton(
                    onClick = onStartClick,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = NayaPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Workout starten",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Get gradient start color based on intensity level
 */
private fun getIntensityGradientStart(intensity: WorkoutIntensityLevel): Color {
    return when (intensity) {
        WorkoutIntensityLevel.SANFT -> Color(0xFF667eea)  // Soft blue-purple
        WorkoutIntensityLevel.AKTIV -> Color(0xFF764ba2)  // Purple
        WorkoutIntensityLevel.POWER -> Color(0xFFf093fb)  // Pink-purple
    }
}

/**
 * Get gradient end color based on intensity level
 */
private fun getIntensityGradientEnd(intensity: WorkoutIntensityLevel): Color {
    return when (intensity) {
        WorkoutIntensityLevel.SANFT -> Color(0xFF48c6ef)  // Soft cyan
        WorkoutIntensityLevel.AKTIV -> Color(0xFF667eea)  // Blue-purple
        WorkoutIntensityLevel.POWER -> Color(0xFFf5576c)  // Red-pink
    }
}

/**
 * Get exercise preview text (first 2-3 exercises)
 */
private fun getExercisePreview(workout: WorkoutTemplate): String {
    val exercises = workout.exercises.take(3)
    val names = exercises.map { it.exerciseName }

    return if (workout.exercises.size > 3) {
        "${names.joinToString(", ")} +${workout.exercises.size - 3}"
    } else {
        names.joinToString(", ")
    }
}
