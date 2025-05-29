package com.vibus.live.data.api

import retrofit2.Response
import retrofit2.http.*

interface InfluxApiService {

    @POST("api/v2/query?org=SVT-Vicenza")
    @Headers("Content-Type: application/vnd.flux")
    suspend fun query(
        @Header("Authorization") token: String,
        @Body query: String
    ): Response<String>

    companion object {
        const val BASE_URL = "http://more-elk-slightly.ngrok-free.app/"
        const val TOKEN = "Token svt-super-secret-token-123456789"

        // Flux Queries
        const val REAL_TIME_BUSES_QUERY = """
            from(bucket: "bus-data")
              |> range(start: -2m)
              |> filter(fn: (r) => r._measurement == "bus_positions")
              |> pivot(rowKey:["bus_id", "line"], columnKey: ["_field"], valueColumn: "_value")
              |> group()
              |> unique(column: "bus_id")
              |> drop(columns: ["_start", "_stop", "_time"])
        """

        const val LINE_STATS_QUERY = """
            from(bucket: "bus-data")
              |> range(start: -30m)
              |> filter(fn: (r) => r._measurement == "bus_positions")
              |> filter(fn: (r) => r._field == "speed" or r._field == "delay" or r._field == "passengers")
              |> group(columns: ["line", "_field"])
              |> mean()
        """

        const val SYSTEM_STATUS_QUERY = """
            from(bucket: "bus-data")
              |> range(start: -5m)
              |> filter(fn: (r) => r._measurement == "bus_positions")
              |> filter(fn: (r) => r._field == "passengers")
              |> unique(column: "bus_id")
              |> sum()
        """
    }
}

// Response DTOs
data class InfluxResponse(
    val results: List<InfluxResult>
)

data class InfluxResult(
    val series: List<InfluxSeries>?
)

data class InfluxSeries(
    val name: String,
    val columns: List<String>,
    val values: List<List<Any?>>?,
    val tags: Map<String, String>?
)