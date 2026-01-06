// screens/nutrition/BarcodeScannerScreen.kt
package com.example.menotracker.screens.nutrition

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.example.menotracker.data.models.MealType
import com.example.menotracker.data.models.NayaFood
import com.example.menotracker.data.repository.CommunityFoodRepository
import com.example.menotracker.data.repository.OpenFoodFactsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * BARCODE SCANNER SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Real-time barcode scanner using ML Kit and CameraX
 * - Scans EAN-13, UPC-A, QR codes, and more
 * - Automatic product lookup from Open Food Facts + USDA
 * - Quick-add to current meal
 *
 * Required dependencies in build.gradle:
 * - implementation("com.google.mlkit:barcode-scanning:17.3.0")
 * - implementation("com.google.accompanist:accompanist-permissions:0.34.0")
 * ═══════════════════════════════════════════════════════════════════════════════
 */

private const val TAG = "BarcodeScanner"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen(
    onNavigateBack: () -> Unit,
    onProductFound: (NayaFood) -> Unit,
    onAddToMeal: (NayaFood, Float, MealType) -> Unit,
    onAddAndContinue: ((NayaFood, Float, MealType) -> Unit)? = null, // For multi-ingredient flow
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
            BarcodeScannerContent(
                onNavigateBack = onNavigateBack,
                onProductFound = onProductFound,
                onAddToMeal = onAddToMeal,
                onAddAndContinue = onAddAndContinue,
                initialMealType = initialMealType
            )
        }
        cameraPermissionState.status.shouldShowRationale -> {
            PermissionRationaleScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onNavigateBack = onNavigateBack
            )
        }
        else -> {
            PermissionDeniedScreen(onNavigateBack = onNavigateBack)
        }
    }
}

