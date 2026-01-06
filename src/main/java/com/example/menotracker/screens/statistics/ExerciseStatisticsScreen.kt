// screens/statistics/ExerciseStatisticsScreen.kt

package com.example.menotracker.screens.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.ExerciseStatistics
import com.example.menotracker.data.models.PRHistory
import com.example.menotracker.data.models.PRType
import com.example.menotracker.data.models.TrendDirection
import com.example.menotracker.ui.theme.AppBackground
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

// Design System (matching other screens)
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1E1E1E)
private val surfaceColor = Color(0xFF1A1A1A)
private val greenSuccess = Color(0xFF4CAF50)
private val redDecline = Color(0xFFF44336)
private val blueInfo = Color(0xFF2196F3)

@Composable
fun ExerciseStatisticsScreen(
    exerciseId: String,
    exerciseName: String,
    statistics: ExerciseStatistics?,
    prHistory: List<PRHistory>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                ExerciseStatsHeader(
                    exerciseName = exerciseName,
                    onBackClick = onBackClick
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = orangeGlow)
                    }
                }
            } else if (statistics == null) {
                // No stats yet
                item {
                    NoStatsCard()
                }
            } else {
                // PR Cards Section
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically { -40 }
                    ) {
                        PRCardsSection(statistics = statistics)
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }

                // Estimated 1RM Card
                if (statistics.estimated1rmKg != null) {
                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn() + slideInVertically { -40 }
                        ) {
                            Estimated1RMCard(
                                e1rm = statistics.estimated1rmKg,
                                formula = statistics.estimated1rmFormula ?: "epley"
                            )
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }

                // Lifetime Stats Section
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically { -40 }
                    ) {
                        LifetimeStatsSection(statistics = statistics)
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }

                // Performance Trend Card
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically { -40 }
                    ) {
                        PerformanceTrendCard(statistics = statistics)
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }

                // PR History Timeline
                if (prHistory.isNotEmpty()) {
                    item {
                        PRHistorySectionHeader()
                    }

                    items(prHistory.take(10)) { pr ->
                        PRHistoryItem(pr = pr)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseStatsHeader(
    exerciseName: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textWhite
            )
        }

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                text = exerciseName,
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                text = "Exercise Statistics",
                color = textGray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NoStatsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                tint = textGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No Statistics Yet",
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Complete a workout with this exercise to start tracking your progress",
                color = textGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PR CARDS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PRCardsSection(statistics: ExerciseStatistics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "PERSONAL RECORDS",
                color = textGray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Weight PR (Main)
        if (statistics.prWeightKg != null) {
            MainPRCard(
                title = "Weight PR",
                value = "${statistics.prWeightKg.toInt()}kg",
                subtitle = "x ${statistics.prWeightReps ?: 1} reps",
                date = statistics.prWeightDate,
                icon = Icons.Default.FitnessCenter
            )
            Spacer(Modifier.height(12.dp))
        }

        // Secondary PRs Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (statistics.prReps != null) {
                SmallPRCard(
                    modifier = Modifier.weight(1f),
                    title = "Max Reps",
                    value = "${statistics.prReps}",
                    subtitle = "@ ${statistics.prRepsWeightKg?.toInt() ?: 0}kg",
                    icon = Icons.Outlined.Repeat
                )
            }
            if (statistics.prVolumeKg != null) {
                SmallPRCard(
                    modifier = Modifier.weight(1f),
                    title = "Volume PR",
                    value = "${statistics.prVolumeKg.toInt()}",
                    subtitle = "kg in session",
                    icon = Icons.Outlined.DataUsage
                )
            }
        }

        // Velocity PR if available
        if (statistics.prVelocity != null) {
            Spacer(Modifier.height(12.dp))
            SmallPRCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Velocity PR",
                value = String.format("%.2f", statistics.prVelocity),
                subtitle = "m/s",
                icon = Icons.Outlined.Speed
            )
        }
    }
}

