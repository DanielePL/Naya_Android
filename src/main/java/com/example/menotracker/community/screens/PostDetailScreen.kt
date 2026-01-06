package com.example.menotracker.community.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.community.components.*
import com.example.menotracker.community.viewmodels.FeedViewModel

/**
 * Screen showing a single post with comments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: FeedViewModel = viewModel()
) {
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(postId, currentUserId) {
        viewModel.initialize(currentUserId)
        viewModel.loadPostDetail(postId)
    }

    val detailState by viewModel.postDetailState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Comment input - fixed at bottom with proper spacing
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Add a comment...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            singleLine = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    viewModel.addComment(postId, commentText.trim())
                                    commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (commentText.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Spacer for system navigation bar
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
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
                        text = "Error loading post",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detailState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadPostDetail(postId) }) {
                        Text("Retry")
                    }
                }
            }
            detailState.post != null -> {
                val post = detailState.post!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Post content
                    item {
                        FeedPostCard(
                            post = post,
                            onUserClick = onUserClick,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onCommentClick = { /* Already on comments */ },
                            onPostClick = { /* Already viewing */ },
                            onMenuClick = { action ->
                                when (action) {
                                    FeedPostMenuAction.DELETE -> {
                                        viewModel.deletePost(post.id)
                                        onNavigateBack()
                                    }
                                    FeedPostMenuAction.REPORT -> { /* TODO */ }
                                }
                            },
                            isOwnPost = post.userId == currentUserId
                        )
                    }

                    // Comments header
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Comments
                    if (detailState.isLoadingComments) {
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
                    } else if (detailState.comments.isEmpty()) {
                        item {
                            Text(
                                text = "No comments yet. Be the first!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(
                            items = detailState.comments,
                            key = { it.id }
                        ) { comment ->
                            CommentItem(
                                displayName = comment.userDisplayName ?: "User",
                                avatarUrl = comment.userAvatarUrl,
                                content = comment.content,
                                timestamp = comment.createdAt ?: "",
                                onUserClick = { onUserClick(comment.userId) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Bottom spacing for keyboard
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Clear state when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPostDetail()
        }
    }
}

@Composable
private fun CommentItem(
    displayName: String,
    avatarUrl: String?,
    content: String,
    timestamp: String,
    onUserClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            avatarUrl = avatarUrl,
            displayName = displayName,
            size = 36,
            onClick = onUserClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                TimeAgo(timestamp = timestamp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
