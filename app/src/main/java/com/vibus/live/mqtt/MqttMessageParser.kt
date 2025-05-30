package com.vibus.live.mqtt

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.vibus.live.data.*
import com.vibus.live.data.mqtt.MqttBusPositionMessage
import com.vibus.live.data.mqtt.MqttError
import com.vibus.live.data.mqtt.MqttLineStatsMessage
import com.vibus.live.data.mqtt.MqttMessage
import com.vibus.live.data.mqtt.MqttResult
import com.vibus.live.data.mqtt.MqttSystemStatusMessage
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttMessageParser @Inject constructor() {

    companion object {
        private const val TAG = "MqttMessageParser"
    }

    private val gson = Gson()

    /**
     * Parse messaggio generico MQTT
     */
    fun parseMessage(message: MqttMessage): MqttResult<ParsedMqttData> {
        return try {
            Log.d(TAG, "Parsing message from topic: ${message.topic}")
            Log.d(TAG, "Payload: ${message.payload.take(200)}...")

            when {
                message.topic.matches(Regex("vibus/autobus/.+/posizione")) -> {
                    parseBusPosition(message)
                }
                message.topic.matches(Regex("vibus/linea/.+/statistiche")) -> {
                    parseLineStats(message)
                }
                message.topic.matches(Regex("vibus/sistema/.+/stato")) -> {
                    parseSystemStatus(message)
                }
                else -> {
                    Log.w(TAG, "Unknown topic pattern: ${message.topic}")
                    MqttResult.Error(
                        MqttError.MessageParsingFailed(
                        message.topic,
                        message.payload,
                        "Unknown topic pattern"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message from ${message.topic}", e)
            MqttResult.Error(
                MqttError.MessageParsingFailed(
                message.topic,
                message.payload,
                e.message ?: "Unknown parsing error"
            ))
        }
    }

    /**
     * Parse messaggio posizione autobus
     */
    private fun parseBusPosition(message: MqttMessage): MqttResult<ParsedMqttData> {
        return try {
            val mqttData = gson.fromJson(message.payload, MqttBusPositionMessage::class.java)

            // Validazione dati
            if (mqttData.busId.isBlank() || mqttData.line.isBlank()) {
                return MqttResult.Error(
                    MqttError.MessageParsingFailed(
                    message.topic,
                    message.payload,
                    "Missing required fields: busId or line"
                ))
            }

            // Conversione a modello app
            val bus = Bus(
                id = mqttData.busId,
                line = mqttData.line,
                lineName = mqttData.lineName,
                position = Position(
                    latitude = mqttData.position.latitude,
                    longitude = mqttData.position.longitude
                ),
                speed = mqttData.speed,
                bearing = mqttData.bearing,
                delay = mqttData.delay,
                passengers = mqttData.passengers,
                status = parseBusStatus(mqttData.status),
                lastUpdate = parseTimestamp(mqttData.timestamp)
            )

            Log.d(TAG, "Parsed bus position: ${bus.id} at ${bus.position.latitude}, ${bus.position.longitude}")

            MqttResult.Success(ParsedMqttData.BusPosition(bus))

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parsing error for bus position", e)
            MqttResult.Error(
                MqttError.MessageParsingFailed(
                message.topic,
                message.payload,
                "Invalid JSON format: ${e.message}"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing bus position", e)
            MqttResult.Error(
                MqttError.MessageParsingFailed(
                message.topic,
                message.payload,
                "Unexpected error: ${e.message}"
            ))
        }
    }

    /**
     * Parse messaggio statistiche linea
     */
    private fun parseLineStats(message: MqttMessage): MqttResult<ParsedMqttData> {
        return try {
            val mqttData = gson.fromJson(message.payload, MqttLineStatsMessage::class.java)

            // Validazione
            if (mqttData.line.isBlank()) {
                return MqttResult.Error(
                    MqttError.MessageParsingFailed(
                    message.topic,
                    message.payload,
                    "Missing required field: line"
                ))
            }

            // Conversione a modello app
            val lineStats = LineStats(
                line = mqttData.line,
                activeBuses = mqttData.activeBuses,
                averageSpeed = mqttData.averageSpeed ?: 25.0,
                averageDelay = mqttData.averageDelay,
                maxDelay = mqttData.maxDelay,
                onTimePercentage = mqttData.onTimePercentage,
                totalPassengers = mqttData.totalPassengers ?: 0,
                lastUpdate = parseTimestamp(mqttData.timestamp)
            )

            Log.d(TAG, "Parsed line stats: Line ${lineStats.line}, ${lineStats.activeBuses} buses")

            MqttResult.Success(ParsedMqttData.LineStatistics(lineStats))

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parsing error for line stats", e)
            MqttResult.Error(
                MqttError.MessageParsingFailed(
                message.topic,
                message.payload,
                "Invalid JSON format: ${e.message}"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing line stats", e)
            MqttResult.Error(
                MqttError.MessageParsingFailed(
                message.topic,
                message.payload,
                "Unexpected error: ${e.message}"
            ))
        }
    }

    /**
     * Parse messaggio stato sistema
     */
    private fun parseSystemStatus(message: MqttMessage): MqttResult<ParsedMqttData> {
        return try {
            val mqttData = gson.fromJson(message.payload, MqttSystemStatusMessage::class.java)

            // Conversione a modello app
            val systemStatus = SystemStatus(
                totalBuses = mqttData.totalBuses,
                activeBuses = mqttData.activeBuses,
                totalPassengers = mqttData.totalPassengers,
                averageSystemDelay = mqttData.averageSystemDelay,
                systemHealth = parseSystemHealth(mqttData.systemHealth),
                lastUpdate = parseTimestamp(mqttData.timestamp)
            )

            Log.d(TAG, "Parsed system status: ${systemStatus.activeBuses}/${systemStatus.totalBuses} buses active")

            MqttResult.Success(ParsedMqttData.SystemStatus(systemStatus))

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parsing error for system status", e)
            MqttResult.Error(
                MqttError.MessageParsingFailed(
                message.topic,
                message.payload,
                "Invalid JSON format: ${e.message}"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing system status", e)
            MqttResult.Error(
                MqttError.MessageParsingFailed(
                message.topic,
                message.payload,
                "Unexpected error: ${e.message}"
            ))
        }
    }

    /**
     * Parse timestamp ISO
     */
    private fun parseTimestamp(timestamp: String): LocalDateTime {
        return try {
            // Prova diversi formati di timestamp
            when {
                timestamp.contains('T') -> {
                    // ISO format: 2025-01-20T15:30:45.123Z
                    LocalDateTime.parse(timestamp.removeSuffix("Z"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
                timestamp.contains(' ') -> {
                    // Format: 2025-01-20 15:30:45
                    LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                }
                else -> {
                    // Default to now if parsing fails
                    Log.w(TAG, "Unable to parse timestamp: $timestamp, using current time")
                    LocalDateTime.now()
                }
            }
        } catch (e: DateTimeParseException) {
            Log.w(TAG, "Timestamp parsing failed for: $timestamp, using current time", e)
            LocalDateTime.now()
        }
    }

    /**
     * Parse status autobus
     */
    private fun parseBusStatus(status: String): BusStatus {
        return when (status.lowercase().trim()) {
            "in_service", "active", "running" -> BusStatus.IN_SERVICE
            "out_of_service", "inactive", "stopped" -> BusStatus.OUT_OF_SERVICE
            "maintenance", "repair" -> BusStatus.MAINTENANCE
            "delayed", "late" -> BusStatus.DELAYED
            else -> {
                Log.w(TAG, "Unknown bus status: $status, defaulting to IN_SERVICE")
                BusStatus.IN_SERVICE
            }
        }
    }

    /**
     * Parse system health
     */
    private fun parseSystemHealth(health: String): SystemHealth {
        return when (health.lowercase().trim()) {
            "excellent", "perfect" -> SystemHealth.EXCELLENT
            "good", "ok" -> SystemHealth.GOOD
            "fair", "average" -> SystemHealth.FAIR
            "poor", "bad" -> SystemHealth.POOR
            "critical", "emergency" -> SystemHealth.CRITICAL
            else -> {
                Log.w(TAG, "Unknown system health: $health, defaulting to GOOD")
                SystemHealth.GOOD
            }
        }
    }

    /**
     * Estrai ID autobus dal topic
     */
    fun extractBusIdFromTopic(topic: String): String? {
        val regex = Regex("vibus/autobus/(.+)/posizione")
        return regex.find(topic)?.groupValues?.get(1)
    }

    /**
     * Estrai ID linea dal topic
     */
    fun extractLineIdFromTopic(topic: String): String? {
        val regex = Regex("vibus/linea/(.+)/statistiche")
        return regex.find(topic)?.groupValues?.get(1)
    }

    /**
     * Estrai componente sistema dal topic
     */
    fun extractSystemComponentFromTopic(topic: String): String? {
        val regex = Regex("vibus/sistema/(.+)/stato")
        return regex.find(topic)?.groupValues?.get(1)
    }

    /**
     * Valida dati posizione
     */
    private fun isValidPosition(lat: Double, lon: Double): Boolean {
        // Vicenza bounds approssimativi
        return lat in 45.0..46.0 && lon in 11.0..12.0
    }

    /**
     * Valida velocità autobus
     */
    private fun isValidSpeed(speed: Double): Boolean {
        return speed in 0.0..100.0 // km/h ragionevoli per autobus urbani
    }

    /**
     * Valida numero passeggeri
     */
    private fun isValidPassengerCount(passengers: Int): Boolean {
        return passengers in 0..80 // Capacità tipica autobus urbano
    }
}

/**
 * Dati parsati da messaggi MQTT
 */
sealed class ParsedMqttData {
    data class BusPosition(val bus: Bus) : ParsedMqttData()
    data class LineStatistics(val lineStats: LineStats) : ParsedMqttData()
    data class SystemStatus(val systemStatus: com.vibus.live.data.SystemStatus) : ParsedMqttData()
}

/**
 * Cache per messaggi processati (evita duplicati)
 */
data class MessageCache(
    val busPositions: MutableMap<String, Bus> = mutableMapOf(),
    val lineStats: MutableMap<String, LineStats> = mutableMapOf(),
    var systemStatus: SystemStatus? = null,
    val lastUpdate: MutableMap<String, LocalDateTime> = mutableMapOf()
) {

    fun shouldProcessMessage(topic: String, timestamp: LocalDateTime): Boolean {
        val lastProcessed = lastUpdate[topic]
        return lastProcessed == null || timestamp.isAfter(lastProcessed)
    }

    fun markProcessed(topic: String, timestamp: LocalDateTime) {
        lastUpdate[topic] = timestamp
    }

    fun getBusPositions(): List<Bus> = busPositions.values.toList()

    fun getLineStatistics(): List<LineStats> = lineStats.values.toList()

    fun updateBusPosition(bus: Bus) {
        busPositions[bus.id] = bus
        markProcessed("bus_${bus.id}", bus.lastUpdate)
    }

    fun updateLineStats(stats: LineStats) {
        lineStats[stats.line] = stats
        markProcessed("line_${stats.line}", stats.lastUpdate)
    }

    fun updateSystemStatus(status: SystemStatus) {
        systemStatus = status
        markProcessed("system", status.lastUpdate)
    }

    fun cleanup(maxAge: Duration) {
        val cutoff = LocalDateTime.now().minus(maxAge)

        // Rimuovi dati vecchi
        busPositions.entries.removeAll { it.value.lastUpdate.isBefore(cutoff) }
        lineStats.entries.removeAll { it.value.lastUpdate.isBefore(cutoff) }
        lastUpdate.entries.removeAll { it.value.isBefore(cutoff) }

        // Reset system status se troppo vecchio
        systemStatus?.let { status ->
            if (status.lastUpdate.isBefore(cutoff)) {
                systemStatus = null
            }
        }
    }
}