package com.example.aapremote.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module

val networkJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

val networkModule = module {

    single {
        AapApiProvider(
            tokenManager = get(),
            json = networkJson
        )
    }

    factory<AapApiService> {
        get<AapApiProvider>().getApiService()
    }

    factory<EdaApiService> {
        get<AapApiProvider>().getEdaApiService()
    }

    factory {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    factory { ApiVersionDetector(get()) }
}
