package io.github.leogallego.ansiblejane.network

interface IAapApiProvider {
    fun getApiService(): AapApiClient
    fun getEdaApiService(): EdaApiClient
    fun getPlatformApiService(): PlatformApiClient
    fun evictInstance(instanceId: String)
}
