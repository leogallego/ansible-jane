package com.example.aapremote.fakes

import com.example.aapremote.data.ITemplateRepository
import com.example.aapremote.data.TemplateListResult
import com.example.aapremote.model.JobTemplate

class FakeTemplateRepository : ITemplateRepository {
    var templates = listOf<JobTemplate>()
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var launchResult = 42
    var getTemplatesCalled = 0
    var launchJobCalled = 0

    override suspend fun getTemplates(page: Int, search: String?, labelFilter: String?): Result<TemplateListResult> {
        getTemplatesCalled++
        if (shouldFail) return Result.failure(failureException)
        return Result.success(TemplateListResult(templates, hasMore = false, totalCount = templates.size))
    }

    override suspend fun launchJob(templateId: Int, extraVars: String?): Result<Int> {
        launchJobCalled++
        if (shouldFail) return Result.failure(failureException)
        return Result.success(launchResult)
    }
}
