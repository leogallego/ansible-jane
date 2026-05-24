package io.github.leogallego.ansiblejane.assistant.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.serialization.TypeToken
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

@Serializable
class EmptyArgs

abstract class AapLocalTool<TArgs : Any>(
    argsType: TypeToken,
    private val argsSerializer: KSerializer<TArgs>,
    name: String,
    description: String,
    override val destructive: Boolean = false
) : SimpleTool<TArgs>(argsType, name, description), LocalTool {

    override val spec: ToolSpec by lazy { descriptorToSpec(descriptor) }

    override suspend fun execute(args: JsonObject): ToolResult = try {
        val typedArgs = bridgeJson.decodeFromJsonElement(argsSerializer, args)
        ToolResult(success = true, data = execute(typedArgs))
    } catch (e: Exception) {
        ToolResult(
            success = false,
            data = "Error: ${e.message}",
            errorType = ErrorType.SERVER_ERROR
        )
    }

    companion object {
        val bridgeJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }

        fun descriptorToSpec(descriptor: ToolDescriptor): ToolSpec {
            val properties = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            val requiredNames = mutableListOf<String>()

            for (param in descriptor.requiredParameters) {
                properties[param.name] = paramToSchema(param)
                requiredNames.add(param.name)
            }
            for (param in descriptor.optionalParameters) {
                properties[param.name] = paramToSchema(param)
            }

            val schema = buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", JsonObject(properties))
                if (requiredNames.isNotEmpty()) {
                    put("required", buildJsonArray {
                        requiredNames.forEach { add(JsonPrimitive(it)) }
                    })
                }
            }
            return ToolSpec(
                name = descriptor.name,
                description = descriptor.description,
                parametersSchema = schema
            )
        }

        private fun paramToSchema(param: ToolParameterDescriptor): JsonObject = buildJsonObject {
            val typeStr = when (param.type) {
                is ToolParameterType.Integer -> "integer"
                is ToolParameterType.Float -> "number"
                is ToolParameterType.Boolean -> "boolean"
                is ToolParameterType.List -> "array"
                is ToolParameterType.Enum -> "string"
                else -> "string"
            }
            put("type", JsonPrimitive(typeStr))
            put("description", JsonPrimitive(param.description))
            if (param.type is ToolParameterType.Enum) {
                val entries = (param.type as ToolParameterType.Enum).entries
                put("enum", buildJsonArray {
                    entries.forEach { add(JsonPrimitive(it)) }
                })
            }
        }
    }
}
