// app/src/main/java/com/example/myapplicationtest/data/NutritionRepository.kt

package com.example.menotracker.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.menotracker.data.models.*
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import com.example.menotracker.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════
// NUTRITION REPOSITORY
// ═══════════════════════════════════════════════════════════════

object NutritionRepository {
    private const val TAG = "NutritionRepository"

    // Use shared authenticated Supabase client (same as other repositories)
    private val supabase get() = SupabaseClient.client

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES FOR INSERT OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    @Serializable
    private data class NutritionLogInsert(
        val user_id: String,
        val date: String,
        val target_calories: Float,
        val target_protein: Float,
        val target_carbs: Float,
        val target_fat: Float
    )

    @Serializable
    private data class MealInsert(
        val nutrition_log_id: String,
        val meal_type: String,
        val meal_name: String,
        val time: String,
        val photo_url: String? = null,
        val ai_confidence: Float? = null
    )

    @Serializable
    private data class MealItemInsert(
        val meal_id: String,
        val item_name: String,
        val quantity: Float,
        val quantity_unit: String,
        // Core macros
        val calories: Float,
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        // Carb details
        val fiber: Float = 0f,
        val sugar: Float = 0f,
        // Fat details
        val saturated_fat: Float = 0f,
        val unsaturated_fat: Float = 0f,
        val trans_fat: Float = 0f,
        val omega3: Float = 0f,
        val omega6: Float = 0f,
        val cholesterol: Float = 0f,
        // Minerals
        val sodium: Float = 0f,
        val potassium: Float = 0f,
        val calcium: Float = 0f,
        val iron: Float = 0f,
        val magnesium: Float = 0f,
        val zinc: Float = 0f,
        val phosphorus: Float = 0f,
        // Vitamins
        val vitamin_a: Float = 0f,
        val vitamin_c: Float = 0f,
        val vitamin_d: Float = 0f,
        val vitamin_e: Float = 0f,
        val vitamin_k: Float = 0f,
        val vitamin_b1: Float = 0f,
        val vitamin_b2: Float = 0f,
        val vitamin_b3: Float = 0f,
        val vitamin_b6: Float = 0f,
        val vitamin_b12: Float = 0f,
        val folate: Float = 0f
    )

    @Serializable
    private data class NutritionGoalInsert(
        val user_id: String,
        val goal_type: String,
        val target_calories: Float,
        val target_protein: Float,
        val target_carbs: Float,
        val target_fat: Float,
        val meals_per_day: Int,
        val start_date: String? = null,
        val end_date: String? = null,
        val is_active: Boolean
    )

    // ═══════════════════════════════════════════════════════════════
    // AI PHOTO ANALYSIS (OpenAI Vision)
    // ═══════════════════════════════════════════════════════════════

    private val openAIService = OpenAIService.create()

    suspend fun analyzeMealPhoto(
        imageFile: File,
        mealType: MealType? = null,
        additionalContext: String? = null
    ): Result<AIPhotoAnalysisResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📸 Starting AI meal photo analysis with OpenAI...")

            // 1. Compress image to reduce upload time
            val originalBitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
            val originalSize = imageFile.length()
            Log.d(TAG, "📷 Original image: ${originalBitmap.width}x${originalBitmap.height}, ${originalSize / 1024}KB")

