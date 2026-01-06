// app/src/main/java/com/example/myapplicationtest/screens/nutrition/ExtendedNutrientsCard.kt

package com.example.menotracker.screens.nutrition

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.*

// ═══════════════════════════════════════════════════════════════
// PROMETHEUS COLORS
// ═══════════════════════════════════════════════════════════════

private val NayaOrange = Color(0xFFE67E22)
private val CardBackground = Color(0xFF1E1E1E)
private val CardBackgroundLight = Color(0xFF2A2A2A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFAAAAAA)
private val SuccessGreen = Color(0xFF27AE60)
private val WarningYellow = Color(0xFFF39C12)
private val DangerRed = Color(0xFFE74C3C)
private val ProteinBlue = Color(0xFF3498DB)
private val CarbsPurple = Color(0xFF9B59B6)
private val FatYellow = Color(0xFFF39C12)
private val MineralTeal = Color(0xFF1ABC9C)
private val VitaminGreen = Color(0xFF2ECC71)

// ═══════════════════════════════════════════════════════════════
// MAIN EXTENDED NUTRIENTS CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun ExtendedNutrientsCard(
    nutritionLog: NutritionLog?,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<NutrientCategory?>(null) }

    val extendedNutrients = nutritionLog?.getExtendedNutrients() ?: ExtendedNutrientsSummary()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with expand toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        tint = NayaOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DETAILED NUTRIENTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextGray,
                        letterSpacing = 1.sp
                    )
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextGray
                )
            }

            // Quick summary (always visible)
            Spacer(modifier = Modifier.height(12.dp))
            QuickNutrientSummary(extendedNutrients)

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = TextGray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Category tabs
                    CategoryTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category content
                    when (selectedCategory) {
                        NutrientCategory.CARB_DETAILS -> CarbDetailsSection(extendedNutrients)
                        NutrientCategory.FAT_DETAILS -> FatDetailsSection(extendedNutrients)
                        NutrientCategory.MINERALS -> MineralsSection(extendedNutrients)
                        NutrientCategory.VITAMINS -> VitaminsSection(extendedNutrients)
                        null -> AllCategoriesOverview(extendedNutrients)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// QUICK SUMMARY (Always visible - Key nutrients)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun QuickNutrientSummary(nutrients: ExtendedNutrientsSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickNutrientChip(
            label = "Fiber",
            value = nutrients.fiber,
            target = DailyNutrientTargets.FIBER,
            unit = "g",
            isMaxLimit = false,
            color = CarbsPurple
        )
        QuickNutrientChip(
            label = "Sugar",
            value = nutrients.sugar,
            target = DailyNutrientTargets.SUGAR_MAX,
            unit = "g",
            isMaxLimit = true,
            color = WarningYellow
        )
        QuickNutrientChip(
            label = "Sodium",
            value = nutrients.sodium,
            target = DailyNutrientTargets.SODIUM_MAX,
            unit = "mg",
            isMaxLimit = true,
            color = MineralTeal
        )
        QuickNutrientChip(
            label = "Sat Fat",
            value = nutrients.saturatedFat,
            target = DailyNutrientTargets.SATURATED_FAT_MAX,
            unit = "g",
            isMaxLimit = true,
            color = FatYellow
        )
    }
}

@Composable
private fun QuickNutrientChip(
    label: String,
    value: Float,
    target: Float,
    unit: String,
    isMaxLimit: Boolean,
    color: Color
) {
    val progress = if (target > 0) value / target else 0f
    val status = DailyNutrientTargets.getStatus(value, target, isMaxLimit)
    val statusColor = when (status) {
        NutrientStatus.GOOD -> SuccessGreen
        NutrientStatus.WARNING -> WarningYellow
        NutrientStatus.OVER_LIMIT -> DangerRed
        NutrientStatus.LOW -> TextGray
        NutrientStatus.NORMAL -> color
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (value >= 1000) "${(value/1000).toInt()}k" else "${value.toInt()}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .width(50.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = statusColor,
            trackColor = CardBackgroundLight
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// CATEGORY TABS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CategoryTabs(
    selectedCategory: NutrientCategory?,
    onCategorySelected: (NutrientCategory?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryTab(
            label = "All",
            isSelected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            color = NayaOrange
        )
        CategoryTab(
            label = "Carbs",
            isSelected = selectedCategory == NutrientCategory.CARB_DETAILS,
            onClick = { onCategorySelected(NutrientCategory.CARB_DETAILS) },
            color = CarbsPurple
        )
        CategoryTab(
            label = "Fats",
            isSelected = selectedCategory == NutrientCategory.FAT_DETAILS,
            onClick = { onCategorySelected(NutrientCategory.FAT_DETAILS) },
            color = FatYellow
        )
        CategoryTab(
            label = "Minerals",
            isSelected = selectedCategory == NutrientCategory.MINERALS,
            onClick = { onCategorySelected(NutrientCategory.MINERALS) },
            color = MineralTeal
        )
        CategoryTab(
            label = "Vitamins",
            isSelected = selectedCategory == NutrientCategory.VITAMINS,
            onClick = { onCategorySelected(NutrientCategory.VITAMINS) },
            color = VitaminGreen
        )
    }
}

@Composable
private fun RowScope.CategoryTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else TextGray,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            maxLines = 1
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// CATEGORY SECTIONS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AllCategoriesOverview(nutrients: ExtendedNutrientsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Fat Quality Score
        FatQualityIndicator(nutrients)

        // Key highlights from each category
        Text(
            text = "KEY HIGHLIGHTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextGray,
            letterSpacing = 0.5.sp
        )

        // Show most important nutrients
        NutrientRow("Fiber", nutrients.fiber, DailyNutrientTargets.FIBER, "g", false, CarbsPurple)
        NutrientRow("Omega-3", nutrients.omega3, DailyNutrientTargets.OMEGA3, "mg", false, FatYellow)
        NutrientRow("Vitamin D", nutrients.vitaminD, DailyNutrientTargets.VITAMIN_D, "mcg", false, VitaminGreen)
        NutrientRow("Iron", nutrients.iron, DailyNutrientTargets.IRON, "mg", false, MineralTeal)
        NutrientRow("Magnesium", nutrients.magnesium, DailyNutrientTargets.MAGNESIUM, "mg", false, MineralTeal)
    }
}

