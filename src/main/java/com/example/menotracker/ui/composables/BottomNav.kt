package com.example.menotracker.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaGlass
import com.example.menotracker.ui.theme.NayaGlassBorder

// Glassmorphism background for footer
private val glassBackground = Color(0xFF1A1A1A).copy(alpha = 0.85f)
private val topBorderAccent = Brush.horizontalGradient(
    colors = listOf(
        Color.Transparent,
        Color.White.copy(alpha = 0.12f),
        NayaPrimary.copy(alpha = 0.35f),
        Color.White.copy(alpha = 0.12f),
        Color.Transparent
    )
)

@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    // Glassmorphism container with premium top border accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(glassBackground)
            .border(
                width = 1.dp,
                brush = topBorderAccent,
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
    ) {
        NavigationBar(
            containerColor = glassBackground,
            contentColor = NayaPrimary,
            modifier = Modifier.fillMaxWidth(),
            windowInsets = NavigationBarDefaults.windowInsets // Keep the fix for footer height
        ) {
        // Tab 0: Home
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(28.dp)) },
            label = { Text("Home", fontSize = 12.sp) },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NayaPrimary,
                selectedTextColor = NayaPrimary,
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999),
                indicatorColor = Color.Transparent
            )
        )

        // Tab 1: Library
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.LocalLibrary, contentDescription = "Library", modifier = Modifier.size(28.dp)) },
            label = { Text("Library", fontSize = 12.sp) },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NayaPrimary,
                selectedTextColor = NayaPrimary,
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999),
                indicatorColor = Color.Transparent
            )
        )

        // Tab 2: Training
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = "Training", modifier = Modifier.size(28.dp)) },
            label = { Text("Training", fontSize = 12.sp) },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NayaPrimary,
                selectedTextColor = NayaPrimary,
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999),
                indicatorColor = Color.Transparent
            )
        )

        // Tab 3: Calendar
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Calendar", modifier = Modifier.size(28.dp)) },
            label = { Text("Calendar", fontSize = 12.sp) },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NayaPrimary,
                selectedTextColor = NayaPrimary,
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999),
                indicatorColor = Color.Transparent
            )
        )

            // Tab 4: Account
            NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Account", modifier = Modifier.size(28.dp)) },
                label = { Text("Account", fontSize = 12.sp) },
                selected = selectedTab == 4,
                onClick = { onTabSelected(4) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NayaPrimary,
                    selectedTextColor = NayaPrimary,
                    unselectedIconColor = Color(0xFF999999),
                    unselectedTextColor = Color(0xFF999999),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
