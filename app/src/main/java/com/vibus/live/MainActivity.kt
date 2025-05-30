package com.vibus.live

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.vibus.live.ui.theme.ViBusLiveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Installa splash screen prima di super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Configura animazione di uscita dello splash screen
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Animazione personalizzata di fade out
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_Y,
                0f,
                -splashScreenView.view.height.toFloat()
            )
            slideUp.interpolator = AnticipateInterpolator()
            slideUp.duration = 500L

            slideUp.doOnEnd {
                splashScreenView.remove()
            }
            slideUp.start()
        }

        // Abilita edge-to-edge per un'esperienza moderna
        enableEdgeToEdge()

        // Configurazione window per gestione status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ViBusLiveTheme {
                // Usa WindowInsets per gestire correttamente le aree di sistema
                ViBusNavGraph(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding() // Padding per tastiera
                )
            }
        }
    }
}