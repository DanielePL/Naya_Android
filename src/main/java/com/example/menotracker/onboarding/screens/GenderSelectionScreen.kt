package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.Gender
import com.example.menotracker.ui.theme.*

/**
 * Gender Selection Screen
 *
 * Uses shared OnboardingComponents for consistent design.
 */
@Composable
fun GenderSelectionScreen(
    currentStep: Int,
    totalSteps: Int,
    onGenderSelected: (Gender) -> Unit,
    onBack: () -> Unit
) {
    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "What's Your Gender?",
        subtitle = "This helps us calculate accurate strength standards and progress estimates",
        onBack = onBack,
        footer = {
            // No continue button - selection auto-advances
        }
    ) {
        Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))

        // Gender options - larger cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingMd)
        ) {
            GenderCard(
                icon = Icons.Default.Male,
                label = "Male",
                modifier = Modifier.weight(1f),
                onClick = { onGenderSelected(Gender.MALE) }
            )

            GenderCard(
                icon = Icons.Default.Female,
                label = "Female",
                modifier = Modifier.weight(1f),
                onClick = { onGenderSelected(Gender.FEMALE) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Privacy note
        Text(
            text = "This is only used for strength calculations and is stored locally on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = NayaTextTertiary,
            textAlign = TextAlign.Center,
            fontFamily = Poppins,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GenderCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable { onClick() },
        color = NayaSurface,
        shape = RoundedCornerShape(OnboardingTokens.radiusMedium),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = NayaTextTertiary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(OnboardingTokens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NayaOrange,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = NayaTextPrimary,
                fontFamily = Poppins
            )
        }
    }
}
