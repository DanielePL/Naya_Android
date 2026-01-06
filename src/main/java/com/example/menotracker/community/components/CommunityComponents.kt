package com.example.menotracker.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// ═══════════════════════════════════════════════════════════════════════════
// AVATAR
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun UserAvatar(
    avatarUrl: String?,
    displayName: String?,
    size: Int = 40,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val sizeDp = size.dp

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayName ?: "User avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = displayName?.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// USER ROW
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun UserRow(
    displayName: String,
    avatarUrl: String?,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = avatarUrl,
            displayName = displayName,
            size = 48
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        trailing?.invoke()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FOLLOW BUTTON
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun FollowButton(
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onFollowClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Following")
        }
    } else {
        Button(
            onClick = onFollowClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Follow")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// STAT ITEM
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ENGAGEMENT ROW (Likes, Comments)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun EngagementRow(
    likesCount: Int,
    commentsCount: Int,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like button
        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatCount(likesCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Comment button
        IconButton(onClick = onCommentClick) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "Comments",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatCount(commentsCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TABS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun CommunityTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// WORKOUT STATS CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun WorkoutStatsRow(
    volumeKg: Double,
    sets: Int,
    reps: Int,
    durationMinutes: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        WorkoutStatItem(
            value = formatVolume(volumeKg),
            label = "Volume"
        )
        WorkoutStatItem(
            value = "$sets",
            label = "Sets"
        )
        WorkoutStatItem(
            value = "$reps",
            label = "Reps"
        )
        if (durationMinutes != null && durationMinutes > 0) {
            WorkoutStatItem(
                value = "${durationMinutes}m",
                label = "Time"
            )
        }
    }
}

@Composable
private fun WorkoutStatItem(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PR BADGE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PrBadge(
    prsAchieved: Int,
    modifier: Modifier = Modifier
) {
    if (prsAchieved > 0) {
        Row(
            modifier = modifier
                .background(
                    Color(0xFFFFD700).copy(alpha = 0.2f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "PR",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (prsAchieved == 1) "1 PR" else "$prsAchieved PRs",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFB8860B),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TIME AGO
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun TimeAgo(
    timestamp: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatTimeAgo(timestamp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

fun formatVolume(volumeKg: Double): String {
    return when {
        volumeKg >= 1_000_000 -> String.format("%.1fM kg", volumeKg / 1_000_000.0)
        volumeKg >= 1_000 -> String.format("%.1fK kg", volumeKg / 1_000.0)
        else -> String.format("%.0f kg", volumeKg)
    }
}

fun formatTimeAgo(timestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(timestamp)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)

        when {
            duration.toMinutes() < 1 -> "just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> "${duration.toHours()}h ago"
            duration.toDays() < 7 -> "${duration.toDays()}d ago"
            duration.toDays() < 30 -> "${duration.toDays() / 7}w ago"
            else -> "${duration.toDays() / 30}mo ago"
        }
    } catch (e: Exception) {
        ""
    }
}
