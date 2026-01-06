// app/src/main/java/com/example/myapplicationtest/screens/training/TrainingScreen.kt

package com.example.menotracker.screens.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.ProgramRepository
import com.example.menotracker.data.WorkoutAssignmentRepository
import com.example.menotracker.data.models.ProgramTemplateDay
import com.example.menotracker.data.models.UserProgram
import com.example.menotracker.data.models.UserProgramProgress
import com.example.menotracker.viewmodels.AccountViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaTextWhite
import com.example.menotracker.ui.theme.NayaTextGray
import com.example.menotracker.ui.theme.NayaTextSecondary
import com.example.menotracker.ui.theme.NayaSurface
import com.example.menotracker.ui.theme.NayaGlass
import com.example.menotracker.ui.theme.NayaGlassBorder
import com.example.menotracker.ui.theme.SpaceGrotesk
import com.example.menotracker.ui.theme.Poppins
import com.example.menotracker.ui.theme.glassBackground
import com.example.menotracker.viewmodels.WorkoutBuilderViewModel
import com.example.menotracker.viewmodels.WorkoutTemplate
import com.example.menotracker.billing.Feature
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.billing.UpgradePrompt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Design System - Using Theme Colors
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = NayaTextWhite
private val textGray = NayaTextGray
private val cardBackground = NayaGlass
private val surfaceColor = NayaSurface

/**
 * Simplified Training Screen for 40+ target audience.
 * "Pick & Go" approach: Pre-made workouts with intensity levels.
 *
 * Phase 1: Level-based filtering (Sanft/Aktiv/Power)
 * Phase 2 (future): Video previews with auto-play
 */
