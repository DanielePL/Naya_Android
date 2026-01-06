package com.example.menotracker.community.data

import android.util.Log
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.data.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for community profile and follow operations.
 * Handles user following, community profiles, and user discovery.
 */
object CommunityRepository {
    private const val TAG = "CommunityRepository"

    // Table names
    private const val FOLLOWS_TABLE = "community_follows"
    private const val PROFILES_TABLE = "community_profiles"

    // ═══════════════════════════════════════════════════════════════════════
    // FOLLOW OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Follow a user
     */
    suspend fun followUser(followerId: String, targetUserId: String): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Following user: $targetUserId")
                }

                val follow = mapOf(
                    "follower_id" to followerId,
                    "following_id" to targetUserId,
                    "status" to "active"
                )

                SupabaseClient.client
                    .from(FOLLOWS_TABLE)
                    .insert(follow)

                Log.d(TAG, "✅ Successfully followed user $targetUserId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error following user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unfollow a user
     */
    suspend fun unfollowUser(followerId: String, targetUserId: String): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Unfollowing user: $targetUserId")
                }

                SupabaseClient.client
                    .from(FOLLOWS_TABLE)
                    .delete {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", targetUserId)
                        }
                    }

                Log.d(TAG, "✅ Successfully unfollowed user $targetUserId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unfollowing user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get followers for a user
     */
    suspend fun getFollowers(userId: String): Result<List<Follow>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val follows = SupabaseClient.client
                    .from(FOLLOWS_TABLE)
                    .select {
                        filter {
                            eq("following_id", userId)
                            eq("status", "active")
                        }
                    }
                    .decodeList<Follow>()

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${follows.size} followers for user $userId")
                }
                Result.success(follows)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading followers: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get users that a user is following
     */
    suspend fun getFollowing(userId: String): Result<List<Follow>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val follows = SupabaseClient.client
                    .from(FOLLOWS_TABLE)
                    .select {
                        filter {
                            eq("follower_id", userId)
                            eq("status", "active")
                        }
                    }
                    .decodeList<Follow>()

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${follows.size} following for user $userId")
                }
                Result.success(follows)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading following: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get follower count for a user
     */
    suspend fun getFollowerCount(userId: String): Result<Int> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(0)
        }

        return try {
            withContext(Dispatchers.IO) {
                val follows = SupabaseClient.client
                    .from(FOLLOWS_TABLE)
                    .select {
                        filter {
                            eq("following_id", userId)
                            eq("status", "active")
                        }
                    }
                    .decodeList<Follow>()

                Result.success(follows.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting follower count: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get following count for a user
     */
    suspend fun getFollowingCount(userId: String): Result<Int> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(0)
        }

        return try {
            withContext(Dispatchers.IO) {
                val follows = SupabaseClient.client
                    .from(FOLLOWS_TABLE)
                    .select {
                        filter {
                            eq("follower_id", userId)
                            eq("status", "active")
                        }
                    }
                    .decodeList<Follow>()

                Result.success(follows.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting following count: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user A follows user B
     */
    suspend fun isFollowing(followerId: String, targetUserId: String): Result<Boolean> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(false)
        }

        return try {
            withContext(Dispatchers.IO) {
                val follows = SupabaseClient.client
                    .from(FOLLOWS_TABLE)
                    .select {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", targetUserId)
                            eq("status", "active")
                        }
                    }
                    .decodeList<Follow>()

                Result.success(follows.isNotEmpty())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking follow status: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get community stats for a user
     */
    suspend fun getUserCommunityStats(
        currentUserId: String,
        targetUserId: String
    ): Result<UserCommunityStats> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(UserCommunityStats())
        }

        return try {
            withContext(Dispatchers.IO) {
                // Get follower count
                val followerCount = getFollowerCount(targetUserId).getOrDefault(0)

                // Get following count
                val followingCount = getFollowingCount(targetUserId).getOrDefault(0)

                // Check if current user follows target
                val isFollowing = isFollowing(currentUserId, targetUserId).getOrDefault(false)

                // Check if target follows current user
                val isFollowedBy = isFollowing(targetUserId, currentUserId).getOrDefault(false)

                // Get posts count (from FeedRepository)
                val postsCount = FeedRepository.getPostsCountForUser(targetUserId).getOrDefault(0)

                Result.success(
                    UserCommunityStats(
                        followersCount = followerCount,
                        followingCount = followingCount,
                        postsCount = postsCount,
                        isFollowing = isFollowing,
                        isFollowedBy = isFollowedBy
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting user community stats: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COMMUNITY PROFILE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get community profile for a user
     */
    suspend fun getCommunityProfile(userId: String): Result<CommunityProfile?> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val profiles = SupabaseClient.client
                    .from(PROFILES_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<CommunityProfile>()

                Result.success(profiles.firstOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting community profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get or create community profile
     */
    suspend fun getOrCreateCommunityProfile(userId: String): Result<CommunityProfile> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val existingProfile = getCommunityProfile(userId).getOrNull()

                if (existingProfile != null) {
                    Result.success(existingProfile)
                } else {
                    // Create default profile
                    val newProfile = CommunityProfile(userId = userId)

                    SupabaseClient.client
                        .from(PROFILES_TABLE)
                        .insert(newProfile)

                    Log.d(TAG, "✅ Created community profile for user $userId")
                    Result.success(newProfile)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting/creating community profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update community profile settings
     */
    suspend fun updateCommunityProfile(profile: CommunityProfile): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from(PROFILES_TABLE)
                    .upsert(profile)

                Log.d(TAG, "✅ Updated community profile for ${profile.userId}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating community profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update profile visibility
     */
    suspend fun setProfilePublic(userId: String, isPublic: Boolean): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from(PROFILES_TABLE)
                    .update(mapOf("is_public" to isPublic)) {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                Log.d(TAG, "✅ Set profile public=$isPublic for user $userId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating profile visibility: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update leaderboard opt-in
     */
    suspend fun setLeaderboardOptIn(userId: String, optIn: Boolean): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from(PROFILES_TABLE)
                    .update(mapOf("show_in_leaderboard" to optIn)) {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                Log.d(TAG, "✅ Set leaderboard opt-in=$optIn for user $userId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating leaderboard opt-in: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload media file (stub - not yet implemented)
     */
    suspend fun uploadMedia(uri: android.net.Uri, context: android.content.Context): String? {
        Log.w(TAG, "⚠️ uploadMedia not yet implemented")
        return null
    }

    /**
     * Create community post (stub - not yet implemented)
     */
    suspend fun createCommunityPost(
        postId: String,
        userId: String,
        content: String,
        postType: String,
        imageUrls: List<String>,
        videoUrls: List<String>
    ): Boolean {
        Log.w(TAG, "⚠️ createCommunityPost not yet implemented")
        return false
    }
}