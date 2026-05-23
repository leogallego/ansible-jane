package io.github.leogallego.ansiblejane.network

interface IAapApiProvider {
    fun getApiService(): AapApiService
    fun getEdaApiService(): EdaApiService
    fun evictInstance(instanceId: String)
}
