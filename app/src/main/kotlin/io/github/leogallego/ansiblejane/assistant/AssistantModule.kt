package io.github.leogallego.ansiblejane.assistant

import io.github.leogallego.ansiblejane.assistant.data.AssistantRepository
import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.presentation.AssistantViewModel
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.network.AuthInterceptor
import io.github.leogallego.ansiblejane.network.CertTrustManager
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.network.networkJson
import io.github.leogallego.ansiblejane.presentation.settings.SettingsViewModel
import io.github.leogallego.ansiblejane.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val assistantModule = module {
    single {
        McpServerManager(
            httpClientFactory = { instance, serverConfig ->
                OkHttpClient.Builder()
                    .apply {
                        if (serverConfig.useInstanceAuth) {
                            addInterceptor(
                                AuthInterceptor(
                                    tokenProvider = { instance.token },
                                    instanceIdProvider = { instance.id }
                                )
                            )
                        }
                    }
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                                else HttpLoggingInterceptor.Level.NONE
                    })
                    .apply {
                        if (instance.trustSelfSigned) {
                            val tm = CertTrustManager.createTrustAllManager()
                            sslSocketFactory(CertTrustManager.createSslSocketFactory(tm), tm)
                            hostnameVerifier { _, _ -> true }
                        }
                    }
                    .build()
            },
            json = networkJson
        )
    }

    single { AssistantRepository(get(), get()) } bind IAssistantRepository::class

    single(named("llm")) {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    viewModel {
        AssistantViewModel(
            mcpServerManager = get(),
            repository = get(),
            tokenManager = get(),
            localTools = getAll<LocalTool>()
        )
    }

    viewModel {
        SettingsViewModel(
            tokenManager = get(),
            apiProvider = get(),
            userPreferences = get(),
            assistantRepository = get(),
            mcpServerManager = get(),
            instanceDiscovery = get(),
            httpClient = get(named("llm")),
            json = networkJson
        )
    }
}
