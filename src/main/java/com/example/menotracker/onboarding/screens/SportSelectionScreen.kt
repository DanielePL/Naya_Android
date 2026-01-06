package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import com.example.menotracker.onboarding.data.Sport
import com.example.menotracker.ui.theme.*

/**
 * Sport Selection Screen
 *
 * Uses shared OnboardingComponents for consistent design.
 */
@Composable
fun SportSelectionScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedSports: List<Sport>,
    onToggleSport: (Sport) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "What Sports Do You Train?",
        subtitle = "Select all that apply",
        onBack = onBack,
        footer = {
            OnboardingPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = selectedSports.isNotEmpty()
            )
        }
    ) {
        // Sport grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingSm + 4.dp),
            verticalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingSm + 4.dp)
        ) {
            items(Sport.entries.toList()) { sport ->
                SportChip(
                    sport = sport,
                    isSelected = selectedSports.contains(sport),
                    onClick = { onToggleSport(sport) }
                )
            }
        }
    }
}

private fun getSportIcon(sport: Sport): ImageVector {
    return when (sport) {
        Sport.WEIGHTLIFTING -> Icons.Default.FitnessCenter
        Sport.POWERLIFTING -> Icons.Default.FitnessCenter
        Sport.STRONGMAN -> Icons.Default.FitnessCenter
        Sport.CROSSFIT -> Icons.Default.Bolt
        Sport.HYROX -> Icons.AutoMirrored.Filled.DirectionsRun
        Sport.BODYBUILDING -> Icons.Default.FitnessCenter
        Sport.ENDURANCE -> Icons.AutoMirrored.Filled.DirectionsRun
        Sport.TEAM_SPORTS -> Icons.Default.SportsSoccer
        Sport.GENERAL -> Icons.Default.SelfImprovement
    }
}

@Composable
private fun SportChip(
    sport: Sport,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) NayaPrimary.copy(alpha = 0.2f) else NayaSurface
    val borderColor = if (isSelected) NayaPrimary else NayaTextTertiary.copy(alpha = 0.1f)

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(OnboardingTokens.spacingSm)
            ) {
                Icon(
                    imageVector = getSportIcon(sport),
                    contentDescription = null,
                    tint = if (isSelected) NayaOrange else NayaTextSecondary,
                    modifier = Modifier.size(OnboardingTokens.iconSize)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sport.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) NayaTextPrimary else NayaTextSecondary,
                    textAlign = TextAlign.Center,
                    fontFamily = Poppins,
                    maxLines = 2
                )
            }

            // Check mark overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .background(NayaPrimary, shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NayaTextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
