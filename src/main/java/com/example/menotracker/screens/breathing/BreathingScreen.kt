package com.example.menotracker.screens.breathing

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.menotracker.ui.theme.*
import com.example.menotracker.viewmodels.BreathingUiState
import com.example.menotracker.viewmodels.BreathingViewModel

// ═══════════════════════════════════════════════════════════════
// NAYA BREATHING - Design System
// ═══════════════════════════════════════════════════════════════

// Breathing-specific accent (Violet - Ruhe & Entspannung)
private val breathingPrimary = NayaPrimary                 // #A78BFA
private val breathingLight = Color(0xFFC4B5FD)             // Lighter violet
private val breathingDark = Color(0xFF8B5CF6)              // Darker violet

// Text & Surface (from NAYA theme)
private val textPrimary = NayaTextWhite
private val textSecondary = NayaTextSecondary
private val textTertiary = NayaTextTertiary
private val cardSurface = NayaSurface
private val glassSurface = NayaGlass

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
            title = {
                Text(
                    "Premium Atemübung",
                    color = textPrimary,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Diese Atemübung erfordert ein Premium-Abo. Upgrade um alle 5 Übungen freizuschalten.",
                    color = textSecondary,
                    fontFamily = Poppins
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUpgradePrompt()
                        onNavigateToPaywall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = breathingPrimary)
                ) {
                    Text("Upgraden", fontFamily = Poppins, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpgradePrompt() }) {
                    Text("Vielleicht später", color = textSecondary, fontFamily = Poppins)
                }
            },
            containerColor = cardSurface
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
                            // Icon container
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(breathingPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Air,
                                    contentDescription = null,
                                    tint = breathingPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ATEMÜBUNGEN",
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Beruhige deinen Geist",
                                    color = breathingPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = Poppins
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurück",
                                tint = textPrimary
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
                        text = "Übungen",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
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
                            text = "Letzte Sessions",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = glassSurface,
        border = BorderStroke(1.dp, breathingPrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = breathingPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Dein Fortschritt",
                    color = breathingPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpaceGrotesk
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                    label = "Minuten"
                )
                StatItem(
                    value = "${stats.currentStreak}",
                    label = "Tage Streak"
                )
            }

            stats.favoriteExercise?.let { favorite ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = textTertiary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(breathingPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = favorite.emoji,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Favorit",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontFamily = Poppins
                        )
                        Text(
                            text = favorite.displayName,
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Poppins
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
            color = textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk
        )
        Text(
            text = label,
            color = textSecondary,
            fontSize = 12.sp,
            fontFamily = Poppins
        )
    }
}

@Composable
private fun FeaturedExerciseCard(
    exercise: BreathingExerciseType,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            breathingPrimary.copy(alpha = 0.7f),
                            NayaSecondary.copy(alpha = 0.5f)
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
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "GRATIS",
                                color = textPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = exercise.description,
                        color = textPrimary.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontFamily = Poppins,
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
                            text = "${exercise.defaultCycles} Zyklen"
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Starten",
                        tint = textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isLocked) glassSurface.copy(alpha = 0.5f) else glassSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isLocked) textTertiary.copy(alpha = 0.1f)
            else breathingPrimary.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon with difficulty-based background
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
                        color = if (isLocked) textSecondary else textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SpaceGrotesk
                    )

                    if (isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Gesperrt",
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = exercise.targetSymptoms.take(2).joinToString(" | "),
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontFamily = Poppins
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = textTertiary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${exercise.totalDurationSeconds / 60} min",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontFamily = Poppins,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Difficulty
                    Text(
                        text = exercise.difficulty.displayName,
                        color = Color(exercise.difficulty.color),
                        fontSize = 11.sp,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Play/Lock icon with background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLocked) textTertiary.copy(alpha = 0.1f)
                        else breathingPrimary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = if (isLocked) "Gesperrt" else "Starten",
                    tint = if (isLocked) textSecondary else breathingPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
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
            tint = textPrimary.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = textPrimary.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontFamily = Poppins
        )
    }
}

@Composable
private fun RecentSessionCard(session: BreathingSession) {
    val exercise = session.exerciseEnum ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = glassSurface,
        border = BorderStroke(1.dp, textTertiary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(breathingPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exercise.emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.displayName,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = "${session.durationSeconds / 60} min | ${session.cyclesCompleted} Zyklen",
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontFamily = Poppins
                )
            }

            // Mood improvement
            session.moodImprovement?.let { improvement ->
                if (improvement > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = breathingPrimary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = breathingPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "+$improvement",
                                color = breathingPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }
        }
    }
}
