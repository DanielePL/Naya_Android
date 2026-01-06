package com.example.menotracker.onboarding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.menotracker.ui.theme.*
import com.example.menotracker.ui.theme.AppBackground

/**
 * Shared Onboarding Components
 *
 * Based on BRANDING.md Design System:
 * - Background: Warm gradient (#1A1512 → #141414)
 * - Cards: 16dp radius, glassmorphism
 * - Buttons: 12dp radius, 56dp height
 * - Typography: SpaceGrotesk (headlines), Poppins (body)
 * - Spacing: sm=8dp, md=16dp, lg=24dp, xl=32dp
 */

// ═══════════════════════════════════════════════════════════════════════════
// DESIGN TOKENS (from BRANDING.md)
// ═══════════════════════════════════════════════════════════════════════════

object OnboardingTokens {
    // Spacing
    val spacingSm = 8.dp
    val spacingMd = 16.dp
    val spacingLg = 24.dp
    val spacingXl = 32.dp

    // Shapes
    val radiusSmall = 12.dp   // Buttons, small elements
    val radiusMedium = 16.dp  // Cards
    val radiusLarge = 24.dp   // Large cards, modals

    // Component sizes
    val buttonHeight = 56.dp
    val progressBarHeight = 4.dp
    val iconContainerSize = 48.dp
    val iconSize = 28.dp
    val checkboxSize = 28.dp
    val radioSize = 24.dp

    // Card padding
    val cardPadding = 20.dp
    val cardPaddingCompact = 16.dp
}

// ═══════════════════════════════════════════════════════════════════════════
// BACKGROUND - Uses AppBackground with gradient_bg_dark.jpg
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Onboarding background - uses AppBackground for consistent branding
 * with gradient_bg_dark.jpg brush texture
 */
