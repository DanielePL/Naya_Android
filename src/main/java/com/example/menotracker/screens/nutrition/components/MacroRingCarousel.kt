package com.example.menotracker.screens.nutrition.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.*
import com.example.menotracker.data.NutritionTipEngine
import com.example.menotracker.data.NutritionTip
import com.example.menotracker.data.QuickWin
import com.example.menotracker.data.TipCategory

// ═══════════════════════════════════════════════════════════════
// PROMETHEUS BRANDING COLORS
// ═══════════════════════════════════════════════════════════════

private val NayaOrange = Color(0xFFE67E22)
private val Surface = Color(0xFF1C1C1C)
private val SurfaceVariant = Color(0xFF262626)
private val TextPrimary = Color(0xFFFAFAFA)
private val TextSecondary = Color(0xFF999999)
private val GlassBase = Color(0xFF333333)

// Quality Colors
private val QualityExcellent = Color(0xFF4CAF50)
private val QualityGood = Color(0xFF8BC34A)
private val QualityModerate = Color(0xFFF97316)
private val QualityPoor = Color(0xFFEF4444)

// Macro Colors
private val CaloriesOrange = Color(0xFFF97316)
private val ProteinBlue = Color(0xFF3B82F6)
private val CarbsGreen = Color(0xFF10B981)
private val FatYellow = Color(0xFFFBBF24)
private val AnabolicPurple = Color(0xFFA78BFA)
private val ProcessedRed = Color(0xFFDC2626)  // Red for ultra-processed tracking

// ═══════════════════════════════════════════════════════════════
// MACRO RING DATA
// ═══════════════════════════════════════════════════════════════

data class MacroRingData(
    val type: MacroType,
    val current: Float,
    val target: Float,
    val unit: String,
    val qualityLevel: QualityLevel?,
    val qualityDetails: String?,
    val breakdown: List<MacroBreakdownItem> = emptyList(),
    val quickWins: List<QuickWin> = emptyList()  // Actionable tips
)

data class MacroBreakdownItem(
    val name: String,
    val amount: Float,
    val unit: String,
    val qualityScore: Float? = null
)

enum class MacroType(
    val displayName: String,
    val shortName: String,
    val icon: ImageVector,
    val baseColor: Color
) {
    CALORIES("Calories", "kcal", Icons.Default.LocalFireDepartment, CaloriesOrange),
    PROTEIN("Protein", "Prot", Icons.Default.FitnessCenter, ProteinBlue),
    CARBS("Carbs", "Carb", Icons.Default.Grain, CarbsGreen),
    FATS("Fats", "Fat", Icons.Default.WaterDrop, FatYellow),
    PROCESSED("Processed", "UPF", Icons.Default.WarningAmber, ProcessedRed),  // NOVA Ultra-Processed tracking
    ANABOLIC("Anabolic", "MPS", Icons.Default.Bolt, AnabolicPurple)  // Muscle Protein Synthesis
}

// ═══════════════════════════════════════════════════════════════
// MAIN COMPONENT - TAP TO FOCUS
// ═══════════════════════════════════════════════════════════════

