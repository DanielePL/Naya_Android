// data/repository/FoodSearchRepository.kt
package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.RestaurantRepository
import com.example.menotracker.data.models.NayaFood
import com.example.menotracker.data.repository.OpenFoodFactsRepository
import com.example.menotracker.data.repository.UserFoodRepository
import com.example.menotracker.data.models.FoodSource as ModelFoodSource
import com.example.menotracker.screens.nutrition.FoodDataSource
import com.example.menotracker.screens.nutrition.FoodPortion
import com.example.menotracker.screens.nutrition.FoodSource
import com.example.menotracker.screens.nutrition.SearchableFood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * FOOD SEARCH REPOSITORY
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Unified search across multiple food data sources:
 * - USDA FoodData Central (1.9M+ foods)
 * - Restaurant Database (500+ fast food meals)
 * - Open Food Facts (coming soon)
 * - User's recent/favorite foods
 *
 * Handles:
 * - Parallel API calls for speed
 * - Result normalization to SearchableFood
 * - Caching for performance
 * - Error handling per source
 * ═══════════════════════════════════════════════════════════════════════════════
 */
object FoodSearchRepository {

    private const val TAG = "FoodSearchRepository"

    // Search result cache (query -> results)
    private val searchCache = ConcurrentHashMap<String, CachedSearchResults>()
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // Recent foods cache per user
    private val recentFoodsCache = ConcurrentHashMap<String, List<SearchableFood>>()


