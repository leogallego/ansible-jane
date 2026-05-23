package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.network.AapApiService
import io.github.leogallego.ansiblejane.network.EdaApiService
import io.github.leogallego.ansiblejane.network.IAapApiProvider

class FakeAapApiProvider : IAapApiProvider {
    val evictedInstances = mutableListOf<String>()

    override fun getApiService(): AapApiService {
        throw UnsupportedOperationException("Use repository fakes instead")
    }

    override fun getEdaApiService(): EdaApiService {
        throw UnsupportedOperationException("Use repository fakes instead")
    }

    override fun evictInstance(instanceId: String) {
        evictedInstances.add(instanceId)
    }
}
