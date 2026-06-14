package io.github.leogallego.ansiblejane.assistant

import io.github.leogallego.ansiblejane.assistant.engine.ToolRouter
import io.github.leogallego.ansiblejane.assistant.presentation.AssistantViewModel
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.network.createPlatformHttpClient
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.network.networkJson
import io.github.leogallego.ansiblejane.presentation.settings.SettingsViewModel
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
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

    single {
        ToolRouter(
            initialLocalTools = getAll<LocalTool>(),
            repository = get()
        )
    }

    viewModel {
        AssistantViewModel(
            mcpServerManager = get(),
            repository = get(),
            tokenManager = get(),
            manifestRepository = get(),
            toolRouter = get(),
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
            toolRouter = get(),
            httpClient = get(named("llm")),
            json = networkJson
        )
    }
}
