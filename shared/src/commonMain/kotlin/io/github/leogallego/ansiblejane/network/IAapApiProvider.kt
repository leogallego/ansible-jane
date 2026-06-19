package io.github.leogallego.ansiblejane.network

interface IAapApiProvider {
    fun getApiService(): AapApiClient
    fun getEdaApiService(): EdaApiClient
    fun getPlatformApiService(): PlatformApiClient
    fun getHubApiService(): HubApiClient
    fun evictInstance(instanceId: String)
}
