package com.example.aapremote.data

import com.example.aapremote.model.JobTemplate
import com.example.aapremote.model.LaunchRequest
import com.example.aapremote.network.AapApiProvider

class TemplateRepository(private val apiProvider: AapApiProvider) : ITemplateRepository {

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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun launchJob(templateId: Int, extraVars: String?): Result<Int> {
        return try {
            val request = LaunchRequest(extraVars = extraVars)
            val response = apiProvider.getApiService().launchJob(templateId, request)
            Result.success(response.job)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class TemplateListResult(
    val templates: List<JobTemplate>,
    val hasMore: Boolean,
    val totalCount: Int
)
