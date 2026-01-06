// screens/nutrition/FoodSearchScreen.kt
package com.example.menotracker.screens.nutrition

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.menotracker.ui.theme.NayaOrange
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.MealItem
import com.example.menotracker.data.models.MealType
import com.example.menotracker.data.models.FoodModification
import com.example.menotracker.data.models.ModificationCategory
import com.example.menotracker.data.models.MilkModifications
import com.example.menotracker.data.models.SweetenerModifications
import com.example.menotracker.data.models.SizeModifications
import com.example.menotracker.data.models.detectRelevantModifications
import com.example.menotracker.data.models.getModificationOptions
import com.example.menotracker.data.repository.FoodSearchRepository
import com.example.menotracker.data.repository.UserFoodRepository
import com.example.menotracker.ui.theme.NayaSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * FOOD SEARCH SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Unified food search across multiple data sources:
 * - USDA FoodData Central (1.9M+ foods)
 * - Restaurant Database (500+ fast food meals)
 * - Open Food Facts (coming soon - barcode scanning)
 * - User's recent/favorite foods
 *
 * Features:
 * - Real-time search with debouncing
 * - Source filtering (USDA, Restaurant, Recent)
 * - Portion size selection
 * - Quick-add for common items
 * ═══════════════════════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    mealType: MealType,
    mealId: String,
    onFoodSelected: (MealItem) -> Unit,
    onNavigateBack: () -> Unit,
    onBarcodeScan: (() -> Unit)? = null,  // Optional barcode scanner
    userId: String? = null,  // For loading user's personal food library
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<SearchableFood>>(emptyList()) }
    var selectedSource by remember { mutableStateOf(FoodSource.ALL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Portion selection state
    var selectedFood by remember { mutableStateOf<SearchableFood?>(null) }
    var showPortionSheet by remember { mutableStateOf(false) }

    // Custom food creation state
    var showCustomFoodSheet by remember { mutableStateOf(false) }
    var customFoodName by remember { mutableStateOf("") }

    // Recent searches (would come from DataStore in production)
    var recentSearches by remember { mutableStateOf(listOf("chicken breast", "rice", "banana", "protein shake")) }

    // Debounced search
    LaunchedEffect(searchQuery, selectedSource) {
        if (searchQuery.length >= 2) {
            delay(300) // Debounce
            isSearching = true
            errorMessage = null

            try {
                searchResults = performSearch(searchQuery, selectedSource)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore - user navigated away or search was cancelled
            } catch (e: Exception) {
                // Only show user-relevant errors, not technical ones
                val isTechnicalError = e.message?.contains("coroutine") == true ||
                        e.message?.contains("scope") == true ||
                        e.message?.contains("composition") == true
                if (!isTechnicalError) {
                    errorMessage = e.message ?: "Search failed"
                }
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        } else if (searchQuery.isEmpty()) {
            searchResults = emptyList()
        }
    }

    // Auto-focus search field
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    // Portion selection sheet with modifications
    if (showPortionSheet && selectedFood != null) {
        PortionSelectionSheetWithModifications(
            food = selectedFood!!,
            mealId = mealId,
            userId = userId,
            onDismiss = {
                showPortionSheet = false
                selectedFood = null
            },
            onConfirm = { mealItem ->
                showPortionSheet = false
                selectedFood = null
                onFoodSelected(mealItem)
            }
        )
    }

    // Custom food creation sheet
    if (showCustomFoodSheet) {
        CustomFoodCreationSheet(
            initialName = if (searchQuery.isNotBlank()) searchQuery else customFoodName,
            mealId = mealId,
            userId = userId,
            onDismiss = {
                showCustomFoodSheet = false
                customFoodName = ""
            },
            onFoodCreated = { mealItem ->
                showCustomFoodSheet = false
                customFoodName = ""
                onFoodSelected(mealItem)
            }
        )
    }

    Scaffold(
        topBar = {
            // Custom search header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Back button + Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        Text(
                            text = "Add to ${mealType.displayName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        // Barcode scanner button
                        if (onBarcodeScan != null) {
                            IconButton(onClick = onBarcodeScan) {
                                Icon(
                                    Icons.Outlined.QrCodeScanner,
                                    contentDescription = "Scan Barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Search foods...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            } else if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Source filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(FoodSource.entries) { source ->
                            FilterChip(
                                selected = selectedSource == source,
                                onClick = { selectedSource = source },
                                label = { Text(source.displayName) },
                                leadingIcon = if (selectedSource == source) {
                                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                                } else {
                                    { Icon(source.icon, contentDescription = null, Modifier.size(16.dp)) }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Error message
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Content
            if (searchQuery.isEmpty()) {
                // Show recent searches and quick picks
                RecentSearchesAndQuickPicks(
                    recentSearches = recentSearches,
                    onSearchClick = { searchQuery = it },
                    onQuickPick = { food ->
                        selectedFood = food
                        showPortionSheet = true
                    },
                    userId = userId
                )
            } else if (isSearching && searchResults.isEmpty()) {
                // Currently searching - show loading state
                SearchingState(query = searchQuery)
            } else if (searchResults.isEmpty() && !isSearching) {
                // No results found
                NoResultsState(
                    query = searchQuery,
                    onCreateCustom = {
                        customFoodName = searchQuery
                        showCustomFoodSheet = true
                    }
                )
            } else {
                // Search results
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Results count or still searching indicator
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${searchResults.size} results",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = NayaOrange
                                )
                            }
                        }
                    }

                    items(searchResults) { food ->
                        FoodSearchResultCard(
                            food = food,
                            onClick = {
                                selectedFood = food
                                showPortionSheet = true

                                // Add to recent searches
                                if (!recentSearches.contains(searchQuery.lowercase())) {
                                    recentSearches = (listOf(searchQuery.lowercase()) + recentSearches).take(10)
                                }
                            }
                        )
                    }

                    // Load more indicator (for pagination)
                    if (searchResults.size >= 25) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Scroll for more results...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// FOOD SEARCH RESULT CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FoodSearchResultCard(
    food: SearchableFood,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(food.sourceColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = food.sourceIcon,
                    contentDescription = null,
                    tint = food.sourceColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Food info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (food.brand != null) {
                    Text(
                        text = food.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Macros row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MacroChip(
                        value = "${food.caloriesPer100g.toInt()}",
                        label = "kcal",
                        color = Color(0xFFE57373)
                    )
                    MacroChip(
                        value = "${food.proteinPer100g.toInt()}g",
                        label = "P",
                        color = Color(0xFF64B5F6)
                    )
                    MacroChip(
                        value = "${food.carbsPer100g.toInt()}g",
                        label = "C",
                        color = Color(0xFFFFB74D)
                    )
                    MacroChip(
                        value = "${food.fatPer100g.toInt()}g",
                        label = "F",
                        color = Color(0xFF81C784)
                    )
                }
            }

            // Arrow
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MacroChip(
    value: String,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$value $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// PORTION SELECTION SHEET
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortionSelectionSheet(
    food: SearchableFood,
    mealId: String,
    onDismiss: () -> Unit,
    onConfirm: (MealItem) -> Unit
) {
    var selectedPortion by remember { mutableStateOf(food.defaultPortion) }
    var customQuantity by remember { mutableStateOf(food.defaultPortion.gramWeight.toString()) }
    var useCustom by remember { mutableStateOf(false) }

    val effectiveGrams = if (useCustom) {
        customQuantity.toFloatOrNull() ?: food.defaultPortion.gramWeight
    } else {
        selectedPortion.gramWeight
    }

    // Calculate adjusted macros
    val multiplier = effectiveGrams / 100f
    val adjustedCalories = food.caloriesPer100g * multiplier
    val adjustedProtein = food.proteinPer100g * multiplier
    val adjustedCarbs = food.carbsPer100g * multiplier
    val adjustedFat = food.fatPer100g * multiplier

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = food.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (food.brand != null) {
                Text(
                    text = food.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Portion selection
            Text(
                text = "Select Portion",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Preset portions
            if (food.portions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(food.portions) { portion ->
                        FilterChip(
                            selected = !useCustom && selectedPortion == portion,
                            onClick = {
                                selectedPortion = portion
                                useCustom = false
                            },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(portion.description)
                                    Text(
                                        "${portion.gramWeight.toInt()}g",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        )
                    }

                    // Custom option
                    item {
                        FilterChip(
                            selected = useCustom,
                            onClick = { useCustom = true },
                            label = { Text("Custom") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Custom input
            AnimatedVisibility(visible = useCustom || food.portions.isEmpty()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customQuantity,
                        onValueChange = {
                            if (it.isEmpty() || it.toFloatOrNull() != null) {
                                customQuantity = it
                            }
                        },
                        label = { Text("Amount (grams)") },
                        suffix = { Text("g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nutrition preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Nutrition for ${effectiveGrams.toInt()}g",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NutritionPreviewItem(
                            value = adjustedCalories.toInt().toString(),
                            label = "Calories",
                            color = Color(0xFFE57373)
                        )
                        NutritionPreviewItem(
                            value = "${adjustedProtein.toInt()}g",
                            label = "Protein",
                            color = Color(0xFF64B5F6)
                        )
                        NutritionPreviewItem(
                            value = "${adjustedCarbs.toInt()}g",
                            label = "Carbs",
                            color = Color(0xFFFFB74D)
                        )
                        NutritionPreviewItem(
                            value = "${adjustedFat.toInt()}g",
                            label = "Fat",
                            color = Color(0xFF81C784)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add button
            Button(
                onClick = {
                    val mealItem = food.toMealItem(
                        mealId = mealId,
                        quantity = effectiveGrams
                    )
                    onConfirm(mealItem)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add ${adjustedCalories.toInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NutritionPreviewItem(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// ENHANCED PORTION SELECTION SHEET WITH MODIFICATIONS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Enhanced portion selection sheet with food modifications
 * - Milk type (Oat, Soy, Almond, etc.)
 * - Sugar level (No Sugar, Half, Regular)
 * - Size (Small, Medium, Large, Starbucks sizes)
 * - Auto-saves to user library
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortionSelectionSheetWithModifications(
    food: SearchableFood,
    mealId: String,
    userId: String?,
    onDismiss: () -> Unit,
    onConfirm: (MealItem) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Portion state
    var selectedPortion by remember { mutableStateOf(food.defaultPortion) }
    var customQuantity by remember { mutableStateOf(food.defaultPortion.gramWeight.toString()) }
    var useCustom by remember { mutableStateOf(false) }

    // Modification state
    val relevantCategories = remember(food.name, food.brand) {
        detectRelevantModifications(food.name, food.brand)
    }
    var selectedModifications by remember { mutableStateOf<Map<ModificationCategory, FoodModification>>(emptyMap()) }
    var isSaving by remember { mutableStateOf(false) }

    // Calculate effective grams
    val effectiveGrams = if (useCustom) {
        customQuantity.toFloatOrNull() ?: food.defaultPortion.gramWeight
    } else {
        selectedPortion.gramWeight
    }

    // Calculate adjusted macros with modifications
    val modificationCalorieAdjustment = selectedModifications.values.sumOf { it.calorieAdjustment.toDouble() }.toFloat()
    val modificationProteinAdjustment = selectedModifications.values.sumOf { it.proteinAdjustment.toDouble() }.toFloat()
    val modificationCarbAdjustment = selectedModifications.values.sumOf { it.carbAdjustment.toDouble() }.toFloat()
    val modificationFatAdjustment = selectedModifications.values.sumOf { it.fatAdjustment.toDouble() }.toFloat()

    val multiplier = effectiveGrams / 100f
    val adjustedCalories = (food.caloriesPer100g + modificationCalorieAdjustment) * multiplier
    val adjustedProtein = (food.proteinPer100g + modificationProteinAdjustment) * multiplier
    val adjustedCarbs = (food.carbsPer100g + modificationCarbAdjustment) * multiplier
    val adjustedFat = (food.fatPer100g + modificationFatAdjustment) * multiplier

    // Generate display name with modifications
    val displayName = remember(food.name, food.brand, selectedModifications) {
        buildString {
            food.brand?.let { append("$it ") }
            append(food.name)
            if (selectedModifications.isNotEmpty()) {
                append(" ")
                append(selectedModifications.values.mapNotNull { it.shortLabel }.joinToString(" "))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with emoji
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(food.sourceColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = food.emoji, fontSize = 24.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (food.brand != null && selectedModifications.isEmpty()) {
                        Text(
                            text = food.brand,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // MODIFICATIONS SECTION (only for relevant foods)
            // ═══════════════════════════════════════════════════════════

            if (relevantCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                // Header with customization icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = NayaOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Customize",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modification chips for each category
                relevantCategories.forEach { category ->
                    ModificationCategoryRow(
                        category = category,
                        brand = food.brand,
                        selectedModification = selectedModifications[category],
                        onSelect = { modification ->
                            selectedModifications = if (modification == null) {
                                selectedModifications - category
                            } else {
                                selectedModifications + (category to modification)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════
            // PORTION SELECTION
            // ═══════════════════════════════════════════════════════════

            Text(
                text = "Portion Size",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Preset portions
            if (food.portions.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(food.portions) { portion ->
                        FilterChip(
                            selected = !useCustom && selectedPortion == portion,
                            onClick = {
                                selectedPortion = portion
                                useCustom = false
                            },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(portion.description, fontSize = 13.sp)
                                    Text(
                                        "${portion.gramWeight.toInt()}g",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        )
                    }

                    // Custom option
                    item {
                        FilterChip(
                            selected = useCustom,
                            onClick = { useCustom = true },
                            label = { Text("Custom") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Custom input
            AnimatedVisibility(visible = useCustom || food.portions.isEmpty()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customQuantity,
                        onValueChange = {
                            if (it.isEmpty() || it.toFloatOrNull() != null) {
                                customQuantity = it
                            }
                        },
                        label = { Text("Amount (grams)") },
                        suffix = { Text("g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═══════════════════════════════════════════════════════════
            // NUTRITION PREVIEW
            // ═══════════════════════════════════════════════════════════

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
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
                            text = "Nutrition for ${effectiveGrams.toInt()}g",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        // Show modification impact
                        if (modificationCalorieAdjustment != 0f) {
                            val sign = if (modificationCalorieAdjustment > 0) "+" else ""
                            Text(
                                text = "$sign${(modificationCalorieAdjustment * multiplier).toInt()} from mods",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (modificationCalorieAdjustment > 0) Color(0xFFE57373) else Color(0xFF81C784)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NutritionPreviewItem(
                            value = adjustedCalories.toInt().toString(),
                            label = "Calories",
                            color = Color(0xFFE57373)
                        )
                        NutritionPreviewItem(
                            value = "${adjustedProtein.toInt()}g",
                            label = "Protein",
                            color = Color(0xFF64B5F6)
                        )
                        NutritionPreviewItem(
                            value = "${adjustedCarbs.toInt()}g",
                            label = "Carbs",
                            color = Color(0xFFFFB74D)
                        )
                        NutritionPreviewItem(
                            value = "${adjustedFat.toInt()}g",
                            label = "Fat",
                            color = Color(0xFF81C784)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════════════
            // ADD BUTTON (with auto-save)
            // ═══════════════════════════════════════════════════════════

            // Naya Orange Button
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true

                        // Create meal item with adjusted macros
                        val mealItem = MealItem(
                            id = UUID.randomUUID().toString(),
                            mealId = mealId,
                            foodId = food.id,
                            itemName = displayName,
                            quantity = effectiveGrams,
                            quantityUnit = "g",
                            calories = adjustedCalories,
                            protein = adjustedProtein,
                            carbs = adjustedCarbs,
                            fat = adjustedFat,
                            fiber = food.fiberPer100g * multiplier,
                            sugar = food.sugarPer100g * multiplier,
                            sodium = food.sodiumPer100g * multiplier
                        )

                        // Auto-save to user library if user is logged in
                        // (only for non-user-library foods, or if modifications were added)
                        if (userId != null && (food.source != FoodDataSource.USER_LIBRARY || selectedModifications.isNotEmpty())) {
                            try {
                                // Check if this exact food+modifications already exists
                                val existing = UserFoodRepository.findExisting(
                                    userId = userId,
                                    baseFoodId = food.id,
                                    baseName = food.name,
                                    modifications = selectedModifications.values.toList()
                                ).getOrNull()

                                if (existing != null) {
                                    // Increment use count for existing food
                                    UserFoodRepository.incrementUseCount(existing.id)
                                } else {
                                    // Save as new user food
                                    UserFoodRepository.saveUserFood(
                                        userId = userId,
                                        food = food,
                                        modifications = selectedModifications.values.toList(),
                                        servingSize = effectiveGrams,
                                        servingUnit = "g"
                                    )
                                }
                            } catch (e: Exception) {
                                // Silently fail - don't block the main action
                                android.util.Log.e("FoodSearch", "Failed to save user food", e)
                            }
                        }

                        isSaving = false
                        onConfirm(mealItem)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NayaOrange,
                    contentColor = Color.White
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add ${adjustedCalories.toInt()} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Hint about saving
            if (userId != null && food.source != FoodDataSource.USER_LIBRARY) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE91E63).copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Will be saved to My Foods for quick access",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Row of modification chips for a single category
 */
@Composable
private fun ModificationCategoryRow(
    category: ModificationCategory,
    brand: String?,
    selectedModification: FoodModification?,
    onSelect: (FoodModification?) -> Unit
) {
    val options = remember(category, brand) {
        getModificationOptions(category, brand)
    }

    if (options.isEmpty()) return

    Column {
        // Category label
        Text(
            text = when (category) {
                ModificationCategory.MILK -> "Milk"
                ModificationCategory.SWEETENER -> "Sugar"
                ModificationCategory.SIZE -> "Size"
                ModificationCategory.EXTRA -> "Extra"
                ModificationCategory.TOPPING -> "Topping"
                ModificationCategory.PREPARATION -> "Preparation"
                ModificationCategory.SIDE -> "Side"
                ModificationCategory.OTHER -> "Other"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Modification chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "Default" option to clear selection
            item {
                FilterChip(
                    selected = selectedModification == null,
                    onClick = { onSelect(null) },
                    label = { Text("Default") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            items(options) { modification ->
                FilterChip(
                    selected = selectedModification?.value == modification.value,
                    onClick = { onSelect(modification) },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(modification.shortLabel ?: modification.value, fontSize = 13.sp)
                            if (modification.calorieAdjustment != 0f) {
                                val sign = if (modification.calorieAdjustment > 0) "+" else ""
                                Text(
                                    text = "$sign${modification.calorieAdjustment.toInt()} cal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (modification.calorieAdjustment > 0)
                                        Color(0xFFE57373) else Color(0xFF81C784)
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        // Naya Orange theme for all selections
                        selectedContainerColor = NayaOrange.copy(alpha = 0.2f),
                        selectedLabelColor = NayaOrange
                    )
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// RECENT SEARCHES & QUICK PICKS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RecentSearchesAndQuickPicks(
    recentSearches: List<String>,
    onSearchClick: (String) -> Unit,
    onQuickPick: (SearchableFood) -> Unit,
    userId: String? = null
) {
    val scope = rememberCoroutineScope()

    // Load user's top picks from their personal library
    var userTopPicks by remember { mutableStateOf<List<SearchableFood>>(emptyList()) }
    var isLoadingUserFoods by remember { mutableStateOf(false) }

    // Load user foods when userId is available
    LaunchedEffect(userId) {
        if (!userId.isNullOrBlank()) {
            isLoadingUserFoods = true
            try {
                val result = FoodSearchRepository.getUserTopPicks(userId, limit = 6)
                userTopPicks = result.getOrElse { emptyList() }
            } catch (e: Exception) {
                userTopPicks = emptyList()
            } finally {
                isLoadingUserFoods = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MY FOODS SECTION (User's personal foods - shown FIRST!)
        if (userTopPicks.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),  // Pink to match USER_LIBRARY
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "My Foods",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                MyFoodsGrid(
                    foods = userTopPicks,
                    onFoodClick = onQuickPick
                )
            }
        } else if (isLoadingUserFoods) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFE91E63)
                    )
                    Text(
                        text = "Loading your foods...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Recent searches
        if (recentSearches.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentSearches) { search ->
                        SuggestionChip(
                            onClick = { onSearchClick(search) },
                            label = { Text(search) },
                            icon = {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Quick picks (common foods) - only show if user doesn't have their own foods
        if (userTopPicks.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quick Picks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                QuickPicksGrid(onQuickPick = onQuickPick)
            }
        }

        // Categories
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Browse Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            CategoriesGrid(onCategoryClick = onSearchClick)
        }
    }
}

/**
 * Grid showing user's personal foods (favorites + most used)
 */
@Composable
private fun MyFoodsGrid(
    foods: List<SearchableFood>,
    onFoodClick: (SearchableFood) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        foods.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { food ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFoodClick(food) },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE91E63).copy(alpha = 0.1f)  // Pink tint
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFE91E63).copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Emoji or icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE91E63).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = food.emoji,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${food.caloriesPer100g.toInt()} kcal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${food.proteinPer100g.toInt()}g P",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64B5F6)
                                    )
                                }
                            }
                        }
                    }
                }
                // Fill empty space if odd number
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickPicksGrid(
    onQuickPick: (SearchableFood) -> Unit
) {
    val quickPicks = remember { getQuickPickFoods() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quickPicks.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { food ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onQuickPick(food) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NayaOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getQuickPickIcon(food.id),
                                    contentDescription = food.name,
                                    modifier = Modifier.size(20.dp),
                                    tint = NayaOrange
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${food.caloriesPer100g.toInt()} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                // Fill empty space if odd number
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Get appropriate icon for quick pick foods
 */
private fun getQuickPickIcon(foodId: String): ImageVector {
    return when (foodId) {
        "quick_chicken" -> Icons.Outlined.SetMeal
        "quick_rice" -> Icons.Outlined.RiceBowl
        "quick_banana" -> Icons.Outlined.Park
        "quick_eggs" -> Icons.Outlined.Egg
        "quick_oats" -> Icons.Outlined.BreakfastDining
        "quick_salmon" -> Icons.Outlined.SetMeal
        "quick_broccoli" -> Icons.Outlined.Spa
        "quick_avocado" -> Icons.Outlined.Eco
        else -> Icons.Outlined.Restaurant
    }
}

@Composable
private fun CategoriesGrid(
    onCategoryClick: (String) -> Unit
) {
    val categories = listOf(
        FoodCategory(Icons.Outlined.SetMeal, "Protein", "chicken beef fish"),
        FoodCategory(Icons.Outlined.Spa, "Vegetables", "broccoli spinach"),
        FoodCategory(Icons.Outlined.Park, "Fruits", "apple banana"),
        FoodCategory(Icons.Outlined.Grain, "Grains", "rice oats bread"),
        FoodCategory(Icons.Outlined.WaterDrop, "Dairy", "milk cheese yogurt"),
        FoodCategory(Icons.Outlined.Fastfood, "Fast Food", "burger pizza")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(3).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { category ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryClick(category.searchQuery) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.name,
                                modifier = Modifier.size(28.dp),
                                tint = NayaOrange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// NO RESULTS STATE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchingState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated search icon
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = NayaOrange,
            strokeWidth = 3.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Searching for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Give me a moment, I'll find it for you...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoResultsState(
    query: String,
    onCreateCustom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No results for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try different keywords or create a custom food",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = onCreateCustom) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Custom Food")
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// DATA MODELS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Unified food model for search results from any source
 */
data class SearchableFood(
    val id: String,
    val source: FoodDataSource,
    val name: String,
    val brand: String? = null,
    val emoji: String = "🍽️",

    // Nutrition per 100g
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float = 0f,
    val sugarPer100g: Float = 0f,
    val sodiumPer100g: Float = 0f,

    // Portions
    val portions: List<FoodPortion> = emptyList(),
    val defaultPortion: FoodPortion = FoodPortion("100g", 100f),

    // Metadata
    val barcode: String? = null,
    val category: String? = null,
    val verified: Boolean = false
) {
    val sourceIcon: ImageVector
        get() = when (source) {
            FoodDataSource.USER_LIBRARY -> Icons.Outlined.Favorite  // User's foods first!
            FoodDataSource.USDA -> Icons.Outlined.VerifiedUser
            FoodDataSource.RESTAURANT -> Icons.Outlined.Restaurant
            FoodDataSource.OPEN_FOOD_FACTS -> Icons.Outlined.QrCode
            FoodDataSource.USER_CREATED -> Icons.Outlined.Person
            FoodDataSource.RECENT -> Icons.Outlined.History
        }

    val sourceColor: Color
        get() = when (source) {
            FoodDataSource.USER_LIBRARY -> Color(0xFFE91E63)   // Pink - stands out
            FoodDataSource.USDA -> Color(0xFF4CAF50)           // Green
            FoodDataSource.RESTAURANT -> Color(0xFFFF9800)     // Orange
            FoodDataSource.OPEN_FOOD_FACTS -> Color(0xFF2196F3) // Blue
            FoodDataSource.USER_CREATED -> Color(0xFF9C27B0)   // Purple
            FoodDataSource.RECENT -> Color(0xFF607D8B)         // Grey
        }

    /**
     * Convert to MealItem for saving
     */
    fun toMealItem(mealId: String, quantity: Float): MealItem {
        val multiplier = quantity / 100f
        return MealItem(
            id = UUID.randomUUID().toString(),
            mealId = mealId,
            foodId = id,
            itemName = if (brand != null) "$name ($brand)" else name,
            quantity = quantity,
            quantityUnit = "g",
            calories = caloriesPer100g * multiplier,
            protein = proteinPer100g * multiplier,
            carbs = carbsPer100g * multiplier,
            fat = fatPer100g * multiplier,
            fiber = fiberPer100g * multiplier,
            sugar = sugarPer100g * multiplier,
            sodium = sodiumPer100g * multiplier
        )
    }
}

data class FoodPortion(
    val description: String,
    val gramWeight: Float
)

enum class FoodDataSource {
    USER_LIBRARY,   // User's personal food library (highest priority)
    USDA,
    RESTAURANT,
    OPEN_FOOD_FACTS,
    USER_CREATED,
    RECENT
}

enum class FoodSource(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Search),
    MY_FOODS("My Foods", Icons.Outlined.Favorite),   // User's personal library
    USDA("USDA", Icons.Outlined.VerifiedUser),
    OPEN_FOOD_FACTS("Products", Icons.Outlined.QrCode),
    RESTAURANT("Restaurants", Icons.Outlined.Restaurant),
    RECENT("Recent", Icons.Outlined.History)
}

private data class FoodCategory(
    val icon: ImageVector,
    val name: String,
    val searchQuery: String
)


// ═══════════════════════════════════════════════════════════════════════════════
// SEARCH FUNCTION - CONNECTED TO REPOSITORIES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Perform search across data sources using FoodSearchRepository
 */
private suspend fun performSearch(query: String, source: FoodSource): List<SearchableFood> {
    return FoodSearchRepository.searchFoods(
        query = query,
        source = source,
        limit = 25
    ).getOrElse { emptyList() }
}

/**
 * Get quick pick foods from FoodSearchRepository
 */
private fun getQuickPickFoods(): List<SearchableFood> {
    return FoodSearchRepository.getQuickPickFoods()
}


// ═══════════════════════════════════════════════════════════════════════════════
// CUSTOM FOOD CREATION SHEET
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Bottom sheet for creating a custom food item with manual macro entry.
 * Saves to user's personal food library for quick access later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomFoodCreationSheet(
    initialName: String,
    mealId: String,
    userId: String?,
    onDismiss: () -> Unit,
    onFoodCreated: (MealItem) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Form state
    var foodName by remember { mutableStateOf(initialName) }
    var servingSize by remember { mutableStateOf("100") }
    var servingUnit by remember { mutableStateOf("g") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Validation
    val isValid = foodName.isNotBlank() &&
            calories.toFloatOrNull() != null &&
            protein.toFloatOrNull() != null &&
            carbs.toFloatOrNull() != null &&
            fat.toFloatOrNull() != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NayaSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Custom Food",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Food Name
            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food Name") },
                placeholder = { Text("e.g., Homemade Protein Pancakes") },
                leadingIcon = {
                    Icon(Icons.Default.Restaurant, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NayaOrange,
                    focusedLabelColor = NayaOrange
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Serving Size Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = servingSize,
                    onValueChange = { servingSize = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Serving Size") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NayaOrange
                    )
                )

                // Unit selector
                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(0.6f)
                ) {
                    OutlinedTextField(
                        value = servingUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NayaOrange
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        listOf("g", "ml", "oz", "cup", "tbsp", "tsp", "piece").forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = {
                                    servingUnit = unit
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Macros Section Header
            Text(
                text = "NUTRITION (per serving)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Calories (Full width, emphasized)
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Calories *") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFF97316) // Orange
                    )
                },
                suffix = { Text("kcal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NayaOrange,
                    focusedLabelColor = NayaOrange
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Macros Row (Protein, Carbs, Fat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroInputField(
                    value = protein,
                    onValueChange = { protein = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Protein *",
                    color = Color(0xFF3B82F6), // Blue
                    modifier = Modifier.weight(1f)
                )
                MacroInputField(
                    value = carbs,
                    onValueChange = { carbs = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Carbs *",
                    color = Color(0xFF10B981), // Green
                    modifier = Modifier.weight(1f)
                )
                MacroInputField(
                    value = fat,
                    onValueChange = { fat = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Fat *",
                    color = Color(0xFFFBBF24), // Yellow
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optional: Fiber & Sugar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroInputField(
                    value = fiber,
                    onValueChange = { fiber = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Fiber",
                    color = Color(0xFFA78BFA), // Purple
                    modifier = Modifier.weight(1f)
                )
                MacroInputField(
                    value = sugar,
                    onValueChange = { sugar = it.filter { c -> c.isDigit() || c == '.' } },
                    label = "Sugar",
                    color = Color(0xFFEC4899), // Pink
                    modifier = Modifier.weight(1f)
                )
            }

            // Calculated macro check
            val totalMacros = (protein.toFloatOrNull() ?: 0f) * 4 +
                    (carbs.toFloatOrNull() ?: 0f) * 4 +
                    (fat.toFloatOrNull() ?: 0f) * 9
            val enteredCalories = calories.toFloatOrNull() ?: 0f
            val calorieDiff = enteredCalories - totalMacros

            if (enteredCalories > 0 && totalMacros > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val diffText = if (calorieDiff >= 0) "+${calorieDiff.toInt()}" else calorieDiff.toInt().toString()
                Text(
                    text = "Macros = ${totalMacros.toInt()} kcal ($diffText vs entered)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (kotlin.math.abs(calorieDiff) < 50) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Error message
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (!isValid) {
                        errorMessage = "Please fill in all required fields (*)"
                        return@Button
                    }

                    isSaving = true
                    errorMessage = null

                    // Create the MealItem
                    val servingSizeFloat = servingSize.toFloatOrNull() ?: 100f
                    val mealItem = MealItem(
                        id = UUID.randomUUID().toString(),
                        mealId = mealId,
                        foodId = "custom_${UUID.randomUUID()}",
                        itemName = foodName.trim(),
                        quantity = servingSizeFloat,
                        quantityUnit = servingUnit,
                        calories = calories.toFloatOrNull() ?: 0f,
                        protein = protein.toFloatOrNull() ?: 0f,
                        carbs = carbs.toFloatOrNull() ?: 0f,
                        fat = fat.toFloatOrNull() ?: 0f,
                        fiber = fiber.toFloatOrNull() ?: 0f,
                        sugar = sugar.toFloatOrNull() ?: 0f
                    )

                    // Optionally save to user's food library for future use
                    if (userId != null) {
                        scope.launch {
                            try {
                                UserFoodRepository.saveCustomFood(
                                    userId = userId,
                                    name = foodName.trim(),
                                    servingSize = servingSizeFloat,
                                    servingUnit = servingUnit,
                                    calories = calories.toFloatOrNull() ?: 0f,
                                    protein = protein.toFloatOrNull() ?: 0f,
                                    carbs = carbs.toFloatOrNull() ?: 0f,
                                    fat = fat.toFloatOrNull() ?: 0f,
                                    fiber = fiber.toFloatOrNull(),
                                    sugar = sugar.toFloatOrNull()
                                )
                            } catch (e: Exception) {
                                // Log but don't block - food can still be added to current meal
                                android.util.Log.e("CustomFood", "Failed to save to library: ${e.message}")
                            }
                        }
                    }

                    onFoodCreated(mealItem)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = isValid && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NayaOrange,
                    disabledContainerColor = NayaOrange.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add to Meal",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Small macro input field for the custom food form
 */
@Composable
private fun MacroInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        suffix = { Text("g", style = MaterialTheme.typography.bodySmall) },
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            focusedLabelColor = color
        )
    )
}