package com.vibus.live.data.mqtt

// Data classes esistenti per posizioni autobus
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

// NUOVE data classes per statistiche
data class MqttLineStats(
    val line: String,
    val line_name: String,
    val active_buses: Int,
    val average_speed: Double,
    val average_delay: Double,
    val max_delay: Double,
    val on_time_percentage: Double,
    val total_passengers: Int,
    val day_type: String,
    val timestamp: String
)

data class MqttSystemStatus(
    val total_buses: Int,
    val active_buses: Int,
    val total_passengers: Int,
    val average_system_delay: Double,
    val average_system_speed: Double,
    val system_health: String,
    val system_on_time_percentage: Double? = null,
    val line_distribution: Map<String, Int>,
    val uptime_percentage: Double,
    val timestamp: String
)

// Stati di connessione MQTT
sealed class MqttConnectionState {
    object Disconnected : MqttConnectionState()
    object Connecting : MqttConnectionState()
    object Connected : MqttConnectionState()
    data class Error(val message: String) : MqttConnectionState()
}