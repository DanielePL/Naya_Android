// app/src/main/java/com/example/myapplicationtest/viewmodels/NutritionViewModel.kt

package com.example.menotracker.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.data.NutritionRepository
import com.example.menotracker.data.PreWorkoutNutritionService
import com.example.menotracker.data.repository.HydrationRepository
import com.example.menotracker.data.models.*
import com.example.menotracker.screens.nutrition.AdjustableItem
import com.example.menotracker.screens.nutrition.MealAdjustmentState
import com.example.menotracker.screens.nutrition.CookingOilLevel
import com.example.menotracker.screens.nutrition.SauceLevel
import com.example.menotracker.screens.nutrition.PortionSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import java.io.File
import java.util.UUID

// ═══════════════════════════════════════════════════════════════
// NUTRITION VIEW MODEL
// Manages all nutrition tracking state
// ═══════════════════════════════════════════════════════════════

class NutritionViewModel : ViewModel() {
    private val TAG = "NutritionViewModel"

    // Current nutrition log (for selected date)
    private val _nutritionLog = MutableStateFlow<NutritionLog?>(null)
    val nutritionLog: StateFlow<NutritionLog?> = _nutritionLog.asStateFlow()

    // Active nutrition goal
    private val _nutritionGoal = MutableStateFlow<NutritionGoal?>(null)
    val nutritionGoal: StateFlow<NutritionGoal?> = _nutritionGoal.asStateFlow()

    // AI Analysis state
    private val _aiAnalysisResult = MutableStateFlow<AIPhotoAnalysisResponse?>(null)
    val aiAnalysisResult: StateFlow<AIPhotoAnalysisResponse?> = _aiAnalysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Store imageFile for later upload when saving
    private val _analyzedImageFile = MutableStateFlow<File?>(null)
    val analyzedImageFile: StateFlow<File?> = _analyzedImageFile.asStateFlow()

    // Selected meal type (persists through photo capture → analysis result flow)
    private val _selectedMealType = MutableStateFlow(MealType.LUNCH)
    val selectedMealType: StateFlow<MealType> = _selectedMealType.asStateFlow()

