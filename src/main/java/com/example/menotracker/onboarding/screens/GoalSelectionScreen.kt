package com.example.menotracker.onboarding.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.PrimaryGoal
import com.example.menotracker.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Goal Selection Screen
 *
 * 3 main goals + "All of the Above":
 * - Aesthetics - Gym bros, weight loss, body recomp
 * - Strength - Powerlifters, strength athletes
 * - Sport Performance - Athletes, competitors
 * - All of the Above - Serious athletes
 *
 * Multiple selection allowed - covers hybrid athletes
 *
 * Features:
 * - Stagger animation on entry (cards appear one by one)
 * - Scale animation on selection
 * - Uses shared OnboardingComponents for consistent design
 */
@Composable
fun GoalSelectionScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedGoals: List<PrimaryGoal>,
    onToggleGoal: (PrimaryGoal) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    // Stagger animation state
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100) // Small delay before starting animations
        animationTriggered = true
    }

    // Map goals to icons
    fun getIconForGoal(goal: PrimaryGoal): ImageVector = when (goal) {
        PrimaryGoal.BODY_COMP -> Icons.Default.Person
        PrimaryGoal.STRENGTH -> Icons.Default.FitnessCenter
        PrimaryGoal.PERFORMANCE -> Icons.Default.EmojiEvents
        PrimaryGoal.COMPLETE -> Icons.Default.Bolt
    }

    // Goals in strategic order: most common first, "all" last
    val goals = listOf(
        PrimaryGoal.BODY_COMP,      // Aesthetics - most gym-goers
        PrimaryGoal.STRENGTH,       // Strength
        PrimaryGoal.PERFORMANCE,    // Sport Performance
        PrimaryGoal.COMPLETE        // All of the Above
    )

    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "What's your goal?",
        subtitle = "Select all that apply",
        onBack = onBack,
        footer = {
            OnboardingPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = selectedGoals.isNotEmpty()
            )
        }
    ) {
        // Goal cards with stagger animation
        goals.forEachIndexed { index, goal ->
            val isAllOption = goal == PrimaryGoal.COMPLETE
            val isSelected = selectedGoals.contains(goal)

            // Stagger animation for each card
            val cardAlpha by animateFloatAsState(
                targetValue = if (animationTriggered) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = index * 80, // 80ms stagger between cards
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
                icon = getIconForGoal(goal),
                title = goal.displayName,
                description = goal.description,
                isSelected = isSelected,
                onClick = { onToggleGoal(goal) },
                accentColor = if (isAllOption) NayaOrange else NayaPrimary,
                selectionType = SelectionType.CHECKBOX,
                modifier = Modifier
                    .alpha(cardAlpha)
                    .offset(y = cardOffset.dp)
                    .scale(selectionScale)
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp)) // 12dp
        }
    }
}
