package com.example.menotracker.community.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.menotracker.community.components.*
import com.example.menotracker.community.data.models.FeedTab
import com.example.menotracker.community.viewmodels.FeedViewModel

/**
 * Feed screen showing posts from followed users or discover feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val feedState by viewModel.feedState.collectAsState()
    val listState = rememberLazyListState()

    // Load more when reaching end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= feedState.posts.size - 3 && feedState.hasMore && !feedState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Feed type tabs (Following / Discover)
        FeedTypeSelector(
            currentTab = feedState.currentTab,
            onTabSelected = { viewModel.switchTab(it) }
        )

        PullToRefreshBox(
            isRefreshing = feedState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                feedState.error != null && feedState.posts.isEmpty() -> {
                    ErrorState(
                        message = feedState.error ?: "Unknown error",
                        onRetry = { viewModel.refresh() }
                    )
                }
                feedState.posts.isEmpty() && !feedState.isLoading -> {
                    EmptyFeedState(currentTab = feedState.currentTab)
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = feedState.posts,
                            key = { it.id }
                        ) { post ->
                            FeedPostCard(
                                post = post,
                                onUserClick = onUserClick,
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onCommentClick = { onPostClick(post.id) },
                                onPostClick = { onPostClick(post.id) },
                                onMenuClick = { action ->
                                    when (action) {
                                        FeedPostMenuAction.DELETE -> viewModel.deletePost(post.id)
                                        FeedPostMenuAction.REPORT -> { /* TODO: Report functionality */ }
                                    }
                                }
                            )
                        }

                        // Loading more indicator
                        if (feedState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTypeSelector(
    currentTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentTab == FeedTab.FOLLOWING,
            onClick = { onTabSelected(FeedTab.FOLLOWING) },
            label = { Text("Following") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        FilterChip(
            selected = currentTab == FeedTab.DISCOVER,
            onClick = { onTabSelected(FeedTab.DISCOVER) },
            label = { Text("Discover") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
private fun EmptyFeedState(currentTab: FeedTab) {
    EmptyState(
        icon = {
            Icon(
                imageVector = if (currentTab == FeedTab.FOLLOWING)
                    Icons.Default.People else Icons.Default.Explore,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        title = if (currentTab == FeedTab.FOLLOWING)
            "No posts yet" else "Discover workouts",
        message = if (currentTab == FeedTab.FOLLOWING)
            "Follow other members to see their workouts here"
        else
            "Be the first to share your workout!",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ErrorState(
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
            text = "Something went wrong",
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
