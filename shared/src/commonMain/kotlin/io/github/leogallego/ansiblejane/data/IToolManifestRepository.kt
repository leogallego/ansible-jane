package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.ToolManifest

interface IToolManifestRepository {
    suspend fun saveManifest(instanceId: String, manifest: ToolManifest)
    suspend fun loadManifest(instanceId: String): ToolManifest?
    suspend fun deleteManifest(instanceId: String)
}