@Composable
private fun CarbDetailsSection(nutrients: ExtendedNutrientsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "CARBOHYDRATE BREAKDOWN",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CarbsPurple,
            letterSpacing = 0.5.sp
        )

        NutrientRow("Fiber", nutrients.fiber, DailyNutrientTargets.FIBER, "g", false, CarbsPurple)
        NutrientRow("Sugar", nutrients.sugar, DailyNutrientTargets.SUGAR_MAX, "g", true, WarningYellow)
        NutrientRow("Net Carbs", nutrients.netCarbs, 250f, "g", false, CarbsPurple) // Approximate target

        // Info card
        InfoCard(
            text = "Net Carbs = Total Carbs - Fiber. Fiber doesn't spike blood sugar.",
            color = CarbsPurple
        )
    }
}

@Composable
private fun FatDetailsSection(nutrients: ExtendedNutrientsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "FAT BREAKDOWN",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = FatYellow,
            letterSpacing = 0.5.sp
        )

        FatQualityIndicator(nutrients)

        Spacer(modifier = Modifier.height(4.dp))

        NutrientRow("Saturated Fat", nutrients.saturatedFat, DailyNutrientTargets.SATURATED_FAT_MAX, "g", true, DangerRed)
        NutrientRow("Unsaturated Fat", nutrients.unsaturatedFat, 50f, "g", false, SuccessGreen) // Approximate
        NutrientRow("Trans Fat", nutrients.transFat, DailyNutrientTargets.TRANS_FAT_MAX, "g", true, DangerRed)
        NutrientRow("Cholesterol", nutrients.cholesterol, DailyNutrientTargets.CHOLESTEROL_MAX, "mg", true, WarningYellow)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ESSENTIAL FATTY ACIDS",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextGray,
            letterSpacing = 0.5.sp
        )

        NutrientRow("Omega-3", nutrients.omega3, DailyNutrientTargets.OMEGA3, "mg", false, SuccessGreen)
        NutrientRow("Omega-6", nutrients.omega6, DailyNutrientTargets.OMEGA6, "mg", false, FatYellow)

        // Omega ratio info
        if (nutrients.omega3 > 0) {
            val ratio = nutrients.omega6to3Ratio
            InfoCard(
                text = "Omega-6:3 Ratio: ${String.format("%.1f", ratio)}:1 ${if (ratio <= 4) "(Good)" else "(Consider more Omega-3)"}",
                color = if (ratio <= 4) SuccessGreen else WarningYellow
            )
        }
    }
}

@Composable
private fun MineralsSection(nutrients: ExtendedNutrientsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "MINERALS",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MineralTeal,
            letterSpacing = 0.5.sp
        )

        NutrientRow("Sodium", nutrients.sodium, DailyNutrientTargets.SODIUM_MAX, "mg", true, WarningYellow)
        NutrientRow("Potassium", nutrients.potassium, DailyNutrientTargets.POTASSIUM, "mg", false, MineralTeal)
        NutrientRow("Calcium", nutrients.calcium, DailyNutrientTargets.CALCIUM, "mg", false, MineralTeal)
        NutrientRow("Iron", nutrients.iron, DailyNutrientTargets.IRON, "mg", false, MineralTeal)
        NutrientRow("Magnesium", nutrients.magnesium, DailyNutrientTargets.MAGNESIUM, "mg", false, MineralTeal)
        NutrientRow("Zinc", nutrients.zinc, DailyNutrientTargets.ZINC, "mg", false, MineralTeal)
        NutrientRow("Phosphorus", nutrients.phosphorus, DailyNutrientTargets.PHOSPHORUS, "mg", false, MineralTeal)

        // Sodium:Potassium ratio info
        if (nutrients.potassium > 0 && nutrients.sodium > 0) {
            val ratio = nutrients.sodium / nutrients.potassium
            InfoCard(
                text = "Na:K Ratio: ${String.format("%.2f", ratio)} ${if (ratio < 1) "(Good)" else "(Consider more potassium)"}",
                color = if (ratio < 1) SuccessGreen else WarningYellow
            )
        }
    }
}

