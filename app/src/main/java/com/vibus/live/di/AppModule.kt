package com.vibus.live.di

import com.vibus.live.data.api.InfluxApiService
import com.vibus.live.data.api.NetworkConfig
import com.vibus.live.data.mqtt.MqttService
import com.vibus.live.data.repository.BusRepository
import com.vibus.live.data.repository.BusRepositoryImpl
import com.vibus.live.data.repository.MqttOnlyBusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
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

                println("🌐 HTTP Request: ${request.method} ${request.url}")
                println("🔑 Headers: ${request.headers}")

                val response = chain.proceed(request)
                println("📡 HTTP Response: ${response.code} ${response.message}")
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

    @Provides
    @Singleton
    fun provideMqttService(): MqttService {
        return MqttService()
    }

    @Provides
    @Singleton
    fun provideBusRepositoryImpl(apiService: InfluxApiService): BusRepositoryImpl {
        return BusRepositoryImpl(apiService)
    }

    // TEMPORANEO: Usa solo MQTT per test - nessun fallback HTTP
    @Provides
    @Singleton
    fun provideBusRepository(
        mqttService: MqttService
    ): BusRepository {
        return MqttOnlyBusRepository(mqttService)
    }
}