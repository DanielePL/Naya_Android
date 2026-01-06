package com.example.menotracker.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// NAYA VIOLET - Sanftes Lila/Violett für Wellness (aufgehellt)
// ═══════════════════════════════════════════════════════════════
val NayaPrimary = Color(0xFFA78BFA)           // Helles Violett (Primary) - aufgehellt
val NayaVioletBright = Color(0xFFC4B5FD)      // Sehr hell für Hover/Active States
val NayaVioletGlow = Color(0xFFB89EFC)        // Sanfter Glow-Effekt
val NayaVioletDark = Color(0xFF8B5CF6)        // Dunkler für Kontrast/Pressed States

// Legacy Aliases (für Kompatibilität mit bestehendem Code)
val NayaOrangeBright = NayaVioletBright
val NayaOrangeGlow = NayaVioletGlow
val NayaOrangeDark = NayaVioletDark
val NayaOrange = NayaPrimary
val MenoPrimary = NayaPrimary
val MenoVioletBright = NayaVioletBright
val MenoVioletGlow = NayaVioletGlow
val MenoVioletDark = NayaVioletDark

// ═══════════════════════════════════════════════════════════════
// DARK MODE - BACKGROUND & SURFACE (Coaching-Software aligned)
// ═══════════════════════════════════════════════════════════════
val NayaBackground = Color(0xFF141414)         // HSL(0, 0%, 8%) - Coaching Background
val NayaSurface = Color(0xFF1C1C1C)            // Leicht heller für Cards
val NayaSurfaceVariant = Color(0xFF262626)     // Für sekundäre Surfaces

// Legacy Aliases
val MenoBackground = NayaBackground
val MenoSurface = NayaSurface
val MenoSurfaceVariant = NayaSurfaceVariant

// ═══════════════════════════════════════════════════════════════
// GLASSMORPHISM - Coaching-Software Style
// ═══════════════════════════════════════════════════════════════
val NayaGlass = Color(0xFF333333)              // Base für Glass-Effekte
val NayaGlassBorder = Color(0xFFFFFFFF)        // Wird mit 10% Alpha verwendet
val NayaGlassHover = Color(0xFF404040)         // Hover-State für Glass

// Legacy Aliases
val MenoGlass = NayaGlass
val MenoGlassBorder = NayaGlassBorder
val MenoGlassHover = NayaGlassHover

// ═══════════════════════════════════════════════════════════════
// DARK MODE - TEXT & CONTENT (Coaching-Software Specs)
// ═══════════════════════════════════════════════════════════════
val NayaOnPrimary = Color(0xFFFFFFFF)
val NayaOnBackground = Color(0xFFFAFAFA)       // HSL(0, 0%, 98%) - Primary Text
val NayaOnSurface = Color(0xFFFAFAFA)          // HSL(0, 0%, 98%) - Primary Text
val NayaTextPrimary = Color(0xFFFAFAFA)        // HSL(0, 0%, 98%)
val NayaTextWhite = Color(0xFFFAFAFA)          // Für Kompatibilität
val NayaTextGray = Color(0xFF999999)           // HSL(0, 0%, 60%) - Secondary
val NayaTextSecondary = Color(0xFF999999)      // HSL(0, 0%, 60%)
val NayaTextTertiary = Color(0xFF666666)       // HSL(0, 0%, 45%) - Muted
val NayaUnselected = Color(0xFF666666)         // HSL(0, 0%, 45%)

// Legacy Aliases
val MenoOnPrimary = NayaOnPrimary
val MenoOnBackground = NayaOnBackground
val MenoOnSurface = NayaOnSurface
val MenoTextPrimary = NayaTextPrimary
val MenoTextWhite = NayaTextWhite
val MenoTextGray = NayaTextGray
val MenoTextSecondary = NayaTextSecondary
val MenoTextTertiary = NayaTextTertiary
val MenoUnselected = NayaUnselected

// ═══════════════════════════════════════════════════════════════
// LIGHT MODE - BACKGROUND & SURFACE
// ═══════════════════════════════════════════════════════════════
val NayaBackgroundLight = Color(0xFFFAFAFA)
val NayaSurfaceLight = Color(0xFFFFFFFF)
val NayaSurfaceVariantLight = Color(0xFFF5F5F5)

// Legacy Aliases
val MenoBackgroundLight = NayaBackgroundLight
val MenoSurfaceLight = NayaSurfaceLight
val MenoSurfaceVariantLight = NayaSurfaceVariantLight

// ═══════════════════════════════════════════════════════════════
// LIGHT MODE - TEXT & CONTENT
// ═══════════════════════════════════════════════════════════════
val NayaOnPrimaryLight = Color(0xFFFFFFFF)
val NayaOnBackgroundLight = Color(0xFF1A1A1A)
val NayaOnSurfaceLight = Color(0xFF1A1A1A)
val NayaTextDark = Color(0xFF1A1A1A)
val NayaTextGrayLight = Color(0xFF666666)
val NayaTextSecondaryLight = Color(0xFF4A4A4A)
val NayaUnselectedLight = Color(0xFFBBBBBB)