@Composable
private fun VitaminsSection(nutrients: ExtendedNutrientsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "FAT-SOLUBLE VITAMINS",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = VitaminGreen,
            letterSpacing = 0.5.sp
        )

        NutrientRow("Vitamin A", nutrients.vitaminA, DailyNutrientTargets.VITAMIN_A, "mcg", false, VitaminGreen)
        NutrientRow("Vitamin D", nutrients.vitaminD, DailyNutrientTargets.VITAMIN_D, "mcg", false, VitaminGreen)
        NutrientRow("Vitamin E", nutrients.vitaminE, DailyNutrientTargets.VITAMIN_E, "mg", false, VitaminGreen)
        NutrientRow("Vitamin K", nutrients.vitaminK, DailyNutrientTargets.VITAMIN_K, "mcg", false, VitaminGreen)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "WATER-SOLUBLE VITAMINS",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = VitaminGreen,
            letterSpacing = 0.5.sp
        )

        NutrientRow("Vitamin C", nutrients.vitaminC, DailyNutrientTargets.VITAMIN_C, "mg", false, VitaminGreen)
        NutrientRow("Thiamin (B1)", nutrients.vitaminB1, DailyNutrientTargets.VITAMIN_B1, "mg", false, VitaminGreen)
        NutrientRow("Riboflavin (B2)", nutrients.vitaminB2, DailyNutrientTargets.VITAMIN_B2, "mg", false, VitaminGreen)
        NutrientRow("Niacin (B3)", nutrients.vitaminB3, DailyNutrientTargets.VITAMIN_B3, "mg", false, VitaminGreen)
        NutrientRow("Vitamin B6", nutrients.vitaminB6, DailyNutrientTargets.VITAMIN_B6, "mg", false, VitaminGreen)
        NutrientRow("Vitamin B12", nutrients.vitaminB12, DailyNutrientTargets.VITAMIN_B12, "mcg", false, VitaminGreen)
        NutrientRow("Folate", nutrients.folate, DailyNutrientTargets.FOLATE, "mcg", false, VitaminGreen)
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun NutrientRow(
    name: String,
    value: Float,
    target: Float,
    unit: String,
    isMaxLimit: Boolean,
    color: Color
) {
    val progress = if (target > 0) value / target else 0f
    val status = DailyNutrientTargets.getStatus(value, target, isMaxLimit)
    val progressColor = when (status) {
        NutrientStatus.GOOD -> SuccessGreen
        NutrientStatus.WARNING -> WarningYellow
        NutrientStatus.OVER_LIMIT -> DangerRed
        NutrientStatus.LOW -> TextGray.copy(alpha = 0.7f)
        NutrientStatus.NORMAL -> color
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name
        Text(
            text = name,
            fontSize = 13.sp,
            color = TextWhite,
            modifier = Modifier.weight(1f)
        )

        // Value / Target
        Text(
            text = "${formatValue(value)}/${formatValue(target)}$unit",
            fontSize = 12.sp,
            color = TextGray,
            modifier = Modifier.width(90.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .width(60.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = CardBackgroundLight
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Percentage
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = progressColor,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun FatQualityIndicator(nutrients: ExtendedNutrientsSummary) {
    val quality = nutrients.fatQualityPercent

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackgroundLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Fat Quality Score",
                    fontSize = 12.sp,
                    color = TextGray
                )
                Text(
                    text = "${quality.toInt()}% unsaturated",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        quality >= 60 -> SuccessGreen
                        quality >= 40 -> WarningYellow
                        else -> DangerRed
                    }
                )
            }

            Icon(
                when {
                    quality >= 60 -> Icons.Default.ThumbUp
                    quality >= 40 -> Icons.Default.ThumbsUpDown
                    else -> Icons.Default.Warning
                },
                contentDescription = null,
                tint = when {
                    quality >= 60 -> SuccessGreen
                    quality >= 40 -> WarningYellow
                    else -> DangerRed
                },
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun InfoCard(text: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                color = color,
                lineHeight = 14.sp
            )
        }
    }
}

private fun formatValue(value: Float): String {
    return when {
        value >= 1000 -> "${(value / 1000).toInt()}k"
        value >= 100 -> "${value.toInt()}"
        value >= 10 -> String.format("%.0f", value)
        value >= 1 -> String.format("%.1f", value)
        else -> String.format("%.2f", value)
    }
}