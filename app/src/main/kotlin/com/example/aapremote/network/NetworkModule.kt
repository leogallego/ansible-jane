package com.example.aapremote.network

import com.example.aapremote.data.TokenManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val networkJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

val networkModule = module {

    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val tokenManager = get<TokenManager>()

        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { tokenManager.cachedToken })
            .addInterceptor(loggingInterceptor)
            .build()
    }

    single {
        val tokenManager = get<TokenManager>()
        val baseUrl = tokenManager.cachedBaseUrl ?: "https://placeholder.example.com"
        val apiVersion = tokenManager.cachedApiVersion

        Retrofit.Builder()
            .baseUrl("${baseUrl.trimEnd('/')}${apiVersion.prefix}")
            .client(get())
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single<AapApiService> {
        get<Retrofit>().create(AapApiService::class.java)
    }

    single { ApiVersionDetector(get()) }
}
