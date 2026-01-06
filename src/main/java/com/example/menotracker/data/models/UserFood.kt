package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════
// USER FOOD LIBRARY
// Personal food entries with customizations (Matcha Latte + Oat Milk)
// ═══════════════════════════════════════════════════════════════

/**
 * A user's personalized food entry with modifications.
 * Example: "Starbucks Matcha Latte with Oat Milk, No Sugar"
 */
@Serializable
data class UserFood(
    val id: String,

    @SerialName("user_id")
    val userId: String,

    // ═══════════════════════════════════════════════════════════
    // ORIGIN TRACKING
    // ═══════════════════════════════════════════════════════════

    @SerialName("base_food_id")
    val baseFoodId: String? = null,       // Original food ID from search

    @SerialName("base_food_source")
    val baseFoodSource: String? = null,   // "USDA", "OPEN_FOOD_FACTS", "RESTAURANT", "MANUAL"

    // ═══════════════════════════════════════════════════════════
    // DISPLAY INFO
    // ═══════════════════════════════════════════════════════════

    @SerialName("display_name")
    val displayName: String,              // "Starbucks Matcha Latte Oat Milk" - what user sees

    @SerialName("base_name")
    val baseName: String,                 // "Matcha Latte" - original/base name

    val brand: String? = null,            // "Starbucks", "McDonald's", etc.

    val emoji: String? = null,            // Optional emoji for quick recognition

    val category: String? = null,         // "Coffee", "Fast Food", "Meal Prep", etc.

    // ═══════════════════════════════════════════════════════════
    // MODIFICATIONS (the magic!)
    // ═══════════════════════════════════════════════════════════

    val modifications: List<FoodModification> = emptyList(),

    // ═══════════════════════════════════════════════════════════
    // NUTRITIONAL INFO (adjusted for modifications)
    // Per 100g or per serving
    // ═══════════════════════════════════════════════════════════

    @SerialName("is_per_serving")
    val isPerServing: Boolean = false,    // true = values are per serving, false = per 100g

    // Core macros
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,

    // Carb details
    val fiber: Float = 0f,
    val sugar: Float = 0f,

    // Fat details
    @SerialName("saturated_fat") val saturatedFat: Float = 0f,
    @SerialName("unsaturated_fat") val unsaturatedFat: Float = 0f,
    @SerialName("trans_fat") val transFat: Float = 0f,
    @SerialName("omega_3") val omega3: Float = 0f,
    @SerialName("omega_6") val omega6: Float = 0f,
    val cholesterol: Float = 0f,

    // Minerals
    val sodium: Float = 0f,
    val potassium: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    val magnesium: Float = 0f,

    // ═══════════════════════════════════════════════════════════
    // SERVING INFO
    // ═══════════════════════════════════════════════════════════

    @SerialName("default_serving_size")
    val defaultServingSize: Float = 100f,

    @SerialName("default_serving_unit")
    val defaultServingUnit: String = "g",

    @SerialName("serving_description")
    val servingDescription: String? = null,  // "1 Grande (473ml)", "1 Portion"

    // Available portions for this food
    val portions: List<UserFoodPortion> = emptyList(),

    // ═══════════════════════════════════════════════════════════
    // USAGE TRACKING
    // ═══════════════════════════════════════════════════════════

    @SerialName("use_count")
    val useCount: Int = 0,                // How often this was used

    @SerialName("last_used_at")
    val lastUsedAt: Long? = null,         // Unix timestamp

    @SerialName("is_favorite")
    val isFavorite: Boolean = false,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // ═══════════════════════════════════════════════════════════
    // NOVA CLASSIFICATION
    // ═══════════════════════════════════════════════════════════

    @SerialName("nova_classification")
    val novaClassification: Int? = null   // 1-4 NOVA score
) {
    /**
     * Get the full display name including modifications
     * e.g., "Matcha Latte • Oat Milk • No Sugar"
     */
    fun getFullDisplayName(): String {
        if (modifications.isEmpty()) return displayName

        val modLabels = modifications.mapNotNull { it.displayLabel }
        return if (modLabels.isNotEmpty()) {
            "$baseName • ${modLabels.joinToString(" • ")}"
        } else {
            displayName
        }
    }

    /**
     * Get short modification summary
     * e.g., "Oat, No Sugar"
     */
    fun getModificationSummary(): String? {
        if (modifications.isEmpty()) return null
        return modifications.mapNotNull { it.shortLabel }.joinToString(", ")
    }

    /**
     * Check if this food matches a search query
     */
    fun matchesQuery(query: String): Boolean {
        val q = query.lowercase()
        return displayName.lowercase().contains(q) ||
               baseName.lowercase().contains(q) ||
               brand?.lowercase()?.contains(q) == true ||
               modifications.any { it.displayLabel?.lowercase()?.contains(q) == true }
    }

    /**
     * Calculate macros for a given serving size
     */
    fun getMacrosForServing(servingSize: Float, servingUnit: String): MacroValues {
        val multiplier = if (isPerServing) {
            servingSize / defaultServingSize
        } else {
            servingSize / 100f
        }

        return MacroValues(
            calories = calories * multiplier,
            protein = protein * multiplier,
            carbs = carbs * multiplier,
            fat = fat * multiplier,
            fiber = fiber * multiplier,
            sugar = sugar * multiplier
        )
    }
}

