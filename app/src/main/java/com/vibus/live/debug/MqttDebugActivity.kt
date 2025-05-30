package com.vibus.live.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vibus.live.data.mqtt.*
import com.vibus.live.data.repository.MqttBusRepository
import com.vibus.live.service.MqttBackgroundService
import com.vibus.live.ui.screens.dashboard.DashboardViewModel
import com.vibus.live.ui.theme.ViBusLiveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class MqttDebugActivity : ComponentActivity() {

    @Inject
    lateinit var mqttRepository: MqttBusRepository

    @Inject
    lateinit var mqttManager: MqttManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ViBusLiveTheme {
                MqttDebugScreen(
                    mqttRepository = mqttRepository,
                    mqttManager = mqttManager,
                    onStartService = {
                        MqttBackgroundService.startService(this)
                    },
                    onStopService = {
                        MqttBackgroundService.stopService(this)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttDebugScreen(
    mqttRepository: MqttBusRepository,
    mqttManager: MqttManager,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val connectionState by mqttManager.connectionState.collectAsStateWithLifecycle()

    // Fix per eventi e messaggi - usa remember con default
    var connectionEvents by remember { mutableStateOf<List<MqttConnectionEvent>>(emptyList()) }
    var messages by remember { mutableStateOf<List<MqttMessage>>(emptyList()) }

    // Collect events manualmente
    LaunchedEffect(Unit) {
        mqttManager.connectionEvents.collect { event ->
            connectionEvents = (connectionEvents + event).takeLast(20)
        }
    }

    LaunchedEffect(Unit) {
        mqttManager.messages.collect { message ->
            messages = (messages + message).takeLast(50)
        }
    }
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var connectionStats by remember { mutableStateOf<MqttConnectionStats?>(null) }
    var isServiceRunning by remember { mutableStateOf(false) }

    // Aggiorna statistiche periodicamente
    LaunchedEffect(Unit) {
        while (true) {
            connectionStats = mqttRepository.getMqttStats()
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MQTT Debug Monitor") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            connectionStats = mqttRepository.getMqttStats()
                        }
                    }) {
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
            // Service Controls
            item {
                ServiceControlCard(
                    isServiceRunning = isServiceRunning,
                    onStartService = {
                        onStartService()
                        isServiceRunning = true
                    },
                    onStopService = {
                        onStopService()
                        isServiceRunning = false
                    }
                )
            }

            // Connection Status
            item {
                ConnectionStatusCard(
                    connectionState = connectionState,
                    connectionStats = connectionStats
                )
            }

            // Repository Actions
            item {
                RepositoryActionsCard(
                    mqttRepository = mqttRepository,
                    scope = scope
                )
            }

            // Data Status
            item {
                DataStatusCard(
                    busCount = uiState.buses.size,
                    lineStatsCount = uiState.lineStats.size,
                    systemStatus = uiState.systemStatus,
                    isLoading = uiState.isLoading,
                    error = uiState.error
                )
            }

            // Connection Events
            item {
                Text(
                    text = "Connection Events (${connectionEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(connectionEvents.take(10)) { event ->
                ConnectionEventCard(event = event)
            }

            // Recent Messages
            item {
                Text(
                    text = "Recent MQTT Messages (${messages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(messages.take(20)) { message ->
                MqttMessageCard(message = message)
            }
        }
    }
}

@Composable
fun ServiceControlCard(
    isServiceRunning: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MQTT Background Service",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartService,
                    enabled = !isServiceRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Service")
                }

                Button(
                    onClick = onStopService,
                    enabled = isServiceRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop Service")
                }
            }

            Text(
                text = if (isServiceRunning) "🟢 Service Running" else "🔴 Service Stopped",
                style = MaterialTheme.typography.bodySmall,
                color = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: MqttConnectionState,
    connectionStats: MqttConnectionStats?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                MqttConnectionState.CONNECTED -> Color(0xFFE8F5E8)
                MqttConnectionState.ERROR -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (connectionState) {
                        MqttConnectionState.CONNECTED -> Icons.Default.CheckCircle
                        MqttConnectionState.CONNECTING -> Icons.Default.HourglassEmpty
                        MqttConnectionState.RECONNECTING -> Icons.Default.Refresh
                        MqttConnectionState.ERROR -> Icons.Default.Error
                        MqttConnectionState.DISCONNECTED -> Icons.Default.Cancel
                    },
                    contentDescription = null,
                    tint = when (connectionState) {
                        MqttConnectionState.CONNECTED -> Color(0xFF4CAF50)
                        MqttConnectionState.ERROR -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "MQTT Connection: ${connectionState.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            connectionStats?.let { stats ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatRow("Broker", stats.brokerHost)
                    StatRow("Client ID", stats.clientId)
                    StatRow("Uptime", "${stats.connectionUptime / 1000}s")
                    StatRow("Messages", "${stats.messagesReceived}")
                    StatRow("Lost", "${stats.messagesLost}")
                    StatRow("Reconnects", "${stats.reconnectCount}")

                    if (stats.lastError != null) {
                        StatRow("Last Error", stats.lastError, isError = true)
                    }
                }
            }
        }
    }
}

@Composable
fun RepositoryActionsCard(
    mqttRepository: MqttBusRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Repository Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            mqttRepository.initialize()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Initialize")
                }

                Button(
                    onClick = {
                        scope.launch {
                            mqttRepository.reconnect()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reconnect")
                }

                Button(
                    onClick = {
                        scope.launch {
                            mqttRepository.disconnect()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
fun DataStatusCard(
    busCount: Int,
    lineStatsCount: Int,
    systemStatus: com.vibus.live.data.SystemStatus?,
    isLoading: Boolean,
    error: String?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Data Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading data...")
                }
            } else if (error != null) {
                Text(
                    text = "❌ Error: $error",
                    color = Color(0xFFF44336),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatRow("Buses", "$busCount")
                    StatRow("Line Stats", "$lineStatsCount")
                    StatRow("System Status", if (systemStatus != null) "Available" else "Unavailable")

                    systemStatus?.let { status ->
                        StatRow("Active Buses", "${status.activeBuses}/${status.totalBuses}")
                        StatRow("Total Passengers", "${status.totalPassengers}")
                        StatRow("System Health", status.systemHealth.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionEventCard(
    event: MqttConnectionEvent
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.state) {
                MqttConnectionState.CONNECTED -> Color(0xFFE8F5E8)
                MqttConnectionState.ERROR -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.state.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = event.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (event.message != null) {
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (event.error != null) {
                Text(
                    text = "Error: ${event.error.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun MqttMessageCard(
    message: MqttMessage
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.topic,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "QoS:${message.qos}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message.payload.take(100) + if (message.payload.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isError) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
        )
    }
}