// screens/session/components/TimerSelectionCard.kt
package com.example.menotracker.screens.session.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.viewmodels.WodTimerConfig

/**
 * Timer type options for selection
 */
enum class TimerType(val displayName: String, val shortName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val description: String) {
    NONE("Kein Timer", "-", Icons.Default.TimerOff, "Normales Training ohne Timer"),
    AMRAP("AMRAP", "AMRAP", Icons.Default.AllInclusive, "So viele Runden wie möglich"),
    EMOM("EMOM", "EMOM", Icons.Default.Timer, "Jede Minute starten"),
    FOR_TIME("For Time", "TIME", Icons.Default.Flag, "Auf Zeit absolvieren"),
    TABATA("Tabata", "TABATA", Icons.Default.FitnessCenter, "20s Work / 10s Rest")
}

/**
 * Smart Start Card - Combines Timer Selection with START button
 * Clean, compact design that expands to show timer options
 */
@Composable
fun TimerSelectionCard(
    currentTimerConfig: WodTimerConfig?,
    isWorkoutStarted: Boolean,
    onTimerSelected: (WodTimerConfig?) -> Unit,
    onStartWorkout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(
        when (currentTimerConfig?.wodType) {
            "amrap" -> TimerType.AMRAP
            "emom" -> TimerType.EMOM
            "for_time" -> TimerType.FOR_TIME
            "tabata" -> TimerType.TABATA
            else -> TimerType.NONE
        }
    ) }

    // Timer settings
    var timeCapMinutes by remember { mutableStateOf(currentTimerConfig?.timeCapSeconds?.div(60)?.toString() ?: "10") }
    var emomMinutes by remember { mutableStateOf((currentTimerConfig?.timeCapSeconds?.div(60))?.toString() ?: "10") }
    var emomIntervalSeconds by remember { mutableStateOf(currentTimerConfig?.emomIntervalSeconds?.toString() ?: "60") }
    var tabataRounds by remember { mutableStateOf(currentTimerConfig?.tabataRounds?.toString() ?: "8") }
    var tabataWorkSeconds by remember { mutableStateOf(currentTimerConfig?.tabataWorkSeconds?.toString() ?: "20") }
    var tabataRestSeconds by remember { mutableStateOf(currentTimerConfig?.tabataRestSeconds?.toString() ?: "10") }

    // Don't show anything if workout started - timer is shown in WodTimerCard
    if (isWorkoutStarted) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1410).copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, NayaOrangeGlow.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== HEADER: Timer + START =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Timer selection chip
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded },
                    color = if (selectedType != TimerType.NONE)
                        NayaPrimary.copy(alpha = 0.15f)
                    else
                        Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (selectedType != TimerType.NONE)
                            NayaPrimary.copy(alpha = 0.5f)
                        else
                            Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (selectedType != TimerType.NONE) NayaPrimary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )

                        if (selectedType == TimerType.NONE) {
                            Text(
                                text = "Timer",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        } else {
                            // Timer type badge
                            Surface(
                                color = NayaPrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = selectedType.shortName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = getTimerSummary(selectedType, timeCapMinutes, emomMinutes, emomIntervalSeconds, tabataRounds),
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Right side: BIG START BUTTON
                Button(
                    onClick = {
                        // Apply timer config before starting
                        if (selectedType != TimerType.NONE) {
                            val config = when (selectedType) {
                                TimerType.AMRAP -> WodTimerConfig(
                                    wodType = "amrap",
                                    timeCapSeconds = (timeCapMinutes.toIntOrNull() ?: 10) * 60
                                )
                                TimerType.EMOM -> WodTimerConfig(
                                    wodType = "emom",
                                    timeCapSeconds = (emomMinutes.toIntOrNull() ?: 10) * 60,
                                    emomIntervalSeconds = emomIntervalSeconds.toIntOrNull() ?: 60
                                )
                                TimerType.FOR_TIME -> WodTimerConfig(
                                    wodType = "for_time",
                                    timeCapSeconds = (timeCapMinutes.toIntOrNull() ?: 0).let { if (it > 0) it * 60 else null }
                                )
                                TimerType.TABATA -> WodTimerConfig(
                                    wodType = "tabata",
                                    tabataRounds = tabataRounds.toIntOrNull() ?: 8,
                                    tabataWorkSeconds = tabataWorkSeconds.toIntOrNull() ?: 20,
                                    tabataRestSeconds = tabataRestSeconds.toIntOrNull() ?: 10
                                )
                                else -> null
                            }
                            onTimerSelected(config)
                        }
                        onStartWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Green
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "START",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // ===== EXPANDED: Timer Options =====
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    // Timer type selection chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TimerType.values().forEach { type ->
                            SmartTimerChip(
                                type = type,
                                isSelected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    if (type == TimerType.NONE) {
                                        onTimerSelected(null)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Timer-specific settings
                    if (selectedType != TimerType.NONE) {
                        Text(
                            text = selectedType.description,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        when (selectedType) {
                            TimerType.AMRAP -> {
                                CompactTimerSetting(
                                    label = "Zeit",
                                    value = timeCapMinutes,
                                    unit = "min",
                                    onValueChange = { timeCapMinutes = it }
                                )
                            }

                            TimerType.EMOM -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CompactTimerSetting(
                                        label = "Dauer",
                                        value = emomMinutes,
                                        unit = "min",
                                        onValueChange = { emomMinutes = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactTimerSetting(
                                        label = "Intervall",
                                        value = emomIntervalSeconds,
                                        unit = "sek",
                                        onValueChange = { emomIntervalSeconds = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            TimerType.FOR_TIME -> {
                                CompactTimerSetting(
                                    label = "Time Cap (0 = kein Limit)",
                                    value = timeCapMinutes,
                                    unit = "min",
                                    onValueChange = { timeCapMinutes = it }
                                )
                            }

                            TimerType.TABATA -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CompactTimerSetting(
                                        label = "Runden",
                                        value = tabataRounds,
                                        unit = "x",
                                        onValueChange = { tabataRounds = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactTimerSetting(
                                        label = "Work",
                                        value = tabataWorkSeconds,
                                        unit = "s",
                                        onValueChange = { tabataWorkSeconds = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactTimerSetting(
                                        label = "Rest",
                                        value = tabataRestSeconds,
                                        unit = "s",
                                        onValueChange = { tabataRestSeconds = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

/**
 * Smart compact timer chip for type selection
 */
@Composable
private fun SmartTimerChip(
    type: TimerType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        color = if (isSelected) NayaPrimary else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (isSelected) NayaPrimary else Color.Gray.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (type == TimerType.NONE) "Aus" else type.shortName,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Compact timer setting with inline unit
 */
@Composable
private fun CompactTimerSetting(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue: String ->
                    if (newValue.all { char -> char.isDigit() } || newValue.isEmpty()) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NayaPrimary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = NayaPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Text(
                text = unit,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

private fun getTimerSummary(
    type: TimerType,
    timeCapMinutes: String,
    emomMinutes: String,
    emomIntervalSeconds: String,
    tabataRounds: String
): String {
    return when (type) {
        TimerType.AMRAP -> "${timeCapMinutes} min"
        TimerType.EMOM -> "${emomMinutes} min • ${emomIntervalSeconds}s"
        TimerType.FOR_TIME -> if (timeCapMinutes != "0") "${timeCapMinutes} min cap" else "∞"
        TimerType.TABATA -> "${tabataRounds}x (20/10)"
        else -> ""
    }
}