/**
 * Simple macro values container
 */
@Serializable
data class MacroValues(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f
)

/**
 * A modification to a base food
 * Examples: Oat Milk, No Sugar, Grande Size
 */
@Serializable
data class FoodModification(
    val category: ModificationCategory,

    val value: String,              // "oat", "none", "grande"

    @SerialName("display_label")
    val displayLabel: String?,      // "Oat Milk" - for full display

    @SerialName("short_label")
    val shortLabel: String?,        // "Oat" - for compact display

    // Optional: how this modification affects macros
    @SerialName("calorie_adjustment")
    val calorieAdjustment: Float = 0f,  // +/- calories

    @SerialName("protein_adjustment")
    val proteinAdjustment: Float = 0f,

    @SerialName("carb_adjustment")
    val carbAdjustment: Float = 0f,

    @SerialName("fat_adjustment")
    val fatAdjustment: Float = 0f
)

/**
 * Modification categories for food customization
 */
@Serializable
enum class ModificationCategory {
    @SerialName("milk") MILK,           // dairy, oat, soy, almond, coconut, none
    @SerialName("sweetener") SWEETENER, // regular, half, none, stevia, honey
    @SerialName("size") SIZE,           // small, medium, large, grande, venti
    @SerialName("extra") EXTRA,         // extra shot, extra cheese, double meat
    @SerialName("topping") TOPPING,     // whipped cream, chocolate drizzle
    @SerialName("preparation") PREPARATION, // grilled, fried, steamed, raw
    @SerialName("side") SIDE,           // with fries, with salad
    @SerialName("other") OTHER          // any other modification
}

/**
 * Portion definition for a user food
 */
@Serializable
data class UserFoodPortion(
    val name: String,           // "Grande", "1 Portion", "100g"
    val amount: Float,          // 473, 1, 100
    val unit: String,           // "ml", "portion", "g"

    @SerialName("grams_equivalent")
    val gramsEquivalent: Float? = null  // For conversion to grams
)

// ═══════════════════════════════════════════════════════════════
// COMMON MODIFICATION PRESETS
// ═══════════════════════════════════════════════════════════════

object MilkModifications {
    val REGULAR = FoodModification(
        category = ModificationCategory.MILK,
        value = "regular",
        displayLabel = "Regular Milk",
        shortLabel = "Milk",
        calorieAdjustment = 0f
    )

    val OAT = FoodModification(
        category = ModificationCategory.MILK,
        value = "oat",
        displayLabel = "Oat Milk",
        shortLabel = "Oat",
        calorieAdjustment = 20f,  // Oat milk typically adds ~20 cal
        carbAdjustment = 4f
    )

    val SOY = FoodModification(
        category = ModificationCategory.MILK,
        value = "soy",
        displayLabel = "Soy Milk",
        shortLabel = "Soy",
        calorieAdjustment = -10f,
        proteinAdjustment = 1f
    )

    val ALMOND = FoodModification(
        category = ModificationCategory.MILK,
        value = "almond",
        displayLabel = "Almond Milk",
        shortLabel = "Almond",
        calorieAdjustment = -30f,
        carbAdjustment = -3f
    )

    val COCONUT = FoodModification(
        category = ModificationCategory.MILK,
        value = "coconut",
        displayLabel = "Coconut Milk",
        shortLabel = "Coconut",
        calorieAdjustment = -15f,
        fatAdjustment = 1f
    )

    val NONE = FoodModification(
        category = ModificationCategory.MILK,
        value = "none",
        displayLabel = "No Milk",
        shortLabel = "Black",
        calorieAdjustment = -50f
    )

    val ALL = listOf(REGULAR, OAT, SOY, ALMOND, COCONUT, NONE)
}

object SweetenerModifications {
    val REGULAR = FoodModification(
        category = ModificationCategory.SWEETENER,
        value = "regular",
        displayLabel = "Regular Sugar",
        shortLabel = "Regular",
        calorieAdjustment = 0f
    )

    val HALF = FoodModification(
        category = ModificationCategory.SWEETENER,
        value = "half",
        displayLabel = "Half Sugar",
        shortLabel = "Half",
        calorieAdjustment = -20f,
        carbAdjustment = -5f
    )

