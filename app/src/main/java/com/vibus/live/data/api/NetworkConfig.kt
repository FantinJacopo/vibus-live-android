package com.vibus.live.data.api

import com.vibus.live.BuildConfig

object NetworkConfig {

    // Configurazioni per diversi ambienti
    object Development {
        // Per emulatore Android
        const val INFLUX_BASE_URL_EMULATOR = "http://10.0.2.2:8086/"

        // Per dispositivo fisico - sostituisci con l'IP del tuo PC
        const val INFLUX_BASE_URL_DEVICE = "http://192.168.1.100:8086/"

        // ngrok - URL statico configurato
        const val INFLUX_BASE_URL_NGROK = "https://more-elk-slightly.ngrok-free.app/"
    }

    object Production {
        // URL produzione quando disponibile
        const val INFLUX_BASE_URL = "https://api.svt.vi.it/"
    }

    // URL attualmente utilizzato per HTTP fallback
    val CURRENT_BASE_URL = if (BuildConfig.ENABLE_MQTT) {
        Development.INFLUX_BASE_URL_NGROK // Usato solo per fallback
    } else {
        Development.INFLUX_BASE_URL_NGROK // Usato per polling HTTP
    }

    // Credenziali InfluxDB (dal docker-compose.yml)
    const val INFLUX_TOKEN = "svt-super-secret-token-123456789"
    const val INFLUX_ORG = "SVT-Vicenza"
    const val INFLUX_BUCKET = "bus-data"

    // Timeout configurations
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // Update intervals (usati solo se HTTP mode)
    const val REAL_TIME_UPDATE_INTERVAL_MS = 10_000L // 10 secondi
    const val STATS_UPDATE_INTERVAL_MS = 60_000L     // 1 minuto
    const val FALLBACK_RETRY_DELAY_MS = 15_000L      // 15 secondi

    // Data retention periods for queries
    const val REAL_TIME_DATA_RANGE = "-5m"     // Ultimi 5 minuti
    const val STATS_DATA_RANGE = "-30m"        // Ultimi 30 minuti
    const val SYSTEM_STATUS_RANGE = "-10m"     // Ultimi 10 minuti

    // MQTT Configuration (moved from MqttConfig for consistency)
    object Mqtt {
        const val PRIMARY_BROKER_URL = BuildConfig.MQTT_BROKER_HOST
        //const val FALLBACK_BROKER_URL = BuildConfig.MQTT_FALLBACK_URL

        // Topics
        const val TOPIC_BUS_POSITIONS = "vibus/autobus/+/posizione"
        const val TOPIC_LINE_STATS = "vibus/linea/+/statistiche"
        const val TOPIC_SYSTEM_STATUS = "vibus/sistema/+/stato"
    }
}