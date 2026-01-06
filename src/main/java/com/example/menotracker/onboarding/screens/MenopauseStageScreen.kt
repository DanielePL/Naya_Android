package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.OnboardingMenopauseStage

/**
 * Menopause Stage Selection Screen
 *
 * Asks the user which menopause stage they're currently in.
 * This helps personalize symptom tracking and recommendations.
 */
@Composable
fun MenopauseStageScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedStage: OnboardingMenopauseStage?,
    onStageSelected: (OnboardingMenopauseStage) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingPrimingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        valueText = "Every woman's journey is unique. Understanding where you are helps us provide guidance that actually fits your life.",
        questionText = "Where are you in your journey?",
        onBack = onBack,
        footer = {
            OnboardingPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = selectedStage != null
            )
        }
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingMd),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(OnboardingMenopauseStage.entries) { stage ->
                OnboardingSelectionCard(
                    icon = getStageIcon(stage),
                    title = stage.displayName,
                    description = stage.description,
                    isSelected = selectedStage == stage,
                    onClick = { onStageSelected(stage) },
                    selectionType = SelectionType.RADIO
                )
            }
        }
    }
}

/**
 * Get icon for each menopause stage
 */
private fun getStageIcon(stage: OnboardingMenopauseStage): ImageVector {
    return when (stage) {
        OnboardingMenopauseStage.PREMENOPAUSE -> Icons.Default.CalendarToday
        OnboardingMenopauseStage.EARLY_PERIMENOPAUSE -> Icons.Default.Schedule
        OnboardingMenopauseStage.LATE_PERIMENOPAUSE -> Icons.Default.HourglassBottom
        OnboardingMenopauseStage.MENOPAUSE -> Icons.Default.CheckCircle
        OnboardingMenopauseStage.POSTMENOPAUSE -> Icons.Default.Verified
        OnboardingMenopauseStage.UNSURE -> Icons.Default.Help
    }
}

