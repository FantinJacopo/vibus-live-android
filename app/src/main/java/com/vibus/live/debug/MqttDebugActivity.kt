package com.vibus.live.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibus.live.data.mqtt.MqttConnectionState
import com.vibus.live.data.repository.MqttBusRepository
import com.vibus.live.ui.screens.dashboard.DashboardViewModel
import com.vibus.live.ui.theme.ViBusLiveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MqttDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ViBusLiveTheme {
                MqttDebugScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttDebugScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mqttState by viewModel.mqttConnectionState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug - MQTT Connection") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MqttConnectionCard(connectionState = mqttState)
            }

            item {
                MqttConfigurationCard()
            }

            item {
                DataSourceCard(
                    mqttState = mqttState,
                    busCount = uiState.buses.size,
                    isLoading = uiState.isLoading,
                    error = uiState.error
                )
            }

            if (uiState.buses.isNotEmpty()) {
                item {
                    Text(
                        text = "Autobus Ricevuti (${uiState.buses.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.buses) { bus ->
                    BusDataCard(bus = bus)
                }
            }
        }
    }
}

@Composable
fun MqttConnectionCard(connectionState: MqttConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is MqttConnectionState.Connected -> Color(0xFFE8F5E8)
                is MqttConnectionState.Error -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (connectionState) {
                    is MqttConnectionState.Connected -> Icons.Default.CheckCircle
                    is MqttConnectionState.Connecting -> Icons.Default.HourglassEmpty
                    is MqttConnectionState.Error -> Icons.Default.Error
                    is MqttConnectionState.Disconnected -> Icons.Default.WifiOff
                },
                contentDescription = null,
                tint = when (connectionState) {
                    is MqttConnectionState.Connected -> Color(0xFF4CAF50)
                    is MqttConnectionState.Error -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = when (connectionState) {
                        is MqttConnectionState.Connected -> "✅ MQTT Connesso"
                        is MqttConnectionState.Connecting -> "🔄 Connessione MQTT..."
                        is MqttConnectionState.Disconnected -> "⚪ MQTT Disconnesso"
                        is MqttConnectionState.Error -> "❌ Errore MQTT"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when (connectionState) {
                        is MqttConnectionState.Connected -> "Ricevendo dati in tempo reale via MQTT"
                        is MqttConnectionState.Connecting -> "Tentativo di connessione al broker MQTT"
                        is MqttConnectionState.Disconnected -> "Usando fallback HTTP"
                        is MqttConnectionState.Error -> "Errore: ${connectionState.message}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MqttConfigurationCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Configurazione MQTT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ConfigRow("Broker Host", com.vibus.live.data.api.NetworkConfig.CURRENT_MQTT_HOST)
            ConfigRow("Broker Port", com.vibus.live.data.api.NetworkConfig.CURRENT_MQTT_PORT.toString())
            ConfigRow("Topic", com.vibus.live.data.api.NetworkConfig.MQTT_TOPIC_BUS_POSITION)
            ConfigRow("Client ID", "vibus_android_*")
        }
    }
}

@Composable
fun DataSourceCard(
    mqttState: MqttConnectionState,
    busCount: Int,
    isLoading: Boolean,
    error: String?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Stato Sorgente Dati",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        mqttState is MqttConnectionState.Connected -> Icons.Default.Wifi
                        error != null -> Icons.Default.Error
                        isLoading -> Icons.Default.HourglassEmpty
                        else -> Icons.Default.Http
                    },
                    contentDescription = null,
                    tint = when {
                        mqttState is MqttConnectionState.Connected -> Color(0xFF4CAF50)
                        error != null -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when {
                        mqttState is MqttConnectionState.Connected -> "Dati MQTT in tempo reale"
                        error != null -> "Errore di connessione"
                        isLoading -> "Caricamento dati..."
                        else -> "Dati HTTP (fallback)"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (busCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$busCount autobus ricevuti",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Errore: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun BusDataCard(bus: com.vibus.live.data.Bus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${bus.id} - Linea ${bus.line}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = bus.lastUpdate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ConfigRow("Posizione", "${bus.position.latitude}, ${bus.position.longitude}")
            ConfigRow("Velocità", "${bus.speed} km/h")
            ConfigRow("Passeggeri", "${bus.passengers}")
            ConfigRow("Ritardo", "${bus.delay} min")
        }
    }
}