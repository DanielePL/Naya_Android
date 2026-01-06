package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

// ═══════════════════════════════════════════════════════════════
// MACRO QUALITY MODELS
// Scientific quality scoring for protein, carbs, and fats
// ═══════════════════════════════════════════════════════════════

/**
 * Quality level enum for visual indicators (ring colors)
 */
enum class QualityLevel(val score: Float) {
    EXCELLENT(1.0f),   // Green - optimal quality
    GOOD(0.75f),       // Light green
    MODERATE(0.5f),    // Orange - room for improvement
    POOR(0.25f);       // Red - needs attention

    companion object {
        fun fromScore(score: Float): QualityLevel = when {
            score >= 0.85f -> EXCELLENT
            score >= 0.65f -> GOOD
            score >= 0.45f -> MODERATE
            else -> POOR
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// NOVA CLASSIFICATION - Ultra-Processed Food Tracking
// Based on NOVA food classification system (Monteiro et al.)
// ═══════════════════════════════════════════════════════════════

/**
 * NOVA Classification System for food processing levels
 * NOVA 1: Unprocessed or minimally processed foods
 * NOVA 2: Processed culinary ingredients
 * NOVA 3: Processed foods
 * NOVA 4: Ultra-processed food products (UPF)
 */
enum class NovaClassification(val level: Int, val description: String, val healthImpact: String) {
    UNPROCESSED(1, "Unprocessed / Minimal", "Optimal - whole foods as nature intended"),
    CULINARY_INGREDIENT(2, "Culinary Ingredient", "Neutral - used for cooking"),
    PROCESSED(3, "Processed", "Moderate - some additives/preservation"),
    ULTRA_PROCESSED(4, "Ultra-Processed", "Concerning - industrial formulations");

    companion object {
        fun fromLevel(level: Int): NovaClassification = entries.find { it.level == level } ?: PROCESSED
    }
}

/**
 * NOVA classification scores for common foods
 * Research: Ultra-processed foods linked to obesity, heart disease, diabetes, cancer
 * Target: < 20% of daily calories from NOVA 4
 */
object NovaClassificationScores {

    // ─────────────────────────────────────────────────────────────
    // NOVA 1: Unprocessed / Minimally Processed
    // Fresh, frozen, dried, fermented foods without added substances
    // ─────────────────────────────────────────────────────────────
    private val NOVA_1_FOODS = listOf(
        // Proteins
        "egg", "eier", "chicken", "hähnchen", "poulet", "beef", "rind", "steak", "pork", "schwein",
        "fish", "fisch", "salmon", "lachs", "tuna", "thunfisch", "shrimp", "garnelen", "turkey", "pute",
        "lamb", "lamm", "duck", "ente", "venison", "wild",
        // Dairy (unprocessed)
        "milk", "milch", "greek yogurt", "skyr", "quark", "plain yogurt", "naturjoghurt",
        "cottage cheese", "hüttenkäse",
        // Vegetables
        "vegetable", "gemüse", "broccoli", "brokkoli", "spinach", "spinat", "carrot", "karotte", "möhre",
        "tomato", "tomate", "cucumber", "gurke", "pepper", "paprika", "onion", "zwiebel", "garlic", "knoblauch",
        "lettuce", "salat", "kale", "grünkohl", "cabbage", "kohl", "zucchini", "eggplant", "aubergine",
        "asparagus", "spargel", "mushroom", "pilz", "celery", "sellerie", "cauliflower", "blumenkohl",
        "brussels sprout", "rosenkohl", "green bean", "grüne bohne", "pea", "erbse",
        // Fruits
        "apple", "apfel", "banana", "banane", "orange", "berry", "beere", "strawberry", "erdbeere",
        "blueberry", "heidelbeere", "raspberry", "himbeere", "grape", "traube", "watermelon", "wassermelone",
        "mango", "pineapple", "ananas", "peach", "pfirsich", "pear", "birne", "plum", "pflaume",
        "cherry", "kirsche", "kiwi", "avocado", "lemon", "zitrone", "lime", "limette", "grapefruit",
        // Grains (whole, unprocessed)
        "rice", "reis", "oats", "hafer", "quinoa", "buckwheat", "buchweizen", "millet", "hirse",
        "barley", "gerste", "bulgur", "couscous",
        // Legumes
        "lentil", "linsen", "chickpea", "kichererbse", "black bean", "schwarze bohne", "kidney bean",
        "white bean", "weisse bohne", "bean", "bohne", "edamame",
        // Nuts & Seeds (plain)
        "almond", "mandel", "walnut", "walnuss", "cashew", "peanut", "erdnuss", "pistachio", "pistazie",
        "hazelnut", "haselnuss", "macadamia", "pecan", "pekannuss", "brazil nut", "paranuss",
        "sunflower seed", "sonnenblumenkern", "pumpkin seed", "kürbiskern", "chia", "flax", "leinsamen",
        "hemp seed", "hanfsamen", "sesame", "sesam",
        // Other
        "tofu", "tempeh", "seitan", "date", "dattel", "fig", "feige", "raisin", "rosine",
        "dried fruit", "trockenfrüchte", "honey", "honig", "maple syrup", "ahornsirup"
    )

    // ─────────────────────────────────────────────────────────────
    // NOVA 2: Processed Culinary Ingredients
    // Substances extracted from NOVA 1 foods, used in cooking
    // ─────────────────────────────────────────────────────────────
    private val NOVA_2_FOODS = listOf(
        "olive oil", "olivenöl", "coconut oil", "kokosöl", "butter", "ghee",
        "vegetable oil", "pflanzenöl", "sunflower oil", "sonnenblumenöl", "rapeseed oil", "rapsöl",
        "avocado oil", "avocadoöl", "sesame oil", "sesamöl",
        "flour", "mehl", "cornstarch", "maisstärke", "starch", "stärke",
        "sugar", "zucker", "salt", "salz", "vinegar", "essig",
        "cream", "sahne", "heavy cream", "schlagsahne"
    )

    // ─────────────────────────────────────────────────────────────
    // NOVA 3: Processed Foods
    // NOVA 1 foods + salt, oil, sugar, or other NOVA 2 substances
    // ─────────────────────────────────────────────────────────────
    private val NOVA_3_FOODS = listOf(
        // Preserved vegetables/fruits
        "canned", "dose", "pickled", "eingelegt", "preserved",
        // Cheese
        "cheese", "käse", "parmesan", "cheddar", "mozzarella", "feta", "gouda", "brie", "camembert",
        "goat cheese", "ziegenkäse", "cream cheese", "frischkäse",
        // Breads (simple)
        "bread", "brot", "sourdough", "sauerteig", "baguette", "ciabatta", "focaccia",
        "whole grain bread", "vollkornbrot", "rye bread", "roggenbrot",
        // Cured meats (traditional)
        "ham", "schinken", "prosciutto", "bacon", "speck", "smoked salmon", "räucherlachs",
        "smoked fish", "räucherfisch", "jerky",
        // Fermented
        "sauerkraut", "kimchi", "miso", "soy sauce", "sojasosse",
        // Simple preparations
        "hummus", "nut butter", "nussbutter", "peanut butter", "erdnussbutter",
        "almond butter", "mandelbutter", "tahini",
        // Wine/Beer (traditional)
        "wine", "wein", "beer", "bier"
    )

    // ─────────────────────────────────────────────────────────────
    // NOVA 4: Ultra-Processed Foods (UPF)
    // Industrial formulations with 5+ ingredients
    // Contains additives: emulsifiers, preservatives, artificial flavors/colors
    // ─────────────────────────────────────────────────────────────
    private val NOVA_4_FOODS = listOf(
        // Soft drinks & sweetened beverages
        "soda", "cola", "fanta", "sprite", "pepsi", "energy drink", "energydrink",
        "red bull", "monster", "rockstar", "sports drink", "gatorade", "powerade",
        "frappuccino", "sweetened", "lemonade", "eistee", "ice tea", "fruit juice",
        "smoothie", "fruchtsaft", "nektar",
        // Snacks
        "chip", "chips", "crisp", "doritos", "pringles", "cheetos", "popcorn",
        "pretzel", "brezel", "cracker", "rice cake", "reiswaffel",
        // Candy & sweets
        "candy", "süssigkeit", "chocolate bar", "schokoriegel", "gummy", "gummibärchen",
        "haribo", "m&m", "snickers", "mars", "twix", "kit kat", "oreo",
        "cookie", "keks", "biscuit", "brownie", "cake", "kuchen", "pastry", "gebäck",
        "donut", "muffin", "croissant", "danish", "ice cream", "eis", "gelato",
        "frozen yogurt", "pudding", "mousse",
        // Fast food
        "fast food", "mcdonald", "burger king", "wendy", "kfc", "subway",
        "pizza", "nugget", "hot dog", "hotdog", "french fries", "pommes",
        "fried chicken", "wrap", "burrito", "taco",
        // Processed meats
        "sausage", "wurst", "würstchen", "frankfurter", "wiener", "bratwurst",
        "salami", "pepperoni", "mortadella", "bologna", "hot dog", "chorizo",
        "chicken nugget", "fish stick", "fischstäbchen", "deli meat", "aufschnitt",
        // Instant/convenience foods
        "instant", "fertiggericht", "ready meal", "microwave", "mikrowelle",
        "frozen meal", "tiefkühl", "ramen", "cup noodle", "instant noodle",
        "mac and cheese", "mac & cheese", "pizza roll", "burrito bowl",
        // Breakfast cereals (sweetened)
        "cereal", "müsli", "corn flakes", "frosties", "cheerios", "granola bar",
        "protein bar", "riegel", "clif bar", "kind bar", "quest bar",
        // Sauces & condiments (industrial)
        "ketchup", "mayonnaise", "mayo", "bbq sauce", "ranch", "thousand island",
        "salad dressing", "salatdressing", "teriyaki", "sweet chili",
        // Dairy alternatives (processed)
        "oat milk", "hafermilch", "almond milk", "mandelmilch", "soy milk", "sojamilch",
        "coconut milk", "kokosmilch", "plant milk", "pflanzenmilch",
        // Spreads
        "nutella", "margarine", "jam", "marmelade", "jelly", "spread",
        // Protein supplements
        "protein powder", "whey", "casein", "mass gainer", "pre workout", "bcaa",
        "creatine", "supplement",
        // Other UPF
        "flavored", "aromatisiert", "artificially", "künstlich", "diet", "light",
        "sugar-free", "zuckerfrei", "low-fat", "fettarm", "fat-free", "fettfrei"
    )

    /**
     * Get NOVA classification for a food item
     */
    fun getClassification(foodName: String): NovaClassification {
        val name = foodName.lowercase()

        // Check NOVA 4 first (most restrictive)
        if (NOVA_4_FOODS.any { name.contains(it) }) {
            return NovaClassification.ULTRA_PROCESSED
        }

        // Check NOVA 3
        if (NOVA_3_FOODS.any { name.contains(it) }) {
            return NovaClassification.PROCESSED
        }

        // Check NOVA 2
        if (NOVA_2_FOODS.any { name.contains(it) }) {
            return NovaClassification.CULINARY_INGREDIENT
        }

        // Check NOVA 1
        if (NOVA_1_FOODS.any { name.contains(it) }) {
            return NovaClassification.UNPROCESSED
        }

        // Default to processed for unknown foods
        return NovaClassification.PROCESSED
    }

    /**
     * Get a numerical score (1.0 = best, 0.0 = worst)
     */
    fun getScore(foodName: String): Float {
        return when (getClassification(foodName)) {
            NovaClassification.UNPROCESSED -> 1.0f
            NovaClassification.CULINARY_INGREDIENT -> 0.85f
            NovaClassification.PROCESSED -> 0.5f
            NovaClassification.ULTRA_PROCESSED -> 0.15f
        }
    }
}

/**
 * Processed food score for a meal or day
 */
data class ProcessedFoodScore(
    val novaDistribution: Map<NovaClassification, Float>,  // Calories per category
    val ultraProcessedCalories: Float,                      // NOVA 4 calories
    val totalCalories: Float,
    val ultraProcessedPercentage: Float,                    // Target: < 20%
    val weightedNovaScore: Float,                           // 0.0 - 1.0 (higher = less processed)
    val qualityLevel: QualityLevel,
    val worstOffenders: List<String>,                       // UPF foods to reduce
    val suggestion: String?
) {
    companion object {
        const val TARGET_UPF_PERCENT = 20f       // Ideal: < 20% from ultra-processed
        const val WARNING_UPF_PERCENT = 30f     // Warning threshold
        const val CRITICAL_UPF_PERCENT = 50f    // Critical - major health concern
    }
}

// ═══════════════════════════════════════════════════════════════
// PROTEIN QUALITY - Based on DIAAS (Digestible Indispensable Amino Acid Score)
// ═══════════════════════════════════════════════════════════════

/**
 * DIAAS scores for common protein sources
 * Based on FAO 2013 recommendations and recent research
 * Score > 1.0 = excellent, 0.75-1.0 = good, < 0.75 = incomplete
 */
object ProteinDiaasScores {
    // Animal proteins (high bioavailability)
    const val WHEY_ISOLATE = 1.09f
    const val WHOLE_EGG = 1.13f
    const val EGG_WHITE = 1.08f
    const val BEEF = 1.01f
    const val CHICKEN_BREAST = 1.08f
    const val FISH_GENERAL = 1.00f
    const val MILK = 1.14f
    const val CASEIN = 1.00f
    const val GREEK_YOGURT = 1.08f

    // Plant proteins (often limiting in lysine/methionine)
    const val SOY_ISOLATE = 0.90f
    const val PEA_PROTEIN = 0.82f
    const val RICE_PROTEIN = 0.60f
    const val HEMP_PROTEIN = 0.63f
    const val BEANS_GENERAL = 0.65f
    const val LENTILS = 0.70f
    const val CHICKPEAS = 0.68f
    const val TOFU = 0.85f
    const val TEMPEH = 0.88f

    // Grains (low, limiting in lysine)
    const val WHEAT = 0.45f
    const val RICE = 0.60f
    const val OATS = 0.54f
    const val QUINOA = 0.81f  // Complete amino acid profile

    // Mixed/processed
    const val BREAD = 0.40f
    const val PASTA = 0.45f
    const val PIZZA = 0.55f
    const val BURGER_PATTY = 0.95f

    // Default for unknown foods
    const val DEFAULT = 0.70f

    /**
     * Get DIAAS score for a food item based on name matching
     */
    fun getScore(foodName: String): Float {
        val name = foodName.lowercase()
        return when {
            // High quality proteins
            name.contains("whey") -> WHEY_ISOLATE
            name.contains("egg") && name.contains("white") -> EGG_WHITE
            name.contains("egg") -> WHOLE_EGG
            name.contains("chicken") || name.contains("hähnchen") || name.contains("poulet") -> CHICKEN_BREAST
            name.contains("beef") || name.contains("rind") || name.contains("steak") -> BEEF
            name.contains("fish") || name.contains("fisch") || name.contains("salmon") ||
                name.contains("lachs") || name.contains("tuna") || name.contains("thunfisch") -> FISH_GENERAL
            name.contains("greek yogurt") || name.contains("skyr") || name.contains("quark") -> GREEK_YOGURT
            name.contains("milk") || name.contains("milch") -> MILK
            name.contains("casein") -> CASEIN

            // Plant proteins
            name.contains("soy") || name.contains("soja") -> SOY_ISOLATE
            name.contains("tofu") -> TOFU
            name.contains("tempeh") -> TEMPEH
            name.contains("pea protein") || name.contains("erbsenprotein") -> PEA_PROTEIN
            name.contains("rice protein") || name.contains("reisprotein") -> RICE_PROTEIN
            name.contains("hemp") || name.contains("hanf") -> HEMP_PROTEIN
            name.contains("lentil") || name.contains("linsen") -> LENTILS
            name.contains("chickpea") || name.contains("kichererbse") -> CHICKPEAS
            name.contains("bean") || name.contains("bohne") -> BEANS_GENERAL
            name.contains("quinoa") -> QUINOA

            // Grains
            name.contains("bread") || name.contains("brot") -> BREAD
            name.contains("pasta") || name.contains("nudel") || name.contains("spaghetti") -> PASTA
            name.contains("rice") || name.contains("reis") -> RICE
            name.contains("oat") || name.contains("hafer") -> OATS
            name.contains("wheat") || name.contains("weizen") -> WHEAT

            // Mixed
            name.contains("pizza") -> PIZZA
            name.contains("burger") -> BURGER_PATTY

            else -> DEFAULT
        }
    }
}

/**
 * Essential Amino Acid profile for detailed analysis
 */
data class EssentialAminoAcids(
    val leucine: Float = 0f,      // Key for muscle protein synthesis
    val isoleucine: Float = 0f,
    val valine: Float = 0f,
    val lysine: Float = 0f,       // Often limiting in plant proteins
    val methionine: Float = 0f,   // Often limiting in legumes
    val phenylalanine: Float = 0f,
    val threonine: Float = 0f,
    val tryptophan: Float = 0f,
    val histidine: Float = 0f
) {
    val totalBCAAs: Float get() = leucine + isoleucine + valine

    // Check if any essential amino acid is limiting
    val limitingAminoAcid: String?
        get() {
            // Simplified check - real implementation would compare to reference pattern
            val min = listOf(
                "Leucine" to leucine,
                "Lysine" to lysine,
                "Methionine" to methionine
            ).minByOrNull { it.second }
            return if ((min?.second ?: 0f) < 20f) min?.first else null
        }
}

/**
 * Aggregated protein quality for a meal or day
 */
data class ProteinQualityScore(
    val weightedDiaasScore: Float,      // 0.0 - 1.5 (weighted by protein grams)
    val qualityLevel: QualityLevel,
    val totalProteinGrams: Float,
    val highQualityProteinGrams: Float, // DIAAS >= 0.9
    val limitingFactors: List<String>,  // e.g., "Low leucine", "Incomplete amino acids"
    val suggestion: String?             // e.g., "Add dairy to complete amino acid profile"
) {
    val highQualityPercentage: Float
        get() = if (totalProteinGrams > 0) (highQualityProteinGrams / totalProteinGrams) * 100 else 0f
}

// ═══════════════════════════════════════════════════════════════
// CARB QUALITY - Based on Glycemic Index/Load
// ═══════════════════════════════════════════════════════════════

/**
 * Glycemic Index categories
 * Low GI = slow energy release, better insulin sensitivity
 * High GI = fast spike, can lead to crashes
 */
object GlycemicIndexScores {
    // Low GI (< 55) - Green
    const val VEGETABLES = 15
    const val LEGUMES = 30
    const val MOST_FRUITS = 40
    const val OATS = 55
    const val WHOLE_GRAIN_BREAD = 50
    const val SWEET_POTATO = 54
    const val QUINOA = 53
    const val BROWN_RICE = 50

    // Medium GI (56-69) - Orange
    const val BANANA_RIPE = 62
    const val WHITE_RICE = 65
    const val WHOLE_WHEAT_BREAD = 69
    const val HONEY = 61

    // High GI (> 70) - Red
    const val WHITE_BREAD = 75
    const val WHITE_POTATO = 78
    const val RICE_CAKES = 82
    const val CORN_FLAKES = 81
    const val GLUCOSE = 100
    const val SUGAR = 65
    const val SPORTS_DRINK = 78
    const val CANDY = 80

    // Default
    const val DEFAULT = 55

    fun getGI(foodName: String): Int {
        val name = foodName.lowercase()
        return when {
            // Low GI
            name.contains("vegetable") || name.contains("gemüse") || name.contains("salad") ||
                name.contains("salat") || name.contains("broccoli") || name.contains("spinach") -> VEGETABLES
            name.contains("lentil") || name.contains("linsen") || name.contains("bean") ||
                name.contains("bohne") || name.contains("chickpea") -> LEGUMES
            name.contains("apple") || name.contains("apfel") || name.contains("berry") ||
                name.contains("beere") || name.contains("orange") -> MOST_FRUITS
            name.contains("oat") || name.contains("hafer") -> OATS
            name.contains("quinoa") -> QUINOA
            name.contains("sweet potato") || name.contains("süsskartoffel") -> SWEET_POTATO
            name.contains("brown rice") || name.contains("vollkornreis") -> BROWN_RICE

            // Medium GI
            name.contains("banana") || name.contains("banane") -> BANANA_RIPE
            name.contains("honey") || name.contains("honig") -> HONEY
            name.contains("whole wheat") || name.contains("vollkorn") -> WHOLE_WHEAT_BREAD

            // High GI
            name.contains("white bread") || name.contains("weissbrot") || name.contains("toast") -> WHITE_BREAD
            name.contains("potato") || name.contains("kartoffel") -> WHITE_POTATO
            name.contains("rice cake") || name.contains("reiswaffel") -> RICE_CAKES
            name.contains("corn flakes") || name.contains("cornflakes") -> CORN_FLAKES
            name.contains("candy") || name.contains("süssigkeit") || name.contains("gummy") -> CANDY
            name.contains("sugar") || name.contains("zucker") -> SUGAR
            name.contains("sports drink") || name.contains("gatorade") || name.contains("powerade") -> SPORTS_DRINK
            name.contains("white rice") || (name.contains("rice") && !name.contains("brown")) -> WHITE_RICE

            else -> DEFAULT
        }
    }
}

/**
 * Carb quality score for a meal or day
 */
data class CarbQualityScore(
    val averageGI: Float,               // Weighted by carb grams
    val totalGlycemicLoad: Float,       // GI * carbs / 100
    val qualityLevel: QualityLevel,
    val fiberToSugarRatio: Float,       // Higher = better
    val complexCarbPercentage: Float,   // Low GI carbs / total carbs
    val suggestion: String?
) {
    companion object {
        // GL thresholds per meal
        const val LOW_GL = 10f
        const val MEDIUM_GL = 19f
        // Daily GL target
        const val DAILY_GL_TARGET = 100f
    }
}

// ═══════════════════════════════════════════════════════════════
// FAT QUALITY - Based on fatty acid composition
// ═══════════════════════════════════════════════════════════════

/**
 * Fat quality score for a meal or day
 */
data class FatQualityScore(
    val omega6to3Ratio: Float,          // Target: 1:1 to 4:1
    val transFatGrams: Float,           // Should be 0!
    val saturatedFatPercentage: Float,  // % of total fat
    val unsaturatedFatPercentage: Float,
    val qualityLevel: QualityLevel,
    val hasTransFat: Boolean,
    val suggestion: String?
) {
    companion object {
        const val OPTIMAL_OMEGA_RATIO = 4f      // Max 4:1
        const val WARNING_OMEGA_RATIO = 10f     // Concerning
        const val MAX_SATURATED_PERCENT = 30f   // < 30% of total fat
    }
}

// ═══════════════════════════════════════════════════════════════
// ANABOLIC WINDOW TRACKING
// ═══════════════════════════════════════════════════════════════

/**
 * Anabolic window state for post-workout nutrition tracking
 */
@Serializable
data class AnabolicWindow(
    @SerialName("workout_end_time") val workoutEndTimeMillis: Long,
    @SerialName("was_fasted_workout") val wasFastedWorkout: Boolean = false,
    @SerialName("protein_intake_since") val proteinIntakeSince: Float = 0f,
    @SerialName("carb_intake_since") val carbIntakeSince: Float = 0f,
    @SerialName("workout_type") val workoutType: String? = null,
    @SerialName("workout_intensity") val workoutIntensity: Float = 0.7f  // 0-1, affects window importance
) {
    // Window duration: 30 min if fasted, 2h standard, up to 5-6h with diminishing returns
    val windowDurationMinutes: Int
        get() = if (wasFastedWorkout) 30 else 120

    val extendedWindowMinutes: Int = 360  // 6 hours total opportunity

    fun getMinutesRemaining(): Long {
        val elapsedMs = System.currentTimeMillis() - workoutEndTimeMillis
        val elapsedMinutes = elapsedMs / 60000
        return (windowDurationMinutes - elapsedMinutes).coerceAtLeast(0)
    }

    fun getExtendedMinutesRemaining(): Long {
        val elapsedMs = System.currentTimeMillis() - workoutEndTimeMillis
        val elapsedMinutes = elapsedMs / 60000
        return (extendedWindowMinutes - elapsedMinutes).coerceAtLeast(0)
    }

    fun isWindowActive(): Boolean = getMinutesRemaining() > 0

    fun isExtendedWindowActive(): Boolean = getExtendedMinutesRemaining() > 0

    // Progress through the window (1.0 = just started, 0.0 = window closed)
    fun getWindowProgress(): Float {
        val remaining = getMinutesRemaining()
        return (remaining.toFloat() / windowDurationMinutes).coerceIn(0f, 1f)
    }

    // Recommended protein based on lean body mass estimate
    fun getRecommendedProtein(bodyWeightKg: Float, leanMassPercent: Float = 0.8f): Float {
        val leanMass = bodyWeightKg * leanMassPercent
        return leanMass * 0.4f  // 0.4g per kg lean mass
    }

    // Check if protein target is met
    fun isProteinTargetMet(bodyWeightKg: Float): Boolean {
        return proteinIntakeSince >= getRecommendedProtein(bodyWeightKg)
    }

    // Get urgency level for notifications
    fun getUrgency(): AnabolicWindowUrgency {
        val remaining = getMinutesRemaining()
        val progress = proteinIntakeSince

        return when {
            !isWindowActive() && progress < 20f -> AnabolicWindowUrgency.MISSED
            !isWindowActive() -> AnabolicWindowUrgency.CLOSED
            remaining <= 10 && progress < 20f -> AnabolicWindowUrgency.CRITICAL
            remaining <= 30 && progress < 20f -> AnabolicWindowUrgency.HIGH
            progress < 20f -> AnabolicWindowUrgency.MODERATE
            else -> AnabolicWindowUrgency.SATISFIED
        }
    }
}

enum class AnabolicWindowUrgency {
    SATISFIED,   // Got enough protein
    MODERATE,    // Window open, time remaining
    HIGH,        // Window closing soon, no protein yet
    CRITICAL,    // < 10 min left, no protein
    CLOSED,      // Window closed but got some protein
    MISSED       // Window closed, no protein intake
}

// ═══════════════════════════════════════════════════════════════
// AGGREGATED MACRO QUALITY FOR UI
// ═══════════════════════════════════════════════════════════════

/**
 * Complete macro quality summary for the carousel UI
 */
data class MacroQualitySummary(
    val protein: ProteinQualityScore,
    val carbs: CarbQualityScore,
    val fats: FatQualityScore,
    val processedFood: ProcessedFoodScore,  // NEW: NOVA classification
    val anabolicWindow: AnabolicWindow?,

    // Overall daily score (weighted average)
    val overallQualityScore: Float,
    val overallQualityLevel: QualityLevel
) {
    companion object {
        fun calculate(
            nutritionLog: NutritionLog,
            anabolicWindow: AnabolicWindow? = null
        ): MacroQualitySummary {
            val proteinScore = calculateProteinQuality(nutritionLog)
            val carbScore = calculateCarbQuality(nutritionLog)
            val fatScore = calculateFatQuality(nutritionLog)
            val processedScore = calculateProcessedFoodScore(nutritionLog)

            // Weighted overall: Protein 30%, Carbs 20%, Fats 20%, Processing 30%
            // Processing level now has significant weight (research shows UPF is a major health factor)
            val overall = (proteinScore.weightedDiaasScore * 0.3f +
                          (1f - (carbScore.averageGI / 100f)) * 0.2f +
                          fatScore.unsaturatedFatPercentage / 100f * 0.2f +
                          processedScore.weightedNovaScore * 0.3f)
                .coerceIn(0f, 1f)

            return MacroQualitySummary(
                protein = proteinScore,
                carbs = carbScore,
                fats = fatScore,
                processedFood = processedScore,
                anabolicWindow = anabolicWindow,
                overallQualityScore = overall,
                overallQualityLevel = QualityLevel.fromScore(overall)
            )
        }

        private fun calculateProteinQuality(log: NutritionLog): ProteinQualityScore {
            var totalWeightedDiaas = 0f
            var totalProtein = 0f
            var highQualityProtein = 0f

            log.meals.forEach { meal ->
                meal.items.forEach { item ->
                    val diaas = ProteinDiaasScores.getScore(item.itemName)
                    val protein = item.protein

                    totalWeightedDiaas += diaas * protein
                    totalProtein += protein

                    if (diaas >= 0.9f) {
                        highQualityProtein += protein
                    }
                }
            }

            val weightedScore = if (totalProtein > 0) totalWeightedDiaas / totalProtein else 0.7f
            val qualityLevel = QualityLevel.fromScore(weightedScore)

            val limitingFactors = mutableListOf<String>()
            val suggestion: String?

            when {
                weightedScore < 0.6f -> {
                    limitingFactors.add("Low bioavailability")
                    limitingFactors.add("Incomplete amino acids")
                    suggestion = "Add animal protein or combine legumes with grains"
                }
                weightedScore < 0.8f -> {
                    limitingFactors.add("Moderate bioavailability")
                    suggestion = "Consider adding eggs, dairy, or quality protein powder"
                }
                highQualityProtein / totalProtein < 0.5f -> {
                    suggestion = "Good mix! Consider more complete proteins for optimal MPS"
                }
                else -> {
                    suggestion = null
                }
            }

            return ProteinQualityScore(
                weightedDiaasScore = weightedScore,
                qualityLevel = qualityLevel,
                totalProteinGrams = totalProtein,
                highQualityProteinGrams = highQualityProtein,
                limitingFactors = limitingFactors,
                suggestion = suggestion
            )
        }

        private fun calculateCarbQuality(log: NutritionLog): CarbQualityScore {
            var totalWeightedGI = 0f
            var totalCarbs = 0f
            var lowGICarbs = 0f
            var totalGL = 0f

            log.meals.forEach { meal ->
                meal.items.forEach { item ->
                    val gi = GlycemicIndexScores.getGI(item.itemName)
                    val carbs = item.carbs

                    totalWeightedGI += gi * carbs
                    totalCarbs += carbs
                    totalGL += (gi * carbs) / 100f

                    if (gi < 55) {
                        lowGICarbs += carbs
                    }
                }
            }

            val avgGI = if (totalCarbs > 0) totalWeightedGI / totalCarbs else 55f
            val complexCarbPercent = if (totalCarbs > 0) (lowGICarbs / totalCarbs) * 100 else 50f

            val fiberToSugar = if (log.totalSugar > 0) log.totalFiber / log.totalSugar else 1f

            // Quality based on GI and fiber/sugar ratio
            val qualityScore = ((100f - avgGI) / 100f * 0.5f +
                               complexCarbPercent / 100f * 0.3f +
                               fiberToSugar.coerceAtMost(2f) / 2f * 0.2f)

            val qualityLevel = QualityLevel.fromScore(qualityScore)

            val suggestion = when {
                avgGI > 70 -> "High glycemic load - add fiber-rich foods to slow absorption"
                avgGI > 60 && fiberToSugar < 0.5f -> "Increase fiber intake for better blood sugar control"
                fiberToSugar < 0.3f -> "Too much sugar relative to fiber"
                else -> null
            }

            return CarbQualityScore(
                averageGI = avgGI,
                totalGlycemicLoad = totalGL,
                qualityLevel = qualityLevel,
                fiberToSugarRatio = fiberToSugar,
                complexCarbPercentage = complexCarbPercent,
                suggestion = suggestion
            )
        }

        private fun calculateFatQuality(log: NutritionLog): FatQualityScore {
            val totalFat = log.totalFat
            val saturated = log.totalSaturatedFat
            val unsaturated = log.totalUnsaturatedFat
            val transFat = log.totalTransFat
            val omega3 = log.totalOmega3
            val omega6 = log.totalOmega6

            val saturatedPercent = if (totalFat > 0) (saturated / totalFat) * 100 else 0f
            val unsaturatedPercent = if (totalFat > 0) (unsaturated / totalFat) * 100 else 50f
            val omega6to3 = if (omega3 > 0) omega6 / omega3 else 15f  // Western diet default

            // Quality score
            val ratioScore = (1f - (omega6to3 / 20f).coerceAtMost(1f))
            val transScore = if (transFat > 0) 0f else 1f
            val satScore = (1f - (saturatedPercent / 50f).coerceAtMost(1f))

            val qualityScore = (ratioScore * 0.4f + transScore * 0.3f + satScore * 0.3f)
            val qualityLevel = QualityLevel.fromScore(qualityScore)

            val suggestion = when {
                transFat > 0 -> "⚠️ Trans fats detected - avoid these completely!"
                omega6to3 > 10 -> "Omega ratio too high - add fatty fish, walnuts, or flaxseed"
                saturatedPercent > 35 -> "High saturated fat - balance with olive oil, avocado, nuts"
                omega6to3 > 6 -> "Consider more omega-3 sources"
                else -> null
            }

            return FatQualityScore(
                omega6to3Ratio = omega6to3,
                transFatGrams = transFat,
                saturatedFatPercentage = saturatedPercent,
                unsaturatedFatPercentage = unsaturatedPercent,
                qualityLevel = qualityLevel,
                hasTransFat = transFat > 0,
                suggestion = suggestion
            )
        }

        /**
         * Calculate processed food score using NOVA classification
         */
        private fun calculateProcessedFoodScore(log: NutritionLog): ProcessedFoodScore {
            val novaCalories = mutableMapOf<NovaClassification, Float>()
            NovaClassification.entries.forEach { novaCalories[it] = 0f }

            val ultraProcessedFoods = mutableListOf<Pair<String, Float>>()  // name to calories
            var totalCalories = 0f

            log.meals.forEach { meal ->
                meal.items.forEach { item ->
                    val classification = NovaClassificationScores.getClassification(item.itemName)
                    val calories = item.calories

                    novaCalories[classification] = (novaCalories[classification] ?: 0f) + calories
                    totalCalories += calories

                    // Track ultra-processed foods
                    if (classification == NovaClassification.ULTRA_PROCESSED && calories > 0) {
                        ultraProcessedFoods.add(item.itemName to calories)
                    }
                }
            }

            val upfCalories = novaCalories[NovaClassification.ULTRA_PROCESSED] ?: 0f
            val upfPercentage = if (totalCalories > 0) (upfCalories / totalCalories) * 100 else 0f

            // Calculate weighted NOVA score (higher = less processed = better)
            var weightedNovaScore = 0f
            if (totalCalories > 0) {
                novaCalories.forEach { (classification, calories) ->
                    val score = when (classification) {
                        NovaClassification.UNPROCESSED -> 1.0f
                        NovaClassification.CULINARY_INGREDIENT -> 0.85f
                        NovaClassification.PROCESSED -> 0.5f
                        NovaClassification.ULTRA_PROCESSED -> 0.15f
                    }
                    weightedNovaScore += score * (calories / totalCalories)
                }
            } else {
                weightedNovaScore = 0.7f  // Default
            }

            // Quality level based on UPF percentage
            val qualityLevel = when {
                upfPercentage < ProcessedFoodScore.TARGET_UPF_PERCENT -> QualityLevel.EXCELLENT
                upfPercentage < ProcessedFoodScore.WARNING_UPF_PERCENT -> QualityLevel.GOOD
                upfPercentage < ProcessedFoodScore.CRITICAL_UPF_PERCENT -> QualityLevel.MODERATE
                else -> QualityLevel.POOR
            }

            // Get worst offenders (top 3 UPF by calories)
            val worstOffenders = ultraProcessedFoods
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }

            // Generate suggestion
            val suggestion = when {
                upfPercentage >= ProcessedFoodScore.CRITICAL_UPF_PERCENT -> {
                    "⚠️ Over half your calories are ultra-processed. Prioritize whole foods!"
                }
                upfPercentage >= ProcessedFoodScore.WARNING_UPF_PERCENT -> {
                    if (worstOffenders.isNotEmpty()) {
                        "Try replacing ${worstOffenders.first()} with a whole food alternative"
                    } else {
                        "Consider replacing some processed items with whole foods"
                    }
                }
                upfPercentage >= ProcessedFoodScore.TARGET_UPF_PERCENT -> {
                    "Good progress! A few more swaps could optimize your diet further"
                }
                else -> null
            }

            return ProcessedFoodScore(
                novaDistribution = novaCalories.toMap(),
                ultraProcessedCalories = upfCalories,
                totalCalories = totalCalories,
                ultraProcessedPercentage = upfPercentage,
                weightedNovaScore = weightedNovaScore,
                qualityLevel = qualityLevel,
                worstOffenders = worstOffenders,
                suggestion = suggestion
            )
        }
    }
}