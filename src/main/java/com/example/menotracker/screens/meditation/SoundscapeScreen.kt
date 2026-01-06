package com.example.menotracker.screens.meditation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.audio.AudioManager
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.data.models.*
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
 * Soundscape Mixer Screen
 * Allows users to mix multiple ambient sounds together
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundscapeScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: MeditationViewModel = viewModel()
) {
    val soundscapeLayers by viewModel.soundscapeLayers.collectAsState()
    var showUpgradeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.initialize(userId)
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
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = tealAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "SOUNDSCAPE",
                                    color = textWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Create your ambiance",
                                    color = tealAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.clearSoundscape()
                            onNavigateBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textWhite
                            )
                        }
                    },
                    actions = {
                        // Stop all button
                        if (soundscapeLayers.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSoundscape() }) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop all",
                                    tint = textWhite
                                )
                            }
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
                // Active layers
                if (soundscapeLayers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Active Sounds",
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(soundscapeLayers) { layer ->
                        ActiveSoundLayer(
                            layer = layer,
                            onVolumeChange = { viewModel.setSoundscapeLayerVolume(layer.sound, it) },
                            onRemove = { viewModel.removeSoundscapeLayer(layer.sound) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Presets
                item {
                    Text(
                        text = "Presets",
                        color = textWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    PresetRow(
                        onPresetSelected = { preset ->
                            if (preset.isFree || SubscriptionManager.canUseSoundscapeMixer()) {
                                viewModel.loadSoundscapePreset(preset)
                            } else {
                                showUpgradeDialog = true
                            }
                        },
                        onLockedClick = { showUpgradeDialog = true }
                    )
                }

                // Sound library
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sound Library",
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${soundscapeLayers.size}/4",
                            color = textGray,
                            fontSize = 14.sp
                        )
                    }
                }

                item {
                    SoundLibraryGrid(
                        activeSounds = soundscapeLayers.map { it.sound },
                        maxLayers = 4,
                        onSoundToggle = { sound ->
                            val isActive = soundscapeLayers.any { it.sound == sound }
                            if (isActive) {
                                viewModel.removeSoundscapeLayer(sound)
                            } else if (soundscapeLayers.size < 4) {
                                viewModel.addSoundscapeLayer(sound)
                            }
                        },
                        onLockedClick = { showUpgradeDialog = true }
                    )
                }

                // Instructions
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    InstructionsCard()
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
                    "The Soundscape Mixer and premium sounds require a Premium subscription.",
                    color = textGray
                )
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

@Composable
private fun ActiveSoundLayer(
    layer: SoundLayer,
    onVolumeChange: (Float) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tealAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = layer.sound.emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and volume slider
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = layer.sound.displayName,
                    color = textWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = layer.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.height(32.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = tealAccent,
                        activeTrackColor = tealAccent,
                        inactiveTrackColor = Color(0xFF2D2D2D)
                    )
                )
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textGray
                )
            }
        }
    }
}

@Composable
private fun PresetRow(
    onPresetSelected: (SoundscapePreset) -> Unit,
    onLockedClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(SoundscapePreset.PRESETS) { preset ->
            PresetCard(
                preset = preset,
                isLocked = !preset.isFree && !SubscriptionManager.canUseSoundscapeMixer(),
                onClick = {
                    if (preset.isFree || SubscriptionManager.canUseSoundscapeMixer()) {
                        onPresetSelected(preset)
                    } else {
                        onLockedClick()
                    }
                }
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: SoundscapePreset,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) cardBg.copy(alpha = 0.6f) else cardBg
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
                // Sound emojis
                Row {
                    preset.layers.take(2).forEach { layer ->
                        Text(text = layer.sound.emoji, fontSize = 20.sp)
                    }
                }

                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = textGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = preset.name,
                color = if (isLocked) textGray else textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = preset.description,
                color = textGray.copy(alpha = if (isLocked) 0.5f else 1f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SoundLibraryGrid(
    activeSounds: List<AmbientSound>,
    maxLayers: Int,
    onSoundToggle: (AmbientSound) -> Unit,
    onLockedClick: () -> Unit
) {
    val sounds = AmbientSound.entries

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(200.dp)
    ) {
        items(sounds) { sound ->
            val isActive = sound in activeSounds
            val isLocked = !sound.isFree && !SubscriptionManager.canAccessSound(sound.isFree)
            val isDisabled = !isActive && activeSounds.size >= maxLayers

            SoundGridItem(
                sound = sound,
                isActive = isActive,
                isLocked = isLocked,
                isDisabled = isDisabled,
                onClick = {
                    when {
                        isLocked -> onLockedClick()
                        isDisabled -> { /* Max reached */ }
                        else -> onSoundToggle(sound)
                    }
                }
            )
        }
    }
}

@Composable
private fun SoundGridItem(
    sound: AmbientSound,
    isActive: Boolean,
    isLocked: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> tealAccent.copy(alpha = 0.3f)
            isLocked || isDisabled -> cardBg.copy(alpha = 0.4f)
            else -> cardBg
        },
        label = "soundItemBg"
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                Box {
                    Text(
                        text = sound.emoji,
                        fontSize = 32.sp,
                        color = when {
                            isLocked || isDisabled -> textGray.copy(alpha = 0.4f)
                            else -> textWhite
                        }
                    )

                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = textGray,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = tealAccent,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sound.displayName,
                    color = when {
                        isLocked || isDisabled -> textGray.copy(alpha = 0.4f)
                        else -> textGray
                    },
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = tealAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How to use",
                    color = textWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "• Tap sounds to add them to your mix (up to 4)\n• Adjust individual volumes with the sliders\n• Use presets for quick combinations\n• Audio continues when you leave this screen",
                color = textGray,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
