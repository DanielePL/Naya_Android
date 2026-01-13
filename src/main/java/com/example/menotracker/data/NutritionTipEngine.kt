package com.example.menotracker.data

import com.example.menotracker.data.models.*

// ═══════════════════════════════════════════════════════════════
// NUTRITION TIP ENGINE
// Generates personalized, actionable nutrition tips
// Based on user's dietary preferences and current macro quality
// ═══════════════════════════════════════════════════════════════

/**
 * Actionable nutrition tip with specific swap suggestions
 */
data class NutritionTip(
    val category: TipCategory,
    val priority: TipPriority,
    val headline: String,           // Short attention grabber
    val explanation: String,        // Why this matters
    val quickWins: List<QuickWin>,  // Specific actions to take
    val impact: String?             // Expected improvement
)

data class QuickWin(
    val action: String,             // e.g., "Add 2 eggs at breakfast"
    val benefit: String,            // e.g., "+0.12 DIAAS"
    val swap: FoodSwap? = null      // Optional swap suggestion
)

data class FoodSwap(
    val from: String,
    val to: String,
    val benefit: String,
    val calorieImpact: Int? = null  // Positive = more cals, negative = fewer
)

enum class TipCategory {
    PROTEIN_QUALITY,
    PROTEIN_QUANTITY,
    CARB_QUALITY,
    CARB_TIMING,
    FAT_BALANCE,
    OMEGA_RATIO,
    PROCESSED_FOOD,
    TIMING,
    HYDRATION,

    // Menopause-specific categories
    BONE_HEALTH,           // Calcium, Vitamin D
    PHYTOESTROGENS,        // Soy, flaxseed for hot flash relief
    HOT_FLASH_TRIGGERS,    // Foods to avoid (caffeine, alcohol, spicy)
    MOOD_SUPPORT,          // Magnesium, B vitamins, tryptophan
    HORMONE_BALANCE,       // Foods that support hormonal health
    INFLAMMATION,          // Anti-inflammatory foods for joint pain
    SLEEP_NUTRITION        // Foods that support better sleep
}

enum class TipPriority {
    CRITICAL,   // Needs immediate attention (e.g., trans fats, very low protein)
    HIGH,       // Should address soon
    MEDIUM,     // Room for improvement
    LOW         // Minor optimization
}

// ═══════════════════════════════════════════════════════════════
// MENOPAUSE NUTRITION CONTEXT
// Tracks menopause-specific nutritional needs and symptoms
// ═══════════════════════════════════════════════════════════════

/**
 * Context for menopause-specific nutrition recommendations
 */
data class MenopauseNutritionContext(
    // Daily intake tracking
    val calciumMg: Float = 0f,           // Target: 1200mg/day
    val vitaminDIu: Float = 0f,          // Target: 800-1000 IU/day
    val magnesiumMg: Float = 0f,         // Target: 320mg/day
    val omega3Grams: Float = 0f,         // Target: 1-2g EPA+DHA/day

    // Phytoestrogen intake
    val soyServings: Int = 0,            // Tofu, tempeh, edamame
    val flaxseedTbsp: Float = 0f,        // Ground flaxseed

    // Potential triggers consumed today
    val caffeineServings: Int = 0,       // Coffee, tea, energy drinks
    val alcoholServings: Int = 0,        // Wine, beer, spirits
    val spicyFoodServings: Int = 0,      // Spicy meals

    // Hydration
    val waterGlasses: Int = 0,           // Target: 8+ glasses

    // Current symptoms (from symptom tracker)
    val hasHotFlashes: Boolean = false,
    val hasSleepIssues: Boolean = false,
    val hasMoodSwings: Boolean = false,
    val hasJointPain: Boolean = false,
    val hasFatigue: Boolean = false,
    val hasBrainFog: Boolean = false
) {
    // Target values for menopause nutrition
    companion object {
        const val CALCIUM_TARGET_MG = 1200f
        const val VITAMIN_D_TARGET_IU = 1000f
        const val MAGNESIUM_TARGET_MG = 320f
        const val OMEGA3_TARGET_G = 1.5f
        const val WATER_TARGET_GLASSES = 8
        const val MAX_CAFFEINE_SERVINGS = 2
        const val MAX_ALCOHOL_SERVINGS = 1
    }

    val calciumPercentage: Float get() = (calciumMg / CALCIUM_TARGET_MG * 100).coerceAtMost(100f)
    val vitaminDPercentage: Float get() = (vitaminDIu / VITAMIN_D_TARGET_IU * 100).coerceAtMost(100f)
    val magnesiumPercentage: Float get() = (magnesiumMg / MAGNESIUM_TARGET_MG * 100).coerceAtMost(100f)
    val hydrationPercentage: Float get() = (waterGlasses.toFloat() / WATER_TARGET_GLASSES * 100).coerceAtMost(100f)
}

/**
 * Main tip engine - generates personalized tips based on dietary profile
 */
