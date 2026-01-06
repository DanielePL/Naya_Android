package com.example.menotracker.onboarding.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.menotracker.data.models.DietaryPreference
import com.example.menotracker.data.models.FoodAllergy
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Dietary Preferences Screen
 *
 * Collects user's dietary preferences for personalized nutrition recommendations.
 * Uses glassmorphism design consistent with other onboarding screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietaryPreferencesScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedDiets: List<DietaryPreference>,
    selectedAllergies: List<FoodAllergy>,
    foodDislikes: List<String>,
    customAllergyNote: String?,
    onToggleDiet: (DietaryPreference) -> Unit,
    onToggleAllergy: (FoodAllergy) -> Unit,
    onUpdateDislikes: (List<String>) -> Unit,
    onUpdateCustomAllergyNote: (String?) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    var showDislikeInput by remember { mutableStateOf(false) }
    var currentDislikeInput by remember { mutableStateOf("") }

    // Stagger animation
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        animationTriggered = true
    }

    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "How Do You Eat?",
        subtitle = "Helps us personalize your nutrition tips",
        onBack = onBack,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingMd)
            ) {
                OnboardingSecondaryButton(
                    text = "Skip",
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                )
                OnboardingPrimaryButton(
                    text = "Continue",
                    onClick = onContinue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Diet type selection with stagger animation
            DietaryPreference.entries.forEachIndexed { index, diet ->
                val cardAlpha by animateFloatAsState(
                    targetValue = if (animationTriggered) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 60,
                        easing = FastOutSlowInEasing
                    ),
                    label = "dietCardAlpha$index"
                )

                OnboardingSelectionCard(
                    icon = getIconForDiet(diet),
                    title = diet.displayName,
                    description = diet.description,
                    isSelected = diet in selectedDiets,
                    onClick = { onToggleDiet(diet) },
                    selectionType = SelectionType.CHECKBOX,
                    modifier = Modifier.alpha(cardAlpha)
                )
                Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Allergies section in glass container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBackground(cornerRadius = OnboardingTokens.radiusLarge, alpha = 0.3f),
                color = androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(OnboardingTokens.radiusLarge)
            ) {
                Column(
                    modifier = Modifier.padding(OnboardingTokens.cardPadding)
                ) {
                    Text(
                        text = "Any Food Allergies?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NayaTextPrimary,
                        fontFamily = Poppins
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Select all that apply",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaTextTertiary,
                        fontFamily = Poppins
                    )

                    Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

                    // Allergy chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingSm),
                        verticalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingSm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FoodAllergy.entries.forEach { allergy ->
                            GlassChip(
                                label = allergy.displayName,
                                isSelected = allergy in selectedAllergies,
                                onClick = { onToggleAllergy(allergy) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))

            // Food dislikes section in glass container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBackground(cornerRadius = OnboardingTokens.radiusLarge, alpha = 0.3f),
                color = androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(OnboardingTokens.radiusLarge)
            ) {
                Column(
                    modifier = Modifier.padding(OnboardingTokens.cardPadding)
                ) {
                    Text(
                        text = "Foods You Don't Like?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NayaTextPrimary,
                        fontFamily = Poppins
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Optional - helps avoid unwanted suggestions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaTextTertiary,
                        fontFamily = Poppins
                    )

                    Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

                    // Dislike tags
                    if (foodDislikes.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingSm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(foodDislikes) { dislike ->
                                GlassTag(
                                    text = dislike,
                                    onRemove = { onUpdateDislikes(foodDislikes - dislike) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
                    }

                    // Add dislike input
                    if (showDislikeInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentDislikeInput,
                                onValueChange = { currentDislikeInput = it },
                                placeholder = {
                                    Text("e.g. Mushrooms", color = NayaTextTertiary)
                                },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NayaPrimary,
                                    unfocusedBorderColor = NayaGlass,
                                    focusedTextColor = NayaTextPrimary,
                                    unfocusedTextColor = NayaTextPrimary,
                                    cursorColor = NayaPrimary,
                                    focusedContainerColor = NayaGlass.copy(alpha = 0.3f),
                                    unfocusedContainerColor = NayaGlass.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm))
                            IconButton(
                                onClick = {
                                    if (currentDislikeInput.isNotBlank()) {
                                        onUpdateDislikes(foodDislikes + currentDislikeInput.trim())
                                        currentDislikeInput = ""
                                    }
                                    showDislikeInput = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Add",
                                    tint = NayaPrimary
                                )
                            }
                        }
                    } else {
                        TextButton(onClick = { showDislikeInput = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NayaTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm))
                            Text(
                                text = "Add food you don't like",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NayaTextSecondary,
                                fontFamily = Poppins
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))
        }
    }
}

/**
 * Glassmorphism chip for allergy selection
 */
@Composable
private fun GlassChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                NayaPrimary.copy(alpha = 0.8f),
                                NayaPrimary.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = NayaGlass,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            )
            .clickable { onClick() },
        color = if (isSelected) {
            NayaPrimary.copy(alpha = 0.2f)
        } else {
            NayaGlass.copy(alpha = 0.3f)
        },
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = NayaPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) NayaPrimary else NayaTextSecondary,
                fontFamily = Poppins
            )
        }
    }
}

/**
 * Glassmorphism tag for food dislikes
 */
@Composable
private fun GlassTag(
    text: String,
    onRemove: () -> Unit
) {
    Surface(
        color = NayaGlass.copy(alpha = 0.4f),
        shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = NayaGlass
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = NayaTextPrimary,
                fontFamily = Poppins
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = NayaTextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getIconForDiet(diet: DietaryPreference): ImageVector {
    return when (diet) {
        DietaryPreference.OMNIVORE -> Icons.Default.Restaurant
        DietaryPreference.PESCATARIAN -> Icons.Default.SetMeal
        DietaryPreference.VEGETARIAN -> Icons.Default.Eco
        DietaryPreference.VEGAN -> Icons.Default.Spa
        DietaryPreference.KETO -> Icons.Default.LocalFireDepartment
        DietaryPreference.HALAL -> Icons.Default.Star
        DietaryPreference.KOSHER -> Icons.Default.Star
    }
}