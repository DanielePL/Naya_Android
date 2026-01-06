// screens/nutrition/LabelCaptureScreen.kt
package com.example.menotracker.screens.nutrition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.menotracker.data.models.NayaFood
import com.example.menotracker.data.repository.CommunityFoodRepository
import com.example.menotracker.data.repository.NutritionLabelParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

private const val TAG = "LabelCapture"

sealed class LabelCaptureState {
    data object Camera : LabelCaptureState()
    data object Processing : LabelCaptureState()
    data class Review(
        val bitmap: Bitmap,
        val parsedNutrition: NutritionLabelParser.ParsedNutrition,
        val warnings: List<String>
    ) : LabelCaptureState()
    data object Saving : LabelCaptureState()
    data class Success(val food: NayaFood) : LabelCaptureState()
    data class Error(val message: String) : LabelCaptureState()
}

@Composable
fun LabelCaptureScreen(
    barcode: String,
    onNavigateBack: () -> Unit,
    onSuccess: (NayaFood) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<LabelCaptureState>(LabelCaptureState.Camera) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var productName by remember { mutableStateOf("") }
    var brandName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }

    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            textRecognizer.close()
            cameraExecutor.shutdown()
        }
    }

    fun captureAndProcess() {
        val capture = imageCapture ?: return
        state = LabelCaptureState.Processing

        val photoFile = File(context.cacheDir, "label_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    scope.launch {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                                BitmapFactory.decodeFile(photoFile.absolutePath, options)
                            }

                            if (bitmap == null) {
                                state = LabelCaptureState.Error("Failed to load image")
                                return@launch
                            }

                            val inputImage = InputImage.fromBitmap(bitmap, 0)
                            val visionText = textRecognizer.process(inputImage).await()
                            val ocrText = visionText.text

                            val parsed = NutritionLabelParser.parse(ocrText)
                            val warnings = NutritionLabelParser.validateNutrition(parsed)

                            calories = parsed.calories?.toInt()?.toString() ?: ""
                            protein = parsed.protein?.let { "%.1f".format(it) } ?: ""
                            carbs = parsed.carbs?.let { "%.1f".format(it) } ?: ""
                            fat = parsed.fat?.let { "%.1f".format(it) } ?: ""
                            fiber = parsed.fiber?.let { "%.1f".format(it) } ?: ""
                            sugar = parsed.sugar?.let { "%.1f".format(it) } ?: ""

                            state = LabelCaptureState.Review(bitmap, parsed, warnings)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing image", e)
                            state = LabelCaptureState.Error("Failed to process: ${e.message}")
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    state = LabelCaptureState.Error("Capture failed: ${exception.message}")
                }
            }
        )
    }

    fun saveFood(bitmap: Bitmap, rawText: String, confidence: Float) {
        if (productName.isBlank() || calories.isBlank() || protein.isBlank() || carbs.isBlank() || fat.isBlank()) return
        state = LabelCaptureState.Saving

        scope.launch {
            try {
                val imageFile = withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                    file
                }

                val imageUrl = CommunityFoodRepository.uploadLabelImage(barcode, imageFile).getOrNull()

                CommunityFoodRepository.saveFood(
                    barcode = barcode,
                    name = productName.trim(),
                    brand = brandName.trim().takeIf { it.isNotEmpty() },
                    calories = calories.toFloatOrNull() ?: 0f,
                    protein = protein.toFloatOrNull() ?: 0f,
                    carbs = carbs.toFloatOrNull() ?: 0f,
                    fat = fat.toFloatOrNull() ?: 0f,
                    fiber = fiber.toFloatOrNull(),
                    sugar = sugar.toFloatOrNull(),
                    ocrRawText = rawText,
                    labelImageUrl = imageUrl,
                    confidenceScore = confidence
                ).onSuccess { savedFood ->
                    val food = CommunityFoodRepository.toNayaFood(savedFood)
                    state = LabelCaptureState.Success(food)
                }.onFailure { error ->
                    state = LabelCaptureState.Error("Save failed: ${error.message}")
                }

                withContext(Dispatchers.IO) { imageFile.delete() }
            } catch (e: Exception) {
                state = LabelCaptureState.Error("Error: ${e.message}")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is LabelCaptureState.Camera -> {
                CameraPreviewContent(lifecycleOwner, isFlashOn) { cam, capture ->
                    camera = cam
                    imageCapture = capture
                }
                CameraOverlay(barcode, isFlashOn, { isFlashOn = !isFlashOn; camera?.cameraControl?.enableTorch(isFlashOn) }, { captureAndProcess() }, onNavigateBack)
            }
            is LabelCaptureState.Processing -> ProcessingContent()
            is LabelCaptureState.Review -> ReviewContent(
                currentState.bitmap, currentState.parsedNutrition, currentState.warnings,
                productName, brandName, calories, protein, carbs, fat, fiber, sugar,
                { productName = it }, { brandName = it }, { calories = it }, { protein = it },
                { carbs = it }, { fat = it }, { fiber = it }, { sugar = it },
                { state = LabelCaptureState.Camera },
                { saveFood(currentState.bitmap, currentState.parsedNutrition.rawText, currentState.parsedNutrition.confidence) },
                onNavigateBack
            )
            is LabelCaptureState.Saving -> SavingContent()
            is LabelCaptureState.Success -> SuccessContent(currentState.food) { onSuccess(currentState.food) }
            is LabelCaptureState.Error -> ErrorContent(currentState.message, { state = LabelCaptureState.Camera }, onNavigateBack)
        }
    }
}

