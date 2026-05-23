package io.github.leogallego.ansiblejane.assistant.tools

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun ToolSpec.toToolDescriptor(): ToolDescriptor {
    val schema = compactSchema(parametersSchema)
    val properties = schema["properties"]?.jsonObject ?: emptyMap()
    val requiredNames = schema["required"]?.jsonArray
        ?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet()

    val required = mutableListOf<ToolParameterDescriptor>()
    val optional = mutableListOf<ToolParameterDescriptor>()

    properties.forEach { (paramName, paramValue) ->
        val prop = paramValue.jsonObject
        val descriptor = ToolParameterDescriptor(
            name = paramName,
            description = paramName,
            type = parseParamType(prop)
        )
        if (paramName in requiredNames) required.add(descriptor) else optional.add(descriptor)
    }

    return ToolDescriptor(
        name = name,
        description = description,
        requiredParameters = required,
        optionalParameters = optional
    )
}

private fun parseParamType(prop: JsonObject): ToolParameterType {
    val enumValues = prop["enum"]?.jsonArray
    if (enumValues != null) {
        return ToolParameterType.Enum(enumValues.map { it.jsonPrimitive.content }.toTypedArray())
    }
    return when (prop["type"]?.jsonPrimitive?.content) {
        "integer" -> ToolParameterType.Integer
        "number" -> ToolParameterType.Float
        "boolean" -> ToolParameterType.Boolean
        "array" -> ToolParameterType.List(ToolParameterType.String)
        else -> ToolParameterType.String
    }
}

private fun compactSchema(schema: JsonObject): JsonObject {
    val builder = mutableMapOf<String, JsonElement>()
    schema["type"]?.let { builder["type"] = it }
    schema["required"]?.jsonArray?.let { arr ->
        if (arr.isNotEmpty()) builder["required"] = arr
    }
    schema["properties"]?.jsonObject?.let { props ->
        val compactedProps = mutableMapOf<String, JsonElement>()
        props.forEach { (key, value) ->
            val prop = value.jsonObject
            val compacted = mutableMapOf<String, JsonElement>()
            prop["type"]?.let { compacted["type"] = it }
            prop["enum"]?.jsonArray?.let { enumArr ->
                if (enumArr.size > 8) {
                    compacted["enum"] = JsonArray(enumArr.take(8))
                } else {
                    compacted["enum"] = enumArr
                }
            }
            compactedProps[key] = JsonObject(compacted)
        }
        builder["properties"] = JsonObject(compactedProps)
    }
    return JsonObject(builder)
}
