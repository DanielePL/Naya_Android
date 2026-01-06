// data/repository/USDARepository.kt
package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
// import io.ktor.client.plugins.logging.* // Dependency not available
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import com.example.menotracker.BuildConfig


/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * USDA FOODDATA CENTRAL REPOSITORY
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Complete API client for USDA FoodData Central
 * - 1.9M+ foods (Foundation, SR Legacy, Branded, Survey)
 * - Free API with 1,000 requests/hour limit
 * - Public domain data (CC0)
 *
 * API Docs: https://fdc.nal.usda.gov/api-guide/
 * Get API Key: https://fdc.nal.usda.gov/api-key-signup/
 * ═══════════════════════════════════════════════════════════════════════════════
 */
object USDARepository {

    private const val TAG = "USDARepository"
    private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1"

    // ⚠️ IMPORTANT: Replace with your own API key from https://fdc.nal.usda.gov/api-key-signup/
    // For testing, you can use "DEMO_KEY" but it has very low rate limits
    private var apiKey: String = BuildConfig.USDA_API_KEY.ifEmpty { "DEMO_KEY" }

    // In-memory cache for quick lookups
    private val searchCache = ConcurrentHashMap<String, CachedSearchResult>()
    private val detailsCache = ConcurrentHashMap<Int, CachedFoodDetails>()
    private val barcodeCache = ConcurrentHashMap<String, CachedBarcodeResult>()

    // Cache expiry times
    private const val SEARCH_CACHE_DURATION_MS = 30 * 60 * 1000L      // 30 minutes
    private const val DETAILS_CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    private const val BARCODE_CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days

    // JSON configuration
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // HTTP Client
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        // Logging disabled - dependency not available
        // install(Logging) { ... }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 30000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Set the USDA API key
     * Get your free key at: https://fdc.nal.usda.gov/api-key-signup/
     */
    fun setApiKey(key: String) {
        apiKey = key
        Log.d(TAG, "API key configured")
    }

