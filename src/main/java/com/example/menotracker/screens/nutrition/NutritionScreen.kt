// app/src/main/java/com/example/myapplicationtest/screens/nutrition/NutritionScreen.kt
// REDESIGNED: Protein-Hero, Time-Aware, Wellness-First

package com.example.menotracker.screens.nutrition

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.menotracker.data.models.*
import com.example.menotracker.viewmodels.AccountViewModel
import com.example.menotracker.viewmodels.NutritionViewModel
import com.example.menotracker.screens.nutrition.components.ProteinHeroCard
import com.example.menotracker.screens.nutrition.components.TimeAwareHeroCard
import com.example.menotracker.screens.nutrition.components.CompactMealList
import com.example.menotracker.screens.nutrition.components.MealTimeWindow
import com.example.menotracker.screens.nutrition.components.MacroRingCarousel
import com.example.menotracker.screens.nutrition.components.PreWorkoutAlertCard
import com.example.menotracker.screens.nutrition.components.PostWorkoutAlertCard
import com.example.menotracker.data.PreWorkoutNutritionService
import com.example.menotracker.data.WorkoutPatternRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import com.example.menotracker.billing.Feature
import com.example.menotracker.billing.FeatureGate
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.onboarding.data.SportDatabase
import com.example.menotracker.ui.theme.AppBackground

private const val TAG = "NutritionScreen"

// ═══════════════════════════════════════════════════════════════
// NAYA BRANDING COLORS (from BRANDING.md)
// ═══════════════════════════════════════════════════════════════

// Primary Brand
private val NayaOrange = Color(0xFFE67E22)
private val NayaOrangeBright = Color(0xFFF39C12)
private val NayaOrangeDark = Color(0xFFD35400)

// Dark Mode Backgrounds
private val Background = Color(0xFF141414)
private val Surface = Color(0xFF1C1C1C)
private val SurfaceVariant = Color(0xFF262626)

// Text Colors
private val TextPrimary = Color(0xFFFAFAFA)
private val TextSecondary = Color(0xFF999999)
private val TextTertiary = Color(0xFF666666)

// Glass
private val GlassBase = Color(0xFF333333)

// Semantic
private val SuccessGreen = Color(0xFF4CAF50)
private val ErrorRed = Color(0xFFE53935)

// Nutrition Macro Colors (from BRANDING.md)
private val CaloriesOrange = Color(0xFFF97316)
private val ProteinBlue = Color(0xFF3B82F6)
private val CarbsGreen = Color(0xFF10B981)
private val CarbsPurple = Color(0xFF9B59B6)
private val FatYellow = Color(0xFFFBBF24)

// Legacy aliases for compatibility
private val OrangeGlow = NayaOrangeBright
private val DarkBackground = Background
private val CardBackground = Surface
private val CardBackgroundLight = SurfaceVariant
private val TextWhite = TextPrimary
private val TextGray = TextSecondary
private val DeleteRed = ErrorRed

