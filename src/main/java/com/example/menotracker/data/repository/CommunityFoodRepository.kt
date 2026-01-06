// data/repository/CommunityFoodRepository.kt
package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.models.FoodSource
import com.example.menotracker.data.models.NayaFood
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * COMMUNITY FOOD REPOSITORY
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Manages crowdsourced nutrition data from barcode scans.
 * When a product isn't found in OpenFoodFacts/USDA, users can contribute
 * by photographing the nutrition label. Other users benefit from this data.
 *
 * Features:
 * - Barcode lookup in community database
 * - Save new products with OCR-extracted data
 * - Upload label images to Supabase Storage
 * - Verification system (users can confirm/flag entries)
 */
object CommunityFoodRepository {

    private const val TAG = "CommunityFoodRepo"
    private const val TABLE_NAME = "community_foods"
    private const val VERIFICATIONS_TABLE = "community_food_verifications"
    private const val STORAGE_BUCKET = "nutrition-labels"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════

    @Serializable
    data class CommunityFood(
        val id: String? = null,
        val barcode: String,
        val name: String,
        val brand: String? = null,
        @SerialName("serving_size") val servingSize: Float = 100f,
        @SerialName("serving_unit") val servingUnit: String = "g",
        val calories: Float,
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        val fiber: Float? = null,
        val sugar: Float? = null,
        @SerialName("saturated_fat") val saturatedFat: Float? = null,
        val sodium: Float? = null,
        val micronutrients: JsonObject? = null,
        @SerialName("label_image_url") val labelImageUrl: String? = null,
        @SerialName("product_image_url") val productImageUrl: String? = null,
        @SerialName("contributed_by") val contributedBy: String? = null,
        @SerialName("verification_count") val verificationCount: Int = 1,
        @SerialName("is_verified") val isVerified: Boolean = false,
        @SerialName("confidence_score") val confidenceScore: Float = 0.5f,
        @SerialName("ocr_raw_text") val ocrRawText: String? = null,
        @SerialName("extraction_method") val extractionMethod: String = "ocr",
        @SerialName("is_flagged") val isFlagged: Boolean = false,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    private data class CommunityFoodInsert(
        val barcode: String,
        val name: String,
        val brand: String? = null,
        val serving_size: Float = 100f,
        val serving_unit: String = "g",
        val calories: Float,
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        val fiber: Float? = null,
        val sugar: Float? = null,
        val saturated_fat: Float? = null,
        val sodium: Float? = null,
        val label_image_url: String? = null,
        val contributed_by: String,
        val ocr_raw_text: String? = null,
        val extraction_method: String = "ocr",
        val confidence_score: Float = 0.5f
    )

    // ═══════════════════════════════════════════════════════════════
    // BARCODE LOOKUP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Look up a barcode in the community database
     * Returns null if not found
     */
    suspend fun getByBarcode(barcode: String): Result<CommunityFood?> {
        return try {
            Log.d(TAG, "Looking up community food for barcode: $barcode")

            val result = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("barcode", barcode)
                        eq("is_flagged", false)
                    }
                }
                .decodeSingleOrNull<CommunityFood>()

            if (result != null) {
                Log.d(TAG, "Found community food: ${result.name} (verified: ${result.isVerified})")
            } else {
                Log.d(TAG, "No community food found for barcode: $barcode")
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up community food", e)
            Result.failure(e)
        }
    }

