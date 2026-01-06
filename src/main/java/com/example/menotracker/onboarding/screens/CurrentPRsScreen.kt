package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.CurrentPRs
import com.example.menotracker.onboarding.data.LiftType
import com.example.menotracker.onboarding.data.Sport
import com.example.menotracker.ui.theme.*

/**
 * Sport-specific PR collection screen.
 * Shows relevant lifts based on the user's selected sports.
 *
 * Uses shared OnboardingComponents for consistent design.
 */
@Composable
fun CurrentPRsScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedSports: List<Sport>,
    currentPRs: CurrentPRs,
    onUpdatePRs: (CurrentPRs) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    // Get lifts relevant to the user's selected sports
    val relevantLifts = remember(selectedSports) {
        getRelevantLiftsForSports(selectedSports)
    }

    // State for each lift's input value
    val prInputs = remember(relevantLifts) {
        mutableStateMapOf<LiftType, String>().apply {
            relevantLifts.forEach { lift ->
                this[lift] = currentPRs.getPR(lift)?.toString() ?: ""
            }
        }
    }

    // Bodyweight is always optional
    var bodyweightText by remember { mutableStateOf(currentPRs.bodyweightKg?.toString() ?: "") }

    val hasAnyPR = prInputs.values.any { it.isNotBlank() }

    // Calculate totals for display
    val powerliftingTotal = calculatePowerliftingTotal(prInputs)
    val weightliftingTotal = calculateWeightliftingTotal(prInputs)

    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "What Are Your Current PRs?",
        subtitle = "Enter what you know - you can add more later in your profile",
        onBack = onBack,
        footer = {
            OnboardingPrimaryButton(
                text = "Continue",
                onClick = {
                    // Build updated CurrentPRs from inputs
                    var updatedPRs = CurrentPRs(bodyweightKg = bodyweightText.toFloatOrNull())
                    prInputs.forEach { (lift, value) ->
                        val floatValue = value.toFloatOrNull()
                        if (floatValue != null && floatValue > 0) {
                            updatedPRs = updatedPRs.withPR(lift, floatValue)
                        }
                    }
                    onUpdatePRs(updatedPRs)
                    onContinue()
                },
                enabled = hasAnyPR
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "I don't know my PRs yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NayaTextTertiary,
                    fontFamily = Poppins
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Group lifts by sport category for better UX
            val liftGroups = groupLiftsBySport(relevantLifts, selectedSports)

            liftGroups.forEach { (sportLabel, lifts) ->
                if (liftGroups.size > 1) {
                    // Show sport category header if multiple sports
                    Text(
                        text = sportLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = NayaOrange,
                        fontFamily = Poppins,
                        modifier = Modifier.padding(bottom = OnboardingTokens.spacingSm + 4.dp, top = OnboardingTokens.spacingSm)
                    )
                }

                lifts.forEach { lift ->
                    PRInputField(
                        label = lift.displayName,
                        value = prInputs[lift] ?: "",
                        onValueChange = { prInputs[lift] = it },
                        placeholder = getPlaceholderForLift(lift)
                    )
                    Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
                }
            }

            // Bodyweight (always optional)
            Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm))
            PRInputField(
                label = "Bodyweight (optional)",
                value = bodyweightText,
                onValueChange = { bodyweightText = it },
                placeholder = "e.g. 80"
            )

            // Total displays
            Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))

            // Show Powerlifting Total if SBD lifts are present
            if (powerliftingTotal != null && powerliftingTotal > 0) {
                TotalDisplay(
                    label = "Powerlifting Total (SBD)",
                    total = powerliftingTotal
                )
                Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp))
            }

            // Show Weightlifting Total if Snatch + C&J are present
            if (weightliftingTotal != null && weightliftingTotal > 0) {
                TotalDisplay(
                    label = "Weightlifting Total",
                    total = weightliftingTotal
                )
            }
        }
    }
}

/**
 * Get relevant lifts based on selected sports.
 * Prioritizes primary lifts for each sport, removes duplicates.
 */
private fun getRelevantLiftsForSports(sports: List<Sport>): List<LiftType> {
    if (sports.isEmpty()) {
        // Default to basic lifts if no sport selected
        return listOf(LiftType.SQUAT, LiftType.BENCH_PRESS, LiftType.DEADLIFT)
    }

    val lifts = mutableListOf<LiftType>()

    // Add primary lifts for each sport in order
    sports.forEach { sport ->
        LiftType.getPrimaryLiftsForSport(sport).forEach { lift ->
            if (lift !in lifts) {
                lifts.add(lift)
            }
        }
    }

    return lifts
}

