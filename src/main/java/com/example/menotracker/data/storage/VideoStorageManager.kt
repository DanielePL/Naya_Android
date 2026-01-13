package com.example.menotracker.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import io.github.jan.supabase.postgrest.from
import com.example.menotracker.data.CoachRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Video Storage Manager
 *
 * Handles video storage with smart logic:
 * - Single users: Device storage only (saves costs)
 * - Coaching users: Cloud storage via Supabase (enables coach access)
 */
class VideoStorageManager(
    private val context: Context,
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "VideoStorageManager"
        private const val BUCKET_NAME = "workout-videos"
        private const val ADMIN_BUCKET_NAME = "workout-templates"  // Public bucket for admin-uploaded workout videos
        private const val DEVICE_STORAGE_DIR = "Naya/videos"
        private const val GUEST_USER_ID = "00000000-0000-0000-0000-000000000000"
    }

    /**
     * Storage type for video
     */
    enum class StorageType {
        DEVICE,  // Local device storage (single users)
        CLOUD    // Supabase cloud storage (coaching users)
    }

    /**
     * Upload result
     */
    data class UploadResult(
        val success: Boolean,
        val videoUrl: String? = null,
        val localPath: String? = null,
        val storageType: StorageType,
        val error: String? = null
    )

    /**
     * Save video based on user type
     *
     * @param videoUri URI of the video file
     * @param userId User UUID
     * @param setId Set UUID
     * @param hasActiveCoach Whether user has active coach
     * @param forceCloud Force cloud upload (e.g., for VBT analysis that requires backend processing)
     */
    suspend fun saveVideo(
        videoUri: Uri,
        userId: String,
        setId: String,
        hasActiveCoach: Boolean,
        forceCloud: Boolean = false  // For VBT: always upload to cloud so backend can analyze
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val useCloud = hasActiveCoach || forceCloud
            Log.d(TAG, "📹 Saving video: userId=$userId, setId=$setId, hasCoach=$hasActiveCoach, forceCloud=$forceCloud → useCloud=$useCloud")

            if (useCloud) {
                // Upload to cloud (coaching user OR VBT analysis)
                uploadToCloud(videoUri, userId, setId)
            } else {
                // Single user without VBT → Save to device
                saveToDevice(videoUri, setId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving video: ${e.message}", e)
            val useCloud = hasActiveCoach || forceCloud
            UploadResult(
                success = false,
                storageType = if (useCloud) StorageType.CLOUD else StorageType.DEVICE,
                error = e.message
            )
        }
    }

    /**
     * Helper to open InputStream from any URI type (file:// or content://)
     */
    private fun openInputStream(uri: Uri): InputStream? {
        return try {
            if (uri.scheme == "file") {
                // file:// URI - use FileInputStream directly
                val path = uri.path ?: return null
                FileInputStream(File(path))
            } else {
                // content:// URI - use ContentResolver
                context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to open InputStream: ${e.message}", e)
            null
        }
    }

    /**
     * Upload video to Supabase Storage (Cloud)
     */
    private suspend fun uploadToCloud(
        videoUri: Uri,
        userId: String,
        setId: String
    ): UploadResult {
        try {
            Log.d(TAG, "☁️ Uploading to Supabase Storage... URI: $videoUri")

            // Read video file (handles both file:// and content:// URIs)
            Log.d(TAG, "📂 Opening video file: $videoUri")
            val inputStream = openInputStream(videoUri)
                ?: return UploadResult(
                    success = false,
                    storageType = StorageType.CLOUD,
                    error = "Cannot open video file"
                )

            val videoBytes = inputStream.readBytes()
            inputStream.close()
            Log.d(TAG, "📦 Video size: ${videoBytes.size / 1024 / 1024}MB (${videoBytes.size} bytes)")

            // Upload path: users/{userId}/sets/{setId}.mp4
            val uploadPath = "users/$userId/sets/$setId.mp4"
            Log.d(TAG, "📤 Uploading to bucket '$BUCKET_NAME' path: $uploadPath")

            // Upload to Supabase
            val bucket = supabase.storage.from(BUCKET_NAME)
            Log.d(TAG, "🪣 Got bucket reference, starting upload...")
            bucket.upload(uploadPath, videoBytes, upsert = true)
            Log.d(TAG, "✅ Upload completed!")

            // Get public URL (requires signed URL for private bucket)
            Log.d(TAG, "🔗 Creating signed URL...")
            val signedUrl = bucket.createSignedUrl(uploadPath, expiresIn = 365.days)

            Log.d(TAG, "✅ Cloud upload successful: $signedUrl")

            return UploadResult(
                success = true,
                videoUrl = signedUrl,
                storageType = StorageType.CLOUD
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cloud upload failed: ${e.message}", e)
            return UploadResult(
                success = false,
                storageType = StorageType.CLOUD,
                error = "Upload failed: ${e.message}"
            )
        }
    }

    /**
     * Save video to device storage (Local) - saves to gallery/Movies folder
     * Videos are visible in the device's gallery app
     */
    private suspend fun saveToDevice(
        videoUri: Uri,
        setId: String
    ): UploadResult {
        try {
            Log.d(TAG, "📱 Saving to device gallery... URI: $videoUri")

            // Read video bytes from source URI
            val inputStream: InputStream = openInputStream(videoUri)
                ?: return UploadResult(
                    success = false,
                    storageType = StorageType.DEVICE,
                    error = "Cannot open video file"
                )

            val videoBytes = inputStream.readBytes()
            inputStream.close()

            // Generate filename with timestamp for gallery
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Naya_${timestamp}_${setId.take(8)}.mp4"

            val savedUri: Uri?
            val localPath: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ : Use MediaStore API (scoped storage)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Naya")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                savedUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (savedUri == null) {
                    return UploadResult(
                        success = false,
                        storageType = StorageType.DEVICE,
                        error = "Failed to create media entry"
                    )
                }

                // Write video data
                resolver.openOutputStream(savedUri)?.use { output ->
                    output.write(videoBytes)
                }

                // Mark as complete (no longer pending)
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(savedUri, contentValues, null, null)

                localPath = savedUri.toString()
                Log.d(TAG, "✅ Saved to gallery (MediaStore): $localPath")

            } else {
                // Android 9 and below: Direct file access
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val nayaDir = File(moviesDir, "Naya")
                if (!nayaDir.exists()) {
                    nayaDir.mkdirs()
                }

                val videoFile = File(nayaDir, fileName)
                FileOutputStream(videoFile).use { output ->
                    output.write(videoBytes)
                }

                // Notify media scanner so video appears in gallery
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(videoFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )

                localPath = videoFile.absolutePath
                Log.d(TAG, "✅ Saved to gallery (legacy): $localPath")
            }

            // Also save a copy to app-private storage for reliable playback
            val privateDir = File(context.getExternalFilesDir(null), DEVICE_STORAGE_DIR)
            if (!privateDir.exists()) {
                privateDir.mkdirs()
            }
            val privateFile = File(privateDir, "$setId.mp4")
            FileOutputStream(privateFile).use { output ->
                output.write(videoBytes)
            }
            Log.d(TAG, "✅ Also saved to app storage: ${privateFile.absolutePath}")

            return UploadResult(
                success = true,
                localPath = privateFile.absolutePath, // Return app-private path for reliable playback
                storageType = StorageType.DEVICE
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Device save failed: ${e.message}", e)
            return UploadResult(
                success = false,
                storageType = StorageType.DEVICE,
                error = "Save failed: ${e.message}"
            )
        }
    }

    /**
     * Delete video from storage
     */
    suspend fun deleteVideo(
        userId: String,
        setId: String,
        storageType: StorageType
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            when (storageType) {
                StorageType.CLOUD -> {
                    // Delete from Supabase
                    val uploadPath = "users/$userId/sets/$setId.mp4"
                    val bucket = supabase.storage.from(BUCKET_NAME)
                    bucket.delete(uploadPath)
                    Log.d(TAG, "✅ Cloud video deleted: $uploadPath")
                    true
                }
                StorageType.DEVICE -> {
                    // Delete from device
                    val storageDir = File(context.getExternalFilesDir(null), DEVICE_STORAGE_DIR)
                    val videoFile = File(storageDir, "$setId.mp4")
                    val deleted = videoFile.delete()
                    Log.d(TAG, "✅ Device video deleted: ${videoFile.absolutePath}")
                    deleted
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting video: ${e.message}", e)
            false
        }
    }

    /**
     * Get video URL for playback
     */
    suspend fun getVideoUrl(
        userId: String,
        setId: String,
        storageType: StorageType
    ): String? = withContext(Dispatchers.IO) {
        try {
            when (storageType) {
                StorageType.CLOUD -> {
                    // Get signed URL from Supabase
                    val uploadPath = "users/$userId/sets/$setId.mp4"
                    val bucket = supabase.storage.from(BUCKET_NAME)
                    bucket.createSignedUrl(uploadPath, expiresIn = 1.hours)
                }
                StorageType.DEVICE -> {
                    // Return local file path
                    val storageDir = File(context.getExternalFilesDir(null), DEVICE_STORAGE_DIR)
                    val videoFile = File(storageDir, "$setId.mp4")
                    if (videoFile.exists()) {
                        "file://${videoFile.absolutePath}"
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting video URL: ${e.message}", e)
            null
        }
    }

    /**
     * Check if user has active coach (determines storage type)
     * Uses coach_client_connections table (same as CoachRepository)
     * Guest users always return false (device storage only)
     */
    suspend fun checkUserHasCoach(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Guest users always use device storage
            if (isGuestUser(userId)) {
                Log.d(TAG, "👤 Guest user detected - using device storage")
                return@withContext false
            }

            Log.d(TAG, "🔍 Checking coach status for user: $userId")

            // Query coach_client_connections table - reuse CoachConnection from CoachRepository
            val result = supabase.from("coach_client_connections")
                .select {
                    filter {
                        eq("client_id", userId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<CoachRepository.CoachConnection>()

            val hasCoach = result.isNotEmpty()
            Log.d(TAG, "📱 User has coach: $hasCoach (found ${result.size} connections)")

            // Debug: Log connection details
            result.forEach { connection ->
                Log.d(TAG, "📋 Connection: coach=${connection.coachId}, status=${connection.status}")
            }

            hasCoach
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking coach status: ${e.message}", e)
            false  // Default to device storage on error
        }
    }

    /**
     * Check if user is a guest (not authenticated with Supabase)
     */
    private fun isGuestUser(userId: String): Boolean {
        return userId == GUEST_USER_ID || userId.isBlank()
    }

    // ==================== ADMIN VIDEO UPLOAD ====================

    /**
     * Admin result for workout template video upload
     */
    data class AdminUploadResult(
        val success: Boolean,
        val videoUrl: String? = null,
        val error: String? = null
    )

    /**
     * Upload a workout template video (Admin only)
     *
     * Videos are stored in a public bucket for all users to access.
     * Path: templates/{workoutTemplateId}/preview.mp4
     *
     * @param videoUri URI of the video file
     * @param workoutTemplateId The workout template UUID
     * @return AdminUploadResult with public URL or error
     */
    suspend fun uploadWorkoutTemplateVideo(
        videoUri: Uri,
        workoutTemplateId: String
    ): AdminUploadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎬 [ADMIN] Uploading workout template video...")
            Log.d(TAG, "📁 Video URI: $videoUri")
            Log.d(TAG, "🏋️ Template ID: $workoutTemplateId")

            // Read video file
            val inputStream = openInputStream(videoUri)
                ?: return@withContext AdminUploadResult(
                    success = false,
                    error = "Cannot open video file"
                )

            val videoBytes = inputStream.readBytes()
            inputStream.close()

            val fileSizeMB = videoBytes.size / 1024.0 / 1024.0
            Log.d(TAG, "📦 Video size: %.2f MB (%d bytes)".format(fileSizeMB, videoBytes.size))

            // Check file size (limit to 100MB)
            if (fileSizeMB > 100) {
                return@withContext AdminUploadResult(
                    success = false,
                    error = "Video too large (max 100MB). Size: %.1f MB".format(fileSizeMB)
                )
            }

            // Upload path: templates/{workoutTemplateId}/preview.mp4
            val uploadPath = "templates/$workoutTemplateId/preview.mp4"
            Log.d(TAG, "📤 Uploading to bucket '$ADMIN_BUCKET_NAME' path: $uploadPath")

            // Upload to Supabase
            val bucket = supabase.storage.from(ADMIN_BUCKET_NAME)
            bucket.upload(uploadPath, videoBytes, upsert = true)
            Log.d(TAG, "✅ Upload completed!")

            // Get public URL (public bucket - no signed URL needed)
            val publicUrl = bucket.publicUrl(uploadPath)
            Log.d(TAG, "🔗 Public URL: $publicUrl")

            AdminUploadResult(
                success = true,
                videoUrl = publicUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ [ADMIN] Upload failed: ${e.message}", e)
            AdminUploadResult(
                success = false,
                error = "Upload failed: ${e.message}"
            )
        }
    }

    /**
     * Delete a workout template video (Admin only)
     *
     * @param workoutTemplateId The workout template UUID
     * @return true if deleted successfully
     */
    suspend fun deleteWorkoutTemplateVideo(
        workoutTemplateId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val uploadPath = "templates/$workoutTemplateId/preview.mp4"
            val bucket = supabase.storage.from(ADMIN_BUCKET_NAME)
            bucket.delete(uploadPath)
            Log.d(TAG, "✅ [ADMIN] Video deleted: $uploadPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ [ADMIN] Delete failed: ${e.message}", e)
            false
        }
    }
}
