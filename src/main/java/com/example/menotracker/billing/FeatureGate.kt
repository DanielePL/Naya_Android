package com.example.menotracker.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrange

/**
 * Feature Gate Composable
 *
 * Shows locked content overlay when user doesn't have access to a feature.
 * Call SubscriptionManager.hasAccess(feature) to check access before showing content.
 */
@Composable
fun FeatureGate(
    feature: Feature,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hasAccess = SubscriptionManager.hasAccess(feature)

    Box(modifier = modifier) {
        // Always show the content (but blurred/dimmed if locked)
        content()

        // Overlay when locked
        if (!hasAccess) {
            LockedOverlay(
                feature = feature,
                onUpgradeClick = onUpgradeClick
            )
        }
    }
}

/**
 * Simple access check - use this for navigation guards
 */
@Composable
fun rememberHasAccess(feature: Feature): Boolean {
    val tier by SubscriptionManager.currentTier.collectAsState()
    val premiumChoice by SubscriptionManager.premiumFeatureChoice.collectAsState()
    return SubscriptionManager.hasAccess(feature)
}

/**
 * Locked Overlay - shown when feature is not available
 */
@Composable
private fun LockedOverlay(
    feature: Feature,
    onUpgradeClick: () -> Unit
) {
    val requiredTier = SubscriptionManager.getRequiredTier(feature)
    val tierColor = when (requiredTier) {
        SubscriptionTier.ELITE -> NayaOrange
        else -> NayaPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.9f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Lock icon
            Surface(
                color = tierColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = tierColor,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feature name
            Text(
                text = getFeatureTitle(feature),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = getFeatureDescription(feature),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Upgrade button
            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tierColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upgrade to ${requiredTier.displayName}",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price hint
            Text(
                text = "Starting at ${requiredTier.yearlyPrice}/year",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Compact locked badge - for use in lists/cards
 */
@Composable
fun LockedBadge(
    feature: Feature,
    modifier: Modifier = Modifier
) {
    if (!SubscriptionManager.hasAccess(feature)) {
        val requiredTier = SubscriptionManager.getRequiredTier(feature)
        val tierColor = when (requiredTier) {
            SubscriptionTier.ELITE -> NayaOrange
            else -> NayaPrimary
        }

        Surface(
            color = tierColor.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp),
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = tierColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = requiredTier.displayName.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = tierColor
                )
            }
        }
    }
}

/**
 * Inline upgrade prompt - for use in feature entry points
 */
@Composable
fun UpgradePrompt(
    feature: Feature,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requiredTier = SubscriptionManager.getRequiredTier(feature)
    val tierColor = when (requiredTier) {
        SubscriptionTier.ELITE -> NayaOrange
        else -> NayaPrimary
    }

    Surface(
        color = tierColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = tierColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getFeatureTitle(feature),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Requires ${requiredTier.displayName}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tierColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Upgrade",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun getFeatureTitle(feature: Feature): String {
    return when (feature) {
        Feature.LIBRARY -> "Exercise Library"
        Feature.WORKOUT_TRACKING -> "Workout Tracking"
        Feature.COMMUNITY_POSTS -> "Community"
        Feature.BAR_SPEED -> "Bar Speed Tracker"
        Feature.NUTRITION -> "Nutrition Tracker"
        Feature.AI_COACH -> "AI Coach"
        Feature.PHYSICAL_COACH -> "Physical Coach"
        Feature.UNLIMITED_WORKOUTS -> "Unlimited Workouts"
        Feature.WOD_SCANNER -> "WOD Scanner"
        Feature.MOOD_JOURNALING -> "Mood Journaling"
        Feature.BASIC_BREATHING -> "Breathing Exercises"
        Feature.UNLIMITED_MOOD_ENTRIES -> "Unlimited Mood Entries"
        Feature.BREATHING_LIBRARY -> "Full Breathing Library"
        Feature.BASIC_MEDITATION -> "Meditation"
        Feature.BASIC_SOUNDS -> "Ambient Sounds"
        Feature.MEDITATION_LIBRARY -> "Full Meditation Library"
        Feature.PREMIUM_SOUNDS -> "Premium Sounds"
        Feature.SOUNDSCAPE_MIXER -> "Soundscape Mixer"
    }
}

private fun getFeatureDescription(feature: Feature): String {
    return when (feature) {
        Feature.BAR_SPEED -> "Track bar velocity and optimize your training with real-time feedback"
        Feature.NUTRITION -> "Log meals, track macros, and get personalized nutrition insights"
        Feature.AI_COACH -> "Get personalized training advice powered by AI"
        Feature.PHYSICAL_COACH -> "Connect with real coaches for personalized guidance"
        Feature.UNLIMITED_WORKOUTS -> "Create unlimited workout templates"
        Feature.WOD_SCANNER -> "Scan WODs from photos, Excel, or PDF files with AI"
        Feature.MOOD_JOURNALING -> "Track your mood and emotions with guided journaling"
        Feature.BASIC_BREATHING -> "Practice calming breathing exercises for relaxation"
        Feature.UNLIMITED_MOOD_ENTRIES -> "Unlimited mood journal entries for comprehensive tracking"
        Feature.BREATHING_LIBRARY -> "Access all 5 breathing exercises for every symptom"
        else -> "Upgrade to unlock this feature"
    }
}