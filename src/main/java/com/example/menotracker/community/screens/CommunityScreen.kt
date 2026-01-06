package com.example.menotracker.community.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.community.components.CommunityTabRow
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.community.viewmodels.*

/**
 * Main Community screen with tabs for Feed, Leaderboard, Challenges, and Activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    userId: String,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToChallenge: (String) -> Unit,
    onNavigateToMaxOutFriday: () -> Unit = {},
    onNavigateToCreateChallenge: () -> Unit = {},
    onNavigateBack: () -> Unit,
    communityViewModel: CommunityViewModel = viewModel(),
    feedViewModel: FeedViewModel = viewModel(),
    leaderboardViewModel: LeaderboardViewModel = viewModel(),
    challengeViewModel: ChallengeViewModel = viewModel(),
    gamificationViewModel: GamificationViewModel = viewModel()
) {
    // Feature flag check
    if (!CommunityFeatureFlag.ENABLED) {
        FeatureDisabledScreen(onNavigateBack = onNavigateBack)
        return
    }

    // Initialize ViewModels
    LaunchedEffect(userId) {
        communityViewModel.initialize(userId)
        feedViewModel.initialize(userId)
        leaderboardViewModel.initialize(userId)
        challengeViewModel.initialize(userId)
        gamificationViewModel.initialize(userId)
    }

    val selectedTab by communityViewModel.selectedTab.collectAsState()
    val tabs = listOf("Feed", "Leaderboard", "Challenges", "Activity")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Create challenge button
                    IconButton(onClick = onNavigateToCreateChallenge) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Challenge"
                        )
                    }
                    // Profile button
                    IconButton(onClick = { onNavigateToProfile(userId) }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "My Profile"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            CommunityTabRow(
                selectedTabIndex = selectedTab.ordinal,
                tabs = tabs,
                onTabSelected = { index ->
                    communityViewModel.selectTab(CommunityTab.entries[index])
                }
            )

            // Tab content
            when (selectedTab) {
                CommunityTab.FEED -> {
                    FeedScreen(
                        viewModel = feedViewModel,
                        onUserClick = onNavigateToProfile,
                        onPostClick = onNavigateToPost
                    )
                }
                CommunityTab.LEADERBOARD -> {
                    LeaderboardScreen(
                        viewModel = leaderboardViewModel,
                        onUserClick = onNavigateToProfile
                    )
                }
                CommunityTab.CHALLENGES -> {
                    ChallengeListScreen(
                        viewModel = challengeViewModel,
                        onChallengeClick = onNavigateToChallenge,
                        onMaxOutFridayClick = onNavigateToMaxOutFriday,
                        onCreateChallengeClick = onNavigateToCreateChallenge
                    )
                }
                CommunityTab.ACTIVITY -> {
                    val activityState by gamificationViewModel.activityFeedState.collectAsState()
                    ActivityFeedScreen(
                        activities = activityState.activities,
                        isLoading = activityState.isLoading,
                        onRefresh = { gamificationViewModel.refreshActivityFeed() },
                        onUserClick = onNavigateToProfile,
                        onPostClick = onNavigateToPost,
                        onChallengeClick = onNavigateToChallenge
                    )
                }
            }
        }
    }
}

/**
 * Shown when community feature is disabled
 */
@Composable
private fun FeatureDisabledScreen(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Construction,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Community Coming Soon",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This feature is still in development",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNavigateBack) {
                Text("Go Back")
            }
        }
    }
}
