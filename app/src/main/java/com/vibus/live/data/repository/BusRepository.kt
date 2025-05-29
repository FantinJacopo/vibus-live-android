package com.vibus.live.data.repository

import com.vibus.live.data.Bus
import com.vibus.live.data.LineStats
import com.vibus.live.data.SystemStatus
import kotlinx.coroutines.flow.Flow

interface BusRepository {
    fun getRealTimeBuses(): Flow<List<Bus>>
    suspend fun getLineStats(): List<LineStats>
    suspend fun getSystemStatus(): SystemStatus
}