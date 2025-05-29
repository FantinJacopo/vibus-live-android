package com.vibus.live.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import com.vibus.live.data.Bus
import com.vibus.live.data.delayText
import com.vibus.live.data.displayName
import com.vibus.live.utils.MapsConfigChecker


@Composable
private fun GoogleMapsHeader(
    busCount: Int,
    isMapLoaded: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    onShowConfig: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    hasError -> MaterialTheme.colorScheme.errorContainer
                    isMapLoaded -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Google Maps - Autobus SVT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    hasError -> "Errore configurazione Google Maps"
                    isMapLoaded -> "$busCount autobus visualizzati"
                    else -> "Caricamento Google Maps..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            if (hasError) {
                IconButton(onClick = onShowConfig) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info configurazione",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Riprova",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                imageVector = when {
                    hasError -> Icons.Default.Error
                    isMapLoaded -> Icons.Default.Map
                    else -> Icons.Default.HourglassEmpty
                },
                contentDescription = null,
                tint = when {
                    hasError -> MaterialTheme.colorScheme.error
                    isMapLoaded -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun RealGoogleMapsCard(
    buses: List<Bus>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFallback by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    // Check Google Maps configuration once
    LaunchedEffect(Unit) {
        val configReport = MapsConfigChecker.getConfigurationReport(context)
        Log.d("GoogleMapsCard", "Configuration report:\n$configReport")

        val configResult = MapsConfigChecker.checkGoogleMapsConfiguration(context)
        if (configResult !is com.vibus.live.utils.MapsConfigResult.ValidApiKey) {
            mapError = "Configurazione Google Maps non valida"
            showFallback = true
        }
    }

    LaunchedEffect(buses) {
        Log.d("GoogleMapsCard", "Received ${buses.size} buses for Google Maps")
        buses.forEach { bus ->
            Log.d("GoogleMapsCard", "Bus: ${bus.id} at ${bus.position.latitude}, ${bus.position.longitude}")
        }
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Header
            GoogleMapsHeader(
                busCount = buses.size,
                isMapLoaded = isMapLoaded,
                hasError = mapError != null,
                onRetry = {
                    showFallback = false
                    mapError = null
                    isMapLoaded = false
                },
                onShowConfig = {
                    val report = MapsConfigChecker.getConfigurationReport(context)
                    Log.i("GoogleMapsCard", "Config on demand:\n$report")
                }
            )

            if (showFallback || mapError != null) {
                // Fallback: SimpleBusGrid
                SimpleBusGridFallback(
                    buses = buses,
                    error = mapError,
                    modifier = Modifier.height(280.dp)
                )
            } else {
                // Google Maps reale
                Box(modifier = Modifier.height(280.dp)) {
                    if (buses.isEmpty()) {
                        LoadingMapView()
                    } else {
                        GoogleMapsView(
                            buses = buses,
                            onMapLoaded = { isMapLoaded = true },
                            onMapError = { error ->
                                Log.e("GoogleMapsCard", "Map error: $error")
                                mapError = error
                                showFallback = true
                            }
                        )
                    }

                    // Debug overlay con timestamp aggiornamenti
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "Maps: ${if (isMapLoaded) "✓" else "..."} | Buses: ${buses.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (buses.isNotEmpty()) {
                                val lastUpdate = buses.maxByOrNull { it.lastUpdate }?.lastUpdate
                                Text(
                                    text = "Last: ${lastUpdate?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "---"}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleMapsHeader(
    busCount: Int,
    isMapLoaded: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    hasError -> MaterialTheme.colorScheme.errorContainer
                    isMapLoaded -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Google Maps - Autobus SVT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    hasError -> "Errore caricamento mappa"
                    isMapLoaded -> "$busCount autobus visualizzati"
                    else -> "Caricamento mappa..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            if (hasError) {
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Riprova"
                    )
                }
            }

            Icon(
                imageVector = when {
                    hasError -> Icons.Default.Error
                    isMapLoaded -> Icons.Default.Map
                    else -> Icons.Default.HourglassEmpty
                },
                contentDescription = null,
                tint = when {
                    hasError -> MaterialTheme.colorScheme.error
                    isMapLoaded -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun GoogleMapsView(
    buses: List<Bus>,
    onMapLoaded: () -> Unit,
    onMapError: (String) -> Unit
) {
    val context = LocalContext.current
    val vicenzaCenter = LatLng(45.5477, 11.5458)
    var hasErrorOccurred by remember { mutableStateOf(false) }
    var mapLoadedSuccessfully by remember { mutableStateOf(false) }

    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(vicenzaCenter, 12f)
    }

    // Map properties
    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = false,
                isTrafficEnabled = false,
                mapType = MapType.NORMAL,
                isIndoorEnabled = false
            )
        )
    }

    // Map UI settings
    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                tiltGesturesEnabled = true,
                rotationGesturesEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false
            )
        )
    }

    // Timeout per considerare la mappa fallita (solo se non è già caricata)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(15000) // 15 secondi
        if (!hasErrorOccurred && !mapLoadedSuccessfully) {
            hasErrorOccurred = true
            onMapError("Timeout: Google Maps potrebbe non essere configurato correttamente")
        }
    }

    // Log degli aggiornamenti autobus
    LaunchedEffect(buses) {
        if (mapLoadedSuccessfully) {
            Log.d("GoogleMapsCard", "Updating map with ${buses.size} buses")
            buses.forEach { bus ->
                Log.d("GoogleMapsCard", "Updating bus: ${bus.id} at ${bus.position.latitude}, ${bus.position.longitude}")
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = {
            Log.d("GoogleMapsCard", "Google Maps loaded successfully!")
            mapLoadedSuccessfully = true
            onMapLoaded()
        },
        onMapClick = { latLng ->
            Log.d("GoogleMapsCard", "Map clicked at: $latLng")
        }
    ) {
        // Aggiungi marker per ogni autobus - i marker si aggiornano automaticamente
        // quando la lista buses cambia
        buses.forEach { bus ->
            val position = LatLng(bus.position.latitude, bus.position.longitude)

            Marker(
                state = MarkerState(position = position),
                title = bus.displayName,
                snippet = buildString {
                    append("${bus.lineName}\n")
                    append("Velocità: ${String.format("%.1f", bus.speed)} km/h\n")
                    append("Passeggeri: ${bus.passengers}\n")
                    append("Ritardo: ${bus.delayText}\n")
                    append("Aggiornato: ${bus.lastUpdate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))}")
                },
                // Personalizza il colore del marker
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
                onClick = { marker ->
                    Log.d("GoogleMapsCard", "Marker clicked: ${marker.title}")
                    false // Ritorna false per mostrare l'info window
                }
            )
        }
    }
}

@Composable
private fun LoadingMapView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Inizializzazione Google Maps...",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Attendere dati autobus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimpleBusGridFallback(
    buses: List<Bus>,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Error message
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ $error\nMostrando vista alternativa:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Simplified bus grid
        if (buses.isNotEmpty()) {
            SimpleBusGrid(
                buses = buses,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}