package com.example.menotracker.data

import android.content.Context
import android.graphics.*
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Video Watermark Renderer
 *
 * Adds a referral watermark to videos before sharing.
 * Watermark appears in bottom-right corner with referral link.
 *
 * Example: "PROMETHEUS • 10% OFF → naya.app/r/ABC123"
 */
class VideoWatermarkRenderer(private val context: Context) {

    companion object {
        private const val TAG = "VideoWatermark"

        // Watermark styling
        private const val WATERMARK_PADDING_PERCENT = 0.03f  // 3% from edges
        private const val WATERMARK_FONT_SIZE_PERCENT = 0.025f  // 2.5% of video height
        private const val WATERMARK_BG_ALPHA = 180  // Semi-transparent background
    }

    // Brand colors
    private val violetPrimary = Color.parseColor("#8B5CF6")
    private val whiteText = Color.WHITE
    private val darkBg = Color.parseColor("#1A1410")

    /**
     * Result of watermark rendering
     */
    data class WatermarkResult(
        val success: Boolean,
        val outputPath: String? = null,
        val error: String? = null
    )

    /**
     * Add watermark to video with referral link
     *
     * @param inputUri Original video URI
     * @param referralCode User's referral code (e.g., "ABC123")
     * @param exerciseName Optional exercise name for context
     * @param velocity Optional velocity for context
     * @return WatermarkResult with path to watermarked video
     */
    suspend fun addWatermark(
        inputUri: Uri,
        referralCode: String,
        exerciseName: String? = null,
        velocity: Float? = null
    ): WatermarkResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📹 Adding watermark to video: $inputUri, code: $referralCode")

