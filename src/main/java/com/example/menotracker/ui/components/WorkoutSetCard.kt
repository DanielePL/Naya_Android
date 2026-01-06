package com.example.menotracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.menotracker.ui.theme.NayaPrimary

/**
 * Workout Set Card with Video Thumbnail
 *
 * Displays set information with optional video thumbnail
 * Option A Design: Video thumbnail on left, metrics on right
 */
@Composable
fun WorkoutSetCard(
    setNumber: Int,
    exerciseName: String,
    weight: Double?,
    reps: Int?,
    rpe: Float?,
    videoUrl: String?,
    velocityMetrics: VelocityMetrics?,
    hasCoachingBadge: Boolean = false,
    onVideoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Set number and exercise name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Set $setNumber: $exerciseName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Coaching badge (if applicable)
                if (hasCoachingBadge) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NayaPrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "🔥 Coaching",
                            style = MaterialTheme.typography.labelSmall,
                            color = NayaPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Weight and Reps
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (weight != null && reps != null) {
                    Text(
                        text = "$weight kg × $reps reps",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (rpe != null) {
                    Text(
                        text = " | RPE $rpe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content: Video Thumbnail + Velocity Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Video Thumbnail (if available)
                if (videoUrl != null) {
                    VideoThumbnail(
                        videoUrl = videoUrl,
                        onClick = onVideoClick,
                        modifier = Modifier
                            .width(100.dp)
                            .height(80.dp)
                    )
                }

                // Velocity Metrics (if available)
                if (velocityMetrics != null) {
                    VelocityMetricsDisplay(
                        metrics = velocityMetrics,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Video Thumbnail with Play Button Overlay
 */
@Composable
fun VideoThumbnail(
    videoUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // Thumbnail image (first frame of video)
        // For now, using placeholder - can be replaced with actual thumbnail extraction
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1410))
        ) {
            // Optional: Load thumbnail from video URL or placeholder
            // Image(painter = rememberAsyncImagePainter(videoUrl), contentDescription = null)
        }

        // Play button overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Play Video",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        // "Video" label
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = NayaPrimary.copy(alpha = 0.9f)
        ) {
            Text(
                text = "Video",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Velocity Metrics Display
 */
@Composable
fun VelocityMetricsDisplay(
    metrics: VelocityMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = Color(0xFF00D9FF),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Velocity Metrics",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Peak Velocity
        MetricRow(
            label = "Peak:",
            value = if (metrics.unit == "m/s") {
                "${String.format("%.2f", metrics.avgPeakVelocity)} m/s"
            } else {
                "${metrics.avgPeakVelocity.toInt()}"
            },
            color = Color(0xFF00D9FF)
        )

        // Velocity Drop
        MetricRow(
            label = "Drop:",
            value = "${String.format("%.1f", metrics.velocityDrop)}%",
            color = if (metrics.velocityDrop > 20) {
                Color(0xFFFF6B6B)  // Red for high drop
            } else {
                Color(0xFF00FF88)  // Green for low drop
            }
        )

        // Reps Detected
        MetricRow(
            label = "Reps:",
            value = "${metrics.repsDetected}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Calibration Tier Badge
        if (metrics.calibrationTier != "relative") {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF00FF88).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "✓ Calibrated",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00FF88),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Metric Row (Label + Value)
 */
@Composable
fun MetricRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * Data class for Velocity Metrics
 */
data class VelocityMetrics(
    val avgPeakVelocity: Double,
    val velocityDrop: Double,
    val repsDetected: Int,
    val unit: String,  // "m/s" or "speed_index"
    val calibrationTier: String  // "pro", "calibrated", "relative"
)
