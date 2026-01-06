// QuickAddSheet.kt - Quick Add Frequent Meals Feature
// 10-second meal logging for Naya Nutrition

package com.example.menotracker.screens.nutrition

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.NayaColors

// ═══════════════════════════════════════════════════════════════
// QUICK ADD BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    frequentMeals: List<FrequentMeal>,
    favoriteFrequentMeals: List<FrequentMeal>,
    selectedMeal: FrequentMeal?,
    frequentAddOns: List<FrequentAddOn>,
    selectedAddOns: List<FrequentMealItem>,
    isLoading: Boolean,
    onSelectMeal: (FrequentMeal) -> Unit,
    onDirectAdd: (FrequentMeal) -> Unit,
    onToggleAddOn: (FrequentAddOn) -> Unit,
    onToggleFavorite: (FrequentMeal) -> Unit,
    onConfirmAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Frequent", "Favorites")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NayaColors.BackgroundDark,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = NayaColors.TextSecondary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(Modifier.size(32.dp, 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = NayaColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Add",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NayaColors.TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NayaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = NayaColors.Primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NayaColors.Primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Default.TrendingUp else Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                                if (index == 0 && frequentMeals.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge(
                                        containerColor = NayaColors.Primary.copy(alpha = 0.2f),
                                        contentColor = NayaColors.Primary
                                    ) {
                                        Text("${frequentMeals.size}", fontSize = 10.sp)
                                    }
                                }
                            }
                        },
                        selectedContentColor = NayaColors.Primary,
                        unselectedContentColor = NayaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selection state
            AnimatedContent(
                targetState = selectedMeal,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "meal_selection"
            ) { meal ->
                if (meal != null) {
                    // Add-ons selection view
                    AddOnsSelectionView(
                        meal = meal,
                        addOns = frequentAddOns,
                        selectedAddOns = selectedAddOns,
                        isLoading = isLoading,
                        onToggleAddOn = onToggleAddOn,
                        onConfirm = onConfirmAdd,
                        onBack = { onSelectMeal(meal) } // Deselect
                    )
                } else {
                    // Meals list view - sorted by usage count (most logged first)
                    val displayMeals = when (selectedTab) {
                        0 -> frequentMeals.sortedByDescending { it.usageCount }
                        1 -> favoriteFrequentMeals.sortedByDescending { it.usageCount }
                        else -> emptyList()
                    }

                    if (displayMeals.isEmpty()) {
                        EmptyQuickAddView(
                            isFavorites = selectedTab == 1,
                            onSearchFood = onDismiss // Navigate to search
                        )
                    } else {
                        FrequentMealsList(
                            meals = displayMeals,
                            onSelectMeal = onSelectMeal,
                            onDirectAdd = onDirectAdd,
                            onToggleFavorite = onToggleFavorite,
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FREQUENT MEALS LIST
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FrequentMealsList(
    meals: List<FrequentMeal>,
    onSelectMeal: (FrequentMeal) -> Unit,
    onDirectAdd: (FrequentMeal) -> Unit,
    onToggleFavorite: (FrequentMeal) -> Unit,
    isLoading: Boolean
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 400.dp)
    ) {
        items(meals, key = { it.id }) { meal ->
            FrequentMealCard(
                meal = meal,
                onSelect = { onSelectMeal(meal) },
                onDirectAdd = { onDirectAdd(meal) },
                onToggleFavorite = { onToggleFavorite(meal) },
                isLoading = isLoading
            )
        }
    }
}

@Composable
private fun FrequentMealCard(
    meal: FrequentMeal,
    onSelect: () -> Unit,
    onDirectAdd: () -> Unit,
    onToggleFavorite: () -> Unit,
    isLoading: Boolean
) {
    val mealIcon = when (meal.mealTypeEnum) {
        MealType.BREAKFAST -> Icons.Default.WbSunny
        MealType.LUNCH -> Icons.Default.Restaurant
        MealType.DINNER -> Icons.Default.DinnerDining
        MealType.SHAKE -> Icons.Default.LocalDrink
        MealType.SNACK -> Icons.Default.Cookie
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = NayaColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Meal type icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NayaColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = mealIcon,
                        contentDescription = null,
                        tint = NayaColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    // Name + favorite star
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meal.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = NayaColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (meal.isFavorite) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = NayaColors.Warning,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Macros summary
                    Text(
                        text = "${meal.totalCalories.toInt()} kcal • ${meal.totalProtein.toInt()}g P",
                        style = MaterialTheme.typography.bodySmall,
                        color = NayaColors.TextSecondary
                    )

                    // Usage count
                    Text(
                        text = "${meal.usageCount}x logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = NayaColors.TextTertiary
                    )
                }
            }

            // Right: Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite toggle
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (meal.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (meal.isFavorite) NayaColors.Warning else NayaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Expand for add-ons
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add extras",
                        tint = NayaColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Direct add button
                Button(
                    onClick = onDirectAdd,
                    enabled = !isLoading,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NayaColors.Primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ADD-ONS SELECTION VIEW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AddOnsSelectionView(
    meal: FrequentMeal,
    addOns: List<FrequentAddOn>,
    selectedAddOns: List<FrequentMealItem>,
    isLoading: Boolean,
    onToggleAddOn: (FrequentAddOn) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Back button + Meal name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NayaColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NayaColors.TextPrimary
            )
        }

        // Base meal summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = NayaColors.Primary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroChip("Kcal", meal.totalCalories.toInt().toString(), NayaColors.Calories)
                MacroChip("P", "${meal.totalProtein.toInt()}g", NayaColors.Protein)
                MacroChip("C", "${meal.totalCarbs.toInt()}g", NayaColors.Carbs)
                MacroChip("F", "${meal.totalFat.toInt()}g", NayaColors.Fat)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add-ons section
        if (addOns.isNotEmpty()) {
            Text(
                text = "Often combined with:",
                style = MaterialTheme.typography.labelLarge,
                color = NayaColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(addOns, key = { it.id }) { addOn ->
                    val isSelected = selectedAddOns.any { it.name == addOn.addonName }
                    AddOnChip(
                        addOn = addOn,
                        isSelected = isSelected,
                        onClick = { onToggleAddOn(addOn) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Total with add-ons
        val extraCalories = selectedAddOns.sumOf { it.calories.toDouble() }.toFloat()
        val extraProtein = selectedAddOns.sumOf { it.protein.toDouble() }.toFloat()
        val totalCalories = meal.totalCalories + extraCalories
        val totalProtein = meal.totalProtein + extraProtein

        if (selectedAddOns.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = NayaColors.Success.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "With extras:",
                        style = MaterialTheme.typography.labelMedium,
                        color = NayaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${totalCalories.toInt()} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NayaColors.Calories
                        )
                        Text(
                            text = "${totalProtein.toInt()}g protein",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NayaColors.Protein
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Confirm button
        Button(
            onClick = onConfirm,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NayaColors.Primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedAddOns.isEmpty()) "Quick Add" else "Add with Extras",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AddOnChip(
    addOn: FrequentAddOn,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column {
                Text(
                    text = addOn.addonName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "+${addOn.addonCalories.toInt()} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) NayaColors.Primary else NayaColors.TextSecondary
                )
            }
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = NayaColors.CardBackground,
            selectedContainerColor = NayaColors.Primary.copy(alpha = 0.2f),
            selectedLabelColor = NayaColors.Primary,
            selectedLeadingIconColor = NayaColors.Primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = NayaColors.Border,
            selectedBorderColor = NayaColors.Primary,
            enabled = true,
            selected = isSelected
        )
    )
}

@Composable
private fun MacroChip(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NayaColors.TextSecondary
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptyQuickAddView(
    isFavorites: Boolean,
    onSearchFood: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isFavorites) {
            // Favorites tab empty state
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = NayaColors.Warning.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No favorites yet",
                style = MaterialTheme.typography.titleMedium,
                color = NayaColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap the ☆ star on any meal to save it here",
                style = MaterialTheme.typography.bodyMedium,
                color = NayaColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            // Frequent tab empty state - explain the concept better
            Icon(
                imageVector = Icons.Outlined.FlashOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = NayaColors.Primary.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your Quick Adds will appear here",
                style = MaterialTheme.typography.titleMedium,
                color = NayaColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // How it works section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        NayaColors.CardBackground,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "How it works:",
                    style = MaterialTheme.typography.labelLarge,
                    color = NayaColors.Primary,
                    fontWeight = FontWeight.Bold
                )

                // Step 1
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "1.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaColors.Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = "Log a meal using Search, Snap, or Scan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaColors.TextSecondary
                    )
                }

                // Step 2
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "2.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaColors.Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = "Meals you log often will automatically appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaColors.TextSecondary
                    )
                }

                // Step 3
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "3.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaColors.Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = "One tap to log them again!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dismiss button instead of confusing "Search & Log"
            OutlinedButton(
                onClick = onSearchFood,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NayaColors.TextSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it")
            }
        }
    }
}