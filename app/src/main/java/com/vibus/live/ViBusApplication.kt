package com.vibus.live

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ViBusApplication : Application() {

    companion object {
        private const val TAG = "ViBusApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ViBus Live Application initialized")

        // Inizializzazioni globali se necessarie
        initializeApp()
    }

    private fun initializeApp() {
        // Setup globale dell'app
        Log.d(TAG, "Initializing app components...")

        // Qui potresti aggiungere:
        // - Configurazione crash reporting
        // - Analytics
        // - Configurazioni globali
        // - Setup logging

        Log.d(TAG, "App initialization completed")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "ViBus Live Application terminated")
    }
}