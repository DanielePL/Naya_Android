package com.example.menotracker.onboarding.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.CoachSituation
import kotlinx.coroutines.delay

/**
 * Coach Situation Screen
 *
 * Asks experienced/elite athletes about their coaching situation.
 * Uses glassmorphism design consistent with other onboarding screens.
 *
 * Options:
 * - Has a coach (connect them)
 * - Use AI Coach (Naya coaches)
 * - Self-coached (independent)
 */
@Composable
fun CoachSituationScreen(
    currentStep: Int,
    totalSteps: Int,
    onSituationSelected: (CoachSituation) -> Unit,
    onBack: () -> Unit
) {
    // Stagger animation for cards
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        animationTriggered = true
    }

    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "Do You Work With a Coach?",
        subtitle = "We'll tailor the experience to your setup",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingMd)
        ) {
            CoachSituation.entries.forEachIndexed { index, situation ->
                // Staggered animation per card
                val cardAlpha by animateFloatAsState(
                    targetValue = if (animationTriggered) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 350,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    ),
                    label = "cardAlpha$index"
                )

                OnboardingSelectionCard(
                    icon = getCoachIcon(situation),
                    title = situation.displayName,
                    description = situation.description,
                    isSelected = false, // No pre-selection, tap to select and continue
                    onClick = { onSituationSelected(situation) },
                    selectionType = SelectionType.RADIO,
                    modifier = Modifier.alpha(cardAlpha)
                )
            }
        }
    }
}

private fun getCoachIcon(situation: CoachSituation): ImageVector {
    return when (situation) {
        CoachSituation.HAS_COACH -> Icons.Default.Person
        CoachSituation.USE_AI_COACH -> Icons.Default.Psychology
        CoachSituation.SELF_COACHED -> Icons.Default.SelfImprovement
    }
}