@Composable
fun MacroRingCarousel(
    modifier: Modifier = Modifier,
    nutritionLog: NutritionLog?,
    macroQuality: MacroQualitySummary?,
    anabolicWindow: AnabolicWindow? = null,
    dietaryProfile: DietaryProfile = DietaryProfile.DEFAULT,
    onRingClick: (MacroType) -> Unit = {}
) {
    // Create tip engine with user's dietary profile
    val tipEngine = remember(dietaryProfile) {
        NutritionTipEngine(dietaryProfile)
    }

    val rings = remember(nutritionLog, macroQuality, anabolicWindow, dietaryProfile) {
        buildRingData(nutritionLog, macroQuality, anabolicWindow, tipEngine)
    }

    // Start with Protein focused (index 1)
    var focusedIndex by remember { mutableIntStateOf(1) }

    // Glassmorphism container
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF252525).copy(alpha = 0.9f),
                        Color(0xFF1C1C1C).copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // ═══════════════════════════════════════════════════════
        // ALL RINGS IN A ROW - TAP TO FOCUS
        // ═══════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            rings.forEachIndexed { index, ring ->
                val isFocused = index == focusedIndex

                TappableRing(
                    ring = ring,
                    isFocused = isFocused,
                    onClick = {
                        focusedIndex = index
                        onRingClick(ring.type)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═══════════════════════════════════════════════════════
        // DETAILS FOR FOCUSED RING
        // ═══════════════════════════════════════════════════════
        val focusedRing = rings.getOrNull(focusedIndex)
        focusedRing?.let { ring ->
            MacroQualityDetails(ring = ring)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAPPABLE RING - ANIMATES SIZE ON FOCUS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TappableRing(
    ring: MacroRingData,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    // Animate size - focused ring is notably larger
    val size by animateDpAsState(
        targetValue = if (isFocused) 88.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ringSize"
    )

    // Animate alpha
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.55f,
        animationSpec = tween(200),
        label = "ringAlpha"
    )

    // Animate stroke width - thicker for focused ring
    val strokeWidth by animateDpAsState(
        targetValue = if (isFocused) 10.dp else 4.dp,
        animationSpec = tween(200),
        label = "strokeWidth"
    )

    val progress = (ring.current / ring.target).coerceIn(0f, 1.5f)

    // Animate progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Quality-based ring color or base color
    val ringColor = ring.qualityLevel?.let { getQualityColor(it) } ?: ring.type.baseColor

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ring - aspectRatio(1f) ensures it stays circular
        Box(
            modifier = Modifier
                .size(size)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            // Background track
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
            ) {
                val stroke = strokeWidth.toPx()
                val radius = (this.size.minDimension - stroke) / 2

                drawCircle(
                    color = SurfaceVariant,
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            // Progress arc
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
            ) {
                val stroke = strokeWidth.toPx()

                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress.coerceAtMost(1f),
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(this.size.width - stroke, this.size.height - stroke)
                )

                // Overflow indicator (> 100%)
                if (animatedProgress > 1f) {
                    val overflowSweep = 360f * (animatedProgress - 1f).coerceAtMost(0.5f)
                    drawArc(
                        color = ringColor.copy(alpha = 0.4f),
                        startAngle = -90f,
                        sweepAngle = overflowSweep,
                        useCenter = false,
                        style = Stroke(width = stroke * 0.6f, cap = StrokeCap.Round),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(this.size.width - stroke, this.size.height - stroke)
                    )
                }
            }

            // Center value - smaller text for smaller rings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatValue(ring.current),
                    fontSize = if (isFocused) 14.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringColor.copy(alpha = alpha)
                )
                if (isFocused) {
                    Text(
                        text = "/${formatValue(ring.target)}",
                        fontSize = 8.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Label
        Text(
            text = ring.type.shortName,
            fontSize = if (isFocused) 12.sp else 10.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isFocused) TextPrimary else TextSecondary.copy(alpha = 0.7f)
        )

        // Quality indicator dot (only when focused and has quality)
        if (isFocused && ring.qualityLevel != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(getQualityColor(ring.qualityLevel))
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// QUALITY DETAILS PANEL
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MacroQualityDetails(
    ring: MacroRingData,
    modifier: Modifier = Modifier
) {
    // Collapsed by default to save space
    var isExpanded by remember { mutableStateOf(false) }

    // Check if there's expandable content
    val hasExpandableContent = (ring.quickWins.isNotEmpty() && ring.qualityLevel != QualityLevel.EXCELLENT)
        || ring.breakdown.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = GlassBase.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // Header: Macro name + Quality badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ring.type.icon,
                    contentDescription = null,
                    tint = ring.type.baseColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ring.type.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            ring.qualityLevel?.let { level ->
                QualityBadge(qualityLevel = level)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress text + Quality message in one row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress text
            Text(
                text = "${formatValue(ring.current)} / ${formatValue(ring.target)}${ring.unit}",
                fontSize = 14.sp,
                color = TextSecondary
            )

            // Expand/Collapse button (only if there's expandable content)
            if (hasExpandableContent) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Less" else "Tips",
                        fontSize = 11.sp,
                        color = NayaOrange,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = NayaOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Quality message (always visible, compact)
        ring.qualityDetails?.let { details ->
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = when (ring.qualityLevel) {
                        QualityLevel.EXCELLENT -> Icons.Default.CheckCircle
                        QualityLevel.GOOD -> Icons.Default.ThumbUp
                        QualityLevel.MODERATE -> Icons.Default.Info
                        QualityLevel.POOR -> Icons.Default.Warning
                        null -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = ring.qualityLevel?.let { getQualityColor(it) } ?: TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = details,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    lineHeight = 14.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ═══════════════════════════════════════════════════════════
        // EXPANDABLE CONTENT - Quick Wins & Sources
        // ═══════════════════════════════════════════════════════════
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column {
                // QUICK WINS SECTION - Actionable tips when not excellent
                if (ring.quickWins.isNotEmpty() && ring.qualityLevel != QualityLevel.EXCELLENT) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Wins Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = NayaOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick Wins",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NayaOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Win Items
                    ring.quickWins.take(3).forEach { quickWin ->
                        QuickWinRow(quickWin = quickWin)
                    }
                }

                // Breakdown list
                if (ring.breakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Breakdown header
                    Text(
                        text = "Sources",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    ring.breakdown.take(4).forEach { item ->
                        BreakdownRow(item = item, macroColor = ring.type.baseColor)
                    }

                    if (ring.breakdown.size > 4) {
                        Text(
                            text = "+${ring.breakdown.size - 4} more",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// QUICK WIN ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun QuickWinRow(quickWin: QuickWin) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Action
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "•",
                fontSize = 12.sp,
                color = NayaOrange,
                modifier = Modifier.padding(end = 6.dp, top = 1.dp)
            )
            Text(
                text = quickWin.action,
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 16.sp
            )
        }

        // Benefit badge
        Text(
            text = quickWin.benefit,
            fontSize = 10.sp,
            color = QualityGood,
            modifier = Modifier.padding(start = 14.dp, top = 2.dp)
        )

        // Swap suggestion if available
        quickWin.swap?.let { swap ->
            Row(
                modifier = Modifier
                    .padding(start = 14.dp, top = 4.dp)
                    .background(
                        color = SurfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = swap.from,
                    fontSize = 10.sp,
                    color = QualityPoor,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(12.dp)
                        .padding(horizontal = 2.dp)
                )
                Text(
                    text = swap.to,
                    fontSize = 10.sp,
                    color = QualityGood,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = swap.benefit,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    item: MacroBreakdownItem,
    macroColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quality dot
        item.qualityScore?.let { score ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(getQualityColor(QualityLevel.fromScore(score)))
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Name (limited width to prevent collision with progress bar)
        Text(
            text = item.name,
            fontSize = 11.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f, fill = false).widthIn(max = 150.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        // Mini progress bar
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((item.amount / 50f).coerceIn(0f, 1f))
                    .background(
                        item.qualityScore?.let { getQualityColor(QualityLevel.fromScore(it)) }
                            ?: macroColor
                    )
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Value
        Text(
            text = "${item.amount.toInt()}${item.unit}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.width(35.dp),
            textAlign = TextAlign.End
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// QUALITY BADGE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun QualityBadge(qualityLevel: QualityLevel) {
    val (text, color) = when (qualityLevel) {
        QualityLevel.EXCELLENT -> "Excellent" to QualityExcellent
        QualityLevel.GOOD -> "Good" to QualityGood
        QualityLevel.MODERATE -> "Moderate" to QualityModerate
        QualityLevel.POOR -> "Needs Work" to QualityPoor
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun getQualityColor(level: QualityLevel): Color = when (level) {
    QualityLevel.EXCELLENT -> QualityExcellent
    QualityLevel.GOOD -> QualityGood
    QualityLevel.MODERATE -> QualityModerate
    QualityLevel.POOR -> QualityPoor
}

private fun formatValue(value: Float): String {
    return if (value >= 1000) {
        String.format("%.1fk", value / 1000)
    } else {
        value.toInt().toString()
    }
}

private fun buildRingData(
    nutritionLog: NutritionLog?,
    macroQuality: MacroQualitySummary?,
    anabolicWindow: AnabolicWindow?,
    tipEngine: NutritionTipEngine? = null
): List<MacroRingData> {
    val log = nutritionLog ?: return getEmptyRingData()

    // Generate tips from the engine
    val tips = macroQuality?.let { tipEngine?.generateTips(it) } ?: emptyList()

    val rings = mutableListOf<MacroRingData>()

    // 1. CALORIES
    rings.add(
        MacroRingData(
            type = MacroType.CALORIES,
            current = log.totalCalories,
            target = log.targetCalories ?: 2200f,
            unit = "",
            qualityLevel = null,
            qualityDetails = getCalorieMessage(log.totalCalories, log.targetCalories ?: 2200f),
            breakdown = emptyList()
        )
    )

    // 2. PROTEIN
    val proteinBreakdown = log.meals.flatMap { meal ->
        meal.items.map { item ->
            MacroBreakdownItem(
                name = item.itemName,
                amount = item.protein,
                unit = "g",
                qualityScore = ProteinDiaasScores.getScore(item.itemName)
            )
        }
    }.filter { it.amount > 0 }.sortedByDescending { it.amount }

    // Get quick wins for protein
    val proteinQuickWins = tips
        .filter { it.category == TipCategory.PROTEIN_QUALITY || it.category == TipCategory.PROTEIN_QUANTITY }
        .flatMap { it.quickWins }

    rings.add(
        MacroRingData(
            type = MacroType.PROTEIN,
            current = log.totalProtein,
            target = log.targetProtein ?: 150f,
            unit = "g",
            qualityLevel = macroQuality?.protein?.qualityLevel,
            qualityDetails = macroQuality?.protein?.suggestion
                ?: "DIAAS: ${String.format("%.2f", macroQuality?.protein?.weightedDiaasScore ?: 0.7f)}",
            breakdown = proteinBreakdown,
            quickWins = proteinQuickWins
        )
    )

    // 3. CARBS
    val carbsBreakdown = log.meals.flatMap { meal ->
        meal.items.map { item ->
            val gi = GlycemicIndexScores.getGI(item.itemName)
            MacroBreakdownItem(
                name = item.itemName,
                amount = item.carbs,
                unit = "g",
                qualityScore = (100f - gi) / 100f
            )
        }
    }.filter { it.amount > 0 }.sortedByDescending { it.amount }

    // Get quick wins for carbs
    val carbsQuickWins = tips
        .filter { it.category == TipCategory.CARB_QUALITY || it.category == TipCategory.CARB_TIMING }
        .flatMap { it.quickWins }

    rings.add(
        MacroRingData(
            type = MacroType.CARBS,
            current = log.totalCarbs,
            target = log.targetCarbs ?: 250f,
            unit = "g",
            qualityLevel = macroQuality?.carbs?.qualityLevel,
            qualityDetails = macroQuality?.carbs?.suggestion
                ?: "GI: ${macroQuality?.carbs?.averageGI?.toInt() ?: 55} · GL: ${macroQuality?.carbs?.totalGlycemicLoad?.toInt() ?: 0}",
            breakdown = carbsBreakdown,
            quickWins = carbsQuickWins
        )
    )

    // 4. FATS
    val fatsBreakdown = listOf(
        MacroBreakdownItem("Saturated", log.totalSaturatedFat, "g", 0.3f),
        MacroBreakdownItem("Unsaturated", log.totalUnsaturatedFat, "g", 0.9f),
        MacroBreakdownItem("Omega-3", log.totalOmega3 / 1000f, "g", 1.0f),
        MacroBreakdownItem("Omega-6", log.totalOmega6 / 1000f, "g", 0.6f),
        MacroBreakdownItem("Trans Fat", log.totalTransFat, "g", 0.0f)
    ).filter { it.amount > 0 }

    // Get quick wins for fats
    val fatsQuickWins = tips
        .filter { it.category == TipCategory.FAT_BALANCE || it.category == TipCategory.OMEGA_RATIO }
        .flatMap { it.quickWins }

    rings.add(
        MacroRingData(
            type = MacroType.FATS,
            current = log.totalFat,
            target = log.targetFat ?: 70f,
            unit = "g",
            qualityLevel = macroQuality?.fats?.qualityLevel,
            qualityDetails = macroQuality?.fats?.suggestion
                ?: "Ω6:Ω3 = ${String.format("%.1f", macroQuality?.fats?.omega6to3Ratio ?: 15f)}:1",
            breakdown = fatsBreakdown,
            quickWins = fatsQuickWins
        )
    )

    // 5. PROCESSED FOOD (NOVA Classification - UPF tracking)
    macroQuality?.processedFood?.let { processedScore ->
        // Build breakdown by NOVA category
        val novaBreakdown = processedScore.novaDistribution
            .filter { it.value > 0 }
            .map { (classification, calories) ->
                val percentOfTotal = if (processedScore.totalCalories > 0) {
                    (calories / processedScore.totalCalories * 100)
                } else 0f

                MacroBreakdownItem(
                    name = when (classification) {
                        NovaClassification.UNPROCESSED -> "NOVA 1 - Whole Foods"
                        NovaClassification.CULINARY_INGREDIENT -> "NOVA 2 - Ingredients"
                        NovaClassification.PROCESSED -> "NOVA 3 - Processed"
                        NovaClassification.ULTRA_PROCESSED -> "NOVA 4 - Ultra-Processed"
                    },
                    amount = percentOfTotal,
                    unit = "%",
                    qualityScore = when (classification) {
                        NovaClassification.UNPROCESSED -> 1.0f
                        NovaClassification.CULINARY_INGREDIENT -> 0.85f
                        NovaClassification.PROCESSED -> 0.5f
                        NovaClassification.ULTRA_PROCESSED -> 0.15f
                    }
                )
            }
            .sortedByDescending { it.amount }

        // Add worst offenders if any
        val worstOffendersBreakdown = processedScore.worstOffenders.take(3).map { foodName ->
            MacroBreakdownItem(
                name = "↳ $foodName",
                amount = 0f,
                unit = "",
                qualityScore = 0.15f  // Red for UPF
            )
        }

        val fullBreakdown = novaBreakdown + worstOffendersBreakdown

        // Get quick wins for processed food
        val processedQuickWins = tips
            .filter { it.category == TipCategory.PROCESSED_FOOD }
            .flatMap { it.quickWins }

        rings.add(
            MacroRingData(
                type = MacroType.PROCESSED,
                // Show UPF % as current, target is 20% max
                current = processedScore.ultraProcessedPercentage,
                target = ProcessedFoodScore.TARGET_UPF_PERCENT,
                unit = "%",
                // Inverted quality - lower UPF = better quality
                qualityLevel = processedScore.qualityLevel,
                qualityDetails = processedScore.suggestion
                    ?: "UPF: ${processedScore.ultraProcessedPercentage.toInt()}% of calories (target: <${ProcessedFoodScore.TARGET_UPF_PERCENT.toInt()}%)",
                breakdown = fullBreakdown,
                quickWins = processedQuickWins
            )
        )
    }

    // 6. ANABOLIC (only if active)
    anabolicWindow?.let { window ->
        if (window.isExtendedWindowActive()) {
            val minutesRemaining = window.getMinutesRemaining()
            val targetProtein = window.getRecommendedProtein(80f)

            rings.add(
                MacroRingData(
                    type = MacroType.ANABOLIC,
                    current = window.proteinIntakeSince,
                    target = targetProtein,
                    unit = "g",
                    qualityLevel = when (window.getUrgency()) {
                        AnabolicWindowUrgency.SATISFIED -> QualityLevel.EXCELLENT
                        AnabolicWindowUrgency.MODERATE -> QualityLevel.GOOD
                        AnabolicWindowUrgency.HIGH -> QualityLevel.MODERATE
                        AnabolicWindowUrgency.CRITICAL, AnabolicWindowUrgency.MISSED -> QualityLevel.POOR
                        AnabolicWindowUrgency.CLOSED -> QualityLevel.MODERATE
                    },
                    qualityDetails = when {
                        minutesRemaining > 0 -> "⏱️ ${minutesRemaining}min left"
                        window.proteinIntakeSince >= targetProtein -> "✅ Window utilized!"
                        else -> "Window closed"
                    },
                    breakdown = emptyList()
                )
            )
        }
    }

    return rings
}

private fun getEmptyRingData(): List<MacroRingData> = listOf(
    MacroRingData(MacroType.CALORIES, 0f, 2200f, "", null, "Start logging to track calories", emptyList()),
    MacroRingData(MacroType.PROTEIN, 0f, 150f, "g", null, "Log meals to see protein quality", emptyList()),
    MacroRingData(MacroType.CARBS, 0f, 250f, "g", null, "Track carb quality with GI scores", emptyList()),
    MacroRingData(MacroType.FATS, 0f, 70f, "g", null, "Monitor fat composition", emptyList()),
    MacroRingData(MacroType.PROCESSED, 0f, 20f, "%", QualityLevel.EXCELLENT, "Track ultra-processed food intake", emptyList())
)

private fun getCalorieMessage(current: Float, target: Float): String {
    val diff = target - current
    return when {
        diff > 500 -> "${diff.toInt()} kcal remaining"
        diff > 0 -> "${diff.toInt()} kcal to go"
        diff > -200 -> "On target!"
        else -> "${(-diff).toInt()} kcal over"
    }
}