            // Create output file
            val outputDir = File(context.cacheDir, "watermarked_videos")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "watermarked_${System.currentTimeMillis()}.mp4")

            // Get input path
            val inputPath = getPathFromUri(inputUri)
            if (inputPath == null) {
                return@withContext WatermarkResult(
                    success = false,
                    error = "Cannot read input video"
                )
            }

            // Build watermark text
            val watermarkText = buildWatermarkText(referralCode, exerciseName, velocity)

            // Process video with watermark
            val success = processVideoWithWatermark(
                inputPath = inputPath,
                outputPath = outputFile.absolutePath,
                watermarkText = watermarkText
            )

            if (success && outputFile.exists() && outputFile.length() > 0) {
                Log.d(TAG, "✅ Watermarked video created: ${outputFile.absolutePath}")
                WatermarkResult(
                    success = true,
                    outputPath = outputFile.absolutePath
                )
            } else {
                WatermarkResult(
                    success = false,
                    error = "Failed to create watermarked video"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Watermark error: ${e.message}", e)
            WatermarkResult(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Build watermark text from referral code and optional context
     */
    private fun buildWatermarkText(
        referralCode: String,
        exerciseName: String?,
        velocity: Float?
    ): String {
        return buildString {
            append("NAYA")

            if (exerciseName != null && velocity != null) {
                append(" • ${String.format("%.2f", velocity)} m/s")
            }

            append(" • 10% OFF → naya.app/r/$referralCode")
        }
    }

    /**
     * Get file path from URI (handles file:// and content://)
     */
    private fun getPathFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "file" -> uri.path
            "content" -> {
                // Copy content to temp file
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "temp_input_${System.currentTimeMillis()}.mp4")
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile.absolutePath
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy content URI: ${e.message}")
                    null
                }
            }
            else -> uri.path
        }
    }

    /**
     * Process video and add watermark overlay
     * Uses MediaCodec for efficient video processing
     */
    private fun processVideoWithWatermark(
        inputPath: String,
        outputPath: String,
        watermarkText: String
    ): Boolean {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            // Setup extractor
            extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            // Find video track
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                when {
                    mime.startsWith("video/") && videoTrackIndex < 0 -> {
                        videoTrackIndex = i
                        videoFormat = format
                    }
                    mime.startsWith("audio/") && audioTrackIndex < 0 -> {
                        audioTrackIndex = i
                        audioFormat = format
                    }
                }
            }

            if (videoTrackIndex < 0 || videoFormat == null) {
                Log.e(TAG, "No video track found")
                return false
            }

            val width = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val rotation = try {
                videoFormat.getInteger(MediaFormat.KEY_ROTATION)
            } catch (e: Exception) { 0 }

            Log.d(TAG, "Video: ${width}x${height}, rotation: $rotation")

            // Use simpler approach: just copy video with watermark drawn on frames
            // For now, use a frame-by-frame approach with bitmap manipulation

            return processWithBitmapOverlay(inputPath, outputPath, watermarkText)

        } catch (e: Exception) {
            Log.e(TAG, "Video processing error: ${e.message}", e)
            return false
        } finally {
            try {
                extractor?.release()
                decoder?.stop()
                decoder?.release()
                encoder?.stop()
                encoder?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup error: ${e.message}")
            }
        }
    }

    /**
     * Simpler approach: Extract frames, add watermark, re-encode
     * This is slower but more reliable across devices
     */
    private fun processWithBitmapOverlay(
        inputPath: String,
        outputPath: String,
        watermarkText: String
    ): Boolean {
        var retriever: android.media.MediaMetadataRetriever? = null
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null

        try {
            // Get video metadata
            retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(inputPath)

            val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
            val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
            val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frameRate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 30f

            Log.d(TAG, "Video info: ${width}x${height}, rotation=$rotation, duration=${durationMs}ms, fps=$frameRate")

            // Setup encoder
            val outputFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate.toInt())
            outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            // Setup muxer
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            if (rotation != 0) {
                muxer.setOrientationHint(rotation)
            }

            // Prepare watermark paint
            val watermarkPaint = createWatermarkPaint(height)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkBg
                alpha = WATERMARK_BG_ALPHA
            }

            // Process frames
            val frameIntervalUs = (1_000_000L / frameRate).toLong()
            var presentationTimeUs = 0L
            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            // Extract and process frames
            var frameCount = 0
            val totalFrames = ((durationMs * frameRate) / 1000).toInt()

            while (presentationTimeUs < durationMs * 1000) {
                // Get frame at this timestamp
                val frameBitmap = retriever.getFrameAtTime(
                    presentationTimeUs,
                    android.media.MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (frameBitmap != null) {
                    // Create output bitmap with watermark
                    val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(outputBitmap)

                    // Draw original frame (scaled if needed)
                    val srcRect = Rect(0, 0, frameBitmap.width, frameBitmap.height)
                    val dstRect = Rect(0, 0, width, height)
                    canvas.drawBitmap(frameBitmap, srcRect, dstRect, null)

                    // Draw watermark
                    drawWatermark(canvas, watermarkText, watermarkPaint, bgPaint, width, height)

                    // Encode frame
                    val surfaceCanvas = inputSurface.lockCanvas(null)
                    surfaceCanvas.drawBitmap(outputBitmap, 0f, 0f, null)
                    inputSurface.unlockCanvasAndPost(surfaceCanvas)

                    // Clean up bitmaps
                    frameBitmap.recycle()
                    outputBitmap.recycle()

                    // Drain encoder
                    drainEncoder(encoder, muxer, bufferInfo, false) {
                        if (!muxerStarted) {
                            videoTrackIndex = muxer!!.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        videoTrackIndex
                    }
                }

                presentationTimeUs += frameIntervalUs
                frameCount++

                // Log progress every 30 frames
                if (frameCount % 30 == 0) {
                    Log.d(TAG, "Processing: $frameCount/$totalFrames frames")
                }
            }

            // Signal end of stream
            encoder.signalEndOfInputStream()
            drainEncoder(encoder, muxer, bufferInfo, true) { videoTrackIndex }

            Log.d(TAG, "✅ Processed $frameCount frames with watermark")

            // Copy audio track (if exists)
            copyAudioTrack(inputPath, outputPath, muxer)

            return true

        } catch (e: Exception) {
            Log.e(TAG, "Bitmap overlay error: ${e.message}", e)
            return false
        } finally {
            try {
                retriever?.release()
                extractor?.release()
                encoder?.stop()
                encoder?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup error: ${e.message}")
            }
        }
    }

    /**
     * Create paint for watermark text
     */
    private fun createWatermarkPaint(videoHeight: Int): Paint {
        val fontSize = (videoHeight * WATERMARK_FONT_SIZE_PERCENT).coerceIn(14f, 32f)

        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteText
            textSize = fontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
    }

    /**
     * Draw watermark on canvas (bottom-right corner)
     */
    private fun drawWatermark(
        canvas: Canvas,
        text: String,
        textPaint: Paint,
        bgPaint: Paint,
        width: Int,
        height: Int
    ) {
        val padding = (height * WATERMARK_PADDING_PERCENT).coerceIn(8f, 24f)

        // Measure text
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textWidth = textPaint.measureText(text)
        val textHeight = textBounds.height()

        // Position (bottom-right)
        val x = width - textWidth - padding - 8f
        val y = height - padding - 8f

        // Draw background
        val bgPadding = 8f
        canvas.drawRoundRect(
            x - bgPadding,
            y - textHeight - bgPadding,
            x + textWidth + bgPadding,
            y + bgPadding,
            6f, 6f,
            bgPaint
        )

        // Draw orange accent line on left
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = violetPrimary
        }
        canvas.drawRoundRect(
            x - bgPadding,
            y - textHeight - bgPadding,
            x - bgPadding + 4f,
            y + bgPadding,
            2f, 2f,
            accentPaint
        )

        // Draw text
        canvas.drawText(text, x, y, textPaint)
    }

    /**
     * Drain encoder output
     */
    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer?,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        getTrackIndex: () -> Int
    ) {
        while (true) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)

            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // Format changed, handled by callback
                }
                outputBufferIndex >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex) ?: continue

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0 && muxer != null) {
                        val trackIndex = getTrackIndex()
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        return
                    }
                }
            }
        }
    }

    /**
     * Copy audio track from source to output
     */
    private fun copyAudioTrack(inputPath: String, outputPath: String, muxer: MediaMuxer?) {
        if (muxer == null) return

        var extractor: MediaExtractor? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            // Find audio track
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val audioTrackIndex = muxer.addTrack(format)

                    // Copy audio data
                    val buffer = ByteBuffer.allocate(256 * 1024)
                    val bufferInfo = MediaCodec.BufferInfo()

                    while (true) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) break

                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = extractor.sampleTime
                        bufferInfo.flags = extractor.sampleFlags

                        muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                        extractor.advance()
                    }

                    Log.d(TAG, "✅ Audio track copied")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio copy error: ${e.message}")
        } finally {
            extractor?.release()
        }
    }

    /**
     * Clean up temporary files
     */
    fun cleanupTempFiles() {
        try {
            val cacheDir = File(context.cacheDir, "watermarked_videos")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    // Delete files older than 1 hour
                    if (System.currentTimeMillis() - file.lastModified() > 3600000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }
}