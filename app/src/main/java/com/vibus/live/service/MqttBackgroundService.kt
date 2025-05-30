package com.vibus.live.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vibus.live.R
import com.vibus.live.data.mqtt.MqttConnectionState
import com.vibus.live.data.mqtt.MqttConnectionStats
import com.vibus.live.data.mqtt.MqttManager
import com.vibus.live.data.mqtt.MqttResult
import com.vibus.live.data.repository.MqttBusRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MqttBackgroundService : Service() {

    companion object {
        private const val TAG = "MqttBackgroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mqtt_service_channel"
        private const val ACTION_START = "com.vibus.live.START_MQTT_SERVICE"
        private const val ACTION_STOP = "com.vibus.live.STOP_MQTT_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, MqttBackgroundService::class.java).apply {
                action = ACTION_START
            }

            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MqttBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var mqttRepository: MqttBusRepository

    @Inject
    lateinit var mqttManager: MqttManager

    private var isServiceStarted = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var connectionStats = MqttConnectionStats(
        isConnected = false,
        connectionUptime = 0,
        messagesReceived = 0,
        messagesLost = 0,
        reconnectCount = 0,
        lastError = null,
        brokerHost = "Unknown",
        clientId = "Unknown"
    )

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MQTT Background Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startMqttService()
            }
            ACTION_STOP -> {
                stopMqttService()
            }
        }

        return START_STICKY // Riavvia automaticamente se ucciso dal sistema
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // Non supportiamo binding
    }

    override fun onDestroy() {
        Log.d(TAG, "MQTT Background Service destroyed")
        stopMqttService()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMqttService() {
        if (isServiceStarted) {
            Log.d(TAG, "Service already started")
            return
        }

        Log.d(TAG, "Starting MQTT foreground service")
        isServiceStarted = true

        // Avvia come foreground service
        startForeground(NOTIFICATION_ID, createNotification())

        // Inizializza MQTT
        serviceScope.launch {
            try {
                Log.d(TAG, "Initializing MQTT repository...")

                when (val result = mqttRepository.initialize()) {
                    is MqttResult.Success -> {
                        Log.d(TAG, "MQTT repository initialized successfully")
                        startMonitoring()
                    }
                    is MqttResult.Error -> {
                        Log.e(TAG, "Failed to initialize MQTT: ${result.error}")
                        updateNotification("MQTT Connection Failed")
                    }
                    else -> {
                        Log.d(TAG, "MQTT initialization in progress...")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MQTT service", e)
                updateNotification("Service Error")
            }
        }
    }

    private fun stopMqttService() {
        if (!isServiceStarted) {
            return
        }

        Log.d(TAG, "Stopping MQTT service")
        isServiceStarted = false

        // Disconnetti MQTT
        serviceScope.launch {
            try {
                mqttRepository.disconnect()
                mqttRepository.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MQTT", e)
            }
        }

        // Ferma foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startMonitoring() {
        // Monitor stato connessione MQTT
        mqttManager.connectionState
            .onEach { state ->
                Log.d(TAG, "MQTT connection state changed: $state")
                updateConnectionStats()
                updateNotificationForState(state)
            }
            .launchIn(serviceScope)

        // Monitor eventi di connessione
        mqttManager.connectionEvents
            .onEach { event ->
                Log.d(TAG, "MQTT connection event: ${event.state} - ${event.message}")
                if (event.error != null) {
                    Log.e(TAG, "MQTT connection error", event.error)
                }
            }
            .launchIn(serviceScope)

        // Monitor messaggi ricevuti (per debugging)
        mqttManager.messages
            .onEach { message ->
                Log.v(TAG, "MQTT message received: ${message.topic}")
                updateConnectionStats()
            }
            .launchIn(serviceScope)
    }

    private fun updateConnectionStats() {
        connectionStats = mqttRepository.getMqttStats()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ViBus MQTT Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantiene la connessione MQTT per dati autobus in tempo reale"
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, MqttBackgroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ViBus Live - Connesso")
            .setContentText("Ricevendo dati autobus in tempo reale")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Disconnetti",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ViBus Live")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationForState(state: MqttConnectionState) {
        val (title, text, icon) = when (state) {
            MqttConnectionState.CONNECTED -> Triple(
                "ViBus Live - Connesso",
                "📡 ${connectionStats.messagesReceived} messaggi ricevuti",
                R.drawable.ic_launcher_foreground
            )
            MqttConnectionState.CONNECTING -> Triple(
                "ViBus Live - Connessione...",
                "🔄 Connessione al server MQTT in corso",
                R.drawable.ic_launcher_foreground
            )
            MqttConnectionState.RECONNECTING -> Triple(
                "ViBus Live - Riconnessione...",
                "🔄 Tentativo ${connectionStats.reconnectCount + 1}",
                R.drawable.ic_launcher_foreground
            )
            MqttConnectionState.DISCONNECTED -> Triple(
                "ViBus Live - Disconnesso",
                "❌ Connessione MQTT terminata",
                R.drawable.ic_launcher_foreground
            )
            MqttConnectionState.ERROR -> Triple(
                "ViBus Live - Errore",
                "⚠️ ${connectionStats.lastError ?: "Errore di connessione"}",
                R.drawable.ic_launcher_foreground
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setOngoing(state == MqttConnectionState.CONNECTED || state == MqttConnectionState.CONNECTING)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}