class NutritionTipEngine(
    private val dietaryProfile: DietaryProfile = DietaryProfile.DEFAULT
) {

    // ═══════════════════════════════════════════════════════════════
    // MAIN TIP GENERATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generate all relevant tips for current nutrition state
     */
    fun generateTips(macroQuality: MacroQualitySummary): List<NutritionTip> {
        val tips = mutableListOf<NutritionTip>()

        // Protein tips
        generateProteinTips(macroQuality.protein)?.let { tips.add(it) }

        // Carb tips
        generateCarbTips(macroQuality.carbs)?.let { tips.add(it) }

        // Fat tips
        generateFatTips(macroQuality.fats)?.let { tips.add(it) }

        // Processed food tips
        generateProcessedFoodTips(macroQuality.processedFood)?.let { tips.add(it) }

        // Sort by priority
        return tips.sortedBy { it.priority.ordinal }
    }

    /**
     * Generate all tips including menopause-specific recommendations
     */
    fun generateAllTips(
        macroQuality: MacroQualitySummary,
        menopauseContext: MenopauseNutritionContext
    ): List<NutritionTip> {
        val tips = mutableListOf<NutritionTip>()

        // Standard macro tips
        tips.addAll(generateTips(macroQuality))

        // Menopause-specific tips
        tips.addAll(generateMenopauseTips(menopauseContext))

        // Sort by priority
        return tips.sortedBy { it.priority.ordinal }
    }

    /**
     * Generate menopause-specific nutrition tips
     */
    fun generateMenopauseTips(context: MenopauseNutritionContext): List<NutritionTip> {
        val tips = mutableListOf<NutritionTip>()

        // Bone health (calcium & vitamin D)
        generateBoneHealthTips(context)?.let { tips.add(it) }

        // Hot flash management
        generateHotFlashTips(context)?.let { tips.add(it) }

        // Mood & energy support
        generateMoodSupportTips(context)?.let { tips.add(it) }

        // Sleep nutrition
        generateSleepNutritionTips(context)?.let { tips.add(it) }

        // Joint health / inflammation
        generateInflammationTips(context)?.let { tips.add(it) }

        // Hydration
        generateHydrationTips(context)?.let { tips.add(it) }

        // Phytoestrogen tips for hormone balance
        generatePhytoestrogenTips(context)?.let { tips.add(it) }

        return tips.sortedBy { it.priority.ordinal }
    }

    /**
     * Get the most important tip to show
     */
    fun getTopTip(macroQuality: MacroQualitySummary): NutritionTip? {
        return generateTips(macroQuality).firstOrNull()
    }

    /**
     * Get the most important menopause tip
     */
    fun getTopMenopauseTip(context: MenopauseNutritionContext): NutritionTip? {
        return generateMenopauseTips(context).firstOrNull()
    }

    // ═══════════════════════════════════════════════════════════════
    // PROTEIN TIPS
    // ═══════════════════════════════════════════════════════════════

    private fun generateProteinTips(score: ProteinQualityScore): NutritionTip? {
        if (score.qualityLevel == QualityLevel.EXCELLENT) return null

        val quickWins = mutableListOf<QuickWin>()
        val priority: TipPriority
        val headline: String
        val explanation: String

        when {
            score.weightedDiaasScore < 0.6f -> {
                priority = TipPriority.HIGH
                headline = "Protein quality needs attention"
                explanation = "Your protein sources have low bioavailability (DIAAS ${String.format("%.2f", score.weightedDiaasScore)}). " +
                             "This means your body can't use all the protein efficiently for muscle building."

                // Add diet-specific suggestions
                quickWins.addAll(getProteinQuickWins(score, needsHighQuality = true))
            }
            score.weightedDiaasScore < 0.8f -> {
                priority = TipPriority.MEDIUM
                headline = "Boost your protein quality"
                explanation = "Your protein mix is okay (DIAAS ${String.format("%.2f", score.weightedDiaasScore)}), " +
                             "but adding complete proteins will improve muscle protein synthesis."

                quickWins.addAll(getProteinQuickWins(score, needsHighQuality = false))
            }
            score.highQualityPercentage < 50f -> {
                priority = TipPriority.LOW
                headline = "Optimize protein sources"
                explanation = "Good overall! Only ${score.highQualityPercentage.toInt()}% from high-quality sources. " +
                             "A small tweak could maximize gains."

                quickWins.addAll(getProteinQuickWins(score, needsHighQuality = false))
            }
            else -> return null
        }

        return NutritionTip(
            category = TipCategory.PROTEIN_QUALITY,
            priority = priority,
            headline = headline,
            explanation = explanation,
            quickWins = quickWins.take(3),
            impact = "Target DIAAS: >0.85 for optimal muscle synthesis"
        )
    }

    private fun getProteinQuickWins(score: ProteinQualityScore, needsHighQuality: Boolean): List<QuickWin> {
        val wins = mutableListOf<QuickWin>()

        // Check dietary preferences (can have multiple, e.g., Vegan + Halal)
        when {
            dietaryProfile.isVegan -> {
                wins.add(QuickWin(
                    action = "Combine rice + beans in same meal",
                    benefit = "Complete amino acids",
                    swap = FoodSwap("Rice alone", "Rice + black beans", "+0.25 DIAAS")
                ))
                wins.add(QuickWin(
                    action = "Add tofu or tempeh to lunch",
                    benefit = "+20g complete protein",
                    swap = null
                ))
                if (needsHighQuality && !dietaryProfile.allergies.contains(FoodAllergy.SOY)) {
                    wins.add(QuickWin(
                        action = "Try pea + rice protein blend",
                        benefit = "DIAAS 0.91 when combined",
                        swap = FoodSwap("Single plant protein", "Pea + rice blend", "+0.3 DIAAS")
                    ))
                }
            }
            dietaryProfile.isVegetarian -> {
                if (!dietaryProfile.allergies.contains(FoodAllergy.EGGS)) {
                    wins.add(QuickWin(
                        action = "Add 2 eggs at breakfast",
                        benefit = "+12g protein, DIAAS 1.13",
                        swap = null
                    ))
                }
                if (!dietaryProfile.allergies.contains(FoodAllergy.DAIRY)) {
                    wins.add(QuickWin(
                        action = "Swap regular yogurt for Greek/Skyr",
                        benefit = "2x protein, same calories",
                        swap = FoodSwap("Regular yogurt", "Greek yogurt", "+10g protein")
                    ))
                    wins.add(QuickWin(
                        action = "Add cottage cheese as snack",
                        benefit = "+25g high-quality protein",
                        swap = null
                    ))
                }
            }
            dietaryProfile.hasPreference(DietaryPreference.PESCATARIAN) -> {
                wins.add(QuickWin(
                    action = "Add salmon or tuna 2-3x per week",
                    benefit = "DIAAS 1.0 + omega-3",
                    swap = null
                ))
                if (!dietaryProfile.allergies.contains(FoodAllergy.EGGS)) {
                    wins.add(QuickWin(
                        action = "Eggs for breakfast",
                        benefit = "DIAAS 1.13 - highest quality",
                        swap = null
                    ))
                }
                if (!dietaryProfile.allergies.contains(FoodAllergy.SHELLFISH)) {
                    wins.add(QuickWin(
                        action = "Try shrimp or prawns",
                        benefit = "25g protein per 100g, very lean",
                        swap = null
                    ))
                }
            }
            dietaryProfile.isKeto -> {
                wins.add(QuickWin(
                    action = "Prioritize fatty fish (salmon, mackerel)",
                    benefit = "Protein + healthy fats, keto-friendly",
                    swap = null
                ))
                if (!dietaryProfile.allergies.contains(FoodAllergy.EGGS)) {
                    wins.add(QuickWin(
                        action = "Whole eggs (don't skip yolks!)",
                        benefit = "Complete amino acids + fat",
                        swap = null
                    ))
                }
                wins.add(QuickWin(
                    action = "Beef, lamb for higher fat content",
                    benefit = "Protein + fat macros aligned",
                    swap = null
                ))
            }
            else -> {
                // Omnivore, Halal, Kosher (or combinations)
                if (!dietaryProfile.allergies.contains(FoodAllergy.EGGS)) {
                    wins.add(QuickWin(
                        action = "Add 2-3 eggs at breakfast",
                        benefit = "+15-20g protein, DIAAS 1.13",
                        swap = null
                    ))
                }
                if (!dietaryProfile.allergies.contains(FoodAllergy.DAIRY)) {
                    wins.add(QuickWin(
                        action = "Greek yogurt or Skyr as snack",
                        benefit = "+20g protein, DIAAS 1.08",
                        swap = FoodSwap("Regular snack", "200g Greek yogurt", "+20g quality protein")
                    ))
                }
                wins.add(QuickWin(
                    action = "Chicken breast for lean protein",
                    benefit = "31g protein per 100g, DIAAS 1.08",
                    swap = null
                ))

                // Pork suggestions only if not halal/kosher
                if (!dietaryProfile.isHalal && !dietaryProfile.isKosher) {
                    // Can suggest pork
                }
            }
        }

        // Filter out any suggestions with allergens
        return wins.filter { win ->
            val swapTo = win.swap?.to?.lowercase() ?: ""
            val action = win.action.lowercase()
            dietaryProfile.allergies.none { allergy ->
                allergy.getFoodsToAvoid().any { allergen ->
                    swapTo.contains(allergen) || action.contains(allergen)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CARB TIPS
    // ═══════════════════════════════════════════════════════════════

    private fun generateCarbTips(score: CarbQualityScore): NutritionTip? {
        if (score.qualityLevel == QualityLevel.EXCELLENT) return null

        val quickWins = mutableListOf<QuickWin>()
        val priority: TipPriority
        val headline: String
        val explanation: String

        when {
            score.averageGI > 70 -> {
                priority = TipPriority.HIGH
                headline = "Blood sugar spikes detected"
                explanation = "Your carb choices (avg GI ${score.averageGI.toInt()}) cause rapid blood sugar spikes. " +
                             "This leads to energy crashes and increased fat storage."

                quickWins.addAll(getCarbQuickWins(needsLowGI = true))
            }
            score.averageGI > 60 -> {
                priority = TipPriority.MEDIUM
                headline = "Room for better carb choices"
                explanation = "Average GI of ${score.averageGI.toInt()} is okay, but lower GI options " +
                             "will give you more stable energy throughout the day."

                quickWins.addAll(getCarbQuickWins(needsLowGI = true))
            }
            score.fiberToSugarRatio < 0.3f -> {
                priority = TipPriority.MEDIUM
                headline = "Increase fiber intake"
                explanation = "Your fiber-to-sugar ratio is low (${String.format("%.1f", score.fiberToSugarRatio)}). " +
                             "More fiber improves digestion and blood sugar control."

                quickWins.add(QuickWin(
                    action = "Add vegetables to every meal",
                    benefit = "+5-10g fiber per meal",
                    swap = null
                ))
                quickWins.add(QuickWin(
                    action = "Choose whole fruits over juice",
                    benefit = "Keeps fiber intact",
                    swap = FoodSwap("Orange juice", "Whole orange", "+3g fiber, -10g sugar")
                ))
            }
            else -> return null
        }

        return NutritionTip(
            category = TipCategory.CARB_QUALITY,
            priority = priority,
            headline = headline,
            explanation = explanation,
            quickWins = quickWins.take(3),
            impact = "Target GI: <55 for stable energy"
        )
    }

    private fun getCarbQuickWins(needsLowGI: Boolean): List<QuickWin> {
        val wins = mutableListOf<QuickWin>()

        // Universal low-GI swaps
        if (!dietaryProfile.allergies.contains(FoodAllergy.GLUTEN)) {
            wins.add(QuickWin(
                action = "Swap white bread for sourdough",
                benefit = "-20 GI points",
                swap = FoodSwap("White bread (GI 75)", "Sourdough (GI 54)", "-20 GI")
            ))
        }

        wins.add(QuickWin(
            action = "White rice → Basmati or brown rice",
            benefit = "-15 GI points",
            swap = FoodSwap("White rice (GI 73)", "Basmati (GI 58)", "-15 GI")
        ))

        wins.add(QuickWin(
            action = "Add protein/fat to carb meals",
            benefit = "Slows absorption, lowers effective GI",
            swap = null
        ))

        // Keto-specific
        if (dietaryProfile.isKeto) {
            wins.clear()
            wins.add(QuickWin(
                action = "Replace grains with cauliflower rice",
                benefit = "90% fewer carbs",
                swap = FoodSwap("Rice (45g carbs)", "Cauliflower rice (5g)", "-40g carbs")
            ))
            wins.add(QuickWin(
                action = "Zucchini noodles instead of pasta",
                benefit = "95% fewer carbs",
                swap = FoodSwap("Pasta (75g carbs)", "Zoodles (4g)", "-70g carbs")
            ))
        }

        return wins
    }

    // ═══════════════════════════════════════════════════════════════
    // FAT TIPS
    // ═══════════════════════════════════════════════════════════════

    private fun generateFatTips(score: FatQualityScore): NutritionTip? {
        val quickWins = mutableListOf<QuickWin>()
        val priority: TipPriority
        val headline: String
        val explanation: String

        when {
            score.hasTransFat -> {
                priority = TipPriority.CRITICAL
                headline = "Trans fats detected!"
                explanation = "Trans fats are the worst type of fat - they increase bad cholesterol, " +
                             "decrease good cholesterol, and increase heart disease risk. Eliminate completely."

                quickWins.add(QuickWin(
                    action = "Check labels for 'hydrogenated' oils",
                    benefit = "These are hidden trans fats",
                    swap = null
                ))
                quickWins.add(QuickWin(
                    action = "Avoid deep-fried fast food",
                    benefit = "Major trans fat source",
                    swap = FoodSwap("Fried foods", "Grilled or baked", "Zero trans fats")
                ))
            }
            score.omega6to3Ratio > 10 -> {
                priority = TipPriority.HIGH
                headline = "Omega balance off"
                explanation = "Your omega-6:3 ratio is ${String.format("%.1f", score.omega6to3Ratio)}:1 " +
                             "(target <4:1). High omega-6 promotes inflammation."

                quickWins.addAll(getOmegaQuickWins())
            }
            score.saturatedFatPercentage > 35 -> {
                priority = TipPriority.MEDIUM
                headline = "High saturated fat intake"
                explanation = "${score.saturatedFatPercentage.toInt()}% of your fats are saturated. " +
                             "Balance with more unsaturated sources for heart health."

                quickWins.add(QuickWin(
                    action = "Cook with olive oil instead of butter",
                    benefit = "Swap saturated for monounsaturated",
                    swap = FoodSwap("Butter", "Olive oil", "Heart-healthy fats")
                ))
                quickWins.add(QuickWin(
                    action = "Add avocado to meals",
                    benefit = "Healthy monounsaturated fats",
                    swap = null
                ))
            }
            score.qualityLevel == QualityLevel.EXCELLENT -> return null
            else -> return null
        }

        return NutritionTip(
            category = TipCategory.FAT_BALANCE,
            priority = priority,
            headline = headline,
            explanation = explanation,
            quickWins = quickWins.take(3),
            impact = if (score.hasTransFat) "Eliminate trans fats completely"
                    else "Target omega ratio: 4:1 or lower"
        )
    }

    private fun getOmegaQuickWins(): List<QuickWin> {
        val wins = mutableListOf<QuickWin>()

        when {
            dietaryProfile.isVegan -> {
                wins.add(QuickWin(
                    action = "Add 2 tbsp ground flaxseed daily",
                    benefit = "+3g ALA omega-3",
                    swap = null
                ))
                wins.add(QuickWin(
                    action = "Include walnuts as snack",
                    benefit = "Best nut for omega-3",
                    swap = FoodSwap("Almonds", "Walnuts", "+2.5g omega-3")
                ))
                wins.add(QuickWin(
                    action = "Consider algae omega-3 supplement",
                    benefit = "Direct EPA/DHA source (vegan)",
                    swap = null
                ))
            }
            dietaryProfile.isVegetarian -> {
                if (!dietaryProfile.allergies.contains(FoodAllergy.EGGS)) {
                    wins.add(QuickWin(
                        action = "Choose omega-3 enriched eggs",
                        benefit = "5x more omega-3 than regular",
                        swap = FoodSwap("Regular eggs", "Omega-3 eggs", "+300mg omega-3/egg")
                    ))
                }
                wins.add(QuickWin(
                    action = "Daily chia seeds in smoothie/yogurt",
                    benefit = "+5g omega-3 per 2 tbsp",
                    swap = null
                ))
            }
            else -> {
                // Can eat fish
                if (!dietaryProfile.allergies.contains(FoodAllergy.FISH)) {
                    wins.add(QuickWin(
                        action = "Fatty fish 2-3x per week",
                        benefit = "Best omega-3 source (EPA/DHA)",
                        swap = FoodSwap("Chicken dinner", "Salmon dinner", "+2g omega-3")
                    ))
                }
                wins.add(QuickWin(
                    action = "Swap sunflower oil for olive oil",
                    benefit = "Reduces omega-6 intake",
                    swap = FoodSwap("Sunflower oil", "Olive oil", "-10:1 ratio improvement")
                ))
                wins.add(QuickWin(
                    action = "Reduce processed snacks",
                    benefit = "Major omega-6 source (seed oils)",
                    swap = null
                ))
            }
        }

        return wins
    }

    // ═══════════════════════════════════════════════════════════════
    // PROCESSED FOOD TIPS
    // ═══════════════════════════════════════════════════════════════

    private fun generateProcessedFoodTips(score: ProcessedFoodScore): NutritionTip? {
        if (score.qualityLevel == QualityLevel.EXCELLENT) return null

        val quickWins = mutableListOf<QuickWin>()
        val priority: TipPriority
        val headline: String
        val explanation: String

        when {
            score.ultraProcessedPercentage >= 50 -> {
                priority = TipPriority.CRITICAL
                headline = "Too much ultra-processed food"
                explanation = "${score.ultraProcessedPercentage.toInt()}% of your calories come from UPF. " +
                             "Research links high UPF intake to obesity, heart disease, and diabetes."

                // Suggest swaps for their worst offenders
                score.worstOffenders.take(2).forEach { food ->
                    getSwapForUPF(food)?.let { swap ->
                        quickWins.add(QuickWin(
                            action = "Replace $food",
                            benefit = "Whole food alternative",
                            swap = swap
                        ))
                    }
                }
            }
            score.ultraProcessedPercentage >= 30 -> {
                priority = TipPriority.HIGH
                headline = "Reduce processed foods"
                explanation = "${score.ultraProcessedPercentage.toInt()}% UPF is above ideal (<20%). " +
                             "Small swaps can make a big difference."

                score.worstOffenders.firstOrNull()?.let { food ->
                    getSwapForUPF(food)?.let { swap ->
                        quickWins.add(QuickWin(
                            action = "Swap out $food",
                            benefit = "Easy win for better health",
                            swap = swap
                        ))
                    }
                }
            }
            score.ultraProcessedPercentage >= 20 -> {
                priority = TipPriority.MEDIUM
                headline = "Almost there!"
                explanation = "You're close to the <20% UPF target. " +
                             "A few more swaps and you're in the optimal zone."

                quickWins.add(QuickWin(
                    action = "Prep snacks in advance",
                    benefit = "Avoid convenience UPF",
                    swap = null
                ))
            }
            else -> return null
        }

        // Universal UPF tips
        quickWins.add(QuickWin(
            action = "Cook more meals from scratch",
            benefit = "Full control over ingredients",
            swap = null
        ))

        return NutritionTip(
            category = TipCategory.PROCESSED_FOOD,
            priority = priority,
            headline = headline,
            explanation = explanation,
            quickWins = quickWins.take(3),
            impact = "Target: <20% calories from ultra-processed foods"
        )
    }

    private fun getSwapForUPF(foodName: String): FoodSwap? {
        val name = foodName.lowercase()

        return when {
            // Protein bars/shakes
            name.contains("protein bar") || name.contains("riegel") -> {
                if (dietaryProfile.isVegan) {
                    FoodSwap(foodName, "Nuts + dates", "Same energy, no additives")
                } else if (!dietaryProfile.allergies.contains(FoodAllergy.DAIRY)) {
                    FoodSwap(foodName, "Greek yogurt + nuts", "Real food protein")
                } else {
                    FoodSwap(foodName, "Boiled eggs + fruit", "Whole food protein")
                }
            }
            name.contains("protein powder") || name.contains("whey") -> {
                if (!dietaryProfile.allergies.contains(FoodAllergy.DAIRY)) {
                    FoodSwap(foodName, "Cottage cheese or Skyr", "Real food, same protein")
                } else {
                    null // Keep protein powder for vegans/dairy-free
                }
            }

            // Energy drinks
            name.contains("energy drink") || name.contains("red bull") || name.contains("monster") -> {
                FoodSwap(foodName, "Black coffee + banana", "Natural caffeine + energy")
            }

            // Cereals
            name.contains("cereal") || name.contains("corn flakes") || name.contains("müsli") -> {
                if (!dietaryProfile.allergies.contains(FoodAllergy.GLUTEN)) {
                    FoodSwap(foodName, "Oatmeal + berries", "Whole grain, lower GI")
                } else {
                    FoodSwap(foodName, "Chia pudding + fruit", "Gluten-free, high fiber")
                }
            }

            // Chips/snacks
            name.contains("chip") || name.contains("crisp") || name.contains("doritos") -> {
                if (!dietaryProfile.allergies.contains(FoodAllergy.NUTS)) {
                    FoodSwap(foodName, "Mixed nuts", "Healthy fats, protein")
                } else {
                    FoodSwap(foodName, "Veggie sticks + hummus", "Fiber + protein")
                }
            }

            // Soft drinks
            name.contains("soda") || name.contains("cola") || name.contains("fanta") -> {
                FoodSwap(foodName, "Sparkling water + lemon", "Zero sugar, refreshing")
            }

            // Fast food
            name.contains("mcdonald") || name.contains("burger king") || name.contains("nugget") -> {
                FoodSwap(foodName, "Homemade burger/chicken", "Same taste, real ingredients")
            }

            // Instant noodles
            name.contains("instant") || name.contains("ramen") || name.contains("cup noodle") -> {
                FoodSwap(foodName, "Rice + stir-fry veggies", "Quick, much healthier")
            }

            // Ice cream
            name.contains("ice cream") || name.contains("eis") -> {
                if (!dietaryProfile.allergies.contains(FoodAllergy.DAIRY)) {
                    FoodSwap(foodName, "Frozen Greek yogurt", "Protein + probiotics")
                } else {
                    FoodSwap(foodName, "Frozen banana 'nice cream'", "Natural sweetness")
                }
            }

            // Candy
            name.contains("candy") || name.contains("gummy") || name.contains("chocolate bar") -> {
                FoodSwap(foodName, "Dark chocolate + dried fruit", "Antioxidants, less sugar")
            }

            // Default
            else -> null
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MENOPAUSE-SPECIFIC TIPS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bone Health Tips - Calcium & Vitamin D
     * Critical during menopause due to estrogen decline
     */
    private fun generateBoneHealthTips(context: MenopauseNutritionContext): NutritionTip? {
        val quickWins = mutableListOf<QuickWin>()
        val priority: TipPriority
        val headline: String
        val explanation: String

        val calciumLow = context.calciumMg < MenopauseNutritionContext.CALCIUM_TARGET_MG * 0.5f
        val vitaminDLow = context.vitaminDIu < MenopauseNutritionContext.VITAMIN_D_TARGET_IU * 0.5f

        when {
            calciumLow && vitaminDLow -> {
                priority = TipPriority.HIGH
                headline = "Bone health needs attention"
                explanation = "Your calcium (${context.calciumMg.toInt()}mg) and vitamin D (${context.vitaminDIu.toInt()} IU) " +
                        "are both low. During menopause, bone loss accelerates - these nutrients are essential."

                quickWins.addAll(getCalciumQuickWins())
                quickWins.addAll(getVitaminDQuickWins())
            }
            calciumLow -> {
                priority = TipPriority.HIGH
                headline = "Boost your calcium intake"
                explanation = "Only ${context.calciumMg.toInt()}mg calcium today (target: 1200mg). " +
                        "Estrogen decline during menopause increases bone loss - calcium is your defense."

                quickWins.addAll(getCalciumQuickWins())
            }
            vitaminDLow -> {
                priority = TipPriority.MEDIUM
                headline = "Vitamin D running low"
                explanation = "Vitamin D (${context.vitaminDIu.toInt()} IU) helps your body absorb calcium. " +
                        "Without it, even good calcium intake won't fully protect your bones."

                quickWins.addAll(getVitaminDQuickWins())
            }
            context.calciumPercentage < 80 -> {
                priority = TipPriority.LOW
                headline = "Almost at calcium goal"
                explanation = "You're at ${context.calciumPercentage.toInt()}% of your calcium target. " +
                        "A small boost will help maintain strong bones."

                quickWins.add(QuickWin(
                    action = "Add a calcium-rich snack",
                    benefit = "+200-300mg calcium",
                    swap = null
                ))
            }
            else -> return null
        }

        return NutritionTip(
            category = TipCategory.BONE_HEALTH,
            priority = priority,
            headline = headline,
            explanation = explanation,
            quickWins = quickWins.take(3),
            impact = "Target: 1200mg calcium + 1000 IU vitamin D daily"
        )
    }

    private fun getCalciumQuickWins(): List<QuickWin> {
        val wins = mutableListOf<QuickWin>()

        if (!dietaryProfile.allergies.contains(FoodAllergy.DAIRY)) {
            wins.add(QuickWin(
                action = "Greek yogurt or Skyr (200g)",
                benefit = "+250mg calcium + protein",
                swap = null
            ))
            wins.add(QuickWin(
                action = "Add cheese to meals",
                benefit = "Parmesan: 330mg per 30g",
                swap = null
            ))
        } else {
            wins.add(QuickWin(
                action = "Fortified plant milk (250ml)",
                benefit = "+300mg calcium (check label)",
                swap = FoodSwap("Regular plant milk", "Calcium-fortified", "+300mg calcium")
            ))
        }

        wins.add(QuickWin(
            action = "Sardines with bones (100g)",
            benefit = "+380mg calcium + omega-3",
            swap = null
        ))

        wins.add(QuickWin(
            action = "Dark leafy greens (kale, bok choy)",
            benefit = "+100-200mg per serving",
            swap = null
        ))

        if (!dietaryProfile.allergies.contains(FoodAllergy.SOY)) {
            wins.add(QuickWin(
                action = "Calcium-set tofu (100g)",
                benefit = "+350mg calcium + phytoestrogens",
                swap = null
            ))
        }

        return wins.filter { win ->
            dietaryProfile.isFoodAllowed(win.action)
        }
    }

    private fun getVitaminDQuickWins(): List<QuickWin> {
        val wins = mutableListOf<QuickWin>()

        wins.add(QuickWin(
            action = "Fatty fish (salmon, mackerel)",
            benefit = "400-600 IU per serving",
            swap = null
        ))

        if (!dietaryProfile.allergies.contains(FoodAllergy.EGGS)) {
            wins.add(QuickWin(
                action = "Egg yolks (2 eggs)",
                benefit = "+80 IU vitamin D",
                swap = null
            ))
        }

        wins.add(QuickWin(
            action = "15 min midday sun exposure",
            benefit = "Natural vitamin D synthesis",
            swap = null
        ))

        wins.add(QuickWin(
            action = "Consider vitamin D3 supplement",
            benefit = "Reliable daily dose (1000 IU)",
            swap = null
        ))

        return wins
    }

    /**
     * Hot Flash Management Tips
     * Foods to avoid and cooling strategies
     */
    private fun generateHotFlashTips(context: MenopauseNutritionContext): NutritionTip? {
        if (!context.hasHotFlashes) return null

        val quickWins = mutableListOf<QuickWin>()
        val priority: TipPriority
        val headline: String
        val explanation: String

        val hasTriggers = context.caffeineServings > 2 ||
                context.alcoholServings > 1 ||
                context.spicyFoodServings > 0

        when {
            hasTriggers && context.caffeineServings > 2 -> {
                priority = TipPriority.HIGH
                headline = "Caffeine may trigger hot flashes"
                explanation = "You've had ${context.caffeineServings} caffeinated drinks today. " +
                        "Caffeine is a known hot flash trigger - try reducing after noon."

                quickWins.add(QuickWin(
                    action = "Switch to decaf after 12pm",
                    benefit = "Fewer afternoon/evening hot flashes",
                    swap = FoodSwap("Regular coffee", "Decaf or herbal tea", "Reduced triggers")
                ))
                quickWins.add(QuickWin(
                    action = "Try green tea (less caffeine)",
                    benefit = "Gentle energy + antioxidants",
                    swap = FoodSwap("Coffee (95mg caffeine)", "Green tea (30mg)", "-65mg caffeine")
                ))
            }
            hasTriggers && context.alcoholServings > 1 -> {
                priority = TipPriority.HIGH
                headline = "Alcohol intensifies hot flashes"
                explanation = "Alcohol dilates blood vessels and can trigger intense hot flashes. " +
                        "Even 1-2 drinks can have an effect."

                quickWins.add(QuickWin(
                    action = "Limit to 1 drink max",
                    benefit = "Significantly fewer triggers",
                    swap = null
                ))
                quickWins.add(QuickWin(
                    action = "Try alcohol-free wine/beer",
                    benefit = "Social without the flush",
                    swap = FoodSwap("Wine", "Alcohol-free wine", "No hot flash trigger")
                ))
            }
            hasTriggers && context.spicyFoodServings > 0 -> {
                priority = TipPriority.MEDIUM
                headline = "Spicy food = hot flash risk"
                explanation = "Spicy foods raise body temperature and can trigger hot flashes. " +
                        "Try milder alternatives."

                quickWins.add(QuickWin(
                    action = "Use herbs instead of chili",
                    benefit = "Flavor without the heat",
                    swap = FoodSwap("Chili/hot sauce", "Cumin, paprika, herbs", "No trigger")
                ))
            }
            else -> {
                priority = TipPriority.LOW
                headline = "Cooling foods for hot flashes"
                explanation = "Some foods can help cool your body naturally and reduce hot flash intensity."

                quickWins.add(QuickWin(
                    action = "Eat cooling foods: cucumber, watermelon",
                    benefit = "Natural body cooling",
                    swap = null
                ))
                quickWins.add(QuickWin(
                    action = "Cold water with meals",
                    benefit = "Helps regulate temperature",
                    swap = null
                ))
            }
        }

        // Always add phytoestrogen suggestion for hot flashes
        if (!dietaryProfile.allergies.contains(FoodAllergy.SOY)) {
            quickWins.add(QuickWin(
                action = "Add soy foods (tofu, edamame)",
                benefit = "Phytoestrogens may reduce hot flashes",
                swap = null
            ))
        }

        return NutritionTip(
            category = TipCategory.HOT_FLASH_TRIGGERS,
            priority = priority,
            headline = headline,
            explanation = explanation,
            quickWins = quickWins.take(3),
            impact = "Identify and avoid your personal triggers"
        )
    }

    /**
     * Mood Support Tips - Magnesium, B vitamins, tryptophan
     */
    private fun generateMoodSupportTips(context: MenopauseNutritionContext): NutritionTip? {
        if (!context.hasMoodSwings && !context.hasFatigue && !context.hasBrainFog) return null

        val quickWins = mutableListOf<QuickWin>()
        val priority: TipPriority
        val headline: String
        val explanation: String

        val magnesiumLow = context.magnesiumMg < MenopauseNutritionContext.MAGNESIUM_TARGET_MG * 0.5f

        when {
            context.hasMoodSwings && magnesiumLow -> {
                priority = TipPriority.HIGH
                headline = "Magnesium for mood stability"
                explanation = "Magnesium is essential for neurotransmitter function. Low levels " +
                        "(you're at ${context.magnesiumMg.toInt()}mg) can worsen mood swings during menopause."

                quickWins.addAll(getMagnesiumQuickWins())
            }
            context.hasBrainFog -> {
                priority = TipPriority.MEDIUM
                headline = "Brain fog? Feed your brain"
                explanation = "Menopause brain fog is real! Omega-3s, B vitamins, and stable blood sugar " +
                        "all support cognitive function."

                quickWins.add(QuickWin(
                    action = "Fatty fish 2-3x per week",
                    benefit = "DHA for brain function",
                    swap = null
                ))
                quickWins.add(QuickWin(
                    action = "Berries as daily snack",
                    benefit = "Antioxidants protect brain cells",
                    swap = FoodSwap("Processed snack", "Handful of berries", "Brain boost")
                ))
                quickWins.add(QuickWin(
                    action = "Avoid sugar spikes",
                    benefit = "Stable energy = clearer thinking",
                    swap = null
                ))
            }
            context.hasFatigue -> {
                priority = TipPriority.MEDIUM
                headline = "Energy-boosting nutrition"
                explanation = "Fatigue during menopause often relates to iron, B12, and blood sugar. " +
                        "The right foods can help restore your energy."

                quickWins.add(QuickWin(
                    action = "Iron-rich foods (spinach, legumes)",
                    benefit = "Supports energy production",
                    swap = null
                ))
                quickWins.add(QuickWin(
                    action = "B12 sources (eggs, fish, fortified foods)",
                    benefit = "Essential for energy metabolism",
                    swap = null
                ))
                quickWins.add(QuickWin(
                    action = "Complex carbs over simple",
                    benefit = "Sustained energy release",
                    swap = FoodSwap("White bread", "Whole grain", "Longer lasting energy")
                ))
            }
            else -> return null
        }

        return NutritionTip(
            category = TipCategory.MOOD_SUPPORT,
            priority = priority,
            headline = headline,
            explanation = explanation,
            quickWins = quickWins.take(3),
            impact = "Nutrition directly affects mood and energy"
        )
    }

    private fun getMagnesiumQuickWins(): List<QuickWin> {
        val wins = mutableListOf<QuickWin>()

        wins.add(QuickWin(
            action = "Dark chocolate (30g, 70%+)",
            benefit = "+65mg magnesium + mood boost",
            swap = FoodSwap("Milk chocolate", "Dark chocolate 70%+", "+50mg magnesium")
        ))

        if (!dietaryProfile.allergies.contains(FoodAllergy.NUTS)) {
            wins.add(QuickWin(
                action = "Handful of almonds or cashews",
                benefit = "+80mg magnesium per 30g",
                swap = null
            ))
        }

        wins.add(QuickWin(
            action = "Add spinach or swiss chard",
            benefit = "+150mg magnesium per cup cooked",
            swap = null
        ))

        wins.add(QuickWin(
            action = "Pumpkin seeds as snack",
            benefit = "+150mg magnesium per 30g",
            swap = null
        ))

        wins.add(QuickWin(
            action = "Avocado (half)",
            benefit = "+30mg magnesium + healthy fats",
            swap = null
        ))

        return wins.filter { win ->
            dietaryProfile.isFoodAllowed(win.action)
        }
    }

    /**
     * Sleep Nutrition Tips
     */
    private fun generateSleepNutritionTips(context: MenopauseNutritionContext): NutritionTip? {
        if (!context.hasSleepIssues) return null

        val quickWins = mutableListOf<QuickWin>()

        // Tryptophan-rich foods
        if (!dietaryProfile.allergies.contains(FoodAllergy.DAIRY)) {
            quickWins.add(QuickWin(
                action = "Warm milk before bed",
                benefit = "Tryptophan + calming ritual",
                swap = null
            ))
        }

        quickWins.add(QuickWin(
            action = "Turkey or chicken for dinner",
            benefit = "High in sleep-promoting tryptophan",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "Tart cherry juice (30ml)",
            benefit = "Natural melatonin source",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "Magnesium-rich dinner (fish, leafy greens)",
            benefit = "Helps muscle relaxation",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "No caffeine after 2pm",
            benefit = "Caffeine affects sleep for 6+ hours",
            swap = FoodSwap("Afternoon coffee", "Herbal tea", "Better sleep tonight")
        ))

        quickWins.add(QuickWin(
            action = "Light dinner, not heavy",
            benefit = "Easier digestion = better sleep",
            swap = null
        ))

        return NutritionTip(
            category = TipCategory.SLEEP_NUTRITION,
            priority = TipPriority.MEDIUM,
            headline = "Eat for better sleep",
            explanation = "What and when you eat affects sleep quality. " +
                    "Certain foods promote relaxation while others disrupt your sleep cycle.",
            quickWins = quickWins.filter { dietaryProfile.isFoodAllowed(it.action) }.take(3),
            impact = "Better nutrition = deeper, more restful sleep"
        )
    }

    /**
     * Inflammation / Joint Pain Tips
     */
    private fun generateInflammationTips(context: MenopauseNutritionContext): NutritionTip? {
        if (!context.hasJointPain) return null

        val quickWins = mutableListOf<QuickWin>()

        // Anti-inflammatory foods
        quickWins.add(QuickWin(
            action = "Fatty fish 3x per week",
            benefit = "Omega-3s reduce inflammation",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "Turmeric with black pepper",
            benefit = "Powerful anti-inflammatory (curcumin)",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "Extra virgin olive oil daily",
            benefit = "Oleocanthal = natural anti-inflammatory",
            swap = FoodSwap("Vegetable oil", "Extra virgin olive oil", "Anti-inflammatory")
        ))

        quickWins.add(QuickWin(
            action = "Berries, especially blueberries",
            benefit = "Anthocyanins fight inflammation",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "Ginger in cooking or tea",
            benefit = "Reduces joint stiffness",
            swap = null
        ))

        // Foods to reduce
        quickWins.add(QuickWin(
            action = "Reduce sugar and refined carbs",
            benefit = "These promote inflammation",
            swap = FoodSwap("Sugary snacks", "Nuts + fruit", "Anti-inflammatory swap")
        ))

        return NutritionTip(
            category = TipCategory.INFLAMMATION,
            priority = TipPriority.MEDIUM,
            headline = "Fight joint pain with food",
            explanation = "Joint pain often increases during menopause due to declining estrogen. " +
                    "Anti-inflammatory foods can provide real relief.",
            quickWins = quickWins.filter { dietaryProfile.isFoodAllowed(it.action) }.take(3),
            impact = "Anti-inflammatory diet can reduce pain by 30-50%"
        )
    }

    /**
     * Hydration Tips
     */
    private fun generateHydrationTips(context: MenopauseNutritionContext): NutritionTip? {
        val waterTarget = MenopauseNutritionContext.WATER_TARGET_GLASSES

        // Extra important if having hot flashes
        val adjustedTarget = if (context.hasHotFlashes) waterTarget + 2 else waterTarget

        if (context.waterGlasses >= adjustedTarget) return null

        val quickWins = mutableListOf<QuickWin>()
        val priority: TipPriority
        val headline: String
        val explanation: String

        when {
            context.waterGlasses < 4 -> {
                priority = TipPriority.HIGH
                headline = "Hydration critical!"
                explanation = "Only ${context.waterGlasses} glasses today! Dehydration worsens hot flashes, " +
                        "fatigue, and brain fog. Aim for ${adjustedTarget}+ glasses."

                quickWins.add(QuickWin(
                    action = "Drink a glass NOW",
                    benefit = "Immediate hydration",
                    swap = null
                ))
            }
            context.hasHotFlashes && context.waterGlasses < adjustedTarget -> {
                priority = TipPriority.MEDIUM
                headline = "Extra water for hot flashes"
                explanation = "Hot flashes cause fluid loss through sweating. You need more water than usual - " +
                        "aim for ${adjustedTarget} glasses minimum."

                quickWins.add(QuickWin(
                    action = "Ice water during hot flashes",
                    benefit = "Cools you down + rehydrates",
                    swap = null
                ))
            }
            else -> {
                priority = TipPriority.LOW
                headline = "Stay hydrated"
                explanation = "You're at ${context.waterGlasses}/$adjustedTarget glasses. " +
                        "Good hydration supports everything from skin to mood."
            }
        }

        quickWins.add(QuickWin(
            action = "Water bottle at your desk",
            benefit = "Visible reminder to drink",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "Herbal tea counts too!",
            benefit = "Hydrating + calming (chamomile, peppermint)",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "Eat water-rich foods",
            benefit = "Cucumber, watermelon, oranges",
            swap = null
        ))

        return NutritionTip(
            category = TipCategory.HYDRATION,
            priority = priority,
            headline = headline,
            explanation = explanation,
            quickWins = quickWins.take(3),
            impact = "Target: ${adjustedTarget}+ glasses daily"
        )
    }

    /**
     * Phytoestrogen Tips - Natural hormone support
     */
    private fun generatePhytoestrogenTips(context: MenopauseNutritionContext): NutritionTip? {
        // Only suggest if having hormone-related symptoms
        if (!context.hasHotFlashes && !context.hasMoodSwings) return null

        // Skip if soy allergy
        if (dietaryProfile.allergies.contains(FoodAllergy.SOY)) return null

        val hasEnoughPhytoestrogens = context.soyServings >= 1 || context.flaxseedTbsp >= 2

        if (hasEnoughPhytoestrogens) return null

        val quickWins = mutableListOf<QuickWin>()

        quickWins.add(QuickWin(
            action = "Tofu or tempeh (100g daily)",
            benefit = "Isoflavones may reduce hot flashes by 20-50%",
            swap = FoodSwap("Meat at one meal", "Tofu stir-fry", "Phytoestrogen boost")
        ))

        quickWins.add(QuickWin(
            action = "Edamame as snack",
            benefit = "Easy soy serving + protein",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "2 tbsp ground flaxseed daily",
            benefit = "Lignans = plant estrogens",
            swap = null
        ))

        quickWins.add(QuickWin(
            action = "Soy milk in smoothies/coffee",
            benefit = "Simple daily phytoestrogen source",
            swap = FoodSwap("Regular milk", "Soy milk", "+25mg isoflavones")
        ))

        return NutritionTip(
            category = TipCategory.PHYTOESTROGENS,
            priority = TipPriority.MEDIUM,
            headline = "Natural hormone helpers",
            explanation = "Phytoestrogens in soy and flaxseed can gently ease menopause symptoms. " +
                    "Studies show 40-80mg isoflavones daily may reduce hot flashes.",
            quickWins = quickWins.take(3),
            impact = "May reduce hot flash frequency by 20-50%"
        )
    }
}