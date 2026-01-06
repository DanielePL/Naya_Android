package com.example.menotracker.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import com.example.menotracker.ui.theme.NayaPrimary

// ═══════════════════════════════════════════════════════════════
// COLORS
// ═══════════════════════════════════════════════════════════════

private val SuccessGreen = Color(0xFF00FF88)
private val WarningYellow = Color(0xFFFFD93D)
private val ErrorRed = Color(0xFFFF4757)
private val NayaOrange = NayaPrimary
private val DarkBgTransparent = Color(0xCC1A1410)

// Tolerances in degrees
private const val LEVEL_TOLERANCE = 2.0f
private const val WARNING_TOLERANCE = 5.0f

/**
 * Compact Leveling Overlay for Camera Preview
 * Shows a bubble level indicator to help align the camera
 */
@Composable
fun LevelingOverlay(
    modifier: Modifier = Modifier,
    showValues: Boolean = true
) {
    val context = LocalContext.current

    // Sensor state
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }

    // Register sensor listener
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // For VERTICAL phone orientation (portrait mode):
                    // When phone is held upright and level:
                    // - x ≈ 0 (no tilt left/right)
                    // - y ≈ -9.8 (gravity pulling down)
                    // - z ≈ 0 (phone facing forward)

                    // Roll: Left/Right tilt (x-axis deviation)
                    roll = atan2(x, sqrt(y * y + z * z)) * (180 / Math.PI).toFloat()

                    // Pitch: Forward/Backward tilt (z-axis deviation)
                    // For vertical phone, we want to measure how much it's tilted forward/back
                    pitch = atan2(z, sqrt(x * x + y * y)) * (180 / Math.PI).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val isLevel = abs(pitch) <= LEVEL_TOLERANCE && abs(roll) <= LEVEL_TOLERANCE
    val maxAngle = maxOf(abs(pitch), abs(roll))

    Box(modifier = modifier) {
        CompactBubbleLevel(
            pitch = pitch,
            roll = roll,
            isLevel = isLevel,
            showValues = showValues
        )
    }
}

@Composable
private fun CompactBubbleLevel(
    pitch: Float,
    roll: Float,
    isLevel: Boolean,
    showValues: Boolean,
    modifier: Modifier = Modifier
) {
    // Animate bubble position
    val animatedPitch by animateFloatAsState(
        targetValue = pitch.coerceIn(-15f, 15f),
        animationSpec = spring(dampingRatio = 0.7f),
        label = "pitch"
    )
    val animatedRoll by animateFloatAsState(
        targetValue = roll.coerceIn(-15f, 15f),
        animationSpec = spring(dampingRatio = 0.7f),
        label = "roll"
    )

    val maxAngle = maxOf(abs(pitch), abs(roll))

    val bubbleColor by animateColorAsState(
        targetValue = when {
            isLevel -> SuccessGreen
            maxAngle <= WARNING_TOLERANCE -> WarningYellow
            else -> ErrorRed
        },
        label = "bubbleColor"
    )

    Column(
        modifier = modifier
            .background(DarkBgTransparent, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini bubble level
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1410))
                .border(2.dp, bubbleColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val maxRadius = size.minDimension / 2 - 8.dp.toPx()

                // Target zone (green circle in center)
                val targetRadius = maxRadius * 0.35f
                drawCircle(
                    color = SuccessGreen.copy(alpha = 0.2f),
                    radius = targetRadius,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = SuccessGreen.copy(alpha = 0.5f),
                    radius = targetRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Crosshairs
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(centerX, centerY - maxRadius),
                    end = Offset(centerX, centerY + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(centerX - maxRadius, centerY),
                    end = Offset(centerX + maxRadius, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                // Bubble position
                val bubbleX = centerX + (animatedRoll / 15f) * maxRadius
                val bubbleY = centerY + (animatedPitch / 15f) * maxRadius
                val bubbleRadius = 8.dp.toPx()

                // Bubble shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = bubbleRadius,
                    center = Offset(bubbleX + 1.dp.toPx(), bubbleY + 1.dp.toPx())
                )

                // Bubble
                drawCircle(
                    color = bubbleColor,
                    radius = bubbleRadius,
                    center = Offset(bubbleX, bubbleY)
                )

                // Bubble highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = bubbleRadius * 0.3f,
                    center = Offset(bubbleX - bubbleRadius * 0.3f, bubbleY - bubbleRadius * 0.3f)
                )
            }
        }

        if (showValues) {
            Spacer(modifier = Modifier.height(4.dp))

            // Status text
            Text(
                text = when {
                    isLevel -> "LEVEL ✓"
                    maxAngle <= WARNING_TOLERANCE -> "ADJUST"
                    else -> "TILT!"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = bubbleColor
            )

            // Angle values
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "P:${String.format("%.1f", pitch)}°",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "R:${String.format("%.1f", roll)}°",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Minimal leveling indicator (just the bubble, no text)
 */
@Composable
fun MinimalLevelingIndicator(
    modifier: Modifier = Modifier
) {
    LevelingOverlay(
        modifier = modifier,
        showValues = false
    )
}