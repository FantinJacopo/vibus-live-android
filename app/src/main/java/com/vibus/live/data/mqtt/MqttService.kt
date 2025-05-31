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
    }

    private var mqttClient: Mqtt3AsyncClient? = null
    private val gson = Gson()

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val _busPositions = MutableStateFlow<List<MqttBusPosition>>(emptyList())
    val busPositions: StateFlow<List<MqttBusPosition>> = _busPositions.asStateFlow()

    private val receivedPositions = mutableMapOf<String, MqttBusPosition>()

    fun connect() {
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
                } else {
                    Log.i(TAG, "Successfully connected to MQTT broker at ${NetworkConfig.CURRENT_MQTT_HOST}:${NetworkConfig.CURRENT_MQTT_PORT}")
                    _connectionState.value = MqttConnectionState.Connected
                    subscribeToTopics()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating MQTT client", e)
            _connectionState.value = MqttConnectionState.Error(e.message ?: "Client creation failed")
        }
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
            Log.v(TAG, "Payload: $payload")

            val busPosition = gson.fromJson(payload, MqttBusPosition::class.java)

            if (busPosition != null && busPosition.bus_id.isNotEmpty()) {
                receivedPositions[busPosition.bus_id] = busPosition
                _busPositions.value = receivedPositions.values.toList()

                Log.d(TAG, "Updated bus position: ${busPosition.bus_id} at (${busPosition.position.lat}, ${busPosition.position.lon})")
            } else {
                Log.w(TAG, "Invalid bus position data received from topic: $topic")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing MQTT message", e)
        }
    }

    fun disconnect() {
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
            _connectionState.value = MqttConnectionState.Disconnected
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
                "Positions: ${receivedPositions.size}"
    }
}