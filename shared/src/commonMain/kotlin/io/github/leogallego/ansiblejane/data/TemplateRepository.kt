package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.JobTemplate
import io.github.leogallego.ansiblejane.model.LaunchRequest
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlin.coroutines.cancellation.CancellationException

class TemplateRepository(private val apiProvider: IAapApiProvider) : ITemplateRepository {

    override suspend fun getTemplates(
        page: Int,
        search: String?,
        labelFilter: String?
    ): Result<TemplateListResult> {
        return try {
            val response = apiProvider.getApiService().getJobTemplates(
                page = page,
                search = search,
                labelsFilter = labelFilter
            )
            Result.success(
                TemplateListResult(
                    templates = response.results,
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

    override suspend fun launchJob(templateId: Int, extraVars: String?): Result<Int> {
        return try {
            val request = LaunchRequest(extraVars = extraVars)
            val response = apiProvider.getApiService().launchJob(templateId, request)
            Result.success(response.job)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
