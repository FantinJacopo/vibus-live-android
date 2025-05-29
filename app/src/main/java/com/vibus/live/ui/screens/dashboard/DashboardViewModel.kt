package com.vibus.live.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibus.live.data.Bus
import com.vibus.live.data.LineStats
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.repository.BusRepository
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
}

data class DashboardUiState(
    val buses: List<Bus> = emptyList(),
    val lineStats: List<LineStats> = emptyList(),
    val systemStatus: SystemStatus? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)