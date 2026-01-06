package com.example.menotracker.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repository for managing partner referral codes
 * Allows users to enter referral codes from partners (influencers, gyms, coaches)
 */
object ReferralRepository {
    private const val TAG = "ReferralRepository"

    private val supabase = SupabaseClient.client

    /**
     * Validates a referral code and returns the partner info if valid
     */
    suspend fun validateReferralCode(code: String): Result<PartnerInfo?> {
        return try {
            Log.d(TAG, "Validating referral code: $code")

            val result = supabase.postgrest["partners"]
                .select(Columns.list("id", "name", "referral_code", "partner_type", "status")) {
                    filter {
                        eq("referral_code", code.uppercase().trim())
                        eq("status", "active")
                    }
                }
                .decodeList<PartnerInfo>()

            if (result.isNotEmpty()) {
                Log.d(TAG, "Valid referral code found: ${result.first().name}")
                Result.success(result.first())
            } else {
                Log.d(TAG, "Referral code not found or inactive")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating referral code: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Records a referral code entry (for analytics tracking)
     */
    suspend fun recordReferralEntry(
        userId: String,
        referralCode: String,
        partnerId: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Recording referral entry for user: $userId, code: $referralCode")

            val entry = ReferralCodeEntryInsert(
                userId = userId,
                referralCode = referralCode.uppercase().trim(),
                partnerId = partnerId,
                source = "app_settings"
            )

            supabase.postgrest["referral_code_entries"]
                .insert(entry)

            Log.d(TAG, "Referral entry recorded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording referral entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a partner referral record when a user subscribes
     * This links the user to the partner for commission tracking
     */
    suspend fun createPartnerReferral(
        userId: String,
        partnerId: String,
        subscriptionType: String,
        subscriptionAmountUsd: Double
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Creating partner referral for user: $userId, partner: $partnerId")

            val referral = PartnerReferralInsert(
                partnerId = partnerId,
                referredUserId = userId,
                subscriptionType = subscriptionType,
                subscriptionAmountUsd = subscriptionAmountUsd,
                status = "active"
            )

            supabase.postgrest["partner_referrals"]
                .insert(referral)

            Log.d(TAG, "Partner referral created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating partner referral: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Gets the partner ID associated with a user (if any)
     */
    suspend fun getUserReferralPartner(userId: String): Result<String?> {
        return try {
            Log.d(TAG, "Getting referral partner for user: $userId")

            val result = supabase.postgrest["partner_referrals"]
                .select(Columns.list("partner_id")) {
                    filter {
                        eq("referred_user_id", userId)
                    }
                    limit(1)
                }
                .decodeList<PartnerIdResult>()

            val partnerId = result.firstOrNull()?.partnerId
            Log.d(TAG, "Found partner: $partnerId")
            Result.success(partnerId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user referral partner: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user has already used a referral code
     */
    suspend fun hasUserUsedReferralCode(userId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Checking if user has used referral code: $userId")

            val result = supabase.postgrest["referral_code_entries"]
                .select(Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                    }
                    limit(1)
                }
                .decodeList<IdResult>()

            val hasUsed = result.isNotEmpty()
            Log.d(TAG, "User has used referral code: $hasUsed")
            Result.success(hasUsed)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking referral code usage: ${e.message}", e)
            Result.failure(e)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════

@Serializable
data class PartnerInfo(
    val id: String,
    val name: String,
    @SerialName("referral_code")
    val referralCode: String,
    @SerialName("partner_type")
    val partnerType: String,
    val status: String
)

@Serializable
data class ReferralCodeEntryInsert(
    @SerialName("user_id")
    val userId: String,
    @SerialName("referral_code")
    val referralCode: String,
    @SerialName("partner_id")
    val partnerId: String,
    val source: String
)

@Serializable
data class PartnerReferralInsert(
    @SerialName("partner_id")
    val partnerId: String,
    @SerialName("referred_user_id")
    val referredUserId: String,
    @SerialName("subscription_type")
    val subscriptionType: String,
    @SerialName("subscription_amount_usd")
    val subscriptionAmountUsd: Double,
    val status: String
)

@Serializable
data class PartnerIdResult(
    @SerialName("partner_id")
    val partnerId: String
)

@Serializable
data class IdResult(
    val id: String
)