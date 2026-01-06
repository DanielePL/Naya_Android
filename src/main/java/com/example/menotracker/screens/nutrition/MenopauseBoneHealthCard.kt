package com.example.menotracker.screens.nutrition

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.ExtendedNutrientsSummary
import com.example.menotracker.data.models.NutritionLog

/**
 * Menopause Bone Health Card
 *
 * Zeigt die wichtigsten Nährstoffe für Knochengesundheit in der Menopause:
 * - Calcium (1200mg Ziel)
 * - Vitamin D (20mcg / 800 IU Ziel)
 * - Omega-3 (1000mg Ziel)
 * - Magnesium (320mg Ziel)
 */

// Design colors - Lavender theme
private val LavenderPrimary = Color(0xFFA78BFA)
private val LavenderLight = Color(0xFFC4B5FD)
private val TealAccent = Color(0xFF14B8A6)
private val PinkAccent = Color(0xFFEC4899)
private val CalciumColor = Color(0xFF60A5FA) // Blue
private val VitaminDColor = Color(0xFFFBBF24) // Yellow/Sun
private val Omega3Color = Color(0xFF34D399) // Green
private val MagnesiumColor = Color(0xFFA78BFA) // Purple
private val CardBg = Color(0xFF1E1E1E)
private val CardBgLight = Color(0xFF2A2A2A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF9CA3AF)
private val SuccessGreen = Color(0xFF10B981)
private val WarningOrange = Color(0xFFF97316)

// Menopause-spezifische Ziele (höher als Standard)
object MenopauseNutrientGoals {
    const val CALCIUM_MG = 1200f      // vs 1000mg Standard
    const val VITAMIN_D_MCG = 20f     // = 800 IU
    const val OMEGA3_MG = 1000f
    const val MAGNESIUM_MG = 320f
    const val PROTEIN_G_PER_KG = 1.2f // Erhöht für Muskelerhalt
}

@Composable
fun MenopauseBoneHealthCard(
    nutritionLog: NutritionLog?,
    modifier: Modifier = Modifier,
    onAddFoodClick: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    val nutrients = nutritionLog?.getExtendedNutrients() ?: ExtendedNutrientsSummary()

    // Calculate progress
    val calciumProgress = (nutrients.calcium / MenopauseNutrientGoals.CALCIUM_MG).coerceIn(0f, 1f)
    val vitaminDProgress = (nutrients.vitaminD / MenopauseNutrientGoals.VITAMIN_D_MCG).coerceIn(0f, 1f)
    val omega3Progress = (nutrients.omega3 / MenopauseNutrientGoals.OMEGA3_MG).coerceIn(0f, 1f)
    val magnesiumProgress = (nutrients.magnesium / MenopauseNutrientGoals.MAGNESIUM_MG).coerceIn(0f, 1f)

    // Overall bone health score
    val boneHealthScore = ((calciumProgress + vitaminDProgress + omega3Progress + magnesiumProgress) / 4 * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bone icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LavenderPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = LavenderPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Knochengesundheit",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Menopause-Nährstoffe",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                // Score badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoneHealthScoreBadge(score = boneHealthScore)
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Overview - Always visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickNutrientCircle(
                    label = "Calcium",
                    value = nutrients.calcium,
                    target = MenopauseNutrientGoals.CALCIUM_MG,
                    unit = "mg",
                    color = CalciumColor,
                    icon = Icons.Default.Brightness7
                )
                QuickNutrientCircle(
                    label = "Vitamin D",
                    value = nutrients.vitaminD,
                    target = MenopauseNutrientGoals.VITAMIN_D_MCG,
                    unit = "mcg",
                    color = VitaminDColor,
                    icon = Icons.Default.WbSunny
                )
                QuickNutrientCircle(
                    label = "Omega-3",
                    value = nutrients.omega3,
                    target = MenopauseNutrientGoals.OMEGA3_MG,
                    unit = "mg",
                    color = Omega3Color,
                    icon = Icons.Default.Waves
                )
                QuickNutrientCircle(
                    label = "Magnesium",
                    value = nutrients.magnesium,
                    target = MenopauseNutrientGoals.MAGNESIUM_MG,
                    unit = "mg",
                    color = MagnesiumColor,
                    icon = Icons.Default.Bolt
                )
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = TextGray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Detailed breakdown
                    DetailedNutrientRow(
                        name = "Calcium",
                        value = nutrients.calcium,
                        target = MenopauseNutrientGoals.CALCIUM_MG,
                        unit = "mg",
                        color = CalciumColor,
                        tip = "Milch, Joghurt, Käse, Grünkohl, Sardinen"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailedNutrientRow(
                        name = "Vitamin D",
                        value = nutrients.vitaminD,
                        target = MenopauseNutrientGoals.VITAMIN_D_MCG,
                        unit = "mcg",
                        color = VitaminDColor,
                        tip = "Sonnenlicht, Lachs, Eier, Supplementierung"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailedNutrientRow(
                        name = "Omega-3",
                        value = nutrients.omega3,
                        target = MenopauseNutrientGoals.OMEGA3_MG,
                        unit = "mg",
                        color = Omega3Color,
                        tip = "Lachs, Makrele, Walnüsse, Leinsamen"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailedNutrientRow(
                        name = "Magnesium",
                        value = nutrients.magnesium,
                        target = MenopauseNutrientGoals.MAGNESIUM_MG,
                        unit = "mg",
                        color = MagnesiumColor,
                        tip = "Nüsse, Samen, Vollkorn, dunkle Schokolade"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Recommendations
                    BoneHealthRecommendations(
                        calciumProgress = calciumProgress,
                        vitaminDProgress = vitaminDProgress,
                        omega3Progress = omega3Progress
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add food button
                    Button(
                        onClick = onAddFoodClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LavenderPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lebensmittel hinzufügen")
                    }
                }
            }
        }
    }
}

@Composable
private fun BoneHealthScoreBadge(score: Int) {
    val color = when {
        score >= 75 -> SuccessGreen
        score >= 50 -> VitaminDColor
        else -> WarningOrange
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$score%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun QuickNutrientCircle(
    label: String,
    value: Float,
    target: Float,
    unit: String,
    color: Color,
    icon: ImageVector
) {
    val progress = (value / target).coerceIn(0f, 1f)
    val isComplete = progress >= 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Background circle
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(52.dp),
                strokeWidth = 4.dp,
                color = CardBgLight
            )
            // Progress circle
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(52.dp),
                strokeWidth = 4.dp,
                color = if (isComplete) SuccessGreen else color
            )
            // Icon
            Icon(
                icon,
                contentDescription = null,
                tint = if (isComplete) SuccessGreen else color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${value.toInt()}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isComplete) SuccessGreen else TextWhite
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextGray
        )
    }
}

