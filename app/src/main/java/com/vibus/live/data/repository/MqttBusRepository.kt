package com.vibus.live.data.repository

import android.util.Log
import com.vibus.live.data.*
import com.vibus.live.data.mqtt.*
import com.vibus.live.mqtt.MessageCache
import com.vibus.live.mqtt.MqttMessageParser
import com.vibus.live.mqtt.ParsedMqttData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttBusRepository @Inject constructor(
    private val mqttManager: MqttManager,
    private val messageParser: MqttMessageParser,
    private val httpFallbackRepository: BusRepositoryImpl // Fallback HTTP
) : BusRepository {

    companion object {
        private const val TAG = "MqttBusRepository"
        private const val CACHE_CLEANUP_INTERVAL_MINUTES = 5L
        private const val FALLBACK_THRESHOLD_FAILURES = 3
    }

    private val messageCache = MessageCache()
    private var consecutiveFailures = 0
    private var isInitialized = false
    private var useFallback = false

    /**
     * Inizializza connessione MQTT e subscriptions
     */
    suspend fun initialize(): MqttResult<Unit> = supervisorScope {
        try {
            Log.d(TAG, "=== INITIALIZING MQTT BUS REPOSITORY ===")

            if (isInitialized) {
                Log.d(TAG, "Already initialized")
                return@supervisorScope MqttResult.Success(Unit)
            }

            // Connetti al broker
            val brokerConfig = MqttConfig.getConfigForEnvironment(MqttConfig.Environment.TESTING)

            when (val connectResult = mqttManager.connect(brokerConfig)) {
                is MqttResult.Success -> {
                    Log.d(TAG, "Connected to MQTT broker successfully")
                }
                is MqttResult.Error -> {
                    Log.e(TAG, "Failed to connect to MQTT broker: ${connectResult.error}")
                    enableFallback()
                    return@supervisorScope connectResult
                }
                else -> {
                    return@supervisorScope MqttResult.Loading("Connecting...")
                }
            }

            // Subscribe ai topic necessari
            subscribeToTopics()

            // Avvia processing dei messaggi
            startMessageProcessing()

            // Avvia cleanup periodico
            startCacheCleanup()

            isInitialized = true
            consecutiveFailures = 0
            useFallback = false

            Log.d(TAG, "MQTT Bus Repository initialized successfully")
            MqttResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MQTT repository", e)
            enableFallback()
            MqttResult.Error(MqttError.UnknownError(e.message ?: "Initialization failed"))
        }
    }

    /**
     * Subscribe ai topic MQTT
     */
    private suspend fun subscribeToTopics() {
        Log.d(TAG, "Subscribing to MQTT topics...")

        // Subscribe posizioni autobus
        mqttManager.subscribe(
            MqttConfig.Topics.BUS_POSITIONS,
            MqttConfig.Client.QOS_BUS_POSITIONS
        ).also { result ->
            when (result) {
                is MqttResult.Success -> Log.d(TAG, "Subscribed to bus positions")
                is MqttResult.Error -> Log.e(TAG, "Failed to subscribe to bus positions: ${result.error}")
                else -> {}
            }
        }

        // Subscribe statistiche linee
        mqttManager.subscribe(
            MqttConfig.Topics.LINE_STATS,
            MqttConfig.Client.QOS_STATS
        ).also { result ->
            when (result) {
                is MqttResult.Success -> Log.d(TAG, "Subscribed to line stats")
                is MqttResult.Error -> Log.e(TAG, "Failed to subscribe to line stats: ${result.error}")
                else -> {}
            }
        }

        // Subscribe stato sistema
        mqttManager.subscribe(
            MqttConfig.Topics.SYSTEM_STATUS,
            MqttConfig.Client.QOS_STATUS
        ).also { result ->
            when (result) {
                is MqttResult.Success -> Log.d(TAG, "Subscribed to system status")
                is MqttResult.Error -> Log.e(TAG, "Failed to subscribe to system status: ${result.error}")
                else -> {}
            }
        }
    }

    /**
     * Avvia processing dei messaggi MQTT
     */
    private suspend fun startMessageProcessing() = supervisorScope {
        launch {
            mqttManager.messages.collect { mqttMessage ->
                try {
                    Log.d(TAG, "Processing MQTT message from: ${mqttMessage.topic}")

                    when (val parseResult = messageParser.parseMessage(mqttMessage)) {
                        is MqttResult.Success -> {
                            when (val data = parseResult.data) {
                                is ParsedMqttData.BusPosition -> {
                                    messageCache.updateBusPosition(data.bus)
                                    consecutiveFailures = 0
                                }
                                is ParsedMqttData.LineStatistics -> {
                                    messageCache.updateLineStats(data.lineStats)
                                }
                                is ParsedMqttData.SystemStatus -> {
                                    messageCache.updateSystemStatus(data.systemStatus)
                                }
                            }
                        }
                        is MqttResult.Error -> {
                            Log.e(TAG, "Failed to parse message: ${parseResult.error}")
                            handleFailure()
                        }
                        else -> { /* Loading state */ }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing MQTT message", e)
                    handleFailure()
                }
            }
        }

        // Monitor stato connessione
        launch {
            mqttManager.connectionState.collect { state ->
                Log.d(TAG, "MQTT connection state: $state")
                when (state) {
                    MqttConnectionState.DISCONNECTED,
                    MqttConnectionState.ERROR -> {
                        handleFailure()
                    }
                    MqttConnectionState.CONNECTED -> {
                        consecutiveFailures = 0
                        if (useFallback) {
                            Log.d(TAG, "Reconnected to MQTT, disabling fallback")
                            useFallback = false
                        }
                    }
                    else -> { /* Other states */ }
                }
            }
        }
    }

    /**
     * Avvia cleanup periodico della cache
     */
    private suspend fun startCacheCleanup() = supervisorScope {
        launch {
            while (true) {
                delay(CACHE_CLEANUP_INTERVAL_MINUTES * 60 * 1000)
                try {
                    messageCache.cleanup(maxAge = MqttConfig.Cache.BUS_DATA_TTL_MINUTES.minutes.toJavaDuration())
                    Log.d(TAG, "Cache cleanup completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Cache cleanup failed", e)
                }
            }
        }
    }

    /**
     * Stream di autobus in tempo reale
     */
    override fun getRealTimeBuses(): Flow<List<Bus>> = flow {
        // Inizializza se necessario
        if (!isInitialized) {
            initialize()
        }

        // Se fallback attivo, usa HTTP
        if (useFallback) {
            Log.d(TAG, "Using HTTP fallback for real-time buses")
            httpFallbackRepository.getRealTimeBuses().collect { buses ->
                emit(buses)
            }
            return@flow
        }

        // Emetti dati dalla cache MQTT
        while (true) {
            try {
                val buses = messageCache.getBusPositions()

                if (buses.isNotEmpty()) {
                    Log.d(TAG, "Emitting ${buses.size} buses from MQTT cache")
                    emit(buses)
                } else {
                    // Se nessun dato MQTT, usa fallback dati generati
                    Log.w(TAG, "No MQTT data available, generating fallback data")
                    val fallbackBuses = generateFallbackBuses()
                    emit(fallbackBuses)
                }

                delay(1000) // Emetti ogni secondo per aggiornamenti fluidi

            } catch (e: Exception) {
                Log.e(TAG, "Error in real-time buses flow", e)
                handleFailure()

                // Fallback se troppi errori
                if (useFallback) {
                    httpFallbackRepository.getRealTimeBuses().collect { buses ->
                        emit(buses)
                    }
                    return@flow
                }
            }
        }
    }

    /**
     * Ottieni statistiche linee
     */
    override suspend fun getLineStats(): List<LineStats> {
        return try {
            if (useFallback) {
                Log.d(TAG, "Using HTTP fallback for line stats")
                return httpFallbackRepository.getLineStats()
            }

            val stats = messageCache.getLineStatistics()

            if (stats.isNotEmpty()) {
                Log.d(TAG, "Retrieved ${stats.size} line stats from MQTT cache")
                stats
            } else {
                Log.w(TAG, "No MQTT line stats available, using fallback")
                generateFallbackLineStats()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting line stats", e)
            handleFailure()

            if (useFallback) {
                httpFallbackRepository.getLineStats()
            } else {
                generateFallbackLineStats()
            }
        }
    }

    /**
     * Ottieni stato sistema
     */
    override suspend fun getSystemStatus(): SystemStatus {
        return try {
            if (useFallback) {
                Log.d(TAG, "Using HTTP fallback for system status")
                return httpFallbackRepository.getSystemStatus()
            }

            val status = messageCache.systemStatus

            if (status != null) {
                Log.d(TAG, "Retrieved system status from MQTT cache")
                status
            } else {
                Log.w(TAG, "No MQTT system status available, using fallback")
                generateFallbackSystemStatus()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting system status", e)
            handleFailure()

            if (useFallback) {
                httpFallbackRepository.getSystemStatus()
            } else {
                generateFallbackSystemStatus()
            }
        }
    }

    /**
     * Gestione fallimenti
     */
    private fun handleFailure() {
        consecutiveFailures++
        Log.w(TAG, "Consecutive failures: $consecutiveFailures")

        if (consecutiveFailures >= FALLBACK_THRESHOLD_FAILURES) {
            enableFallback()
        }
    }

    /**
     * Abilita fallback HTTP
     */
    private fun enableFallback() {
        if (!useFallback) {
            Log.w(TAG, "Enabling HTTP fallback due to MQTT failures")
            useFallback = true
        }
    }

    /**
     * Statistiche MQTT
     */
    fun getMqttStats(): MqttConnectionStats {
        return mqttManager.getConnectionStats()
    }

    /**
     * Forza riconnessione MQTT
     */
    suspend fun reconnect(): MqttResult<Unit> {
        Log.d(TAG, "Forcing MQTT reconnection")
        consecutiveFailures = 0
        useFallback = false

        val brokerConfig = MqttConfig.getConfigForEnvironment(MqttConfig.Environment.TESTING)
        return mqttManager.connect(brokerConfig)
    }

    /**
     * Disconnetti MQTT
     */
    suspend fun disconnect() {
        Log.d(TAG, "Disconnecting MQTT")
        mqttManager.disconnect()
        isInitialized = false
    }

    /**
     * Cleanup risorse
     */
    fun cleanup() {
        mqttManager.cleanup()
    }

    // === FALLBACK DATA GENERATION ===

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

                val lat = vicenzaCenter.latitude + radius * Math.cos(angle)
                val lon = vicenzaCenter.longitude + radius * Math.sin(angle)

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
            systemHealth = SystemHealth.values()[Random.nextInt(SystemHealth.values().size)],
            lastUpdate = LocalDateTime.now()
        )
    }
}