    /**
     * Check if API key is configured (not demo key)
     */
    fun isApiKeyConfigured(): Boolean {
        return apiKey != "DEMO_KEY" && apiKey.isNotBlank()
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // SEARCH FOODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search for foods by query string
     *
     * @param query Search term (e.g., "chicken breast", "apple")
     * @param dataTypes Filter by data types (Foundation, SR Legacy, Branded, Survey (FNDDS))
     * @param pageSize Number of results per page (max 200)
     * @param pageNumber Page number for pagination
     * @param brandOwner Filter by brand (for Branded foods only)
     * @return USDASearchResponse with matching foods
     */
    suspend fun searchFoods(
        query: String,
        dataTypes: List<USDADataType>? = null,
        pageSize: Int = 25,
        pageNumber: Int = 1,
        brandOwner: String? = null,
        useCache: Boolean = true
    ): Result<USDASearchResponse> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cacheKey = buildSearchCacheKey(query, dataTypes, pageSize, pageNumber, brandOwner)
            if (useCache) {
                searchCache[cacheKey]?.let { cached ->
                    if (!cached.isExpired()) {
                        Log.d(TAG, "Search cache hit for: $query")
                        return@withContext Result.success(cached.result)
                    }
                }
            }

            Log.d(TAG, "Searching USDA for: $query")

            val response = client.post("$BASE_URL/foods/search") {
                parameter("api_key", apiKey)
                setBody(USDASearchRequest(
                    query = query,
                    dataType = dataTypes?.map { it.apiValue },
                    pageSize = pageSize.coerceIn(1, 200),
                    pageNumber = pageNumber,
                    brandOwner = brandOwner
                ))
            }

            if (response.status.isSuccess()) {
                val result = response.body<USDASearchResponse>()
                Log.d(TAG, "Found ${result.totalHits} foods for: $query")

                // Cache the result
                searchCache[cacheKey] = CachedSearchResult(result)

                Result.success(result)
            } else {
                val error = response.bodyAsText()
                Log.e(TAG, "Search failed: ${response.status} - $error")
                Result.failure(USDAApiException(response.status.value, error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            Result.failure(e)
        }
    }

    /**
     * Search foods and return normalized Naya format
     */
    suspend fun searchFoodsNormalized(
        query: String,
        dataTypes: List<USDADataType>? = null,
        pageSize: Int = 25,
        pageNumber: Int = 1
    ): Result<List<NayaFood>> {
        return searchFoods(query, dataTypes, pageSize, pageNumber).map { response ->
            response.foods.map { it.toNayaFood() }
        }
    }

    /**
     * Quick search with default settings (most common use case)
     */
    suspend fun quickSearch(query: String, limit: Int = 20): Result<List<NayaFood>> {
        return searchFoodsNormalized(
            query = query,
            dataTypes = listOf(USDADataType.FOUNDATION, USDADataType.SR_LEGACY, USDADataType.BRANDED),
            pageSize = limit
        )
    }

    /**
     * Search only Foundation foods (raw ingredients with detailed data)
     */
    suspend fun searchFoundationFoods(query: String, limit: Int = 20): Result<List<NayaFood>> {
        return searchFoodsNormalized(
            query = query,
            dataTypes = listOf(USDADataType.FOUNDATION),
            pageSize = limit
        )
    }

    /**
     * Search only Branded foods (commercial products)
     */
    suspend fun searchBrandedFoods(
        query: String,
        limit: Int = 20,
        brandOwner: String? = null
    ): Result<List<NayaFood>> {
        return searchFoods(
            query = query,
            dataTypes = listOf(USDADataType.BRANDED),
            pageSize = limit,
            brandOwner = brandOwner
        ).map { response ->
            response.foods.map { it.toNayaFood() }
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // GET FOOD DETAILS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get full details for a specific food by FDC ID
     * Includes complete nutrient data, portions, and ingredients
     *
     * @param fdcId FoodData Central ID
     * @return Full food details
     */
    suspend fun getFoodDetails(
        fdcId: Int,
        useCache: Boolean = true
    ): Result<USDAFoodDetails> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            if (useCache) {
                detailsCache[fdcId]?.let { cached ->
                    if (!cached.isExpired()) {
                        Log.d(TAG, "Details cache hit for: $fdcId")
                        return@withContext Result.success(cached.details)
                    }
                }
            }

            Log.d(TAG, "Fetching details for FDC ID: $fdcId")

            val response = client.get("$BASE_URL/food/$fdcId") {
                parameter("api_key", apiKey)
            }

            if (response.status.isSuccess()) {
                val details = response.body<USDAFoodDetails>()
                Log.d(TAG, "Got details for: ${details.description}")

                // Cache the result
                detailsCache[fdcId] = CachedFoodDetails(details)

                Result.success(details)
            } else {
                val error = response.bodyAsText()
                Log.e(TAG, "Details fetch failed: ${response.status} - $error")
                Result.failure(USDAApiException(response.status.value, error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Details fetch error", e)
            Result.failure(e)
        }
    }

    /**
     * Get food details in normalized Naya format
     */
    suspend fun getFoodDetailsNormalized(fdcId: Int): Result<NayaFood> {
        return getFoodDetails(fdcId).map { it.toNayaFood() }
    }

    /**
     * Get details for multiple foods at once
     */
    suspend fun getMultipleFoodDetails(
        fdcIds: List<Int>
    ): Result<List<USDAFoodDetails>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching details for ${fdcIds.size} foods")

            val response = client.post("$BASE_URL/foods") {
                parameter("api_key", apiKey)
                setBody(mapOf("fdcIds" to fdcIds))
            }

            if (response.status.isSuccess()) {
                val details = response.body<List<USDAFoodDetails>>()
                Log.d(TAG, "Got details for ${details.size} foods")

                // Cache all results
                details.forEach { food ->
                    detailsCache[food.fdcId] = CachedFoodDetails(food)
                }

                Result.success(details)
            } else {
                val error = response.bodyAsText()
                Log.e(TAG, "Bulk fetch failed: ${response.status} - $error")
                Result.failure(USDAApiException(response.status.value, error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bulk fetch error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // BARCODE LOOKUP
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search for a food by UPC/GTIN barcode
     * Only works for Branded foods
     *
     * @param barcode UPC or GTIN barcode string
     * @return Matching food if found
     */
    suspend fun searchByBarcode(
        barcode: String,
        useCache: Boolean = true
    ): Result<NayaFood?> = withContext(Dispatchers.IO) {
        try {
            val cleanBarcode = barcode.trim().replace("-", "")

            // Check cache first
            if (useCache) {
                barcodeCache[cleanBarcode]?.let { cached ->
                    if (!cached.isExpired()) {
                        Log.d(TAG, "Barcode cache hit for: $cleanBarcode")
                        return@withContext Result.success(cached.food)
                    }
                }
            }

            Log.d(TAG, "Searching USDA for barcode: $cleanBarcode")

            // Search in Branded foods by GTIN/UPC
            val response = client.post("$BASE_URL/foods/search") {
                parameter("api_key", apiKey)
                setBody(USDASearchRequest(
                    query = cleanBarcode,
                    dataType = listOf("Branded"),
                    pageSize = 5
                ))
            }

            if (response.status.isSuccess()) {
                val result = response.body<USDASearchResponse>()

                // Find exact barcode match
                val matchingFood = result.foods.find { food ->
                    food.gtinUpc?.replace("-", "") == cleanBarcode
                }

                val nayaFood = matchingFood?.toNayaFood()

                // Cache the result (even if null)
                barcodeCache[cleanBarcode] = CachedBarcodeResult(nayaFood)

                if (nayaFood != null) {
                    Log.d(TAG, "Found food for barcode: ${nayaFood.name}")
                } else {
                    Log.d(TAG, "No exact barcode match found in USDA")
                }

                Result.success(nayaFood)
            } else {
                val error = response.bodyAsText()
                Log.e(TAG, "Barcode search failed: ${response.status} - $error")
                Result.failure(USDAApiException(response.status.value, error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Barcode search error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // LIST FOODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get a paginated list of foods
     * Useful for browsing or syncing offline database
     */
    suspend fun listFoods(
        dataTypes: List<USDADataType>? = null,
        pageSize: Int = 50,
        pageNumber: Int = 1
    ): Result<USDAFoodsListResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Listing foods - page $pageNumber")

            val response = client.post("$BASE_URL/foods/list") {
                parameter("api_key", apiKey)
                setBody(USDAFoodsListRequest(
                    dataType = dataTypes?.map { it.apiValue },
                    pageSize = pageSize.coerceIn(1, 200),
                    pageNumber = pageNumber
                ))
            }

            if (response.status.isSuccess()) {
                val result = response.body<USDAFoodsListResponse>()
                Log.d(TAG, "Listed ${result.foods.size} foods")
                Result.success(result)
            } else {
                val error = response.bodyAsText()
                Log.e(TAG, "List failed: ${response.status} - $error")
                Result.failure(USDAApiException(response.status.value, error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "List error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // NUTRIENT HELPERS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Get specific nutrient value from a food
     */
    fun getNutrientValue(food: USDAFoodDetails, nutrientId: Int): Double? {
        return food.foodNutrients.find { it.nutrient?.id == nutrientId }?.amount
    }

    /**
     * Get macros as a simple map
     */
    fun getMacros(food: USDAFoodDetails): Map<String, Double> {
        return mapOf(
            "calories" to (getNutrientValue(food, USDANutrientIds.ENERGY_KCAL) ?: 0.0),
            "protein" to (getNutrientValue(food, USDANutrientIds.PROTEIN) ?: 0.0),
            "carbs" to (getNutrientValue(food, USDANutrientIds.CARBOHYDRATES) ?: 0.0),
            "fat" to (getNutrientValue(food, USDANutrientIds.TOTAL_FAT) ?: 0.0),
            "fiber" to (getNutrientValue(food, USDANutrientIds.FIBER) ?: 0.0),
            "sugar" to (getNutrientValue(food, USDANutrientIds.SUGARS) ?: 0.0)
        )
    }

    /**
     * Calculate nutrition for a specific portion size
     */
    fun calculateForPortion(
        food: NayaFood,
        portionGrams: Float
    ): NayaFood {
        // USDA data is per 100g, so we scale accordingly
        val multiplier = portionGrams / 100f

        return food.copy(
            calories = food.calories * multiplier,
            protein = food.protein * multiplier,
            carbs = food.carbs * multiplier,
            fat = food.fat * multiplier,
            fiber = food.fiber * multiplier,
            sugar = food.sugar * multiplier,
            saturatedFat = food.saturatedFat * multiplier,
            transFat = food.transFat * multiplier,
            sodium = food.sodium * multiplier,
            cholesterol = food.cholesterol * multiplier,
            micronutrients = food.micronutrients.mapValues { (_, value) ->
                value.copy(value = value.value * multiplier)
            },
            servingSize = portionGrams
        )
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Clear all caches
     */
    fun clearCache() {
        searchCache.clear()
        detailsCache.clear()
        barcodeCache.clear()
        Log.d(TAG, "All caches cleared")
    }

    /**
     * Clear expired cache entries
     */
    fun clearExpiredCache() {
        val now = System.currentTimeMillis()

        searchCache.entries.removeIf { it.value.isExpired() }
        detailsCache.entries.removeIf { it.value.isExpired() }
        barcodeCache.entries.removeIf { it.value.isExpired() }

        Log.d(TAG, "Expired cache entries cleared")
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            searchCacheSize = searchCache.size,
            detailsCacheSize = detailsCache.size,
            barcodeCacheSize = barcodeCache.size
        )
    }

    private fun buildSearchCacheKey(
        query: String,
        dataTypes: List<USDADataType>?,
        pageSize: Int,
        pageNumber: Int,
        brandOwner: String?
    ): String {
        return "${query.lowercase()}_${dataTypes?.joinToString(",") { it.name }}_${pageSize}_${pageNumber}_$brandOwner"
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // COMMON FOOD SHORTCUTS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Pre-defined searches for common food categories
     */
    object CommonFoods {

        suspend fun searchProteins(query: String = ""): Result<List<NayaFood>> {
            val searchQuery = if (query.isBlank()) "chicken breast beef fish" else query
            return quickSearch(searchQuery)
        }

        suspend fun searchVegetables(query: String = ""): Result<List<NayaFood>> {
            val searchQuery = if (query.isBlank()) "broccoli spinach carrot" else query
            return searchFoundationFoods(searchQuery)
        }

        suspend fun searchFruits(query: String = ""): Result<List<NayaFood>> {
            val searchQuery = if (query.isBlank()) "apple banana orange" else query
            return searchFoundationFoods(searchQuery)
        }

        suspend fun searchGrains(query: String = ""): Result<List<NayaFood>> {
            val searchQuery = if (query.isBlank()) "rice oats bread" else query
            return quickSearch(searchQuery)
        }

        suspend fun searchDairy(query: String = ""): Result<List<NayaFood>> {
            val searchQuery = if (query.isBlank()) "milk cheese yogurt" else query
            return quickSearch(searchQuery)
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// DATA TYPES ENUM
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * USDA FoodData Central data types
 */
enum class USDADataType(val apiValue: String, val displayName: String) {
    FOUNDATION("Foundation", "Foundation Foods"),
    SR_LEGACY("SR Legacy", "Standard Reference"),
    BRANDED("Branded", "Branded Products"),
    SURVEY("Survey (FNDDS)", "Survey Foods");

    companion object {
        fun fromApiValue(value: String): USDADataType? {
            return entries.find { it.apiValue == value }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// CACHE CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

private data class CachedSearchResult(
    val result: USDASearchResponse,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 30 * 60 * 1000L
}

private data class CachedFoodDetails(
    val details: USDAFoodDetails,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 24 * 60 * 60 * 1000L
}

private data class CachedBarcodeResult(
    val food: NayaFood?,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 7 * 24 * 60 * 60 * 1000L
}

data class CacheStats(
    val searchCacheSize: Int,
    val detailsCacheSize: Int,
    val barcodeCacheSize: Int
)


// ═══════════════════════════════════════════════════════════════════════════════
// EXCEPTIONS
// ═══════════════════════════════════════════════════════════════════════════════

class USDAApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message) {

    fun isRateLimited(): Boolean = statusCode == 429
    fun isUnauthorized(): Boolean = statusCode == 401 || statusCode == 403
    fun isNotFound(): Boolean = statusCode == 404

    override fun toString(): String = "USDAApiException(status=$statusCode, message=$message)"
}


// ═══════════════════════════════════════════════════════════════════════════════
// USAGE EXAMPLES
// ═══════════════════════════════════════════════════════════════════════════════

/*
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXAMPLE USAGE
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * // 1. Configure API key (do this once at app startup)
 * USDARepository.setApiKey("your-api-key-here")
 *
 * // 2. Quick search for foods
 * val results = USDARepository.quickSearch("chicken breast")
 * results.onSuccess { foods ->
 *     foods.forEach { food ->
 *         println("${food.name}: ${food.calories} kcal, ${food.protein}g protein")
 *     }
 * }
 *
 * // 3. Search with filters
 * val brandedFoods = USDARepository.searchBrandedFoods(
 *     query = "protein bar",
 *     brandOwner = "Quest"
 * )
 *
 * // 4. Get full details for a specific food
 * val details = USDARepository.getFoodDetailsNormalized(fdcId = 171705)
 * details.onSuccess { food ->
 *     println("Micronutrients: ${food.micronutrients}")
 *     println("Portions: ${food.portions}")
 * }
 *
 * // 5. Barcode lookup
 * val product = USDARepository.searchByBarcode("012345678905")
 * product.onSuccess { food ->
 *     if (food != null) {
 *         println("Found: ${food.name} by ${food.brand}")
 *     }
 * }
 *
 * // 6. Calculate for specific portion
 * val chickenBreast = USDARepository.quickSearch("chicken breast").getOrNull()?.firstOrNull()
 * chickenBreast?.let {
 *     val portion150g = USDARepository.calculateForPortion(it, 150f)
 *     println("150g chicken: ${portion150g.calories} kcal, ${portion150g.protein}g protein")
 * }
 *
 * // 7. Common food shortcuts
 * val proteins = USDARepository.CommonFoods.searchProteins()
 * val vegetables = USDARepository.CommonFoods.searchVegetables()
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */