package com.vibus.live.data.repository

import android.util.Log
import com.vibus.live.data.Bus
import com.vibus.live.data.BusStatus
import com.vibus.live.data.LineStats
import com.vibus.live.data.Position
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.SystemHealth
import com.vibus.live.data.api.NetworkConfig.STATS_UPDATE_INTERVAL_MS
import com.vibus.live.data.mqtt.MqttBusPosition
import com.vibus.live.data.mqtt.MqttConnectionState
import com.vibus.live.data.mqtt.MqttLineStats
import com.vibus.live.data.mqtt.MqttSystemStatus
import com.vibus.live.data.mqtt.MqttService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MqttOnlyBusRepository @Inject constructor(
    private val mqttService: MqttService
) : BusRepository {

    companion object {
        private const val TAG = "MqttOnlyRepository"
    }

    // ===== POSIZIONI AUTOBUS (esistente) =====
    override fun getRealTimeBuses(): Flow<List<Bus>> {
        Log.d(TAG, "Starting MQTT-only data stream for bus positions")

        if (!mqttService.isConnected()) {
            Log.d(TAG, "Connecting to MQTT...")
            mqttService.connect()
        }

        return mqttService.busPositions.map { mqttPositions ->
            if (mqttPositions.isNotEmpty()) {
                Log.d(TAG, "MQTT bus data received: ${mqttPositions.size} buses")
                mqttPositions.map { convertMqttToBus(it) }
            } else {
                Log.d(TAG, "No MQTT bus data yet - waiting for messages...")
                emptyList()
            }
        }
    }

    // ===== STATISTICHE LINEE (AGGIORNATO - USA MQTT REALE) =====
    override suspend fun getLineStats(): List<LineStats> {
        return try {
            Log.d(TAG, "Getting line statistics from MQTT...")

            // Ottieni le statistiche correnti da MQTT
            val mqttStats = mqttService.getCurrentLineStats()

            if (mqttStats.isNotEmpty()) {
                Log.d(TAG, "✅ Using real MQTT line stats: ${mqttStats.size} lines")

                mqttStats.map { convertMqttToLineStats(it) }.also { stats ->
                    stats.forEach { stat ->
                        Log.d(TAG, "Line ${stat.line}: ${stat.activeBuses} buses, avg delay ${stat.averageDelay}min, on-time ${stat.onTimePercentage}%")
                    }
                }
            } else {
                Log.w(TAG, "No MQTT line stats available yet, using fallback")
                generateFallbackLineStats()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MQTT line stats", e)
            generateFallbackLineStats()
        }
    }

    // ===== STATO SISTEMA (AGGIORNATO - USA MQTT REALE) =====
    override suspend fun getSystemStatus(): SystemStatus {
        return try {
            Log.d(TAG, "Getting system status from MQTT...")

            val mqttSystemStatus = mqttService.getCurrentSystemStatus()

            if (mqttSystemStatus != null) {
                Log.d(TAG, "✅ Using real MQTT system status")
                convertMqttToSystemStatus(mqttSystemStatus).also { status ->
                    Log.d(TAG, "System: ${status.activeBuses}/${status.totalBuses} buses, health: ${status.systemHealth}, avg delay: ${status.averageSystemDelay}min")
                }
            } else {
                Log.w(TAG, "No MQTT system status available yet, using fallback")
                generateFallbackSystemStatus()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MQTT system status", e)
            generateFallbackSystemStatus()
        }
    }

    // ===== CONVERSIONI DA MQTT A MODELLI APP =====

    private fun convertMqttToBus(mqttBus: MqttBusPosition): Bus {
        return Bus(
            id = mqttBus.bus_id,
            line = mqttBus.line,
            lineName = mqttBus.line_name,
            position = Position(
                latitude = mqttBus.position.lat,
                longitude = mqttBus.position.lon
            ),
            speed = mqttBus.speed,
            bearing = mqttBus.bearing,
            delay = mqttBus.delay,
            passengers = mqttBus.passengers,
            status = parseBusStatus(mqttBus.status),
            lastUpdate = parseTimestamp(mqttBus.timestamp)
        )
    }

    private fun convertMqttToLineStats(mqttStats: MqttLineStats): LineStats {
        return LineStats(
            line = mqttStats.line,
            activeBuses = mqttStats.active_buses,
            averageSpeed = mqttStats.average_speed,
            averageDelay = mqttStats.average_delay,
            maxDelay = mqttStats.max_delay,
            onTimePercentage = mqttStats.on_time_percentage,
            totalPassengers = mqttStats.total_passengers,
            lastUpdate = parseTimestamp(mqttStats.timestamp)
        )
    }

    private fun convertMqttToSystemStatus(mqttStatus: MqttSystemStatus): SystemStatus {
        return SystemStatus(
            totalBuses = mqttStatus.total_buses,
            activeBuses = mqttStatus.active_buses,
            totalPassengers = mqttStatus.total_passengers,
            averageSystemDelay = mqttStatus.average_system_delay,
            systemHealth = parseSystemHealth(mqttStatus.system_health),
            lastUpdate = parseTimestamp(mqttStatus.timestamp)
        )
    }

    // ===== HELPER FUNCTIONS =====

    private fun parseBusStatus(status: String): BusStatus {
        return when (status.lowercase()) {
            "in_service" -> BusStatus.IN_SERVICE
            "out_of_service" -> BusStatus.OUT_OF_SERVICE
            "maintenance" -> BusStatus.MAINTENANCE
            "delayed" -> BusStatus.DELAYED
            else -> BusStatus.IN_SERVICE
        }
    }

    private fun parseSystemHealth(health: String): SystemHealth {
        return when (health.uppercase()) {
            "EXCELLENT" -> SystemHealth.EXCELLENT
            "GOOD" -> SystemHealth.GOOD
            "FAIR" -> SystemHealth.FAIR
            "POOR" -> SystemHealth.POOR
            "CRITICAL" -> SystemHealth.CRITICAL
            else -> SystemHealth.FAIR
        }
    }

    private fun parseTimestamp(timestamp: String): LocalDateTime {
        return try {
            LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $timestamp", e)
            LocalDateTime.now()
        }
    }

    // ===== MQTT CONNECTION STATE =====

    fun getMqttConnectionState(): Flow<MqttConnectionState> {
        return mqttService.connectionState
    }

    fun disconnectMqtt() {
        Log.d(TAG, "Repository disconnect called but MQTT connection preserved")
    }

    // ===== FALLBACK FUNCTIONS (usate solo se MQTT non disponibile) =====

    private fun generateFallbackLineStats(): List<LineStats> {
        Log.d(TAG, "Generating fallback line stats (MQTT not available)")
        return listOf("1", "2", "3", "5", "7").map { line ->
            LineStats(
                line = line,
                activeBuses = Random.nextInt(1, 4),
                averageSpeed = 25.0 + Random.nextDouble() * 10.0,
                averageDelay = (Random.nextDouble() - 0.3) * 3.0,
                maxDelay = Random.nextDouble() * 8.0,
                onTimePercentage = 75.0 + Random.nextDouble() * 20.0,
                totalPassengers = Random.nextInt(50, 200),
                lastUpdate = LocalDateTime.now()
            )
        }
    }

    private fun generateFallbackSystemStatus(): SystemStatus {
        Log.d(TAG, "Generating fallback system status (MQTT not available)")
        val currentBusCount = mqttService.getCurrentPositions().size
        return SystemStatus(
            totalBuses = 10,
            activeBuses = if (currentBusCount > 0) currentBusCount else 8,
            totalPassengers = Random.nextInt(200, 400),
            averageSystemDelay = (Random.nextDouble() - 0.2) * 2.0,
            systemHealth = if (currentBusCount > 0) {
                SystemHealth.GOOD
            } else {
                SystemHealth.FAIR
            },
            lastUpdate = LocalDateTime.now()
        )
    }

    // ===== DEBUG FUNCTIONS =====

// Aggiungi questi metodi alla classe BusRepositoryImpl per compatibilità con la nuova interface

    // ===== NUOVI METODI FLOW REAL-TIME =====
    override fun getRealTimeLineStats(): Flow<List<LineStats>> = flow {
        while (true) {
            try {
                val stats = getLineStats()
                emit(stats)
                delay(30_000L) // Aggiorna ogni 30 secondi
            } catch (e: Exception) {
                Log.e(TAG, "Error in real-time line stats flow", e)
                emit(generateFallbackLineStats())
                delay(STATS_UPDATE_INTERVAL_MS)
            }
        }
    }

    override fun getRealTimeSystemStatus(): Flow<SystemStatus?> = flow {
        while (true) {
            try {
                val status = getSystemStatus()
                emit(status)
                delay(30_000L) // Aggiorna ogni 30 secondi
            } catch (e: Exception) {
                Log.e(TAG, "Error in real-time system status flow", e)
                emit(generateFallbackSystemStatus())
                delay(STATS_UPDATE_INTERVAL_MS)
            }
        }
    }
}