@Composable
private fun DetailedNutrientRow(
    name: String,
    value: Float,
    target: Float,
    unit: String,
    color: Color,
    tip: String
) {
    val progress = (value / target).coerceIn(0f, 1f)
    val remaining = (target - value).coerceAtLeast(0f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Text(
                text = "${value.toInt()} / ${target.toInt()} $unit",
                fontSize = 13.sp,
                color = if (progress >= 1f) SuccessGreen else TextGray
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (progress >= 1f) SuccessGreen else color,
            trackColor = CardBgLight
        )

        if (remaining > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Noch ${remaining.toInt()} $unit - $tip",
                fontSize = 11.sp,
                color = TextGray
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Tagesziel erreicht!",
                    fontSize = 11.sp,
                    color = SuccessGreen
                )
            }
        }
    }
}

@Composable
private fun BoneHealthRecommendations(
    calciumProgress: Float,
    vitaminDProgress: Float,
    omega3Progress: Float
) {
    val recommendations = mutableListOf<String>()

    if (calciumProgress < 0.5f) {
        recommendations.add("Tipp: Ein Glas Milch (300mg) oder 30g Käse (220mg) würden helfen!")
    }
    if (vitaminDProgress < 0.5f) {
        recommendations.add("Tipp: 15 Min Sonnenlicht oder ein Vitamin D Supplement empfohlen.")
    }
    if (omega3Progress < 0.5f) {
        recommendations.add("Tipp: Lachs (100g = 2000mg Omega-3) oder 1 EL Leinsamen hinzufügen.")
    }

    if (recommendations.isEmpty() && calciumProgress >= 0.75f && vitaminDProgress >= 0.75f) {
        recommendations.add("Super! Du bist auf gutem Weg für deine Knochengesundheit heute!")
    }

    if (recommendations.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = LavenderPrimary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = LavenderLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Empfehlungen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LavenderLight
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                recommendations.forEach { rec ->
                    Text(
                        text = rec,
                        fontSize = 12.sp,
                        color = TextWhite.copy(alpha = 0.9f),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Phytoöstrogen-Highlight für Lebensmittel
 * Zeigt an, ob ein Lebensmittel reich an Phytoöstrogenen ist
 */
@Composable
fun PhytoEstrogenBadge(
    foodName: String,
    modifier: Modifier = Modifier
) {
    val isPhytoEstrogenRich = isPhytoEstrogenFood(foodName)

    if (isPhytoEstrogenRich) {
        Surface(
            modifier = modifier,
            color = PinkAccent.copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Spa,
                    contentDescription = null,
                    tint = PinkAccent,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Phytoöstrogen",
                    fontSize = 9.sp,
                    color = PinkAccent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Prüft ob ein Lebensmittel reich an Phytoöstrogenen ist
 */
private fun isPhytoEstrogenFood(foodName: String): Boolean {
    val phytoEstrogenFoods = listOf(
        "soja", "tofu", "tempeh", "edamame", "sojamilch", "soy",
        "leinsamen", "flaxseed", "linseed",
        "sesam", "sesame",
        "kichererbse", "chickpea", "hummus",
        "linse", "lentil",
        "bohne", "bean",
        "hafer", "oat",
        "gerste", "barley",
        "rotklee", "red clover",
        "hopfen", "hops"
    )

    val lowerName = foodName.lowercase()
    return phytoEstrogenFoods.any { lowerName.contains(it) }
}
