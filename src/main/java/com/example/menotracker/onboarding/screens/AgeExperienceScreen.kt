package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrange

/**
 * Screen to collect user's age and training experience years.
 * This data is critical for realistic timeline calculations.
 */
@Composable
fun AgeExperienceScreen(
    currentStep: Int,
    totalSteps: Int,
    initialAge: Int?,
    initialTrainingYears: Int?,
    onContinue: (age: Int, trainingYears: Int) -> Unit,
    onBack: () -> Unit
) {
    var ageText by remember { mutableStateOf(initialAge?.toString() ?: "") }
    var trainingYearsText by remember { mutableStateOf(initialTrainingYears?.toString() ?: "") }

    val age = ageText.toIntOrNull()
    val trainingYears = trainingYearsText.toIntOrNull()

    // Validation
    val isAgeValid = age != null && age in 14..99
    val isTrainingYearsValid = trainingYears != null && trainingYears >= 0 && trainingYears <= (age ?: 99)
    val canContinue = isAgeValid && isTrainingYearsValid

    // Experience category for display
    val experienceCategory = when (trainingYears) {
        null -> null
        0 -> "Complete Beginner"
        in 1..2 -> "Novice"
        in 3..5 -> "Intermediate"
        in 6..10 -> "Advanced"
        in 11..20 -> "Veteran"
        else -> "Lifetime Lifter"
    }

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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "A Bit About You",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This helps us give you realistic expectations and tailored guidance",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Age input
            Text(
                text = "Your Age",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ageText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d{0,2}$"))) {
                        ageText = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. 35",
                        color = Color.White.copy(alpha = 0.3f)
                    )
                },
                suffix = {
                    Text(
                        text = "years old",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NayaPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    cursorColor = NayaPrimary,
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Age context (only for masters)
            if (age != null && age >= 40) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = NayaOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = NayaOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Masters lifter - we'll adjust expectations accordingly",
                            fontSize = 13.sp,
                            color = NayaOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Training years input
            Text(
                text = "Years of Strength Training",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = trainingYearsText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d{0,2}$"))) {
                        trainingYearsText = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. 5",
                        color = Color.White.copy(alpha = 0.3f)
                    )
                },
                suffix = {
                    Text(
                        text = "years",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NayaPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    cursorColor = NayaPrimary,
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Experience category badge
            if (experienceCategory != null && trainingYears != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = NayaPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = experienceCategory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NayaPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Veteran message
            if (trainingYears != null && trainingYears >= 10) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = NayaOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Veteran Perspective",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "With $trainingYears years of training, your progress rate naturally slows. " +
                                   "We'll set realistic timelines and celebrate the small wins that matter at your level.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Continue button
        Button(
            onClick = {
                if (age != null && trainingYears != null) {
                    onContinue(age, trainingYears)
                }
            },
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NayaPrimary,
                disabledContainerColor = NayaPrimary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Privacy note
        Text(
            text = "Used only for progress calculations. Stored locally on your device.",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}