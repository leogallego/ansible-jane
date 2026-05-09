package com.example.aapremote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Credential(
    val id: Int,
    val name: String = "",
    val description: String = "",
    @SerialName("credential_type") val credentialType: Int = 0,
    @SerialName("summary_fields") val summaryFields: CredentialSummaryFields = CredentialSummaryFields(),
    val managed: Boolean = false
) {
    val credentialTypeName: String
        get() = summaryFields.credentialType?.name ?: ""

    val organizationName: String
        get() = summaryFields.organization?.name ?: ""
}

@Serializable
data class CredentialSummaryFields(
    @SerialName("credential_type") val credentialType: CredentialTypeRef? = null,
    val organization: OrganizationRef? = null
)

@Serializable
data class CredentialTypeRef(
    val id: Int,
    val name: String = ""
)

@Serializable
data class OrganizationRef(
    val id: Int,
    val name: String = ""
)
