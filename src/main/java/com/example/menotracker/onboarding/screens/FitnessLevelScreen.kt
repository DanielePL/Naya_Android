package com.example.menotracker.onboarding.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.ExperienceLevel
import com.example.menotracker.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Fitness Level Screen
 *
 * Simple 4-option selector replacing complex benchmark inputs.
 * Users can optionally add detailed numbers later in their profile.
 *
 * Key simplification for mass-market appeal:
 * - No intimidating number inputs
 * - No "I don't know my PR" anxiety
 * - Just pick your level and go
 *
 * Features:
 * - Stagger animation on entry
 * - Scale animation on selection
 * - Uses shared OnboardingComponents for consistent design
 */
@Composable
fun FitnessLevelScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedLevel: ExperienceLevel?,
    onSelectLevel: (ExperienceLevel) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onAddDetails: (() -> Unit)? = null
) {
    // Stagger animation state
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        animationTriggered = true
    }

    // Level options with icons and descriptions
    data class LevelOption(
        val level: ExperienceLevel,
        val icon: ImageVector,
        val title: String,
        val description: String
    )

    val levels = listOf(
        LevelOption(
            level = ExperienceLevel.BEGINNER,
            icon = Icons.Default.PlayArrow,
            title = "Just Starting",
            description = "New to training or getting back into it"
        ),
        LevelOption(
            level = ExperienceLevel.INTERMEDIATE,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            title = "Getting Serious",
            description = "1-3 years of consistent training"
        ),
        LevelOption(
            level = ExperienceLevel.EXPERIENCED,
            icon = Icons.Default.Whatshot,
            title = "Experienced",
            description = "3+ years, know my way around"
        ),
        LevelOption(
            level = ExperienceLevel.ELITE,
            icon = Icons.Default.EmojiEvents,
            title = "Competitive",
            description = "Competitions, coaching, serious athlete"
        )
    )

    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "Your fitness level?",
        subtitle = "This helps us personalize your experience",
        onBack = onBack,
        footer = {
            OnboardingPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = selectedLevel != null
            )
        }
    ) {
        // Level cards with stagger animation
        levels.forEachIndexed { index, option ->
            val isSelected = selectedLevel == option.level

            // Stagger animation for each card
            val cardAlpha by animateFloatAsState(
                targetValue = if (animationTriggered) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = index * 80,
                    easing = FastOutSlowInEasing
                ),
                label = "cardAlpha$index"
            )

            val cardOffset by animateFloatAsState(
                targetValue = if (animationTriggered) 0f else 30f,
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = index * 80,
                    easing = FastOutSlowInEasing
                ),
                label = "cardOffset$index"
            )

            // Selection scale animation
            val selectionScale by animateFloatAsState(
                targetValue = if (isSelected) 1.02f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "selectionScale$index"
            )

            OnboardingSelectionCard(
                icon = option.icon,
                title = option.title,
                description = option.description,
                isSelected = isSelected,
                onClick = { onSelectLevel(option.level) },
                selectionType = SelectionType.RADIO,
                modifier = Modifier
                    .alpha(cardAlpha)
                    .offset(y = cardOffset.dp)
                    .scale(selectionScale)
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp)) // 12dp
        }
    }
}
