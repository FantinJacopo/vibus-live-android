package com.vibus.live.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibus.live.data.Bus
import com.vibus.live.data.LineStats
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.repository.BusRepository
import com.vibus.live.data.repository.MqttBusRepository
import com.vibus.live.data.mqtt.MqttResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Cast per accesso funzioni MQTT specifiche
    private val mqttRepository = busRepository as? MqttBusRepository

    init {
        initializeDataSources()
        loadData()
        startRealTimeUpdates()
    }

    private fun initializeDataSources() {
        viewModelScope.launch {
            try {
                // Inizializza MQTT se disponibile
                mqttRepository?.let { mqtt ->
                    when (val result = mqtt.initialize()) {
                        is MqttResult.Success -> {
                            _uiState.update { it.copy(isMqttConnected = true, error = null) }
                        }
                        is MqttResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isMqttConnected = false,
                                    error = "MQTT connection failed: ${result.error.message}"
                                )
                            }
                        }
                        else -> {
                            _uiState.update { it.copy(isMqttConnected = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isMqttConnected = false,
                        error = "Initialization failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            busRepository.getRealTimeBuses()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Unknown error",
                            isLoading = false,
                            isMqttConnected = false
                        )
                    }
                }
                .collect { buses ->
                    _uiState.update {
                        it.copy(
                            buses = buses,
                            isLoading = false,
                            error = null,
                            lastUpdate = System.currentTimeMillis()
                        )
                    }
                }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val lineStats = busRepository.getLineStats()
                val systemStatus = busRepository.getSystemStatus()

                _uiState.update {
                    it.copy(
                        lineStats = lineStats,
                        systemStatus = systemStatus,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Errore sconosciuto"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadData()

        // Forza riconnessione MQTT se disponibile
        viewModelScope.launch {
            mqttRepository?.let { mqtt ->
                try {
                    mqtt.reconnect()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = "Reconnect failed: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Ottieni statistiche MQTT per debug
     */
    fun getMqttStats(): com.vibus.live.data.mqtt.MqttConnectionStats? {
        return mqttRepository?.getMqttStats()
    }

    /**
     * Forza riconnessione MQTT
     */
    fun forceMqttReconnect() {
        viewModelScope.launch {
            mqttRepository?.let { mqtt ->
                when (val result = mqtt.reconnect()) {
                    is MqttResult.Success -> {
                        _uiState.update { it.copy(isMqttConnected = true, error = null) }
                    }
                    is MqttResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isMqttConnected = false,
                                error = "Reconnect failed: ${result.error.message}"
                            )
                        }
                    }
                    else -> { /* Loading state */ }
                }
            }
        }
    }

    /**
     * Disconnetti MQTT
     */
    fun disconnectMqtt() {
        viewModelScope.launch {
            mqttRepository?.disconnect()
            _uiState.update { it.copy(isMqttConnected = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mqttRepository?.cleanup()
    }
}

data class DashboardUiState(
    val buses: List<Bus> = emptyList(),
    val lineStats: List<LineStats> = emptyList(),
    val systemStatus: SystemStatus? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isMqttConnected: Boolean = false,
    val lastUpdate: Long = 0L
)