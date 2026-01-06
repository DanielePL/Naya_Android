package com.example.menotracker.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menotracker.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════════
// DESIGN TOKENS
// ═══════════════════════════════════════════════════════════════════════════════

private val HeroGradient = Brush.linearGradient(
    colors = listOf(NayaOrangeBright, NayaPrimary)
)

// Workout Card: Dark/Anthracite with violet accent - Kraft, Fokus, Premium
private val WorkoutCardBackground = Color(0xFF1A1A1A)
private val WorkoutCardBorderColor = NayaPrimary.copy(alpha = 0.3f)
private val NayaVioletAccent = NayaPrimary

/**
 * Get time-based gradient for nutrition cards
 * The card "lives" with the user's daily rhythm
 */
@Composable
private fun getNutritionGradient(): Brush {
    val hour = java.time.LocalTime.now().hour
    val theme = getNutritionThemeForHour(hour)
    return Brush.linearGradient(
        colors = listOf(
            Color(theme.gradientStartHex.toInt()),
            Color(theme.gradientEndHex.toInt())
        )
    )
}

/**
 * Get time-based theme data for nutrition cards
 */
@Composable
private fun getCurrentNutritionTheme(): TimeBasedNutritionTheme {
    val hour = java.time.LocalTime.now().hour
    return getNutritionThemeForHour(hour)
}

// ═══════════════════════════════════════════════════════════════════════════════
// SMART HOME HEADER WITH DUAL STREAKS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SmartHomeHeader(
    userName: String,
    profileImageUrl: String?,
    userTier: UserTier,
    streaks: UserStreaks,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = getGreeting()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = greeting,
                color = NayaTextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = Poppins
            )
            Text(
                text = userName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk
            )

            // Dual Streak Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Workout Streak (only show if user has workout tier and streak > 0)
                if (userTier.hasWorkout() && streaks.workoutStreak > 0) {
                    StreakBadge(
                        count = streaks.workoutStreak,
                        icon = Icons.Default.LocalFireDepartment,
                        color = NayaPrimary,
                        label = null
                    )
                }

                // Nutrition Streak (only show if user has nutrition tier and streak > 0)
                if (userTier.hasNutrition() && streaks.nutritionStreak > 0) {
                    StreakBadge(
                        count = streaks.nutritionStreak,
                        icon = Icons.Default.Restaurant,
                        color = Color(0xFF4CAF50),
                        label = null
                    )
                }
            }
        }

        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable(onClick = onAvatarClick)
                .background(HeroGradient),
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun StreakBadge(
    count: Int,
    icon: ImageVector,
    color: Color,
    label: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            if (label != null) {
                Text(
                    text = label,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good Morning,"
        in 12..17 -> "Good Afternoon,"
        in 18..21 -> "Good Evening,"
        else -> "Hey there,"
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DUAL HERO CARDS (Nutrition + Workout stacked)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Dual Hero Cards - Both Nutrition and Workout cards stacked vertically
 * Both cards are compact horizontal layouts
 */
@Composable
fun DualHeroCards(
    nutritionHeroType: HeroCardType,
    workoutName: String?,
    onNutritionClick: () -> Unit,
    onWorkoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Nutrition Card (time-based colors)
        CompactNutritionCard(
            heroCardType = nutritionHeroType,
            onClick = onNutritionClick
        )

        // Workout Card (dark theme)
        CompactWorkoutCard(
            workoutName = workoutName,
            onClick = onWorkoutClick
        )
    }
}

/**
 * Legacy HeroCard for backward compatibility
 */
@Composable
fun HeroCard(
    heroCardType: HeroCardType,
    workoutStreak: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Now just shows compact nutrition card for nutrition types
    // Workout card shown separately in DualHeroCards
    if (heroCardType != HeroCardType.START_WORKOUT) {
        CompactNutritionCard(
            heroCardType = heroCardType,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        CompactWorkoutCard(
            workoutName = null,
            onClick = onClick,
            modifier = modifier
        )
    }
}

/**
 * Compact Nutrition Card - Horizontal layout with time-based colors
 */
@Composable
fun CompactNutritionCard(
    heroCardType: HeroCardType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = getCurrentNutritionTheme()
    val gradient = getNutritionGradient()
    val displayInfo = heroCardType.getDisplayInfo()
    val gradientStartColor = Color(theme.gradientStartHex.toInt())

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(gradient, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Icon + Text
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Time Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme.emoji,
                            fontSize = 20.sp
                        )
                    }

                    Column {
                        Text(
                            text = displayInfo.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = displayInfo.subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontFamily = Poppins,
                            maxLines = 1
                        )
                    }
                }

                // Right: CTA Button
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = gradientStartColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = displayInfo.ctaText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = Poppins
                    )
                }
            }
        }
    }
}

/**
 * Compact Workout Card - Horizontal layout with dark theme
 */
@Composable
fun CompactWorkoutCard(
    workoutName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = WorkoutCardBackground,
        border = BorderStroke(1.dp, WorkoutCardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Text
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Workout Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NayaVioletAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = NayaVioletAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = workoutName ?: "Start Workout",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk,
                        maxLines = 1
                    )
                    Text(
                        text = "Move for your wellbeing",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontFamily = Poppins
                    )
                }
            }

            // Right: CTA Button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NayaVioletAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "GO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = Poppins
                )
            }
        }
    }
}

