package com.example.menotracker.community.data

import android.util.Log
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.data.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Repository for member discovery operations.
 * Handles searching members and getting suggestions.
 */
object MemberDiscoveryRepository {
    private const val TAG = "MemberDiscoveryRepo"
    private const val DEFAULT_PAGE_SIZE = 20

    // ═══════════════════════════════════════════════════════════════════════
    // SEARCH MEMBERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Search members with filters
     * Uses RPC function for optimized query with following status
     * Note: RPC function name is legacy "search_athletes" for backend compatibility
     */
    suspend fun searchMembers(
        currentUserId: String,
        filters: MemberSearchFilters,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<MemberSearchResult>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_current_user_id", JsonPrimitive(currentUserId))
                    put("p_search_query",
                        if (filters.searchQuery.isBlank()) JsonNull
                        else JsonPrimitive(filters.searchQuery))
                    put("p_sport_filter",
                        if (filters.sportFilter.isNullOrBlank()) JsonNull
                        else JsonPrimitive(filters.sportFilter))
                    put("p_experience_level",
                        if (filters.experienceLevel == null ||
                            filters.experienceLevel == ExperienceLevelFilter.ALL) JsonNull
                        else JsonPrimitive(filters.experienceLevel.value))
                    put("p_gender",
                        if (filters.genderFilter == null ||
                            filters.genderFilter == GenderFilter.ALL) JsonNull
                        else JsonPrimitive(filters.genderFilter.value))
                    put("p_limit", JsonPrimitive(limit))
                    put("p_offset", JsonPrimitive(offset))
                }

                val results = SupabaseClient.client.postgrest
                    .rpc("search_athletes", params) // Legacy RPC name for backend compatibility
                    .decodeList<MemberSearchResult>()

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Search returned ${results.size} members")
                }
                Result.success(results)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching members: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SUGGESTED MEMBERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get suggested members for HomeScreen
     * Returns members with similar interests who user doesn't follow
     * Note: RPC function name is legacy "get_suggested_athletes" for backend compatibility
     */
    suspend fun getSuggestedMembers(
        currentUserId: String,
        limit: Int = 5
    ): Result<List<SuggestedMember>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_current_user_id", JsonPrimitive(currentUserId))
                    put("p_limit", JsonPrimitive(limit))
                }

                val suggestions = SupabaseClient.client.postgrest
                    .rpc("get_suggested_athletes", params) // Legacy RPC name for backend compatibility
                    .decodeList<SuggestedMember>()

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${suggestions.size} suggested members")
                }
                Result.success(suggestions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading suggestions: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FOLLOW ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Quick follow from search/suggestions
     * Delegates to CommunityRepository
     */
    suspend fun quickFollow(currentUserId: String, targetUserId: String): Result<Unit> {
        return CommunityRepository.followUser(currentUserId, targetUserId)
    }

    /**
     * Quick unfollow
     * Delegates to CommunityRepository
     */
    suspend fun quickUnfollow(currentUserId: String, targetUserId: String): Result<Unit> {
        return CommunityRepository.unfollowUser(currentUserId, targetUserId)
    }
}
