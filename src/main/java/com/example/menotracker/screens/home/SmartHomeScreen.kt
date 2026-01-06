package com.example.menotracker.screens.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menotracker.community.components.PostMediaCarousel
import com.example.menotracker.community.data.models.FeedPost
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.community.viewmodels.ChallengeViewModel
import com.example.menotracker.community.viewmodels.FeedViewModel
import com.example.menotracker.community.viewmodels.GamificationViewModel
import com.example.menotracker.community.viewmodels.MemberDiscoveryViewModel
import com.example.menotracker.community.components.SuggestedMembersSection
import com.example.menotracker.data.models.MealType
import com.example.menotracker.ui.theme.*
import com.example.menotracker.viewmodels.AccountViewModel
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Smart Home Screen with:
 * - Hero Card (context-aware: Workout or Nutrition)
 * - Secondary Cards (status or promo)
 * - Dual Streaks in header
 * - Community Feed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartHomeScreen(
    userName: String = "Champion",
    accountViewModel: AccountViewModel = viewModel(),
    smartHomeViewModel: SmartHomeViewModel = viewModel(),
    onStartWorkout: () -> Unit = {},
    onNavigateToNutrition: (MealType?) -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToMaxOutFriday: () -> Unit = {},
    onNavigateToPost: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToMemberDiscovery: () -> Unit = {}
) {
    val userProfile by accountViewModel.userProfile.collectAsState()
    val currentUserId = userProfile?.id ?: ""
    val context = LocalContext.current

    // Smart Home State
    val smartHomeState by smartHomeViewModel.state.collectAsState()

    // Community ViewModels
    val feedViewModel: FeedViewModel = viewModel()
    val challengeViewModel: ChallengeViewModel = viewModel()
    val gamificationViewModel: GamificationViewModel = viewModel()
    val memberDiscoveryViewModel: MemberDiscoveryViewModel = viewModel()

    // Community State
    val feedState by feedViewModel.feedState.collectAsState()
    val memberDiscoveryState by memberDiscoveryViewModel.state.collectAsState()
    val maxOutFridayState by challengeViewModel.maxOutFridayState.collectAsState()

    // Max Out Friday Popup State
    var showMaxOutFridayPopup by remember { mutableStateOf(false) }

    // Refresh hero card when screen becomes visible
    LaunchedEffect(Unit) {
        smartHomeViewModel.refreshHeroCard()
    }

    // Check Max Out Friday popup
    LaunchedEffect(Unit) {
        if (CommunityFeatureFlag.MAX_OUT_FRIDAY_ENABLED) {
            val today = LocalDate.now()
            val isFriday = today.dayOfWeek == DayOfWeek.FRIDAY
            val prefs = context.getSharedPreferences("naya_prefs", Context.MODE_PRIVATE)
            val lastShown = prefs.getString("max_out_friday_last_shown", null)
            val isDisabled = prefs.getBoolean("max_out_friday_disabled", false)

            if (isFriday && !isDisabled && lastShown != today.toString()) {
                showMaxOutFridayPopup = true
                prefs.edit().putString("max_out_friday_last_shown", today.toString()).apply()
            }
        }
    }

    // Initialize Community ViewModels
    LaunchedEffect(currentUserId, CommunityFeatureFlag.ENABLED) {
        if (CommunityFeatureFlag.ENABLED) {
            // Initialize feed even without userId (for global feed)
            feedViewModel.initialize(currentUserId)
            if (currentUserId.isNotEmpty()) {
                challengeViewModel.initialize(currentUserId)
                gamificationViewModel.initialize(currentUserId)
                memberDiscoveryViewModel.initialize(currentUserId)
            }
        }
    }

    val isRefreshing = feedState.isLoading

    // Max Out Friday Popup Dialog
    if (showMaxOutFridayPopup && maxOutFridayState.info != null) {
        MaxOutFridayDialog(
            exerciseName = maxOutFridayState.info?.exerciseName ?: "Exercise",
            participantsCount = maxOutFridayState.info?.participantsCount ?: 0,
            onJoin = {
                showMaxOutFridayPopup = false
                onNavigateToMaxOutFriday()
            },
            onDismiss = { showMaxOutFridayPopup = false },
            onDisable = {
                showMaxOutFridayPopup = false
                context.getSharedPreferences("naya_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("max_out_friday_disabled", true)
                    .apply()
            }
        )
    }

    AppBackground {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                feedViewModel.loadFeed(refresh = true)
                challengeViewModel.refresh()
                smartHomeViewModel.refreshHeroCard()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 48.dp, bottom = 100.dp)
            ) {
                // ═══════════════════════════════════════════════════════════════
                // SMART HEADER WITH DUAL STREAKS
                // ═══════════════════════════════════════════════════════════════
                item {
                    SmartHomeHeader(
                        userName = userProfile?.name ?: userName,
                        profileImageUrl = userProfile?.profileImageUrl,
                        userTier = smartHomeState.userTier,
                        streaks = smartHomeState.streaks,
                        onAvatarClick = onNavigateToAccount
                    )
                }

                // ═══════════════════════════════════════════════════════════════
                // DUAL HERO CARDS (Nutrition + Workout stacked)
                // Both cards compact horizontal layout
                // ═══════════════════════════════════════════════════════════════
                item {
                    // Get the current nutrition hero type based on time
                    val nutritionHeroType = getCurrentNutritionHeroType()

                    DualHeroCards(
                        nutritionHeroType = nutritionHeroType,
                        workoutName = null, // TODO: Get next planned workout name
                        onNutritionClick = {
                            // Navigate with the correct meal type based on time
                            when (nutritionHeroType) {
                                HeroCardType.LOG_BREAKFAST -> onNavigateToNutrition(MealType.BREAKFAST)
                                HeroCardType.LOG_LUNCH -> onNavigateToNutrition(MealType.LUNCH)
                                HeroCardType.LOG_DINNER -> onNavigateToNutrition(MealType.DINNER)
                                else -> onNavigateToNutrition(null)
                            }
                        },
                        onWorkoutClick = onStartWorkout
                    )
                }

                // ═══════════════════════════════════════════════════════════════
                // SOFT PROMPT (Quick Question Card - after 7 days if pattern unclear)
                // ═══════════════════════════════════════════════════════════════
                if (smartHomeState.showSoftPrompt) {
                    item {
                        QuickQuestionCard(
                            onTimeSlotSelected = { timeSlot ->
                                smartHomeViewModel.onWorkoutTimeSelected(timeSlot)
                            },
                            onSkip = {
                                smartHomeViewModel.onSoftPromptDismissed()
                            }
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // SUGGESTED MEMBERS SECTION
                // ═══════════════════════════════════════════════════════════════
                if (CommunityFeatureFlag.ENABLED && memberDiscoveryState.suggestedMembers.isNotEmpty()) {
                    item {
                        SuggestedMembersSection(
                            members = memberDiscoveryState.suggestedMembers,
                            isLoading = memberDiscoveryState.isLoading,
                            onMemberClick = { userId -> onNavigateToUserProfile(userId) },
                            onFollowClick = { userId -> memberDiscoveryViewModel.toggleFollow(userId) },
                            onSeeAllClick = onNavigateToMemberDiscovery,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // COMMUNITY FEED HEADER
                // ═══════════════════════════════════════════════════════════════
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMMUNITY",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )

                            // Create Post Button
                            IconButton(
                                onClick = onNavigateToCreatePost,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddBox,
                                    contentDescription = "Create Post",
                                    tint = NayaOrangeGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Member Finder Button
                            IconButton(
                                onClick = onNavigateToMemberDiscovery,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Find Members",
                                    tint = NayaOrangeGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        TextButton(onClick = onNavigateToCommunity) {
                            Text(
                                text = "See All",
                                color = NayaOrangeGlow,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // FEED POSTS
                // ═══════════════════════════════════════════════════════════════
                if (feedState.posts.isEmpty() && !feedState.isLoading) {
                    item {
                        SmartEmptyFeedCard(
                            onFindMembers = onNavigateToMemberDiscovery,
                            onStartWorkout = onStartWorkout
                        )
                    }
                } else {
                    items(
                        items = feedState.posts.take(5),
                        key = { it.id }
                    ) { post ->
                        SmartFeedPostCard(
                            post = post,
                            onPostClick = { onNavigateToPost(post.id) },
                            onUserClick = { onNavigateToUserProfile(post.userId) },
                            onLikeClick = { feedViewModel.toggleLike(post.id) }
                        )
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // INVITE FRIENDS CARD
                // ═══════════════════════════════════════════════════════════════
                item {
                    SmartInviteFriendCard()
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FEED POST CARD (Reused from HomeScreen)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SmartFeedPostCard(
    post: FeedPost,
    onPostClick: () -> Unit,
    onUserClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPostClick() }
            .glassBackground(cornerRadius = 16.dp, alpha = 0.35f),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onUserClick() }
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NayaOrangeGlow, NayaPrimary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (post.userAvatar != null) {
                            AsyncImage(
                                model = post.userAvatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (post.userName ?: "U").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = post.userName ?: "User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        post.createdAt?.let { timestamp ->
                            Text(
                                text = formatTimeAgo(timestamp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // PR Badge
                if (post.prsAchieved > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NayaTertiary.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = NayaTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${post.prsAchieved} PR",
                                color = NayaTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Workout Info
            Text(
                text = post.workoutName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Stats Row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmartWorkoutStat(Icons.Default.FitnessCenter, "${post.totalSets} sets")
                SmartWorkoutStat(Icons.Default.Repeat, "${post.totalReps} reps")
                if (post.totalVolumeKg > 0) {
                    SmartWorkoutStat(Icons.Default.Scale, "${post.totalVolumeKg.toInt()} kg")
                }
                post.durationMinutes?.let { duration ->
                    SmartWorkoutStat(Icons.Default.Timer, "${duration}min")
                }
            }

            // Media
            if (post.hasMedia) {
                PostMediaCarousel(
                    imageUrls = post.imageUrls ?: emptyList(),
                    videoUrls = post.videoUrls ?: emptyList(),
                    onDoubleTap = onLikeClick
                )
            }

            // Caption
            post.caption?.let { caption ->
                if (caption.isNotBlank()) {
                    Text(
                        text = caption,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Interaction Row
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onLikeClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (post.isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (post.likesCount > 0) "${post.likesCount}" else "Like",
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = onPostClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comment",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (post.commentsCount > 0) "${post.commentsCount}" else "Comment",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartWorkoutStat(icon: ImageVector, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EMPTY FEED CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SmartEmptyFeedCard(
    onFindMembers: () -> Unit,
    onStartWorkout: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .glassBackground(cornerRadius = 16.dp, alpha = 0.35f),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = NayaOrangeGlow.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "Your feed is empty",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = SpaceGrotesk,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Follow members or complete workouts to see activity here",
                fontSize = 14.sp,
                fontFamily = Poppins,
                color = NayaTextSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onFindMembers,
                    border = BorderStroke(1.dp, NayaOrangeGlow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Find Members", color = NayaOrangeGlow, fontFamily = Poppins)
                }

                Button(
                    onClick = onStartWorkout,
                    colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Workout", color = Color.White, fontFamily = Poppins)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// INVITE FRIEND CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SmartInviteFriendCard() {
    val context = LocalContext.current
    val whatsAppGreen = Color(0xFF25D366)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .glassBackground(cornerRadius = 24.dp, alpha = 0.35f),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = whatsAppGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "SPREAD THE FIRE",
                    color = NayaTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    fontFamily = Poppins
                )
            }

            Text(
                text = "Training is better with friends. Share Naya and build your crew!",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontFamily = Poppins
            )

            Button(
                onClick = { sendInvite(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = whatsAppGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "INVITE A FRIEND",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                    fontFamily = Poppins
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAX OUT FRIDAY DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MaxOutFridayDialog(
    exerciseName: String,
    participantsCount: Int,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
    onDisable: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NayaVioletDark, NayaPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = "MAX OUT FRIDAY",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = NayaPrimary
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("This week's challenge:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(exerciseName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("$participantsCount members already competing", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Join Challenge", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onDisable) {
                    Text("Don't show again", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Get the current nutrition hero type based on time of day
 */
private fun getCurrentNutritionHeroType(): HeroCardType {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 6..10 -> HeroCardType.LOG_BREAKFAST
        in 11..14 -> HeroCardType.LOG_LUNCH
        in 17..21 -> HeroCardType.LOG_DINNER
        else -> HeroCardType.QUICK_LOG
    }
}

private fun formatTimeAgo(timestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(timestamp)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)

        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> "${duration.toHours()}h ago"
            duration.toDays() < 7 -> "${duration.toDays()}d ago"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    .withZone(java.time.ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        ""
    }
}

private fun sendInvite(context: Context) {
    val playStoreLink = "https://play.google.com/store/apps/details?id=com.example.menotracker"
    val message = """
        Hey! I'm using Naya for my training - it's awesome!

        Want to train together? Download it here:
        $playStoreLink
    """.trimIndent()

    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}