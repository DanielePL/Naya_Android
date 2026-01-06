package com.example.menotracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.audio.AudioManager
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.data.models.AmbientSound
import com.example.menotracker.data.models.BackgroundMusic
import com.example.menotracker.data.models.ChimeSound

// Colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val tealAccent = Color(0xFF14B8A6)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)
private val cardBgLight = Color(0xFF2D2D2D)

/**
 * Compact horizontal sound picker for session setup
 */
@Composable
fun SoundPickerRow(
    selectedSound: AmbientSound?,
    onSoundSelected: (AmbientSound?) -> Unit,
    onLockedSoundClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Background Sound",
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (selectedSound != null) {
                TextButton(
                    onClick = { onSoundSelected(null) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("None", color = textGray, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AmbientSound.entries) { sound ->
                val isSelected = selectedSound == sound
                val isLocked = !sound.isFree && !SubscriptionManager.canAccessSound(sound.isFree)

                SoundChip(
                    emoji = sound.emoji,
                    name = sound.displayName,
                    isSelected = isSelected,
                    isLocked = isLocked,
                    onClick = {
                        if (isLocked) {
                            onLockedSoundClick()
                        } else {
                            onSoundSelected(if (isSelected) null else sound)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SoundChip(
    emoji: String,
    name: String,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> lavenderPrimary.copy(alpha = 0.3f)
            isLocked -> cardBg.copy(alpha = 0.5f)
            else -> cardBg
        },
        label = "chipBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) lavenderPrimary else Color.Transparent,
        label = "chipBorder"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Text(
                    text = emoji,
                    fontSize = 28.sp,
                    color = if (isLocked) textGray.copy(alpha = 0.5f) else textWhite
                )

                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = textGray,
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name,
                color = if (isLocked) textGray.copy(alpha = 0.5f) else textGray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Full sound picker grid for soundscape/meditation
 */
@Composable
fun SoundPickerGrid(
    selectedSounds: List<AmbientSound>,
    onSoundToggle: (AmbientSound) -> Unit,
    onLockedSoundClick: () -> Unit,
    maxSelections: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ambient Sounds",
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${selectedSounds.size}/$maxSelections",
                color = textGray,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(AmbientSound.entries) { sound ->
                val isSelected = sound in selectedSounds
                val isLocked = !sound.isFree && !SubscriptionManager.canAccessSound(sound.isFree)
                val isDisabled = !isSelected && selectedSounds.size >= maxSelections

                SoundGridItem(
                    sound = sound,
                    isSelected = isSelected,
                    isLocked = isLocked,
                    isDisabled = isDisabled,
                    onClick = {
                        when {
                            isLocked -> onLockedSoundClick()
                            isDisabled -> { /* Max reached */ }
                            else -> onSoundToggle(sound)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SoundGridItem(
    sound: AmbientSound,
    isSelected: Boolean,
    isLocked: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> lavenderPrimary.copy(alpha = 0.3f)
            isLocked || isDisabled -> cardBg.copy(alpha = 0.4f)
            else -> cardBg
        },
        label = "gridItemBg"
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

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = lavenderPrimary,
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
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Volume slider component
 */
@Composable
fun VolumeSlider(
    label: String,
    emoji: String,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.width(40.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    color = textGray,
                    fontSize = 12.sp
                )
                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                    thumbColor = lavenderPrimary,
                    activeTrackColor = lavenderPrimary,
                    inactiveTrackColor = cardBgLight
                )
            )
        }
    }
}

/**
 * Chime selector
 */
@Composable
fun ChimePicker(
    selectedChime: ChimeSound,
    onChimeSelected: (ChimeSound) -> Unit,
    chimeEnabled: Boolean,
    onChimeEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = lavenderLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Session Chimes",
                        color = textWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = chimeEnabled,
                    onCheckedChange = onChimeEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = lavenderPrimary,
                        checkedTrackColor = lavenderPrimary.copy(alpha = 0.5f)
                    )
                )
            }

            if (chimeEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ChimeSound.entries) { chime ->
                        val isSelected = selectedChime == chime

                        FilterChip(
                            selected = isSelected,
                            onClick = { onChimeSelected(chime) },
                            label = {
                                Text(
                                    text = chime.displayName,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = lavenderPrimary.copy(alpha = 0.3f),
                                selectedLabelColor = textWhite
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preview button
                TextButton(
                    onClick = {
                        AudioManager.playChime(selectedChime)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview",
                        tint = tealAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Preview",
                        color = tealAccent,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Music picker for meditation
 */
@Composable
fun MusicPickerRow(
    selectedMusic: BackgroundMusic?,
    onMusicSelected: (BackgroundMusic?) -> Unit,
    onLockedMusicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Background Music",
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (selectedMusic != null) {
                TextButton(
                    onClick = { onMusicSelected(null) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("None", color = textGray, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(BackgroundMusic.entries) { music ->
                val isSelected = selectedMusic == music
                val isLocked = !music.isFree && !SubscriptionManager.canAccessSound(music.isFree)

                SoundChip(
                    emoji = music.emoji,
                    name = music.displayName,
                    isSelected = isSelected,
                    isLocked = isLocked,
                    onClick = {
                        if (isLocked) {
                            onLockedMusicClick()
                        } else {
                            onMusicSelected(if (isSelected) null else music)
                        }
                    }
                )
            }
        }
    }
}
