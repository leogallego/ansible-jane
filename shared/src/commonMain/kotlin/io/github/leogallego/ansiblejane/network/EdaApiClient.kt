package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class EdaApiClient(private val client: HttpClient) {

    suspend fun getAuditRules(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): PaginatedResponse<EdaRuleAudit> = client.get("audit-rules/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
    }.body()

    suspend fun getAuditRule(id: Int): EdaRuleAudit =
        client.get("audit-rules/$id/").body()

    suspend fun getActivations(
        page: Int = 1,
        pageSize: Int = 20
    ): PaginatedResponse<EdaActivation> = client.get("activations/") {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()

    suspend fun getActivation(id: Int): EdaActivation =
        client.get("activations/$id/").body()

    suspend fun getRulebooks(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): PaginatedResponse<EdaRulebook> = client.get("rulebooks/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        name?.let { parameter("name", it) }
    }.body()

    suspend fun getDecisionEnvironments(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): PaginatedResponse<EdaDecisionEnvironment> = client.get("decision-environments/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        name?.let { parameter("name", it) }
    }.body()

    suspend fun getProjects(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): PaginatedResponse<EdaProject> = client.get("projects/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        name?.let { parameter("name", it) }
    }.body()

    suspend fun getCredentials(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): PaginatedResponse<EdaCredential> = client.get("credentials/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        name?.let { parameter("name", it) }
    }.body()

    suspend fun getCredentialTypes(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): PaginatedResponse<EdaCredentialType> = client.get("credential-types/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        name?.let { parameter("name", it) }
    }.body()

    suspend fun getEventStreams(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): PaginatedResponse<EdaEventStream> = client.get("event-streams/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        name?.let { parameter("name", it) }
    }.body()

    suspend fun getUsers(
        page: Int = 1,
        pageSize: Int = 20
    ): PaginatedResponse<EdaUser> = client.get("users/") {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()
}
