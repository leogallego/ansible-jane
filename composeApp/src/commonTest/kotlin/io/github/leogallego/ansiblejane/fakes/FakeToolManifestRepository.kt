package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.IToolManifestRepository
import io.github.leogallego.ansiblejane.model.ToolManifest

class FakeToolManifestRepository : IToolManifestRepository {
    private val manifests = mutableMapOf<String, ToolManifest>()

    override suspend fun saveManifest(instanceId: String, manifest: ToolManifest) {
        manifests[instanceId] = manifest
    }

    override suspend fun loadManifest(instanceId: String): ToolManifest? = manifests[instanceId]

    override suspend fun deleteManifest(instanceId: String) {
        manifests.remove(instanceId)
    }
}