/**
 * Group lifts by sport for better UI organization
 */
private fun groupLiftsBySport(
    lifts: List<LiftType>,
    sports: List<Sport>
): List<Pair<String, List<LiftType>>> {
    // If only one sport or very few lifts, don't group
    if (sports.size <= 1 || lifts.size <= 4) {
        return listOf("Your Lifts" to lifts)
    }

    val groups = mutableListOf<Pair<String, List<LiftType>>>()
    val usedLifts = mutableSetOf<LiftType>()

    sports.forEach { sport ->
        val sportLifts = LiftType.getPrimaryLiftsForSport(sport)
            .filter { it in lifts && it !in usedLifts }

        if (sportLifts.isNotEmpty()) {
            groups.add(sport.displayName to sportLifts)
            usedLifts.addAll(sportLifts)
        }
    }

    // Add any remaining lifts
    val remaining = lifts.filter { it !in usedLifts }
    if (remaining.isNotEmpty()) {
        groups.add("Other" to remaining)
    }

    return groups
}

/**
 * Get placeholder example value for a lift type
 */
private fun getPlaceholderForLift(lift: LiftType): String {
    return when (lift) {
        LiftType.SQUAT -> "e.g. 140"
        LiftType.BENCH_PRESS -> "e.g. 100"
        LiftType.DEADLIFT -> "e.g. 180"
        LiftType.SNATCH -> "e.g. 80"
        LiftType.CLEAN_AND_JERK -> "e.g. 100"
        LiftType.LOG_LIFT -> "e.g. 100"
        LiftType.AXLE_CLEAN_PRESS -> "e.g. 90"
        LiftType.YOKE_CARRY -> "e.g. 250"
        LiftType.FARMERS_WALK -> "e.g. 120 per hand"
        LiftType.ATLAS_STONES -> "e.g. 120"
        LiftType.FRONT_SQUAT -> "e.g. 110"
        LiftType.OVERHEAD_SQUAT -> "e.g. 70"
        LiftType.THRUSTER -> "e.g. 70"
        LiftType.OVERHEAD_PRESS -> "e.g. 60"
    }
}

/**
 * Calculate powerlifting total (SBD)
 */
private fun calculatePowerliftingTotal(inputs: Map<LiftType, String>): Float? {
    val squat = inputs[LiftType.SQUAT]?.toFloatOrNull()
    val bench = inputs[LiftType.BENCH_PRESS]?.toFloatOrNull()
    val deadlift = inputs[LiftType.DEADLIFT]?.toFloatOrNull()

    return if (squat != null && bench != null && deadlift != null) {
        squat + bench + deadlift
    } else null
}

/**
 * Calculate weightlifting total (Snatch + C&J)
 */
private fun calculateWeightliftingTotal(inputs: Map<LiftType, String>): Float? {
    val snatch = inputs[LiftType.SNATCH]?.toFloatOrNull()
    val cj = inputs[LiftType.CLEAN_AND_JERK]?.toFloatOrNull()

    return if (snatch != null && cj != null) {
        snatch + cj
    } else null
}

@Composable
private fun TotalDisplay(
    label: String,
    total: Float
) {
    Surface(
        color = NayaPrimary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(OnboardingTokens.spacingMd),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = NayaTextPrimary,
                fontFamily = Poppins
            )
            Text(
                text = "${total.toInt()} kg",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = NayaOrange,
                fontFamily = SpaceGrotesk
            )
        }
    }
}

@Composable
private fun PRInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = NayaTextSecondary,
            fontFamily = Poppins
        )

        Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm))

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Only allow numbers and decimal point
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = NayaTextTertiary
                )
            },
            suffix = {
                Text(
                    text = "kg",
                    color = NayaTextTertiary,
                    fontFamily = Poppins
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = NayaTextPrimary,
                unfocusedTextColor = NayaTextPrimary,
                focusedBorderColor = NayaPrimary,
                unfocusedBorderColor = NayaTextTertiary.copy(alpha = 0.3f),
                cursorColor = NayaPrimary,
                focusedContainerColor = NayaSurface,
                unfocusedContainerColor = NayaSurface
            ),
            shape = RoundedCornerShape(OnboardingTokens.radiusSmall)
        )
    }
}
