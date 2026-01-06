package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.MenopauseGoal
import com.example.menotracker.ui.theme.Poppins
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaTextSecondary

/**
 * Menopause Goals Selection Screen
 *
 * Allows multi-selection of wellness goals.
 * This helps personalize the app experience and AI coach recommendations.
 */
@Composable
fun MenopauseGoalsScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedGoals: List<MenopauseGoal>,
    onGoalToggled: (MenopauseGoal) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingPrimingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        valueText = "Your priorities matter. We'll focus on what's most important to you and create a plan that fits your lifestyle.",
        questionText = "What matters most to you right now?",
        onBack = onBack,
        footer = {
            // Show selection count
            if (selectedGoals.isNotEmpty()) {
                Text(
                    text = "${selectedGoals.size}/3 goals selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedGoals.size == 3) {
                        NayaPrimary
                    } else {
                        NayaTextSecondary
                    },
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = OnboardingTokens.spacingSm)
                )
            }

            OnboardingPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = selectedGoals.isNotEmpty()
            )
        }
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingSm),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MenopauseGoal.entries) { goal ->
                val isSelected = goal in selectedGoals
                val isMaxReached = selectedGoals.size >= 3 && !isSelected

                OnboardingSelectionCard(
                    icon = getGoalIcon(goal),
                    title = goal.displayName,
                    description = goal.description,
                    isSelected = isSelected,
                    onClick = {
                        if (!isMaxReached || isSelected) {
                            onGoalToggled(goal)
                        }
                    },
                    selectionType = SelectionType.CHECKBOX
                )
            }
        }
    }
}

/**
 * Get icon for each goal
 */
private fun getGoalIcon(goal: MenopauseGoal): ImageVector {
    return when (goal) {
        MenopauseGoal.SYMPTOM_RELIEF -> Icons.Default.Healing
        MenopauseGoal.BONE_HEALTH -> Icons.Default.AccessibilityNew
        MenopauseGoal.WEIGHT_MANAGEMENT -> Icons.Default.MonitorWeight
        MenopauseGoal.ENERGY_VITALITY -> Icons.Default.Bolt
        MenopauseGoal.SLEEP_QUALITY -> Icons.Default.Bedtime
        MenopauseGoal.STRESS_MOOD -> Icons.Default.SelfImprovement
        MenopauseGoal.HORMONE_BALANCE -> Icons.Default.Balance
    }
}

