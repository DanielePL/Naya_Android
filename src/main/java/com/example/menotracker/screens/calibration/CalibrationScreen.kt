// File: android/app/src/main/java/com/example/myapplicationtest/screens/calibration/CalibrationScreen.kt

package com.example.menotracker.screens.calibration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

// 🎨 Naya Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.9f)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF2a1f1a), Color(0xFF0f0f0f), Color(0xFF1a1410))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    onNavigateBack: () -> Unit = {},
    onCalibrationComplete: (CalibrationData) -> Unit = {},
    viewModel: CalibrationViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Load existing calibration on start
    LaunchedEffect(Unit) {
        viewModel.loadCalibration(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
            when (uiState) {
                is CalibrationUiState.Idle -> {
                    // Start button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { viewModel.startCalibration() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = orangePrimary
                            )
                        ) {
                            Text("Start Calibration")
                        }
                    }
                }

                is CalibrationUiState.Step -> {
                    val step = (uiState as CalibrationUiState.Step).step
                    when (step) {
                        CalibrationStep.INTRO -> IntroStep(
                            onNext = { viewModel.nextStep() },
                            onSkip = onNavigateBack
                        )
                        CalibrationStep.SELECT_OBJECT -> SelectObjectStep(
                            viewModel = viewModel,
                            onNext = { viewModel.nextStep() },
                            onBack = { viewModel.previousStep() }
                        )
                        CalibrationStep.INPUT_CUSTOM_SIZE -> InputCustomSizeStep(
                            viewModel = viewModel,
                            onNext = { viewModel.nextStep() },
                            onBack = { viewModel.previousStep() }
                        )
                        CalibrationStep.POSITION_OBJECT -> PositionObjectStep(
                            viewModel = viewModel,
                            onNext = { viewModel.nextStep() },
                            onBack = { viewModel.previousStep() }
                        )
                        CalibrationStep.CAPTURE -> CaptureStep(
                            viewModel = viewModel,
                            onBack = { viewModel.previousStep() }
                        )
                        CalibrationStep.PROCESSING -> ProcessingStep()
                        else -> {}
                    }
                }

                is CalibrationUiState.Success -> {
                    val calibrationData = (uiState as CalibrationUiState.Success).calibration
                    SuccessStep(
                        calibrationData = calibrationData,
                        onSave = {
                            viewModel.saveCalibration(context, calibrationData)
                            onCalibrationComplete(calibrationData)
                        },
                        onRetry = {
                            viewModel.reset()
                            viewModel.startCalibration()
                        }
                    )
                }

                is CalibrationUiState.Error -> {
                    val message = (uiState as CalibrationUiState.Error).message
                    ErrorStep(
                        message = message,
                        onRetry = {
                            viewModel.reset()
                            viewModel.startCalibration()
                        },
                        onCancel = onNavigateBack
                    )
                }

                is CalibrationUiState.Processing -> {
                    ProcessingStep()
                }
            }
        }
    }
}

@Composable
private fun IntroStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.weight(0.2f))

        // Icon
        Icon(
            Icons.Default.Straighten,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = orangePrimary
        )

        // Title
        Text(
            "📏 Calibrate for Accuracy",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite,
            textAlign = TextAlign.Center
        )

        // Description
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoRow(
                    icon = Icons.Default.Speed,
                    title = "Accurate m/s",
                    description = "Get real velocity measurements instead of relative speed"
                )
                InfoRow(
                    icon = Icons.Default.TrendingUp,
                    title = "Professional Grade",
                    description = "Same accuracy as $1000+ VBT devices"
                )
                InfoRow(
                    icon = Icons.Default.Timer,
                    title = "Takes 30 seconds",
                    description = "Quick and easy setup with any ruler or plate"
                )
            }
        }

        Spacer(Modifier.weight(0.3f))

        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("START CALIBRATION", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now", color = textGray)
            }
        }

        Spacer(Modifier.weight(0.1f))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = orangePrimary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textWhite
            )
            Text(
                description,
                fontSize = 12.sp,
                color = textGray
            )
        }
    }
}

