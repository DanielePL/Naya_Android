// app/src/main/java/com/example/myapplicationtest/screens/nutrition/MealAdjustmentSheet.kt

package com.example.menotracker.screens.nutrition

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.AIAnalyzedItem
import com.example.menotracker.data.models.AIPhotoAnalysisResponse
import com.example.menotracker.data.models.MealType
import java.util.UUID

// ═══════════════════════════════════════════════════════════════
// ADJUSTMENT DATA MODELS
// ═══════════════════════════════════════════════════════════════

/**
 * Wrapper for an AI analyzed item that can be adjusted
 */
data class AdjustableItem(
    val id: String = UUID.randomUUID().toString(),
    val originalItem: AIAnalyzedItem,
    val adjustedQuantity: Float,
    val isRemoved: Boolean = false,
    val isManuallyAdded: Boolean = false
) {
    // Calculate adjusted macros based on quantity change
    private val quantityMultiplier: Float
        get() = if (originalItem.quantity_value > 0) {
            adjustedQuantity / originalItem.quantity_value
        } else 1f

    val adjustedCalories: Float get() = originalItem.calories * quantityMultiplier
    val adjustedProtein: Float get() = originalItem.protein * quantityMultiplier
    val adjustedCarbs: Float get() = originalItem.carbs * quantityMultiplier
    val adjustedFat: Float get() = originalItem.fat * quantityMultiplier
    val adjustedFiber: Float get() = originalItem.fiber * quantityMultiplier
    val adjustedSugar: Float get() = originalItem.sugar * quantityMultiplier
    val adjustedSodium: Float get() = originalItem.sodium * quantityMultiplier
}

/**
 * Global cooking/preparation modifiers
 */
enum class CookingOilLevel(val displayName: String, val caloriesAdd: Float, val fatAdd: Float) {
    NONE("None", 0f, 0f),
    LIGHT("Light (1 tsp)", 40f, 4.5f),
    MEDIUM("Medium (1 tbsp)", 120f, 14f),
    HEAVY("Heavy (2 tbsp)", 240f, 28f)
}

enum class SauceLevel(val displayName: String, val caloriesAdd: Float, val carbsAdd: Float, val fatAdd: Float) {
    NONE("None", 0f, 0f, 0f),
    LIGHT("Light", 30f, 5f, 1f),
    MEDIUM("Medium", 60f, 10f, 2f),
    HEAVY("Heavy", 120f, 15f, 5f)
}

enum class PortionSize(val displayName: String, val multiplier: Float, val icon: ImageVector) {
    SMALL("S", 0.7f, Icons.Outlined.RemoveCircleOutline),
    MEDIUM("M", 1.0f, Icons.Outlined.Circle),
    LARGE("L", 1.3f, Icons.Outlined.AddCircleOutline),
    EXTRA_LARGE("XL", 1.6f, Icons.Filled.AddCircle)
}

/**
 * Complete adjustment state
 */
data class MealAdjustmentState(
    val items: List<AdjustableItem>,
    val oilLevel: CookingOilLevel = CookingOilLevel.NONE,
    val sauceLevel: SauceLevel = SauceLevel.NONE,
    val portionSize: PortionSize = PortionSize.MEDIUM,
    val originalAnalysis: AIPhotoAnalysisResponse
) {
    // Get only active (non-removed) items
    val activeItems: List<AdjustableItem>
        get() = items.filter { !it.isRemoved }

    // Calculate totals from items (before global modifiers)
    private val itemsCalories: Float get() = activeItems.sumOf { it.adjustedCalories.toDouble() }.toFloat()
    private val itemsProtein: Float get() = activeItems.sumOf { it.adjustedProtein.toDouble() }.toFloat()
    private val itemsCarbs: Float get() = activeItems.sumOf { it.adjustedCarbs.toDouble() }.toFloat()
    private val itemsFat: Float get() = activeItems.sumOf { it.adjustedFat.toDouble() }.toFloat()

    // Apply portion multiplier then add modifiers
    val totalCalories: Float
        get() = (itemsCalories * portionSize.multiplier) + oilLevel.caloriesAdd + sauceLevel.caloriesAdd

    val totalProtein: Float
        get() = itemsProtein * portionSize.multiplier

    val totalCarbs: Float
        get() = (itemsCarbs * portionSize.multiplier) + sauceLevel.carbsAdd

    val totalFat: Float
        get() = (itemsFat * portionSize.multiplier) + oilLevel.fatAdd + sauceLevel.fatAdd

    // Calculate difference from original
    val caloriesDiff: Float get() = totalCalories - originalAnalysis.total.calories
    val proteinDiff: Float get() = totalProtein - originalAnalysis.total.protein
    val carbsDiff: Float get() = totalCarbs - originalAnalysis.total.carbs
    val fatDiff: Float get() = totalFat - originalAnalysis.total.fat
}

// ═══════════════════════════════════════════════════════════════
// MAIN COMPONENT
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealAdjustmentSheet(
    analysis: AIPhotoAnalysisResponse,
    mealType: MealType,
    onDismiss: () -> Unit,
    onSave: (adjustedItems: List<AdjustableItem>, state: MealAdjustmentState) -> Unit,
    modifier: Modifier = Modifier
) {
    // Initialize state from AI analysis
    var adjustmentState by remember {
        mutableStateOf(
            MealAdjustmentState(
                items = analysis.items.map { item ->
                    AdjustableItem(
                        originalItem = item,
                        adjustedQuantity = item.quantity_value
                    )
                },
                originalAnalysis = analysis
            )
        )
    }

    // Manual item add state
    var showAddItemDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Manual item add dialog
    if (showAddItemDialog) {
        ManualItemAddDialog(
            onDismiss = { showAddItemDialog = false },
            onAdd = { newItem ->
                adjustmentState = adjustmentState.copy(
                    items = adjustmentState.items + AdjustableItem(
                        originalItem = newItem,
                        adjustedQuantity = newItem.quantity_value,
                        isManuallyAdded = true
                    )
                )
                showAddItemDialog = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFF444444),
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(Modifier.size(width = 40.dp, height = 4.dp))
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // ═══════════════════════════════════════════════════════
            // HEADER
            // ═══════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Adjust Meal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = analysis.meal_name,
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )
                }

                // AI Confidence Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getConfidenceColor(analysis.ai_confidence).copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = getConfidenceColor(analysis.ai_confidence)
                        )
                        Text(
                            text = "${(analysis.ai_confidence * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = getConfidenceColor(analysis.ai_confidence)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════
            // SCROLLABLE CONTENT
            // ═══════════════════════════════════════════════════════
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─────────────────────────────────────────────────────
                // PORTION SIZE SELECTOR
                // ─────────────────────────────────────────────────────
                item {
                    AdjustmentSection(title = "Portion Size", icon = Icons.Outlined.Scale) {
                        PortionSizeSelector(
                            selected = adjustmentState.portionSize,
                            onSelect = { size ->
                                adjustmentState = adjustmentState.copy(portionSize = size)
                            }
                        )
                    }
                }

                // ─────────────────────────────────────────────────────
                // COOKING MODIFIERS
                // ─────────────────────────────────────────────────────
                item {
                    AdjustmentSection(title = "Cooking Modifiers", icon = Icons.Outlined.LocalFireDepartment) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Oil Level
                            ModifierRow(
                                label = "Cooking Oil",
                                icon = Icons.Outlined.WaterDrop,
                                options = CookingOilLevel.entries.map { it.displayName },
                                selectedIndex = adjustmentState.oilLevel.ordinal,
                                onSelect = { index ->
                                    adjustmentState = adjustmentState.copy(
                                        oilLevel = CookingOilLevel.entries[index]
                                    )
                                },
                                caloriesImpact = adjustmentState.oilLevel.caloriesAdd
                            )

                            // Sauce Level
                            ModifierRow(
                                label = "Sauce/Dressing",
                                icon = Icons.Outlined.Opacity,
                                options = SauceLevel.entries.map { it.displayName },
                                selectedIndex = adjustmentState.sauceLevel.ordinal,
                                onSelect = { index ->
                                    adjustmentState = adjustmentState.copy(
                                        sauceLevel = SauceLevel.entries[index]
                                    )
                                },
                                caloriesImpact = adjustmentState.sauceLevel.caloriesAdd
                            )
                        }
                    }
                }

                // ─────────────────────────────────────────────────────
                // INDIVIDUAL ITEMS
                // ─────────────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restaurant,
                                contentDescription = null,
                                tint = Color(0xFFFF6B00),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Items (${adjustmentState.activeItems.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // Add Item Button
                        TextButton(
                            onClick = { showAddItemDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFFF6B00)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add item",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 14.sp)
                        }
                    }
                }

                // Item Cards
                items(
                    items = adjustmentState.items,
                    key = { it.id }
                ) { item ->
                    AnimatedVisibility(
                        visible = !item.isRemoved,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        AdjustableItemCard(
                            item = item,
                            onQuantityChange = { newQuantity ->
                                adjustmentState = adjustmentState.copy(
                                    items = adjustmentState.items.map {
                                        if (it.id == item.id) it.copy(adjustedQuantity = newQuantity)
                                        else it
                                    }
                                )
                            },
                            onRemove = {
                                adjustmentState = adjustmentState.copy(
                                    items = adjustmentState.items.map {
                                        if (it.id == item.id) it.copy(isRemoved = true)
                                        else it
                                    }
                                )
                            },
                            onRestore = {
                                adjustmentState = adjustmentState.copy(
                                    items = adjustmentState.items.map {
                                        if (it.id == item.id) it.copy(isRemoved = false)
                                        else it
                                    }
                                )
                            }
                        )
                    }
                }

                // Spacer for bottom padding
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // ═══════════════════════════════════════════════════════
            // ADJUSTED TOTALS (Sticky Footer)
            // ═══════════════════════════════════════════════════════
            Spacer(modifier = Modifier.height(12.dp))

            AdjustedTotalsCard(state = adjustmentState)

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════
            // ACTION BUTTONS
            // ═══════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF888888)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF444444))
                    )
                ) {
                    Text("Cancel")
                }

                // Save Button
                Button(
                    onClick = {
                        onSave(adjustmentState.activeItems, adjustmentState)
                    },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B00)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Meal",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SUB-COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AdjustmentSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF242424)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFF6B00),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun PortionSizeSelector(
    selected: PortionSize,
    onSelect: (PortionSize) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PortionSize.entries.forEach { size ->
            val isSelected = size == selected

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(size) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color(0xFFFF6B00) else Color(0xFF333333)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = size.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${(size.multiplier * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModifierRow(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    caloriesImpact: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color(0xFFCCCCCC)
                )
            }

            // Calories impact badge
            AnimatedVisibility(visible = caloriesImpact > 0) {
                Text(
                    text = "+${caloriesImpact.toInt()} kcal",
                    fontSize = 12.sp,
                    color = Color(0xFFFF6B00),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelect(index) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Color(0xFFFF6B00).copy(alpha = 0.2f) else Color(0xFF1A1A1A),
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B00))
                    } else null
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(vertical = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFFF6B00) else Color(0xFF888888),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustableItemCard(
    item: AdjustableItem,
    onQuantityChange: (Float) -> Unit,
    onRemove: () -> Unit,
    onRestore: () -> Unit
) {
    val confidenceColor = getConfidenceColor(item.originalItem.confidence)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF242424)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Confidence indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(confidenceColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Item info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.originalItem.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.adjustedCalories.toInt()} kcal • ${item.adjustedProtein.toInt()}g P",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
            }

            // Quantity adjuster
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Minus button
                IconButton(
                    onClick = {
                        val step = getStepSize(item.originalItem.quantity_unit)
                        val newQty = (item.adjustedQuantity - step).coerceAtLeast(step)
                        onQuantityChange(newQty)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quantity display
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF333333)
                ) {
                    Text(
                        text = formatQuantity(item.adjustedQuantity, item.originalItem.quantity_unit),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Plus button
                IconButton(
                    onClick = {
                        val step = getStepSize(item.originalItem.quantity_unit)
                        onQuantityChange(item.adjustedQuantity + step)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase",
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustedTotalsCard(state: MealAdjustmentState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E3A1E) // Dark green tint
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adjusted Total",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF88CC88)
                )

                // Show diff from original
                val totalDiff = state.caloriesDiff
                if (totalDiff != 0f) {
                    Text(
                        text = if (totalDiff > 0) "+${totalDiff.toInt()}" else "${totalDiff.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (totalDiff > 0) Color(0xFFFFAA00) else Color(0xFF88CC88)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Macro row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroTotalItem(
                    label = "kcal",
                    value = state.totalCalories,
                    diff = state.caloriesDiff,
                    color = Color(0xFFFF6B00)
                )
                MacroTotalItem(
                    label = "Protein",
                    value = state.totalProtein,
                    diff = state.proteinDiff,
                    suffix = "g",
                    color = Color(0xFF4CAF50)
                )
                MacroTotalItem(
                    label = "Carbs",
                    value = state.totalCarbs,
                    diff = state.carbsDiff,
                    suffix = "g",
                    color = Color(0xFF2196F3)
                )
                MacroTotalItem(
                    label = "Fat",
                    value = state.totalFat,
                    diff = state.fatDiff,
                    suffix = "g",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun MacroTotalItem(
    label: String,
    value: Float,
    diff: Float,
    suffix: String = "",
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${value.toInt()}$suffix",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF888888)
        )
        // Small diff indicator
        if (diff != 0f) {
            Text(
                text = if (diff > 0) "+${diff.toInt()}" else "${diff.toInt()}",
                fontSize = 10.sp,
                color = if (diff > 0) Color(0xFFFFAA00).copy(alpha = 0.7f) else Color(0xFF88CC88).copy(alpha = 0.7f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun getConfidenceColor(confidence: Float): Color {
    return when {
        confidence >= 0.85f -> Color(0xFF4CAF50) // Green - high confidence
        confidence >= 0.65f -> Color(0xFFFFAA00) // Yellow - medium
        else -> Color(0xFFFF5252) // Red - low confidence
    }
}

private fun getStepSize(unit: String): Float {
    return when (unit.lowercase()) {
        "g" -> 10f
        "ml" -> 10f
        "oz" -> 0.5f
        "cup" -> 0.25f
        "piece", "pcs" -> 1f
        "tbsp" -> 0.5f
        "tsp" -> 0.5f
        else -> 10f
    }
}

private fun formatQuantity(value: Float, unit: String): String {
    return when (unit.lowercase()) {
        "g", "ml" -> "${value.toInt()}$unit"
        "oz", "cup", "tbsp", "tsp" -> "${"%.1f".format(value)}$unit"
        "piece", "pcs" -> "${value.toInt()} pcs"
        else -> "${value.toInt()}$unit"
    }
}

// ═══════════════════════════════════════════════════════════════
// MANUAL ITEM ADD DIALOG
// ═══════════════════════════════════════════════════════════════

/**
 * Dialog for manually adding an item to the meal
 * Used when AI missed an item or user wants to add something
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualItemAddDialog(
    onDismiss: () -> Unit,
    onAdd: (AIAnalyzedItem) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val isValid = itemName.isNotBlank() &&
            quantity.toFloatOrNull() != null &&
            calories.toFloatOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color(0xFFFF6B00)
                )
                Text("Add Item", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item Name
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g., Side salad") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        focusedLabelColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Color(0xFFFF6B00),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Quantity Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Qty") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color(0xFFFF6B00),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Unit dropdown
                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF6B00),
                                unfocusedBorderColor = Color(0xFF444444),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false },
                            containerColor = Color(0xFF2A2A2A)
                        ) {
                            listOf("g", "ml", "oz", "cup", "piece", "tbsp").forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u, color = Color.White) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Macros Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Calories
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it.filter { c -> c.isDigit() } },
                        label = { Text("kcal *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color(0xFFFF6B00),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    // Protein
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("P") },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color(0xFF4CAF50),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    // Carbs
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("C") },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color(0xFF2196F3),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    // Fat
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("F") },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color(0xFFFF9800),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                // Helper text
                Text(
                    text = "* Required fields. Macros in grams.",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantityValue = quantity.toFloatOrNull() ?: 100f
                    val newItem = AIAnalyzedItem(
                        name = itemName.trim(),
                        quantity = "$quantityValue $unit", // Display string like "100 g"
                        quantity_value = quantityValue,
                        quantity_unit = unit,
                        calories = calories.toFloatOrNull() ?: 0f,
                        protein = protein.toFloatOrNull() ?: 0f,
                        carbs = carbs.toFloatOrNull() ?: 0f,
                        fat = fat.toFloatOrNull() ?: 0f,
                        confidence = 1.0f // Manual entry = full confidence
                    )
                    onAdd(newItem)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B00),
                    disabledContainerColor = Color(0xFF444444)
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF888888))
            }
        }
    )
}