package com.example.menotracker.community.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.community.components.*
import com.example.menotracker.community.viewmodels.CommunityViewModel

/**
 * Screen showing another user's profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToFollowers: (String) -> Unit,
    onNavigateToFollowing: (String) -> Unit,
    onNavigateToPost: (String) -> Unit,
    viewModel: CommunityViewModel = viewModel()
) {
    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    val state by viewModel.userProfileState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.profile?.displayName ?: "Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.isCurrentUser) {
                        IconButton(onClick = { /* TODO: Settings */ }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error loading profile",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadUserProfile(userId) }) {
                        Text("Retry")
                    }
                }
            }
            state.profile != null -> {
                val profile = state.profile!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Profile header
                    item {
                        ProfileHeader(
                            profile = profile,
                            isCurrentUser = state.isCurrentUser,
                            onFollowClick = {
                                if (profile.isFollowing) {
                                    viewModel.unfollowUser(userId)
                                } else {
                                    viewModel.followUser(userId)
                                }
                            },
                            onFollowersClick = { onNavigateToFollowers(userId) },
                            onFollowingClick = { onNavigateToFollowing(userId) }
                        )
                    }

                    // Bio
                    if (!profile.bio.isNullOrBlank()) {
                        item {
                            Text(
                                text = profile.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Divider
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Posts section
                    item {
                        Text(
                            text = "Workouts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Loading posts
                    if (state.isLoadingPosts) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    } else if (state.posts.isEmpty()) {
                        item {
                            // Empty or private
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!profile.isPublic && !profile.isFollowing && !state.isCurrentUser) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "This account is private",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Follow to see their workouts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No workouts shared yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = state.posts,
                            key = { it.id }
                        ) { post ->
                            FeedPostCardCompact(
                                post = post,
                                onClick = { onNavigateToPost(post.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Clear state when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearUserProfile()
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: com.example.menotracker.community.data.models.CommunityUserProfile,
    isCurrentUser: Boolean,
    onFollowClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        UserAvatar(
            avatarUrl = profile.avatarUrl,
            displayName = profile.displayName,
            size = 80
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Name
        Text(
            text = profile.displayName ?: "User",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Private badge
        if (!profile.isPublic) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Private account",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = formatCount(profile.postsCount),
                label = "Posts"
            )
            StatItem(
                value = formatCount(profile.followersCount),
                label = "Followers",
                onClick = onFollowersClick
            )
            StatItem(
                value = formatCount(profile.followingCount),
                label = "Following",
                onClick = onFollowingClick
            )
        }

        // Follow button (if not own profile)
        if (!isCurrentUser) {
            Spacer(modifier = Modifier.height(16.dp))
            FollowButton(
                isFollowing = profile.isFollowing,
                onFollowClick = onFollowClick,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    }
}
