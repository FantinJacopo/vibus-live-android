package com.vibus.live.data.mqtt

import java.util.UUID

object MqttConfig {

    // Configurazioni broker MQTT
    object Broker {
        const val HOST_LOCAL = "localhost"
        const val HOST_NGROK = "more-elk-slightly.ngrok-free.app"
        const val PORT_TCP = 1883
        const val PORT_WEBSOCKET = 9001

        // URL principale - usa ngrok per accesso esterno
        const val CURRENT_HOST = HOST_NGROK
        const val CURRENT_PORT = PORT_TCP

        // Credenziali (se necessario)
        const val USERNAME = ""  // Vuoto per broker senza auth
        const val PASSWORD = ""
    }

    // Topics ViBus
    object Topics {
        const val BUS_POSITIONS = "vibus/autobus/+/posizione"
        const val LINE_STATS = "vibus/linea/+/statistiche"
        const val SYSTEM_STATUS = "vibus/sistema/+/stato"

        // Pattern per topic specifici
        fun busPositionTopic(busId: String) = "vibus/autobus/$busId/posizione"
        fun lineStatsTopic(lineId: String) = "vibus/linea/$lineId/statistiche"
        fun systemStatusTopic(component: String) = "vibus/sistema/$component/stato"
    }

    // Configurazioni client
    object Client {
        const val CLIENT_ID_PREFIX = "vibus_android"
        const val KEEP_ALIVE_SECONDS = 60
        const val CONNECTION_TIMEOUT_SECONDS = 30
        const val AUTO_RECONNECT = true
        const val CLEAN_SESSION = true

        // QoS levels
        const val QOS_BUS_POSITIONS = 1    // At least once per posizioni
        const val QOS_STATS = 0           // At most once per statistiche
        const val QOS_STATUS = 1          // At least once per stato sistema

        // Genera ID client unico
        fun generateClientId(): String {
            val uuid = UUID.randomUUID().toString().take(8)
            return "${CLIENT_ID_PREFIX}_$uuid"
        }
    }

    // Configurazioni cache e performance
    object Cache {
        const val BUS_DATA_TTL_MINUTES = 5
        const val STATS_DATA_TTL_MINUTES = 30
        const val MAX_CACHED_BUSES = 50
        const val MESSAGE_BUFFER_SIZE = 100
        const val RECONNECT_DELAY_BASE_MS = 1000L
        const val RECONNECT_MAX_DELAY_MS = 30000L
    }

    // Configurazioni fallback
    object Fallback {
        const val ENABLE_HTTP_FALLBACK = true
        const val FALLBACK_AFTER_FAILURES = 3
        const val HTTP_POLLING_INTERVAL_MS = 15000L
    }

    // Configurazioni per diversi ambienti
    enum class Environment {
        DEVELOPMENT,
        TESTING,
        PRODUCTION
    }

    fun getConfigForEnvironment(env: Environment): BrokerConfig {
        return when (env) {
            Environment.DEVELOPMENT -> BrokerConfig(
                host = Broker.HOST_LOCAL,
                port = Broker.PORT_TCP,
                useSSL = false,
                username = null,
                password = null
            )
            Environment.TESTING -> BrokerConfig(
                host = Broker.HOST_NGROK,
                port = Broker.PORT_TCP,
                useSSL = false,
                username = null,
                password = null
            )
            Environment.PRODUCTION -> BrokerConfig(
                host = "mqtt.svt.vi.it", // URL produzione ipotetico
                port = 8883,
                useSSL = true,
                username = Broker.USERNAME.takeIf { it.isNotEmpty() },
                password = Broker.PASSWORD.takeIf { it.isNotEmpty() }
            )
        }
    }
}

data class BrokerConfig(
    val host: String,
    val port: Int,
    val useSSL: Boolean,
    val username: String?,
    val password: String?
) {
    val brokerUrl: String
        get() = if (useSSL) "ssl://$host:$port" else "tcp://$host:$port"
}