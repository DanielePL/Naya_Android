// data/repository/WodInputRouter.kt
package com.example.menotracker.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * WOD INPUT ROUTER - INTELLIGENT DETECTION & ROUTING
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Analyzes input (camera/gallery) to determine the most efficient processing route:
 *
 * HYBRID APPROACH:
 * 1. Try LOCAL OCR first (ML Kit) - FREE
 * 2. If text quality good + patterns match → Parse locally
 * 3. If text quality poor OR complex format → Fall back to Vision AI
 *
 * SUPPORTED INPUTS:
 * - Camera photo (whiteboard, screenshot)
 * - Gallery image (PNG, JPG)
 * - Excel file (.xlsx, .xls) - from influencer programs
 * - PDF file (.pdf) - from influencer programs
 *
 * Cost savings: ~70-90% reduction by routing clear text to free local processing
 */
object WodInputRouter {

    private const val TAG = "WodInputRouter"

    /**
     * Input source types
     */
    enum class InputType {
        CAMERA_PHOTO,
        GALLERY_IMAGE,
        EXCEL_FILE,
        PDF_FILE,
        UNKNOWN
    }

    /**
     * Detection result with routing recommendation
     */
    sealed class DetectionResult {
        /**
         * Text successfully extracted locally - can try local parsing
         */
        data class TextExtracted(
            val rawText: String,
            val confidence: Float,
            val source: InputType,
            val recommendAI: Boolean, // true if text quality is poor
            val detectedPatterns: List<String> = emptyList()
        ) : DetectionResult()

        /**
         * File parsed successfully (Excel/PDF)
         */
        data class FileParsed(
            val rawText: String,
            val source: InputType,
            val rows: List<List<String>> = emptyList() // For structured data
        ) : DetectionResult()

        /**
         * Local extraction failed - must use Vision AI
         */
        data class RequiresVisionAI(
            val reason: String,
            val bitmap: Bitmap? = null,
            val file: File? = null
        ) : DetectionResult()

        /**
         * Error during processing
         */
        data class Error(val message: String) : DetectionResult()
    }

    /**
     * WOD-specific keywords to detect (multi-language)
     */
    private val wodKeywords = listOf(
        // English WOD types
        "for time", "amrap", "emom", "tabata", "chipper", "ladder",
        "death by", "buy in", "buy out", "cash out", "metcon",
        // German WOD types
        "auf zeit", "so schnell wie möglich",
        // Rep schemes
        "21-15-9", "15-12-9", "10-9-8-7-6-5-4-3-2-1", "rounds",
        // Common movements
        "thrusters", "burpees", "pull-ups", "pullups", "pull ups",
        "box jumps", "box jump", "kettlebell", "kb swing",
        "deadlift", "squat", "clean", "snatch", "jerk",
        "wall balls", "wall ball", "double unders", "du",
        "toes to bar", "t2b", "muscle ups", "muscle-ups",
        "handstand", "hspu", "rope climb", "row", "run",
        // Weight indicators
        "kg", "lbs", "lb", "rx", "scaled", "m/f", "♂/♀"
    )

    /**
     * Minimum keyword matches to consider it a valid WOD
     */
    private const val MIN_WOD_KEYWORD_MATCHES = 2

    /**
     * Confidence threshold below which we recommend AI
     */
    private const val AI_RECOMMENDATION_THRESHOLD = 0.6f

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Detect input type from URI
     */
    fun detectInputType(uri: Uri, context: Context): InputType {
        val mimeType = context.contentResolver.getType(uri)
        val fileName = uri.lastPathSegment?.lowercase() ?: ""

        return when {
            mimeType?.startsWith("image/") == true -> InputType.GALLERY_IMAGE
            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
            mimeType == "application/vnd.ms-excel" ||
            fileName.endsWith(".xlsx") ||
            fileName.endsWith(".xls") -> InputType.EXCEL_FILE
            mimeType == "application/pdf" ||
            fileName.endsWith(".pdf") -> InputType.PDF_FILE
            else -> InputType.UNKNOWN
        }
    }