@Composable
fun OnboardingBackground(
    content: @Composable BoxScope.() -> Unit
) {
    AppBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCAFFOLD (Main Layout Structure)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard onboarding screen scaffold
 * Provides consistent layout: Header → Progress → Title → Content → Footer
 */
@Composable
fun OnboardingScaffold(
    currentStep: Int,
    totalSteps: Int,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    showProgress: Boolean = true,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    OnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = OnboardingTokens.spacingLg)
        ) {
            // Header
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = totalSteps,
                onBack = onBack
            )

            // Progress bar (animated)
            if (showProgress) {
                OnboardingAnimatedProgressBar(
                    currentStep = currentStep,
                    totalSteps = totalSteps
                )
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))

            // Title section
            OnboardingTitle(
                title = title,
                subtitle = subtitle
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingXl))

            // Main content
            Column(
                modifier = Modifier.weight(1f),
                content = content
            )

            // Footer (optional)
            if (footer != null) {
                Column(
                    modifier = Modifier.padding(vertical = OnboardingTokens.spacingMd),
                    content = footer
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PRIMING SCAFFOLD (Value first, then question)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Priming-based onboarding scaffold
 * Shows VALUE/REASON first, then the QUESTION
 * This increases engagement by explaining WHY before asking WHAT
 */
@Composable
fun OnboardingPrimingScaffold(
    currentStep: Int,
    totalSteps: Int,
    valueText: String,           // WHY we're asking (empathetic, benefit-focused)
    questionText: String,        // The actual question
    onBack: () -> Unit,
    showProgress: Boolean = true,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    OnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = OnboardingTokens.spacingLg)
        ) {
            // Header
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = totalSteps,
                onBack = onBack
            )

            // Progress bar (animated)
            if (showProgress) {
                OnboardingAnimatedProgressBar(
                    currentStep = currentStep,
                    totalSteps = totalSteps
                )
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))

            // VALUE/REASON section (priming) - smaller, empathetic text
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = NayaTextSecondary,
                fontFamily = Poppins,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // QUESTION section - bold, prominent
            Text(
                text = questionText,
                style = MaterialTheme.typography.headlineMedium,
                color = NayaTextPrimary,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingXl))

            // Main content
            Column(
                modifier = Modifier.weight(1f),
                content = content
            )

            // Footer (optional)
            if (footer != null) {
                Column(
                    modifier = Modifier.padding(vertical = OnboardingTokens.spacingMd),
                    content = footer
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard header with back button and step counter
 */
@Composable
fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    showStepCounter: Boolean = true
) {
    Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = NayaTextPrimary
            )
        }

        if (showStepCounter) {
            Text(
                text = "Step $currentStep of $totalSteps",
                style = MaterialTheme.typography.labelLarge,
                color = NayaTextTertiary,
                fontFamily = Poppins
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PROGRESS BAR
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard progress indicator
 */
@Composable
fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int
) {
    LinearProgressIndicator(
        progress = { currentStep.toFloat() / totalSteps },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OnboardingTokens.spacingMd)
            .height(OnboardingTokens.progressBarHeight)
            .clip(RoundedCornerShape(2.dp)),
        color = NayaPrimary,
        trackColor = NayaGlass.copy(alpha = 0.3f)
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// TITLE SECTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard title + subtitle section
 */
@Composable
fun OnboardingTitle(
    title: String,
    subtitle: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.displayMedium,
        color = NayaTextPrimary,
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp)) // 12dp

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = NayaTextSecondary,
        fontFamily = Poppins
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// SELECTION CARD (for Goals, Fitness Level, etc.)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard selection card with glassmorphism - TrainingScreen style
 * Uses 24dp radius and glassBackground for consistency with app design
 */
@Composable
fun OnboardingSelectionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = NayaPrimary,
    selectionType: SelectionType = SelectionType.CHECKBOX
) {
    val iconColor = if (isSelected) accentColor else NayaTextSecondary

    // TrainingScreen-style glass card with 24dp radius
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.nayaGlow(
                        glowColor = accentColor,
                        glowRadius = 25.dp,
                        glowAlpha = 0.3f,
                        cornerRadius = OnboardingTokens.radiusLarge
                    )
                } else Modifier
            )
            .glassBackground(cornerRadius = OnboardingTokens.radiusLarge, alpha = 0.4f)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.7f),
                                accentColor.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(OnboardingTokens.radiusLarge)
                    )
                } else Modifier
            )
            .clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(OnboardingTokens.radiusLarge)
    ) {
        Row(
            modifier = Modifier.padding(OnboardingTokens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with accent background
            Box(
                modifier = Modifier
                    .size(OnboardingTokens.iconContainerSize)
                    .clip(RoundedCornerShape(OnboardingTokens.radiusSmall))
                    .background(
                        color = if (isSelected) {
                            accentColor.copy(alpha = 0.2f)
                        } else {
                            NayaTextSecondary.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(OnboardingTokens.iconSize)
                )
            }

            Spacer(modifier = Modifier.width(OnboardingTokens.spacingMd))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NayaTextPrimary,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NayaTextSecondary,
                    fontFamily = Poppins
                )
            }

            Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm))

            // Selection indicator
            when (selectionType) {
                SelectionType.CHECKBOX -> OnboardingCheckbox(
                    isSelected = isSelected,
                    accentColor = accentColor
                )
                SelectionType.RADIO -> OnboardingRadio(
                    isSelected = isSelected,
                    accentColor = accentColor
                )
            }
        }
    }
}

enum class SelectionType {
    CHECKBOX,
    RADIO
}

// ═══════════════════════════════════════════════════════════════════════════
// ICON CONTAINER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Standard icon container with radial gradient
 */
@Composable
fun OnboardingIconContainer(
    icon: ImageVector,
    tint: Color,
    size: Dp = OnboardingTokens.iconContainerSize,
    iconSize: Dp = OnboardingTokens.iconSize
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(OnboardingTokens.radiusSmall))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.25f),
                        tint.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SELECTION INDICATORS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Checkbox indicator for multi-select
 */
