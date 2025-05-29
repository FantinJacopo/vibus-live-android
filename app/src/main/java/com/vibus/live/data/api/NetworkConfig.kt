package com.vibus.live.data.api

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

    // URL attualmente utilizzato - cambia questo secondo le tue necessità
    // Usa ngrok per accesso da reti esterne (dispositivi fisici, test remoti)
    const val CURRENT_BASE_URL = Development.INFLUX_BASE_URL_NGROK

    // Credenziali InfluxDB (dal docker-compose.yml)
    const val INFLUX_TOKEN = "svt-super-secret-token-123456789"
    const val INFLUX_ORG = "SVT-Vicenza"
    const val INFLUX_BUCKET = "bus-data"

    // Timeout configurations
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    // Update intervals
    const val REAL_TIME_UPDATE_INTERVAL_MS = 10_000L // 10 secondi
    const val STATS_UPDATE_INTERVAL_MS = 60_000L     // 1 minuto
    const val FALLBACK_RETRY_DELAY_MS = 15_000L      // 15 secondi

    // Data retention periods for queries
    const val REAL_TIME_DATA_RANGE = "-5m"     // Ultimi 5 minuti
    const val STATS_DATA_RANGE = "-30m"        // Ultimi 30 minuti
    const val SYSTEM_STATUS_RANGE = "-10m"     // Ultimi 10 minuti
}