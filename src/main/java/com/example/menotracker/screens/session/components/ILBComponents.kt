package com.example.menotracker.screens.session.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.ILBTestResult
import com.example.menotracker.data.models.ILBWarmupSet
import com.example.menotracker.data.models.WeightChangePreview

// ILB Design Colors
private val ilbViolet = Color(0xFFA78BFA)
private val ilbVioletGlow = Color(0xFFC4B5FD)
private val ilbGreen = Color(0xFF00C853)
private val ilbRed = Color(0xFFFF5252)
private val cardBackground = Color(0xFF1a1410)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)

/**
 * ILB Test Week Banner
 *
 * Zeigt am Anfang eines Workouts an, dass dies eine ILB Test-Woche ist.
 * Erklärt dem User was zu tun ist.
 */
@Composable
fun ILBTestWeekBanner(
    weekNumber: Int,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    var isVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardBackground
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ilbViolet.copy(alpha = 0.15f),
                                ilbVioletGlow.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = ilbViolet.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Assessment,
                                contentDescription = null,
                                tint = ilbViolet,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ILB Test Woche $weekNumber",
                                    color = ilbViolet,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Individuelles Leistungsbild",
                                    color = textGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (onDismiss != null) {
                            IconButton(
                                onClick = {
                                    isVisible = false
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Schließen",
                                    tint = textGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instructions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        InstructionItem(
                            number = "1",
                            text = "Warmup nach Protokoll absolvieren"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InstructionItem(
                            number = "2",
                            text = "Bei MAX-Sets: Maximale Wiederholungen ausführen"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InstructionItem(
                            number = "3",
                            text = "Nach dem Workout werden deine Gewichte automatisch angepasst"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "💪 Gib alles - dein neuer 1RM wird berechnet!",
                        color = ilbVioletGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionItem(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ilbViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = ilbViolet,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = textWhite,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * ILB Test Result Card
 *
 * Zeigt das Ergebnis eines AMRAP-Tests an.
 */
@Composable
fun ILBTestResultCard(
    result: ILBTestResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            if (result.isImproved) ilbGreen.copy(alpha = 0.1f) else ilbViolet.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise name and test details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.exerciseName,
                    color = textWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${result.testWeight.toInt()}kg × ${result.testReps} reps",
                    color = textGray,
                    fontSize = 13.sp
                )
            }

            // New 1RM
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${result.new1RM.toInt()}kg",
                    color = if (result.isImproved) ilbGreen else textWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = result.getDisplayMessage(),
                    color = when {
                        result.isImproved -> ilbGreen
                        result.isDeclined -> ilbRed
                        else -> textGray
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * ILB Session Summary
 *
 * Zeigt eine Übersicht aller ILB-Testergebnisse am Ende des Workouts.
 */
@Composable
fun ILBSessionSummary(
    results: List<ILBTestResult>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = ilbViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ILB Test Ergebnisse",
                        color = textWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = textGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary stats
            val improvements = results.count { it.isImproved }
            val totalChange = results.mapNotNull { it.changeKg }.sum()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ilbViolet.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${results.size}",
                    label = "Tests"
                )
                StatItem(
                    value = "$improvements",
                    label = "Verbessert",
                    valueColor = if (improvements > 0) ilbGreen else textWhite
                )
                StatItem(
                    value = "${if (totalChange > 0) "+" else ""}${totalChange.toInt()}kg",
                    label = "Gesamt",
                    valueColor = if (totalChange > 0) ilbGreen else if (totalChange < 0) ilbRed else textWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Individual results
            results.forEach { result ->
                ILBTestResultCard(result = result)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Deine Arbeitsgewichte wurden automatisch aktualisiert.",
                color = textGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color = textWhite
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = textGray,
            fontSize = 12.sp
        )
    }
}

/**
 * ILB Warmup Set Card
 *
 * Zeigt einen Warmup-Satz im ILB-Protokoll an.
 */
@Composable
fun ILBWarmupSetCard(
    warmupSet: ILBWarmupSet,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCompleted)
                    ilbViolet.copy(alpha = 0.1f)
                else
                    Color.Black.copy(alpha = 0.2f)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Warmup label
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(textGray.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "W${warmupSet.setNumber}",
                color = textGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Percentage
        Text(
            text = "${(warmupSet.percentage * 100).toInt()}%",
            color = textGray,
            fontSize = 14.sp,
            modifier = Modifier.width(40.dp)
        )

        // Weight x Reps
        Text(
            text = "${warmupSet.weight.toInt()}kg × ${warmupSet.targetReps}",
            color = if (isCompleted) ilbViolet else textWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Completion indicator
        if (isCompleted) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Completed",
                tint = ilbViolet,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Weight Change Preview Row
 *
 * Zeigt die Gewichtsänderung für eine Rep-Range an.
 */
@Composable
fun WeightChangePreviewRow(
    preview: WeightChangePreview,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${preview.reps} Reps",
            color = textGray,
            fontSize = 14.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Old weight (if available)
            if (preview.oldWeight != null) {
                Text(
                    text = "${preview.oldWeight.toInt()}kg",
                    color = textGray,
                    fontSize = 14.sp
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = textGray,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                )
            }

            // New weight
            Text(
                text = "${preview.newWeight.toInt()}kg",
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Change indicator
            if (preview.change != null && preview.change != 0f) {
                val changeColor = if (preview.change > 0) ilbGreen else ilbRed
                Text(
                    text = " (${if (preview.change > 0) "+" else ""}${preview.change.toInt()})",
                    color = changeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
