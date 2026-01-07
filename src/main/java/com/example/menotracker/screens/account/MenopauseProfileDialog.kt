package com.example.menotracker.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val primaryColor = NayaPrimary
private val accentColor = NayaOrangeGlow

/**
 * Dialog zum Bearbeiten des Menopause-Profils
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenopauseProfileDialog(
    currentProfile: MenopauseProfile?,
    onDismiss: () -> Unit,
    onSave: (
        stage: MenopauseStage,
        lastPeriodDate: LocalDate?,
        hrtStatus: HRTStatus,
        primarySymptoms: List<MenopauseSymptomType>
    ) -> Unit
) {
    var selectedStage by remember {
        mutableStateOf(currentProfile?.stageEnum ?: MenopauseStage.PREMENOPAUSE)
    }
    var selectedHrtStatus by remember {
        mutableStateOf(currentProfile?.hrtStatusEnum ?: HRTStatus.NONE)
    }
    var selectedSymptoms by remember {
        mutableStateOf(
            currentProfile?.primarySymptoms?.mapNotNull { symptomName ->
                try {
                    MenopauseSymptomType.valueOf(symptomName.uppercase())
                } catch (e: Exception) {
                    null
                }
            }?.toSet() ?: emptySet()
        )
    }
    var lastPeriodDateString by remember {
        mutableStateOf(currentProfile?.lastPeriodDate ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Date Picker State
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            if (lastPeriodDateString.isNotEmpty()) {
                LocalDate.parse(lastPeriodDateString).toEpochDay() * 24 * 60 * 60 * 1000
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
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
                    Text(
                        text = if (currentProfile == null) "Set Up Profile" else "Edit Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider()

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // Menopause Stage Section
                    item {
                        SectionHeader(
                            icon = Icons.Default.Timeline,
                            title = "Menopause Stage"
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MenopauseStage.entries.forEach { stage ->
                                StageOption(
                                    stage = stage,
                                    isSelected = selectedStage == stage,
                                    onClick = { selectedStage = stage }
                                )
                            }
                        }
                    }

                    // Last Period Date Section
                    item {
                        SectionHeader(
                            icon = Icons.Default.CalendarToday,
                            title = "Last Period"
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lastPeriodDateString.isNotEmpty()) {
                                        formatDisplayDate(lastPeriodDateString)
                                    } else {
                                        "Select date"
                                    },
                                    color = if (lastPeriodDateString.isNotEmpty()) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                            }
                        }
                        if (lastPeriodDateString.isNotEmpty()) {
                            val daysSince = calculateDaysSince(lastPeriodDateString)
                            if (daysSince != null) {
                                Text(
                                    text = "$daysSince days ago",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
                    }

                    // HRT Status Section
                    item {
                        SectionHeader(
                            icon = Icons.Default.Medication,
                            title = "Hormone Replacement Therapy (HRT)"
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(HRTStatus.entries) { status ->
                                HrtStatusChip(
                                    status = status,
                                    isSelected = selectedHrtStatus == status,
                                    onClick = { selectedHrtStatus = status }
                                )
                            }
                        }
                    }

                    // Primary Symptoms Section
                    item {
                        SectionHeader(
                            icon = Icons.Default.Healing,
                            title = "Primary Symptoms",
                            subtitle = "Select up to 5 main symptoms"
                        )
                        Spacer(Modifier.height(12.dp))

                        // Group symptoms by category
                        val symptomsByCategory = MenopauseSymptomType.entries.groupBy { it.category }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            symptomsByCategory.forEach { (category, symptoms) ->
                                Column {
                                    Text(
                                        text = category.displayName,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    FlowRowSymptoms(
                                        symptoms = symptoms,
                                        selectedSymptoms = selectedSymptoms,
                                        onSymptomToggle = { symptom ->
                                            selectedSymptoms = if (symptom in selectedSymptoms) {
                                                selectedSymptoms - symptom
                                            } else if (selectedSymptoms.size < 5) {
                                                selectedSymptoms + symptom
                                            } else {
                                                selectedSymptoms
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }

                // Footer with Save Button
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val lastPeriodDate = try {
                                if (lastPeriodDateString.isNotEmpty()) {
                                    LocalDate.parse(lastPeriodDateString)
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                            onSave(
                                selectedStage,
                                lastPeriodDate,
                                selectedHrtStatus,
                                selectedSymptoms.toList()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            lastPeriodDateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StageOption(
    stage: MenopauseStage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) primaryColor else Color.Transparent
    val backgroundColor = if (isSelected) {
        primaryColor.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = primaryColor
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stage.displayName,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stage.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HrtStatusChip(
    status: HRTStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Text(
            text = status.displayName,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun FlowRowSymptoms(
    symptoms: List<MenopauseSymptomType>,
    selectedSymptoms: Set<MenopauseSymptomType>,
    onSymptomToggle: (MenopauseSymptomType) -> Unit
) {
    // Simple wrap layout using Column and Row
    val chunkedSymptoms = symptoms.chunked(2)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunkedSymptoms.forEach { rowSymptoms ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSymptoms.forEach { symptom ->
                    SymptomSelectChip(
                        symptom = symptom,
                        isSelected = symptom in selectedSymptoms,
                        onClick = { onSymptomToggle(symptom) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space if row has less items
                if (rowSymptoms.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SymptomSelectChip(
    symptom: MenopauseSymptomType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        primaryColor.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = if (isSelected) primaryColor else Color.Transparent

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getSymptomIcon(symptom),
                contentDescription = null,
                tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = symptom.displayName,
                fontSize = 13.sp,
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
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

private fun formatDisplayDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

private fun calculateDaysSince(dateString: String): Long? {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now())
    } catch (e: Exception) {
        null
    }
}