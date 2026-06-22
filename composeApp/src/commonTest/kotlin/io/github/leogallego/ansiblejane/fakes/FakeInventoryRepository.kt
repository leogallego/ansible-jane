package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.IInventoryRepository
import io.github.leogallego.ansiblejane.data.InventoryListResult
import io.github.leogallego.ansiblejane.model.Inventory

class FakeInventoryRepository : IInventoryRepository {
    var inventories = listOf<Inventory>()
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var hasMore = false
    private var resultOverride: ((Int) -> Result<InventoryListResult>)? = null

    fun setResultForPage(block: (Int) -> Result<InventoryListResult>) {
        resultOverride = block
    }

    override suspend fun getInventories(page: Int, pageSize: Int, search: String?): Result<InventoryListResult> {
        resultOverride?.let { return it(page) }
        if (shouldFail) return Result.failure(failureException)
        return Result.success(InventoryListResult(inventories, hasMore = hasMore, totalCount = inventories.size))
    }

    override suspend fun getInventory(id: Int): Result<Inventory> {
        if (shouldFail) return Result.failure(failureException)
        val inv = inventories.find { it.id == id }
        return if (inv != null) Result.success(inv) else Result.failure(RuntimeException("Not found"))
    }
}
