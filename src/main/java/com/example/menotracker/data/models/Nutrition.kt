// app/src/main/java/com/example/myapplicationtest/data/models/Nutrition.kt

package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════
// NUTRITION DATA MODELS
// ═══════════════════════════════════════════════════════════════

@Serializable
data class Food(
    val id: String,
    val userId: String? = null,
    val name: String,
    val brand: String? = null,
    val servingSize: Float,
    val servingUnit: String = "g",
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val category: String? = null,
    val isPublic: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class NutritionLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String, // "2025-01-24"
    @SerialName("target_calories") val targetCalories: Float? = null,
    @SerialName("target_protein") val targetProtein: Float? = null,
    @SerialName("target_carbs") val targetCarbs: Float? = null,
    @SerialName("target_fat") val targetFat: Float? = null,
    @SerialName("workout_session_id") val workoutSessionId: String? = null,
    val notes: String? = null,
    val meals: List<Meal> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
) {
    // ═══════════════════════════════════════════════════════════
    // CORE MACROS DAILY TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalCalories: Float get() = meals.sumOf { it.totalCalories.toDouble() }.toFloat()
    val totalProtein: Float get() = meals.sumOf { it.totalProtein.toDouble() }.toFloat()
    val totalCarbs: Float get() = meals.sumOf { it.totalCarbs.toDouble() }.toFloat()
    val totalFat: Float get() = meals.sumOf { it.totalFat.toDouble() }.toFloat()

    // ═══════════════════════════════════════════════════════════
    // CARB DETAILS DAILY TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalFiber: Float get() = meals.sumOf { it.totalFiber.toDouble() }.toFloat()
    val totalSugar: Float get() = meals.sumOf { it.totalSugar.toDouble() }.toFloat()
    val totalNetCarbs: Float get() = (totalCarbs - totalFiber).coerceAtLeast(0f)

    // ═══════════════════════════════════════════════════════════
    // FAT DETAILS DAILY TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalSaturatedFat: Float get() = meals.sumOf { it.totalSaturatedFat.toDouble() }.toFloat()
    val totalUnsaturatedFat: Float get() = meals.sumOf { it.totalUnsaturatedFat.toDouble() }.toFloat()
    val totalTransFat: Float get() = meals.sumOf { it.totalTransFat.toDouble() }.toFloat()
    val totalOmega3: Float get() = meals.sumOf { it.totalOmega3.toDouble() }.toFloat()
    val totalOmega6: Float get() = meals.sumOf { it.totalOmega6.toDouble() }.toFloat()
    val totalCholesterol: Float get() = meals.sumOf { it.totalCholesterol.toDouble() }.toFloat()

    // ═══════════════════════════════════════════════════════════
    // MINERALS DAILY TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalSodium: Float get() = meals.sumOf { it.totalSodium.toDouble() }.toFloat()
    val totalPotassium: Float get() = meals.sumOf { it.totalPotassium.toDouble() }.toFloat()
    val totalCalcium: Float get() = meals.sumOf { it.totalCalcium.toDouble() }.toFloat()
    val totalIron: Float get() = meals.sumOf { it.totalIron.toDouble() }.toFloat()
    val totalMagnesium: Float get() = meals.sumOf { it.totalMagnesium.toDouble() }.toFloat()
    val totalZinc: Float get() = meals.sumOf { it.totalZinc.toDouble() }.toFloat()
    val totalPhosphorus: Float get() = meals.sumOf { it.totalPhosphorus.toDouble() }.toFloat()

    // ═══════════════════════════════════════════════════════════
    // VITAMINS DAILY TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalVitaminA: Float get() = meals.sumOf { it.totalVitaminA.toDouble() }.toFloat()
    val totalVitaminC: Float get() = meals.sumOf { it.totalVitaminC.toDouble() }.toFloat()
    val totalVitaminD: Float get() = meals.sumOf { it.totalVitaminD.toDouble() }.toFloat()
    val totalVitaminE: Float get() = meals.sumOf { it.totalVitaminE.toDouble() }.toFloat()
    val totalVitaminK: Float get() = meals.sumOf { it.totalVitaminK.toDouble() }.toFloat()
    val totalVitaminB1: Float get() = meals.sumOf { it.totalVitaminB1.toDouble() }.toFloat()
    val totalVitaminB2: Float get() = meals.sumOf { it.totalVitaminB2.toDouble() }.toFloat()
    val totalVitaminB3: Float get() = meals.sumOf { it.totalVitaminB3.toDouble() }.toFloat()
    val totalVitaminB6: Float get() = meals.sumOf { it.totalVitaminB6.toDouble() }.toFloat()
    val totalVitaminB12: Float get() = meals.sumOf { it.totalVitaminB12.toDouble() }.toFloat()
    val totalFolate: Float get() = meals.sumOf { it.totalFolate.toDouble() }.toFloat()

    // ═══════════════════════════════════════════════════════════
    // PROGRESS CALCULATIONS
    // ═══════════════════════════════════════════════════════════
    val caloriesProgress: Float
        get() = if (targetCalories != null && targetCalories > 0) {
            (totalCalories / targetCalories).coerceIn(0f, 1f)
        } else 0f

    val proteinProgress: Float
        get() = if (targetProtein != null && targetProtein > 0) {
            (totalProtein / targetProtein).coerceIn(0f, 1f)
        } else 0f

    val carbsProgress: Float
        get() = if (targetCarbs != null && targetCarbs > 0) {
            (totalCarbs / targetCarbs).coerceIn(0f, 1f)
        } else 0f

    val fatProgress: Float
        get() = if (targetFat != null && targetFat > 0) {
            (totalFat / targetFat).coerceIn(0f, 1f)
        } else 0f

    // Helper to get meals by type
    fun getMealsByType(type: MealType): List<Meal> = meals.filter { it.mealType == type }

    // Get total calories for a specific meal type
    fun getCaloriesForType(type: MealType): Float =
        getMealsByType(type).sumOf { it.totalCalories.toDouble() }.toFloat()

    // Get extended nutrients summary for UI
    fun getExtendedNutrients(): ExtendedNutrientsSummary = ExtendedNutrientsSummary(
        // Carb details
        fiber = totalFiber,
        sugar = totalSugar,
        netCarbs = totalNetCarbs,
        // Fat details
        saturatedFat = totalSaturatedFat,
        unsaturatedFat = totalUnsaturatedFat,
        transFat = totalTransFat,
        omega3 = totalOmega3,
        omega6 = totalOmega6,
        cholesterol = totalCholesterol,
        // Minerals
        sodium = totalSodium,
        potassium = totalPotassium,
        calcium = totalCalcium,
        iron = totalIron,
        magnesium = totalMagnesium,
        zinc = totalZinc,
        phosphorus = totalPhosphorus,
        // Vitamins
        vitaminA = totalVitaminA,
        vitaminC = totalVitaminC,
        vitaminD = totalVitaminD,
        vitaminE = totalVitaminE,
        vitaminK = totalVitaminK,
        vitaminB1 = totalVitaminB1,
        vitaminB2 = totalVitaminB2,
        vitaminB3 = totalVitaminB3,
        vitaminB6 = totalVitaminB6,
        vitaminB12 = totalVitaminB12,
        folate = totalFolate
    )
}

