package com.example.menotracker.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.BodyMeasurement
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasurementsEditDialog(
    currentMeasurement: BodyMeasurement?,
    userId: String,
    onDismiss: () -> Unit,
    onSave: (BodyMeasurement) -> Unit
) {
    // State for each measurement
    var neck by remember { mutableStateOf(currentMeasurement?.neck?.toString() ?: "") }
    var shoulders by remember { mutableStateOf(currentMeasurement?.shoulders?.toString() ?: "") }
    var chest by remember { mutableStateOf(currentMeasurement?.chest?.toString() ?: "") }
    var arms by remember { mutableStateOf(currentMeasurement?.arms?.toString() ?: "") }
    var forearms by remember { mutableStateOf(currentMeasurement?.forearms?.toString() ?: "") }
    var waist by remember { mutableStateOf(currentMeasurement?.waist?.toString() ?: "") }
    var hips by remember { mutableStateOf(currentMeasurement?.hips?.toString() ?: "") }
    var glutes by remember { mutableStateOf(currentMeasurement?.glutes?.toString() ?: "") }
    var legs by remember { mutableStateOf(currentMeasurement?.legs?.toString() ?: "") }
    var calves by remember { mutableStateOf(currentMeasurement?.calves?.toString() ?: "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "Body Measurements",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Text(
                text = "All measurements in cm",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            // Upper Body
            Text(
                text = "Upper Body",
                color = orangeGlow,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeasurementField(
                    value = neck,
                    onValueChange = { neck = it },
                    label = "Neck",
                    modifier = Modifier.weight(1f)
                )
                MeasurementField(
                    value = shoulders,
                    onValueChange = { shoulders = it },
                    label = "Shoulders",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeasurementField(
                    value = chest,
                    onValueChange = { chest = it },
                    label = "Chest",
                    modifier = Modifier.weight(1f)
                )
                MeasurementField(
                    value = arms,
                    onValueChange = { arms = it },
                    label = "Arms",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeasurementField(
                    value = forearms,
                    onValueChange = { forearms = it },
                    label = "Forearms",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // Core & Lower Body
            Text(
                text = "Core & Lower Body",
                color = orangeGlow,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeasurementField(
                    value = waist,
                    onValueChange = { waist = it },
                    label = "Waist",
                    modifier = Modifier.weight(1f)
                )
                MeasurementField(
                    value = hips,
                    onValueChange = { hips = it },
                    label = "Hips",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeasurementField(
                    value = glutes,
                    onValueChange = { glutes = it },
                    label = "Glutes",
                    modifier = Modifier.weight(1f)
                )
                MeasurementField(
                    value = legs,
                    onValueChange = { legs = it },
                    label = "Legs",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeasurementField(
                    value = calves,
                    onValueChange = { calves = it },
                    label = "Calves",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val measurement = BodyMeasurement(
                            id = currentMeasurement?.id ?: java.util.UUID.randomUUID().toString(),
                            clientId = userId,
                            date = java.time.LocalDate.now().toString(),
                            neck = neck.toDoubleOrNull(),
                            shoulders = shoulders.toDoubleOrNull(),
                            chest = chest.toDoubleOrNull(),
                            arms = arms.toDoubleOrNull(),
                            forearms = forearms.toDoubleOrNull(),
                            waist = waist.toDoubleOrNull(),
                            hips = hips.toDoubleOrNull(),
                            glutes = glutes.toDoubleOrNull(),
                            legs = legs.toDoubleOrNull(),
                            calves = calves.toDoubleOrNull()
                        )
                        onSave(measurement)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = orangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun MeasurementField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Only allow numbers and decimal point
            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(newValue)
            }
        },
        label = { Text(label, fontSize = 12.sp) },
        suffix = { Text("cm", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = orangePrimary,
            focusedLabelColor = orangePrimary,
            cursorColor = orangePrimary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}