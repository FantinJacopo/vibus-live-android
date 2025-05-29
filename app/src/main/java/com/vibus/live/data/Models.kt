package com.vibus.live.data

import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime

data class Bus(
    val id: String,
    val line: String,
    val lineName: String,
    val position: Position,
    val speed: Double,
    val bearing: Int,
    val delay: Double,
    val passengers: Int,
    val status: BusStatus,
    val lastUpdate: LocalDateTime
)

data class Position(
    val latitude: Double,
    val longitude: Double
)

enum class BusStatus {
    IN_SERVICE,
    OUT_OF_SERVICE,
    MAINTENANCE,
    DELAYED
}

data class BusLine(
    val id: String,
    val name: String,
    val color: String,
    val activeBuses: Int,
    val averageSpeed: Double,
    val averageDelay: Double,
    val onTimePercentage: Double
)

data class LineStats(
    val line: String,
    val activeBuses: Int,
    val averageSpeed: Double,
    val averageDelay: Double,
    val maxDelay: Double,
    val onTimePercentage: Double,
    val totalPassengers: Int,
    val lastUpdate: LocalDateTime
)

data class SystemStatus(
    val totalBuses: Int,
    val activeBuses: Int,
    val totalPassengers: Int,
    val averageSystemDelay: Double,
    val systemHealth: SystemHealth,
    val lastUpdate: LocalDateTime
)

enum class SystemHealth {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL
}

// Helper extensions
val Bus.displayName: String
    get() = "$id - Linea $line"

val Bus.statusColor: Color
    get() = when (status) {
        BusStatus.IN_SERVICE -> Color.Green
        BusStatus.OUT_OF_SERVICE -> Color.Gray
        BusStatus.MAINTENANCE -> Color.Red
        BusStatus.DELAYED -> Color.Yellow
    }

val Bus.delayText: String
    get() = when {
        delay > 0 -> "+${delay.toInt()} min"
        delay < 0 -> "${delay.toInt()} min"
        else -> "In orario"
    }