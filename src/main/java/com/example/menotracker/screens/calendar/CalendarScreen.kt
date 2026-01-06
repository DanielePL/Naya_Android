package com.example.menotracker.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.ui.theme.NayaOrange
import com.example.menotracker.viewmodels.CalendarViewModel
import com.example.menotracker.viewmodels.WorkoutDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onAddWorkoutClick: () -> Unit = {}
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val workoutDays = viewModel.workoutDays

    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Calendar") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Go to Today", tint = Color.White)
                        }
                    }
                )
            },
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddWorkoutClick,
                    containerColor = NayaOrange,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Workout",
                        tint = Color.White
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Month Navigation and Calendar Grid
                item {
                    CalendarCard(currentMonth, selectedDate, workoutDays) {
                        currentMonth = it
                    }
                }

                // Stats Card
                item {
                    StatsCard(workoutDays, currentMonth)
                }

                // Today's Plan
                item {
                    TodaysPlanCard(selectedDate, workoutDays)
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    workoutDays: List<WorkoutDay>,
    onMonthChanged: (YearMonth) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthNavigation(currentMonth, onMonthChanged)
            Spacer(modifier = Modifier.height(16.dp))
            CalendarGrid(currentMonth, selectedDate, workoutDays)
        }
    }
}

@Composable
private fun MonthNavigation(currentMonth: YearMonth, onMonthChanged: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color.White)
        }
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        IconButton(onClick = { onMonthChanged(currentMonth.plusMonths(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.White)
        }
    }
}


@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    workoutDays: List<WorkoutDay>
) {
    var localSelectedDate by remember { mutableStateOf(selectedDate) }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(text = day, color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(260.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDayOfMonth = currentMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 for Monday, 7 for Sunday

            // Add empty cells for days before the first of the month
            items(firstDayOfWeek - 1) {
                Box(modifier = Modifier.size(36.dp))
            }

            items(daysInMonth) { day ->
                val date = currentMonth.atDay(day + 1)
                val hasWorkout = workoutDays.any { it.date == date }
                val isSelected = date == localSelectedDate

                CalendarDay(day + 1, isSelected, hasWorkout) {
                    localSelectedDate = date
                }
            }
        }
    }
}


@Composable
private fun CalendarDay(
    day: Int,
    isSelected: Boolean,
    hasWorkout: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) NayaOrange else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasWorkout && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Color.Green, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun StatsCard(workoutDays: List<WorkoutDay>, currentMonth: YearMonth) {
    val thisWeekWorkouts = workoutDays.count {
        it.date.isAfter(LocalDate.now().minusDays(7))
    }
    val dayStreak = calculateStreak(workoutDays)
    val thisMonthWorkouts = workoutDays.count {
        it.date.year == currentMonth.year && it.date.month == currentMonth.month
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(value = "$thisWeekWorkouts/5", label = "This Week")
            StatItem(value = dayStreak.toString(), label = "Day Streak")
            StatItem(value = thisMonthWorkouts.toString(), label = "This Month")
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun TodaysPlanCard(selectedDate: LocalDate, workoutDays: List<WorkoutDay>) {
    val workoutsForDate = workoutDays.firstOrNull { it.date == selectedDate }?.workouts ?: emptyList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TODAY'S PLAN",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "+ Add", color = NayaOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))
                    .uppercase(Locale.getDefault()),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (workoutsForDate.isNotEmpty()) {
                workoutsForDate.forEach { workout ->
                    WorkoutItem(workout)
                }
            } else {
                Text("No workouts scheduled", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun WorkoutItem(workout: com.example.menotracker.viewmodels.Workout) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.dp, Color.Gray, CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(workout.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("${workout.time} • ${workout.duration}", color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Details", tint = Color.Gray)
    }
}

private fun calculateStreak(workoutDays: List<WorkoutDay>): Int {
    if (workoutDays.isEmpty()) return 0
    val sortedDates = workoutDays.map { it.date }.sorted().distinct()
    var streak = 0
    var currentDate = LocalDate.now()

    // Check if today has a workout
    if (sortedDates.contains(currentDate)) {
        streak++
        currentDate = currentDate.minusDays(1)
    }

    // Check for consecutive days backwards
    while (sortedDates.contains(currentDate)) {
        streak++
        currentDate = currentDate.minusDays(1)
    }

    return streak
}
