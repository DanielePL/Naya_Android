// screens/statistics/TrainingStatisticsScreen.kt

package com.example.menotracker.screens.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.data.StatisticsRepository
import com.example.menotracker.data.models.ExerciseStatistics
import com.example.menotracker.data.models.PRHistory
import com.example.menotracker.data.models.PRType
import com.example.menotracker.data.models.WorkoutHistory
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.viewmodels.AccountViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

// Design System - Naya Lab Style
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val cyanAccent = Color(0xFF00D9FF)
private val greenSuccess = Color(0xFF00FF88)
private val yellowWarning = Color(0xFFFFD93D)
private val redError = Color(0xFFFF4757)
private val purpleAccent = Color(0xFFB388FF)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBg = Color(0xFF1E1E1E)

enum class TimeRange(val label: String, val days: Int) {
    WEEK("7D", 7),
    MONTH("30D", 30),
    QUARTER("90D", 90),
    YEAR("1Y", 365),
    ALL("All", -1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingStatisticsScreen(
    accountViewModel: AccountViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToExerciseStats: (exerciseId: String, exerciseName: String) -> Unit = { _, _ -> }
) {
    val userProfile by accountViewModel.userProfile.collectAsState()
    val userId = userProfile?.id

    var selectedTimeRange by remember { mutableStateOf(TimeRange.MONTH) }

    // State for statistics
    var workoutHistory by remember { mutableStateOf<List<WorkoutHistory>>(emptyList()) }
    var exerciseStats by remember { mutableStateOf<List<ExerciseStatistics>>(emptyList()) }
    var recentPRs by remember { mutableStateOf<List<PRHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Computed stats
    val totalWorkouts = workoutHistory.size
    val totalVolume = workoutHistory.sumOf { it.totalVolumeKg }
    val totalSets = workoutHistory.sumOf { it.totalSets }
    val totalPRs = recentPRs.size

    // Weekly volume data for chart (mock for now, would come from DB)
    val weeklyVolumeData = remember(workoutHistory) {
        if (workoutHistory.isEmpty()) {
            listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        } else {
            // Group by week and calculate volume
            workoutHistory.take(8).mapIndexed { index, workout ->
                workout.totalVolumeKg.toFloat()
            }.reversed().let {
                if (it.size < 8) List(8 - it.size) { 0f } + it else it
            }
        }
    }

    // Calculate change percentage
    val volumeChange = if (weeklyVolumeData.size >= 2 && weeklyVolumeData.first() > 0) {
        val first = weeklyVolumeData.first()
        val last = weeklyVolumeData.last()
        if (first > 0) ((last - first) / first * 100).toInt() else 0
    } else 0

    // Calculate this week vs last week comparison
    val currentDate = LocalDate.now()
    val weekFields = WeekFields.of(Locale.getDefault())
    val currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())
    val currentYear = currentDate.year
    val lastWeek = if (currentWeek > 1) currentWeek - 1 else 52
    val lastWeekYear = if (currentWeek > 1) currentYear else currentYear - 1

    // Filter workouts by week
    val thisWeekWorkouts = remember(workoutHistory, currentWeek, currentYear) {
        workoutHistory.filter { it.completedWeek == currentWeek && it.completedYear == currentYear }
    }
    val lastWeekWorkouts = remember(workoutHistory, lastWeek, lastWeekYear) {
        workoutHistory.filter { it.completedWeek == lastWeek && it.completedYear == lastWeekYear }
    }

    // Calculate weekly stats
    val thisWeekCount = thisWeekWorkouts.size
    val lastWeekCount = lastWeekWorkouts.size
    val thisWeekVolume = thisWeekWorkouts.sumOf { it.totalVolumeKg }
    val lastWeekVolume = lastWeekWorkouts.sumOf { it.totalVolumeKg }
    val thisWeekPRCount = thisWeekWorkouts.sumOf { it.prsAchieved }
    val lastWeekPRCount = lastWeekWorkouts.sumOf { it.prsAchieved }

    // Calculate percentage changes
    val workoutChangePercent = if (lastWeekCount > 0) {
        ((thisWeekCount - lastWeekCount).toDouble() / lastWeekCount * 100).toInt()
    } else if (thisWeekCount > 0) 100 else 0

    val volumeChangePercent = if (lastWeekVolume > 0) {
        ((thisWeekVolume - lastWeekVolume) / lastWeekVolume * 100).toInt()
    } else if (thisWeekVolume > 0) 100 else 0

    val prChangeCount = thisWeekPRCount - lastWeekPRCount

    // Load statistics
    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading = true
            try {
                StatisticsRepository.getWorkoutHistory(userId, limit = 50).onSuccess { history ->
                    workoutHistory = history
                }
                StatisticsRepository.getAllExerciseStatistics(userId).onSuccess { stats ->
                    exerciseStats = stats
                }
                StatisticsRepository.getRecentPRs(userId, limit = 10).onSuccess { prs ->
                    recentPRs = prs
                }
            } catch (e: Exception) {
                android.util.Log.e("TrainingStatisticsScreen", "Error loading stats: ${e.message}")
            }
            isLoading = false
        } else {
            isLoading = false
        }
    }

    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Training Statistics",
                            color = textWhite,
                            fontWeight = FontWeight.Bold
                        )
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
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp)
            ) {
                // Time Range Selector
                item {
                    TimeRangeSelector(
                        selectedRange = selectedTimeRange,
                        onRangeSelected = { selectedTimeRange = it }
                    )
                }

                // Summary Card with Count Badges
                item {
                    SummaryCard(
                        totalWorkouts = totalWorkouts,
                        totalVolume = totalVolume,
                        totalSets = totalSets,
                        totalPRs = totalPRs,
                        isLoading = isLoading
                    )
                }

                // Volume Trend Chart
                item {
                    TrendChartCard(
                        title = "TOTAL VOLUME",
                        subtitle = "Training load over time",
                        data = weeklyVolumeData,
                        labels = listOf("W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8"),
                        unit = "kg",
                        color = orangeGlow,
                        changePercent = if (volumeChange >= 0) "+$volumeChange%" else "$volumeChange%",
                        currentValue = totalVolume
                    )
                }

                // Weekly Comparison Card
                item {
                    WeeklyComparisonCard(
                        thisWeekWorkouts = thisWeekCount,
                        lastWeekWorkouts = lastWeekCount,
                        thisWeekVolume = thisWeekVolume,
                        lastWeekVolume = lastWeekVolume,
                        thisWeekPRs = thisWeekPRCount,
                        lastWeekPRs = lastWeekPRCount
                    )
                }

                // PR Timeline
                item {
                    PRTimelineCard(prList = recentPRs.take(5))
                }

                // Top Exercises Section
                item {
                    TopExercisesCard(
                        exerciseStats = exerciseStats.take(5),
                        onExerciseClick = onNavigateToExerciseStats
                    )
                }

                // Recent Workouts
                item {
                    RecentWorkoutsCard(workouts = workoutHistory.take(5))
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeRange.entries.forEach { range ->
            val isSelected = range == selectedRange

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onRangeSelected(range) },
                color = if (isSelected) orangePrimary.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) orangeGlow else textGray.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = range.label,
                    color = if (isSelected) orangeGlow else textGray,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalWorkouts: Int,
    totalVolume: Double,
    totalSets: Int,
    totalPRs: Int,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TRAINING SUMMARY",
                        color = textGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Your progress at a glance",
                        color = textGray.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                Surface(
                    color = orangeGlow.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = orangeGlow)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryCountBadge(
                        count = totalWorkouts,
                        label = "Workouts",
                        color = cyanAccent
                    )
                    SummaryCountBadge(
                        count = totalSets,
                        label = "Sets",
                        color = orangeGlow
                    )
                    SummaryCountBadge(
                        count = totalPRs,
                        label = "PRs",
                        color = yellowWarning
                    )
                    SummaryCountBadge(
                        count = (totalVolume / 1000).toInt(),
                        label = "Tons",
                        color = greenSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCountBadge(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = count.toString(),
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = textGray,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TrendChartCard(
    title: String,
    subtitle: String,
    data: List<Float>,
    labels: List<String>,
    unit: String,
    color: Color,
    changePercent: String,
    currentValue: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
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
                Column {
                    Text(
                        text = title,
                        color = textGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = subtitle,
                        color = textGray.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // Change badge
                val isPositive = changePercent.startsWith("+")
                Surface(
                    color = if (isPositive) greenSuccess.copy(alpha = 0.15f) else redError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isPositive) greenSuccess else redError,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = changePercent,
                            color = if (isPositive) greenSuccess else redError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Current value
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatVolumeShort(currentValue),
                    color = color,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " $unit",
                    color = color.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                TrendLineChart(
                    data = data,
                    labels = labels,
                    color = color,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun TrendLineChart(
    data: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty() || data.all { it == 0f }) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Complete workouts to see trends",
                color = textGray,
                fontSize = 12.sp
            )
        }
        return
    }

    val maxVal = (data.maxOrNull() ?: 1f) * 1.05f
    val minVal = (data.minOrNull() ?: 0f) * 0.95f
    val range = if (maxVal - minVal > 0) maxVal - minVal else 1f

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 20f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        // Draw grid
        val gridColor = Color.Gray.copy(alpha = 0.15f)
        for (i in 0..4) {
            val y = padding + (chartHeight * i / 4)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // Draw line
        if (data.size >= 2) {
            val path = Path()
            val points = data.mapIndexed { index, value ->
                val x = padding + (index.toFloat() / (data.size - 1)) * chartWidth
                val normalizedVal = if (range > 0) (value - minVal) / range else 0.5f
                val y = padding + chartHeight * (1f - normalizedVal)
                Offset(x, y)
            }

            // Draw area fill
            val areaPath = Path().apply {
                moveTo(points.first().x, height - padding)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, height - padding)
                close()
            }
            drawPath(
                path = areaPath,
                color = color.copy(alpha = 0.1f)
            )

            // Draw line
            path.moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point ->
                path.lineTo(point.x, point.y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Draw points
            points.forEachIndexed { index, point ->
                val isLast = index == points.lastIndex
                drawCircle(
                    color = if (isLast) color else color.copy(alpha = 0.6f),
                    radius = if (isLast) 8f else 5f,
                    center = point
                )
                if (isLast) {
                    drawCircle(
                        color = Color(0xFF1E1E1E),
                        radius = 4f,
                        center = point
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyComparisonCard(
    thisWeekWorkouts: Int,
    lastWeekWorkouts: Int,
    thisWeekVolume: Double,
    lastWeekVolume: Double,
    thisWeekPRs: Int,
    lastWeekPRs: Int
) {
    // Calculate real changes
    val workoutDiff = thisWeekWorkouts - lastWeekWorkouts
    val workoutChangePercent = if (lastWeekWorkouts > 0) {
        ((thisWeekWorkouts - lastWeekWorkouts).toDouble() / lastWeekWorkouts * 100).toInt()
    } else if (thisWeekWorkouts > 0) 100 else 0

    val volumeChangePercent = if (lastWeekVolume > 0) {
        ((thisWeekVolume - lastWeekVolume) / lastWeekVolume * 100).toInt()
    } else if (thisWeekVolume > 0) 100 else 0

    val prDiff = thisWeekPRs - lastWeekPRs

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "THIS WEEK VS LAST WEEK",
                color = textGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ComparisonItem(
                    label = "Workouts",
                    thisWeek = "$thisWeekWorkouts",
                    change = if (workoutDiff >= 0) "+$workoutDiff" else "$workoutDiff",
                    isPositive = workoutDiff >= 0
                )
                ComparisonItem(
                    label = "Volume",
                    thisWeek = formatVolumeShort(thisWeekVolume),
                    change = if (volumeChangePercent >= 0) "+$volumeChangePercent%" else "$volumeChangePercent%",
                    isPositive = volumeChangePercent >= 0
                )
                ComparisonItem(
                    label = "PRs",
                    thisWeek = "$thisWeekPRs",
                    change = if (prDiff >= 0) "+$prDiff" else "$prDiff",
                    isPositive = prDiff >= 0
                )
            }
        }
    }
}

@Composable
private fun ComparisonItem(
    label: String,
    thisWeek: String,
    change: String,
    isPositive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = textGray,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = thisWeek,
            color = textWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (isPositive) greenSuccess else redError,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = change,
                color = if (isPositive) greenSuccess else redError,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PRTimelineCard(prList: List<PRHistory>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
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
                    text = "RECENT PRs",
                    color = textGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = yellowWarning,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (prList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = textGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No PRs yet. Keep pushing!",
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                prList.forEach { pr ->
                    PRTimelineItem(pr = pr)
                    if (pr != prList.last()) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PRTimelineItem(pr: PRHistory) {
    val (displayValue, improvement) = when (pr.prType) {
        PRType.WEIGHT -> Pair("${pr.weightKg?.toInt() ?: 0}kg x ${pr.reps ?: 0}", "+Weight")
        PRType.REPS -> Pair("${pr.reps ?: 0} reps", "+Reps")
        PRType.VOLUME -> Pair("${(pr.volumeKg ?: 0.0).toInt()}kg", "+Volume")
        PRType.VELOCITY -> Pair(String.format("%.2f m/s", pr.velocity ?: 0.0), "+Speed")
        PRType.ESTIMATED_1RM -> Pair("${pr.weightKg?.toInt() ?: 0}kg", "+e1RM")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timeline dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(yellowWarning, CircleShape)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pr.getDisplayName().take(25),
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatDate(pr.achievedAt),
                color = textGray,
                fontSize = 11.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = displayValue,
                color = yellowWarning,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = improvement,
                color = greenSuccess,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TopExercisesCard(
    exerciseStats: List<ExerciseStatistics>,
    onExerciseClick: (exerciseId: String, exerciseName: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
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
                    text = "TOP EXERCISES",
                    color = textGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (exerciseStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FitnessCenter,
                            contentDescription = null,
                            tint = textGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Complete workouts to see stats",
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                exerciseStats.forEachIndexed { index, stat ->
                    TopExerciseItem(
                        rank = index + 1,
                        stat = stat,
                        onClick = { onExerciseClick(stat.exerciseId, stat.getDisplayName()) }
                    )
                    if (stat != exerciseStats.last()) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopExerciseItem(
    rank: Int,
    stat: ExerciseStatistics,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Surface(
            color = when (rank) {
                1 -> yellowWarning.copy(alpha = 0.2f)
                2 -> textGray.copy(alpha = 0.3f)
                3 -> orangeGlow.copy(alpha = 0.2f)
                else -> textGray.copy(alpha = 0.15f)
            },
            shape = CircleShape,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$rank",
                    color = when (rank) {
                        1 -> yellowWarning
                        2 -> textGray
                        3 -> orangeGlow
                        else -> textGray
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.getDisplayName().take(25),
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${stat.totalSets} sets • ${formatVolumeShort(stat.totalVolumeKg)}",
                color = textGray,
                fontSize = 11.sp
            )
        }

        if (stat.prWeightKg != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${stat.prWeightKg.toInt()}kg",
                    color = orangeGlow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PR",
                    color = textGray,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = textGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun RecentWorkoutsCard(workouts: List<WorkoutHistory>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
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
                    text = "RECENT WORKOUTS",
                    color = textGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = cyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = textGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No workout history yet",
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                workouts.forEach { workout ->
                    RecentWorkoutItem(workout = workout)
                    if (workout != workouts.last()) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentWorkoutItem(workout: WorkoutHistory) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date badge
        Surface(
            color = cyanAccent.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (day, month) = parseDateParts(workout.completedAt)
                Text(
                    text = day,
                    color = cyanAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = month,
                    color = cyanAccent.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = workout.workoutName,
                    color = textWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (workout.prsAchieved > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = yellowWarning.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${workout.prsAchieved} PR",
                            color = yellowWarning,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = "${workout.totalExercises} exercises • ${workout.totalSets} sets",
                color = textGray,
                fontSize = 11.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatVolumeShort(workout.totalVolumeKg),
                color = orangeGlow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = workout.getDurationDisplay(),
                color = textGray,
                fontSize = 10.sp
            )
        }
    }
}

private fun formatVolumeShort(volume: Double): String {
    return when {
        volume >= 1000000 -> String.format("%.1fM", volume / 1000000)
        volume >= 1000 -> String.format("%.1fk", volume / 1000)
        else -> String.format("%.0f", volume)
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("MMM dd")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun parseDateParts(isoDate: String): Pair<String, String> {
    return try {
        val instant = Instant.parse(isoDate)
        val dayFormatter = DateTimeFormatter.ofPattern("dd")
            .withZone(ZoneId.systemDefault())
        val monthFormatter = DateTimeFormatter.ofPattern("MMM")
            .withZone(ZoneId.systemDefault())
        Pair(dayFormatter.format(instant), monthFormatter.format(instant).uppercase())
    } catch (e: Exception) {
        Pair("--", "---")
    }
}
