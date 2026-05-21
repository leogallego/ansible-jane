package com.example.aapremote.assistant

import com.example.aapremote.assistant.data.AssistantRepository
import com.example.aapremote.assistant.data.IAssistantRepository
import com.example.aapremote.assistant.presentation.AssistantViewModel
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.network.AuthInterceptor
import com.example.aapremote.network.CertTrustManager
import com.example.aapremote.network.mcp.McpServerManager
import com.example.aapremote.network.networkJson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val assistantModule = module {
    single {
        McpServerManager(
            httpClientFactory = { instance ->
                OkHttpClient.Builder()
                    .addInterceptor(
                        AuthInterceptor(
                            tokenProvider = { instance.token },
                            instanceIdProvider = { instance.id }
                        )
                    )
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
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

    single { AssistantRepository(get()) } bind IAssistantRepository::class

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
            httpClient = get(named("llm")),
            json = networkJson,
            localTools = getAll<LocalTool>()
        )
    }
}
