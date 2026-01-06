package com.example.menotracker.screens.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Sleep Lab Tab
 *
 * Displays sleep tracking data:
 * - Sleep duration
 * - Sleep quality
 * - Night interruptions (hot flashes, bathroom, etc.)
 * - Weekly trends
 */

// Design colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val indigoDeep = Color(0xFF4F46E5)
private val tealAccent = Color(0xFF14B8A6)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)

// Sleep interruption types
enum class SleepInterruption(
    val displayName: String,
    val color: Color
) {
    NIGHT_SWEAT("Nachtschweiß", Color(0xFF6366F1)),
    HOT_FLASH("Hitzewallung", Color(0xFFF97316)),
    BATHROOM("Toilette", Color(0xFF14B8A6)),
    ANXIETY("Unruhe", Color(0xFFEC4899)),
    OTHER("Sonstiges", Color(0xFF9CA3AF))
}

// Sample data class
data class SleepEntry(
    val date: LocalDate,
    val bedTime: LocalTime,
    val wakeTime: LocalTime,
    val qualityRating: Int, // 1-5 stars
    val interruptions: List<SleepInterruption>
) {
    val durationMinutes: Long
        get() {
            var duration = ChronoUnit.MINUTES.between(bedTime, wakeTime)
            if (duration < 0) duration += 24 * 60 // Handle crossing midnight
            return duration
        }

    val durationFormatted: String
        get() {
            val hours = durationMinutes / 60
            val mins = durationMinutes % 60
            return "${hours}h ${mins}m"
        }
}

@Composable
fun SleepLabTab() {
    // Sample data - will be replaced with real data from repository
    val recentSleep = remember {
        listOf(
            SleepEntry(
                date = LocalDate.now(),
                bedTime = LocalTime.of(22, 30),
                wakeTime = LocalTime.of(6, 15),
                qualityRating = 3,
                interruptions = listOf(SleepInterruption.NIGHT_SWEAT, SleepInterruption.BATHROOM)
            ),
            SleepEntry(
                date = LocalDate.now().minusDays(1),
                bedTime = LocalTime.of(23, 0),
                wakeTime = LocalTime.of(6, 45),
                qualityRating = 4,
                interruptions = listOf(SleepInterruption.BATHROOM)
            ),
            SleepEntry(
                date = LocalDate.now().minusDays(2),
                bedTime = LocalTime.of(22, 15),
                wakeTime = LocalTime.of(5, 30),
                qualityRating = 2,
                interruptions = listOf(SleepInterruption.HOT_FLASH, SleepInterruption.NIGHT_SWEAT, SleepInterruption.ANXIETY)
            ),
            SleepEntry(
                date = LocalDate.now().minusDays(3),
                bedTime = LocalTime.of(22, 45),
                wakeTime = LocalTime.of(7, 0),
                qualityRating = 5,
                interruptions = emptyList()
            )
        )
    }

    val avgDuration = recentSleep.map { it.durationMinutes }.average()
    val avgQuality = recentSleep.map { it.qualityRating }.average()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Sleep Score Card
        item {
            SleepScoreCard(
                avgDuration = avgDuration,
                avgQuality = avgQuality,
                totalInterruptions = recentSleep.sumOf { it.interruptions.size }
            )
        }

        // Quick Log Button
        item {
            QuickLogSleepButton(
                onLogSleep = { /* Navigate to sleep logging */ }
            )
        }

        // Weekly Stats
        item {
            Text(
                text = "Wochenübersicht",
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            WeeklySleepChart(entries = recentSleep)
        }

        // Interruption Summary
        item {
            InterruptionSummaryCard(
                interruptions = recentSleep.flatMap { it.interruptions }
            )
        }

        // Recent Entries
        item {
            Text(
                text = "Letzte Nächte",
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(recentSleep) { entry ->
            SleepEntryCard(entry = entry)
        }

        // Spacer at bottom
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SleepScoreCard(
    avgDuration: Double,
    avgQuality: Double,
    totalInterruptions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
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
                        text = "Schlaf-Score",
                        color = lavenderLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Diese Woche",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }

                // Sleep Quality Ring
                SleepQualityRing(
                    quality = avgQuality.toFloat() / 5f,
                    size = 80.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Average Duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hours = (avgDuration / 60).toInt()
                    val mins = (avgDuration % 60).toInt()
                    Text(
                        text = "${hours}h ${mins}m",
                        color = textWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ø Dauer",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }

                // Quality Stars
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < avgQuality.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (index < avgQuality.toInt()) Color(0xFFFBBF24) else textGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Ø Qualität",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }

                // Interruptions
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalInterruptions",
                        color = if (totalInterruptions > 5) Color(0xFFEF4444) else textWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Unterbrechungen",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepQualityRing(
    quality: Float, // 0-1
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2

            // Background ring
            drawCircle(
                color = Color(0xFF374151),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Progress ring
            drawArc(
                color = lavenderLight,
                startAngle = -90f,
                sweepAngle = quality * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.toPx() - strokeWidth, size.toPx() - strokeWidth)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(quality * 100).toInt()}%",
                color = textWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickLogSleepButton(
    onLogSleep: () -> Unit
) {
    Button(
        onClick = onLogSleep,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = indigoDeep
        )
    ) {
        Icon(
            imageVector = Icons.Default.Bedtime,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Schlaf erfassen",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WeeklySleepChart(
    entries: List<SleepEntry>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Simple bar chart showing sleep duration per day
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxDuration = entries.maxOfOrNull { it.durationMinutes } ?: 480L

                entries.takeLast(7).forEach { entry ->
                    val heightFraction = (entry.durationMinutes.toFloat() / maxDuration).coerceIn(0.1f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bar
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height((100 * heightFraction).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (entry.qualityRating >= 4) tealAccent
                                    else if (entry.qualityRating >= 3) lavenderLight
                                    else Color(0xFFF97316)
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Day label
                        Text(
                            text = entry.date.dayOfWeek.name.take(2),
                            color = textGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterruptionSummaryCard(
    interruptions: List<SleepInterruption>
) {
    val grouped = interruptions.groupingBy { it }.eachCount()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Unterbrechungs-Gründe",
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (grouped.isEmpty()) {
                Text(
                    text = "Keine Unterbrechungen diese Woche!",
                    color = tealAccent,
                    fontSize = 14.sp
                )
            } else {
                grouped.entries.sortedByDescending { it.value }.forEach { (interruption, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(interruption.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = interruption.displayName,
                                color = textGray,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "${count}x",
                            color = textWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepEntryCard(
    entry: SleepEntry
) {
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
            // Moon Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(indigoDeep.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = lavenderLight,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.date.format(DateTimeFormatter.ofPattern("EEEE, dd.MM.")),
                    color = textWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${entry.bedTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${entry.wakeTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                        color = textGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${entry.durationFormatted}",
                        color = lavenderLight,
                        fontSize = 13.sp
                    )
                }

                if (entry.interruptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        entry.interruptions.take(3).forEach { interruption ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = interruption.color.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = interruption.displayName,
                                    color = interruption.color,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quality Stars
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    repeat(entry.qualityRating) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
