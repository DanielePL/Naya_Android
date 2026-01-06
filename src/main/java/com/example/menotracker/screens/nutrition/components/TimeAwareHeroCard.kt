package com.example.menotracker.screens.nutrition.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.FrequentMeal
import com.example.menotracker.data.models.MealType
import com.example.menotracker.data.repository.HydrationRepository
import com.example.menotracker.screens.home.getNutritionThemeForHour
import com.example.menotracker.ui.theme.NayaPrimary
import java.time.LocalTime

// ═══════════════════════════════════════════════════════════════
// PROMETHEUS BRANDING COLORS (from BRANDING.md)
// ═══════════════════════════════════════════════════════════════

// Text Colors
private val TextPrimary = Color(0xFFFAFAFA)
private val TextSecondary = Color(0xFF999999)

// Snack stays pink for distinction
private val SnackPink = Color(0xFFE91E63)

// Hydration blue
private val WaterBlue = Color(0xFF2196F3)

// ═══════════════════════════════════════════════════════════════
// MEAL TIME WINDOW
// ═══════════════════════════════════════════════════════════════

enum class MealTimeWindow(
    val mealType: MealType,
    val startHour: Int,
    val endHour: Int,
    val prompt: String
) {
    BREAKFAST(MealType.BREAKFAST, 6, 10, "What's for BREAKFAST?"),
    LUNCH(MealType.LUNCH, 11, 14, "What's for LUNCH?"),
    AFTERNOON_SNACK(MealType.SNACK, 14, 17, "Snack time?"),
    DINNER(MealType.DINNER, 17, 21, "What's for DINNER?"),
    EVENING_SNACK(MealType.SNACK, 21, 24, "Late night snack?"),
    EARLY_MORNING(MealType.SHAKE, 0, 6, "Early protein?");

    companion object {
        fun current(): MealTimeWindow {
            val hour = LocalTime.now().hour
            return entries.find { hour in it.startHour until it.endHour }
                ?: LUNCH
        }
    }
}

/**
 * Get dynamic Naya Orange based on time of day
 * - Morning: Soft Gold/Amber (sunrise)
 * - Midday: Bright Orange (peak sun)
 * - Evening: Sunset Orange-Red (warm)
 * - Night: Deep ember glow
 */
@Composable
private fun getTimeBasedOrange(): Color {
    val hour = LocalTime.now().hour
    val theme = getNutritionThemeForHour(hour)
    // IMPORTANT: Convert Long to Int for Color constructor
    // Color(Long) expects encoded color space, Color(Int) expects ARGB
    return Color(theme.gradientStartHex.toInt())
}