private fun getHeroIcon(iconType: HeroCardIcon): ImageVector = when (iconType) {
    HeroCardIcon.WORKOUT -> Icons.Default.FitnessCenter
    HeroCardIcon.BREAKFAST -> Icons.Default.WbSunny
    HeroCardIcon.LUNCH -> Icons.Default.LightMode
    HeroCardIcon.DINNER -> Icons.Default.DarkMode
    HeroCardIcon.NUTRITION -> Icons.Default.Restaurant
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECONDARY CARDS ROW
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SecondaryCardsRow(
    userTier: UserTier,
    streaks: UserStreaks,
    workoutPromoSize: PromoCardSize,
    nutritionPromoSize: PromoCardSize,
    onWorkoutCardClick: () -> Unit,
    onNutritionCardClick: () -> Unit,
    onWorkoutPromoClick: () -> Unit,
    onNutritionPromoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Card: Workout
        if (userTier.hasWorkout()) {
            WorkoutStatusCard(
                streak = streaks.workoutStreak,
                onClick = onWorkoutCardClick,
                modifier = Modifier.weight(1f)
            )
        } else if (workoutPromoSize != PromoCardSize.HIDDEN) {
            WorkoutPromoCard(
                size = workoutPromoSize,
                onClick = onWorkoutPromoClick,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Right Card: Nutrition
        if (userTier.hasNutrition()) {
            NutritionStatusCard(
                streak = streaks.nutritionStreak,
                onClick = onNutritionCardClick,
                modifier = Modifier.weight(1f)
            )
        } else if (nutritionPromoSize != PromoCardSize.HIDDEN) {
            NutritionPromoCard(
                size = nutritionPromoSize,
                onClick = onNutritionPromoClick,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS CARDS (for users with the tier)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun WorkoutStatusCard(
    streak: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .glassBackground(cornerRadius = 16.dp, alpha = 0.35f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = NayaPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Training",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Poppins
                )
            }

            if (streak > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = NayaPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$streak day streak",
                        color = NayaTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = Poppins
                    )
                }
            } else {
                Text(
                    text = "Start your streak!",
                    color = NayaTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = Poppins
                )
            }
        }
    }
}

/**
 * Nutrition Status Card with time-based accent color
 * The accent color follows the daily rhythm like the Nutrition Hero Card
 */
@Composable
fun NutritionStatusCard(
    streak: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = getCurrentNutritionTheme()
    val accentColor = Color(theme.gradientStartHex.toInt())

    Surface(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Nutrition",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Poppins
                )
            }

            if (streak > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$streak day streak",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontFamily = Poppins
                    )
                }
            } else {
                Text(
                    text = "Track your meals!",
                    color = NayaTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = Poppins
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PROMO CARDS (for users without the tier)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun WorkoutPromoCard(
    size: PromoCardSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (size == PromoCardSize.HIDDEN) return

    Surface(
        modifier = modifier
            .height(if (size == PromoCardSize.FULL) 100.dp else 60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = NayaPrimary.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, NayaPrimary.copy(alpha = 0.3f))
    ) {
        if (size == PromoCardSize.FULL) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = NayaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Training",
                        color = NayaPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Poppins
                    )
                }
                Text(
                    text = "Unlock workout tracking",
                    color = NayaTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = Poppins
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = NayaPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "+ Training",
                    color = NayaPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Poppins
                )
            }
        }
    }
}

@Composable
fun NutritionPromoCard(
    size: PromoCardSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (size == PromoCardSize.HIDDEN) return

    val nutritionGreen = Color(0xFF4CAF50)

    Surface(
        modifier = modifier
            .height(if (size == PromoCardSize.FULL) 100.dp else 60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = nutritionGreen.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, nutritionGreen.copy(alpha = 0.3f))
    ) {
        if (size == PromoCardSize.FULL) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = nutritionGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Nutrition",
                        color = nutritionGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Poppins
                    )
                }
                Text(
                    text = "Track your macros",
                    color = NayaTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = Poppins
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = nutritionGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "+ Nutrition",
                    color = nutritionGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Poppins
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SOFT PROMPT CARD (Quick Question)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun QuickQuestionCard(
    onTimeSlotSelected: (WorkoutTimeSlot) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val questionData = QuickQuestionCard()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .glassBackground(cornerRadius = 20.dp, alpha = 0.4f),
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
                Text(
                    text = questionData.question,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SpaceGrotesk
                )
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = NayaTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Time Slot Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                questionData.options.forEach { option ->
                    TimeSlotButton(
                        emoji = option.emoji,
                        label = option.label,
                        onClick = { onTimeSlotSelected(option.timeSlot) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Skip Option
            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = questionData.skipOption,
                    color = NayaTextSecondary,
                    fontSize = 13.sp,
                    fontFamily = Poppins
                )
            }
        }
    }
}

@Composable
private fun TimeSlotButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = NayaPrimary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, NayaPrimary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = Poppins,
                textAlign = TextAlign.Center
            )
        }
    }
}