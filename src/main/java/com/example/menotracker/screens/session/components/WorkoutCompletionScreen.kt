package com.example.menotracker.screens.session.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.viewmodels.SetData
import java.util.Locale

// Design System colors
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)

/**
 * Confetti particle data
 */
private data class ConfettiParticle(
    val id: Int,
    val startX: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val rotationSpeed: Float,
    val horizontalDrift: Float
)

/**
 * Confetti animation overlay
 */
@Composable
private fun ConfettiOverlay() {
    val confettiColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFF4CAF50), // Green
        Color(0xFFFF6B35), // Orange
        Color(0xFF2196F3), // Blue
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFFFFFFFF)  // White
    )

    // Generate confetti particles
    val particles = remember {
        (0..50).map { id ->
            ConfettiParticle(
                id = id,
                startX = kotlin.random.Random.nextFloat(),
                color = confettiColors.random(),
                size = kotlin.random.Random.nextFloat() * 10f + 6f,
                speed = kotlin.random.Random.nextFloat() * 0.5f + 0.3f,
                rotationSpeed = kotlin.random.Random.nextFloat() * 360f,
                horizontalDrift = (kotlin.random.Random.nextFloat() - 0.5f) * 0.3f
            )
        }
    }

    // Animation progress
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_fall"
    )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        particles.forEach { particle ->
            // Calculate Y position with staggered start
            val staggeredProgress = (animationProgress + particle.id * 0.02f) % 1f
            val yPos = staggeredProgress * size.height * 1.2f - size.height * 0.1f

            // Calculate X position with drift
            val xPos = particle.startX * size.width +
                    kotlin.math.sin(staggeredProgress * 6.28f) * 30f +
                    particle.horizontalDrift * staggeredProgress * size.width

            // Only draw if in visible area
            if (yPos >= -20f && yPos <= size.height + 20f) {
                // Rotation
                val rotation = staggeredProgress * particle.rotationSpeed

                rotate(degrees = rotation, pivot = androidx.compose.ui.geometry.Offset(xPos, yPos)) {
                    // Draw confetti piece (rectangle or circle)
                    if (particle.id % 3 == 0) {
                        // Circle confetti
                        drawCircle(
                            color = particle.color,
                            radius = particle.size / 2,
                            center = androidx.compose.ui.geometry.Offset(xPos, yPos)
                        )
                    } else {
                        // Rectangle confetti
                        drawRect(
                            color = particle.color,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                xPos - particle.size / 2,
                                yPos - particle.size / 4
                            ),
                            size = androidx.compose.ui.geometry.Size(particle.size, particle.size / 2)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stat card for workout completion screen
 */
@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    unit: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (unit != null) {
                Text(
                    text = unit,
                    color = color.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
            }
        }
        Text(
            text = label,
            color = textGray,
            fontSize = 12.sp
        )
    }
}

/**
 * Full-screen workout completion celebration screen
 */
@Composable
fun WorkoutCompletionScreen(
    workoutName: String,
    totalTime: String,
    totalSetsCompleted: Int,
    totalVolumeKg: Double,
    totalReps: Int,
    totalExercises: Int,
    prsAchieved: Int,
    prExerciseNames: List<String>,
    exercises: List<ExerciseWithSets>,
    setDataMap: Map<String, SetData>,
    workoutId: String,
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    // Celebration colors
    val goldColor = Color(0xFFFFD700)
    val greenColor = Color(0xFF4CAF50)

    // Calculate completion rate
    val totalPlannedSets = exercises.sumOf { it.sets.size }
    val completionRate = if (totalPlannedSets > 0) {
        totalSetsCompleted.toFloat() / totalPlannedSets.toFloat()
    } else 1f

    // Smart motivational message based on performance
    val motivationalMessage = remember(completionRate, prsAchieved) {
        when {
            // Perfect workout with PRs
            completionRate >= 0.95f && prsAchieved > 0 -> listOf(
                "LEGENDARY! New records AND full completion!",
                "Unstoppable! PRs smashed, workout crushed!",
                "History made today! What a performance!",
                "Peak performance unlocked!"
            ).random()

            // Perfect workout, no PRs
            completionRate >= 0.95f -> listOf(
                "Perfect execution! Every set crushed!",
                "Flawless session! Consistency is key!",
                "100% commitment, 100% results!",
                "Nothing left on the table!"
            ).random()

            // Great workout (75%+)
            completionRate >= 0.75f -> listOf(
                "Great session! Consistency builds champions!",
                "Strong work! Progress over perfection!",
                "Quality reps, quality gains!",
                "Another step toward your goals!"
            ).random()

            // Moderate workout (50-75%)
            completionRate >= 0.50f -> listOf(
                "Solid effort! Every rep counts!",
                "Good work today! Recovery matters too!",
                "Building the habit is half the battle!",
                "Progress isn't always linear - keep going!"
            ).random()

            // Started but didn't finish much (<50%)
            else -> listOf(
                "Showing up is already strong!",
                "Some days are harder - you still showed up!",
                "Every rep is a step forward!",
                "The hardest part is starting - you did it!",
                "Rest up, come back stronger!"
            ).random()
        }
    }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")

    // Trophy pulse animation
    val trophyScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophy_pulse"
    )

    // Trophy glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophy_glow"
    )

    // Entry animations
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showContent = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(500),
        label = "content_alpha"
    )

    val contentScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "content_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF1A1410),
                        Color(0xFF0D0D0D)
                    )
                )
            )
    ) {
        // Confetti overlay
        ConfettiOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textGray
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Trophy/Celebration icon with animations
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = trophyScale
                        scaleY = trophyScale
                    },
                contentAlignment = Alignment.Center
            ) { // Outer glow rings - always gold for trophy!
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(70.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    goldColor.copy(alpha = glowAlpha * 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(60.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    goldColor.copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Trophy icon - always gold!
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Completed",
                    tint = goldColor,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // "WORKOUT COMPLETE" title with shimmer effect
            Text(
                text = "WORKOUT COMPLETE",
                color = greenColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(8.dp))

            // Workout name
            Text(
                text = workoutName,
                color = textWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(8.dp))

            // Motivational message
            Text(
                text = motivationalMessage,
                color = orangeGlow,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            // PR Badge (if any)
            if (prsAchieved > 0) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = goldColor.copy(alpha = 0.15f)),
                    border = BorderStroke(2.dp, goldColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = goldColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$prsAchieved PERSONAL RECORD${if (prsAchieved > 1) "S" else ""}!",
                                color = goldColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            prExerciseNames.forEach { name ->
                                Text(
                                    text = name,
                                    color = goldColor.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Duration
                StatCard(
                    icon = Icons.Default.Timer,
                    value = totalTime,
                    label = "Duration",
                    color = orangeGlow
                )

                // Volume
                StatCard(
                    icon = Icons.Default.FitnessCenter,
                    value = String.format(Locale.US, "%.0f", totalVolumeKg),
                    unit = "kg",
                    label = "Volume",
                    color = textWhite
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Sets
                StatCard(
                    icon = Icons.Default.Repeat,
                    value = "$totalSetsCompleted",
                    label = "Sets",
                    color = textWhite
                )

                // Reps
                StatCard(
                    icon = Icons.Default.SportsMartialArts,
                    value = "$totalReps",
                    label = "Reps",
                    color = textWhite
                )

                // Exercises
                StatCard(
                    icon = Icons.AutoMirrored.Filled.List,
                    value = "$totalExercises",
                    label = "Exercises",
                    color = textWhite
                )
            }

            Spacer(Modifier.height(12.dp))

            // Exercise summary (scrollable) - with max height to ensure visibility
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 220.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Exercise Summary",
                        color = textWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        exercises.forEachIndexed { index, exercise ->
                            val completedSets = exercise.sets.indices.count { setIdx ->
                                val setId = "${workoutId}_${exercise.exerciseId}_set${setIdx + 1}"
                                setDataMap[setId]?.isCompleted ?: false
                            }
                            val hasPR = prExerciseNames.contains(exercise.exerciseName)

                            // Calculate exercise stats
                            var exerciseVolume = 0.0
                            var exerciseReps = 0
                            var bestWeight = 0.0
                            var bestReps = 0

                            exercise.sets.indices.forEach { setIdx ->
                                val setId = "${workoutId}_${exercise.exerciseId}_set${setIdx + 1}"
                                val setData = setDataMap[setId]
                                if (setData?.isCompleted == true) {
                                    val weight = setData.completedWeight ?: 0.0
                                    val reps = setData.completedReps ?: 0
                                    exerciseVolume += weight * reps
                                    exerciseReps += reps
                                    if (weight > bestWeight) {
                                        bestWeight = weight
                                        bestReps = reps
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (hasPR) goldColor.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                // Top row: checkmark, name, PR badge, sets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Checkmark or exercise number
                                    if (completedSets == exercise.sets.size) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = greenColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(textGray.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = textGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Exercise name
                                    Text(
                                        text = exercise.exerciseName,
                                        color = if (hasPR) goldColor else textWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // PR badge
                                    if (hasPR) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "PR",
                                            tint = goldColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Sets completed
                                    Text(
                                        text = "$completedSets/${exercise.sets.size}",
                                        color = if (completedSets == exercise.sets.size) greenColor else textGray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Bottom row: Best set, Volume, Total reps
                                if (completedSets > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Best set
                                        Text(
                                            text = "Best: ${bestWeight.toInt()}kg × $bestReps",
                                            color = textGray,
                                            fontSize = 11.sp
                                        )

                                        // Volume
                                        Text(
                                            text = "Vol: ${exerciseVolume.toInt()}kg",
                                            color = textGray,
                                            fontSize = 11.sp
                                        )

                                        // Total reps
                                        Text(
                                            text = "Reps: $exerciseReps",
                                            color = textGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // COMPLETE button with extra bottom padding for navigation bar
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = greenColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "COMPLETE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Extra bottom padding for navigation bar / system buttons
            Spacer(Modifier.height(48.dp))
        }
    }
}
