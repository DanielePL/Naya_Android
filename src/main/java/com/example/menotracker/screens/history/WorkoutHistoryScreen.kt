package com.example.menotracker.screens.history

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.StatisticsRepository
import com.example.menotracker.data.models.WorkoutHistory
import com.example.menotracker.data.models.UserTrainingSummary
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaBackground

// Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.6f)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF1E1E1E), NayaBackground, Color(0xFF1a1410))
)

enum class HistoryViewMode {
    LIST, CALENDAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onWorkoutClick: (WorkoutHistory) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var viewMode by remember { mutableStateOf(HistoryViewMode.LIST) }
    var workoutHistory by remember { mutableStateOf<List<WorkoutHistory>>(emptyList()) }
    var trainingSummary by remember { mutableStateOf<UserTrainingSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var monthWorkouts by remember { mutableStateOf<List<WorkoutHistory>>(emptyList()) }

    // Load data on launch
    LaunchedEffect(userId) {
        isLoading = true
        coroutineScope.launch {
            // Load workout history
            StatisticsRepository.getWorkoutHistory(userId, limit = 50).onSuccess {
                workoutHistory = it
                Log.d("WorkoutHistory", "Loaded ${it.size} workouts")
            }.onFailure {
                Log.e("WorkoutHistory", "Error loading history: ${it.message}")
            }

            // Load training summary
            StatisticsRepository.getUserTrainingSummary(userId).onSuccess {
                trainingSummary = it
            }

            isLoading = false
        }
    }

    // Load month data when month changes
    LaunchedEffect(selectedMonth) {
        StatisticsRepository.getWorkoutHistoryForMonth(
            userId = userId,
            year = selectedMonth.year,
            month = selectedMonth.monthValue
        ).onSuccess {
            monthWorkouts = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History", color = textWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = textWhite)
                    }
                },
                actions = {
                    // View mode toggle
                    IconButton(onClick = {
                        viewMode = if (viewMode == HistoryViewMode.LIST) HistoryViewMode.CALENDAR else HistoryViewMode.LIST
                    }) {
                        Icon(
                            if (viewMode == HistoryViewMode.LIST) Icons.Default.DateRange else Icons.Default.List,
                            contentDescription = "Toggle view",
                            tint = orangeGlow
                        )
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Summary Card
                    trainingSummary?.let { summary ->
                        TrainingSummaryCard(summary)
                    }

                    // Content based on view mode
                    when (viewMode) {
                        HistoryViewMode.LIST -> {
                            WorkoutListView(
                                workouts = workoutHistory,
                                onWorkoutClick = onWorkoutClick
                            )
                        }
                        HistoryViewMode.CALENDAR -> {
                            CalendarView(
                                selectedMonth = selectedMonth,
                                monthWorkouts = monthWorkouts,
                                onMonthChange = { selectedMonth = it },
                                onWorkoutClick = onWorkoutClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingSummaryCard(summary: UserTrainingSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Training Summary",
                    color = textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (summary.currentStreakDays > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${summary.currentStreakDays} day streak",
                            color = orangeGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total",
                    value = "${summary.totalWorkouts}",
                    subLabel = "workouts"
                )
                StatItem(
                    label = "Volume",
                    value = summary.getTotalVolumeDisplay(),
                    subLabel = "lifetime"
                )
                StatItem(
                    label = "This Week",
                    value = "${summary.weekWorkouts}",
                    subLabel = "workouts"
                )
                StatItem(
                    label = "PRs",
                    value = "${summary.totalPRs}",
                    subLabel = "achieved"
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, subLabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = textGray, fontSize = 10.sp)
        Text(value, color = textWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(subLabel, color = textGray, fontSize = 10.sp)
    }
}

@Composable
private fun WorkoutListView(
    workouts: List<WorkoutHistory>,
    onWorkoutClick: (WorkoutHistory) -> Unit
) {
    if (workouts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = textGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No workouts yet",
                    color = textGray,
                    fontSize = 16.sp
                )
                Text(
                    "Complete a workout to see it here",
                    color = textGray.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(workouts) { workout ->
                WorkoutHistoryCard(
                    workout = workout,
                    onClick = { onWorkoutClick(workout) }
                )
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(
    workout: WorkoutHistory,
    onClick: () -> Unit
) {
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
            // Date Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(50.dp)
            ) {
                val dayOfMonth = workout.completedDate?.let {
                    try {
                        LocalDate.parse(it).dayOfMonth.toString()
                    } catch (e: Exception) { "?" }
                } ?: "?"

                val monthAbbr = workout.completedDate?.let {
                    try {
                        LocalDate.parse(it).month.name.take(3)
                    } catch (e: Exception) { "" }
                } ?: ""

                Text(
                    dayOfMonth,
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    monthAbbr,
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            // Workout Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        workout.workoutName,
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (workout.hasPRs) {
                        Spacer(Modifier.width(8.dp))
                        Text("🏆", fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "${workout.totalSets} sets",
                        color = textGray,
                        fontSize = 12.sp
                    )
                    Text(
                        workout.getVolumeDisplay(),
                        color = textGray,
                        fontSize = 12.sp
                    )
                    Text(
                        workout.getDurationDisplay(),
                        color = textGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Arrow
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textGray
            )
        }
    }
}

@Composable
private fun CalendarView(
    selectedMonth: YearMonth,
    monthWorkouts: List<WorkoutHistory>,
    onMonthChange: (YearMonth) -> Unit,
    onWorkoutClick: (WorkoutHistory) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Month Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, "Previous", tint = textWhite)
            }

            Text(
                selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            IconButton(
                onClick = { onMonthChange(selectedMonth.plusMonths(1)) },
                enabled = selectedMonth.isBefore(YearMonth.now())
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    "Next",
                    tint = if (selectedMonth.isBefore(YearMonth.now())) textWhite else textGray.copy(alpha = 0.3f)
                )
            }
        }

        // Day Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    day,
                    color = textGray,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Calendar Grid
        val workoutDates = monthWorkouts.mapNotNull { it.completedDate }.toSet()
        val firstDayOfMonth = selectedMonth.atDay(1)
        val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1) // 0 = Monday
        val daysInMonth = selectedMonth.lengthOfMonth()
        val today = LocalDate.now()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Empty cells for days before month starts
            items(startDayOfWeek) {
                Box(modifier = Modifier.size(48.dp))
            }

            // Days of the month
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val date = selectedMonth.atDay(day)
                val dateString = date.toString()
                val hasWorkout = workoutDates.contains(dateString)
                val isToday = date == today
                val workoutForDay = monthWorkouts.find { it.completedDate == dateString }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasWorkout) {
                            workoutForDay?.let { onWorkoutClick(it) }
                        }
                        .background(
                            when {
                                hasWorkout -> orangePrimary.copy(alpha = 0.8f)
                                isToday -> orangePrimary.copy(alpha = 0.2f)
                                else -> Color.Transparent
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$day",
                        color = when {
                            hasWorkout -> textWhite
                            isToday -> orangeGlow
                            date.isAfter(today) -> textGray.copy(alpha = 0.3f)
                            else -> textWhite
                        },
                        fontWeight = if (hasWorkout || isToday) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Month Summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${monthWorkouts.size}", color = orangeGlow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("workouts", color = textGray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val totalVolume = monthWorkouts.sumOf { it.totalVolumeKg }
                    Text(
                        if (totalVolume >= 1000) String.format("%.1fk", totalVolume / 1000) else "${totalVolume.toInt()}",
                        color = orangeGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text("kg volume", color = textGray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val totalPRs = monthWorkouts.sumOf { it.prsAchieved }
                    Text("$totalPRs", color = orangeGlow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("PRs", color = textGray, fontSize = 12.sp)
                }
            }
        }
    }
}
