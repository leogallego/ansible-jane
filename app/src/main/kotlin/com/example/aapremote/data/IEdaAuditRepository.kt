package com.example.aapremote.data

interface IEdaAuditRepository {
    suspend fun getAuditRules(page: Int = 1, pageSize: Int = 20): Result<EdaAuditResult>
}
