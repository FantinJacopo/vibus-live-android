package com.vibus.live.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibus.live.data.Bus
import com.vibus.live.data.LineStats
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.delayText
import com.vibus.live.ui.theme.ErrorGradient
import com.vibus.live.ui.theme.StatusBlue
import com.vibus.live.ui.theme.StatusGreen
import com.vibus.live.ui.theme.getLineColor
import com.vibus.live.ui.theme.getLineGradient
import com.vibus.live.ui.theme.getStatusColor
import java.text.DecimalFormat

@Composable
fun EnhancedBusCard(
    bus: Bus,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150), label = "scale"
    )

    val lineColor = getLineColor(bus.line)
    val statusColor = getStatusColor(bus.delay)

    GradientCard(
        colors = getLineGradient(bus.line),
        modifier = modifier.scale(scale),
        cornerRadius = 20
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicatore linea animato
            AnimatedLineIndicator(
                line = bus.line,
                color = lineColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Informazioni autobus
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bus.id,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = bus.lineName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Statistiche con icone animate
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedStat(
                        icon = Icons.Default.Speed,
                        value = "${DecimalFormat("#.#").format(bus.speed)} km/h",
                        color = StatusBlue
                    )

                    AnimatedStat(
                        icon = Icons.Default.Person,
                        value = "${bus.passengers}",
                        color = StatusGreen
                    )
                }
            }

            // Status e ritardo
            Column(
                horizontalAlignment = Alignment.End
            ) {
                StatusBadge(
                    text = bus.delayText,
                    color = statusColor,
                    animated = bus.delay > 2.0
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Indicatore di movimento
                MovementIndicator(
                    bearing = bus.bearing,
                    speed = bus.speed
                )
            }
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
            .size(56.dp)
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = color.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = line,
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun AnimatedStat(
    icon: ImageVector,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
    val safeSpeed = if (speed > 0.0) speed else 0.1
    val rotationSpeed = (safeSpeed / 50.0).coerceIn(0.1, 2.0)
    val rotationDuration = (2000.0 / rotationSpeed).toInt().coerceAtLeast(500)

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

    Icon(
        imageVector = Icons.Default.Navigation,
        contentDescription = "Direzione",
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .size(24.dp)
            .rotate(bearing.toFloat() + rotation * 0.1f)
    )
}

@Composable
fun EnhancedLineStatsCard(
    lineStats: LineStats,
    modifier: Modifier = Modifier
) {
    val lineColor = getLineColor(lineStats.line)

    GradientCard(
        colors = getLineGradient(lineStats.line),
        modifier = modifier,
        cornerRadius = 24
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header con indicatore linea
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedLineIndicator(
                    line = lineStats.line,
                    color = lineColor,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Linea ${lineStats.line}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = lineColor
                    )
                    Text(
                        text = "${lineStats.activeBuses} autobus attivi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Statistiche animate con controlli sicuri
            StatisticRow(
                label = "Velocità Media",
                value = "${DecimalFormat("#.#").format(lineStats.averageSpeed)} km/h",
                icon = Icons.Default.Speed,
                color = StatusBlue,
                progress = calculateSafeProgress(lineStats.averageSpeed, 50.0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatisticRow(
                label = "Ritardo Medio",
                value = "${DecimalFormat("#.#").format(lineStats.averageDelay)} min",
                icon = Icons.Default.Schedule,
                color = getStatusColor(lineStats.averageDelay),
                progress = calculateSafeProgress(kotlin.math.abs(lineStats.averageDelay), 10.0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatisticRow(
                label = "Puntualità",
                value = "${DecimalFormat("#.#").format(lineStats.onTimePercentage)}%",
                icon = Icons.Default.CheckCircle,
                color = StatusGreen,
                progress = calculateSafeProgress(lineStats.onTimePercentage, 100.0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatisticRow(
                label = "Passeggeri",
                value = "${lineStats.totalPassengers}",
                icon = Icons.Default.People,
                color = StatusGreen,
                progress = calculateSafeProgress(lineStats.totalPassengers.toDouble(), 500.0)
            )
        }
    }
}

// Funzione helper per calcoli sicuri di progress
private fun calculateSafeProgress(value: Double, maxValue: Double): Float {
    return if (value > 0.0 && maxValue > 0.0 && value.isFinite() && maxValue.isFinite()) {
        (value / maxValue).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
}

@Composable
private fun StatisticRow(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // Fix: Assicuriamoci che progress sia sempre valido
    val safeProgress = if (progress.isNaN() || progress.isInfinite()) 0f else progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar animata con valore sicuro
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun SystemStatsRow(
    systemStatus: SystemStatus?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        systemStatus?.let { status ->
            AnimatedCounter(
                value = status.activeBuses,
                label = "Autobus\nAttivi",
                icon = Icons.Default.DirectionsBus,
                color = StatusBlue,
                modifier = Modifier.weight(1f)
            )

            AnimatedCounter(
                value = status.totalPassengers,
                label = "Passeggeri\nTotali",
                icon = Icons.Default.People,
                color = StatusGreen,
                modifier = Modifier.weight(1f)
            )

            AnimatedCounter(
                value = status.averageSystemDelay.toInt(),
                label = "Ritardo\nSistema",
                icon = Icons.Default.Schedule,
                color = getStatusColor(status.averageSystemDelay),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Manteniamo la compatibilità con il vecchio BusCard
@Composable
fun BusCard(
    bus: Bus,
    modifier: Modifier = Modifier
) {
    EnhancedBusCard(bus = bus, modifier = modifier)
}

@Composable
fun LineStatsCard(
    lineStats: LineStats,
    modifier: Modifier = Modifier
) {
    EnhancedLineStatsCard(lineStats = lineStats, modifier = modifier)
}

// Componenti legacy mantenuti per compatibilità
@Composable
fun SystemStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GradientCard(
        colors = listOf(color.copy(alpha = 0.1f), color.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulsingIcon(
                icon = icon,
                tint = color,
                size = 32
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = color
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    AnimatedStat(
        icon = icon,
        value = "$label: $value",
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        GradientCard(
            colors = ErrorGradient,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PulsingIcon(
                    icon = Icons.Default.Error,
                    tint = MaterialTheme.colorScheme.error,
                    size = 64
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Errore di Connessione",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                FloatingActionCardButton(
                    icon = Icons.Default.Refresh,
                    label = "Riprova",
                    onClick = onRetry,
                    backgroundColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}