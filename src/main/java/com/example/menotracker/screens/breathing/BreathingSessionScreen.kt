package com.example.menotracker.screens.breathing

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
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
import com.example.menotracker.ui.components.SoundPickerRow
import com.example.menotracker.ui.components.VolumeSlider
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.viewmodels.BreathingViewModel

// Colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val tealAccent = Color(0xFF14B8A6)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)

/**
 * Active Breathing Session Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingSessionScreen(
    exerciseType: BreathingExerciseType,
    userId: String,
    onNavigateBack: () -> Unit,
    onSessionComplete: () -> Unit,
    viewModel: BreathingViewModel = viewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Audio state
    val selectedSound by viewModel.selectedSound.collectAsState()
    val soundVolume by viewModel.soundVolume.collectAsState()
    val chimeEnabled by viewModel.chimeEnabled.collectAsState()
    val selectedChime by viewModel.selectedChime.collectAsState()

    // Show upgrade prompt
    var showUpgradeDialog by remember { mutableStateOf(false) }

    // Initialize session
    LaunchedEffect(exerciseType) {
        viewModel.initialize(userId)
        viewModel.selectExercise(exerciseType)
    }

    // Session states
    var showMoodBeforeSheet by remember { mutableStateOf(true) }
    var showMoodAfterSheet by remember { mutableStateOf(false) }
    var selectedMoodBefore by remember { mutableStateOf<Int?>(null) }

    // Check if session is complete
    LaunchedEffect(sessionState) {
        sessionState?.let { state ->
            if (state.currentPhase == BreathingPhase.REST && !state.isRunning && state.totalSecondsRemaining <= 0) {
                showMoodAfterSheet = true
            }
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
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
                            text = exerciseType.displayName,
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = exerciseType.targetSymptoms.first(),
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }

                    // Placeholder for symmetry
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Breathing Animation Circle
                sessionState?.let { state ->
                    BreathingAnimationCircle(
                        sessionState = state,
                        modifier = Modifier.size(300.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Phase indicator
                    PhaseIndicator(
                        currentPhase = state.currentPhase,
                        hasHoldOut = exerciseType.pattern.holdOutSeconds > 0
                    )
                } ?: run {
                    // Loading state
                    CircularProgressIndicator(color = lavenderPrimary)
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Sound settings (only before session starts)
                sessionState?.let { state ->
                    if (!state.isRunning && !state.isPaused) {
                        SoundSettingsSection(
                            selectedSound = selectedSound,
                            soundVolume = soundVolume,
                            chimeEnabled = chimeEnabled,
                            selectedChime = selectedChime,
                            onSoundSelected = { viewModel.setSelectedSound(it) },
                            onVolumeChange = { viewModel.setSoundVolume(it) },
                            onChimeEnabledChange = { viewModel.setChimeEnabled(it) },
                            onChimeSelected = { viewModel.setSelectedChime(it) },
                            onLockedSoundClick = { showUpgradeDialog = true }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Control buttons
                sessionState?.let { state ->
                    SessionControlButtons(
                        isRunning = state.isRunning,
                        isPaused = state.isPaused,
                        isComplete = state.currentPhase == BreathingPhase.REST && state.totalSecondsRemaining <= 0,
                        onStart = { viewModel.startSession() },
                        onPause = { viewModel.pauseSession() },
                        onResume = { viewModel.resumeSession() },
                        onComplete = { showMoodAfterSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Pattern info
                PatternInfo(pattern = exerciseType.pattern)

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Mood Before Sheet
            if (showMoodBeforeSheet && sessionState != null) {
                MoodSelectionSheet(
                    title = "How are you feeling now?",
                    subtitle = "Before we begin",
                    onSelect = { mood ->
                        selectedMoodBefore = mood
                        viewModel.setMoodBefore(mood)
                        showMoodBeforeSheet = false
                    },
                    onSkip = {
                        showMoodBeforeSheet = false
                    }
                )
            }

            // Mood After Sheet
            if (showMoodAfterSheet) {
                MoodSelectionSheet(
                    title = "How do you feel now?",
                    subtitle = "After the session",
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
                        Text(
                            "Premium Sound",
                            color = textWhite,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            "Unlock all ambient sounds and music with Premium.",
                            color = textGray
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showUpgradeDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = lavenderPrimary
                            )
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
                // Complete button
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .height(64.dp)
                        .width(200.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
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
                // Pause button
                Button(
                    onClick = onPause,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBg
                    )
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
                // Resume button
                Button(
                    onClick = onResume,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = lavenderPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            else -> {
                // Start button
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .height(64.dp)
                        .width(200.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = lavenderPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternInfo(pattern: BreathingPattern) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PatternStep("Inhale", "${pattern.inhaleSeconds}s", tealAccent)

            if (pattern.holdInSeconds > 0) {
                PatternStep("Hold", "${pattern.holdInSeconds}s", lavenderLight)
            }

            PatternStep("Exhale", "${pattern.exhaleSeconds}s", Color(0xFFEC4899))

            if (pattern.holdOutSeconds > 0) {
                PatternStep("Hold", "${pattern.holdOutSeconds}s", lavenderPrimary)
            }
        }
    }
}

@Composable
private fun PatternStep(label: String, duration: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = duration,
            color = textWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = textGray,
            fontSize = 11.sp
        )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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

                // Mood options (1-5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoodOption(emoji = "1", label = "Bad", color = Color(0xFFEF4444)) { onSelect(1) }
                    MoodOption(emoji = "2", label = "Low", color = Color(0xFFF97316)) { onSelect(2) }
                    MoodOption(emoji = "3", label = "Okay", color = Color(0xFFFBBF24)) { onSelect(3) }
                    MoodOption(emoji = "4", label = "Good", color = Color(0xFF84CC16)) { onSelect(4) }
                    MoodOption(emoji = "5", label = "Great", color = Color(0xFF10B981)) { onSelect(5) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip",
                        color = textGray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodOption(
    emoji: String,
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
                text = emoji,
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

/**
 * Sound settings section for pre-session setup
 */
@Composable
private fun SoundSettingsSection(
    selectedSound: AmbientSound?,
    soundVolume: Float,
    chimeEnabled: Boolean,
    selectedChime: ChimeSound,
    onSoundSelected: (AmbientSound?) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onChimeEnabledChange: (Boolean) -> Unit,
    onChimeSelected: (ChimeSound) -> Unit,
    onLockedSoundClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Sound picker row
            SoundPickerRow(
                selectedSound = selectedSound,
                onSoundSelected = onSoundSelected,
                onLockedSoundClick = onLockedSoundClick
            )

            // Volume slider (only if sound selected)
            if (selectedSound != null) {
                Spacer(modifier = Modifier.height(16.dp))
                VolumeSlider(
                    label = "Volume",
                    emoji = selectedSound.emoji,
                    volume = soundVolume,
                    onVolumeChange = onVolumeChange
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
