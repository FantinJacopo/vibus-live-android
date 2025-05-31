package com.vibus.live.data.repository

import android.util.Log
import com.vibus.live.data.Bus
import com.vibus.live.data.BusStatus
import com.vibus.live.data.LineStats
import com.vibus.live.data.Position
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.mqtt.MqttBusPosition
import com.vibus.live.data.mqtt.MqttConnectionState
import com.vibus.live.data.mqtt.MqttService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
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

    override fun getRealTimeBuses(): Flow<List<Bus>> {
        Log.d(TAG, "Starting MQTT-only data stream")

        // Connetti MQTT all'avvio
        if (!mqttService.isConnected()) {
            Log.d(TAG, "Connecting to MQTT...")
            mqttService.connect()
        }

        // Stream solo MQTT - nessun fallback HTTP
        return mqttService.busPositions.map { mqttPositions ->
            if (mqttPositions.isNotEmpty()) {
                Log.d(TAG, "MQTT data received: ${mqttPositions.size} buses")
                mqttPositions.map { convertMqttToBus(it) }
            } else {
                Log.d(TAG, "No MQTT data yet - waiting for messages...")
                emptyList()
            }
        }
    }

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

    private fun parseBusStatus(status: String): BusStatus {
        return when (status.lowercase()) {
            "in_service" -> BusStatus.IN_SERVICE
            "out_of_service" -> BusStatus.OUT_OF_SERVICE
            "maintenance" -> BusStatus.MAINTENANCE
            "delayed" -> BusStatus.DELAYED
            else -> BusStatus.IN_SERVICE
        }
    }

    private fun parseTimestamp(timestamp: String): LocalDateTime {
        return try {
            LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }

    override suspend fun getLineStats(): List<LineStats> {
        Log.d(TAG, "Generating mock line stats (no HTTP)")
        return generateMockLineStats()
    }

    override suspend fun getSystemStatus(): SystemStatus {
        Log.d(TAG, "Generating mock system status (no HTTP)")
        return generateMockSystemStatus()
    }

    fun getMqttConnectionState(): Flow<MqttConnectionState> {
        return mqttService.connectionState
    }

    fun disconnectMqtt() {
        mqttService.disconnect()
    }

    private fun generateMockLineStats(): List<LineStats> {
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

    private fun generateMockSystemStatus(): SystemStatus {
        val currentBusCount = mqttService.getCurrentPositions().size
        return SystemStatus(
            totalBuses = 10,
            activeBuses = if (currentBusCount > 0) currentBusCount else 8,
            totalPassengers = Random.nextInt(200, 400),
            averageSystemDelay = (Random.nextDouble() - 0.2) * 2.0,
            systemHealth = if (currentBusCount > 0) {
                com.vibus.live.data.SystemHealth.GOOD
            } else {
                com.vibus.live.data.SystemHealth.FAIR
            },
            lastUpdate = LocalDateTime.now()
        )
    }
}