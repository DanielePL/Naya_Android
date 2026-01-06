package com.example.menotracker.screens.lab

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.data.models.*
import com.example.menotracker.viewmodels.MoodUiState
import com.example.menotracker.viewmodels.MoodViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Design colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val pinkAccent = Color(0xFFEC4899)
private val tealAccent = Color(0xFF14B8A6)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)

/**
 * Mood Lab Tab
 *
 * Displays mood journaling features:
 * - Quick mood logging with emojis
 * - Weekly mood summary
 * - Mood trends visualization
 * - Recent mood entries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodLabTab(
    userId: String,
    viewModel: MoodViewModel = viewModel(),
    onUpgradeClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayMoods by viewModel.todayMoods.collectAsState()
    val weeklyMoods by viewModel.weeklyMoods.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showEntrySheet by viewModel.showEntrySheet.collectAsState()
    val showUpgradePrompt by viewModel.showUpgradePrompt.collectAsState()

    // Initialize with userId
    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    // Entry Sheet
    if (showEntrySheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeEntrySheet() },
            containerColor = Color(0xFF121212)
        ) {
            MoodEntrySheet(
                onDismiss = { viewModel.closeEntrySheet() },
                onSubmit = { moodType, intensity, timeOfDay, triggers, journalText ->
                    viewModel.logMood(moodType, intensity, timeOfDay, triggers, journalText)
                },
                remainingSlots = uiState.remainingSlots,
                hasUnlimitedAccess = uiState.hasUnlimitedAccess
            )
        }
    }

    // Upgrade Prompt Dialog
    if (showUpgradePrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpgradePrompt() },
            title = { Text("Weekly Limit Reached", color = textWhite) },
            text = {
                Text(
                    "You've used all 3 mood entries for this week. Upgrade to Premium for unlimited mood journaling.",
                    color = textGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUpgradePrompt()
                        onUpgradeClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lavenderPrimary)
                ) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpgradePrompt() }) {
                    Text("Maybe Later", color = textGray)
                }
            },
            containerColor = cardBg
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Quick Mood Buttons
        item {
            QuickMoodSection(
                onMoodClick = { mood -> viewModel.quickLogMood(mood) },
                canAddMood = uiState.canAddMood,
                onLimitReached = { viewModel.openEntrySheet() }
            )
        }

        // Summary Card
        item {
            MoodSummaryCard(
                uiState = uiState,
                onAddMoodClick = { viewModel.openEntrySheet() }
            )
        }

        // Weekly Overview
        uiState.weeklyStats?.let { stats ->
            item {
                WeeklyMoodOverview(stats = stats)
            }
        }

        // Today's Moods Header
        if (todayMoods.isNotEmpty()) {
            item {
                Text(
                    text = "Today's Check-ins",
                    color = textWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(todayMoods) { mood ->
                MoodEntryCard(
                    entry = mood,
                    onDelete = { viewModel.deleteMood(mood.id) }
                )
            }
        }

        // Recent Moods (excluding today)
        val recentMoods = weeklyMoods.filter { entry ->
            val entryDate = entry.loggedAt.substring(0, 10)
            val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            entryDate != today
        }.take(5)

        if (recentMoods.isNotEmpty()) {
            item {
                Text(
                    text = "This Week",
                    color = textWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(recentMoods) { mood ->
                MoodEntryCard(
                    entry = mood,
                    onDelete = { viewModel.deleteMood(mood.id) }
                )
            }
        }

        // Empty state
        if (weeklyMoods.isEmpty() && !isLoading) {
            item {
                EmptyMoodState(
                    onAddClick = { viewModel.openEntrySheet() }
                )
            }
        }

        // Error message
        error?.let { errorMessage ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = errorMessage, color = Color.White)
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

@Composable
private fun QuickMoodSection(
    onMoodClick: (MoodType) -> Unit,
    canAddMood: Boolean,
    onLimitReached: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "How are you feeling right now?",
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MoodType.entries) { mood ->
                    QuickMoodButton(
                        mood = mood,
                        onClick = {
                            if (canAddMood) {
                                onMoodClick(mood)
                            } else {
                                onLimitReached()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickMoodButton(
    mood: MoodType,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(mood.color).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mood.emoji,
                fontSize = 28.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = mood.displayName,
            color = textGray,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun MoodSummaryCard(
    uiState: MoodUiState,
    onAddMoodClick: () -> Unit
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
                Text(
                    text = "This Week",
                    color = lavenderLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                // Remaining slots badge (for free users)
                if (!uiState.hasUnlimitedAccess) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.remainingSlots > 0) {
                            lavenderPrimary.copy(alpha = 0.2f)
                        } else {
                            Color(0xFFF59E0B).copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = "${uiState.remainingSlots} left",
                            color = if (uiState.remainingSlots > 0) lavenderLight else Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Total entries
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.weeklyStats?.totalEntries ?: uiState.todayMoodCount}",
                        color = textWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Check-ins",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }

                // Dominant mood
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val dominantMood = uiState.weeklyStats?.dominantMood ?: uiState.latestMood
                    Text(
                        text = dominantMood?.emoji ?: "--",
                        fontSize = 32.sp
                    )
                    Text(
                        text = dominantMood?.displayName ?: "No data",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }

                // Average intensity
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val avgIntensity = uiState.weeklyStats?.averageIntensity ?: 0f
                    Text(
                        text = if (avgIntensity > 0) String.format("%.1f", avgIntensity) else "--",
                        color = textWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Avg Intensity",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Add detailed entry button
            Button(
                onClick = onAddMoodClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = lavenderPrimary),
                enabled = uiState.canAddMood
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detailed Check-in")
            }
        }
    }
}

@Composable
private fun WeeklyMoodOverview(
    stats: WeeklyMoodSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Mood Distribution",
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mood distribution bars
            stats.moodDistribution.entries
                .sortedByDescending { it.value.occurrenceCount }
                .take(4)
                .forEach { (moodType, moodStats) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = moodType.emoji,
                            fontSize = 20.sp,
                            modifier = Modifier.width(32.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2D2D2D))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(moodStats.percentage.coerceIn(0.05f, 1f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(moodType.color))
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${(moodStats.percentage * 100).toInt()}%",
                            color = textGray,
                            fontSize = 12.sp,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

            // Top triggers
            if (stats.mostCommonTriggers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF2D2D2D))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Common Triggers",
                    color = textGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.mostCommonTriggers.take(3).forEach { (trigger, count) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = lavenderPrimary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = trigger.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$count",
                                    color = textWhite,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodEntryCard(
    entry: MoodEntry,
    onDelete: () -> Unit
) {
    val moodType = entry.moodEnum ?: return

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
            // Mood Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(moodType.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = moodType.emoji,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = moodType.displayName,
                        color = textWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    entry.timeOfDayEnum?.let { time ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = time.emoji,
                            fontSize = 14.sp
                        )
                    }
                }

                // Journal text preview
                entry.journalText?.let { text ->
                    Text(
                        text = text,
                        color = textGray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Time
                val formattedTime = try {
                    val dateTime = LocalDateTime.parse(entry.loggedAt.replace("Z", ""))
                    dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: Exception) {
                    entry.loggedAt.substring(11, 16)
                }

                Text(
                    text = formattedTime,
                    color = textGray.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            // Intensity indicator
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${entry.intensity}",
                    color = getIntensityColor(entry.intensity),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/5",
                    color = textGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyMoodState(
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Start tracking your mood",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No mood entries yet",
                color = textWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track how you feel throughout the week to identify patterns and triggers.",
                color = textGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = lavenderPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Your First Mood")
            }
        }
    }
}

private fun getIntensityColor(intensity: Int): Color {
    return when (intensity) {
        1 -> Color(0xFF10B981) // Green - very mild
        2 -> Color(0xFF84CC16) // Light green
        3 -> Color(0xFFFBBF24) // Yellow - moderate
        4 -> Color(0xFFF97316) // Orange
        5 -> Color(0xFFEF4444) // Red - intense
        else -> Color(0xFFFBBF24)
    }
}
