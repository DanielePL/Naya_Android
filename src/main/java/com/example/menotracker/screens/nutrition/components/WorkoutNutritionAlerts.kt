package com.example.menotracker.screens.nutrition.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════
// PROMETHEUS COLORS
// ═══════════════════════════════════════════════════════════════

private val NayaOrange = Color(0xFFE67E22)
private val Surface = Color(0xFF1C1C1C)
private val SurfaceVariant = Color(0xFF262626)
private val TextPrimary = Color(0xFFFAFAFA)
private val TextSecondary = Color(0xFF999999)
private val GlassBase = Color(0xFF333333)

// Alert colors
private val AlertCritical = Color(0xFFEF4444)
private val AlertHigh = Color(0xFFF97316)
private val AlertMedium = Color(0xFFFBBF24)
private val AlertLow = Color(0xFF10B981)
private val AlertNone = Color(0xFF6B7280)

// Macro colors
private val ProteinBlue = Color(0xFF3B82F6)
private val CarbsGreen = Color(0xFF10B981)
private val FatYellow = Color(0xFFFBBF24)

// ═══════════════════════════════════════════════════════════════
// PRE-WORKOUT ALERT CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun PreWorkoutAlertCard(
    state: PreWorkoutNutritionState,
    onLogMealClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't show if no action needed
    if (state.recommendedAction == PreWorkoutAction.NO_WORKOUT_PREDICTED ||
        state.recommendedAction == PreWorkoutAction.WELL_FUELED ||
        state.urgency == AlertUrgency.NONE) {
        return
    }

    val alertColor = getAlertColor(state.urgency)
    val isPulsing = state.urgency == AlertUrgency.CRITICAL || state.urgency == AlertUrgency.HIGH

    // Pulse animation for urgent alerts
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isPulsing) {
                    Modifier.border(
                        width = 2.dp,
                        color = alertColor.copy(alpha = pulseAlpha),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = GlassBase.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Urgency icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(alertColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAlertIcon(state.urgency),
                            contentDescription = null,
                            tint = alertColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Pre-Workout Nutrition",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        state.hoursUntilWorkout?.let { hours ->
                            Text(
                                text = "Workout in ${formatHours(hours)}",
                                fontSize = 12.sp,
                                color = alertColor
                            )
                        }
                    }
                }

                // Dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Message
            Text(
                text = state.message,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            // Macro recommendations (if applicable)
            if (state.recommendedAction != PreWorkoutAction.TOO_LATE_TO_EAT &&
                state.recommendedAction != PreWorkoutAction.WORKOUT_IN_PROGRESS) {

                Spacer(modifier = Modifier.height(12.dp))

                MacroRecommendationRow(
                    protein = state.proteinRecommendation,
                    carbs = state.carbRecommendation,
                    fat = state.fatRecommendation
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Log meal button
                Button(
                    onClick = onLogMealClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NayaOrange
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Meal", fontSize = 13.sp)
                }

                // Remind later button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(TextSecondary.copy(alpha = 0.5f), TextSecondary.copy(alpha = 0.5f)))
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Later", fontSize = 13.sp)
                }
            }

            // Confidence indicator
            if (state.confidence < 0.7f) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Based on ${(state.confidence * 100).toInt()}% pattern confidence",
                    fontSize = 10.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// POST-WORKOUT ALERT CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun PostWorkoutAlertCard(
    state: PostWorkoutNutritionState,
    onLogMealClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't show if window closed or target met
    if (state.windowPhase == PostWorkoutPhase.CLOSED ||
        state.urgency == AlertUrgency.NONE) {
        return
    }

    val alertColor = getAlertColor(state.urgency)
    val isPulsing = state.urgency == AlertUrgency.CRITICAL

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isPulsing) {
                    Modifier.border(
                        width = 2.dp,
                        color = alertColor.copy(alpha = pulseAlpha),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = GlassBase.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(alertColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = alertColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Anabolic Window",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = getPhaseText(state.windowPhase, state.minutesSinceWorkout),
                            fontSize = 12.sp,
                            color = alertColor
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Message
            Text(
                text = state.message,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            ProteinProgressBar(
                consumed = state.proteinConsumedSince,
                target = state.proteinTarget,
                color = ProteinBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Log meal button
            Button(
                onClick = onLogMealClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = alertColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Post-Workout Meal", fontSize = 13.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// COMPACT WORKOUT STATUS CHIP
// ═══════════════════════════════════════════════════════════════

@Composable
fun WorkoutStatusChip(
    preWorkoutState: PreWorkoutNutritionState?,
    postWorkoutState: PostWorkoutNutritionState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine which state to show
    val (text, icon, color) = when {
        postWorkoutState != null && postWorkoutState.windowPhase != PostWorkoutPhase.CLOSED -> {
            Triple(
                "Anabolic ${postWorkoutState.minutesSinceWorkout}min",
                Icons.Default.Bolt,
                getAlertColor(postWorkoutState.urgency)
            )
        }
        preWorkoutState != null && preWorkoutState.urgency != AlertUrgency.NONE -> {
            Triple(
                "Pre-workout ${formatHoursShort(preWorkoutState.hoursUntilWorkout ?: 0f)}",
                Icons.Default.FitnessCenter,
                getAlertColor(preWorkoutState.urgency)
            )
        }
        else -> return // Don't show chip if nothing relevant
    }

    Surface(
        modifier = modifier.clickable { onClick() },
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MacroRecommendationRow(
    protein: IntRange,
    carbs: IntRange,
    fat: IntRange
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SurfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroChip(
            label = "Protein",
            range = protein,
            unit = "g",
            color = ProteinBlue
        )
        MacroChip(
            label = "Carbs",
            range = carbs,
            unit = "g",
            color = CarbsGreen
        )
        MacroChip(
            label = "Fat",
            range = fat,
            unit = "g",
            color = FatYellow
        )
    }
}

@Composable
private fun MacroChip(
    label: String,
    range: IntRange,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
        Text(
            text = "${range.first}-${range.last}$unit",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ProteinProgressBar(
    consumed: Float,
    target: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Protein Progress",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Text(
                text = "${consumed.toInt()}g / ${target.toInt()}g",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((consumed / target).coerceIn(0f, 1f))
                    .background(color)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun getAlertColor(urgency: AlertUrgency): Color = when (urgency) {
    AlertUrgency.CRITICAL -> AlertCritical
    AlertUrgency.HIGH -> AlertHigh
    AlertUrgency.MEDIUM -> AlertMedium
    AlertUrgency.LOW -> AlertLow
    AlertUrgency.NONE -> AlertNone
}

private fun getAlertIcon(urgency: AlertUrgency): ImageVector = when (urgency) {
    AlertUrgency.CRITICAL -> Icons.Default.Warning
    AlertUrgency.HIGH -> Icons.Default.PriorityHigh
    AlertUrgency.MEDIUM -> Icons.Default.Info
    AlertUrgency.LOW -> Icons.Default.LightMode
    AlertUrgency.NONE -> Icons.Default.CheckCircle
}

private fun formatHours(hours: Float): String = when {
    hours < 1f -> "${(hours * 60).toInt()}min"
    hours < 2f -> "~${hours.toInt()}h ${((hours - hours.toInt()) * 60).toInt()}min"
    else -> "~${hours.toInt()}h"
}

private fun formatHoursShort(hours: Float): String = when {
    hours < 1f -> "${(hours * 60).toInt()}m"
    else -> "${hours.toInt()}h"
}

private fun getPhaseText(phase: PostWorkoutPhase, minutes: Long): String = when (phase) {
    PostWorkoutPhase.IMMEDIATE -> "Critical window - ${30 - minutes}min left"
    PostWorkoutPhase.OPTIMAL -> "Optimal window - ${120 - minutes}min left"
    PostWorkoutPhase.EXTENDED -> "Extended window"
    PostWorkoutPhase.CLOSED -> "Window closed"
}