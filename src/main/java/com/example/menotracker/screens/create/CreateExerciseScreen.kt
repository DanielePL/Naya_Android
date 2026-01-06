// app/src/main/java/com/example/myapplicationtest/screens/create/CreateExerciseScreen.kt

package com.example.menotracker.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaBackground

// 🎨 Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)
private val cardBorder = orangeGlow.copy(alpha = 0.5f)
private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1E1E),
        NayaBackground,
        Color(0xFF1a1410)
    )
)

// 💪 Muscle Groups (18 Groups - Detailed)
private val muscleGroups = listOf(
    "Chest (Pectoralis Major/Minor)",
    "Latissimus Dorsi",
    "Upper Trapezius",
    "Middle Trapezius (Transversal)",
    "Lower Trapezius / Rhomboids",
    "Quadriceps",
    "Hamstrings",
    "Glutes",
    "Shoulders (Deltoids - Anterior/Lateral/Posterior)",
    "Biceps",
    "Triceps",
    "Forearms",
    "Calves (Gastrocnemius/Soleus)",
    "Core - Rectus Abdominis",
    "Core - Obliques",
    "Core - Transversus Abdominis",
    "Erector Spinae (Lower Back)",
    "Serratus Anterior"
)

// 🏋️ Equipment Options (Complete List)
private val equipmentOptions = listOf(
    "Barbell",
    "Dumbbell",
    "Kettlebell",
    "Weight Plate",
    "Resistance Band",
    "Cable Machine",
    "Smith Machine",
    "Squat Rack / Power Rack",
    "Bench (Flat)",
    "Bench (Incline)",
    "Bench (Decline)",
    "Adjustable Bench",
    "Pull-Up Bar",
    "Dip Station",
    "Plyo Box",
    "Medicine Ball",
    "Sandbag",
    "Sled",
    "Trap Bar / Hex Bar",
    "Safety Bar",
    "Landmine",
    "EZ Curl Bar",
    "Weightlifting Platform",
    "Gymnastics Rings / TRX",
    "Rowing Machine",
    "Assault Bike / Air Bike",
    "Treadmill",
    "SkiErg",
    "Machine (General)",
    "Ropes (Battle Ropes / Climbing Rope)",
    "Bodyweight"
)

// 📊 Training Parameter Types
enum class TrackingType {
    REPS_SETS_WEIGHT,           // Traditional strength
    DURATION,                    // Planks, holds
    DISTANCE,                    // Running, rowing
    WEIGHT_DISTANCE,            // Farmer's carry
    WEIGHT_DURATION,            // Sandbag hold
    DURATION_DISTANCE,          // Sled push for time
    WEIGHT_DURATION_DISTANCE    // Yoke carry
    // Note: VBT exercises can only be created by admins
}

