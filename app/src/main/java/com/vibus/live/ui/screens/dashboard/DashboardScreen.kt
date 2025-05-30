package com.vibus.live.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibus.live.data.LineStats
import com.vibus.live.ui.components.*
import com.vibus.live.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Animazione per l'entrata
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ViBus Live",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "SVT Vicenza",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    FloatingActionCardButton(
                        icon = Icons.Default.Refresh,
                        label = "Aggiorna",
                        onClick = { viewModel.refresh() },
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(PrimaryGradient)
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                BeautifulLoadingScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.error != null -> {
                BeautifulErrorScreen(
                    error = uiState.error!!,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(800, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(800))
                ) {
                    BeautifulDashboardContent(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun BeautifulLoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            brush = Brush.radialGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.background
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WaveLoadingIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Connessione al sistema SVT...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Caricamento dati autobus in tempo reale",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BeautifulErrorScreen(
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
                    icon = Icons.Default.WifiOff,
                    tint = MaterialTheme.colorScheme.error,
                    size = 64
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Oops! Problema di connessione",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                FloatingActionCardButton(
                    icon = Icons.Default.Refresh,
                    label = "Riprova Connessione",
                    onClick = onRetry,
                    backgroundColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun BeautifulDashboardContent(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome Header animato
        item {
            WelcomeHeader(systemStatus = uiState.systemStatus)
        }

        // System Status Cards animate
        item {
            AnimatedSystemStatsRow(
                systemStatus = uiState.systemStatus,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Real-time Map con header migliorato
        item {
            BeautifulMapSection(
                buses = uiState.buses,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Line Statistics con animazioni
        if (uiState.lineStats.isNotEmpty()) {
            item {
                BeautifulLineStatsSection(
                    lineStats = uiState.lineStats
                )
            }
        }

        // Real-time Bus List con animazioni staggered
        if (uiState.buses.isNotEmpty()) {
            item {
                Text(
                    text = "🚌 Autobus in Tempo Reale",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            itemsIndexed(
                items = uiState.buses,
                key = { _, bus -> bus.id }
            ) { index, bus ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
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
                    EnhancedBusCard(
                        bus = bus,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (!uiState.isLoading) {
            item {
                EmptyBusesCard()
            }
        }

        // Footer spacer
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WelcomeHeader(
    systemStatus: com.vibus.live.data.SystemStatus?,
    modifier: Modifier = Modifier
) {
    GradientCard(
        colors = SecondaryGradient,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🌟 Benvenuto in ViBus Live",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Monitoraggio in tempo reale del trasporto pubblico SVT",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                systemStatus?.let { status ->
                    StatusBadge(
                        text = "Sistema ${status.systemHealth.name}",
                        color = when (status.systemHealth) {
                            com.vibus.live.data.SystemHealth.EXCELLENT -> StatusGreen
                            com.vibus.live.data.SystemHealth.GOOD -> StatusBlue
                            com.vibus.live.data.SystemHealth.FAIR -> StatusYellow
                            com.vibus.live.data.SystemHealth.POOR -> StatusRed
                            com.vibus.live.data.SystemHealth.CRITICAL -> StatusRed
                        },
                        animated = status.systemHealth != com.vibus.live.data.SystemHealth.EXCELLENT
                    )
                }
            }

            PulsingIcon(
                icon = Icons.Default.LocationOn,
                tint = MaterialTheme.colorScheme.primary,
                size = 48
            )
        }
    }
}

@Composable
private fun AnimatedSystemStatsRow(
    systemStatus: com.vibus.live.data.SystemStatus?,
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
                label = "Ritardo Medio\n(minuti)",
                icon = Icons.Default.Schedule,
                color = getStatusColor(status.averageSystemDelay),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BeautifulMapSection(
    buses: List<com.vibus.live.data.Bus>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header semplice senza dropdown
        Text(
            text = "🗺️ Mappa Tempo Reale",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${buses.size} autobus attualmente in servizio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mappa sempre visibile
        RealGoogleMapsCard(
            buses = buses,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

@Composable
private fun BeautifulLineStatsSection(
    lineStats: List<LineStats>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "📊 Statistiche per Linea",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(lineStats) { index, stats ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(
                            durationMillis = 400,
                            delayMillis = index * 100,
                            easing = FastOutSlowInEasing
                        )
                    )
                ) {
                    EnhancedLineStatsCard(
                        lineStats = stats,
                        modifier = Modifier.width(300.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBusesCard(
    modifier: Modifier = Modifier
) {
    GradientCard(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PulsingIcon(
                    icon = Icons.Default.DirectionsBus,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 64
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nessun autobus attivo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "I dati verranno aggiornati automaticamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}