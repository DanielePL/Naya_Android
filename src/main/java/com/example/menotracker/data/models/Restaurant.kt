// app/src/main/java/com/example/myapplicationtest/data/models/Restaurant.kt

package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════════
// RESTAURANT DATA MODELS
// ═══════════════════════════════════════════════════════════════════════════════
//
// Models for the Naya Food Universe:
// • Restaurant chains (McDonald's, Starbucks, etc.)
// • Individual restaurant locations
// • Menu items with regional nutrition variants
// • User history and "your usual" tracking
// • Crowdsourced submissions
//
// ═══════════════════════════════════════════════════════════════════════════════


// ═══════════════════════════════════════════════════════════════════════════════
// RESTAURANT CHAIN
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class RestaurantChain(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,

    // Branding
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("primary_color") val primaryColor: String? = null,

    // Metadata
    @SerialName("cuisine_type") val cuisineType: String? = null,
    @SerialName("headquarters_country") val headquartersCountry: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,

    // Verification
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("nutrition_source") val nutritionSource: String? = null,
    @SerialName("last_verified_at") val lastVerifiedAt: String? = null,

    // Stats
    @SerialName("total_locations") val totalLocations: Int = 0,
    @SerialName("total_meals") val totalMeals: Int = 0,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    // Helper to get cuisine icon
    val cuisineIcon: String
        get() = when (cuisineType?.lowercase()) {
            "fast food" -> "🍔"
            "coffee" -> "☕"
            "pizza" -> "🍕"
            "mexican" -> "🌮"
            "chinese" -> "🥡"
            "sandwiches" -> "🥪"
            "chicken" -> "🍗"
            "burgers" -> "🍔"
            "bakery" -> "🥐"
            else -> "🍽️"
        }
}


// ═══════════════════════════════════════════════════════════════════════════════
// RESTAURANT (Individual Location)
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class Restaurant(
    val id: String,

    // Chain relationship
    @SerialName("chain_id") val chainId: String? = null,

    // Basic info
    val name: String,

    // Location
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Address
    @SerialName("address_line1") val addressLine1: String? = null,
    @SerialName("address_line2") val addressLine2: String? = null,
    val city: String? = null,
    @SerialName("state_province") val stateProvince: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val country: String,
    @SerialName("country_name") val countryName: String? = null,

    // External IDs
    @SerialName("google_place_id") val googlePlaceId: String? = null,
    @SerialName("foursquare_id") val foursquareId: String? = null,
    @SerialName("yelp_id") val yelpId: String? = null,

    // Verification
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_source") val verificationSource: String? = null,
    @SerialName("submitted_by_user_id") val submittedByUserId: String? = null,

    // Stats
    @SerialName("total_meals") val totalMeals: Int = 0,
    @SerialName("total_submissions") val totalSubmissions: Int = 0,
    @SerialName("avg_rating") val avgRating: Float? = null,

    // Status
    @SerialName("is_active") val isActive: Boolean = true,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,

    // Nested chain data (when joined)
    val chain: RestaurantChain? = null
) {
    // Full address string
    val fullAddress: String
        get() = buildString {
            addressLine1?.let { append(it) }
            city?.let {
                if (isNotEmpty()) append(", ")
                append(it)
            }
            stateProvince?.let {
                if (isNotEmpty()) append(", ")
                append(it)
            }
            postalCode?.let {
                if (isNotEmpty()) append(" ")
                append(it)
            }
        }

    // Display name (chain name if available, otherwise restaurant name)
    val displayName: String
        get() = chain?.name ?: name

    // Logo URL (from chain if available)
    val logoUrl: String?
        get() = chain?.logoUrl
}


// ═══════════════════════════════════════════════════════════════════════════════
// NEARBY RESTAURANT (Result from geo-query)
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class NearbyRestaurant(
    val id: String,
    val name: String,

    // Chain info (from join)
    @SerialName("chain_name") val chainName: String? = null,
    @SerialName("chain_logo") val chainLogo: String? = null,
    @SerialName("chain_cuisine_type") val chainCuisineType: String? = null,

    // Distance in meters from user
    @SerialName("distance_meters") val distanceMeters: Double,

    // Stats
    @SerialName("total_meals") val totalMeals: Int = 0,
    @SerialName("is_verified") val isVerified: Boolean = false,

    // Location
    val latitude: Double,
    val longitude: Double
) {
    // Display distance (e.g., "120m" or "1.2km")
    val displayDistance: String
        get() = when {
            distanceMeters < 1000 -> "${distanceMeters.toInt()}m"
            else -> "${"%.1f".format(distanceMeters / 1000)}km"
        }

    // Display name (chain name if available)
    val displayName: String
        get() = chainName ?: name
}


