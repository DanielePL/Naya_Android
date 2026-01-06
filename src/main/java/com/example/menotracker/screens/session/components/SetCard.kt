package com.example.menotracker.screens.session.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.WorkoutSessionRepository
import com.example.menotracker.data.models.RepMaxCalculator
import com.example.menotracker.data.models.UserStrengthProfile
import com.example.menotracker.data.models.WeightRecommendation
import com.example.menotracker.data.models.getWeightRecommendation
import com.example.menotracker.debug.DebugLogger
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.viewmodels.VelocityMetricsData
import com.example.menotracker.viewmodels.WorkoutSessionViewModel
import com.example.menotracker.viewmodels.SetType
import com.example.menotracker.data.models.ILBTestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Design System colors
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)

/**
 * SetCard - Individual set row with weight/reps input and completion state
 *
 * Features:
 * - Weight and reps input fields
 * - Tap to complete (success), Long-press to fail
 * - VBT camera integration
 * - Video playback for recorded sets
 * - PR-based weight recommendations (NEW)
 */
@Composable
fun SetCard(
    setId: String,
    setNumber: Int,
    targetReps: Int,
    targetWeight: Double,
    targetRestSeconds: Int,
    isActive: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean,
    completedReps: Int?,
    completedWeight: Double?,
    videoPath: String?,
    vbtVelocity: Float?,
    velocityMetrics: VelocityMetricsData? = null,  // Full VBT metrics from backend
    currentReps: String,
    currentWeight: String,
    exerciseName: String, // For weight recommendations
    strengthProfile: UserStrengthProfile?, // For weight recommendations
    workoutSessionViewModel: WorkoutSessionViewModel,
    workoutSessionRepository: WorkoutSessionRepository,
    coroutineScope: CoroutineScope,
    onComplete: (reps: Int, weight: Double) -> Unit,
    onFail: (reps: Int, weight: Double) -> Unit,
    onUncomplete: () -> Unit,
    onVBTClick: () -> Unit,
    onVideoClick: (String, VelocityMetricsData?) -> Unit,  // Now passes metrics for video playback
    onAdjustNextSetWeight: ((Double) -> Unit)? = null,  // Callback to adjust NEXT set's weight
    // ILB: AMRAP mode support
    setType: SetType = SetType.STANDARD,
    ilbTestResult: ILBTestResult? = null,  // Result from ILB test (shown after AMRAP completion)
    onAMRAPComplete: ((reps: Int, weight: Double) -> Unit)? = null  // Callback for ILB processing
) {
    // Haptic feedback for long-press
    val hapticFeedback = LocalHapticFeedback.current

    // DEBUG: Log every recomposition
    DebugLogger.logRecomposition("SetCard #$setNumber")
    DebugLogger.logSetStateUpdate(
        setId = setId,
        isCompleted = isCompleted,
        videoPath = videoPath
    )

    // ✅ Initialize with values from ViewModel (persistent!)
    var reps by remember { mutableStateOf(currentReps) }
    var weight by remember { mutableStateOf(currentWeight) }

    // ✅ Sync local state with ViewModel when props change
    LaunchedEffect(currentReps) {
        if (currentReps != reps) {
            reps = currentReps
        }
    }
    LaunchedEffect(currentWeight) {
        if (currentWeight != weight) {
            weight = currentWeight
        }
    }

    // 🎯 PR-based weight recommendation (calculated in background)
    val weightRecommendation: WeightRecommendation? = remember(exerciseName, targetReps, strengthProfile) {
        strengthProfile?.getWeightRecommendation(exerciseName, targetReps)
    }

    // 📊 Get the variant 1RM for real-time calculations
    val variant1RM: Float? = weightRecommendation?.variant1RM

    // 🔄 Track if user has MANUALLY edited each field
    // Once user manually edits a field, we stop auto-calculating it
    // This allows athletes to override the submax recommendations
    var userEditedWeight by remember { mutableStateOf(false) }
    var userEditedReps by remember { mutableStateOf(false) }

    // PREVIOUS column: Shows weight x reps from LAST TRAINING SESSION
    // targetWeight is pre-filled from StatisticsRepository.getLastUsedWeights()
    // If no previous data exists, show PR-based recommendation or just reps
    val previousDisplay = when {
        targetWeight > 0 && targetReps > 0 -> "${targetWeight.toInt()} x $targetReps"
        weightRecommendation != null && targetReps > 0 -> "${weightRecommendation.recommendedWeight.toInt()} x $targetReps"
        targetReps > 0 -> "- x $targetReps"
        else -> "-"
    }

    // KG Placeholder: Use PR-based recommendation if available, else last training weight
    // This auto-fills the weight field based on the user's strength profile
    val weightPlaceholder = when {
        weightRecommendation != null -> "${weightRecommendation.recommendedWeight.toInt()}"
        targetWeight > 0 -> "${targetWeight.toInt()}"
        else -> "0"
    }

    // Determine colors based on completion state - Naya Orange for success
    val setNumberColor = when {
        isFailed -> Color.Red
        isCompleted -> orangeGlow
        else -> textWhite
    }
    val setNumberBgColor = when {
        isFailed -> Color.Red.copy(alpha = 0.2f)
        isCompleted -> orangeGlow.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    // VBT Metrics colors
    val vbtCyan = Color(0xFF00D9FF)
    val vbtGreen = Color(0xFF00FF88)
    val vbtRed = Color(0xFFFF6B6B)

    // Main Column containing set row + optional VBT metrics row
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) cardBackground else Color.Transparent
            )
    ) {
    // Hevy-style single row layout
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SET number or AMRAP badge (changes color based on success/fail)
        val isAMRAP = setType == SetType.AMRAP
        val amrapOrange = Color(0xFFFF6B00)  // Bright orange for AMRAP

        if (isAMRAP) {
            // AMRAP Badge - distinctive styling
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isCompleted -> amrapOrange.copy(alpha = 0.3f)
                            else -> amrapOrange.copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCompleted) amrapOrange else amrapOrange.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MAX",
                    color = if (isCompleted) amrapOrange else amrapOrange.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            // Standard set number
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(setNumberBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$setNumber",
                    color = setNumberColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // PREVIOUS column - shows last training weight x reps (or AMRAP instruction)
        Column(modifier = Modifier.weight(1f)) {
            if (isAMRAP) {
                // AMRAP mode: Show test weight and instruction
                Text(
                    text = if (isCompleted && ilbTestResult != null)
                        "${ilbTestResult.new1RM.toInt()}kg 1RM"
                    else
                        "Go to failure!",
                    color = if (isCompleted) amrapOrange else textGray,
                    fontSize = 14.sp,
                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                )
            } else {
                Text(
                    text = previousDisplay,
                    color = textGray,
                    fontSize = 14.sp
                )
            }
        }

        // KG input with recommendation placeholder
        // Max 4 characters (up to 999.9kg or 9999kg without decimal)
        // 🔄 REAL-TIME CALC: When weight changes, auto-calculate reps based on 1RM
        androidx.compose.foundation.text.BasicTextField(
            value = if (isCompleted) "${completedWeight?.toInt() ?: 0}" else weight,
            onValueChange = { newValue ->
                // Allow digits and one decimal point, max 5 chars (e.g., "500", "99.5")
                val filtered = newValue.filter { it.isDigit() || it == '.' }
                val decimalCount = filtered.count { it == '.' }
                if (decimalCount <= 1 && filtered.length <= 5) {
                    // Validate max weight (999kg realistic max)
                    val numValue = filtered.toDoubleOrNull()
                    if (numValue == null || numValue <= 999) {
                        weight = filtered
                        userEditedWeight = true  // User manually edited weight

                        // 🔄 REAL-TIME REPS CALCULATION
                        // Only auto-calc reps if user hasn't manually edited reps yet
                        // This allows athletes to override recommendations
                        if (variant1RM != null && numValue != null && numValue > 0 && !userEditedReps) {
                            val percentage = (numValue / variant1RM).toFloat()
                            val calculatedReps = RepMaxCalculator.getRepsForPercentage(percentage)
                            if (calculatedReps in 1..30) {
                                reps = calculatedReps.toString()
                            }
                        }

                        workoutSessionViewModel.updateCurrentInputs(setId, reps, weight)
                        coroutineScope.launch {
                            workoutSessionRepository.updateSetCurrentInputs(setId, reps, weight)
                        }
                    }
                }
            },
            modifier = Modifier
                .width(56.dp)
                .background(
                    if (isCompleted) Color.Transparent else textGray.copy(alpha = 0.1f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 4.dp, vertical = 8.dp),
            textStyle = TextStyle(
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            enabled = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (weight.isEmpty() && !isCompleted) {
                        Text(
                            text = weightPlaceholder,
                            color = if (weightRecommendation != null)
                                orangeGlow.copy(alpha = 0.5f)
                            else
                                textGray.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // REPS input - max 3 digits (up to 999 reps, realistic max ~100)
        // 🔄 REAL-TIME CALC: When reps changes, auto-calculate weight based on 1RM
        androidx.compose.foundation.text.BasicTextField(
            value = if (isCompleted) "${completedReps ?: 0}" else reps,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }
                // Max 3 digits, max value 999 (realistically no one does 1000 reps)
                if (filtered.length <= 3) {
                    val numValue = filtered.toIntOrNull()
                    if (numValue == null || numValue <= 999) {
                        reps = filtered
                        userEditedReps = true  // User manually edited reps

                        // 🔄 REAL-TIME WEIGHT CALCULATION
                        // Only auto-calc weight if user hasn't manually edited weight yet
                        // This allows athletes to override recommendations
                        if (variant1RM != null && numValue != null && numValue > 0 && !userEditedWeight) {
                            val calculatedWeight = RepMaxCalculator.calculateWeightForReps(
                                oneRM = variant1RM,
                                targetReps = numValue,
                                roundTo = 2.5f
                            )
                            if (calculatedWeight > 0 && calculatedWeight <= 999) {
                                weight = calculatedWeight.toInt().toString()
                            }
                        }

                        workoutSessionViewModel.updateCurrentInputs(setId, reps, weight)
                        coroutineScope.launch {
                            workoutSessionRepository.updateSetCurrentInputs(setId, reps, weight)
                        }
                    }
                }
            },
            modifier = Modifier
                .width(48.dp)
                .background(
                    if (isCompleted) Color.Transparent else textGray.copy(alpha = 0.1f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 4.dp, vertical = 8.dp),
            textStyle = TextStyle(
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            enabled = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (reps.isEmpty() && !isCompleted) {
                        Text(
                            // Show "–" if no target reps set (user must enter), otherwise show target
                            text = if (targetReps > 0) "$targetReps" else "–",
                            color = textGray.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Camera button - enabled for any uncompleted set
        IconButton(
            onClick = onVBTClick,
            enabled = !isCompleted,
            modifier = Modifier.size(40.dp)
        ) {
            if (videoPath != null) {
                // Show video thumbnail/play icon if video exists
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF4A90E2))
                        .clickable {
                            android.util.Log.e("VBT_DEBUG", "▶️ SetCard PLAY CLICKED!")
                            android.util.Log.e("VBT_DEBUG", "▶️ setId: $setId")
                            android.util.Log.e("VBT_DEBUG", "▶️ videoPath: $videoPath")
                            android.util.Log.e("VBT_DEBUG", "▶️ velocityMetrics: $velocityMetrics")
                            android.util.Log.e("VBT_DEBUG", "▶️ velocityMetrics.peakVelocity: ${velocityMetrics?.peakVelocity}")
                            android.util.Log.e("VBT_DEBUG", "▶️ velocityMetrics.totalReps: ${velocityMetrics?.totalReps}")
                            onVideoClick(videoPath, velocityMetrics)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = textWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = "Record form check",
                    tint = if (!isCompleted) Color(0xFF4A90E2) else textGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ✅ Custom completion button with Long-Press for FAIL
        // Tap = Success (orange check), Long-Press = Fail (red X)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isFailed -> Color.Red.copy(alpha = 0.15f)
                        isCompleted -> orangeGlow.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                )
                .pointerInput(isCompleted, isFailed) {
                    detectTapGestures(
                        onTap = {
                            if (!isCompleted) {
                                // Normal tap = Success
                                // User MUST enter reps if no target was set (targetReps = 0)
                                val enteredReps = reps.toIntOrNull()
                                val finalReps = enteredReps ?: if (targetReps > 0) targetReps else null
                                val finalWeight = weight.toDoubleOrNull()
                                    ?: weightRecommendation?.recommendedWeight?.toDouble()
                                    ?: targetWeight
                                if (finalReps != null && finalReps > 0 && finalWeight > 0) {
                                    onComplete(finalReps, finalWeight)
                                    // ILB: If AMRAP mode, also trigger ILB processing
                                    if (setType == SetType.AMRAP && onAMRAPComplete != null) {
                                        onAMRAPComplete(finalReps, finalWeight)
                                    }
                                }
                                // If finalReps is null, user needs to enter reps first (no-op)
                            } else {
                                // Tap on completed = Uncomplete (allow corrections)
                                onUncomplete()
                            }
                        },
                        onLongPress = {
                            if (!isCompleted) {
                                // Long-press = FAIL
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                val finalReps = reps.toIntOrNull() ?: 0
                                val finalWeight = weight.toDoubleOrNull()
                                    ?: weightRecommendation?.recommendedWeight?.toDouble()
                                    ?: targetWeight
                                if (finalWeight > 0) {
                                    onFail(finalReps, finalWeight)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                isFailed -> {
                    // ❌ Red X for failed
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
                isCompleted -> {
                     // ✓ Orange check for success (Naya branding)
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = orangeGlow,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    // Empty checkbox style
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Transparent)
                            .then(
                                Modifier.background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Empty state - just border
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(textGray.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    } // End of main Row

    // VBT Display - Compact mode with toggle
    if (velocityMetrics != null) {
        CompactVBTDisplay(
            velocityMetrics = velocityMetrics,
            plannedLoad = targetWeight,  // For VBT formula calculation
            onLoadAdjustmentAccepted = { adjustmentKg ->
                // Adjust the NEXT set's weight (not this set)
                onAdjustNextSetWeight?.invoke(adjustmentKg)
            }
        )
    }
    } // End of Column
}

/**
 * Compact VBT Metrics Row - displays key velocity metrics inline
 */
@Composable
private fun VelocityMetricsRow(
    metrics: VelocityMetricsData,
    vbtCyan: Color,
    vbtGreen: Color,
    vbtRed: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, end = 8.dp, bottom = 8.dp) // Align with content, skip set number column
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A1A1F))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Peak Velocity
        MetricChip(
            label = "Peak",
            value = String.format("%.2f", metrics.peakVelocity),
            unit = "m/s",
            color = vbtCyan
        )

        // Velocity Drop (color based on value)
        val dropColor = when {
            metrics.velocityDrop < 10 -> vbtGreen
            metrics.velocityDrop < 20 -> Color(0xFFFFD93D)  // Yellow/warning
            else -> vbtRed
        }
        MetricChip(
            label = "Drop",
            value = String.format("%.0f", metrics.velocityDrop),
            unit = "%",
            color = dropColor
        )

        // Technique Score
        val techniqueColor = when {
            metrics.techniqueScore >= 80 -> vbtGreen
            metrics.techniqueScore >= 60 -> Color(0xFFFFD93D)
            else -> vbtRed
        }
        MetricChip(
            label = "Tech",
            value = String.format("%.0f", metrics.techniqueScore),
            unit = "",
            color = techniqueColor
        )

        // Overall Grade Badge
        GradeBadge(grade = metrics.overallGrade)
    }
}

/**
 * Compact metric chip for VBT row
 */
@Composable
private fun MetricChip(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFF666666),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    color = color.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 1.dp, bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * Grade badge (A, B, C, D, F)
 */
@Composable
private fun GradeBadge(grade: String) {
    val gradeColor = when (grade) {
        "A", "A+" -> Color(0xFF00FF88)
        "B", "B+" -> Color(0xFF00D9FF)
        "C", "C+" -> Color(0xFFFFD93D)
        "D" -> Color(0xFFFF9500)
        else -> Color(0xFFFF6B6B)
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(gradeColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = grade,
            color = gradeColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPACT VBT DISPLAY - Toggle between Simple (Scores) and Nerd (Metrics) Mode
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Compact VBT Display with mode toggle
 *
 * Simple Mode: Power + Technique glassmorphism boxes + Load adjustment
 * Nerd Mode: Peak, Drop%, Tech, Grade (detailed metrics)
 *
 * Tap to toggle between modes
 */
@Composable
private fun CompactVBTDisplay(
    velocityMetrics: VelocityMetricsData,
    plannedLoad: Double,  // Target weight for VBT formula
    onLoadAdjustmentAccepted: (Double) -> Unit
) {
    // Toggle state - Simple mode by default (user-friendly)
    var isNerdMode by remember { mutableStateOf(false) }

    // Velocity-based load adjustment estimation
    // Uses absolute velocity thresholds based on exercise type
    val maxAdjustmentPercent = 0.10  // MAX ±10% per set (industry standard)
    val absoluteMaxKg = 12.5  // ABSOLUTE max: never suggest more than ±12.5kg
    val maxAdjustmentKg = minOf(plannedLoad * maxAdjustmentPercent, absoluteMaxKg)

    // For squats/deadlifts, typical velocity zones:
    // > 0.8 m/s = light/speed work → could add load
    // 0.5-0.8 m/s = working sets → normal
    // 0.3-0.5 m/s = heavy/near max → consider reducing
    // < 0.3 m/s = at/near 1RM → definitely reduce
    val peakVel = velocityMetrics.peakVelocity
    val rawAdjustment = when {
        peakVel > 1.0 -> plannedLoad * 0.10   // Very fast → +10%
        peakVel > 0.8 -> plannedLoad * 0.05   // Fast → +5%
        peakVel < 0.25 -> plannedLoad * -0.10 // Near 1RM → -10%
        peakVel < 0.35 -> plannedLoad * -0.075 // Very heavy → -7.5%
        peakVel < 0.45 -> plannedLoad * -0.05 // Heavy → -5%
        else -> 0.0  // Normal working range (0.45-0.8 m/s)
    }
    // Round to nearest 2.5kg and cap
    val cappedAdjustment = rawAdjustment.coerceIn(-maxAdjustmentKg, maxAdjustmentKg)
    val loadAdjustment: Double? = (kotlin.math.round(cappedAdjustment / 2.5) * 2.5)
        .takeIf { kotlin.math.abs(it) >= 2.5 }

    // Auto-regulation checkbox state
    var adjustmentAccepted by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: VBT Metrics Box - Tap to toggle mode
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A1A1F))
                .clickable { isNerdMode = !isNerdMode }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isNerdMode) {
                // NERD MODE: Detailed metrics
                NerdModeContent(velocityMetrics)
            } else {
                // SIMPLE MODE: Power + Technique scores
                SimpleModeContent(velocityMetrics)
            }
        }

        // RIGHT: Load Adjustment Box (if recommendation available)
        if (loadAdjustment != null && loadAdjustment != 0.0) {
            LoadAdjustmentBox(
                adjustmentKg = loadAdjustment,
                isAccepted = adjustmentAccepted,
                onToggle = { accepted ->
                    adjustmentAccepted = accepted
                    if (accepted) {
                        onLoadAdjustmentAccepted(loadAdjustment)
                    }
                }
            )
        }
    }
}

/**
 * Simple Mode: Power + Technique glassmorphism score boxes
 */
@Composable
private fun SimpleModeContent(metrics: VelocityMetricsData) {
    // Power score based on peak velocity (higher = more power)
    // Scale: 0.5m/s = 50, 1.0m/s = 75, 1.5m/s = 100
    val powerScore = ((metrics.peakVelocity / 1.5f) * 100).coerceIn(0f, 100f).toInt()
    val powerColor = when {
        powerScore >= 80 -> Color(0xFF00FF88)  // Green
        powerScore >= 60 -> Color(0xFFFFD93D)  // Yellow
        else -> Color(0xFFFF6B6B)              // Red
    }

    // Technique score from backend (ensure valid integer)
    val techniqueScore = metrics.techniqueScore.takeIf { it.isFinite() }?.toInt() ?: 0
    val techniqueColor = when {
        techniqueScore >= 80 -> Color(0xFF00FF88)  // Green
        techniqueScore >= 60 -> Color(0xFFFFD93D)  // Yellow
        else -> Color(0xFFFF6B6B)                  // Red
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // POWER Score Box - Glassmorphism
        GlassmorphismScoreBox(
            label = "POWER",
            score = powerScore,
            color = powerColor
        )

        // TECHNIQUE Score Box - Glassmorphism
        GlassmorphismScoreBox(
            label = "TECH",
            score = techniqueScore,
            color = techniqueColor
        )
    }
}

/**
 * Nerd Mode: Detailed metrics (Peak, Drop, Tech, Grade)
 */
@Composable
private fun NerdModeContent(metrics: VelocityMetricsData) {
    val vbtCyan = Color(0xFF00D9FF)
    val vbtGreen = Color(0xFF00FF88)
    val vbtRed = Color(0xFFFF6B6B)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Peak Velocity
        MetricChip(
            label = "Peak",
            value = String.format("%.2f", metrics.peakVelocity),
            unit = "m/s",
            color = vbtCyan
        )

        // Velocity Drop
        val dropColor = when {
            metrics.velocityDrop < 10 -> vbtGreen
            metrics.velocityDrop < 20 -> Color(0xFFFFD93D)
            else -> vbtRed
        }
        MetricChip(
            label = "Drop",
            value = String.format("%.0f", metrics.velocityDrop),
            unit = "%",
            color = dropColor
        )

        // Technique
        val techniqueColor = when {
            metrics.techniqueScore >= 80 -> vbtGreen
            metrics.techniqueScore >= 60 -> Color(0xFFFFD93D)
            else -> vbtRed
        }
        MetricChip(
            label = "Tech",
            value = String.format("%.0f", metrics.techniqueScore),
            unit = "",
            color = techniqueColor
        )

        // Grade Badge
        GradeBadge(grade = metrics.overallGrade)
    }
}

/**
 * Glassmorphism Score Box - Power/Technique display
 * Fixed width ensures all boxes are same size
 */
@Composable
private fun GlassmorphismScoreBox(
    label: String,
    score: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .width(90.dp)  // Fixed width - all boxes same size
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.2f),
                        color.copy(alpha = 0.05f)
                    )
                )
            )
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = score.toString(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Load Adjustment Box - Auto-regulation checkbox
 * Shows +/- kg recommendation with up/down arrow
 */
@Composable
private fun LoadAdjustmentBox(
    adjustmentKg: Double,
    isAccepted: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val isIncrease = adjustmentKg > 0
    val color = if (isIncrease) Color(0xFF00FF88) else Color(0xFFFF6B6B)
    val arrow = if (isIncrease) "↑" else "↓"
    val sign = if (isIncrease) "+" else ""

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = if (isAccepted) 0.3f else 0.15f),
                        color.copy(alpha = if (isAccepted) 0.15f else 0.05f)
                    )
                )
            )
            .border(
                width = if (isAccepted) 2.dp else 1.dp,
                color = color.copy(alpha = if (isAccepted) 0.8f else 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onToggle(!isAccepted) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = arrow,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${sign}${adjustmentKg.toInt()}kg",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // Checkbox indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isAccepted) color else Color.Transparent)
                .border(1.5.dp, color, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isAccepted) {
                Text(
                    text = "✓",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
