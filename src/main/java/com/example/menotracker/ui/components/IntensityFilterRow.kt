package com.example.menotracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.menotracker.data.models.WorkoutIntensityLevel
import com.example.menotracker.ui.theme.NayaGlass
import com.example.menotracker.ui.theme.NayaPrimary

/**
 * Horizontal row of filter chips for intensity level selection.
 * Allows filtering workouts by Sanft/Aktiv/Power or showing all.
 */
@Composable
fun IntensityFilterRow(
    selectedIntensity: WorkoutIntensityLevel?,
    onIntensitySelected: (WorkoutIntensityLevel?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Alle" chip
        IntensityFilterChip(
            label = "Alle",
            selected = selectedIntensity == null,
            onClick = { onIntensitySelected(null) }
        )

        // Individual intensity chips
        WorkoutIntensityLevel.allLevels().forEach { intensity ->
            IntensityFilterChip(
                label = intensity.displayName,
                selected = selectedIntensity == intensity,
                onClick = { onIntensitySelected(intensity) },
                intensity = intensity
            )
        }
    }
}

/**
 * Single filter chip for intensity selection
 */
@Composable
private fun IntensityFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    intensity: WorkoutIntensityLevel? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = NayaGlass,
            labelColor = Color.White.copy(alpha = 0.8f),
            selectedContainerColor = intensity?.let { getIntensityColor(it) } ?: NayaPrimary,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color.White.copy(alpha = 0.1f),
            selectedBorderColor = Color.Transparent,
            enabled = true,
            selected = selected
        )
    )
}

/**
 * Get color for intensity level
 */
private fun getIntensityColor(intensity: WorkoutIntensityLevel): Color {
    return when (intensity) {
        WorkoutIntensityLevel.SANFT -> Color(0xFF48c6ef)   // Soft cyan/blue
        WorkoutIntensityLevel.AKTIV -> Color(0xFF764ba2)   // Purple
        WorkoutIntensityLevel.POWER -> Color(0xFFf5576c)   // Red-pink
    }
}