            // Scale down if too large (max 1024px on longest side)
            val maxDimension = 1024
            val scale = minOf(
                maxDimension.toFloat() / originalBitmap.width,
                maxDimension.toFloat() / originalBitmap.height,
                1.0f  // Don't scale up
            )
            val scaledWidth = (originalBitmap.width * scale).toInt()
            val scaledHeight = (originalBitmap.height * scale).toInt()
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                originalBitmap, scaledWidth, scaledHeight, true
            )

            // Compress to JPEG (85% quality)
            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()

            // Clean up bitmaps
            originalBitmap.recycle()
            scaledBitmap.recycle()

            // 2. Convert to base64
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            Log.d(TAG, "📷 Compressed image: ${scaledWidth}x${scaledHeight}, ${imageBytes.size / 1024}KB (${imageBytes.size * 100 / originalSize}% of original)")

            // 2. Build prompt for OpenAI with extended nutrients
            val prompt = buildString {
                append("You are a nutrition expert analyzing a meal photo. ")
                mealType?.let { append("This is a ${it.displayName}. ") }
                additionalContext?.let { append(it) }
                append("\n\nIMPORTANT: You MUST respond with ONLY valid JSON, no explanations or text before/after. ")
                append("If you cannot identify specific foods, make your best estimate based on what you see. ")
                append("Analyze this meal photo and provide DETAILED nutrition information including micronutrients. ")
                append("Estimate all nutrients based on typical food composition data. ")
                append("Return ONLY valid JSON with this EXACT structure:\n")
                append("{\n")
                append("  \"meal_name\": \"descriptive name\",\n")
                append("  \"items\": [\n")
                append("    {\n")
                append("      \"name\": \"item name\",\n")
                append("      \"quantity\": \"estimated portion\",\n")
                append("      \"quantity_value\": numeric_value,\n")
                append("      \"quantity_unit\": \"g\",\n")
                append("      \"calories\": numeric_value,\n")
                append("      \"protein\": grams,\n")
                append("      \"carbs\": grams,\n")
                append("      \"fat\": grams,\n")
                append("      \"fiber\": grams,\n")
                append("      \"sugar\": grams,\n")
                append("      \"saturated_fat\": grams,\n")
                append("      \"sodium\": milligrams,\n")
                append("      \"potassium\": milligrams,\n")
                append("      \"calcium\": milligrams,\n")
                append("      \"iron\": milligrams,\n")
                append("      \"vitamin_c\": milligrams,\n")
                append("      \"vitamin_d\": micrograms,\n")
                append("      \"confidence\": 0.0-1.0\n")
                append("    }\n")
                append("  ],\n")
                append("  \"total\": {\n")
                append("    \"calories\": total_sum,\n")
                append("    \"protein\": total_sum,\n")
                append("    \"carbs\": total_sum,\n")
                append("    \"fat\": total_sum\n")
                append("  },\n")
                append("  \"ai_confidence\": 0.0-1.0,\n")
                append("  \"suggestions\": \"optional tips\"\n")
                append("}")
            }

            // 3. Build OpenAI Vision API request
            val openAIRequest = OpenAIRequest(
                model = "gpt-4o",  // GPT-4 Turbo with Vision
                maxTokens = 2048,
                messages = listOf(
                    OpenAIMessage(
                        role = "user",
                        content = listOf(
                            OpenAIContent(
                                type = "text",
                                text = prompt
                            ),
                            OpenAIContent(
                                type = "image_url",
                                imageUrl = OpenAIImageUrl(
                                    url = "data:image/jpeg;base64,$base64Image"
                                )
                            )
                        )
                    )
                )
            )

            Log.d(TAG, "🔄 Calling OpenAI Vision API...")

            // 4. Call OpenAI API
            val response = openAIService.sendMessage(
                authorization = "Bearer ${ApiConfig.OPENAI_API_KEY}",
                request = openAIRequest
            )

            Log.d(TAG, "✅ OpenAI API response received (${response.usage.totalTokens} tokens)")

            // 4b. Track usage for cost analytics (fire-and-forget)
            val currentUserId = try { supabase.auth.currentUserOrNull()?.id } catch (e: Exception) { null }
            UsageTracker.logOpenAIVision(
                userId = currentUserId,
                inputTokens = response.usage.promptTokens,
                outputTokens = response.usage.completionTokens,
                model = "gpt-4o",
                imageSizeKb = (imageBytes.size / 1024),
                success = true
            )

            // 5. Extract analysis text from first choice
            val analysisText = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("No response content from OpenAI")

            Log.d(TAG, "📝 Analysis text: ${analysisText.take(200)}...")

            // 6. Extract JSON from response (AI might add text before/after JSON)
            val jsonStart = analysisText.indexOf("{")
            val jsonEnd = analysisText.lastIndexOf("}") + 1
            val jsonText = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                analysisText.substring(jsonStart, jsonEnd)
            } else {
                // No JSON found - AI refused or couldn't analyze
                Log.w(TAG, "⚠️ No JSON in response: ${analysisText.take(100)}")
                return@withContext Result.failure(
                    Exception("Could not analyze image. The AI responded: ${analysisText.take(150)}...")
                )
            }

            // 7. Parse nutrition analysis
            val analysis = try {
                json.decodeFromString<AIPhotoAnalysisResponse>(jsonText)
            } catch (parseError: Exception) {
                Log.e(TAG, "❌ JSON parse error: ${parseError.message}")
                Log.e(TAG, "❌ JSON text was: ${jsonText.take(200)}")
                return@withContext Result.failure(
                    Exception("Failed to parse nutrition data: ${parseError.message}")
                )
            }

            Log.d(TAG, "✅ Analysis complete: ${analysis.meal_name}")

            Result.success(analysis.copy(success = true))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error analyzing meal photo: ${e.message}", e)
            e.printStackTrace()

            // Track failed API call
            val currentUserId = try { supabase.auth.currentUserOrNull()?.id } catch (ex: Exception) { null }
            UsageTracker.logOpenAIVision(
                userId = currentUserId,
                inputTokens = 0,
                outputTokens = 0,
                model = "gpt-4o",
                success = false,
                errorMessage = e.message
            )

            Result.success(
                AIPhotoAnalysisResponse(
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NUTRITION LOGS
    // ═══════════════════════════════════════════════════════════════

    suspend fun getNutritionLog(userId: String, date: String): Result<NutritionLog> {
        return try {
            Log.d(TAG, "📊 Fetching nutrition log for $date")

            // Try to get existing log
            val logs = supabase.postgrest["nutrition_logs"]
                .select(
                    columns = Columns.raw("""
                        id, user_id, date, target_calories, target_protein, target_carbs, target_fat,
                        workout_session_id, notes, created_at,
                        meals (
                            id, nutrition_log_id, meal_type, meal_name, time, photo_url,
                            ai_analysis_id, ai_confidence, notes, created_at,
                            meal_items (
                                id, meal_id, food_id, item_name, quantity, quantity_unit,
                                calories, protein, carbs, fat,
                                fiber, sugar, saturated_fat, sodium, potassium,
                                calcium, iron, vitamin_c, vitamin_d, created_at
                            )
                        )
                    """)
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("date", date)
                    }
                }
                .decodeList<NutritionLog>()

            val log = logs.firstOrNull()

            if (log != null) {
                Log.d(TAG, "✅ Existing log found: ${log.meals.size} meals")
                Result.success(log)
            } else {
                // Create new log using RPC function (SECURITY DEFINER bypasses RLS)
                Log.d(TAG, "Creating new log for $date using RPC")

                // ✅ FIX: Use RPC function instead of direct INSERT
                // This uses SECURITY DEFINER and bypasses RLS issues
                val logIdResult = supabase.postgrest.rpc(
                    "get_or_create_nutrition_log",
                    mapOf(
                        "p_user_id" to userId,
                        "p_date" to date
                    )
                ).decodeAs<String>()

                Log.d(TAG, "✅ RPC returned log ID: $logIdResult")

                // Fetch the newly created/found log with proper structure
                val newLogs = supabase.postgrest["nutrition_logs"]
                    .select(
                        columns = Columns.raw("""
                            id, user_id, date, target_calories, target_protein, target_carbs, target_fat,
                            workout_session_id, notes, created_at,
                            meals (
                                id, nutrition_log_id, meal_type, meal_name, time, photo_url,
                                ai_analysis_id, ai_confidence, notes, created_at,
                                meal_items (
                                    id, meal_id, food_id, item_name, quantity, quantity_unit,
                                    calories, protein, carbs, fat, created_at
                                )
                            )
                        """)
                    ) {
                        filter {
                            eq("id", logIdResult)
                        }
                    }
                    .decodeList<NutritionLog>()

                val newLog = newLogs.firstOrNull()
                    ?: throw Exception("Failed to fetch created nutrition log")

                Log.d(TAG, "✅ New log created: ${newLog.id}")
                Result.success(newLog)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching nutrition log", e)
            Result.failure(e)
        }
    }

    suspend fun getNutritionLogs(
        userId: String,
        startDate: String,
        endDate: String
    ): Result<List<NutritionLog>> {
        return try {
            Log.d(TAG, "📊 Fetching nutrition logs from $startDate to $endDate")

            val logs = supabase.postgrest["nutrition_logs"]
                .select(
                    columns = Columns.raw("""
                        id, user_id, date, target_calories, target_protein, target_carbs, target_fat,
                        workout_session_id, notes, created_at,
                        meals (
                            id, nutrition_log_id, meal_type, meal_name, time, photo_url,
                            ai_analysis_id, ai_confidence, notes, created_at,
                            meal_items (
                                id, meal_id, food_id, item_name, quantity, quantity_unit,
                                calories, protein, carbs, fat,
                                fiber, sugar, saturated_fat, sodium, potassium,
                                calcium, iron, vitamin_c, vitamin_d, created_at
                            )
                        )
                    """)
                ) {
                    filter {
                        eq("user_id", userId)
                        gte("date", startDate)
                        lte("date", endDate)
                    }
                    order(column = "date", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<NutritionLog>()

            Log.d(TAG, "✅ Fetched ${logs.size} nutrition logs")
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching nutrition logs", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MEALS
    // ═══════════════════════════════════════════════════════════════

    suspend fun saveMealFromAIAnalysis(
        userId: String,
        date: String,
        mealType: MealType,
        analysis: AIPhotoAnalysisResponse,
        photoUrl: String? = null
    ): Result<Meal> {
        return try {
            Log.d(TAG, "💾 Saving meal from AI analysis: ${analysis.meal_name}")

            // 1. Get or create nutrition log
            val logResult = getNutritionLog(userId, date)
            if (logResult.isFailure) {
                return Result.failure(logResult.exceptionOrNull() ?: Exception("Failed to get nutrition log"))
            }
            val log = logResult.getOrThrow()

            // 2. Insert meal using typed data class
            val mealData = MealInsert(
                nutrition_log_id = log.id,
                meal_type = mealType.name.lowercase(),
                meal_name = analysis.meal_name,
                time = kotlinx.datetime.Clock.System.now().toString(),
                photo_url = photoUrl,
                ai_confidence = analysis.ai_confidence
            )

            // ✅ FIX: Insert using typed object
            supabase.postgrest["meals"]
                .insert(mealData)

            // Fetch the newly inserted meal to get its ID
            val insertedMeals = supabase.postgrest["meals"]
                .select(columns = Columns.raw("""
                    id, nutrition_log_id, meal_type, meal_name, time, photo_url,
                    ai_analysis_id, ai_confidence, notes, created_at
                """)) {
                    filter {
                        eq("nutrition_log_id", log.id)
                        eq("meal_name", analysis.meal_name)
                    }
                    order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }
                .decodeList<Meal>()

            val mealResponse = insertedMeals.firstOrNull()
                ?: throw Exception("Failed to insert meal")

            Log.d(TAG, "✅ Meal inserted with ID: ${mealResponse.id}")

            // 3. Insert meal items using typed data class (including ALL extended nutrients)
            analysis.items.forEach { item ->
                val itemData = MealItemInsert(
                    meal_id = mealResponse.id,
                    item_name = item.name,
                    quantity = item.quantity_value,
                    quantity_unit = item.quantity_unit,
                    // Core macros
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    // Carb details
                    fiber = item.fiber,
                    sugar = item.sugar,
                    // Fat details
                    saturated_fat = item.saturated_fat,
                    unsaturated_fat = item.unsaturated_fat,
                    trans_fat = item.trans_fat,
                    omega3 = item.omega3,
                    omega6 = item.omega6,
                    cholesterol = item.cholesterol,
                    // Minerals
                    sodium = item.sodium,
                    potassium = item.potassium,
                    calcium = item.calcium,
                    iron = item.iron,
                    magnesium = item.magnesium,
                    zinc = item.zinc,
                    phosphorus = item.phosphorus,
                    // Vitamins
                    vitamin_a = item.vitamin_a,
                    vitamin_c = item.vitamin_c,
                    vitamin_d = item.vitamin_d,
                    vitamin_e = item.vitamin_e,
                    vitamin_k = item.vitamin_k,
                    vitamin_b1 = item.vitamin_b1,
                    vitamin_b2 = item.vitamin_b2,
                    vitamin_b3 = item.vitamin_b3,
                    vitamin_b6 = item.vitamin_b6,
                    vitamin_b12 = item.vitamin_b12,
                    folate = item.folate
                )

                // ✅ FIX: Insert using typed object
                supabase.postgrest["meal_items"]
                    .insert(itemData)
            }

            Log.d(TAG, "✅ Meal saved successfully with ${analysis.items.size} items")

            // 4. Fetch complete meal with items
            val completeMeals = supabase.postgrest["meals"]
                .select(
                    columns = Columns.raw("""
                        id, nutrition_log_id, meal_type, meal_name, time, photo_url,
                        ai_analysis_id, ai_confidence, notes, created_at,
                        meal_items (
                            id, meal_id, food_id, item_name, quantity, quantity_unit,
                            calories, protein, carbs, fat, created_at
                        )
                    """)
                ) {
                    filter {
                        eq("id", mealResponse.id)
                    }
                }
                .decodeList<Meal>()

            val completeMeal = completeMeals.firstOrNull()
                ?: throw Exception("Failed to fetch complete meal")

            Result.success(completeMeal)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving meal", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMeal(mealId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Deleting meal: $mealId")

            supabase.postgrest["meals"]
                .delete {
                    filter {
                        eq("id", mealId)
                    }
                }

            Log.d(TAG, "✅ Meal deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting meal", e)
            Result.failure(e)
        }
    }

    /**
     * Update meal and its items
     * - Updates meal metadata (name, time, notes)
     * - Updates existing items
     * - Adds new items (id starts with "new_")
     * - Deletes removed items
     */
    suspend fun updateMeal(
        meal: Meal,
        items: List<MealItem>,
        deletedItemIds: List<String>
    ): Result<Meal> {
        return try {
            Log.d(TAG, "📝 Updating meal: ${meal.id}")
            Log.d(TAG, "📝 Items to update/add: ${items.size}")
            Log.d(TAG, "📝 Items to delete: ${deletedItemIds.size}")

            // 1. Update meal metadata
            @Serializable
            data class MealUpdate(
                val meal_name: String?,
                val time: String,
                val notes: String?
            )

            supabase.postgrest["meals"]
                .update(MealUpdate(
                    meal_name = meal.mealName,
                    time = meal.time,
                    notes = meal.notes
                )) {
                    filter { eq("id", meal.id) }
                }

            Log.d(TAG, "✅ Meal metadata updated")

            // 2. Delete removed items
            if (deletedItemIds.isNotEmpty()) {
                deletedItemIds.forEach { itemId ->
                    supabase.postgrest["meal_items"]
                        .delete {
                            filter { eq("id", itemId) }
                        }
                }
                Log.d(TAG, "✅ Deleted ${deletedItemIds.size} items")
            }

            // 3. Update existing items or insert new ones
            items.forEach { item ->
                if (item.id.startsWith("new_")) {
                    // Insert new item
                    val itemData = MealItemInsert(
                        meal_id = meal.id,
                        item_name = item.itemName,
                        quantity = item.quantity,
                        quantity_unit = item.quantityUnit,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        fiber = item.fiber,
                        sugar = item.sugar,
                        saturated_fat = item.saturatedFat,
                        unsaturated_fat = item.unsaturatedFat,
                        trans_fat = item.transFat,
                        omega3 = item.omega3,
                        omega6 = item.omega6,
                        cholesterol = item.cholesterol,
                        sodium = item.sodium,
                        potassium = item.potassium,
                        calcium = item.calcium,
                        iron = item.iron,
                        magnesium = item.magnesium,
                        zinc = item.zinc,
                        phosphorus = item.phosphorus,
                        vitamin_a = item.vitaminA,
                        vitamin_c = item.vitaminC,
                        vitamin_d = item.vitaminD,
                        vitamin_e = item.vitaminE,
                        vitamin_k = item.vitaminK,
                        vitamin_b1 = item.vitaminB1,
                        vitamin_b2 = item.vitaminB2,
                        vitamin_b3 = item.vitaminB3,
                        vitamin_b6 = item.vitaminB6,
                        vitamin_b12 = item.vitaminB12,
                        folate = item.folate
                    )
                    supabase.postgrest["meal_items"].insert(itemData)
                    Log.d(TAG, "✅ Inserted new item: ${item.itemName}")
                } else {
                    // Update existing item
                    @Serializable
                    data class MealItemUpdate(
                        val item_name: String,
                        val quantity: Float,
                        val quantity_unit: String,
                        val calories: Float,
                        val protein: Float,
                        val carbs: Float,
                        val fat: Float,
                        val fiber: Float,
                        val sugar: Float,
                        val saturated_fat: Float,
                        val unsaturated_fat: Float,
                        val trans_fat: Float,
                        val omega3: Float,
                        val omega6: Float,
                        val cholesterol: Float,
                        val sodium: Float,
                        val potassium: Float,
                        val calcium: Float,
                        val iron: Float,
                        val magnesium: Float,
                        val zinc: Float,
                        val phosphorus: Float,
                        val vitamin_a: Float,
                        val vitamin_c: Float,
                        val vitamin_d: Float,
                        val vitamin_e: Float,
                        val vitamin_k: Float,
                        val vitamin_b1: Float,
                        val vitamin_b2: Float,
                        val vitamin_b3: Float,
                        val vitamin_b6: Float,
                        val vitamin_b12: Float,
                        val folate: Float
                    )

                    supabase.postgrest["meal_items"]
                        .update(MealItemUpdate(
                            item_name = item.itemName,
                            quantity = item.quantity,
                            quantity_unit = item.quantityUnit,
                            calories = item.calories,
                            protein = item.protein,
                            carbs = item.carbs,
                            fat = item.fat,
                            fiber = item.fiber,
                            sugar = item.sugar,
                            saturated_fat = item.saturatedFat,
                            unsaturated_fat = item.unsaturatedFat,
                            trans_fat = item.transFat,
                            omega3 = item.omega3,
                            omega6 = item.omega6,
                            cholesterol = item.cholesterol,
                            sodium = item.sodium,
                            potassium = item.potassium,
                            calcium = item.calcium,
                            iron = item.iron,
                            magnesium = item.magnesium,
                            zinc = item.zinc,
                            phosphorus = item.phosphorus,
                            vitamin_a = item.vitaminA,
                            vitamin_c = item.vitaminC,
                            vitamin_d = item.vitaminD,
                            vitamin_e = item.vitaminE,
                            vitamin_k = item.vitaminK,
                            vitamin_b1 = item.vitaminB1,
                            vitamin_b2 = item.vitaminB2,
                            vitamin_b3 = item.vitaminB3,
                            vitamin_b6 = item.vitaminB6,
                            vitamin_b12 = item.vitaminB12,
                            folate = item.folate
                        )) {
                            filter { eq("id", item.id) }
                        }
                    Log.d(TAG, "✅ Updated item: ${item.itemName}")
                }
            }

            // 4. Fetch updated meal with all items
            val updatedMeals = supabase.postgrest["meals"]
                .select(
                    columns = Columns.raw("""
                        id, nutrition_log_id, meal_type, meal_name, time, photo_url,
                        ai_analysis_id, ai_confidence, notes, created_at,
                        meal_items (
                            id, meal_id, food_id, item_name, quantity, quantity_unit,
                            calories, protein, carbs, fat,
                            fiber, sugar, saturated_fat, unsaturated_fat, trans_fat,
                            omega3, omega6, cholesterol,
                            sodium, potassium, calcium, iron, magnesium, zinc, phosphorus,
                            vitamin_a, vitamin_c, vitamin_d, vitamin_e, vitamin_k,
                            vitamin_b1, vitamin_b2, vitamin_b3, vitamin_b6, vitamin_b12, folate,
                            created_at
                        )
                    """)
                ) {
                    filter { eq("id", meal.id) }
                }
                .decodeList<Meal>()

            val updatedMeal = updatedMeals.firstOrNull()
                ?: throw Exception("Failed to fetch updated meal")

            Log.d(TAG, "✅ Meal update complete: ${updatedMeal.items.size} items")
            Result.success(updatedMeal)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating meal", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NUTRITION GOALS
    // ═══════════════════════════════════════════════════════════════

    suspend fun getActiveNutritionGoal(userId: String): Result<NutritionGoal?> {
        return try {
            Log.d(TAG, "🎯 Fetching active nutrition goal")

            val goals = supabase.postgrest["nutrition_goals"]
                .select() {
                    filter {
                        eq("user_id", userId)
                        eq("is_active", true)
                    }
                    limit(1)
                }
                .decodeList<NutritionGoal>()

            val goal = goals.firstOrNull()
            Log.d(TAG, if (goal != null) "✅ Active goal found: ${goal.goalType}" else "ℹ️ No active goal")

            Result.success(goal)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching active goal", e)
            Result.failure(e)
        }
    }

    suspend fun saveNutritionGoal(goal: NutritionGoal): Result<NutritionGoal> {
        return try {
            Log.d(TAG, "💾 Saving nutrition goal: ${goal.goalType}")

            // Deactivate existing active goals using typed update
            if (goal.isActive) {
                @Serializable
                data class IsActiveUpdate(val is_active: Boolean)

                supabase.postgrest["nutrition_goals"]
                    .update(IsActiveUpdate(is_active = false)) {
                        filter {
                            eq("user_id", goal.userId)
                            eq("is_active", true)
                        }
                    }
            }

            // Insert new goal using typed data class
            val goalData = NutritionGoalInsert(
                user_id = goal.userId,
                goal_type = goal.goalType.name.lowercase(),
                target_calories = goal.targetCalories,
                target_protein = goal.targetProtein,
                target_carbs = goal.targetCarbs,
                target_fat = goal.targetFat,
                meals_per_day = goal.mealsPerDay,
                start_date = goal.startDate,
                end_date = goal.endDate,
                is_active = goal.isActive
            )

            // ✅ FIX: Insert using typed object
            val savedGoal = supabase.postgrest["nutrition_goals"]
                .insert(goalData) {
                    select()
                }
                .decodeSingle<NutritionGoal>()

            Log.d(TAG, "✅ Nutrition goal saved successfully")
            Result.success(savedGoal)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving nutrition goal", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RECENT FOODS (for Food Search)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Data class for recent meal item from Supabase
     */
    @Serializable
    data class RecentMealItem(
        val id: String,
        val item_name: String,
        val quantity: Float,
        val quantity_unit: String,
        val calories: Float,
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        val fiber: Float = 0f,
        val sugar: Float = 0f,
        val sodium: Float = 0f,
        val created_at: String
    )

    /**
     * Get user's recently logged food items (unique by name)
     * Returns the most recent 50 unique foods the user has logged
     */
    suspend fun getRecentMealItems(userId: String, limit: Int = 50): Result<List<RecentMealItem>> {
        return try {
            Log.d(TAG, "🕐 Fetching recent meal items for user: $userId")

            // Query meal_items through meals and nutrition_logs to get user's items
            // We need to join through the hierarchy: meal_items -> meals -> nutrition_logs
            val items = supabase.postgrest.rpc(
                function = "get_recent_meal_items",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_limit", limit)
                }
            ).decodeList<RecentMealItem>()

            Log.d(TAG, "✅ Found ${items.size} recent meal items")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching recent meal items", e)
            // Fallback: Try direct query if RPC doesn't exist
            try {
                Log.d(TAG, "🔄 Trying fallback query...")
                getRecentMealItemsFallback(userId, limit)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "❌ Fallback also failed", fallbackError)
                Result.failure(e)
            }
        }
    }

    /**
     * Fallback query for recent meal items (without RPC function)
     */
    private suspend fun getRecentMealItemsFallback(userId: String, limit: Int): Result<List<RecentMealItem>> {
        // Get recent nutrition logs for this user
        val recentLogs = supabase.postgrest["nutrition_logs"]
            .select(columns = Columns.raw("id")) {
                filter { eq("user_id", userId) }
                order(column = "date", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(30) // Last 30 days of logs
            }
            .decodeList<LogIdOnly>()

        if (recentLogs.isEmpty()) {
            Log.d(TAG, "ℹ️ No nutrition logs found")
            return Result.success(emptyList())
        }

        val logIds = recentLogs.map { it.id }

        // Get meals for these logs
        val meals = supabase.postgrest["meals"]
            .select(columns = Columns.raw("id")) {
                filter { isIn("nutrition_log_id", logIds) }
            }
            .decodeList<MealIdOnly>()

        if (meals.isEmpty()) {
            Log.d(TAG, "ℹ️ No meals found")
            return Result.success(emptyList())
        }

        val mealIds = meals.map { it.id }

        // Get meal items for these meals
        val items = supabase.postgrest["meal_items"]
            .select(columns = Columns.raw("""
                id, item_name, quantity, quantity_unit,
                calories, protein, carbs, fat, fiber, sugar, sodium, created_at
            """)) {
                filter { isIn("meal_id", mealIds) }
                order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<RecentMealItem>()

        // Deduplicate by item_name (keep most recent)
        val uniqueItems = items
            .distinctBy { it.item_name.lowercase().trim() }
            .take(limit)

        Log.d(TAG, "✅ Fallback found ${uniqueItems.size} unique recent items")
        return Result.success(uniqueItems)
    }

    @Serializable
    private data class LogIdOnly(val id: String)

    @Serializable
    private data class MealIdOnly(val id: String)

    // ═══════════════════════════════════════════════════════════════
    // MEAL PHOTO STORAGE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Save meal photo - automatically chooses storage based on user's coach status
     * - WITH Coach → Supabase Storage (cloud)
     * - WITHOUT Coach → Local device storage
     */
    suspend fun saveMealPhoto(
        context: Context,
        imageFile: File,
        userId: String,
        hasCoach: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext if (hasCoach) {
            uploadMealPhotoToSupabase(imageFile, userId)
        } else {
            saveMealPhotoLocally(context, imageFile, userId)
        }
    }

    /**
     * Upload meal photo to Supabase Storage (for users with coach)
     */
    private suspend fun uploadMealPhotoToSupabase(
        imageFile: File,
        userId: String
    ): Result<String> {
        return try {
            Log.d(TAG, "☁️ Uploading meal photo to Supabase Storage...")

            val timestamp = System.currentTimeMillis()
            val fileName = "meal_${userId}_${timestamp}.jpg"
            val bucketName = "meal-photos"
            val filePath = "$userId/$fileName"

            // Read file bytes
            val imageBytes = imageFile.readBytes()

            // Upload to Supabase Storage
            val bucket = supabase.storage.from(bucketName)
            bucket.upload(filePath, imageBytes)

            // Get public URL
            val publicUrl = bucket.publicUrl(filePath)

            // Track storage upload for cost analytics
            UsageTracker.logStorageUpload(
                userId = userId,
                bucket = bucketName,
                fileSizeBytes = imageBytes.size.toLong(),
                fileType = "image/jpeg",
                success = true
            )

            Log.d(TAG, "✅ Photo uploaded to Supabase: $publicUrl")
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading photo to Supabase", e)

            // Track failed upload
            UsageTracker.logStorageUpload(
                userId = userId,
                bucket = "meal-photos",
                fileSizeBytes = 0,
                success = false,
                errorMessage = e.message
            )

            Result.failure(e)
        }
    }

    /**
     * Save meal photo locally on device (for users without coach)
     */
    private suspend fun saveMealPhotoLocally(
        context: Context,
        imageFile: File,
        userId: String
    ): Result<String> {
        return try {
            Log.d(TAG, "📱 Saving meal photo locally...")

            // Get app's internal storage directory
            val mealPhotosDir = File(context.filesDir, "meal_photos/$userId")

            // Create directory if it doesn't exist
            if (!mealPhotosDir.exists()) {
                mealPhotosDir.mkdirs()
            }

            // Generate unique filename
            val timestamp = System.currentTimeMillis()
            val fileName = "meal_${timestamp}.jpg"
            val destFile = File(mealPhotosDir, fileName)

            // Copy image file to app storage
            imageFile.copyTo(destFile, overwrite = true)

            val localPath = destFile.absolutePath

            Log.d(TAG, "✅ Photo saved locally: $localPath")
            Result.success(localPath)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving photo locally", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FREQUENT MEALS (Quick Add Feature)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get frequent meals for a user, sorted by favorites first, then usage count
     */
    suspend fun getFrequentMeals(
        userId: String,
        limit: Int = 10,
        mealType: MealType? = null
    ): Result<List<FrequentMeal>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Fetching frequent meals for user: $userId")

            val query = supabase.postgrest["frequent_meals"]
                .select() {
                    filter {
                        eq("user_id", userId)
                        if (mealType != null) {
                            eq("meal_type", mealType.name.lowercase())
                        }
                    }
                    order("is_favorite", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    order("usage_count", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    order("last_used_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<FrequentMealDB>()

            // Convert DB model to domain model (parse JSONB items)
            val frequentMeals = query.map { it.toDomainModel() }

            Log.d(TAG, "✅ Fetched ${frequentMeals.size} frequent meals")
            Result.success(frequentMeals)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching frequent meals", e)
            Result.failure(e)
        }
    }

    /**
     * Get favorite frequent meals only
     */
    suspend fun getFavoriteFrequentMeals(userId: String): Result<List<FrequentMeal>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "⭐ Fetching favorite frequent meals")

            val query = supabase.postgrest["frequent_meals"]
                .select() {
                    filter {
                        eq("user_id", userId)
                        eq("is_favorite", true)
                    }
                    order("usage_count", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<FrequentMealDB>()

            val favorites = query.map { it.toDomainModel() }
            Log.d(TAG, "✅ Fetched ${favorites.size} favorites")
            Result.success(favorites)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching favorites", e)
            Result.failure(e)
        }
    }

    /**
     * Upsert a frequent meal - increments usage if exists, creates if not
     * Called automatically after saving a meal via AI analysis
     * @param isFavorite If true, marks the meal as a favorite (user explicitly chose Quick Add)
     */
    suspend fun upsertFrequentMeal(
        userId: String,
        mealName: String,
        mealType: MealType,
        items: List<FrequentMealItem>,
        totalCalories: Float,
        totalProtein: Float,
        totalCarbs: Float,
        totalFat: Float,
        isFavorite: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 Upserting frequent meal: $mealName (favorite=$isFavorite)")

            // Convert items to JSON string
            val itemsJson = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(FrequentMealItem.serializer()),
                items
            )

            // Use the DB function for upsert - use JsonObject to avoid serialization issues
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_name", mealName)
                put("p_meal_type", mealType.name.lowercase())
                put("p_items", json.parseToJsonElement(itemsJson))
                put("p_total_calories", totalCalories)
                put("p_total_protein", totalProtein)
                put("p_total_carbs", totalCarbs)
                put("p_total_fat", totalFat)
            }

            val result = supabase.postgrest.rpc(
                "upsert_frequent_meal",
                params
            ).decodeAs<String>()

            Log.d(TAG, "✅ Frequent meal upserted: $result")

            // If user explicitly marked as favorite, set the favorite flag
            if (isFavorite) {
                toggleFrequentMealFavorite(result, true)
                Log.d(TAG, "⭐ Marked as favorite")
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error upserting frequent meal", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle favorite status for a frequent meal
     */
    suspend fun toggleFrequentMealFavorite(mealId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "⭐ Setting favorite=$isFavorite for meal: $mealId")

            @Serializable
            data class FavoriteUpdate(val is_favorite: Boolean)

            supabase.postgrest["frequent_meals"]
                .update(FavoriteUpdate(is_favorite = isFavorite)) {
                    filter { eq("id", mealId) }
                }

            Log.d(TAG, "✅ Favorite toggled")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error toggling favorite", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a frequent meal
     */
    suspend fun deleteFrequentMeal(mealId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Deleting frequent meal: $mealId")

            supabase.postgrest["frequent_meals"]
                .delete { filter { eq("id", mealId) } }

            Log.d(TAG, "✅ Frequent meal deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting frequent meal", e)
            Result.failure(e)
        }
    }

    /**
     * Get common add-ons for a base meal
     */
    suspend fun getFrequentAddOns(baseMealName: String, userId: String): Result<List<FrequentAddOn>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Fetching add-ons for: $baseMealName")

            val addons = supabase.postgrest["frequent_addons"]
                .select() {
                    filter {
                        eq("user_id", userId)
                        eq("base_meal_name", baseMealName)
                    }
                    order("combination_count", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(5)
                }
                .decodeList<FrequentAddOn>()

            Log.d(TAG, "✅ Fetched ${addons.size} add-ons")
            Result.success(addons)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching add-ons", e)
            Result.failure(e)
        }
    }

    /**
     * Record an add-on combination (called when user adds extra to a frequent meal)
     */
    suspend fun recordAddOnCombination(
        userId: String,
        baseMealId: String?,
        baseMealName: String,
        addonItem: FrequentMealItem
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Recording add-on: ${addonItem.name} for $baseMealName")

            supabase.postgrest.rpc(
                "record_addon_combination",
                mapOf(
                    "p_user_id" to userId,
                    "p_base_meal_id" to baseMealId,
                    "p_base_meal_name" to baseMealName,
                    "p_addon_name" to addonItem.name,
                    "p_addon_quantity" to addonItem.quantity,
                    "p_addon_unit" to addonItem.quantityUnit,
                    "p_addon_calories" to addonItem.calories,
                    "p_addon_protein" to addonItem.protein,
                    "p_addon_carbs" to addonItem.carbs,
                    "p_addon_fat" to addonItem.fat
                )
            )

            Log.d(TAG, "✅ Add-on combination recorded")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error recording add-on", e)
            Result.failure(e)
        }
    }

    /**
     * Quick-add a frequent meal to today's log
     * Creates a new meal entry using the stored frequent meal data
     */
    suspend fun quickAddFrequentMeal(
        userId: String,
        frequentMeal: FrequentMeal,
        additionalItems: List<FrequentMealItem> = emptyList()
    ): Result<Meal> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "⚡ Quick-adding frequent meal: ${frequentMeal.name}")

            val today = java.time.LocalDate.now().toString()

            // Combine base items with additional items
            val allItems = frequentMeal.items + additionalItems
            val totalCalories = allItems.sumOf { it.calories.toDouble() }.toFloat()
            val totalProtein = allItems.sumOf { it.protein.toDouble() }.toFloat()
            val totalCarbs = allItems.sumOf { it.carbs.toDouble() }.toFloat()
            val totalFat = allItems.sumOf { it.fat.toDouble() }.toFloat()

            // Create AI analysis response from frequent meal data
            val analysisResponse = AIPhotoAnalysisResponse(
                success = true,
                meal_name = frequentMeal.name,
                items = allItems.map { item ->
                    AIAnalyzedItem(
                        name = item.name,
                        quantity = "${item.quantity}${item.quantityUnit}",
                        quantity_value = item.quantity,
                        quantity_unit = item.quantityUnit,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        confidence = 1.0f, // High confidence for saved meals
                        fiber = item.fiber,
                        sugar = item.sugar,
                        sodium = item.sodium
                    )
                },
                total = AIAnalyzedTotals(
                    calories = totalCalories,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat
                ),
                ai_confidence = 1.0f
            )

            // Save the meal using existing method
            val result = saveMealFromAIAnalysis(
                userId = userId,
                date = today,
                mealType = frequentMeal.mealTypeEnum,
                analysis = analysisResponse,
                photoUrl = null
            )

            if (result.isSuccess) {
                // Update frequent meal usage count
                upsertFrequentMeal(
                    userId = userId,
                    mealName = frequentMeal.name,
                    mealType = frequentMeal.mealTypeEnum,
                    items = frequentMeal.items,
                    totalCalories = frequentMeal.totalCalories,
                    totalProtein = frequentMeal.totalProtein,
                    totalCarbs = frequentMeal.totalCarbs,
                    totalFat = frequentMeal.totalFat
                )

                // Record add-on combinations if any
                additionalItems.forEach { addon ->
                    recordAddOnCombination(
                        userId = userId,
                        baseMealId = frequentMeal.id,
                        baseMealName = frequentMeal.name,
                        addonItem = addon
                    )
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error quick-adding meal", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADD SINGLE FOOD ITEM (from FoodSearchScreen)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add a single food item from search results to today's log
     */
    suspend fun addSingleFoodItem(
        userId: String,
        mealType: MealType,
        mealItem: MealItem
    ): Result<Meal> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "➕ Adding single food item: ${mealItem.itemName}")

            val today = java.time.LocalDate.now().toString()

            // Create AI analysis response from single item
            val analysisResponse = AIPhotoAnalysisResponse(
                success = true,
                meal_name = mealItem.itemName,
                items = listOf(
                    AIAnalyzedItem(
                        name = mealItem.itemName,
                        quantity = "${mealItem.quantity.toInt()}${mealItem.quantityUnit}",
                        quantity_value = mealItem.quantity,
                        quantity_unit = mealItem.quantityUnit,
                        calories = mealItem.calories,
                        protein = mealItem.protein,
                        carbs = mealItem.carbs,
                        fat = mealItem.fat,
                        confidence = 1.0f,
                        fiber = mealItem.fiber,
                        sugar = mealItem.sugar,
                        sodium = mealItem.sodium
                    )
                ),
                total = AIAnalyzedTotals(
                    calories = mealItem.calories,
                    protein = mealItem.protein,
                    carbs = mealItem.carbs,
                    fat = mealItem.fat
                ),
                ai_confidence = 1.0f
            )

            // Save the meal using existing method
            saveMealFromAIAnalysis(
                userId = userId,
                date = today,
                mealType = mealType,
                analysis = analysisResponse,
                photoUrl = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding single food item", e)
            Result.failure(e)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DB MODEL FOR FREQUENT MEALS (handles JSONB items column)
// ═══════════════════════════════════════════════════════════════

@Serializable
private data class FrequentMealDB(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("meal_type") val mealType: String,
    val items: String, // JSONB comes as string
    @SerialName("total_calories") val totalCalories: Float = 0f,
    @SerialName("total_protein") val totalProtein: Float = 0f,
    @SerialName("total_carbs") val totalCarbs: Float = 0f,
    @SerialName("total_fat") val totalFat: Float = 0f,
    @SerialName("usage_count") val usageCount: Int = 1,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_custom") val isCustom: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomainModel(): FrequentMeal {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val parsedItems = try {
            json.decodeFromString<List<FrequentMealItem>>(items)
        } catch (e: Exception) {
            emptyList()
        }

        return FrequentMeal(
            id = id,
            userId = userId,
            name = name,
            mealType = mealType,
            items = parsedItems,
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            usageCount = usageCount,
            lastUsedAt = lastUsedAt,
            isFavorite = isFavorite,
            isCustom = isCustom,
            createdAt = createdAt
        )
    }
}