package com.vibus.live.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibus.live.data.LineStats
import com.vibus.live.ui.components.AnimatedCounter
import com.vibus.live.ui.components.EnhancedBusCard
import com.vibus.live.ui.components.EnhancedLineStatsCard
import com.vibus.live.ui.components.EnhancedWelcomeHeader
import com.vibus.live.ui.components.FloatingActionCardButton
import com.vibus.live.ui.components.GradientCard
import com.vibus.live.ui.components.PulsingIcon
import com.vibus.live.ui.components.RealGoogleMapsCard
import com.vibus.live.ui.components.WaveLoadingIndicator
import com.vibus.live.ui.theme.ErrorGradient
import com.vibus.live.ui.theme.SVTBlue
import com.vibus.live.ui.theme.SVTLightBlue
import com.vibus.live.ui.theme.StatusBlue
import com.vibus.live.ui.theme.StatusGreen
import com.vibus.live.ui.theme.StatusRed
import com.vibus.live.ui.theme.getStatusColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToMap: (String?) -> Unit = {},
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Icona app con gradiente
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "ViBus Live",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1
                            )

                            if (uiState.buses.isNotEmpty() && !uiState.isLoading) {
                                Text(
                                    text = "${uiState.buses.size} autobus attivi",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Badge di stato connessione
                    if (!uiState.isLoading && uiState.error == null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusGreen.copy(alpha = 0.9f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "🟢 LIVE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (uiState.error != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusRed.copy(alpha = 0.9f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "⚠️ OFFLINE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Pulsante refresh con indicatore di caricamento
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Aggiorna",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Pulsante mappa fullscreen
                    IconButton(
                        onClick = { onNavigateToMap(null) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Mappa Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(SVTBlue, SVTLightBlue)
                        )
                    )
                    .statusBarsPadding()
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
                        onNavigateToMap = onNavigateToMap,
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
    onNavigateToMap: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Enhanced Welcome Header animato
        item {
            EnhancedWelcomeHeader(systemStatus = uiState.systemStatus)
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
                        onBusClick = { onNavigateToMap(bus.id) },
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