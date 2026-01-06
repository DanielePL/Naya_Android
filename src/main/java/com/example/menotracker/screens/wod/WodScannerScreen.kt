// screens/wod/WodScannerScreen.kt
package com.example.menotracker.screens.wod

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.menotracker.billing.Feature
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.billing.SubscriptionTier
import com.example.menotracker.data.NayaBackendRepository
import com.example.menotracker.data.models.*
import com.example.menotracker.data.repository.WodInputRouter
import com.example.menotracker.data.repository.WodTextParser
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "WodScanner"

/**
 * State machine for WOD Scanner flow - now with hybrid support
 */
sealed class WodScannerState {
    data object Camera : WodScannerState()
    data object Processing : WodScannerState()

    // Local OCR extraction result
    data class TextExtracted(
        val rawText: String,
        val confidence: Float,
        val bitmap: Bitmap?,
        val source: WodInputRouter.InputType
    ) : WodScannerState()

    // Local parsing result
    data class LocalParsed(
        val parsedWod: ParsedWod,
        val confidence: Float,
        val bitmap: Bitmap?,
        val rawText: String,
        val needsAIReview: Boolean
    ) : WodScannerState()

    // AI parsing result (fallback)
    data class AIParsed(
        val bitmap: Bitmap?,
        val parsedWod: ParsedWod,
        val confidence: Double
    ) : WodScannerState()

    data object Saving : WodScannerState()
    data class Success(
        val wodTemplateId: String,
        val wodName: String,
        val movementsSaved: Int
    ) : WodScannerState()
    data class Error(val message: String) : WodScannerState()
}

/**
 * Detection state for live preview
 */
data class LiveDetectionState(
    val hasWodText: Boolean = false,
    val confidence: Float = 0f
)

