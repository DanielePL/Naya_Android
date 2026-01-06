package com.example.menotracker.screens.nutrition.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.Meal
import com.example.menotracker.data.models.MealType
import com.example.menotracker.data.models.NutritionLog

// ═══════════════════════════════════════════════════════════════
// PROMETHEUS BRANDING COLORS (from BRANDING.md)
// ═══════════════════════════════════════════════════════════════

// Dark Mode Backgrounds
private val Surface = Color(0xFF1C1C1C)

// Text Colors
private val TextPrimary = Color(0xFFFAFAFA)
private val TextSecondary = Color(0xFF999999)

// Nutrition Macro Colors
private val ProteinBlue = Color(0xFF3B82F6)
private val CaloriesOrange = Color(0xFFF97316)

// Semantic
private val SuccessGreen = Color(0xFF4CAF50)

// ═══════════════════════════════════════════════════════════════
// COMPACT MEAL LIST
// ═══════════════════════════════════════════════════════════════

@Composable
fun CompactMealList(
    modifier: Modifier = Modifier,
    nutritionLog: NutritionLog?,
    onMealClick: (MealType) -> Unit,
    onMealEdit: (Meal) -> Unit,
    onMealDelete: (Meal) -> Unit
) {
    // Glassmorphism container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MealType.orderedValues().forEach { mealType ->
                val meals = nutritionLog?.getMealsByType(mealType) ?: emptyList()
                val totalCalories = nutritionLog?.getCaloriesForType(mealType) ?: 0f
                val totalProtein = meals.sumOf { it.totalProtein.toDouble() }.toFloat()
                val isLogged = meals.isNotEmpty()

                CompactMealRow(
                    mealType = mealType,
                    isLogged = isLogged,
                    calories = totalCalories,
                    protein = totalProtein,
                    itemCount = meals.sumOf { it.items.size },
                    onClick = { onMealClick(mealType) },
                    onEdit = if (meals.isNotEmpty()) { { onMealEdit(meals.first()) } } else null
                )

                if (mealType != MealType.SNACK) {
                    HorizontalDivider(
                        color = TextSecondary.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// COMPACT MEAL ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CompactMealRow(
    mealType: MealType,
    isLogged: Boolean,
    calories: Float,
    protein: Float,
    itemCount: Int,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Icon(
            imageVector = if (isLogged) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isLogged) SuccessGreen else TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Meal name
        Text(
            text = mealType.displayName,
            fontSize = 15.sp,
            fontWeight = if (isLogged) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isLogged) TextPrimary else TextSecondary,
            modifier = Modifier.weight(1f)
        )

        // Stats (wenn geloggt)
        if (isLogged) {
            Text(
                text = "${protein.toInt()}g P",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ProteinBlue
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${calories.toInt()} kcal",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CaloriesOrange
            )

            // Edit button
            onEdit?.let {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = it,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Text(
                text = "–",
                fontSize = 14.sp,
                color = TextSecondary.copy(alpha = 0.4f)
            )
        }
    }
}