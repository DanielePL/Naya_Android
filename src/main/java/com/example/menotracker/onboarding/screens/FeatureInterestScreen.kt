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
import com.example.menotracker.onboarding.data.FeatureInterest
import kotlinx.coroutines.delay

/**
 * Feature Interest Screen - Discover what features user needs
 * "What Do You Want to Improve?"
 *
 * Uses OnboardingScaffold and OnboardingSelectionCard for consistent design.
 */
@Composable
fun FeatureInterestScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedInterests: List<FeatureInterest>,
    onToggleInterest: (FeatureInterest) -> Unit,
    onContinue: () -> Unit,
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
        title = "What Do You Want to Improve?",
        subtitle = "Select all that apply - we'll personalize your experience",
        onBack = onBack,
        footer = {
            OnboardingPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = selectedInterests.isNotEmpty()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingMd)
        ) {
            FeatureInterest.entries.forEachIndexed { index, interest ->
                // Staggered animation per card
                val cardAlpha by animateFloatAsState(
                    targetValue = if (animationTriggered) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 350,
                        delayMillis = index * 80,
                        easing = FastOutSlowInEasing
                    ),
                    label = "cardAlpha$index"
                )

                OnboardingSelectionCard(
                    icon = getInterestIcon(interest),
                    title = interest.displayName,
                    description = interest.description,
                    isSelected = selectedInterests.contains(interest),
                    onClick = { onToggleInterest(interest) },
                    selectionType = SelectionType.CHECKBOX,
                    modifier = Modifier.alpha(cardAlpha)
                )
            }
        }
    }
}

private fun getInterestIcon(interest: FeatureInterest): ImageVector {
    return when (interest) {
        FeatureInterest.NUTRITION -> Icons.Default.Restaurant
        FeatureInterest.AI_COACH -> Icons.Default.Psychology
        FeatureInterest.TEMPLATES -> Icons.Default.LibraryBooks
    }
}