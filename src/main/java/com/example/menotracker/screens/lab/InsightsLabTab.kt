package com.example.menotracker.screens.lab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

/**
 * INSIGHTS LAB TAB
 *
 * AI-Powered Training Recommendations.
 * Intelligent analysis of your training data.
 *
 * Features:
 * - Fatigue Alerts
 * - Progress Insights
 * - Technique Feedback
 * - Load Recommendations
 * - Recovery Suggestions
 *
 * @author Naya Team
 */

// Colors
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val cyanAccent = Color(0xFF00D9FF)
private val greenSuccess = Color(0xFF00FF88)
private val yellowWarning = Color(0xFFFFD93D)
private val redError = Color(0xFFFF4757)
private val purpleAccent = Color(0xFFB388FF)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val cardBg = Color(0xFF1E1E1E)

enum class InsightType {
    FATIGUE,
    PROGRESS,
    TECHNIQUE,
    LOAD,
    RECOVERY
}

enum class InsightPriority {
    HIGH,
    MEDIUM,
    LOW
}

data class LabInsight(
    val type: InsightType,
    val priority: InsightPriority,
    val title: String,
    val description: String,
    val action: String?,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun InsightsLabTab() {
    // Mock insights data
    val insights = remember {
        listOf(
            LabInsight(
                type = InsightType.FATIGUE,
                priority = InsightPriority.HIGH,
                title = "Velocity Drop Detected",
                description = "Your squat velocity dropped 25% by rep 5 in today's session. This indicates accumulated fatigue.",
                action = "Consider reducing volume or adding rest between sets.",
                icon = Icons.Default.Warning,
                color = redError
            ),
            LabInsight(
                type = InsightType.PROGRESS,
                priority = InsightPriority.MEDIUM,
                title = "Squat 1RM Increased",
                description = "Your estimated squat 1RM increased by 5kg (8%) over the last 4 weeks. Progressive overload is working!",
                action = null,
                icon = Icons.Default.TrendingUp,
                color = greenSuccess
            ),
            LabInsight(
                type = InsightType.TECHNIQUE,
                priority = InsightPriority.MEDIUM,
                title = "Bar Path Drifting",
                description = "Bar path has been drifting right on heavy squats (>10% drift at 120kg+).",
                action = "Focus on even foot pressure and consider unilateral work.",
                icon = Icons.Default.Straighten,
                color = yellowWarning
            ),
            LabInsight(
                type = InsightType.LOAD,
                priority = InsightPriority.HIGH,
                title = "ACWR Spike Warning",
                description = "Your training load increased 45% this week. ACWR at 1.45 indicates elevated injury risk.",
                action = "Consider a lighter session tomorrow or active recovery.",
                icon = Icons.Default.Speed,
                color = orangeGlow
            ),
            LabInsight(
                type = InsightType.RECOVERY,
                priority = InsightPriority.LOW,
                title = "Optimal Recovery Day",
                description = "Based on your training pattern, tomorrow is ideal for a rest day or light cardio.",
                action = "Schedule light activity or mobility work.",
                icon = Icons.Default.SelfImprovement,
                color = cyanAccent
            ),
            LabInsight(
                type = InsightType.PROGRESS,
                priority = InsightPriority.LOW,
                title = "Technique Improving",
                description = "Your average technique score improved from 72 to 87 over 8 weeks. Great consistency!",
                action = null,
                icon = Icons.Default.CheckCircle,
                color = greenSuccess
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header with summary
        item {
            InsightsSummaryCard(insights = insights)
        }

        // High Priority Insights
        val highPriority = insights.filter { it.priority == InsightPriority.HIGH }
        if (highPriority.isNotEmpty()) {
            item {
                Text(
                    text = "REQUIRES ATTENTION",
                    color = redError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            items(highPriority) { insight ->
                InsightCard(insight = insight)
            }
        }

        // Medium Priority Insights
        val mediumPriority = insights.filter { it.priority == InsightPriority.MEDIUM }
        if (mediumPriority.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "RECOMMENDATIONS",
                    color = yellowWarning,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            items(mediumPriority) { insight ->
                InsightCard(insight = insight)
            }
        }

        // Low Priority Insights
        val lowPriority = insights.filter { it.priority == InsightPriority.LOW }
        if (lowPriority.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "GOOD TO KNOW",
                    color = greenSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            items(lowPriority) { insight ->
                InsightCard(insight = insight)
            }
        }

        // Ask Coach Card
        item {
            AskCoachCard()
        }

        // AI Info Card
        item {
            AIInfoCard()
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun InsightsSummaryCard(insights: List<LabInsight>) {
    val highCount = insights.count { it.priority == InsightPriority.HIGH }
    val mediumCount = insights.count { it.priority == InsightPriority.MEDIUM }
    val lowCount = insights.count { it.priority == InsightPriority.LOW }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI INSIGHTS",
                        color = textGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Based on your training data",
                        color = textGray.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                Surface(
                    color = purpleAccent.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = purpleAccent,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InsightCountBadge(
                    count = highCount,
                    label = "High",
                    color = redError
                )
                InsightCountBadge(
                    count = mediumCount,
                    label = "Medium",
                    color = yellowWarning
                )
                InsightCountBadge(
                    count = lowCount,
                    label = "Low",
                    color = greenSuccess
                )
            }
        }
    }
}

@Composable
private fun InsightCountBadge(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = count.toString(),
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = textGray,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun InsightCard(insight: LabInsight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when (insight.priority) {
                InsightPriority.HIGH -> insight.color.copy(alpha = 0.5f)
                else -> Color.Transparent
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Surface(
                    color = insight.color.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = insight.icon,
                            contentDescription = null,
                            tint = insight.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Type badge
                    Text(
                        text = insight.type.name,
                        color = insight.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    // Title
                    Text(
                        text = insight.title,
                        color = textWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Priority indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when (insight.priority) {
                                InsightPriority.HIGH -> redError
                                InsightPriority.MEDIUM -> yellowWarning
                                InsightPriority.LOW -> greenSuccess
                            },
                            CircleShape
                        )
                )
            }

            Spacer(Modifier.height(12.dp))

            // Description
            Text(
                text = insight.description,
                color = textGray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Action (if present)
            insight.action?.let { action ->
                Spacer(Modifier.height(12.dp))

                Surface(
                    color = insight.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = insight.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = action,
                            color = textWhite,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AskCoachCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = orangePrimary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, orangeGlow.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = orangePrimary.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ask Naya Coach",
                    color = textWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Get personalized advice based on your lab data",
                    color = textGray,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AIInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = textGray,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Insights are generated using machine learning models trained on sports science research and your personal training data. They are suggestions, not medical advice.",
                color = textGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}