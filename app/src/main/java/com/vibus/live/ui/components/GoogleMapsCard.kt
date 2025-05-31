package com.vibus.live.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
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
import com.vibus.live.ui.theme.*

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

    GradientCard(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surface
        ),
        modifier = modifier,
        cornerRadius = 24
    ) {
        Column {
            // Header migliorato con gradiente
            EnhancedGoogleMapsHeader(
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
                // Fallback migliorato con animazioni
                EnhancedBusGridFallback(
                    buses = buses,
                    error = mapError,
                    modifier = Modifier.height(280.dp)
                )
            } else {
                // Google Maps reale con effetti
                Box(modifier = Modifier.height(280.dp)) {
                    if (buses.isEmpty()) {
                        BeautifulLoadingMapView()
                    } else {
                        EnhancedGoogleMapsView(
                            buses = buses,
                            onMapLoaded = { isMapLoaded = true },
                            onMapError = { error ->
                                Log.e("GoogleMapsCard", "Map error: $error")
                                mapError = error
                                showFallback = true
                            }
                        )
                    }

                    // Overlay informativo migliorato
                    EnhancedMapOverlay(
                        isMapLoaded = isMapLoaded,
                        buses = buses,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedGoogleMapsHeader(
    busCount: Int,
    isMapLoaded: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    onShowConfig: () -> Unit
) {
    val headerColor = when {
        hasError -> MaterialTheme.colorScheme.errorContainer
        isMapLoaded -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        headerColor,
                        headerColor.copy(alpha = 0.7f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconColor = when {
                hasError -> MaterialTheme.colorScheme.error
                isMapLoaded -> StatusGreen
                else -> MaterialTheme.colorScheme.primary
            }

            PulsingIcon(
                icon = when {
                    hasError -> Icons.Default.ErrorOutline
                    isMapLoaded -> Icons.Default.Map
                    else -> Icons.Default.HourglassEmpty
                },
                tint = iconColor,
                size = 32
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Mappa Tempo Reale",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        hasError -> "Errore nel caricamento della mappa"
                        isMapLoaded -> "$busCount autobus visualizzati"
                        else -> "Caricamento Google Maps..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasError) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionCardButton(
                    icon = Icons.Default.Info,
                    label = "Info",
                    onClick = onShowConfig,
                    backgroundColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.height(36.dp)
                )

                FloatingActionCardButton(
                    icon = Icons.Default.Refresh,
                    label = "Riprova",
                    onClick = onRetry,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(36.dp)
                )
            }
        }
    }
}

@Composable
private fun EnhancedGoogleMapsView(
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

    // Map properties migliorati
    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = false,
                isTrafficEnabled = false,
                mapType = MapType.NORMAL,
                isIndoorEnabled = false,
                mapStyleOptions = null // Qui potresti aggiungere uno stile personalizzato
            )
        )
    }

    // Map UI settings ottimizzati
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

    // Timeout per considerare la mappa fallita
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
            Log.d("GoogleMapsCard", "Updating enhanced map with ${buses.size} buses")
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = {
            Log.d("GoogleMapsCard", "Enhanced Google Maps loaded successfully!")
            mapLoadedSuccessfully = true
            onMapLoaded()
        },
        onMapClick = { latLng ->
            Log.d("GoogleMapsCard", "Enhanced map clicked at: $latLng")
        }
    ) {
        // Aggiungi marker migliorati per ogni autobus
        buses.forEach { bus ->
            val position = LatLng(bus.position.latitude, bus.position.longitude)
            val markerColor = when (bus.line) {
                "1" -> BitmapDescriptorFactory.HUE_RED
                "2" -> BitmapDescriptorFactory.HUE_BLUE
                "3" -> BitmapDescriptorFactory.HUE_CYAN
                "5" -> BitmapDescriptorFactory.HUE_VIOLET
                "7" -> BitmapDescriptorFactory.HUE_ORANGE
                else -> BitmapDescriptorFactory.HUE_GREEN
            }

            Marker(
                state = MarkerState(position = position),
                title = "🚌 ${bus.displayName}",
                snippet = buildString {
                    append("${bus.lineName}\n")
                    append("⚡ ${String.format("%.1f", bus.speed)} km/h\n")
                    append("👥 ${bus.passengers} passeggeri\n")
                    append("⏱️ ${bus.delayText}\n")
                    append("🕐 ${bus.lastUpdate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))}")
                },
                icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                alpha = 0.9f,
                onClick = { marker ->
                    Log.d("GoogleMapsCard", "Enhanced marker clicked: ${marker.title}")
                    false // Ritorna false per mostrare l'info window
                }
            )
        }
    }
}

@Composable
private fun BeautifulLoadingMapView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WaveLoadingIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🌍 Inizializzazione Google Maps...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Caricamento posizioni autobus SVT",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnhancedMapOverlay(
    isMapLoaded: Boolean,
    buses: List<Bus>,
    modifier: Modifier = Modifier
) {
    // Mostra solo se ci sono autobus e la mappa è caricata
    if (buses.isNotEmpty() && isMapLoaded) {
        val lastUpdate = buses.maxByOrNull { it.lastUpdate }?.lastUpdate

        Surface(
            modifier = modifier.padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "🔄 ${lastUpdate?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "---"}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun EnhancedBusGridFallback(
    buses: List<Bus>,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp)
    ) {
        // Error message con stile migliorato
        if (error != null) {
            GradientCard(
                colors = ErrorGradient,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "⚠️ Google Maps non disponibile",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Mostrando vista alternativa degli autobus",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Grid di autobus migliorata
        if (buses.isNotEmpty()) {
            EnhancedSimpleBusGrid(
                buses = buses,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PulsingIcon(
                        icon = Icons.Default.DirectionsBus,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 48
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Nessun autobus disponibile",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedSimpleBusGrid(
    buses: List<Bus>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        itemsIndexed(
            items = buses,
            key = { _, bus -> bus.id }
        ) { index, bus ->
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 50,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 50
                    )
                )
            ) {
                EnhancedBusGridItem(bus = bus)
            }
        }
    }
}

@Composable
private fun EnhancedBusGridItem(
    bus: Bus,
    modifier: Modifier = Modifier
) {
    val lineColor = getLineColor(bus.line)
    val statusColor = getStatusColor(bus.delay)

    GradientCard(
        colors = getLineGradient(bus.line),
        modifier = modifier,
        cornerRadius = 16
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicatore linea con animazione
            AnimatedLineIndicator(
                line = bus.line,
                color = lineColor,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ID autobus
            Text(
                text = bus.id,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Nome linea
            Text(
                text = bus.lineName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Statistiche compatte
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactStatChip(
                    icon = Icons.Default.Speed,
                    value = "${bus.speed.toInt()}",
                    color = StatusBlue
                )
                CompactStatChip(
                    icon = Icons.Default.Person,
                    value = "${bus.passengers}",
                    color = StatusGreen
                )
            }

            // Badge ritardo se presente
            if (bus.delay != 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusBadge(
                    text = bus.delayText,
                    color = statusColor,
                    animated = bus.delay > 2.0,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun AnimatedLineIndicator(
    line: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "line-indicator")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "border"
    )

    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = color.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = line,
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}