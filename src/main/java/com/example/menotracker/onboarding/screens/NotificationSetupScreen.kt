package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.menotracker.ui.theme.*

/**
 * Notification Setup Screen
 *
 * Final step in onboarding - configure notification preferences.
 *
 * Uses shared OnboardingComponents for consistent design.
 */
@Composable
fun NotificationSetupScreen(
    showNutritionReminder: Boolean = true,
    onEnable: (trainingReminders: Boolean, mealReminders: Boolean, weeklyReports: Boolean) -> Unit,
    onSkip: () -> Unit
) {
    var trainingReminders by remember { mutableStateOf(true) }
    var mealReminders by remember { mutableStateOf(showNutritionReminder) }
    var weeklyReports by remember { mutableStateOf(true) }

    OnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(OnboardingTokens.spacingLg)
        ) {
            // Skip button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = NayaTextTertiary,
                        fontFamily = Poppins
                    )
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingXl))

            // Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = OnboardingTokens.spacingLg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = NayaPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Title
            Text(
                text = "Stay on Track",
                style = MaterialTheme.typography.displayMedium,
                color = NayaTextPrimary,
                textAlign = TextAlign.Center,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp))

            Text(
                text = "Naya can remind you to:",
                style = MaterialTheme.typography.bodyLarge,
                color = NayaTextSecondary,
                textAlign = TextAlign.Center,
                fontFamily = Poppins,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingXl))

            // Notification options
            NotificationOption(
                icon = Icons.Default.FitnessCenter,
                title = "Training Reminders",
                description = "Don't miss your workouts",
                isEnabled = trainingReminders,
                onToggle = { trainingReminders = it }
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            if (showNutritionReminder) {
                NotificationOption(
                    icon = Icons.Default.Restaurant,
                    title = "Meal Logging Reminders",
                    description = "Track your nutrition",
                    isEnabled = mealReminders,
                    onToggle = { mealReminders = it }
                )

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
            }

            NotificationOption(
                icon = Icons.Default.Assessment,
                title = "Weekly Progress Reports",
                description = "See your improvement",
                isEnabled = weeklyReports,
                onToggle = { weeklyReports = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Enable button
            OnboardingPrimaryButton(
                text = "Enable & Finish",
                onClick = { onEnable(trainingReminders, mealReminders, weeklyReports) }
            )
        }
    }
}

/**
 * Notification option card with toggle
 * Uses standardized design tokens
 */
@Composable
private fun NotificationOption(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NayaSurface,
        shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
        border = BorderStroke(
            width = 1.dp,
            color = NayaTextTertiary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OnboardingTokens.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NayaPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(OnboardingTokens.spacingMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NayaTextPrimary,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NayaTextTertiary,
                    fontFamily = Poppins
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NayaTextPrimary,
                    checkedTrackColor = NayaPrimary,
                    uncheckedThumbColor = NayaTextPrimary,
                    uncheckedTrackColor = NayaTextTertiary.copy(alpha = 0.3f)
                )
            )
        }
    }
}
