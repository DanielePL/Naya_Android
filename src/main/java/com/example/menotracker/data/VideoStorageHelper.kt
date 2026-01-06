package com.example.menotracker.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Helper for managing video file storage
 * Copies videos from temp cache to permanent app storage
 */
object VideoStorageHelper {

    private const val TAG = "VideoStorageHelper"

    /**
     * Get the permanent video storage directory
     * Creates it if it doesn't exist
     */
    fun getVideoStorageDir(context: Context): File {
        val videoDir = File(context.filesDir, "workout_videos")
        if (!videoDir.exists()) {
            videoDir.mkdirs()
            android.util.Log.d(TAG, "📁 Created video storage directory: ${videoDir.absolutePath}")
        }
        return videoDir
    }

    /**
     * Copy video from temp location to permanent storage
     * Returns the permanent file path
     */
    fun copyVideoToPermanentStorage(
        context: Context,
        sourceFile: File,
        setId: String
    ): File? {
        return try {
            val videoDir = getVideoStorageDir(context)
            val permanentFile = File(videoDir, "${setId}.mp4")

            // Copy file
            sourceFile.inputStream().use { input ->
                FileOutputStream(permanentFile).use { output ->
                    input.copyTo(output)
                }
            }

            android.util.Log.d(TAG, "✅ Video copied to permanent storage:")
            android.util.Log.d(TAG, "   Source: ${sourceFile.absolutePath}")
            android.util.Log.d(TAG, "   Destination: ${permanentFile.absolutePath}")
            android.util.Log.d(TAG, "   Size: ${permanentFile.length() / 1024} KB")

            permanentFile
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to copy video to permanent storage", e)
            null
        }
    }

    /**
     * Get video file by set ID
     */
    fun getVideoFile(context: Context, setId: String): File? {
        val videoDir = getVideoStorageDir(context)
        val videoFile = File(videoDir, "${setId}.mp4")
        return if (videoFile.exists()) videoFile else null
    }

    /**
     * Delete video file
     */
    fun deleteVideo(context: Context, setId: String): Boolean {
        val videoFile = getVideoFile(context, setId)
        return videoFile?.delete() ?: false
    }

    /**
     * Get total storage used by videos in MB
     */
    fun getTotalStorageUsedMB(context: Context): Double {
        val videoDir = getVideoStorageDir(context)
        val totalBytes = videoDir.listFiles()?.sumOf { it.length() } ?: 0L
        return totalBytes / (1024.0 * 1024.0)
    }

    /**
     * Save analyzed video to device gallery (MediaStore)
     * This creates a separate file in the gallery with the pose overlay
     */
    fun saveAnalyzedVideoToGallery(context: Context, videoFile: File, analysisId: String): Uri? {
        return try {
            android.util.Log.d(TAG, "📸 Saving analyzed video to gallery...")
            android.util.Log.d(TAG, "   Source: ${videoFile.absolutePath}")

            // Create content values for MediaStore
            val timestamp = System.currentTimeMillis()
            val displayName = "naya_analyzed_${timestamp}.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Naya")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            // Insert into MediaStore
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val videoUri = context.contentResolver.insert(collection, contentValues)
                ?: run {
                    android.util.Log.e(TAG, "❌ Failed to create MediaStore entry")
                    return null
                }

            // Copy video data to MediaStore
            context.contentResolver.openOutputStream(videoUri)?.use { outputStream ->
                videoFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Mark as not pending (make visible in gallery)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(videoUri, contentValues, null, null)
            }

            android.util.Log.d(TAG, "✅ Analyzed video saved to gallery:")
            android.util.Log.d(TAG, "   URI: $videoUri")
            android.util.Log.d(TAG, "   Name: $displayName")

            videoUri
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to save analyzed video to gallery", e)
            null
        }
    }
}
