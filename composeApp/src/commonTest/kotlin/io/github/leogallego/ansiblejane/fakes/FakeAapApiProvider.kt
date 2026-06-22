package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.network.AapApiClient
import io.github.leogallego.ansiblejane.network.EdaApiClient
import io.github.leogallego.ansiblejane.network.HubApiClient
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import io.github.leogallego.ansiblejane.network.PlatformApiClient

class FakeAapApiProvider : IAapApiProvider {
    val evictedInstances = mutableListOf<String>()

    override fun getApiService(): AapApiClient {
        throw UnsupportedOperationException("Use repository fakes instead")
    }

    override fun getEdaApiService(): EdaApiClient {
        throw UnsupportedOperationException("Use repository fakes instead")
    }

    override fun getPlatformApiService(): PlatformApiClient {
        throw UnsupportedOperationException("Use repository fakes instead")
    }

    override fun getHubApiService(): HubApiClient {
        throw UnsupportedOperationException("Use repository fakes instead")
    }

    override fun evictInstance(instanceId: String) {
        evictedInstances.add(instanceId)
    }
}
