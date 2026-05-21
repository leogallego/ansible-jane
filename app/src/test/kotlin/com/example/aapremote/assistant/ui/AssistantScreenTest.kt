package com.example.aapremote.assistant.ui

// SKIPPED: AssistantViewModel has complex dependencies that cannot be easily faked:
// - McpServerManager (requires real coroutine scope and mcp client infrastructure)
// - OkHttpClient (networking)
// - Json (serialization)
// - List<LocalTool> (requires AAP API service instances)
//
// The ViewModel's init block connects to MCP servers and loads LLM config,
// making it impractical to construct with simple fakes.
// Consider extracting a simpler interface or adding a test constructor.
