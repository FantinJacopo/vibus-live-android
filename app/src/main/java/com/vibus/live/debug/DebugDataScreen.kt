package com.vibus.live.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibus.live.data.Bus
import com.vibus.live.ui.screens.dashboard.DashboardViewModel
import com.vibus.live.ui.theme.getLineColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDataScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug - Dati InfluxDB") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status generale
            item {
                StatusCard(
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    busCount = uiState.buses.size
                )
            }

            // Lista dettagliata autobus
            if (uiState.buses.isNotEmpty()) {
                item {
                    Text(
                        text = "Dati Autobus (${uiState.buses.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.buses) { bus ->
                    BusDebugCard(bus = bus)
                }
            } else if (!uiState.isLoading) {
                item {
                    EmptyDataCard()
                }
            }

            // Statistiche
            if (uiState.lineStats.isNotEmpty()) {
                item {
                    Text(
                        text = "Statistiche Linee (${uiState.lineStats.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                items(uiState.lineStats) { stats ->
                    LineStatsDebugCard(stats = stats)
                }
            }

            // System Status
            uiState.systemStatus?.let { systemStatus ->
                item {
                    Text(
                        text = "Stato Sistema",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                item {
                    SystemStatusDebugCard(systemStatus = systemStatus)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    isLoading: Boolean,
    error: String?,
    busCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                error != null -> MaterialTheme.colorScheme.errorContainer
                isLoading -> MaterialTheme.colorScheme.primaryContainer
                busCount > 0 -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Status Connessione",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Caricamento dati...")
                    }
                }
                error != null -> {
                    Text(
                        text = "Errore: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                busCount > 0 -> {
                    Text(
                        text = "✅ Connesso - $busCount autobus ricevuti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                else -> {
                    Text(
                        text = "⚠️ Nessun dato ricevuto",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun BusDebugCard(
    bus: Bus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getLineColor(bus.line).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "${bus.id} - Linea ${bus.line}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            DebugDataRow("Nome Linea", bus.lineName)
            DebugDataRow("Posizione", "${bus.position.latitude}, ${bus.position.longitude}")
            DebugDataRow("Velocità", "${bus.speed} km/h")
            DebugDataRow("Direzione", "${bus.bearing}°")
            DebugDataRow("Ritardo", "${bus.delay} min")
            DebugDataRow("Passeggeri", "${bus.passengers}")
            DebugDataRow("Stato", bus.status.toString())
            DebugDataRow("Ultimo Aggiornamento", bus.lastUpdate.toString())
        }
    }
}

@Composable
private fun LineStatsDebugCard(
    stats: com.vibus.live.data.LineStats
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Statistiche Linea ${stats.line}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            DebugDataRow("Autobus Attivi", "${stats.activeBuses}")
            DebugDataRow("Velocità Media", "${stats.averageSpeed} km/h")
            DebugDataRow("Ritardo Medio", "${stats.averageDelay} min")
            DebugDataRow("Ritardo Massimo", "${stats.maxDelay} min")
            DebugDataRow("Puntualità", "${stats.onTimePercentage}%")
            DebugDataRow("Passeggeri Totali", "${stats.totalPassengers}")
        }
    }
}

@Composable
private fun SystemStatusDebugCard(
    systemStatus: com.vibus.live.data.SystemStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Stato Sistema SVT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            DebugDataRow("Autobus Totali", "${systemStatus.totalBuses}")
            DebugDataRow("Autobus Attivi", "${systemStatus.activeBuses}")
            DebugDataRow("Passeggeri Totali", "${systemStatus.totalPassengers}")
            DebugDataRow("Ritardo Sistema", "${systemStatus.averageSystemDelay} min")
            DebugDataRow("Salute Sistema", systemStatus.systemHealth.toString())
        }
    }
}

@Composable
private fun EmptyDataCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📊",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nessun dato disponibile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verifica che Node-RED stia simulando i dati",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DebugDataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}