// ═══════════════════════════════════════════════════════════════
// TIME AWARE HERO CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun TimeAwareHeroCard(
    modifier: Modifier = Modifier,
    quickAddSuggestion: FrequentMeal? = null,
    hydrationLog: HydrationRepository.HydrationLog? = null,
    onSnapClick: (MealType) -> Unit,
    onScanClick: () -> Unit,
    onSearchClick: (MealType) -> Unit,
    onQuickAddClick: () -> Unit,
    onSnackClick: () -> Unit,
    onAddWater: (Int) -> Unit = {},
    onQuickAdd: (FrequentMeal) -> Unit
) {
    // Time-based default, but user can override
    val defaultMealWindow = remember { MealTimeWindow.current() }
    var selectedMealType by remember { mutableStateOf(defaultMealWindow.mealType) }
    var showMealDropdown by remember { mutableStateOf(false) }

    // Dynamic Naya Orange based on time of day
    val timeBasedOrange = getTimeBasedOrange()

    // Glassmorphism Card with orange accent
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
                        timeBasedOrange.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.15f),
                        timeBasedOrange.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ═══════════════════════════════════════════
            // MEAL TYPE SELECTOR (Dropdown) + SNACK CHIP
            // ═══════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Meal Type Dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showMealDropdown = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getMealTypeIcon(selectedMealType),
                            contentDescription = null,
                            tint = timeBasedOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getMealPrompt(selectedMealType),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select meal type",
                            tint = timeBasedOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Dropdown Menu
                    DropdownMenu(
                        expanded = showMealDropdown,
                        onDismissRequest = { showMealDropdown = false }
                    ) {
                        // Main meals only (no snack - that has its own button)
                        listOf(
                            MealType.BREAKFAST to "Breakfast",
                            MealType.LUNCH to "Lunch",
                            MealType.DINNER to "Dinner",
                            MealType.SHAKE to "Shake"
                        ).forEach { (mealType, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getMealTypeIcon(mealType),
                                            contentDescription = null,
                                            tint = if (mealType == selectedMealType) timeBasedOrange else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            color = if (mealType == selectedMealType) timeBasedOrange else TextPrimary,
                                            fontWeight = if (mealType == selectedMealType) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    selectedMealType = mealType
                                    showMealDropdown = false
                                }
                            )
                        }
                    }
                }

                // Quick Action Chips Row (Hydration + Snack)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hydration Chip - Quick add 250ml water
                    Surface(
                        modifier = Modifier.clickable { onAddWater(250) },
                        color = WaterBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = WaterBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formatHydrationProgress(hydrationLog),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = WaterBlue
                            )
                        }
                    }

                    // Snack Chip (always visible for quick snack access)
                    Surface(
                        modifier = Modifier.clickable { onSnackClick() },
                        color = SnackPink.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Cookie,
                                contentDescription = null,
                                tint = SnackPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Snack",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SnackPink
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════
            // ACTION BUTTONS (4 nebeneinander)
            // ═══════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // QUICK (opens full QuickAddSheet)
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bolt,
                    label = "Quick",
                    color = timeBasedOrange,
                    onClick = onQuickAddClick
                )

                // SNAP - AI Food Photo
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CameraAlt,
                    label = "Snap",
                    color = timeBasedOrange,
                    onClick = { onSnapClick(selectedMealType) }
                )

                // SCAN - Barcode Scanner
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.QrCodeScanner,
                    label = "Scan",
                    color = timeBasedOrange,
                    onClick = onScanClick
                )

                // SEARCH - uses selected meal type
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Search,
                    label = "Search",
                    color = timeBasedOrange,
                    onClick = { onSearchClick(selectedMealType) }
                )
            }

            // ═══════════════════════════════════════════
            // QUICK ADD SUGGESTION (wenn vorhanden)
            // ═══════════════════════════════════════════
            quickAddSuggestion?.let { meal ->
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickAdd(meal) },
                    color = timeBasedOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = timeBasedOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Quick: ${meal.name}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${meal.totalCalories.toInt()} kcal • ${meal.totalProtein.toInt()}g P",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            tint = timeBasedOrange
                        )
                    }
                }
            }
        }
    }  // Close glassmorphism Box
}

// ═══════════════════════════════════════════════════════════════
// ACTION BUTTON
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun getMealTypeIcon(mealType: MealType): ImageVector {
    return when (mealType) {
        MealType.BREAKFAST -> Icons.Default.WbSunny
        MealType.LUNCH -> Icons.Default.Restaurant
        MealType.DINNER -> Icons.Default.Nightlight
        MealType.SNACK -> Icons.Default.Cookie
        MealType.SHAKE -> Icons.Default.LocalCafe
    }
}

private fun getMealPrompt(mealType: MealType): String {
    return when (mealType) {
        MealType.BREAKFAST -> "Breakfast"
        MealType.LUNCH -> "Lunch"
        MealType.DINNER -> "Dinner"
        MealType.SNACK -> "Snack"
        MealType.SHAKE -> "Shake"
    }
}

/**
 * Format hydration progress for compact display
 * e.g., "0.5/2.5L" or "+250ml" for quick add feedback
 */
private fun formatHydrationProgress(hydrationLog: HydrationRepository.HydrationLog?): String {
    val currentMl = hydrationLog?.waterIntakeMl ?: 0
    val targetMl = hydrationLog?.targetMl ?: 2500

    val currentL = currentMl / 1000f
    val targetL = targetMl / 1000f

    return if (currentL >= 1f || targetL >= 1f) {
        "${String.format("%.1f", currentL)}/${String.format("%.1f", targetL)}L"
    } else {
        "${currentMl}/${targetMl}ml"
    }
}