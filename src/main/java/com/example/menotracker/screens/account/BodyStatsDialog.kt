// app/src/main/java/com/example/myapplicationtest/screens/account/BodyStatsDialog.kt

package com.example.menotracker.screens.account

import androidx.compose.foundation.BorderStroke
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
import com.example.menotracker.data.models.ActivityLevel
import com.example.menotracker.data.models.Gender
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF2A2A2A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyStatsDialog(
    currentName: String,
    currentWeight: String,
    currentHeight: String,
    currentAge: String,
    currentGender: Gender?,
    currentActivityLevel: ActivityLevel?,
    currentYears: String,
    onDismiss: () -> Unit,
    onSave: (name: String, weight: String, height: String, age: String, gender: Gender?, activityLevel: ActivityLevel?, years: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var weight by remember { mutableStateOf(currentWeight) }
    var height by remember { mutableStateOf(currentHeight) }
    var age by remember { mutableStateOf(currentAge) }
    var gender by remember { mutableStateOf(currentGender) }
    var activityLevel by remember { mutableStateOf(currentActivityLevel) }
    var years by remember { mutableStateOf(currentYears) }

    var showActivityDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Body Stats & Profile",
                    color = textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = textGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        focusedBorderColor = orangeGlow,
                        unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                        cursorColor = orangeGlow
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Gender Selection
                Text(
                    text = "Gender",
                    color = textGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Gender.entries.forEach { g ->
                        Surface(
                            onClick = { gender = g },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (gender == g) orangePrimary.copy(alpha = 0.2f) else cardBackground,
                            border = if (gender == g) BorderStroke(1.5.dp, orangePrimary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (g == Gender.MALE) Icons.Default.Male else Icons.Default.Female,
                                    contentDescription = null,
                                    tint = if (gender == g) orangePrimary else textGray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = g.displayName,
                                    color = if (gender == g) orangePrimary else textGray,
                                    fontWeight = if (gender == g) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Age and Weight Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                age = it
                            }
                        },
                        label = { Text("Age", color = textGray) },
                        suffix = { Text("yrs", color = textGray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = orangeGlow,
                            unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                            cursorColor = orangeGlow
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = weight,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() || char == '.' }) {
                                weight = it
                            }
                        },
                        label = { Text("Weight", color = textGray) },
                        suffix = { Text("kg", color = textGray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = orangeGlow,
                            unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                            cursorColor = orangeGlow
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Height and Training Experience Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                height = it
                            }
                        },
                        label = { Text("Height", color = textGray) },
                        suffix = { Text("cm", color = textGray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = orangeGlow,
                            unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                            cursorColor = orangeGlow
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = years,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                years = it
                            }
                        },
                        label = { Text("Experience", color = textGray) },
                        suffix = { Text("yrs", color = textGray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = orangeGlow,
                            unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                            cursorColor = orangeGlow
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Activity Level
                Text(
                    text = "Activity Level",
                    color = textGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = showActivityDropdown,
                    onExpandedChange = { showActivityDropdown = it }
                ) {
                    OutlinedTextField(
                        value = activityLevel?.displayName ?: "Select activity level",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showActivityDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (activityLevel != null) textWhite else textGray,
                            unfocusedTextColor = if (activityLevel != null) textWhite else textGray,
                            focusedBorderColor = orangeGlow,
                            unfocusedBorderColor = textGray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = showActivityDropdown,
                        onDismissRequest = { showActivityDropdown = false },
                        containerColor = Color(0xFF2A2A2A)
                    ) {
                        ActivityLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = level.displayName,
                                            color = textWhite,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = level.description,
                                            color = textGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                onClick = {
                                    activityLevel = level
                                    showActivityDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (level) {
                                            ActivityLevel.SEDENTARY -> Icons.Default.Weekend
                                            ActivityLevel.LIGHT -> Icons.Default.DirectionsWalk
                                            ActivityLevel.MODERATE -> Icons.Default.DirectionsRun
                                            ActivityLevel.ACTIVE -> Icons.Default.FitnessCenter
                                            ActivityLevel.VERY_ACTIVE -> Icons.Default.LocalFireDepartment
                                        },
                                        contentDescription = null,
                                        tint = if (activityLevel == level) orangePrimary else textGray
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, weight, height, age, gender, activityLevel, years) },
                enabled = name.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAVE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textGray)
            }
        },
        titleContentColor = textWhite,
        textContentColor = textGray
    )
}