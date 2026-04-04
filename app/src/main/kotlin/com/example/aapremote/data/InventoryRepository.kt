package com.example.aapremote.data

import com.example.aapremote.model.Inventory
import com.example.aapremote.network.AapApiService

class InventoryRepository(private val apiService: AapApiService) {

    suspend fun getInventories(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<InventoryListResult> {
        return try {
            val response = apiService.getInventories(
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

    suspend fun getInventory(id: Int): Result<Inventory> {
        return try {
            Result.success(apiService.getInventory(id))
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
