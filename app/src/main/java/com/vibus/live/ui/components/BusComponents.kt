package com.vibus.live.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibus.live.data.Bus
import com.vibus.live.data.LineStats
import com.vibus.live.data.SystemStatus
import com.vibus.live.data.delayText
import com.vibus.live.ui.theme.SVTBlue
import com.vibus.live.ui.theme.SVTError
import com.vibus.live.ui.theme.SVTLightBlue
import com.vibus.live.ui.theme.SVTWarning
import com.vibus.live.ui.theme.getLineColor
import java.text.DecimalFormat

@Composable
fun BusCard(
    bus: Bus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Line Color Indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getLineColor(bus.line)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bus.line,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Bus Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bus.id,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = bus.lineName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stats
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${DecimalFormat("#.#").format(bus.speed)} km/h",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Passengers",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${bus.passengers}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Delay indicator
                Text(
                    text = bus.delayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        bus.delay > 2 -> SVTError
                        bus.delay > 0 -> SVTWarning
                        else -> Color.Green
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LineStatsCard(
    lineStats: LineStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = getLineColor(lineStats.line).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(getLineColor(lineStats.line)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lineStats.line,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Linea ${lineStats.line}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            StatItem(
                label = "Autobus Attivi",
                value = lineStats.activeBuses.toString(),
                icon = Icons.Default.DirectionsBus
            )

            StatItem(
                label = "Velocità Media",
                value = "${DecimalFormat("#.#").format(lineStats.averageSpeed)} km/h",
                icon = Icons.Default.Speed
            )

            StatItem(
                label = "Ritardo Medio",
                value = "${DecimalFormat("#.#").format(lineStats.averageDelay)} min",
                icon = Icons.Default.Schedule
            )

            StatItem(
                label = "Puntualità",
                value = "${DecimalFormat("#.#").format(lineStats.onTimePercentage)}%",
                icon = Icons.Default.CheckCircle
            )
        }
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
            SystemStatCard(
                title = "Autobus Attivi",
                value = "${status.activeBuses}/${status.totalBuses}",
                icon = Icons.Default.DirectionsBus,
                color = SVTBlue,
                modifier = Modifier.weight(1f)
            )

            SystemStatCard(
                title = "Passeggeri",
                value = status.totalPassengers.toString(),
                icon = Icons.Default.People,
                color = SVTLightBlue,
                modifier = Modifier.weight(1f)
            )

            SystemStatCard(
                title = "Ritardo Sistema",
                value = "${DecimalFormat("#.#").format(status.averageSystemDelay)} min",
                icon = Icons.Default.Schedule,
                color = when {
                    status.averageSystemDelay > 3 -> SVTError
                    status.averageSystemDelay > 1 -> SVTWarning
                    else -> Color.Green
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SystemStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
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
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Errore",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("Riprova")
        }
    }
}