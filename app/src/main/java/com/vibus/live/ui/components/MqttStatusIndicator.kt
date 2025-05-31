package com.vibus.live.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibus.live.data.mqtt.MqttConnectionState
import com.vibus.live.ui.theme.StatusGreen
import com.vibus.live.ui.theme.StatusRed
import com.vibus.live.ui.theme.StatusYellow

@Composable
fun MqttStatusIndicator(
    connectionState: MqttConnectionState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mqtt-status")

    val (icon, color, text) = when (connectionState) {
        is MqttConnectionState.Connected -> Triple(
            Icons.Default.Wifi,
            StatusGreen,
            "MQTT"
        )
        is MqttConnectionState.Connecting -> Triple(
            Icons.Default.WifiFind,
            StatusYellow,
            "Connecting..."
        )
        is MqttConnectionState.Disconnected -> Triple(
            Icons.Default.WifiOff,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "HTTP"
        )
        is MqttConnectionState.Error -> Triple(
            Icons.Default.WifiOff,
            StatusRed,
            "HTTP"
        )
    }

    val alpha by if (connectionState is MqttConnectionState.Connected) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = alpha * 0.15f),
        shadowElevation = if (connectionState is MqttConnectionState.Connected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun MqttDebugPanel(
    connectionState: MqttConnectionState,
    busCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Connessione Dati",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MqttStatusIndicator(connectionState = connectionState)

                Text(
                    text = when (connectionState) {
                        is MqttConnectionState.Connected -> "Tempo reale MQTT attivo"
                        is MqttConnectionState.Connecting -> "Connessione MQTT..."
                        is MqttConnectionState.Disconnected -> "Modalità HTTP (fallback)"
                        is MqttConnectionState.Error -> "MQTT error - usando HTTP"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (busCount > 0) {
                Text(
                    text = "$busCount autobus ricevuti",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}