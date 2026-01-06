package com.example.menotracker.community.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.menotracker.community.components.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.community.viewmodels.ChallengeViewModel

/**
 * Screen showing all challenges including Max Out Friday.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeListScreen(
    viewModel: ChallengeViewModel,
    onChallengeClick: (String) -> Unit,
    onMaxOutFridayClick: () -> Unit = {},
    onCreateChallengeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val challengesState by viewModel.challengesState.collectAsState()
    val maxOutFridayState by viewModel.maxOutFridayState.collectAsState()

    PullToRefreshBox(
        isRefreshing = challengesState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        when {
            challengesState.error != null &&
                    challengesState.activeChallenges.isEmpty() &&
                    maxOutFridayState.info == null -> {
                ChallengeErrorState(
                    message = challengesState.error ?: "Unknown error",
                    onRetry = { viewModel.refresh() }
                )
            }
            challengesState.activeChallenges.isEmpty() &&
                    challengesState.upcomingChallenges.isEmpty() &&
                    maxOutFridayState.info == null &&
                    !challengesState.isLoading -> {
                EmptyChallengesState()
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Max Out Friday (featured) - Hero Card
                    if (CommunityFeatureFlag.MAX_OUT_FRIDAY_ENABLED) {
                        item(key = "max_out_friday_hero") {
                            MaxOutFridayHeroCard(
                                maxOutFridayInfo = maxOutFridayState.info,
                                onClick = onMaxOutFridayClick
                            )
                        }
                    }

                    // Create Challenge Button
                    item(key = "create_challenge") {
                        OutlinedButton(
                            onClick = onCreateChallengeClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Your Own Challenge")
                        }
                    }

                    // Active challenges
                    if (challengesState.activeChallenges.isNotEmpty()) {
                        item(key = "active_header") {
                            Text(
                                text = "Active Challenges",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(
                            items = challengesState.activeChallenges,
                            key = { "active_${it.id}" }
                        ) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                userParticipating = false, // TODO: Check participation
                                onClick = { onChallengeClick(challenge.id) }
                            )
                        }
                    }

                    // Upcoming challenges
                    if (challengesState.upcomingChallenges.isNotEmpty()) {
                        item(key = "upcoming_header") {
                            Text(
                                text = "Coming Soon",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(
                            items = challengesState.upcomingChallenges,
                            key = { "upcoming_${it.id}" }
                        ) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                userParticipating = false,
                                onClick = { onChallengeClick(challenge.id) }
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
private fun EmptyChallengesState() {
    EmptyState(
        icon = {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        title = "No challenges yet",
        message = "Check back soon for new challenges!",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ChallengeErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Couldn't load challenges",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
