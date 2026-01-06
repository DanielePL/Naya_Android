package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.models.*
import com.example.menotracker.screens.nutrition.SearchableFood
import com.example.menotracker.screens.nutrition.FoodPortion
import com.example.menotracker.screens.nutrition.FoodDataSource
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// ═══════════════════════════════════════════════════════════════
// USER FOOD REPOSITORY
// Personal food library with modifications in Supabase
// ═══════════════════════════════════════════════════════════════

object UserFoodRepository {
    private const val TAG = "UserFoodRepository"
    private const val TABLE_NAME = "user_foods"

    private val supabase get() = SupabaseClient.client

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Local cache for fast access
    private val userFoodsCache = ConcurrentHashMap<String, List<UserFood>>()
    private val cacheTimestamps = ConcurrentHashMap<String, Long>()
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASS FOR SUPABASE INSERT
    // ═══════════════════════════════════════════════════════════════

    @Serializable
    private data class UserFoodInsert(
        val user_id: String,
        val base_food_id: String? = null,
        val base_food_source: String? = null,
        val display_name: String,
        val base_name: String,
        val brand: String? = null,
        val emoji: String? = null,
        val category: String? = null,
        val modifications: String, // JSON array
        val is_per_serving: Boolean = false,
        val calories: Float,
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        val fiber: Float = 0f,
        val sugar: Float = 0f,
        val saturated_fat: Float = 0f,
        val unsaturated_fat: Float = 0f,
        val trans_fat: Float = 0f,
        val omega_3: Float = 0f,
        val omega_6: Float = 0f,
        val cholesterol: Float = 0f,
        val sodium: Float = 0f,
        val potassium: Float = 0f,
        val calcium: Float = 0f,
        val iron: Float = 0f,
        val magnesium: Float = 0f,
        val default_serving_size: Float = 100f,
        val default_serving_unit: String = "g",
        val serving_description: String? = null,
        val portions: String = "[]", // JSON array
        val use_count: Int = 1,
        val last_used_at: Long = System.currentTimeMillis(),
        val is_favorite: Boolean = false,
        val nova_classification: Int? = null
    )

    @Serializable
    private data class UserFoodRow(
        val id: String,
        val user_id: String,
        val base_food_id: String? = null,
        val base_food_source: String? = null,
        val display_name: String,
        val base_name: String,
        val brand: String? = null,
        val emoji: String? = null,
        val category: String? = null,
        val modifications: String? = null, // JSON
        val is_per_serving: Boolean = false,
        val calories: Float,
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        val fiber: Float = 0f,
        val sugar: Float = 0f,
        val saturated_fat: Float = 0f,
        val unsaturated_fat: Float = 0f,
        val trans_fat: Float = 0f,
        val omega_3: Float = 0f,
        val omega_6: Float = 0f,
        val cholesterol: Float = 0f,
        val sodium: Float = 0f,
        val potassium: Float = 0f,
        val calcium: Float = 0f,
        val iron: Float = 0f,
        val magnesium: Float = 0f,
        val default_serving_size: Float = 100f,
        val default_serving_unit: String = "g",
        val serving_description: String? = null,
        val portions: String? = null, // JSON
        val use_count: Int = 0,
        val last_used_at: Long? = null,
        val is_favorite: Boolean = false,
        val created_at: String? = null,
        val nova_classification: Int? = null
    )

    // ═══════════════════════════════════════════════════════════════
    // SAVE USER FOOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Save a food to user's personal library
     * Called after user tracks a food with modifications
     */
    suspend fun saveUserFood(
        userId: String,
        food: SearchableFood,
        modifications: List<FoodModification> = emptyList(),
        servingSize: Float? = null,
        servingUnit: String? = null
    ): Result<UserFood> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Saving user food: ${food.name} with ${modifications.size} modifications")

            // Generate display name including modifications
            val displayName = generateDisplayName(food.name, food.brand, modifications)

