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

data class MqttBusPosition(
    val bus_id: String,
    val line: String,
    val line_name: String,
    val position: MqttPosition,
    val speed: Double,
    val bearing: Int,
    val delay: Double,
    val passengers: Int,
    val status: String,
    val timestamp: String
)

data class MqttPosition(
    val lat: Double,
    val lon: Double
)

sealed class MqttConnectionState {
    object Disconnected : MqttConnectionState()
    object Connecting : MqttConnectionState()
    object Connected : MqttConnectionState()
    data class Error(val message: String) : MqttConnectionState()
}

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

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val _busPositions = MutableStateFlow<List<MqttBusPosition>>(emptyList())
    val busPositions: StateFlow<List<MqttBusPosition>> = _busPositions.asStateFlow()

    private val receivedPositions = mutableMapOf<String, MqttBusPosition>()

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

                    // Riprova automaticamente se dovremmo rimanere connessi
                    if (shouldStayConnected) {
                        Log.d(TAG, "Scheduling reconnection in ${RECONNECT_DELAY_MS}ms")
                        scheduleReconnection()
                    }
                } else {
                    Log.i(TAG, "Successfully connected to MQTT broker")
                    _connectionState.value = MqttConnectionState.Connected
                    isInitialized = true
                    subscribeToTopics()
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
        // Usa un handler per ritentare la connessione
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (shouldStayConnected && !isConnected()) {
                Log.d(TAG, "Attempting automatic reconnection...")
                startConnection()
            }
        }, RECONNECT_DELAY_MS)
    }

    private fun subscribeToTopics() {
        mqttClient?.subscribeWith()
            ?.topicFilter(NetworkConfig.MQTT_TOPIC_BUS_POSITION)
            ?.callback { publish: Mqtt3Publish ->
                handleIncomingMessage(publish)
            }
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Failed to subscribe to topic: ${NetworkConfig.MQTT_TOPIC_BUS_POSITION}", throwable)
                } else {
                    Log.i(TAG, "Successfully subscribed to topic: ${NetworkConfig.MQTT_TOPIC_BUS_POSITION}")
                }
            }
    }

    private fun handleIncomingMessage(publish: Mqtt3Publish) {
        try {
            val topic = publish.topic.toString()
            val payload = String(publish.payloadAsBytes)

            Log.d(TAG, "Received MQTT message from topic: $topic")
            Log.v(TAG, "Raw payload: $payload")

            // Prova a parsare il JSON, gestendo doppia codifica
            val busPosition = try {
                // Prima prova il parsing diretto
                gson.fromJson(payload, MqttBusPosition::class.java)
            } catch (e: Exception) {
                Log.d(TAG, "Direct parsing failed, trying to decode JSON string...")
                try {
                    // Se fallisce, prova a decodificare la stringa JSON
                    val decodedPayload = gson.fromJson(payload, String::class.java)
                    Log.v(TAG, "Decoded payload: $decodedPayload")
                    gson.fromJson(decodedPayload, MqttBusPosition::class.java)
                } catch (e2: Exception) {
                    Log.e(TAG, "Both parsing methods failed", e2)
                    Log.e(TAG, "Original payload: $payload")
                    null
                }
            }

            if (busPosition != null && busPosition.bus_id.isNotEmpty()) {
                receivedPositions[busPosition.bus_id] = busPosition
                _busPositions.value = receivedPositions.values.toList()

                Log.d(TAG, "Successfully parsed bus: ${busPosition.bus_id} at (${busPosition.position.lat}, ${busPosition.position.lon})")
            } else {
                Log.w(TAG, "Invalid or null bus position data from topic: $topic")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing MQTT message from topic: ${publish.topic}", e)
            Log.e(TAG, "Payload causing error: ${String(publish.payloadAsBytes)}")
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
                receivedPositions.clear()
                _busPositions.value = emptyList()
                isInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
            _connectionState.value = MqttConnectionState.Disconnected
            isInitialized = false
        }
    }

    // Nuova funzione per disconnessione temporanea (non chiama shouldStayConnected = false)
    fun pause() {
        Log.d(TAG, "Pausing MQTT connection (will reconnect automatically)")
        // Non cambiamo shouldStayConnected, così si riconnette automaticamente
    }

    // Nuova funzione per riconnessione manuale
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

    fun getConnectionInfo(): String {
        return "Host: ${NetworkConfig.CURRENT_MQTT_HOST}:${NetworkConfig.CURRENT_MQTT_PORT}, " +
                "Topic: ${NetworkConfig.MQTT_TOPIC_BUS_POSITION}, " +
                "State: ${_connectionState.value}, " +
                "Positions: ${receivedPositions.size}, " +
                "ShouldStayConnected: $shouldStayConnected"
    }
}