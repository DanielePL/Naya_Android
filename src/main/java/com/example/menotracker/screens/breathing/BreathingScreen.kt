package com.example.menotracker.screens.breathing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.viewmodels.BreathingUiState
import com.example.menotracker.viewmodels.BreathingViewModel

// Colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val tealAccent = Color(0xFF14B8A6)
private val pinkAccent = Color(0xFFEC4899)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)

/**
 * Main Breathing Exercises Screen
 * Shows list of available exercises with tier gating
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSession: (BreathingExerciseType) -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: BreathingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showUpgradePrompt by viewModel.showUpgradePrompt.collectAsState()

    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    // Upgrade prompt dialog
    if (showUpgradePrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpgradePrompt() },
            title = { Text("Premium Exercise", color = textWhite) },
            text = {
                Text(
                    "This breathing exercise requires a Premium subscription. Upgrade to unlock all 5 exercises.",
                    color = textGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUpgradePrompt()
                        onNavigateToPaywall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lavenderPrimary)
                ) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpgradePrompt() }) {
                    Text("Maybe Later", color = textGray)
                }
            },
            containerColor = cardBg
        )
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = null,
                                tint = lavenderLight,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "BREATHING",
                                    color = textWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Calm your mind",
                                    color = lavenderLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Stats Card
                uiState.totalStats?.let { stats ->
                    item {
                        BreathingStatsCard(stats = stats)
                    }
                }

                // Section: Available Exercises
                item {
                    Text(
                        text = "Exercises",
                        color = textWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Free exercise highlight
                val freeExercise = BreathingExerciseType.RELAXATION_478
                item {
                    FeaturedExerciseCard(
                        exercise = freeExercise,
                        onClick = { onNavigateToSession(freeExercise) }
                    )
                }

                // Other exercises
                items(BreathingExerciseType.entries.filter { it != freeExercise }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        isLocked = !uiState.hasFullAccess && !exercise.isFree,
                        onClick = {
                            if (uiState.hasFullAccess || exercise.isFree) {
                                onNavigateToSession(exercise)
                            } else {
                                viewModel.selectExercise(exercise) // Will show upgrade prompt
                            }
                        }
                    )
                }

                // Recent sessions
                if (uiState.recentSessions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recent Sessions",
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(uiState.recentSessions) { session ->
                        RecentSessionCard(session = session)
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun BreathingStatsCard(stats: BreathingStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Your Progress",
                color = lavenderLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    value = "${stats.totalSessions}",
                    label = "Sessions"
                )
                StatItem(
                    value = "${stats.totalMinutes}",
                    label = "Minutes"
                )
                StatItem(
                    value = "${stats.currentStreak}",
                    label = "Day Streak"
                )
            }

            stats.favoriteExercise?.let { favorite ->
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF2D2D2D))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = favorite.emoji,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Favorite",
                            color = textGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = favorite.displayName,
                            color = textWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = textWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = textGray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FeaturedExerciseCard(
    exercise: BreathingExerciseType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            lavenderPrimary.copy(alpha = 0.8f),
                            tealAccent.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animation preview
                BreathingPreviewCircle(
                    color = Color.White,
                    modifier = Modifier.size(70.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exercise.displayName,
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "FREE",
                                color = textWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = exercise.description,
                        color = textWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExerciseInfoChip(
                            icon = Icons.Default.Timer,
                            text = "${exercise.totalDurationSeconds / 60} min"
                        )
                        ExerciseInfoChip(
                            icon = Icons.Default.Repeat,
                            text = "${exercise.defaultCycles} cycles"
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    tint = textWhite,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: BreathingExerciseType,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) cardBg.copy(alpha = 0.6f) else cardBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Color(exercise.difficulty.color).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exercise.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.displayName,
                        color = if (isLocked) textGray else textWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = textGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = exercise.targetSymptoms.take(2).joinToString(" | "),
                    color = textGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Duration
                    Text(
                        text = "${exercise.totalDurationSeconds / 60} min",
                        color = textGray,
                        fontSize = 11.sp
                    )

                    Text(text = "|", color = textGray.copy(alpha = 0.5f), fontSize = 11.sp)

                    // Difficulty
                    Text(
                        text = exercise.difficulty.displayName,
                        color = Color(exercise.difficulty.color),
                        fontSize = 11.sp
                    )
                }
            }

            // Play/Lock icon
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                contentDescription = if (isLocked) "Locked" else "Start",
                tint = if (isLocked) textGray else lavenderLight,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ExerciseInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textWhite.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = textWhite.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RecentSessionCard(session: BreathingSession) {
    val exercise = session.exerciseEnum ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.emoji,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.displayName,
                    color = textWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${session.durationSeconds / 60} min | ${session.cyclesCompleted} cycles",
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            // Mood improvement
            session.moodImprovement?.let { improvement ->
                if (improvement > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "+$improvement",
                            color = Color(0xFF10B981),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
