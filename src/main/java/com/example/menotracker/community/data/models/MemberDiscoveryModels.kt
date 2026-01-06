package com.example.menotracker.community.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// MEMBER SEARCH RESULT
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class MemberSearchResult(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("primary_sport") val primarySport: String? = null,
    @SerialName("secondary_sport") val secondarySport: String? = null,
    @SerialName("tertiary_sport") val tertiarySport: String? = null,
    @SerialName("experience_level") val experienceLevel: String? = null,
    val gender: String? = null,
    @SerialName("training_experience") val trainingExperience: Int? = null,
    @SerialName("is_following") val isFollowing: Boolean = false,
    @SerialName("followers_count") val followersCount: Long = 0,
    @SerialName("common_sports_count") val commonSportsCount: Int = 0
) {
    val displayName: String get() = name
    val avatarUrl: String? get() = profileImageUrl
}

// ═══════════════════════════════════════════════════════════════════════════
// SUGGESTED MEMBER
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class SuggestedMember(
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("primary_sport") val primarySport: String? = null,
    @SerialName("experience_level") val experienceLevel: String? = null,
    @SerialName("is_following") val isFollowing: Boolean = false,
    @SerialName("followers_count") val followersCount: Long = 0,
    @SerialName("common_sports_count") val commonSportsCount: Int = 0,
    @SerialName("suggestion_reason") val suggestionReason: String? = null
) {
    val displayName: String get() = name
    val avatarUrl: String? get() = profileImageUrl
}

// ═══════════════════════════════════════════════════════════════════════════
// SEARCH FILTERS
// ═══════════════════════════════════════════════════════════════════════════

data class MemberSearchFilters(
    val searchQuery: String = "",
    val sportFilter: String? = null,
    val experienceLevel: ExperienceLevelFilter? = null,
    val genderFilter: GenderFilter? = null
) {
    fun hasActiveFilters(): Boolean {
        return sportFilter != null ||
                (experienceLevel != null && experienceLevel != ExperienceLevelFilter.ALL) ||
                (genderFilter != null && genderFilter != GenderFilter.ALL)
    }
}

enum class ExperienceLevelFilter(val displayName: String, val value: String) {
    ALL("All Levels", ""),
    BEGINNER("Beginner", "BEGINNER"),
    INTERMEDIATE("Intermediate", "INTERMEDIATE"),
    EXPERIENCED("Experienced", "EXPERIENCED"),
    ELITE("Elite", "ELITE")
}

enum class GenderFilter(val displayName: String, val value: String) {
    ALL("All", ""),
    MALE("Male", "male"),
    FEMALE("Female", "female")
}

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════════════

data class MemberDiscoveryState(
    val searchResults: List<MemberSearchResult> = emptyList(),
    val suggestedMembers: List<SuggestedMember> = emptyList(),
    val filters: MemberSearchFilters = MemberSearchFilters(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)