@Composable
private fun CameraPreviewContent(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    isFlashOn: Boolean,
    onCameraReady: (Camera, ImageCapture) -> Unit
) {
    val context = LocalContext.current
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    onCameraReady(camera, imageCapture)
                } catch (e: Exception) { Log.e(TAG, "Camera binding failed", e) }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CameraOverlay(barcode: String, isFlashOn: Boolean, onToggleFlash: () -> Unit, onCapture: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top bar with back, title, flash
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
                    "Photograph Label",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Fill the screen with the nutrition table",
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

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barcode info chip
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Barcode: $barcode",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capture button
            Button(
                onClick = onCapture,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    "Capture",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ProcessingContent() {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Text("Analyzing nutrition label...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Extracting values with OCR", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun ReviewContent(
    bitmap: Bitmap, parsed: NutritionLabelParser.ParsedNutrition, warnings: List<String>,
    productName: String, brandName: String, calories: String, protein: String, carbs: String, fat: String, fiber: String, sugar: String,
    onProductNameChange: (String) -> Unit, onBrandNameChange: (String) -> Unit, onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit, onCarbsChange: (String) -> Unit, onFatChange: (String) -> Unit,
    onFiberChange: (String) -> Unit, onSugarChange: (String) -> Unit,
    onRetake: () -> Unit, onSave: () -> Unit, onBack: () -> Unit
) {
    val canSave = productName.isNotBlank() && calories.isNotBlank() && protein.isNotBlank() && carbs.isNotBlank() && fat.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Review & Save", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onRetake) { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Retake") }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp)) {
                androidx.compose.foundation.Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Label", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("OCR Confidence", style = MaterialTheme.typography.bodyMedium)
                val color = when { parsed.confidence >= 0.7f -> Color(0xFF4CAF50); parsed.confidence >= 0.4f -> Color(0xFFFF9800); else -> Color(0xFFF44336) }
                Text("${(parsed.confidence * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }

            if (warnings.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Please verify", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = Color(0xFF795548)) }
                    }
                }
            }

            if (!parsed.isValid) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = Color(0xFFF44336), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Missing: ${parsed.missingFields.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                    }
                }
            }

            HorizontalDivider()
            Text("Product Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = productName, onValueChange = onProductNameChange, label = { Text("Product Name *") }, placeholder = { Text("e.g., Protein Bar") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = productName.isBlank())
            OutlinedTextField(value = brandName, onValueChange = onBrandNameChange, label = { Text("Brand (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            HorizontalDivider()
            Text("Nutrition per 100g", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NutritionField(calories, onCaloriesChange, "Calories *", "kcal", Modifier.weight(1f), calories.isBlank())
                NutritionField(protein, onProteinChange, "Protein *", "g", Modifier.weight(1f), protein.isBlank())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NutritionField(carbs, onCarbsChange, "Carbs *", "g", Modifier.weight(1f), carbs.isBlank())
                NutritionField(fat, onFatChange, "Fat *", "g", Modifier.weight(1f), fat.isBlank())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NutritionField(fiber, onFiberChange, "Fiber", "g", Modifier.weight(1f))
                NutritionField(sugar, onSugarChange, "Sugar", "g", Modifier.weight(1f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = canSave, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Save")
            }
        }
    }
}

@Composable
private fun NutritionField(value: String, onValueChange: (String) -> Unit, label: String, unit: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) onValueChange(it) },
        label = { Text(label) }, suffix = { Text(unit) }, modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = isError
    )
}

@Composable
private fun SavingContent() {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Text("Saving to Community...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SuccessContent(food: NayaFood, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(32.dp)) {
            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = Color(0xFF4CAF50))
            }
            Text("Thank You!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("\"${food.name}\" added to community database.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.People, null, tint = Color(0xFF4CAF50))
                    Text("Other users can now scan this product!", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Done") }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Error, null, modifier = Modifier.size(72.dp), tint = Color(0xFFF44336))
            Text("Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Cancel") }
                Button(onClick = onRetry) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Retry") }
            }
        }
    }
}