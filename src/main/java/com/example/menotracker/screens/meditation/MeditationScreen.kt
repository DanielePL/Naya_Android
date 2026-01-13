package com.example.menotracker.screens.meditation

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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.*
import com.example.menotracker.viewmodels.MeditationUiState
import com.example.menotracker.viewmodels.MeditationViewModel

// ═══════════════════════════════════════════════════════════════
// NAYA MEDITATION - Design System
// ═══════════════════════════════════════════════════════════════

// Meditation-specific accent (Teal - Ruhe & Balance)
private val meditationPrimary = NayaSecondary              // #14B8A6
private val meditationLight = Color(0xFF2DD4BF)            // Lighter teal
private val meditationDark = Color(0xFF0D9488)             // Darker teal

// Text & Surface (from NAYA theme)
private val textPrimary = NayaTextWhite
private val textSecondary = NayaTextSecondary
private val textTertiary = NayaTextTertiary
private val cardSurface = NayaSurface
private val glassSurface = NayaGlass

/**
 * Main Meditation Screen
 * Shows list of available meditations with tier gating
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSession: (MeditationType) -> Unit,
    onNavigateToSoundscape: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: MeditationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showUpgradePrompt by viewModel.showUpgradePrompt.collectAsState()

    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    // Upgrade prompt dialog
    if (showUpgradePrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpgradePrompt() },
            title = {
                Text(
                    "Premium Meditation",
                    color = textPrimary,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Diese Meditation erfordert ein Premium-Abo. Upgrade um alle Meditationen und den Soundscape-Mixer freizuschalten.",
                    color = textSecondary,
                    fontFamily = Poppins
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUpgradePrompt()
                        onNavigateToPaywall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
                ) {
                    Text("Upgraden", fontFamily = Poppins, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpgradePrompt() }) {
                    Text("Vielleicht später", color = textSecondary, fontFamily = Poppins)
                }
            },
            containerColor = cardSurface
        )
    }

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
                            // Animated icon container
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(meditationPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelfImprovement,
                                    contentDescription = null,
                                    tint = meditationPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "MEDITATION",
                                    color = textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Finde deine innere Ruhe",
                                    color = meditationPrimary,
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
                                contentDescription = "Zurück",
                                tint = textPrimary
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
                // Stats Card
                uiState.totalStats?.let { stats ->
                    item {
                        MeditationStatsCard(stats = stats)
                    }
                }

                // Soundscape Quick Access
                item {
                    SoundscapeCard(
                        isLocked = !uiState.hasFullAccess,
                        onClick = {
                            if (uiState.hasFullAccess) {
                                onNavigateToSoundscape()
                            } else {
                                onNavigateToPaywall()
                            }
                        }
                    )
                }

                // Categories
                item {
                    Text(
                        text = "Geführte Meditationen",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                }

                // Category filter chips
                item {
                    CategoryChips()
                }

                // Featured free meditations
                val freeMeditations = MeditationType.getFreeMeditations()
                if (freeMeditations.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(meditationPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kostenlos",
                                color = meditationPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }

                    freeMeditations.forEach { meditation ->
                        item {
                            MeditationCard(
                                meditation = meditation,
                                isLocked = false,
                                isFeatured = true,
                                onClick = { onNavigateToSession(meditation) }
                            )
                        }
                    }
                }

                // Premium meditations by category
                val premiumMeditations = MeditationType.getPremiumMeditations()
                if (premiumMeditations.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NayaPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Premium",
                                color = NayaPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NayaPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    premiumMeditations.forEach { meditation ->
                        item {
                            MeditationCard(
                                meditation = meditation,
                                isLocked = !uiState.hasFullAccess,
                                isFeatured = false,
                                onClick = {
                                    if (uiState.hasFullAccess) {
                                        onNavigateToSession(meditation)
                                    } else {
                                        viewModel.selectMeditation(meditation)
                                    }
                                }
                            )
                        }
                    }
                }

                // Recent sessions
                if (uiState.recentSessions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Letzte Sessions",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                    }

                    items(uiState.recentSessions) { session ->
                        RecentMeditationCard(session = session)
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MeditationStatsCard(stats: MeditationStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = glassSurface,
        border = BorderStroke(1.dp, meditationPrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = meditationPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Deine Reise",
                    color = meditationPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpaceGrotesk
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(value = "${stats.totalSessions}", label = "Sessions")
                StatItem(value = "${stats.totalMinutes}", label = "Minuten")
                StatItem(value = "${stats.currentStreak}", label = "Tage Streak")
            }

            stats.favoriteType?.let { favorite ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = textTertiary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(meditationPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = favorite.emoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Favorit",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontFamily = Poppins
                        )
                        Text(
                            text = favorite.displayName,
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Poppins
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk
        )
        Text(
            text = label,
            color = textSecondary,
            fontSize = 12.sp,
            fontFamily = Poppins
        )
    }
}

@Composable
private fun SoundscapeCard(
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
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
                            meditationPrimary.copy(alpha = 0.6f),
                            NayaPrimary.copy(alpha = 0.4f)
                        )
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
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Soundscape Mixer",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NayaPrimary.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Premium",
                                        tint = textPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "PRO",
                                        color = textPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceGrotesk
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mixe mehrere Sounds für deine perfekte Atmosphäre",
                        color = textPrimary.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontFamily = Poppins
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Öffnen",
                    tint = textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryChips() {
    val categories = MeditationCategory.entries
    var selectedCategory by remember { mutableStateOf<MeditationCategory?>(null) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = {
                    Text(
                        "Alle",
                        fontSize = 12.sp,
                        fontFamily = Poppins
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = meditationPrimary.copy(alpha = 0.3f),
                    selectedLabelColor = textPrimary,
                    labelColor = textSecondary
                )
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { selectedCategory = category },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category.emoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            category.displayName,
                            fontSize = 12.sp,
                            fontFamily = Poppins
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = meditationPrimary.copy(alpha = 0.3f),
                    selectedLabelColor = textPrimary,
                    labelColor = textSecondary
                )
            )
        }
    }
}

@Composable
private fun MeditationCard(
    meditation: MeditationType,
    isLocked: Boolean,
    isFeatured: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isLocked) glassSurface.copy(alpha = 0.5f) else glassSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isFeatured) meditationPrimary.copy(alpha = 0.3f)
            else if (isLocked) textTertiary.copy(alpha = 0.1f)
            else NayaPrimary.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon with accent background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFeatured) meditationPrimary.copy(alpha = 0.15f)
                        else NayaPrimary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = meditation.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meditation.displayName,
                        color = if (isLocked) textSecondary else textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SpaceGrotesk
                    )

                    if (isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Gesperrt",
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = meditation.description,
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontFamily = Poppins,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = textTertiary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${meditation.defaultDurationMinutes} min",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontFamily = Poppins,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Category
                    Text(
                        text = meditation.category.displayName,
                        color = if (isFeatured) meditationPrimary else NayaPrimary,
                        fontSize = 11.sp,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Play/Lock icon with background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLocked) textTertiary.copy(alpha = 0.1f)
                        else if (isFeatured) meditationPrimary.copy(alpha = 0.15f)
                        else NayaPrimary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = if (isLocked) "Gesperrt" else "Starten",
                    tint = if (isLocked) textSecondary else if (isFeatured) meditationPrimary else NayaPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentMeditationCard(session: MeditationSession) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = glassSurface,
        border = BorderStroke(1.dp, textTertiary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(meditationPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.meditationType.emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.meditationType.displayName,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = "${session.durationSeconds / 60} min",
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontFamily = Poppins
                )
            }

            // Mood improvement
            val moodBefore = session.moodBefore
            val moodAfter = session.moodAfter
            if (moodBefore != null && moodAfter != null) {
                val improvement = moodAfter - moodBefore
                if (improvement > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = meditationPrimary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = meditationPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "+$improvement",
                                color = meditationPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }
        }
    }
}