@Composable
private fun MainPRCard(
    title: String,
    value: String,
    subtitle: String,
    date: String?,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            orangePrimary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Surface(
                    shape = CircleShape,
                    color = orangePrimary.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = orangeGlow,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = textGray,
                        fontSize = 13.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = value,
                            color = orangeGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                        Text(
                            text = subtitle,
                            color = textWhite,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (date != null) {
                        Text(
                            text = formatDate(date),
                            color = textGray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Trophy Icon
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = orangeGlow.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun SmallPRCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, textGray.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    color = textGray,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    color = textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = subtitle,
                    color = textGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ESTIMATED 1RM CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun Estimated1RMCard(e1rm: Double, formula: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, blueInfo.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = blueInfo.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "1",
                        color = blueInfo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estimated 1RM",
                    color = textGray,
                    fontSize = 13.sp
                )
                Text(
                    text = "${e1rm.toInt()}kg",
                    color = blueInfo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = surfaceColor
            ) {
                Text(
                    text = formula.uppercase(),
                    color = textGray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// LIFETIME STATS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LifetimeStatsSection(statistics: ExerciseStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timeline,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "LIFETIME STATS",
                    color = textGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = formatVolume(statistics.totalVolumeKg),
                    label = "Total Volume",
                    unit = "kg"
                )
                StatItem(
                    value = "${statistics.totalSets}",
                    label = "Total Sets"
                )
                StatItem(
                    value = "${statistics.totalReps}",
                    label = "Total Reps"
                )
                StatItem(
                    value = "${statistics.totalSessions}",
                    label = "Sessions"
                )
            }

            if (statistics.avgWeightKg != null || statistics.avgRepsPerSet != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = textGray.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                // Averages Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (statistics.avgWeightKg != null) {
                        StatItem(
                            value = "${statistics.avgWeightKg.toInt()}",
                            label = "Avg Weight",
                            unit = "kg"
                        )
                    }
                    if (statistics.avgRepsPerSet != null) {
                        StatItem(
                            value = String.format("%.1f", statistics.avgRepsPerSet),
                            label = "Avg Reps/Set"
                        )
                    }
                    if (statistics.avgSetsPerSession != null) {
                        StatItem(
                            value = String.format("%.1f", statistics.avgSetsPerSession),
                            label = "Avg Sets/Session"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    unit: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                color = textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            if (unit != null) {
                Text(
                    text = unit,
                    color = orangeGlow,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Text(
            text = label,
            color = textGray,
            fontSize = 11.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PERFORMANCE TREND CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PerformanceTrendCard(statistics: ExerciseStatistics) {
    val trendColor = when (statistics.trendDirection) {
        TrendDirection.IMPROVING -> greenSuccess
        TrendDirection.DECLINING -> redDecline
        else -> textGray
    }
    val trendIcon = when (statistics.trendDirection) {
        TrendDirection.IMPROVING -> Icons.AutoMirrored.Filled.TrendingUp
        TrendDirection.DECLINING -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, trendColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
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
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "PERFORMANCE TREND",
                        color = textGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Trend Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = trendColor.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(16.dp)
                        )
                        if (statistics.trendPercentage != null) {
                            Text(
                                text = "${if (statistics.trendPercentage >= 0) "+" else ""}${statistics.trendPercentage.toInt()}%",
                                color = trendColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Recent Performance Comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecentVsLifetimeStat(
                    label = "Weight",
                    recent = statistics.recentAvgWeightKg?.toInt()?.toString() ?: "-",
                    lifetime = statistics.avgWeightKg?.toInt()?.toString() ?: "-",
                    unit = "kg"
                )
                RecentVsLifetimeStat(
                    label = "Reps",
                    recent = statistics.recentAvgReps?.let { String.format("%.1f", it) } ?: "-",
                    lifetime = statistics.avgRepsPerSet?.let { String.format("%.1f", it) } ?: "-"
                )
                RecentVsLifetimeStat(
                    label = "Sessions",
                    recent = "${statistics.recentSessions}",
                    lifetime = "${statistics.totalSessions}"
                )
            }

            Spacer(Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(orangeGlow, CircleShape)
                )
                Spacer(Modifier.width(4.dp))
                Text("Last 4 weeks", color = textGray, fontSize = 10.sp)
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(textGray, CircleShape)
                )
                Spacer(Modifier.width(4.dp))
                Text("Lifetime", color = textGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RecentVsLifetimeStat(
    label: String,
    recent: String,
    lifetime: String,
    unit: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = textGray,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = recent,
                color = orangeGlow,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            if (unit != null) {
                Text(
                    text = unit,
                    color = orangeGlow,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Text(
            text = "vs $lifetime",
            color = textGray,
            fontSize = 10.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PR HISTORY TIMELINE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PRHistorySectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = orangeGlow,
            modifier = Modifier.size(20.dp)
        )
        Text(
            "PR HISTORY",
            color = textGray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PRHistoryItem(pr: PRHistory) {
    val (prLabel, prValue, prUnit) = when (pr.prType) {
        PRType.WEIGHT -> Triple("Weight PR", "${pr.weightKg?.toInt() ?: 0}kg x ${pr.reps ?: 0}", "")
        PRType.REPS -> Triple("Rep PR", "${pr.reps ?: 0} reps", "@ ${pr.weightKg?.toInt() ?: 0}kg")
        PRType.VOLUME -> Triple("Volume PR", "${pr.volumeKg?.toInt() ?: 0}kg", "total")
        PRType.VELOCITY -> Triple("Velocity PR", String.format("%.2f", pr.velocity ?: 0.0), "m/s")
        PRType.ESTIMATED_1RM -> Triple("1RM", "${pr.weightKg?.toInt() ?: 0}kg", "estimated")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, textGray.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeline dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(orangeGlow, CircleShape)
            )

            Spacer(Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prLabel,
                    color = textGray,
                    fontSize = 11.sp
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = prValue,
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (prUnit.isNotEmpty()) {
                        Text(
                            text = prUnit,
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Improvement badge
            if (pr.improvementPercentage != null && pr.improvementPercentage > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = greenSuccess.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "+${pr.improvementPercentage.toInt()}%",
                        color = greenSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            // Date
            Text(
                text = formatDate(pr.achievedAt),
                color = textGray,
                fontSize = 11.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun formatDate(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val now = Instant.now()
        val daysBetween = ChronoUnit.DAYS.between(instant, now)

        when {
            daysBetween == 0L -> "Today"
            daysBetween == 1L -> "Yesterday"
            daysBetween < 7 -> "$daysBetween days ago"
            daysBetween < 30 -> "${daysBetween / 7} weeks ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        isoDate.take(10) // fallback to date portion
    }
}

private fun formatVolume(volumeKg: Double): String {
    return when {
        volumeKg >= 1_000_000 -> String.format("%.1fM", volumeKg / 1_000_000)
        volumeKg >= 1_000 -> String.format("%.1fK", volumeKg / 1_000)
        else -> volumeKg.toInt().toString()
    }
}
