package com.example.menotracker.community.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.menotracker.community.data.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Screen for creating a new user challenge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeScreen(
    onNavigateBack: () -> Unit,
    onChallengeCreated: (String) -> Unit,  // Navigate to created challenge
    onInviteUsers: () -> Unit  // Navigate to user picker
) {
    var state by remember { mutableStateOf(CreateChallengeState()) }
    var showDatePicker by remember { mutableStateOf<String?>(null) }  // "start" or "end"
    var showExercisePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Challenge") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // TODO: Submit challenge
                            state = state.copy(isCreating = true)
                        },
                        enabled = isFormValid(state) && !state.isCreating
                    ) {
                        if (state.isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Challenge Type Selection
            item {
                Text(
                    text = "Challenge Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChallengeTypeChip(
                        type = ChallengeType.CUSTOM,
                        label = "Max Weight",
                        icon = Icons.Default.FitnessCenter,
                        selected = state.challengeType == ChallengeType.CUSTOM,
                        onClick = { state = state.copy(challengeType = ChallengeType.CUSTOM) },
                        modifier = Modifier.weight(1f)
                    )
                    ChallengeTypeChip(
                        type = ChallengeType.VOLUME,
                        label = "Volume",
                        icon = Icons.Default.StackedBarChart,
                        selected = state.challengeType == ChallengeType.VOLUME,
                        onClick = { state = state.copy(challengeType = ChallengeType.VOLUME) },
                        modifier = Modifier.weight(1f)
                    )
                    ChallengeTypeChip(
                        type = ChallengeType.STREAK,
                        label = "Streak",
                        icon = Icons.Default.LocalFireDepartment,
                        selected = state.challengeType == ChallengeType.STREAK,
                        onClick = { state = state.copy(challengeType = ChallengeType.STREAK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Title
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { state = state.copy(title = it) },
                    label = { Text("Challenge Title") },
                    placeholder = { Text("e.g., Bench Press Battle") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.title.isBlank() && state.error != null
                )
            }

            // Description
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { state = state.copy(description = it) },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("What's this challenge about?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }

            // Type-specific fields
            item {
                when (state.challengeType) {
                    ChallengeType.CUSTOM, ChallengeType.MAX_OUT_FRIDAY -> {
                        ExerciseSelector(
                            selectedExercise = state.selectedExercise,
                            onSelectExercise = { showExercisePicker = true }
                        )
                    }
                    ChallengeType.VOLUME -> {
                        VolumeTargetInput(
                            targetVolume = state.targetVolume,
                            onTargetChange = { state = state.copy(targetVolume = it) }
                        )
                    }
                    ChallengeType.STREAK -> {
                        StreakTargetInput(
                            targetDays = state.targetStreakDays,
                            onTargetChange = { state = state.copy(targetStreakDays = it) }
                        )
                    }
                }
            }

            // Date Selection
            item {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatePickerField(
                        label = "Start Date",
                        date = state.startDate,
                        onClick = { showDatePicker = "start" },
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerField(
                        label = "End Date",
                        date = state.endDate,
                        onClick = { showDatePicker = "end" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick duration presets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DurationPreset("1 Week", 7) { days ->
                        val start = LocalDate.now()
                        val end = start.plusDays(days.toLong())
                        state = state.copy(
                            startDate = start.toString(),
                            endDate = end.toString()
                        )
                    }
                    DurationPreset("2 Weeks", 14) { days ->
                        val start = LocalDate.now()
                        val end = start.plusDays(days.toLong())
                        state = state.copy(
                            startDate = start.toString(),
                            endDate = end.toString()
                        )
                    }
                    DurationPreset("1 Month", 30) { days ->
                        val start = LocalDate.now()
                        val end = start.plusDays(days.toLong())
                        state = state.copy(
                            startDate = start.toString(),
                            endDate = end.toString()
                        )
                    }
                }
            }

            // Visibility
            item {
                Text(
                    text = "Visibility",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.isPublic,
                        onClick = { state = state.copy(isPublic = true) },
                        label = { Text("Public") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Public,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = !state.isPublic,
                        onClick = { state = state.copy(isPublic = false) },
                        label = { Text("Friends Only") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.People,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                if (!state.isPublic) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onInviteUsers
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Invite Friends",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (state.invitedUsers.isEmpty())
                                        "No friends invited yet"
                                    else
                                        "${state.invitedUsers.size} friends invited",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }

            // Error message
            if (state.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = state.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Exercise Picker Dialog
    if (showExercisePicker) {
        ExercisePickerDialog(
            onDismiss = { showExercisePicker = false },
            onSelect = { id, name ->
                state = state.copy(selectedExercise = id to name)
                showExercisePicker = false
            }
        )
    }

    // Date Picker Dialog
    showDatePicker?.let { field ->
        DatePickerDialog(
            onDismiss = { showDatePicker = null },
            onSelect = { date ->
                state = if (field == "start") {
                    state.copy(startDate = date)
                } else {
                    state.copy(endDate = date)
                }
                showDatePicker = null
            }
        )
    }
}

@Composable
private fun ChallengeTypeChip(
    type: ChallengeType,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExerciseSelector(
    selectedExercise: Pair<String, String>?,
    onSelectExercise: () -> Unit
) {
    Text(
        text = "Exercise",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelectExercise
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = selectedExercise?.second ?: "Select Exercise",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedExercise != null)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun VolumeTargetInput(
    targetVolume: Double?,
    onTargetChange: (Double?) -> Unit
) {
    Text(
        text = "Target Volume",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = targetVolume?.toString() ?: "",
        onValueChange = { onTargetChange(it.toDoubleOrNull()) },
        label = { Text("Target Volume (kg)") },
        placeholder = { Text("e.g., 10000") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        suffix = { Text("kg") }
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Total weight lifted across all workouts during the challenge period",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StreakTargetInput(
    targetDays: Int?,
    onTargetChange: (Int?) -> Unit
) {
    Text(
        text = "Target Streak",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = targetDays?.toString() ?: "",
        onValueChange = { onTargetChange(it.toIntOrNull()) },
        label = { Text("Target Days") },
        placeholder = { Text("e.g., 7") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        suffix = { Text("days") }
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Consecutive days of working out to complete the challenge",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DatePickerField(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (date.isNotBlank()) formatDate(date) else "Select",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (date.isNotBlank())
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DurationPreset(
    label: String,
    days: Int,
    onSelect: (Int) -> Unit
) {
    AssistChip(
        onClick = { onSelect(days) },
        label = { Text(label) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit
) {
    val exercises = listOf(
        "bench_press" to "Bench Press",
        "squat" to "Squat",
        "deadlift" to "Deadlift",
        "overhead_press" to "Overhead Press",
        "barbell_row" to "Barbell Row",
        "weighted_pullup" to "Weighted Pull-Up",
        "leg_press" to "Leg Press",
        "incline_bench" to "Incline Bench Press",
        "lat_pulldown" to "Lat Pulldown",
        "cable_row" to "Cable Row",
        "dumbbell_curl" to "Dumbbell Curl",
        "tricep_pushdown" to "Tricep Pushdown"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Exercise") },
        text = {
            LazyColumn {
                items(exercises) { (id, name) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        modifier = Modifier.clickable { onSelect(id, name) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
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
                        onSelect(date.toString())
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun isFormValid(state: CreateChallengeState): Boolean {
    return state.title.isNotBlank() &&
            state.startDate.isNotBlank() &&
            state.endDate.isNotBlank() &&
            when (state.challengeType) {
                ChallengeType.CUSTOM, ChallengeType.MAX_OUT_FRIDAY -> state.selectedExercise != null
                ChallengeType.VOLUME -> state.targetVolume != null && state.targetVolume > 0
                ChallengeType.STREAK -> state.targetStreakDays != null && state.targetStreakDays > 0
            }
}

private fun formatDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        isoDate
    }
}