// ═══════════════════════════════════════════════════════════════════════════════
// RESTAURANT MEAL (Menu Item)
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class RestaurantMeal(
    val id: String,

    // Relationships
    @SerialName("chain_id") val chainId: String? = null,
    @SerialName("restaurant_id") val restaurantId: String? = null,

    // Basic info
    val name: String,
    val description: String? = null,
    val category: String? = null,

    // Serving info
    @SerialName("serving_size") val servingSize: Float? = null,
    @SerialName("serving_unit") val servingUnit: String = "g",
    @SerialName("serving_description") val servingDescription: String? = null,

    // ═══════════════════════════════════════════════════════════════
    // CORE MACROS
    // ═══════════════════════════════════════════════════════════════
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,

    // ═══════════════════════════════════════════════════════════════
    // EXTENDED NUTRIENTS
    // ═══════════════════════════════════════════════════════════════
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    @SerialName("saturated_fat") val saturatedFat: Float = 0f,
    @SerialName("trans_fat") val transFat: Float = 0f,
    val cholesterol: Float = 0f,
    val sodium: Float = 0f,
    val potassium: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    @SerialName("vitamin_a") val vitaminA: Float = 0f,
    @SerialName("vitamin_c") val vitaminC: Float = 0f,
    @SerialName("vitamin_d") val vitaminD: Float = 0f,

    // Regional variant
    val region: String = "GLOBAL",
    @SerialName("region_notes") val regionNotes: String? = null,

    // Media
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,

    // Verification
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_count") val verificationCount: Int = 0,
    @SerialName("data_source") val dataSource: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,

    // AI fields
    @SerialName("ai_confidence") val aiConfidence: Float? = null,

    // Popularity
    @SerialName("popularity_score") val popularityScore: Int = 0,
    @SerialName("is_popular") val isPopular: Boolean = false,
    val tags: List<String>? = null,

    // Customization
    @SerialName("is_customizable") val isCustomizable: Boolean = false,
    @SerialName("base_item_id") val baseItemId: String? = null,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,

    // Nested chain data (when joined)
    val chain: RestaurantChainMinimal? = null
) {
    // Quick macro display
    val macroSummary: String
        get() = "${calories.toInt()} kcal | ${protein.toInt()}g P | ${carbs.toInt()}g C | ${fat.toInt()}g F"

    // Serving display
    val servingDisplay: String
        get() = servingDescription ?: "${servingSize?.toInt() ?: "1"} $servingUnit"

    // Verification badge text
    val verificationBadge: String?
        get() = when {
            isVerified && dataSource == "official" -> "✓ Official"
            isVerified -> "✓ Verified"
            verificationCount >= 3 -> "👥 Community"
            else -> null
        }

    // Region flag emoji
    val regionFlag: String
        get() = when (region) {
            "US" -> "🇺🇸"
            "EU" -> "🇪🇺"
            "ASIA" -> "🌏"
            else -> "🌍"
        }
}

// Minimal chain data for nested queries
@Serializable
data class RestaurantChainMinimal(
    val id: String,
    val name: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("cuisine_type") val cuisineType: String? = null
)


// ═══════════════════════════════════════════════════════════════════════════════
// USER RESTAURANT HISTORY
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class UserRestaurantHistory(
    val id: String,

    @SerialName("user_id") val userId: String,
    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("meal_id") val mealId: String? = null,
    @SerialName("nutrition_log_id") val nutritionLogId: String? = null,

    @SerialName("times_visited") val timesVisited: Int = 1,
    @SerialName("last_visited_at") val lastVisitedAt: String? = null,

    @SerialName("favorite_meal_ids") val favoriteMealIds: List<String>? = null,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,

    // Nested data (when joined)
    val restaurant: Restaurant? = null,
    val meal: RestaurantMeal? = null
) {
    // Is this a "usual" (ordered 3+ times)
    val isUsual: Boolean
        get() = timesVisited >= 3
}


