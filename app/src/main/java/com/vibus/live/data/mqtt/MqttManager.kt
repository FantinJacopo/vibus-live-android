package com.vibus.live.data.mqtt

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttManager @Inject constructor() {

    companion object {
        private const val TAG = "MqttManager"
    }

    private var mqttClient: Mqtt3AsyncClient? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Flussi reattivi
    private val _connectionState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<MqttConnectionEvent>(
        replay = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val connectionEvents: SharedFlow<MqttConnectionEvent> = _connectionEvents.asSharedFlow()

    private val _messages = MutableSharedFlow<MqttMessage>(
        replay = 50,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<MqttMessage> = _messages.asSharedFlow()

    // Statistiche
    private val messagesReceived = AtomicLong(0)
    private val messagesLost = AtomicLong(0)
    private var connectionStartTime: Long = 0
    private var reconnectCount = 0
    private var lastError: String? = null
    private var currentBrokerConfig: BrokerConfig? = null

    // Subscriptions attive
    private val activeSubscriptions = mutableSetOf<MqttSubscription>()

    // Auto-reconnect
    private var autoReconnectJob: Job? = null
    private var reconnectDelay = MqttConfig.Cache.RECONNECT_DELAY_BASE_MS

    /**
     * Connetti al broker MQTT
     */
    suspend fun connect(config: BrokerConfig): MqttResult<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== CONNECTING TO MQTT BROKER ===")
            Log.d(TAG, "Broker: ${config.brokerUrl}")

            if (isConnected()) {
                Log.d(TAG, "Already connected")
                return@withContext MqttResult.Success(Unit)
            }

            _connectionState.value = MqttConnectionState.CONNECTING
            emitConnectionEvent(
                MqttConnectionState.CONNECTING,
                "Connecting to ${config.host}:${config.port}"
            )

            currentBrokerConfig = config

            // Crea client HiveMQ
            val clientBuilder = MqttClient.builder()
                .useMqttVersion3()
                .identifier(MqttConfig.Client.generateClientId())
                .serverHost(config.host)
                .serverPort(config.port)

            mqttClient = clientBuilder.buildAsync()

            // Configura connessione
            val connectBuilder = mqttClient!!.connectWith()
                .keepAlive(MqttConfig.Client.KEEP_ALIVE_SECONDS)
                .cleanSession(MqttConfig.Client.CLEAN_SESSION)

            // Aggiungi credenziali se presenti
            if (!config.username.isNullOrEmpty()) {
                connectBuilder.simpleAuth()
                    .username(config.username)
                    .password(config.password?.toByteArray(StandardCharsets.UTF_8)!!)
                    .applySimpleAuth()
            }

            // Esegui connessione
            val connectFuture: CompletableFuture<Mqtt3ConnAck> = connectBuilder.send()

            val connAck = connectFuture.get() // Blocking call - siamo già in Dispatchers.IO

            Log.d(TAG, "Connected! Return code: ${connAck.returnCode}")

            // Setup message handler
            setupMessageHandler()

            connectionStartTime = System.currentTimeMillis()
            reconnectCount = 0
            lastError = null
            resetReconnectDelay()

            _connectionState.value = MqttConnectionState.CONNECTED
            emitConnectionEvent(MqttConnectionState.CONNECTED, "Connected to ${config.host}")

            // Ri-subscribe ai topic precedenti
            resubscribeAll()

            MqttResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            lastError = e.message
            _connectionState.value = MqttConnectionState.ERROR
            emitConnectionEvent(MqttConnectionState.ERROR, "Connection failed: ${e.message}", e)

            // Avvia auto-reconnect se abilitato
            if (MqttConfig.Client.AUTO_RECONNECT) {
                startAutoReconnect()
            }

            MqttResult.Error(MqttError.ConnectionFailed(e.message ?: "Unknown error"))
        }
    }

    /**
     * Disconnetti dal broker
     */
    suspend fun disconnect(): MqttResult<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Disconnecting from MQTT broker")

            autoReconnectJob?.cancel()
            autoReconnectJob = null

            mqttClient?.let { client ->
                if (client.state == MqttClientState.CONNECTED) {
                    client.disconnect().get()
                }
            }

            mqttClient = null
            activeSubscriptions.clear()

            _connectionState.value = MqttConnectionState.DISCONNECTED
            emitConnectionEvent(MqttConnectionState.DISCONNECTED, "Disconnected")

            MqttResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Disconnect failed", e)
            MqttResult.Error(MqttError.UnknownError(e.message ?: "Disconnect failed"))
        }
    }

    /**
     * Subscribe a un topic
     */
    suspend fun subscribe(topic: String, qos: Int = 1): MqttResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Subscribing to topic: $topic (QoS: $qos)")

                val client = mqttClient ?: return@withContext MqttResult.Error(
                    MqttError.SubscriptionFailed(topic, "Client not connected")
                )

                if (client.state != MqttClientState.CONNECTED) {
                    return@withContext MqttResult.Error(
                        MqttError.SubscriptionFailed(topic, "Client not connected")
                    )
                }

                val subAck: Mqtt3SubAck = client.subscribeWith()
                    .topicFilter(topic)
                    .qos(MqttQos.fromCode(qos)!!)
                    .send()
                    .get()

                Log.d(TAG, "Successfully subscribed to: $topic")

                val subscription = MqttSubscription(topic, qos, true)
                activeSubscriptions.add(subscription)

                MqttResult.Success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "Subscription failed for topic: $topic", e)
                MqttResult.Error(MqttError.SubscriptionFailed(topic, e.message ?: "Unknown error"))
            }
        }

    /**
     * Unsubscribe da un topic
     */
    suspend fun unsubscribe(topic: String): MqttResult<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Unsubscribing from topic: $topic")

            val client = mqttClient ?: return@withContext MqttResult.Success(Unit)

            if (client.state == MqttClientState.CONNECTED) {
                client.unsubscribeWith()
                    .topicFilter(topic)
                    .send()
                    .get()

                Log.d(TAG, "Successfully unsubscribed from: $topic")
            }

            activeSubscriptions.removeAll { it.topic == topic }

            MqttResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Unsubscribe failed for topic: $topic", e)
            MqttResult.Error(MqttError.UnknownError(e.message ?: "Unsubscribe failed"))
        }
    }

    /**
     * Stato di connessione
     */
    fun isConnected(): Boolean {
        return mqttClient?.state == MqttClientState.CONNECTED
    }

    /**
     * Statistiche connessione
     */
    fun getConnectionStats(): MqttConnectionStats {
        val client = mqttClient
        val config = currentBrokerConfig

        return MqttConnectionStats(
            isConnected = isConnected(),
            connectionUptime = if (connectionStartTime > 0) System.currentTimeMillis() - connectionStartTime else 0,
            messagesReceived = messagesReceived.get(),
            messagesLost = messagesLost.get(),
            reconnectCount = reconnectCount,
            lastError = lastError,
            brokerHost = config?.host ?: "Unknown",
            clientId = client?.config?.clientIdentifier?.toString() ?: "Unknown"
        )
    }

    /**
     * Setup message handler
     */
    private fun setupMessageHandler() {
        mqttClient?.let { client ->
            Log.d(TAG, "Setting up message handler")

            // Usa il metodo corretto per HiveMQ
            client.toAsync().publishes(MqttGlobalPublishFilter.ALL, { publish: Mqtt3Publish ->
                try {
                    val topic = publish.topic.toString()
                    val payloadBuffer = publish.payload.orElse(null)
                    val payload = if (payloadBuffer != null) {
                        StandardCharsets.UTF_8.decode(payloadBuffer).toString()
                    } else {
                        ""
                    }
                    val qos = publish.qos.code
                    val retained = publish.isRetain

                    Log.d(TAG, "Received message - Topic: $topic, Payload length: ${payload.length}")

                    val message = MqttMessage(topic, payload, qos, retained)

                    // Emit message in coroutine scope
                    coroutineScope.launch {
                        _messages.emit(message)
                        messagesReceived.incrementAndGet()
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing message", e)
                    messagesLost.incrementAndGet()
                }
            })
        }
    }

    /**
     * Auto-reconnect
     */
    private fun startAutoReconnect() {
        autoReconnectJob?.cancel()
        autoReconnectJob = coroutineScope.launch {
            while (currentCoroutineContext().isActive && !isConnected()) {
                try {
                    Log.d(TAG, "Auto-reconnect attempt in ${reconnectDelay}ms")
                    _connectionState.value = MqttConnectionState.RECONNECTING
                    emitConnectionEvent(MqttConnectionState.RECONNECTING, "Reconnecting in ${reconnectDelay}ms")

                    delay(reconnectDelay)

                    currentBrokerConfig?.let { config ->
                        reconnectCount++
                        when (val result = connect(config)) {
                            is MqttResult.Success -> {
                                Log.d(TAG, "Auto-reconnect successful")
                                return@launch
                            }
                            is MqttResult.Error -> {
                                Log.w(TAG, "Auto-reconnect failed: ${result.error}")
                                increaseReconnectDelay()
                            }
                            else -> { /* Loading state, continue */ }
                        }
                    } ?: run {
                        Log.e(TAG, "No broker config for auto-reconnect")
                        return@launch
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Auto-reconnect error", e)
                    increaseReconnectDelay()
                }
            }
        }
    }

    /**
     * Re-subscribe a tutti i topic attivi
     */
    private suspend fun resubscribeAll() {
        val subscriptions = activeSubscriptions.toList()
        activeSubscriptions.clear()

        subscriptions.forEach { subscription ->
            subscribe(subscription.topic, subscription.qos)
        }
    }

    /**
     * Gestione delay di reconnect con backoff exponential
     */
    private fun increaseReconnectDelay() {
        reconnectDelay = (reconnectDelay * 2).coerceAtMost(MqttConfig.Cache.RECONNECT_MAX_DELAY_MS)
    }

    private fun resetReconnectDelay() {
        reconnectDelay = MqttConfig.Cache.RECONNECT_DELAY_BASE_MS
    }

    /**
     * Emetti evento di connessione
     */
    private suspend fun emitConnectionEvent(
        state: MqttConnectionState,
        message: String,
        error: Throwable? = null
    ) {
        val event = MqttConnectionEvent(state, message, error)
        _connectionEvents.emit(event)
    }

    /**
     * Cleanup risorse
     */
    fun cleanup() {
        coroutineScope.launch {
            disconnect()
        }
        coroutineScope.cancel()
    }
}