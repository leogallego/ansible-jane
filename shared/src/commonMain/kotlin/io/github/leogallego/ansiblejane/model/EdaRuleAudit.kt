package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdaRuleAudit(
    val id: Int,
    val name: String = "",
    val status: String = "",
    @SerialName("activation_instance_id") val activationInstanceId: Int? = null,
    @SerialName("fired_at") val firedAt: String = "",
    @SerialName("rule_name") val ruleName: String? = null,
    @SerialName("rule_set_name") val ruleSetName: String? = null,
    @SerialName("ruleset_name") val rulesetName: String? = null,
    @SerialName("activation_name") val activationName: String? = null,
    @SerialName("created_at") val createdAt: String = ""
) {
    val displayRuleName: String
        get() = ruleName ?: name

    val displayRuleSetName: String
        get() = ruleSetName ?: rulesetName ?: ""
}
