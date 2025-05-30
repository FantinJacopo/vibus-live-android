package com.vibus.live.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vibus.live.ui.theme.ViBusLiveTheme
import com.vibus.live.utils.MapsConfigChecker
import com.vibus.live.utils.MapsConfigResult

class MapsDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ViBusLiveTheme {
                MapsDebugScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsDebugScreen() {
    val context = LocalContext.current
    var configReport by remember { mutableStateOf("") }
    var configResult by remember { mutableStateOf<MapsConfigResult?>(null) }
    var showMap by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        configReport = MapsConfigChecker.getConfigurationReport(context)
        configResult = MapsConfigChecker.checkGoogleMapsConfiguration(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug - Google Maps") },
                actions = {
                    IconButton(onClick = {
                        configReport = MapsConfigChecker.getConfigurationReport(context)
                        configResult = MapsConfigChecker.checkGoogleMapsConfiguration(context)
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
            // Configuration Status
            item {
                ConfigurationStatusCard(configResult = configResult)
            }

            // Configuration Report
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Configuration Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = configReport,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Test Map Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showMap = !showMap },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (showMap) Icons.Default.VisibilityOff else Icons.Default.Map,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showMap) "Nascondi Mappa" else "Testa Mappa")
                    }
                }
            }

            // Test Map
            if (showMap) {
                item {
                    TestMapCard(configResult = configResult)
                }
            }

            // Instructions
            item {
                InstructionsCard(configResult = configResult)
            }
        }
    }
}

@Composable
fun ConfigurationStatusCard(configResult: MapsConfigResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (configResult) {
                is MapsConfigResult.ValidApiKey -> Color(0xFFE8F5E8)
                null -> MaterialTheme.colorScheme.surfaceVariant
                else -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (configResult) {
                    is MapsConfigResult.ValidApiKey -> Icons.Default.CheckCircle
                    null -> Icons.Default.HourglassEmpty
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (configResult) {
                    is MapsConfigResult.ValidApiKey -> Color(0xFF4CAF50)
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = when (configResult) {
                        is MapsConfigResult.ValidApiKey -> "✅ Configurazione OK"
                        is MapsConfigResult.MissingApiKey -> "❌ API Key Mancante"
                        is MapsConfigResult.PlaceholderApiKey -> "⚠️ API Key Placeholder"
                        is MapsConfigResult.TemplateApiKey -> "⚠️ API Key Template"
                        is MapsConfigResult.InvalidApiKey -> "❌ API Key Non Valida"
                        is MapsConfigResult.NoMetaData -> "❌ Meta-data Mancanti"
                        is MapsConfigResult.Error -> "❌ Errore di Configurazione"
                        null -> "🔄 Controllo in corso..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when (configResult) {
                        is MapsConfigResult.ValidApiKey -> "Google Maps dovrebbe funzionare correttamente"
                        is MapsConfigResult.MissingApiKey -> "Aggiungi una Google Maps API Key"
                        is MapsConfigResult.PlaceholderApiKey -> "Sostituisci \${MAPS_API_KEY} con vera API key"
                        is MapsConfigResult.TemplateApiKey -> "Sostituisci il template con vera API key"
                        is MapsConfigResult.InvalidApiKey -> "Verifica che l'API key sia corretta"
                        is MapsConfigResult.NoMetaData -> "Verifica AndroidManifest.xml"
                        is MapsConfigResult.Error -> "Errore: ${configResult.message}"
                        null -> "Verificando la configurazione..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TestMapCard(configResult: MapsConfigResult?) {
    var mapError by remember { mutableStateOf<String?>(null) }
    var mapLoaded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Test Google Maps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (mapLoaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mappa caricata",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }

            // Map or Error
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                when {
                    configResult !is MapsConfigResult.ValidApiKey -> {
                        // Show error message instead of map
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Configurazione non valida",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFF44336)
                                )
                                Text(
                                    text = "Correggi la configurazione prima di testare",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    mapError != null -> {
                        // Show error
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Errore nel caricamento della mappa",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFF44336)
                                )
                                Text(
                                    text = mapError ?: "Errore sconosciuto",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        // Show actual Google Map
                        val vicenza = LatLng(45.5477, 11.5458)
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(vicenza, 12f)
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            onMapLoaded = {
                                mapLoaded = true
                                mapError = null
                            }
                        ) {
                            Marker(
                                state = MarkerState(position = vicenza),
                                title = "Vicenza Centro",
                                snippet = "Test marker per ViBus Live"
                            )
                        }
                    }
                }

                // Loading indicator
                if (!mapLoaded && mapError == null && configResult is MapsConfigResult.ValidApiKey) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Status
            if (mapLoaded) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mappa caricata con successo! Google Maps funziona correttamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun InstructionsCard(configResult: MapsConfigResult?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Istruzioni",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (configResult) {
                is MapsConfigResult.ValidApiKey -> {
                    Text(
                        text = "✅ La configurazione sembra corretta! Se la mappa di test funziona, anche l'app principale dovrebbe funzionare.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50)
                    )
                }
                is MapsConfigResult.MissingApiKey, is MapsConfigResult.PlaceholderApiKey -> {
                    Text(
                        text = """
                            📝 Per risolvere il problema:
                            
                            1. Vai su Google Cloud Console
                            2. Crea un progetto e abilita 'Maps SDK for Android'
                            3. Crea una API Key
                            4. Aggiungi l'API Key al file local.properties:
                               MAPS_API_KEY=la_tua_api_key_qui
                            5. Ricompila l'app
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    Text(
                        text = "Controlla il report di configurazione sopra per i dettagli specifici del problema.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}