package com.example.menotracker.onboarding.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.onboarding.data.*
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrange
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PromiseCommitmentScreen(
    currentPRs: CurrentPRs,
    goalPRs: GoalPRs,
    commitment: TrainingCommitment,
    gender: Gender,
    experienceLevel: ExperienceLevel?,
    age: Int? = null,
    trainingYears: Int? = null,
    onCommit: () -> Unit,
    onBack: () -> Unit
) {
   // Determine if user is a veteran (10+ years training)
    val isVeteran = (trainingYears ?: 0) >= 10

    // Only calculate timeline for non-veterans
    val timelineResult = remember(currentPRs, goalPRs, commitment, gender, experienceLevel, age, trainingYears) {
        if (!isVeteran) {
            TimelineCalculator.calculateTimeline(
                currentPRs = currentPRs,
                goalPRs = goalPRs,
                commitment = commitment,
                gender = gender,
                experienceLevel = experienceLevel,
                age = age,
                trainingYears = trainingYears
            )
        } else null
    }

    // Timeline formatting only for non-veterans
    val formattedTimeline = timelineResult?.let {
        TimelineCalculator.formatTimeline(Pair(it.minWeeks, it.maxWeeks))
    }
    val motivationalMessage = timelineResult?.let {
        TimelineCalculator.getMotivationalMessage(Pair(it.minWeeks, it.maxWeeks))
    }

    // Calculate target date only for non-veterans
    val targetDate = timelineResult?.let {
        val avgWeeks = (it.minWeeks + it.maxWeeks) / 2
        LocalDate.now().plusWeeks(avgWeeks.toLong())
    }
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    // Animation states
    var showContent by remember { mutableStateOf(false) }
    var showPromise by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
        kotlinx.coroutines.delay(500)
        showPromise = true
    }

    // Pulsing animation for the commit button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF1A1A1A),
                        Color(0xFF0D0D0D)
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Back button - smaller
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Goal Section - compact
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Goal Total Badge
            Surface(
                color = NayaOrange.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "YOUR GOAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NayaOrange,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Big goal total
            Text(
                text = "${goalPRs.totalKg?.toInt() ?: 0} kg",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Total (S/B/D)",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Individual lift goals - inline
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LiftGoalChip("S", goalPRs.squatKg)
                LiftGoalChip("B", goalPRs.benchKg)
                LiftGoalChip("D", goalPRs.deadliftKg)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Different cards based on veteran status
        if (isVeteran) {
            // VETERAN: No timeline, just commitment card
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = NayaOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Veteran Lifter",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NayaOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "With ${trainingYears ?: 10}+ years under the bar, you know your body better than any algorithm.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Commitment summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CommitmentBadge(
                            icon = Icons.Default.FitnessCenter,
                            value = "${commitment.sessionsPerWeek}x/week"
                        )
                        CommitmentBadge(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "Effort ${commitment.effortLevel}/10"
                        )
                    }
                }
            }
        } else {
            // NON-VETERAN: Show timeline
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = NayaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Estimated Timeline",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = formattedTimeline ?: "",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = NayaPrimary
                    )

                    targetDate?.let { date ->
                        Text(
                            text = "Target: ${date.format(dateFormatter)}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    motivationalMessage?.let { message ->
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Commitment summary - inline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CommitmentBadge(
                            icon = Icons.Default.FitnessCenter,
                            value = "${commitment.sessionsPerWeek}x/week"
                        )
                        CommitmentBadge(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "Effort ${commitment.effortLevel}/10"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Promise Card - compact
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Handshake,
                    contentDescription = null,
                    tint = NayaOrange,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Promise To Myself",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (isVeteran) {
                            "I commit to training ${commitment.sessionsPerWeek}x per week to reach my ${goalPRs.totalKg?.toInt() ?: 0}kg goal."
                        } else {
                            "I commit to training ${commitment.sessionsPerWeek}x per week to reach ${goalPRs.totalKg?.toInt() ?: 0}kg total by ${targetDate?.format(dateFormatter) ?: "my target date"}."
                        },
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Commit Button - always visible at bottom
        Button(
            onClick = onCommit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(scale),
            colors = ButtonDefaults.buttonColors(
                containerColor = NayaPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "I'm Committed",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Let's make it happen",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LiftGoalChip(
    label: String,
    value: Float?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            text = "${value?.toInt() ?: "-"}",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun CommitmentBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}