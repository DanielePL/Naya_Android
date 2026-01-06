package com.example.menotracker.community.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.community.data.models.*

// ═══════════════════════════════════════════════════════════════════════════
// LEVEL & XP DISPLAY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Displays user level with XP progress bar.
 */
@Composable
fun LevelProgressCard(
    userLevel: UserLevel?,
    modifier: Modifier = Modifier
) {
    val level = userLevel?.level ?: 1
    val progress = userLevel?.xpProgress ?: 0f
    val title = UserLevel.getLevelTitle(level)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelBadge(level = level)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Level $level",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // XP count
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${userLevel?.totalXp ?: 0} XP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${userLevel?.xpForNextLevel ?: 100} to next",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun LevelBadge(
    level: Int,
    size: Int = 48,
    modifier: Modifier = Modifier
) {
    val (color1, color2) = when {
        level >= 50 -> Color(0xFFFFD700) to Color(0xFFFFA000)  // Gold
        level >= 25 -> Color(0xFFE040FB) to Color(0xFF7C4DFF)  // Purple
        level >= 10 -> Color(0xFF2196F3) to Color(0xFF00BCD4)  // Blue
        else -> Color(0xFF4CAF50) to Color(0xFF8BC34A)         // Green
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(listOf(color1, color2))
            )
            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$level",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontSize = (size / 2.5).sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// STREAK DISPLAY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Displays current streak with fire animation.
 */
@Composable
fun StreakCard(
    streak: UserStreak?,
    modifier: Modifier = Modifier
) {
    val currentStreak = streak?.currentStreak ?: 0
    val longestStreak = streak?.longestStreak ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentStreak >= 7)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fire icon with animation
            val infiniteTransition = rememberInfiniteTransition(label = "streak")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (currentStreak > 0) 1.15f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "streak_scale"
            )

            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size((36 * scale).dp),
                    tint = when {
                        currentStreak >= 30 -> Color(0xFFFF5722)
                        currentStreak >= 7 -> Color(0xFFFF9800)
                        currentStreak > 0 -> Color(0xFFFFC107)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$currentStreak Day Streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Best: $longestStreak days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Freeze tokens
            if ((streak?.freezeTokens ?: 0) > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${streak?.freezeTokens}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BADGES DISPLAY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Horizontal scrolling list of earned badges.
 */
@Composable
fun BadgesRow(
    badges: List<UserBadge>,
    onBadgeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Badges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${badges.size} earned",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (badges.isEmpty()) {
            Text(
                text = "Complete challenges to earn badges!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(badges.take(10)) { badge ->
                    BadgeItem(
                        badge = badge,
                        onClick = { onBadgeClick(badge.badgeId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeItem(
    badge: UserBadge,
    onClick: () -> Unit
) {
    val badgeColor = when (badge.badgeRarity) {
        BadgeRarity.LEGENDARY -> Color(0xFFFFD700)
        BadgeRarity.EPIC -> Color(0xFFE040FB)
        BadgeRarity.RARE -> Color(0xFF2196F3)
        BadgeRarity.COMMON -> Color(0xFF4CAF50)
        null -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = badgeColor.copy(alpha = 0.1f)
        ),
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f))
                    .border(2.dp, badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getBadgeIcon(badge.badgeIcon ?: "star"),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = badge.badgeName ?: "Badge",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getBadgeIcon(iconName: String): ImageVector {
    return when (iconName) {
        "fitness_center" -> Icons.Default.FitnessCenter
        "emoji_events" -> Icons.Default.EmojiEvents
        "military_tech" -> Icons.Default.MilitaryTech
        "workspace_premium" -> Icons.Default.WorkspacePremium
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "whatshot" -> Icons.Default.Whatshot
        "bolt" -> Icons.Default.Bolt
        "trending_up" -> Icons.Default.TrendingUp
        "share" -> Icons.Default.Share
        "people" -> Icons.Default.People
        "groups" -> Icons.Default.Groups
        else -> Icons.Default.Star
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// XP POPUP (For showing XP gains)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun XpGainPopup(
    amount: Int,
    source: String,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "xp")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xp_scale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onDismiss()
    }

    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size((32 * scale).dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "+$amount XP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPACT STATS ROW
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Compact row showing level, streak, and badges count.
 */
@Composable
fun GamificationStatsRow(
    level: Int,
    streak: Int,
    badgesCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CompactStat(
            icon = Icons.Default.TrendingUp,
            value = "Lv $level",
            label = "Level",
            color = MaterialTheme.colorScheme.primary
        )

        CompactStat(
            icon = Icons.Default.LocalFireDepartment,
            value = "$streak",
            label = "Streak",
            color = Color(0xFFFF5722)
        )

        CompactStat(
            icon = Icons.Default.EmojiEvents,
            value = "$badgesCount",
            label = "Badges",
            color = Color(0xFFFFD700)
        )
    }
}

@Composable
private fun CompactStat(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Note: RankBadge is defined in LeaderboardRow.kt to avoid duplication