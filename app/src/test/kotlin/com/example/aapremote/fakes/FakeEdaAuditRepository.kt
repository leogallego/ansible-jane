package com.example.aapremote.fakes

import com.example.aapremote.data.EdaAuditResult
import com.example.aapremote.data.IEdaAuditRepository
import com.example.aapremote.model.EdaRuleAudit

class FakeEdaAuditRepository : IEdaAuditRepository {
    var auditRules = listOf<EdaRuleAudit>()
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var hasMore = false

    override suspend fun getAuditRules(page: Int, pageSize: Int): Result<EdaAuditResult> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(EdaAuditResult(auditRules, hasMore = hasMore, totalCount = auditRules.size))
    }
}