@Composable
fun OnboardingCheckbox(
    isSelected: Boolean,
    accentColor: Color = NayaPrimary
) {
    Box(
        modifier = Modifier
            .size(OnboardingTokens.checkboxSize)
            .background(
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (!isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = NayaTextTertiary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = NayaTextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Radio indicator for single-select
 */
@Composable
fun OnboardingRadio(
    isSelected: Boolean,
    accentColor: Color = NayaPrimary
) {
    Box(
        modifier = Modifier
            .size(OnboardingTokens.radioSize)
            .background(
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(OnboardingTokens.radioSize / 2)
            )
            .border(
                width = 2.dp,
                color = if (isSelected) accentColor else NayaTextTertiary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(OnboardingTokens.radioSize / 2)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = NayaTextPrimary,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BUTTONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Primary CTA button
 */
@Composable
fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(OnboardingTokens.buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = NayaPrimary,
            disabledContainerColor = NayaPrimary.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(OnboardingTokens.radiusSmall)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontFamily = Poppins
        )
        if (showArrow) {
            Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

/**
 * Secondary/outlined button
 */
@Composable
fun OnboardingSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(OnboardingTokens.buttonHeight),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = NayaTextPrimary
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(NayaTextTertiary.copy(alpha = 0.5f))
        ),
        shape = RoundedCornerShape(OnboardingTokens.radiusSmall)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = Poppins
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ANIMATED PROGRESS BAR
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Animated progress bar with smooth transitions
 */
@Composable
fun OnboardingAnimatedProgressBar(
    currentStep: Int,
    totalSteps: Int
) {
    val progress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "progress"
    )

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OnboardingTokens.spacingMd)
            .height(OnboardingTokens.progressBarHeight)
            .clip(RoundedCornerShape(2.dp)),
        color = NayaPrimary,
        trackColor = NayaGlass.copy(alpha = 0.3f)
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// SLIDER COMPONENT (for Training Commitment, etc.)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Premium slider with glassmorphism card and dynamic glow
 * Glass container with color glow matching the slider value
 */
@Composable
fun OnboardingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplay: String,
    description: String,
    modifier: Modifier = Modifier,
    valueColor: Color = NayaOrange,
    labels: List<String> = emptyList() // [min, mid, max]
) {
    // Glass card with glow matching the value color
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .nayaGlow(
                glowColor = valueColor,
                glowRadius = 20.dp,
                glowAlpha = 0.25f,
                cornerRadius = OnboardingTokens.radiusLarge
            )
            .glassBackground(cornerRadius = OnboardingTokens.radiusLarge, alpha = 0.35f),
        color = Color.Transparent,
        shape = RoundedCornerShape(OnboardingTokens.radiusLarge)
    ) {
        Column(
            modifier = Modifier.padding(OnboardingTokens.cardPadding)
        ) {
            // Header row with label and value badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NayaTextPrimary,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium
                )
                // Value badge with accent background
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(OnboardingTokens.radiusSmall))
                        .background(valueColor.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = valueDisplay,
                        style = MaterialTheme.typography.headlineSmall,
                        color = valueColor,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Slider
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = valueColor,
                    activeTrackColor = valueColor,
                    inactiveTrackColor = NayaGlass.copy(alpha = 0.5f)
                )
            )

            // Labels row
            if (labels.size >= 3) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { labelText ->
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.bodySmall,
                            color = NayaTextTertiary,
                            fontFamily = Poppins
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Description with subtle background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OnboardingTokens.radiusSmall))
                    .background(NayaGlass.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NayaTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = Poppins
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SEARCH FIELD (for Sport Selection)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Premium search field with glassmorphism
 * Requires minimum 3 characters before showing results
 */
@Composable
fun OnboardingSearchField(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isActive: Boolean,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    hasSelection: Boolean = false,
    leadingIcon: ImageVector = Icons.Default.Search,
    onClear: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    Column(modifier = modifier) {
        // Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = OnboardingTokens.spacingSm)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = NayaTextPrimary,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium
            )
            if (isRequired) {
                Text(
                    text = " *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NayaPrimary,
                    fontFamily = Poppins
                )
            }
        }

        // Search field with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OnboardingTokens.radiusSmall))
                .background(
                    brush = Brush.linearGradient(
                        colors = when {
                            hasSelection -> listOf(
                                NayaPrimary.copy(alpha = 0.15f),
                                Color(0xFF1C1C1C).copy(alpha = 0.9f)
                            )
                            isActive -> listOf(
                                NayaPrimary.copy(alpha = 0.1f),
                                Color(0xFF1C1C1C).copy(alpha = 0.85f)
                            )
                            else -> listOf(
                                Color(0xFF252525).copy(alpha = 0.85f),
                                Color(0xFF1C1C1C).copy(alpha = 0.75f)
                            )
                        }
                    )
                )
                .border(
                    width = if (isActive || hasSelection) 2.dp else 1.dp,
                    brush = Brush.linearGradient(
                        colors = when {
                            hasSelection -> listOf(
                                NayaPrimary.copy(alpha = 0.6f),
                                NayaPrimary.copy(alpha = 0.3f)
                            )
                            isActive -> listOf(
                                NayaPrimary.copy(alpha = 0.8f),
                                NayaPrimary.copy(alpha = 0.4f)
                            )
                            else -> listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(OnboardingTokens.radiusSmall)
                )
                .padding(horizontal = OnboardingTokens.spacingMd, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Leading icon
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (hasSelection) NayaPrimary else NayaTextTertiary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm + 4.dp))

                // Text input
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocusChange(it.isFocused) },
                    textStyle = TextStyle(
                        color = NayaTextPrimary,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontFamily = Poppins
                    ),
                    cursorBrush = SolidColor(NayaPrimary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NayaTextTertiary,
                                    fontFamily = Poppins
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Clear button
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = NayaTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Hint text when typing but not enough chars
        if (query.isNotEmpty() && query.length < 3 && !hasSelection && isActive) {
            Text(
                text = "Type ${3 - query.length} more character${if (3 - query.length > 1) "s" else ""}...",
                style = MaterialTheme.typography.bodySmall,
                color = NayaTextTertiary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// VIDEO SHOWCASE (for Feature Promo Screen)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Feature showcase with animated placeholder
 * Square aspect ratio with glassmorphism frame
 */
@Composable
fun OnboardingVideoShowcase(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video/Animation container - square aspect ratio
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .nayaGlow(
                    glowColor = NayaPrimary,
                    glowRadius = 30.dp,
                    glowAlpha = 0.3f,
                    cornerRadius = OnboardingTokens.radiusLarge
                )
                .clip(RoundedCornerShape(OnboardingTokens.radiusLarge))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF252525).copy(alpha = 0.9f),
                            Color(0xFF1C1C1C).copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NayaPrimary.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.15f),
                            NayaPrimary.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(OnboardingTokens.radiusLarge)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder with pulsing animation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.scale(pulseScale).alpha(pulseAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = NayaPrimary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm))
                Text(
                    text = "Video Coming Soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NayaTextTertiary,
                    fontFamily = Poppins
                )
            }
        }

        Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = NayaTextPrimary,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = NayaTextSecondary,
            fontFamily = Poppins,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = OnboardingTokens.spacingMd)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FEATURE SHOWCASE SCAFFOLD (for Feature Promo Screen)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Scaffold variant for feature showcase screens
 * No step counter, centered content
 */
@Composable
fun OnboardingShowcaseScaffold(
    onBack: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    OnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = OnboardingTokens.spacingLg)
        ) {
            // Header without step counter
            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NayaTextPrimary
                    )
                }
            }

            // Main content - centered
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = content
            )

            // Footer
            Column(
                modifier = Modifier.padding(vertical = OnboardingTokens.spacingMd),
                content = footer
            )
        }
    }
}
