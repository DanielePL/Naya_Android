package com.example.menotracker.community.data

import android.util.Log
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import com.example.menotracker.data.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Repository for community feed operations.
 * Handles posts, likes, comments, and feed retrieval.
 */
object FeedRepository {
    private const val TAG = "FeedRepository"

    // Table names
    private const val POSTS_TABLE = "community_posts"
    private const val LIKES_TABLE = "community_likes"
    private const val COMMENTS_TABLE = "community_comments"

    // Pagination
    private const val DEFAULT_PAGE_SIZE = 20

    // ═══════════════════════════════════════════════════════════════════════
    // FEED RETRIEVAL
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get feed for current user (posts from followed users + own posts)
     * Uses RPC function for optimized query
     */
    suspend fun getFeed(
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<FeedPost>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.FEED_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_limit", JsonPrimitive(limit))
                    put("p_offset", JsonPrimitive(offset))
                }

                val posts = SupabaseClient.client.postgrest
                    .rpc("get_community_feed", params)
                    .decodeList<FeedPost>()

                // Debug: Log video_urls for each post
                posts.forEach { post ->
                    if (!post.videoUrls.isNullOrEmpty()) {
                        Log.d(TAG, "📹 Post ${post.id} has ${post.videoUrls.size} videos: ${post.videoUrls}")
                    }
                }
                Log.d(TAG, "📊 Feed loaded: ${posts.size} posts, ${posts.count { !it.videoUrls.isNullOrEmpty() }} with videos")

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${posts.size} posts for feed")
                }
                Result.success(posts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading feed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get discover feed (public posts from non-followed users)
     * Uses RPC function for optimized query
     */
    suspend fun getDiscoverFeed(
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<FeedPost>> {
        if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.DISCOVER_ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val params = buildJsonObject {
                    put("p_limit", JsonPrimitive(limit))
                    put("p_offset", JsonPrimitive(offset))
                }

                val posts = SupabaseClient.client.postgrest
                    .rpc("get_discover_feed", params)
                    .decodeList<FeedPost>()

                // Debug: Log video_urls for each post
                posts.forEach { post ->
                    if (!post.videoUrls.isNullOrEmpty()) {
                        Log.d(TAG, "📹 Discover Post ${post.id} has ${post.videoUrls.size} videos: ${post.videoUrls}")
                    }
                }
                Log.d(TAG, "📊 Discover feed loaded: ${posts.size} posts, ${posts.count { !it.videoUrls.isNullOrEmpty() }} with videos")

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${posts.size} posts for discover")
                }
                Result.success(posts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading discover feed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get posts for a specific user
     */
    suspend fun getPostsForUser(
        userId: String,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0
    ): Result<List<FeedPost>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val posts = SupabaseClient.client
                    .from(POSTS_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<CommunityPost>()

                // Fetch user profile data
                var userName: String? = null
                var userAvatar: String? = null
                try {
                    val profiles = SupabaseClient.client
                        .from("user_profiles")
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeList<UserProfileBasic>()
                    profiles.firstOrNull()?.let { profile ->
                        userName = profile.name
                        userAvatar = profile.profileImageUrl
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not load profile for user $userId")
                }

                // Convert to FeedPost with user data
                val feedPosts = posts.map { post ->
                    FeedPost(
                        id = post.id,
                        userId = post.userId,
                        userName = userName,
                        userAvatar = userAvatar,
                        workoutName = post.workoutName,
                        totalVolumeKg = post.totalVolumeKg,
                        totalSets = post.totalSets,
                        totalReps = post.totalReps,
                        durationMinutes = post.durationMinutes,
                        prsAchieved = post.prsAchieved,
                        prExercises = post.prExercises,
                        caption = post.caption,
                        imageUrls = post.imageUrls,
                        videoUrls = post.videoUrls,
                        likesCount = post.likesCount,
                        commentsCount = post.commentsCount,
                        createdAt = post.createdAt,
                        isLiked = false  // Would need separate query
                    )
                }

                Result.success(feedPosts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading user posts: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get posts count for a user
     */
    suspend fun getPostsCountForUser(userId: String): Result<Int> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(0)
        }

        return try {
            withContext(Dispatchers.IO) {
                val posts = SupabaseClient.client
                    .from(POSTS_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<CommunityPost>()

                Result.success(posts.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting posts count: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single post by ID
     */
    suspend fun getPost(postId: String): Result<FeedPost?> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(null)
        }

        return try {
            withContext(Dispatchers.IO) {
                val posts = SupabaseClient.client
                    .from(POSTS_TABLE)
                    .select {
                        filter {
                            eq("id", postId)
                        }
                    }
                    .decodeList<CommunityPost>()

                val post = posts.firstOrNull()
                if (post != null) {
                    // Fetch user profile data
                    var userName: String? = null
                    var userAvatar: String? = null
                    try {
                        val profiles = SupabaseClient.client
                            .from("user_profiles")
                            .select {
                                filter {
                                    eq("id", post.userId)
                                }
                            }
                            .decodeList<UserProfileBasic>()
                        profiles.firstOrNull()?.let { profile ->
                            userName = profile.name
                            userAvatar = profile.profileImageUrl
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not load profile for user ${post.userId}")
                    }

                    Result.success(
                        FeedPost(
                            id = post.id,
                            userId = post.userId,
                            userName = userName,
                            userAvatar = userAvatar,
                            workoutName = post.workoutName,
                            totalVolumeKg = post.totalVolumeKg,
                            totalSets = post.totalSets,
                            totalReps = post.totalReps,
                            durationMinutes = post.durationMinutes,
                            prsAchieved = post.prsAchieved,
                            prExercises = post.prExercises,
                            caption = post.caption,
                            imageUrls = post.imageUrls,
                            videoUrls = post.videoUrls,
                            likesCount = post.likesCount,
                            commentsCount = post.commentsCount,
                            createdAt = post.createdAt
                        )
                    )
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading post: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST CREATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Create a new post (share workout)
     */
    suspend fun createPost(
        userId: String,
        workoutHistoryId: String?,
        workoutName: String,
        totalVolumeKg: Double,
        totalSets: Int,
        totalReps: Int,
        durationMinutes: Int?,
        prsAchieved: Int,
        prExercises: List<String>?,
        caption: String?,
        videoUrls: List<String>? = null,
        visibility: PostVisibility = PostVisibility.FOLLOWERS
    ): Result<CommunityPost> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val post = CommunityPost(
                    userId = userId,
                    workoutHistoryId = workoutHistoryId,
                    workoutName = workoutName,
                    totalVolumeKg = totalVolumeKg,
                    totalSets = totalSets,
                    totalReps = totalReps,
                    durationMinutes = durationMinutes,
                    prsAchieved = prsAchieved,
                    prExercises = prExercises,
                    caption = caption,
                    videoUrls = videoUrls,
                    visibility = visibility
                )

                val insertedPosts = SupabaseClient.client
                    .from(POSTS_TABLE)
                    .insert(post) {
                        select()
                    }
                    .decodeList<CommunityPost>()

                val insertedPost = insertedPosts.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to create post"))

                Log.d(TAG, "✅ Created post: ${insertedPost.id} with ${videoUrls?.size ?: 0} videos")
                Result.success(insertedPost)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating post: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Create a community post (general post with content, not workout-based)
     */
    suspend fun createCommunityPost(
        userId: String,
        content: String,
        postType: String,
        imageUrls: List<String>? = null,
        videoUrls: List<String>? = null,
        visibility: PostVisibility = PostVisibility.PUBLIC
    ): Result<CommunityPost> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val post = CommunityPost(
                    userId = userId,
                    workoutHistoryId = null,
                    workoutName = postType, // Use postType as workoutName for display
                    totalVolumeKg = 0.0,
                    totalSets = 0,
                    totalReps = 0,
                    durationMinutes = null,
                    prsAchieved = 0,
                    prExercises = null,
                    caption = content,
                    imageUrls = imageUrls,
                    videoUrls = videoUrls,
                    visibility = visibility
                )

                val insertedPosts = SupabaseClient.client
                    .from(POSTS_TABLE)
                    .insert(post) {
                        select()
                    }
                    .decodeList<CommunityPost>()

                val insertedPost = insertedPosts.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to create post"))

                Log.d(TAG, "✅ Created community post: ${insertedPost.id}")
                Result.success(insertedPost)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating community post: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload media (image) for community post
     * Returns the public URL of the uploaded image
     */
    suspend fun uploadCommunityImage(
        userId: String,
        imageBytes: ByteArray,
        imageIndex: Int
    ): Result<String> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val timestamp = System.currentTimeMillis()
                val uploadPath = "posts/$userId/${timestamp}_img_$imageIndex.jpg"

                // Use community-videos bucket (can hold images too) or create separate bucket
                val bucket = SupabaseClient.client.storage.from(COMMUNITY_VIDEO_BUCKET)
                bucket.upload(uploadPath, imageBytes, upsert = true)

                // Get public URL
                val publicUrl = bucket.publicUrl(uploadPath)

                Log.d(TAG, "✅ Image uploaded: $publicUrl")
                Result.success(publicUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading image: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a post
     */
    suspend fun deletePost(postId: String, userId: String): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from(POSTS_TABLE)
                    .delete {
                        filter {
                            eq("id", postId)
                            eq("user_id", userId)  // Ensure user owns the post
                        }
                    }

                Log.d(TAG, "✅ Deleted post: $postId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting post: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIKES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Like a post
     */
    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val like = CommunityLike(
                    postId = postId,
                    userId = userId
                )

                SupabaseClient.client
                    .from(LIKES_TABLE)
                    .insert(like)

                Log.d(TAG, "✅ Liked post: $postId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // May fail if already liked (unique constraint)
            if (e.message?.contains("duplicate") == true ||
                e.message?.contains("unique") == true) {
                Log.d(TAG, "Post already liked: $postId")
                Result.success(Unit)
            } else {
                Log.e(TAG, "❌ Error liking post: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Unlike a post
     */
    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from(LIKES_TABLE)
                    .delete {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }

                Log.d(TAG, "✅ Unliked post: $postId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unliking post: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user has liked a post
     */
    suspend fun hasLikedPost(postId: String, userId: String): Result<Boolean> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(false)
        }

        return try {
            withContext(Dispatchers.IO) {
                val likes = SupabaseClient.client
                    .from(LIKES_TABLE)
                    .select {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<CommunityLike>()

                Result.success(likes.isNotEmpty())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking like status: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COMMENTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get comments for a post with user info
     */
    suspend fun getComments(postId: String): Result<List<CommunityComment>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                // First get comments
                val comments = SupabaseClient.client
                    .from(COMMENTS_TABLE)
                    .select {
                        filter {
                            eq("post_id", postId)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<CommunityComment>()

                // Then get user info for each unique user
                val userIds = comments.map { it.userId }.distinct()
                val userInfoMap = mutableMapOf<String, Pair<String?, String?>>()

                userIds.forEach { userId ->
                    try {
                        val profiles = SupabaseClient.client
                            .from("user_profiles")
                            .select {
                                filter {
                                    eq("id", userId)
                                }
                            }
                            .decodeList<UserProfileBasic>()
                        profiles.firstOrNull()?.let { profile ->
                            userInfoMap[userId] = Pair(profile.name, profile.profileImageUrl)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not load profile for user $userId")
                    }
                }

                // Enrich comments with user data
                val enrichedComments = comments.map { comment ->
                    val userInfo = userInfoMap[comment.userId]
                    comment.copy(
                        userName = userInfo?.first,
                        userAvatar = userInfo?.second
                    )
                }

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Loaded ${enrichedComments.size} comments for post $postId")
                }
                Result.success(enrichedComments)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading comments: ${e.message}", e)
            Result.failure(e)
        }
    }

    @kotlinx.serialization.Serializable
    private data class UserProfileBasic(
        val id: String = "",
        val name: String? = null,
        @SerialName("profile_image_url") val profileImageUrl: String? = null
    )

    /**
     * Add a comment to a post
     */
    suspend fun addComment(
        postId: String,
        userId: String,
        content: String,
        parentCommentId: String? = null
    ): Result<CommunityComment> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val comment = CommunityComment(
                    postId = postId,
                    userId = userId,
                    content = content,
                    parentCommentId = parentCommentId
                )

                val insertedComments = SupabaseClient.client
                    .from(COMMENTS_TABLE)
                    .insert(comment) {
                        select()
                    }
                    .decodeList<CommunityComment>()

                var insertedComment = insertedComments.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Failed to create comment"))

                // Enrich with user info
                try {
                    val profiles = SupabaseClient.client
                        .from("user_profiles")
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeList<UserProfileBasic>()
                    profiles.firstOrNull()?.let { profile ->
                        insertedComment = insertedComment.copy(
                            userName = profile.name,
                            userAvatar = profile.profileImageUrl
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not load profile for current user")
                }

                Log.d(TAG, "✅ Added comment to post $postId")
                Result.success(insertedComment)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a comment
     */
    suspend fun deleteComment(commentId: String, userId: String): Result<Unit> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from(COMMENTS_TABLE)
                    .delete {
                        filter {
                            eq("id", commentId)
                            eq("user_id", userId)  // Ensure user owns the comment
                        }
                    }

                Log.d(TAG, "✅ Deleted comment: $commentId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VIDEO UPLOAD
    // ═══════════════════════════════════════════════════════════════════════

    private const val COMMUNITY_VIDEO_BUCKET = "community-videos"

    /**
     * Upload a video for community post
     * Returns the public URL of the uploaded video
     */
    suspend fun uploadCommunityVideo(
        userId: String,
        videoPath: String,
        videoIndex: Int
    ): Result<String> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        return try {
            withContext(Dispatchers.IO) {
                val videoBytes: ByteArray
                val fileName: String

                // Check if path is a URL (from cloud storage) or local file
                if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
                    // Download from URL first (video is already in cloud, need to copy to community bucket)
                    Log.d(TAG, "📥 Downloading video from cloud URL for community upload...")
                    val url = java.net.URL(videoPath)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 30000
                    connection.readTimeout = 60000

                    try {
                        videoBytes = connection.inputStream.readBytes()
                        fileName = "cloud_video_$videoIndex.mp4"
                        Log.d(TAG, "📥 Downloaded ${videoBytes.size / 1024}KB from cloud")
                    } finally {
                        connection.disconnect()
                    }
                } else {
                    // Local file path
                    val videoFile = java.io.File(videoPath)
                    if (!videoFile.exists()) {
                        return@withContext Result.failure(Exception("Video file not found: $videoPath"))
                    }
                    videoBytes = videoFile.readBytes()
                    fileName = videoFile.name
                    Log.d(TAG, "📹 Reading local video: $fileName, size: ${videoBytes.size / 1024}KB")
                }

                val timestamp = System.currentTimeMillis()
                val uploadPath = "posts/$userId/${timestamp}_$videoIndex.mp4"

                // Upload to Supabase Storage (community-videos bucket)
                val bucket = SupabaseClient.client.storage.from(COMMUNITY_VIDEO_BUCKET)
                bucket.upload(uploadPath, videoBytes, upsert = true)

                // Get public URL
                val publicUrl = bucket.publicUrl(uploadPath)

                Log.d(TAG, "✅ Video uploaded to community: $publicUrl")
                Result.success(publicUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading video: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload multiple videos for a community post
     * Returns list of public URLs
     */
    suspend fun uploadCommunityVideos(
        userId: String,
        videoPaths: List<String>
    ): Result<List<String>> {
        if (!CommunityFeatureFlag.ENABLED) {
            return Result.failure(Exception("Community feature is disabled"))
        }

        if (videoPaths.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            withContext(Dispatchers.IO) {
                val uploadedUrls = mutableListOf<String>()

                videoPaths.forEachIndexed { index, path ->
                    val result = uploadCommunityVideo(userId, path, index)
                    result.onSuccess { url ->
                        uploadedUrls.add(url)
                    }.onFailure { error ->
                        Log.w(TAG, "⚠️ Failed to upload video $index: ${error.message}")
                        // Continue with other videos even if one fails
                    }
                }

                Log.d(TAG, "✅ Uploaded ${uploadedUrls.size}/${videoPaths.size} videos")
                Result.success(uploadedUrls)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading videos: ${e.message}", e)
            Result.failure(e)
        }
    }
}