@Composable
private fun BarcodeScannerContent(
    onNavigateBack: () -> Unit,
    onProductFound: (NayaFood) -> Unit,
    onAddToMeal: (NayaFood, Float, MealType) -> Unit,
    onAddAndContinue: ((NayaFood, Float, MealType) -> Unit)?,
    initialMealType: MealType
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Scanner state
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Scanning) }
    var foundProduct by remember { mutableStateOf<NayaFood?>(null) }
    var portionGrams by remember { mutableFloatStateOf(100f) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var selectedMealType by remember { mutableStateOf(initialMealType) }

    // Cooldown to prevent rapid re-scanning
    var canScan by remember { mutableStateOf(true) }

    // Barcode scanner
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
            cameraExecutor.shutdown()
        }
    }

    // Handle barcode detection
    fun onBarcodeDetected(barcode: String) {
        // Only process if we're actively scanning (not showing a result already)
        if (!canScan || barcode == lastScannedBarcode || scannerState !is ScannerState.Scanning) return

        canScan = false
        lastScannedBarcode = barcode
        scannerState = ScannerState.Loading(barcode)

        scope.launch {
            Log.d(TAG, "Looking up barcode: $barcode")

            // Step 1: Check Community Foods first (crowdsourced data)
            val communityResult = CommunityFoodRepository.getByBarcode(barcode)
            communityResult.getOrNull()?.let { communityFood ->
                Log.d(TAG, "Found in Community Foods: ${communityFood.name}")
                val food = CommunityFoodRepository.toNayaFood(communityFood)
                foundProduct = food
                portionGrams = food.servingSize ?: 100f
                scannerState = ScannerState.Found(food)
                onProductFound(food)
                delay(3000)
                canScan = true
                return@launch
            }

            // Step 2: Search with fallback (OFF → USDA)
            val result = OpenFoodFactsRepository.searchBarcodeWithFallback(barcode)

            result.onSuccess { food ->
                if (food != null) {
                    Log.d(TAG, "Found product: ${food.name}")
                    foundProduct = food
                    portionGrams = food.servingSize ?: 100f
                    scannerState = ScannerState.Found(food)
                    onProductFound(food)
                } else {
                    Log.d(TAG, "Product not found for barcode: $barcode")
                    scannerState = ScannerState.NotFound(barcode)
                }
            }.onFailure { error ->
                Log.e(TAG, "Lookup error", error)
                scannerState = ScannerState.Error(error.message ?: "Unknown error")
            }

            // Allow re-scanning after delay
            delay(3000)
            canScan = true
        }
    }

    // Show LabelCaptureScreen when in LabelCapture mode
    if (scannerState is ScannerState.LabelCapture) {
        LabelCaptureScreen(
            barcode = (scannerState as ScannerState.LabelCapture).barcode,
            onNavigateBack = {
                // Go back to scanning mode
                scannerState = ScannerState.Scanning
                lastScannedBarcode = null
                canScan = true
            },
            onSuccess = { food ->
                // Product was saved successfully - show it as found
                foundProduct = food
                portionGrams = food.servingSize ?: 100f
                scannerState = ScannerState.Found(food)
                onProductFound(food)
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
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

                    // Preview use case
                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    // Image analysis for barcode scanning
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImage(imageProxy, barcodeScanner) { barcode ->
                                    onBarcodeDetected(barcode)
                                }
                            }
                        }

                    // Select back camera
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Scanning overlay
        ScannerOverlay(
            scannerState = scannerState,
            isFlashOn = isFlashOn
        )

        // Top bar
        TopBar(
            onNavigateBack = onNavigateBack,
            isFlashOn = isFlashOn,
            onToggleFlash = {
                isFlashOn = !isFlashOn
                camera?.cameraControl?.enableTorch(isFlashOn)
            }
        )

        // Bottom result card
        AnimatedVisibility(
            visible = scannerState is ScannerState.Found ||
                    scannerState is ScannerState.NotFound ||
                    scannerState is ScannerState.Loading,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            ResultCard(
                scannerState = scannerState,
                foundProduct = foundProduct,
                portionGrams = portionGrams,
                onPortionChange = { portionGrams = it },
                selectedMealType = selectedMealType,
                onMealTypeChange = { selectedMealType = it },
                onAddToMeal = { food, grams ->
                    onAddToMeal(food, grams, selectedMealType)
                    // Reset for next scan
                    scannerState = ScannerState.Scanning
                    foundProduct = null
                    lastScannedBarcode = null
                },
                onAddAndContinue = if (onAddAndContinue != null) { food, grams ->
                    onAddAndContinue(food, grams, selectedMealType)
                    // Reset for next scan but stay on screen
                    scannerState = ScannerState.Scanning
                    foundProduct = null
                    lastScannedBarcode = null
                    canScan = true
                } else null,
                onDismiss = {
                    scannerState = ScannerState.Scanning
                    foundProduct = null
                    lastScannedBarcode = null
                    canScan = true
                },
                onScanLabel = { barcode ->
                    scannerState = ScannerState.LabelCapture(barcode)
                }
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImage(
    imageProxy: ImageProxy,
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                // Get the first valid barcode
                barcodes.firstOrNull { barcode ->
                    barcode.valueType == Barcode.TYPE_PRODUCT ||
                            barcode.valueType == Barcode.TYPE_ISBN ||
                            barcode.valueType == Barcode.TYPE_TEXT ||
                            barcode.format == Barcode.FORMAT_EAN_13 ||
                            barcode.format == Barcode.FORMAT_EAN_8 ||
                            barcode.format == Barcode.FORMAT_UPC_A ||
                            barcode.format == Barcode.FORMAT_UPC_E
                }?.rawValue?.let { value ->
                    onBarcodeDetected(value)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// SCANNER OVERLAY
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScannerOverlay(
    scannerState: ScannerState,
    isFlashOn: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Darkened edges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        radius = 800f
                    )
                )
        )

        // Scanning frame - positioned higher to avoid being covered by result panel
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 120.dp), // Push frame towards top
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Scan frame
            Box(
                modifier = Modifier
                    .size(280.dp, 180.dp)
                    .border(
                        width = 3.dp,
                        color = when (scannerState) {
                            is ScannerState.Found -> Color(0xFF4CAF50)
                            is ScannerState.NotFound -> Color(0xFFFF9800)
                            is ScannerState.Error -> Color(0xFFF44336)
                            is ScannerState.Loading -> Color(0xFF2196F3)
                            else -> Color.White
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                // Corner accents
                CornerAccents(
                    color = when (scannerState) {
                        is ScannerState.Found -> Color(0xFF4CAF50)
                        is ScannerState.NotFound -> Color(0xFFFF9800)
                        is ScannerState.Error -> Color(0xFFF44336)
                        is ScannerState.Loading -> Color(0xFF2196F3)
                        else -> Color.White
                    }
                )

                // Loading indicator
                if (scannerState is ScannerState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF2196F3)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status text
            Text(
                text = when (scannerState) {
                    is ScannerState.Scanning -> "Position barcode in frame"
                    is ScannerState.Loading -> "Looking up ${scannerState.barcode}..."
                    is ScannerState.Found -> "Product found!"
                    is ScannerState.NotFound -> "Product not in database"
                    is ScannerState.LabelCapture -> "Scanning label..."
                    is ScannerState.Error -> "Error: ${scannerState.message}"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            if (scannerState is ScannerState.Scanning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Supports EAN-13, UPC-A, and QR codes",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CornerAccents(color: Color) {
    val cornerSize = 24.dp
    val strokeWidth = 4.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(cornerSize)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(strokeWidth)
                    .background(color)
            )
        }

        // Top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(cornerSize)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(strokeWidth)
                    .background(color)
            )
        }

        // Bottom-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(cornerSize)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(strokeWidth)
                    .background(color)
            )
        }

        // Bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(cornerSize)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .height(strokeWidth)
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxHeight()
                    .width(strokeWidth)
                    .background(color)
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════════════════════════════

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
        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(48.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Title
        Text(
            text = "Scan Barcode",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // Flash toggle
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
                contentDescription = "Toggle Flash",
                tint = if (isFlashOn) Color.Black else Color.White
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// RESULT CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultCard(
    scannerState: ScannerState,
    foundProduct: NayaFood?,
    portionGrams: Float,
    onPortionChange: (Float) -> Unit,
    selectedMealType: MealType,
    onMealTypeChange: (MealType) -> Unit,
    onAddToMeal: (NayaFood, Float) -> Unit,
    onAddAndContinue: ((NayaFood, Float) -> Unit)?,
    onDismiss: () -> Unit,
    onScanLabel: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        when (scannerState) {
            is ScannerState.Loading -> {
                LoadingContent(barcode = scannerState.barcode)
            }
            is ScannerState.Found -> {
                foundProduct?.let { food ->
                    FoundProductContent(
                        food = food,
                        portionGrams = portionGrams,
                        onPortionChange = onPortionChange,
                        selectedMealType = selectedMealType,
                        onMealTypeChange = onMealTypeChange,
                        onAddToMeal = { onAddToMeal(food, portionGrams) },
                        onAddAndContinue = if (onAddAndContinue != null) {
                            { onAddAndContinue(food, portionGrams) }
                        } else null,
                        onDismiss = onDismiss
                    )
                }
            }
            is ScannerState.NotFound -> {
                NotFoundContent(
                    barcode = scannerState.barcode,
                    onDismiss = onDismiss,
                    onScanLabel = { onScanLabel(scannerState.barcode) }
                )
            }
            is ScannerState.Error -> {
                ErrorContent(
                    message = scannerState.message,
                    onDismiss = onDismiss
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun LoadingContent(barcode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
        Column {
            Text(
                text = "Looking up product...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Barcode: $barcode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun FoundProductContent(
    food: NayaFood,
    portionGrams: Float,
    onPortionChange: (Float) -> Unit,
    selectedMealType: MealType,
    onMealTypeChange: (MealType) -> Unit,
    onAddToMeal: () -> Unit,
    onAddAndContinue: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    // Calculate scaled nutrition
    val multiplier = portionGrams / 100f
    val scaledCalories = food.calories * multiplier
    val scaledProtein = food.protein * multiplier
    val scaledCarbs = food.carbs * multiplier
    val scaledFat = food.fat * multiplier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Product header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                food.brand?.let { brand ->
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Nutri-Score badge
            food.nutriscoreGrade?.let { grade ->
                NutriScoreBadge(grade = grade)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Meal Type Dropdown Selector
        MealTypeDropdown(
            selectedMealType = selectedMealType,
            onMealTypeChange = onMealTypeChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Portion selector
        Column {
            Text(
                text = "Portion Size",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick portion buttons
                val servingGrams = food.servingSize ?: 100f
                val servingLabel = food.servingDescription ?: "${servingGrams.toInt()}g"
                listOf(
                    "50g" to 50f,
                    "100g" to 100f,
                    servingLabel to servingGrams
                ).distinctBy { it.second }.forEach { (label, grams) ->
                    FilterChip(
                        selected = portionGrams == grams,
                        onClick = { onPortionChange(grams) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom slider
            Slider(
                value = portionGrams,
                onValueChange = onPortionChange,
                valueRange = 10f..500f,
                steps = 48,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${portionGrams.toInt()}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Macros display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroItem(
                label = "Calories",
                value = "${scaledCalories.toInt()}",
                unit = "kcal",
                color = Color(0xFFFF6B6B)
            )
            MacroItem(
                label = "Protein",
                value = String.format("%.1f", scaledProtein),
                unit = "g",
                color = Color(0xFF4ECDC4)
            )
            MacroItem(
                label = "Carbs",
                value = String.format("%.1f", scaledCarbs),
                unit = "g",
                color = Color(0xFFFFE66D)
            )
            MacroItem(
                label = "Fat",
                value = String.format("%.1f", scaledFat),
                unit = "g",
                color = Color(0xFF95E1D3)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Scan Another")
            }

            Button(
                onClick = onAddToMeal,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = getMealTypeColor(selectedMealType)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Log ${selectedMealType.displayName}",
                    maxLines = 1
                )
            }
        }

        // Multi-ingredient "Log & Scan More" button
        if (onAddAndContinue != null) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onAddAndContinue,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF2196F3)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log & Scan More")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MEAL TYPE DROPDOWN
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MealTypeDropdown(
    selectedMealType: MealType,
    onMealTypeChange: (MealType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Add to",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box {
            // Main button showing current selection
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(12.dp),
                color = getMealTypeColor(selectedMealType).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    getMealTypeColor(selectedMealType).copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Icon
                        Icon(
                            imageVector = getMealTypeIcon(selectedMealType),
                            contentDescription = null,
                            tint = getMealTypeColor(selectedMealType),
                            modifier = Modifier.size(24.dp)
                        )
                        // Text
                        Text(
                            text = selectedMealType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = getMealTypeColor(selectedMealType)
                        )
                    }

                    // Dropdown arrow
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select meal",
                        tint = getMealTypeColor(selectedMealType)
                    )
                }
            }

            // Dropdown menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                MealType.entries.forEach { mealType ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = getMealTypeIcon(mealType),
                                    contentDescription = null,
                                    tint = getMealTypeColor(mealType),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = mealType.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (mealType == selectedMealType) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onMealTypeChange(mealType)
                            expanded = false
                        },
                        leadingIcon = null,
                        trailingIcon = if (mealType == selectedMealType) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = getMealTypeColor(mealType)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * Get icon for meal type - using standard Material Icons
 */
private fun getMealTypeIcon(mealType: MealType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (mealType) {
        MealType.BREAKFAST -> Icons.Default.WbSunny          // Morning sun
        MealType.LUNCH -> Icons.Default.Restaurant           // Restaurant/fork-knife
        MealType.DINNER -> Icons.Default.Nightlight          // Night/moon
        MealType.SNACK -> Icons.Default.Fastfood             // Fast food/snack
        MealType.SHAKE -> Icons.Default.LocalDrink           // Drink
    }
}

/**
 * Get color for meal type
 */
private fun getMealTypeColor(mealType: MealType): Color {
    return when (mealType) {
        MealType.BREAKFAST -> Color(0xFFFF9800) // Orange
        MealType.LUNCH -> Color(0xFF4CAF50)     // Green
        MealType.DINNER -> Color(0xFF673AB7)    // Purple
        MealType.SNACK -> Color(0xFFE91E63)     // Pink
        MealType.SHAKE -> Color(0xFF2196F3)     // Blue
    }
}

@Composable
private fun MacroItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun NutriScoreBadge(grade: String) {
    val (bgColor, textColor) = when (grade.uppercase()) {
        "A" -> Color(0xFF038141) to Color.White
        "B" -> Color(0xFF85BB2F) to Color.Black
        "C" -> Color(0xFFFECB02) to Color.Black
        "D" -> Color(0xFFEE8100) to Color.White
        "E" -> Color(0xFFE63E11) to Color.White
        else -> Color.Gray to Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = grade.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun NotFoundContent(
    barcode: String,
    onDismiss: () -> Unit,
    onScanLabel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFF9800)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Product Not Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Barcode $barcode isn't in our database yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Community contribution card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Help the Community!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scan the nutrition label and we'll add it to our database for everyone.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary action: Scan Label
        Button(
            onClick = onScanLabel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Nutrition Label")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary action
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Another Barcode")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFF44336)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onDismiss) {
            Text("Try Again")
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// PERMISSION SCREENS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PermissionRationaleScreen(
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Camera Permission Needed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "To scan barcodes, Naya needs access to your camera. Your camera is only used for scanning and nothing is stored.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Camera Access")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateBack) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.NoPhotography,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Camera Access Denied",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Without camera access, barcode scanning isn't possible. You can enable it in your device settings.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onNavigateBack) {
                Text("Go Back")
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// STATE
// ═══════════════════════════════════════════════════════════════════════════════

sealed class ScannerState {
    data object Scanning : ScannerState()
    data class Loading(val barcode: String) : ScannerState()
    data class Found(val food: NayaFood) : ScannerState()
    data class NotFound(val barcode: String) : ScannerState()
    data class LabelCapture(val barcode: String) : ScannerState()  // New: Capture nutrition label
    data class Error(val message: String) : ScannerState()
}