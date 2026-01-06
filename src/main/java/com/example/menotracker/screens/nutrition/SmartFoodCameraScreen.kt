// screens/nutrition/SmartFoodCameraScreen.kt
package com.example.menotracker.screens.nutrition

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.menotracker.data.models.MealType
import com.example.menotracker.data.models.NayaFood
import com.example.menotracker.data.repository.CommunityFoodRepository
import com.example.menotracker.data.repository.FoodInputRouter
import com.example.menotracker.data.repository.NutritionLabelParser
import com.example.menotracker.data.repository.OpenFoodFactsRepository
import com.example.menotracker.ui.theme.NayaPrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

private const val TAG = "SmartFoodCamera"

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * SMART FOOD CAMERA SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Unified camera with intelligent routing:
 * - Real-time detection of barcodes and nutrition labels
 * - Visual feedback showing what's detected
 * - Automatic routing to the most cost-effective processing method
 *
 * Flow:
 * 1. User points camera at food/product
 * 2. Real-time detection shows: "Barcode detected" / "Label detected" / "Ready for AI"
 * 3. User captures photo
 * 4. System routes to: OpenFoodFacts (free) / OCR Parser (free) / GPT-4V (paid)
 */

sealed class SmartCameraState {
    data object Scanning : SmartCameraState()
    data class BarcodeDetected(val code: String) : SmartCameraState()
    data object LabelDetected : SmartCameraState()
    data object Processing : SmartCameraState()
    data class ProductFound(val food: NayaFood, val source: String) : SmartCameraState()
    data class LabelParsed(val nutrition: NutritionLabelParser.ParsedNutrition, val bitmap: Bitmap) : SmartCameraState()
    data object ReadyForAI : SmartCameraState()
    data class BarcodeNotFound(val code: String) : SmartCameraState()  // Barcode not in database
    data class Error(val message: String) : SmartCameraState()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmartFoodCameraScreen(
    onNavigateBack: () -> Unit,
    onProductFound: (NayaFood) -> Unit,
    onNeedAIAnalysis: (File) -> Unit,
    onLabelNeedsReview: (Bitmap, NutritionLabelParser.ParsedNutrition) -> Unit,
    initialMealType: MealType = MealType.SNACK
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            SmartCameraContent(
                onNavigateBack = onNavigateBack,
                onProductFound = onProductFound,
                onNeedAIAnalysis = onNeedAIAnalysis,
                onLabelNeedsReview = onLabelNeedsReview,
                initialMealType = initialMealType
            )
        }
        cameraPermissionState.status.shouldShowRationale -> {
            PermissionRationaleContent(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onNavigateBack = onNavigateBack
            )
        }
        else -> {
            PermissionDeniedContent(onNavigateBack = onNavigateBack)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun SmartCameraContent(
    onNavigateBack: () -> Unit,
    onProductFound: (NayaFood) -> Unit,
    onNeedAIAnalysis: (File) -> Unit,
    onLabelNeedsReview: (Bitmap, NutritionLabelParser.ParsedNutrition) -> Unit,
    initialMealType: MealType
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var cameraState by remember { mutableStateOf<SmartCameraState>(SmartCameraState.Scanning) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Detection state for live preview
    var lastDetectedBarcode by remember { mutableStateOf<String?>(null) }
    var labelDetectedInPreview by remember { mutableStateOf(false) }
    var lastDetectionTime by remember { mutableLongStateOf(0L) }

    // Track what mode user is expecting based on live detection
    var expectedDetectionMode by remember { mutableStateOf<String?>(null) } // "barcode", "label", or null

    // Cooldown to prevent rapid re-detection
    val detectionCooldownMs = 500L

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Track if we came from BarcodeNotFound state (should only process labels)
    var wasInBarcodeNotFoundState by remember { mutableStateOf(false) }

    // Process captured image
    fun processCapture(file: File, captureExpectedMode: String?, forceLabel: Boolean = false) {
        cameraState = SmartCameraState.Processing

        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapWithRotation(file.absolutePath)
                }

                if (bitmap == null) {
                    cameraState = SmartCameraState.Error("Failed to load image")
                    return@launch
                }

                // Analyze the image
                val result = FoodInputRouter.analyzeImage(bitmap)
                Log.d(TAG, "📊 Analysis result: ${result.input}, expected mode: $captureExpectedMode, forceLabel: $forceLabel")

                when (val detection = result.input) {
                    is FoodInputRouter.DetectedInput.Barcode -> {
                        // If we came from BarcodeNotFound, skip barcode and treat as label/prepared food
                        if (forceLabel) {
                            Log.d(TAG, "🏷️ Ignoring barcode - user wants to scan label")
                            // Try to find nutrition label instead
                            val labelResult = FoodInputRouter.findNutritionLabel(bitmap)
                            if (labelResult != null) {
                                cameraState = SmartCameraState.LabelParsed(labelResult, bitmap)
                                onLabelNeedsReview(bitmap, labelResult)
                            } else {
                                // No label found - route to AI for prepared food analysis
                                Log.d(TAG, "🍽️ No label found - routing to AI")
                                cameraState = SmartCameraState.ReadyForAI
                                onNeedAIAnalysis(file)
                            }
                            return@launch
                        }
                        Log.d(TAG, "🔍 Processing barcode: ${detection.code}")
                        cameraState = SmartCameraState.BarcodeDetected(detection.code)

                        // Lookup in database (on IO thread)
                        val food = withContext(Dispatchers.IO) {
                            lookupBarcodeSync(detection.code)
                        }

                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            if (food != null) {
                                Log.d(TAG, "✅ Product found: ${food.name}")
                                cameraState = SmartCameraState.ProductFound(food, "Database")
                                onProductFound(food)
                            } else {
                                Log.d(TAG, "❌ Barcode not in database: ${detection.code}")
                                cameraState = SmartCameraState.BarcodeNotFound(detection.code)
                            }
                        }
                    }

                    is FoodInputRouter.DetectedInput.NutritionLabel -> {
                        Log.d(TAG, "📋 Processing nutrition label")
                        val parsed = detection.parsedNutrition
                            ?: NutritionLabelParser.parse(detection.rawText)

                        if (parsed.isValid) {
                            cameraState = SmartCameraState.LabelParsed(parsed, bitmap)
                            onLabelNeedsReview(bitmap, parsed)
                        } else {
                            // Label detected but couldn't parse - let user review
                            cameraState = SmartCameraState.LabelParsed(parsed, bitmap)
                            onLabelNeedsReview(bitmap, parsed)
                        }
                    }

                    is FoodInputRouter.DetectedInput.PreparedFood,
                    is FoodInputRouter.DetectedInput.Unknown -> {
                        // Check if user was expecting barcode/label but we didn't find it
                        when (captureExpectedMode) {
                            "barcode" -> {
                                Log.w(TAG, "⚠️ Live preview detected barcode but capture didn't find it")
                                cameraState = SmartCameraState.Error("Barcode not captured. Hold steady and try again.")
                            }
                            "label" -> {
                                Log.w(TAG, "⚠️ Live preview detected label but capture didn't find it")
                                cameraState = SmartCameraState.Error("Label not readable. Try better lighting or angle.")
                            }
                            else -> {
                                // User was in Scanning mode (no barcode/label detected in preview)
                                // This means they're photographing prepared food → route to AI
                                Log.d(TAG, "🍽️ Prepared food - routing to AI")
                                cameraState = SmartCameraState.ReadyForAI
                                onNeedAIAnalysis(file)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Processing error: ${e.message}", e)
                cameraState = SmartCameraState.Error("Processing failed: ${e.message}")
            }
        }
    }

    // Capture photo
    fun capturePhoto() {
        val capture = imageCapture ?: return

        // Capture the current expected mode before async capture
        val captureExpectedMode = expectedDetectionMode
        // Check if we're in BarcodeNotFound state - force label scanning
        val forceLabel = cameraState is SmartCameraState.BarcodeNotFound

        val photoFile = createImageFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "📸 Photo saved: ${photoFile.absolutePath}, expected: $captureExpectedMode, forceLabel: $forceLabel")
                    processCapture(photoFile, captureExpectedMode, forceLabel)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed: ${exception.message}")
                    cameraState = SmartCameraState.Error("Capture failed")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview with live detection
        AndroidView(
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

                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                    // Live detection analyzer
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val now = System.currentTimeMillis()
                                if (now - lastDetectionTime < detectionCooldownMs) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                FoodInputRouter.quickCheckFrame(imageProxy) { hasBarcode, hasLabel ->
                                    lastDetectionTime = now

                                    if (hasBarcode) {
                                        expectedDetectionMode = "barcode"
                                        if (cameraState is SmartCameraState.Scanning) {
                                            cameraState = SmartCameraState.BarcodeDetected("")
                                        }
                                    } else if (hasLabel) {
                                        expectedDetectionMode = "label"
                                        labelDetectedInPreview = true
                                        if (cameraState is SmartCameraState.Scanning) {
                                            cameraState = SmartCameraState.LabelDetected
                                        }
                                    } else {
                                        expectedDetectionMode = null
                                        labelDetectedInPreview = false
                                        // Don't reset if in a state that requires user action
                                        if (cameraState !is SmartCameraState.Processing &&
                                            cameraState !is SmartCameraState.ProductFound &&
                                            cameraState !is SmartCameraState.LabelParsed &&
                                            cameraState !is SmartCameraState.BarcodeNotFound &&
                                            cameraState !is SmartCameraState.Error) {
                                            cameraState = SmartCameraState.Scanning
                                        }
                                    }
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imgCapture,
                            imageAnalysis
                        )
                        imageCapture = imgCapture
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Detection overlay
        DetectionOverlay(cameraState = cameraState)

        // Top bar
        TopBar(
            onNavigateBack = onNavigateBack,
            isFlashOn = isFlashOn,
            onToggleFlash = {
                isFlashOn = !isFlashOn
                camera?.cameraControl?.enableTorch(isFlashOn)
            }
        )

        // Bottom controls
        BottomControls(
            cameraState = cameraState,
            onCapture = { capturePhoto() },
            onRetry = {
                cameraState = SmartCameraState.Scanning
                lastDetectedBarcode = null
                labelDetectedInPreview = false
            }
        )
    }
}

@Composable
private fun DetectionOverlay(cameraState: SmartCameraState) {
    val overlayColor = when (cameraState) {
        is SmartCameraState.BarcodeDetected -> Color(0xFF4CAF50)  // Green
        is SmartCameraState.LabelDetected -> Color(0xFF2196F3)    // Blue
        is SmartCameraState.Processing -> Color(0xFFFF9800)       // Orange
        is SmartCameraState.ProductFound -> Color(0xFF4CAF50)     // Green
        is SmartCameraState.LabelParsed -> Color(0xFF4CAF50)      // Green
        is SmartCameraState.BarcodeNotFound -> Color(0xFFFF9800)  // Orange - prompts action
        is SmartCameraState.Error -> Color(0xFFF44336)            // Red
        else -> Color.White
    }

    // Live detection feedback text
    val detectionInfo = when (cameraState) {
        is SmartCameraState.BarcodeDetected -> Triple("Barcode erkannt", Icons.Default.QrCodeScanner, Color(0xFF4CAF50))
        is SmartCameraState.LabelDetected -> Triple("Nährwert-Label erkannt", Icons.Default.Description, Color(0xFF2196F3))
        is SmartCameraState.Processing -> Triple("Wird analysiert...", Icons.Default.Sync, Color(0xFFFF9800))
        is SmartCameraState.Error -> Triple((cameraState as SmartCameraState.Error).message, Icons.Default.Error, Color(0xFFF44336))
        else -> null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle corner brackets
        // Top-left corner
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 120.dp)
        ) {
            CornerBracket(color = overlayColor, rotation = 0f)
        }
        // Top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp, top = 120.dp)
        ) {
            CornerBracket(color = overlayColor, rotation = 90f)
        }
        // Bottom-left corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 180.dp)
        ) {
            CornerBracket(color = overlayColor, rotation = 270f)
        }
        // Bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 180.dp)
        ) {
            CornerBracket(color = overlayColor, rotation = 180f)
        }

        // Live detection feedback badge - shows what camera sees in real-time
        AnimatedVisibility(
            visible = detectionInfo != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 150.dp)
        ) {
            detectionInfo?.let { (text, icon, color) ->
                Surface(
                    color = color,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = text,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Processing indicator in center
        if (cameraState is SmartCameraState.Processing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = overlayColor,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

@Composable
private fun CornerBracket(color: Color, rotation: Float) {
    val bracketSize = 40.dp
    val strokeWidth = 3.dp

    Box(
        modifier = Modifier
            .size(bracketSize)
            .graphicsLayer { rotationZ = rotation }
    ) {
        // Horizontal line
        Box(
            modifier = Modifier
                .width(bracketSize)
                .height(strokeWidth)
                .background(color)
        )
        // Vertical line
        Box(
            modifier = Modifier
                .width(strokeWidth)
                .height(bracketSize)
                .background(color)
        )
    }
}

@Composable
private fun TopBar(
    onNavigateBack: () -> Unit,
    isFlashOn: Boolean,
    onToggleFlash: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Smart Food Scanner",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Auto-detects barcode, label, or food",
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
                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Flash",
                tint = if (isFlashOn) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun BottomControls(
    cameraState: SmartCameraState,
    onCapture: () -> Unit,
    onRetry: () -> Unit
) {
    val canCapture = cameraState is SmartCameraState.Scanning ||
            cameraState is SmartCameraState.BarcodeDetected ||
            cameraState is SmartCameraState.LabelDetected ||
            cameraState is SmartCameraState.BarcodeNotFound  // Allow capture for label scan

    val showRetry = cameraState is SmartCameraState.Error
    val showBarcodeNotFound = cameraState is SmartCameraState.BarcodeNotFound

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barcode not found - prompt to scan label
        AnimatedVisibility(
            visible = showBarcodeNotFound,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Color(0xFFFF9800),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Produkt nicht in Datenbank",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Fotografiere das Nährwert-Label auf der Verpackung",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showBarcodeNotFound) {
            Spacer(Modifier.height(16.dp))
        }

        // Capture button (shows different icon for label scan mode)
        AnimatedVisibility(
            visible = canCapture,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Button(
                onClick = onCapture,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showBarcodeNotFound) Color(0xFF2196F3) else Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = if (showBarcodeNotFound) Icons.Default.Description else Icons.Default.CameraAlt,
                    contentDescription = if (showBarcodeNotFound) "Scan Label" else "Capture",
                    tint = if (showBarcodeNotFound) Color.White else Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Processing indicator
        AnimatedVisibility(
            visible = cameraState is SmartCameraState.Processing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = NayaPrimary,
                    strokeWidth = 4.dp
                )
                Text(
                    text = "Analyzing...",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Retry button
        AnimatedVisibility(
            visible = showRetry,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════

private suspend fun lookupBarcodeSync(barcode: String): NayaFood? {
    Log.d(TAG, "🔎 Looking up barcode: $barcode")

    // Try Community Foods first
    try {
        CommunityFoodRepository.getByBarcode(barcode).getOrNull()?.let { communityFood ->
            Log.d(TAG, "✅ Found in Community Foods: ${communityFood.name}")
            return CommunityFoodRepository.toNayaFood(communityFood)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Community Foods lookup failed: ${e.message}")
    }

    // Try OpenFoodFacts
    try {
        OpenFoodFactsRepository.searchBarcodeWithFallback(barcode).getOrNull()?.let { food ->
            Log.d(TAG, "✅ Found in OpenFoodFacts: ${food.name}")
            return food
        }
    } catch (e: Exception) {
        Log.w(TAG, "OpenFoodFacts lookup failed: ${e.message}")
    }

    Log.d(TAG, "❌ Barcode not found in any database")
    return null
}

private fun createImageFile(context: android.content.Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.filesDir, "smart_capture").apply {
        if (!exists()) mkdirs()
    }
    return File(storageDir, "SMART_${timeStamp}_${System.nanoTime()}.jpg")
}

private fun loadBitmapWithRotation(filePath: String): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        val bitmap = BitmapFactory.decodeFile(filePath, options) ?: return null

        val exif = ExifInterface(filePath)
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

        if (rotationDegrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        rotated
    } catch (e: Exception) {
        Log.e(TAG, "Bitmap load error: ${e.message}")
        null
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PERMISSION SCREENS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CameraAlt,
                null,
                modifier = Modifier.size(72.dp),
                tint = NayaPrimary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Camera Access Needed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "To scan food, barcodes, and nutrition labels, we need camera access.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text("Grant Camera Access")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onNavigateBack) { Text("Go Back") }
        }
    }
}

@Composable
private fun PermissionDeniedContent(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.NoPhotography,
                null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFFF44336)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Camera Access Denied",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Enable camera access in your device settings to use this feature.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onNavigateBack) { Text("Go Back") }
        }
    }
}