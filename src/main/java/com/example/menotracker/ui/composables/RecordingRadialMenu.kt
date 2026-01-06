package com.example.menotracker.ui.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaBackground
import com.example.menotracker.ui.theme.NayaSurface
import com.example.menotracker.ui.theme.NayaTextSecondary
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════
// DATA CLASS - Simplified to 2 options: Clean or Metrics
// ═══════════════════════════════════════════════════════════════

data class RecordingMode(
    val isClean: Boolean = false,
    val showMetrics: Boolean = false
) {
    fun hasAnySelection(): Boolean = isClean || showMetrics

    fun getActiveLabel(): String = when {
        showMetrics -> "Metrics"
        isClean -> "Clean"
        else -> ""
    }
}

// ═══════════════════════════════════════════════════════════════
// RADIAL MENU OPTION - Simplified to 2 options
// ═══════════════════════════════════════════════════════════════

private enum class RadialOption(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val color: Color
) {
    CLEAN(
        icon = Icons.Default.Videocam,
        label = "Clean",
        description = "Raw video",
        color = Color(0xFF9E9E9E)  // Gray
    ),
    METRICS(
        icon = Icons.Default.Speed,
        label = "Metrics",
        description = "VBT Overlay",
        color = NayaPrimary  // Orange
    )
}

// ═══════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════

@Composable
fun RecordingRadialMenu(
    isVisible: Boolean,
    isVbtEnabled: Boolean,
    currentMode: RecordingMode,
    onModeChanged: (RecordingMode) -> Unit,
    onDismiss: () -> Unit,
    onStartRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for menu expansion
    val expandProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "expand"
    )

    if (expandProgress > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * expandProgress))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            // Radial options
            val options = if (isVbtEnabled) {
                RadialOption.entries.toList()
            } else {
                RadialOption.entries.filter { it != RadialOption.METRICS }
            }

            val angleStep = 360f / options.size
            val radius = 120.dp

            options.forEachIndexed { index, option ->
                val angle = -90f + (angleStep * index) // Start from top
                val angleRad = Math.toRadians(angle.toDouble())

                val offsetX = (cos(angleRad) * radius.value).dp
                val offsetY = (sin(angleRad) * radius.value).dp

                val isSelected = when (option) {
                    RadialOption.CLEAN -> currentMode.isClean
                    RadialOption.METRICS -> currentMode.showMetrics
                }

                // Staggered animation for each option
                val delay = index * 50
                val itemProgress by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    label = "item_$index"
                )

                RadialOptionItem(
                    option = option,
                    isSelected = isSelected,
                    progress = itemProgress,
                    offsetX = offsetX * expandProgress,
                    offsetY = offsetY * expandProgress,
                    onClick = {
                        val newMode = handleOptionClick(option, currentMode)
                        onModeChanged(newMode)
                    }
                )
            }

            // Center Record Button
            CenterRecordButton(
                progress = expandProgress,
                currentMode = currentMode,
                onClick = {
                    if (currentMode.hasAnySelection()) {
                        onStartRecording()
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RADIAL OPTION ITEM
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RadialOptionItem(
    option: RadialOption,
    isSelected: Boolean,
    progress: Float,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .scale(progress)
            .alpha(progress)
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isSelected) option.color else NayaSurface
                )
                .border(
                    width = 2.dp,
                    color = if (isSelected) option.color else Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = if (isSelected) Color.White else NayaTextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label
        Text(
            text = option.label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) option.color else Color.White
        )

        // Description
        Text(
            text = option.description,
            fontSize = 10.sp,
            color = NayaTextSecondary
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// CENTER RECORD BUTTON
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CenterRecordButton(
    progress: Float,
    currentMode: RecordingMode,
    onClick: () -> Unit
) {
    val hasSelection = currentMode.hasAnySelection()

    // Pulsing animation when ready to record
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasSelection) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(progress)
    ) {
        // Main record button
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (hasSelection) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF4444),
                                Color(0xFFCC0000)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                NayaSurface,
                                Color(0xFF2A2A2A)
                            )
                        )
                    }
                )
                .border(
                    width = 3.dp,
                    color = if (hasSelection) Color.White else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(enabled = hasSelection, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = "Record",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active mode indicator
        if (hasSelection) {
            Text(
                text = currentMode.getActiveLabel(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        } else {
            Text(
                text = "Select an option",
                fontSize = 12.sp,
                color = NayaTextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SELECTION LOGIC - Clean and Metrics are mutually exclusive
// ═══════════════════════════════════════════════════════════════

private fun handleOptionClick(option: RadialOption, currentMode: RecordingMode): RecordingMode {
    return when (option) {
        RadialOption.CLEAN -> {
            // Toggle CLEAN, deselect METRICS
            RecordingMode(isClean = !currentMode.isClean, showMetrics = false)
        }
        RadialOption.METRICS -> {
            // Toggle METRICS, deselect CLEAN
            RecordingMode(isClean = false, showMetrics = !currentMode.showMetrics)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RECORD BUTTON WITH BADGE (for showing active mode)
// ═══════════════════════════════════════════════════════════════

@Composable
fun RecordButtonWithBadge(
    currentMode: RecordingMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = currentMode.hasAnySelection()

    Box(modifier = modifier) {
        // Main Record Button
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF4444),
                            Color(0xFFCC0000)
                        )
                    )
                )
                .border(
                    width = 3.dp,
                    color = Color.White,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = "Record",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // Badge showing active mode
        if (hasSelection) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentMode.showMetrics) NayaPrimary else Color(0xFF9E9E9E)
                    )
                    .border(2.dp, NayaBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentMode.showMetrics) Icons.Default.Speed else Icons.Default.Videocam,
                    contentDescription = currentMode.getActiveLabel(),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun RecordingRadialMenuPreview() {
    var mode by remember { mutableStateOf(RecordingMode(showMetrics = true)) }

    RecordingRadialMenu(
        isVisible = true,
        isVbtEnabled = true,
        currentMode = mode,
        onModeChanged = { mode = it },
        onDismiss = {},
        onStartRecording = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun RecordingRadialMenuNoVbtPreview() {
    var mode by remember { mutableStateOf(RecordingMode()) }

    RecordingRadialMenu(
        isVisible = true,
        isVbtEnabled = false,
        currentMode = mode,
        onModeChanged = { mode = it },
        onDismiss = {},
        onStartRecording = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun RecordButtonWithBadgePreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        // No selection
        RecordButtonWithBadge(
            currentMode = RecordingMode(),
            onClick = {}
        )
        // Metrics selected
        RecordButtonWithBadge(
            currentMode = RecordingMode(showMetrics = true),
            onClick = {}
        )
        // Clean selected
        RecordButtonWithBadge(
            currentMode = RecordingMode(isClean = true),
            onClick = {}
        )
    }
}