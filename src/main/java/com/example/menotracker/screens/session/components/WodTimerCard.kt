// screens/session/components/WodTimerCard.kt
package com.example.menotracker.screens.session.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.viewmodels.WodTimerConfig
import kotlinx.coroutines.delay

/**
 * WOD Timer states
 */
enum class WodTimerState {
    NOT_STARTED,
    COUNTDOWN,  // 3-2-1 countdown before start
    RUNNING,
    PAUSED,
    COMPLETED
}

/**
 * Tabata phase
 */
enum class TabataPhase {
    WORK,
    REST
}

/**
 * Safe vibration helper that checks API level
 */
@Suppress("DEPRECATION")
private fun Vibrator.vibrateCompat(milliseconds: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrate(milliseconds)
    }
}

@Suppress("DEPRECATION")
private fun Vibrator.vibratePatternCompat(pattern: LongArray) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        vibrate(pattern, -1)
    }
}

/**
 * WOD Timer Card - displays timer based on WOD type
 *
 * Timer modes:
 * - AMRAP: Count down from time cap
 * - EMOM: Count down each minute interval
 * - For Time: Count up with optional time cap
 * - Tabata: 20s work / 10s rest intervals
 */
@Composable
fun WodTimerCard(
    timerConfig: WodTimerConfig,
    onTimerComplete: () -> Unit = {},
    onRoundComplete: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator }

    // Timer state
    var timerState by remember { mutableStateOf(WodTimerState.NOT_STARTED) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var countdownValue by remember { mutableIntStateOf(3) }

    // EMOM/Tabata specific state
    var currentRound by remember { mutableIntStateOf(1) }
    var intervalSecondsRemaining by remember { mutableIntStateOf(timerConfig.emomIntervalSeconds) }

    // Tabata state
    var tabataPhase by remember { mutableStateOf(TabataPhase.WORK) }
    var tabataRound by remember { mutableIntStateOf(1) }
    var tabataSecondsRemaining by remember { mutableIntStateOf(timerConfig.tabataWorkSeconds) }

    // AMRAP rounds tracking
    var amrapRounds by remember { mutableIntStateOf(0) }
    var amrapReps by remember { mutableIntStateOf(0) }

    // Calculate time values
    val timeCapSeconds = timerConfig.timeCapSeconds ?: 0
    val remainingSeconds = (timeCapSeconds - elapsedSeconds).coerceAtLeast(0)

    // Timer effect
    LaunchedEffect(timerState) {
        if (timerState == WodTimerState.COUNTDOWN) {
            // 3-2-1 countdown
            for (i in 3 downTo 1) {
                countdownValue = i
                vibrator?.vibrateCompat(100)
                delay(1000)
            }
            // GO!
            countdownValue = 0
            vibrator?.vibrateCompat(500)
            delay(500)
            timerState = WodTimerState.RUNNING
        }

        while (timerState == WodTimerState.RUNNING) {
            delay(1000)
            elapsedSeconds++

            when (timerConfig.wodType) {
                "amrap" -> {
                    // Check if time is up
                    if (elapsedSeconds >= timeCapSeconds) {
                        timerState = WodTimerState.COMPLETED
                        vibrator?.vibratePatternCompat(longArrayOf(0, 500, 200, 500, 200, 500))
                        onTimerComplete()
                    }
                    // Beep at last 10 seconds
                    if (remainingSeconds <= 10 && remainingSeconds > 0) {
                        vibrator?.vibrateCompat(100)
                    }
                }

                "emom" -> {
                    intervalSecondsRemaining--
                    if (intervalSecondsRemaining <= 0) {
                        // New minute
                        currentRound++
                        intervalSecondsRemaining = timerConfig.emomIntervalSeconds
                        vibrator?.vibrateCompat(300)
                        onRoundComplete(currentRound)

                        // Check if EMOM is complete
                        if (elapsedSeconds >= timeCapSeconds) {
                            timerState = WodTimerState.COMPLETED
                            vibrator?.vibratePatternCompat(longArrayOf(0, 500, 200, 500))
                            onTimerComplete()
                        }
                    }
                    // Beep at last 5 seconds of each minute
                    if (intervalSecondsRemaining <= 5 && intervalSecondsRemaining > 0) {
                        vibrator?.vibrateCompat(50)
                    }
                }

                "tabata" -> {
                    tabataSecondsRemaining--
                    if (tabataSecondsRemaining <= 0) {
                        if (tabataPhase == TabataPhase.WORK) {
                            // Switch to rest
                            tabataPhase = TabataPhase.REST
                            tabataSecondsRemaining = timerConfig.tabataRestSeconds
                            vibrator?.vibrateCompat(200)
                        } else {
                            // Switch to work, next round
                            tabataRound++
                            if (tabataRound > timerConfig.tabataRounds) {
                                timerState = WodTimerState.COMPLETED
                                vibrator?.vibratePatternCompat(longArrayOf(0, 500, 200, 500))
                                onTimerComplete()
                            } else {
                                tabataPhase = TabataPhase.WORK
                                tabataSecondsRemaining = timerConfig.tabataWorkSeconds
                                vibrator?.vibrateCompat(400)
                                onRoundComplete(tabataRound)
                            }
                        }
                    }
                    // Beep at last 3 seconds
                    if (tabataSecondsRemaining <= 3 && tabataSecondsRemaining > 0) {
                        vibrator?.vibrateCompat(50)
                    }
                }

                "for_time" -> {
                    // Count up - check time cap
                    if (timeCapSeconds > 0 && elapsedSeconds >= timeCapSeconds) {
                        timerState = WodTimerState.COMPLETED
                        vibrator?.vibratePatternCompat(longArrayOf(0, 500, 200, 500))
                        onTimerComplete()
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1410).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // WOD Type Badge
            Surface(
                color = NayaPrimary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = timerConfig.getDisplayName(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))

            // Main Timer Display
            when (timerState) {
                WodTimerState.NOT_STARTED -> {
                    // Show start button
                    TimerDisplay(
                        seconds = when (timerConfig.wodType) {
                            "for_time" -> 0
                            "tabata" -> timerConfig.tabataWorkSeconds
                            else -> timeCapSeconds
                        },
                        isCountdown = timerConfig.wodType != "for_time"
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { timerState = WodTimerState.COUNTDOWN },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("START WOD", fontWeight = FontWeight.Bold)
                    }
                }

                WodTimerState.COUNTDOWN -> {
                    // 3-2-1 countdown
                    CountdownDisplay(countdownValue)
                }

                WodTimerState.RUNNING, WodTimerState.PAUSED -> {
                    // Active timer
                    when (timerConfig.wodType) {
                        "amrap" -> {
                            AmrapTimerDisplay(
                                remainingSeconds = remainingSeconds,
                                totalSeconds = timeCapSeconds,
                                rounds = amrapRounds,
                                reps = amrapReps,
                                onAddRound = {
                                    amrapRounds++
                                    vibrator?.vibrateCompat(100)
                                },
                                onAddReps = { reps ->
                                    amrapReps += reps
                                }
                            )
                        }

                        "emom" -> {
                            val interval = timerConfig.emomIntervalSeconds.coerceAtLeast(1)
                            EmomTimerDisplay(
                                currentRound = currentRound,
                                totalRounds = (timeCapSeconds / interval).coerceAtLeast(1),
                                intervalRemaining = intervalSecondsRemaining,
                                intervalTotal = interval
                            )
                        }

                        "tabata" -> {
                            TabataTimerDisplay(
                                phase = tabataPhase,
                                secondsRemaining = tabataSecondsRemaining,
                                currentRound = tabataRound,
                                totalRounds = timerConfig.tabataRounds
                            )
                        }

                        "for_time" -> {
                            ForTimeTimerDisplay(
                                elapsedSeconds = elapsedSeconds,
                                timeCapSeconds = if (timeCapSeconds > 0) timeCapSeconds else null,
                                onComplete = {
                                    timerState = WodTimerState.COMPLETED
                                    vibrator?.vibratePatternCompat(longArrayOf(0, 500, 200, 500))
                                    onTimerComplete()
                                }
                            )
                        }

                        else -> {
                            TimerDisplay(
                                seconds = if (timeCapSeconds > 0) remainingSeconds else elapsedSeconds,
                                isCountdown = timeCapSeconds > 0
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Pause/Resume button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                timerState = if (timerState == WodTimerState.PAUSED)
                                    WodTimerState.RUNNING else WodTimerState.PAUSED
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (timerState == WodTimerState.PAUSED)
                                    Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        ) {
                            Icon(
                                if (timerState == WodTimerState.PAUSED)
                                    Icons.Default.PlayArrow else Icons.Default.Pause,
                                null
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (timerState == WodTimerState.PAUSED) "Resume" else "Pause")
                        }

                        if (timerConfig.wodType == "for_time") {
                            Button(
                                onClick = {
                                    timerState = WodTimerState.COMPLETED
                                    onTimerComplete()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
                            ) {
                                Icon(Icons.Default.Flag, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Finish")
                            }
                        }
                    }
                }

                WodTimerState.COMPLETED -> {
                    // Completion state
                    CompletionDisplay(
                        wodType = timerConfig.wodType,
                        finalTime = elapsedSeconds,
                        amrapRounds = amrapRounds,
                        amrapReps = amrapReps
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerDisplay(
    seconds: Int,
    isCountdown: Boolean
) {
    val minutes = seconds / 60
    val secs = seconds % 60

    Text(
        text = "%02d:%02d".format(minutes, secs),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 64.sp
    )

    Text(
        text = if (isCountdown) "Time Cap" else "Elapsed",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )
}

@Composable
private fun CountdownDisplay(value: Int) {
    val scale by animateFloatAsState(
        targetValue = if (value > 0) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "countdown_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(150.dp)
    ) {
        Text(
            text = if (value > 0) value.toString() else "GO!",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = if (value > 0) Color.White else Color(0xFF4CAF50),
            fontSize = if (value > 0) 96.sp else 72.sp,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
        )
    }
}

@Composable
private fun AmrapTimerDisplay(
    remainingSeconds: Int,
    totalSeconds: Int,
    rounds: Int,
    reps: Int,
    onAddRound: () -> Unit,
    onAddReps: (Int) -> Unit
) {
    val progress = 1f - (remainingSeconds.toFloat() / totalSeconds)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular progress with time
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                // Background arc
                drawArc(
                    color = Color.Gray.copy(alpha = 0.3f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )

                // Progress arc
                drawArc(
                    color = NayaPrimary,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingSeconds <= 60) Color(0xFFF44336) else Color.White
                )
                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Score display
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$rounds",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = NayaPrimary
                )
                Text("Rounds", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+$reps",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text("Reps", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Add round button
        Button(
            onClick = onAddRound,
            colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(4.dp))
            Text("Round Complete (+1)")
        }
    }
}

@Composable
private fun EmomTimerDisplay(
    currentRound: Int,
    totalRounds: Int,
    intervalRemaining: Int,
    intervalTotal: Int
) {
    val progress = 1f - (intervalRemaining.toFloat() / intervalTotal)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Round indicator
        Text(
            text = "Minute $currentRound of $totalRounds",
            style = MaterialTheme.typography.titleMedium,
            color = NayaPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // Circular timer
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawArc(
                    color = Color.Gray.copy(alpha = 0.3f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(NayaPrimary, Color(0xFFFF6B35))
                    ),
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$intervalRemaining",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (intervalRemaining <= 10) Color(0xFFFF9800) else Color.White,
                    fontSize = 72.sp
                )
                Text(
                    text = "seconds",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        if (intervalRemaining <= 5) {
            Text(
                text = "GET READY!",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TabataTimerDisplay(
    phase: TabataPhase,
    secondsRemaining: Int,
    currentRound: Int,
    totalRounds: Int
) {
    val phaseColor = if (phase == TabataPhase.WORK) Color(0xFF4CAF50) else Color(0xFFF44336)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Phase indicator
        Surface(
            color = phaseColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (phase == TabataPhase.WORK) "WORK!" else "REST",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(24.dp))

        // Big timer
        Text(
            text = "$secondsRemaining",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = phaseColor,
            fontSize = 120.sp
        )

        Spacer(Modifier.height(16.dp))

        // Round progress
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalRounds) { index ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index + 1 < currentRound -> NayaPrimary
                                index + 1 == currentRound -> phaseColor
                                else -> Color.Gray.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }

        Text(
            text = "Round $currentRound of $totalRounds",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ForTimeTimerDisplay(
    elapsedSeconds: Int,
    timeCapSeconds: Int?,
    onComplete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Elapsed time (counting up)
        Text(
            text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 72.sp
        )

        Text(
            text = "elapsed",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        // Time cap warning
        timeCapSeconds?.let { cap ->
            val remaining = cap - elapsedSeconds
            if (remaining > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Cap: %02d:%02d remaining".format(remaining / 60, remaining % 60),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remaining <= 60) Color(0xFFF44336) else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun CompletionDisplay(
    wodType: String,
    finalTime: Int,
    amrapRounds: Int,
    amrapReps: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "WOD COMPLETE!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NayaPrimary
        )

        Spacer(Modifier.height(16.dp))

        when (wodType) {
            "amrap" -> {
                Text(
                    text = "Score: $amrapRounds rounds + $amrapReps reps",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            "for_time" -> {
                Text(
                    text = "Time: %02d:%02d".format(finalTime / 60, finalTime % 60),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            else -> {
                Text(
                    text = "Total Time: %02d:%02d".format(finalTime / 60, finalTime % 60),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
    }
}
