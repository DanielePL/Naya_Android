package com.example.menotracker.community.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.community.components.*
import com.example.menotracker.community.data.models.MaxOutFridayRotation
import com.example.menotracker.community.viewmodels.ChallengeViewModel
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Dedicated Max Out Friday screen with countdown, leaderboard, and history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxOutFridayScreen(
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onSubmitEntry: () -> Unit,  // Navigate to workout with selected exercise
    viewModel: ChallengeViewModel = viewModel()
) {
    val maxOutFridayState by viewModel.maxOutFridayState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()

    // Load challenge detail when info is available
    LaunchedEffect(maxOutFridayState.info?.id) {
        maxOutFridayState.info?.id?.let { challengeId ->
            viewModel.loadChallengeDetail(challengeId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Max Out Friday") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero section with countdown
            item {
                MaxOutFridayHero(
                    info = maxOutFridayState.info,
                    timeRemainingMillis = maxOutFridayState.timeRemainingMillis
                )
            }

            // User's entry status
            maxOutFridayState.info?.let { info ->
                item {
                    UserEntryCard(
                        userEntryKg = info.userEntryKg,
                        userRank = info.userRank,
                        totalParticipants = info.participantsCount,
                        exerciseName = info.exerciseName,
                        onSubmitEntry = onSubmitEntry
                    )
                }
            }

            // Top 10 Leaderboard
            item {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (detailState.isLoadingEntries) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (detailState.entries.isEmpty()) {
                item {
                    EmptyLeaderboardCard()
                }
            } else {
                itemsIndexed(
                    items = detailState.entries.take(10),
                    key = { _, entry -> entry.id }
                ) { index, entry ->
                    LeaderboardEntryRow(
                        rank = index + 1,
                        displayName = entry.userDisplayName ?: "User",
                        avatarUrl = entry.userAvatarUrl,
                        valueKg = entry.valueKg ?: 0.0,
                        isPr = entry.isPr,
                        isCurrentUser = entry.userId == detailState.userEntry?.userId,
                        onClick = { onUserClick(entry.userId) }
                    )
                }
            }

            // Next week preview
            item {
                NextWeekPreview()
            }

            // Previous winners
            if (maxOutFridayState.previousWinners.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Champions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    PreviousWinnersSection(winners = maxOutFridayState.previousWinners)
                }
            }

            // Exercise rotation info
            item {
                ExerciseRotationInfo()
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MaxOutFridayHero(
    info: com.example.menotracker.community.data.models.MaxOutFridayInfo?,
    timeRemainingMillis: Long
) {
    // Animated countdown
    var remainingTime by remember { mutableLongStateOf(timeRemainingMillis) }

    LaunchedEffect(timeRemainingMillis) {
        remainingTime = timeRemainingMillis
        while (remainingTime > 0) {
            delay(1000)
            remainingTime -= 1000
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF6B00),
                            Color(0xFFFF8C00),
                            Color(0xFFFFAB00)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Fire icon with animation
                val infiniteTransition = rememberInfiniteTransition(label = "fire")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "fire_scale"
                )

                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size((48 * scale).dp),
                    tint = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MAX OUT FRIDAY",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Current exercise
                Text(
                    text = info?.exerciseName ?: "Loading...",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Countdown timer
                CountdownTimer(remainingMillis = remainingTime)

                Spacer(modifier = Modifier.height(16.dp))

                // Participants count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${info?.participantsCount ?: 0} members competing",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownTimer(remainingMillis: Long) {
    val duration = Duration.ofMillis(remainingMillis.coerceAtLeast(0))
    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeUnit(value = days.toInt(), label = "DAYS")
        TimeSeparator()
        TimeUnit(value = hours.toInt(), label = "HRS")
        TimeSeparator()
        TimeUnit(value = minutes.toInt(), label = "MIN")
        TimeSeparator()
        TimeUnit(value = seconds.toInt(), label = "SEC")
    }
}

@Composable
private fun TimeUnit(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TimeSeparator() {
    Text(
        text = ":",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
private fun UserEntryCard(
    userEntryKg: Double?,
    userRank: Int?,
    totalParticipants: Int,
    exerciseName: String,
    onSubmitEntry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (userEntryKg != null)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userEntryKg != null) {
                // User has submitted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${userEntryKg.toInt()} kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Your Best",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "#${userRank ?: "-"}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "of $totalParticipants",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onSubmitEntry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Improve Your Entry")
                }
            } else {
                // Not yet participated
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Join the Challenge!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Complete a workout with $exerciseName to participate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSubmitEntry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start $exerciseName Workout")
                }
            }
        }
    }
}

@Composable
private fun EmptyLeaderboardCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Entries Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Be the first to submit!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeaderboardEntryRow(
    rank: Int,
    displayName: String,
    avatarUrl: String?,
    valueKg: Double,
    isPr: Boolean,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            RankBadge(rank = rank, modifier = Modifier.width(48.dp))

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            UserAvatar(
                avatarUrl = avatarUrl,
                displayName = displayName,
                size = 44
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name & PR badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium
                )
                if (isPr) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Personal Record",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB8860B)
                        )
                    }
                }
            }

            // Weight
            Text(
                text = "${valueKg.toInt()} kg",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when (rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun NextWeekPreview() {
    val nextWeekNumber = (java.time.LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfYear()) + 1)
    val nextExercise = MaxOutFridayRotation.getExerciseForWeek(nextWeekNumber)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Next Week",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = nextExercise.second,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun PreviousWinnersSection(
    winners: List<com.example.menotracker.community.data.models.PreviousWinner>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        winners.forEach { winner ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = winner.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = winner.winnerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${winner.winningWeightKg.toInt()} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRotationInfo() {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Exercise Rotation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Every Friday features a different exercise:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    MaxOutFridayRotation.EXERCISES.forEachIndexed { index, (_, name) ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}