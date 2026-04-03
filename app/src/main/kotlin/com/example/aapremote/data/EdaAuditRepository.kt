package com.example.aapremote.data

import com.example.aapremote.model.EdaRuleAudit
import com.example.aapremote.network.EdaApiService

class EdaAuditRepository(private val edaApiService: EdaApiService) {

    suspend fun getAuditRules(page: Int = 1, pageSize: Int = 20): Result<EdaAuditResult> {
        return try {
            val response = edaApiService.getAuditRules(
                page = page,
                pageSize = pageSize
            )
            Result.success(
                EdaAuditResult(
                    auditRules = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to load EDA audit events: ${e.message}", e))
        }
    }
}

data class EdaAuditResult(
    val auditRules: List<EdaRuleAudit>,
    val hasMore: Boolean,
    val totalCount: Int
)