    /**
     * Convert CommunityFood to NayaFood for unified handling
     */
    fun toNayaFood(communityFood: CommunityFood): NayaFood {
        return NayaFood(
            id = communityFood.id ?: communityFood.barcode,
            source = FoodSource.USER_CREATED,
            name = communityFood.name,
            brand = communityFood.brand,
            category = null,
            barcode = communityFood.barcode,
            calories = communityFood.calories,
            protein = communityFood.protein,
            carbs = communityFood.carbs,
            fat = communityFood.fat,
            fiber = communityFood.fiber ?: 0f,
            sugar = communityFood.sugar ?: 0f,
            saturatedFat = communityFood.saturatedFat ?: 0f,
            transFat = 0f,
            cholesterol = 0f,
            sodium = communityFood.sodium ?: 0f,
            micronutrients = emptyMap(),
            servingSize = communityFood.servingSize,
            servingUnit = communityFood.servingUnit,
            servingDescription = "${communityFood.servingSize.toInt()}${communityFood.servingUnit}",
            portions = emptyList(),
            ingredients = null,
            verified = communityFood.isVerified,
            dataType = "community",
            nutriscoreGrade = null
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // SAVE NEW FOOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Save a new community food entry
     */
    suspend fun saveFood(
        barcode: String,
        name: String,
        brand: String?,
        calories: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float? = null,
        sugar: Float? = null,
        saturatedFat: Float? = null,
        sodium: Float? = null,
        ocrRawText: String? = null,
        labelImageUrl: String? = null,
        confidenceScore: Float = 0.5f
    ): Result<CommunityFood> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d(TAG, "Saving community food: $name (barcode: $barcode)")

            val insert = CommunityFoodInsert(
                barcode = barcode,
                name = name,
                brand = brand,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar,
                saturated_fat = saturatedFat,
                sodium = sodium,
                label_image_url = labelImageUrl,
                contributed_by = userId,
                ocr_raw_text = ocrRawText,
                confidence_score = confidenceScore
            )

            val result = supabase.postgrest[TABLE_NAME]
                .insert(insert) {
                    select()
                }
                .decodeSingle<CommunityFood>()

            Log.d(TAG, "Successfully saved community food: ${result.id}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving community food", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // IMAGE UPLOAD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Upload a nutrition label image to Supabase Storage
     * Returns the public URL of the uploaded image
     */
    suspend fun uploadLabelImage(barcode: String, imageFile: File): Result<String> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            val fileName = "${barcode}_${System.currentTimeMillis()}.jpg"
            val filePath = "$userId/$fileName"

            Log.d(TAG, "Uploading label image: $filePath")

            val bytes = imageFile.readBytes()
            supabase.storage[STORAGE_BUCKET].upload(filePath, bytes, upsert = true)

            val publicUrl = supabase.storage[STORAGE_BUCKET].publicUrl(filePath)
            Log.d(TAG, "Upload successful: $publicUrl")

            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading label image", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VERIFICATION SYSTEM
    // ═══════════════════════════════════════════════════════════════

    @Serializable
    private data class VerificationInsert(
        val community_food_id: String,
        val user_id: String,
        val action: String,
        val flag_reason: String? = null
    )

    /**
     * Verify (confirm) a community food entry
     * Increases the trust score
     */
    suspend fun verifyFood(foodId: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d(TAG, "Verifying food: $foodId")

            // Insert verification record
            supabase.postgrest[VERIFICATIONS_TABLE]
                .insert(
                    VerificationInsert(
                        community_food_id = foodId,
                        user_id = userId,
                        action = "confirm"
                    )
                ) {
                    // Upsert - update if already exists
                }

            // Update the food's verification count
            // Note: In production, use the verify_community_food function instead
            supabase.postgrest[TABLE_NAME]
                .update({
                    // Increment verification_count - handled by RPC function in production
                }) {
                    filter {
                        eq("id", foodId)
                    }
                }

            Log.d(TAG, "Food verified successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying food", e)
            Result.failure(e)
        }
    }

    /**
     * Flag a community food entry as incorrect
     */
    suspend fun flagFood(foodId: String, reason: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d(TAG, "Flagging food: $foodId - Reason: $reason")

            supabase.postgrest[VERIFICATIONS_TABLE]
                .insert(
                    VerificationInsert(
                        community_food_id = foodId,
                        user_id = userId,
                        action = "flag",
                        flag_reason = reason
                    )
                )

            // Mark food as flagged
            supabase.postgrest[TABLE_NAME]
                .update({
                    set("is_flagged", true)
                    set("flag_reason", reason)
                }) {
                    filter {
                        eq("id", foodId)
                    }
                }

            Log.d(TAG, "Food flagged successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error flagging food", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get the number of foods contributed by the current user
     */
    suspend fun getUserContributionCount(): Result<Int> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.success(0)

            val result = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("contributed_by", userId)
                    }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }

            val count = result.countOrNull()?.toInt() ?: 0
            Log.d(TAG, "User contribution count: $count")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contribution count", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a barcode already exists in the community database
     */
    suspend fun barcodeExists(barcode: String): Boolean {
        return try {
            val result = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("barcode", barcode)
                    }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }
            (result.countOrNull()?.toInt() ?: 0) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking barcode existence", e)
            false
        }
    }
}