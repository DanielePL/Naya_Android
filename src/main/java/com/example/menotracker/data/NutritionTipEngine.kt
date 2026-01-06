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
    HYDRATION
}

enum class TipPriority {
    CRITICAL,   // Needs immediate attention (e.g., trans fats, very low protein)
    HIGH,       // Should address soon
    MEDIUM,     // Room for improvement
    LOW         // Minor optimization
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
     * Get the most important tip to show
     */
    fun getTopTip(macroQuality: MacroQualitySummary): NutritionTip? {
        return generateTips(macroQuality).firstOrNull()
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
}