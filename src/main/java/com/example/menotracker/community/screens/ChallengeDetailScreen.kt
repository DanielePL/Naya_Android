package com.example.menotracker.community.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.community.components.*
import com.example.menotracker.community.data.models.ChallengeStatus
import com.example.menotracker.community.viewmodels.ChallengeViewModel

/**
 * Screen showing details of a single challenge with leaderboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ChallengeViewModel = viewModel()
) {
    LaunchedEffect(challengeId) {
        viewModel.loadChallengeDetail(challengeId)
    }

    val detailState by viewModel.detailState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detailState.challenge?.title ?: "Challenge") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            detailState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            detailState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error loading challenge",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detailState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadChallengeDetail(challengeId) }) {
                        Text("Retry")
                    }
                }
            }
            detailState.challenge != null -> {
                val challenge = detailState.challenge!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Challenge info card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ChallengeTypeIcon(type = challenge.challengeType)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = challenge.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        ChallengeTimeLabel(
                                            startDate = challenge.startDate,
                                            endDate = challenge.endDate,
                                            status = challenge.status
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                challenge.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${challenge.participantsCount} participants",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // User's participation status
                    if (challenge.status == ChallengeStatus.ACTIVE) {
                        item {
                            if (detailState.hasParticipated) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "You're participating!",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            detailState.userEntry?.let { entry ->
                                                Text(
                                                    text = "Your best: ${entry.valueKg?.toInt() ?: 0} kg",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Join this challenge",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Complete a workout with ${challenge.exerciseName ?: "the exercise"} to participate",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Leaderboard header
                    item {
                        Text(
                            text = "Leaderboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Leaderboard entries
                    if (detailState.isLoadingEntries) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (detailState.entries.isEmpty()) {
                        item {
                            EmptyState(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                title = "No entries yet",
                                message = "Be the first to participate!"
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = detailState.entries,
                            key = { _, entry -> entry.id }
                        ) { index, entry ->
                            ChallengeEntryRow(
                                rank = index + 1,
                                displayName = entry.userDisplayName ?: "User",
                                avatarUrl = entry.userAvatarUrl,
                                valueKg = entry.valueKg,
                                valueReps = entry.valueReps,
                                isPr = entry.isPr,
                                isCurrentUser = entry.userId == detailState.userEntry?.userId,
                                onClick = { onUserClick(entry.userId) }
                            )
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeEntryRow(
    rank: Int,
    displayName: String,
    avatarUrl: String?,
    valueKg: Double?,
    valueReps: Int?,
    isPr: Boolean,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        RankBadge(rank = rank, modifier = Modifier.width(40.dp))

        Spacer(modifier = Modifier.width(12.dp))

        // Avatar
        UserAvatar(
            avatarUrl = avatarUrl,
            displayName = displayName,
            size = 40,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium
            )
            if (isPr) {
                Text(
                    text = "Personal Record",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Value
        Column(horizontalAlignment = Alignment.End) {
            if (valueKg != null) {
                Text(
                    text = "${valueKg.toInt()} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (valueReps != null) {
                Text(
                    text = "$valueReps reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
