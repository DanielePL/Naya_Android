package com.example.menotracker.screens.campus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val backgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF2a1f1a), Color(0xFF0f0f0f), Color(0xFF1a1410))
)

@Composable
fun CampusScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Naya Campus",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Coming Soon...",
            color = Color.Gray,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 60.dp)
        )
    }
}
