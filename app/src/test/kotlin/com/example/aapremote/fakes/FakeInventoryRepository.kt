package com.example.aapremote.fakes

import com.example.aapremote.data.IInventoryRepository
import com.example.aapremote.data.InventoryListResult
import com.example.aapremote.model.Inventory

class FakeInventoryRepository : IInventoryRepository {
    var inventories = listOf<Inventory>()
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")

    override suspend fun getInventories(page: Int, pageSize: Int, search: String?): Result<InventoryListResult> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(InventoryListResult(inventories, hasMore = false, totalCount = inventories.size))
    }

    override suspend fun getInventory(id: Int): Result<Inventory> {
        if (shouldFail) return Result.failure(failureException)
        val inv = inventories.find { it.id == id }
        return if (inv != null) Result.success(inv) else Result.failure(RuntimeException("Not found"))
    }
}
