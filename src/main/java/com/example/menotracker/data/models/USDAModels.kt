// data/models/USDAModels.kt
package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * USDA FOODDATA CENTRAL - DATA MODELS
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Complete data models for USDA FoodData Central API integration
 * API Docs: https://fdc.nal.usda.gov/api-guide/
 *
 * Data Types in USDA:
 * - Foundation: Raw ingredients with detailed nutrient data
 * - SR Legacy: Standard Reference (historical USDA data)
 * - Branded: Commercial products from manufacturers
 * - Survey (FNDDS): Foods as consumed in dietary surveys
 * ═══════════════════════════════════════════════════════════════════════════════
 */


// ═══════════════════════════════════════════════════════════════════════════════
// SEARCH API MODELS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Request body for food search
 */
@Serializable
data class USDASearchRequest(
    val query: String,
    val dataType: List<String>? = null,  // Foundation, SR Legacy, Branded, Survey (FNDDS)
    val pageSize: Int = 25,
    val pageNumber: Int = 1,
    val sortBy: String? = null,          // dataType.keyword, lowercaseDescription.keyword, fdcId, publishedDate
    val sortOrder: String? = null,       // asc, desc
    val brandOwner: String? = null,      // Filter by brand (Branded foods only)
    val requireAllWords: Boolean? = null
)

/**
 * Response from food search endpoint
 */
@Serializable
data class USDASearchResponse(
    val totalHits: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val pageList: List<Int>? = null,
    val foods: List<USDASearchResultFood> = emptyList()
)

/**
 * Individual food item in search results
 */
@Serializable
data class USDASearchResultFood(
    val fdcId: Int,
    val description: String,
    val dataType: String? = null,
    val gtinUpc: String? = null,           // Barcode for branded products
    val publishedDate: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val ingredients: String? = null,
    val marketCountry: String? = null,
    val foodCategory: String? = null,
    val modifiedDate: String? = null,
    val dataSource: String? = null,
    val packageWeight: String? = null,
    val servingSizeUnit: String? = null,
    val servingSize: Double? = null,
    val householdServingFullText: String? = null,
    val tradeChannels: List<String>? = null,
    val allHighlightFields: String? = null,
    val score: Double? = null,
    val microbes: List<String>? = null,
    val foodNutrients: List<USDASearchNutrient>? = null,
    val finalFoodInputFoods: List<USDAInputFood>? = null,
    val foodMeasures: List<USDAFoodMeasure>? = null,
    val foodAttributes: List<USDAFoodAttribute>? = null,
    val foodAttributeTypes: List<USDAFoodAttributeType>? = null,
    val foodVersionIds: List<Int>? = null
)

/**
 * Nutrient data in search results (abbreviated)
 */
@Serializable
data class USDASearchNutrient(
    val nutrientId: Int? = null,
    val nutrientName: String? = null,
    val nutrientNumber: String? = null,
    val unitName: String? = null,
    val derivationCode: String? = null,
    val derivationDescription: String? = null,
    val derivationId: Int? = null,
    val value: Double? = null,
    val foodNutrientSourceId: Int? = null,
    val foodNutrientSourceCode: String? = null,
    val foodNutrientSourceDescription: String? = null,
    val rank: Int? = null,
    val indentLevel: Int? = null,
    val foodNutrientId: Int? = null,
    val percentDailyValue: Int? = null
)


// ═══════════════════════════════════════════════════════════════════════════════
// FOOD DETAILS API MODELS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Full food details response
 */
@Serializable
data class USDAFoodDetails(
    val fdcId: Int,
    val description: String,
    val dataType: String? = null,
    val publicationDate: String? = null,
    val foodClass: String? = null,
    val modifiedDate: String? = null,
    val availableDate: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val dataSource: String? = null,
    val brandedFoodCategory: String? = null,
    val gtinUpc: String? = null,
    val householdServingFullText: String? = null,
    val ingredients: String? = null,
    val marketCountry: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val packageWeight: String? = null,
    val preparationStateCode: String? = null,
    val foodCategory: USDAFoodCategory? = null,
    val foodNutrients: List<USDANutrient> = emptyList(),
    val foodPortions: List<USDAFoodPortion>? = null,
    val foodComponents: List<USDAFoodComponent>? = null,
    val foodAttributes: List<USDAFoodAttribute>? = null,
    val nutrientConversionFactors: List<USDANutrientConversionFactor>? = null,
    val isHistoricalReference: Boolean? = null,
    val ndbNumber: Int? = null,
    val foodUpdateLog: List<USDAFoodUpdateLog>? = null,
    val labelNutrients: USDALabelNutrients? = null,
    val inputFoods: List<USDAInputFood>? = null
)

