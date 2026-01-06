package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════
// DIETARY PREFERENCE MODELS
// User's dietary choices for personalized nutrition tips
// ═══════════════════════════════════════════════════════════════

/**
 * User's primary dietary preference
 */
@Serializable
enum class DietaryPreference(
    val displayName: String,
    val description: String,
    val emoji: String
) {
    @SerialName("omnivore")
    OMNIVORE(
        displayName = "Omnivore",
        description = "I eat everything",
        emoji = "🍖"
    ),

    @SerialName("pescatarian")
    PESCATARIAN(
        displayName = "Pescatarian",
        description = "No meat, but fish & seafood",
        emoji = "🐟"
    ),

    @SerialName("vegetarian")
    VEGETARIAN(
        displayName = "Vegetarian",
        description = "No meat or fish",
        emoji = "🥗"
    ),

    @SerialName("vegan")
    VEGAN(
        displayName = "Vegan",
        description = "Plant-based only",
        emoji = "🌱"
    ),

    @SerialName("keto")
    KETO(
        displayName = "Keto / Low Carb",
        description = "High fat, low carb",
        emoji = "🥑"
    ),

    @SerialName("halal")
    HALAL(
        displayName = "Halal",
        description = "Halal dietary laws",
        emoji = "☪️"
    ),

    @SerialName("kosher")
    KOSHER(
        displayName = "Kosher",
        description = "Jewish dietary laws",
        emoji = "✡️"
    );

    /**
     * Check if a food category is allowed for this diet
     */
    fun allowsCategory(category: FoodCategory): Boolean {
        return when (this) {
            OMNIVORE -> true
            PESCATARIAN -> category !in listOf(FoodCategory.RED_MEAT, FoodCategory.POULTRY)
            VEGETARIAN -> category !in listOf(FoodCategory.RED_MEAT, FoodCategory.POULTRY, FoodCategory.FISH, FoodCategory.SHELLFISH)
            VEGAN -> category in listOf(
                FoodCategory.VEGETABLES, FoodCategory.FRUITS, FoodCategory.LEGUMES,
                FoodCategory.GRAINS, FoodCategory.NUTS, FoodCategory.SEEDS,
                FoodCategory.PLANT_PROTEIN, FoodCategory.PLANT_OILS
            )
            KETO -> category !in listOf(FoodCategory.GRAINS, FoodCategory.HIGH_SUGAR_FRUITS, FoodCategory.LEGUMES)
            HALAL -> category != FoodCategory.PORK
            KOSHER -> category !in listOf(FoodCategory.PORK, FoodCategory.SHELLFISH)
        }
    }
}

/**
 * Food categories for dietary filtering
 */
enum class FoodCategory {
    // Animal proteins
    RED_MEAT,           // Beef, lamb, pork
    POULTRY,            // Chicken, turkey, duck
    PORK,               // Specifically pork (for halal/kosher)
    FISH,               // All fish
    SHELLFISH,          // Shrimp, crab, lobster
    EGGS,
    DAIRY,

    // Plant-based
    VEGETABLES,
    FRUITS,
    HIGH_SUGAR_FRUITS,  // Banana, grapes, mango (for keto)
    LEGUMES,            // Beans, lentils, chickpeas
    GRAINS,
    NUTS,
    SEEDS,
    PLANT_PROTEIN,      // Tofu, tempeh, seitan
    PLANT_OILS,

    // Other
    PROCESSED,
    SUPPLEMENTS
}

/**
 * Common food allergies
 */
@Serializable
enum class FoodAllergy(
    val displayName: String,
    val description: String,
    val emoji: String
) {
    @SerialName("dairy")
    DAIRY("Dairy", "Milk, cheese, yogurt, butter", "🥛"),

    @SerialName("gluten")
    GLUTEN("Gluten", "Wheat, barley, rye", "🌾"),

    @SerialName("nuts")
    NUTS("Tree Nuts", "Almonds, walnuts, cashews, etc.", "🥜"),

    @SerialName("peanuts")
    PEANUTS("Peanuts", "Peanuts and peanut products", "🥜"),

    @SerialName("soy")
    SOY("Soy", "Tofu, tempeh, soy sauce, edamame", "🫘"),

    @SerialName("eggs")
    EGGS("Eggs", "Eggs and egg products", "🥚"),

    @SerialName("shellfish")
    SHELLFISH("Shellfish", "Shrimp, crab, lobster, mussels", "🦐"),

    @SerialName("fish")
    FISH("Fish", "All fish species", "🐟"),

    @SerialName("sesame")
    SESAME("Sesame", "Sesame seeds, tahini, sesame oil", "🫘"),

    @SerialName("sulfites")
    SULFITES("Sulfites", "Wine, dried fruits, some preservatives", "🍷");

    /**
     * Foods to avoid for this allergy
     */
    fun getFoodsToAvoid(): List<String> {
        return when (this) {
            DAIRY -> listOf("milk", "cheese", "yogurt", "butter", "cream", "whey", "casein", "ghee", "quark", "skyr")
            GLUTEN -> listOf("wheat", "bread", "pasta", "flour", "barley", "rye", "couscous", "bulgur", "seitan")
            NUTS -> listOf("almond", "walnut", "cashew", "pistachio", "hazelnut", "pecan", "macadamia", "brazil nut")
            PEANUTS -> listOf("peanut", "erdnuss")
            SOY -> listOf("soy", "tofu", "tempeh", "edamame", "miso", "soja")
            EGGS -> listOf("egg", "eier", "mayonnaise", "mayo")
            SHELLFISH -> listOf("shrimp", "crab", "lobster", "mussel", "clam", "oyster", "garnele", "krabbe")
            FISH -> listOf("fish", "salmon", "tuna", "cod", "fisch", "lachs", "thunfisch")
            SESAME -> listOf("sesame", "tahini", "sesam")
            SULFITES -> listOf("wine", "dried fruit", "wein")
        }
    }
}

