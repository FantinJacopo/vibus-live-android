package com.vibus.live.ui.theme

import androidx.compose.ui.graphics.Color

// SVT Brand Colors
val SVTBlue = Color(0xFF004E89)
val SVTLightBlue = Color(0xFF00A8CC)
val SVTAccent = Color(0xFF7209B7)
val SVTWarning = Color(0xFFFF8500)
val SVTError = Color(0xFFFF6B35)

// Line Colors (matching simulator)
val Line1Color = Color(0xFFFF6B35) // Stanga-Ospedale
val Line2Color = Color(0xFF004E89) // Anconetta-Ferrovieri
val Line3Color = Color(0xFF00A8CC) // Maddalene-Cattane
val Line5Color = Color(0xFF7209B7) // Villaggio-Centro
val Line7Color = Color(0xFFFF8500) // Laghetto-Stadio

// Material Design Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Background Colors
val BackgroundLight = Color(0xFFFFFBFE)
val BackgroundDark = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFF5F5F5)
val SurfaceDark = Color(0xFF2D2D2D)

fun getLineColor(lineNumber: String): Color {
    return when (lineNumber) {
        "1" -> Line1Color
        "2" -> Line2Color
        "3" -> Line3Color
        "5" -> Line5Color
        "7" -> Line7Color
        else -> SVTBlue
    }
}