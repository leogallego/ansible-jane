package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Inventory
import io.github.leogallego.ansiblejane.network.IAapApiProvider

class InventoryRepository(private val apiProvider: IAapApiProvider) : IInventoryRepository {

    override suspend fun getInventories(
        page: Int,
        pageSize: Int,
        search: String?
    ): Result<InventoryListResult> {
        return try {
            val response = apiProvider.getApiService().getInventories(
                page = page,
                pageSize = pageSize,
                search = search
            )
            Result.success(
                InventoryListResult(
                    inventories = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInventory(id: Int): Result<Inventory> {
        return try {
            Result.success(apiProvider.getApiService().getInventory(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class InventoryListResult(
    val inventories: List<Inventory>,
    val hasMore: Boolean,
    val totalCount: Int
)