/**
 * Complete dietary profile for a user
 */
@Serializable
data class DietaryProfile(
    @SerialName("preferences")
    val preferences: List<DietaryPreference> = listOf(DietaryPreference.OMNIVORE),

    @SerialName("allergies")
    val allergies: List<FoodAllergy> = emptyList(),

    @SerialName("dislikes")
    val dislikes: List<String> = emptyList(),  // Free-form foods user doesn't like

    @SerialName("preferences_set_at")
    val preferencesSetAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if the user has a specific dietary preference
     */
    fun hasPreference(preference: DietaryPreference): Boolean {
        return preference in preferences
    }

    /**
     * Check if the user is following a vegan diet
     */
    val isVegan: Boolean get() = hasPreference(DietaryPreference.VEGAN)

    /**
     * Check if the user is following a vegetarian diet
     */
    val isVegetarian: Boolean get() = hasPreference(DietaryPreference.VEGETARIAN) || isVegan

    /**
     * Check if the user is following a keto diet
     */
    val isKeto: Boolean get() = hasPreference(DietaryPreference.KETO)

    /**
     * Check if the user is following halal dietary laws
     */
    val isHalal: Boolean get() = hasPreference(DietaryPreference.HALAL)

    /**
     * Check if the user is following kosher dietary laws
     */
    val isKosher: Boolean get() = hasPreference(DietaryPreference.KOSHER)

    /**
     * Check if a food is allowed based on diet preferences and allergies.
     * Food must be allowed by ALL selected preferences (e.g., Vegan + Halal = must be both)
     */
    fun isFoodAllowed(foodName: String): Boolean {
        val name = foodName.lowercase()

        // Check allergies first
        allergies.forEach { allergy ->
            if (allergy.getFoodsToAvoid().any { name.contains(it) }) {
                return false
            }
        }

        // Check ALL dietary preferences - food must be allowed by each one
        val category = categorizeFood(name)
        val effectivePreferences = preferences.ifEmpty { listOf(DietaryPreference.OMNIVORE) }
        if (effectivePreferences.any { !it.allowsCategory(category) }) {
            return false
        }

        // Check dislikes
        if (dislikes.any { name.contains(it.lowercase()) }) {
            return false
        }

        return true
    }

    /**
     * Categorize a food item for dietary filtering
     */
    private fun categorizeFood(foodName: String): FoodCategory {
        val name = foodName.lowercase()
        return when {
            // Red meat
            name.contains("beef") || name.contains("steak") || name.contains("rind") ||
            name.contains("lamb") || name.contains("lamm") -> FoodCategory.RED_MEAT

            // Pork specifically
            name.contains("pork") || name.contains("schwein") || name.contains("bacon") ||
            name.contains("ham") || name.contains("schinken") || name.contains("sausage") ||
            name.contains("wurst") -> FoodCategory.PORK

            // Poultry
            name.contains("chicken") || name.contains("hähnchen") || name.contains("turkey") ||
            name.contains("pute") || name.contains("duck") || name.contains("ente") -> FoodCategory.POULTRY

            // Shellfish
            name.contains("shrimp") || name.contains("crab") || name.contains("lobster") ||
            name.contains("mussel") || name.contains("garnele") -> FoodCategory.SHELLFISH

            // Fish
            name.contains("fish") || name.contains("fisch") || name.contains("salmon") ||
            name.contains("tuna") || name.contains("cod") -> FoodCategory.FISH

            // Eggs
            name.contains("egg") || name.contains("eier") -> FoodCategory.EGGS

            // Dairy
            name.contains("milk") || name.contains("milch") || name.contains("cheese") ||
            name.contains("käse") || name.contains("yogurt") || name.contains("joghurt") ||
            name.contains("whey") || name.contains("casein") -> FoodCategory.DAIRY

            // Legumes
            name.contains("bean") || name.contains("bohne") || name.contains("lentil") ||
            name.contains("linse") || name.contains("chickpea") -> FoodCategory.LEGUMES

            // Grains
            name.contains("rice") || name.contains("reis") || name.contains("bread") ||
            name.contains("brot") || name.contains("pasta") || name.contains("oat") -> FoodCategory.GRAINS

            // Plant protein
            name.contains("tofu") || name.contains("tempeh") || name.contains("seitan") -> FoodCategory.PLANT_PROTEIN

            // Nuts
            name.contains("almond") || name.contains("walnut") || name.contains("cashew") ||
            name.contains("nut") && !name.contains("donut") -> FoodCategory.NUTS

            // Seeds
            name.contains("seed") || name.contains("chia") || name.contains("flax") -> FoodCategory.SEEDS

            // Fruits
            name.contains("fruit") || name.contains("apple") || name.contains("berry") ||
            name.contains("orange") -> FoodCategory.FRUITS

            // Vegetables (default for unprocessed plant foods)
            name.contains("vegetable") || name.contains("broccoli") || name.contains("spinach") ||
            name.contains("salad") -> FoodCategory.VEGETABLES

            else -> FoodCategory.PROCESSED
        }
    }

    companion object {
        val DEFAULT = DietaryProfile()
    }
}