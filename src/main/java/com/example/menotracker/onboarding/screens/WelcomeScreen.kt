package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.BuildConfig
import com.example.menotracker.R
import com.example.menotracker.ui.theme.*

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    onDevSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // ═══════════════════════════════════════════════════════════════
        // FULLSCREEN HERO IMAGE
        // ═══════════════════════════════════════════════════════════════
        Image(
            painter = painterResource(id = R.drawable.onboarding_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ═══════════════════════════════════════════════════════════════
        // GRADIENT OVERLAY (Dark at top and bottom for text readability)
        // ═══════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // ═══════════════════════════════════════════════════════════════
        // CONTENT OVERLAY
        // ═══════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =====================================================
            // DEV SKIP BUTTON - TOP RIGHT (Debug builds only)
            // =====================================================
            if (BuildConfig.DEBUG) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        modifier = Modifier
                            .clickable { onDevSkip() }
                            .padding(4.dp),
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "DEV SKIP",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Push title down (more weight above = lower position)
            Spacer(modifier = Modifier.weight(1.6f))

            // App Name
            Text(
                text = "NAYA",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 4.sp,
                fontFamily = SpaceGrotesk
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "Thrive through menopause\nwith personalized wellness",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                fontFamily = Poppins
            )

            // Push buttons to bottom
            Spacer(modifier = Modifier.weight(1f))

            // =====================================================
            // CALL TO ACTION SECTION
            // =====================================================

            // Get Started Button
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NayaPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Poppins
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login Link
            TextButton(onClick = onLogin) {
                Text(
                    text = "Already have an account? Sign in",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontFamily = Poppins
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
