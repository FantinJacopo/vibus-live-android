package com.vibus.live.di

import com.vibus.live.BuildConfig
import com.vibus.live.data.api.InfluxApiService
import com.vibus.live.data.api.NetworkConfig
import com.vibus.live.data.mqtt.MqttManager
import com.vibus.live.mqtt.MqttMessageParser
import com.vibus.live.data.repository.BusRepository
import com.vibus.live.data.repository.BusRepositoryImpl
import com.vibus.live.data.repository.MqttBusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // === MQTT PROVIDERS ===

    @Provides
    @Singleton
    fun provideMqttManager(): MqttManager {
        return MqttManager()
    }

    @Provides
    @Singleton
    fun provideMqttMessageParser(): MqttMessageParser {
        return MqttMessageParser()
    }

    // === HTTP PROVIDERS (per fallback) ===

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply {
                        if (NetworkConfig.CURRENT_BASE_URL.contains("ngrok")) {
                            addHeader("ngrok-skip-browser-warning", "true")
                        }
                    }
                    .build()

                if (BuildConfig.DEBUG) {
                    println("🌐 HTTP Request: ${request.method} ${request.url}")
                    println("🔑 Headers: ${request.headers}")
                }

                val response = chain.proceed(request)

                if (BuildConfig.DEBUG) {
                    println("📡 HTTP Response: ${response.code} ${response.message}")
                }

                response
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(InfluxApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideInfluxApiService(retrofit: Retrofit): InfluxApiService {
        return retrofit.create(InfluxApiService::class.java)
    }

    // === REPOSITORY PROVIDERS ===

    @Provides
    @Singleton
    @Named("http")
    fun provideHttpBusRepository(apiService: InfluxApiService): BusRepositoryImpl {
        return BusRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    @Named("mqtt")
    fun provideMqttBusRepository(
        mqttManager: MqttManager,
        messageParser: MqttMessageParser,
        @Named("http") httpRepository: BusRepositoryImpl
    ): MqttBusRepository {
        return MqttBusRepository(mqttManager, messageParser, httpRepository)
    }

    /**
     * Repository principale - MQTT con fallback HTTP
     *
     * Puoi cambiare il provider per testare:
     * - @Named("mqtt") per MQTT
     * - @Named("http") per HTTP
     */
    @Provides
    @Singleton
    fun provideBusRepository(
        @Named("mqtt") mqttRepository: MqttBusRepository
    ): BusRepository {
        return mqttRepository
    }

    // === CONFIGURAZIONI ===

    @Provides
    @Singleton
    @Named("enableMqtt")
    fun provideEnableMqtt(): Boolean {
        // Abilita MQTT solo in debug o quando esplicitamente richiesto
        return try {
            BuildConfig.DEBUG || BuildConfig.ENABLE_MQTT
        } catch (e: Exception) {
            true // Default to enabled
        }
    }

    @Provides
    @Singleton
    @Named("enableHttpFallback")
    fun provideEnableHttpFallback(): Boolean {
        return true // Sempre abilitato per resilienza
    }

    @Provides
    @Singleton
    @Named("mqttBrokerHost")
    fun provideMqttBrokerHost(): String {
        return if (BuildConfig.DEBUG) {
            "more-elk-slightly.ngrok-free.app" // ngrok per testing
        } else {
            "mqtt.svt.vi.it" // Produzione ipotetico
        }
    }

    @Provides
    @Singleton
    @Named("mqttBrokerPort")
    fun provideMqttBrokerPort(): Int {
        return if (BuildConfig.DEBUG) 1883 else 8883
    }
}