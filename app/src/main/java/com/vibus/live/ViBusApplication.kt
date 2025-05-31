package com.vibus.live

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vibus.live.data.mqtt.MqttService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ViBusApplication : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var mqttService: MqttService

    companion object {
        private const val TAG = "ViBusApplication"
    }

    override fun onCreate() {
        super<Application>.onCreate()  // Specifica che vogliamo Application.onCreate()
        Log.d(TAG, "ViBus Application starting...")

        // Osserva il lifecycle dell'app per gestire MQTT
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // Lifecycle observer methods - questi sono per DefaultLifecycleObserver
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App in foreground - ensuring MQTT connection")

        // App in foreground - assicurati che MQTT sia connesso
        if (!mqttService.isConnected()) {
            mqttService.connect()
        } else {
            mqttService.resume()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "App in background - pausing MQTT (will auto-reconnect)")

        // App in background - pausa MQTT ma lascia che si riconnetta automaticamente
        mqttService.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "App terminating - disconnecting MQTT")

        // App che termina completamente - disconnetti MQTT
        mqttService.disconnect()
    }
}