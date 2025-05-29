package com.vibus.live.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vibus.live.data.Bus
import com.vibus.live.data.delayText
import com.vibus.live.data.displayName
import com.vibus.live.ui.theme.getLineColor

@Composable
fun BusMapCard(
    buses: List<Bus>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (buses.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Caricamento mappa...")
                    }
                }
            } else {
                // Google Maps
                val vicenzaCenter = LatLng(45.5477, 11.5458)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(vicenzaCenter, 13f)
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        isTrafficEnabled = false
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        compassEnabled = true,
                        scrollGesturesEnabled = true,
                        zoomGesturesEnabled = true
                    )
                ) {
                    // Add markers for each bus
                    buses.forEach { bus ->
                        val position = LatLng(bus.position.latitude, bus.position.longitude)

                        Marker(
                            state = MarkerState(position = position),
                            title = bus.displayName,
                            snippet = buildString {
                                append("${bus.lineName}\n")
                                append("Velocità: ${String.format("%.1f", bus.speed)} km/h\n")
                                append("Passeggeri: ${bus.passengers}\n")
                                append("Ricardo: ${bus.delayText}")
                            },
                            // You can customize marker appearance here
                            // For now using default markers
                        )
                    }
                }

                // Map overlay with line legend
                MapLegend(
                    buses = buses,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun MapLegend(
    buses: List<Bus>,
    modifier: Modifier = Modifier
) {
    val uniqueLines = buses.map { it.line }.distinct().sorted()

    if (uniqueLines.isNotEmpty()) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Linee Attive",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                uniqueLines.forEach { line ->
                    val busCount = buses.count { it.line == line }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = getLineColor(line),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Linea $line ($busCount)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}