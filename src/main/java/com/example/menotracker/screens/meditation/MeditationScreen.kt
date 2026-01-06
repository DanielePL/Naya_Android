package com.example.menotracker.screens.meditation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.viewmodels.MeditationUiState
import com.example.menotracker.viewmodels.MeditationViewModel

// Colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val tealAccent = Color(0xFF14B8A6)
private val pinkAccent = Color(0xFFEC4899)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)

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
            title = { Text("Premium Meditation", color = textWhite) },
            text = {
                Text(
                    "This meditation requires a Premium subscription. Upgrade to unlock all meditations and the Soundscape mixer.",
                    color = textGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUpgradePrompt()
                        onNavigateToPaywall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lavenderPrimary)
                ) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpgradePrompt() }) {
                    Text("Maybe Later", color = textGray)
                }
            },
            containerColor = cardBg
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
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = tealAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "MEDITATION",
                                    color = textWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Find your peace",
                                    color = tealAccent,
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
                        text = "Guided Meditations",
                        color = textWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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
                        Text(
                            text = "Free",
                            color = tealAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                            Text(
                                text = "Premium",
                                color = lavenderLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = lavenderLight,
                                modifier = Modifier.size(16.dp)
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
                            text = "Recent Sessions",
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Your Journey",
                color = tealAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(value = "${stats.totalSessions}", label = "Sessions")
                StatItem(value = "${stats.totalMinutes}", label = "Minutes")
                StatItem(value = "${stats.currentStreak}", label = "Day Streak")
            }

            stats.favoriteType?.let { favorite ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2D2D2D))
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = favorite.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Favorite",
                            color = textGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = favorite.displayName,
                            color = textWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
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
            color = textWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = textGray,
            fontSize = 12.sp
        )
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            tealAccent.copy(alpha = 0.7f),
                            lavenderPrimary.copy(alpha = 0.5f)
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
                        tint = textWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Soundscape Mixer",
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Premium",
                                tint = textWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Mix multiple sounds to create your perfect ambiance",
                        color = textWhite.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = textWhite,
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
                label = { Text("All", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = lavenderPrimary.copy(alpha = 0.3f),
                    selectedLabelColor = textWhite
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
                        Text(category.displayName, fontSize = 12.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = lavenderPrimary.copy(alpha = 0.3f),
                    selectedLabelColor = textWhite
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) cardBg.copy(alpha = 0.6f) else cardBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFeatured) tealAccent.copy(alpha = 0.15f)
                        else lavenderPrimary.copy(alpha = 0.15f)
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
                        color = if (isLocked) textGray else textWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = textGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = meditation.description,
                    color = textGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Duration
                    Text(
                        text = "${meditation.defaultDurationMinutes} min",
                        color = textGray,
                        fontSize = 11.sp
                    )

                    Text(text = "|", color = textGray.copy(alpha = 0.5f), fontSize = 11.sp)

                    // Category
                    Text(
                        text = meditation.category.displayName,
                        color = if (isFeatured) tealAccent else lavenderLight,
                        fontSize = 11.sp
                    )
                }
            }

            // Play/Lock icon
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                contentDescription = if (isLocked) "Locked" else "Start",
                tint = if (isLocked) textGray else if (isFeatured) tealAccent else lavenderLight,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun RecentMeditationCard(session: MeditationSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.meditationType.emoji,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.meditationType.displayName,
                    color = textWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${session.durationSeconds / 60} min",
                    color = textGray,
                    fontSize = 12.sp
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
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "+$improvement",
                            color = Color(0xFF10B981),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
