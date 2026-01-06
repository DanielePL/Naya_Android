package com.example.menotracker.screens.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaOrangeGlow

// Design System colors
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)

/** Simple data class for exercise picker */
data class ExerciseInfo(
    val id: String,
    val name: String,
    val equipment: String,
    val muscleGroup: String
)

/**
 * Dialog to pick an exercise from the library
 */
@Composable
fun ExercisePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onExerciseSelected: (ExerciseInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var exercises by remember { mutableStateOf<List<ExerciseInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load exercises from repository
    LaunchedEffect(Unit) {
        try {
            // Initialize repository if not done
            com.example.menotracker.data.ExerciseRepository.initialize()

            val loadedExercises = com.example.menotracker.data.ExerciseRepository.createdExercises
            exercises = loadedExercises.map { ex: com.example.menotracker.data.models.Exercise ->
                ExerciseInfo(
                    id = ex.id,
                    name = ex.name,
                    equipment = ex.equipment?.firstOrNull() ?: "Bodyweight",
                    muscleGroup = ex.muscleCategory ?: ""
                )
            }
            isLoading = false
        } catch (e: Exception) {
            android.util.Log.e("ExercisePicker", "Failed to load exercises: ${e.message}")
            isLoading = false
        }
    }

    val filteredExercises = remember(searchQuery, exercises) {
        if (searchQuery.isBlank()) exercises
        else exercises.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.muscleGroup.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = textWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search exercises...", color = textGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = textGray)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        focusedBorderColor = orangeGlow,
                        unfocusedBorderColor = textGray.copy(alpha = 0.3f)
                    )
                )

                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = orangeGlow)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExercises.size) { index ->
                            val exercise = filteredExercises[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExerciseSelected(exercise) },
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            exercise.name,
                                            color = textWhite,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "${exercise.muscleGroup} • ${exercise.equipment}",
                                            color = textGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Select",
                                        tint = orangeGlow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textGray)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = textWhite
    )
}

/**
 * Dialog to save workout template changes
 */
@Composable
fun SaveTemplateDialog(
    workoutName: String,
    onDismiss: () -> Unit,
    onKeepOriginal: () -> Unit,
    onSaveAsNew: (String) -> Unit,
    onUpdateExisting: () -> Unit
) {
    var newTemplateName by remember { mutableStateOf("$workoutName (Modified)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Save Changes?", color = textWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "You modified this workout",
                    color = textGray,
                    fontSize = 14.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Option 1: Keep original
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onKeepOriginal() },
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = textGray)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Don't save changes", color = textWhite, fontWeight = FontWeight.Medium)
                            Text("Keep original template", color = textGray, fontSize = 12.sp)
                        }
                    }
                }

                // Option 2: Update existing
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpdateExisting() },
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = orangeGlow)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Update \"$workoutName\"", color = textWhite, fontWeight = FontWeight.Medium)
                            Text("Replace with modified version", color = textGray, fontSize = 12.sp)
                        }
                    }
                }

                // Option 3: Save as new
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Green)
                            Text("Save as new template", color = textWhite, fontWeight = FontWeight.Medium)
                        }
                        OutlinedTextField(
                            value = newTemplateName,
                            onValueChange = { newTemplateName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Template name", color = textGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = Color.Green,
                                unfocusedBorderColor = textGray.copy(alpha = 0.3f)
                            )
                        )
                        Button(
                            onClick = { onSaveAsNew(newTemplateName) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save New Template", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = textWhite
    )
}