@Serializable
data class Meal(
    val id: String,
    @SerialName("nutrition_log_id") val nutritionLogId: String,
    @SerialName("meal_type") val mealType: MealType,
    @SerialName("meal_name") val mealName: String? = null,
    val time: String, // ISO timestamp
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("ai_analysis_id") val aiAnalysisId: String? = null,
    @SerialName("ai_confidence") val aiConfidence: Float? = null,
    val notes: String? = null,
    @SerialName("meal_items") val items: List<MealItem> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
) {
    // ═══════════════════════════════════════════════════════════
    // CORE MACROS TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalCalories: Float get() = items.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein: Float get() = items.sumOf { it.protein.toDouble() }.toFloat()
    val totalCarbs: Float get() = items.sumOf { it.carbs.toDouble() }.toFloat()
    val totalFat: Float get() = items.sumOf { it.fat.toDouble() }.toFloat()

    // ═══════════════════════════════════════════════════════════
    // CARB DETAILS TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalFiber: Float get() = items.sumOf { it.fiber.toDouble() }.toFloat()
    val totalSugar: Float get() = items.sumOf { it.sugar.toDouble() }.toFloat()
    val totalNetCarbs: Float get() = (totalCarbs - totalFiber).coerceAtLeast(0f)

    // ═══════════════════════════════════════════════════════════
    // FAT DETAILS TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalSaturatedFat: Float get() = items.sumOf { it.saturatedFat.toDouble() }.toFloat()
    val totalUnsaturatedFat: Float get() = items.sumOf { it.unsaturatedFat.toDouble() }.toFloat()
    val totalTransFat: Float get() = items.sumOf { it.transFat.toDouble() }.toFloat()
    val totalOmega3: Float get() = items.sumOf { it.omega3.toDouble() }.toFloat()
    val totalOmega6: Float get() = items.sumOf { it.omega6.toDouble() }.toFloat()
    val totalCholesterol: Float get() = items.sumOf { it.cholesterol.toDouble() }.toFloat()

    // ═══════════════════════════════════════════════════════════
    // MINERALS TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalSodium: Float get() = items.sumOf { it.sodium.toDouble() }.toFloat()
    val totalPotassium: Float get() = items.sumOf { it.potassium.toDouble() }.toFloat()
    val totalCalcium: Float get() = items.sumOf { it.calcium.toDouble() }.toFloat()
    val totalIron: Float get() = items.sumOf { it.iron.toDouble() }.toFloat()
    val totalMagnesium: Float get() = items.sumOf { it.magnesium.toDouble() }.toFloat()
    val totalZinc: Float get() = items.sumOf { it.zinc.toDouble() }.toFloat()
    val totalPhosphorus: Float get() = items.sumOf { it.phosphorus.toDouble() }.toFloat()

    // ═══════════════════════════════════════════════════════════
    // VITAMINS TOTALS
    // ═══════════════════════════════════════════════════════════
    val totalVitaminA: Float get() = items.sumOf { it.vitaminA.toDouble() }.toFloat()
    val totalVitaminC: Float get() = items.sumOf { it.vitaminC.toDouble() }.toFloat()
    val totalVitaminD: Float get() = items.sumOf { it.vitaminD.toDouble() }.toFloat()
    val totalVitaminE: Float get() = items.sumOf { it.vitaminE.toDouble() }.toFloat()
    val totalVitaminK: Float get() = items.sumOf { it.vitaminK.toDouble() }.toFloat()
    val totalVitaminB1: Float get() = items.sumOf { it.vitaminB1.toDouble() }.toFloat()
    val totalVitaminB2: Float get() = items.sumOf { it.vitaminB2.toDouble() }.toFloat()
    val totalVitaminB3: Float get() = items.sumOf { it.vitaminB3.toDouble() }.toFloat()
    val totalVitaminB6: Float get() = items.sumOf { it.vitaminB6.toDouble() }.toFloat()
    val totalVitaminB12: Float get() = items.sumOf { it.vitaminB12.toDouble() }.toFloat()
    val totalFolate: Float get() = items.sumOf { it.folate.toDouble() }.toFloat()

    val displayTime: String
        get() {
            return try {
                val timeStr = time.substringAfter('T').substringBefore('.')
                val parts = timeStr.split(':')
                val hour = parts[0].toInt()
                val minute = parts[1]
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
                "$displayHour:$minute $amPm"
            } catch (e: Exception) {
                "Unknown time"
            }
        }
}