    val NONE = FoodModification(
        category = ModificationCategory.SWEETENER,
        value = "none",
        displayLabel = "No Sugar",
        shortLabel = "No Sugar",
        calorieAdjustment = -40f,
        carbAdjustment = -10f
    )

    val STEVIA = FoodModification(
        category = ModificationCategory.SWEETENER,
        value = "stevia",
        displayLabel = "Stevia",
        shortLabel = "Stevia",
        calorieAdjustment = -40f,
        carbAdjustment = -10f
    )

    val HONEY = FoodModification(
        category = ModificationCategory.SWEETENER,
        value = "honey",
        displayLabel = "Honey",
        shortLabel = "Honey",
        calorieAdjustment = 10f,
        carbAdjustment = 3f
    )

    val ALL = listOf(REGULAR, HALF, NONE, STEVIA, HONEY)
}

object SizeModifications {
    val SMALL = FoodModification(
        category = ModificationCategory.SIZE,
        value = "small",
        displayLabel = "Small",
        shortLabel = "S"
    )

    val MEDIUM = FoodModification(
        category = ModificationCategory.SIZE,
        value = "medium",
        displayLabel = "Medium",
        shortLabel = "M"
    )

    val LARGE = FoodModification(
        category = ModificationCategory.SIZE,
        value = "large",
        displayLabel = "Large",
        shortLabel = "L"
    )

    // Starbucks sizes
    val TALL = FoodModification(
        category = ModificationCategory.SIZE,
        value = "tall",
        displayLabel = "Tall (354ml)",
        shortLabel = "Tall"
    )

    val GRANDE = FoodModification(
        category = ModificationCategory.SIZE,
        value = "grande",
        displayLabel = "Grande (473ml)",
        shortLabel = "Grande"
    )

    val VENTI = FoodModification(
        category = ModificationCategory.SIZE,
        value = "venti",
        displayLabel = "Venti (591ml)",
        shortLabel = "Venti"
    )

    val STANDARD = listOf(SMALL, MEDIUM, LARGE)
    val STARBUCKS = listOf(TALL, GRANDE, VENTI)
}

// ═══════════════════════════════════════════════════════════════
// EXTENSION: Detect which modifications might be relevant
// ═══════════════════════════════════════════════════════════════

/**
 * Detect relevant modification categories based on food name/type
 */
fun detectRelevantModifications(foodName: String, brand: String? = null): List<ModificationCategory> {
    val name = foodName.lowercase()
    val brandLower = brand?.lowercase() ?: ""

    val relevant = mutableListOf<ModificationCategory>()

    // Coffee/Tea drinks → Milk + Sweetener + Size
    if (name.contains("latte") || name.contains("cappuccino") ||
        name.contains("coffee") || name.contains("macchiato") ||
        name.contains("tea") || name.contains("matcha") ||
        name.contains("chai") || name.contains("mocha")) {
        relevant.add(ModificationCategory.MILK)
        relevant.add(ModificationCategory.SWEETENER)
        relevant.add(ModificationCategory.SIZE)
    }

    // Starbucks → Use their sizes
    if (brandLower.contains("starbucks")) {
        if (!relevant.contains(ModificationCategory.SIZE)) {
            relevant.add(ModificationCategory.SIZE)
        }
    }

    // Smoothies/Shakes → Size + Extras
    if (name.contains("smoothie") || name.contains("shake") ||
        name.contains("frappe") || name.contains("frappuccino")) {
        relevant.add(ModificationCategory.SIZE)
        relevant.add(ModificationCategory.TOPPING) // Whipped cream, etc.
    }

    // Burgers/Sandwiches → Extras + Sides
    if (name.contains("burger") || name.contains("sandwich") ||
        name.contains("wrap") || name.contains("sub")) {
        relevant.add(ModificationCategory.EXTRA)  // Extra cheese, bacon
        relevant.add(ModificationCategory.SIDE)   // With fries
    }

    // Protein dishes → Preparation
    if (name.contains("chicken") || name.contains("fish") ||
        name.contains("steak") || name.contains("salmon")) {
        relevant.add(ModificationCategory.PREPARATION) // Grilled, fried
    }

    // Salads → Extras
    if (name.contains("salad") || name.contains("bowl")) {
        relevant.add(ModificationCategory.EXTRA) // Dressing, protein
    }

    return relevant.distinct()
}

/**
 * Get available modification options for a category
 */
fun getModificationOptions(
    category: ModificationCategory,
    brand: String? = null
): List<FoodModification> {
    return when (category) {
        ModificationCategory.MILK -> MilkModifications.ALL
        ModificationCategory.SWEETENER -> SweetenerModifications.ALL
        ModificationCategory.SIZE -> {
            when {
                brand?.lowercase()?.contains("starbucks") == true -> SizeModifications.STARBUCKS
                else -> SizeModifications.STANDARD
            }
        }
        else -> emptyList()  // Custom modifications for other categories
    }
}