package com.example.menotracker.screens.nutrition.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.repository.HydrationRepository

// ═══════════════════════════════════════════════════════════════
// COLORS
// ═══════════════════════════════════════════════════════════════

private val WaterBlue = Color(0xFF2196F3)
private val WaterBlueDark = Color(0xFF1565C0)
private val WaterBlueLight = Color(0xFF64B5F6)
private val Surface = Color(0xFF1C1C1C)
private val SurfaceVariant = Color(0xFF262626)
private val TextPrimary = Color(0xFFFAFAFA)
private val TextSecondary = Color(0xFF999999)
private val SuccessGreen = Color(0xFF4CAF50)

/**
 * Collapsible hydration tracking card for NutritionScreen
 * Collapsed by default to save space, expands to show quick-add buttons
 */
@Composable
fun HydrationCard(
    hydrationLog: HydrationRepository.HydrationLog?,
    onAddWater: (Int) -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collapsed by default to keep Hero Card visible without scrolling
    var isExpanded by remember { mutableStateOf(false) }

    val currentMl = hydrationLog?.waterIntakeMl ?: 0
    val targetMl = hydrationLog?.targetMl ?: 2500
    val progress = if (targetMl > 0) (currentMl.toFloat() / targetMl).coerceIn(0f, 1f) else 0f
    val isGoalReached = currentMl >= targetMl

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "progress"
    )

    // Color changes when goal reached
    val progressColor by animateColorAsState(
        targetValue = if (isGoalReached) SuccessGreen else WaterBlue,
        animationSpec = tween(300),
        label = "color"
    )

    // Glassmorphism container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF252525).copy(alpha = 0.9f),
                        Color(0xFF1C1C1C).copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Compact Header Row (always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Icon + Title + Progress
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Water drop icon (smaller)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(WaterBlueLight, WaterBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGoalReached) Icons.Filled.Check else Icons.Filled.WaterDrop,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Title + Progress inline
                    Text(
                        text = "Hydration",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    // Compact progress bar (always visible)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(progressColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Amount + Expand toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${formatWaterAmount(currentMl)} / ${formatWaterAmount(targetMl)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = progressColor
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = WaterBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expandable Content: Quick Add Buttons
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Add Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HydrationRepository.quickAddAmounts.forEach { amount ->
                            QuickAddButton(
                                amountMl = amount,
                                onClick = { onAddWater(amount) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick add button for preset water amounts
 */
@Composable
private fun QuickAddButton(
    amountMl: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = SurfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = WaterBlue,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatWaterAmountShort(amountMl),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

/**
 * Format water amount for display (e.g., "1.5 L" or "750 ml")
 */
private fun formatWaterAmount(ml: Int): String {
    return if (ml >= 1000) {
        val liters = ml / 1000f
        if (liters == liters.toInt().toFloat()) {
            "${liters.toInt()} L"
        } else {
            "${"%.1f".format(liters)} L"
        }
    } else {
        "$ml ml"
    }
}

/**
 * Short format for buttons (e.g., "250ml", "1L")
 */
private fun formatWaterAmountShort(ml: Int): String {
    return when {
        ml >= 1000 -> "${ml / 1000}L"
        else -> "${ml}ml"
    }
}

/**
 * Minimal hydration indicator for when space is limited
 * Shows just the water drop icon with progress ring
 */
@Composable
fun HydrationIndicator(
    hydrationLog: HydrationRepository.HydrationLog?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = hydrationLog?.progressPercent ?: 0f
    val isGoalReached = hydrationLog?.isGoalReached ?: false

    val progressColor by animateColorAsState(
        targetValue = if (isGoalReached) SuccessGreen else WaterBlue,
        animationSpec = tween(300),
        label = "color"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = SurfaceVariant,
            strokeWidth = 4.dp,
        )

        // Progress circle
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            strokeWidth = 4.dp,
        )

        // Center icon
        Icon(
            imageVector = if (isGoalReached) Icons.Filled.Check else Icons.Filled.WaterDrop,
            contentDescription = "Hydration",
            tint = progressColor,
            modifier = Modifier.size(20.dp)
        )
    }
}