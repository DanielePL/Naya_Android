package com.example.menotracker.community.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.menotracker.community.components.*
import com.example.menotracker.community.viewmodels.LeaderboardViewModel

/**
 * Leaderboard screen showing exercise rankings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val exercises = viewModel.getAvailableExercises()

    Column(modifier = modifier.fillMaxSize()) {
        // Friends / Global toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !state.friendsOnly,
                onClick = { viewModel.setFriendsOnly(false) },
                label = { Text("Global") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            FilterChip(
                selected = state.friendsOnly,
                onClick = { viewModel.setFriendsOnly(true) },
                label = { Text("Friends") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        // Exercise selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exercises.forEach { exercise ->
                ExerciseChip(
                    name = exercise.name,
                    isSelected = state.selectedExercise == exercise.id,
                    onClick = { viewModel.selectExercise(exercise.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Leaderboard content
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.error != null && state.entries.isEmpty() -> {
                    LeaderboardErrorState(
                        message = state.error ?: "Unknown error",
                        onRetry = { viewModel.refresh() }
                    )
                }
                state.entries.isEmpty() && !state.isLoading -> {
                    EmptyLeaderboardState(exerciseName = viewModel.getExerciseName())
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header
                        item {
                            Text(
                                text = "${viewModel.getExerciseName()} Leaderboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Entries
                        items(
                            items = state.entries,
                            key = { "${it.exerciseId}_${it.userId}" }
                        ) { entry ->
                            LeaderboardRow(
                                entry = entry,
                                isCurrentUser = entry.rank == state.currentUserRank,
                                onClick = { onUserClick(entry.userId) }
                            )
                        }

                        // Current user rank card (if not in visible list)
                        state.currentUserRank?.let { rank ->
                            if (rank > state.entries.size) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    state.entries.find { it.rank == rank }?.let { userEntry ->
                                        CurrentUserRankCard(
                                            entry = userEntry,
                                            onClick = { /* Already showing user's data */ }
                                        )
                                    }
                                }
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
}

@Composable
private fun EmptyLeaderboardState(exerciseName: String) {
    EmptyState(
        icon = {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        title = "No entries yet",
        message = "Be the first to set a $exerciseName record!",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun LeaderboardErrorState(
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
            text = "Couldn't load leaderboard",
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
