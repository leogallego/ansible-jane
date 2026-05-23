package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.EdaAuditResult
import io.github.leogallego.ansiblejane.data.IEdaAuditRepository
import io.github.leogallego.ansiblejane.model.EdaRuleAudit

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
