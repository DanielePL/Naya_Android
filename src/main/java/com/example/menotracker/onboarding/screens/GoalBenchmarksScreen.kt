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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.onboarding.data.*
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrange

/**
 * GOAL Benchmarks Screen
 *
 * Collects the user's target/goal benchmarks.
 * Shows the same benchmarks as SportBenchmarksScreen but for goals.
 * Displays current values for comparison.
 */
@Composable
fun GoalBenchmarksScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedSport: UnifiedSport?,
    currentBenchmarks: Map<String, String>,
    goalBenchmarks: Map<String, String>,
    trainingYears: Int?,
    age: Int?,
    onUpdateGoalBenchmarks: (Map<String, String>) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    // Get relevant benchmarks for the selected sport
    val benchmarks = remember(selectedSport) {
        selectedSport?.let { SportBenchmarks.getBenchmarksForSport(it) } ?: emptyList()
    }

    // Only show benchmarks that have current values
    val benchmarksWithCurrentValues = remember(benchmarks, currentBenchmarks) {
        benchmarks.filter { benchmark ->
            currentBenchmarks[benchmark.id]?.isNotBlank() == true
        }
    }

    // State for each goal benchmark input
    val goalInputs = remember(benchmarksWithCurrentValues) {
        mutableStateMapOf<String, String>().apply {
            benchmarksWithCurrentValues.forEach { benchmark ->
                this[benchmark.id] = goalBenchmarks[benchmark.id] ?: ""
            }
        }
    }

    val hasAnyGoal = goalInputs.values.any { it.isNotBlank() }

    // Get sport-specific title
    val (title, subtitle) = getGoalScreenTitleForSport(selectedSport)

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

            // Sport badge
            selectedSport?.let { sport ->
                GoalBadge(sport = sport)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Title
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Goal benchmark inputs with current values shown
            if (benchmarksWithCurrentValues.isEmpty()) {
                // No current benchmarks - show message
                NoCurrentBenchmarksMessage()
            } else {
                benchmarksWithCurrentValues.forEach { benchmark ->
                    GoalBenchmarkInputField(
                        benchmark = benchmark,
                        currentValue = currentBenchmarks[benchmark.id] ?: "",
                        goalValue = goalInputs[benchmark.id] ?: "",
                        onGoalValueChange = { goalInputs[benchmark.id] = it }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Motivational message based on experience
            if (benchmarksWithCurrentValues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                MotivationalCard(trainingYears = trainingYears, age = age)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Buttons
        Column {
            Button(
                onClick = {
                    onUpdateGoalBenchmarks(goalInputs.filterValues { it.isNotBlank() })
                    onContinue()
                },
                enabled = hasAnyGoal || benchmarksWithCurrentValues.isEmpty(),
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
                    text = if (benchmarksWithCurrentValues.isEmpty()) "Continue" else "Set Goals",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }

            if (benchmarksWithCurrentValues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "I'll set goals later",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Get sport-specific goal title and subtitle
 */
private fun getGoalScreenTitleForSport(sport: UnifiedSport?): Pair<String, String> {
    if (sport == null) {
        return "Your Goals" to "What do you want to achieve?"
    }

    return when (sport.category) {
        SportCategory.STRENGTH -> {
            "Your Goal PRs" to "What numbers do you want to hit?"
        }
        SportCategory.ENDURANCE -> {
            "Your Goal Times" to "What times are you chasing?"
        }
        SportCategory.BALL, SportCategory.TEAM -> {
            "Your Athletic Goals" to "Where do you want to be?"
        }
        SportCategory.COMBAT -> {
            "Your Fighting Goals" to "Where do you want to be?"
        }
        SportCategory.WATER -> {
            "Your Goal Times" to "What times do you want to hit?"
        }
        SportCategory.GYMNASTICS -> {
            "Your Skill Goals" to "What do you want to achieve?"
        }
        SportCategory.OUTDOOR -> {
            "Your Goals" to "What are you working towards?"
        }
        SportCategory.ATHLETICS -> {
            "Your Goal PRs" to "What personal bests are you chasing?"
        }
        else -> {
            "Your Goals" to "What do you want to achieve?"
        }
    }
}

/**
 * Goal badge showing the sport with target icon
 */
@Composable
private fun GoalBadge(sport: UnifiedSport) {
    Surface(
        color = NayaOrange.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = NayaOrange,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Goal Setting: ${sport.name}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Goal benchmark input with current value comparison
 */
@Composable
private fun GoalBenchmarkInputField(
    benchmark: BenchmarkDefinition,
    currentValue: String,
    goalValue: String,
    onGoalValueChange: (String) -> Unit
) {
    Column {
        // Benchmark name with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getGoalBenchmarkIcon(benchmark.type),
                contentDescription = null,
                tint = NayaOrange,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = benchmark.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current vs Goal comparison row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current value (read-only)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentValue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = benchmark.unit,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = NayaOrange,
                modifier = Modifier
                    .padding(top = 28.dp)
                    .size(24.dp)
            )

            // Goal value (editable)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Goal",
                    fontSize = 12.sp,
                    color = NayaOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = goalValue,
                    onValueChange = { newValue ->
                        when (benchmark.type) {
                            BenchmarkType.WEIGHT, BenchmarkType.HEIGHT,
                            BenchmarkType.DISTANCE, BenchmarkType.REPS,
                            BenchmarkType.SCORE -> {
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    onGoalValueChange(newValue)
                                }
                            }
                            BenchmarkType.TIME -> {
                                if (newValue.isEmpty() || newValue.matches(Regex("^[\\d:]*$"))) {
                                    onGoalValueChange(newValue)
                                }
                            }
                            BenchmarkType.LEVEL, BenchmarkType.SPEED -> {
                                onGoalValueChange(newValue)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Target",
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    },
                    suffix = {
                        Text(
                            text = benchmark.unit,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when (benchmark.type) {
                            BenchmarkType.WEIGHT, BenchmarkType.HEIGHT,
                            BenchmarkType.DISTANCE, BenchmarkType.REPS,
                            BenchmarkType.SCORE -> KeyboardType.Decimal
                            BenchmarkType.TIME -> KeyboardType.Number
                            BenchmarkType.LEVEL, BenchmarkType.SPEED -> KeyboardType.Text
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NayaOrange,
                        unfocusedBorderColor = NayaOrange.copy(alpha = 0.5f),
                        cursorColor = NayaOrange,
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

/**
 * Message when no current benchmarks exist
 */
@Composable
private fun NoCurrentBenchmarksMessage() {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No benchmarks to set goals for yet",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "You can set goals later in your profile",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Motivational card based on experience level
 */
@Composable
private fun MotivationalCard(trainingYears: Int?, age: Int?) {
    val message = when {
        trainingYears == null || trainingYears == 0 -> {
            "Set realistic goals for your first year - consistency beats intensity!"
        }
        trainingYears in 1..2 -> {
            "You're in the sweet spot for rapid gains. Dream big!"
        }
        trainingYears in 3..5 -> {
            "Intermediate athletes make steady progress. Every PR counts!"
        }
        else -> {
            "Advanced athletes know: small improvements = big victories"
        }
    }

    Surface(
        color = NayaPrimary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = NayaOrange,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Get icon for benchmark type (goal version)
 */
private fun getGoalBenchmarkIcon(type: BenchmarkType): ImageVector {
    return when (type) {
        BenchmarkType.WEIGHT -> Icons.Default.FitnessCenter
        BenchmarkType.TIME -> Icons.Default.Timer
        BenchmarkType.DISTANCE -> Icons.Default.Straighten
        BenchmarkType.REPS -> Icons.Default.Repeat
        BenchmarkType.HEIGHT -> Icons.Default.Height
        BenchmarkType.SPEED -> Icons.Default.Speed
        BenchmarkType.SCORE -> Icons.Default.Star
        BenchmarkType.LEVEL -> Icons.Default.TrendingUp
    }
}