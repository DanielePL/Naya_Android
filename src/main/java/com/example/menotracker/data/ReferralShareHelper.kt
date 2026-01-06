package com.example.menotracker.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Referral Share Helper
 *
 * Combines video watermarking with sharing functionality.
 * Handles the complete flow:
 * 1. Get user's referral code
 * 2. Add watermark to video
 * 3. Share with referral link
 */
class ReferralShareHelper(private val context: Context) {

    companion object {
        private const val TAG = "ReferralShareHelper"
    }

    private val watermarkRenderer = VideoWatermarkRenderer(context)

    /**
     * Share result
     */
    data class ShareResult(
        val success: Boolean,
        val watermarkedVideoPath: String? = null,
        val referralLink: String? = null,
        val error: String? = null
    )

    /**
     * Prepare video for sharing with watermark
     *
     * @param videoUri Original video URI
     * @param exerciseName Optional exercise name
     * @param weight Optional weight used
     * @param velocity Optional velocity achieved
     * @return ShareResult with watermarked video path
     */
    suspend fun prepareVideoForSharing(
        videoUri: Uri,
        exerciseName: String? = null,
        weight: Float? = null,
        velocity: Float? = null
    ): ShareResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 Preparing video for sharing: $videoUri")

            // 1. Get referral code
            val referralCodeResult = UserReferralRepository.getOrCreateReferralCode()
            val referralCode = referralCodeResult.getOrNull()

            if (referralCode == null) {
                Log.w(TAG, "⚠️ Could not get referral code, sharing without watermark")
                return@withContext ShareResult(
                    success = true,
                    watermarkedVideoPath = null, // Share original video
                    referralLink = null
                )
            }

            // 2. Add watermark to video
            val watermarkResult = watermarkRenderer.addWatermark(
                inputUri = videoUri,
                referralCode = referralCode,
                exerciseName = exerciseName,
                velocity = velocity
            )

            if (!watermarkResult.success) {
                Log.e(TAG, "❌ Watermark failed: ${watermarkResult.error}")
                // Fall back to sharing without watermark
                return@withContext ShareResult(
                    success = true,
                    watermarkedVideoPath = null,
                    referralLink = "https://naya.app/r/$referralCode"
                )
            }

            // 3. Return result
            ShareResult(
                success = true,
                watermarkedVideoPath = watermarkResult.outputPath,
                referralLink = "https://naya.app/r/$referralCode"
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Share preparation error: ${e.message}", e)
            ShareResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Share video to external app (Instagram, TikTok, etc.)
     *
     * @param videoPath Path to video (watermarked or original)
     * @param caption Share caption
     * @param targetPackage Target app package (null for system share sheet)
     */
    fun shareVideo(
        videoPath: String,
        caption: String,
        targetPackage: String? = null
    ) {
        try {
            val file = File(videoPath)
            if (!file.exists()) {
                Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                targetPackage?.let { setPackage(it) }
            }

            if (targetPackage != null) {
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // App not installed, open share sheet
                    Log.w(TAG, "Target app not installed: $targetPackage")
                    intent.setPackage(null)
                    context.startActivity(Intent.createChooser(intent, "Share video"))
                }
            } else {
                context.startActivity(Intent.createChooser(intent, "Share video"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Share error: ${e.message}", e)
            Toast.makeText(context, "Could not share video", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share to Instagram Story
     */
    fun shareToInstagramStory(videoPath: String) {
        try {
            val file = File(videoPath)
            if (!file.exists()) {
                Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "video/*")
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("source_application", context.packageName)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Instagram not installed or story share not supported
                Log.w(TAG, "Instagram story share failed: ${e.message}")
                shareVideo(videoPath, "", "com.instagram.android")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Instagram story error: ${e.message}", e)
            Toast.makeText(context, "Could not share to Instagram", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share to TikTok
     */
    fun shareToTikTok(videoPath: String) {
        shareVideo(videoPath, "", "com.zhiliaoapp.musically")
    }

    /**
     * Generate share caption with referral link
     *
     * @param exerciseName Exercise performed
     * @param weight Weight used
     * @param velocity Velocity achieved
     * @param referralCode User's referral code
     */
    suspend fun generateShareCaption(
        exerciseName: String? = null,
        weight: Float? = null,
        velocity: Float? = null
    ): String {
        return UserReferralRepository.getShareText(exerciseName, weight, velocity)
    }

    /**
     * Copy referral link to clipboard
     */
    suspend fun copyReferralLink(): Boolean {
        return try {
            val link = UserReferralRepository.getReferralLink().getOrNull()
            if (link != null) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Referral Link", link)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Referral link copied!", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Copy link error: ${e.message}")
            false
        }
    }

    /**
     * Get short watermark text for display
     */
    suspend fun getWatermarkPreview(): String {
        return UserReferralRepository.getShortWatermark()
    }

    /**
     * Clean up temporary watermarked videos
     */
    fun cleanup() {
        watermarkRenderer.cleanupTempFiles()
    }
}

/**
 * Share destination platforms
 */
enum class SharePlatform(
    val displayName: String,
    val packageName: String?
) {
    INSTAGRAM("Instagram", "com.instagram.android"),
    INSTAGRAM_STORY("Instagram Story", "com.instagram.android"),
    TIKTOK("TikTok", "com.zhiliaoapp.musically"),
    FACEBOOK("Facebook", "com.facebook.katana"),
    WHATSAPP("WhatsApp", "com.whatsapp"),
    TWITTER("X (Twitter)", "com.twitter.android"),
    OTHER("More...", null)
}