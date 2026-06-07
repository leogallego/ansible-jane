package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Inventory

interface IInventoryRepository {
    suspend fun getInventories(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<InventoryListResult>

    suspend fun getInventory(id: Int): Result<Inventory>
}
