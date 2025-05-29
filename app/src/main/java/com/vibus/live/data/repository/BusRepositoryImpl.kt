package com.vibus.live.data.repository

import com.vibus.live.data.Bus
import com.vibus.live.data.BusStatus
import com.vibus.live.data.LineStats
import com.vibus.live.data.Position
import com.vibus.live.data.SystemHealth
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.api.InfluxApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class BusRepositoryImpl @Inject constructor(
    private val api: InfluxApiService
) : BusRepository {

    override fun getRealTimeBuses(): Flow<List<Bus>> = flow {
        while (true) {
            try {
                // For demo, we'll simulate data since parsing InfluxDB CSV is complex
                // In a real app, you'd parse the API response properly
                val buses = generateMockBuses()
                emit(buses)
                delay(10_000) // Update every 10 seconds
            } catch (e: Exception) {
                // Fallback to mock data
                emit(generateMockBuses())
                delay(15_000) // Longer delay on error
            }
        }
    }

    override suspend fun getLineStats(): List<LineStats> {
        return try {
            // Mock implementation - in real app, parse InfluxDB response
            generateMockLineStats()
        } catch (e: Exception) {
            generateMockLineStats()
        }
    }

    override suspend fun getSystemStatus(): SystemStatus {
        return try {
            // Mock implementation
            generateMockSystemStatus()
        } catch (e: Exception) {
            generateMockSystemStatus()
        }
    }

    // Mock data generators (replace with real API parsing in production)
    private fun generateMockBuses(): List<Bus> {
        val lines = listOf(
            "1" to "Stanga-Ospedale",
            "2" to "Anconetta-Ferrovieri",
            "3" to "Maddalene-Cattane",
            "5" to "Villaggio-Centro",
            "7" to "Laghetto-Stadio"
        )

        val vicenzaCenter = Position(45.5477, 11.5458)
        val buses = mutableListOf<Bus>()

        lines.forEach { (lineId, lineName) ->
            repeat(2) { busIndex ->
                buses.add(
                    Bus(
                        id = "SVT${lineId}${(busIndex + 1).toString().padStart(2, '0')}",
                        line = lineId,
                        lineName = lineName,
                        position = Position(
                            latitude = vicenzaCenter.latitude + (Random.nextDouble() - 0.5) * 0.02,
                            longitude = vicenzaCenter.longitude + (Random.nextDouble() - 0.5) * 0.02
                        ),
                        speed = 25.0 + Random.nextDouble() * 15.0,
                        bearing = Random.nextInt(0, 360),
                        delay = (Random.nextDouble() - 0.3) * 5.0,
                        passengers = Random.nextInt(0, 45),
                        status = if (Random.nextDouble() > 0.1) BusStatus.IN_SERVICE else BusStatus.DELAYED,
                        lastUpdate = LocalDateTime.now()
                    )
                )
            }
        }

        return buses
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
        return SystemStatus(
            totalBuses = 10,
            activeBuses = 8 + Random.nextInt(0, 3),
            totalPassengers = Random.nextInt(200, 400),
            averageSystemDelay = (Random.nextDouble() - 0.2) * 2.0,
            systemHealth = SystemHealth.values()[Random.nextInt(SystemHealth.values().size)],
            lastUpdate = LocalDateTime.now()
        )
    }
}

interface BusRepository {
    fun getRealTimeBuses(): Flow<List<Bus>>
    suspend fun getLineStats(): List<LineStats>
    suspend fun getSystemStatus(): SystemStatus
}