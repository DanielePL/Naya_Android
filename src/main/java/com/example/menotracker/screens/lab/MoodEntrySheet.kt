package com.example.menotracker.screens.lab

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.MoodTrigger
import com.example.menotracker.data.models.MoodType
import com.example.menotracker.data.models.TimeOfDay

// Design colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)
private val surfaceBg = Color(0xFF121212)

/**
 * Bottom Sheet for logging a mood entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodEntrySheet(
    onDismiss: () -> Unit,
    onSubmit: (
        moodType: MoodType,
        intensity: Int,
        timeOfDay: TimeOfDay?,
        triggers: List<MoodTrigger>?,
        journalText: String?
    ) -> Unit,
    remainingSlots: Int,
    hasUnlimitedAccess: Boolean
) {
    var selectedMood by remember { mutableStateOf<MoodType?>(null) }
    var intensity by remember { mutableFloatStateOf(3f) }
    var selectedTimeOfDay by remember { mutableStateOf<TimeOfDay?>(null) }
    var selectedTriggers by remember { mutableStateOf<Set<MoodTrigger>>(emptySet()) }
    var journalText by remember { mutableStateOf("") }
    var showTriggers by remember { mutableStateOf(false) }

    val canSubmit = selectedMood != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = surfaceBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "How are you feeling?",
                    color = textWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textGray
                    )
                }
            }

            // Remaining slots indicator (for free users)
            if (!hasUnlimitedAccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$remainingSlots entries remaining this week",
                    color = if (remainingSlots <= 1) Color(0xFFF59E0B) else textGray,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mood Selection
            Text(
                text = "Select your mood",
                color = textGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MoodType.entries) { mood ->
                    MoodButton(
                        mood = mood,
                        isSelected = selectedMood == mood,
                        onClick = { selectedMood = mood }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Intensity Slider
            Text(
                text = "Intensity",
                color = textGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mild",
                    color = textGray,
                    fontSize = 12.sp
                )
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = lavenderPrimary,
                        activeTrackColor = lavenderPrimary,
                        inactiveTrackColor = cardBg
                    )
                )
                Text(
                    text = "Strong",
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Time of Day
            Text(
                text = "Time of day (optional)",
                color = textGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeOfDay.entries.forEach { time ->
                    TimeOfDayChip(
                        timeOfDay = time,
                        isSelected = selectedTimeOfDay == time,
                        onClick = {
                            selectedTimeOfDay = if (selectedTimeOfDay == time) null else time
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Triggers (expandable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTriggers = !showTriggers },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "What triggered this? (optional)",
                    color = textGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (showTriggers) "Hide" else "Show",
                    color = lavenderLight,
                    fontSize = 14.sp
                )
            }

            if (showTriggers) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoodTrigger.entries.forEach { trigger ->
                        TriggerChip(
                            trigger = trigger,
                            isSelected = trigger in selectedTriggers,
                            onClick = {
                                selectedTriggers = if (trigger in selectedTriggers) {
                                    selectedTriggers - trigger
                                } else {
                                    selectedTriggers + trigger
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Journal Text
            Text(
                text = "Add a note (optional)",
                color = textGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = journalText,
                onValueChange = { journalText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = {
                    Text("How are you feeling today?", color = textGray.copy(alpha = 0.5f))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = lavenderPrimary,
                    unfocusedBorderColor = cardBg,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    cursorColor = lavenderPrimary,
                    focusedTextColor = textWhite,
                    unfocusedTextColor = textWhite
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    selectedMood?.let { mood ->
                        onSubmit(
                            mood,
                            intensity.toInt(),
                            selectedTimeOfDay,
                            selectedTriggers.toList().takeIf { it.isNotEmpty() },
                            journalText.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = lavenderPrimary,
                    disabledContainerColor = cardBg
                )
            ) {
                Text(
                    text = "Save Mood",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MoodButton(
    mood: MoodType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(),
        label = "scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(mood.color).copy(alpha = 0.2f) else cardBg,
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(mood.color) else Color.Transparent,
        label = "borderColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = mood.emoji,
            fontSize = 36.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = mood.displayName,
            color = if (isSelected) textWhite else textGray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TimeOfDayChip(
    timeOfDay: TimeOfDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) lavenderPrimary.copy(alpha = 0.2f) else cardBg,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, lavenderPrimary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeOfDay.emoji,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeOfDay.displayName,
                color = if (isSelected) textWhite else textGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TriggerChip(
    trigger: MoodTrigger,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) lavenderPrimary.copy(alpha = 0.2f) else cardBg,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, lavenderPrimary)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trigger.emoji,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = trigger.displayName,
                color = if (isSelected) textWhite else textGray,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Simple FlowRow implementation for trigger chips
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple wrapping layout using Column of Rows
    // For production, use accompanist FlowRow or Compose 1.4+ FlowRow
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
