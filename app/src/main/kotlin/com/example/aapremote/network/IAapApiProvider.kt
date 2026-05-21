package com.example.aapremote.network

interface IAapApiProvider {
    fun getApiService(): AapApiService
    fun getEdaApiService(): EdaApiService
    fun evictInstance(instanceId: String)
}
