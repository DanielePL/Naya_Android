package com.example.menotracker.community.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.community.components.*
import com.example.menotracker.community.viewmodels.CommunityViewModel

/**
 * Screen showing followers or following list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersScreen(
    userId: String,
    currentUserId: String,
    showFollowers: Boolean, // true = followers, false = following
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: CommunityViewModel = viewModel()
) {
    LaunchedEffect(userId, showFollowers) {
        if (showFollowers) {
            viewModel.loadFollowers(userId)
        } else {
            viewModel.loadFollowing(userId)
        }
    }

    val followers by viewModel.followers.collectAsState()
    val following by viewModel.following.collectAsState()
    val isLoading by viewModel.isLoadingFollows.collectAsState()

    val users = if (showFollowers) followers else following
    val title = if (showFollowers) "Followers" else "Following"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            users.isEmpty() -> {
                EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    title = if (showFollowers) "No followers yet" else "Not following anyone",
                    message = if (showFollowers)
                        "When people follow this account, they'll appear here"
                    else
                        "When this account follows people, they'll appear here",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(
                        items = users,
                        key = { it.id }
                    ) { follow ->
                        val displayUserId = if (showFollowers) follow.followerId else follow.followingId
                        val displayName = follow.userDisplayName ?: "User"
                        val avatarUrl = follow.userAvatarUrl

                        UserRow(
                            displayName = displayName,
                            avatarUrl = avatarUrl,
                            onClick = { onUserClick(displayUserId) },
                            trailing = {
                                // Show follow button if not current user
                                if (displayUserId != currentUserId) {
                                    // Note: Would need to track follow status per user
                                    // For now, simplified without follow button in list
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    // Clear state when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearFollows()
        }
    }
}
