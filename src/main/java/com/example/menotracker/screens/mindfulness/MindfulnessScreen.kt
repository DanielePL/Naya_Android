package com.example.menotracker.screens.mindfulness

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.*

// ═══════════════════════════════════════════════════════════════
// NAYA MINDFULNESS - Design System
// ═══════════════════════════════════════════════════════════════

// Mindfulness-specific accent (Teal - Ruhe & Balance)
private val mindfulnessPrimary = NayaSecondary           // #14B8A6
private val mindfulnessLight = Color(0xFF2DD4BF)         // Lighter teal
private val mindfulnessDark = Color(0xFF0D9488)          // Darker teal

// Category colors
private val breathingColor = NayaPrimary                 // Violet
private val meditationColor = mindfulnessPrimary         // Teal
private val soundscapeColor = NayaAccent                 // Pink

// Text & Surface (from NAYA theme)
private val textPrimary = NayaTextWhite
private val textSecondary = NayaTextSecondary
private val textTertiary = NayaTextTertiary
private val cardSurface = NayaSurface
private val glassSurface = NayaGlass

/**
 * NAYA Mindfulness Hub
 * Premium wellness experience with Breathing, Meditation & Soundscape
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
    val hasPremium = SubscriptionManager.hasPremiumAccess()

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MindfulnessTopBar(onNavigateBack = onNavigateBack)
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hero Section with animated breathing visual
                item {
                    HeroSection(
                        onQuickBreathing = { onNavigateToBreathingSession(BreathingExerciseType.RELAXATION_478) },
                        onQuickMeditation = { onNavigateToMeditationSession(MeditationType.BODY_SCAN) }
                    )
                }

                // Today's Focus - Quick Actions
                item {
                    SectionHeader(
                        title = "Quick Start",
                        subtitle = "Start a session directly",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    QuickStartGrid(
                        onBreathingClick = { onNavigateToBreathingSession(BreathingExerciseType.RELAXATION_478) },
                        onMeditationClick = { onNavigateToMeditationSession(MeditationType.BODY_SCAN) },
                        onSoundscapeClick = {
                            if (SubscriptionManager.canUseSoundscapeMixer()) {
                                onNavigateToSoundscape()
                            } else {
                                showUpgradeDialog = true
                            }
                        },
                        hasPremium = hasPremium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Main Categories
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Explore",
                        subtitle = "Choose your focus",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Breathing Category Card
                item {
                    CategoryFeatureCard(
                        title = "Breathing",
                        subtitle = "Calm your nervous system",
                        description = "Science-based techniques for relaxation and stress relief",
                        icon = Icons.Outlined.Air,
                        accentColor = breathingColor,
                        exerciseCount = BreathingExerciseType.entries.size,
                        freeCount = 1,
                        onClick = onNavigateToBreathing,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Meditation Category Card
                item {
                    CategoryFeatureCard(
                        title = "Meditation",
                        subtitle = "Find inner peace",
                        description = "Guided meditations for every moment",
                        icon = Icons.Outlined.SelfImprovement,
                        accentColor = meditationColor,
                        exerciseCount = MeditationType.entries.size,
                        freeCount = 2,
                        onClick = onNavigateToMeditation,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Soundscape Mixer Card
                item {
                    SoundscapeMixerCard(
                        isLocked = !SubscriptionManager.canUseSoundscapeMixer(),
                        onClick = {
                            if (SubscriptionManager.canUseSoundscapeMixer()) {
                                onNavigateToSoundscape()
                            } else {
                                showUpgradeDialog = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Free Sounds Preview
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Ambient Sounds",
                        subtitle = "Free",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    FreeSoundsRow(modifier = Modifier.padding(start = 16.dp))
                }

                // Premium Upsell (if not premium)
                if (!hasPremium) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        PremiumUpsellCard(
                            onUpgradeClick = onNavigateToPaywall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Upgrade Dialog
    if (showUpgradeDialog) {
        NayaUpgradeDialog(
            feature = "Soundscape Mixer",
            description = "Mix multiple sounds together and relax with your personal soundscape.",
            onUpgrade = {
                showUpgradeDialog = false
                onNavigateToPaywall()
            },
            onDismiss = { showUpgradeDialog = false }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MindfulnessTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated icon
                val infiniteTransition = rememberInfiniteTransition(label = "icon")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = mindfulnessLight,
                    modifier = Modifier
                        .size(28.dp)
                        .scale(scale)
                )

                Column {
                    Text(
                        text = "MINDFULNESS",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Breathe · Meditate · Relax",
                        color = mindfulnessLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = Poppins
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// ═══════════════════════════════════════════════════════════════
// HERO SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeroSection(
    onQuickBreathing: () -> Unit,
    onQuickMeditation: () -> Unit
) {
    // Animated breathing circle
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        mindfulnessPrimary.copy(alpha = 0.3f),
                        mindfulnessDark.copy(alpha = 0.1f)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Breathing visualization
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(breathScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                mindfulnessLight.copy(alpha = 0.6f),
                                mindfulnessPrimary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Air,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Take a moment",
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk
            )

            Text(
                text = "Breathe in deeply and let go",
                color = textSecondary,
                fontSize = 14.sp,
                fontFamily = Poppins
            )

            // Quick action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onQuickBreathing,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = breathingColor.copy(alpha = 0.3f),
                        contentColor = textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("4-7-8 Breathing", fontFamily = Poppins, fontSize = 13.sp)
                }

                FilledTonalButton(
                    onClick = onQuickMeditation,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = meditationColor.copy(alpha = 0.3f),
                        contentColor = textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SelfImprovement,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Body Scan", fontFamily = Poppins, fontSize = 13.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk
        )
        subtitle?.let {
            Text(
                text = it,
                color = textSecondary,
                fontSize = 13.sp,
                fontFamily = Poppins
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// QUICK START GRID
// ═══════════════════════════════════════════════════════════════

@Composable
private fun QuickStartGrid(
    onBreathingClick: () -> Unit,
    onMeditationClick: () -> Unit,
    onSoundscapeClick: () -> Unit,
    hasPremium: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStartTile(
            emoji = "🍃",
            title = "Breathe",
            duration = "4 min",
            color = breathingColor,
            onClick = onBreathingClick,
            modifier = Modifier.weight(1f)
        )

        QuickStartTile(
            emoji = "🧘",
            title = "Meditation",
            duration = "10 min",
            color = meditationColor,
            onClick = onMeditationClick,
            modifier = Modifier.weight(1f)
        )

        QuickStartTile(
            emoji = "🎧",
            title = "Sounds",
            duration = "∞",
            color = soundscapeColor,
            isLocked = !hasPremium,
            onClick = onSoundscapeClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStartTile(
    emoji: String,
    title: String,
    duration: String,
    color: Color,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = glassSurface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 32.sp,
                    color = if (isLocked) textTertiary else Color.Unspecified
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    color = if (isLocked) textTertiary else textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Poppins
                )
                Text(
                    text = duration,
                    color = if (isLocked) textTertiary else color,
                    fontSize = 12.sp,
                    fontFamily = Poppins
                )
            }

            // Lock badge
            if (isLocked) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = NayaPrimary.copy(alpha = 0.3f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Premium",
                        tint = NayaPrimary,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(12.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CATEGORY FEATURE CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CategoryFeatureCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    exerciseCount: Int,
    freeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.25f),
                            accentColor.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.3f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                    Text(
                        text = subtitle,
                        color = accentColor,
                        fontSize = 13.sp,
                        fontFamily = Poppins
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$exerciseCount exercises · $freeCount free",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontFamily = Poppins
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SOUNDSCAPE MIXER CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SoundscapeMixerCard(
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = glassSurface,
        border = BorderStroke(
            1.dp,
            if (isLocked) NayaBorder else soundscapeColor.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated mixer icon
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = soundscapeColor.copy(alpha = if (isLocked) 0.1f else 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = if (isLocked) textTertiary else soundscapeColor,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Soundscape Mixer",
                        color = if (isLocked) textSecondary else textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SpaceGrotesk
                    )
                    if (isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PremiumBadge()
                    }
                }
                Text(
                    text = "Mix up to 4 sounds together",
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontFamily = Poppins
                )
            }

            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isLocked) textTertiary else soundscapeColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PremiumBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = NayaPrimary.copy(alpha = 0.3f)
    ) {
        Text(
            text = "PREMIUM",
            color = NayaPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Poppins,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FREE SOUNDS ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FreeSoundsRow(modifier: Modifier = Modifier) {
    val freeSounds = AmbientSound.getFreeSounds()

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        items(freeSounds) { sound ->
            SoundChip(sound = sound)
        }
    }
}

@Composable
private fun SoundChip(sound: AmbientSound) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = glassSurface,
        border = BorderStroke(1.dp, NayaBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = sound.emoji, fontSize = 20.sp)
            Text(
                text = sound.displayName,
                color = textPrimary,
                fontSize = 13.sp,
                fontFamily = Poppins
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREMIUM UPSELL CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PremiumUpsellCard(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NayaPrimary.copy(alpha = 0.2f),
                            NayaAccent.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NayaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Unlock full access",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Benefits grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        BenefitItem(text = "5 breathing exercises")
                        BenefitItem(text = "8 meditations")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        BenefitItem(text = "Soundscape Mixer")
                        BenefitItem(text = "All sounds & music")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NayaPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get Premium",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = mindfulnessLight,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = textPrimary,
            fontSize = 13.sp,
            fontFamily = Poppins
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// UPGRADE DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun NayaUpgradeDialog(
    feature: String,
    description: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = NayaPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Premium Feature",
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk
            )
        },
        text = {
            Column {
                Text(
                    text = feature,
                    color = NayaPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Poppins
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    color = textSecondary,
                    fontFamily = Poppins,
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
            ) {
                Text("Upgrade", fontFamily = Poppins)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = textSecondary, fontFamily = Poppins)
            }
        },
        containerColor = cardSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
