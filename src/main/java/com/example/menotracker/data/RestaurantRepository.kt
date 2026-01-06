// app/src/main/java/com/example/myapplicationtest/data/RestaurantRepository.kt

package com.example.menotracker.data

import android.util.Log
import com.example.menotracker.data.models.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ═══════════════════════════════════════════════════════════════════════════════
// RESTAURANT REPOSITORY
// ═══════════════════════════════════════════════════════════════════════════════
//
// Handles all restaurant-related data operations:
// • Geo-based restaurant discovery (find nearby)
// • Restaurant & chain lookups
// • Menu/meal queries with regional variants
// • User history & "your usual" tracking
// • Crowdsourced submissions
//
// ═══════════════════════════════════════════════════════════════════════════════

object RestaurantRepository {
    private const val TAG = "RestaurantRepository"

    // ═══════════════════════════════════════════════════════════════════════════
    // GEO QUERIES - Find Nearby Restaurants
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find restaurants within a radius of the user's location
     * Uses PostGIS for accurate distance calculations
     *
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param radiusMeters Search radius (default 500m)
     * @param maxResults Maximum results to return (default 20)
     */
    suspend fun findNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 500,
        maxResults: Int = 20
    ): Result<List<NearbyRestaurant>> {
        return try {
            Log.d(TAG, "📍 Finding restaurants near ($latitude, $longitude) within ${radiusMeters}m")

            val result = SupabaseClient.client.postgrest.rpc(
                function = "find_nearby_restaurants",
                parameters = buildJsonObject {
                    put("p_latitude", latitude)
                    put("p_longitude", longitude)
                    put("p_radius_meters", radiusMeters)
                    put("p_limit", maxResults)
                }
            ).decodeList<NearbyRestaurant>()

            Log.d(TAG, "✅ Found ${result.size} nearby restaurants")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finding nearby restaurants", e)
            Result.failure(e)
        }
    }

    /**
     * Find restaurants near user, filtered by cuisine type
     */
    suspend fun findNearbyRestaurantsByCuisine(
        latitude: Double,
        longitude: Double,
        cuisineType: String,
        radiusMeters: Int = 1000,
        maxResults: Int = 20
    ): Result<List<NearbyRestaurant>> {
        return try {
            Log.d(TAG, "🍕 Finding $cuisineType restaurants near ($latitude, $longitude)")

            // First get nearby, then filter by cuisine
            val nearbyResult = findNearbyRestaurants(latitude, longitude, radiusMeters, maxResults * 2)

            if (nearbyResult.isSuccess) {
                val filtered = nearbyResult.getOrNull()
                    ?.filter { it.chainCuisineType?.equals(cuisineType, ignoreCase = true) == true }
                    ?.take(maxResults)
                    ?: emptyList()

                Log.d(TAG, "✅ Found ${filtered.size} $cuisineType restaurants")
                Result.success(filtered)
            } else {
                nearbyResult
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finding restaurants by cuisine", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESTAURANT LOOKUPS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get a single restaurant by ID with chain info
     */
    suspend fun getRestaurant(restaurantId: String): Result<Restaurant?> {
        return try {
            Log.d(TAG, "🏪 Fetching restaurant: $restaurantId")

            val result = SupabaseClient.client.from("restaurants")
                .select(columns = Columns.raw("*, chain:restaurant_chains(*)")) {
                    filter {
                        eq("id", restaurantId)
                    }
                }
                .decodeSingleOrNull<Restaurant>()

            if (result != null) {
                Log.d(TAG, "✅ Restaurant loaded: ${result.displayName}")
            } else {
                Log.d(TAG, "⚠️ Restaurant not found: $restaurantId")
            }
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching restaurant", e)
            Result.failure(e)
        }
    }

    /**
     * Get restaurant chain by ID
     */
    suspend fun getChain(chainId: String): Result<RestaurantChain?> {
        return try {
            Log.d(TAG, "🔗 Fetching chain: $chainId")

            val result = SupabaseClient.client.from("restaurant_chains")
                .select {
                    filter {
                        eq("id", chainId)
                    }
                }
                .decodeSingleOrNull<RestaurantChain>()

            if (result != null) {
                Log.d(TAG, "✅ Chain loaded: ${result.name}")
            }
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching chain", e)
            Result.failure(e)
        }
    }

    /**
     * Search restaurant chains by name
     */
    suspend fun searchChains(
        query: String,
        limit: Int = 20
    ): Result<List<RestaurantChain>> {
        return try {
            Log.d(TAG, "🔍 Searching chains: '$query'")

            val result = SupabaseClient.client.from("restaurant_chains")
                .select {
                    filter {
                        ilike("name", "%$query%")
                    }
                    order("total_meals", Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<RestaurantChain>()

            Log.d(TAG, "✅ Found ${result.size} chains matching '$query'")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error searching chains", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MEAL QUERIES (Core functionality for Phase 1)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get a single meal by ID
     */
    suspend fun getMeal(mealId: String): Result<RestaurantMeal?> {
        return try {
            Log.d(TAG, "🍔 Fetching meal: $mealId")

            val result = SupabaseClient.client.from("restaurant_meals")
                .select(columns = Columns.raw("*, chain:restaurant_chains(id, name, logo_url)")) {
                    filter {
                        eq("id", mealId)
                    }
                }
                .decodeSingleOrNull<RestaurantMeal>()

            if (result != null) {
                Log.d(TAG, "✅ Meal loaded: ${result.name}")
            } else {
                Log.d(TAG, "⚠️ Meal not found: $mealId")
            }
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching meal", e)
            Result.failure(e)
        }
    }

    /**
     * Search meals globally (across all chains)
     */
    suspend fun searchMeals(
        query: String,
        limit: Int = 30
    ): Result<List<RestaurantMeal>> {
        return try {
            Log.d(TAG, "🔍 Searching meals: '$query'")

            val result = SupabaseClient.client.from("restaurant_meals")
                .select(columns = Columns.raw("*, chain:restaurant_chains(id, name, logo_url)")) {
                    filter {
                        ilike("name", "%$query%")
                    }
                    order("is_popular", Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<RestaurantMeal>()

            Log.d(TAG, "✅ Found ${result.size} meals matching '$query'")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error searching meals", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POPULAR MEALS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get popular meals for a specific chain
     */
    suspend fun getPopularMeals(
        chainId: String,
        limit: Int = 10
    ): Result<List<RestaurantMeal>> {
        return try {
            Log.d(TAG, "🔥 Fetching popular meals for chain: $chainId")

            val result = SupabaseClient.client.from("restaurant_meals")
                .select(columns = Columns.raw("*, chain:restaurant_chains(id, name, logo_url)")) {
                    filter {
                        eq("chain_id", chainId)
                        eq("is_popular", true)
                    }
                    order("popularity_score", Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<RestaurantMeal>()

            Log.d(TAG, "✅ Found ${result.size} popular meals")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching popular meals", e)
            Result.failure(e)
        }
    }

    /**
     * Get meals by category for a chain
     */
    suspend fun getMealsByCategory(
        chainId: String,
        category: String,
        limit: Int = 30
    ): Result<List<RestaurantMeal>> {
        return try {
            Log.d(TAG, "📂 Fetching $category meals for chain: $chainId")

            val result = SupabaseClient.client.from("restaurant_meals")
                .select(columns = Columns.raw("*, chain:restaurant_chains(id, name, logo_url)")) {
                    filter {
                        eq("chain_id", chainId)
                        ilike("category", "%$category%")
                    }
                    order("is_popular", Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<RestaurantMeal>()

            Log.d(TAG, "✅ Found ${result.size} meals in category '$category'")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching meals by category", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER RESTAURANT HISTORY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Record a user's visit to a restaurant (when logging a meal)
     */
    suspend fun recordUserRestaurantVisit(
        userId: String,
        restaurantId: String,
        mealId: String? = null
    ): Result<Unit> {
        return try {
            Log.d(TAG, "📝 Recording restaurant visit for user")

            SupabaseClient.client.postgrest.rpc(
                function = "record_restaurant_visit",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_restaurant_id", restaurantId)
                    mealId?.let { put("p_meal_id", it) }
                }
            )

            Log.d(TAG, "✅ Visit recorded")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Could not record visit (non-critical)", e)
            // Non-critical failure - don't block main flow
            Result.success(Unit)
        }
    }

    /**
     * Get user's frequently visited restaurants
     */
    suspend fun getUserFrequentRestaurants(
        userId: String,
        limit: Int = 10
    ): Result<List<UserRestaurantHistory>> {
        return try {
            Log.d(TAG, "🏪 Fetching frequent restaurants for user")

            val result = SupabaseClient.client.from("user_restaurant_history")
                .select(columns = Columns.raw("*, restaurant:restaurants(*, chain:restaurant_chains(*))")) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("times_visited", Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<UserRestaurantHistory>()

            Log.d(TAG, "✅ Found ${result.size} frequent restaurants")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching frequent restaurants", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's "usual" meals (ordered 3+ times at same restaurant)
     */
    suspend fun getUserUsualMeals(
        userId: String,
        limit: Int = 10
    ): Result<List<UserRestaurantHistory>> {
        return try {
            Log.d(TAG, "⭐ Fetching user's usual meals")

            val result = SupabaseClient.client.from("user_restaurant_history")
                .select(columns = Columns.raw("*, restaurant:restaurants(*, chain:restaurant_chains(*)), meal:restaurant_meals(*)")) {
                    filter {
                        eq("user_id", userId)
                        gte("times_visited", 3)
                    }
                    order("times_visited", Order.DESCENDING)
                    limit(count = (limit * 2).toLong()) // Fetch extra to filter
                }
                .decodeList<UserRestaurantHistory>()
                .filter { it.mealId != null } // Filter in Kotlin
                .take(limit)

            Log.d(TAG, "✅ Found ${result.size} usual meals")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching usual meals", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's recent restaurant meals
     */
    suspend fun getUserRecentRestaurantMeals(
        userId: String,
        limit: Int = 20
    ): Result<List<UserRestaurantHistory>> {
        return try {
            Log.d(TAG, "🕒 Fetching recent restaurant meals for user")

            val result = SupabaseClient.client.from("user_restaurant_history")
                .select(columns = Columns.raw("*, restaurant:restaurants(*, chain:restaurant_chains(*)), meal:restaurant_meals(*)")) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("last_visited_at", Order.DESCENDING)
                    limit(count = (limit * 2).toLong()) // Fetch extra to filter
                }
                .decodeList<UserRestaurantHistory>()
                .filter { it.mealId != null } // Filter in Kotlin
                .take(limit)

            Log.d(TAG, "✅ Found ${result.size} recent restaurant meals")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching recent meals", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CROWDSOURCED SUBMISSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Submit a new meal to a restaurant (crowdsourced)
     */
    suspend fun submitMeal(
        userId: String,
        restaurantId: String,
        mealName: String,
        calories: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        photoUrl: String? = null,
        aiAnalysisJson: String? = null,
        notes: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        distanceFromRestaurant: Float? = null
    ): Result<MealSubmission> {
        return try {
            Log.d(TAG, "📤 Submitting meal: $mealName")

            val submission = MealSubmissionInsert(
                userId = userId,
                restaurantId = restaurantId,
                mealName = mealName,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                photoUrl = photoUrl,
                aiAnalysisJson = aiAnalysisJson,
                notes = notes,
                submissionLatitude = latitude,
                submissionLongitude = longitude,
                distanceFromRestaurant = distanceFromRestaurant
            )

            val result = SupabaseClient.client.from("meal_submissions")
                .insert(submission)
                .decodeSingle<MealSubmission>()

            Log.d(TAG, "✅ Meal submitted: ${result.id}")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error submitting meal", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's submissions
     */
    suspend fun getUserSubmissions(
        userId: String,
        limit: Int = 50
    ): Result<List<MealSubmission>> {
        return try {
            Log.d(TAG, "📋 Fetching user submissions")

            val result = SupabaseClient.client.from("meal_submissions")
                .select(columns = Columns.raw("*, restaurant:restaurants(*, chain:restaurant_chains(*))")) {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<MealSubmission>()

            Log.d(TAG, "✅ Found ${result.size} submissions")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching submissions", e)
            Result.failure(e)
        }
    }

    /**
     * Vote on a submission (upvote/downvote)
     */
    suspend fun voteOnSubmission(
        userId: String,
        submissionId: String,
        isUpvote: Boolean
    ): Result<Unit> {
        return try {
            Log.d(TAG, "${if (isUpvote) "👍" else "👎"} Voting on submission: $submissionId")

            SupabaseClient.client.postgrest.rpc(
                function = "vote_on_submission",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_submission_id", submissionId)
                    put("p_is_upvote", isUpvote)
                }
            )

            Log.d(TAG, "✅ Vote recorded")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error voting", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's contribution stats
     */
    suspend fun getUserContributionStats(userId: String): Result<UserFoodContributionStats?> {
        return try {
            Log.d(TAG, "📊 Fetching contribution stats for user")

            val result = SupabaseClient.client.from("user_food_contribution_stats")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserFoodContributionStats>()

            if (result != null) {
                Log.d(TAG, "✅ Stats loaded: ${result.totalSubmissions} submissions, level ${result.contributionLevel}")
            }
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching contribution stats", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUICK LOG FROM RESTAURANT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Create nutrition log entry from a restaurant meal
     * Returns the meal items ready to be saved via NutritionRepository
     */
    fun convertRestaurantMealToItems(
        meal: RestaurantMeal,
        quantity: Float = 1f
    ): List<AIAnalyzedItem> {
        return listOf(
            AIAnalyzedItem(
                name = meal.name,
                quantity = meal.servingDescription ?: "${meal.servingSize?.toInt() ?: 1}${meal.servingUnit}",
                quantity_value = (meal.servingSize ?: 1f) * quantity,
                quantity_unit = meal.servingUnit,
                calories = meal.calories * quantity,
                protein = meal.protein * quantity,
                carbs = meal.carbs * quantity,
                fat = meal.fat * quantity,
                confidence = if (meal.isVerified) 1.0f else 0.8f,
                fiber = meal.fiber * quantity,
                sugar = meal.sugar * quantity,
                saturated_fat = meal.saturatedFat * quantity,
                sodium = meal.sodium * quantity,
                potassium = meal.potassium * quantity,
                calcium = meal.calcium * quantity,
                iron = meal.iron * quantity,
                vitamin_c = meal.vitaminC * quantity,
                vitamin_d = meal.vitaminD * quantity
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate distance between two points in meters (Haversine formula)
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toFloat()
    }

    /**
     * Determine region from country code
     */
    fun getRegionFromCountry(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "US", "CA", "MX" -> "US"
            "GB", "DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH" -> "EU"
            "JP", "KR", "CN", "TW", "TH", "VN", "SG", "MY", "ID", "PH" -> "ASIA"
            else -> "GLOBAL"
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER DATA CLASSES FOR DECODING
// ═══════════════════════════════════════════════════════════════════════════════

@kotlinx.serialization.Serializable
private data class CategoryWrapper(val category: String)

@kotlinx.serialization.Serializable
private data class VoteWrapper(
    val upvotes: Int = 0,
    val downvotes: Int = 0
)

@kotlinx.serialization.Serializable
private data class MealSubmissionInsert(
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("restaurant_id") val restaurantId: String,
    @kotlinx.serialization.SerialName("meal_name") val mealName: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    @kotlinx.serialization.SerialName("photo_url") val photoUrl: String? = null,
    @kotlinx.serialization.SerialName("ai_analysis_json") val aiAnalysisJson: String? = null,
    val notes: String? = null,
    @kotlinx.serialization.SerialName("submission_latitude") val submissionLatitude: Double? = null,
    @kotlinx.serialization.SerialName("submission_longitude") val submissionLongitude: Double? = null,
    @kotlinx.serialization.SerialName("distance_from_restaurant") val distanceFromRestaurant: Float? = null
)