// Legacy Aliases
val MenoOnPrimaryLight = NayaOnPrimaryLight
val MenoOnBackgroundLight = NayaOnBackgroundLight
val MenoOnSurfaceLight = NayaOnSurfaceLight
val MenoTextDark = NayaTextDark
val MenoTextGrayLight = NayaTextGrayLight
val MenoTextSecondaryLight = NayaTextSecondaryLight
val MenoUnselectedLight = NayaUnselectedLight

// ═══════════════════════════════════════════════════════════════
// NAYA ACCENT COLORS
// ═══════════════════════════════════════════════════════════════
val NayaSecondary = Color(0xFF14B8A6)          // Teal - Ruhe & Balance
val NayaAccent = Color(0xFFEC4899)             // Pink - Feminine Touch
val NayaTertiary = Color(0xFFF59E0B)           // Amber - Energie & Wärme

// Symptom Category Colors
val NayaVasomotor = Color(0xFFEF4444)          // Red - Hitzewallungen
val NayaSleep = Color(0xFF6366F1)              // Indigo - Schlaf
val NayaMood = Color(0xFFA855F7)               // Purple - Stimmung
val NayaBone = Color(0xFF10B981)               // Emerald - Knochen

// Legacy Aliases
val MenoSecondary = NayaSecondary
val MenoAccent = NayaAccent
val MenoTertiary = NayaTertiary
val MenoVasomotor = NayaVasomotor
val MenoSleep = NayaSleep
val MenoMood = NayaMood
val MenoBone = NayaBone

// ═══════════════════════════════════════════════════════════════
// SEMANTIC COLORS - For specific UI purposes
// ═══════════════════════════════════════════════════════════════
val NayaSuccess = Color(0xFF10B981)            // Emerald for success states
val NayaError = Color(0xFFEF4444)              // Red for errors
val NayaWarning = Color(0xFFF59E0B)            // Amber for warnings
val NayaInfo = Color(0xFF3B82F6)               // Blue for info

// Legacy Aliases
val MenoSuccess = NayaSuccess
val MenoError = NayaError
val MenoWarning = NayaWarning
val MenoInfo = NayaInfo

// Nutrition-specific macro colors
val NayaCalories = Color(0xFFA78BFA)           // Violett for calories (brand color)
val NayaProtein = Color(0xFF3B82F6)            // Blue for protein
val NayaCarbs = Color(0xFF10B981)              // Green for carbs
val NayaFat = Color(0xFFFBBF24)                // Yellow for fat

// Legacy Aliases
val MenoCalories = NayaCalories
val MenoProtein = NayaProtein
val MenoCarbs = NayaCarbs
val MenoFat = NayaFat

// Border and divider colors
val NayaBorder = Color(0xFF333333)             // Subtle border for dark mode
val NayaBorderLight = Color(0xFFE0E0E0)        // Subtle border for light mode

// Legacy Aliases
val MenoBorder = NayaBorder
val MenoBorderLight = NayaBorderLight

// ═══════════════════════════════════════════════════════════════
// NAYA COLORS OBJECT - Convenience access for components
// ═══════════════════════════════════════════════════════════════
object NayaColors {
    // Primary brand colors
    val Primary = NayaPrimary
    val PrimaryBright = NayaVioletBright
    val PrimaryDark = NayaVioletDark
    val PrimaryGlow = NayaVioletGlow

    // Accent colors
    val Secondary = NayaSecondary          // Teal
    val Accent = NayaAccent                // Pink
    val Tertiary = NayaTertiary            // Amber

    // Symptom category colors
    val Vasomotor = NayaVasomotor          // Hitzewallungen
    val Sleep = NayaSleep                  // Schlaf
    val Mood = NayaMood                    // Stimmung
    val Bone = NayaBone                    // Knochen

    // Background colors
    val BackgroundDark = NayaBackground
    val BackgroundLight = NayaBackgroundLight
    val Surface = NayaSurface
    val SurfaceVariant = NayaSurfaceVariant
    val CardBackground = NayaSurface

    // Glass/Morphism
    val Glass = NayaGlass
    val GlassBorder = NayaGlassBorder
    val GlassHover = NayaGlassHover

    // Text colors
    val TextPrimary = NayaTextWhite
    val TextSecondary = NayaTextSecondary
    val TextTertiary = NayaTextGray
    val TextDark = NayaTextDark

    // Semantic colors
    val Success = NayaSuccess
    val Error = NayaError
    val Warning = NayaWarning
    val Info = NayaInfo

    // Nutrition macro colors
    val Calories = NayaCalories
    val Protein = NayaProtein
    val Carbs = NayaCarbs
    val Fat = NayaFat

    // Borders
    val Border = NayaBorder
    val BorderLight = NayaBorderLight
}

// Legacy Aliases for compatibility
val MenoColors = NayaColors