    // ═══════════════════════════════════════════════════════════════════════════════
    // MAIN SEARCH FUNCTION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search for foods across all enabled sources
     *
     * @param query Search term
     * @param source Filter by source (ALL, USDA, RESTAURANT, RECENT)
     * @param userId User ID for recent foods lookup
     * @param limit Max results per source
     * @return Combined list of SearchableFood from all sources
     */
    suspend fun searchFoods(
        query: String,
        source: FoodSource = FoodSource.ALL,
        userId: String? = null,
        limit: Int = 25
    ): Result<List<SearchableFood>> = withContext(Dispatchers.IO) {
        try {
            val trimmedQuery = query.trim().lowercase()

            if (trimmedQuery.length < 2) {
                return@withContext Result.success(emptyList())
            }

            // Check cache first
            val cacheKey = "${trimmedQuery}_${source.name}_$limit"
            searchCache[cacheKey]?.let { cached ->
                if (!cached.isExpired()) {
                    Log.d(TAG, "Cache hit for: $trimmedQuery")
                    return@withContext Result.success(cached.results)
                }
            }

            Log.d(TAG, "🔍 Searching for: $trimmedQuery (source: $source)")

            val results = mutableListOf<SearchableFood>()
            val errors = mutableListOf<String>()

            // Parallel search based on selected source
            when (source) {
                FoodSource.ALL -> {
                    // Search all sources in parallel - USER FOODS FIRST for priority!
                    val deferredResults = listOf(
                        async { if (userId != null) searchUserFoods(userId, trimmedQuery) else Result.success(emptyList()) },
                        async { searchUSDA(trimmedQuery, limit / 2) },
                        async { searchOpenFoodFacts(trimmedQuery, limit / 2) },
                        async { searchRestaurants(trimmedQuery, limit / 3) }
                    )

                    deferredResults.awaitAll().forEachIndexed { index, result ->
                        when (index) {
                            0 -> result.fold(
                                onSuccess = { results.addAll(it) },
                                onFailure = { errors.add("My Foods: ${it.message}") }
                            )
                            1 -> result.fold(
                                onSuccess = { results.addAll(it) },
                                onFailure = { errors.add("USDA: ${it.message}") }
                            )
                            2 -> result.fold(
                                onSuccess = { results.addAll(it) },
                                onFailure = { errors.add("Products: ${it.message}") }
                            )
                            3 -> result.fold(
                                onSuccess = { results.addAll(it) },
                                onFailure = { errors.add("Restaurant: ${it.message}") }
                            )
                        }
                    }
                }

                FoodSource.MY_FOODS -> {
                    // User's personal food library (saved foods with modifications)
                    if (userId != null) {
                        searchUserFoods(userId, trimmedQuery).fold(
                            onSuccess = { results.addAll(it) },
                            onFailure = { errors.add("My Foods: ${it.message}") }
                        )
                    }
                }

                FoodSource.USDA -> {
                    searchUSDA(trimmedQuery, limit).fold(
                        onSuccess = { results.addAll(it) },
                        onFailure = { errors.add("USDA: ${it.message}") }
                    )
                }

                FoodSource.RESTAURANT -> {
                    searchRestaurants(trimmedQuery, limit).fold(
                        onSuccess = { results.addAll(it) },
                        onFailure = { errors.add("Restaurant: ${it.message}") }
                    )
                }

                FoodSource.RECENT -> {
                    if (userId != null) {
                        getRecentFoods(userId, trimmedQuery).fold(
                            onSuccess = { results.addAll(it) },
                            onFailure = { errors.add("Recent: ${it.message}") }
                        )
                    }
                }

                FoodSource.OPEN_FOOD_FACTS -> {
                    searchOpenFoodFacts(trimmedQuery, limit).fold(
                        onSuccess = { results.addAll(it) },
                        onFailure = { errors.add("Products: ${it.message}") }
                    )
                }
            }

            // Sort results: USER_LIBRARY FIRST, then verified, then by relevance (name match)
            val sortedResults = results.sortedWith(
                compareByDescending<SearchableFood> { it.source == FoodDataSource.USER_LIBRARY }  // User's foods at the TOP!
                    .thenByDescending { it.verified }
                    .thenByDescending { it.name.lowercase().startsWith(trimmedQuery) }
                    .thenByDescending { it.name.lowercase().contains(trimmedQuery) }
                    .thenBy { it.name }
            )

            // Cache results
            searchCache[cacheKey] = CachedSearchResults(sortedResults)

            Log.d(TAG, "✅ Found ${sortedResults.size} results (${errors.size} errors)")
            if (errors.isNotEmpty()) {
                Log.w(TAG, "Search errors: ${errors.joinToString()}")
            }

            Result.success(sortedResults)

        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // USDA SEARCH
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search USDA FoodData Central
     */
    private suspend fun searchUSDA(query: String, limit: Int): Result<List<SearchableFood>> {
        return try {
            Log.d(TAG, "🥗 Searching USDA for: $query")

            USDARepository.quickSearch(query, limit).map { foods ->
                foods.map { food -> food.toSearchableFood() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "USDA search error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // OPEN FOOD FACTS SEARCH
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search Open Food Facts (packaged products, barcoded items)
     */
    private suspend fun searchOpenFoodFacts(query: String, limit: Int): Result<List<SearchableFood>> {
        return try {
            Log.d(TAG, "📦 Searching Open Food Facts for: $query")

            OpenFoodFactsRepository.searchProductsNormalized(query, limit).map { foods ->
                foods.map { food -> food.toSearchableFood() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Open Food Facts search error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // RESTAURANT SEARCH
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search Restaurant Database
     */
    private suspend fun searchRestaurants(query: String, limit: Int): Result<List<SearchableFood>> {
        return try {
            Log.d(TAG, "🍔 Searching Restaurants for: $query")

            val result = RestaurantRepository.searchMeals(query, limit)

            result.map { meals ->
                meals.map { meal ->
                    // Restaurant meals are typically per-serving, convert to per-100g for consistency
                    val servingGrams = meal.servingSize ?: 100f
                    val multiplier = 100f / servingGrams

                    SearchableFood(
                        id = "rest_${meal.id}",
                        source = FoodDataSource.RESTAURANT,
                        name = meal.name,
                        brand = meal.chain?.name,  // Get chain name from nested object
                        emoji = getRestaurantEmoji(meal.category),
                        caloriesPer100g = meal.calories * multiplier,
                        proteinPer100g = meal.protein * multiplier,
                        carbsPer100g = meal.carbs * multiplier,
                        fatPer100g = meal.fat * multiplier,
                        fiberPer100g = meal.fiber * multiplier,
                        sugarPer100g = meal.sugar * multiplier,
                        sodiumPer100g = meal.sodium * multiplier,
                        portions = listOf(
                            FoodPortion("1 serving (${servingGrams.toInt()}g)", servingGrams),
                            FoodPortion("100g", 100f)
                        ),
                        defaultPortion = FoodPortion("1 serving (${servingGrams.toInt()}g)", servingGrams),
                        category = meal.category,
                        verified = meal.isVerified
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restaurant search error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // USER FOODS (Personal Library with Modifications)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search user's personal food library from Supabase
     * This includes saved foods with modifications (e.g., "Matcha Latte Oat Milk No Sugar")
     */
    private suspend fun searchUserFoods(userId: String, query: String): Result<List<SearchableFood>> {
        return try {
            Log.d(TAG, "⭐ Searching user's personal foods for: $query")

            // Get user foods from Supabase (already converted to SearchableFood)
            UserFoodRepository.getUserFoodsAsSearchable(userId, query)

        } catch (e: Exception) {
            Log.e(TAG, "User foods search error", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's top picks (favorites + most used)
     * Perfect for quick access in search screen
     */
    suspend fun getUserTopPicks(userId: String, limit: Int = 10): Result<List<SearchableFood>> {
        return try {
            Log.d(TAG, "⭐ Getting top picks for user: $userId")
            UserFoodRepository.getTopPicksAsSearchable(userId, limit)
        } catch (e: Exception) {
            Log.e(TAG, "Top picks error", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's favorite foods
     */
    suspend fun getUserFavorites(userId: String): Result<List<SearchableFood>> {
        return try {
            Log.d(TAG, "❤️ Getting favorites for user: $userId")
            UserFoodRepository.getFavoritesAsSearchable(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Favorites error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // RECENT FOODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get user's recently logged foods
     */
    private suspend fun getRecentFoods(userId: String, query: String? = null): Result<List<SearchableFood>> {
        return try {
            // Check cache first
            recentFoodsCache[userId]?.let { cached ->
                val filtered = if (query != null) {
                    cached.filter { it.name.lowercase().contains(query.lowercase()) }
                } else {
                    cached
                }
                if (filtered.isNotEmpty()) {
                    return Result.success(filtered)
                }
            }

            Log.d(TAG, "🕐 Loading recent foods from Supabase...")

            // Load from NutritionRepository
            val recentItemsResult = com.example.menotracker.data.NutritionRepository.getRecentMealItems(userId, 50)

            if (recentItemsResult.isFailure) {
                Log.e(TAG, "❌ Failed to load recent foods: ${recentItemsResult.exceptionOrNull()?.message}")
                return Result.success(emptyList())
            }

            val recentItems = recentItemsResult.getOrThrow()

            // Convert to SearchableFood
            val searchableFoods = recentItems.map { item ->
                // Calculate per 100g values from the stored quantity
                val quantityGrams = if (item.quantity_unit.lowercase() == "g") {
                    item.quantity
                } else {
                    // Assume 100g if not grams (for items like "1 serving")
                    100f
                }
                val multiplier = if (quantityGrams > 0) 100f / quantityGrams else 1f

                SearchableFood(
                    id = "recent_${item.id}",
                    source = FoodDataSource.RECENT,
                    name = item.item_name,
                    emoji = getFoodEmoji(item.item_name),
                    caloriesPer100g = item.calories * multiplier,
                    proteinPer100g = item.protein * multiplier,
                    carbsPer100g = item.carbs * multiplier,
                    fatPer100g = item.fat * multiplier,
                    fiberPer100g = item.fiber * multiplier,
                    sugarPer100g = item.sugar * multiplier,
                    sodiumPer100g = item.sodium * multiplier,
                    portions = listOf(
                        FoodPortion("${item.quantity.toInt()}${item.quantity_unit}", item.quantity),
                        FoodPortion("100g", 100f)
                    ),
                    defaultPortion = FoodPortion("${item.quantity.toInt()}${item.quantity_unit}", item.quantity),
                    verified = true
                )
            }

            // Cache the results
            recentFoodsCache[userId] = searchableFoods
            Log.d(TAG, "✅ Loaded ${searchableFoods.size} recent foods")

            // Filter by query if provided
            val filtered = if (query != null) {
                searchableFoods.filter { it.name.lowercase().contains(query.lowercase()) }
            } else {
                searchableFoods
            }

            Result.success(filtered)

        } catch (e: Exception) {
            Log.e(TAG, "Recent foods error", e)
            Result.failure(e)
        }
    }

    /**
     * Get emoji for food based on name
     */
    private fun getFoodEmoji(foodName: String): String {
        val nameLower = foodName.lowercase()
        return when {
            // Proteins
            nameLower.contains("chicken") -> "🍗"
            nameLower.contains("beef") || nameLower.contains("steak") -> "🥩"
            nameLower.contains("fish") || nameLower.contains("salmon") || nameLower.contains("tuna") -> "🐟"
            nameLower.contains("egg") -> "🥚"
            nameLower.contains("pork") || nameLower.contains("bacon") -> "🥓"
            nameLower.contains("shrimp") || nameLower.contains("prawn") -> "🦐"

            // Dairy
            nameLower.contains("milk") || nameLower.contains("yogurt") -> "🥛"
            nameLower.contains("cheese") -> "🧀"

            // Grains
            nameLower.contains("rice") -> "🍚"
            nameLower.contains("bread") || nameLower.contains("toast") -> "🍞"
            nameLower.contains("pasta") || nameLower.contains("spaghetti") -> "🍝"
            nameLower.contains("oat") || nameLower.contains("cereal") -> "🥣"

            // Fruits
            nameLower.contains("apple") -> "🍎"
            nameLower.contains("banana") -> "🍌"
            nameLower.contains("orange") -> "🍊"
            nameLower.contains("grape") -> "🍇"
            nameLower.contains("strawberry") || nameLower.contains("berry") -> "🍓"
            nameLower.contains("mango") -> "🥭"
            nameLower.contains("avocado") -> "🥑"

            // Vegetables
            nameLower.contains("broccoli") -> "🥦"
            nameLower.contains("carrot") -> "🥕"
            nameLower.contains("salad") || nameLower.contains("lettuce") -> "🥗"
            nameLower.contains("potato") || nameLower.contains("fries") -> "🍟"
            nameLower.contains("tomato") -> "🍅"
            nameLower.contains("corn") -> "🌽"

            // Fast food
            nameLower.contains("burger") -> "🍔"
            nameLower.contains("pizza") -> "🍕"
            nameLower.contains("taco") || nameLower.contains("burrito") -> "🌮"
            nameLower.contains("sandwich") || nameLower.contains("sub") -> "🥪"
            nameLower.contains("hot dog") -> "🌭"

            // Drinks
            nameLower.contains("coffee") || nameLower.contains("latte") -> "☕"
            nameLower.contains("shake") || nameLower.contains("smoothie") -> "🥤"
            nameLower.contains("juice") -> "🧃"
            nameLower.contains("protein") && nameLower.contains("shake") -> "🥛"

            // Sweets
            nameLower.contains("cake") || nameLower.contains("dessert") -> "🍰"
            nameLower.contains("cookie") -> "🍪"
            nameLower.contains("chocolate") -> "🍫"
            nameLower.contains("ice cream") -> "🍨"

            // Nuts & Seeds
            nameLower.contains("nut") || nameLower.contains("almond") || nameLower.contains("peanut") -> "🥜"

            // Default
            else -> "🍽️"
        }
    }

    /**
     * Add a food to recent foods cache
     */
    fun addToRecentFoods(userId: String, food: SearchableFood) {
        val current = recentFoodsCache[userId]?.toMutableList() ?: mutableListOf()

        // Remove if already exists (to move to top)
        current.removeAll { it.id == food.id }

        // Add to beginning
        current.add(0, food.copy(source = FoodDataSource.RECENT))

        // Keep only last 50
        recentFoodsCache[userId] = current.take(50)
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // BARCODE LOOKUP
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search by barcode using OpenFoodFacts with USDA fallback
     */
    suspend fun searchByBarcode(barcode: String): Result<SearchableFood?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Searching barcode: $barcode")

            // Use OpenFoodFacts with USDA fallback
            OpenFoodFactsRepository.searchBarcodeWithFallback(barcode).map { nayaFood ->
                nayaFood?.toSearchableFood()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Barcode search error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // FOOD DETAILS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get full details for a food item
     */
    suspend fun getFoodDetails(foodId: String): Result<SearchableFood> = withContext(Dispatchers.IO) {
        try {
            when {
                foodId.startsWith("usda_") -> {
                    // Extract USDA FDC ID from the food ID
                    val fdcIdString = foodId.removePrefix("usda_")
                    val fdcId = fdcIdString.toIntOrNull()

                    if (fdcId == null) {
                        Log.e(TAG, "Invalid USDA food ID: $foodId")
                        return@withContext Result.failure(IllegalArgumentException("Invalid USDA food ID format"))
                    }

                    Log.d(TAG, "🔍 Fetching USDA details for FDC ID: $fdcId")

                    // Get details from USDA API
                    USDARepository.getFoodDetailsNormalized(fdcId).map { nayaFood ->
                        nayaFood.toSearchableFood()
                    }
                }

                foodId.startsWith("rest_") -> {
                    val mealId = foodId.removePrefix("rest_")
                    val mealResult = RestaurantRepository.getMeal(mealId)

                    mealResult.mapCatching { meal ->
                        if (meal == null) throw IllegalArgumentException("Meal not found: $mealId")

                        val servingGrams = meal.servingSize ?: 100f
                        val multiplier = 100f / servingGrams

                        SearchableFood(
                            id = foodId,
                            source = FoodDataSource.RESTAURANT,
                            name = meal.name,
                            brand = meal.chain?.name,
                            emoji = getRestaurantEmoji(meal.category),
                            caloriesPer100g = meal.calories * multiplier,
                            proteinPer100g = meal.protein * multiplier,
                            carbsPer100g = meal.carbs * multiplier,
                            fatPer100g = meal.fat * multiplier,
                            portions = listOf(
                                FoodPortion("1 serving (${servingGrams.toInt()}g)", servingGrams)
                            ),
                            category = meal.category,
                            verified = meal.isVerified
                        )
                    }
                }

                foodId.startsWith("quick_") -> {
                    // Quick pick foods - return from static list
                    val quickFood = getQuickPickFoods().find { it.id == foodId }
                    if (quickFood != null) {
                        Result.success(quickFood)
                    } else {
                        Result.failure(IllegalArgumentException("Quick pick not found: $foodId"))
                    }
                }

                else -> Result.failure(IllegalArgumentException("Unknown food source"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get details error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // QUICK PICKS (Common Foods)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get common quick-pick foods
     */
    fun getQuickPickFoods(): List<SearchableFood> {
        return listOf(
            createQuickPick("quick_chicken", "Chicken Breast", "🍗", 165f, 31f, 0f, 3.6f,
                listOf(FoodPortion("1 breast (174g)", 174f), FoodPortion("100g", 100f))),
            createQuickPick("quick_rice", "White Rice (cooked)", "🍚", 130f, 2.7f, 28f, 0.3f,
                listOf(FoodPortion("1 cup (158g)", 158f), FoodPortion("100g", 100f))),
            createQuickPick("quick_banana", "Banana", "🍌", 89f, 1.1f, 23f, 0.3f,
                listOf(FoodPortion("1 medium (118g)", 118f), FoodPortion("100g", 100f))),
            createQuickPick("quick_eggs", "Eggs (whole)", "🥚", 155f, 13f, 1.1f, 11f,
                listOf(FoodPortion("1 large (50g)", 50f), FoodPortion("2 eggs (100g)", 100f))),
            createQuickPick("quick_oats", "Oatmeal (cooked)", "🥣", 68f, 2.4f, 12f, 1.4f,
                listOf(FoodPortion("1 cup (234g)", 234f), FoodPortion("100g", 100f))),
            createQuickPick("quick_salmon", "Salmon", "🐟", 208f, 20f, 0f, 13f,
                listOf(FoodPortion("1 fillet (150g)", 150f), FoodPortion("100g", 100f))),
            createQuickPick("quick_broccoli", "Broccoli", "🥦", 34f, 2.8f, 7f, 0.4f,
                listOf(FoodPortion("1 cup (91g)", 91f), FoodPortion("100g", 100f))),
            createQuickPick("quick_avocado", "Avocado", "🥑", 160f, 2f, 9f, 15f,
                listOf(FoodPortion("1/2 avocado (100g)", 100f), FoodPortion("1 whole (200g)", 200f)))
        )
    }

    private fun createQuickPick(
        id: String,
        name: String,
        emoji: String,
        calories: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        portions: List<FoodPortion>
    ): SearchableFood {
        return SearchableFood(
            id = id,
            source = FoodDataSource.USDA,
            name = name,
            emoji = emoji,
            caloriesPer100g = calories,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            portions = portions,
            defaultPortion = portions.first(),
            verified = true
        )
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════

    fun clearCache() {
        searchCache.clear()
        recentFoodsCache.clear()
        Log.d(TAG, "Cache cleared")
    }

    fun clearExpiredCache() {
        searchCache.entries.removeIf { it.value.isExpired() }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun getCategoryEmoji(category: String?): String {
        return when (category?.lowercase()) {
            // Proteins
            "poultry", "poultry products" -> "🍗"
            "beef", "beef products" -> "🥩"
            "pork", "pork products" -> "🥓"
            "fish", "finfish and shellfish products" -> "🐟"
            "lamb", "lamb, veal, and game products" -> "🍖"
            "sausages and luncheon meats" -> "🌭"

            // Dairy & Eggs
            "dairy", "dairy and egg products" -> "🥛"
            "cheese" -> "🧀"
            "eggs" -> "🥚"

            // Grains
            "grains", "cereal grains and pasta" -> "🌾"
            "bread", "baked products" -> "🍞"
            "breakfast cereals" -> "🥣"

            // Fruits & Vegetables
            "fruits", "fruits and fruit juices" -> "🍎"
            "vegetables", "vegetables and vegetable products" -> "🥬"
            "legumes", "legumes and legume products" -> "🫘"

            // Nuts & Seeds
            "nuts", "nut and seed products" -> "🥜"

            // Beverages
            "beverages" -> "🥤"
            "coffee" -> "☕"

            // Sweets
            "sweets", "sugars and sweets" -> "🍬"
            "snacks" -> "🍿"

            // Fast Food Categories
            "burgers" -> "🍔"
            "pizza" -> "🍕"
            "mexican" -> "🌮"
            "chicken" -> "🍗"
            "sandwiches" -> "🥪"
            "salads" -> "🥗"
            "breakfast" -> "🍳"
            "desserts" -> "🍰"
            "drinks" -> "🥤"
            "sides" -> "🍟"

            // Default
            else -> "🍽️"
        }
    }

    private fun getRestaurantEmoji(category: String?): String {
        return when (category?.lowercase()) {
            "burgers" -> "🍔"
            "pizza" -> "🍕"
            "mexican", "burritos", "tacos" -> "🌮"
            "chicken", "wings" -> "🍗"
            "sandwiches", "subs" -> "🥪"
            "salads", "bowls" -> "🥗"
            "breakfast" -> "🍳"
            "desserts", "ice cream" -> "🍨"
            "drinks", "beverages", "coffee" -> "☕"
            "sides", "fries" -> "🍟"
            "asian", "chinese" -> "🥡"
            "seafood" -> "🦐"
            else -> "🍽️"
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// CACHE CLASS
// ═══════════════════════════════════════════════════════════════════════════════

private data class CachedSearchResults(
    val results: List<SearchableFood>,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 5 * 60 * 1000L
}


// ═══════════════════════════════════════════════════════════════════════════════
// PROMETHEUSFOOOD -> SEARCHABLEFOOD EXTENSION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Convert NayaFood (from API) to SearchableFood (for UI)
 */
fun NayaFood.toSearchableFood(): SearchableFood {
    // Determine the FoodDataSource from NayaFood.source
    val dataSource = when (source) {
        ModelFoodSource.USDA_FOUNDATION,
        ModelFoodSource.USDA_SR_LEGACY,
        ModelFoodSource.USDA_BRANDED,
        ModelFoodSource.USDA_SURVEY -> FoodDataSource.USDA
        ModelFoodSource.OPEN_FOOD_FACTS -> FoodDataSource.OPEN_FOOD_FACTS
        ModelFoodSource.RESTAURANT -> FoodDataSource.RESTAURANT
        ModelFoodSource.USER_CREATED -> FoodDataSource.USER_CREATED
        ModelFoodSource.AI_ANALYZED -> FoodDataSource.USDA
    }

    // Build portions list from NayaFood
    val portionsList = portions.map { portion ->
        FoodPortion(
            description = portion.description,
            gramWeight = portion.gramWeight
        )
    }.ifEmpty {
        // Default portions if none provided
        listOf(FoodPortion("100g", 100f))
    }

    // Determine default portion
    val effectiveServingSize = servingSize ?: 100f
    val defaultPortionItem = if (effectiveServingSize > 0 && effectiveServingSize != 100f) {
        FoodPortion(servingDescription ?: "${effectiveServingSize.toInt()}g", effectiveServingSize)
    } else {
        portionsList.firstOrNull() ?: FoodPortion("100g", 100f)
    }

    // Get emoji based on category
    val emoji = getCategoryEmoji(category)

    return SearchableFood(
        id = "usda_${id.removePrefix("usda_").removePrefix("off_")}",
        source = dataSource,
        name = name,
        brand = brand,
        emoji = emoji,
        // All values are per 100g
        caloriesPer100g = calories,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        fiberPer100g = fiber,
        sugarPer100g = sugar,
        sodiumPer100g = sodium,
        portions = portionsList,
        defaultPortion = defaultPortionItem,
        barcode = barcode,
        category = category,
        verified = verified
    )
}

/**
 * Get emoji for food category
 */
private fun getCategoryEmoji(category: String?): String {
    return when (category?.lowercase()) {
        // Proteins
        "poultry", "poultry products" -> "🍗"
        "beef", "beef products" -> "🥩"
        "pork", "pork products" -> "🥓"
        "fish", "finfish and shellfish products" -> "🐟"
        "lamb", "lamb, veal, and game products" -> "🍖"
        "sausages and luncheon meats" -> "🌭"

        // Dairy & Eggs
        "dairy", "dairy and egg products" -> "🥛"
        "cheese" -> "🧀"
        "eggs" -> "🥚"

        // Grains
        "grains", "cereal grains and pasta" -> "🌾"
        "bread", "baked products" -> "🍞"
        "breakfast cereals" -> "🥣"

        // Fruits & Vegetables
        "fruits", "fruits and fruit juices" -> "🍎"
        "vegetables", "vegetables and vegetable products" -> "🥬"
        "legumes", "legumes and legume products" -> "🫘"

        // Nuts & Seeds
        "nuts", "nut and seed products" -> "🥜"

        // Beverages
        "beverages" -> "🥤"
        "coffee" -> "☕"

        // Sweets
        "sweets", "sugars and sweets" -> "🍬"
        "snacks" -> "🍿"

        // Default
        else -> "🍽️"
    }
}



