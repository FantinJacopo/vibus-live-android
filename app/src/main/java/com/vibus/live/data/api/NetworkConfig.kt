package com.vibus.live.data.api

object NetworkConfig {

    object Development {
        const val INFLUX_BASE_URL_EMULATOR = "http://10.0.2.2:8086/"
        const val INFLUX_BASE_URL_DEVICE = "http://192.168.1.100:8086/"
        const val INFLUX_BASE_URL_NGROK = "https://more-elk-slightly.ngrok-free.app/"

        // MQTT Configuration
        const val MQTT_BROKER_HOST_EMULATOR = "10.0.2.2"
        const val MQTT_BROKER_HOST_DEVICE = "192.168.1.100"
        const val MQTT_BROKER_HOST_NGROK = "4.tcp.eu.ngrok.io"
        const val MQTT_BROKER_PORT = 12345
        const val MQTT_BROKER_PORT_NGROK = 15454
    }

    object Production {
        const val INFLUX_BASE_URL = "https://api.svt.vi.it/"
        const val MQTT_BROKER_HOST = "mqtt.svt.vi.it"
        const val MQTT_BROKER_PORT = 1883
    }

    // Current Configuration
    const val CURRENT_BASE_URL = Development.INFLUX_BASE_URL_EMULATOR
    const val CURRENT_MQTT_HOST = Development.MQTT_BROKER_HOST_NGROK
    const val CURRENT_MQTT_PORT = Development.MQTT_BROKER_PORT_NGROK

    // InfluxDB Credentials
    const val INFLUX_TOKEN = "svt-super-secret-token-123456789"
    const val INFLUX_ORG = "SVT-Vicenza"
    const val INFLUX_BUCKET = "bus-data"

    // MQTT Topics - AGGIORNATO CON NUOVI TOPIC
    const val MQTT_TOPIC_BUS_POSITION = "vibus/autobus/+/posizione"
    const val MQTT_TOPIC_LINE_STATS = "vibus/linea/+/statistiche"        // NUOVO
    const val MQTT_TOPIC_SYSTEM_STATUS = "vibus/sistema/generale/stato"  // NUOVO

    // Timeout configurations
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // Update intervals
    const val REAL_TIME_UPDATE_INTERVAL_MS = 10_000L
    const val STATS_UPDATE_INTERVAL_MS = 10_000L
    const val FALLBACK_RETRY_DELAY_MS = 15_000L

    // MQTT Configuration
    const val MQTT_KEEP_ALIVE_INTERVAL = 60
    const val MQTT_CONNECTION_TIMEOUT = 30
    const val MQTT_AUTO_RECONNECT = true

    // Data retention periods for queries
    const val REAL_TIME_DATA_RANGE = "-5m"
    const val STATS_DATA_RANGE = "-30m"
    const val SYSTEM_STATUS_RANGE = "-10m"

    // Helper per determinare l'host MQTT corretto
    fun getMqttHost(): String {
        return CURRENT_MQTT_HOST
    }

    fun getMqttPort(): Int {
        return CURRENT_MQTT_PORT
    }
}