package com.example.menotracker.screens.breathing

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.BreathingPhase
import com.example.menotracker.data.models.BreathingSessionState

// Colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val tealAccent = Color(0xFF14B8A6)
private val pinkAccent = Color(0xFFEC4899)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)

/**
 * Animated breathing circle that expands/contracts based on breathing phase
 */
@Composable
fun BreathingAnimationCircle(
    sessionState: BreathingSessionState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = sessionState.progress,
        animationSpec = tween(
            durationMillis = 100,
            easing = LinearEasing
        ),
        label = "breathProgress"
    )

    // Glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow circle
        Canvas(modifier = Modifier.size(280.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = size.minDimension / 2
            val currentRadius = maxRadius * animatedProgress

            // Glow effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        getPhaseColor(sessionState.currentPhase).copy(alpha = glowAlpha * 0.5f),
                        getPhaseColor(sessionState.currentPhase).copy(alpha = 0f)
                    ),
                    center = Offset(centerX, centerY),
                    radius = currentRadius * 1.3f
                ),
                radius = currentRadius * 1.3f,
                center = Offset(centerX, centerY)
            )

            // Main circle with gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        getPhaseColor(sessionState.currentPhase).copy(alpha = 0.8f),
                        getPhaseColor(sessionState.currentPhase).copy(alpha = 0.4f)
                    ),
                    center = Offset(centerX, centerY),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = Offset(centerX, centerY)
            )

            // Inner ring
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = currentRadius * 0.85f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Phase instruction
            Text(
                text = sessionState.currentPhase.instruction,
                color = textWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Countdown
            if (sessionState.phaseSecondsRemaining > 0) {
                Text(
                    text = "${sessionState.phaseSecondsRemaining}",
                    color = textWhite.copy(alpha = 0.8f),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cycle indicator
            Text(
                text = "Cycle ${sessionState.currentCycle} of ${sessionState.totalCycles}",
                color = textGray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Simple breathing circle for exercise preview
 */
@Composable
fun BreathingPreviewCircle(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "preview")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "previewScale"
    )

    Canvas(modifier = modifier.size(60.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = (size.minDimension / 2) * scale

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.8f),
                    color.copy(alpha = 0.3f)
                ),
                center = Offset(centerX, centerY),
                radius = radius
            ),
            radius = radius,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * Get color based on breathing phase
 */
private fun getPhaseColor(phase: BreathingPhase): Color {
    return when (phase) {
        BreathingPhase.INHALE -> tealAccent
        BreathingPhase.HOLD_IN -> lavenderLight
        BreathingPhase.EXHALE -> pinkAccent
        BreathingPhase.HOLD_OUT -> lavenderPrimary
        BreathingPhase.REST -> Color(0xFF10B981)
    }
}

/**
 * Phase indicator dots
 */
@Composable
fun PhaseIndicator(
    currentPhase: BreathingPhase,
    hasHoldOut: Boolean,
    modifier: Modifier = Modifier
) {
    val phases = if (hasHoldOut) {
        listOf(
            BreathingPhase.INHALE,
            BreathingPhase.HOLD_IN,
            BreathingPhase.EXHALE,
            BreathingPhase.HOLD_OUT
        )
    } else {
        listOf(
            BreathingPhase.INHALE,
            BreathingPhase.HOLD_IN,
            BreathingPhase.EXHALE
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        phases.forEach { phase ->
            val isActive = phase == currentPhase
            val color = getPhaseColor(phase)

            Canvas(modifier = Modifier.size(if (isActive) 12.dp else 8.dp)) {
                drawCircle(
                    color = color.copy(alpha = if (isActive) 1f else 0.4f),
                    radius = size.minDimension / 2
                )
            }
        }
    }
}
