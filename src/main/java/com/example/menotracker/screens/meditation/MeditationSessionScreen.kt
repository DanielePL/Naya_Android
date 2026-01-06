package com.example.menotracker.screens.meditation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.components.ChimePicker
import com.example.menotracker.ui.components.MusicPickerRow
import com.example.menotracker.ui.components.SoundPickerRow
import com.example.menotracker.ui.components.VolumeSlider
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.viewmodels.MeditationViewModel

// Colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val tealAccent = Color(0xFF14B8A6)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)

/**
 * Active Meditation Session Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationSessionScreen(
    meditationType: MeditationType,
    userId: String,
    onNavigateBack: () -> Unit,
    onSessionComplete: () -> Unit,
    viewModel: MeditationViewModel = viewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()

    // Audio state
    val selectedSound by viewModel.selectedSound.collectAsState()
    val selectedMusic by viewModel.selectedMusic.collectAsState()
    val soundVolume by viewModel.soundVolume.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()
    val chimeEnabled by viewModel.chimeEnabled.collectAsState()
    val selectedChime by viewModel.selectedChime.collectAsState()

    // UI state
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showMoodBeforeSheet by remember { mutableStateOf(true) }
    var showMoodAfterSheet by remember { mutableStateOf(false) }

    // Initialize session
    LaunchedEffect(meditationType) {
        viewModel.initialize(userId)
        viewModel.selectMeditation(meditationType)
    }

    // Check if session is complete
    LaunchedEffect(sessionState) {
        sessionState?.let { state ->
            if (state.isComplete && !state.isRunning) {
                showMoodAfterSheet = true
            }
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        viewModel.cancelSession()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textWhite
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = meditationType.displayName,
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = meditationType.category.displayName,
                            color = tealAccent,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.weight(0.3f))

                // Meditation visualization
                sessionState?.let { state ->
                    MeditationVisualizer(
                        progress = state.progress,
                        isRunning = state.isRunning,
                        phase = state.currentPhase,
                        emoji = meditationType.emoji,
                        modifier = Modifier.size(280.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Timer display
                    TimerDisplay(
                        remainingSeconds = state.remainingSeconds,
                        totalSeconds = state.totalDurationSeconds,
                        phase = state.currentPhase
                    )

                } ?: run {
                    CircularProgressIndicator(color = tealAccent)
                }

                Spacer(modifier = Modifier.weight(0.3f))

                // Duration selector (before session starts)
                sessionState?.let { state ->
                    if (!state.isRunning && !state.isPaused && state.remainingSeconds == state.totalDurationSeconds) {
                        DurationSelector(
                            availableDurations = meditationType.availableDurations,
                            selectedDuration = selectedDuration,
                            onSelect = { viewModel.setDuration(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sound settings
                        SoundSettingsSection(
                            selectedSound = selectedSound,
                            selectedMusic = selectedMusic,
                            soundVolume = soundVolume,
                            musicVolume = musicVolume,
                            chimeEnabled = chimeEnabled,
                            selectedChime = selectedChime,
                            onSoundSelected = { viewModel.setSelectedSound(it) },
                            onMusicSelected = { viewModel.setSelectedMusic(it) },
                            onSoundVolumeChange = { viewModel.setSoundVolume(it) },
                            onMusicVolumeChange = { viewModel.setMusicVolume(it) },
                            onChimeEnabledChange = { viewModel.setChimeEnabled(it) },
                            onChimeSelected = { viewModel.setSelectedChime(it) },
                            onLockedClick = { showUpgradeDialog = true }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Control buttons
                sessionState?.let { state ->
                    SessionControlButtons(
                        isRunning = state.isRunning,
                        isPaused = state.isPaused,
                        isComplete = state.isComplete,
                        onStart = { viewModel.startSession() },
                        onPause = { viewModel.pauseSession() },
                        onResume = { viewModel.resumeSession() },
                        onComplete = { showMoodAfterSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Mood Before Sheet
            if (showMoodBeforeSheet && sessionState != null) {
                MoodSelectionSheet(
                    title = "How are you feeling?",
                    subtitle = "Before we begin",
                    onSelect = { mood ->
                        viewModel.setMoodBefore(mood)
                        showMoodBeforeSheet = false
                    },
                    onSkip = { showMoodBeforeSheet = false }
                )
            }

            // Mood After Sheet
            if (showMoodAfterSheet) {
                MoodSelectionSheet(
                    title = "How do you feel now?",
                    subtitle = "After your meditation",
                    onSelect = { mood ->
                        showMoodAfterSheet = false
                        viewModel.completeSession(mood)
                        onSessionComplete()
                    },
                    onSkip = {
                        showMoodAfterSheet = false
                        viewModel.completeSession(null)
                        onSessionComplete()
                    }
                )
            }

            // Upgrade dialog
            if (showUpgradeDialog) {
                AlertDialog(
                    onDismissRequest = { showUpgradeDialog = false },
                    title = {
                        Text("Premium Sound", color = textWhite, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text("Unlock all ambient sounds and music with Premium.", color = textGray)
                    },
                    confirmButton = {
                        Button(
                            onClick = { showUpgradeDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = lavenderPrimary)
                        ) {
                            Text("Learn More")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpgradeDialog = false }) {
                            Text("Cancel", color = textGray)
                        }
                    },
                    containerColor = cardBg
                )
            }
        }
    }
}

@Composable
private fun MeditationVisualizer(
    progress: Float,
    isRunning: Boolean,
    phase: MeditationPhase,
    emoji: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    // Gentle breathing animation
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // Pulsing glow
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val scale = if (isRunning) breathingScale else 1f
    val alpha = if (isRunning) glowAlpha else 0.3f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale * 1.2f)
                .alpha(alpha)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tealAccent.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main circle
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tealAccent.copy(alpha = 0.3f),
                            lavenderPrimary.copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = emoji,
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = phase.displayName,
                    color = textWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Progress ring
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = tealAccent,
            strokeWidth = 4.dp,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun TimerDisplay(
    remainingSeconds: Int,
    totalSeconds: Int,
    phase: MeditationPhase
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            color = textWhite,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light
        )

        Text(
            text = "of ${totalSeconds / 60} min",
            color = textGray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DurationSelector(
    availableDurations: List<Int>,
    selectedDuration: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        availableDurations.forEach { duration ->
            val isSelected = duration == selectedDuration

            FilterChip(
                selected = isSelected,
                onClick = { onSelect(duration) },
                label = {
                    Text(
                        text = "$duration min",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tealAccent.copy(alpha = 0.3f),
                    selectedLabelColor = textWhite
                )
            )
        }
    }
}

@Composable
private fun SoundSettingsSection(
    selectedSound: AmbientSound?,
    selectedMusic: BackgroundMusic?,
    soundVolume: Float,
    musicVolume: Float,
    chimeEnabled: Boolean,
    selectedChime: ChimeSound,
    onSoundSelected: (AmbientSound?) -> Unit,
    onMusicSelected: (BackgroundMusic?) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onChimeEnabledChange: (Boolean) -> Unit,
    onChimeSelected: (ChimeSound) -> Unit,
    onLockedClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ambient sound
            SoundPickerRow(
                selectedSound = selectedSound,
                onSoundSelected = onSoundSelected,
                onLockedSoundClick = onLockedClick
            )

            // Sound volume
            if (selectedSound != null) {
                Spacer(modifier = Modifier.height(12.dp))
                VolumeSlider(
                    label = "Sound",
                    emoji = selectedSound.emoji,
                    volume = soundVolume,
                    onVolumeChange = onSoundVolumeChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Background music
            MusicPickerRow(
                selectedMusic = selectedMusic,
                onMusicSelected = onMusicSelected,
                onLockedMusicClick = onLockedClick
            )

            // Music volume
            if (selectedMusic != null) {
                Spacer(modifier = Modifier.height(12.dp))
                VolumeSlider(
                    label = "Music",
                    emoji = selectedMusic.emoji,
                    volume = musicVolume,
                    onVolumeChange = onMusicVolumeChange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chime picker
            ChimePicker(
                selectedChime = selectedChime,
                onChimeSelected = onChimeSelected,
                chimeEnabled = chimeEnabled,
                onChimeEnabledChange = onChimeEnabledChange
            )
        }
    }
}

@Composable
private fun SessionControlButtons(
    isRunning: Boolean,
    isPaused: Boolean,
    isComplete: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            isComplete -> {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.height(64.dp).width(200.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            isRunning -> {
                Button(
                    onClick = onPause,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = cardBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = textWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            isPaused -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = tealAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            else -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.height(64.dp).width(200.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tealAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Begin",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodSelectionSheet(
    title: String,
    subtitle: String,
    onSelect: (Int) -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = textWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    color = textGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoodOption(value = 1, label = "Bad", color = Color(0xFFEF4444)) { onSelect(1) }
                    MoodOption(value = 2, label = "Low", color = Color(0xFFF97316)) { onSelect(2) }
                    MoodOption(value = 3, label = "Okay", color = Color(0xFFFBBF24)) { onSelect(3) }
                    MoodOption(value = 4, label = "Good", color = Color(0xFF84CC16)) { onSelect(4) }
                    MoodOption(value = 5, label = "Great", color = Color(0xFF10B981)) { onSelect(5) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onSkip) {
                    Text(text = "Skip", color = textGray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun MoodOption(
    value: Int,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$value",
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = textGray,
            fontSize = 11.sp
        )
    }
}
