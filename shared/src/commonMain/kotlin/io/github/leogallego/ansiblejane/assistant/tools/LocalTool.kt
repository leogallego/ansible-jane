package io.github.leogallego.ansiblejane.assistant.tools

interface LocalTool : Tool {
    override val isDestructive: Boolean
        get() = false
}
