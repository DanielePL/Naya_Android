// app/src/main/java/com/example/myapplicationtest/screens/workoutbuilder/SetInputDialog.kt

package com.example.menotracker.screens.workoutbuilder

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.menotracker.data.ExerciseSet
import com.example.menotracker.data.RestTimePresets
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

// Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.95f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetInputDialog(
    exerciseName: String,
    currentSets: List<ExerciseSet>,
    onDismiss: () -> Unit,
    onSave: (List<ExerciseSet>) -> Unit
) {
    var sets by remember { mutableStateOf(currentSets.toMutableList()) }
    var showRestTimePicker by remember { mutableStateOf(false) }
    var selectedSetForRest by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = exerciseName,
                            color = textWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Configure Sets",
                            color = textGray,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textGray
                        )
                    }
                }

                Divider(color = textGray.copy(alpha = 0.2f))

                // Sets List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sets.forEachIndexed { index, set ->
                        SetInputRow(
                            setNumber = index + 1,
                            exerciseSet = set,
                            onRepsChange = { newReps ->
                                sets = sets.toMutableList().apply {
                                    this[index] = set.copy(targetReps = newReps)
                                }
                            },
                            onWeightChange = { newWeight ->
                                sets = sets.toMutableList().apply {
                                    this[index] = set.copy(targetWeight = newWeight)
                                }
                            },
                            onRestClick = {
                                selectedSetForRest = index
                                showRestTimePicker = true
                            },
                            onDelete = {
                                if (sets.size > 1) {
                                    sets.removeAt(index)
                                    sets = sets.mapIndexed { idx, s ->
                                        s.copy(setNumber = idx + 1)
                                    }.toMutableList()
                                }
                            },
                            canDelete = sets.size > 1
                        )
                    }
                }

                // Add Set Button
                OutlinedButton(
                    onClick = {
                        val lastSet = sets.lastOrNull()
                        val newSet = ExerciseSet(
                            setNumber = sets.size + 1,
                            targetReps = lastSet?.targetReps ?: 10,
                            targetWeight = lastSet?.targetWeight ?: 0.0,
                            restSeconds = lastSet?.restSeconds ?: 90
                        )
                        sets = sets.toMutableList().apply { add(newSet) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = orangeGlow
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(orangeGlow)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Add Set", fontWeight = FontWeight.Bold)
                }

                Divider(color = textGray.copy(alpha = 0.2f))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textGray
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onSave(sets) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = orangeGlow
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Rest Time Picker Dialog
    if (showRestTimePicker && selectedSetForRest != null) {
        RestTimePickerDialog(
            currentRestSeconds = sets[selectedSetForRest!!].restSeconds,
            onDismiss = { showRestTimePicker = false },
            onSelectRest = { restSeconds ->
                val index = selectedSetForRest!!
                sets = sets.toMutableList().apply {
                    this[index] = this[index].copy(restSeconds = restSeconds)
                }
                showRestTimePicker = false
            }
        )
    }
}

@Composable
private fun SetInputRow(
    setNumber: Int,
    exerciseSet: ExerciseSet,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Double) -> Unit,
    onRestClick: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    var repsText by remember { mutableStateOf(exerciseSet.targetReps.toString()) }
    var weightText by remember { mutableStateOf(
        if (exerciseSet.targetWeight % 1.0 == 0.0) {
            exerciseSet.targetWeight.toInt().toString()
        } else {
            exerciseSet.targetWeight.toString()
        }
    ) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2a2a)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Set $setNumber",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Set",
                            tint = textGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reps Input
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "REPS",
                        color = textGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { newValue ->
                            repsText = newValue
                            newValue.toIntOrNull()?.let { onRepsChange(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            color = textWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = orangeGlow,
                            unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                            cursorColor = orangeGlow
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Text("×", color = textGray, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                // Weight Input
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "WEIGHT (KG)",
                        color = textGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { newValue ->
                            weightText = newValue
                            newValue.toDoubleOrNull()?.let { onWeightChange(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            color = textWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = orangeGlow,
                            unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                            cursorColor = orangeGlow
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Rest Time Button
            OutlinedButton(
                onClick = onRestClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = textWhite
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(textGray.copy(alpha = 0.3f))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = orangeGlow
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatRestTime(exerciseSet.restSeconds),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun RestTimePickerDialog(
    currentRestSeconds: Int,
    onDismiss: () -> Unit,
    onSelectRest: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Rest Time",
                    color = textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Divider(color = textGray.copy(alpha = 0.2f))

                RestTimePresets.getPresets().forEach { (label, seconds) ->
                    OutlinedButton(
                        onClick = { onSelectRest(seconds) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (currentRestSeconds == seconds) {
                                orangeGlow.copy(alpha = 0.2f)
                            } else {
                                Color.Transparent
                            },
                            contentColor = if (currentRestSeconds == seconds) {
                                orangeGlow
                            } else {
                                textWhite
                            }
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = if (currentRestSeconds == seconds) 2.dp else 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (currentRestSeconds == seconds) orangeGlow else textGray.copy(alpha = 0.3f)
                            )
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            fontWeight = if (currentRestSeconds == seconds) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private fun formatRestTime(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s rest"
        seconds % 60 == 0 -> "${seconds / 60}min rest"
        else -> "${seconds / 60}min ${seconds % 60}s rest"
    }
}