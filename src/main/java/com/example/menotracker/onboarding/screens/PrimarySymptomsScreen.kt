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
import com.example.menotracker.onboarding.data.OnboardingSymptom
import com.example.menotracker.ui.theme.Poppins
import com.example.menotracker.ui.theme.NayaTextSecondary

/**
 * Primary Symptoms Selection Screen
 *
 * Allows multi-selection of current menopause symptoms.
 * This helps personalize tracking and AI coach recommendations.
 */
@Composable
fun PrimarySymptomsScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedSymptoms: List<OnboardingSymptom>,
    onSymptomToggled: (OnboardingSymptom) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingPrimingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        valueText = "You're not alone in this. Knowing what you're experiencing helps us offer support that actually makes a difference.",
        questionText = "What are you experiencing?",
        onBack = onBack,
        footer = {
            // Show selection count
            if (selectedSymptoms.isNotEmpty() && OnboardingSymptom.NONE !in selectedSymptoms) {
                Text(
                    text = "${selectedSymptoms.size} symptom${if (selectedSymptoms.size > 1) "s" else ""} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NayaTextSecondary,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = OnboardingTokens.spacingSm)
                )
            }

            OnboardingPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = selectedSymptoms.isNotEmpty()
            )
        }
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingSm),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Filter out NONE for the main list, show it last
            val mainSymptoms = OnboardingSymptom.entries.filter { it != OnboardingSymptom.NONE }

            items(mainSymptoms) { symptom ->
                val isSelected = symptom in selectedSymptoms
                val isDisabled = OnboardingSymptom.NONE in selectedSymptoms

                OnboardingSelectionCard(
                    icon = getSymptomIcon(symptom),
                    title = symptom.displayName,
                    description = symptom.description,
                    isSelected = isSelected,
                    onClick = {
                        if (!isDisabled) {
                            onSymptomToggled(symptom)
                        }
                    },
                    selectionType = SelectionType.CHECKBOX
                )
            }

            // Spacer before "None" option
            item {
                Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
            }

            // "None/Other" option - exclusive selection
            item {
                val isNoneSelected = OnboardingSymptom.NONE in selectedSymptoms

                OnboardingSelectionCard(
                    icon = Icons.Default.Check,
                    title = OnboardingSymptom.NONE.displayName,
                    description = OnboardingSymptom.NONE.description,
                    isSelected = isNoneSelected,
                    onClick = { onSymptomToggled(OnboardingSymptom.NONE) },
                    selectionType = SelectionType.RADIO
                )
            }
        }
    }
}

/**
 * Get icon for each symptom
 */
private fun getSymptomIcon(symptom: OnboardingSymptom): ImageVector {
    return when (symptom) {
        OnboardingSymptom.HOT_FLASHES -> Icons.Default.Whatshot
        OnboardingSymptom.NIGHT_SWEATS -> Icons.Default.NightsStay
        OnboardingSymptom.SLEEP_ISSUES -> Icons.Default.Bedtime
        OnboardingSymptom.MOOD_SWINGS -> Icons.Default.Mood
        OnboardingSymptom.FATIGUE -> Icons.Default.BatteryAlert
        OnboardingSymptom.BRAIN_FOG -> Icons.Default.Psychology
        OnboardingSymptom.WEIGHT_GAIN -> Icons.Default.MonitorWeight
        OnboardingSymptom.JOINT_PAIN -> Icons.Default.Accessibility
        OnboardingSymptom.ANXIETY -> Icons.Default.Warning
        OnboardingSymptom.LOW_LIBIDO -> Icons.Default.FavoriteBorder
        OnboardingSymptom.NONE -> Icons.Default.Check
    }
}

