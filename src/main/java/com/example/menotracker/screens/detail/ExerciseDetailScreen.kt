// app/src/main/java/com/example/myapplicationtest/screens/detail/ExerciseDetailScreen.kt

package com.example.menotracker.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.Exercise
import androidx.compose.foundation.BorderStroke
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

// ═══════════════════════════════════════════════════════════════
// DESIGN SYSTEM
// ═══════════════════════════════════════════════════════════════

private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color.White
private val textGray = Color.Gray
private val surfaceColor = Color(0xFF1A1A1A)
private val backgroundColor = Color(0xFF0F0F0F)
private val vbtPowerColor = Color(0xFFFFD700) // Gold for Power
private val vbtTechniqueColor = Color(0xFF00D4FF) // Cyan for Technique

// ═══════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textWhite
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Exercise Details",
                color = textWhite,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Exercise Name
            Text(
                text = exercise.name,
                color = textWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            // Badges Row
            BadgesSection(exercise)

            // ═══════════════════════════════════════════════════════════════
            // MEASUREMENT CAPABILITIES SECTION (VBT)
            // ═══════════════════════════════════════════════════════════════
            if (exercise.supportsPowerScore || exercise.supportsTechniqueScore) {
                MeasurementCapabilitiesSection(exercise)
            }

            // Muscle Groups Section
            if (exercise.mainMuscle != null || !exercise.secondaryMuscles.isNullOrEmpty()) {
                MuscleGroupsSection(exercise)
            }

            // Equipment Section
            if (!exercise.equipment.isNullOrEmpty()) {
                EquipmentSection(exercise)
            }

            // Prescription Section (Tempo & Rest)
            if (exercise.tempo != null || exercise.restTimeInSeconds != null) {
                PrescriptionSection(exercise)
            }

            // Tracking Parameters Section
            TrackingParametersSection(exercise)

            // Tutorial Section
            if (!exercise.tutorial.isNullOrEmpty()) {
                TutorialSection(exercise.tutorial)
            }

            // Notes Section
            if (!exercise.notes.isNullOrEmpty()) {
                NotesSection(exercise.notes)
            }

            // Video Section
            if (!exercise.videoUrl.isNullOrEmpty()) {
                VideoSection(exercise.videoUrl)
            }

            // Bottom Spacer
            Spacer(Modifier.height(100.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MEASUREMENT CAPABILITIES SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MeasurementCapabilitiesSection(exercise: Exercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MEASUREMENT CAPABILITIES",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            // Power Score
            if (exercise.supportsPowerScore) {
                MeasurementCapabilityCard(
                    icon = "⚡",
                    title = "Power Score (VBT)",
                    description = "Measures bar velocity and explosive power output",
                    color = vbtPowerColor
                )
            }

            // Technique Score
            if (exercise.supportsTechniqueScore) {
                MeasurementCapabilityCard(
                    icon = "✓",
                    title = "Technique Score",
                    description = "Analyzes bar path and movement quality",
                    color = vbtTechniqueColor
                )
            }

            // VBT Info Note
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = orangePrimary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Use your phone's camera to track these metrics during your sets",
                        color = textWhite,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementCapabilityCard(
    icon: String,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, color)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Text Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = textGray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// BADGES SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun BadgesSection(exercise: Exercise) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Main Muscle Badge
        exercise.mainMuscle?.let { muscle ->
            CategoryChip(
                label = muscle,
                color = orangePrimary
            )
        }

        // Equipment Badge (first one)
        exercise.equipment?.firstOrNull()?.let { equip ->
            CategoryChip(
                label = equip,
                color = surfaceColor
            )
        }

        // VBT Category Badge
        exercise.vbtCategory?.let { category ->
            CategoryChip(
                label = category,
                color = orangeGlow
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = if (color == surfaceColor) 0.8f else 0.2f)
    ) {
        Text(
            text = label,
            color = if (color == surfaceColor) textWhite else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// MUSCLE GROUPS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MuscleGroupsSection(exercise: Exercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    text = "MUSCLE GROUPS",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            // Primary Muscle
            exercise.mainMuscle?.let { primary ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("Primary:", color = textGray, fontSize = 14.sp, modifier = Modifier.width(80.dp))
                    Text(primary, color = textWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Secondary Muscles
            if (!exercise.secondaryMuscles.isNullOrEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("Secondary:", color = textGray, fontSize = 14.sp, modifier = Modifier.width(80.dp))
                    Text(
                        exercise.secondaryMuscles.joinToString(", "),
                        color = textWhite,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EQUIPMENT SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EquipmentSection(exercise: Exercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "EQUIPMENT",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            Text(
                exercise.equipment?.joinToString(", ") ?: "",
                color = textWhite,
                fontSize = 14.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PRESCRIPTION SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PrescriptionSection(exercise: Exercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "PRESCRIPTION",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Tempo
                exercise.tempo?.let { tempo ->
                    PrescriptionItem(
                        icon = Icons.Default.Speed,
                        label = "Tempo",
                        value = tempo
                    )
                }

                // Rest Time
                exercise.restTimeInSeconds?.let { rest ->
                    PrescriptionItem(
                        icon = Icons.Default.Timer,
                        label = "Rest",
                        value = "${rest}s"
                    )
                }
            }
        }
    }
}

@Composable
private fun PrescriptionItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = orangePrimary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            value,
            color = textWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = textGray,
            fontSize = 12.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// TRACKING PARAMETERS SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TrackingParametersSection(exercise: Exercise) {
    val trackingParams = buildList {
        if (exercise.trackSets) add("Sets")
        if (exercise.trackReps) add("Reps")
        if (exercise.trackWeight) add("Weight")
        if (exercise.trackRpe) add("RPE")
        if (exercise.trackDuration) add("Duration")
        if (exercise.trackDistance) add("Distance")
    }

    if (trackingParams.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "TRACKING",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            // Tracking Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                trackingParams.forEach { param ->
                    TrackingChip(param)
                }
            }
        }
    }
}

@Composable
private fun TrackingChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = orangePrimary.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = orangeGlow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TUTORIAL SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TutorialSection(tutorial: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "HOW TO PERFORM",
            color = orangeGlow,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )

        val steps = tutorial.split("\n").filter { it.isNotBlank() }

        steps.forEachIndexed { index, step ->
            InstructionCard(
                number = index + 1,
                text = step.trim().removePrefix("${index + 1}. ").trim()
            )
        }
    }
}

@Composable
private fun InstructionCard(number: Int, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Number Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = orangePrimary
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Instruction Text
            Text(
                text = text,
                color = textWhite,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// NOTES SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun NotesSection(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "NOTES & TIPS",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = notes,
                color = textWhite,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// VIDEO SECTION
// ═══════════════════════════════════════════════════════════════

@Composable
private fun VideoSection(videoUrl: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "VIDEO TUTORIAL",
                    color = orangeGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            Button(
                onClick = { /* TODO: Open video */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Watch Tutorial",
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                videoUrl,
                color = textGray,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}