    // Selected date (for viewing past days)
    private val _selectedDate = MutableStateFlow(getCurrentDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // STREAK DATA
    // ═══════════════════════════════════════════════════════════════

    private val _nutritionStreak = MutableStateFlow(NutritionStreak())
    val nutritionStreak: StateFlow<NutritionStreak> = _nutritionStreak.asStateFlow()

    // Historical nutrition logs (for insights)
    private val _nutritionLogs = MutableStateFlow<List<NutritionLog>>(emptyList())
    val nutritionLogs: StateFlow<List<NutritionLog>> = _nutritionLogs.asStateFlow()

    // Cache for logged dates (to avoid re-fetching)
    private var cachedUserId: String? = null

    // ═══════════════════════════════════════════════════════════════
    // MEAL ADJUSTMENT STATE (for persisting adjustments)
    // ═══════════════════════════════════════════════════════════════

    private val _currentAdjustmentState = MutableStateFlow<MealAdjustmentState?>(null)
    val currentAdjustmentState: StateFlow<MealAdjustmentState?> = _currentAdjustmentState.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════

    init {
        Log.d(TAG, "🍽️ NutritionViewModel initialized")
    }

    // ═══════════════════════════════════════════════════════════════
    // DATA LOADING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Load nutrition data for TODAY (convenience method)
     */
    fun loadTodayData(userId: String) {
        loadDataForDate(userId, getCurrentDate())
    }

    /**
     * Load nutrition data for a SPECIFIC DATE
     */
    fun loadDataForDate(userId: String, date: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _selectedDate.value = date
                Log.d(TAG, "📊 Loading nutrition data for date: $date, user: $userId")

                // Load nutrition goal (only once per user)
                if (_nutritionGoal.value == null || cachedUserId != userId) {
                    val goalResult = NutritionRepository.getActiveNutritionGoal(userId)
                    if (goalResult.isSuccess) {
                        _nutritionGoal.value = goalResult.getOrNull()
                        Log.d(TAG, "✅ Nutrition goal loaded: ${_nutritionGoal.value?.goalType}")
                    }
                    cachedUserId = userId
                }

                // Load log for the specified date
                val logResult = NutritionRepository.getNutritionLog(userId, date)
                if (logResult.isSuccess) {
                    _nutritionLog.value = logResult.getOrNull()
                    Log.d(TAG, "✅ Log loaded for $date: ${_nutritionLog.value?.meals?.size ?: 0} meals")
                } else {
                    // For past dates without data, show empty state (don't create new log)
                    if (date != getCurrentDate()) {
                        _nutritionLog.value = null
                        Log.d(TAG, "ℹ️ No data for $date")
                    } else {
                        _errorMessage.value = "Failed to load nutrition data"
                        Log.e(TAG, "❌ Failed to load nutrition log")
                    }
                }

                // Load streak data (only if not already loaded for this user)
                if (cachedUserId == userId) {
                    loadStreakData(userId)
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error loading data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load streak and weekly stats
     */
    private suspend fun loadStreakData(userId: String) {
        try {
            Log.d(TAG, "🔥 Loading streak data...")

            // Get logs for the past 30 days to calculate streak
            val today = getCurrentDate()
            val thirtyDaysAgo = kotlinx.datetime.Clock.System.now()
                .minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                .toString()
                .substringBefore('T')

            val logsResult = NutritionRepository.getNutritionLogs(userId, thirtyDaysAgo, today)

            if (logsResult.isSuccess) {
                val logs = logsResult.getOrNull() ?: emptyList()

                // Store logs for insights screen
                _nutritionLogs.value = logs

                // Get dates that have at least one meal logged
                val loggedDates = logs
                    .filter { it.meals.isNotEmpty() }
                    .map { it.date }
                    .toSet()

                // Calculate current streak
                val currentStreak = calculateCurrentStreak(loggedDates)

                // Calculate longest streak from available data
                val longestStreak = calculateLongestStreak(loggedDates)

                // Calculate this week's stats
                val weekStart = getWeekStartDate()
                val thisWeekLogs = logs.filter { it.date >= weekStart && it.meals.isNotEmpty() }
                val thisWeekDays = thisWeekLogs.size
                val thisWeekAvgCalories = if (thisWeekLogs.isNotEmpty()) {
                    thisWeekLogs.map { it.totalCalories }.average().toFloat()
                } else 0f
                val thisWeekAvgProtein = if (thisWeekLogs.isNotEmpty()) {
                    thisWeekLogs.map { it.totalProtein }.average().toFloat()
                } else 0f

                _nutritionStreak.value = NutritionStreak(
                    currentStreak = currentStreak,
                    longestStreak = maxOf(longestStreak, currentStreak),
                    thisWeekDays = thisWeekDays,
                    thisWeekAvgCalories = thisWeekAvgCalories,
                    thisWeekAvgProtein = thisWeekAvgProtein,
                    loggedDates = loggedDates
                )

                Log.d(TAG, "✅ Streak loaded: $currentStreak days, this week: $thisWeekDays/7")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading streak data", e)
        }
    }

    /**
     * Calculate current streak (consecutive days with logged meals)
     */
    private fun calculateCurrentStreak(loggedDates: Set<String>): Int {
        if (loggedDates.isEmpty()) return 0

        var streak = 0
        var checkDate = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        // Check if today has meals - if not, start from yesterday
        val todayStr = checkDate.toString()
        if (!loggedDates.contains(todayStr)) {
            checkDate = checkDate.minus(1, DateTimeUnit.DAY)
        }

        // Count consecutive days backwards
        while (loggedDates.contains(checkDate.toString())) {
            streak++
            checkDate = checkDate.minus(1, DateTimeUnit.DAY)
        }

        return streak
    }

    /**
     * Calculate longest streak from the available logged dates
     * Finds the longest sequence of consecutive days
     */
    private fun calculateLongestStreak(loggedDates: Set<String>): Int {
        if (loggedDates.isEmpty()) return 0

        // Parse and sort dates
        val sortedDates = loggedDates
            .mapNotNull { dateStr ->
                try {
                    kotlinx.datetime.LocalDate.parse(dateStr)
                } catch (e: Exception) {
                    null
                }
            }
            .sorted()

        if (sortedDates.isEmpty()) return 0

        var longestStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            val prevDate = sortedDates[i - 1]
            val currDate = sortedDates[i]

            // Check if dates are consecutive (difference of 1 day)
            val daysDiff = currDate.toEpochDays() - prevDate.toEpochDays()

            if (daysDiff == 1) {
                // Consecutive day
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                // Gap in dates, reset current streak
                currentStreak = 1
            }
        }

        return longestStreak
    }

    /**
     * Get the start date of the current week (Monday)
     */
    private fun getWeekStartDate(): String {
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val dayOfWeek = today.dayOfWeek.ordinal // Monday = 0
        val weekStart = today.minus(dayOfWeek, DateTimeUnit.DAY)
        return weekStart.toString()
    }

    // ═══════════════════════════════════════════════════════════════
    // AI PHOTO ANALYSIS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Analyze meal photo with AI
     */
    fun analyzeMealPhoto(
        imageFile: File,
        mealType: MealType? = null,
        additionalContext: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isAnalyzing.value = true
                _errorMessage.value = null
                _aiAnalysisResult.value = null  // Clear previous result
                _currentAdjustmentState.value = null // Clear any previous adjustments
                Log.d(TAG, "📸 Starting meal photo analysis...")
                Log.d(TAG, "📁 Image file: ${imageFile.absolutePath}, exists: ${imageFile.exists()}, size: ${imageFile.length()}")

                val result = NutritionRepository.analyzeMealPhoto(
                    imageFile = imageFile,
                    mealType = mealType,
                    additionalContext = additionalContext
                )

                Log.d(TAG, "🔍 Analysis result isSuccess: ${result.isSuccess}")

                if (result.isSuccess) {
                    val analysis = result.getOrNull()
                    Log.d(TAG, "🔍 Analysis object: $analysis")
                    Log.d(TAG, "🔍 Analysis.success: ${analysis?.success}")

                    if (analysis != null && analysis.success) {
                        // Only set result if analysis actually succeeded
                        _aiAnalysisResult.value = analysis
                        _analyzedImageFile.value = imageFile
                        Log.d(TAG, "✅ Analysis complete: ${analysis.meal_name}")
                    } else {
                        // Analysis returned but with success=false (AI error)
                        _aiAnalysisResult.value = null
                        _errorMessage.value = analysis?.error ?: "Analysis failed"
                        Log.e(TAG, "❌ Analysis returned error: ${analysis?.error}")
                    }
                } else {
                    _aiAnalysisResult.value = null
                    _errorMessage.value = "Failed to analyze photo: ${result.exceptionOrNull()?.message}"
                    Log.e(TAG, "❌ Analysis failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _aiAnalysisResult.value = null
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error analyzing photo", e)
                e.printStackTrace()
            } finally {
                _isAnalyzing.value = false
                Log.d(TAG, "🏁 Analysis finished. aiAnalysisResult is null: ${_aiAnalysisResult.value == null}, errorMessage: ${_errorMessage.value}")
            }
        }
    }

    /**
     * Save AI analysis result as meal (ORIGINAL - no adjustments)
     */
    fun saveAnalysisAsMeal(
        context: Context,
        userId: String,
        mealType: MealType,
        imageFile: File?,
        hasCoach: Boolean,
        saveAsQuickAdd: Boolean = false
    ) {
        // If we have adjustments, use those instead
        val adjustmentState = _currentAdjustmentState.value
        if (adjustmentState != null) {
            saveAdjustedMealFromAnalysis(
                context = context,
                userId = userId,
                mealType = mealType,
                imageFile = imageFile,
                hasCoach = hasCoach,
                adjustmentState = adjustmentState,
                saveAsQuickAdd = saveAsQuickAdd
            )
            return
        }

        val analysis = _aiAnalysisResult.value
        if (analysis == null) {
            _errorMessage.value = "No analysis to save"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                Log.d(TAG, "💾 Saving meal from analysis (no adjustments)...")

                // 1. Save photo (Supabase if hasCoach, local if not)
                var photoUrl: String? = null
                if (imageFile != null) {
                    Log.d(TAG, if (hasCoach) "☁️ Uploading to Supabase..." else "📱 Saving locally...")

                    val photoResult = NutritionRepository.saveMealPhoto(
                        context = context,
                        imageFile = imageFile,
                        userId = userId,
                        hasCoach = hasCoach
                    )

                    if (photoResult.isSuccess) {
                        photoUrl = photoResult.getOrNull()
                        Log.d(TAG, "✅ Photo saved: $photoUrl")
                    } else {
                        Log.e(TAG, "⚠️ Photo save failed, continuing without photo")
                    }
                }

                // 2. Save meal to database (always save to TODAY)
                val date = getCurrentDate()
                val result = NutritionRepository.saveMealFromAIAnalysis(
                    userId = userId,
                    date = date,
                    mealType = mealType,
                    analysis = analysis,
                    photoUrl = photoUrl
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Meal saved successfully")

                    // Record meal time for smart pre-workout nutrition alerts
                    PreWorkoutNutritionService.getInstance(context).recordMealTime()
                    Log.d(TAG, "🍽️ Meal time recorded for workout nutrition timing")

                    // Save as Quick Add if requested
                    if (saveAsQuickAdd) {
                        saveAsQuickAddFavorite(
                            userId = userId,
                            mealName = analysis.meal_name,
                            mealType = mealType,
                            items = analysis.items,
                            totalCalories = analysis.total.calories,
                            totalProtein = analysis.total.protein,
                            totalCarbs = analysis.total.carbs,
                            totalFat = analysis.total.fat
                        )
                    }

                    // Reload today's data
                    loadTodayData(userId)

                    // Reload frequent meals if we added one
                    if (saveAsQuickAdd) {
                        loadFrequentMeals(userId)
                    }

                    // Clear analysis result
                    _aiAnalysisResult.value = null
                    _analyzedImageFile.value = null
                    _currentAdjustmentState.value = null
                } else {
                    _errorMessage.value = "Failed to save meal: ${result.exceptionOrNull()?.message}"
                    Log.e(TAG, "❌ Failed to save meal", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error saving meal", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADJUSTED MEAL SAVING (NEW)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Store the adjustment state from MealAdjustmentSheet
     */
    fun setAdjustmentState(state: MealAdjustmentState?) {
        _currentAdjustmentState.value = state
        Log.d(TAG, "📝 Adjustment state updated: ${state != null}")
        if (state != null) {
            Log.d(TAG, "   - Portion: ${state.portionSize.displayName}")
            Log.d(TAG, "   - Oil: ${state.oilLevel.displayName}")
            Log.d(TAG, "   - Sauce: ${state.sauceLevel.displayName}")
            Log.d(TAG, "   - Active items: ${state.activeItems.size}")
            Log.d(TAG, "   - Total kcal: ${state.totalCalories}")
        }
    }

    /**
     * Save meal WITH adjustments (portion size, oil, sauce, item modifications)
     */
    private fun saveAdjustedMealFromAnalysis(
        context: Context,
        userId: String,
        mealType: MealType,
        imageFile: File?,
        hasCoach: Boolean,
        adjustmentState: MealAdjustmentState,
        saveAsQuickAdd: Boolean = false
    ) {
        val analysis = _aiAnalysisResult.value
        if (analysis == null) {
            _errorMessage.value = "No analysis to save"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                Log.d(TAG, "💾 Saving ADJUSTED meal from analysis...")
                Log.d(TAG, "📊 Adjustments applied:")
                Log.d(TAG, "   - Portion: ${adjustmentState.portionSize.displayName} (${adjustmentState.portionSize.multiplier}x)")
                Log.d(TAG, "   - Oil: ${adjustmentState.oilLevel.displayName} (+${adjustmentState.oilLevel.caloriesAdd} kcal)")
                Log.d(TAG, "   - Sauce: ${adjustmentState.sauceLevel.displayName} (+${adjustmentState.sauceLevel.caloriesAdd} kcal)")
                Log.d(TAG, "   - Items: ${adjustmentState.activeItems.size} (${adjustmentState.items.size - adjustmentState.activeItems.size} removed)")

                // 1. Save photo
                var photoUrl: String? = null
                if (imageFile != null) {
                    Log.d(TAG, if (hasCoach) "☁️ Uploading to Supabase..." else "📱 Saving locally...")

                    val photoResult = NutritionRepository.saveMealPhoto(
                        context = context,
                        imageFile = imageFile,
                        userId = userId,
                        hasCoach = hasCoach
                    )

                    if (photoResult.isSuccess) {
                        photoUrl = photoResult.getOrNull()
                        Log.d(TAG, "✅ Photo saved: $photoUrl")
                    } else {
                        Log.e(TAG, "⚠️ Photo save failed, continuing without photo")
                    }
                }

                // 2. Create adjusted AI response for saving
                val adjustedAnalysis = createAdjustedAnalysis(analysis, adjustmentState)

                // 3. Save meal to database
                val date = getCurrentDate()
                val result = NutritionRepository.saveMealFromAIAnalysis(
                    userId = userId,
                    date = date,
                    mealType = mealType,
                    analysis = adjustedAnalysis,
                    photoUrl = photoUrl
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Adjusted meal saved successfully")
                    Log.d(TAG, "   - Original kcal: ${analysis.total.calories}")
                    Log.d(TAG, "   - Adjusted kcal: ${adjustedAnalysis.total.calories}")

                    // Record meal time for smart pre-workout nutrition alerts
                    PreWorkoutNutritionService.getInstance(context).recordMealTime()
                    Log.d(TAG, "🍽️ Meal time recorded for workout nutrition timing")

                    // Save as Quick Add if requested
                    if (saveAsQuickAdd) {
                        saveAsQuickAddFavorite(
                            userId = userId,
                            mealName = adjustedAnalysis.meal_name,
                            mealType = mealType,
                            items = adjustedAnalysis.items,
                            totalCalories = adjustedAnalysis.total.calories,
                            totalProtein = adjustedAnalysis.total.protein,
                            totalCarbs = adjustedAnalysis.total.carbs,
                            totalFat = adjustedAnalysis.total.fat
                        )
                    }

                    // Reload today's data
                    loadTodayData(userId)

                    // Reload frequent meals if we added one
                    if (saveAsQuickAdd) {
                        loadFrequentMeals(userId)
                    }

                    // Clear all states
                    _aiAnalysisResult.value = null
                    _analyzedImageFile.value = null
                    _currentAdjustmentState.value = null
                } else {
                    _errorMessage.value = "Failed to save meal: ${result.exceptionOrNull()?.message}"
                    Log.e(TAG, "❌ Failed to save meal", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error saving adjusted meal", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create an adjusted AIPhotoAnalysisResponse from the adjustment state
     */
    private fun createAdjustedAnalysis(
        original: AIPhotoAnalysisResponse,
        state: MealAdjustmentState
    ): AIPhotoAnalysisResponse {
        // Convert active AdjustableItems back to AIAnalyzedItems with adjusted values
        val adjustedItems = state.activeItems.map { adjustable ->
            adjustable.originalItem.copy(
                // Apply portion multiplier to quantity
                quantity_value = adjustable.adjustedQuantity * state.portionSize.multiplier,
                quantity = "${(adjustable.adjustedQuantity * state.portionSize.multiplier).toInt()}${adjustable.originalItem.quantity_unit}",
                // Apply portion multiplier to all macros
                calories = adjustable.adjustedCalories * state.portionSize.multiplier,
                protein = adjustable.adjustedProtein * state.portionSize.multiplier,
                carbs = adjustable.adjustedCarbs * state.portionSize.multiplier,
                fat = adjustable.adjustedFat * state.portionSize.multiplier,
                fiber = adjustable.adjustedFiber * state.portionSize.multiplier,
                sugar = adjustable.adjustedSugar * state.portionSize.multiplier,
                sodium = adjustable.adjustedSodium * state.portionSize.multiplier
            )
        }

        // Add modifier items if oil or sauce was added
        val modifierItems = mutableListOf<AIAnalyzedItem>()

        if (state.oilLevel != CookingOilLevel.NONE) {
            modifierItems.add(
                AIAnalyzedItem(
                    name = "Cooking Oil (${state.oilLevel.displayName})",
                    quantity = state.oilLevel.displayName,
                    quantity_value = 1f,
                    quantity_unit = "serving",
                    calories = state.oilLevel.caloriesAdd,
                    protein = 0f,
                    carbs = 0f,
                    fat = state.oilLevel.fatAdd,
                    confidence = 1.0f
                )
            )
        }

        if (state.sauceLevel != SauceLevel.NONE) {
            modifierItems.add(
                AIAnalyzedItem(
                    name = "Sauce/Dressing (${state.sauceLevel.displayName})",
                    quantity = state.sauceLevel.displayName,
                    quantity_value = 1f,
                    quantity_unit = "serving",
                    calories = state.sauceLevel.caloriesAdd,
                    protein = 0f,
                    carbs = state.sauceLevel.carbsAdd,
                    fat = state.sauceLevel.fatAdd,
                    confidence = 1.0f
                )
            )
        }

        // Combine adjusted food items + modifier items
        val allItems = adjustedItems + modifierItems

        // Build adjustment notes for meal name
        val adjustmentNotes = buildList {
            if (state.portionSize != PortionSize.MEDIUM) {
                add("${state.portionSize.displayName} portion")
            }
            if (state.oilLevel != CookingOilLevel.NONE) {
                add("+oil")
            }
            if (state.sauceLevel != SauceLevel.NONE) {
                add("+sauce")
            }
        }

        val adjustedMealName = if (adjustmentNotes.isNotEmpty()) {
            "${original.meal_name} (${adjustmentNotes.joinToString(", ")})"
        } else {
            original.meal_name
        }

        return original.copy(
            meal_name = adjustedMealName,
            items = allItems,
            total = AIAnalyzedTotals(
                calories = state.totalCalories,
                protein = state.totalProtein,
                carbs = state.totalCarbs,
                fat = state.totalFat
            ),
            suggestions = original.suggestions?.let {
                "$it\n[Adjusted by user]"
            } ?: "[Adjusted by user]"
        )
    }

    /**
     * Clear AI analysis result
     */
    fun clearAnalysisResult() {
        _aiAnalysisResult.value = null
        _analyzedImageFile.value = null
        _currentAdjustmentState.value = null
    }

    // ═══════════════════════════════════════════════════════════════
    // MEAL OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Delete meal
     */
    fun deleteMeal(userId: String, mealId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Deleting meal: $mealId")

                val result = NutritionRepository.deleteMeal(mealId)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Meal deleted")

                    // Reload data for current selected date
                    loadDataForDate(userId, _selectedDate.value)
                } else {
                    _errorMessage.value = "Failed to delete meal"
                    Log.e(TAG, "❌ Failed to delete meal", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error deleting meal", e)
            }
        }
    }

    /**
     * Update meal and its items
     */
    fun updateMeal(
        userId: String,
        meal: Meal,
        items: List<MealItem>,
        deletedItemIds: List<String>
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "📝 Updating meal: ${meal.id}")
                Log.d(TAG, "📝 Items to update: ${items.size}")
                Log.d(TAG, "📝 Items to delete: ${deletedItemIds.size}")

                val result = NutritionRepository.updateMeal(
                    meal = meal,
                    items = items,
                    deletedItemIds = deletedItemIds
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Meal updated successfully")
                    // Reload data to reflect changes
                    loadDataForDate(userId, _selectedDate.value)
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update meal"
                    Log.e(TAG, "❌ Failed to update meal", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error updating meal", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NUTRITION GOALS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Save nutrition goal
     */
    fun saveNutritionGoal(goal: NutritionGoal) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "💾 Saving nutrition goal...")

                val result = NutritionRepository.saveNutritionGoal(goal)
                if (result.isSuccess) {
                    _nutritionGoal.value = result.getOrNull()
                    Log.d(TAG, "✅ Goal saved successfully")
                } else {
                    _errorMessage.value = "Failed to save goal"
                    Log.e(TAG, "❌ Failed to save goal", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error saving goal", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // QUICK ADD (Frequent Meals Feature)
    // ═══════════════════════════════════════════════════════════════

    // Frequent meals list
    private val _frequentMeals = MutableStateFlow<List<FrequentMeal>>(emptyList())
    val frequentMeals: StateFlow<List<FrequentMeal>> = _frequentMeals.asStateFlow()

    // Favorite frequent meals
    private val _favoriteFrequentMeals = MutableStateFlow<List<FrequentMeal>>(emptyList())
    val favoriteFrequentMeals: StateFlow<List<FrequentMeal>> = _favoriteFrequentMeals.asStateFlow()

    // Selected frequent meal for add-ons
    private val _selectedFrequentMeal = MutableStateFlow<FrequentMeal?>(null)
    val selectedFrequentMeal: StateFlow<FrequentMeal?> = _selectedFrequentMeal.asStateFlow()

    // Available add-ons for selected meal
    private val _frequentAddOns = MutableStateFlow<List<FrequentAddOn>>(emptyList())
    val frequentAddOns: StateFlow<List<FrequentAddOn>> = _frequentAddOns.asStateFlow()

    // Selected add-ons to include
    private val _selectedAddOns = MutableStateFlow<List<FrequentMealItem>>(emptyList())
    val selectedAddOns: StateFlow<List<FrequentMealItem>> = _selectedAddOns.asStateFlow()

    // Quick add sheet visibility
    private val _showQuickAddSheet = MutableStateFlow(false)
    val showQuickAddSheet: StateFlow<Boolean> = _showQuickAddSheet.asStateFlow()

    // Quick add loading state
    private val _isQuickAdding = MutableStateFlow(false)
    val isQuickAdding: StateFlow<Boolean> = _isQuickAdding.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // ANABOLIC WINDOW (Post-Workout Nutrition Timing)
    // ═══════════════════════════════════════════════════════════════

    private val _anabolicWindow = MutableStateFlow<AnabolicWindow?>(null)
    val anabolicWindow: StateFlow<AnabolicWindow?> = _anabolicWindow.asStateFlow()

    private val _preWorkoutState = MutableStateFlow<PreWorkoutNutritionState?>(null)
    val preWorkoutState: StateFlow<PreWorkoutNutritionState?> = _preWorkoutState.asStateFlow()

    private val _postWorkoutState = MutableStateFlow<PostWorkoutNutritionState?>(null)
    val postWorkoutState: StateFlow<PostWorkoutNutritionState?> = _postWorkoutState.asStateFlow()

    /**
     * Initialize workout nutrition states from PreWorkoutNutritionService
     * Should be called when entering NutritionScreen
     */
    fun initializeWorkoutNutritionState(context: Context) {
        viewModelScope.launch {
            try {
                val service = PreWorkoutNutritionService.getInstance(context)

                // Collect pre-workout state
                launch {
                    service.preWorkoutState.collect { state ->
                        _preWorkoutState.value = state
                        Log.d(TAG, "🏋️ Pre-workout state updated: ${state?.recommendedAction}")
                    }
                }

                // Collect post-workout state and create AnabolicWindow
                launch {
                    service.postWorkoutState.collect { state ->
                        _postWorkoutState.value = state
                        Log.d(TAG, "🏋️ Post-workout state updated: ${state?.minutesSinceWorkout} min ago")

                        // Create AnabolicWindow from post-workout state if active
                        if (state != null && state.windowPhase != PostWorkoutPhase.CLOSED) {
                            val window = AnabolicWindow(
                                workoutEndTimeMillis = state.workoutEndTime.toEpochMilli(),
                                wasFastedWorkout = state.wasFasted,
                                proteinIntakeSince = state.proteinConsumedSince,
                                carbIntakeSince = state.carbsConsumedSince,
                                workoutType = null, // Could be enhanced later
                                workoutIntensity = 0.7f
                            )
                            _anabolicWindow.value = window
                            Log.d(TAG, "💪 Anabolic window active: ${window.getMinutesRemaining()} min remaining")
                        } else {
                            _anabolicWindow.value = null
                        }
                    }
                }

                Log.d(TAG, "✅ Workout nutrition states initialized")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initializing workout nutrition state", e)
            }
        }
    }

    /**
     * Manually trigger workout end (for testing or manual tracking)
     */
    fun notifyWorkoutEnded(context: Context, wasFasted: Boolean = false) {
        val service = PreWorkoutNutritionService.getInstance(context)
        val state = service.startPostWorkoutTracking(
            workoutEndTime = java.time.Instant.now(),
            wasFasted = wasFasted
        )
        _postWorkoutState.value = state
        Log.d(TAG, "🏁 Workout ended, post-workout tracking started")
    }

    /**
     * Load frequent meals for user
     */
    fun loadFrequentMeals(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "⚡ Loading frequent meals for user: $userId")

                // Load all frequent meals (sorted by usage)
                val result = NutritionRepository.getFrequentMeals(userId, limit = 15)
                if (result.isSuccess) {
                    val meals = result.getOrNull() ?: emptyList()
                    _frequentMeals.value = meals
                    Log.d(TAG, "✅ Loaded ${meals.size} frequent meals")
                    meals.forEach { Log.d(TAG, "  - ${it.name} (favorite=${it.isFavorite})") }
                } else {
                    Log.e(TAG, "❌ Failed to load frequent meals: ${result.exceptionOrNull()?.message}")
                }

                // Load favorites
                val favResult = NutritionRepository.getFavoriteFrequentMeals(userId)
                if (favResult.isSuccess) {
                    val favs = favResult.getOrNull() ?: emptyList()
                    _favoriteFrequentMeals.value = favs
                    Log.d(TAG, "✅ Loaded ${favs.size} favorites")
                    favs.forEach { Log.d(TAG, "  ⭐ ${it.name}") }
                } else {
                    Log.e(TAG, "❌ Failed to load favorites: ${favResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading frequent meals", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Show quick add sheet
     */
    fun showQuickAdd() {
        _showQuickAddSheet.value = true
        _selectedFrequentMeal.value = null
        _selectedAddOns.value = emptyList()
    }

    /**
     * Hide quick add sheet
     */
    fun hideQuickAdd() {
        _showQuickAddSheet.value = false
        _selectedFrequentMeal.value = null
        _selectedAddOns.value = emptyList()
        _frequentAddOns.value = emptyList()
    }

    /**
     * Select a frequent meal for potential add-ons
     */
    fun selectFrequentMeal(userId: String, meal: FrequentMeal) {
        _selectedFrequentMeal.value = meal
        _selectedAddOns.value = emptyList()

        // Load common add-ons for this meal
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = NutritionRepository.getFrequentAddOns(meal.name, userId)
                if (result.isSuccess) {
                    _frequentAddOns.value = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "✅ Loaded ${_frequentAddOns.value.size} add-ons for ${meal.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading add-ons", e)
            }
        }
    }

    /**
     * Toggle an add-on selection
     */
    fun toggleAddOn(addOn: FrequentAddOn) {
        val item = FrequentMealItem(
            name = addOn.addonName,
            quantity = addOn.addonQuantity,
            quantityUnit = addOn.addonUnit,
            calories = addOn.addonCalories,
            protein = addOn.addonProtein,
            carbs = addOn.addonCarbs,
            fat = addOn.addonFat
        )

        val current = _selectedAddOns.value.toMutableList()
        val existing = current.find { it.name == item.name }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(item)
        }
        _selectedAddOns.value = current
    }

    /**
     * Quick add selected frequent meal (with optional add-ons)
     */
    fun quickAddMeal(userId: String) {
        val meal = _selectedFrequentMeal.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isQuickAdding.value = true
                Log.d(TAG, "⚡ Quick-adding: ${meal.name}")

                val result = NutritionRepository.quickAddFrequentMeal(
                    userId = userId,
                    frequentMeal = meal,
                    additionalItems = _selectedAddOns.value
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Quick add successful!")

                    // Reload today's data
                    loadTodayData(userId)

                    // Reload frequent meals (usage count updated)
                    loadFrequentMeals(userId)

                    // Close sheet
                    _showQuickAddSheet.value = false
                    _selectedFrequentMeal.value = null
                    _selectedAddOns.value = emptyList()
                } else {
                    _errorMessage.value = "Failed to quick add: ${result.exceptionOrNull()?.message}"
                    Log.e(TAG, "❌ Quick add failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error quick-adding meal", e)
            } finally {
                _isQuickAdding.value = false
            }
        }
    }

    /**
     * Add a single meal item from FoodSearchScreen to today's log
     */
    fun addMealItemToLog(userId: String, mealType: MealType, mealItem: MealItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "➕ Adding meal item: ${mealItem.itemName}")

                val result = NutritionRepository.addSingleFoodItem(
                    userId = userId,
                    mealType = mealType,
                    mealItem = mealItem
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Meal item added successfully!")
                    // Reload today's data
                    loadTodayData(userId)
                } else {
                    _errorMessage.value = "Failed to add: ${result.exceptionOrNull()?.message}"
                    Log.e(TAG, "❌ Add meal item failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error adding meal item", e)
            }
        }
    }

    /**
     * Direct quick add (no add-ons selection, just add immediately)
     */
    fun directQuickAdd(userId: String, meal: FrequentMeal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isQuickAdding.value = true
                Log.d(TAG, "⚡ Direct quick-add: ${meal.name}")

                val result = NutritionRepository.quickAddFrequentMeal(
                    userId = userId,
                    frequentMeal = meal,
                    additionalItems = emptyList()
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Direct quick add successful!")

                    // Reload today's data
                    loadTodayData(userId)

                    // Reload frequent meals (usage count updated)
                    loadFrequentMeals(userId)
                } else {
                    _errorMessage.value = "Failed to add: ${result.exceptionOrNull()?.message}"
                    Log.e(TAG, "❌ Direct quick add failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error direct quick-adding meal", e)
            } finally {
                _isQuickAdding.value = false
            }
        }
    }

    /**
     * Toggle favorite status for a frequent meal
     */
    fun toggleFrequentMealFavorite(userId: String, meal: FrequentMeal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newFavorite = !meal.isFavorite
                val result = NutritionRepository.toggleFrequentMealFavorite(meal.id, newFavorite)

                if (result.isSuccess) {
                    Log.d(TAG, "⭐ Favorite toggled: ${meal.name} = $newFavorite")

                    // Update local state
                    _frequentMeals.value = _frequentMeals.value.map {
                        if (it.id == meal.id) it.copy(isFavorite = newFavorite) else it
                    }

                    if (newFavorite) {
                        _favoriteFrequentMeals.value = _favoriteFrequentMeals.value + meal.copy(isFavorite = true)
                    } else {
                        _favoriteFrequentMeals.value = _favoriteFrequentMeals.value.filter { it.id != meal.id }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error toggling favorite", e)
            }
        }
    }

    /**
     * Toggle favorite status for a logged meal
     * If the meal doesn't exist as a FrequentMeal, creates one with isFavorite = true
     * If it exists, toggles its favorite status
     */
    fun toggleLoggedMealFavorite(userId: String, meal: Meal) {
        viewModelScope.launch {
            try {
                val mealName = meal.mealName ?: "Meal"
                Log.d(TAG, "⭐ toggleLoggedMealFavorite called for: $mealName")
                Log.d(TAG, "⭐ Current frequentMeals count: ${_frequentMeals.value.size}")
                Log.d(TAG, "⭐ Current favoriteFrequentMeals count: ${_favoriteFrequentMeals.value.size}")
                Log.d(TAG, "⭐ Looking for match with mealName: '$mealName'")

                // Check if this meal already exists as a FrequentMeal
                val existingFrequent = _frequentMeals.value.find {
                    Log.d(TAG, "⭐ Comparing '${it.name}' with '$mealName'")
                    it.name == mealName
                } ?: _favoriteFrequentMeals.value.find { it.name == mealName }

                if (existingFrequent != null) {
                    Log.d(TAG, "⭐ Found existing FrequentMeal: ${existingFrequent.name}, isFavorite=${existingFrequent.isFavorite}")

                    // Toggle existing FrequentMeal's favorite status
                    val newFavorite = !existingFrequent.isFavorite

                    withContext(Dispatchers.IO) {
                        NutritionRepository.toggleFrequentMealFavorite(existingFrequent.id, newFavorite)
                    }

                    Log.d(TAG, "⭐ Toggled to: $newFavorite, now updating local state")

                    // Update local state on main thread
                    _frequentMeals.value = _frequentMeals.value.map {
                        if (it.id == existingFrequent.id) it.copy(isFavorite = newFavorite) else it
                    }

                    if (newFavorite) {
                        _favoriteFrequentMeals.value = _favoriteFrequentMeals.value + existingFrequent.copy(isFavorite = true)
                    } else {
                        _favoriteFrequentMeals.value = _favoriteFrequentMeals.value.filter { it.id != existingFrequent.id }
                    }

                    Log.d(TAG, "⭐ Updated favoriteFrequentMeals count: ${_favoriteFrequentMeals.value.size}")
                } else {
                    // Create new FrequentMeal from logged meal with isFavorite = true
                    Log.d(TAG, "⭐ No existing FrequentMeal found, creating new favorite: $mealName")

                    val frequentItems = meal.items.map { item ->
                        FrequentMealItem(
                            name = item.itemName,
                            quantity = item.quantity,
                            quantityUnit = item.quantityUnit,
                            calories = item.calories,
                            protein = item.protein,
                            carbs = item.carbs,
                            fat = item.fat,
                            fiber = item.fiber,
                            sugar = item.sugar,
                            sodium = item.sodium
                        )
                    }

                    val result = withContext(Dispatchers.IO) {
                        NutritionRepository.upsertFrequentMeal(
                            userId = userId,
                            mealName = mealName,
                            mealType = meal.mealType,
                            items = frequentItems,
                            totalCalories = meal.totalCalories,
                            totalProtein = meal.totalProtein,
                            totalCarbs = meal.totalCarbs,
                            totalFat = meal.totalFat,
                            isFavorite = true
                        )
                    }

                    if (result.isSuccess) {
                        val newMealId = result.getOrNull() ?: ""
                        Log.d(TAG, "✅ Created new favorite with id: $newMealId")

                        // Immediately add to local state so UI updates
                        val newFrequentMeal = FrequentMeal(
                            id = newMealId,
                            userId = userId,
                            name = mealName,
                            mealType = meal.mealType.name.lowercase(),
                            items = frequentItems,
                            totalCalories = meal.totalCalories,
                            totalProtein = meal.totalProtein,
                            totalCarbs = meal.totalCarbs,
                            totalFat = meal.totalFat,
                            usageCount = 1,
                            isFavorite = true
                        )

                        _frequentMeals.value = _frequentMeals.value + newFrequentMeal
                        _favoriteFrequentMeals.value = _favoriteFrequentMeals.value + newFrequentMeal

                        Log.d(TAG, "⭐ Added to local state. Favorites count: ${_favoriteFrequentMeals.value.size}")
                    } else {
                        Log.e(TAG, "❌ Failed to create favorite: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error toggling logged meal favorite", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete a frequent meal
     */
    fun deleteFrequentMeal(mealId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = NutritionRepository.deleteFrequentMeal(mealId)
                if (result.isSuccess) {
                    Log.d(TAG, "🗑️ Frequent meal deleted")
                    _frequentMeals.value = _frequentMeals.value.filter { it.id != mealId }
                    _favoriteFrequentMeals.value = _favoriteFrequentMeals.value.filter { it.id != mealId }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting frequent meal", e)
            }
        }
    }

    /**
     * Save meal as Quick Add favorite (user explicitly marked it)
     */
    private suspend fun saveAsQuickAddFavorite(
        userId: String,
        mealName: String,
        mealType: MealType,
        items: List<AIAnalyzedItem>,
        totalCalories: Float,
        totalProtein: Float,
        totalCarbs: Float,
        totalFat: Float
    ) {
        try {
            Log.d(TAG, "⚡ Saving as Quick Add favorite: $mealName")

            val frequentItems = items.map { item ->
                FrequentMealItem(
                    name = item.name,
                    quantity = item.quantity_value,
                    quantityUnit = item.quantity_unit,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    fiber = item.fiber,
                    sugar = item.sugar,
                    sodium = item.sodium
                )
            }

            NutritionRepository.upsertFrequentMeal(
                userId = userId,
                mealName = mealName,
                mealType = mealType,
                items = frequentItems,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                isFavorite = true  // Mark as favorite since user explicitly chose Quick Add
            )

            Log.d(TAG, "✅ Quick Add favorite saved")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error saving Quick Add favorite (non-critical)", e)
        }
    }

    /**
     * Auto-learn: Save current meal as frequent meal after saving via AI analysis
     * Called internally after saveMealFromAIAnalysis
     */
    private fun autoLearnFrequentMeal(
        userId: String,
        mealName: String,
        mealType: MealType,
        items: List<AIAnalyzedItem>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🧠 Auto-learning frequent meal: $mealName")

                val frequentItems = items.map { item ->
                    FrequentMealItem(
                        name = item.name,
                        quantity = item.quantity_value,
                        quantityUnit = item.quantity_unit,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        fiber = item.fiber,
                        sugar = item.sugar,
                        sodium = item.sodium
                    )
                }

                val totalCalories = items.sumOf { it.calories.toDouble() }.toFloat()
                val totalProtein = items.sumOf { it.protein.toDouble() }.toFloat()
                val totalCarbs = items.sumOf { it.carbs.toDouble() }.toFloat()
                val totalFat = items.sumOf { it.fat.toDouble() }.toFloat()

                NutritionRepository.upsertFrequentMeal(
                    userId = userId,
                    mealName = mealName,
                    mealType = mealType,
                    items = frequentItems,
                    totalCalories = totalCalories,
                    totalProtein = totalProtein,
                    totalCarbs = totalCarbs,
                    totalFat = totalFat
                )

                Log.d(TAG, "✅ Frequent meal learned")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error auto-learning frequent meal (non-critical)", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HYDRATION TRACKING
    // ═══════════════════════════════════════════════════════════════

    private val _hydrationLog = MutableStateFlow<HydrationRepository.HydrationLog?>(null)
    val hydrationLog: StateFlow<HydrationRepository.HydrationLog?> = _hydrationLog.asStateFlow()

    private val _isAddingWater = MutableStateFlow(false)
    val isAddingWater: StateFlow<Boolean> = _isAddingWater.asStateFlow()

    /**
     * Load today's hydration log
     */
    fun loadHydrationData(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "💧 Loading hydration data for user: $userId")
                val result = HydrationRepository.getTodayLog(userId)
                if (result.isSuccess) {
                    _hydrationLog.value = result.getOrNull()
                    Log.d(TAG, "✅ Hydration loaded: ${_hydrationLog.value?.waterIntakeMl}ml / ${_hydrationLog.value?.targetMl}ml")
                } else {
                    Log.e(TAG, "❌ Failed to load hydration: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading hydration data", e)
            }
        }
    }

    /**
     * Add water intake (in milliliters)
     */
    fun addWater(userId: String, amountMl: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isAddingWater.value = true
                Log.d(TAG, "💧 Adding ${amountMl}ml water")

                val result = HydrationRepository.addWater(userId, amountMl)
                if (result.isSuccess) {
                    _hydrationLog.value = result.getOrNull()
                    Log.d(TAG, "✅ Water added: ${_hydrationLog.value?.waterIntakeMl}ml total")
                } else {
                    _errorMessage.value = "Failed to add water"
                    Log.e(TAG, "❌ Failed to add water", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error adding water", e)
            } finally {
                _isAddingWater.value = false
            }
        }
    }

    /**
     * Set daily water target (in milliliters)
     */
    fun setWaterTarget(userId: String, targetMl: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🎯 Setting water target to ${targetMl}ml")
                val result = HydrationRepository.setDailyTarget(userId, targetMl)
                if (result.isSuccess) {
                    _hydrationLog.value = result.getOrNull()
                    Log.d(TAG, "✅ Water target set: ${_hydrationLog.value?.targetMl}ml")
                } else {
                    _errorMessage.value = "Failed to set target"
                    Log.e(TAG, "❌ Failed to set target", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Error setting target", e)
            }
        }
    }

    /**
     * Reset today's water intake
     */
    fun resetWaterIntake(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Resetting water intake")
                val result = HydrationRepository.resetToday(userId)
                if (result.isSuccess) {
                    _hydrationLog.value = result.getOrNull()
                    Log.d(TAG, "✅ Water intake reset")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error resetting water intake", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    fun clearError() {
        _errorMessage.value = null
    }

    fun setSelectedMealType(mealType: MealType) {
        _selectedMealType.value = mealType
    }

    private fun getCurrentDate(): String {
        return kotlinx.datetime.Clock.System.now()
            .toString()
            .substringBefore('T') // "2025-01-24"
    }
}