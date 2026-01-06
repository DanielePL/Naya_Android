package com.example.menotracker.onboarding.screens

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
import androidx.compose.ui.unit.dp
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.ComboVideo
import com.example.menotracker.onboarding.data.FeatureInterest
import com.example.menotracker.onboarding.data.FeatureVideo
import com.example.menotracker.onboarding.data.PersonaType
import com.example.menotracker.ui.theme.*

/**
 * Feature Promo Screen - Shows personalized video showcase based on persona
 *
 * Video selection based on persona:
 * - NUTRITION_FOCUSED: Nutrition video (meal logging, macros)
 * - STRENGTH_FOCUSED / PERFORMANCE_FOCUSED: VBT video (velocity tracking)
 * - COMPLETE_ATHLETE: Elite video (AI Coach + VBT + Nutrition)
 *
 * Square video format with animated placeholder.
 */
@Composable
fun FeaturePromoScreen(
    featureVideos: List<FeatureVideo>,
    personaType: PersonaType?,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    comboVideo: ComboVideo? = null,
    selectedFeatures: List<FeatureInterest> = emptyList()
) {
    // Get video content based on persona
    val (videoTitle, videoDescription) = getVideoContent(personaType)
    val featuresToShow = if (selectedFeatures.isNotEmpty()) selectedFeatures else comboVideo?.features ?: emptyList()

    OnboardingShowcaseScaffold(
        onBack = onSkip,  // Back acts as skip on this screen
        footer = {
            // Feature checkmarks (compact)
            if (featuresToShow.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = OnboardingTokens.spacingMd),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    featuresToShow.take(4).forEach { feature ->
                        FeatureChip(
                            icon = getIconForInterest(feature),
                            text = feature.displayName
                        )
                    }
                }
            }

            OnboardingPrimaryButton(
                text = "Let's Get Started",
                onClick = onContinue
            )
        }
    ) {
        OnboardingVideoShowcase(
            title = videoTitle,
            description = videoDescription
        )
    }
}

/**
 * Get video content based on persona type
 */
private fun getVideoContent(personaType: PersonaType?): Pair<String, String> {
    return when (personaType) {
        PersonaType.NUTRITION_FOCUSED -> Pair(
            "Track Your Nutrition",
            "Log meals in seconds. Hit your macros. See real results."
        )
        PersonaType.STRENGTH_FOCUSED -> Pair(
            "Train with Velocity",
            "Track bar speed. Auto-regulate intensity. Maximize strength gains."
        )
        PersonaType.PERFORMANCE_FOCUSED -> Pair(
            "Optimize Performance",
            "Data-driven training. Track every metric that matters."
        )
        PersonaType.COMPLETE_ATHLETE -> Pair(
            "Your Complete System",
            "AI coaching, velocity tracking, and nutrition — all in one."
        )
        null -> Pair(
            "Your Training System",
            "Everything you need to reach your goals."
        )
    }
}

/**
 * Compact feature chip for footer
 */
@Composable
private fun FeatureChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
        color = NayaPrimary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = OnboardingTokens.spacingSm + 4.dp,
                vertical = OnboardingTokens.spacingSm
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NayaPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = NayaTextPrimary,
                fontWeight = FontWeight.Medium,
                fontFamily = Poppins
            )
        }
    }
}

/**
 * Maps FeatureInterest to appropriate Material icons
 */
private fun getIconForInterest(interest: FeatureInterest): ImageVector {
    return when (interest) {
        FeatureInterest.NUTRITION -> Icons.Default.Restaurant
        FeatureInterest.AI_COACH -> Icons.Default.Psychology
        FeatureInterest.TEMPLATES -> Icons.Default.LibraryBooks
    }
}