package com.example.menotracker.onboarding.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.billing.BillingPeriod
import com.example.menotracker.billing.BillingViewModel
import com.example.menotracker.billing.PurchaseResult
import com.example.menotracker.billing.SubscriptionTier
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.PersonaType
import com.example.menotracker.ui.theme.*

/**
 * 3-Tier Paywall Screen
 *
 * FREE: Library, 3 Workouts, Community Posts
 * PREMIUM ($59/year): + VBT OR Nutrition (choose one)
 * ELITE ($99/year): Everything + AI Coach + Physical Coach
 *
 * Uses shared OnboardingComponents for consistent design.
 */
@Composable
fun PaywallScreen(
    personaType: PersonaType?,
    benefits: List<String>,
    onSubscribe: () -> Unit,
    onContinueFree: () -> Unit,
    onRestorePurchase: () -> Unit,
    billingViewModel: BillingViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Billing states
    val selectedTier by billingViewModel.selectedTier.collectAsState()
    val selectedBillingPeriod by billingViewModel.selectedBillingPeriod.collectAsState()
    val isConnected by billingViewModel.isConnected.collectAsState()
    val isLoading by billingViewModel.isLoading.collectAsState()
    val currentTier by billingViewModel.subscriptionTier.collectAsState()
    val purchaseResult by billingViewModel.purchaseResult.collectAsState()

    // Handle purchase result
    LaunchedEffect(purchaseResult) {
        when (purchaseResult) {
            is PurchaseResult.Success -> {
                billingViewModel.clearPurchaseResult()
                onSubscribe()
            }
            is PurchaseResult.Error -> {
                // Error is shown in UI
            }
            else -> {}
        }
    }

    // If already subscribed, skip paywall
    LaunchedEffect(currentTier) {
        if (currentTier != SubscriptionTier.FREE) {
            onSubscribe()
        }
    }

    OnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header with close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(OnboardingTokens.spacingMd),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = onContinueFree) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NayaTextSecondary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OnboardingTokens.spacingLg - 4.dp), // 20dp
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Choose Your Plan",
                    style = MaterialTheme.typography.displayMedium,
                    color = NayaTextPrimary,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp))

                Text(
                    text = getPersonalizedTagline(personaType),
                    style = MaterialTheme.typography.bodyLarge,
                    color = NayaTextSecondary,
                    textAlign = TextAlign.Center,
                    fontFamily = Poppins
                )

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg - 4.dp))

                // Billing period toggle
                BillingPeriodToggle(
                    selectedPeriod = selectedBillingPeriod,
                    onSelectPeriod = { billingViewModel.selectBillingPeriod(it) }
                )

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

                // Tier cards
                TierCard(
                    tier = SubscriptionTier.FREE,
                    isSelected = selectedTier == SubscriptionTier.FREE,
                    billingPeriod = selectedBillingPeriod,
                    billingViewModel = billingViewModel,
                    onClick = { billingViewModel.selectTier(SubscriptionTier.FREE) }
                )

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 2.dp))

                TierCard(
                    tier = SubscriptionTier.PREMIUM,
                    isSelected = selectedTier == SubscriptionTier.PREMIUM,
                    billingPeriod = selectedBillingPeriod,
                    billingViewModel = billingViewModel,
                    onClick = { billingViewModel.selectTier(SubscriptionTier.PREMIUM) },
                    badge = "POPULAR"
                )

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 2.dp))

                TierCard(
                    tier = SubscriptionTier.ELITE,
                    isSelected = selectedTier == SubscriptionTier.ELITE,
                    billingPeriod = selectedBillingPeriod,
                    billingViewModel = billingViewModel,
                    onClick = { billingViewModel.selectTier(SubscriptionTier.ELITE) },
                    badge = "BEST VALUE"
                )

                // Error message
                val errorResult = purchaseResult as? PurchaseResult.Error
                if (errorResult != null) {
                    Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp))
                    Surface(
                        color = NayaError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(OnboardingTokens.spacingSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(OnboardingTokens.spacingSm + 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = NayaError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm))
                            Text(
                                text = errorResult.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = NayaError
                            )
                        }
                    }
                }

                // Connection warning
                if (!isConnected) {
                    Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp))
                    Surface(
                        color = NayaWarning.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(OnboardingTokens.spacingSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(OnboardingTokens.spacingSm + 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = NayaWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm))
                            Text(
                                text = "Connecting to Google Play...",
                                style = MaterialTheme.typography.bodySmall,
                                color = NayaWarning
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
            }

            // Bottom action section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OnboardingTokens.spacingLg - 4.dp, vertical = OnboardingTokens.spacingMd),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingPrimaryButton(
                    text = "Continue",
                    onClick = onContinueFree,
                    showArrow = false
                )

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 2.dp))

                // Restore purchase
                TextButton(
                    onClick = {
                        billingViewModel.restorePurchases()
                        onRestorePurchase()
                    }
                ) {
                    Text(
                        text = "Restore Purchase",
                        style = MaterialTheme.typography.bodySmall,
                        color = NayaTextTertiary,
                        textDecoration = TextDecoration.Underline,
                        fontFamily = Poppins
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingPeriodToggle(
    selectedPeriod: BillingPeriod,
    onSelectPeriod: (BillingPeriod) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(OnboardingTokens.spacingSm + 2.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF252525).copy(alpha = 0.85f),
                        Color(0xFF1C1C1C).copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(OnboardingTokens.spacingSm + 2.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            PeriodButton(
                text = "Yearly",
                savingText = "Save ~30%",
                isSelected = selectedPeriod == BillingPeriod.YEARLY,
                onClick = { onSelectPeriod(BillingPeriod.YEARLY) }
            )
            PeriodButton(
                text = "Monthly",
                savingText = null,
                isSelected = selectedPeriod == BillingPeriod.MONTHLY,
                onClick = { onSelectPeriod(BillingPeriod.MONTHLY) }
            )
        }
    }
}

@Composable
private fun PeriodButton(
    text: String,
    savingText: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) NayaPrimary else Color.Transparent,
        shape = RoundedCornerShape(OnboardingTokens.spacingSm)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = OnboardingTokens.spacingLg - 4.dp, vertical = OnboardingTokens.spacingSm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = NayaTextPrimary,
                fontFamily = Poppins
            )
            if (savingText != null && isSelected) {
                Text(
                    text = savingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = NayaTextPrimary.copy(alpha = 0.8f),
                    fontFamily = Poppins
                )
            }
        }
    }
}

