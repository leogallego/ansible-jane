package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SurveySpec(
    val name: String = "",
    val description: String = "",
    val spec: List<SurveyQuestion> = emptyList()
)

@Serializable
data class SurveyQuestion(
    @SerialName("question_name") val questionName: String = "",
    @SerialName("question_description") val questionDescription: String = "",
    val required: Boolean = false,
    val type: String = "",
    val variable: String = "",
    val default: JsonElement? = null,
    val choices: JsonElement? = null,
    val min: Int? = null,
    val max: Int? = null
)
