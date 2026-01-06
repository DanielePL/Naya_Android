package com.example.menotracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// NAYA Design System - Rounded shapes for modern look
val NayaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // Kleine Elemente, Chips
    small = RoundedCornerShape(12.dp),       // Buttons, kleine Cards
    medium = RoundedCornerShape(16.dp),      // Standard Cards
    large = RoundedCornerShape(24.dp),       // Große Cards, Modals
    extraLarge = RoundedCornerShape(28.dp)   // Full-screen Sheets
)

// Legacy Alias for compatibility
val MenoShapes = NayaShapes
