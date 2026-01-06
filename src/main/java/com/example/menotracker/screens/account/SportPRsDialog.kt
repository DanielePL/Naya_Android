// app/src/main/java/com/example/myapplicationtest/screens/account/SportPRsDialog.kt

package com.example.menotracker.screens.account

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportPRsDialog(
    sport: String,
    existingPRs: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    // Get PRs for the specific sport
    val prs = remember(sport) { getPRsForSport(sport) }
    val prValues = remember(existingPRs) {
        mutableStateMapOf<String, String>().apply {
            prs.forEach { pr ->
                this[pr] = existingPRs[pr] ?: ""
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = getSportEmoji(sport),
                            fontSize = 32.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = sport,
                            color = textWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personal Records",
                            color = textGray,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textWhite
                        )
                    }
                }
                Divider(color = textGray.copy(alpha = 0.2f))

                // PRs List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    items(prs.size) { index ->
                        val pr = prs[index]
                        PRInputField(
                            label = pr,
                            value = prValues[pr] ?: "",
                            onValueChange = { prValues[pr] = it },
                            unit = getUnitForPR(pr)
                        )
                    }
                }

                Divider(color = textGray.copy(alpha = 0.2f))

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textGray
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onSave(prValues.toMap())
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = orangePrimary
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PRInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            if (it.all { char -> char.isDigit() || char == '.' }) {
                onValueChange(it)
            }
        },
        label = { Text(label, color = textGray) },
        suffix = { Text(unit, color = textGray) },
        modifier = Modifier.fillMaxWidth(),
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

// ═══════════════════════════════════════════════════════════════
// SPORT-SPECIFIC PRS
// ═══════════════════════════════════════════════════════════════

private fun getPRsForSport(sport: String): List<String> {
    return when (sport) {
        "Olympic Weightlifting" -> listOf(
            "Snatch",
            "Clean & Jerk",
            "Clean",
            "Jerk",
            "Power Snatch",
            "Power Clean",
            "Front Squat",
            "Back Squat",
            "Overhead Squat"
        )

        "Powerlifting" -> listOf(
            "Squat",
            "Bench Press",
            "Deadlift",
            "Total (SBD)",
            "Wilks Score"
        )

        "Strongman" -> listOf(
            "Atlas Stone",
            "Log Press",
            "Axle Deadlift",
            "Yoke Walk",
            "Farmer's Walk",
            "Tire Flip",
            "Keg Toss",
            "Car Deadlift"
        )

        "CrossFit" -> listOf(
            "Fran",
            "Grace",
            "Isabel",
            "Helen",
            "Cindy",
            "Murph",
            "Clean & Jerk",
            "Snatch",
            "Back Squat",
            "Deadlift",
            "Strict Pull-ups"
        )

        "General Strength" -> listOf(
            "Squat",
            "Deadlift",
            "Bench Press",
            "Overhead Press",
            "Weighted Pull-ups",
            "Weighted Dips",
            "Barbell Row",
            "Front Squat"
        )

        "Hyrox" -> listOf(
            "1km SkiErg",
            "50m Sled Push",
            "50m Sled Pull",
            "80m Burpee Broad Jump",
            "1km Row",
            "200m Farmers Carry",
            "100m Sandbag Lunges",
            "75/100 Wall Balls",
            "Total Race Time"
        )

        else -> emptyList()
    }
}

private fun getUnitForPR(pr: String): String {
    return when {
        // Reps (CrossFit gymnastics - bodyweight only)
        pr in listOf("Cindy", "Strict Pull-ups") -> "reps"
        pr.contains("Wall Balls") -> "reps"

        // Wilks (Powerlifting)
        pr == "Wilks Score" -> "pts"

        // Time-based (CrossFit WODs)
        pr in listOf("Fran", "Grace", "Isabel", "Helen", "Murph", "Total Race Time") -> "min:sec"

        // Time-based (Hyrox - specific to cardio/sled/carries)
        pr.contains("SkiErg") || pr.contains("1km Row") || pr == "1km Row" -> "min:sec"
        pr.contains("Sled Push") || pr.contains("Sled Pull") -> "sec"
        pr.contains("Farmers Carry") || pr.contains("Sandbag Lunges") || pr.contains("Burpee Broad Jump") -> "sec"

        // Everything else is weight (including Weighted Pull-ups, Weighted Dips, Barbell Row)
        else -> "kg"
    }
}

private fun getSportEmoji(sport: String): String {
    return when (sport) {
        "Olympic Weightlifting" -> "🏋️"
        "Powerlifting" -> "💪"
        "Strongman" -> "🦾"
        "CrossFit" -> "🔥"
        "General Strength" -> "⚡"
        "Hyrox" -> "🏃"
        else -> "🏆"
    }
}