// 📊 Exercise Data Class
data class ExerciseTemplate(
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val trackingType: TrackingType,
    val notes: String,

    // Advanced Parameters
    val targetRPE: Int? = null,
    val tempo: String? = null // e.g., "2-1-2" (eccentric-pause-concentric)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(
    onNavigateBack: () -> Unit,
    onExerciseSaved: (ExerciseTemplate) -> Unit = {}
) {
    // 📝 Basic Info
    var exerciseName by remember { mutableStateOf("") }
    var primaryMuscle by remember { mutableStateOf("") }
    var secondaryMusclesText by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // 📊 Tracking Type
    var trackingType by remember { mutableStateOf(TrackingType.REPS_SETS_WEIGHT) }

    // 🎯 Advanced Parameters
    var tempo by remember { mutableStateOf("") }
    var targetRPE by remember { mutableStateOf("") }

    // 🎛️ UI State
    var showPrimaryMuscleMenu by remember { mutableStateOf(false) }
    var showSecondaryMuscleMenu by remember { mutableStateOf(false) }
    var showEquipmentMenu by remember { mutableStateOf(false) }
    var showTrackingTypeMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var expandAdvanced by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = orangeGlow
                        )
                        Text("Create Exercise", color = textWhite)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textWhite
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showSaveDialog = true },
                        enabled = exerciseName.isNotEmpty() &&
                                primaryMuscle.isNotEmpty() &&
                                equipment.isNotEmpty()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = if (exerciseName.isNotEmpty() &&
                                    primaryMuscle.isNotEmpty() &&
                                    equipment.isNotEmpty()) {
                                    orangeGlow
                                } else {
                                    textGray
                                }
                            )
                            Text(
                                text = "SAVE",
                                color = if (exerciseName.isNotEmpty() &&
                                    primaryMuscle.isNotEmpty() &&
                                    equipment.isNotEmpty()) {
                                    orangeGlow
                                } else {
                                    textGray
                                },
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
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

                // ========================================
                // 📝 BASIC INFORMATION
                // ========================================
                item {
                    SectionHeader(
                        icon = Icons.Default.Info,
                        title = "BASIC INFORMATION"
                    )
                }

                // Exercise Name
                item {
                    InputCard(
                        label = "EXERCISE NAME",
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        placeholder = "e.g. Barbell Bench Press",
                        icon = Icons.Default.FitnessCenter
                    )
                }

                // ========================================
                // 💪 MUSCLE GROUPS
                // ========================================
                item {
                    SectionHeader(
                        icon = Icons.Default.Accessibility,
                        title = "MUSCLE GROUPS"
                    )
                }

                // Primary Muscle
                item {
                    DropdownField(
                        label = "Primary Muscle Group",
                        value = primaryMuscle,
                        options = muscleGroups,
                        expanded = showPrimaryMuscleMenu,
                        onExpandedChange = { showPrimaryMuscleMenu = it },
                        onOptionSelected = {
                            primaryMuscle = it
                            showPrimaryMuscleMenu = false
                        },
                        placeholder = "Select primary target muscle"
                    )
                }

                // Secondary Muscles
                item {
                    DropdownField(
                        label = "Secondary Muscle Groups (Optional)",
                        value = secondaryMusclesText,
                        options = muscleGroups.filter { it != primaryMuscle },
                        expanded = showSecondaryMuscleMenu,
                        onExpandedChange = { showSecondaryMuscleMenu = it },
                        onOptionSelected = {
                            secondaryMusclesText = if (secondaryMusclesText.isEmpty()) {
                                it
                            } else {
                                "$secondaryMusclesText, $it"
                            }
                            showSecondaryMuscleMenu = false
                        },
                        placeholder = "e.g. Triceps, Shoulders",
                        allowMultiple = true
                    )
                }

                // ========================================
                // 🏋️ EQUIPMENT
                // ========================================
                item {
                    SectionHeader(
                        icon = Icons.Default.FitnessCenter,
                        title = "EQUIPMENT"
                    )
                }

                // Equipment Dropdown
                item {
                    DropdownField(
                        label = "Equipment Required",
                        value = equipment,
                        options = equipmentOptions,
                        expanded = showEquipmentMenu,
                        onExpandedChange = { showEquipmentMenu = it },
                        onOptionSelected = {
                            equipment = it
                            showEquipmentMenu = false
                        },
                        placeholder = "Select required equipment"
                    )
                }

                // ========================================
                // 📊 TRACKING PARAMETERS
                // ========================================
                item {
                    SectionHeader(
                        icon = Icons.Default.Analytics,
                        title = "TRACKING PARAMETERS"
                    )
                }

                // Tracking Type
                item {
                    TrackingTypeSelector(
                        selectedType = trackingType,
                        onTypeSelected = { trackingType = it }
                    )
                }

                // ========================================
                // 🎯 ADVANCED PARAMETERS (Expandable)
                // ========================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = BorderStroke(1.5.dp, cardBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = orangeGlow
                                    )
                                    Text(
                                        "ADVANCED PARAMETERS",
                                        color = orangeGlow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                                IconButton(onClick = { expandAdvanced = !expandAdvanced }) {
                                    Icon(
                                        imageVector = if (expandAdvanced) {
                                            Icons.Default.ExpandLess
                                        } else {
                                            Icons.Default.ExpandMore
                                        },
                                        contentDescription = null,
                                        tint = textGray
                                    )
                                }
                            }

                            // Expandable Content
                            if (expandAdvanced) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Divider(color = textGray.copy(alpha = 0.3f))

                                    // Tempo
                                    OutlinedTextField(
                                        value = tempo,
                                        onValueChange = { tempo = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Tempo (Optional)", color = textGray) },
                                        placeholder = {
                                            Text(
                                                "e.g., 2-1-2 (eccentric-pause-concentric)",
                                                color = textGray.copy(alpha = 0.5f)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = textGray
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

                                    // Target RPE
                                    OutlinedTextField(
                                        value = targetRPE,
                                        onValueChange = { targetRPE = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Target RPE (Optional)", color = textGray) },
                                        placeholder = {
                                            Text(
                                                "Rate of Perceived Exertion (1-10)",
                                                color = textGray.copy(alpha = 0.5f)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = textGray
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

                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }

                // ========================================
                // 📝 NOTES
                // ========================================
                item {
                    SectionHeader(
                        icon = Icons.Default.Notes,
                        title = "NOTES & CUES"
                    )
                }

                // Notes TextField
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
                                "EXECUTION NOTES",
                                color = textGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                placeholder = {
                                    Text(
                                        "Add execution tips, coaching cues, or exercise variations...\n\ne.g., \"Keep chest up, drive through heels, maintain neutral spine\"",
                                        color = textGray.copy(alpha = 0.5f),
                                        fontSize = 13.sp
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textWhite,
                                    unfocusedTextColor = textWhite,
                                    focusedBorderColor = orangeGlow,
                                    unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                                    cursorColor = orangeGlow,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                maxLines = 6,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = orangeGlow.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = orangeGlow,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "ProTip",
                                    color = orangeGlow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Custom exercises will be available in your exercise library and can be added to any workout or program.",
                                    color = textGray,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ========================================
    // 💾 SAVE CONFIRMATION DIALOG
    // ========================================
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Save Exercise?",
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "You're about to save:",
                        color = textGray,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    // Exercise Name
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = orangeGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            exerciseName,
                            color = orangeGlow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = textGray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))

                    // Details
                    DetailRow(Icons.Default.Accessibility, "Primary", primaryMuscle)
                    if (secondaryMusclesText.isNotEmpty()) {
                        DetailRow(Icons.Default.Group, "Secondary", secondaryMusclesText)
                    }
                    DetailRow(Icons.Default.Construction, "Equipment", equipment)
                    DetailRow(Icons.Default.Analytics, "Tracking", trackingType.name.replace('_', ' '))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val exercise = ExerciseTemplate(
                            name = exerciseName,
                            primaryMuscle = primaryMuscle,
                            secondaryMuscles = secondaryMusclesText.split(",").map { it.trim() },
                            equipment = equipment,
                            trackingType = trackingType,
                            notes = notes,
                            targetRPE = targetRPE.toIntOrNull(),
                            tempo = tempo.ifEmpty { null }
                        )
                        onExerciseSaved(exercise)
                        showSaveDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = orangePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Text("SAVE EXERCISE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel", color = textGray, fontWeight = FontWeight.Medium)
                }
            },
            titleContentColor = textWhite,
            textContentColor = textGray
        )
    }
}

// ========================================
// 🎨 COMPOSABLE COMPONENTS
// ========================================

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = orangeGlow,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            color = orangeGlow,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun InputCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textGray,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    color = textGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        placeholder,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit,
    placeholder: String,
    allowMultiple: Boolean = false
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label, color = textGray) },
            placeholder = {
                Text(
                    placeholder,
                    color = textGray.copy(alpha = 0.5f)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textWhite,
                unfocusedTextColor = textWhite,
                focusedBorderColor = orangeGlow,
                unfocusedBorderColor = textGray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .background(Color(0xFF1E1E1E))
                .heightIn(max = 400.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = textWhite, fontSize = 14.sp) },
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.background(
                        if (value.contains(option)) {
                            orangeGlow.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun TrackingTypeSelector(
    selectedType: TrackingType,
    onTypeSelected: (TrackingType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.5.dp, cardBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "TRACKING TYPE",
                color = textGray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackingTypeOption(
                    type = TrackingType.REPS_SETS_WEIGHT,
                    label = "Reps, Sets & Weight",
                    description = "Traditional strength training",
                    icon = Icons.Default.FitnessCenter,
                    selected = selectedType == TrackingType.REPS_SETS_WEIGHT,
                    onClick = { onTypeSelected(TrackingType.REPS_SETS_WEIGHT) }
                )

                TrackingTypeOption(
                    type = TrackingType.DURATION,
                    label = "Duration",
                    description = "Time-based (planks, holds)",
                    icon = Icons.Default.Timer,
                    selected = selectedType == TrackingType.DURATION,
                    onClick = { onTypeSelected(TrackingType.DURATION) }
                )

                TrackingTypeOption(
                    type = TrackingType.DISTANCE,
                    label = "Distance",
                    description = "Meters/km (running, rowing)",
                    icon = Icons.Default.DirectionsRun,
                    selected = selectedType == TrackingType.DISTANCE,
                    onClick = { onTypeSelected(TrackingType.DISTANCE) }
                )

                TrackingTypeOption(
                    type = TrackingType.WEIGHT_DISTANCE,
                    label = "Weight + Distance",
                    description = "Farmer's carry, sled drag",
                    icon = Icons.Default.AltRoute,
                    selected = selectedType == TrackingType.WEIGHT_DISTANCE,
                    onClick = { onTypeSelected(TrackingType.WEIGHT_DISTANCE) }
                )

                TrackingTypeOption(
                    type = TrackingType.WEIGHT_DURATION,
                    label = "Weight + Duration",
                    description = "Sandbag holds, static lifts",
                    icon = Icons.Default.Schedule,
                    selected = selectedType == TrackingType.WEIGHT_DURATION,
                    onClick = { onTypeSelected(TrackingType.WEIGHT_DURATION) }
                )
            }
        }
    }
}

@Composable
private fun TrackingTypeOption(
    type: TrackingType,
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                orangeGlow.copy(alpha = 0.2f)
            } else {
                Color(0xFF2A2A2A)
            }
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) orangeGlow else textGray.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) orangeGlow else textGray,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = if (selected) orangeGlow else textWhite,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    description,
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = orangeGlow,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                color = textGray,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                value,
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}