    /**
     * Process input from URI (gallery, file picker)
     */
    suspend fun processUri(
        uri: Uri,
        context: Context
    ): DetectionResult = withContext(Dispatchers.IO) {
        val inputType = detectInputType(uri, context)
        Log.d(TAG, "📁 Processing URI: $uri, type: $inputType")

        when (inputType) {
            InputType.GALLERY_IMAGE -> processImage(uri, context)
            InputType.EXCEL_FILE -> processExcel(uri, context)
            InputType.PDF_FILE -> processPdf(uri, context)
            InputType.CAMERA_PHOTO -> processImage(uri, context)
            InputType.UNKNOWN -> DetectionResult.Error("Unsupported file type")
        }
    }

    /**
     * Process bitmap directly (from camera capture)
     */
    suspend fun processBitmap(
        bitmap: Bitmap,
        source: InputType = InputType.CAMERA_PHOTO
    ): DetectionResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "📸 Processing bitmap: ${bitmap.width}x${bitmap.height}")
        extractTextFromBitmap(bitmap, source)
    }

    /**
     * Quick check during live camera preview
     */
    @androidx.camera.core.ExperimentalGetImage
    fun quickCheckFrame(
        imageProxy: androidx.camera.core.ImageProxy,
        onResult: (hasWodText: Boolean, confidence: Float) -> Unit
    ) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            onResult(false, 0f)
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val normalizedText = visionText.text.lowercase()
                val keywordMatches = wodKeywords.count { keyword ->
                    normalizedText.contains(keyword)
                }
                val hasWodText = keywordMatches >= MIN_WOD_KEYWORD_MATCHES
                val confidence = (keywordMatches.toFloat() / 5f).coerceAtMost(1f)

                onResult(hasWodText, confidence)
            }
            .addOnFailureListener {
                onResult(false, 0f)
            }
            .addOnCompleteListener {
                imageProxy.close()
                recognizer.close()
            }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE PROCESSING METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Process image from URI
     */
    private suspend fun processImage(uri: Uri, context: Context): DetectionResult {
        return try {
            val bitmap = loadBitmapFromUri(uri, context)
            if (bitmap == null) {
                return DetectionResult.Error("Failed to load image")
            }
            extractTextFromBitmap(bitmap, InputType.GALLERY_IMAGE)
        } catch (e: Exception) {
            Log.e(TAG, "Image processing error: ${e.message}", e)
            DetectionResult.Error("Image processing failed: ${e.message}")
        }
    }

    /**
     * Extract text from bitmap using ML Kit OCR
     */
    private suspend fun extractTextFromBitmap(
        bitmap: Bitmap,
        source: InputType
    ): DetectionResult = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = recognizer.process(inputImage).await()
            recognizer.close()

            val fullText = visionText.text
            if (fullText.isBlank()) {
                Log.d(TAG, "⚠️ No text detected in image")
                return@withContext DetectionResult.RequiresVisionAI(
                    reason = "No text detected - may be handwritten or low quality",
                    bitmap = bitmap
                )
            }

            // Calculate confidence based on:
            // 1. Number of text blocks detected
            // 2. Average confidence of blocks
            // 3. WOD keyword matches
            val blockCount = visionText.textBlocks.size
            val normalizedText = fullText.lowercase()

            val keywordMatches = wodKeywords.count { keyword ->
                normalizedText.contains(keyword)
            }
            val detectedPatterns = wodKeywords.filter { normalizedText.contains(it) }

            // Calculate overall confidence
            val textDensity = (blockCount.toFloat() / 10f).coerceAtMost(1f)
            val keywordScore = (keywordMatches.toFloat() / 5f).coerceAtMost(1f)
            val lengthScore = (fullText.length.toFloat() / 200f).coerceAtMost(1f)

            val confidence = (textDensity * 0.2f + keywordScore * 0.5f + lengthScore * 0.3f)

            Log.d(TAG, """
                📝 OCR Results:
                - Text length: ${fullText.length} chars
                - Blocks: $blockCount
                - Keyword matches: $keywordMatches
                - Confidence: ${(confidence * 100).toInt()}%
                - Detected patterns: $detectedPatterns
            """.trimIndent())

            // If low confidence or no WOD keywords, recommend AI
            val recommendAI = confidence < AI_RECOMMENDATION_THRESHOLD ||
                              keywordMatches < MIN_WOD_KEYWORD_MATCHES

            DetectionResult.TextExtracted(
                rawText = fullText,
                confidence = confidence,
                source = source,
                recommendAI = recommendAI,
                detectedPatterns = detectedPatterns
            )
        } catch (e: Exception) {
            Log.e(TAG, "OCR error: ${e.message}", e)
            DetectionResult.RequiresVisionAI(
                reason = "OCR failed: ${e.message}",
                bitmap = bitmap
            )
        }
    }

    /**
     * Process Excel file (.xlsx, .xls)
     */
    private suspend fun processExcel(uri: Uri, context: Context): DetectionResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return DetectionResult.Error("Cannot open Excel file")

            val result = ExcelParser.parse(inputStream)
            inputStream.close()

            when (result) {
                is ExcelParser.ParseResult.Success -> {
                    Log.d(TAG, "📊 Excel parsed: ${result.rows.size} rows")
                    DetectionResult.FileParsed(
                        rawText = result.text,
                        source = InputType.EXCEL_FILE,
                        rows = result.rows
                    )
                }
                is ExcelParser.ParseResult.Error -> {
                    DetectionResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excel processing error: ${e.message}", e)
            DetectionResult.Error("Excel processing failed: ${e.message}")
        }
    }

    /**
     * Process PDF file
     */
    private suspend fun processPdf(uri: Uri, context: Context): DetectionResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return DetectionResult.Error("Cannot open PDF file")

            val result = PdfParser.parse(inputStream)
            inputStream.close()

            when (result) {
                is PdfParser.ParseResult.Success -> {
                    Log.d(TAG, "📄 PDF parsed: ${result.text.length} chars")
                    DetectionResult.FileParsed(
                        rawText = result.text,
                        source = InputType.PDF_FILE
                    )
                }
                is PdfParser.ParseResult.Error -> {
                    DetectionResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF processing error: ${e.message}", e)
            DetectionResult.Error("PDF processing failed: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Try to get rotation from EXIF
            val rotatedBitmap = try {
                val exifStream = context.contentResolver.openInputStream(uri) ?: return bitmap
                val exif = ExifInterface(exifStream)
                exifStream.close()

                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                if (rotationDegrees != 0f && bitmap != null) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees) }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else bitmap
            } catch (e: Exception) {
                bitmap
            }

            rotatedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap load error: ${e.message}")
            null
        }
    }

    /**
     * Check if this result should use AI
     */
    fun shouldUseAI(result: DetectionResult): Boolean {
        return when (result) {
            is DetectionResult.TextExtracted -> result.recommendAI
            is DetectionResult.RequiresVisionAI -> true
            is DetectionResult.FileParsed -> false // Files can be parsed locally
            is DetectionResult.Error -> false
        }
    }

    /**
     * Get recommendation message for user
     */
    fun getRecommendationMessage(result: DetectionResult): String {
        return when (result) {
            is DetectionResult.TextExtracted -> {
                if (result.recommendAI) {
                    "Text quality low - using AI for better accuracy"
                } else {
                    "Text detected - parsing locally (free)"
                }
            }
            is DetectionResult.FileParsed -> "File parsed successfully (free)"
            is DetectionResult.RequiresVisionAI -> result.reason
            is DetectionResult.Error -> result.message
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXCEL PARSER (Using Apache POI)
// ═══════════════════════════════════════════════════════════════════════════════

object ExcelParser {

    private const val TAG = "ExcelParser"

    sealed class ParseResult {
        data class Success(
            val text: String,
            val rows: List<List<String>>
        ) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    /**
     * Parse Excel file using Apache POI
     */
    fun parse(inputStream: InputStream): ParseResult {
        return try {
            parseWithPoi(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Excel parse error: ${e.message}", e)
            // Fallback to basic text extraction
            try {
                inputStream.reset()
                parseAsText(inputStream)
            } catch (resetError: Exception) {
                ParseResult.Error("Excel parsing failed: ${e.message}")
            }
        }
    }

    private fun parseWithPoi(inputStream: InputStream): ParseResult {
        return try {
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)
            val rows = mutableListOf<List<String>>()
            val textBuilder = StringBuilder()

            // Process all sheets
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)

                for (row in sheet) {
                    val cells = mutableListOf<String>()
                    for (cell in row) {
                        val value = when (cell.cellType) {
                            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
                            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                    cell.localDateTimeCellValue?.toString() ?: ""
                                } else {
                                    cell.numericCellValue.toString()
                                }
                            }
                            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                                try {
                                    cell.stringCellValue
                                } catch (e: Exception) {
                                    cell.numericCellValue.toString()
                                }
                            }
                            else -> ""
                        }
                        cells.add(value)
                        if (value.isNotBlank()) {
                            textBuilder.append(value).append("\t")
                        }
                    }
                    if (cells.isNotEmpty()) {
                        rows.add(cells)
                        textBuilder.append("\n")
                    }
                }
            }

            workbook.close()

            Log.d(TAG, "Excel parsed: ${rows.size} rows, ${textBuilder.length} chars")

            ParseResult.Success(
                text = textBuilder.toString(),
                rows = rows
            )
        } catch (e: Exception) {
            Log.e(TAG, "POI parsing failed: ${e.message}", e)
            // Try parsing as old XLS format
            try {
                parseOldExcel(inputStream)
            } catch (xlsError: Exception) {
                ParseResult.Error("Excel parsing failed: ${e.message}")
            }
        }
    }

    private fun parseOldExcel(inputStream: InputStream): ParseResult {
        return try {
            val workbook = org.apache.poi.hssf.usermodel.HSSFWorkbook(inputStream)
            val rows = mutableListOf<List<String>>()
            val textBuilder = StringBuilder()

            val sheet = workbook.getSheetAt(0)
            for (row in sheet) {
                val cells = mutableListOf<String>()
                for (cell in row) {
                    val value = cell.toString()
                    cells.add(value)
                    if (value.isNotBlank()) {
                        textBuilder.append(value).append("\t")
                    }
                }
                if (cells.isNotEmpty()) {
                    rows.add(cells)
                    textBuilder.append("\n")
                }
            }

            workbook.close()

            ParseResult.Success(text = textBuilder.toString(), rows = rows)
        } catch (e: Exception) {
            ParseResult.Error("Old Excel format parsing failed: ${e.message}")
        }
    }

    private fun parseAsText(inputStream: InputStream): ParseResult {
        return try {
            val bytes = inputStream.readBytes()
            val text = String(bytes, Charsets.UTF_8)
                .replace(Regex("[^\\x20-\\x7E\\n\\t]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (text.length > 50) {
                ParseResult.Success(text = text, rows = emptyList())
            } else {
                ParseResult.Error("Could not extract text from Excel file")
            }
        } catch (e: Exception) {
            ParseResult.Error("Excel text extraction failed")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PDF PARSER (Using PdfBox-Android)
// ═══════════════════════════════════════════════════════════════════════════════

object PdfParser {

    private const val TAG = "PdfParser"

    sealed class ParseResult {
        data class Success(val text: String) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    /**
     * Parse PDF file using PdfBox-Android
     */
    fun parse(inputStream: InputStream): ParseResult {
        return try {
            parseWithPdfBox(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "PDF parse error: ${e.message}", e)
            ParseResult.Error("PDF parsing failed: ${e.message}")
        }
    }

    private fun parseWithPdfBox(inputStream: InputStream): ParseResult {
        return try {
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            val text = stripper.getText(document)
            document.close()

            Log.d(TAG, "PDF parsed: ${text.length} chars")

            if (text.isNotBlank()) {
                ParseResult.Success(text)
            } else {
                ParseResult.Error("PDF appears to be empty or contains only images")
            }
        } catch (e: Exception) {
            Log.e(TAG, "PdfBox parsing failed: ${e.message}", e)
            ParseResult.Error("PDF parsing failed: ${e.message}")
        }
    }
}