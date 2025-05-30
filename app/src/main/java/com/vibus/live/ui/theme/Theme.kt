package com.vibus.live.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SVTBlue,
    onPrimary = Color.White,
    primaryContainer = SVTBlue.copy(alpha = 0.2f),
    onPrimaryContainer = SVTLightBlue,

    secondary = SVTLightBlue,
    onSecondary = Color.White,
    secondaryContainer = SVTLightBlue.copy(alpha = 0.2f),
    onSecondaryContainer = SVTLightBlue,

    tertiary = SVTAccent,
    onTertiary = Color.White,
    tertiaryContainer = SVTAccent.copy(alpha = 0.2f),
    onTertiaryContainer = SVTAccent,

    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = CardDark,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),

    error = SVTError,
    onError = Color.White,
    errorContainer = SVTError.copy(alpha = 0.2f),
    onErrorContainer = SVTError,

    outline = Color.White.copy(alpha = 0.12f),
    outlineVariant = Color.White.copy(alpha = 0.06f),
)

private val LightColorScheme = lightColorScheme(
    primary = SVTBlue,
    onPrimary = Color.White,
    primaryContainer = SVTBlue.copy(alpha = 0.1f),
    onPrimaryContainer = SVTBlue,

    secondary = SVTLightBlue,
    onSecondary = Color.White,
    secondaryContainer = SVTLightBlue.copy(alpha = 0.1f),
    onSecondaryContainer = SVTLightBlue,

    tertiary = SVTAccent,
    onTertiary = Color.White,
    tertiaryContainer = SVTAccent.copy(alpha = 0.1f),
    onTertiaryContainer = SVTAccent,

    background = BackgroundLight,
    onBackground = Color.Black,
    surface = SurfaceLight,
    onSurface = Color.Black,
    surfaceVariant = CardLight,
    onSurfaceVariant = Color.Black.copy(alpha = 0.7f),

    error = SVTError,
    onError = Color.White,
    errorContainer = SVTError.copy(alpha = 0.1f),
    onErrorContainer = SVTError,

    outline = Color.Black.copy(alpha = 0.12f),
    outlineVariant = Color.Black.copy(alpha = 0.06f),
)

@Composable
fun ViBusLiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent SVT branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Configura la status bar per un look più moderno
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Abilita edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)

            WindowCompat.getInsetsController(window, view).apply {
                // Status bar sempre con testo chiaro per il gradiente SVT
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}