@Composable
private fun SelectObjectStep(
    viewModel: CalibrationViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val selectedObject by viewModel.selectedObject.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            "Choose Reference Object",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite
        )

        Text(
            "Select an object you have available for calibration",
            fontSize = 14.sp,
            color = textGray
        )

        Spacer(Modifier.height(8.dp))

        // Object options
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ReferenceObject.values().toList()) { obj ->
                ReferenceObjectCard(
                    obj = obj,
                    isSelected = obj == selectedObject,
                    onSelect = { viewModel.selectReferenceObject(obj) }
                )
            }
        }

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = selectedObject != null,
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun ReferenceObjectCard(
    obj: ReferenceObject,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) orangePrimary.copy(alpha = 0.2f) else cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, orangePrimary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                obj.icon,
                fontSize = 32.sp
            )

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    obj.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textWhite
                )
                Text(
                    obj.description,
                    fontSize = 12.sp,
                    color = textGray
                )
                if (obj != ReferenceObject.CUSTOM) {
                    Text(
                        "${obj.sizeMeters}m",
                        fontSize = 11.sp,
                        color = orangeGlow
                    )
                }
            }

            // Checkmark
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = orangePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun InputCustomSizeStep(
    viewModel: CalibrationViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var sizeInput by remember { mutableStateOf("") }
    val customSize by viewModel.customSize.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Enter Object Size",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite
        )

        Text(
            "Measure your object accurately and enter the size in meters",
            fontSize = 14.sp,
            color = textGray
        )

        Spacer(Modifier.height(24.dp))

        // Input field
        OutlinedTextField(
            value = sizeInput,
            onValueChange = {
                sizeInput = it
                it.toFloatOrNull()?.let { size ->
                    viewModel.setCustomSize(size)
                }
            },
            label = { Text("Size (meters)") },
            placeholder = { Text("e.g., 1.0") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Example conversions
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Blue.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "💡 Conversion Help",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textWhite
                )
                Text("50cm = 0.5m", fontSize = 12.sp, color = textGray)
                Text("1m = 100cm", fontSize = 12.sp, color = textGray)
                Text("45cm = 0.45m", fontSize = 12.sp, color = textGray)
            }
        }

        Spacer(Modifier.weight(1f))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                enabled = customSize != null && (customSize ?: 0f) > 0f,
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun PositionObjectStep(
    viewModel: CalibrationViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val selectedObject by viewModel.selectedObject.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Position Your Object",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite
        )

        Text(
            "Follow these instructions for best results:",
            fontSize = 14.sp,
            color = textGray
        )

        Spacer(Modifier.height(8.dp))

        // Instructions
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InstructionRow("1️⃣", "Place ${selectedObject?.displayName} in frame")
                InstructionRow("2️⃣", "Object should be clearly visible")
                InstructionRow("3️⃣", "Stand 2-3 meters away")
                InstructionRow("4️⃣", "Keep phone steady and upright")
                InstructionRow("5️⃣", "Good lighting is important")
            }
        }

        Spacer(Modifier.weight(1f))

        // Preview placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = textGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Camera preview will appear here",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
            ) {
                Text("Ready to Capture")
            }
        }
    }
}

@Composable
private fun InstructionRow(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(number, fontSize = 20.sp)
        Text(text, fontSize = 14.sp, color = textWhite)
    }
}

@Composable
private fun CaptureStep(
    viewModel: CalibrationViewModel,
    onBack: () -> Unit
) {
    // TODO: Implement camera capture
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Camera Capture",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                // Simulate capture for now
                // TODO: Replace with real camera capture
                val dummyBitmap = android.graphics.Bitmap.createBitmap(1920, 1080, android.graphics.Bitmap.Config.ARGB_8888)
                viewModel.captureImage(dummyBitmap)
            },
            colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
        ) {
            Icon(Icons.Default.CameraAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("CAPTURE")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
private fun ProcessingStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = orangePrimary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Processing...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Detecting object and calculating calibration",
            fontSize = 14.sp,
            color = textGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessStep(
    calibrationData: CalibrationData,
    onSave: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.weight(0.2f))

        // Success icon
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Green
        )

        Text(
            "✅ Calibration Successful!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite,
            textAlign = TextAlign.Center
        )

        // Results card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultRow("Pixels per meter", "${calibrationData.pixelsPerMeter.toInt()} px/m")
                ResultRow("Accuracy", "±${calibrationData.accuracyEstimateCm}cm")
                ResultRow("Method", calibrationData.method.displayName)
                ResultRow("Reference", "${calibrationData.referenceDistanceMeters}m")
            }
        }

        Spacer(Modifier.weight(0.3f))

        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAVE & START USING", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try Again")
            }
        }

        Spacer(Modifier.weight(0.1f))
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = textGray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textWhite)
    }
}

@Composable
private fun ErrorStep(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFF4444)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Calibration Failed",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite
        )

        Spacer(Modifier.height(8.dp))

        Text(
            message,
            fontSize = 14.sp,
            color = textGray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
            ) {
                Text("Try Again")
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
