package com.example.aapremote.data

import com.example.aapremote.model.EdaRuleAudit
import com.example.aapremote.network.AapApiProvider

class EdaAuditRepository(private val apiProvider: AapApiProvider) : IEdaAuditRepository {

    override suspend fun getAuditRules(page: Int, pageSize: Int): Result<EdaAuditResult> {
        return try {
            val response = apiProvider.getEdaApiService().getAuditRules(
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
            Result.failure(e)
        }
    }
}

data class EdaAuditResult(
    val auditRules: List<EdaRuleAudit>,
    val hasMore: Boolean,
    val totalCount: Int
)
