package com.vibus.live.data.mqtt

import android.util.Log
import com.google.gson.Gson
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.vibus.live.data.api.NetworkConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttService @Inject constructor() {

    companion object {
        private const val TAG = "MqttService"
        private const val CLIENT_ID_PREFIX = "vibus_android_"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private var mqttClient: Mqtt3AsyncClient? = null
    private val gson = Gson()
    private var isInitialized = false
    private var shouldStayConnected = true

    // Stati di connessione
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    // Dati autobus (esistente)
    private val _busPositions = MutableStateFlow<List<MqttBusPosition>>(emptyList())
    val busPositions: StateFlow<List<MqttBusPosition>> = _busPositions.asStateFlow()

    // NUOVO: Statistiche linee
    private val _lineStats = MutableStateFlow<List<MqttLineStats>>(emptyList())
    val lineStats: StateFlow<List<MqttLineStats>> = _lineStats.asStateFlow()

    // NUOVO: Stato sistema
    private val _systemStatus = MutableStateFlow<MqttSystemStatus?>(null)
    val systemStatus: StateFlow<MqttSystemStatus?> = _systemStatus.asStateFlow()

    // Storage interno
    private val receivedPositions = mutableMapOf<String, MqttBusPosition>()
    private val receivedLineStats = mutableMapOf<String, MqttLineStats>()

    fun connect() {
        if (isInitialized && isConnected()) {
            Log.d(TAG, "MQTT already connected, skipping connection attempt")
            return
        }

        shouldStayConnected = true
        startConnection()
    }

    private fun startConnection() {
        try {
            _connectionState.value = MqttConnectionState.Connecting
            Log.d(TAG, "Connecting to MQTT broker at ${NetworkConfig.CURRENT_MQTT_HOST}:${NetworkConfig.CURRENT_MQTT_PORT}")

            val clientId = CLIENT_ID_PREFIX + System.currentTimeMillis()

            mqttClient = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(NetworkConfig.CURRENT_MQTT_HOST)
                .serverPort(NetworkConfig.CURRENT_MQTT_PORT)
                .automaticReconnect()
                .initialDelay(2, TimeUnit.SECONDS)
                .maxDelay(30, TimeUnit.SECONDS)
                .applyAutomaticReconnect()
                .buildAsync()

            mqttClient?.connect()?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Failed to connect to MQTT broker", throwable)
                    _connectionState.value = MqttConnectionState.Error("Connection failed: ${throwable.message}")

                    if (shouldStayConnected) {
                        Log.d(TAG, "Scheduling reconnection in ${RECONNECT_DELAY_MS}ms")
                        scheduleReconnection()
                    }
                } else {
                    Log.i(TAG, "Successfully connected to MQTT broker")
                    _connectionState.value = MqttConnectionState.Connected
                    isInitialized = true
                    subscribeToAllTopics()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating MQTT client", e)
            _connectionState.value = MqttConnectionState.Error(e.message ?: "Client creation failed")

            if (shouldStayConnected) {
                scheduleReconnection()
            }
        }
    }

    private fun scheduleReconnection() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (shouldStayConnected && !isConnected()) {
                Log.d(TAG, "Attempting automatic reconnection...")
                startConnection()
            }
        }, RECONNECT_DELAY_MS)
    }

    // AGGIORNATO: Subscribe a tutti i topic
    private fun subscribeToAllTopics() {
        // 1. Posizioni autobus (esistente)
        subscribeToTopic(
            NetworkConfig.MQTT_TOPIC_BUS_POSITION,
            "bus positions"
        ) { publish -> handleBusPositionMessage(publish) }

        // 2. NUOVO: Statistiche linee
        subscribeToTopic(
            NetworkConfig.MQTT_TOPIC_LINE_STATS,
            "line statistics"
        ) { publish -> handleLineStatsMessage(publish) }

        // 3. NUOVO: Stato sistema
        subscribeToTopic(
            NetworkConfig.MQTT_TOPIC_SYSTEM_STATUS,
            "system status"
        ) { publish -> handleSystemStatusMessage(publish) }
    }

    private fun subscribeToTopic(
        topic: String,
        description: String,
        callback: (Mqtt3Publish) -> Unit
    ) {
        mqttClient?.subscribeWith()
            ?.topicFilter(topic)
            ?.callback(callback)
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Failed to subscribe to $description: $topic", throwable)
                } else {
                    Log.i(TAG, "✅ Successfully subscribed to $description: $topic")
                }
            }
    }

    // Handler per posizioni autobus (esistente, rinominato per chiarezza)
    private fun handleBusPositionMessage(publish: Mqtt3Publish) {
        try {
            val topic = publish.topic.toString()
            val payload = String(publish.payloadAsBytes)

            Log.d(TAG, "Received bus position from topic: $topic")

            val busPosition = parseJsonPayload<MqttBusPosition>(payload)

            if (busPosition != null && busPosition.bus_id.isNotEmpty()) {
                receivedPositions[busPosition.bus_id] = busPosition
                _busPositions.value = receivedPositions.values.toList()

                Log.d(TAG, "✅ Updated bus position: ${busPosition.bus_id} at (${busPosition.position.lat}, ${busPosition.position.lon})")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing bus position message", e)
        }
    }

    // NUOVO: Handler per statistiche linee
    private fun handleLineStatsMessage(publish: Mqtt3Publish) {
        try {
            val topic = publish.topic.toString()
            val payload = String(publish.payloadAsBytes)

            Log.d(TAG, "Received line stats from topic: $topic")

            val lineStats = parseJsonPayload<MqttLineStats>(payload)

            if (lineStats != null && lineStats.line.isNotEmpty()) {
                receivedLineStats[lineStats.line] = lineStats
                _lineStats.value = receivedLineStats.values.toList()

                Log.d(TAG, "✅ Updated line stats for line ${lineStats.line}: ${lineStats.active_buses} buses, avg delay ${lineStats.average_delay}min, on-time ${lineStats.on_time_percentage}%")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing line stats message", e)
        }
    }

    // NUOVO: Handler per stato sistema
    private fun handleSystemStatusMessage(publish: Mqtt3Publish) {
        try {
            val payload = String(publish.payloadAsBytes)

            Log.d(TAG, "Received system status")

            val systemStatus = parseJsonPayload<MqttSystemStatus>(payload)

            if (systemStatus != null) {
                _systemStatus.value = systemStatus
                Log.d(TAG, "✅ Updated system status: ${systemStatus.active_buses}/${systemStatus.total_buses} buses, health: ${systemStatus.system_health}, avg delay: ${systemStatus.average_system_delay}min")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing system status message", e)
        }
    }

    // Helper per parsing JSON con gestione doppia codifica
    private inline fun <reified T> parseJsonPayload(payload: String): T? {
        return try {
            // Prima prova il parsing diretto
            gson.fromJson(payload, T::class.java)
        } catch (e: Exception) {
            Log.d(TAG, "Direct parsing failed, trying to decode JSON string...")
            try {
                // Se fallisce, prova a decodificare la stringa JSON
                val decodedPayload = gson.fromJson(payload, String::class.java)
                gson.fromJson(decodedPayload, T::class.java)
            } catch (e2: Exception) {
                Log.e(TAG, "Both parsing methods failed for ${T::class.java.simpleName}", e2)
                null
            }
        }
    }

    fun disconnect() {
        shouldStayConnected = false
        performDisconnect()
    }

    private fun performDisconnect() {
        try {
            mqttClient?.disconnect()?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Error disconnecting from MQTT broker", throwable)
                } else {
                    Log.i(TAG, "Successfully disconnected from MQTT broker")
                }
                _connectionState.value = MqttConnectionState.Disconnected
                clearAllData()
                isInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
            _connectionState.value = MqttConnectionState.Disconnected
            isInitialized = false
        }
    }

    private fun clearAllData() {
        receivedPositions.clear()
        receivedLineStats.clear()
        _busPositions.value = emptyList()
        _lineStats.value = emptyList()
        _systemStatus.value = null
    }

    fun pause() {
        Log.d(TAG, "Pausing MQTT connection (will reconnect automatically)")
    }

    fun resume() {
        Log.d(TAG, "Resuming MQTT connection")
        if (!isConnected() && shouldStayConnected) {
            startConnection()
        }
    }

    fun isConnected(): Boolean {
        return _connectionState.value is MqttConnectionState.Connected &&
                mqttClient?.state?.isConnected == true
    }

    fun getCurrentPositions(): List<MqttBusPosition> {
        return _busPositions.value
    }

    fun getCurrentLineStats(): List<MqttLineStats> {
        return _lineStats.value
    }

    fun getCurrentSystemStatus(): MqttSystemStatus? {
        return _systemStatus.value
    }

    fun getConnectionInfo(): String {
        return "Host: ${NetworkConfig.CURRENT_MQTT_HOST}:${NetworkConfig.CURRENT_MQTT_PORT}, " +
                "State: ${_connectionState.value}, " +
                "Buses: ${receivedPositions.size}, " +
                "LineStats: ${receivedLineStats.size}, " +
                "SystemStatus: ${if (_systemStatus.value != null) "✓" else "✗"}, " +
                "ShouldStayConnected: $shouldStayConnected"
    }
}