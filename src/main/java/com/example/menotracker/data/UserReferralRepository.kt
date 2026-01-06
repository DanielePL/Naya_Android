package com.example.menotracker.data

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Repository for User-to-User Referral System (Viral Video Sharing)
 *
 * Different from ReferralRepository (Partner referrals)!
 *
 * Features:
 * - Each user gets unique 6-char referral code
 * - Video watermark with referral link
 * - 10% discount for referred users
 * - +1 free month for referrer when referred user subscribes yearly
 */
object UserReferralRepository {
    private const val TAG = "UserReferralRepo"
    private const val BASE_REFERRAL_URL = "https://naya.app/r/"

    private val supabase = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════

    @Serializable
    data class UserReferralCode(
        @SerialName("user_id") val userId: String,
        @SerialName("code") val code: String,
        @SerialName("total_clicks") val totalClicks: Int = 0,
        @SerialName("total_signups") val totalSignups: Int = 0,
        @SerialName("total_subscriptions") val totalSubscriptions: Int = 0,
        @SerialName("total_rewards_earned") val totalRewardsEarned: Int = 0
    )

    @Serializable
    data class ReferralStats(
        @SerialName("referral_code") val referralCode: String,
        @SerialName("total_clicks") val totalClicks: Int,
        @SerialName("total_signups") val totalSignups: Int,
        @SerialName("total_subscriptions") val totalSubscriptions: Int,
        @SerialName("total_free_months_earned") val totalFreeMonthsEarned: Int,
        @SerialName("pending_rewards") val pendingRewards: Int,
        @SerialName("referral_link") val referralLink: String
    )

    @Serializable
    data class UserReferral(
        @SerialName("id") val id: String,
        @SerialName("referrer_id") val referrerId: String,
        @SerialName("referred_id") val referredId: String? = null,
        @SerialName("code") val code: String,
        @SerialName("status") val status: String,
        @SerialName("subscription_type") val subscriptionType: String? = null,
        @SerialName("clicked_at") val clickedAt: String? = null,
        @SerialName("signed_up_at") val signedUpAt: String? = null,
        @SerialName("subscribed_at") val subscribedAt: String? = null
    )

    @Serializable
    data class ReferralReward(
        @SerialName("id") val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("referral_id") val referralId: String,
        @SerialName("reward_type") val rewardType: String,
        @SerialName("months_granted") val monthsGranted: Int,
        @SerialName("status") val status: String,
        @SerialName("granted_at") val grantedAt: String? = null
    )

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API - Getting Referral Code
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get or create a unique referral code for the current user
     */
    suspend fun getOrCreateReferralCode(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not logged in"))

            Log.d(TAG, "Getting/creating referral code for user: $userId")

            // Call Supabase RPC function
            val code = supabase.postgrest.rpc(
                function = "get_or_create_referral_code",
                parameters = buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                }
            ).decodeAs<String>()

            Log.d(TAG, "✅ Referral code: $code")
            Result.success(code)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting referral code: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get the full referral link for sharing
     */
    suspend fun getReferralLink(): Result<String> = withContext(Dispatchers.IO) {
        getOrCreateReferralCode().map { code ->
            "$BASE_REFERRAL_URL$code"
        }
    }

