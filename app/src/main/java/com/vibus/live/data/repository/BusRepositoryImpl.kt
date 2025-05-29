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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class BusRepositoryImpl @Inject constructor(
    private val api: InfluxApiService
) : BusRepository {

    companion object {
        private const val TAG = "BusRepository"
        private const val UPDATE_INTERVAL_MS = 10_000L // 10 secondi
        private const val FALLBACK_DELAY_MS = 15_000L // 15 secondi in caso di errore
    }

    override fun getRealTimeBuses(): Flow<List<Bus>> = flow {
        while (true) {
            try {
                Log.d(TAG, "=== FETCHING REAL-TIME DATA ===")
                Log.d(TAG, "Timestamp: ${java.time.LocalDateTime.now()}")

                val response = api.queryFlux(
                    token = InfluxApiService.TOKEN,
                    org = InfluxApiService.ORG,
                    query = InfluxApiService.getRealTimeBusesQuery()
                )

                if (response.isSuccessful && response.body() != null) {
                    val csvData = response.body()!!
                    Log.d(TAG, "Received CSV data: ${csvData.take(200)}...") // Log primi 200 char

                    val buses = parseRealTimeBusData(csvData)
                    Log.d(TAG, "Parsed ${buses.size} buses from InfluxDB")

                    if (buses.isNotEmpty()) {
                        Log.d(TAG, "=== EMITTING ${buses.size} BUSES ===")
                        buses.forEach { bus ->
                            Log.d(TAG, "Emitting: ${bus.id} at ${bus.position.latitude}, ${bus.position.longitude} (updated: ${bus.lastUpdate})")
                        }
                        emit(buses)
                    } else {
                        Log.w(TAG, "No buses found in InfluxDB, using fallback data")
                        val fallback = generateFallbackBuses()
                        Log.d(TAG, "=== EMITTING ${fallback.size} FALLBACK BUSES ===")
                        emit(fallback)
                    }
                } else {
                    Log.e(TAG, "InfluxDB query failed: ${response.code()} - ${response.message()}")
                    val fallback = generateFallbackBuses()
                    Log.d(TAG, "=== EMITTING ${fallback.size} FALLBACK BUSES (ERROR) ===")
                    emit(fallback)
                }

                Log.d(TAG, "Waiting ${UPDATE_INTERVAL_MS}ms for next update...")
                delay(UPDATE_INTERVAL_MS)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching bus data", e)
                val fallback = generateFallbackBuses()
                Log.d(TAG, "=== EMITTING ${fallback.size} FALLBACK BUSES (EXCEPTION) ===")
                emit(fallback)
                delay(FALLBACK_DELAY_MS)
            }
        }
    }

    override suspend fun getLineStats(): List<LineStats> {
        return try {
            Log.d(TAG, "Fetching line statistics from InfluxDB...")

            val response = api.queryFlux(
                token = InfluxApiService.TOKEN,
                org = InfluxApiService.ORG,
                query = InfluxApiService.getLineStatsQuery()
            )

            if (response.isSuccessful && response.body() != null) {
                val csvData = response.body()!!
                Log.d(TAG, "Received line stats CSV: ${csvData.take(200)}...")

                val stats = parseLineStatsData(csvData)
                Log.d(TAG, "Parsed ${stats.size} line statistics")

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
            Log.d(TAG, "Fetching system status from InfluxDB...")

            val response = api.queryFlux(
                token = InfluxApiService.TOKEN,
                org = InfluxApiService.ORG,
                query = InfluxApiService.getSystemStatusQuery()
            )

            if (response.isSuccessful && response.body() != null) {
                val csvData = response.body()!!
                Log.d(TAG, "Received system status CSV: ${csvData.take(200)}...")

                val status = parseSystemStatusData(csvData)
                Log.d(TAG, "Parsed system status: $status")

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

    // Parsing delle risposte CSV di InfluxDB
    private fun parseRealTimeBusData(csvData: String): List<Bus> {
        return try {
            Log.d(TAG, "=== PARSING CSV DATA ===")
            Log.d(TAG, "Raw CSV length: ${csvData.length}")
            Log.d(TAG, "First 500 chars: ${csvData.take(500)}")

            val parsed = InfluxCSVParser.parseCSV(csvData)
            Log.d(TAG, "Headers found: ${parsed.headers}")
            Log.d(TAG, "Number of rows: ${parsed.rows.size}")

            if (parsed.rows.isNotEmpty()) {
                Log.d(TAG, "First row: ${parsed.rows.first()}")
                if (parsed.rows.size > 1) {
                    Log.d(TAG, "Second row: ${parsed.rows[1]}")
                }
            }

            val buses = mutableListOf<Bus>()

            // Trova gli indici delle colonne - prova diversi nomi possibili
            val busIdIndex = findColumnIndex(parsed.headers, listOf("bus_id", "busId", "id"))
            val lineIndex = findColumnIndex(parsed.headers, listOf("line", "lineId"))
            val latIndex = findColumnIndex(parsed.headers, listOf("latitude", "lat"))
            val lonIndex = findColumnIndex(parsed.headers, listOf("longitude", "lon", "lng"))
            val speedIndex = findColumnIndex(parsed.headers, listOf("speed"))
            val bearingIndex = findColumnIndex(parsed.headers, listOf("bearing", "direction"))
            val delayIndex = findColumnIndex(parsed.headers, listOf("delay"))
            val passengersIndex = findColumnIndex(parsed.headers, listOf("passengers"))
            val statusIndex = findColumnIndex(parsed.headers, listOf("status"))

            Log.d(TAG, "Column indices: busId=$busIdIndex, line=$lineIndex, lat=$latIndex, lon=$lonIndex")

            parsed.rows.forEachIndexed { index, row ->
                if (row.isNotEmpty() && busIdIndex >= 0) {
                    val busId = InfluxCSVParser.safeGetValue(row, busIdIndex)
                    val line = InfluxCSVParser.safeGetValue(row, lineIndex)
                    val lat = InfluxCSVParser.safeGetDouble(row, latIndex, 45.5477)
                    val lon = InfluxCSVParser.safeGetDouble(row, lonIndex, 11.5458)

                    Log.d(TAG, "Row $index: busId='$busId', line='$line', lat=$lat, lon=$lon")

                    if (busId.isNotBlank() && line.isNotBlank()) {
                        val bus = Bus(
                            id = busId,
                            line = line,
                            lineName = getLineDisplayName(line),
                            position = Position(
                                latitude = lat,
                                longitude = lon
                            ),
                            speed = InfluxCSVParser.safeGetDouble(row, speedIndex, 25.0),
                            bearing = InfluxCSVParser.safeGetInt(row, bearingIndex, 0),
                            delay = InfluxCSVParser.safeGetDouble(row, delayIndex, 0.0),
                            passengers = InfluxCSVParser.safeGetInt(row, passengersIndex, 0),
                            status = parseBusStatus(InfluxCSVParser.safeGetValue(row, statusIndex, "in_service")),
                            lastUpdate = LocalDateTime.now()
                        )
                        buses.add(bus)
                        Log.d(TAG, "Added bus: ${bus.id} - ${bus.lineName} at (${bus.position.latitude}, ${bus.position.longitude})")
                    } else {
                        Log.w(TAG, "Skipping row $index: busId='$busId', line='$line' (missing required data)")
                    }
                }
            }

            Log.d(TAG, "=== PARSING COMPLETE ===")
            Log.d(TAG, "Total buses parsed: ${buses.size}")
            buses.forEach { bus ->
                Log.d(TAG, "Final bus: ${bus.id} (${bus.line}) - ${bus.position.latitude}, ${bus.position.longitude}")
            }

            buses
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing bus data CSV", e)
            Log.d(TAG, "CSV data that failed: $csvData")
            emptyList()
        }
    }

    // Helper per trovare l'indice di una colonna con nomi alternativi
    private fun findColumnIndex(headers: List<String>, possibleNames: List<String>): Int {
        for (name in possibleNames) {
            val index = headers.indexOfFirst { it.equals(name, ignoreCase = true) }
            if (index >= 0) return index
        }
        return -1
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
                                maxDelay = InfluxCSVParser.safeGetDouble(row, avgDelayIndex, 0.0) + 2.0, // Approssimazione
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
                    totalBuses = 10, // Valore fisso per ora
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

    // Utility functions
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

    // Fallback data per quando InfluxDB non è disponibile
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

        // Usa un timestamp base per simulare movimento consistente
        val currentTime = System.currentTimeMillis()
        val timeOffset = (currentTime / 10000) % 1000 // Cambia ogni 10 secondi

        lines.forEach { (lineId, lineName) ->
            repeat(2) { busIndex ->
                // Simula movimento circolare attorno al centro
                val angle = (timeOffset + busIndex * 180 + lineId.toInt() * 72) * Math.PI / 180
                val radius = 0.01 + (busIndex * 0.005) // Raggio diverso per ogni autobus

                val lat = vicenzaCenter.latitude + radius * Math.cos(angle)
                val lon = vicenzaCenter.longitude + radius * Math.sin(angle)

                buses.add(
                    Bus(
                        id = "SVT${lineId}${(busIndex + 1).toString().padStart(2, '0')}",
                        line = lineId,
                        lineName = lineName,
                        position = Position(
                            latitude = lat,
                            longitude = lon
                        ),
                        speed = 25.0 + Random.nextDouble() * 15.0,
                        bearing = ((angle * 180 / Math.PI) + 90).toInt() % 360, // Direzione del movimento
                        delay = (Random.nextDouble() - 0.3) * 5.0,
                        passengers = Random.nextInt(0, 45),
                        status = if (Random.nextDouble() > 0.1) BusStatus.IN_SERVICE else BusStatus.DELAYED,
                        lastUpdate = LocalDateTime.now()
                    )
                )
            }
        }

        Log.d(TAG, "Generated ${buses.size} fallback buses with simulated movement")
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
            systemHealth = SystemHealth.values()[Random.nextInt(SystemHealth.values().size)],
            lastUpdate = LocalDateTime.now()
        )
    }
}