package com.example.menotracker.screens.mindfulness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.AppBackground

// Colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val tealAccent = Color(0xFF14B8A6)
private val pinkAccent = Color(0xFFEC4899)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)

/**
 * Mindfulness Hub Screen
 * Combines Breathing, Meditation, and Soundscape into one entry point
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToBreathing: () -> Unit,
    onNavigateToBreathingSession: (BreathingExerciseType) -> Unit,
    onNavigateToMeditation: () -> Unit,
    onNavigateToMeditationSession: (MeditationType) -> Unit,
    onNavigateToSoundscape: () -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    var showUpgradeDialog by remember { mutableStateOf(false) }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = lavenderLight,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "MINDFULNESS",
                                    color = textWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Breathe, meditate, relax",
                                    color = lavenderLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Quick Start Section
                item {
                    Text(
                        text = "Quick Start",
                        color = textWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Featured free exercises
                item {
                    QuickStartRow(
                        onBreathingClick = { onNavigateToBreathingSession(BreathingExerciseType.RELAXATION_478) },
                        onMeditationClick = { onNavigateToMeditationSession(MeditationType.BODY_SCAN) }
                    )
                }

                // Main categories
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Explore",
                        color = textWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Breathing card
                item {
                    CategoryCard(
                        title = "Breathing",
                        subtitle = "Calm your nervous system",
                        icon = Icons.Default.Air,
                        accentColor = lavenderLight,
                        gradientColors = listOf(lavenderPrimary, lavenderLight),
                        exerciseCount = "${BreathingExerciseType.entries.size} exercises",
                        onClick = onNavigateToBreathing
                    )
                }

                // Meditation card
                item {
                    CategoryCard(
                        title = "Meditation",
                        subtitle = "Find your inner peace",
                        icon = Icons.Default.SelfImprovement,
                        accentColor = tealAccent,
                        gradientColors = listOf(tealAccent, Color(0xFF0D9488)),
                        exerciseCount = "${MeditationType.entries.size} meditations",
                        onClick = onNavigateToMeditation
                    )
                }

                // Soundscape card (Premium feature)
                item {
                    SoundscapeCard(
                        isLocked = !SubscriptionManager.canUseSoundscapeMixer(),
                        onClick = {
                            if (SubscriptionManager.canUseSoundscapeMixer()) {
                                onNavigateToSoundscape()
                            } else {
                                showUpgradeDialog = true
                            }
                        }
                    )
                }

                // Today's recommendation
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recommended for You",
                        color = textWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    RecommendationCard(
                        onBreathingClick = { onNavigateToBreathingSession(BreathingExerciseType.RELAXATION_478) },
                        onMeditationClick = { onNavigateToMeditationSession(MeditationType.GRATITUDE) }
                    )
                }

                // Free vs Premium comparison
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TierComparisonCard(
                        hasFullAccess = SubscriptionManager.hasPremiumAccess(),
                        onUpgradeClick = onNavigateToPaywall
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Upgrade dialog
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = {
                Text("Premium Feature", color = textWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "The Soundscape Mixer is a Premium feature. Upgrade to mix multiple sounds and access the full library.",
                    color = textGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpgradeDialog = false
                        onNavigateToPaywall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lavenderPrimary)
                ) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Maybe Later", color = textGray)
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
private fun QuickStartRow(
    onBreathingClick: () -> Unit,
    onMeditationClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick breathing
        QuickStartCard(
            emoji = "\uD83C\uDF43",
            title = "4-7-8 Breathing",
            subtitle = "4 min",
            color = lavenderLight,
            modifier = Modifier.weight(1f),
            onClick = onBreathingClick
        )

        // Quick meditation
        QuickStartCard(
            emoji = "\uD83E\uDDD8",
            title = "Body Scan",
            subtitle = "10 min",
            color = tealAccent,
            modifier = Modifier.weight(1f),
            onClick = onMeditationClick
        )
    }
}

@Composable
private fun QuickStartCard(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = textWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = textGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    gradientColors: List<Color>,
    exerciseCount: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.7f) }
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = textWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = textWhite.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exerciseCount,
                        color = textWhite.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = textWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SoundscapeCard(
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(pinkAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = pinkAccent,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Soundscape Mixer",
                        color = if (isLocked) textGray else textWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = lavenderPrimary.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "PREMIUM",
                                color = lavenderLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Mix multiple sounds together",
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isLocked) textGray else textWhite,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    onBreathingClick: () -> Unit,
    onMeditationClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Start your mindfulness journey",
                color = textGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Breathing recommendation
                RecommendationItem(
                    emoji = "\uD83C\uDF43",
                    title = "Evening wind-down",
                    description = "4-7-8 breathing helps calm the nervous system before sleep",
                    buttonText = "Try Now",
                    buttonColor = lavenderLight,
                    modifier = Modifier.weight(1f),
                    onClick = onBreathingClick
                )
            }
        }
    }
}

@Composable
private fun RecommendationItem(
    emoji: String,
    title: String,
    description: String,
    buttonText: String,
    buttonColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(text = emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = textWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            color = textGray,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = buttonText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TierComparisonCard(
    hasFullAccess: Boolean,
    onUpgradeClick: () -> Unit
) {
    if (hasFullAccess) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = lavenderLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock Full Access",
                    color = textWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Free tier
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Free",
                        color = textGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TierFeature(text = "1 breathing exercise")
                    TierFeature(text = "2 meditations")
                    TierFeature(text = "3 ambient sounds")
                }

                // Premium tier
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Premium",
                        color = lavenderLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TierFeature(text = "5 breathing exercises", isPremium = true)
                    TierFeature(text = "8 meditations", isPremium = true)
                    TierFeature(text = "Soundscape mixer", isPremium = true)
                    TierFeature(text = "All sounds & music", isPremium = true)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = lavenderPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Upgrade to Premium")
            }
        }
    }
}

@Composable
private fun TierFeature(
    text: String,
    isPremium: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (isPremium) lavenderLight else textGray,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = if (isPremium) textWhite else textGray,
            fontSize = 11.sp
        )
    }
}
