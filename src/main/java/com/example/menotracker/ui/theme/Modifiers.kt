package com.example.menotracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
// NAYA GLOW EFFECT - Premium Violet Glow
// ═══════════════════════════════════════════════════════════════

/**
 * Adds a violet glow effect behind the composable.
 * Premium glow effect for NAYA brand.
 */
fun Modifier.nayaGlow(
    glowColor: Color = NayaPrimary,
    glowRadius: Dp = 30.dp,
    glowAlpha: Float = 0.3f,
    cornerRadius: Dp = 24.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()

        frameworkPaint.color = glowColor.copy(alpha = 0f).toArgb()
        frameworkPaint.setShadowLayer(
            glowRadius.toPx(),
            0f,
            0f,
            glowColor.copy(alpha = glowAlpha).toArgb()
        )

        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = cornerRadius.toPx(),
            radiusY = cornerRadius.toPx(),
            paint = paint
        )
    }
}

/**
 * Subtle glow for hover/focus states
 */
fun Modifier.nayaGlowSubtle(
    cornerRadius: Dp = 24.dp
): Modifier = nayaGlow(
    glowRadius = 20.dp,
    glowAlpha = 0.2f,
    cornerRadius = cornerRadius
)

/**
 * Intense glow for active/selected states
 */
fun Modifier.nayaGlowIntense(
    cornerRadius: Dp = 24.dp
): Modifier = nayaGlow(
    glowRadius = 40.dp,
    glowAlpha = 0.4f,
    cornerRadius = cornerRadius
)

// Legacy Aliases for compatibility
fun Modifier.menoGlow(
    glowColor: Color = NayaPrimary,
    glowRadius: Dp = 30.dp,
    glowAlpha: Float = 0.3f,
    cornerRadius: Dp = 24.dp
): Modifier = nayaGlow(glowColor, glowRadius, glowAlpha, cornerRadius)

fun Modifier.menoGlowSubtle(
    cornerRadius: Dp = 24.dp
): Modifier = nayaGlowSubtle(cornerRadius)

fun Modifier.menoGlowIntense(
    cornerRadius: Dp = 24.dp
): Modifier = nayaGlowIntense(cornerRadius)

// ═══════════════════════════════════════════════════════════════
// GLASSMORPHISM - Premium Frosted Glass Effects
// ═══════════════════════════════════════════════════════════════

/**
 * Premium glassmorphism surface effect.
 * Creates a frosted glass appearance with subtle highlights and depth.
 * Note: Real blur requires API 31+, using layered transparency as elegant fallback.
 */
fun Modifier.glassBackground(
    cornerRadius: Dp = 24.dp,
    alpha: Float = 0.12f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2A2A2A).copy(alpha = alpha + 0.05f),
                Color(0xFF1A1A1A).copy(alpha = alpha)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.10f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Enhanced glass card - premium frosted effect with inner glow
 */
fun Modifier.glassPremium(
    cornerRadius: Dp = 24.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .drawBehind {
        // Inner shadow/glow for depth
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.3f, size.height * 0.2f),
                radius = size.maxDimension * 0.8f
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF252525).copy(alpha = 0.85f),
                Color(0xFF1C1C1C).copy(alpha = 0.75f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.12f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Glass card with violet accent border
 */
fun Modifier.glassCardAccent(
    cornerRadius: Dp = 24.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF252525).copy(alpha = 0.9f),
                Color(0xFF1C1C1C).copy(alpha = 0.8f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                NayaPrimary.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.15f),
                NayaPrimary.copy(alpha = 0.3f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Glass card with violet glow effect (for Start Workout card)
 */
fun Modifier.glassWithVioletGlow(
    cornerRadius: Dp = 24.dp
): Modifier = this
    .nayaGlow(
        glowColor = NayaPrimary,
        glowRadius = 25.dp,
        glowAlpha = 0.25f,
        cornerRadius = cornerRadius
    )
    .glassPremium(cornerRadius)

// Legacy Alias
fun Modifier.glassWithOrangeGlow(
    cornerRadius: Dp = 24.dp
): Modifier = glassWithVioletGlow(cornerRadius)

/**
 * Elevated glass surface with shadow
 */
fun Modifier.glassElevated(
    cornerRadius: Dp = 24.dp
): Modifier = this
    .nayaGlowSubtle(cornerRadius)
    .glassPremium(cornerRadius)

/**
 * Solid dark surface - for bottom navigation and areas requiring no transparency
 */
fun Modifier.solidDarkBackground(
    cornerRadius: Dp = 0.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color(0xFF141414))

// ═══════════════════════════════════════════════════════════════
// CHAT MESSAGE GLASSMORPHISM
// ═══════════════════════════════════════════════════════════════

/**
 * Glass effect for user message bubbles (sent by current user)
 * Violet-tinted glass with subtle glow
 */
fun Modifier.glassUserMessage(
    shape: RoundedCornerShape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                NayaPrimary.copy(alpha = 0.25f),
                NayaPrimary.copy(alpha = 0.15f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                NayaPrimary.copy(alpha = 0.4f),
                Color.White.copy(alpha = 0.1f)
            )
        ),
        shape = shape
    )

/**
 * Glass effect for AI/Coach message bubbles (received messages)
 * Dark frosted glass with subtle white highlights
 */
fun Modifier.glassAiMessage(
    shape: RoundedCornerShape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2A2A2A).copy(alpha = 0.8f),
                Color(0xFF1E1E1E).copy(alpha = 0.7f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f)
            )
        ),
        shape = shape
    )

/**
 * Glass effect for chat input area
 * Premium frosted panel at bottom of chat
 */
fun Modifier.glassInputArea(
    cornerRadius: Dp = 0.dp
): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E1E1E).copy(alpha = 0.95f),
                Color(0xFF161616).copy(alpha = 0.98f)
            )
        )
    )
    .drawBehind {
        // Top highlight border
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.1f),
                    NayaPrimary.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.1f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx()
        )
    }

// ═══════════════════════════════════════════════════════════════
// NAYA BACKGROUND - Full screen gradient
// ═══════════════════════════════════════════════════════════════

/**
 * Main NAYA app background with subtle violet warmth at top.
 * Dark gradient with subtle violet tint at top.
 */
fun Modifier.nayaBackground(): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1625),  // Warm violet-tinted dark (top)
                Color(0xFF141418),  // Main background with hint of violet
                Color(0xFF121214)   // Solid dark (bottom)
            ),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    )

// Legacy Alias
fun Modifier.menoBackground(): Modifier = nayaBackground()

/**
 * NAYA background brush for use in composables.
 * Returns the standard NAYA gradient brush.
 */
fun nayaBackgroundBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1A1625),  // Warm violet-tinted dark (top)
        Color(0xFF141418),  // Main background with hint of violet
        Color(0xFF121214)   // Solid dark (bottom)
    )
)

// Legacy Alias
fun menoBackgroundBrush(): Brush = nayaBackgroundBrush()