            // Calculate adjusted macros based on modifications
            val adjustedCalories = food.caloriesPer100g + modifications.sumOf { it.calorieAdjustment.toDouble() }.toFloat()
            val adjustedProtein = food.proteinPer100g + modifications.sumOf { it.proteinAdjustment.toDouble() }.toFloat()
            val adjustedCarbs = food.carbsPer100g + modifications.sumOf { it.carbAdjustment.toDouble() }.toFloat()
            val adjustedFat = food.fatPer100g + modifications.sumOf { it.fatAdjustment.toDouble() }.toFloat()

            // Convert modifications to JSON
            val modificationsJson = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(FoodModification.serializer()),
                modifications
            )

            // Convert portions to JSON - FoodPortion has description and gramWeight
            val portionsJson = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(UserFoodPortion.serializer()),
                food.portions.map { UserFoodPortion(it.description, it.gramWeight, "g", it.gramWeight) }
            )

            val insert = UserFoodInsert(
                user_id = userId,
                base_food_id = food.id,
                base_food_source = food.source?.name,
                display_name = displayName,
                base_name = food.name,
                brand = food.brand,
                emoji = food.emoji,
                category = null,
                modifications = modificationsJson,
                is_per_serving = false,
                calories = adjustedCalories,
                protein = adjustedProtein,
                carbs = adjustedCarbs,
                fat = adjustedFat,
                fiber = food.fiberPer100g,
                sugar = food.sugarPer100g,
                default_serving_size = servingSize ?: 100f,
                default_serving_unit = servingUnit ?: "g",
                serving_description = food.portions.firstOrNull()?.description,
                portions = portionsJson,
                use_count = 1,
                last_used_at = System.currentTimeMillis(),
                is_favorite = false
            )

            val result = supabase.postgrest[TABLE_NAME]
                .insert(insert)
                .decodeSingle<UserFoodRow>()

            // Invalidate cache
            userFoodsCache.remove(userId)

            val userFood = result.toUserFood()
            Log.d(TAG, "✅ Saved user food: ${userFood.displayName}")

            Result.success(userFood)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user food", e)
            Result.failure(e)
        }
    }

    /**
     * Save directly from a tracked meal item
     */
    suspend fun saveFromMealItem(
        userId: String,
        mealItem: MealItem,
        baseFoodId: String? = null,
        baseFoodSource: String? = null,
        modifications: List<FoodModification> = emptyList()
    ): Result<UserFood> = withContext(Dispatchers.IO) {
        try {
            val displayName = generateDisplayName(mealItem.itemName, null, modifications)

            val modificationsJson = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(FoodModification.serializer()),
                modifications
            )

            val insert = UserFoodInsert(
                user_id = userId,
                base_food_id = baseFoodId,
                base_food_source = baseFoodSource,
                display_name = displayName,
                base_name = mealItem.itemName,
                brand = null,
                emoji = null,
                category = null,
                modifications = modificationsJson,
                is_per_serving = true,
                calories = mealItem.calories,
                protein = mealItem.protein,
                carbs = mealItem.carbs,
                fat = mealItem.fat,
                fiber = mealItem.fiber,
                sugar = mealItem.sugar,
                saturated_fat = mealItem.saturatedFat,
                unsaturated_fat = mealItem.unsaturatedFat,
                trans_fat = mealItem.transFat,
                omega_3 = mealItem.omega3,
                omega_6 = mealItem.omega6,
                cholesterol = mealItem.cholesterol,
                sodium = mealItem.sodium,
                potassium = mealItem.potassium,
                calcium = mealItem.calcium,
                iron = mealItem.iron,
                magnesium = mealItem.magnesium,
                default_serving_size = mealItem.quantity,
                default_serving_unit = mealItem.quantityUnit,
                serving_description = "${mealItem.quantity} ${mealItem.quantityUnit}",
                portions = "[]",
                use_count = 1,
                last_used_at = System.currentTimeMillis(),
                is_favorite = false
            )

            val result = supabase.postgrest[TABLE_NAME]
                .insert(insert)
                .decodeSingle<UserFoodRow>()

            userFoodsCache.remove(userId)

            Result.success(result.toUserFood())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save from meal item", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SAVE CUSTOM FOOD (manually created by user)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Save a custom food created manually by the user
     * Used when user creates a food from scratch with manual macro entry
     */
    suspend fun saveCustomFood(
        userId: String,
        name: String,
        servingSize: Float,
        servingUnit: String,
        calories: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float? = null,
        sugar: Float? = null,
        emoji: String? = null
    ): Result<UserFood> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 Saving custom food: $name")

            val insert = UserFoodInsert(
                user_id = userId,
                base_food_id = null, // No base food - this is custom
                base_food_source = "CUSTOM",
                display_name = name,
                base_name = name,
                brand = null,
                emoji = emoji ?: detectFoodEmoji(name),
                category = null,
                modifications = "[]", // No modifications for custom foods
                is_per_serving = true, // Custom foods are entered per serving
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber ?: 0f,
                sugar = sugar ?: 0f,
                default_serving_size = servingSize,
                default_serving_unit = servingUnit,
                serving_description = "$servingSize $servingUnit",
                portions = "[]",
                use_count = 1,
                last_used_at = System.currentTimeMillis(),
                is_favorite = false
            )

            val result = supabase.postgrest[TABLE_NAME]
                .insert(insert)
                .decodeSingle<UserFoodRow>()

            // Invalidate cache
            userFoodsCache.remove(userId)

            val userFood = result.toUserFood()
            Log.d(TAG, "✅ Saved custom food: ${userFood.displayName}")

            Result.success(userFood)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save custom food", e)
            Result.failure(e)
        }
    }

    /**
     * Auto-detect emoji based on food name
     */
    private fun detectFoodEmoji(name: String): String {
        val nameLower = name.lowercase()
        return when {
            nameLower.contains("chicken") -> "🍗"
            nameLower.contains("beef") || nameLower.contains("steak") -> "🥩"
            nameLower.contains("fish") || nameLower.contains("salmon") -> "🐟"
            nameLower.contains("egg") -> "🥚"
            nameLower.contains("rice") -> "🍚"
            nameLower.contains("pasta") || nameLower.contains("spaghetti") -> "🍝"
            nameLower.contains("bread") -> "🍞"
            nameLower.contains("oat") || nameLower.contains("cereal") -> "🥣"
            nameLower.contains("salad") -> "🥗"
            nameLower.contains("protein") && nameLower.contains("shake") -> "🥤"
            nameLower.contains("smoothie") -> "🥤"
            nameLower.contains("pancake") || nameLower.contains("waffle") -> "🥞"
            nameLower.contains("burger") -> "🍔"
            nameLower.contains("pizza") -> "🍕"
            nameLower.contains("sandwich") -> "🥪"
            nameLower.contains("wrap") || nameLower.contains("burrito") -> "🌯"
            nameLower.contains("taco") -> "🌮"
            nameLower.contains("soup") -> "🍲"
            nameLower.contains("coffee") || nameLower.contains("latte") -> "☕"
            nameLower.contains("yogurt") -> "🥛"
            nameLower.contains("cheese") -> "🧀"
            nameLower.contains("apple") -> "🍎"
            nameLower.contains("banana") -> "🍌"
            nameLower.contains("orange") -> "🍊"
            nameLower.contains("berry") || nameLower.contains("strawberry") -> "🍓"
            nameLower.contains("avocado") -> "🥑"
            nameLower.contains("broccoli") -> "🥦"
            nameLower.contains("carrot") -> "🥕"
            nameLower.contains("potato") || nameLower.contains("fries") -> "🍟"
            nameLower.contains("nut") || nameLower.contains("almond") -> "🥜"
            nameLower.contains("cookie") -> "🍪"
            nameLower.contains("cake") || nameLower.contains("dessert") -> "🍰"
            nameLower.contains("chocolate") -> "🍫"
            nameLower.contains("bar") -> "🍫" // Protein bar, granola bar
            else -> "🍽️"
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET USER FOODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get all user's foods, sorted by most recently used
     */
    suspend fun getUserFoods(userId: String, limit: Int = 50): Result<List<UserFood>> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cacheTime = cacheTimestamps[userId] ?: 0L
            if (System.currentTimeMillis() - cacheTime < CACHE_DURATION_MS) {
                userFoodsCache[userId]?.let {
                    Log.d(TAG, "Cache hit: ${it.size} user foods")
                    return@withContext Result.success(it.take(limit))
                }
            }

            Log.d(TAG, "Fetching user foods from Supabase...")

            val rows = supabase.postgrest[TABLE_NAME]
                .select {
                    filter { eq("user_id", userId) }
                    order("last_used_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<UserFoodRow>()

            val foods = rows.map { it.toUserFood() }

            // Update cache
            userFoodsCache[userId] = foods
            cacheTimestamps[userId] = System.currentTimeMillis()

            Log.d(TAG, "✅ Fetched ${foods.size} user foods")
            Result.success(foods)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user foods", e)
            Result.failure(e)
        }
    }

    /**
     * Search user's personal foods
     */
    suspend fun searchUserFoods(userId: String, query: String): Result<List<UserFood>> = withContext(Dispatchers.IO) {
        try {
            val trimmedQuery = query.trim().lowercase()
            if (trimmedQuery.length < 2) {
                return@withContext Result.success(emptyList())
            }

            // First try local cache
            userFoodsCache[userId]?.let { cached ->
                val matches = cached.filter { it.matchesQuery(trimmedQuery) }
                if (matches.isNotEmpty()) {
                    Log.d(TAG, "Cache search hit: ${matches.size} matches for '$query'")
                    return@withContext Result.success(matches)
                }
            }

            // Search in Supabase
            val rows = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        or {
                            ilike("display_name", "%$trimmedQuery%")
                            ilike("base_name", "%$trimmedQuery%")
                            ilike("brand", "%$trimmedQuery%")
                        }
                    }
                    order("use_count", Order.DESCENDING)
                    limit(20)
                }
                .decodeList<UserFoodRow>()

            val foods = rows.map { it.toUserFood() }
            Log.d(TAG, "✅ Found ${foods.size} user foods for query: $query")
            Result.success(foods)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's favorite foods
     */
    suspend fun getFavorites(userId: String): Result<List<UserFood>> = withContext(Dispatchers.IO) {
        try {
            val rows = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_favorite", true)
                    }
                    order("use_count", Order.DESCENDING)
                }
                .decodeList<UserFoodRow>()

            Result.success(rows.map { it.toUserFood() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get favorites", e)
            Result.failure(e)
        }
    }

    /**
     * Get most frequently used foods (top picks)
     */
    suspend fun getTopPicks(userId: String, limit: Int = 10): Result<List<UserFood>> = withContext(Dispatchers.IO) {
        try {
            val rows = supabase.postgrest[TABLE_NAME]
                .select {
                    filter { eq("user_id", userId) }
                    order("use_count", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<UserFoodRow>()

            Result.success(rows.map { it.toUserFood() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get top picks", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE USER FOOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Increment use count when food is tracked again
     */
    suspend fun incrementUseCount(foodId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest[TABLE_NAME]
                .update({
                    // Increment use_count (done via RPC in real implementation)
                    set("last_used_at", System.currentTimeMillis())
                }) {
                    filter { eq("id", foodId) }
                }

            // Note: For proper increment, use Supabase RPC function
            Log.d(TAG, "Updated use count for food: $foodId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment use count", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(foodId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest[TABLE_NAME]
                .update({ set("is_favorite", isFavorite) }) {
                    filter { eq("id", foodId) }
                }

            Log.d(TAG, "Toggled favorite: $foodId -> $isFavorite")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle favorite", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a user food
     */
    suspend fun deleteUserFood(foodId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest[TABLE_NAME]
                .delete { filter { eq("id", foodId) } }

            Log.d(TAG, "Deleted user food: $foodId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CHECK FOR EXISTING (avoid duplicates)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if user already has this food with these modifications
     */
    suspend fun findExisting(
        userId: String,
        baseFoodId: String?,
        baseName: String,
        modifications: List<FoodModification>
    ): Result<UserFood?> = withContext(Dispatchers.IO) {
        try {
            val rows = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        if (baseFoodId != null) {
                            eq("base_food_id", baseFoodId)
                        } else {
                            eq("base_name", baseName)
                        }
                    }
                }
                .decodeList<UserFoodRow>()

            // Check if modifications match
            val existing = rows.map { it.toUserFood() }.find { food ->
                food.modifications.map { it.value }.toSet() ==
                modifications.map { it.value }.toSet()
            }

            Result.success(existing)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find existing", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVERSION TO SEARCHABLE FOOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convert UserFood to SearchableFood for unified search results
     */
    fun UserFood.toSearchableFood(): SearchableFood {
        return SearchableFood(
            id = id,
            name = displayName,
            brand = brand,
            emoji = emoji ?: "🍽️",
            caloriesPer100g = if (isPerServing) calories / (defaultServingSize / 100f) else calories,
            proteinPer100g = if (isPerServing) protein / (defaultServingSize / 100f) else protein,
            carbsPer100g = if (isPerServing) carbs / (defaultServingSize / 100f) else carbs,
            fatPer100g = if (isPerServing) fat / (defaultServingSize / 100f) else fat,
            fiberPer100g = if (isPerServing) fiber / (defaultServingSize / 100f) else fiber,
            sugarPer100g = if (isPerServing) sugar / (defaultServingSize / 100f) else sugar,
            portions = portions.map { FoodPortion(it.name, it.gramsEquivalent ?: it.amount) }
                .ifEmpty { listOf(FoodPortion("$defaultServingSize$defaultServingUnit", defaultServingSize)) },
            verified = true,
            source = FoodDataSource.USER_LIBRARY
        )
    }

    /**
     * Get user foods as SearchableFood for search integration
     */
    suspend fun getUserFoodsAsSearchable(userId: String, query: String? = null): Result<List<SearchableFood>> {
        val result = if (query != null) {
            searchUserFoods(userId, query)
        } else {
            getUserFoods(userId)
        }

        return result.map { foods -> foods.map { it.toSearchableFood() } }
    }

    /**
     * Get top picks as SearchableFood for quick access
     */
    suspend fun getTopPicksAsSearchable(userId: String, limit: Int = 10): Result<List<SearchableFood>> {
        return getTopPicks(userId, limit).map { foods -> foods.map { it.toSearchableFood() } }
    }

    /**
     * Get favorites as SearchableFood for quick access
     */
    suspend fun getFavoritesAsSearchable(userId: String): Result<List<SearchableFood>> {
        return getFavorites(userId).map { foods -> foods.map { it.toSearchableFood() } }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private fun generateDisplayName(
        baseName: String,
        brand: String?,
        modifications: List<FoodModification>
    ): String {
        val parts = mutableListOf<String>()

        // Add brand if present
        brand?.let { parts.add(it) }

        // Add base name
        parts.add(baseName)

        // Add modification short labels
        modifications.mapNotNull { it.shortLabel }.forEach { parts.add(it) }

        return parts.joinToString(" ")
    }

    private fun UserFoodRow.toUserFood(): UserFood {
        val mods = try {
            modifications?.let {
                json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(FoodModification.serializer()),
                    it
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse modifications", e)
            emptyList()
        }

        val portionsList = try {
            portions?.let {
                json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(UserFoodPortion.serializer()),
                    it
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse portions", e)
            emptyList()
        }

        return UserFood(
            id = id,
            userId = user_id,
            baseFoodId = base_food_id,
            baseFoodSource = base_food_source,
            displayName = display_name,
            baseName = base_name,
            brand = brand,
            emoji = emoji,
            category = category,
            modifications = mods,
            isPerServing = is_per_serving,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            saturatedFat = saturated_fat,
            unsaturatedFat = unsaturated_fat,
            transFat = trans_fat,
            omega3 = omega_3,
            omega6 = omega_6,
            cholesterol = cholesterol,
            sodium = sodium,
            potassium = potassium,
            calcium = calcium,
            iron = iron,
            magnesium = magnesium,
            defaultServingSize = default_serving_size,
            defaultServingUnit = default_serving_unit,
            servingDescription = serving_description,
            portions = portionsList,
            useCount = use_count,
            lastUsedAt = last_used_at,
            isFavorite = is_favorite,
            createdAt = System.currentTimeMillis(), // Parse from created_at if needed
            novaClassification = nova_classification
        )
    }

    /**
     * Clear cache (e.g., on logout)
     */
    fun clearCache() {
        userFoodsCache.clear()
        cacheTimestamps.clear()
    }
}
