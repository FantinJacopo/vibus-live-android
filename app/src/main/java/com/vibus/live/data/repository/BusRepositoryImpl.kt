package com.vibus.live.data.repository

import android.util.Log
import com.vibus.live.data.Bus
import com.vibus.live.data.BusStatus
import com.vibus.live.data.LineStats
import com.vibus.live.data.Position
import com.vibus.live.data.SystemHealth
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.api.InfluxApiService
import com.vibus.live.data.api.InfluxCSVParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Singleton
class BusRepositoryImpl @Inject constructor(
    private val api: InfluxApiService
) : BusRepository {

    companion object {
        private const val TAG = "BusRepository"
        private const val UPDATE_INTERVAL_MS = 10_000L // 10 secondi
        private const val STATS_UPDATE_INTERVAL_MS = 10_000L // 10 secondi per statistiche
        private const val FALLBACK_DELAY_MS = 15_000L // 15 secondi in caso di errore
    }

    // ===== AUTOBUS REAL-TIME (esistente) =====
    override fun getRealTimeBuses(): Flow<List<Bus>> = flow {
        while (true) {
            try {
                Log.d(TAG, "=== FETCHING REAL-TIME BUS DATA ===")
                Log.d(TAG, "Timestamp: ${LocalDateTime.now()}")

                val response = api.queryFlux(
                    token = InfluxApiService.TOKEN,
                    org = InfluxApiService.ORG,
                    query = InfluxApiService.getRealTimeBusesQuery()
                )

                if (response.isSuccessful && response.body() != null) {
                    val csvData = response.body()!!
                    Log.d(TAG, "Received CSV data: ${csvData.take(200)}...")

                    val buses = parseRealTimeBusData(csvData)
                    Log.d(TAG, "Parsed ${buses.size} buses from InfluxDB")

                    if (buses.isNotEmpty()) {
                        Log.d(TAG, "=== EMITTING ${buses.size} BUSES ===")
                        emit(buses)
                    } else {
                        Log.w(TAG, "No buses found in InfluxDB, using fallback data")
                        val fallback = generateFallbackBuses()
                        emit(fallback)
                    }
                } else {
                    Log.e(TAG, "InfluxDB query failed: ${response.code()} - ${response.message()}")
                    val fallback = generateFallbackBuses()
                    emit(fallback)
                }

                delay(UPDATE_INTERVAL_MS)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching bus data", e)
                val fallback = generateFallbackBuses()
                emit(fallback)
                delay(FALLBACK_DELAY_MS)
            }
        }
    }

    // ===== NUOVO: STATISTICHE LINEE REAL-TIME =====
    override fun getRealTimeLineStats(): Flow<List<LineStats>> = flow {
        while (true) {
            try {
                Log.d(TAG, "=== FETCHING REAL-TIME LINE STATS ===")

                val response = api.queryFlux(
                    token = InfluxApiService.TOKEN,
                    org = InfluxApiService.ORG,
                    query = InfluxApiService.getLineStatsQuery()
                )

                if (response.isSuccessful && response.body() != null) {
                    val csvData = response.body()!!
                    Log.d(TAG, "Received line stats CSV: ${csvData.take(200)}...")

                    val stats = parseLineStatsData(csvData)
                    Log.d(TAG, "Parsed ${stats.size} line statistics from InfluxDB")

                    if (stats.isNotEmpty()) {
                        emit(stats)
                    } else {
                        Log.w(TAG, "No line stats found in InfluxDB, using fallback")
                        emit(generateFallbackLineStats())
                    }
                } else {
                    Log.e(TAG, "Line stats query failed: ${response.code()}")
                    emit(generateFallbackLineStats())
                }

                delay(STATS_UPDATE_INTERVAL_MS)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching line stats", e)
                emit(generateFallbackLineStats())
                delay(FALLBACK_DELAY_MS)
            }
        }
    }

    // ===== NUOVO: STATO SISTEMA REAL-TIME =====
    override fun getRealTimeSystemStatus(): Flow<SystemStatus?> = flow {
        while (true) {
            try {
                Log.d(TAG, "=== FETCHING REAL-TIME SYSTEM STATUS ===")

                val response = api.queryFlux(
                    token = InfluxApiService.TOKEN,
                    org = InfluxApiService.ORG,
                    query = InfluxApiService.getSystemStatusQuery()
                )

                if (response.isSuccessful && response.body() != null) {
                    val csvData = response.body()!!
                    Log.d(TAG, "Received system status CSV: ${csvData.take(200)}...")

                    val status = parseSystemStatusData(csvData)
                    Log.d(TAG, "Parsed system status from InfluxDB: $status")

                    emit(status ?: generateFallbackSystemStatus())
                } else {
                    Log.e(TAG, "System status query failed: ${response.code()}")
                    emit(generateFallbackSystemStatus())
                }

                delay(STATS_UPDATE_INTERVAL_MS)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching system status", e)
                emit(generateFallbackSystemStatus())
                delay(FALLBACK_DELAY_MS)
            }
        }
    }

    // ===== METODI LEGACY (mantenuti per compatibilità) =====
    override suspend fun getLineStats(): List<LineStats> {
        return try {
            Log.d(TAG, "Fetching line statistics from InfluxDB (legacy method)...")

            val response = api.queryFlux(
                token = InfluxApiService.TOKEN,
                org = InfluxApiService.ORG,
                query = InfluxApiService.getLineStatsQuery()
            )

            if (response.isSuccessful && response.body() != null) {
                val csvData = response.body()!!
                val stats = parseLineStatsData(csvData)
                if (stats.isNotEmpty()) stats else generateFallbackLineStats()
            } else {
                Log.e(TAG, "Line stats query failed: ${response.code()}")
                generateFallbackLineStats()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching line stats", e)
            generateFallbackLineStats()
        }
    }

    override suspend fun getSystemStatus(): SystemStatus {
        return try {
            Log.d(TAG, "Fetching system status from InfluxDB (legacy method)...")

            val response = api.queryFlux(
                token = InfluxApiService.TOKEN,
                org = InfluxApiService.ORG,
                query = InfluxApiService.getSystemStatusQuery()
            )

            if (response.isSuccessful && response.body() != null) {
                val csvData = response.body()!!
                val status = parseSystemStatusData(csvData)
                status ?: generateFallbackSystemStatus()
            } else {
                Log.e(TAG, "System status query failed: ${response.code()}")
                generateFallbackSystemStatus()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching system status", e)
            generateFallbackSystemStatus()
        }
    }

    // ===== PARSING DELLE RISPOSTE CSV DI INFLUXDB =====

    private fun parseRealTimeBusData(csvData: String): List<Bus> {
        return try {
            Log.d(TAG, "=== PARSING CSV DATA ===")
            val parsed = InfluxCSVParser.parseCSV(csvData)
            Log.d(TAG, "Headers found: ${parsed.headers}")
            Log.d(TAG, "Number of rows: ${parsed.rows.size}")

            val buses = mutableListOf<Bus>()

            val busIdIndex = findColumnIndex(parsed.headers, listOf("bus_id", "busId", "id"))
            val lineIndex = findColumnIndex(parsed.headers, listOf("line", "lineId"))
            val latIndex = findColumnIndex(parsed.headers, listOf("latitude", "lat"))
            val lonIndex = findColumnIndex(parsed.headers, listOf("longitude", "lon", "lng"))
            val speedIndex = findColumnIndex(parsed.headers, listOf("speed"))
            val bearingIndex = findColumnIndex(parsed.headers, listOf("bearing", "direction"))
            val delayIndex = findColumnIndex(parsed.headers, listOf("delay"))
            val passengersIndex = findColumnIndex(parsed.headers, listOf("passengers"))
            val statusIndex = findColumnIndex(parsed.headers, listOf("status"))

            parsed.rows.forEachIndexed { index, row ->
                if (row.isNotEmpty() && busIdIndex >= 0) {
                    val busId = InfluxCSVParser.safeGetValue(row, busIdIndex)
                    val line = InfluxCSVParser.safeGetValue(row, lineIndex)

                    if (busId.isNotBlank() && line.isNotBlank()) {
                        val bus = Bus(
                            id = busId,
                            line = line,
                            lineName = getLineDisplayName(line),
                            position = Position(
                                latitude = InfluxCSVParser.safeGetDouble(row, latIndex, 45.5477),
                                longitude = InfluxCSVParser.safeGetDouble(row, lonIndex, 11.5458)
                            ),
                            speed = InfluxCSVParser.safeGetDouble(row, speedIndex, 25.0),
                            bearing = InfluxCSVParser.safeGetInt(row, bearingIndex, 0),
                            delay = InfluxCSVParser.safeGetDouble(row, delayIndex, 0.0),
                            passengers = InfluxCSVParser.safeGetInt(row, passengersIndex, 0),
                            status = parseBusStatus(InfluxCSVParser.safeGetValue(row, statusIndex, "in_service")),
                            lastUpdate = LocalDateTime.now()
                        )
                        buses.add(bus)
                    }
                }
            }

            buses
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing bus data CSV", e)
            emptyList()
        }
    }

    private fun parseLineStatsData(csvData: String): List<LineStats> {
        return try {
            val parsed = InfluxCSVParser.parseCSV(csvData)
            val stats = mutableListOf<LineStats>()

            val lineIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "line")
            val avgSpeedIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "avg_speed")
            val avgDelayIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "avg_delay")
            val totalPassengersIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "total_passengers")
            val onTimeIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "on_time_percentage")
            val activeBusesIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "active_buses")

            parsed.rows.forEach { row ->
                if (row.isNotEmpty() && lineIndex >= 0) {
                    val line = InfluxCSVParser.safeGetValue(row, lineIndex)

                    if (line.isNotBlank()) {
                        stats.add(
                            LineStats(
                                line = line,
                                activeBuses = InfluxCSVParser.safeGetInt(row, activeBusesIndex, 2),
                                averageSpeed = InfluxCSVParser.safeGetDouble(row, avgSpeedIndex, 25.0),
                                averageDelay = InfluxCSVParser.safeGetDouble(row, avgDelayIndex, 0.0),
                                maxDelay = InfluxCSVParser.safeGetDouble(row, avgDelayIndex, 0.0) + 2.0,
                                onTimePercentage = InfluxCSVParser.safeGetDouble(row, onTimeIndex, 85.0),
                                totalPassengers = InfluxCSVParser.safeGetInt(row, totalPassengersIndex, 100),
                                lastUpdate = LocalDateTime.now()
                            )
                        )
                    }
                }
            }

            stats
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing line stats CSV", e)
            emptyList()
        }
    }

    private fun parseSystemStatusData(csvData: String): SystemStatus? {
        return try {
            val parsed = InfluxCSVParser.parseCSV(csvData)

            if (parsed.rows.isNotEmpty()) {
                val row = parsed.rows.first()

                val activeBusesIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "active_buses")
                val totalPassengersIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "total_passengers")
                val avgDelayIndex = InfluxCSVParser.getColumnIndex(parsed.headers, "avg_system_delay")

                SystemStatus(
                    totalBuses = 10,
                    activeBuses = InfluxCSVParser.safeGetInt(row, activeBusesIndex, 8),
                    totalPassengers = InfluxCSVParser.safeGetInt(row, totalPassengersIndex, 250),
                    averageSystemDelay = InfluxCSVParser.safeGetDouble(row, avgDelayIndex, 1.5),
                    systemHealth = calculateSystemHealth(InfluxCSVParser.safeGetDouble(row, avgDelayIndex, 1.5)),
                    lastUpdate = LocalDateTime.now()
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing system status CSV", e)
            null
        }
    }

    // ===== UTILITY FUNCTIONS =====

    private fun findColumnIndex(headers: List<String>, possibleNames: List<String>): Int {
        for (name in possibleNames) {
            val index = headers.indexOfFirst { it.equals(name, ignoreCase = true) }
            if (index >= 0) return index
        }
        return -1
    }

    private fun getLineDisplayName(line: String): String {
        return when (line) {
            "1" -> "Stanga-Ospedale"
            "2" -> "Anconetta-Ferrovieri"
            "3" -> "Maddalene-Cattane"
            "5" -> "Villaggio-Centro"
            "7" -> "Laghetto-Stadio"
            else -> "Linea $line"
        }
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

    private fun calculateSystemHealth(avgDelay: Double): SystemHealth {
        return when {
            avgDelay <= 1.0 -> SystemHealth.EXCELLENT
            avgDelay <= 2.0 -> SystemHealth.GOOD
            avgDelay <= 3.0 -> SystemHealth.FAIR
            avgDelay <= 5.0 -> SystemHealth.POOR
            else -> SystemHealth.CRITICAL
        }
    }

    // ===== FALLBACK DATA =====

    private fun generateFallbackBuses(): List<Bus> {
        val lines = listOf(
            "1" to "Stanga-Ospedale",
            "2" to "Anconetta-Ferrovieri",
            "3" to "Maddalene-Cattane",
            "5" to "Villaggio-Centro",
            "7" to "Laghetto-Stadio"
        )

        val vicenzaCenter = Position(45.5477, 11.5458)
        val buses = mutableListOf<Bus>()
        val currentTime = System.currentTimeMillis()
        val timeOffset = (currentTime / 10000) % 1000

        lines.forEach { (lineId, lineName) ->
            repeat(2) { busIndex ->
                val angle = (timeOffset + busIndex * 180 + lineId.toInt() * 72) * Math.PI / 180
                val radius = 0.01 + (busIndex * 0.005)

                val lat = vicenzaCenter.latitude + radius * cos(angle)
                val lon = vicenzaCenter.longitude + radius * sin(angle)

                buses.add(
                    Bus(
                        id = "SVT${lineId}${(busIndex + 1).toString().padStart(2, '0')}",
                        line = lineId,
                        lineName = lineName,
                        position = Position(latitude = lat, longitude = lon),
                        speed = 25.0 + Random.nextDouble() * 15.0,
                        bearing = ((angle * 180 / Math.PI) + 90).toInt() % 360,
                        delay = (Random.nextDouble() - 0.3) * 5.0,
                        passengers = Random.nextInt(0, 45),
                        status = if (Random.nextDouble() > 0.1) BusStatus.IN_SERVICE else BusStatus.DELAYED,
                        lastUpdate = LocalDateTime.now()
                    )
                )
            }
        }

        Log.d(TAG, "Generated ${buses.size} fallback buses")
        return buses
    }

    private fun generateFallbackLineStats(): List<LineStats> {
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
        return SystemStatus(
            totalBuses = 10,
            activeBuses = 8 + Random.nextInt(0, 3),
            totalPassengers = Random.nextInt(200, 400),
            averageSystemDelay = (Random.nextDouble() - 0.2) * 2.0,
            systemHealth = SystemHealth.entries[Random.nextInt(SystemHealth.entries.size)],
            lastUpdate = LocalDateTime.now()
        )
    }
}