package com.vibus.live.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibus.live.data.Bus
import com.vibus.live.ui.theme.*

@Composable
fun SimpleBusGrid(
    buses: List<Bus>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header semplice
        Text(
            text = "🚌 Autobus SVT Live",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${buses.size} autobus in servizio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StatusBadge(
                text = "LIVE",
                color = StatusGreen,
                animated = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid content
        GradientCard(
            colors = SecondaryGradient,
            cornerRadius = 24
        ) {
            if (buses.isEmpty()) {
                // Stato vuoto elegante
                BeautifulEmptyState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else {
                // Grid di autobus con animazioni staggered
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(
                        items = buses,
                        key = { _, bus -> bus.id }
                    ) { index, bus ->
                        this@Column.AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(
                                    durationMillis = 500,
                                    delayMillis = index * 100,
                                    easing = FastOutSlowInEasing
                                )
                            ) + scaleIn(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    delayMillis = index * 100,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    delayMillis = index * 100
                                )
                            )
                        ) {
                            BeautifulBusGridItem(bus = bus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BeautifulGridHeader(
    busCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(PrimaryGradient),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingIcon(
                icon = Icons.Default.GridView,
                tint = Color.White,
                size = 32
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "🚌 Autobus SVT Live",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$busCount autobus in servizio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Indicatore stato in tempo reale
        StatusBadge(
            text = "LIVE",
            color = StatusGreen,
            animated = true
        )
    }
}

@Composable
private fun BeautifulEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icona animata
            val infiniteTransition = rememberInfiniteTransition(label = "empty-state")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "rotation"
            )

            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "🔍 Nessun autobus disponibile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gli autobus appariranno qui non appena saranno attivi",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            WaveLoadingIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun BeautifulBusGridItem(
    bus: Bus,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150), label = "scale"
    )

    val lineColor = getLineColor(bus.line)
    val statusColor = getStatusColor(bus.delay)

    GradientCard(
        colors = getLineGradient(bus.line),
        modifier = modifier.scale(scale),
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicatore linea con pulsazione
            EnhancedLineIndicator(
                line = bus.line,
                color = lineColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Informazioni autobus
            Text(
                text = bus.id,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = bus.lineName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistiche con icone animate
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EnhancedStatChip(
                    icon = Icons.Default.Speed,
                    value = "${bus.speed.toInt()}",
                    unit = "km/h",
                    color = StatusBlue
                )
                EnhancedStatChip(
                    icon = Icons.Default.Person,
                    value = "${bus.passengers}",
                    unit = "",
                    color = StatusGreen
                )
            }

            // Indicatore ritardo se presente
            if (bus.delay != 0.0) {
                Spacer(modifier = Modifier.height(12.dp))

                StatusBadge(
                    text = when {
                        bus.delay > 0 -> "⏰ +${bus.delay.toInt()} min"
                        bus.delay < 0 -> "⚡ ${bus.delay.toInt()} min"
                        else -> "✅ Puntuale"
                    },
                    color = statusColor,
                    animated = bus.delay > 2.0
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Indicatore di movimento
            MovementIndicator(
                bearing = bus.bearing,
                speed = bus.speed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EnhancedLineIndicator(
    line: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "line-indicator")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    Box(
        modifier = modifier
            .size(60.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = glowAlpha),
                        color.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 3.dp,
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = line,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun EnhancedStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )

            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun MovementIndicator(
    bearing: Int,
    speed: Double,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "movement")

    // Fix: Evitiamo divisione per zero e gestiamo speed = 0
    val rotationSpeed = if (speed > 0) {
        (speed / 50f).coerceIn(0.1, 2.0)
    } else {
        0.1 // Velocità minima quando l'autobus è fermo
    }

    val rotationDuration = if (rotationSpeed > 0) {
        (3000 / rotationSpeed).toInt().coerceAtLeast(1000) // Minimo 1000ms
    } else {
        5000 // Durata di default
    }

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = rotationDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = "Direzione movimento",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .rotate(bearing.toFloat() + rotation * 0.2f)
        )
    }
}