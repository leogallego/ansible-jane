package io.github.leogallego.ansiblejane.assistant.tools

import io.github.leogallego.ansiblejane.TestOnly
import kotlinx.serialization.json.JsonObject

@TestOnly
open class ToolStub(
    name: String,
    private val result: ToolResult = ToolResult(success = true),
    description: String = "Stub tool",
    override val serverLabel: String? = null,
    override val toolset: String? = null,
    private val onExecute: ((JsonObject) -> Unit)? = null
) : Tool {
    override val spec = ToolSpec(name, description, JsonObject(emptyMap()))
    override suspend fun execute(args: JsonObject): ToolResult {
        onExecute?.invoke(args)
        return result
    }
}
