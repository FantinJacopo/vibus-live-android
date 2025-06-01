package com.vibus.live.data.repository

import com.vibus.live.data.Bus
import com.vibus.live.data.LineStats
import com.vibus.live.data.SystemStatus
import kotlinx.coroutines.flow.Flow

interface BusRepository {
    // Autobus in tempo reale (già Flow)
    fun getRealTimeBuses(): Flow<List<Bus>>

    // CAMBIATO: da suspend fun a Flow per aggiornamenti automatici
    fun getRealTimeLineStats(): Flow<List<LineStats>>

    // CAMBIATO: da suspend fun a Flow per aggiornamenti automatici
    fun getRealTimeSystemStatus(): Flow<SystemStatus?>

    // Mantieni metodi legacy per compatibilità (deprecati)
    @Deprecated("Use getRealTimeLineStats() Flow instead")
    suspend fun getLineStats(): List<LineStats>

    @Deprecated("Use getRealTimeSystemStatus() Flow instead")
    suspend fun getSystemStatus(): SystemStatus
}