@OptIn(ExperimentalGetImage::class)
@Composable
fun WodScannerScreen(
    userId: String,
    boxName: String? = null,
    onNavigateBack: () -> Unit,
    onWodSaved: (String) -> Unit
) {
    // ═══════════════════════════════════════════════════════════════════════════
    // SUBSCRIPTION CHECK - WOD Scanner requires ELITE tier ($99/year)
    // ═══════════════════════════════════════════════════════════════════════════
    val hasAccess = SubscriptionManager.hasAccess(Feature.WOD_SCANNER)

    if (!hasAccess) {
        WodScannerPaywall(onNavigateBack = onNavigateBack)
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA PERMISSION
    // ═══════════════════════════════════════════════════════════════════════════
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var state by remember { mutableStateOf<WodScannerState>(WodScannerState.Camera) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Editable WOD data
    var editableWodName by remember { mutableStateOf("") }
    var editableBoxName by remember { mutableStateOf(boxName ?: "") }

    // Live detection state
    var liveDetection by remember { mutableStateOf(LiveDetectionState()) }
    var lastDetectionTime by remember { mutableLongStateOf(0L) }
    val detectionCooldownMs = 500L

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HYBRID PROCESSING FLOW
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fall back to Vision AI for parsing
     * NOTE: Must be defined before processWithHybrid and processFile since they call it
     */
    fun processWithAI(bitmap: Bitmap) {
        state = WodScannerState.Processing

        scope.launch {
            try {
                Log.d(TAG, "Using Vision AI for parsing...")

                val base64Image = withContext(Dispatchers.IO) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                }

                val result = NayaBackendRepository.scanWod(
                    imageBase64 = base64Image,
                    userId = userId,
                    boxName = editableBoxName.takeIf { it.isNotBlank() },
                    saveToDatabase = false
                )

                result.onSuccess { response ->
                    if (response.success && response.wod != null) {
                        editableWodName = response.wod.name
                        state = WodScannerState.AIParsed(
                            bitmap = bitmap,
                            parsedWod = response.wod,
                            confidence = response.confidence ?: 0.0
                        )
                    } else {
                        state = WodScannerState.Error(
                            response.error ?: "AI could not parse WOD"
                        )
                    }
                }.onFailure { error ->
                    state = WodScannerState.Error(error.message ?: "Network error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "AI processing error", e)
                state = WodScannerState.Error("AI processing failed: ${e.message}")
            }
        }
    }

    /**
     * Process captured bitmap with hybrid approach
     */
    fun processWithHybrid(bitmap: Bitmap) {
        state = WodScannerState.Processing

        scope.launch {
            try {
                // Step 1: Try local OCR extraction
                Log.d(TAG, "Step 1: Local OCR extraction...")
                val extractionResult = WodInputRouter.processBitmap(bitmap)

                when (extractionResult) {
                    is WodInputRouter.DetectionResult.TextExtracted -> {
                        Log.d(TAG, "  Text extracted: ${extractionResult.rawText.length} chars, confidence: ${extractionResult.confidence}")

                        // Step 2: Try local parsing if text quality is good
                        if (!extractionResult.recommendAI) {
                            Log.d(TAG, "Step 2: Trying local parsing...")
                            val parseResult = WodTextParser.parse(extractionResult.rawText)

                            if (parseResult.wod != null && parseResult.confidence >= 0.5f) {
                                Log.d(TAG, "  Local parse successful! Confidence: ${parseResult.confidence}")
                                editableWodName = parseResult.wod.name
                                state = WodScannerState.LocalParsed(
                                    parsedWod = parseResult.wod,
                                    confidence = parseResult.confidence,
                                    bitmap = bitmap,
                                    rawText = extractionResult.rawText,
                                    needsAIReview = parseResult.requiresAIReview
                                )
                                return@launch
                            }
                        }

                        // Local parsing failed or low confidence - show extracted text
                        Log.d(TAG, "  Local parsing insufficient, showing extracted text")
                        state = WodScannerState.TextExtracted(
                            rawText = extractionResult.rawText,
                            confidence = extractionResult.confidence,
                            bitmap = bitmap,
                            source = extractionResult.source
                        )
                    }

                    is WodInputRouter.DetectionResult.RequiresVisionAI -> {
                        // Go straight to AI
                        Log.d(TAG, "  Local extraction failed, using AI: ${extractionResult.reason}")
                        processWithAI(bitmap)
                    }

                    is WodInputRouter.DetectionResult.Error -> {
                        state = WodScannerState.Error(extractionResult.message)
                    }

                    is WodInputRouter.DetectionResult.FileParsed -> {
                        // This shouldn't happen for bitmaps, but handle it
                        val parseResult = WodTextParser.parse(extractionResult.rawText)
                        if (parseResult.wod != null) {
                            editableWodName = parseResult.wod.name
                            state = WodScannerState.LocalParsed(
                                parsedWod = parseResult.wod,
                                confidence = parseResult.confidence,
                                bitmap = null,
                                rawText = extractionResult.rawText,
                                needsAIReview = parseResult.requiresAIReview
                            )
                        } else {
                            state = WodScannerState.TextExtracted(
                                rawText = extractionResult.rawText,
                                confidence = 0.5f,
                                bitmap = null,
                                source = extractionResult.source
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Hybrid processing error", e)
                state = WodScannerState.Error("Processing failed: ${e.message}")
            }
        }
    }

    /**
     * Process file from gallery (image, Excel, PDF)
     */
    fun processFile(uri: Uri) {
        state = WodScannerState.Processing

        scope.launch {
            try {
                val inputType = WodInputRouter.detectInputType(uri, context)
                Log.d(TAG, "Processing file: $inputType")

                val result = WodInputRouter.processUri(uri, context)

                when (result) {
                    is WodInputRouter.DetectionResult.TextExtracted -> {
                        val parseResult = WodTextParser.parse(result.rawText)
                        if (parseResult.wod != null && parseResult.confidence >= 0.5f) {
                            editableWodName = parseResult.wod.name
                            state = WodScannerState.LocalParsed(
                                parsedWod = parseResult.wod,
                                confidence = parseResult.confidence,
                                bitmap = null,
                                rawText = result.rawText,
                                needsAIReview = parseResult.requiresAIReview
                            )
                        } else {
                            state = WodScannerState.TextExtracted(
                                rawText = result.rawText,
                                confidence = result.confidence,
                                bitmap = null,
                                source = result.source
                            )
                        }
                    }

                    is WodInputRouter.DetectionResult.FileParsed -> {
                        Log.d(TAG, "File parsed: ${result.rawText.length} chars")
                        val parseResult = WodTextParser.parse(result.rawText)

                        if (parseResult.wod != null && parseResult.confidence >= 0.4f) {
                            editableWodName = parseResult.wod.name
                            state = WodScannerState.LocalParsed(
                                parsedWod = parseResult.wod,
                                confidence = parseResult.confidence,
                                bitmap = null,
                                rawText = result.rawText,
                                needsAIReview = parseResult.requiresAIReview
                            )
                        } else {
                            state = WodScannerState.TextExtracted(
                                rawText = result.rawText,
                                confidence = 0.5f,
                                bitmap = null,
                                source = result.source
                            )
                        }
                    }

                    is WodInputRouter.DetectionResult.RequiresVisionAI -> {
                        // Load bitmap and use AI
                        val bitmap = result.bitmap
                        if (bitmap != null) {
                            processWithAI(bitmap)
                        } else {
                            state = WodScannerState.Error("Could not process file")
                        }
                    }

                    is WodInputRouter.DetectionResult.Error -> {
                        state = WodScannerState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "File processing error", e)
                state = WodScannerState.Error("File processing failed: ${e.message}")
            }
        }
    }

    /**
     * Capture photo from camera
     */
    fun captureAndProcess() {
        val capture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture is null")
            state = WodScannerState.Error("Camera not ready")
            return
        }
        state = WodScannerState.Processing
        Log.d(TAG, "Starting capture...")

        val photoFile = File(context.cacheDir, "wod_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Image saved to: ${photoFile.absolutePath}")
                    scope.launch {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                // Load with sample size for memory efficiency
                                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                                val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
                                    ?: return@withContext null

                                // Read EXIF and rotate if needed
                                try {
                                    val exif = androidx.exifinterface.media.ExifInterface(photoFile.absolutePath)
                                    val orientation = exif.getAttributeInt(
                                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                                    )
                                    val rotation = when (orientation) {
                                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                                        else -> 0f
                                    }
                                    if (rotation != 0f) {
                                        val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
                                        android.graphics.Bitmap.createBitmap(
                                            originalBitmap, 0, 0,
                                            originalBitmap.width, originalBitmap.height,
                                            matrix, true
                                        )
                                    } else {
                                        originalBitmap
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "EXIF rotation failed: ${e.message}")
                                    originalBitmap
                                }
                            }

                            if (bitmap == null) {
                                Log.e(TAG, "Failed to decode bitmap")
                                state = WodScannerState.Error("Failed to load image")
                                return@launch
                            }

                            Log.d(TAG, "Bitmap loaded: ${bitmap.width}x${bitmap.height}")

                            // Use hybrid processing
                            processWithHybrid(bitmap)

                            // Cleanup
                            withContext(Dispatchers.IO) { photoFile.delete() }
                        } catch (e: Exception) {
                            Log.e(TAG, "Capture processing error", e)
                            state = WodScannerState.Error("Processing failed: ${e.message}")
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture error: ${exception.message}", exception)
                    state = WodScannerState.Error("Capture failed: ${exception.message}")
                }
            }
        )
    }

    /**
     * Save the parsed WOD to database
     */
    fun saveWod(parsedWod: ParsedWod, bitmap: Bitmap?) {
        state = WodScannerState.Saving

        scope.launch {
            try {
                val base64Image = if (bitmap != null) {
                    withContext(Dispatchers.IO) {
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    }
                } else null

                val result = NayaBackendRepository.scanWod(
                    imageBase64 = base64Image ?: "",
                    userId = userId,
                    boxName = editableBoxName.takeIf { it.isNotBlank() },
                    saveToDatabase = true
                )

                result.onSuccess { response ->
                    if (response.success && response.wodTemplateId != null) {
                        state = WodScannerState.Success(
                            wodTemplateId = response.wodTemplateId,
                            wodName = editableWodName,
                            movementsSaved = response.movementsSaved ?: 0
                        )
                    } else {
                        state = WodScannerState.Error(
                            response.saveError ?: response.error ?: "Failed to save WOD"
                        )
                    }
                }.onFailure { error ->
                    state = WodScannerState.Error(error.message ?: "Network error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Save error", e)
                state = WodScannerState.Error("Save failed: ${e.message}")
            }
        }
    }

    // File picker for gallery, Excel, PDF
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processFile(it) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════════════════════

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is WodScannerState.Camera -> {
                if (hasCameraPermission) {
                    // Camera Preview
                    WodCameraPreview(
                        lifecycleOwner = lifecycleOwner,
                        isFlashOn = isFlashOn,
                        onCameraReady = { cam, capture ->
                            camera = cam
                            imageCapture = capture
                        },
                        onLiveDetection = { hasWod, confidence ->
                            val now = System.currentTimeMillis()
                            if (now - lastDetectionTime > detectionCooldownMs) {
                                lastDetectionTime = now
                                liveDetection = LiveDetectionState(hasWod, confidence)
                            }
                        },
                        cameraExecutor = cameraExecutor
                    )

                    // Overlay
                    WodCameraOverlay(
                        boxName = editableBoxName,
                        onBoxNameChange = { editableBoxName = it },
                        isFlashOn = isFlashOn,
                        onToggleFlash = {
                            isFlashOn = !isFlashOn
                            camera?.cameraControl?.enableTorch(isFlashOn)
                        },
                        onCapture = { captureAndProcess() },
                        onGallery = { fileLauncher.launch("*/*") },
                        onBack = onNavigateBack,
                        liveDetection = liveDetection
                    )
                } else {
                    // Permission denied - show request UI
                    CameraPermissionContent(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onBack = onNavigateBack,
                        onImportFile = { fileLauncher.launch("*/*") }
                    )
                }
            }

            is WodScannerState.Processing -> {
                ProcessingContent()
            }

            is WodScannerState.TextExtracted -> {
                TextExtractedContent(
                    rawText = currentState.rawText,
                    confidence = currentState.confidence,
                    source = currentState.source,
                    bitmap = currentState.bitmap,
                    onRetry = { state = WodScannerState.Camera },
                    onUseAI = {
                        currentState.bitmap?.let { processWithAI(it) }
                            ?: run { state = WodScannerState.Error("No image available for AI") }
                    },
                    onBack = onNavigateBack
                )
            }

            is WodScannerState.LocalParsed -> {
                WodPreviewContent(
                    bitmap = currentState.bitmap,
                    parsedWod = currentState.parsedWod,
                    confidence = currentState.confidence.toDouble(),
                    wodName = editableWodName,
                    onWodNameChange = { editableWodName = it },
                    onRetake = { state = WodScannerState.Camera },
                    onSave = { saveWod(currentState.parsedWod, currentState.bitmap) },
                    onBack = onNavigateBack,
                    isLocalParse = true,
                    needsAIReview = currentState.needsAIReview,
                    onUseAI = {
                        currentState.bitmap?.let { processWithAI(it) }
                    }
                )
            }

            is WodScannerState.AIParsed -> {
                WodPreviewContent(
                    bitmap = currentState.bitmap,
                    parsedWod = currentState.parsedWod,
                    confidence = currentState.confidence,
                    wodName = editableWodName,
                    onWodNameChange = { editableWodName = it },
                    onRetake = { state = WodScannerState.Camera },
                    onSave = { saveWod(currentState.parsedWod, currentState.bitmap) },
                    onBack = onNavigateBack,
                    isLocalParse = false,
                    needsAIReview = false,
                    onUseAI = null
                )
            }

            is WodScannerState.Saving -> {
                SavingContent()
            }

            is WodScannerState.Success -> {
                SuccessContent(
                    wodName = currentState.wodName,
                    movementsSaved = currentState.movementsSaved,
                    onDone = { onWodSaved(currentState.wodTemplateId) }
                )
            }

            is WodScannerState.Error -> {
                ErrorContent(
                    message = currentState.message,
                    onRetry = { state = WodScannerState.Camera },
                    onBack = onNavigateBack
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW WITH LIVE DETECTION
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalGetImage::class)
@Composable
private fun WodCameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    isFlashOn: Boolean,
    onCameraReady: (Camera, ImageCapture) -> Unit,
    onLiveDetection: (Boolean, Float) -> Unit,
    cameraExecutor: java.util.concurrent.ExecutorService
) {
    val context = LocalContext.current

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // Live detection analyzer
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            WodInputRouter.quickCheckFrame(imageProxy) { hasWod, confidence ->
                                onLiveDetection(hasWod, confidence)
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                    onCameraReady(camera, imageCapture)
                } catch (e: Exception) {
                    Log.e(TAG, "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// CAMERA OVERLAY WITH DETECTION STATUS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WodCameraOverlay(
    boxName: String,
    onBoxNameChange: (String) -> Unit,
    isFlashOn: Boolean,
    onToggleFlash: () -> Unit,
    onCapture: () -> Unit,
    onGallery: () -> Unit,
    onBack: () -> Unit,
    liveDetection: LiveDetectionState
) {
    val detectionColor = when {
        liveDetection.hasWodText && liveDetection.confidence > 0.6f -> Color(0xFF4CAF50)
        liveDetection.hasWodText -> Color(0xFFFF9800)
        else -> Color.White
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle gradient overlay - just for readability of controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Smart WOD Scanner",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Photo, Excel, or PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = onToggleFlash,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isFlashOn) Color(0xFFFFC107) else Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    "Flash",
                    tint = if (isFlashOn) Color.Black else Color.White
                )
            }
        }

        // FULL SCREEN - no detection frame, just a subtle indicator when text is detected

        // Detection status badge - positioned near center
        AnimatedVisibility(
            visible = liveDetection.hasWodText,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Surface(
                color = detectionColor.copy(alpha = 0.9f),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.TextFields,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "WOD Detected - Tap to Scan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Box name input
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                OutlinedTextField(
                    value = boxName,
                    onValueChange = onBoxNameChange,
                    label = { Text("Box Name (optional)", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("e.g., CrossFit Berlin", color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NayaPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = NayaPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Capture and Gallery buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery/File button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onGallery,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            "Import File",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Import",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // Capture button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onCapture,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            "Capture",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Scan",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // Placeholder for balance
                Spacer(modifier = Modifier.size(56.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hint text
            Text(
                if (liveDetection.hasWodText) "Tap to scan (Free local processing)"
                else "Supports: Photos, Excel, PDF",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TEXT EXTRACTED STATE (shows raw OCR result)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TextExtractedContent(
    rawText: String,
    confidence: Float,
    source: WodInputRouter.InputType,
    bitmap: Bitmap?,
    onRetry: () -> Unit,
    onUseAI: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Text Extracted",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Retry")
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = NayaPrimary.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when (source) {
                            WodInputRouter.InputType.EXCEL_FILE -> Icons.Default.TableChart
                            WodInputRouter.InputType.PDF_FILE -> Icons.Default.PictureAsPdf
                            else -> Icons.Default.Image
                        },
                        null,
                        tint = NayaPrimary
                    )
                    Text(
                        "Source: ${source.name.replace("_", " ")}",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Confidence: ${(confidence * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            // Raw text
            Text(
                "Extracted Text:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = rawText,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // Warning
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = Color(0xFFFF9800)
                    )
                    Text(
                        "Local parsing couldn't fully interpret this WOD. Use AI for better results.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Bottom action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onUseAI,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Use AI")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WOD PREVIEW (Local or AI parsed)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WodPreviewContent(
    bitmap: Bitmap?,
    parsedWod: ParsedWod,
    confidence: Double,
    wodName: String,
    onWodNameChange: (String) -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    isLocalParse: Boolean,
    needsAIReview: Boolean,
    onUseAI: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Review WOD",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onRetake) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Retake")
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image preview
            bitmap?.let { bmp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured WOD",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            // Parsing method indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isLocalParse) Icons.Default.OfflinePin else Icons.Default.Cloud,
                        null,
                        tint = if (isLocalParse) Color(0xFF4CAF50) else Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        if (isLocalParse) "Parsed locally (Free)" else "Parsed with AI",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                val confidencePercent = (confidence * 100).toInt()
                val color = when {
                    confidencePercent >= 80 -> Color(0xFF4CAF50)
                    confidencePercent >= 50 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
                Text(
                    "${confidencePercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            // AI Review suggestion
            if (needsAIReview && onUseAI != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    onClick = onUseAI
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFF9800))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Low confidence - AI recommended",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                "Tap to use AI for better accuracy",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }

            HorizontalDivider()

            // WOD Name (editable)
            OutlinedTextField(
                value = wodName,
                onValueChange = onWodNameChange,
                label = { Text("WOD Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // WOD Type and Time Cap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WodInfoChip(
                    icon = Icons.Default.Timer,
                    label = parsedWod.wodType.uppercase(),
                    modifier = Modifier.weight(1f)
                )
                if (parsedWod.timeCapSeconds != null) {
                    val minutes = parsedWod.timeCapSeconds / 60
                    WodInfoChip(
                        icon = Icons.Default.Schedule,
                        label = "$minutes min cap",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (parsedWod.targetRounds != null) {
                    WodInfoChip(
                        icon = Icons.Default.Repeat,
                        label = "${parsedWod.targetRounds} rounds",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Rep scheme
            parsedWod.repScheme?.let { scheme ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NayaPrimary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Numbers, null, tint = NayaPrimary)
                        Text(
                            "Rep Scheme: ${scheme.joinToString("-")}",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider()

            // Movements
            Text(
                "Movements (${parsedWod.movements.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            parsedWod.movements.forEachIndexed { index, movement ->
                MovementCard(index = index + 1, movement = movement)
            }

            // Equipment
            parsedWod.equipmentNeeded?.takeIf { it.isNotEmpty() }?.let { equipment ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Equipment Needed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(equipment.joinToString(", "))
                    }
                }
            }
        }

        // Bottom actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save WOD")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WodInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NayaSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = NayaPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MovementCard(index: Int, movement: ParsedMovement) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NayaSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NayaPrimary,
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("$index", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    movement.movementName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val workDescription = buildString {
                    when (movement.repType) {
                        "calories" -> append("${movement.calories ?: 0} cal")
                        "distance" -> append("${movement.distanceMeters ?: 0}m")
                        "time" -> {
                            val secs = movement.timeSeconds ?: 0
                            if (secs >= 60) append("${secs / 60} min") else append("$secs sec")
                        }
                        "max" -> append("Max reps")
                        else -> append("${movement.reps ?: "?"} reps")
                    }

                    if (movement.weightType != "bodyweight" && movement.weightKgMale != null) {
                        if (movement.weightKgFemale != null) {
                            append(" @ ${movement.weightKgMale.toInt()}/${movement.weightKgFemale.toInt()}kg")
                        } else {
                            append(" @ ${movement.weightKgMale.toInt()}kg")
                        }
                    }
                }

                Text(
                    workDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ProcessingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = NayaPrimary
            )
            Text(
                "Processing WOD...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Trying local extraction first (free)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SavingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp), color = NayaPrimary)
            Text("Saving WOD...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SuccessContent(wodName: String, movementsSaved: Int, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)
                )
            }

            Text("WOD Saved!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("\"$wodName\" with $movementsSaved movements", textAlign = TextAlign.Center)

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
            ) {
                Text("View WOD")
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Error, null, modifier = Modifier.size(72.dp), tint = Color(0xFFF44336))
            Text("Scan Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message, textAlign = TextAlign.Center)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Cancel") }
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CAMERA PERMISSION CONTENT
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CameraPermissionContent(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
    onImportFile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Camera icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(NayaPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    null,
                    modifier = Modifier.size(56.dp),
                    tint = NayaPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "To scan WODs from whiteboards, we need access to your camera. You can also import files without camera access.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Allow Camera Access", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onImportFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import from Files", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PAYWALL - WOD Scanner requires ELITE subscription
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WodScannerPaywall(onNavigateBack: () -> Unit) {
    val currentTier by SubscriptionManager.currentTier.collectAsState()
    val trialDaysRemaining = SubscriptionManager.getRemainingTrialDays()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Feature icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(NayaPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = NayaPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                "Smart WOD Scanner",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                "Elite Feature",
                style = MaterialTheme.typography.titleMedium,
                color = NayaPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Features list
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PaywallFeatureItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Photo Scanning",
                        description = "Snap a photo of any whiteboard WOD"
                    )
                    PaywallFeatureItem(
                        icon = Icons.Default.TableChart,
                        title = "Excel Import",
                        description = "Import workouts from Excel files"
                    )
                    PaywallFeatureItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "PDF Support",
                        description = "Parse PDFs from influencer programs"
                    )
                    PaywallFeatureItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI-Powered",
                        description = "Smart detection with AI fallback"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Current tier info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Current Plan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            currentTier.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upgrade button
            Button(
                onClick = { /* TODO: Navigate to paywall/subscription screen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Upgrade to Elite - $99/year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Includes AI Coach, Physical Coach & WOD Scanner",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PaywallFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NayaPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = NayaPrimary, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
