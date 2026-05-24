package io.github.leogallego.ansiblejane.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.bind
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
    } bind IAapApiProvider::class

    factory {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    factory { ApiVersionDetector(get()) }

    single { InstanceDiscovery(networkJson) }
}
