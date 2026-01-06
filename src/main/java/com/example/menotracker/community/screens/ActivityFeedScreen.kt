package com.example.menotracker.community.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.menotracker.community.components.*
import com.example.menotracker.community.data.models.ActivityItem
import com.example.menotracker.community.data.models.ActivityType

/**
 * Activity feed showing recent actions from followed users.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedScreen(
    activities: List<ActivityItem>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onUserClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onChallengeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (activities.isEmpty() && !isLoading) {
            EmptyActivityState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(
                    items = activities,
                    key = { it.id }
                ) { activity ->
                    ActivityItemRow(
                        activity = activity,
                        onUserClick = onUserClick,
                        onItemClick = {
                            when (activity.activityType) {
                                ActivityType.WORKOUT_SHARED,
                                ActivityType.PR_ACHIEVED,
                                ActivityType.LIKE,
                                ActivityType.COMMENT -> {
                                    activity.targetId?.let { onPostClick(it) }
                                }
                                ActivityType.CHALLENGE_JOINED,
                                ActivityType.CHALLENGE_WON -> {
                                    activity.targetId?.let { onChallengeClick(it) }
                                }
                                ActivityType.FOLLOW -> {
                                    activity.targetUserId?.let { onUserClick(it) }
                                }
                                else -> { /* No action */ }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityItemRow(
    activity: ActivityItem,
    onUserClick: (String) -> Unit,
    onItemClick: () -> Unit
) {
    val (icon, iconColor, description) = getActivityDisplay(activity)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Activity icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // User name + action description
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(activity.userName ?: "Someone")
                        }
                        append(" ")
                        append(description)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                // Time ago
                activity.createdAt?.let { timestamp ->
                    Spacer(modifier = Modifier.height(4.dp))
                    TimeAgo(timestamp = timestamp)
                }

                // Extra info from metadata
                activity.metadata?.let { metadata ->
                    getMetadataDisplay(activity.activityType, metadata)?.let { extraInfo ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = extraInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // User avatar (clickable)
            UserAvatar(
                avatarUrl = activity.userAvatar,
                displayName = activity.userName,
                size = 36,
                onClick = { onUserClick(activity.userId) }
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun EmptyActivityState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Activity Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Follow other members to see their activity here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ACTIVITY DISPLAY HELPERS
// ═══════════════════════════════════════════════════════════════════════════

private data class ActivityDisplay(
    val icon: ImageVector,
    val iconColor: Color,
    val description: String
)

@Composable
private fun getActivityDisplay(activity: ActivityItem): ActivityDisplay {
    return when (activity.activityType) {
        ActivityType.WORKOUT_SHARED -> ActivityDisplay(
            icon = Icons.Default.FitnessCenter,
            iconColor = MaterialTheme.colorScheme.primary,
            description = "shared a workout"
        )
        ActivityType.PR_ACHIEVED -> ActivityDisplay(
            icon = Icons.Default.EmojiEvents,
            iconColor = Color(0xFFFFD700),
            description = "set a new personal record!"
        )
        ActivityType.CHALLENGE_JOINED -> ActivityDisplay(
            icon = Icons.Default.Flag,
            iconColor = MaterialTheme.colorScheme.tertiary,
            description = "joined a challenge"
        )
        ActivityType.CHALLENGE_WON -> ActivityDisplay(
            icon = Icons.Default.MilitaryTech,
            iconColor = Color(0xFFFFD700),
            description = "won a challenge!"
        )
        ActivityType.BADGE_EARNED -> ActivityDisplay(
            icon = Icons.Default.WorkspacePremium,
            iconColor = Color(0xFFE040FB),
            description = "earned a new badge"
        )
        ActivityType.LEVEL_UP -> ActivityDisplay(
            icon = Icons.Default.TrendingUp,
            iconColor = Color(0xFF00E676),
            description = "leveled up!"
        )
        ActivityType.FOLLOW -> ActivityDisplay(
            icon = Icons.Default.PersonAdd,
            iconColor = MaterialTheme.colorScheme.secondary,
            description = "started following someone"
        )
        ActivityType.LIKE -> ActivityDisplay(
            icon = Icons.Default.Favorite,
            iconColor = Color.Red,
            description = "liked a workout"
        )
        ActivityType.COMMENT -> ActivityDisplay(
            icon = Icons.Default.Comment,
            iconColor = MaterialTheme.colorScheme.primary,
            description = "commented on a workout"
        )
        ActivityType.STREAK_MILESTONE -> ActivityDisplay(
            icon = Icons.Default.LocalFireDepartment,
            iconColor = Color(0xFFFF5722),
            description = "reached a streak milestone!"
        )
    }
}

private fun getMetadataDisplay(type: ActivityType, metadata: Map<String, String>?): String? {
    if (metadata == null) return null

    return when (type) {
        ActivityType.BADGE_EARNED -> metadata["badge_name"]?.let { "Badge: $it" }
        ActivityType.LEVEL_UP -> metadata["new_level"]?.let { "Now Level $it" }
        ActivityType.STREAK_MILESTONE -> metadata["days"]?.let { "$it day streak!" }
        ActivityType.PR_ACHIEVED -> metadata["exercise"]?.let { it }
        else -> null
    }
}