package com.vibus.live.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import com.vibus.live.data.Bus
import com.vibus.live.data.delayText
import com.vibus.live.data.displayName
import com.vibus.live.ui.screens.dashboard.DashboardViewModel
import com.vibus.live.ui.components.*
import com.vibus.live.ui.theme.*
import com.vibus.live.utils.MapsConfigChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMapScreen(
    selectedBusId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isMapLoaded by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var currentSelectedBusId by remember { mutableStateOf(selectedBusId) }

    // Check Google Maps configuration
    LaunchedEffect(Unit) {
        val configResult = MapsConfigChecker.checkGoogleMapsConfiguration(context)
        if (configResult !is com.vibus.live.utils.MapsConfigResult.ValidApiKey) {
            mapError = "Google Maps non configurato"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mappa SVT",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${uiState.buses.size} autobus attivi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // Badge con ultimo aggiornamento
                    if (uiState.buses.isNotEmpty()) {
                        val lastUpdate = uiState.buses.maxByOrNull { it.lastUpdate }?.lastUpdate
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "🔄 ${lastUpdate?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "---"}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (mapError != null || uiState.buses.isEmpty()) {
                // Fallback se Maps non funziona
                FullscreenMapFallback(
                    buses = uiState.buses,
                    error = mapError,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Mappa fullscreen
                FullscreenGoogleMap(
                    buses = uiState.buses,
                    selectedBusId = currentSelectedBusId,
                    onMapLoaded = { isMapLoaded = true },
                    onMapError = { error -> mapError = error },
                    onBusSelected = { busId -> currentSelectedBusId = busId },
                    onMapClick = { currentSelectedBusId = null }, // Deseleziona quando si clicca sulla mappa
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating info panel - Compatto e posizionato meglio
            if (currentSelectedBusId != null && isMapLoaded) {
                val selectedBus = uiState.buses.find { it.id == currentSelectedBusId }
                selectedBus?.let { bus ->
                    SelectedBusInfoPanel(
                        bus = bus,
                        onDismiss = { currentSelectedBusId = null },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenGoogleMap(
    buses: List<Bus>,
    selectedBusId: String?,
    onMapLoaded: () -> Unit,
    onMapError: (String) -> Unit,
    onBusSelected: (String) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vicenzaCenter = LatLng(45.5477, 11.5458)

    // Se c'è un autobus selezionato, centra su quello
    val selectedBus = buses.find { it.id == selectedBusId }
    val initialPosition = selectedBus?.let {
        LatLng(it.position.latitude, it.position.longitude)
    } ?: vicenzaCenter

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, if (selectedBus != null) 15f else 12f)
    }

    // Aggiorna la camera quando cambia l'autobus selezionato
    LaunchedEffect(selectedBusId) {
        selectedBus?.let { bus ->
            val newPosition = LatLng(bus.position.latitude, bus.position.longitude)
            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(newPosition, 15f)
                ),
                durationMs = 1000
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false,
            isTrafficEnabled = true, // Abilita traffico per mappa fullscreen
            mapType = MapType.NORMAL
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = true,
            myLocationButtonEnabled = false
        ),
        onMapLoaded = onMapLoaded,
        onMapClick = { onMapClick() } // Deseleziona quando si clicca sulla mappa
    ) {
        buses.forEach { bus ->
            val position = LatLng(bus.position.latitude, bus.position.longitude)
            val isSelected = bus.id == selectedBusId

            Marker(
                state = MarkerState(position = position),
                title = "🚌 ${bus.displayName}",
                snippet = buildString {
                    append("${bus.lineName}\n")
                    append("⚡ ${String.format("%.1f", bus.speed)} km/h\n")
                    append("👥 ${bus.passengers} passeggeri\n")
                    append("⏱️ ${bus.delayText}")
                },
                icon = BitmapDescriptorFactory.defaultMarker(
                    when (bus.line) {
                        "1" -> BitmapDescriptorFactory.HUE_RED
                        "2" -> BitmapDescriptorFactory.HUE_BLUE
                        "3" -> BitmapDescriptorFactory.HUE_CYAN
                        "5" -> BitmapDescriptorFactory.HUE_VIOLET
                        "7" -> BitmapDescriptorFactory.HUE_ORANGE
                        else -> BitmapDescriptorFactory.HUE_GREEN
                    }
                ),
                alpha = if (isSelected) 1.0f else 0.8f,
                zIndex = if (isSelected) 1.0f else 0.0f,
                onClick = {
                    onBusSelected(bus.id)
                    false // Non mostrare l'info window di default
                }
            )
        }
    }
}

@Composable
private fun SelectedBusInfoPanel(
    bus: Bus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column {
            // Header con pulsante chiudi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Indicatore linea compatto
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = getLineColor(bus.line),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bus.line,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = bus.id,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = bus.lineName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Pulsante chiudi
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Statistiche compatte
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactStatItem(
                    icon = Icons.Default.Speed,
                    label = "Velocità",
                    value = "${bus.speed.toInt()} km/h",
                    color = StatusBlue
                )

                CompactStatItem(
                    icon = Icons.Default.Person,
                    label = "Passeggeri",
                    value = "${bus.passengers}",
                    color = StatusGreen
                )

                CompactStatItem(
                    icon = Icons.Default.Schedule,
                    label = "Ritardo",
                    value = bus.delayText,
                    color = getStatusColor(bus.delay)
                )
            }
        }
    }
}

@Composable
private fun CompactStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FullscreenMapFallback(
    buses: List<Bus>,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (error != null) {
            GradientCard(
                colors = ErrorGradient,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Mappa non disponibile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (buses.isNotEmpty()) {
            Text(
                text = "Vista Alternativa Autobus",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            SimpleBusGrid(
                buses = buses,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}