// data/repository/OpenFoodFactsRepository.kt
package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Open Food Facts Repository
 *
 * API client for Open Food Facts - the world's largest open food database
 * - 3M+ products worldwide (especially strong in EU)
 * - 100% free, no API key required
 * - Community-driven, open data (ODbL license)
 * - Perfect for barcode scanning of packaged products
 *
 * API Docs: https://openfoodfacts.github.io/openfoodfacts-server/api/
 */
object OpenFoodFactsRepository {

    private const val TAG = "OpenFoodFactsRepo"
    private const val BASE_URL = "https://world.openfoodfacts.org"

    // User-Agent is required by Open Food Facts
    private const val USER_AGENT = "Naya Fitness App - Android - Version 1.0"

    // In-memory cache
    private val barcodeCache = ConcurrentHashMap<String, CachedOFFProduct>()
    private val searchCache = ConcurrentHashMap<String, CachedOFFSearch>()

    // Cache duration
    private const val BARCODE_CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    private const val SEARCH_CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

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
        install(HttpTimeout) {
            requestTimeoutMillis = 20000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 20000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // BARCODE LOOKUP (PRIMARY USE CASE)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Look up a product by barcode (EAN-13, UPC-A, etc.)
     * This is the main function for barcode scanning
     *
     * @param barcode The product barcode (EAN-13, UPC-A, etc.)
     * @return NayaFood if found, null if not in database
     */
    suspend fun getProductByBarcode(
        barcode: String,
        useCache: Boolean = true
    ): Result<NayaFood?> = withContext(Dispatchers.IO) {
        try {
            val cleanBarcode = barcode.trim().replace("-", "").replace(" ", "")

            // Check cache first
            if (useCache) {
                barcodeCache[cleanBarcode]?.let { cached ->
                    if (!cached.isExpired()) {
                        Log.d(TAG, "Cache hit for barcode: $cleanBarcode")
                        return@withContext Result.success(cached.food)
                    }
                }
            }

            Log.d(TAG, "Fetching product for barcode: $cleanBarcode")

            val response = client.get("$BASE_URL/api/v2/product/$cleanBarcode") {
                parameter("fields", PRODUCT_FIELDS)
            }

            if (response.status.isSuccess()) {
                val offResponse = response.body<OFFProductResponse>()

                if (offResponse.status == 1 && offResponse.product != null) {
                    val nayaFood = offResponse.product.toNayaFood(cleanBarcode)

                    // Cache the result
                    barcodeCache[cleanBarcode] = CachedOFFProduct(nayaFood)

                    Log.d(TAG, "Found product: ${nayaFood.name}")
                    Result.success(nayaFood)
                } else {
                    Log.d(TAG, "Product not found in Open Food Facts")
                    // Cache the "not found" result to avoid repeated lookups
                    barcodeCache[cleanBarcode] = CachedOFFProduct(null)
                    Result.success(null)
                }
            } else {
                val error = response.bodyAsText()
                Log.e(TAG, "API error: ${response.status} - $error")
                Result.failure(OFFApiException(response.status.value, error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Barcode lookup error", e)
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // PRODUCT SEARCH
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search for products by name/brand
     */
    suspend fun searchProducts(
        query: String,
        page: Int = 1,
        pageSize: Int = 24,
        useCache: Boolean = true
    ): Result<OFFSearchResult> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = "${query.lowercase()}_${page}_$pageSize"

            // Check cache
            if (useCache) {
                searchCache[cacheKey]?.let { cached ->
                    if (!cached.isExpired()) {
                        Log.d(TAG, "Search cache hit for: $query")
                        return@withContext Result.success(cached.result)
                    }
                }
            }

            Log.d(TAG, "Searching Open Food Facts for: $query")

            val response = client.get("$BASE_URL/cgi/search.pl") {
                parameter("search_terms", query)
                parameter("search_simple", 1)
                parameter("action", "process")
                parameter("json", 1)
                parameter("page", page)
                parameter("page_size", pageSize.coerceIn(1, 100))
                parameter("fields", PRODUCT_FIELDS)
            }

            if (response.status.isSuccess()) {
                val searchResponse = response.body<OFFSearchResponse>()

                val result = OFFSearchResult(
                    totalCount = searchResponse.count ?: 0,
                    page = searchResponse.page ?: 1,
                    pageSize = searchResponse.pageSize ?: pageSize,
                    products = searchResponse.products?.mapNotNull { product ->
                        try {
                            product.toNayaFood(product.code ?: "")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to convert product: ${product.productName}", e)
                            null
                        }
                    } ?: emptyList()
                )

                // Cache the result
                searchCache[cacheKey] = CachedOFFSearch(result)

                Log.d(TAG, "Found ${result.totalCount} products for: $query")
                Result.success(result)
            } else {
                val error = response.bodyAsText()
                Log.e(TAG, "Search failed: ${response.status} - $error")
                Result.failure(OFFApiException(response.status.value, error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            Result.failure(e)
        }
    }

    /**
     * Search and return normalized Naya foods
     */
    suspend fun searchProductsNormalized(
        query: String,
        limit: Int = 20
    ): Result<List<NayaFood>> {
        return searchProducts(query, pageSize = limit).map { it.products }
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // COMBINED BARCODE SEARCH (OFF + USDA FALLBACK)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Search for a barcode in Open Food Facts, then fall back to USDA if not found
     * This gives the best coverage for packaged products
     */
    suspend fun searchBarcodeWithFallback(barcode: String): Result<NayaFood?> {
        // Try Open Food Facts first (better for EU products)
        val offResult = getProductByBarcode(barcode)

        if (offResult.isSuccess && offResult.getOrNull() != null) {
            return offResult
        }

        // Fall back to USDA (better for US products)
        Log.d(TAG, "Barcode not in OFF, trying USDA: $barcode")
        return USDARepository.searchByBarcode(barcode)
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════

    fun clearCache() {
        barcodeCache.clear()
        searchCache.clear()
        Log.d(TAG, "Cache cleared")
    }

    fun clearExpiredCache() {
        barcodeCache.entries.removeIf { it.value.isExpired() }
        searchCache.entries.removeIf { it.value.isExpired() }
        Log.d(TAG, "Expired cache entries cleared")
    }


    // ═══════════════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════

    // Fields to request from API (reduces response size)
    private const val PRODUCT_FIELDS = "code,product_name,brands,quantity,serving_size," +
            "serving_quantity,nutriments,categories_tags,ingredients_text"
}


// ═══════════════════════════════════════════════════════════════════════════════
// API RESPONSE MODELS
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class OFFProductResponse(
    val status: Int? = null,
    @SerialName("status_verbose")
    val statusVerbose: String? = null,
    val product: OFFProduct? = null
)

@Serializable
data class OFFSearchResponse(
    val count: Int? = null,
    val page: Int? = null,
    @SerialName("page_size")
    val pageSize: Int? = null,
    @SerialName("page_count")
    val pageCount: Int? = null,
    val products: List<OFFProduct>? = null
)

@Serializable
data class OFFProduct(
    val code: String? = null,

    @SerialName("product_name")
    val productName: String? = null,

    val brands: String? = null,

    val quantity: String? = null,

    @SerialName("serving_size")
    val servingSize: String? = null,

    @SerialName("serving_quantity")
    val servingQuantity: Float? = null,

    val nutriments: OFFNutriments? = null,

    @SerialName("categories_tags")
    val categoriesTags: List<String>? = null,

    @SerialName("ingredients_text")
    val ingredientsText: String? = null
) {
    /**
     * Convert Open Food Facts product to Naya format
     */
    fun toNayaFood(barcode: String): NayaFood {
        val n = nutriments ?: OFFNutriments()

        // Determine serving size
        val servingSizeGrams = servingQuantity
            ?: parseServingSize(servingSize)
            ?: 100f

        return NayaFood(
            id = "off_$barcode",
            name = productName?.trim() ?: "Unknown Product",
            brand = brands?.split(",")?.firstOrNull()?.trim(),
            barcode = barcode,
            category = categoriesTags?.firstOrNull()?.removePrefix("en:")?.replace("-", " "),

            // Macros (per 100g)
            calories = n.energyKcal100g ?: n.energyKcalServing ?: 0f,
            protein = n.proteins100g ?: n.proteinsServing ?: 0f,
            carbs = n.carbohydrates100g ?: n.carbohydratesServing ?: 0f,
            fat = n.fat100g ?: n.fatServing ?: 0f,
            fiber = n.fiber100g ?: n.fiberServing ?: 0f,
            sugar = n.sugars100g ?: n.sugarsServing ?: 0f,

            // Additional macros
            saturatedFat = n.saturatedFat100g ?: n.saturatedFatServing ?: 0f,
            transFat = n.transFat100g ?: n.transFatServing ?: 0f,
            sodium = (n.sodium100g ?: n.sodiumServing ?: 0f) * 1000f, // Convert g to mg
            cholesterol = (n.cholesterol100g ?: n.cholesterolServing ?: 0f) * 1000f, // Convert g to mg

            // Serving info
            servingSize = servingSizeGrams,
            servingUnit = "g",
            servingDescription = servingSize ?: "${servingSizeGrams.toInt()}g",
            portions = buildPortions(servingSizeGrams, servingSize),

            // Source info
            source = FoodSource.OPEN_FOOD_FACTS,
            ingredients = ingredientsText,
            verified = true,
            dataType = "Open Food Facts"
        )
    }

    private fun parseServingSize(servingSize: String?): Float? {
        if (servingSize == null) return null
        val regex = """(\d+(?:\.\d+)?)\s*g""".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(servingSize)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun buildPortions(servingSizeGrams: Float, servingDescription: String?): List<FoodPortion> {
        val portions = mutableListOf<FoodPortion>()

        // Standard 100g portion
        portions.add(FoodPortion(
            description = "100g",
            gramWeight = 100f
        ))

        // Product serving size (if different from 100g)
        if (servingSizeGrams != 100f && servingSizeGrams > 0) {
            portions.add(FoodPortion(
                description = servingDescription ?: "${servingSizeGrams.toInt()}g",
                gramWeight = servingSizeGrams
            ))
        }

        return portions
    }
}

@Serializable
data class OFFNutriments(
    // Energy
    @SerialName("energy-kcal_100g")
    val energyKcal100g: Float? = null,
    @SerialName("energy-kcal_serving")
    val energyKcalServing: Float? = null,

    // Macros per 100g
    @SerialName("proteins_100g")
    val proteins100g: Float? = null,
    @SerialName("carbohydrates_100g")
    val carbohydrates100g: Float? = null,
    @SerialName("fat_100g")
    val fat100g: Float? = null,
    @SerialName("fiber_100g")
    val fiber100g: Float? = null,
    @SerialName("sugars_100g")
    val sugars100g: Float? = null,
    @SerialName("saturated-fat_100g")
    val saturatedFat100g: Float? = null,
    @SerialName("trans-fat_100g")
    val transFat100g: Float? = null,
    @SerialName("sodium_100g")
    val sodium100g: Float? = null,
    @SerialName("cholesterol_100g")
    val cholesterol100g: Float? = null,

    // Macros per serving
    @SerialName("proteins_serving")
    val proteinsServing: Float? = null,
    @SerialName("carbohydrates_serving")
    val carbohydratesServing: Float? = null,
    @SerialName("fat_serving")
    val fatServing: Float? = null,
    @SerialName("fiber_serving")
    val fiberServing: Float? = null,
    @SerialName("sugars_serving")
    val sugarsServing: Float? = null,
    @SerialName("saturated-fat_serving")
    val saturatedFatServing: Float? = null,
    @SerialName("trans-fat_serving")
    val transFatServing: Float? = null,
    @SerialName("sodium_serving")
    val sodiumServing: Float? = null,
    @SerialName("cholesterol_serving")
    val cholesterolServing: Float? = null
)


// ═══════════════════════════════════════════════════════════════════════════════
// RESULT MODELS
// ═══════════════════════════════════════════════════════════════════════════════

data class OFFSearchResult(
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val products: List<NayaFood>
) {
    val hasMore: Boolean get() = page * pageSize < totalCount
    val nextPage: Int get() = page + 1
}


// ═══════════════════════════════════════════════════════════════════════════════
// CACHE CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

private data class CachedOFFProduct(
    val food: NayaFood?,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 7 * 24 * 60 * 60 * 1000L
}

private data class CachedOFFSearch(
    val result: OFFSearchResult,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 30 * 60 * 1000L
}


// ═══════════════════════════════════════════════════════════════════════════════
// EXCEPTIONS
// ═══════════════════════════════════════════════════════════════════════════════

class OFFApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message) {

    fun isNotFound(): Boolean = statusCode == 404
    fun isRateLimited(): Boolean = statusCode == 429

    override fun toString(): String = "OFFApiException(status=$statusCode, message=$message)"
}