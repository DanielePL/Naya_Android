// app/src/main/java/com/example/myapplicationtest/screens/nutrition/MealEditSheet.kt

package com.example.menotracker.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.Meal
import com.example.menotracker.data.models.MealItem

// ═══════════════════════════════════════════════════════════════
// PROMETHEUS - MEAL EDIT SHEET
// Bottom Sheet for editing meals and their items
// ═══════════════════════════════════════════════════════════════

// Color constants
private val NayaOrange = Color(0xFFE67E22)
private val DarkBackground = Color(0xFF1A1A1A)
private val CardBackground = Color(0xFF2A2A2A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFAAAAAA)
private val DeleteRed = Color(0xFFE74C3C)

// ═══════════════════════════════════════════════════════════════
// EDITABLE DATA CLASSES (mutable copies for editing)
// ═══════════════════════════════════════════════════════════════

data class EditableMealItem(
    val originalId: String,
    var name: String,
    var quantity: Float,
    var unit: String,
    var calories: Float,
    var protein: Float,
    var carbs: Float,
    var fat: Float,
    var isDeleted: Boolean = false
)

fun MealItem.toEditable() = EditableMealItem(
    originalId = id,
    name = itemName,
    quantity = quantity,
    unit = quantityUnit,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat
)

fun EditableMealItem.toMealItem(mealId: String) = MealItem(
    id = originalId,
    mealId = mealId,
    itemName = name,
    quantity = quantity,
    quantityUnit = unit,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat
)

// ═══════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEditSheet(
    meal: Meal,
    onDismiss: () -> Unit,
    onSave: (updatedMeal: Meal, updatedItems: List<MealItem>, deletedItemIds: List<String>) -> Unit,
    onDeleteMeal: (mealId: String) -> Unit
) {
    // Editable state
    var mealName by remember { mutableStateOf(meal.mealName ?: "") }
    var editableItems by remember {
        mutableStateOf(meal.items.map { it.toEditable() })
    }
    var showDeleteMealDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Calculate totals from non-deleted items
    val activeitems = editableItems.filter { !it.isDeleted }
    val totalCalories = activeitems.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = activeitems.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs = activeitems.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat = activeitems.sumOf { it.fat.toDouble() }.toFloat()

    // Check if there are changes
    val hasChanges = remember(mealName, editableItems) {
        mealName != (meal.mealName ?: "") ||
                editableItems.any { it.isDeleted } ||
                editableItems.zip(meal.items).any { (editable, original) ->
                    editable.name != original.itemName ||
                            editable.quantity != original.quantity ||
                            editable.calories != original.calories ||
                            editable.protein != original.protein ||
                            editable.carbs != original.carbs ||
                            editable.fat != original.fat
                }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextGray)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ═══════════════════════════════════════════════════════════
            // HEADER
            // ═══════════════════════════════════════════════════════════

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Meal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                // Delete meal button
                IconButton(
                    onClick = { showDeleteMealDialog = true }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Meal",
                        tint = DeleteRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════
            // MEAL NAME INPUT
            // ═══════════════════════════════════════════════════════════

            OutlinedTextField(
                value = mealName,
                onValueChange = { mealName = it },
                label = { Text("Meal Name") },
                placeholder = { Text("e.g., Chicken with Rice") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NayaOrange,
                    unfocusedBorderColor = TextGray,
                    focusedLabelColor = NayaOrange,
                    unfocusedLabelColor = TextGray,
                    cursorColor = NayaOrange,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ═══════════════════════════════════════════════════════════
            // TOTALS SUMMARY
            // ═══════════════════════════════════════════════════════════

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MacroTotalItem("Calories", totalCalories, "kcal", NayaOrange)
                    MacroTotalItem("Protein", totalProtein, "g", Color(0xFF3498DB))
                    MacroTotalItem("Carbs", totalCarbs, "g", Color(0xFF9B59B6))
                    MacroTotalItem("Fat", totalFat, "g", Color(0xFFF39C12))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════
            // ITEMS HEADER
            // ═══════════════════════════════════════════════════════════

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Items (${activeitems.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )

                TextButton(
                    onClick = { showAddItemDialog = true }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = NayaOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Item",
                        color = NayaOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════════════════════
            // ITEMS LIST
            // ═══════════════════════════════════════════════════════════

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = editableItems,
                    key = { _, item -> item.originalId }
                ) { index, item ->
                    if (!item.isDeleted) {
                        EditableItemCard(
                            item = item,
                            onUpdate = { updated ->
                                editableItems = editableItems.toMutableList().apply {
                                    set(index, updated)
                                }
                            },
                            onDelete = {
                                editableItems = editableItems.toMutableList().apply {
                                    set(index, item.copy(isDeleted = true))
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════════════
            // ACTION BUTTONS
            // ═══════════════════════════════════════════════════════════

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextGray
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(TextGray)
                    )
                ) {
                    Text("Cancel")
                }

                // Save Button
                Button(
                    onClick = {
                        val deletedIds = editableItems
                            .filter { it.isDeleted }
                            .map { it.originalId }

                        val updatedItems = editableItems
                            .filter { !it.isDeleted }
                            .map { it.toMealItem(meal.id) }

                        val updatedMeal = meal.copy(
                            mealName = mealName.ifBlank { null }
                        )

                        onSave(updatedMeal, updatedItems, deletedIds)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NayaOrange,
                        disabledContainerColor = NayaOrange.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE MEAL CONFIRMATION DIALOG
    // ═══════════════════════════════════════════════════════════════

    if (showDeleteMealDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMealDialog = false },
            containerColor = CardBackground,
            title = {
                Text(
                    text = "Delete Meal?",
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "This will permanently delete \"${meal.mealName ?: "this meal"}\" and all its items. This action cannot be undone.",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteMealDialog = false
                        onDeleteMeal(meal.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeleteRed
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMealDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // ADD ITEM DIALOG
    // ═══════════════════════════════════════════════════════════════

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAdd = { newItem ->
                editableItems = editableItems + newItem
                showAddItemDialog = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// MACRO TOTAL ITEM
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MacroTotalItem(
    label: String,
    value: Float,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${value.toInt()}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "$label ($unit)",
            fontSize = 11.sp,
            color = TextGray
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// EDITABLE ITEM CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EditableItemCard(
    item: EditableMealItem,
    onUpdate: (EditableMealItem) -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main row: Name, Quantity, Calories, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item name (truncated)
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Quantity
                Text(
                    text = "${item.quantity.toInt()}${item.unit}",
                    fontSize = 13.sp,
                    color = TextGray
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Calories
                Text(
                    text = "${item.calories.toInt()} kcal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NayaOrange
                )

                // Expand button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextGray
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove item",
                        tint = DeleteRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expanded editing section
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = TextGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Name input
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = { Text("Name", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    colors = compactTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity & Unit row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.quantity.toInt().toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.let { qty ->
                                onUpdate(item.copy(quantity = qty))
                            }
                        },
                        label = { Text("Qty", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        colors = compactTextFieldColors()
                    )

                    OutlinedTextField(
                        value = item.unit,
                        onValueChange = { onUpdate(item.copy(unit = it)) },
                        label = { Text("Unit", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        colors = compactTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Macros row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MacroInput(
                        label = "kcal",
                        value = item.calories,
                        onValueChange = { onUpdate(item.copy(calories = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    MacroInput(
                        label = "P",
                        value = item.protein,
                        onValueChange = { onUpdate(item.copy(protein = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    MacroInput(
                        label = "C",
                        value = item.carbs,
                        onValueChange = { onUpdate(item.copy(carbs = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    MacroInput(
                        label = "F",
                        value = item.fat,
                        onValueChange = { onUpdate(item.copy(fat = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MACRO INPUT FIELD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MacroInput(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toInt().toString(),
        onValueChange = {
            it.toFloatOrNull()?.let { v -> onValueChange(v) }
        },
        label = { Text(label, fontSize = 10.sp) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        colors = compactTextFieldColors()
    )
}

// ═══════════════════════════════════════════════════════════════
// ADD ITEM DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (EditableMealItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("100") }
    var unit by remember { mutableStateOf("g") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("0") }
    var carbs by remember { mutableStateOf("0") }
    var fat by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text("Add Item", color = TextWhite)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("e.g., Chicken Breast") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = compactTextFieldColors()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Qty") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = compactTextFieldColors()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = compactTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = compactTextFieldColors()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("P") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = compactTextFieldColors()
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("C") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = compactTextFieldColors()
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("F") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = compactTextFieldColors()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && calories.isNotBlank()) {
                        onAdd(
                            EditableMealItem(
                                originalId = java.util.UUID.randomUUID().toString(),
                                name = name,
                                quantity = quantity.toFloatOrNull() ?: 100f,
                                unit = unit,
                                calories = calories.toFloatOrNull() ?: 0f,
                                protein = protein.toFloatOrNull() ?: 0f,
                                carbs = carbs.toFloatOrNull() ?: 0f,
                                fat = fat.toFloatOrNull() ?: 0f
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && calories.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NayaOrange)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// COMPACT TEXT FIELD COLORS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun compactTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NayaOrange,
    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
    focusedLabelColor = NayaOrange,
    unfocusedLabelColor = TextGray,
    cursorColor = NayaOrange,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite
)
