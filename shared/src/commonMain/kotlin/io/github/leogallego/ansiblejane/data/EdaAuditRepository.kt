package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.EdaRuleAudit
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlin.coroutines.cancellation.CancellationException

class EdaAuditRepository(private val apiProvider: IAapApiProvider) : IEdaAuditRepository {

    override suspend fun getAuditRules(page: Int, pageSize: Int, search: String?): Result<EdaAuditResult> {
        return try {
            val response = apiProvider.getEdaApiService().getAuditRules(
                page = page,
                pageSize = pageSize,
                search = search
            )
            Result.success(
                EdaAuditResult(
                    auditRules = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