@Serializable
enum class MealType {
    @SerialName("breakfast") BREAKFAST,
    @SerialName("lunch") LUNCH,
    @SerialName("dinner") DINNER,
    @SerialName("shake") SHAKE,
    @SerialName("snack") SNACK;

    val displayName: String
        get() = when (this) {
            BREAKFAST -> "Breakfast"
            LUNCH -> "Lunch"
            DINNER -> "Dinner"
            SHAKE -> "Shake"
            SNACK -> "Snack"
        }

    // Icon name for Material Icons reference
    val iconName: String
        get() = when (this) {
            BREAKFAST -> "WbSunny"
            LUNCH -> "Restaurant"
            DINNER -> "DinnerDining"
            SHAKE -> "LocalDrink"
            SNACK -> "Cookie"
        }

    // Display order for UI sorting
    val displayOrder: Int
        get() = when (this) {
            BREAKFAST -> 0
            LUNCH -> 1
            DINNER -> 2
            SHAKE -> 3
            SNACK -> 4
        }

    companion object {
        // Get all types in display order
        fun orderedValues(): List<MealType> = entries.sortedBy { it.displayOrder }
    }
}

@Serializable
data class MealItem(
    val id: String,
    @SerialName("meal_id") val mealId: String,
    @SerialName("food_id") val foodId: String? = null,
    @SerialName("item_name") val itemName: String,
    val quantity: Float,
    @SerialName("quantity_unit") val quantityUnit: String = "g",

    // ═══════════════════════════════════════════════════════════
    // CORE MACROS
    // ═══════════════════════════════════════════════════════════
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,

    // ═══════════════════════════════════════════════════════════
    // CARB DETAILS
    // ═══════════════════════════════════════════════════════════
    val fiber: Float = 0f,           // Ballaststoffe (g)
    val sugar: Float = 0f,           // Zucker (g)

    // ═══════════════════════════════════════════════════════════
    // FAT DETAILS
    // ═══════════════════════════════════════════════════════════
    @SerialName("saturated_fat") val saturatedFat: Float = 0f,      // gesättigte Fettsäuren (g)
    @SerialName("unsaturated_fat") val unsaturatedFat: Float = 0f,  // ungesättigte Fettsäuren (g)
    @SerialName("trans_fat") val transFat: Float = 0f,              // Transfette (g)
    val omega3: Float = 0f,          // Omega-3 (mg)
    val omega6: Float = 0f,          // Omega-6 (mg)
    val cholesterol: Float = 0f,     // Cholesterin (mg)

    // ═══════════════════════════════════════════════════════════
    // MINERALS
    // ═══════════════════════════════════════════════════════════
    val sodium: Float = 0f,          // Natrium (mg)
    val potassium: Float = 0f,       // Kalium (mg)
    val calcium: Float = 0f,         // Calcium (mg)
    val iron: Float = 0f,            // Eisen (mg)
    val magnesium: Float = 0f,       // Magnesium (mg)
    val zinc: Float = 0f,            // Zink (mg)
    val phosphorus: Float = 0f,      // Phosphor (mg)

    // ═══════════════════════════════════════════════════════════
    // VITAMINS
    // ═══════════════════════════════════════════════════════════
    @SerialName("vitamin_a") val vitaminA: Float = 0f,    // Vitamin A (mcg RAE)
    @SerialName("vitamin_c") val vitaminC: Float = 0f,    // Vitamin C (mg)
    @SerialName("vitamin_d") val vitaminD: Float = 0f,    // Vitamin D (mcg)
    @SerialName("vitamin_e") val vitaminE: Float = 0f,    // Vitamin E (mg)
    @SerialName("vitamin_k") val vitaminK: Float = 0f,    // Vitamin K (mcg)
    @SerialName("vitamin_b1") val vitaminB1: Float = 0f,  // Thiamin (mg)
    @SerialName("vitamin_b2") val vitaminB2: Float = 0f,  // Riboflavin (mg)
    @SerialName("vitamin_b3") val vitaminB3: Float = 0f,  // Niacin (mg)
    @SerialName("vitamin_b6") val vitaminB6: Float = 0f,  // Vitamin B6 (mg)
    @SerialName("vitamin_b12") val vitaminB12: Float = 0f, // Vitamin B12 (mcg)
    val folate: Float = 0f,          // Folsäure (mcg DFE)

    @SerialName("created_at") val createdAt: String? = null
) {
    val displayQuantity: String
        get() = "${quantity.toInt()}$quantityUnit"

    // Net Carbs = Carbs - Fiber
    val netCarbs: Float
        get() = (carbs - fiber).coerceAtLeast(0f)
}

