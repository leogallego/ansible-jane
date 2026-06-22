package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.ITemplateRepository
import io.github.leogallego.ansiblejane.data.TemplateListResult
import io.github.leogallego.ansiblejane.model.JobTemplate

class FakeTemplateRepository : ITemplateRepository {
    var templates = listOf<JobTemplate>()
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var launchResult = 42
    var getTemplatesCalled = 0
    var launchJobCalled = 0
    var hasMore = false
    var lastRequestedPage = 0
    var lastSearchQuery: String? = null
    var lastLabelFilter: String? = null

    override suspend fun getTemplates(page: Int, search: String?, labelFilter: String?): Result<TemplateListResult> {
        getTemplatesCalled++
        lastRequestedPage = page
        lastSearchQuery = search
        lastLabelFilter = labelFilter
        if (shouldFail) return Result.failure(failureException)
        return Result.success(TemplateListResult(templates, hasMore = hasMore, totalCount = templates.size))
    }

    override suspend fun launchJob(templateId: Int, extraVars: String?): Result<Int> {
        launchJobCalled++
        if (shouldFail) return Result.failure(failureException)
        return Result.success(launchResult)
    }
}
