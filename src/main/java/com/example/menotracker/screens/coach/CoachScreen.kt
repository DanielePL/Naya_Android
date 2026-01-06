package com.example.menotracker.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.screens.ai_coach.AICoachScreen
import com.example.menotracker.screens.ai_coach.AICoachViewModel
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaTextWhite
import com.example.menotracker.ui.theme.NayaTextGray
import com.example.menotracker.ui.theme.AppBackground
import kotlinx.coroutines.launch

// Design System - Naya Premium Dark Theme
private val orangePrimary = NayaPrimary
private val textWhite = NayaTextWhite
private val textGray = NayaTextGray

/**
 * Coach Screen with tabs for Naya Coach and My Coach
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(
    aiCoachViewModel: AICoachViewModel,
    physicalCoachViewModel: PhysicalCoachViewModel,
    onNavigateBack: () -> Unit,
    onStartWorkout: (templateId: String) -> Unit = {}
) {
    val tabs = listOf("Naya Coach", "My Coach")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // Drawer state for conversation history (lifted from AICoachScreen)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Use AppBackground for consistent premium gradient with other screens
    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Header - consistent with Training style (icon + title + action buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Left: Icon + Title
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = orangePrimary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Coach",
                    color = textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }

            // Right: Action buttons (only show on Naya Coach tab)
            if (pagerState.currentPage == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Menu button (conversation history)
                    IconButton(onClick = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Conversations",
                            tint = textGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // New Chat button
                    IconButton(onClick = {
                        aiCoachViewModel.createNewChat()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = orangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = orangePrimary,
            divider = {
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            color = if (pagerState.currentPage == index) orangePrimary else textGray,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    // Naya Coach Tab - without header (header is in CoachScreen)
                    AICoachScreen(
                        viewModel = aiCoachViewModel,
                        onNavigateBack = onNavigateBack,
                        onStartWorkout = onStartWorkout,
                        drawerState = drawerState,
                        showHeader = false
                    )
                }
                1 -> {
                    // My Coach Tab
                    PhysicalCoachScreen(
                        viewModel = physicalCoachViewModel
                    )
                }
            }
        }
        }
    }
}