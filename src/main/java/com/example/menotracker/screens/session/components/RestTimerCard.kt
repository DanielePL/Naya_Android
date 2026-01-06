package com.example.menotracker.screens.session.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.viewmodels.TimerData

// Design System colors
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBackground = Color(0xFF1a1410).copy(alpha = 0.4f)

/**
 * RestTimerCard - Compact rest timer display with controls
 *
 * Features:
 * - Timer display with progress bar
 * - Skip button
 * - Add time buttons (+15s, +30s)
 * - Color change when time is low
 */
@Composable
fun RestTimerCard(
    timerData: TimerData,
    onSkip: () -> Unit,
    onAddTime: (Int) -> Unit
) {
    // Compact single-row rest timer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, orangeGlow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Timer icon + time
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = timerData.formatTime(),
                color = if (timerData.remainingSeconds <= 3) orangeGlow else textWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            // Progress Bar (compact)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(textGray.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(timerData.progress)
                        .background(orangeGlow)
                )
            }

            // Skip button
            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Skip", color = textGray, fontSize = 12.sp)
            }

            // +15s button
            TextButton(
                onClick = { onAddTime(15) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("+15s", color = orangeGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // +30s button
            TextButton(
                onClick = { onAddTime(30) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("+30s", color = orangeGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * WorkoutPausedCard - Displayed when workout is paused
 */
@Composable
fun WorkoutPausedCard(
    onResume: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A00).copy(alpha = 0.6f)),
        border = BorderStroke(2.dp, Color.Yellow),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Pause,
                contentDescription = null,
                tint = Color.Yellow,
                modifier = Modifier.size(48.dp)
            )

            Text(
                "WORKOUT PAUSED",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Yellow
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}