@Composable
private fun TierCard(
    tier: SubscriptionTier,
    isSelected: Boolean,
    billingPeriod: BillingPeriod,
    billingViewModel: BillingViewModel,
    onClick: () -> Unit,
    badge: String? = null
) {
    val glowColor = when {
        tier == SubscriptionTier.ELITE -> NayaOrange
        else -> NayaPrimary
    }

    val features = getTierFeatures(tier)
    val price = billingViewModel.getFormattedPrice(tier, billingPeriod)
    val pricePerMonth = if (billingPeriod == BillingPeriod.YEARLY && tier != SubscriptionTier.FREE) {
        billingViewModel.getPricePerMonth(tier)
    } else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.nayaGlow(
                        glowColor = glowColor,
                        glowRadius = 16.dp,
                        glowAlpha = 0.2f,
                        cornerRadius = OnboardingTokens.radiusSmall
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(OnboardingTokens.radiusSmall))
            .background(
                brush = Brush.linearGradient(
                    colors = when {
                        isSelected && tier == SubscriptionTier.ELITE -> listOf(
                            NayaOrange.copy(alpha = 0.2f),
                            Color(0xFF1C1C1C).copy(alpha = 0.9f)
                        )
                        isSelected -> listOf(
                            NayaPrimary.copy(alpha = 0.2f),
                            Color(0xFF1C1C1C).copy(alpha = 0.9f)
                        )
                        else -> listOf(
                            Color(0xFF252525).copy(alpha = 0.85f),
                            Color(0xFF1C1C1C).copy(alpha = 0.75f)
                        )
                    }
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = when {
                        isSelected && tier == SubscriptionTier.ELITE -> listOf(
                            NayaOrange.copy(alpha = 0.8f),
                            NayaOrange.copy(alpha = 0.4f)
                        )
                        isSelected -> listOf(
                            NayaPrimary.copy(alpha = 0.8f),
                            NayaPrimary.copy(alpha = 0.4f)
                        )
                        else -> listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    }
                ),
                shape = RoundedCornerShape(OnboardingTokens.radiusSmall)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(OnboardingTokens.cardPaddingCompact - 2.dp)
        ) {
            // Header row with tier name and price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tier.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = NayaTextPrimary,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold
                        )
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm))
                            Surface(
                                color = if (tier == SubscriptionTier.ELITE) NayaOrange else NayaPrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NayaTextPrimary,
                                    fontFamily = Poppins,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = getTierDescription(tier),
                        style = MaterialTheme.typography.labelSmall,
                        color = NayaTextTertiary,
                        fontFamily = Poppins
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (tier == SubscriptionTier.FREE) "Free" else price,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NayaTextPrimary,
                        fontFamily = SpaceGrotesk
                    )
                    if (pricePerMonth != null) {
                        Text(
                            text = "$pricePerMonth/mo",
                            style = MaterialTheme.typography.labelSmall,
                            color = NayaTextTertiary,
                            fontFamily = Poppins
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 2.dp))

            // Features list
            features.forEach { feature ->
                FeatureRow(
                    icon = feature.icon,
                    text = feature.text,
                    isIncluded = feature.isIncluded,
                    isHighlight = feature.isHighlight
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    text: String,
    isIncluded: Boolean,
    isHighlight: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isIncluded) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = when {
                isHighlight && isIncluded -> NayaOrange
                isIncluded -> NayaPrimary
                else -> NayaTextTertiary.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isIncluded) NayaTextPrimary else NayaTextTertiary,
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = Poppins
        )
    }
}

