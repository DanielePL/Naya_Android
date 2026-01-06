// app/src/main/java/com/example/myapplicationtest/screens/nutrition/NutritionInsightsScreen.kt

package com.example.menotracker.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.*
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.LocalDate as KotlinLocalDate

// ═══════════════════════════════════════════════════════════════
// PROMETHEUS COLORS
// ═══════════════════════════════════════════════════════════════

private val NayaOrange = Color(0xFFE67E22)
private val BackgroundDark = Color(0xFF121212)
private val CardBackground = Color(0xFF1E1E1E)
private val CardBackgroundLight = Color(0xFF2A2A2A)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFFAAAAAA)
private val SuccessGreen = Color(0xFF27AE60)
private val WarningYellow = Color(0xFFF39C12)
private val DangerRed = Color(0xFFE74C3C)
private val ProteinBlue = Color(0xFF3498DB)
private val CarbsPurple = Color(0xFF9B59B6)
private val FatYellow = Color(0xFFF39C12)

// ═══════════════════════════════════════════════════════════════
// TIME PERIOD ENUM
// ═══════════════════════════════════════════════════════════════

enum class InsightPeriod(val displayName: String, val days: Int) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30)
}

// ═══════════════════════════════════════════════════════════════
// INSIGHTS DATA CLASS
// ═══════════════════════════════════════════════════════════════

data class NutritionInsightsData(
    val period: InsightPeriod,
    val logs: List<NutritionLog>,
    val goal: NutritionGoal?
) {
    // Calorie data for chart
    val caloriesByDay: List<Pair<String, Float>>
        get() = logs.sortedBy { it.date }.map { it.date to it.totalCalories }

    // Averages
    val avgCalories: Float
        get() = if (logs.isNotEmpty()) logs.map { it.totalCalories }.average().toFloat() else 0f

    val avgProtein: Float
        get() = if (logs.isNotEmpty()) logs.map { it.totalProtein }.average().toFloat() else 0f

    val avgCarbs: Float
        get() = if (logs.isNotEmpty()) logs.map { it.totalCarbs }.average().toFloat() else 0f

    val avgFat: Float
        get() = if (logs.isNotEmpty()) logs.map { it.totalFat }.average().toFloat() else 0f

    // Macro percentages
    val totalMacroCalories: Float
        get() = (avgProtein * 4) + (avgCarbs * 4) + (avgFat * 9)

    val proteinPercent: Float
        get() = if (totalMacroCalories > 0) ((avgProtein * 4) / totalMacroCalories) * 100 else 0f

    val carbsPercent: Float
        get() = if (totalMacroCalories > 0) ((avgCarbs * 4) / totalMacroCalories) * 100 else 0f

    val fatPercent: Float
        get() = if (totalMacroCalories > 0) ((avgFat * 9) / totalMacroCalories) * 100 else 0f

    // Extended nutrients averages
    val avgFiber: Float
        get() = if (logs.isNotEmpty()) logs.map { it.totalFiber }.average().toFloat() else 0f

    val avgSugar: Float
        get() = if (logs.isNotEmpty()) logs.map { it.totalSugar }.average().toFloat() else 0f

    val avgSodium: Float
        get() = if (logs.isNotEmpty()) logs.map { it.totalSodium }.average().toFloat() else 0f

    val avgVitaminD: Float
        get() = if (logs.isNotEmpty()) logs.map { it.totalVitaminD }.average().toFloat() else 0f

    // Consistency
    val daysLogged: Int
        get() = logs.count { it.meals.isNotEmpty() }

    val totalDays: Int
        get() = period.days

    val consistencyPercent: Float
        get() = if (totalDays > 0) (daysLogged.toFloat() / totalDays) * 100 else 0f

    // Meal patterns
    val mealTypeCounts: Map<MealType, Int>
        get() = logs.flatMap { it.meals }
            .groupingBy { it.mealType }
            .eachCount()

    val totalMeals: Int
        get() = logs.sumOf { it.meals.size }

    val avgMealsPerDay: Float
        get() = if (daysLogged > 0) totalMeals.toFloat() / daysLogged else 0f
}

