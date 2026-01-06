// app/src/main/java/com/example/myapplicationtest/screens/program/ProgramDetailScreen.kt

package com.example.menotracker.screens.program

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.Program
import com.example.menotracker.data.models.Workout
import com.example.menotracker.data.models.ProgramWeek
import com.example.menotracker.data.models.LegacyProgramWorkout
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaOrangeDark
import com.example.menotracker.ui.theme.NayaBackground

// ═══════════════════════════════════════════════════════════════
// DESIGN SYSTEM - COLORS & GRADIENTS
// ═══════════════════════════════════════════════════════════════

private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val orangeDark = NayaOrangeDark

private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1E1E),
        NayaBackground,
        Color(0xFF1a1410)
    )
)

private val buttonGradient = Brush.horizontalGradient(
    colors = listOf(
        NayaOrangeGlow,
        NayaPrimary
    )
)

private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)
private val cardBorder = orangeGlow.copy(alpha = 0.5f)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)

// ═══════════════════════════════════════════════════════════════
// MAIN PROGRAM DETAIL SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    programId: String,
    onNavigateBack: () -> Unit,
    onStartProgram: (String) -> Unit,
    onNavigateToWorkout: (String) -> Unit
) {
    // TODO: Load program from repository
    val program = remember {
        // MOCK DATA for now
        Program(
            id = programId,
            name = "Strength & Hypertrophy",
            description = "12-week program focused on building strength and muscle mass",
            durationInWeeks = 12,
            workoutsPerWeek = 4,
            difficulty = "Intermediate",
            weeks = listOf(
                ProgramWeek(
                    weekNumber = 1,
                    workouts = listOf(
                        LegacyProgramWorkout("w1", "Upper Body Power", "Monday", "75 min"),
                        LegacyProgramWorkout("w2", "Lower Body Strength", "Tuesday", "60 min"),
                        LegacyProgramWorkout("w3", "Rest / Active Recovery", "Wednesday", "30 min"),
                        LegacyProgramWorkout("w4", "Upper Body Hypertrophy", "Thursday", "90 min"),
                        LegacyProgramWorkout("w5", "Lower Body Power", "Friday", "60 min"),
                        LegacyProgramWorkout("w6", "Rest", "Saturday", "0 min"),
                        LegacyProgramWorkout("w7", "Conditioning", "Sunday", "45 min")
                    )
                )
            )
        )
    }

    var showStartDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Program Details", color = textWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Edit */ }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = textWhite
                        )
                    }
                    IconButton(onClick = { /* TODO: Delete */ }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = textWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1a1410)
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Program Header
                item {
                    ProgramHeader(program = program)
                }

                // Program Stats
                item {
                    ProgramStats(program = program)
                }

                // Week Schedule
                item {
                    Text(
                        text = "📅 WEEK SCHEDULE",
                        color = textGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Weekly Workouts
                items(program.weeks.firstOrNull()?.workouts ?: emptyList()) { workout ->
                    WorkoutDayCard(
                        workout = workout,
                        onWorkoutClick = { onNavigateToWorkout(workout.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            // Start Program Button (Floating at bottom)
            Button(
                onClick = { showStartDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(36.dp),
                        ambientColor = orangeGlow,
                        spotColor = orangeGlow
                    ),
                shape = RoundedCornerShape(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 16.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(buttonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = textWhite
                        )
                        Text(
                            text = "START PROGRAM",
                            color = textWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }

    // Start Program Confirmation Dialog
    if (showStartDialog) {
        StartProgramDialog(
            programName = program.name,
            onConfirm = {
                onStartProgram(program.id)
                showStartDialog = false
            },
            onDismiss = { showStartDialog = false }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRAM HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgramHeader(program: Program) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = orangeGlow.copy(alpha = 0.3f),
                spotColor = orangeGlow.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        border = BorderStroke(1.5.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Program Icon & Name
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            orangePrimary.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarViewWeek,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column {
                    Text(
                        text = program.name,
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = orangePrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = program.difficulty,
                            color = orangeGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Description
            if (program.description.isNotEmpty()) {
                Text(
                    text = program.description,
                    color = textGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PROGRAM STATS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProgramStats(program: Program) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = orangeGlow.copy(alpha = 0.3f),
                spotColor = orangeGlow.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        border = BorderStroke(1.5.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Default.CalendarToday,
                value = "${program.durationInWeeks}",
                label = "Weeks"
            )

            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = textGray.copy(alpha = 0.3f)
            )

            StatItem(
                icon = Icons.Default.FitnessCenter,
                value = "${program.workoutsPerWeek}",
                label = "Per Week"
            )

            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = textGray.copy(alpha = 0.3f)
            )

            StatItem(
                icon = Icons.Default.Timer,
                value = "${program.durationInWeeks * program.workoutsPerWeek}",
                label = "Total Workouts"
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = orangeGlow,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            color = textWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = label,
            color = textGray,
            fontSize = 11.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// WORKOUT DAY CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WorkoutDayCard(
    workout: LegacyProgramWorkout,
    onWorkoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRestDay = workout.name.contains("Rest", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isRestDay) { onWorkoutClick() }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isRestDay) Color.Transparent else orangeGlow.copy(alpha = 0.2f),
                spotColor = if (isRestDay) Color.Transparent else orangeGlow.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRestDay) cardBackground.copy(alpha = 0.3f) else cardBackground
        ),
        border = BorderStroke(
            1.dp,
            if (isRestDay) textGray.copy(alpha = 0.3f) else cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Day Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isRestDay)
                                textGray.copy(alpha = 0.2f)
                            else
                                orangePrimary.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRestDay) Icons.Default.Hotel else Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (isRestDay) textGray else orangeGlow,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = workout.day,
                        color = if (isRestDay) textGray else orangeGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = workout.name,
                        color = if (isRestDay) textGray else textWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (workout.duration.isNotEmpty() && workout.duration != "0 min") {
                        Text(
                            text = workout.duration,
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (!isRestDay) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = textGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// START PROGRAM DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StartProgramDialog(
    programName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1a1410),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Rocket,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Start Program?",
                    color = textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You're about to start:",
                    color = textGray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    programName,
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "This will become your active program and today's workout will appear on your Training screen.",
                    color = textGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("LET'S GO! 🔥", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textGray)
            }
        }
    )
}
