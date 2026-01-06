package com.example.menotracker.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.menotracker.community.data.models.LeaderboardEntry

/**
 * Leaderboard row component.
 * Shows rank, user info, and their best lift.
 */
@Composable
fun LeaderboardRow(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rank = entry.rank ?: 0
    val backgroundColor = when {
        isCurrentUser -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        rank == 1 -> Color(0xFFFFD700).copy(alpha = 0.1f)  // Gold
        rank == 2 -> Color(0xFFC0C0C0).copy(alpha = 0.1f)  // Silver
        rank == 3 -> Color(0xFFCD7F32).copy(alpha = 0.1f)  // Bronze
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        RankBadge(
            rank = rank,
            modifier = Modifier.width(40.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Avatar
        UserAvatar(
            avatarUrl = entry.userAvatarUrl,
            displayName = entry.userDisplayName,
            size = 44
        )

        Spacer(modifier = Modifier.width(12.dp))

        // User name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.userDisplayName ?: "User",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.isPr) {
                Text(
                    text = "Personal Record",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Weight
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatWeight(entry.estimated1rmKg),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if ((entry.prReps ?: 1) > 1) {
                Text(
                    text = "${entry.prWeightKg?.toInt() ?: 0}kg x ${entry.prReps ?: 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Rank badge with medal icons for top 3
 */
@Composable
fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (rank) {
            1 -> {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "1st place",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
            }
            2 -> {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "2nd place",
                    tint = Color(0xFFC0C0C0),
                    modifier = Modifier.size(26.dp)
                )
            }
            3 -> {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "3rd place",
                    tint = Color(0xFFCD7F32),
                    modifier = Modifier.size(24.dp)
                )
            }
            else -> {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Current user's rank card (shown at bottom of leaderboard)
 */
@Composable
fun CurrentUserRankCard(
    entry: LeaderboardEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${entry.rank ?: "—"}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.exerciseName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatWeight(entry.estimated1rmKg),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Est. 1RM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Exercise selector chip for leaderboard
 */
@Composable
fun ExerciseChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = name,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════

private fun formatWeight(kg: Double?): String {
    val weight = kg ?: 0.0
    return if (weight >= 1000) {
        String.format("%.1f t", weight / 1000)
    } else {
        String.format("%.0f kg", weight)
    }
}
