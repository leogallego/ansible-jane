package io.github.leogallego.ansiblejane.assistant

import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.presentation.AssistantViewModel
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.network.createPlatformHttpClient
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.network.networkJson
import io.github.leogallego.ansiblejane.presentation.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val assistantModule = module {
    single {
        McpServerManager(
            ktorClientFactory = { instance, serverConfig ->
                createPlatformHttpClient(trustSelfSigned = instance.trustSelfSigned) {
                    defaultRequest {
                        if (serverConfig.useInstanceAuth) {
                            header(HttpHeaders.Authorization, "Bearer ${instance.token}")
                        }
                        serverConfig.headers
                            .filter { (key, _) ->
                                !serverConfig.useInstanceAuth ||
                                    !key.equals(HttpHeaders.Authorization, ignoreCase = true)
                            }
                            .forEach { (key, value) ->
                                header(key, value)
                            }
                    }
                    install(SSE)
                }
            }
        )
    }

    single(named("llm")) {
        createPlatformHttpClient {
            expectSuccess = false
        }
    }

    viewModel {
        AssistantViewModel(
            mcpServerManager = get(),
            repository = get(),
            tokenManager = get(),
            manifestRepository = get(),
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
            manifestRepository = get(),
            instanceDiscovery = get(),
            httpClient = get(named("llm")),
            json = networkJson,
            localTools = getAll<LocalTool>()
        )
    }
}
