package com.vibus.live.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibus.live.data.Bus
import com.vibus.live.data.LineStats
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.mqtt.MqttConnectionState
import com.vibus.live.data.repository.BusRepository
import com.vibus.live.data.repository.MqttBusRepository
import com.vibus.live.data.repository.MqttOnlyBusRepository
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

    val mqttConnectionState: StateFlow<MqttConnectionState> = if (busRepository is MqttOnlyBusRepository) {
        busRepository.getMqttConnectionState().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = MqttConnectionState.Disconnected
        )
    } else {
        flowOf(MqttConnectionState.Disconnected).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = MqttConnectionState.Disconnected
        )
    }

    init {
        loadData()
        startRealTimeUpdates()
    }

    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            busRepository.getRealTimeBuses()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { buses ->
                    _uiState.update {
                        it.copy(
                            buses = buses,
                            isLoading = false,
                            error = null
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
    }

    override fun onCleared() {
        super.onCleared()
        // NON disconnettere MQTT quando il ViewModel viene distrutto
        // MQTT rimane connesso a livello di applicazione
        Log.d("DashboardViewModel", "ViewModel cleared but MQTT connection preserved")
    }
}

data class DashboardUiState(
    val buses: List<Bus> = emptyList(),
    val lineStats: List<LineStats> = emptyList(),
    val systemStatus: SystemStatus? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)