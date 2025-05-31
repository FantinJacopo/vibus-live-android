package com.vibus.live.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibus.live.data.SystemHealth
import com.vibus.live.data.SystemStatus
import com.vibus.live.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun EnhancedWelcomeHeader(
    systemStatus: SystemStatus?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome-header")

    // Animazione gradiente di sfondo
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "gradient"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SVTBlue.copy(alpha = 0.1f + gradientOffset * 0.1f),
                        SVTLightBlue.copy(alpha = 0.1f + gradientOffset * 0.1f),
                        SVTAccent.copy(alpha = 0.05f + gradientOffset * 0.05f)
                    ),
                    start = Offset(0f, gradientOffset * 1000f),
                    end = Offset(1000f, (1f - gradientOffset) * 1000f)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        SVTBlue.copy(alpha = 0.3f),
                        SVTLightBlue.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Titolo animato
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🚍",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .scale(1f + sin(gradientOffset * 2 * PI).toFloat() * 0.1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Benvenuto in ViBus Live",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Monitoraggio in tempo reale del trasporto pubblico SVT di Vicenza",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status badges migliorati
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    systemStatus?.let { status ->
                        EnhancedStatusBadge(
                            text = "Puntualità media ${when(status.systemHealth) {
                                SystemHealth.EXCELLENT -> "ottima"
                                SystemHealth.GOOD -> "buona"
                                SystemHealth.FAIR -> "discreta"
                                SystemHealth.POOR -> "scarsa"
                                SystemHealth.CRITICAL -> "pessima"
                            }}",
                            color = when (status.systemHealth) {
                                SystemHealth.EXCELLENT -> StatusGreen
                                SystemHealth.GOOD -> StatusBlue
                                SystemHealth.FAIR -> StatusYellow
                                SystemHealth.POOR -> StatusRed
                                SystemHealth.CRITICAL -> StatusRed
                            },
                            icon = when (status.systemHealth) {
                                SystemHealth.EXCELLENT -> Icons.Default.CheckCircle
                                SystemHealth.GOOD -> Icons.Default.Verified
                                SystemHealth.FAIR -> Icons.Default.Warning
                                SystemHealth.POOR -> Icons.Default.Error
                                SystemHealth.CRITICAL -> Icons.Default.ErrorOutline
                            },
                            animated = status.systemHealth != SystemHealth.EXCELLENT
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                EnhancedStatusBadge(
                    text = "Tempo Reale",
                    color = StatusGreen,
                    icon = Icons.Default.Schedule,
                    animated = true
                )
            }

            // Icona animata laterale
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Cerchi concentrici animati
                repeat(3) { index ->
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 2000,
                                delayMillis = index * 400
                            ),
                            repeatMode = RepeatMode.Restart
                        ), label = "ripple-$index"
                    )

                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 2000,
                                delayMillis = index * 400
                            ),
                            repeatMode = RepeatMode.Restart
                        ), label = "ripple-alpha-$index"
                    )

                    Box(
                        modifier = Modifier
                            .size((40 + index * 15).dp)
                            .scale(scale)
                            .background(
                                color = SVTBlue.copy(alpha = alpha * 0.3f),
                                shape = CircleShape
                            )
                    )
                }

                // Icona centrale
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = SVTBlue,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun EnhancedStatusBadge(
    text: String,
    color: Color,
    icon: ImageVector,
    animated: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status-badge")

    val alpha by if (animated) {
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
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = alpha * 0.15f),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}