package com.example.menotracker.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.menotracker.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// MEMBER RESULT CARD
// Full-width card for search results list
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun MemberResultCard(
    name: String,
    avatarUrl: String?,
    primarySport: String?,
    experienceLevel: String?,
    followersCount: Long,
    isFollowing: Boolean,
    suggestionReason: String? = null,
    commonSportsCount: Int = 0,
    onClick: () -> Unit,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .glassPremium(cornerRadius = 16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NayaOrangeGlow, NayaPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        fontFamily = SpaceGrotesk
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Name
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = SpaceGrotesk,
                    color = NayaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Activity & Experience Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    primarySport?.let { sport ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = NayaOrangeGlow,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = sport,
                                fontSize = 12.sp,
                                fontFamily = Poppins,
                                color = NayaOrangeGlow,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    experienceLevel?.let { level ->
                        Text(
                            text = "• $level",
                            fontSize = 12.sp,
                            fontFamily = Poppins,
                            color = NayaTextSecondary
                        )
                    }
                }

                // Followers & Common interests
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NayaTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatFollowerCount(followersCount),
                            fontSize = 12.sp,
                            fontFamily = Poppins,
                            color = NayaTextSecondary
                        )
                    }

                    if (commonSportsCount > 0) {
                        Text(
                            text = "$commonSportsCount common interest${if (commonSportsCount > 1) "s" else ""}",
                            fontSize = 12.sp,
                            fontFamily = Poppins,
                            color = NayaOrangeGlow.copy(alpha = 0.8f)
                        )
                    }
                }

                // Suggestion reason
                suggestionReason?.let { reason ->
                    Text(
                        text = reason,
                        fontSize = 11.sp,
                        fontFamily = Poppins,
                        color = NayaTextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Follow button
            Button(
                onClick = onFollowClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing)
                        NayaGlass
                    else
                        NayaPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontSize = 13.sp,
                    fontFamily = Poppins,
                    color = Color.White
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

private fun formatFollowerCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        count == 1L -> "1 follower"
        else -> "$count followers"
    }
}
