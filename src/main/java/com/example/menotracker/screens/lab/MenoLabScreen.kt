package com.example.menotracker.screens.lab

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.AppBackground
import kotlinx.coroutines.launch

/**
 * MENOTRACKER WELLNESS LAB
 *
 * The Wellness Lab for women 40+.
 * Symptom tracking, sleep analysis, trends, and AI-powered insights.
 *
 * Modules:
 * - SYMPTOMS: Track menopause symptoms (hot flashes, mood, energy)
 * - SLEEP: Sleep quality and patterns
 * - TRENDS: Historical analysis & progression
 * - INSIGHTS: AI-powered recommendations
 *
 * @author Menotracker Team
 * @version 1.0
 */

// Design System - Calming colors for menopause app
private val lavenderPrimary = Color(0xFFA78BFA)
private val lavenderGlow = Color(0xFFC4B5FD)
private val tealAccent = Color(0xFF14B8A6)
private val pinkAccent = Color(0xFFEC4899)
private val greenSuccess = Color(0xFF10B981)
private val yellowWarning = Color(0xFFFBBF24)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF9CA3AF)

enum class MenoLabTab(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    SYMPTOMS("Symptoms", Icons.Default.Favorite, "Symptom Tracking"),
    MOOD("Mood", Icons.Default.Mood, "Mood Journaling"),
    SLEEP("Sleep", Icons.Default.Bedtime, "Sleep Analysis"),
    TRENDS("Trends", Icons.Default.TrendingUp, "Historical Analysis"),
    INSIGHTS("Insights", Icons.Default.Lightbulb, "Recommendations")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenoLabScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    userId: String = "",
    modifier: Modifier = Modifier
) {
    MenoLabScreenContent(
        onNavigateBack = onNavigateBack,
        onNavigateToPaywall = onNavigateToPaywall,
        userId = userId,
        modifier = modifier
    )
}

// Legacy alias for backward compatibility
@Composable
fun NayaLabScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    MenoLabScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToPaywall = onNavigateToPaywall,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenoLabScreenContent(
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    userId: String = "",
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { MenoLabTab.entries.size })
    val scope = rememberCoroutineScope()

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                MenoLabTopBar(onNavigateBack = onNavigateBack)
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tab Row
                MenoLabTabRow(
                    selectedTab = MenoLabTab.entries[pagerState.currentPage],
                    onTabSelected = { tab ->
                        scope.launch {
                            pagerState.animateScrollToPage(tab.ordinal)
                        }
                    }
                )

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (MenoLabTab.entries[page]) {
                        MenoLabTab.SYMPTOMS -> SymptomsLabTab()
                        MenoLabTab.MOOD -> MoodLabTab(
                            userId = userId,
                            onUpgradeClick = onNavigateToPaywall
                        )
                        MenoLabTab.SLEEP -> SleepLabTab()
                        MenoLabTab.TRENDS -> TrendsLabTab()
                        MenoLabTab.INSIGHTS -> InsightsLabTab()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenoLabTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = lavenderGlow,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "WELLNESS LAB",
                        color = textWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Deine Gesundheit im Blick",
                        color = lavenderGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textWhite
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun MenoLabTabRow(
    selectedTab: MenoLabTab,
    onTabSelected: (MenoLabTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = lavenderGlow,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            // Custom indicator - none, we use custom styling
        },
        divider = {}
    ) {
        MenoLabTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) lavenderPrimary.copy(alpha = 0.2f) else Color.Transparent,
                label = "tabBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) lavenderGlow else textGray.copy(alpha = 0.3f),
                label = "tabBorder"
            )

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = backgroundColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) lavenderGlow else textGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tab.title,
                            color = if (isSelected) textWhite else textGray,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Tab content composables are defined in separate files:
// - SymptomsLabTab.kt
// - SleepLabTab.kt
// - TrendsLabTab.kt
// - InsightsLabTab.kt
