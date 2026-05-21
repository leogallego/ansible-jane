package com.example.aapremote.fakes

import com.example.aapremote.network.AapApiService
import com.example.aapremote.network.EdaApiService
import com.example.aapremote.network.IAapApiProvider

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
