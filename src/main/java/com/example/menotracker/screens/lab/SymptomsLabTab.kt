package com.example.menotracker.screens.lab

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Symptoms Lab Tab
 *
 * Displays symptom tracking data for menopause symptoms:
 * - Hot flashes
 * - Night sweats
 * - Mood changes
 * - Energy levels
 * - Brain fog
 * - Joint pain
 */

// Design colors
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderLight = Color(0xFFC4B5FD)
private val pinkAccent = Color(0xFFEC4899)
private val tealAccent = Color(0xFF14B8A6)
private val orangeWarm = Color(0xFFF97316)
private val yellowEnergy = Color(0xFFFBBF24)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)
private val cardBg = Color(0xFF1E1E1E)

// Symptom types
enum class MenopauseSymptom(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    HOT_FLASH("Hitzewallung", Icons.Default.Whatshot, orangeWarm),
    NIGHT_SWEAT("Nachtschweiß", Icons.Default.NightsStay, Color(0xFF6366F1)),
    MOOD_SWING("Stimmung", Icons.Default.Mood, pinkAccent),
    FATIGUE("Müdigkeit", Icons.Default.Battery3Bar, yellowEnergy),
    BRAIN_FOG("Brain Fog", Icons.Default.Cloud, Color(0xFF94A3B8)),
    JOINT_PAIN("Gelenkschmerzen", Icons.Default.Accessibility, Color(0xFFEF4444)),
    SLEEP_ISSUE("Schlafprobleme", Icons.Default.Bedtime, lavenderLight),
    ANXIETY("Unruhe", Icons.Default.Psychology, tealAccent)
}

// Sample data class
data class SymptomEntry(
    val symptom: MenopauseSymptom,
    val intensity: Int, // 1-10
    val date: LocalDate,
    val notes: String? = null
)

@Composable
fun SymptomsLabTab() {
    // Sample data - will be replaced with real data from repository
    val recentSymptoms = remember {
        listOf(
            SymptomEntry(MenopauseSymptom.HOT_FLASH, 7, LocalDate.now(), "Nach dem Kaffee"),
            SymptomEntry(MenopauseSymptom.MOOD_SWING, 5, LocalDate.now()),
            SymptomEntry(MenopauseSymptom.FATIGUE, 6, LocalDate.now().minusDays(1)),
            SymptomEntry(MenopauseSymptom.NIGHT_SWEAT, 8, LocalDate.now().minusDays(1), "2x aufgewacht"),
            SymptomEntry(MenopauseSymptom.BRAIN_FOG, 4, LocalDate.now().minusDays(2))
        )
    }

    val symptomStats = remember {
        MenopauseSymptom.entries.associateWith { symptom ->
            recentSymptoms.filter { it.symptom == symptom }.size
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Summary Card
        item {
            SymptomSummaryCard(
                totalSymptoms = recentSymptoms.size,
                mostCommon = symptomStats.maxByOrNull { it.value }?.key,
                avgIntensity = recentSymptoms.map { it.intensity }.average().toFloat()
            )
        }

        // Quick Log Button
        item {
            QuickLogSymptomButton(
                onLogSymptom = { /* Navigate to symptom logging */ }
            )
        }

        // Symptom Overview
        item {
            Text(
                text = "Symptom-Übersicht (7 Tage)",
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Symptom Grid
        item {
            SymptomOverviewGrid(symptomStats = symptomStats)
        }

        // Recent Entries
        item {
            Text(
                text = "Letzte Einträge",
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(recentSymptoms) { entry ->
            SymptomEntryCard(entry = entry)
        }

        // Spacer at bottom
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SymptomSummaryCard(
    totalSymptoms: Int,
    mostCommon: MenopauseSymptom?,
    avgIntensity: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Diese Woche",
                color = lavenderLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Total Symptoms
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalSymptoms",
                        color = textWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Einträge",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }

                // Most Common
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    mostCommon?.let { symptom ->
                        Icon(
                            imageVector = symptom.icon,
                            contentDescription = null,
                            tint = symptom.color,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = symptom.displayName,
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Average Intensity
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f", avgIntensity),
                        color = textWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ø Intensität",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickLogSymptomButton(
    onLogSymptom: () -> Unit
) {
    Button(
        onClick = onLogSymptom,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = lavenderPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Symptom erfassen",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SymptomOverviewGrid(
    symptomStats: Map<MenopauseSymptom, Int>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenopauseSymptom.entries.take(4).forEach { symptom ->
                SymptomStatChip(
                    symptom = symptom,
                    count = symptomStats[symptom] ?: 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenopauseSymptom.entries.drop(4).forEach { symptom ->
                SymptomStatChip(
                    symptom = symptom,
                    count = symptomStats[symptom] ?: 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SymptomStatChip(
    symptom: MenopauseSymptom,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = cardBg
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = symptom.icon,
                contentDescription = null,
                tint = symptom.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SymptomEntryCard(
    entry: SymptomEntry
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symptom Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(entry.symptom.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entry.symptom.icon,
                    contentDescription = null,
                    tint = entry.symptom.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.symptom.displayName,
                    color = textWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                entry.notes?.let { notes ->
                    Text(
                        text = notes,
                        color = textGray,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = entry.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    color = textGray.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            // Intensity
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${entry.intensity}",
                    color = getIntensityColor(entry.intensity),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/10",
                    color = textGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun getIntensityColor(intensity: Int): Color {
    return when {
        intensity <= 3 -> Color(0xFF10B981) // Green
        intensity <= 6 -> Color(0xFFFBBF24) // Yellow
        else -> Color(0xFFEF4444) // Red
    }
}