@Serializable
data class MealTemplate(
    val id: String,
    val userId: String,
    val name: String,
    val mealType: MealType? = null,
    val isFavorite: Boolean = false,
    val totalCalories: Float,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val items: List<MealTemplateItem>,
    val photoUrl: String? = null,
    val createdAt: String? = null
)

@Serializable
data class MealTemplateItem(
    val itemName: String,
    val quantity: Float,
    val quantityUnit: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

@Serializable
data class NutritionGoal(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("goal_type") val goalType: GoalType,
    @SerialName("target_calories") val targetCalories: Float,
    @SerialName("target_protein") val targetProtein: Float,
    @SerialName("target_carbs") val targetCarbs: Float,
    @SerialName("target_fat") val targetFat: Float,
    @SerialName("meals_per_day") val mealsPerDay: Int = 3,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
enum class GoalType {
    @SerialName("cutting") CUTTING,
    @SerialName("bulking") BULKING,
    @SerialName("maintenance") MAINTENANCE,
    @SerialName("performance") PERFORMANCE;

    val displayName: String
        get() = when (this) {
            CUTTING -> "Cutting"
            BULKING -> "Bulking"
            MAINTENANCE -> "Maintenance"
            PERFORMANCE -> "Performance"
        }

    val iconName: String
        get() = when (this) {
            CUTTING -> "TrendingDown"
            BULKING -> "TrendingUp"
            MAINTENANCE -> "TrendingFlat"
            PERFORMANCE -> "Bolt"
        }
}

// ═══════════════════════════════════════════════════════════════
// SHAKE PRESETS (for Quick Add)
// ═══════════════════════════════════════════════════════════════

data class ShakePreset(
    val id: String,
    val name: String,
    val servingSize: Float = 30f,
    val servingUnit: String = "g",
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            ShakePreset(
                id = "whey_standard",
                name = "Whey Protein",
                servingSize = 30f,
                calories = 120f,
                protein = 24f,
                carbs = 3f,
                fat = 1.5f
            ),
            ShakePreset(
                id = "whey_isolate",
                name = "Whey Isolate",
                servingSize = 30f,
                calories = 110f,
                protein = 27f,
                carbs = 1f,
                fat = 0.5f
            ),
            ShakePreset(
                id = "vegan_protein",
                name = "Vegan Protein",
                servingSize = 30f,
                calories = 110f,
                protein = 20f,
                carbs = 5f,
                fat = 2f
            ),
            ShakePreset(
                id = "casein",
                name = "Casein",
                servingSize = 30f,
                calories = 120f,
                protein = 24f,
                carbs = 3f,
                fat = 1f
            ),
            ShakePreset(
                id = "mass_gainer",
                name = "Mass Gainer",
                servingSize = 100f,
                calories = 400f,
                protein = 25f,
                carbs = 60f,
                fat = 5f
            ),
            ShakePreset(
                id = "weight_loss",
                name = "Weight Loss Shake",
                servingSize = 30f,
                calories = 90f,
                protein = 15f,
                carbs = 5f,
                fat = 1f
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// AI PHOTO ANALYSIS MODELS
// ═══════════════════════════════════════════════════════════════

@Serializable
data class AIPhotoAnalysisRequest(
    val image_base64: String,
    val meal_type: String? = null,
    val additional_context: String? = null
)

@Serializable
data class AIPhotoAnalysisResponse(
    val success: Boolean = true,
    val meal_name: String = "",
    val items: List<AIAnalyzedItem> = emptyList(),
    val total: AIAnalyzedTotals = AIAnalyzedTotals(),
    val ai_confidence: Float = 0f,
    val suggestions: String? = null,
    val error: String? = null
)

@Serializable
data class AIAnalyzedItem(
    val name: String,
    val quantity: String,
    val quantity_value: Float,
    val quantity_unit: String,
    // Core macros
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val confidence: Float,
    // Carb details
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    // Fat details
    val saturated_fat: Float = 0f,
    val unsaturated_fat: Float = 0f,
    val trans_fat: Float = 0f,
    val omega3: Float = 0f,
    val omega6: Float = 0f,
    val cholesterol: Float = 0f,
    // Minerals
    val sodium: Float = 0f,
    val potassium: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    val magnesium: Float = 0f,
    val zinc: Float = 0f,
    val phosphorus: Float = 0f,
    // Vitamins
    val vitamin_a: Float = 0f,
    val vitamin_c: Float = 0f,
    val vitamin_d: Float = 0f,
    val vitamin_e: Float = 0f,
    val vitamin_k: Float = 0f,
    val vitamin_b1: Float = 0f,
    val vitamin_b2: Float = 0f,
    val vitamin_b3: Float = 0f,
    val vitamin_b6: Float = 0f,
    val vitamin_b12: Float = 0f,
    val folate: Float = 0f
)

@Serializable
data class AIAnalyzedTotals(
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f
)

// ═══════════════════════════════════════════════════════════════
// NUTRITION STREAK DATA
// ═══════════════════════════════════════════════════════════════

data class NutritionStreak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val thisWeekDays: Int = 0,
    val thisWeekAvgCalories: Float = 0f,
    val thisWeekAvgProtein: Float = 0f,
    val loggedDates: Set<String> = emptySet() // "2025-11-27" format
)

// ═══════════════════════════════════════════════════════════════
// EXTENDED NUTRIENTS SUMMARY (for UI display)
// ═══════════════════════════════════════════════════════════════

data class ExtendedNutrientsSummary(
    // Carb details
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val netCarbs: Float = 0f,
    // Fat details
    val saturatedFat: Float = 0f,
    val unsaturatedFat: Float = 0f,
    val transFat: Float = 0f,
    val omega3: Float = 0f,
    val omega6: Float = 0f,
    val cholesterol: Float = 0f,
    // Minerals
    val sodium: Float = 0f,
    val potassium: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    val magnesium: Float = 0f,
    val zinc: Float = 0f,
    val phosphorus: Float = 0f,
    // Vitamins
    val vitaminA: Float = 0f,
    val vitaminC: Float = 0f,
    val vitaminD: Float = 0f,
    val vitaminE: Float = 0f,
    val vitaminK: Float = 0f,
    val vitaminB1: Float = 0f,
    val vitaminB2: Float = 0f,
    val vitaminB3: Float = 0f,
    val vitaminB6: Float = 0f,
    val vitaminB12: Float = 0f,
    val folate: Float = 0f
) {
    // Omega-6 to Omega-3 ratio (ideal is 1:1 to 4:1)
    val omega6to3Ratio: Float
        get() = if (omega3 > 0) omega6 / omega3 else 0f

    // Fat quality score (higher unsaturated = better)
    val fatQualityPercent: Float
        get() {
            val totalDetailedFat = saturatedFat + unsaturatedFat + transFat
            return if (totalDetailedFat > 0) (unsaturatedFat / totalDetailedFat) * 100 else 0f
        }
}

// ═══════════════════════════════════════════════════════════════
// DAILY RECOMMENDED VALUES (RDA/RDI based on adult male athlete)
// ═══════════════════════════════════════════════════════════════

object DailyNutrientTargets {
    // Carb details
    const val FIBER = 30f              // g (25-38g recommended)
    const val SUGAR_MAX = 50f          // g (WHO recommendation: <10% of calories)

    // Fat details (based on 2500 kcal diet)
    const val SATURATED_FAT_MAX = 22f  // g (<10% of calories)
    const val TRANS_FAT_MAX = 2f       // g (as low as possible)
    const val CHOLESTEROL_MAX = 300f   // mg
    const val OMEGA3 = 1600f           // mg (EPA+DHA 250-500mg, total 1.6g)
    const val OMEGA6 = 17000f          // mg (17g for men)

    // Minerals
    const val SODIUM_MAX = 2300f       // mg (ideally <1500mg)
    const val POTASSIUM = 4700f        // mg
    const val CALCIUM = 1000f          // mg (1000-1200mg)
    const val IRON = 8f                // mg (8-18mg depending on gender)
    const val MAGNESIUM = 420f         // mg (400-420mg for men)
    const val ZINC = 11f               // mg
    const val PHOSPHORUS = 700f        // mg

    // Vitamins
    const val VITAMIN_A = 900f         // mcg RAE
    const val VITAMIN_C = 90f          // mg (90-120mg for athletes)
    const val VITAMIN_D = 20f          // mcg (800 IU)
    const val VITAMIN_E = 15f          // mg
    const val VITAMIN_K = 120f         // mcg
    const val VITAMIN_B1 = 1.2f        // mg (Thiamin)
    const val VITAMIN_B2 = 1.3f        // mg (Riboflavin)
    const val VITAMIN_B3 = 16f         // mg (Niacin)
    const val VITAMIN_B6 = 1.3f        // mg
    const val VITAMIN_B12 = 2.4f       // mcg
    const val FOLATE = 400f            // mcg DFE

    // Get progress for a nutrient (0.0 to 1.0+)
    fun getProgress(current: Float, target: Float, isMaxLimit: Boolean = false): Float {
        if (target <= 0) return 0f
        val progress = current / target
        return if (isMaxLimit) progress else progress.coerceAtMost(1f)
    }

    // Get status color hint
    fun getStatus(current: Float, target: Float, isMaxLimit: Boolean = false): NutrientStatus {
        val progress = current / target
        return when {
            isMaxLimit && progress > 1.0f -> NutrientStatus.OVER_LIMIT
            isMaxLimit && progress > 0.8f -> NutrientStatus.WARNING
            !isMaxLimit && progress < 0.5f -> NutrientStatus.LOW
            !isMaxLimit && progress >= 0.8f -> NutrientStatus.GOOD
            else -> NutrientStatus.NORMAL
        }
    }
}

enum class NutrientStatus {
    LOW,        // Under 50% of target (for nutrients you need more of)
    NORMAL,     // 50-80% of target
    GOOD,       // 80-100% of target (for nutrients you need)
    WARNING,    // 80-100% of max limit (for nutrients to limit)
    OVER_LIMIT  // Over 100% of max limit
}

// ═══════════════════════════════════════════════════════════════
// NUTRIENT INFO (for UI display with units and descriptions)
// ═══════════════════════════════════════════════════════════════

enum class NutrientCategory {
    CARB_DETAILS,
    FAT_DETAILS,
    MINERALS,
    VITAMINS
}

data class NutrientInfo(
    val id: String,
    val name: String,
    val shortName: String,
    val unit: String,
    val dailyTarget: Float,
    val isMaxLimit: Boolean = false,  // true = should stay below target
    val category: NutrientCategory,
    val description: String = ""
) {
    companion object {
        val ALL_NUTRIENTS = listOf(
            // Carb Details
            NutrientInfo("fiber", "Fiber", "Fiber", "g", DailyNutrientTargets.FIBER, false, NutrientCategory.CARB_DETAILS, "Supports digestion & gut health"),
            NutrientInfo("sugar", "Sugar", "Sugar", "g", DailyNutrientTargets.SUGAR_MAX, true, NutrientCategory.CARB_DETAILS, "Keep low for stable energy"),

            // Fat Details
            NutrientInfo("saturated_fat", "Saturated Fat", "Sat Fat", "g", DailyNutrientTargets.SATURATED_FAT_MAX, true, NutrientCategory.FAT_DETAILS, "Limit for heart health"),
            NutrientInfo("trans_fat", "Trans Fat", "Trans", "g", DailyNutrientTargets.TRANS_FAT_MAX, true, NutrientCategory.FAT_DETAILS, "Avoid as much as possible"),
            NutrientInfo("cholesterol", "Cholesterol", "Chol", "mg", DailyNutrientTargets.CHOLESTEROL_MAX, true, NutrientCategory.FAT_DETAILS, "Dietary cholesterol"),
            NutrientInfo("omega3", "Omega-3", "Ω-3", "mg", DailyNutrientTargets.OMEGA3, false, NutrientCategory.FAT_DETAILS, "Anti-inflammatory, brain health"),
            NutrientInfo("omega6", "Omega-6", "Ω-6", "mg", DailyNutrientTargets.OMEGA6, false, NutrientCategory.FAT_DETAILS, "Essential fatty acid"),

            // Minerals
            NutrientInfo("sodium", "Sodium", "Na", "mg", DailyNutrientTargets.SODIUM_MAX, true, NutrientCategory.MINERALS, "Limit for blood pressure"),
            NutrientInfo("potassium", "Potassium", "K", "mg", DailyNutrientTargets.POTASSIUM, false, NutrientCategory.MINERALS, "Muscle & nerve function"),
            NutrientInfo("calcium", "Calcium", "Ca", "mg", DailyNutrientTargets.CALCIUM, false, NutrientCategory.MINERALS, "Bone & muscle health"),
            NutrientInfo("iron", "Iron", "Fe", "mg", DailyNutrientTargets.IRON, false, NutrientCategory.MINERALS, "Oxygen transport"),
            NutrientInfo("magnesium", "Magnesium", "Mg", "mg", DailyNutrientTargets.MAGNESIUM, false, NutrientCategory.MINERALS, "300+ enzyme reactions"),
            NutrientInfo("zinc", "Zinc", "Zn", "mg", DailyNutrientTargets.ZINC, false, NutrientCategory.MINERALS, "Immune & protein synthesis"),
            NutrientInfo("phosphorus", "Phosphorus", "P", "mg", DailyNutrientTargets.PHOSPHORUS, false, NutrientCategory.MINERALS, "Bone & energy production"),

            // Vitamins
            NutrientInfo("vitamin_a", "Vitamin A", "Vit A", "mcg", DailyNutrientTargets.VITAMIN_A, false, NutrientCategory.VITAMINS, "Vision & immune function"),
            NutrientInfo("vitamin_c", "Vitamin C", "Vit C", "mg", DailyNutrientTargets.VITAMIN_C, false, NutrientCategory.VITAMINS, "Antioxidant, collagen"),
            NutrientInfo("vitamin_d", "Vitamin D", "Vit D", "mcg", DailyNutrientTargets.VITAMIN_D, false, NutrientCategory.VITAMINS, "Bone health, immunity"),
            NutrientInfo("vitamin_e", "Vitamin E", "Vit E", "mg", DailyNutrientTargets.VITAMIN_E, false, NutrientCategory.VITAMINS, "Antioxidant"),
            NutrientInfo("vitamin_k", "Vitamin K", "Vit K", "mcg", DailyNutrientTargets.VITAMIN_K, false, NutrientCategory.VITAMINS, "Blood clotting, bones"),
            NutrientInfo("vitamin_b1", "Thiamin (B1)", "B1", "mg", DailyNutrientTargets.VITAMIN_B1, false, NutrientCategory.VITAMINS, "Energy metabolism"),
            NutrientInfo("vitamin_b2", "Riboflavin (B2)", "B2", "mg", DailyNutrientTargets.VITAMIN_B2, false, NutrientCategory.VITAMINS, "Energy & cell function"),
            NutrientInfo("vitamin_b3", "Niacin (B3)", "B3", "mg", DailyNutrientTargets.VITAMIN_B3, false, NutrientCategory.VITAMINS, "Energy & DNA repair"),
            NutrientInfo("vitamin_b6", "Vitamin B6", "B6", "mg", DailyNutrientTargets.VITAMIN_B6, false, NutrientCategory.VITAMINS, "Protein metabolism"),
            NutrientInfo("vitamin_b12", "Vitamin B12", "B12", "mcg", DailyNutrientTargets.VITAMIN_B12, false, NutrientCategory.VITAMINS, "Nerve function, red blood cells"),
            NutrientInfo("folate", "Folate", "Folate", "mcg", DailyNutrientTargets.FOLATE, false, NutrientCategory.VITAMINS, "Cell division, DNA")
        )

        fun getByCategory(category: NutrientCategory): List<NutrientInfo> =
            ALL_NUTRIENTS.filter { it.category == category }

        fun getById(id: String): NutrientInfo? =
            ALL_NUTRIENTS.find { it.id == id }
    }
}

// ═══════════════════════════════════════════════════════════════
// FREQUENT MEALS (Quick Add Feature)
// ═══════════════════════════════════════════════════════════════

/**
 * A frequently used meal for quick-add functionality.
 * Auto-learned from user behavior or manually created.
 */
@Serializable
data class FrequentMeal(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("meal_type") val mealType: String,
    val items: List<FrequentMealItem> = emptyList(),
    @SerialName("total_calories") val totalCalories: Float = 0f,
    @SerialName("total_protein") val totalProtein: Float = 0f,
    @SerialName("total_carbs") val totalCarbs: Float = 0f,
    @SerialName("total_fat") val totalFat: Float = 0f,
    @SerialName("usage_count") val usageCount: Int = 1,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_custom") val isCustom: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @kotlinx.serialization.Transient val isPreset: Boolean = false  // UI-only, not from DB
) {
    val mealTypeEnum: MealType
        get() = try {
            MealType.valueOf(mealType.uppercase())
        } catch (e: Exception) {
            MealType.SNACK
        }
}

/**
 * Item stored within a FrequentMeal (simplified MealItem for storage)
 */
@Serializable
data class FrequentMealItem(
    val name: String,
    val quantity: Float,
    @SerialName("quantity_unit") val quantityUnit: String = "g",
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f
)

/**
 * Common add-on for a base meal (e.g., Banana often added to Protein Shake)
 */
@Serializable
data class FrequentAddOn(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("base_meal_id") val baseMealId: String? = null,
    @SerialName("base_meal_name") val baseMealName: String,
    @SerialName("addon_name") val addonName: String,
    @SerialName("addon_quantity") val addonQuantity: Float = 1f,
    @SerialName("addon_unit") val addonUnit: String = "piece",
    @SerialName("addon_calories") val addonCalories: Float = 0f,
    @SerialName("addon_protein") val addonProtein: Float = 0f,
    @SerialName("addon_carbs") val addonCarbs: Float = 0f,
    @SerialName("addon_fat") val addonFat: Float = 0f,
    @SerialName("combination_count") val combinationCount: Int = 1,
    @SerialName("last_combined_at") val lastCombinedAt: String? = null
)

/**
 * Request to upsert a frequent meal (for API calls)
 */
@Serializable
data class FrequentMealUpsert(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("meal_type") val mealType: String,
    val items: List<FrequentMealItem>,
    @SerialName("total_calories") val totalCalories: Float,
    @SerialName("total_protein") val totalProtein: Float,
    @SerialName("total_carbs") val totalCarbs: Float,
    @SerialName("total_fat") val totalFat: Float
)