/**
 * Full nutrient information
 */
@Serializable
data class USDANutrient(
    val id: Int? = null,
    val amount: Double? = null,
    val nutrient: USDANutrientInfo? = null,
    val foodNutrientDerivation: USDANutrientDerivation? = null,
    val nutrientAnalysisDetails: USDANutrientAnalysisDetails? = null,
    val min: Double? = null,
    val max: Double? = null,
    val median: Double? = null,
    val loq: Double? = null,
    val dataPoints: Int? = null,
    val footnote: String? = null,
    val minYearAcquired: Int? = null
)

/**
 * Nutrient metadata
 */
@Serializable
data class USDANutrientInfo(
    val id: Int,
    val number: String? = null,
    val name: String,
    val rank: Int? = null,
    val unitName: String? = null
)

/**
 * How the nutrient value was derived
 */
@Serializable
data class USDANutrientDerivation(
    val id: Int? = null,
    val code: String? = null,
    val description: String? = null,
    val foodNutrientSource: USDAFoodNutrientSource? = null
)

@Serializable
data class USDAFoodNutrientSource(
    val id: Int? = null,
    val code: String? = null,
    val description: String? = null
)

@Serializable
data class USDANutrientAnalysisDetails(
    val subSampleId: Int? = null,
    val amount: Double? = null,
    val nutrientId: Int? = null,
    val labMethodDescription: String? = null,
    val labMethodOriginalDescription: String? = null,
    val labMethodLink: String? = null,
    val labMethodTechnique: String? = null,
    val nutrientAcquisitionDetails: List<USDANutrientAcquisitionDetail>? = null
)

@Serializable
data class USDANutrientAcquisitionDetail(
    val sampleUnitId: Int? = null,
    val purchaseDate: String? = null,
    val storeCity: String? = null,
    val storeState: String? = null
)


// ═══════════════════════════════════════════════════════════════════════════════
// FOOD PORTIONS & MEASURES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Serving size / portion information
 */
@Serializable
data class USDAFoodPortion(
    val id: Int? = null,
    val amount: Double? = null,
    val dataPoints: Int? = null,
    val gramWeight: Double? = null,
    val minYearAcquired: Int? = null,
    val modifier: String? = null,
    val portionDescription: String? = null,
    val sequenceNumber: Int? = null,
    val measureUnit: USDAMeasureUnit? = null
)

@Serializable
data class USDAMeasureUnit(
    val id: Int? = null,
    val abbreviation: String? = null,
    val name: String? = null
)

@Serializable
data class USDAFoodMeasure(
    val disseminationText: String? = null,
    val gramWeight: Double? = null,
    val id: Int? = null,
    val modifier: String? = null,
    val rank: Int? = null,
    val measureUnitAbbreviation: String? = null,
    val measureUnitName: String? = null,
    val measureUnitId: Int? = null
)


// ═══════════════════════════════════════════════════════════════════════════════
// FOOD ATTRIBUTES & CATEGORIES
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class USDAFoodCategory(
    val id: Int? = null,
    val code: String? = null,
    val description: String? = null
)

@Serializable
data class USDAFoodAttribute(
    val id: Int? = null,
    val sequenceNumber: Int? = null,
    val value: String? = null,
    val foodAttributeType: USDAFoodAttributeType? = null
)

