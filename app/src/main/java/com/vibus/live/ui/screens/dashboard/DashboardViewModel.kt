package com.vibus.live.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibus.live.data.Bus
import com.vibus.live.data.LineStats
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.mqtt.MqttConnectionState
import com.vibus.live.data.repository.BusRepository
import com.vibus.live.data.repository.MqttOnlyBusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val busRepository: BusRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

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
        Log.d(TAG, "Initializing DashboardViewModel with real-time flows...")
        startRealTimeStreams()
    }

    // ===== NUOVO: STREAMS REAL-TIME AUTOMATICI =====
    private fun startRealTimeStreams() {
        // 1. Stream autobus real-time (esistente)
        viewModelScope.launch {
            Log.d(TAG, "Starting real-time bus positions stream...")
            busRepository.getRealTimeBuses()
                .catch { e ->
                    Log.e(TAG, "Error in bus positions stream", e)
                    _uiState.update { it.copy(error = "Errore posizioni autobus: ${e.message}") }
                }
                .collect { buses ->
                    Log.d(TAG, "Received ${buses.size} buses from real-time stream")
                    _uiState.update {
                        it.copy(
                            buses = buses,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }

        // 2. NUOVO: Stream statistiche linee real-time
        viewModelScope.launch {
            Log.d(TAG, "Starting real-time line stats stream...")
            busRepository.getRealTimeLineStats()
                .catch { e ->
                    Log.e(TAG, "Error in line stats stream", e)
                    // Non aggiornare l'errore principale, solo loggare
                }
                .collect { lineStats ->
                    Log.d(TAG, "Received ${lineStats.size} line stats from real-time stream")
                    _uiState.update {
                        it.copy(lineStats = lineStats)
                    }
                }
        }

        // 3. NUOVO: Stream stato sistema real-time
        viewModelScope.launch {
            Log.d(TAG, "Starting real-time system status stream...")
            busRepository.getRealTimeSystemStatus()
                .catch { e ->
                    Log.e(TAG, "Error in system status stream", e)
                    // Non aggiornare l'errore principale, solo loggare
                }
                .collect { systemStatus ->
                    Log.d(TAG, "Received system status from real-time stream: $systemStatus")
                    _uiState.update {
                        it.copy(systemStatus = systemStatus)
                    }
                }
        }
    }

    // ===== METODO REFRESH (ora opzionale) =====
    fun refresh() {
        Log.d(TAG, "Manual refresh requested - real-time streams should handle updates automatically")

        // Con i flow real-time, il refresh manuale serve principalmente per
        // resettare errori o forzare una riconnessione
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Forza reconnect MQTT se necessario
                if (busRepository is MqttOnlyBusRepository) {
                    // I flow real-time si riconnetteranno automaticamente
                    Log.d(TAG, "Manual refresh - MQTT streams will reconnect if needed")
                }

                // Reset loading state - i flow real-time aggiorneranno i dati
                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                Log.e(TAG, "Error during manual refresh", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Errore durante il refresh: ${e.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared - MQTT connection preserved for app-level persistence")
        // MQTT rimane connesso a livello di applicazione
    }

    // ===== DEBUG HELPER =====
    /*fun getDebugInfo(): String {
        return if (busRepository is MqttOnlyBusRepository) {
            busRepository.getDebugInfo()
        } else {
            "Repository type: ${busRepository::class.simpleName}"
        }
    }*/
}

data class DashboardUiState(
    val buses: List<Bus> = emptyList(),
    val lineStats: List<LineStats> = emptyList(),
    val systemStatus: SystemStatus? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)