@Composable
fun TrainingScreen(
    workoutBuilderViewModel: WorkoutBuilderViewModel,
    accountViewModel: AccountViewModel = viewModel(),
    onWorkoutClick: (String) -> Unit = {},
    onVbtClick: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToWorkouts: () -> Unit = {},
    onNavigateToLibrary: (String) -> Unit = {},
    onNavigateToWorkoutBuilder: () -> Unit = {},
    onLoadTemplateAndNavigate: (String) -> Unit = {},
    onStartSession: (WorkoutTemplate) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProgramDashboard: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onStartProgramWorkout: (String) -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onScanWorkout: () -> Unit = {},
    onWorkoutDetail: (WorkoutTemplate) -> Unit = {} // New: Navigate to workout detail
) {
    val scope = rememberCoroutineScope()

    // Collect workouts from ViewModel
    val savedWorkouts by workoutBuilderViewModel.savedWorkouts.collectAsState()
    val publicWorkouts by workoutBuilderViewModel.publicWorkouts.collectAsState()

    // Intensity filter state
    var selectedIntensity by remember { mutableStateOf<com.example.menotracker.data.models.WorkoutIntensityLevel?>(null) }

    // Filter public workouts by intensity
    val filteredPublicWorkouts = remember(publicWorkouts, selectedIntensity) {
        if (selectedIntensity == null) {
            publicWorkouts
        } else {
            publicWorkouts.filter { workout ->
                com.example.menotracker.data.models.WorkoutIntensityLevel.fromString(workout.intensity) == selectedIntensity
            }
        }
    }

    // Coach assigned workouts
    val userProfile by accountViewModel.userProfile.collectAsState()
    val latestAssignment by WorkoutAssignmentRepository.latestAssignment.collectAsState()

    // Load coach-assigned workouts
    LaunchedEffect(userProfile?.id) {
        userProfile?.id?.let { userId ->
            scope.launch {
                WorkoutAssignmentRepository.loadAssignedWorkouts(userId)
            }
        }
    }

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Simplified Header
            item {
                SimplifiedTrainingHeader(
                    onHistoryClick = onNavigateToHistory,
                    onProgramsClick = onNavigateToProgramDashboard,
                    onStatisticsClick = onNavigateToStatistics
                )
            }

            // Intensity Filter Row
            item {
                com.example.menotracker.ui.components.IntensityFilterRow(
                    selectedIntensity = selectedIntensity,
                    onIntensitySelected = { selectedIntensity = it }
                )
            }

            // Coach Assigned Workout (prominent if exists)
            latestAssignment?.let { assignment ->
                item {
                    CoachAssignedWorkoutCard(
                        assignment = assignment,
                        onStartWorkout = { onStartSession(assignment.workoutTemplate) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Public Workouts - Large Preview Cards
            if (filteredPublicWorkouts.isNotEmpty()) {
                item {
                    Text(
                        text = if (selectedIntensity == null) "Fertige Workouts" else "${selectedIntensity?.displayName} Workouts",
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = SpaceGrotesk,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                items(
                    items = filteredPublicWorkouts,
                    key = { it.id }
                ) { workout ->
                    com.example.menotracker.ui.components.WorkoutPreviewCard(
                        workout = workout,
                        onClick = { onWorkoutDetail(workout) },
                        onStartClick = { onStartSession(workout) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // My Workouts Section (smaller, at bottom)
            if (savedWorkouts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Meine Workouts",
                            color = textWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = SpaceGrotesk
                        )
                        TextButton(onClick = onNavigateToWorkouts) {
                            Text(
                                text = "Alle anzeigen",
                                color = orangePrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = savedWorkouts.take(5),
                            key = { it.id }
                        ) { workout ->
                            SmallWorkoutCard(
                                workout = workout,
                                onClick = { onStartSession(workout) }
                            )
                        }
                    }
                }
            }

            // Empty state if no workouts
            if (filteredPublicWorkouts.isEmpty() && savedWorkouts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = textGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Keine Workouts gefunden",
                                color = textGray,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simplified header with only essential navigation
 */
@Composable
private fun SimplifiedTrainingHeader(
    onHistoryClick: () -> Unit = {},
    onProgramsClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Training",
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                fontFamily = SpaceGrotesk
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onStatisticsClick) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = "Statistics",
                    tint = textGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onProgramsClick) {
                Icon(
                    imageVector = Icons.Outlined.CalendarViewWeek,
                    contentDescription = "Programs",
                    tint = textGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "History",
                    tint = textGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Small workout card for "My Workouts" horizontal scroll
 */
@Composable
private fun SmallWorkoutCard(
    workout: WorkoutTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardBackground
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Intensity badge
            val intensity = com.example.menotracker.data.models.WorkoutIntensityLevel.fromString(workout.intensity)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getIntensityColor(intensity).copy(alpha = 0.2f)
            ) {
                Text(
                    text = intensity.displayName,
                    color = getIntensityColor(intensity),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Text(
                text = workout.name,
                color = textWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${workout.exercises.size} Übungen",
                color = textGray,
                fontSize = 12.sp
            )

            // Play button
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = orangePrimary,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.End)
            )
        }
    }
}

/**
 * Get color for intensity level
 */
private fun getIntensityColor(intensity: com.example.menotracker.data.models.WorkoutIntensityLevel): Color {
    return when (intensity) {
        com.example.menotracker.data.models.WorkoutIntensityLevel.SANFT -> Color(0xFF48c6ef)
        com.example.menotracker.data.models.WorkoutIntensityLevel.AKTIV -> Color(0xFF764ba2)
        com.example.menotracker.data.models.WorkoutIntensityLevel.POWER -> Color(0xFFf5576c)
    }
}

// ============================================================================
// LEGACY COMPONENTS (kept for backward compatibility with ProgramScreen)
// These components are used by other screens and should be kept.
// ============================================================================

@Composable
private fun TrainingHeader(
    onHistoryClick: () -> Unit = {},
    onProgramsClick: () -> Unit = {},
    onStatisticsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Training",
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                fontFamily = SpaceGrotesk
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Statistics button
            IconButton(onClick = onStatisticsClick) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = "Statistics",
                    tint = textGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Programs button
            IconButton(onClick = onProgramsClick) {
                Icon(
                    imageVector = Icons.Outlined.CalendarViewWeek,
                    contentDescription = "Programs",
                    tint = textGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            // History button
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "History",
                    tint = textGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    modifier: Modifier = Modifier,
    onCreateWorkout: () -> Unit,
    onFormCheck: () -> Unit,
    onScanWorkout: () -> Unit,
    onBrowseExercises: () -> Unit,
    onBrowsePrograms: () -> Unit,
    remainingWorkouts: Int = Int.MAX_VALUE,
    hasBarSpeedAccess: Boolean = true
) {
    // Glass Card mit 24dp Corner Radius (Coaching-Software Style)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .glassBackground(cornerRadius = 24.dp, alpha = 0.4f),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "QUICK ACTIONS",
                    color = NayaTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontFamily = Poppins
                )
            }

            // Create Workout Button (Primary) - 12dp Corner Radius gemäß Spec
            Button(
                onClick = onCreateWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Create Workout",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = Poppins
                )
            }

            // Show remaining workouts hint for free tier (only if limited)
            if (remainingWorkouts < Int.MAX_VALUE && remainingWorkouts > 0) {
                Text(
                    text = "$remainingWorkouts workout${if (remainingWorkouts == 1) "" else "s"} remaining on Free tier",
                    color = textGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Quick Action Tiles Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bar Speed (with lock indicator if no access)
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Speed,
                    label = "Bar Speed",
                    onClick = onFormCheck,
                    isLocked = !hasBarSpeedAccess
                )

                // Scan Workout - scan whiteboard, screenshot, or photo of workout (VBT tier)
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.DocumentScanner,
                    label = "Scan Workout",
                    onClick = onScanWorkout,
                    isLocked = !hasBarSpeedAccess
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isLocked: Boolean = false
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, if (isLocked) textGray.copy(alpha = 0.1f) else textGray.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isLocked) textGray.copy(alpha = 0.5f) else orangeGlow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    color = if (isLocked) textGray.copy(alpha = 0.5f) else textWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Lock badge in top-right corner
            if (isLocked) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = orangePrimary.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Premium",
                            tint = orangePrimary,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "PREMIUM",
                            color = orangePrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MY WORKOUTS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MyWorkoutsSection(
    workouts: List<WorkoutTemplate>,
    onWorkoutClick: (WorkoutTemplate) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "MY WORKOUTS",
                    color = textGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            TextButton(onClick = onSeeAllClick) {
                Text(
                    "See All",
                    color = orangeGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Vertical workout cards list - better overview for athletes with 4+ workouts/week
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            workouts.sortedByDescending { it.createdAt }.take(8).forEach { workout ->
                WorkoutCard(
                    workout = workout,
                    onClick = { onWorkoutClick(workout) }
                )
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutTemplate,
    onClick: () -> Unit
) {
    val totalSets = workout.exercises.sumOf { it.sets.size }
    // Get first 3 exercise names for preview
    val exercisePreview = workout.exercises.take(3).joinToString(" • ") { it.exerciseName }
    val hasMoreExercises = workout.exercises.size > 3

    // Full-width glass card for vertical list
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .glassBackground(cornerRadius = 16.dp, alpha = 0.35f),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Workout info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Workout name
                Text(
                    text = workout.name,
                    color = textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = SpaceGrotesk
                )

                // Exercise preview
                Text(
                    text = if (hasMoreExercises) "$exercisePreview +${workout.exercises.size - 3}" else exercisePreview,
                    color = textGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exercise count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = orangeGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${workout.exercises.size}",
                            color = orangeGlow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Total sets
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = textGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$totalSets sets",
                            color = textGray,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Right side - Start button
            Surface(
                shape = CircleShape,
                color = orangePrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start workout",
                        tint = orangeGlow,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ACTIVE PROGRAM SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ActiveProgramBanner(
    program: UserProgram,
    onSetStartDate: () -> Unit,
    onNavigateToProgramDashboard: () -> Unit,
    onDeleteProgram: () -> Unit = {},
    onEditProgram: () -> Unit = {},
    onPauseProgram: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progress = program.currentWeek.toFloat() / program.durationWeeks.toFloat()
    var showMenu by remember { mutableStateOf(false) }

    // Glass Card mit 24dp Corner Radius (Coaching-Software Style)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToProgramDashboard() }
            .glassBackground(cornerRadius = 24.dp, alpha = 0.4f),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with program name and settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Program icon
                    Surface(
                        shape = CircleShape,
                        color = orangePrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.CalendarViewWeek,
                                contentDescription = null,
                                tint = orangeGlow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = program.name,
                            color = textWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = "Week ${program.currentWeek} of ${program.durationWeeks}",
                            color = orangeGlow,
                            fontSize = 12.sp,
                            fontFamily = Poppins
                        )
                    }
                }

                // Menu button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Program options",
                            tint = textGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(surfaceColor)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Name", color = textWhite) },
                            onClick = {
                                showMenu = false
                                onEditProgram()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, tint = orangeGlow)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (program.status == "paused") "Resume" else "Pause",
                                    color = textWhite
                                )
                            },
                            onClick = {
                                showMenu = false
                                onPauseProgram()
                            },
                            leadingIcon = {
                                Icon(
                                    if (program.status == "paused") Icons.Default.PlayArrow else Icons.Default.Pause,
                                    null,
                                    tint = orangeGlow
                                )
                            }
                        )
                        HorizontalDivider(color = textGray.copy(alpha = 0.3f))
                        DropdownMenuItem(
                            text = { Text("Delete Program", color = Color(0xFFEF5350)) },
                            onClick = {
                                showMenu = false
                                onDeleteProgram()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350))
                            }
                        )
                    }
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress",
                        color = textGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = orangeGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = orangePrimary,
                    trackColor = surfaceColor
                )
            }

            // Start date info or set start date button
            if (program.startDate == null) {
                OutlinedButton(
                    onClick = onSetStartDate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = orangeGlow)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Set Start Date", fontWeight = FontWeight.Medium)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = textGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Started: ${formatDate(program.startDate)}",
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = onSetStartDate) {
                        Text(
                            text = "Change",
                            color = orangeGlow,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThisWeekWorkoutsSection(
    program: UserProgram,
    days: List<ProgramTemplateDay>,
    progress: List<UserProgramProgress>,
    onStartWorkout: (ProgramTemplateDay) -> Unit,
    onSkipWorkout: (ProgramTemplateDay) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "THIS WEEK",
                    color = textGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            // Week number badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = orangePrimary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "Week ${program.currentWeek}",
                    color = orangeGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (days.isEmpty()) {
            // No workouts for this week
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, textGray.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventBusy,
                        contentDescription = null,
                        tint = textGray,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No workouts scheduled",
                        color = textGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Check your program configuration",
                        color = textGray.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Workout days grid (2 columns)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                days.chunked(2).forEach { rowDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowDays.forEach { day ->
                            val isCompleted = progress.any {
                                it.programTemplateDayId == day.id &&
                                        it.weekNumber == program.currentWeek &&
                                        !it.skipped
                            }
                            val isSkipped = progress.any {
                                it.programTemplateDayId == day.id &&
                                        it.weekNumber == program.currentWeek &&
                                        it.skipped
                            }

                            ProgramDayCard(
                                day = day,
                                isCompleted = isCompleted,
                                isSkipped = isSkipped,
                                onStart = { onStartWorkout(day) },
                                onSkip = { onSkipWorkout(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add spacer if odd number of days
                        if (rowDays.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramDayCard(
    day: ProgramTemplateDay,
    isCompleted: Boolean,
    isSkipped: Boolean,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = when {
        isCompleted -> Color(0xFF1B5E20).copy(alpha = 0.3f) // Green tint
        isSkipped -> surfaceColor.copy(alpha = 0.5f)
        else -> Color(0xFF1A1A1A) // Black fill like Library exercise boxes
    }

    val borderColor = when {
        isCompleted -> Color(0xFF4CAF50)
        isSkipped -> textGray.copy(alpha = 0.3f)
        else -> orangeGlow.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Day header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.displayName,
                    color = if (isSkipped) textGray else textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isSkipped) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = "Skipped",
                        tint = textGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Notes or placeholder
            Text(
                text = day.notes ?: "Workout day",
                color = if (isSkipped) textGray.copy(alpha = 0.5f) else textGray,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Action buttons
            if (!isCompleted && !isSkipped) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Skip button
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, textGray.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textGray)
                    ) {
                        Text("Skip", fontSize = 11.sp)
                    }

                    // Start button
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Completed/Skipped status
                Text(
                    text = if (isCompleted) "Completed" else "Skipped",
                    color = if (isCompleted) Color(0xFF4CAF50) else textGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// START DATE PICKER DIALOG
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePickerDialog(
    currentStartDate: String?,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentStartDate?.let {
            try {
                LocalDate.parse(it).toEpochDay() * 24 * 60 * 60 * 1000
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                            .toString()
                        onDateSelected(date)
                    }
                }
            ) {
                Text("Confirm", color = orangePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textGray)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = surfaceColor
        )
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = surfaceColor,
                titleContentColor = textWhite,
                headlineContentColor = textWhite,
                weekdayContentColor = textGray,
                subheadContentColor = textGray,
                yearContentColor = textWhite,
                currentYearContentColor = orangeGlow,
                selectedYearContainerColor = orangePrimary,
                selectedDayContainerColor = orangePrimary,
                todayContentColor = orangeGlow,
                todayDateBorderColor = orangeGlow
            )
        )
    }
}

// Helper function to format date
private fun formatDate(dateString: String?): String {
    if (dateString == null) return "Not set"
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRAM MANAGEMENT DIALOGS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DeleteProgramDialog(
    programName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = {
            Text(
                "Delete Program",
                color = textWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Are you sure you want to delete \"$programName\"?",
                    color = textWhite
                )
                Text(
                    "This will remove all your progress. This action cannot be undone.",
                    color = textGray,
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF5350)
                )
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textGray)
            }
        }
    )
}

@Composable
private fun EditProgramNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = {
            Text(
                "Edit Program Name",
                color = textWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Program Name", color = textGray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textWhite,
                    unfocusedTextColor = textWhite,
                    focusedBorderColor = orangePrimary,
                    unfocusedBorderColor = textGray.copy(alpha = 0.5f),
                    cursorColor = orangePrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != currentName,
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                )
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textGray)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// COACH ASSIGNED WORKOUT SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CoachAssignedWorkoutCard(
    assignment: WorkoutAssignmentRepository.AssignedWorkout,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val workout = assignment.workoutTemplate
    val totalSets = workout.exercises.sumOf { it.sets.size }
    val exercisePreview = workout.exercises.take(3).joinToString(" • ") { it.exerciseName }
    val hasMoreExercises = workout.exercises.size > 3

    // Glass Card with orange accent for coach assignments
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onStartWorkout() }
            .glassBackground(cornerRadius = 24.dp, alpha = 0.4f),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with coach badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Coach icon badge
                    Surface(
                        shape = CircleShape,
                        color = orangePrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = orangeGlow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Coach Assigned",
                            color = orangeGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            fontFamily = Poppins
                        )
                        assignment.coachName?.let { name ->
                            Text(
                                text = "by $name",
                                color = textGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Start button
                Button(
                    onClick = onStartWorkout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = orangePrimary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Start",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Workout name
            Text(
                text = workout.name,
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SpaceGrotesk
            )

            // Exercise preview
            Text(
                text = if (hasMoreExercises) "$exercisePreview +${workout.exercises.size - 3}" else exercisePreview,
                color = textGray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exercise count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${workout.exercises.size} exercises",
                        color = textWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Total sets
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = textGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$totalSets sets",
                        color = textGray,
                        fontSize = 13.sp
                    )
                }
            }

            // Coach notes (if any)
            assignment.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = surfaceColor.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Comment,
                                contentDescription = null,
                                tint = textGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = notes,
                                color = textGray,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}