// ═══════════════════════════════════════════════════════════════
// MAIN SCREEN COMPOSABLE
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    nutritionViewModel: NutritionViewModel,
    accountViewModel: AccountViewModel,
    onNavigateToMealCapture: (MealType) -> Unit,
    onNavigateToInsights: () -> Unit = {},
    onNavigateToFoodSearch: (MealType) -> Unit = {},
    onNavigateToFoodSnap: (MealType) -> Unit = {},
    onNavigateToBarcodeScanner: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {}
) {
    // Feature gate for Nutrition
    FeatureGate(
        feature = Feature.NUTRITION,
        onUpgradeClick = onNavigateToPaywall,
        modifier = Modifier.fillMaxSize()
    ) {
        NutritionScreenContent(
            nutritionViewModel = nutritionViewModel,
            accountViewModel = accountViewModel,
            onNavigateToMealCapture = onNavigateToMealCapture,
            onNavigateToInsights = onNavigateToInsights,
            onNavigateToFoodSearch = onNavigateToFoodSearch,
            onNavigateToFoodSnap = onNavigateToFoodSnap,
            onNavigateToBarcodeScanner = onNavigateToBarcodeScanner
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NutritionScreenContent(
    nutritionViewModel: NutritionViewModel,
    accountViewModel: AccountViewModel,
    onNavigateToMealCapture: (MealType) -> Unit,
    onNavigateToInsights: () -> Unit = {},
    onNavigateToFoodSearch: (MealType) -> Unit = {},
    onNavigateToFoodSnap: (MealType) -> Unit = {},
    onNavigateToBarcodeScanner: () -> Unit = {}
) {
    // ═══════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var mealToEdit by remember { mutableStateOf<Meal?>(null) }
    var dismissedPreWorkoutAlert by remember { mutableStateOf(false) }
    var dismissedPostWorkoutAlert by remember { mutableStateOf(false) }

    // Workout nutrition state
    val context = androidx.compose.ui.platform.LocalContext.current
    val preWorkoutService = remember { PreWorkoutNutritionService.getInstance(context) }
    val preWorkoutState by preWorkoutService.preWorkoutState.collectAsState(initial = null)
    val postWorkoutState by preWorkoutService.postWorkoutState.collectAsState(initial = null)

    // Data from ViewModel
    val nutritionLog by nutritionViewModel.nutritionLog.collectAsState()
    val nutritionGoal by nutritionViewModel.nutritionGoal.collectAsState()
    val isLoading by nutritionViewModel.isLoading.collectAsState()
    val userProfile by accountViewModel.userProfile.collectAsState()

    // Quick Add state
    val showQuickAddSheet by nutritionViewModel.showQuickAddSheet.collectAsState()
    val frequentMeals by nutritionViewModel.frequentMeals.collectAsState()
    val favoriteFrequentMeals by nutritionViewModel.favoriteFrequentMeals.collectAsState()
    val selectedFrequentMeal by nutritionViewModel.selectedFrequentMeal.collectAsState()
    val frequentAddOns by nutritionViewModel.frequentAddOns.collectAsState()
    val selectedAddOns by nutritionViewModel.selectedAddOns.collectAsState()
    val isQuickAdding by nutritionViewModel.isQuickAdding.collectAsState()

    // Anabolic Window from ViewModel (connected to workout tracking)
    val anabolicWindow by nutritionViewModel.anabolicWindow.collectAsState()

    // Hydration state
    val hydrationLog by nutritionViewModel.hydrationLog.collectAsState()

    // Quick Add Suggestion für aktuellen MealType
    val currentMealWindow = remember { MealTimeWindow.current() }
    val quickAddSuggestion = remember(frequentMeals, currentMealWindow) {
        frequentMeals
            .filter { it.mealTypeEnum == currentMealWindow.mealType }
            .filter { it.usageCount >= 3 }
            .maxByOrNull { it.usageCount }
    }

    // ═══════════════════════════════════════════════════════════
    // LOAD DATA
    // ═══════════════════════════════════════════════════════════

    LaunchedEffect(selectedDate, userProfile?.id) {
        userProfile?.id?.let { userId ->
            Log.d(TAG, "Loading nutrition for date: $selectedDate, user: $userId")
            if (selectedDate == LocalDate.now()) {
                nutritionViewModel.loadTodayData(userId)
            } else {
                nutritionViewModel.loadDataForDate(userId, selectedDate.toString())
            }
        }
    }

    // Load frequent meals once when user is available
    LaunchedEffect(userProfile?.id) {
        userProfile?.id?.let { userId ->
            nutritionViewModel.loadFrequentMeals(userId)
        }
    }

    // Update PreWorkoutService with user profile data for smart nutrition timing
    LaunchedEffect(userProfile?.weight) {
        userProfile?.weight?.let { weight ->
            preWorkoutService.setUserBodyWeight(weight.toFloat())
            preWorkoutService.updatePreWorkoutState()
            Log.d(TAG, "📊 Pre-workout service updated with body weight: ${weight}kg")
        }
    }

    // Initialize workout nutrition state (for Anabolic Window)
    LaunchedEffect(Unit) {
        nutritionViewModel.initializeWorkoutNutritionState(context)
    }

    // Load hydration data
    LaunchedEffect(userProfile?.id) {
        userProfile?.id?.let { userId ->
            nutritionViewModel.loadHydrationData(userId)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UI - REDESIGNED: Protein-Hero, Time-Aware, Athlete-First
    // ═══════════════════════════════════════════════════════════

    AppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ═══════════════════════════════════════════════════
            // ① COMPACT DATE HEADER
            // ═══════════════════════════════════════════════════

            item {
                CompactDateHeader(
                    selectedDate = selectedDate,
                    onPreviousDay = { selectedDate = selectedDate.minusDays(1) },
                    onNextDay = {
                        if (selectedDate < LocalDate.now()) {
                            selectedDate = selectedDate.plusDays(1)
                        }
                    },
                    onOpenCalendar = { showDatePicker = true },
                    onOpenSettings = { showSettingsSheet = true },
                    onOpenInsights = onNavigateToInsights,
                    isToday = selectedDate == LocalDate.now()
                )
            }

            // ═══════════════════════════════════════════════════
            // ② MACRO RING CAROUSEL (Quality-Aware)
            // ═══════════════════════════════════════════════════

            item {
                // Calculate macro quality from nutrition log
                val macroQuality = remember(nutritionLog) {
                    nutritionLog?.let { log ->
                        MacroQualitySummary.calculate(log)
                    }
                }

                MacroRingCarousel(
                    nutritionLog = nutritionLog?.copy(
                        targetCalories = nutritionGoal?.targetCalories,
                        targetProtein = nutritionGoal?.targetProtein,
                        targetCarbs = nutritionGoal?.targetCarbs,
                        targetFat = nutritionGoal?.targetFat
                    ),
                    macroQuality = macroQuality,
                    anabolicWindow = anabolicWindow,  // Connected to workout tracking via ViewModel
                    onRingClick = { macroType ->
                        // Could navigate to detailed macro view
                    }
                )
            }

            // ═══════════════════════════════════════════════════
            // ②.5 WORKOUT NUTRITION ALERTS
            // ═══════════════════════════════════════════════════

            // Pre-Workout Alert (only show if not dismissed and relevant)
            if (!dismissedPreWorkoutAlert && preWorkoutState != null) {
                item {
                    PreWorkoutAlertCard(
                        state = preWorkoutState!!,
                        onLogMealClick = {
                            val currentWindow = MealTimeWindow.current()
                            onNavigateToFoodSearch(currentWindow.mealType)
                        },
                        onDismiss = { dismissedPreWorkoutAlert = true }
                    )
                }
            }

            // Post-Workout Alert (only show if not dismissed and relevant)
            if (!dismissedPostWorkoutAlert && postWorkoutState != null) {
                item {
                    PostWorkoutAlertCard(
                        state = postWorkoutState!!,
                        onLogMealClick = {
                            onNavigateToFoodSearch(MealType.SNACK)
                        },
                        onDismiss = { dismissedPostWorkoutAlert = true }
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // ③ TIME-AWARE HERO ACTION
            // ═══════════════════════════════════════════════════

            item {
                TimeAwareHeroCard(
                    quickAddSuggestion = quickAddSuggestion,
                    hydrationLog = hydrationLog,
                    onSnapClick = { mealType -> onNavigateToFoodSnap(mealType) },
                    onScanClick = onNavigateToBarcodeScanner,
                    onSearchClick = { mealType -> onNavigateToFoodSearch(mealType) },
                    onQuickAddClick = { nutritionViewModel.showQuickAdd() },
                    onSnackClick = { onNavigateToFoodSnap(MealType.SNACK) },
                    onAddWater = { amountMl ->
                        userProfile?.id?.let { userId ->
                            nutritionViewModel.addWater(userId, amountMl)
                        }
                    },
                    onQuickAdd = { meal ->
                        userProfile?.id?.let { userId ->
                            nutritionViewModel.directQuickAdd(userId, meal)
                        }
                    }
                )
            }

            // ═══════════════════════════════════════════════════
            // ④ COMPACT MEAL LIST
            // ═══════════════════════════════════════════════════

            item {
                CompactMealList(
                    nutritionLog = nutritionLog,
                    onMealClick = { mealType -> onNavigateToMealCapture(mealType) },
                    onMealEdit = { meal -> mealToEdit = meal },
                    onMealDelete = { meal ->
                        userProfile?.id?.let { userId ->
                            nutritionViewModel.deleteMeal(userId, meal.id)
                        }
                    }
                )
            }

            // ═══════════════════════════════════════════════════
            // ⑤ MENOPAUSE BONE HEALTH (Calcium, Vitamin D, etc.)
            // ═══════════════════════════════════════════════════

            item {
                MenopauseBoneHealthCard(
                    nutritionLog = nutritionLog,
                    onAddFoodClick = {
                        val currentWindow = MealTimeWindow.current()
                        onNavigateToFoodSearch(currentWindow.mealType)
                    }
                )
            }

            // ═══════════════════════════════════════════════════
            // ⑥ EXTENDED NUTRIENTS (kollapsiert)
            // ═══════════════════════════════════════════════════

            item {
                ExtendedNutrientsCard(
                    nutritionLog = nutritionLog
                )
            }
        }
    }  // Close AppBackground

    // FAB actions are now integrated in TimeAwareHeroCard

    // ═══════════════════════════════════════════════════════════
    // DIALOGS & SHEETS
    // ═══════════════════════════════════════════════════════════

    // Date Picker Modal
    if (showDatePicker) {
        DatePickerModal(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Settings Sheet
    if (showSettingsSheet) {
        SettingsSheet(
            nutritionGoal = nutritionGoal,
            userProfile = userProfile,
            onDismiss = { showSettingsSheet = false },
            onSave = { updatedGoal ->
                nutritionViewModel.saveNutritionGoal(updatedGoal)
                showSettingsSheet = false
            },
            userId = userProfile?.id ?: ""
        )
    }

    // Meal Edit Sheet
    mealToEdit?.let { meal ->
        MealEditSheet(
            meal = meal,
            onDismiss = { mealToEdit = null },
            onSave = { updatedMeal, updatedItems, deletedItemIds ->
                userProfile?.id?.let { userId ->
                    nutritionViewModel.updateMeal(
                        userId = userId,
                        meal = updatedMeal,
                        items = updatedItems,
                        deletedItemIds = deletedItemIds
                    )
                }
                mealToEdit = null
            },
            onDeleteMeal = { mealId ->
                userProfile?.id?.let { userId ->
                    nutritionViewModel.deleteMeal(userId, mealId)
                }
                mealToEdit = null
            }
        )
    }

    // Quick Add Sheet
    if (showQuickAddSheet) {
        QuickAddSheet(
            frequentMeals = frequentMeals,
            favoriteFrequentMeals = favoriteFrequentMeals,
            selectedMeal = selectedFrequentMeal,
            frequentAddOns = frequentAddOns,
            selectedAddOns = selectedAddOns,
            isLoading = isQuickAdding,
            onSelectMeal = { meal ->
                userProfile?.id?.let { userId ->
                    nutritionViewModel.selectFrequentMeal(userId, meal)
                }
            },
            onDirectAdd = { meal ->
                userProfile?.id?.let { userId ->
                    nutritionViewModel.directQuickAdd(userId, meal)
                }
            },
            onToggleAddOn = { addOn ->
                nutritionViewModel.toggleAddOn(addOn)
            },
            onToggleFavorite = { meal ->
                userProfile?.id?.let { userId ->
                    nutritionViewModel.toggleFrequentMealFavorite(userId, meal)
                }
            },
            onConfirmAdd = {
                userProfile?.id?.let { userId ->
                    nutritionViewModel.quickAddMeal(userId)
                }
            },
            onDismiss = {
                nutritionViewModel.hideQuickAdd()
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// COMPACT DATE HEADER (Simplified)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CompactDateHeader(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInsights: () -> Unit,
    isToday: Boolean
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Date navigation
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous day",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = if (isToday) "Today" else selectedDate.format(dateFormatter),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                modifier = Modifier.clickable { onOpenCalendar() }
            )

            IconButton(
                onClick = onNextDay,
                enabled = !isToday,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next day",
                    tint = if (isToday) TextGray.copy(alpha = 0.3f) else TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right: Quick actions
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onOpenInsights,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Insights,
                    contentDescription = "Insights",
                    tint = NayaOrange,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DATE PICKER MODAL
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onDateSelected(date)
                    }
                }
            ) {
                Text("Select", color = NayaOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// ═══════════════════════════════════════════════════════════════
// SETTINGS BOTTOM SHEET - GOAL EDITOR
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    nutritionGoal: NutritionGoal?,
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (NutritionGoal) -> Unit,
    userId: String
) {
    // Editable state
    var selectedGoalType by remember { mutableStateOf(nutritionGoal?.goalType ?: GoalType.MAINTENANCE) }
    var calories by remember { mutableStateOf((nutritionGoal?.targetCalories ?: 2500f).toInt().toString()) }
    var protein by remember { mutableStateOf((nutritionGoal?.targetProtein ?: 180f).toInt().toString()) }
    var carbs by remember { mutableStateOf((nutritionGoal?.targetCarbs ?: 280f).toInt().toString()) }
    var fat by remember { mutableStateOf((nutritionGoal?.targetFat ?: 80f).toInt().toString()) }

    // TDEE calculation
    val tdee = userProfile?.calculateTDEE()
    val canCalculateTDEE = userProfile?.weight != null &&
                           userProfile.height != null &&
                           userProfile.age != null &&
                           userProfile.gender != null &&
                           userProfile.activityLevel != null

    // Check if values changed
    val hasChanges = remember(selectedGoalType, calories, protein, carbs, fat) {
        selectedGoalType != (nutritionGoal?.goalType ?: GoalType.MAINTENANCE) ||
        calories != (nutritionGoal?.targetCalories ?: 2500f).toInt().toString() ||
        protein != (nutritionGoal?.targetProtein ?: 180f).toInt().toString() ||
        carbs != (nutritionGoal?.targetCarbs ?: 280f).toInt().toString() ||
        fat != (nutritionGoal?.targetFat ?: 80f).toInt().toString()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBackground
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
                    text = "Nutrition Goals",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextGray
                    )
                }
            }

            // TDEE Info (if available)
            if (tdee != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NayaOrange.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your TDEE",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                            Text(
                                text = "$tdee kcal/day",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeGlow
                            )
                        }
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = OrangeGlow,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goal Type Selector
            Text(
                text = "GOAL TYPE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextGray,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoalType.entries.forEach { goalType ->
                    GoalTypeChip(
                        goalType = goalType,
                        isSelected = selectedGoalType == goalType,
                        onClick = { selectedGoalType = goalType },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Auto-Calculate Button (if profile data available)
            if (canCalculateTDEE) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        userProfile?.calculateSuggestedMacros(selectedGoalType)?.let { suggestion ->
                            calories = suggestion.calories.toString()
                            protein = suggestion.protein.toString()
                            carbs = suggestion.carbs.toString()
                            fat = suggestion.fat.toString()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NayaOrange.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        tint = OrangeGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Auto-Calculate from Profile",
                        color = OrangeGlow,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Macros Section
            Text(
                text = "DAILY TARGETS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextGray,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Calories Input
            MacroInputField(
                label = "Calories",
                value = calories,
                onValueChange = { calories = it.filter { c -> c.isDigit() } },
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = OrangeGlow
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Macros Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroInputField(
                    label = "Protein",
                    value = protein,
                    onValueChange = { protein = it.filter { c -> c.isDigit() } },
                    unit = "g",
                    icon = Icons.Default.FitnessCenter,
                    iconTint = ProteinBlue,
                    modifier = Modifier.weight(1f)
                )
                MacroInputField(
                    label = "Carbs",
                    value = carbs,
                    onValueChange = { carbs = it.filter { c -> c.isDigit() } },
                    unit = "g",
                    icon = Icons.Default.Grain,
                    iconTint = CarbsPurple,
                    modifier = Modifier.weight(1f)
                )
                MacroInputField(
                    label = "Fat",
                    value = fat,
                    onValueChange = { fat = it.filter { c -> c.isDigit() } },
                    unit = "g",
                    icon = Icons.Default.WaterDrop,
                    iconTint = FatYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            // Calculated info
            val totalCals = calories.toIntOrNull() ?: 0
            val proteinCals = (protein.toIntOrNull() ?: 0) * 4
            val carbsCals = (carbs.toIntOrNull() ?: 0) * 4
            val fatCals = (fat.toIntOrNull() ?: 0) * 9
            val macroCals = proteinCals + carbsCals + fatCals
            val diff = totalCals - macroCals

            if (totalCals > 0 && macroCals > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Macros = ${macroCals} kcal (${if (diff >= 0) "+$diff" else diff} vs target)",
                    fontSize = 12.sp,
                    color = if (kotlin.math.abs(diff) < 100) SuccessGreen else TextGray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (userId.isNotEmpty()) {
                        val newGoal = NutritionGoal(
                            id = nutritionGoal?.id ?: java.util.UUID.randomUUID().toString(),
                            userId = userId,
                            goalType = selectedGoalType,
                            targetCalories = calories.toFloatOrNull() ?: 2500f,
                            targetProtein = protein.toFloatOrNull() ?: 180f,
                            targetCarbs = carbs.toFloatOrNull() ?: 280f,
                            targetFat = fat.toFloatOrNull() ?: 80f,
                            mealsPerDay = nutritionGoal?.mealsPerDay ?: 3,
                            isActive = true
                        )
                        onSave(newGoal)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = hasChanges && userId.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NayaOrange,
                    disabledContainerColor = NayaOrange.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasChanges) "Save Goals" else "No Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GoalTypeChip(
    goalType: GoalType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (goalType) {
        GoalType.CUTTING -> Icons.Default.TrendingDown
        GoalType.BULKING -> Icons.Default.TrendingUp
        GoalType.MAINTENANCE -> Icons.Default.Balance
        GoalType.PERFORMANCE -> Icons.Default.Bolt
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NayaOrange.copy(alpha = 0.2f) else CardBackgroundLight,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, NayaOrange)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) NayaOrange else TextGray,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = goalType.displayName,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) NayaOrange else TextGray
            )
        }
    }
}

@Composable
private fun MacroInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextGray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            ),
            suffix = {
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = TextGray
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NayaOrange,
                unfocusedBorderColor = CardBackgroundLight,
                cursorColor = NayaOrange,
                focusedContainerColor = CardBackgroundLight,
                unfocusedContainerColor = CardBackgroundLight
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PERSONALIZED NUTRITION INDICATOR
// ═══════════════════════════════════════════════════════════════

/**
 * Shows personalized nutrition info based on user's sport, diet preferences, and allergies.
 * Displayed at the top of the NutritionScreen to remind users of their personalized settings.
 */
@Composable
private fun PersonalizedNutritionIndicator(
    sportName: String?,
    nutritionRelevance: Float?,
    dietaryPreference: DietaryPreference?,
    allergies: List<FoodAllergy>,
    trainingFocus: Map<String, Float>?
) {
    // Build description based on training focus
    val macroFocusDescription = remember(trainingFocus) {
        when {
            trainingFocus == null -> null
            (trainingFocus["kraft"] ?: 0f) >= 0.7f -> "High protein for strength"
            (trainingFocus["ausdauer"] ?: 0f) >= 0.7f -> "Carb-focused for endurance"
            (trainingFocus["schnelligkeit"] ?: 0f) >= 0.7f -> "Balanced for speed/power"
            else -> "Balanced macros"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = NayaOrange.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, NayaOrange.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = NayaOrange,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Personalized for you",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NayaOrange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sport chip
                if (sportName != null) {
                    NutritionInfoChip(
                        icon = Icons.Default.SportsScore,
                        text = sportName,
                        color = ProteinBlue
                    )
                }

                // Dietary preference chip
                if (dietaryPreference != null && dietaryPreference != DietaryPreference.OMNIVORE) {
                    NutritionInfoChip(
                        icon = Icons.Default.Restaurant,
                        text = dietaryPreference.displayName,
                        color = CarbsGreen
                    )
                }
            }

            // Macro focus description
            if (macroFocusDescription != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = macroFocusDescription,
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            // Allergies warning
            if (allergies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = FatYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Allergies: ${allergies.joinToString(", ") { it.displayName }}",
                        fontSize = 11.sp,
                        color = FatYellow
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionInfoChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
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
        MealType.DINNER -> Icons.Default.DarkMode
        MealType.SHAKE -> Icons.Default.LocalCafe
        MealType.SNACK -> Icons.Default.Cookie
    }
}