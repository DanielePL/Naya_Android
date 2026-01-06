package com.example.menotracker.screens.nutrition.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════
// PROMETHEUS BRANDING COLORS (from BRANDING.md)
// ═══════════════════════════════════════════════════════════════

// Nutrition Macro Colors (from BRANDING.md)
private val ProteinBlue = Color(0xFF3B82F6)      // Blue
private val CarbsGreen = Color(0xFF10B981)       // Green
private val FatYellow = Color(0xFFFBBF24)        // Yellow
private val CaloriesOrange = Color(0xFFF97316)   // Orange

// Dark Mode Backgrounds (from BRANDING.md)
private val Surface = Color(0xFF1C1C1C)
private val SurfaceVariant = Color(0xFF262626)

// Text Colors (from BRANDING.md)
private val TextPrimary = Color(0xFFFAFAFA)
private val TextSecondary = Color(0xFF999999)

// Glass
private val GlassBase = Color(0xFF333333)

// ═══════════════════════════════════════════════════════════════
// PROTEIN HERO CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun ProteinHeroCard(
    modifier: Modifier = Modifier,
    proteinCurrent: Float,
    proteinTarget: Float,
    carbsCurrent: Float,
    carbsTarget: Float,
    fatCurrent: Float,
    fatTarget: Float,
    caloriesCurrent: Float,
    caloriesTarget: Float
) {
    val proteinProgress = (proteinCurrent / proteinTarget).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═══════════════════════════════════════════
            // PROTEIN HERO CIRCLE
            // ═══════════════════════════════════════════
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circular progress indicator
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Track (gray background)
                    drawCircle(
                        color = SurfaceVariant,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress (blue)
                    drawArc(
                        color = ProteinBlue,
                        startAngle = -90f,
                        sweepAngle = 360f * proteinProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth)
                    )
                }

                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${proteinCurrent.toInt()}g",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProteinBlue
                    )
                    Text(
                        text = "${proteinTarget.toInt()}g",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "PROTEIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═══════════════════════════════════════════
            // CARBS BAR
            // ═══════════════════════════════════════════
            MacroProgressBar(
                label = "C",
                current = carbsCurrent,
                target = carbsTarget,
                color = CarbsGreen,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════
            // FAT BAR
            // ═══════════════════════════════════════════
            MacroProgressBar(
                label = "F",
                current = fatCurrent,
                target = fatTarget,
                color = FatYellow,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))

            Spacer(modifier = Modifier.height(12.dp))

            // ═══════════════════════════════════════════
            // CALORIES
            // ═══════════════════════════════════════════
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = CaloriesOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${caloriesCurrent.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CaloriesOrange
                )
                Text(
                    text = " / ${caloriesTarget.toInt()} kcal",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MACRO PROGRESS BAR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MacroProgressBar(
    label: String,
    current: Float,
    target: Float,
    color: Color,
    unit: String
) {
    val progress = (current / target).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = SurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Value
        Text(
            text = "${current.toInt()}/${target.toInt()}$unit",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.width(80.dp)
        )
    }
}