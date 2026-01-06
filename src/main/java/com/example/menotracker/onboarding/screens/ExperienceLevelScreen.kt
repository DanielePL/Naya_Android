package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.menotracker.onboarding.data.ExperienceLevel
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrange

@Composable
fun ExperienceLevelScreen(
    currentStep: Int,
    totalSteps: Int,
    onLevelSelected: (ExperienceLevel) -> Unit,
    onBack: () -> Unit
) {
    data class LevelOption(val level: ExperienceLevel, val icon: ImageVector, val description: String)
    val levels = listOf(
        LevelOption(ExperienceLevel.BEGINNER, Icons.Default.School, "Less than 1 year"),
        LevelOption(ExperienceLevel.INTERMEDIATE, Icons.AutoMirrored.Filled.TrendingUp, "1-3 years experience"),
        LevelOption(ExperienceLevel.EXPERIENCED, Icons.Default.EmojiEvents, "3+ years, know my numbers"),
        LevelOption(ExperienceLevel.ELITE, Icons.Default.Bolt, "Competitions, coaching")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Header with back button and progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Step $currentStep of $totalSteps",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = NayaPrimary,
            trackColor = Color.White.copy(alpha = 0.1f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "How Long Have You Been Training?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This helps us tailor the complexity",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Level options
        levels.forEach { option ->
            LevelCard(
                icon = option.icon,
                title = option.level.displayName,
                description = option.description,
                onClick = { onLevelSelected(option.level) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LevelCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NayaOrange,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}