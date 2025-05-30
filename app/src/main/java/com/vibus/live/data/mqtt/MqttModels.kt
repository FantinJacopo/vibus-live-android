package com.vibus.live.data.mqtt

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

// Stati di connessione MQTT
enum class MqttConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

// Evento di connessione MQTT
data class MqttConnectionEvent(
    val state: MqttConnectionState,
    val message: String? = null,
    val error: Throwable? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

// Messaggio MQTT ricevuto
data class MqttMessage(
    val topic: String,
    val payload: String,
    val qos: Int,
    val retained: Boolean,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

// Statistiche connessione MQTT
data class MqttConnectionStats(
    val isConnected: Boolean,
    val connectionUptime: Long,
    val messagesReceived: Long,
    val messagesLost: Long,
    val reconnectCount: Int,
    val lastError: String?,
    val brokerHost: String,
    val clientId: String
)

// Modelli JSON ricevuti via MQTT (corrispondono ai dati da Node-RED)

// Messaggio posizione autobus
data class MqttBusPositionMessage(
    @SerializedName("bus_id")
    val busId: String,

    @SerializedName("line")
    val line: String,

    @SerializedName("line_name")
    val lineName: String,

    @SerializedName("position")
    val position: MqttPosition,

    @SerializedName("speed")
    val speed: Double,

    @SerializedName("bearing")
    val bearing: Int,

    @SerializedName("delay")
    val delay: Double,

    @SerializedName("passengers")
    val passengers: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("timestamp")
    val timestamp: String
)

data class MqttPosition(
    @SerializedName("lat")
    val latitude: Double,

    @SerializedName("lon")
    val longitude: Double
)

// Messaggio statistiche linea
data class MqttLineStatsMessage(
    @SerializedName("line")
    val line: String,

    @SerializedName("day_type")
    val dayType: String,

    @SerializedName("avg_delay")
    val averageDelay: Double,

    @SerializedName("max_delay")
    val maxDelay: Double,

    @SerializedName("on_time_percentage")
    val onTimePercentage: Double,

    @SerializedName("active_buses")
    val activeBuses: Int,

    @SerializedName("total_passengers")
    val totalPassengers: Int?,

    @SerializedName("avg_speed")
    val averageSpeed: Double?,

    @SerializedName("timestamp")
    val timestamp: String
)

// Messaggio stato sistema
data class MqttSystemStatusMessage(
    @SerializedName("component")
    val component: String,

    @SerializedName("total_buses")
    val totalBuses: Int,

    @SerializedName("active_buses")
    val activeBuses: Int,

    @SerializedName("total_passengers")
    val totalPassengers: Int,

    @SerializedName("avg_system_delay")
    val averageSystemDelay: Double,

    @SerializedName("system_health")
    val systemHealth: String,

    @SerializedName("timestamp")
    val timestamp: String
)

// Errori MQTT
sealed class MqttError : Exception() {
    data class ConnectionFailed(val reason: String) : MqttError()
    data class SubscriptionFailed(val topic: String, val reason: String) : MqttError()
    data class MessageParsingFailed(val topic: String, val payload: String, val reason: String) : MqttError()
    data class BrokerUnreachable(val brokerUrl: String) : MqttError()
    data class AuthenticationFailed(val brokerUrl: String) : MqttError()
    data class UnknownError(val reason: String) : MqttError()
}

// Configurazione subscriptions
data class MqttSubscription(
    val topic: String,
    val qos: Int,
    val isActive: Boolean = false
)

// Risultato operazione MQTT
sealed class MqttResult<T> {
    data class Success<T>(val data: T) : MqttResult<T>()
    data class Error<T>(val error: MqttError) : MqttResult<T>()
    data class Loading<T>(val message: String) : MqttResult<T>()
}