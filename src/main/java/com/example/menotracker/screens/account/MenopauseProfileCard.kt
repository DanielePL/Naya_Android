package com.example.menotracker.screens.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.glassCardAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val primaryColor = NayaPrimary
private val accentColor = NayaOrangeGlow

/**
 * Card showing the user's menopause profile
 * Displays stage, HRT status, last period, and primary symptoms
 */
@Composable
fun MenopauseProfileCard(
    profile: MenopauseProfile?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .glassCardAccent(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "MY PROFILE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (profile != null) {
                // Stage and HRT Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Menopause Stage
                    ProfileInfoItem(
                        icon = Icons.Default.Timeline,
                        label = "Stage",
                        value = profile.stageEnum.displayName,
                        valueColor = getStageColor(profile.stageEnum)
                    )

                    VerticalDivider(
                        modifier = Modifier.height(60.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    // HRT Status
                    ProfileInfoItem(
                        icon = Icons.Default.Medication,
                        label = "HRT",
                        value = profile.hrtStatusEnum.displayName,
                        valueColor = getHrtColor(profile.hrtStatusEnum)
                    )

                    VerticalDivider(
                        modifier = Modifier.height(60.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    // Last Period
                    val daysSince = profile.lastPeriodDate?.let { calculateDaysSinceLastPeriod(it) }
                    ProfileInfoItem(
                        icon = Icons.Default.CalendarToday,
                        label = "Last Period",
                        value = daysSince?.let { "${it}d" } ?: "--",
                        valueColor = accentColor
                    )
                }

                // Primary Symptoms (if any)
                profile.primarySymptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = "Main Symptoms",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Symptom Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        symptoms.take(4).forEach { symptomName ->
                            val symptom = try {
                                MenopauseSymptomType.valueOf(symptomName.uppercase())
                            } catch (e: Exception) {
                                null
                            }

                            if (symptom != null) {
                                SymptomChip(
                                    icon = getSymptomIcon(symptom),
                                    text = symptom.displayName
                                )
                            }
                        }
                        if (symptoms.size > 4) {
                            Text(
                                text = "+${symptoms.size - 4}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

            } else {
                // Empty State
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Profile not set up yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Set Up Profile")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = accentColor
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = valueColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SymptomChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = primaryColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getStageColor(stage: MenopauseStage): Color {
    return when (stage) {
        MenopauseStage.PREMENOPAUSE -> Color(0xFF10B981) // Green
        MenopauseStage.EARLY_PERIMENOPAUSE -> Color(0xFFFBBF24) // Yellow
        MenopauseStage.LATE_PERIMENOPAUSE -> Color(0xFFF97316) // Orange
        MenopauseStage.MENOPAUSE -> NayaPrimary
        MenopauseStage.POSTMENOPAUSE -> Color(0xFF8B5CF6) // Purple
    }
}

@Composable
private fun getHrtColor(hrtStatus: HRTStatus): Color {
    return when (hrtStatus) {
        HRTStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        HRTStatus.CONSIDERING -> Color(0xFFFBBF24) // Yellow
        HRTStatus.CURRENT -> Color(0xFF10B981) // Green
        HRTStatus.PAST -> Color(0xFF6B7280) // Gray
    }
}

private fun getSymptomIcon(symptom: MenopauseSymptomType): ImageVector {
    return when (symptom) {
        MenopauseSymptomType.HOT_FLASH -> Icons.Default.Whatshot
        MenopauseSymptomType.NIGHT_SWEAT -> Icons.Default.NightsStay
        MenopauseSymptomType.MOOD_SWING -> Icons.Default.Mood
        MenopauseSymptomType.ANXIETY -> Icons.Default.Psychology
        MenopauseSymptomType.FATIGUE -> Icons.Default.BatteryAlert
        MenopauseSymptomType.BRAIN_FOG -> Icons.Default.Cloud
        MenopauseSymptomType.SLEEP_ISSUE -> Icons.Default.Bedtime
        MenopauseSymptomType.JOINT_PAIN -> Icons.Default.AccessibilityNew
        MenopauseSymptomType.HEADACHE -> Icons.Default.Face
        MenopauseSymptomType.WEIGHT_GAIN -> Icons.Default.MonitorWeight
        MenopauseSymptomType.LOW_LIBIDO -> Icons.Default.FavoriteBorder
        MenopauseSymptomType.VAGINAL_DRYNESS -> Icons.Default.WaterDrop
        MenopauseSymptomType.HEART_PALPITATIONS -> Icons.Default.Favorite
        MenopauseSymptomType.IRRITABILITY -> Icons.Default.SentimentDissatisfied
    }
}

private fun calculateDaysSinceLastPeriod(dateString: String): Long? {
    return try {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val lastPeriod = LocalDate.parse(dateString, formatter)
        ChronoUnit.DAYS.between(lastPeriod, LocalDate.now())
    } catch (e: Exception) {
        null
    }
}
