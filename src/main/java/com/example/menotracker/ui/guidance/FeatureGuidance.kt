package com.example.menotracker.ui.guidance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * PROMETHEUS FEATURE GUIDANCE SYSTEM - GLASSMORPHISM EDITION
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * Subtiles, nicht-dominantes Design mit Glassmorphism-Ästhetik
 *
 * 1. GLASS HINT - Subtiler Inline-Tipp mit Glass-Effekt
 * 2. FEATURE SPOTLIGHT - Modal mit Glass-Background
 * 3. INTERACTIVE TOUR - Step-by-Step mit Glass-Cards
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */

// Glassmorphism Colors
private val GlassBackground = Color(0xFF1A1A1A).copy(alpha = 0.75f)
private val GlassBorder = Color.White.copy(alpha = 0.08f)
private val GlassHighlight = Color.White.copy(alpha = 0.05f)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFAAAAAA)
private val TextSecondary = Color(0xFF888888)
private val NayaViolet = Color(0xFFA78BFA)
private val SuccessGreen = Color(0xFF27AE60)

// ═══════════════════════════════════════════════════════════════════════════════
// LAYER 1: GLASS HINT - Subtil, elegant, nicht störend
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * GlassHint - Ein subtiler Tipp mit Glassmorphism-Effekt
 *
 * Nicht dominant, erscheint sanft und verschwindet bei Tap
 */
@Composable
fun GlassHint(
    hint: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = NayaViolet
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(400, easing = EaseOutCubic)
        ),
        exit = fadeOut(tween(300)) + slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = tween(300)
        )
    ) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GlassBackground)
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Subtiler pulsierender Punkt
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = pulseAlpha * 0.8f))
            )

            // Hint Text - klein und subtil
            Text(
                text = hint,
                color = TextWhite.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                letterSpacing = 0.2.sp
            )

            // Subtiler Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * GlassHintCompact - Noch subtiler, nur Icon + kurzer Text
 */
@Composable
fun GlassHintCompact(
    hint: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Lightbulb
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f)
    ) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                .clickable { onDismiss() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NayaViolet.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = hint,
                color = TextWhite.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// LAYER 2: FEATURE SPOTLIGHT - Glassmorphism Modal
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * GlassSpotlight - Elegantes Modal mit Glass-Effekt
 *
 * User muss "Verstanden" tippen - aber das Design ist nicht aufdringlich
 */
@Composable
fun GlassSpotlight(
    title: String,
    points: List<SpotlightPoint>,
    isVisible: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = NayaViolet
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Block clicks */ },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A1A).copy(alpha = 0.95f)
                ),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Icon (optional)
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Title
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    // Points
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        points.take(3).forEach { point ->
                            GlassSpotlightPoint(
                                point = point,
                                accentColor = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Confirm Button - subtil aber klar
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.9f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Got it",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

data class SpotlightPoint(
    val icon: ImageVector,
    val text: String
)

@Composable
private fun GlassSpotlightPoint(
    point: SpotlightPoint,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = point.icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = point.text,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// LAYER 3: GLASS TOUR - Step-by-Step mit Glass-Cards
// ═══════════════════════════════════════════════════════════════════════════════

data class TourStep(
    val id: String,
    val title: String,
    val description: String,
    val targetId: String,
    val position: TourPosition = TourPosition.BOTTOM
)

enum class TourPosition {
    TOP, BOTTOM, LEFT, RIGHT
}

@Composable
fun GlassTourOverlay(
    currentStep: TourStep?,
    totalSteps: Int,
    currentIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = currentStep != null,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200))
    ) {
        if (currentStep == null) return@AnimatedVisibility

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Card(
                modifier = Modifier
                    .align(
                        when (currentStep.position) {
                            TourPosition.TOP -> Alignment.TopCenter
                            TourPosition.BOTTOM -> Alignment.BottomCenter
                            TourPosition.LEFT -> Alignment.CenterStart
                            TourPosition.RIGHT -> Alignment.CenterEnd
                        }
                    )
                    .padding(20.dp)
                    .fillMaxWidth(0.92f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GlassBackground
                ),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Progress + Skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step dots
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(totalSteps) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(if (index == currentIndex) 7.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index <= currentIndex) NayaViolet.copy(alpha = 0.8f)
                                            else TextGray.copy(alpha = 0.25f)
                                        )
                                )
                            }
                        }

                        TextButton(onClick = onSkip) {
                            Text(
                                text = "Skip",
                                color = TextGray.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Title
                    Text(
                        text = currentStep.title,
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Description
                    Text(
                        text = currentStep.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // Next Button
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NayaViolet.copy(alpha = 0.85f)
                        )
                    ) {
                        Text(
                            text = if (currentIndex == totalSteps - 1) "Let's go!" else "Next",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// MINI TOOLTIP - Ultra-subtil für kleine Erklärungen
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun GlassTooltip(
    text: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(GlassBackground)
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                .clickable { onDismiss() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                color = TextWhite.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 2,
                lineHeight = 14.sp
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// CONTEXTUAL BADGE - Status mit Erklärung
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun GlassBadge(
    label: String,
    explanation: String,
    modifier: Modifier = Modifier,
    badgeColor: Color = SuccessGreen
) {
    var showExplanation by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor.copy(alpha = 0.12f))
                .border(1.dp, badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .clickable { showExplanation = !showExplanation }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                color = badgeColor.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        GlassTooltip(
            text = explanation,
            isVisible = showExplanation,
            onDismiss = { showExplanation = false },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// LEGACY SUPPORT - Alte Namen für Kompatibilität
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PulseHint(
    hint: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Lightbulb,
    accentColor: Color = NayaViolet
) = GlassHint(hint, isVisible, onDismiss, modifier, accentColor)

@Composable
fun FeatureSpotlight(
    title: String,
    points: List<SpotlightPoint>,
    isVisible: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = NayaViolet
) = GlassSpotlight(title, points, isVisible, onConfirm, modifier, icon, accentColor)

@Composable
fun TourOverlay(
    currentStep: TourStep?,
    totalSteps: Int,
    currentIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) = GlassTourOverlay(currentStep, totalSteps, currentIndex, onNext, onSkip, modifier)

@Composable
fun MiniTooltip(
    text: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = GlassTooltip(text, isVisible, onDismiss, modifier)

@Composable
fun ContextualBadge(
    label: String,
    explanation: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SuccessGreen.copy(alpha = 0.15f),
    textColor: Color = SuccessGreen
) = GlassBadge(label, explanation, modifier, textColor)
