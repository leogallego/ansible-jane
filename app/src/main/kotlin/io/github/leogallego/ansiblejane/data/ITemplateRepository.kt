package io.github.leogallego.ansiblejane.data

interface ITemplateRepository {
    suspend fun getTemplates(
        page: Int = 1,
        search: String? = null,
        labelFilter: String? = null
    ): Result<TemplateListResult>

    suspend fun launchJob(templateId: Int, extraVars: String? = null): Result<Int>
}
