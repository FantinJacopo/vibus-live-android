package com.vibus.live.ui.theme

import androidx.compose.ui.graphics.Color

// SVT Brand Colors - Versione Migliorata
val SVTBlue = Color(0xFF004E89)
val SVTLightBlue = Color(0xFF00A8CC)
val SVTAccent = Color(0xFF7209B7)
val SVTWarning = Color(0xFFFF8500)
val SVTError = Color(0xFFFF6B35)

// Line Colors con gradazioni più ricche
val Line1Color = Color(0xFFE63946) // Rosso vibrante - Stanga-Ospedale
val Line2Color = Color(0xFF1D3557) // Blu scuro elegante - Anconetta-Ferrovieri
val Line3Color = Color(0xFF457B9D) // Azzurro profondo - Maddalene-Cattane
val Line5Color = Color(0xFF663399) // Viola intenso - Villaggio-Centro
val Line7Color = Color(0xFFFF6B35) // Arancione brillante - Laghetto-Stadio

// Colori di stato con variazioni
val StatusGreen = Color(0xFF06D6A0)
val StatusYellow = Color(0xFFFFD60A)
val StatusRed = Color(0xFFEF476F)
val StatusBlue = Color(0xFF118AB2)
val StatusPurple = Color(0xFF7209B7)

// Gradients per effetti moderni
val PrimaryGradient = listOf(
    Color(0xFF667eea),
    Color(0xFF764ba2)
)

val SecondaryGradient = listOf(
    Color(0xFF4facfe),
    Color(0xFF00f2fe)
)

val SuccessGradient = listOf(
    Color(0xFF11998e),
    Color(0xFF38ef7d)
)

val WarningGradient = listOf(
    Color(0xFFffecd2),
    Color(0xFFfcb69f)
)

val ErrorGradient = listOf(
    Color(0xFFfc4a1a),
    Color(0xFFf7b733)
)

// Material Design Colors aggiornati
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Background Colors con profondità
val BackgroundLight = Color(0xFFFAFBFC)
val BackgroundDark = Color(0xFF121212)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1E1E1E)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF2D2D2D)

// Colori per mappe e overlay
val MapOverlay = Color(0x80000000)
val MapAccent = Color(0xFF00BCD4)
val MapMarkerShadow = Color(0x40000000)

// Funzioni helper per colori dinamici
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

fun getLineGradient(lineNumber: String): List<Color> {
    return when (lineNumber) {
        "1" -> listOf(Line1Color.copy(alpha = 0.8f), Line1Color)
        "2" -> listOf(Line2Color.copy(alpha = 0.8f), Line2Color)
        "3" -> listOf(Line3Color.copy(alpha = 0.8f), Line3Color)
        "5" -> listOf(Line5Color.copy(alpha = 0.8f), Line5Color)
        "7" -> listOf(Line7Color.copy(alpha = 0.8f), Line7Color)
        else -> PrimaryGradient
    }
}

fun getStatusColor(delay: Double): Color {
    return when {
        delay <= -1.0 -> StatusGreen  // In anticipo
        delay <= 1.0 -> StatusBlue   // Puntuale
        delay <= 3.0 -> StatusYellow // Lieve ritardo
        delay <= 5.0 -> StatusRed    // Ritardo
        else -> StatusPurple         // Ritardo grave
    }
}