// ═══════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionInsightsScreen(
    logs: List<NutritionLog>,
    goal: NutritionGoal?,
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(InsightPeriod.WEEKLY) }

    // Filter logs based on period
    val filteredLogs = remember(logs, selectedPeriod) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        logs.filter { log ->
            try {
                val logDate = KotlinLocalDate.parse(log.date)
                val daysDiff = today.toEpochDays() - logDate.toEpochDays()
                daysDiff in 0 until selectedPeriod.days
            } catch (e: Exception) {
                false
            }
        }
    }

    val insightsData = remember(filteredLogs, selectedPeriod, goal) {
        NutritionInsightsData(
            period = selectedPeriod,
            logs = filteredLogs,
            goal = goal
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nutrition Insights",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selector
            item {
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }

            // Calorie Trend Chart
            item {
                CalorieTrendCard(insightsData)
            }

            // Macro Balance
            item {
                MacroBalanceCard(insightsData)
            }

            // Consistency Card
            item {
                ConsistencyCard(insightsData)
            }

            // Nutrient Alerts
            item {
                NutrientAlertsCard(insightsData)
            }

            // Meal Patterns
            item {
                MealPatternsCard(insightsData)
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PERIOD SELECTOR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PeriodSelector(
    selectedPeriod: InsightPeriod,
    onPeriodSelected: (InsightPeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InsightPeriod.entries.forEach { period ->
            val isSelected = period == selectedPeriod
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPeriodSelected(period) },
                color = if (isSelected) NayaOrange else CardBackground,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = period.displayName,
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextGray
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CALORIE TREND CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CalorieTrendCard(data: NutritionInsightsData) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data.caloriesByDay) {
        withContext(Dispatchers.Default) {
            if (data.caloriesByDay.isNotEmpty()) {
                modelProducer.runTransaction {
                    lineSeries {
                        series(data.caloriesByDay.map { it.second })
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = NayaOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "CALORIE TREND",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart
            if (data.caloriesByDay.isNotEmpty()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom()
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data for this period",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Average",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    Text(
                        text = "${data.avgCalories.toInt()} kcal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                data.goal?.let { goal ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Target",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                        Text(
                            text = "${goal.targetCalories.toInt()} kcal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NayaOrange
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MACRO BALANCE CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MacroBalanceCard(data: NutritionInsightsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = NayaOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MACRO BALANCE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Macro bars
            MacroProgressBar(
                label = "Protein",
                value = data.avgProtein,
                percent = data.proteinPercent,
                color = ProteinBlue,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(12.dp))

            MacroProgressBar(
                label = "Carbs",
                value = data.avgCarbs,
                percent = data.carbsPercent,
                color = CarbsPurple,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(12.dp))

            MacroProgressBar(
                label = "Fat",
                value = data.avgFat,
                percent = data.fatPercent,
                color = FatYellow,
                unit = "g"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ideal ratio hint
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackgroundLight),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Ideal for athletes: 30% P / 40% C / 30% F",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroProgressBar(
    label: String,
    value: Float,
    percent: Float,
    color: Color,
    unit: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextWhite
            )
            Text(
                text = "${value.toInt()}$unit (${percent.toInt()}%)",
                fontSize = 13.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = CardBackgroundLight
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// CONSISTENCY CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ConsistencyCard(data: NutritionInsightsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = NayaOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "CONSISTENCY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ConsistencyStat(
                    value = "${data.daysLogged}/${data.totalDays}",
                    label = "Days Logged",
                    icon = Icons.Default.CalendarMonth
                )

                ConsistencyStat(
                    value = "${data.consistencyPercent.toInt()}%",
                    label = "Consistency",
                    icon = Icons.Default.CheckCircle,
                    valueColor = when {
                        data.consistencyPercent >= 80 -> SuccessGreen
                        data.consistencyPercent >= 50 -> WarningYellow
                        else -> DangerRed
                    }
                )

                ConsistencyStat(
                    value = String.format("%.1f", data.avgMealsPerDay),
                    label = "Meals/Day",
                    icon = Icons.Default.Restaurant
                )
            }
        }
    }
}

@Composable
private fun ConsistencyStat(
    value: String,
    label: String,
    icon: ImageVector,
    valueColor: Color = TextWhite
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextGray
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// NUTRIENT ALERTS CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun NutrientAlertsCard(data: NutritionInsightsData) {
    val alerts = buildList {
        // Check fiber (need more)
        if (data.avgFiber < DailyNutrientTargets.FIBER * 0.7f) {
            add(NutrientAlert(
                name = "Fiber",
                current = data.avgFiber,
                target = DailyNutrientTargets.FIBER,
                unit = "g",
                isLow = true
            ))
        }

        // Check vitamin D (need more)
        if (data.avgVitaminD < DailyNutrientTargets.VITAMIN_D * 0.5f) {
            add(NutrientAlert(
                name = "Vitamin D",
                current = data.avgVitaminD,
                target = DailyNutrientTargets.VITAMIN_D,
                unit = "mcg",
                isLow = true
            ))
        }

        // Check sodium (need less)
        if (data.avgSodium > DailyNutrientTargets.SODIUM_MAX * 0.9f) {
            add(NutrientAlert(
                name = "Sodium",
                current = data.avgSodium,
                target = DailyNutrientTargets.SODIUM_MAX,
                unit = "mg",
                isLow = false
            ))
        }

        // Check sugar (need less)
        if (data.avgSugar > DailyNutrientTargets.SUGAR_MAX * 0.9f) {
            add(NutrientAlert(
                name = "Sugar",
                current = data.avgSugar,
                target = DailyNutrientTargets.SUGAR_MAX,
                unit = "g",
                isLow = false
            ))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningYellow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "NUTRIENT ALERTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (alerts.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All nutrients within range",
                        fontSize = 14.sp,
                        color = SuccessGreen
                    )
                }
            } else {
                alerts.forEach { alert ->
                    AlertRow(alert)
                    if (alert != alerts.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private data class NutrientAlert(
    val name: String,
    val current: Float,
    val target: Float,
    val unit: String,
    val isLow: Boolean
)

@Composable
private fun AlertRow(alert: NutrientAlert) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (alert.isLow) WarningYellow.copy(alpha = 0.1f) else DangerRed.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (alert.isLow) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = if (alert.isLow) WarningYellow else DangerRed,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (alert.isLow) "Low: ${alert.name}" else "High: ${alert.name}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (alert.isLow) WarningYellow else DangerRed
            )
            Text(
                text = "${alert.current.toInt()}${alert.unit} avg, ${if (alert.isLow) "need" else "max"} ${alert.target.toInt()}${alert.unit}",
                fontSize = 11.sp,
                color = TextGray
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MEAL PATTERNS CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MealPatternsCard(data: NutritionInsightsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = NayaOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MEAL PATTERNS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Meal type breakdown
            val mealTypes = MealType.orderedValues()
            val maxCount = data.mealTypeCounts.values.maxOrNull() ?: 1

            mealTypes.forEach { mealType ->
                val count = data.mealTypeCounts[mealType] ?: 0
                val percent = if (data.totalMeals > 0) (count.toFloat() / data.totalMeals) * 100 else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mealType.displayName,
                        fontSize = 13.sp,
                        color = TextWhite,
                        modifier = Modifier.width(80.dp)
                    )

                    LinearProgressIndicator(
                        progress = { if (maxCount > 0) count.toFloat() / maxCount else 0f },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NayaOrange,
                        trackColor = CardBackgroundLight
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "$count (${percent.toInt()}%)",
                        fontSize = 12.sp,
                        color = TextGray,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total meals info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Total: ${data.totalMeals} meals in ${data.period.displayName.lowercase()} period",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
        }
    }
}