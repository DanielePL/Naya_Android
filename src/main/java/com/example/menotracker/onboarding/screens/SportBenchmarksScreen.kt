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
 * UNIVERSAL Sport Benchmarks Screen
 *
 * Dynamically adapts to the user's selected sport from onboarding.
 * Shows sport-specific benchmarks instead of hardcoded powerlifting PRs.
 *
 * Examples:
 * - Powerlifter → Squat, Bench, Deadlift (kg)
 * - Runner → 5K, 10K, Marathon times
 * - Swimmer → 50m, 100m, 400m times
 * - Climber → Boulder grade, Lead grade, Pull-ups
 * - Soccer player → Vertical Jump, 40m Sprint, Agility
 */
@Composable
fun SportBenchmarksScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedSport: UnifiedSport?,
    currentBenchmarks: Map<String, String>,
    onUpdateBenchmarks: (Map<String, String>) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    // Get relevant benchmarks for the selected sport
    val benchmarks = remember(selectedSport) {
        selectedSport?.let { SportBenchmarks.getBenchmarksForSport(it) } ?: emptyList()
    }

    // State for each benchmark input
    val benchmarkInputs = remember(benchmarks) {
        mutableStateMapOf<String, String>().apply {
            benchmarks.forEach { benchmark ->
                this[benchmark.id] = currentBenchmarks[benchmark.id] ?: ""
            }
        }
    }

    // Bodyweight is always optional (useful for relative strength)
    var bodyweightText by remember {
        mutableStateOf(currentBenchmarks["bodyweight"] ?: "")
    }

    val hasAnyBenchmark = benchmarkInputs.values.any { it.isNotBlank() }

    // Get sport-specific title and subtitle
    val (title, subtitle) = getScreenTitleForSport(selectedSport)

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
                SportBadge(sport = sport)
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

            // Benchmark inputs
            benchmarks.forEach { benchmark ->
                BenchmarkInputField(
                    benchmark = benchmark,
                    value = benchmarkInputs[benchmark.id] ?: "",
                    onValueChange = { benchmarkInputs[benchmark.id] = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bodyweight (always optional, useful for relative strength calculations)
            if (selectedSport?.category == SportCategory.STRENGTH ||
                selectedSport?.category == SportCategory.COMBAT ||
                selectedSport?.category == SportCategory.GYMNASTICS) {
                Spacer(modifier = Modifier.height(8.dp))
                BenchmarkInputField(
                    benchmark = BenchmarkDefinition(
                        id = "bodyweight",
                        name = "Bodyweight",
                        nameDE = "Körpergewicht",
                        type = BenchmarkType.WEIGHT,
                        unit = "kg",
                        placeholder = "75",
                        isOptional = true,
                        description = "Optional - for relative strength"
                    ),
                    value = bodyweightText,
                    onValueChange = { bodyweightText = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Buttons
        Column {
            Button(
                onClick = {
                    // Build updated benchmarks map
                    val updatedBenchmarks = benchmarkInputs.toMutableMap()
                    if (bodyweightText.isNotBlank()) {
                        updatedBenchmarks["bodyweight"] = bodyweightText
                    }
                    onUpdateBenchmarks(updatedBenchmarks.filterValues { it.isNotBlank() })
                    onContinue()
                },
                enabled = hasAnyBenchmark,
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

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = getSkipTextForSport(selectedSport),
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Get sport-specific title and subtitle
 */
private fun getScreenTitleForSport(sport: UnifiedSport?): Pair<String, String> {
    if (sport == null) {
        return "Your Current Level" to "Enter what you know - you can add more later"
    }

    return when (sport.category) {
        SportCategory.STRENGTH -> {
            "Your Current PRs" to "Enter your personal records in kg"
        }
        SportCategory.ENDURANCE -> {
            "Your Current Times" to "Enter your best times - we'll track your progress"
        }
        SportCategory.BALL, SportCategory.TEAM -> {
            "Your Athletic Benchmarks" to "These help us build your training program"
        }
        SportCategory.COMBAT -> {
            "Your Fighting Fitness" to "Strength and conditioning benchmarks"
        }
        SportCategory.WATER -> {
            "Your Swim Times" to "Enter your best times for each distance"
        }
        SportCategory.GYMNASTICS -> {
            "Your Skills & Strength" to "Bodyweight strength and flexibility"
        }
        SportCategory.OUTDOOR -> {
            if (sport.id.contains("climb") || sport.id.contains("boulder")) {
                "Your Climbing Level" to "Grades and strength benchmarks"
            } else {
                "Your Fitness Level" to "Current performance benchmarks"
            }
        }
        SportCategory.ATHLETICS -> {
            "Your Track & Field PRs" to "Personal bests in your events"
        }
        SportCategory.RACKET -> {
            "Your Athletic Base" to "Speed and agility benchmarks"
        }
        SportCategory.WINTER -> {
            "Your Athletic Base" to "Fitness benchmarks for your sport"
        }
        SportCategory.PRECISION -> {
            "Your Fitness Level" to "General fitness benchmarks"
        }
        SportCategory.OTHER -> {
            "Your Current Level" to "Relevant fitness benchmarks"
        }
    }
}

/**
 * Get sport-specific skip text
 */
private fun getSkipTextForSport(sport: UnifiedSport?): String {
    return when (sport?.category) {
        SportCategory.STRENGTH -> "I don't know my PRs yet"
        SportCategory.ENDURANCE -> "I don't know my times yet"
        SportCategory.WATER -> "I don't know my swim times"
        SportCategory.ATHLETICS -> "I'll add these later"
        else -> "Skip for now"
    }
}

/**
 * Sport badge showing the selected sport
 */
@Composable
private fun SportBadge(sport: UnifiedSport) {
    Surface(
        color = NayaPrimary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getCategoryIcon(sport.category),
                contentDescription = null,
                tint = NayaOrange,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = sport.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Universal benchmark input field
 */
@Composable
private fun BenchmarkInputField(
    benchmark: BenchmarkDefinition,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getBenchmarkIcon(benchmark.type),
                contentDescription = null,
                tint = NayaOrange,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = benchmark.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (benchmark.isOptional) {
                Text(
                    text = "(optional)",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        benchmark.description?.let { desc ->
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 26.dp, top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Allow different input types based on benchmark type
                when (benchmark.type) {
                    BenchmarkType.WEIGHT, BenchmarkType.HEIGHT,
                    BenchmarkType.DISTANCE, BenchmarkType.REPS,
                    BenchmarkType.SCORE -> {
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            onValueChange(newValue)
                        }
                    }
                    BenchmarkType.TIME -> {
                        // Allow time format: digits and colons
                        if (newValue.isEmpty() || newValue.matches(Regex("^[\\d:]*$"))) {
                            onValueChange(newValue)
                        }
                    }
                    BenchmarkType.LEVEL, BenchmarkType.SPEED -> {
                        // Allow alphanumeric for grades like "V5" or "7a"
                        onValueChange(newValue)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "e.g. ${benchmark.placeholder}",
                    color = Color.White.copy(alpha = 0.3f)
                )
            },
            suffix = {
                Text(
                    text = benchmark.unit,
                    color = Color.White.copy(alpha = 0.5f)
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

/**
 * Get icon for benchmark type
 */
private fun getBenchmarkIcon(type: BenchmarkType): ImageVector {
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

/**
 * Get icon for sport category
 */
private fun getCategoryIcon(category: SportCategory): ImageVector {
    return when (category) {
        SportCategory.STRENGTH -> Icons.Default.FitnessCenter
        SportCategory.BALL -> Icons.Default.SportsSoccer
        SportCategory.COMBAT -> Icons.Default.SportsMartialArts
        SportCategory.ENDURANCE -> Icons.Default.DirectionsRun
        SportCategory.RACKET -> Icons.Default.SportsTennis
        SportCategory.WATER -> Icons.Default.Pool
        SportCategory.WINTER -> Icons.Default.AcUnit
        SportCategory.ATHLETICS -> Icons.Default.EmojiEvents
        SportCategory.GYMNASTICS -> Icons.Default.AccessibilityNew
        SportCategory.OUTDOOR -> Icons.Default.Terrain
        SportCategory.PRECISION -> Icons.Default.GpsFixed
        SportCategory.TEAM -> Icons.Default.Groups
        SportCategory.OTHER -> Icons.Default.Sports
    }
}