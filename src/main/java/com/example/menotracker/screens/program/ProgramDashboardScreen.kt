package com.example.menotracker.screens.program

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.TrainingProgramRepository
import com.example.menotracker.data.models.*
import kotlinx.coroutines.launch
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaBackground

// Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val greenSuccess = Color(0xFF4CAF50)
private val yellowWarning = Color(0xFFFFCA28)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.6f)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF1E1E1E), NayaBackground, Color(0xFF1a1410))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDashboardScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onProgramClick: (String) -> Unit,
    onCreateProgram: () -> Unit,
    onStartWorkout: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { TrainingProgramRepository(SupabaseClient.client) }

    var programs by remember { mutableStateOf<List<TrainingProgram>>(emptyList()) }
    var activeProgram by remember { mutableStateOf<ProgramWithWorkouts?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var programToDelete by remember { mutableStateOf<TrainingProgram?>(null) }
    var programToEdit by remember { mutableStateOf<TrainingProgram?>(null) }

    // Load data
    LaunchedEffect(userId) {
        isLoading = true
        coroutineScope.launch {
            // Load all programs
            repository.getPrograms(userId).onSuccess {
                programs = it
                Log.d("ProgramDashboard", "Loaded ${it.size} programs")
            }

            // Load active program with details
            repository.getActiveProgram(userId).onSuccess { active ->
                if (active != null) {
                    repository.getProgramWithWorkouts(active.id).onSuccess {
                        activeProgram = it
                    }
                }
            }

            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programs", color = textWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = textWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Create Program", tint = orangeGlow)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = orangePrimary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Active Program Section
                    item {
                        activeProgram?.let { programWithWorkouts ->
                            ActiveProgramCard(
                                programWithWorkouts = programWithWorkouts,
                                onContinue = {
                                    programWithWorkouts.currentWorkout?.let { workout ->
                                        onStartWorkout(workout.programWorkout.workoutTemplateId)
                                    }
                                },
                                onViewDetails = { onProgramClick(programWithWorkouts.program.id) }
                            )
                        } ?: NoActiveProgramCard(
                            onBrowsePrograms = { /* scroll to list */ }
                        )
                    }

                    // All Programs Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Your Programs",
                                color = textWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            TextButton(onClick = { showCreateDialog = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = orangeGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("New", color = orangeGlow)
                            }
                        }
                    }

                    if (programs.isEmpty()) {
                        item {
                            EmptyProgramsCard(onCreate = { showCreateDialog = true })
                        }
                    } else {
                        items(programs) { program ->
                            ProgramCard(
                                program = program,
                                onClick = { onProgramClick(program.id) },
                                onActivate = {
                                    coroutineScope.launch {
                                        repository.activateProgram(userId, program.id).onSuccess {
                                            // Reload
                                            repository.getProgramWithWorkouts(program.id).onSuccess {
                                                activeProgram = it
                                            }
                                            repository.getPrograms(userId).onSuccess {
                                                programs = it
                                            }
                                        }
                                    }
                                },
                                onDelete = { programToDelete = program },
                                onEdit = { programToEdit = program }
                            )
                        }
                    }

                    // Spacer at bottom
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Create Program Dialog
    if (showCreateDialog) {
        CreateProgramDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description, weeks, goal ->
                coroutineScope.launch {
                    repository.createProgram(
                        userId = userId,
                        name = name,
                        description = description,
                        durationWeeks = weeks,
                        goal = goal
                    ).onSuccess { newProgram ->
                        programs = listOf(newProgram) + programs
                        showCreateDialog = false
                        onProgramClick(newProgram.id)
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    programToDelete?.let { program ->
        DeleteProgramConfirmDialog(
            programName = program.name,
            onDismiss = { programToDelete = null },
            onConfirm = {
                coroutineScope.launch {
                    repository.deleteProgram(program.id).onSuccess {
                        programs = programs.filter { it.id != program.id }
                        if (activeProgram?.program?.id == program.id) {
                            activeProgram = null
                        }
                        programToDelete = null
                    }
                }
            }
        )
    }

    // Edit Name Dialog
    programToEdit?.let { program ->
        EditProgramNameDialog(
            currentName = program.name,
            onDismiss = { programToEdit = null },
            onConfirm = { newName ->
                coroutineScope.launch {
                    repository.updateProgramName(program.id, newName).onSuccess {
                        programs = programs.map {
                            if (it.id == program.id) it.copy(name = newName) else it
                        }
                        programToEdit = null
                    }
                }
            }
        )
    }
}

@Composable
private fun ActiveProgramCard(
    programWithWorkouts: ProgramWithWorkouts,
    onContinue: () -> Unit,
    onViewDetails: () -> Unit
) {
    val program = programWithWorkouts.program
    val nextWorkout = programWithWorkouts.currentWorkout

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(greenSuccess)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Active Program",
                        color = greenSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(onClick = onViewDetails) {
                    Text("Details", color = orangeGlow, fontSize = 12.sp)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Program Name
            Text(
                program.name,
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            program.description?.let {
                Text(
                    it,
                    color = textGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(16.dp))

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Week ${program.currentWeek} of ${program.durationWeeks}",
                        color = textGray,
                        fontSize = 12.sp
                    )
                    Text(
                        "${program.totalSessionsCompleted}/${program.totalSessionsPlanned} workouts",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { program.progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = orangePrimary,
                    trackColor = Color(0xFF333333)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Next Workout
            nextWorkout?.let { workout ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2a2520)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onContinue)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = orangeGlow,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(orangeGlow.copy(alpha = 0.2f))
                                .padding(8.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Next Workout",
                                color = textGray,
                                fontSize = 10.sp
                            )
                            Text(
                                workout.workoutName,
                                color = textWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Week ${workout.programWorkout.weekNumber}, Day ${workout.programWorkout.dayNumber}",
                                color = textGray,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = orangePrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Start", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } ?: run {
                // All workouts completed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(greenSuccess.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = greenSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "All workouts for this week completed!",
                        color = greenSuccess,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun NoActiveProgramCard(onBrowsePrograms: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = textGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No Active Program",
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                "Start a program to track your progress",
                color = textGray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onBrowsePrograms,
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                )
            ) {
                Text("Browse Programs")
            }
        }
    }
}

@Composable
private fun ProgramCard(
    program: TrainingProgram,
    onClick: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    val statusColor = when (program.status) {
        ProgramStatus.IN_PROGRESS -> greenSuccess
        ProgramStatus.COMPLETED -> Color(0xFF2196F3)
        ProgramStatus.PAUSED -> yellowWarning
        else -> textGray
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (program.status) {
                        ProgramStatus.IN_PROGRESS -> Icons.Default.PlayArrow
                        ProgramStatus.COMPLETED -> Icons.Default.CheckCircle
                        ProgramStatus.PAUSED -> Icons.Default.Pause
                        else -> Icons.Default.FitnessCenter
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        program.name,
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (program.isActive) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ACTIVE",
                            color = greenSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(greenSuccess.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "${program.durationWeeks} weeks",
                        color = textGray,
                        fontSize = 12.sp
                    )
                    Text(
                        "${program.workoutsPerWeek}/week",
                        color = textGray,
                        fontSize = 12.sp
                    )
                    if (program.totalSessionsCompleted > 0) {
                        Text(
                            "${program.totalSessionsCompleted}/${program.totalSessionsPlanned} done",
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Progress indicator for in-progress programs
                if (program.status == ProgramStatus.IN_PROGRESS) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { program.progressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = orangePrimary,
                        trackColor = Color(0xFF333333)
                    )
                }
            }

            // Menu button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = textGray
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1a1410))
                ) {
                    if (!program.isActive && program.status != ProgramStatus.COMPLETED) {
                        DropdownMenuItem(
                            text = { Text("Activate", color = textWhite) },
                            onClick = {
                                showMenu = false
                                onActivate()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, null, tint = greenSuccess)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Edit Name", color = textWhite) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, null, tint = orangeGlow)
                        }
                    )
                    HorizontalDivider(color = textGray.copy(alpha = 0.3f))
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFEF5350)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyProgramsCard(onCreate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = textGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No programs yet",
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "Create your first training program",
                color = textGray,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onCreate,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = orangeGlow
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Program")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProgramDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?, weeks: Int, goal: ProgramGoal?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var weeks by remember { mutableStateOf(4) }
    var selectedGoal by remember { mutableStateOf<ProgramGoal?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1a1410),
        title = {
            Text("Create Program", color = textWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Program Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        focusedBorderColor = orangePrimary,
                        unfocusedBorderColor = textGray.copy(alpha = 0.5f),
                        focusedLabelColor = orangePrimary,
                        unfocusedLabelColor = textGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        focusedBorderColor = orangePrimary,
                        unfocusedBorderColor = textGray.copy(alpha = 0.5f),
                        focusedLabelColor = orangePrimary,
                        unfocusedLabelColor = textGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Duration
                Column {
                    Text("Duration", color = textGray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(2, 4, 6, 8, 12)) { w ->
                            FilterChip(
                                selected = weeks == w,
                                onClick = { weeks = w },
                                label = { Text("$w weeks") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = orangePrimary,
                                    selectedLabelColor = textWhite,
                                    containerColor = Color.Transparent,
                                    labelColor = textGray
                                )
                            )
                        }
                    }
                }

                // Goal
                Column {
                    Text("Goal", color = textGray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ProgramGoal.entries.toList()) { goal ->
                            FilterChip(
                                selected = selectedGoal == goal,
                                onClick = {
                                    selectedGoal = if (selectedGoal == goal) null else goal
                                },
                                label = {
                                    Text(
                                        goal.name.lowercase().replaceFirstChar { it.uppercase() }
                                            .replace("_", " ")
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = orangePrimary,
                                    selectedLabelColor = textWhite,
                                    containerColor = Color.Transparent,
                                    labelColor = textGray
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(
                            name,
                            description.takeIf { it.isNotBlank() },
                            weeks,
                            selectedGoal
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                )
            ) {
                Text("Create")
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
private fun DeleteProgramConfirmDialog(
    programName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1a1410),
        title = {
            Text("Delete Program", color = textWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Are you sure you want to delete \"$programName\"?",
                    color = textWhite
                )
                Text(
                    "This will remove all progress and cannot be undone.",
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
        containerColor = Color(0xFF1a1410),
        title = {
            Text("Edit Program Name", color = textWhite, fontWeight = FontWeight.Bold)
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