    /**
     * Get referral code directly (without creating)
     */
    suspend fun getCurrentReferralCode(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not logged in"))

            val result = supabase.from("referral_codes")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserReferralCode>()

            Result.success(result?.code)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting current code: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API - Stats & Rewards
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get referral stats for the current user
     */
    suspend fun getReferralStats(): Result<ReferralStats> = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not logged in"))

            Log.d(TAG, "Getting referral stats for user: $userId")

            val stats = supabase.postgrest.rpc(
                function = "get_referral_stats",
                parameters = buildJsonObject {
                    put("p_user_id", JsonPrimitive(userId))
                }
            ).decodeSingleOrNull<ReferralStats>()

            if (stats != null) {
                Log.d(TAG, "✅ Stats: ${stats.totalClicks} clicks, ${stats.totalSignups} signups")
                Result.success(stats)
            } else {
                // No stats yet, ensure code exists
                getOrCreateReferralCode()
                // Return empty stats
                Result.success(ReferralStats(
                    referralCode = "",
                    totalClicks = 0,
                    totalSignups = 0,
                    totalSubscriptions = 0,
                    totalFreeMonthsEarned = 0,
                    pendingRewards = 0,
                    referralLink = ""
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting stats: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get list of people referred by current user
     */
    suspend fun getMyReferrals(): Result<List<UserReferral>> = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not logged in"))

            val referrals = supabase.from("referrals")
                .select {
                    filter {
                        eq("referrer_id", userId)
                    }
                }
                .decodeList<UserReferral>()

            Log.d(TAG, "✅ Found ${referrals.size} referrals")
            Result.success(referrals)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting referrals: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get pending rewards (free months to claim)
     */
    suspend fun getPendingRewards(): Result<List<ReferralReward>> = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not logged in"))

            val rewards = supabase.from("referral_rewards")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("status", "granted")
                    }
                }
                .decodeList<ReferralReward>()

            Log.d(TAG, "✅ Found ${rewards.size} pending rewards")
            Result.success(rewards)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting rewards: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get total free months earned from referrals
     */
    suspend fun getTotalFreeMonthsEarned(): Result<Int> = withContext(Dispatchers.IO) {
        getReferralStats().map { it.totalFreeMonthsEarned }
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API - For New Users (Onboarding)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validate a referral code (for new users entering a code)
     */
    suspend fun validateCode(code: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanCode = code.uppercase().trim()
            Log.d(TAG, "Validating referral code: $cleanCode")

            val result = supabase.from("referral_codes")
                .select {
                    filter {
                        eq("code", cleanCode)
                    }
                }
                .decodeSingleOrNull<UserReferralCode>()

            val isValid = result != null
            Log.d(TAG, "Code valid: $isValid")
            Result.success(isValid)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating code: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Register current user as referred by a code
     * Call this during onboarding when user enters/clicks a referral code
     */
    suspend fun registerAsReferred(
        referralCode: String,
        source: String = "video_share",
        platform: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not logged in"))

            val cleanCode = referralCode.uppercase().trim()
            Log.d(TAG, "Registering user $userId as referred by: $cleanCode")

            val referralId = supabase.postgrest.rpc(
                function = "register_referred_signup",
                parameters = buildJsonObject {
                    put("p_code", JsonPrimitive(cleanCode))
                    put("p_referred_user_id", JsonPrimitive(userId))
                    put("p_source", JsonPrimitive(source))
                    put("p_platform", JsonPrimitive(platform ?: ""))
                    put("p_device_type", JsonPrimitive("android"))
                }
            ).decodeAs<String>()

            Log.d(TAG, "✅ Registered as referred: $referralId")
            Result.success(referralId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registering referral: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get the discount percentage for a referral code
     * Returns 10 for valid codes, 0 for invalid
     */
    suspend fun getDiscountPercent(code: String): Result<Int> = withContext(Dispatchers.IO) {
        validateCode(code).map { isValid ->
            if (isValid) 10 else 0
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // WATERMARK & SHARE HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get watermark text for video overlay
     */
    suspend fun getWatermarkText(): String {
        return getOrCreateReferralCode().fold(
            onSuccess = { code ->
                "NAYA • 10% OFF → naya.app/r/$code"
            },
            onFailure = {
                "NAYA • naya.app"
            }
        )
    }

    /**
     * Get short watermark (for smaller displays)
     */
    suspend fun getShortWatermark(): String {
        return getOrCreateReferralCode().fold(
            onSuccess = { code -> "naya.app/r/$code" },
            onFailure = { "naya.app" }
        )
    }

    /**
     * Generate share text for social media
     */
    suspend fun getShareText(
        exerciseName: String? = null,
        weight: Float? = null,
        velocity: Float? = null
    ): String {
        val link = getReferralLink().getOrNull() ?: "https://naya.app"

        val statsText = buildString {
            if (exerciseName != null) append("$exerciseName ")
            if (weight != null) append("${weight.toInt()}kg ")
            if (velocity != null) append("@ ${String.format("%.2f", velocity)} m/s")
        }.trim()

        val intro = if (statsText.isNotEmpty()) {
            "$statsText 💪\n\n"
        } else {
            ""
        }

        return "${intro}Tracking my lifts with AI velocity tracking.\n\n" +
               "🔥 Get 10% OFF:\n$link\n\n" +
               "#Naya #VBT #PowerBuilding #GymLife"
    }

    /**
     * Generate Instagram/TikTok optimized caption
     */
    suspend fun getSocialCaption(exerciseName: String? = null): String {
        val code = getOrCreateReferralCode().getOrNull() ?: ""
        val exercise = exerciseName ?: "workout"

        return "Tracking my $exercise with @naya_app 🔥\n\n" +
               "AI velocity tracking for better gains.\n" +
               "Link in bio for 10% OFF 👆\n\n" +
               if (code.isNotEmpty()) "Code: $code" else ""
    }
}