private data class TierFeature(
    val icon: ImageVector,
    val text: String,
    val isIncluded: Boolean,
    val isHighlight: Boolean = false
)

private fun getTierFeatures(tier: SubscriptionTier): List<TierFeature> {
    return when (tier) {
        SubscriptionTier.FREE -> listOf(
            TierFeature(Icons.Default.TrackChanges, "Basic Symptom Tracking", true),
            TierFeature(Icons.Default.Mood, "Mood Check-ins (3x/week)", true),
            TierFeature(Icons.Default.FitnessCenter, "Workouts", true),
            TierFeature(Icons.Default.Group, "Community Posts", true),
            TierFeature(Icons.Default.Air, "1 Breathing Exercise", true),
            TierFeature(Icons.Default.Psychology, "AI Health Coach", false)
        )
        SubscriptionTier.PREMIUM -> listOf(
            TierFeature(Icons.Default.TrackChanges, "Unlimited Symptom Tracking", true),
            TierFeature(Icons.Default.Mood, "Unlimited Mood Journaling", true),
            TierFeature(Icons.Default.Medication, "HRT & Hormone Tracking", true, isHighlight = true),
            TierFeature(Icons.Default.Bedtime, "Sleep Analysis & Insights", true),
            TierFeature(Icons.Default.Air, "Full Breathing Library", true),
            TierFeature(Icons.Default.Insights, "Weekly Insights Report", true),
            TierFeature(Icons.Default.Psychology, "AI Health Coach", false)
        )
        SubscriptionTier.ELITE -> listOf(
            TierFeature(Icons.Default.AllInclusive, "Everything in Premium", true),
            TierFeature(Icons.Default.Psychology, "AI Health Coach", true, isHighlight = true),
            TierFeature(Icons.Default.AutoGraph, "Symptom Predictions", true, isHighlight = true),
            TierFeature(Icons.Default.SelfImprovement, "Meditation & Yoga Library", true),
            TierFeature(Icons.Default.Watch, "Wearable Integration", true),
            TierFeature(Icons.Default.Restaurant, "Nutrition Guidance", true),
            TierFeature(Icons.Default.SupportAgent, "Expert Q&A Access", true, isHighlight = true)
        )
    }
}

private fun getTierDescription(tier: SubscriptionTier): String {
    return when (tier) {
        SubscriptionTier.FREE -> "Start your wellness journey"
        SubscriptionTier.PREMIUM -> "Full tracking & insights"
        SubscriptionTier.ELITE -> "AI-powered personalized care"
    }
}

private fun getPersonalizedTagline(personaType: PersonaType?): String {
    // Menopause-focused taglines
    return "Navigate menopause with confidence"
}
