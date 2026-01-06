package com.example.menotracker.onboarding.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.TrainingCommitment
import com.example.menotracker.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Training Commitment Screen
 *
 * Collects user's training frequency and effort level.
 * Uses OnboardingSlider components for consistent design.
 *
 * Features:
 * - Stagger animation on entry
 * - Dynamic color for effort slider
 * - Contextual descriptions
 */
@Composable
fun TrainingCommitmentScreen(
    currentStep: Int,
    totalSteps: Int,
    commitment: TrainingCommitment,
    onUpdateCommitment: (TrainingCommitment) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    var sessionsPerWeek by remember { mutableFloatStateOf(commitment.sessionsPerWeek.toFloat()) }
    var effortLevel by remember { mutableFloatStateOf(commitment.effortLevel.toFloat()) }

    // Stagger animation state
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        animationTriggered = true
    }

    // Animate sliders appearing
    val slider1Alpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(400, delayMillis = 0, easing = FastOutSlowInEasing),
        label = "slider1Alpha"
    )
    val slider2Alpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(400, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "slider2Alpha"
    )

    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "How Much Can You Commit?",
        subtitle = "Be honest - this helps us calculate a realistic timeline",
        onBack = onBack,
        footer = {
            OnboardingPrimaryButton(
                text = "Continue",
                onClick = {
                    onUpdateCommitment(
                        TrainingCommitment(
                            sessionsPerWeek = sessionsPerWeek.roundToInt(),
                            effortLevel = effortLevel.roundToInt()
                        )
                    )
                    onContinue()
                }
            )
        }
    ) {
        // Vertically center both sliders with equal spacing
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Sessions per week slider (up to 12 for competitive athletes)
            OnboardingSlider(
                label = "Training Sessions / Week",
                value = sessionsPerWeek,
                onValueChange = { sessionsPerWeek = it },
                valueRange = 1f..12f,
                steps = 10,
                valueDisplay = "${sessionsPerWeek.roundToInt()}x",
                description = getFrequencyDescription(sessionsPerWeek.roundToInt()),
                valueColor = NayaOrange,
                labels = listOf("1x", "6x", "12x"),
                modifier = Modifier.alpha(slider1Alpha)
            )

            // Effort level slider
            OnboardingSlider(
                label = "Effort Level",
                value = effortLevel,
                onValueChange = { effortLevel = it },
                valueRange = 1f..10f,
                steps = 8,
                valueDisplay = "${effortLevel.roundToInt()}/10",
                description = getEffortDescription(effortLevel.roundToInt()),
                valueColor = getEffortColor(effortLevel.roundToInt()),
                labels = listOf("Casual", "Moderate", "All In"),
                modifier = Modifier.alpha(slider2Alpha)
            )
        }
    }
}

private fun getFrequencyDescription(sessions: Int): String {
    return when (sessions) {
        1 -> "Maintenance mode - slow but steady"
        2 -> "Light schedule - good for busy weeks"
        3 -> "Balanced - solid progress"
        4 -> "Optimal - great for most goals"
        5 -> "High frequency - faster results"
        6 -> "Serious training - prioritize recovery"
        7 -> "Athlete schedule - double sessions starting"
        8 -> "High volume - pro-level commitment"
        9 -> "Elite territory - multiple daily sessions"
        10 -> "Olympic prep - full-time athlete mode"
        11 -> "Peak phase - max sustainable volume"
        12 -> "Competition mode - train like it's your job"
        else -> ""
    }
}

private fun getEffortDescription(effort: Int): String {
    return when (effort) {
        in 1..3 -> "Taking it easy - progress will be gradual"
        in 4..5 -> "Moderate intensity - steady gains"
        in 6..7 -> "Pushing hard - expect solid results"
        in 8..9 -> "High intensity - fast progress if you recover"
        10 -> "Maximum effort - train like a pro"
        else -> ""
    }
}

private fun getEffortColor(effort: Int): Color {
    return when (effort) {
        in 1..3 -> Color(0xFF4CAF50) // Green
        in 4..5 -> Color(0xFF8BC34A) // Light green
        in 6..7 -> Color(0xFFFFC107) // Yellow/Orange
        in 8..9 -> Color(0xFFFF9800) // Orange
        10 -> Color(0xFFF44336) // Red
        else -> Color.White
    }
}
