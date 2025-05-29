package com.vibus.live.data.api

import retrofit2.Response
import retrofit2.http.*

interface InfluxApiService {

    @POST("api/v2/query")
    @Headers("Content-Type: application/vnd.flux")
    suspend fun queryFlux(
        @Header("Authorization") token: String,
        @Query("org") org: String,
        @Body query: String
    ): Response<String>

    companion object {
        const val BASE_URL = NetworkConfig.CURRENT_BASE_URL
        const val TOKEN = "Token ${NetworkConfig.INFLUX_TOKEN}"
        const val ORG = NetworkConfig.INFLUX_ORG
        const val BUCKET = NetworkConfig.INFLUX_BUCKET

        // Query Flux per ottenere le posizioni degli autobus in tempo reale
        fun getRealTimeBusesQuery(): String = """
            from(bucket: "$BUCKET")
              |> range(start: ${NetworkConfig.REAL_TIME_DATA_RANGE})
              |> filter(fn: (r) => r._measurement == "bus_positions")
              |> pivot(rowKey:["bus_id", "line"], columnKey: ["_field"], valueColumn: "_value")
              |> group()
              |> sort(columns: ["_time"], desc: true)
              |> unique(column: "bus_id")
              |> keep(columns: ["bus_id", "line", "latitude", "longitude", "speed", "bearing", "delay", "passengers", "status", "_time"])
        """.trimIndent()

        // Query per statistiche delle linee
        fun getLineStatsQuery(): String = """
            import "date"
            
            // Calcola statistiche per linea negli ultimi 30 minuti
            data = from(bucket: "$BUCKET")
              |> range(start: -30m)
              |> filter(fn: (r) => r._measurement == "bus_positions")
              |> filter(fn: (r) => r._field == "speed" or r._field == "delay" or r._field == "passengers")
            
            // Velocità media per linea
            avgSpeed = data
              |> filter(fn: (r) => r._field == "speed")
              |> group(columns: ["line"])
              |> mean(column: "_value")
              |> set(key: "_field", value: "avg_speed")
            
            // Ritardo medio per linea  
            avgDelay = data
              |> filter(fn: (r) => r._field == "delay")
              |> group(columns: ["line"])
              |> mean(column: "_value")
              |> set(key: "_field", value: "avg_delay")
            
            // Passeggeri totali per linea
            totalPassengers = data
              |> filter(fn: (r) => r._field == "passengers")
              |> group(columns: ["line"])
              |> last()
              |> sum(column: "_value")
              |> set(key: "_field", value: "total_passengers")
            
            // Unisci i risultati
            union(tables: [avgSpeed, avgDelay, totalPassengers])
              |> pivot(rowKey:["line"], columnKey: ["_field"], valueColumn: "_value")
              |> map(fn: (r) => ({
                  line: r.line,
                  avg_speed: if exists r.avg_speed then r.avg_speed else 0.0,
                  avg_delay: if exists r.avg_delay then r.avg_delay else 0.0,
                  total_passengers: if exists r.total_passengers then int(v: r.total_passengers) else 0,
                  on_time_percentage: if exists r.avg_delay then (if r.avg_delay <= 1.0 then 95.0 else 85.0) else 90.0,
                  active_buses: 2  // Placeholder - in un sistema reale calcoleresti questo
              }))
        """.trimIndent()

        // Query per stato del sistema
        fun getSystemStatusQuery(): String = """
            import "date"
            
            // Conta autobus attivi negli ultimi 5 minuti
            activeBuses = from(bucket: "$BUCKET")
              |> range(start: -5m)
              |> filter(fn: (r) => r._measurement == "bus_positions")
              |> filter(fn: (r) => r._field == "speed")
              |> group()
              |> unique(column: "bus_id")
              |> count()
              |> set(key: "metric", value: "active_buses")
            
            // Passeggeri totali
            totalPassengers = from(bucket: "$BUCKET")
              |> range(start: -5m)
              |> filter(fn: (r) => r._measurement == "bus_positions")
              |> filter(fn: (r) => r._field == "passengers")
              |> group()
              |> last()
              |> sum()
              |> set(key: "metric", value: "total_passengers")
            
            // Ritardo medio del sistema
            avgDelay = from(bucket: "$BUCKET")
              |> range(start: -10m)
              |> filter(fn: (r) => r._measurement == "bus_positions")
              |> filter(fn: (r) => r._field == "delay")
              |> group()
              |> mean()
              |> set(key: "metric", value: "avg_system_delay")
            
            union(tables: [activeBuses, totalPassengers, avgDelay])
              |> pivot(rowKey:["_stop"], columnKey: ["metric"], valueColumn: "_value")
        """.trimIndent()
    }
}

// Data classes per il parsing delle risposte CSV di InfluxDB
data class InfluxCSVResponse(
    val headers: List<String>,
    val rows: List<List<String>>
)

// Utility per parsare le risposte CSV di InfluxDB
object InfluxCSVParser {

    fun parseCSV(csvData: String): InfluxCSVResponse {
        val lines = csvData.trim().split('\n')
        if (lines.isEmpty()) return InfluxCSVResponse(emptyList(), emptyList())

        val headers = lines.first().split(',').map { it.trim() }
        val rows = lines.drop(1).map { line ->
            line.split(',').map { it.trim() }
        }

        return InfluxCSVResponse(headers, rows)
    }

    fun getColumnIndex(headers: List<String>, columnName: String): Int {
        return headers.indexOf(columnName)
    }

    fun safeGetValue(row: List<String>, index: Int, default: String = ""): String {
        return if (index >= 0 && index < row.size) row[index] else default
    }

    fun safeGetDouble(row: List<String>, index: Int, default: Double = 0.0): Double {
        return try {
            if (index >= 0 && index < row.size) {
                row[index].toDoubleOrNull() ?: default
            } else default
        } catch (e: Exception) {
            default
        }
    }

    fun safeGetInt(row: List<String>, index: Int, default: Int = 0): Int {
        return try {
            if (index >= 0 && index < row.size) {
                row[index].toIntOrNull() ?: default
            } else default
        } catch (e: Exception) {
            default
        }
    }
}