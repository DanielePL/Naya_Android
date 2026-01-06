package com.example.menotracker.community.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.People
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
import com.example.menotracker.community.data.models.SuggestedMember
import com.example.menotracker.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// SUGGESTED MEMBERS SECTION
// Horizontal scrolling section for HomeScreen with member recommendations
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun SuggestedMembersSection(
    members: List<SuggestedMember>,
    isLoading: Boolean,
    onMemberClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't show if no members and not loading
    if (members.isEmpty() && !isLoading) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = NayaOrangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MEMBERS FOR YOU",
                    color = NayaTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = SpaceGrotesk,
                    letterSpacing = 1.sp
                )
            }

            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "Find More",
                    color = NayaOrangeGlow,
                    fontSize = 14.sp,
                    fontFamily = Poppins
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NayaOrangeGlow,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            // Loading shimmer
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(3) {
                    SuggestedMemberCardSkeleton()
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(
                    items = members,
                    key = { it.userId }
                ) { member ->
                    SuggestedMemberCard(
                        member = member,
                        onClick = { onMemberClick(member.userId) },
                        onFollowClick = { onFollowClick(member.userId) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SUGGESTED MEMBER CARD
// Individual member card for horizontal scroll
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SuggestedMemberCard(
    member: SuggestedMember,
    onClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
            .glassPremium(cornerRadius = 16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                if (member.profileImageUrl != null) {
                    AsyncImage(
                        model = member.profileImageUrl,
                        contentDescription = member.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = member.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        fontFamily = SpaceGrotesk
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = member.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = SpaceGrotesk,
                color = NayaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Activity badge
            member.primarySport?.let { sport ->
                Text(
                    text = sport,
                    fontSize = 11.sp,
                    fontFamily = Poppins,
                    color = NayaOrangeGlow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Suggestion reason
            member.suggestionReason?.let { reason ->
                Text(
                    text = reason,
                    fontSize = 10.sp,
                    fontFamily = Poppins,
                    color = NayaTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Follow button
            Button(
                onClick = onFollowClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (member.isFollowing)
                        NayaGlass
                    else
                        NayaPrimary
                ),
                contentPadding = PaddingValues(vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (member.isFollowing) "Following" else "Follow",
                    fontSize = 12.sp,
                    fontFamily = Poppins,
                    color = Color.White
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SKELETON/LOADING STATE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SuggestedMemberCardSkeleton() {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .glassPremium(cornerRadius = 16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar skeleton
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NayaGlass)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Name skeleton
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NayaGlass)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Activity skeleton
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NayaGlass)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Button skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NayaGlass)
            )
        }
    }
}
