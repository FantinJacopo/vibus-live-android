package com.vibus.live.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vibus.live.data.api.InfluxApiService
import com.vibus.live.data.api.NetworkConfig
import com.vibus.live.ui.theme.ViBusLiveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/*
<activity
    android:name=".debug.ConnectionTestActivity"
    android:exported="true"
    android:label="Debug - Test Connessione">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
*/

@AndroidEntryPoint
class ConnectionTestActivity : ComponentActivity() {

    @Inject
    lateinit var influxApi: InfluxApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ViBusLiveTheme {
                ConnectionTestScreen(influxApi = influxApi)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionTestScreen(
    influxApi: InfluxApiService
) {
    var testResults by remember { mutableStateOf(listOf<TestResult>()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug - Test Connessione InfluxDB") },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                testResults = runConnectionTests(influxApi) { loading ->
                                    isLoading = loading
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Testa connessione")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ConfigurationCard()
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Testando connessione...")
                            }
                        }
                    }
                }
            }

            if (testResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Risultati Test",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                items(testResults) { result ->
                    TestResultCard(result = result)
                }
            } else if (!isLoading) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Premi il pulsante refresh per testare la connessione")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigurationCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Configurazione Attuale",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            ConfigRow("Base URL", NetworkConfig.CURRENT_BASE_URL)
            ConfigRow("Organizzazione", NetworkConfig.INFLUX_ORG)
            ConfigRow("Bucket", NetworkConfig.INFLUX_BUCKET)
            ConfigRow("Token", "${NetworkConfig.INFLUX_TOKEN.take(20)}...")
        }
    }
}

@Composable
fun ConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
fun TestResultCard(result: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) {
                Color(0xFFE8F5E8)
            } else {
                Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (result.success) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.testName,
                    style = MaterialTheme.typography.titleSmall
                )
                if (result.message.isNotEmpty()) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (result.responsePreview.isNotEmpty()) {
                    Text(
                        text = "Anteprima: ${result.responsePreview}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${result.duration}ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class TestResult(
    val testName: String,
    val success: Boolean,
    val message: String,
    val duration: Long,
    val responsePreview: String = ""
)

suspend fun runConnectionTests(
    influxApi: InfluxApiService,
    onLoadingChange: (Boolean) -> Unit
): List<TestResult> {
    onLoadingChange(true)
    val results = mutableListOf<TestResult>()

    // Test 1: Connessione base
    try {
        val startTime = System.currentTimeMillis()
        val response = influxApi.queryFlux(
            token = "Token ${NetworkConfig.INFLUX_TOKEN}",
            org = NetworkConfig.INFLUX_ORG,
            query = "buckets()"
        )
        val duration = System.currentTimeMillis() - startTime

        if (response.isSuccessful) {
            results.add(
                TestResult(
                    testName = "Connessione Base",
                    success = true,
                    message = "Connessione riuscita (${response.code()})",
                    duration = duration,
                    responsePreview = response.body()?.take(100) ?: ""
                )
            )
        } else {
            results.add(
                TestResult(
                    testName = "Connessione Base",
                    success = false,
                    message = "Errore HTTP: ${response.code()} - ${response.message()}",
                    duration = duration
                )
            )
        }
    } catch (e: Exception) {
        results.add(
            TestResult(
                testName = "Connessione Base",
                success = false,
                message = "Errore di rete: ${e.message}",
                duration = 0
            )
        )
    }

    // Test 2: Query autobus
    try {
        val startTime = System.currentTimeMillis()
        val response = influxApi.queryFlux(
            token = "Token ${NetworkConfig.INFLUX_TOKEN}",
            org = NetworkConfig.INFLUX_ORG,
            query = InfluxApiService.getRealTimeBusesQuery()
        )
        val duration = System.currentTimeMillis() - startTime

        if (response.isSuccessful) {
            val body = response.body() ?: ""
            results.add(
                TestResult(
                    testName = "Query Autobus",
                    success = true,
                    message = "Dati ricevuti (${body.length} caratteri)",
                    duration = duration,
                    responsePreview = body.take(100)
                )
            )
        } else {
            results.add(
                TestResult(
                    testName = "Query Autobus",
                    success = false,
                    message = "Errore: ${response.code()}",
                    duration = duration
                )
            )
        }
    } catch (e: Exception) {
        results.add(
            TestResult(
                testName = "Query Autobus",
                success = false,
                message = "Errore: ${e.message}",
                duration = 0
            )
        )
    }

    onLoadingChange(false)
    return results
}