@Serializable
data class USDAFoodAttributeType(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class USDAFoodComponent(
    val id: Int? = null,
    val name: String? = null,
    val dataPoints: Int? = null,
    val gramWeight: Double? = null,
    val isRefuse: Boolean? = null,
    val minYearAcquired: Int? = null,
    val percentWeight: Double? = null
)

@Serializable
data class USDANutrientConversionFactor(
    val type: String? = null,
    val value: Double? = null
)

@Serializable
data class USDAFoodUpdateLog(
    val discontinuedDate: String? = null,
    val foodAttributes: List<USDAFoodAttribute>? = null,
    val fdcId: Int? = null,
    val description: String? = null,
    val publicationDate: String? = null
)

@Serializable
data class USDAInputFood(
    val id: Int? = null,
    val foodDescription: String? = null,
    val inputFood: USDAFoodDetails? = null,
    val ingredientCode: Int? = null,
    val ingredientDescription: String? = null,
    val ingredientWeight: Double? = null,
    val portionCode: String? = null,
    val portionDescription: String? = null,
    val sequenceNumber: Int? = null,
    val surveyFlag: Int? = null,
    val unit: String? = null,
    val amount: Double? = null,
    val srCode: Int? = null,
    val srDescription: String? = null,
    val retention: Double? = null
)


// ═══════════════════════════════════════════════════════════════════════════════
// LABEL NUTRIENTS (Branded Foods)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Nutrition facts label data for branded products
 */
@Serializable
data class USDALabelNutrients(
    val fat: USDALabelNutrientValue? = null,
    val saturatedFat: USDALabelNutrientValue? = null,
    val transFat: USDALabelNutrientValue? = null,
    val cholesterol: USDALabelNutrientValue? = null,
    val sodium: USDALabelNutrientValue? = null,
    val carbohydrates: USDALabelNutrientValue? = null,
    val fiber: USDALabelNutrientValue? = null,
    val sugars: USDALabelNutrientValue? = null,
    val protein: USDALabelNutrientValue? = null,
    val calcium: USDALabelNutrientValue? = null,
    val iron: USDALabelNutrientValue? = null,
    val potassium: USDALabelNutrientValue? = null,
    val calories: USDALabelNutrientValue? = null,
    val addedSugar: USDALabelNutrientValue? = null,
    val vitaminD: USDALabelNutrientValue? = null
)

@Serializable
data class USDALabelNutrientValue(
    val value: Double? = null
)


// ═══════════════════════════════════════════════════════════════════════════════
// FOODS LIST API MODELS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Request for listing foods (paginated)
 */
@Serializable
data class USDAFoodsListRequest(
    val dataType: List<String>? = null,
    val pageSize: Int = 50,
    val pageNumber: Int = 1,
    val sortBy: String? = null,
    val sortOrder: String? = null
)

/**
 * Response from foods list endpoint
 */
@Serializable
data class USDAFoodsListResponse(
    val totalHits: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val foods: List<USDAAbridgedFood> = emptyList()
)

/**
 * Abridged food data for list views
 */
@Serializable
data class USDAAbridgedFood(
    val fdcId: Int,
    val description: String,
    val dataType: String? = null,
    val gtinUpc: String? = null,
    val publishedDate: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val foodCategory: String? = null
)


// ═══════════════════════════════════════════════════════════════════════════════
// PROMETHEUS NORMALIZED MODELS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Normalized food item for Naya UI
 * Converts USDA data to our internal format
 */
data class NayaFood(
    val id: String,                          // "usda_{fdcId}" for USDA foods
    val source: FoodSource,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val barcode: String? = null,

    // Core macros (per 100g unless servingSize specified)
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,

    // Extended macros
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val saturatedFat: Float = 0f,
    val transFat: Float = 0f,
    val cholesterol: Float = 0f,           // mg
    val sodium: Float = 0f,                // mg

    // Micronutrients (optional, in standard units)
    val micronutrients: Map<String, MicronutrientValue> = emptyMap(),

    // Serving info
    val servingSize: Float? = null,        // grams
    val servingUnit: String = "g",
    val servingDescription: String? = null, // "1 cup", "1 medium apple"
    val portions: List<FoodPortion> = emptyList(),

    // Metadata
    val ingredients: String? = null,
    val verified: Boolean = false,
    val dataType: String? = null,          // Foundation, SR Legacy, Branded, etc.
    val lastUpdated: String? = null,

    // Open Food Facts specific
    val nutriscoreGrade: String? = null    // Nutri-Score: A, B, C, D, E
)

/**
 * Food data source identifier
 */
enum class FoodSource {
    USDA_FOUNDATION,      // Raw ingredients, detailed data
    USDA_SR_LEGACY,       // Standard Reference (historical)
    USDA_BRANDED,         // Commercial products
    USDA_SURVEY,          // FNDDS dietary survey
    OPEN_FOOD_FACTS,      // Barcode scanned products
    RESTAURANT,           // Our restaurant database
    USER_CREATED,         // User-added foods
    AI_ANALYZED           // AI-generated from photo
}

/**
 * Micronutrient with value and unit
 */
data class MicronutrientValue(
    val value: Float,
    val unit: String,              // mg, mcg, IU, etc.
    val percentDV: Float? = null   // Percent daily value
)

/**
 * Available serving portions
 */
data class FoodPortion(
    val description: String,       // "1 cup", "1 slice", "1 medium"
    val gramWeight: Float,         // Weight in grams
    val modifier: String? = null   // Additional info
)


// ═══════════════════════════════════════════════════════════════════════════════
// NUTRIENT ID MAPPINGS (USDA Standard)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Standard USDA Nutrient IDs
 * Reference: https://fdc.nal.usda.gov/portal-data/external/dataDictionary
 */
object USDANutrientIds {
    // Core Macronutrients
    const val ENERGY_KCAL = 1008           // Energy (kcal)
    const val ENERGY_KJ = 1062             // Energy (kJ)
    const val PROTEIN = 1003               // Protein
    const val TOTAL_FAT = 1004             // Total lipid (fat)
    const val CARBOHYDRATES = 1005         // Carbohydrate, by difference
    const val CARBS_BY_SUMMATION = 1050    // Carbohydrate, by summation

    // Fiber & Sugars
    const val FIBER = 1079                 // Fiber, total dietary
    const val SUGARS = 1063                // Sugars, total including NLEA
    const val SUGARS_ADDED = 1235          // Sugars, added
    const val STARCH = 1009                // Starch

    // Fats
    const val SATURATED_FAT = 1258         // Fatty acids, total saturated
    const val MONOUNSATURATED_FAT = 1292   // Fatty acids, total monounsaturated
    const val POLYUNSATURATED_FAT = 1293   // Fatty acids, total polyunsaturated
    const val TRANS_FAT = 1257             // Fatty acids, total trans
    const val CHOLESTEROL = 1253           // Cholesterol

    // Minerals
    const val SODIUM = 1093                // Sodium, Na
    const val POTASSIUM = 1092             // Potassium, K
    const val CALCIUM = 1087               // Calcium, Ca
    const val IRON = 1089                  // Iron, Fe
    const val MAGNESIUM = 1090             // Magnesium, Mg
    const val PHOSPHORUS = 1091            // Phosphorus, P
    const val ZINC = 1095                  // Zinc, Zn
    const val COPPER = 1098                // Copper, Cu
    const val MANGANESE = 1101             // Manganese, Mn
    const val SELENIUM = 1103              // Selenium, Se

    // Vitamins
    const val VITAMIN_A_RAE = 1106         // Vitamin A, RAE
    const val VITAMIN_A_IU = 1104          // Vitamin A, IU
    const val VITAMIN_C = 1162             // Vitamin C, total ascorbic acid
    const val VITAMIN_D = 1114             // Vitamin D (D2 + D3)
    const val VITAMIN_D_IU = 1110          // Vitamin D (D2 + D3), IU
    const val VITAMIN_E = 1109             // Vitamin E (alpha-tocopherol)
    const val VITAMIN_K = 1185             // Vitamin K (phylloquinone)
    const val THIAMIN = 1165               // Thiamin (B1)
    const val RIBOFLAVIN = 1166            // Riboflavin (B2)
    const val NIACIN = 1167                // Niacin (B3)
    const val VITAMIN_B6 = 1175            // Vitamin B-6
    const val FOLATE = 1177                // Folate, total
    const val FOLIC_ACID = 1186            // Folic acid
    const val VITAMIN_B12 = 1178           // Vitamin B-12
    const val CHOLINE = 1180               // Choline, total
    const val BIOTIN = 1176                // Biotin
    const val PANTOTHENIC_ACID = 1170      // Pantothenic acid

    // Amino Acids
    const val TRYPTOPHAN = 1210
    const val THREONINE = 1211
    const val ISOLEUCINE = 1212
    const val LEUCINE = 1213
    const val LYSINE = 1214
    const val METHIONINE = 1215
    const val CYSTINE = 1216
    const val PHENYLALANINE = 1217
    const val TYROSINE = 1218
    const val VALINE = 1219
    const val ARGININE = 1220
    const val HISTIDINE = 1221

    // Other
    const val WATER = 1051                 // Water
    const val ASH = 1007                   // Ash
    const val ALCOHOL = 1018               // Alcohol, ethyl
    const val CAFFEINE = 1057              // Caffeine

    /**
     * Get nutrient value from list by ID
     */
    fun getNutrientValue(nutrients: List<USDANutrient>, nutrientId: Int): Double? {
        return nutrients.find { it.nutrient?.id == nutrientId }?.amount
    }

    /**
     * Get nutrient value from search results by ID
     */
    fun getSearchNutrientValue(nutrients: List<USDASearchNutrient>?, nutrientId: Int): Double? {
        return nutrients?.find { it.nutrientId == nutrientId }?.value
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Convert USDA search result to Naya format
 */
fun USDASearchResultFood.toNayaFood(): NayaFood {
    val nutrients = this.foodNutrients ?: emptyList()

    return NayaFood(
        id = "usda_$fdcId",
        source = when (dataType) {
            "Foundation" -> FoodSource.USDA_FOUNDATION
            "SR Legacy" -> FoodSource.USDA_SR_LEGACY
            "Branded" -> FoodSource.USDA_BRANDED
            "Survey (FNDDS)" -> FoodSource.USDA_SURVEY
            else -> FoodSource.USDA_SR_LEGACY
        },
        name = description,
        brand = brandOwner ?: brandName,
        category = foodCategory,
        barcode = gtinUpc,

        calories = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.ENERGY_KCAL)?.toFloat() ?: 0f,
        protein = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.PROTEIN)?.toFloat() ?: 0f,
        carbs = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.CARBOHYDRATES)?.toFloat() ?: 0f,
        fat = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.TOTAL_FAT)?.toFloat() ?: 0f,
        fiber = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.FIBER)?.toFloat() ?: 0f,
        sugar = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.SUGARS)?.toFloat() ?: 0f,
        saturatedFat = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.SATURATED_FAT)?.toFloat() ?: 0f,
        sodium = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.SODIUM)?.toFloat() ?: 0f,
        cholesterol = USDANutrientIds.getSearchNutrientValue(nutrients, USDANutrientIds.CHOLESTEROL)?.toFloat() ?: 0f,

        servingSize = servingSize?.toFloat(),
        servingUnit = servingSizeUnit ?: "g",
        servingDescription = householdServingFullText,
        portions = foodMeasures?.mapNotNull { measure ->
            if (measure.gramWeight != null && measure.disseminationText != null) {
                FoodPortion(
                    description = measure.disseminationText,
                    gramWeight = measure.gramWeight.toFloat(),
                    modifier = measure.modifier
                )
            } else null
        } ?: emptyList(),

        ingredients = ingredients,
        verified = true,
        dataType = dataType,
        lastUpdated = publishedDate
    )
}

