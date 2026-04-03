package com.example.aapremote.data

import com.example.aapremote.model.JobTemplate
import com.example.aapremote.model.LaunchRequest
import com.example.aapremote.network.AapApiService

class TemplateRepository(private val apiService: AapApiService) {

    suspend fun getTemplates(
        page: Int = 1,
        search: String? = null,
        labelFilter: String? = null
    ): Result<TemplateListResult> {
        return try {
            val response = apiService.getJobTemplates(
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

    suspend fun launchJob(templateId: Int, extraVars: String? = null): Result<Int> {
        return try {
            val request = LaunchRequest(extraVars = extraVars)
            val response = apiService.launchJob(templateId, request)
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