// ═══════════════════════════════════════════════════════════════════════════════
// MEAL SUBMISSION (Crowdsourced)
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class MealSubmission(
    val id: String,

    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("meal_id") val mealId: String? = null,
    @SerialName("user_id") val userId: String,

    @SerialName("meal_name") val mealName: String,

    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,

    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("ai_analysis_json") val aiAnalysisJson: String? = null,
    val notes: String? = null,

    @SerialName("submission_latitude") val submissionLatitude: Double? = null,
    @SerialName("submission_longitude") val submissionLongitude: Double? = null,
    @SerialName("distance_from_restaurant") val distanceFromRestaurant: Float? = null,

    val status: SubmissionStatus = SubmissionStatus.PENDING,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,

    val upvotes: Int = 0,
    val downvotes: Int = 0,
    @SerialName("trust_score") val trustScore: Float? = null,

    @SerialName("created_at") val createdAt: String? = null,

    // Nested data
    val restaurant: Restaurant? = null
) {
    // Net votes
    val netVotes: Int
        get() = upvotes - downvotes

    // Status display
    val statusDisplay: String
        get() = when (status) {
            SubmissionStatus.PENDING -> "⏳ Pending Review"
            SubmissionStatus.APPROVED -> "✅ Approved"
            SubmissionStatus.REJECTED -> "❌ Rejected"
            SubmissionStatus.MERGED -> "🔗 Merged"
        }

    // Was user near the restaurant?
    val wasNearRestaurant: Boolean
        get() = distanceFromRestaurant?.let { it < 200 } ?: false
}

@Serializable
enum class SubmissionStatus {
    @SerialName("pending") PENDING,
    @SerialName("approved") APPROVED,
    @SerialName("rejected") REJECTED,
    @SerialName("merged") MERGED
}


// ═══════════════════════════════════════════════════════════════════════════════
// USER CONTRIBUTION STATS (Gamification)
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class UserFoodContributionStats(
    @SerialName("user_id") val userId: String,

    @SerialName("total_submissions") val totalSubmissions: Int = 0,
    @SerialName("approved_submissions") val approvedSubmissions: Int = 0,
    @SerialName("rejected_submissions") val rejectedSubmissions: Int = 0,

    @SerialName("total_verifications") val totalVerifications: Int = 0,

    @SerialName("trust_score") val trustScore: Float = 0.5f,
    @SerialName("contribution_level") val contributionLevel: ContributionLevel = ContributionLevel.NEWCOMER,

    val badges: List<String>? = null,

    @SerialName("countries_contributed") val countriesContributed: List<String>? = null,
    @SerialName("cities_contributed") val citiesContributed: List<String>? = null,

    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("last_contribution_at") val lastContributionAt: String? = null,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    // Approval rate
    val approvalRate: Float
        get() = if (totalSubmissions > 0) {
            approvedSubmissions.toFloat() / totalSubmissions.toFloat()
        } else 0f

    // Level display with icon
    val levelDisplay: String
        get() = when (contributionLevel) {
            ContributionLevel.NEWCOMER -> "🌱 Newcomer"
            ContributionLevel.CONTRIBUTOR -> "⭐ Contributor"
            ContributionLevel.TRUSTED -> "🏅 Trusted"
            ContributionLevel.EXPERT -> "👑 Expert"
        }

    // Countries count
    val countriesCount: Int
        get() = countriesContributed?.size ?: 0
}

@Serializable
enum class ContributionLevel {
    @SerialName("newcomer") NEWCOMER,
    @SerialName("contributor") CONTRIBUTOR,
    @SerialName("trusted") TRUSTED,
    @SerialName("expert") EXPERT
}


// ═══════════════════════════════════════════════════════════════════════════════
// BADGE DEFINITIONS
// ═══════════════════════════════════════════════════════════════════════════════

enum class FoodScoutBadge(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: String
) {
    FIRST_SCAN("first_scan", "First Scan", "Submitted your first meal", "📸"),
    VERIFIER("verifier", "Verifier", "Verified 10 meals", "🔍"),
    GLOBE_TROTTER("globe_trotter", "Globe Trotter", "Logged meals in 5 countries", "🌍"),
    LOCAL_EXPERT("local_expert", "Local Expert", "50 meals in one city", "🏆"),
    CHAIN_MASTER("chain_master", "Chain Master", "Logged at 20 different chains", "🔗"),
    ACCURACY_KING("accuracy_king", "Accuracy King", "95%+ approval rate (min 20)", "🎯"),
    STREAK_WARRIOR("streak_warrior", "Streak Warrior", "30-day contribution streak", "🔥"),
    EARLY_ADOPTER("early_adopter", "Early Adopter", "Joined in 2025", "🚀");

    companion object {
        fun fromId(id: String): FoodScoutBadge? = entries.find { it.id == id }
    }
}