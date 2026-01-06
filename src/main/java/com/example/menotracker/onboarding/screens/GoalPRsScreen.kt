package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.onboarding.data.CurrentPRs
import com.example.menotracker.onboarding.data.GoalPRs
import com.example.menotracker.onboarding.data.LiftType
import com.example.menotracker.onboarding.data.Sport
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrange

/**
 * Sport-specific goal PR collection screen.
 * Only shows goals for lifts that the user entered current PRs for.
 * Adjusts default goals based on training experience.
 */
@Composable
fun GoalPRsScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedSports: List<Sport>,
    currentPRs: CurrentPRs,
    goalPRs: GoalPRs,
    trainingYears: Int? = null,
    age: Int? = null,
    onUpdateGoalPRs: (GoalPRs) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    // Helper to format float as clean string
    fun Float.toCleanString(): String {
        return if (this == this.toInt().toFloat()) {
            this.toInt().toString()
        } else {
            this.toString()
        }
    }

    // Calculate realistic goal multiplier based on training years
    // Veteran lifters should have much more conservative defaults
    val goalMultiplier = remember(trainingYears) {
        when {
            trainingYears == null -> 1.10f      // No info: default +10%
            trainingYears <= 1 -> 1.15f         // Beginners can aim higher: +15%
            trainingYears <= 3 -> 1.10f         // Novice: +10%
            trainingYears <= 5 -> 1.05f         // Intermediate: +5%
            trainingYears <= 10 -> 1.025f       // Advanced: +2.5%
            trainingYears <= 20 -> 1.01f        // Veteran: +1%
            else -> 1.005f                      // Lifetime lifter: +0.5% (maintenance)
        }
    }

    // Get lifts that user has entered PRs for
    val liftsWithPRs = remember(currentPRs, selectedSports) {
        getLiftsWithPRs(currentPRs, selectedSports)
    }

    // State for each lift's goal value
    val goalInputs = remember(liftsWithPRs, goalMultiplier) {
        mutableStateMapOf<LiftType, String>().apply {
            liftsWithPRs.forEach { lift ->
                val existingGoal = goalPRs.getGoal(lift)
                val currentPR = currentPRs.getPR(lift)
                // Pre-fill with existing goal, or current + experience-adjusted percentage
                this[lift] = existingGoal?.toCleanString()
                    ?: currentPR?.let { (it * goalMultiplier).toInt().toString() }
                    ?: ""
            }
        }
    }

    val hasAnyGoal = goalInputs.values.any { it.isNotBlank() }

    // Calculate totals for display
    val powerliftingGoalTotal = calculatePowerliftingGoalTotal(goalInputs)
    val weightliftingGoalTotal = calculateWeightliftingGoalTotal(goalInputs)
    val powerliftingCurrentTotal = currentPRs.powerliftingTotalKg
    val weightliftingCurrentTotal = currentPRs.weightliftingTotalKg

    // Check for warnings
    val warnings = getGoalWarnings(goalInputs, currentPRs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Header with back button and progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Step $currentStep of $totalSteps",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = NayaPrimary,
            trackColor = Color.White.copy(alpha = 0.1f)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Where Do You Want To Be?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set your target PRs - be ambitious but realistic",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            // Show experience-adjusted context for veteran lifters
            if (trainingYears != null && trainingYears >= 10) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = NayaOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = NayaOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Veteran Lifter",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NayaOrange
                            )
                            Text(
                                text = "With $trainingYears years of training, we've pre-filled conservative targets. " +
                                       "Adjust as you see fit - you know your body best.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Show message if no PRs were entered
            if (liftsWithPRs.isEmpty()) {
                Surface(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = NayaOrange,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No PRs entered yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "You can set goals later in your profile",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Goal input fields for each lift with a current PR
                liftsWithPRs.forEach { lift ->
                    GoalInputField(
                        label = "${lift.displayName} Goal",
                        value = goalInputs[lift] ?: "",
                        onValueChange = { goalInputs[lift] = it },
                        currentPR = currentPRs.getPR(lift)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Warnings
            warnings.forEach { warning ->
                Spacer(modifier = Modifier.height(8.dp))
                WarningBanner(
                    message = warning.message,
                    isError = warning.isError
                )
            }

            // Powerlifting Total & Gain display
            if (powerliftingGoalTotal != null && powerliftingGoalTotal > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                GoalTotalDisplay(
                    label = "Powerlifting Goal Total",
                    goalTotal = powerliftingGoalTotal,
                    currentTotal = powerliftingCurrentTotal
                )
            }

            // Weightlifting Total & Gain display
            if (weightliftingGoalTotal != null && weightliftingGoalTotal > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                GoalTotalDisplay(
                    label = "Weightlifting Goal Total",
                    goalTotal = weightliftingGoalTotal,
                    currentTotal = weightliftingCurrentTotal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Buttons
        Column {
            Button(
                onClick = {
                    // Build updated GoalPRs from inputs
                    var updatedGoals = GoalPRs()
                    goalInputs.forEach { (lift, value) ->
                        val floatValue = value.toFloatOrNull()
                        if (floatValue != null && floatValue > 0) {
                            updatedGoals = updatedGoals.withGoal(lift, floatValue)
                        }
                    }
                    onUpdateGoalPRs(updatedGoals)
                    onContinue()
                },
                enabled = hasAnyGoal || liftsWithPRs.isEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NayaPrimary,
                    disabledContainerColor = NayaPrimary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }

            if (liftsWithPRs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Skip for now",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Get lifts that user has entered current PRs for
 */
private fun getLiftsWithPRs(currentPRs: CurrentPRs, sports: List<Sport>): List<LiftType> {
    // Get all relevant lifts for user's sports
    val relevantLifts = if (sports.isEmpty()) {
        listOf(LiftType.SQUAT, LiftType.BENCH_PRESS, LiftType.DEADLIFT)
    } else {
        sports.flatMap { LiftType.getPrimaryLiftsForSport(it) }.distinct()
    }

    // Filter to only those with entered PRs
    return relevantLifts.filter { lift ->
        val pr = currentPRs.getPR(lift)
        pr != null && pr > 0
    }
}

/**
 * Calculate powerlifting goal total (SBD)
 */
private fun calculatePowerliftingGoalTotal(inputs: Map<LiftType, String>): Float? {
    val squat = inputs[LiftType.SQUAT]?.toFloatOrNull()
    val bench = inputs[LiftType.BENCH_PRESS]?.toFloatOrNull()
    val deadlift = inputs[LiftType.DEADLIFT]?.toFloatOrNull()

    return if (squat != null && bench != null && deadlift != null) {
        squat + bench + deadlift
    } else null
}

/**
 * Calculate weightlifting goal total (Snatch + C&J)
 */
private fun calculateWeightliftingGoalTotal(inputs: Map<LiftType, String>): Float? {
    val snatch = inputs[LiftType.SNATCH]?.toFloatOrNull()
    val cj = inputs[LiftType.CLEAN_AND_JERK]?.toFloatOrNull()

    return if (snatch != null && cj != null) {
        snatch + cj
    } else null
}

data class GoalWarning(val message: String, val isError: Boolean)

/**
 * Get warnings for goal values
 */
private fun getGoalWarnings(
    goalInputs: Map<LiftType, String>,
    currentPRs: CurrentPRs
): List<GoalWarning> {
    val warnings = mutableListOf<GoalWarning>()

    // Check for goals below current PRs
    goalInputs.forEach { (lift, valueStr) ->
        val goalValue = valueStr.toFloatOrNull() ?: return@forEach
        val currentPR = currentPRs.getPR(lift) ?: return@forEach

        if (goalValue < currentPR) {
            warnings.add(GoalWarning(
                message = "${lift.shortName} goal is below your current PR!",
                isError = true
            ))
        }
    }

    // Check for unrealistic values (rough world record limits)
    val unrealisticLimits = mapOf(
        LiftType.SQUAT to 500f,
        LiftType.BENCH_PRESS to 350f,
        LiftType.DEADLIFT to 500f,
        LiftType.SNATCH to 220f,
        LiftType.CLEAN_AND_JERK to 270f,
        LiftType.LOG_LIFT to 230f,
        LiftType.AXLE_CLEAN_PRESS to 200f
    )

    goalInputs.forEach { (lift, valueStr) ->
        val goalValue = valueStr.toFloatOrNull() ?: return@forEach
        val limit = unrealisticLimits[lift] ?: return@forEach

        if (goalValue > limit) {
            warnings.add(GoalWarning(
                message = "${lift.shortName} goal seems very high. Did you mean a different value?",
                isError = false
            ))
        }
    }

    return warnings.distinctBy { it.message }
}

@Composable
private fun GoalTotalDisplay(
    label: String,
    goalTotal: Float,
    currentTotal: Float?
) {
    val totalGain = if (currentTotal != null && currentTotal > 0) {
        goalTotal - currentTotal
    } else 0f

    Surface(
        color = NayaPrimary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "${goalTotal.toInt()} kg",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NayaOrange
                )
            }

            if (totalGain > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "To gain",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "+${totalGain.toInt()} kg",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningBanner(
    message: String,
    isError: Boolean
) {
    val color = if (isError) Color(0xFFF44336) else Color(0xFFFF9800)
    val icon = if (isError) Icons.Default.TrendingDown else Icons.Default.Warning

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = color
            )
        }
    }
}

@Composable
private fun GoalInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    currentPR: Float?
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )

            if (currentPR != null) {
                Text(
                    text = "Current: ${currentPR.toInt()} kg",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = currentPR?.let { "e.g. ${(it * 1.2f).toInt()}" } ?: "Enter goal",
                    color = Color.White.copy(alpha = 0.3f)
                )
            },
            suffix = {
                Text(
                    text = "kg",
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = NayaOrange.copy(alpha = 0.7f)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NayaPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                cursorColor = NayaPrimary,
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}