/**
 * Convert USDA full details to Naya format
 */
fun USDAFoodDetails.toNayaFood(): NayaFood {
    val nutrients = this.foodNutrients

    // Build micronutrients map
    val micronutrients = mutableMapOf<String, MicronutrientValue>()

    // Helper to add micronutrient if present
    fun addMicro(name: String, nutrientId: Int, defaultUnit: String = "mg") {
        USDANutrientIds.getNutrientValue(nutrients, nutrientId)?.let { value ->
            val nutrientInfo = nutrients.find { it.nutrient?.id == nutrientId }?.nutrient
            micronutrients[name] = MicronutrientValue(
                value = value.toFloat(),
                unit = nutrientInfo?.unitName ?: defaultUnit
            )
        }
    }

    // Add all micronutrients
    addMicro("Vitamin A", USDANutrientIds.VITAMIN_A_RAE, "mcg")
    addMicro("Vitamin C", USDANutrientIds.VITAMIN_C, "mg")
    addMicro("Vitamin D", USDANutrientIds.VITAMIN_D, "mcg")
    addMicro("Vitamin E", USDANutrientIds.VITAMIN_E, "mg")
    addMicro("Vitamin K", USDANutrientIds.VITAMIN_K, "mcg")
    addMicro("Thiamin (B1)", USDANutrientIds.THIAMIN, "mg")
    addMicro("Riboflavin (B2)", USDANutrientIds.RIBOFLAVIN, "mg")
    addMicro("Niacin (B3)", USDANutrientIds.NIACIN, "mg")
    addMicro("Vitamin B6", USDANutrientIds.VITAMIN_B6, "mg")
    addMicro("Folate", USDANutrientIds.FOLATE, "mcg")
    addMicro("Vitamin B12", USDANutrientIds.VITAMIN_B12, "mcg")
    addMicro("Calcium", USDANutrientIds.CALCIUM, "mg")
    addMicro("Iron", USDANutrientIds.IRON, "mg")
    addMicro("Magnesium", USDANutrientIds.MAGNESIUM, "mg")
    addMicro("Phosphorus", USDANutrientIds.PHOSPHORUS, "mg")
    addMicro("Potassium", USDANutrientIds.POTASSIUM, "mg")
    addMicro("Zinc", USDANutrientIds.ZINC, "mg")
    addMicro("Copper", USDANutrientIds.COPPER, "mg")
    addMicro("Manganese", USDANutrientIds.MANGANESE, "mg")
    addMicro("Selenium", USDANutrientIds.SELENIUM, "mcg")
    addMicro("Choline", USDANutrientIds.CHOLINE, "mg")

    return NayaFood(
        id = "usda_$fdcId",
        source = when (dataType) {
            "Foundation" -> FoodSource.USDA_FOUNDATION
            "SR Legacy" -> FoodSource.USDA_SR_LEGACY
            "Branded" -> FoodSource.USDA_BRANDED
            "Survey (FNDDS)" -> FoodSource.USDA_SURVEY
            else -> FoodSource.USDA_SR_LEGACY
        },
        name = description,
        brand = brandOwner ?: brandName,
        category = brandedFoodCategory ?: foodCategory?.description,
        barcode = gtinUpc,

        calories = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.ENERGY_KCAL)?.toFloat() ?: 0f,
        protein = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.PROTEIN)?.toFloat() ?: 0f,
        carbs = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.CARBOHYDRATES)?.toFloat() ?: 0f,
        fat = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.TOTAL_FAT)?.toFloat() ?: 0f,
        fiber = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.FIBER)?.toFloat() ?: 0f,
        sugar = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.SUGARS)?.toFloat() ?: 0f,
        saturatedFat = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.SATURATED_FAT)?.toFloat() ?: 0f,
        transFat = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.TRANS_FAT)?.toFloat() ?: 0f,
        sodium = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.SODIUM)?.toFloat() ?: 0f,
        cholesterol = USDANutrientIds.getNutrientValue(nutrients, USDANutrientIds.CHOLESTEROL)?.toFloat() ?: 0f,

        micronutrients = micronutrients,

        servingSize = servingSize?.toFloat(),
        servingUnit = servingSizeUnit ?: "g",
        servingDescription = householdServingFullText,
        portions = foodPortions?.mapNotNull { portion ->
            if (portion.gramWeight != null) {
                FoodPortion(
                    description = portion.portionDescription ?: portion.modifier ?: "serving",
                    gramWeight = portion.gramWeight.toFloat(),
                    modifier = portion.modifier
                )
            } else null
        } ?: emptyList(),

        ingredients = ingredients,
        verified = true,
        dataType = dataType,
        lastUpdated = publicationDate
    )
}