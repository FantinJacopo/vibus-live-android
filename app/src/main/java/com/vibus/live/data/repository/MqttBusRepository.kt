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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
/*
@Singleton
class MqttBusRepository @Inject constructor(
    private val mqttService: MqttService,
    private val httpRepository: BusRepositoryImpl
) : BusRepository {

    companion object {
        private const val TAG = "MqttBusRepository"
        private const val MQTT_RETRY_DELAY = 5000L
        private const val FALLBACK_TIMEOUT = 15000L
    }

    override fun getRealTimeBuses(): Flow<List<Bus>> = flow {
        connectMqttIfNeeded()

        // SOLO MQTT - HTTP fallback temporaneamente disabilitato per test
        mqttService.busPositions.collect { mqttPositions ->
            if (mqttPositions.isNotEmpty()) {
                Log.d(TAG, "Using MQTT data: ${mqttPositions.size} buses")
                val buses = mqttPositions.map { convertMqttToBus(it) }
                emit(buses)
            } else {
                Log.d(TAG, "MQTT connected but no data received yet")
                emit(emptyList())
            }
        }
    }

    private suspend fun connectMqttIfNeeded() {
        if (!mqttService.isConnected()) {
            Log.d(TAG, "Connecting to MQTT...")
            mqttService.connect()
            delay(2000) // Give some time for connection
        }
    }

    private suspend fun getHttpFallbackData(): List<Bus> {
        return try {
            val httpFlow = httpRepository.getRealTimeBuses()
            var httpBuses = emptyList<Bus>()

            httpFlow.collect { buses ->
                httpBuses = buses
                return@collect
            }

            httpBuses
        } catch (e: Exception) {
            Log.e(TAG, "HTTP fallback failed", e)
            emptyList()
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
        return httpRepository.getLineStats()
    }

    override suspend fun getSystemStatus(): SystemStatus {
        return httpRepository.getSystemStatus()
    }

    fun getMqttConnectionState(): Flow<MqttConnectionState> {
        return mqttService.connectionState
    }

    fun disconnectMqtt() {
        mqttService.disconnect()
    }
}*/