// app/src/main/java/com/example/myapplicationtest/screens/create/CreateProgramScreen.kt

package com.example.menotracker.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.ILBMode
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

// Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)
private val cardBorder = orangeGlow.copy(alpha = 0.5f)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2a1f1a),
        Color(0xFF0f0f0f),
        Color(0xFF1a1410)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProgramScreen(
    onNavigateBack: () -> Unit,
    onProgramSaved: () -> Unit = {}
) {
    var programName by remember { mutableStateOf("") }
    var programDescription by remember { mutableStateOf("") }
    var durationWeeks by remember { mutableStateOf("12") }
    var workoutsPerWeek by remember { mutableStateOf("4") }
    var difficulty by remember { mutableStateOf("Intermediate") }

    // ILB Settings
    var ilbMode by remember { mutableStateOf(ILBMode.OFF) }
    var ilbIntervalWeeks by remember { mutableStateOf("6") }

    var showSaveDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Program", color = textWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textWhite
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showSaveDialog = true },
                        enabled = programName.isNotEmpty() &&
                                durationWeeks.isNotEmpty() &&
                                workoutsPerWeek.isNotEmpty() &&
                                !isSaving
                    ) {
                        Text(
                            text = "SAVE",
                            color = if (programName.isNotEmpty() &&
                                durationWeeks.isNotEmpty() &&
                                workoutsPerWeek.isNotEmpty()) {
                                orangeGlow
                            } else {
                                textGray
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Header
                item {
                    Text(
                        text = "BASIC INFORMATION",
                        color = orangeGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Program Name
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = BorderStroke(1.5.dp, cardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PROGRAM NAME",
                                color = textGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )

                            OutlinedTextField(
                                value = programName,
                                onValueChange = { programName = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "e.g. Strength & Hypertrophy",
                                        color = textGray.copy(alpha = 0.5f)
                                    )
                                },
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
                        }
                    }
                }

                // Description
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = BorderStroke(1.5.dp, cardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DESCRIPTION (OPTIONAL)",
                                color = textGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )

                            OutlinedTextField(
                                value = programDescription,
                                onValueChange = { programDescription = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                placeholder = {
                                    Text(
                                        "Describe your program...",
                                        color = textGray.copy(alpha = 0.5f)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textWhite,
                                    unfocusedTextColor = textWhite,
                                    focusedBorderColor = orangeGlow,
                                    unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                                    cursorColor = orangeGlow
                                ),
                                maxLines = 5,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Duration Section
                item {
                    Text(
                        text = "DURATION",
                        color = orangeGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Duration in Weeks
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = durationWeeks,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                    durationWeeks = it
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Duration", color = textGray) },
                            placeholder = { Text("12", color = textGray.copy(alpha = 0.5f)) },
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

                        // Unit Selector
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Unit",
                                color = textGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UnitButton(
                                    text = "Weeks",
                                    isSelected = true,
                                    onClick = { },
                                    modifier = Modifier.weight(1f)
                                )
                                UnitButton(
                                    text = "Months",
                                    isSelected = false,
                                    onClick = { },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Workouts Per Week
                item {
                    OutlinedTextField(
                        value = workoutsPerWeek,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 1) {
                                workoutsPerWeek = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Workouts Per Week", color = textGray) },
                        placeholder = { Text("4", color = textGray.copy(alpha = 0.5f)) },
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

                // Difficulty Level
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "DIFFICULTY LEVEL",
                            color = orangeGlow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                                DifficultyChip(
                                    text = level,
                                    isSelected = difficulty == level,
                                    onClick = { difficulty = level },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ILB Settings Section
                item {
                    Text(
                        text = "PROGRESSION TESTING (ILB)",
                        color = orangeGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = BorderStroke(1.5.dp, cardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Mode Description
                            Text(
                                text = "ILB (Individuelles Leistungsbild) tests your strength periodically via AMRAP sets and automatically adjusts your working weights.",
                                color = textGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            // ILB Mode Selection
                            Text(
                                text = "TEST MODE",
                                color = textGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ILBModeChip(
                                    text = "Off",
                                    description = "Manual progression",
                                    isSelected = ilbMode == ILBMode.OFF,
                                    onClick = { ilbMode = ILBMode.OFF },
                                    modifier = Modifier.weight(1f)
                                )
                                ILBModeChip(
                                    text = "Manual",
                                    description = "Trigger tests yourself",
                                    isSelected = ilbMode == ILBMode.MANUAL,
                                    onClick = { ilbMode = ILBMode.MANUAL },
                                    modifier = Modifier.weight(1f)
                                )
                                ILBModeChip(
                                    text = "Auto",
                                    description = "Scheduled tests",
                                    isSelected = ilbMode == ILBMode.AUTO,
                                    onClick = { ilbMode = ILBMode.AUTO },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Interval Selection (only shown for AUTO mode)
                            if (ilbMode == ILBMode.AUTO) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TEST INTERVAL",
                                        color = textGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Every",
                                            color = textGray,
                                            fontSize = 14.sp
                                        )

                                        OutlinedTextField(
                                            value = ilbIntervalWeeks,
                                            onValueChange = {
                                                if (it.all { char -> char.isDigit() } && it.length <= 2) {
                                                    val num = it.toIntOrNull() ?: 0
                                                    if (num in 0..12) {
                                                        ilbIntervalWeeks = it
                                                    }
                                                }
                                            },
                                            modifier = Modifier.width(70.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = textWhite,
                                                unfocusedTextColor = textWhite,
                                                focusedBorderColor = orangeGlow,
                                                unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                                                cursorColor = orangeGlow
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        Text(
                                            text = "weeks",
                                            color = textGray,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Text(
                                        text = "Recommended: 6-8 weeks for optimal strength gains",
                                        color = textGray.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackground
                        ),
                        border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = orangeGlow,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "You'll be able to add workouts to each week after creating the program.",
                                color = textGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarViewWeek,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Create Program?",
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "You're about to create:",
                        color = textGray,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        programName,
                        color = orangeGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    val ilbModeText = when (ilbMode) {
                        ILBMode.OFF -> "ILB: Off"
                        ILBMode.MANUAL -> "ILB: Manual"
                        ILBMode.AUTO -> "ILB: Auto (every $ilbIntervalWeeks weeks)"
                    }
                    Text(
                        "• $durationWeeks weeks\n• $workoutsPerWeek workouts per week\n• $difficulty level\n• $ilbModeText",
                        color = textGray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Save program to database
                        showSaveDialog = false
                        onProgramSaved()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = orangePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CREATE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = textGray)
                }
            },
            titleContentColor = textWhite,
            textContentColor = textGray
        )
    }
}

@Composable
private fun UnitButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) orangePrimary else textGray.copy(alpha = 0.2f),
            contentColor = if (isSelected) textWhite else textGray
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DifficultyChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) orangePrimary.copy(alpha = 0.2f) else cardBackground
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) orangePrimary else textGray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) orangeGlow else textGray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ILBModeChip(
    text: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) orangePrimary.copy(alpha = 0.2f) else cardBackground.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) orangePrimary else textGray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                color = if (isSelected) orangeGlow else textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = description,
                color = textGray,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}