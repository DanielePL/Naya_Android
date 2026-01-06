// app/src/main/java/com/example/myapplicationtest/screens/nutrition/MealPhotoCaptureScreen.kt

package com.example.menotracker.screens.nutrition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.data.models.MealType
import com.example.menotracker.viewmodels.NutritionViewModel
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "MealPhotoCaptureScreen"

// Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val greenSuccess = Color(0xFF4CAF50)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1A1410)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF2a1f1a), Color(0xFF0f0f0f), Color(0xFF1a1410))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPhotoCaptureScreen(
    onNavigateBack: () -> Unit,
    onPhotoAnalyzed: () -> Unit,
    initialMealType: MealType = MealType.LUNCH,
    nutritionViewModel: NutritionViewModel = viewModel()
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageFile by remember { mutableStateOf<File?>(null) }

    // Use ViewModel's selectedMealType - sync with initialMealType on first load
    val viewModelMealType by nutritionViewModel.selectedMealType.collectAsState()
    var selectedMealType by remember { mutableStateOf(initialMealType) }

    // Sync initial meal type to ViewModel
    LaunchedEffect(initialMealType) {
        nutritionViewModel.setSelectedMealType(initialMealType)
        selectedMealType = initialMealType
    }

    // Update ViewModel when user changes selection
    fun updateMealType(mealType: MealType) {
        selectedMealType = mealType
        nutritionViewModel.setSelectedMealType(mealType)
    }

    // Store the file URI for full-resolution camera capture
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val isAnalyzing by nutritionViewModel.isAnalyzing.collectAsState()
    val errorMessage by nutritionViewModel.errorMessage.collectAsState()
    val aiAnalysisResult by nutritionViewModel.aiAnalysisResult.collectAsState()

    // Track if analysis was started from THIS screen session
    var analysisStartedHere by remember { mutableStateOf(false) }

    // Clear any stale result when entering this screen
    LaunchedEffect(Unit) {
        nutritionViewModel.clearAnalysisResult()
        analysisStartedHere = false
    }

    // Camera launcher for full-resolution photos
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d(TAG, "📷 Camera result: success=$success, pendingFile=${pendingCameraFile?.absolutePath}")

        if (success && pendingCameraFile != null) {
            val file = pendingCameraFile!!

            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "✅ Photo saved successfully: ${file.absolutePath} (${file.length()} bytes)")

                // Load bitmap with correct rotation
                val bitmap = loadBitmapWithCorrectRotation(file.absolutePath)

                if (bitmap != null) {
                    capturedBitmap = bitmap
                    selectedImageUri = Uri.fromFile(file)
                    selectedImageFile = file
                    Log.d(TAG, "✅ Preview bitmap loaded: ${bitmap.width}x${bitmap.height}")
                } else {
                    Log.e(TAG, "❌ Failed to decode bitmap from file")
                }
            } else {
                Log.e(TAG, "❌ Photo file is empty or missing")
            }
        } else {
            Log.d(TAG, "📷 Camera cancelled or failed")
        }

        pendingCameraUri = null
        pendingCameraFile = null
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = createImageFile(context)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingCameraFile = file
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Log.d(TAG, "🖼️ Gallery image selected: $uri")

            val tempFile = createImageFile(context)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    selectedImageUri = Uri.fromFile(tempFile)
                    selectedImageFile = tempFile

                    // Load bitmap with correct rotation
                    capturedBitmap = loadBitmapWithCorrectRotation(tempFile.absolutePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error copying gallery image: ${e.message}")
            }
        }
    }

    // Navigate to result screen when analysis succeeds
    LaunchedEffect(aiAnalysisResult) {
        if (analysisStartedHere) {
            aiAnalysisResult?.let { result ->
                if (result.success) {
                    Log.d(TAG, "✅ Analysis successful, navigating to results")
                    onPhotoAnalyzed()
                }
            }
        }
    }

    // Helper to launch camera
    fun launchCamera() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val file = createImageFile(context)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingCameraFile = file
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snap Your Meal", color = textWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = textWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1a1410)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ═══════════════════════════════════════════════════════════════
                // PHOTO PREVIEW / PLACEHOLDER (Clickable to take photo)
                // ═══════════════════════════════════════════════════════════════
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable(enabled = !isAnalyzing) { launchCamera() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            // Show captured image
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured meal",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Overlay to indicate tap to retake
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = textWhite,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        "Tap to retake",
                                        color = textWhite,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            // Placeholder
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = orangeGlow
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Take a photo of your meal or product",
                                    color = textWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "AI will analyze the nutrition content",
                                    color = textGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // MEAL TYPE SELECTOR (Compact)
                // ═══════════════════════════════════════════════════════════════
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Meal Type",
                            color = textWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        // 2x2 grid + 1
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Row 1: Breakfast, Lunch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MealTypeChip(
                                    mealType = MealType.BREAKFAST,
                                    isSelected = selectedMealType == MealType.BREAKFAST,
                                    onClick = { updateMealType(MealType.BREAKFAST) },
                                    modifier = Modifier.weight(1f)
                                )
                                MealTypeChip(
                                    mealType = MealType.LUNCH,
                                    isSelected = selectedMealType == MealType.LUNCH,
                                    onClick = { updateMealType(MealType.LUNCH) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Row 2: Dinner, Shake
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MealTypeChip(
                                    mealType = MealType.DINNER,
                                    isSelected = selectedMealType == MealType.DINNER,
                                    onClick = { updateMealType(MealType.DINNER) },
                                    modifier = Modifier.weight(1f)
                                )
                                MealTypeChip(
                                    mealType = MealType.SHAKE,
                                    isSelected = selectedMealType == MealType.SHAKE,
                                    onClick = { updateMealType(MealType.SHAKE) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Row 3: Snack (full width)
                            MealTypeChip(
                                mealType = MealType.SNACK,
                                isSelected = selectedMealType == MealType.SNACK,
                                onClick = { updateMealType(MealType.SNACK) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // ACTION BUTTONS (Take Photo + Gallery side by side)
                // ═══════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Take Photo button
                    Button(
                        onClick = { launchCamera() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = orangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAnalyzing
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Take Photo", fontSize = 14.sp)
                    }

                    // Gallery button
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textWhite),
                        enabled = !isAnalyzing
                    ) {
                        Icon(
                            Icons.Default.Image,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery", fontSize = 14.sp)
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // ANALYZE BUTTON
                // ═══════════════════════════════════════════════════════════════
                Button(
                    onClick = {
                        selectedImageFile?.let { file ->
                            if (file.exists() && file.length() > 0) {
                                Log.d(TAG, "🔍 Starting analysis of: ${file.absolutePath}")
                                analysisStartedHere = true
                                nutritionViewModel.analyzeMealPhoto(
                                    imageFile = file,
                                    mealType = selectedMealType,
                                    additionalContext = null
                                )
                            }
                        }
                    },
                    enabled = selectedImageFile != null && !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = greenSuccess,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Analyzing...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze with AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // ERROR MESSAGE (if any)
                // ═══════════════════════════════════════════════════════════════
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                error,
                                color = textWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealTypeChip(
    mealType: MealType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) orangePrimary.copy(alpha = 0.2f) else Color(0xFF2a2a2a).copy(alpha = 0.4f)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, orangePrimary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, textGray.copy(alpha = 0.3f))
        },
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getMealTypeIcon(mealType),
                contentDescription = null,
                tint = if (isSelected) orangeGlow else textGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                mealType.displayName,
                color = if (isSelected) orangeGlow else textWhite,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun getMealTypeIcon(mealType: MealType): ImageVector {
    return when (mealType) {
        MealType.BREAKFAST -> Icons.Default.WbSunny
        MealType.LUNCH -> Icons.Default.Restaurant
        MealType.DINNER -> Icons.Default.DinnerDining
        MealType.SHAKE -> Icons.Default.LocalDrink
        MealType.SNACK -> Icons.Default.Cookie
    }
}

private fun createImageFile(context: android.content.Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    val storageDir = File(context.filesDir, "meal_photos").apply {
        if (!exists()) {
            mkdirs()
            Log.d(TAG, "📁 Created meal_photos directory: $absolutePath")
        }
    }

    val file = File(storageDir, "MEAL_${timeStamp}_${System.nanoTime()}.jpg")
    Log.d(TAG, "📁 Created image file: ${file.absolutePath}")
    return file
}

/**
 * Load bitmap from file with correct EXIF rotation applied.
 * Android cameras often save images in landscape orientation with an EXIF tag
 * indicating the actual rotation. BitmapFactory ignores this tag, so we need
 * to read it and rotate the bitmap manually.
 */
private fun loadBitmapWithCorrectRotation(filePath: String): Bitmap? {
    return try {
        // First, decode with inSampleSize for memory efficiency
        val options = BitmapFactory.Options().apply {
            inSampleSize = 4
        }
        val bitmap = BitmapFactory.decodeFile(filePath, options) ?: return null

        // Read EXIF orientation
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        Log.d(TAG, "📐 EXIF orientation: $orientation")

        // Determine rotation angle
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        // If no rotation needed, return original bitmap
        if (rotationDegrees == 0f) {
            Log.d(TAG, "✅ No rotation needed")
            return bitmap
        }

        // Rotate the bitmap
        Log.d(TAG, "🔄 Rotating bitmap by $rotationDegrees degrees")
        val matrix = Matrix().apply {
            postRotate(rotationDegrees)
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width, bitmap.height,
            matrix,
            true
        )

        // Recycle original if we created a new one
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }

        rotatedBitmap
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error loading bitmap with rotation: ${e.message}")
        // Fallback: try to load without rotation handling
        try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4
            }
            BitmapFactory.decodeFile(filePath, options)
        } catch (e2: Exception) {
            null
        }
    }
}