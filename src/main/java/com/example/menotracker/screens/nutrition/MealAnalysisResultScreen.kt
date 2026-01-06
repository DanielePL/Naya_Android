// app/src/main/java/com/example/myapplicationtest/screens/nutrition/MealAnalysisResultScreen.kt

package com.example.menotracker.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import com.example.menotracker.data.models.MealType
import com.example.menotracker.data.models.AIAnalyzedItem
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.viewmodels.NutritionViewModel
import com.example.menotracker.viewmodels.AccountViewModel
import io.github.jan.supabase.gotrue.auth

// Design System
private val orangePrimary = Color(0xFFFF9D50)
private val orangeGlow = Color(0xFFFFAA5E)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF2a1f1a), Color(0xFF0f0f0f), Color(0xFF1a1410))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealAnalysisResultScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    initialMealType: MealType = MealType.LUNCH,
    nutritionViewModel: NutritionViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel()
) {
    val context = LocalContext.current
    val analysisResult by nutritionViewModel.aiAnalysisResult.collectAsState()
    val isLoading by nutritionViewModel.isLoading.collectAsState()
    val errorMessage by nutritionViewModel.errorMessage.collectAsState()
    val analyzedImageFile by nutritionViewModel.analyzedImageFile.collectAsState()
    val userProfile by accountViewModel.userProfile.collectAsState()

    // Get meal type from ViewModel (set by MealPhotoCaptureScreen)
    val viewModelMealType by nutritionViewModel.selectedMealType.collectAsState()
    var selectedMealType by remember { mutableStateOf(viewModelMealType) }

    // Sync with ViewModel on load
    LaunchedEffect(viewModelMealType) {
        selectedMealType = viewModelMealType
    }

    // ═══════════════════════════════════════════════════════════════
    // NEW: Adjustment Sheet State
    // ═══════════════════════════════════════════════════════════════
    var showAdjustmentSheet by remember { mutableStateOf(false) }
    var adjustedAnalysisState by remember { mutableStateOf<MealAdjustmentState?>(null) }

    // Track save state
    var saveState by remember { mutableStateOf(SaveState.IDLE) }

    // Quick Add toggle - user can mark meal as frequent for quick tracking
    var saveAsQuickAdd by remember { mutableStateOf(false) }

    // Only navigate when save is CONFIRMED successful
    LaunchedEffect(analysisResult, isLoading, errorMessage, saveState) {
        Log.d("MealAnalysisResult", "🔍 LaunchedEffect check:")
        Log.d("MealAnalysisResult", "   saveState=$saveState")
        Log.d("MealAnalysisResult", "   analysisResult=${if (analysisResult != null) "present" else "null"}")
        Log.d("MealAnalysisResult", "   isLoading=$isLoading")
        Log.d("MealAnalysisResult", "   errorMessage=$errorMessage")

        if (saveState == SaveState.SAVING && !isLoading) {
            if (analysisResult == null && errorMessage == null) {
                Log.d("MealAnalysisResult", "✅ Save successful - navigating to NutritionScreen")
                saveState = SaveState.SUCCESS
                onSaveComplete()
            } else if (errorMessage != null) {
                Log.e("MealAnalysisResult", "❌ Save failed: $errorMessage")
                saveState = SaveState.FAILED
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Analysis", color = textWhite) },
                navigationIcon = {
                    IconButton(onClick = {
                        nutritionViewModel.clearAnalysisResult()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, "Close", tint = textWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1a1410)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            analysisResult?.let { result ->
                if (result.success) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Success indicator
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Analysis Complete!",
                                            color = textWhite,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            result.meal_name.ifEmpty { "Unknown meal" },
                                            color = textGray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        // AI Confidence
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "AI Confidence",
                                        color = textGray,
                                        fontSize = 14.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = orangeGlow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "${(result.ai_confidence * 100).toInt()}%",
                                            color = textWhite,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // ═══════════════════════════════════════════════════════════
                        // TOTAL NUTRITION (shows adjusted values if available)
                        // ═══════════════════════════════════════════════════════════
                        item {
                            val displayCalories = adjustedAnalysisState?.totalCalories ?: result.total.calories
                            val displayProtein = adjustedAnalysisState?.totalProtein ?: result.total.protein
                            val displayCarbs = adjustedAnalysisState?.totalCarbs ?: result.total.carbs
                            val displayFat = adjustedAnalysisState?.totalFat ?: result.total.fat

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (adjustedAnalysisState != null) {
                                        Color(0xFF1E3A1E) // Green tint when adjusted
                                    } else {
                                        cardBackground
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (adjustedAnalysisState != null) "Adjusted Nutrition" else "Total Nutrition",
                                            color = if (adjustedAnalysisState != null) Color(0xFF88CC88) else textWhite,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Show adjustment indicator
                                        if (adjustedAnalysisState != null) {
                                            val diff = adjustedAnalysisState!!.caloriesDiff
                                            if (diff != 0f) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (diff > 0) Color(0xFFFFAA00).copy(alpha = 0.2f)
                                                    else Color(0xFF88CC88).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = if (diff > 0) "+${diff.toInt()} kcal" else "${diff.toInt()} kcal",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontSize = 12.sp,
                                                        color = if (diff > 0) Color(0xFFFFAA00) else Color(0xFF88CC88),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        NutritionBadge(
                                            label = "Calories",
                                            value = "${displayCalories.toInt()}",
                                            color = orangePrimary
                                        )
                                        NutritionBadge(
                                            label = "Protein",
                                            value = "${displayProtein.toInt()}g",
                                            color = Color(0xFF4CAF50)
                                        )
                                        NutritionBadge(
                                            label = "Carbs",
                                            value = "${displayCarbs.toInt()}g",
                                            color = Color(0xFF2196F3)
                                        )
                                        NutritionBadge(
                                            label = "Fat",
                                            value = "${displayFat.toInt()}g",
                                            color = Color(0xFFFF9800)
                                        )
                                    }

                                    // Show modifiers if applied
                                    adjustedAnalysisState?.let { state ->
                                        if (state.oilLevel != CookingOilLevel.NONE ||
                                            state.sauceLevel != SauceLevel.NONE ||
                                            state.portionSize != PortionSize.MEDIUM
                                        ) {
                                            Spacer(Modifier.height(12.dp))
                                            HorizontalDivider(color = Color(0xFF333333))
                                            Spacer(Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (state.portionSize != PortionSize.MEDIUM) {
                                                    ModifierTag(
                                                        label = "Portion: ${state.portionSize.displayName}",
                                                        icon = Icons.Default.Scale
                                                    )
                                                }
                                                if (state.oilLevel != CookingOilLevel.NONE) {
                                                    ModifierTag(
                                                        label = "Oil: ${state.oilLevel.displayName}",
                                                        icon = Icons.Default.WaterDrop
                                                    )
                                                }
                                                if (state.sauceLevel != SauceLevel.NONE) {
                                                    ModifierTag(
                                                        label = "Sauce: ${state.sauceLevel.displayName}",
                                                        icon = Icons.Default.Opacity
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Items header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Detected Items",
                                    color = textWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${adjustedAnalysisState?.activeItems?.size ?: result.items.size} items",
                                    color = textGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Items list (show adjusted items if available)
                        val displayItems = adjustedAnalysisState?.activeItems?.map { it.originalItem }
                            ?: result.items

                        items(displayItems) { item ->
                            // Find adjusted values if available
                            val adjustedItem = adjustedAnalysisState?.activeItems?.find {
                                it.originalItem.name == item.name
                            }

                            ItemCard(
                                item = item,
                                adjustedCalories = adjustedItem?.adjustedCalories,
                                adjustedQuantity = adjustedItem?.adjustedQuantity
                            )
                        }

                        // Suggestions
                        result.suggestions?.let { suggestions ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "AI Suggestions",
                                                color = textWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                suggestions,
                                                color = textGray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Meal type selector
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Save as",
                                        color = textWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MealType.entries.toList().chunked(2).forEach { rowTypes ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowTypes.forEach { mealType ->
                                                    MealTypeButton(
                                                        mealType = mealType,
                                                        isSelected = selectedMealType == mealType,
                                                        onClick = { selectedMealType = mealType },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ═══════════════════════════════════════════════════════════
                        // QUICK ADD TOGGLE - Save for fast re-tracking
                        // ═══════════════════════════════════════════════════════════
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (saveAsQuickAdd) {
                                        orangePrimary.copy(alpha = 0.15f)
                                    } else {
                                        cardBackground
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (saveAsQuickAdd) {
                                    androidx.compose.foundation.BorderStroke(1.dp, orangePrimary.copy(alpha = 0.5f))
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = if (saveAsQuickAdd) orangeGlow else textGray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                "Save as Quick Add",
                                                color = if (saveAsQuickAdd) orangeGlow else textWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "Track this meal again with one tap",
                                                color = textGray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = saveAsQuickAdd,
                                        onCheckedChange = { saveAsQuickAdd = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = orangePrimary,
                                            uncheckedThumbColor = textGray,
                                            uncheckedTrackColor = Color(0xFF333333)
                                        )
                                    )
                                }
                            }
                        }

                        // ═══════════════════════════════════════════════════════════
                        // ACTION BUTTONS: Adjust & Save
                        // ═══════════════════════════════════════════════════════════
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // ADJUST BUTTON (opens MealAdjustmentSheet)
                                OutlinedButton(
                                    onClick = { showAdjustmentSheet = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = orangePrimary
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(orangePrimary)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (adjustedAnalysisState != null) "Re-Adjust Portions & Modifiers"
                                        else "Adjust Portions & Modifiers",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // SAVE BUTTON
                                Button(
                                    onClick = {
                                        saveState = SaveState.SAVING
                                        nutritionViewModel.clearError()

                                        val authUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
                                        val effectiveUserId = authUserId ?: userProfile?.id ?: userId

                                        Log.d("MealAnalysis", "🔐 Saving meal with userId: $effectiveUserId")
                                        Log.d("MealAnalysis", "   - Adjusted: ${adjustedAnalysisState != null}")
                                        Log.d("MealAnalysis", "   - Save as Quick Add: $saveAsQuickAdd")

                                        nutritionViewModel.saveAnalysisAsMeal(
                                            context = context,
                                            userId = effectiveUserId,
                                            mealType = selectedMealType,
                                            imageFile = analyzedImageFile,
                                            hasCoach = userProfile?.hasCoach ?: false,
                                            saveAsQuickAdd = saveAsQuickAdd
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading && saveState != SaveState.SAVING,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isLoading || saveState == SaveState.SAVING) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Saving...", fontSize = 18.sp)
                                    } else {
                                        Icon(Icons.Default.Save, null, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Save Meal",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // ═══════════════════════════════════════════════════════════
                    // MEAL ADJUSTMENT SHEET
                    // ═══════════════════════════════════════════════════════════
                    if (showAdjustmentSheet) {
                        MealAdjustmentSheet(
                            analysis = result,
                            mealType = selectedMealType,
                            onDismiss = { showAdjustmentSheet = false },
                            onSave = { adjustedItems, state ->
                                // Store in local state for UI display
                                adjustedAnalysisState = state

                                // Store in ViewModel for saving
                                nutritionViewModel.setAdjustmentState(state)

                                showAdjustmentSheet = false

                                Log.d("MealAnalysis", "📝 Adjustments applied:")
                                Log.d("MealAnalysis", "   - Portion: ${state.portionSize.displayName}")
                                Log.d("MealAnalysis", "   - Oil: ${state.oilLevel.displayName}")
                                Log.d("MealAnalysis", "   - Sauce: ${state.sauceLevel.displayName}")
                                Log.d("MealAnalysis", "   - Active items: ${adjustedItems.size}")
                                Log.d("MealAnalysis", "   - Total kcal: ${state.totalCalories}")
                            }
                        )
                    }

                } else {
                    // Error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Analysis Failed",
                            color = textWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            result.error ?: "Unknown error",
                            color = textGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Error snackbar
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color.Red,
                    action = {
                        TextButton(onClick = {
                            nutritionViewModel.clearError()
                            saveState = SaveState.IDLE
                        }) {
                            Text("Dismiss", color = Color.White)
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

// Save state enum
private enum class SaveState {
    IDLE, SAVING, SUCCESS, FAILED
}

@Composable
private fun NutritionBadge(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = textGray,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ModifierTag(
    label: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF333333)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textGray,
                modifier = Modifier.size(12.dp)
            )
            Text(
                label,
                color = textGray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ItemCard(
    item: AIAnalyzedItem,
    adjustedCalories: Float? = null,
    adjustedQuantity: Float? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        color = textWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Show adjusted quantity if different
                    val displayQuantity = if (adjustedQuantity != null && adjustedQuantity != item.quantity_value) {
                        "${adjustedQuantity.toInt()}${item.quantity_unit} (was ${item.quantity})"
                    } else {
                        item.quantity
                    }
                    Text(
                        displayQuantity,
                        color = if (adjustedQuantity != null) Color(0xFF88CC88) else textGray,
                        fontSize = 12.sp
                    )
                }

                // Show adjusted calories if different
                val displayCalories = adjustedCalories ?: item.calories
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${displayCalories.toInt()} cal",
                        color = orangeGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (adjustedCalories != null && adjustedCalories != item.calories) {
                        Text(
                            "was ${item.calories.toInt()}",
                            color = textGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroChip("P", "${item.protein.toInt()}g", Color(0xFF4CAF50))
                MacroChip("C", "${item.carbs.toInt()}g", Color(0xFF2196F3))
                MacroChip("F", "${item.fat.toInt()}g", Color(0xFFFF9800))
            }
        }
    }
}

@Composable
private fun MacroChip(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            Text(
                value,
                color = textWhite,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MealTypeButton(
    mealType: MealType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) orangePrimary.copy(alpha = 0.2f) else Color(0xFF2a2a2a).copy(alpha = 0.4f)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, orangePrimary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, textGray.copy(alpha = 0.3f))
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getMealTypeIcon(mealType),
                contentDescription = null,
                tint = if (isSelected) orangeGlow else textGray,
                modifier = Modifier.size(20.dp)
            )
            Text(
                mealType.displayName,
                color = if (isSelected) orangeGlow else textWhite,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun getMealTypeIcon(mealType: MealType): ImageVector {
    return when (mealType) {
        MealType.BREAKFAST -> Icons.Default.WbSunny
        MealType.LUNCH -> Icons.Default.Restaurant
        MealType.DINNER -> Icons.Default.DinnerDining
        MealType.SHAKE -> Icons.Default.LocalDrink
        MealType.SNACK -> Icons.Default.Cookie
    }
}