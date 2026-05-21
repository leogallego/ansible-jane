package com.example.aapremote.data

import com.example.aapremote.model.Inventory

interface IInventoryRepository {
    suspend fun getInventories(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<InventoryListResult>

    suspend fun getInventory(id: Int): Result<Inventory>
}
