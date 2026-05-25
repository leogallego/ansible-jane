package io.github.leogallego.ansiblejane.network

import kotlinx.serialization.json.Json
import io.github.leogallego.ansiblejane.BuildConfig
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
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    factory { ApiVersionDetector(get()) }

    single { InstanceDiscovery(networkJson) }
}
