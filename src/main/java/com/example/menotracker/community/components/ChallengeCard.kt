package com.example.menotracker.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.menotracker.community.data.models.Challenge
import com.example.menotracker.community.data.models.ChallengeStatus
import com.example.menotracker.community.data.models.ChallengeType
import com.example.menotracker.community.data.models.MaxOutFridayInfo

/**
 * Challenge preview card for list view.
 */
@Composable
fun ChallengeCard(
    challenge: Challenge,
    userParticipating: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Type icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChallengeTypeIcon(type = challenge.challengeType)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = getChallengeTypeLabel(challenge.challengeType),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (userParticipating) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Participating",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            challenge.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Participants
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${challenge.participantsCount} participants",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Time remaining
                ChallengeTimeLabel(
                    startDate = challenge.startDate,
                    endDate = challenge.endDate,
                    status = challenge.status
                )
            }
        }
    }
}

/**
 * Max Out Friday featured card.
 * Larger, more prominent display for the weekly challenge.
 */
@Composable
fun MaxOutFridayCard(
    info: MaxOutFridayInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAX OUT FRIDAY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exercise name
                Text(
                    text = info.exerciseName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Participants
                Text(
                    text = "${info.participantsCount} members competing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // User's entry if exists
                if (info.userEntryKg != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your best: ${info.userEntryKg.toInt()} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (info.userRank != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Rank #${info.userRank}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Challenge type icon
 */
@Composable
fun ChallengeTypeIcon(
    type: ChallengeType,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (type) {
        ChallengeType.MAX_OUT_FRIDAY -> Icons.Default.LocalFireDepartment to Color(0xFFFF5722)
        ChallengeType.VOLUME -> Icons.Default.FitnessCenter to Color(0xFF2196F3)
        ChallengeType.STREAK -> Icons.Default.Whatshot to Color(0xFFFF9800)
        ChallengeType.CUSTOM -> Icons.Default.Star to MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Challenge time/status label
 */
@Composable
fun ChallengeTimeLabel(
    startDate: String,
    endDate: String,
    status: ChallengeStatus
) {
    val text = when (status) {
        ChallengeStatus.UPCOMING -> "Starts ${formatDateShort(startDate)}"
        ChallengeStatus.ACTIVE -> "Ends ${formatDateShort(endDate)}"
        ChallengeStatus.COMPLETED -> "Ended"
        ChallengeStatus.CANCELLED -> "Cancelled"
    }

    val color = when (status) {
        ChallengeStatus.UPCOMING -> MaterialTheme.colorScheme.tertiary
        ChallengeStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        ChallengeStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
        ChallengeStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color
    )
}

/**
 * Hero card for Max Out Friday in challenge list - always visible even without active challenge.
 * Shows countdown timer and promotes participation.
 */
@Composable
fun MaxOutFridayHeroCard(
    maxOutFridayInfo: MaxOutFridayInfo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = maxOutFridayInfo != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isActive) {
                            listOf(
                                Color(0xFFFF5722),
                                Color(0xFFFF9800)
                            )
                        } else {
                            listOf(
                                Color(0xFF424242),
                                Color(0xFF616161)
                            )
                        }
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                // Header with fire icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MAX OUT FRIDAY",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (isActive) "Weekly Challenge Active" else "Coming Soon",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Arrow icon
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View details",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isActive && maxOutFridayInfo != null) {
                    // Exercise name
                    Text(
                        text = maxOutFridayInfo.exerciseName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Participants
                        Column {
                            Text(
                                text = "${maxOutFridayInfo.participantsCount}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Members",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // User rank if participating
                        if (maxOutFridayInfo.userRank != null) {
                            Column {
                                Text(
                                    text = "#${maxOutFridayInfo.userRank}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Your Rank",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // User's best if entered
                        if (maxOutFridayInfo.userEntryKg != null) {
                            Column {
                                Text(
                                    text = "${maxOutFridayInfo.userEntryKg.toInt()} kg",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Your Best",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Join button if not participating
                    if (maxOutFridayInfo.userEntryKg == null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFFF5722)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Join Challenge",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // No active challenge - show teaser
                    Text(
                        text = "Every Friday, compete for the heaviest lift",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap to see details and previous winners",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════

private fun getChallengeTypeLabel(type: ChallengeType): String {
    return when (type) {
        ChallengeType.MAX_OUT_FRIDAY -> "Weekly Challenge"
        ChallengeType.VOLUME -> "Volume Challenge"
        ChallengeType.STREAK -> "Streak Challenge"
        ChallengeType.CUSTOM -> "Challenge"
    }
}

private fun formatDateShort(isoDate: String): String {
    return try {
        val date = java.time.LocalDate.parse(isoDate.substring(0, 10))
        val now = java.time.LocalDate.now()
        val days = java.time.temporal.ChronoUnit.DAYS.between(now, date)

        when {
            days == 0L -> "today"
            days == 1L -> "tomorrow"
            days == -1L -> "yesterday"
            days in 2..6 -> "in $days days"
            days in -6..-2 -> "${-days} days ago"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                date.format(formatter)
            }
        }
    } catch (e: Exception) {
        ""
    }
}
