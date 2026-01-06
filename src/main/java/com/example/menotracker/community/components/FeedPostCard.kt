package com.example.menotracker.community.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.menotracker.community.data.models.FeedPost
import com.example.menotracker.ui.theme.glassPremium

/**
 * Instagram-style post card for the community feed.
 * Shows workout summary with engagement options.
 */
@Composable
fun FeedPostCard(
    post: FeedPost,
    onUserClick: (String) -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onPostClick: () -> Unit,
    onMenuClick: ((FeedPostMenuAction) -> Unit)? = null,
    isOwnPost: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPostClick() }
            .glassPremium(cornerRadius = 16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: User info + time + menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                UserAvatar(
                    avatarUrl = post.userAvatarUrl,
                    displayName = post.userDisplayName,
                    size = 40,
                    onClick = { onUserClick(post.userId) }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name + time
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUserClick(post.userId) }
                ) {
                    Text(
                        text = post.userDisplayName ?: "User",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    post.createdAt?.let { TimeAgo(timestamp = it) }
                }

                // Menu
                if (onMenuClick != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isOwnPost) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        showMenu = false
                                        onMenuClick(FeedPostMenuAction.DELETE)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Report") },
                                    onClick = {
                                        showMenu = false
                                        onMenuClick(FeedPostMenuAction.REPORT)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Workout name + PR badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.workoutName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (post.prsAchieved > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PrBadge(prsAchieved = post.prsAchieved)
                }
            }

            // PR exercises
            if (!post.prExercises.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.prExercises.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Workout stats
            WorkoutStatsRow(
                volumeKg = post.totalVolumeKg,
                sets = post.totalSets,
                reps = post.totalReps,
                durationMinutes = post.durationMinutes
            )

            // Media Carousel (Images + Videos)
            if (post.hasMedia) {
                android.util.Log.d("FeedPostCard", "📹 Post ${post.id} hasMedia=true, images=${post.imageUrls?.size ?: 0}, videos=${post.videoUrls?.size ?: 0}")
                post.videoUrls?.forEach { url ->
                    android.util.Log.d("FeedPostCard", "📹 Video URL: $url")
                }
                Spacer(modifier = Modifier.height(12.dp))
                PostMediaCarousel(
                    imageUrls = post.imageUrls ?: emptyList(),
                    videoUrls = post.videoUrls ?: emptyList(),
                    onDoubleTap = onLikeClick
                )
            }

            // Caption
            if (!post.caption.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Engagement: likes + comments
            EngagementRow(
                likesCount = post.likesCount,
                commentsCount = post.commentsCount,
                isLiked = post.isLiked,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick
            )
        }
    }
}

/**
 * Compact version for profile/grid view
 */
@Composable
fun FeedPostCardCompact(
    post: FeedPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .glassPremium(cornerRadius = 12.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.workoutName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (post.prsAchieved > 0) {
                    PrBadge(prsAchieved = post.prsAchieved)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatVolume(post.totalVolumeKg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                post.createdAt?.let { TimeAgo(timestamp = it) }
            }
        }
    }
}

enum class FeedPostMenuAction {
    DELETE,
    REPORT
}

// ═══════════════════════════════════════════════════════════════════════════════
// VIDEO COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Carousel for displaying multiple workout videos
 */
@Composable
fun PostVideoCarousel(
    videoUrls: List<String>,
    modifier: Modifier = Modifier
) {
    if (videoUrls.isEmpty()) return

    if (videoUrls.size == 1) {
        // Single video - no pager needed
        PostVideoPlayer(
            videoUrl = videoUrls[0],
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        // Multiple videos - show pager with indicators
        val pagerState = rememberPagerState(pageCount = { videoUrls.size })

        Column(modifier = modifier) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
            ) { page ->
                PostVideoPlayer(
                    videoUrl = videoUrls[page],
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Page indicators
            if (videoUrls.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    videoUrls.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single video player with play/pause overlay
 */
@Composable
fun PostVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var showPlayButton by remember { mutableStateOf(true) }

    // Create ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // Cleanup player on dispose
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Update play state
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
        showPlayButton = !isPlaying
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable {
                isPlaying = !isPlaying
            },
        contentAlignment = Alignment.Center
    ) {
        // Video view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Play/Pause overlay
        if (showPlayButton) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                )
            }
        }

        // Video indicator badge (top right)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// UNIFIED MEDIA CAROUSEL (Images + Videos)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Media item type for carousel
 */
sealed class PostMediaItem {
    data class Image(val url: String) : PostMediaItem()
    data class Video(val url: String) : PostMediaItem()
}

/**
 * Combined carousel for images and videos in posts.
 * Instagram-style with double-tap to like.
 */
@Composable
fun PostMediaCarousel(
    imageUrls: List<String>,
    videoUrls: List<String>,
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Combine all media into a single list (images first, then videos)
    val mediaItems: List<PostMediaItem> = remember(imageUrls, videoUrls) {
        imageUrls.map { PostMediaItem.Image(it) } + videoUrls.map { PostMediaItem.Video(it) }
    }

    if (mediaItems.isEmpty()) return

    // Double-tap like animation state
    var showLikeAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(showLikeAnimation) {
        if (showLikeAnimation) {
            kotlinx.coroutines.delay(800)
            showLikeAnimation = false
        }
    }

    if (mediaItems.size == 1) {
        // Single item - no pager needed
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            onDoubleTap?.invoke()
                            showLikeAnimation = true
                        }
                    )
                }
        ) {
            when (val item = mediaItems[0]) {
                is PostMediaItem.Image -> PostImage(imageUrl = item.url)
                is PostMediaItem.Video -> PostVideoPlayer(videoUrl = item.url)
            }

            // Like animation overlay
            if (showLikeAnimation) {
                LikeAnimationOverlay()
            }
        }
    } else {
        // Multiple items - show pager with indicators
        val pagerState = rememberPagerState(pageCount = { mediaItems.size })

        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    onDoubleTap?.invoke()
                                    showLikeAnimation = true
                                }
                            )
                        }
                ) { page ->
                    when (val item = mediaItems[page]) {
                        is PostMediaItem.Image -> PostImage(imageUrl = item.url)
                        is PostMediaItem.Video -> PostVideoPlayer(videoUrl = item.url)
                    }
                }

                // Media count indicator (top right)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${mediaItems.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Like animation overlay
                if (showLikeAnimation) {
                    LikeAnimationOverlay()
                }
            }

            // Page indicators
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                mediaItems.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Single image display with loading state
 */
@Composable
fun PostImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Post image",
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Failed to load",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/**
 * Like animation overlay (heart that appears on double-tap)
 */
@Composable
private fun LikeAnimationOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MEDIA TYPE BADGE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Badge showing media type (image/video/multiple)
 */
@Composable
fun MediaTypeBadge(
    imageCount: Int,
    videoCount: Int,
    modifier: Modifier = Modifier
) {
    val totalCount = imageCount + videoCount
    if (totalCount == 0) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                totalCount > 1 -> {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$totalCount",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                videoCount > 0 -> {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
