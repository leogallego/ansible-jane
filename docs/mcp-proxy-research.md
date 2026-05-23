# MCP Proxy/Router Solutions for Reducing Tool Context Overhead

**Date:** 2026-05-22 | **Scope:** Evaluate MCP proxy solutions for Claude Code tool context optimization + cross-pollination with Android ToolRouter

## Executive Summary

The MCP tool context overhead problem (14-17K tokens fixed tax from ~60 tools across 4 servers) has spawned a diverse ecosystem of proxy solutions, ranging from simple allowlist filters to full AI gateways. However, **Claude Code itself now ships with the most impactful solution: the Tool Search Tool**, a server-side feature that uses `defer_loading: true` to lazy-load tool schemas on demand, reducing context by 85%+ while improving tool selection accuracy. For external proxying, **mcp-filter** offers the best cost-benefit ratio for Claude Code users today (simple, proven 72% reduction, pip-installable), while **MCProxy** is the most architecturally complete solution for teams willing to run Rust infrastructure. For the Android ToolRouter, the most valuable patterns to borrow are **Tantivy-style full-text indexing** from MCProxy and **the two-phase discovery pattern** (stub first, full schema on demand) from mcp-compressor and the Claude API's own Tool Search Tool.

## Comparison Table

| Project | Language | Stars | Transport | Token Reduction | Approach | Maturity | Claude Code Ready |
|---------|----------|-------|-----------|-----------------|----------|----------|-------------------|
| **Claude Tool Search** (built-in) | N/A | N/A | API-level | ~85% (77K to 8.7K) | defer_loading + search | Production | Native |
| **mcp-filter** | Python | 49 | stdio, SSE | 72-91% | Static allowlist | Stable (v0.1.0) | Drop-in |
| **mcp-compressor** | Rust/TS/Py | 58 | stdio, HTTP | Not quantified | 4-level compression | Active (v0.23.0, 472 commits) | Drop-in |
| **MCProxy** | Rust | 19 | HTTP (streamable) | Not quantified | Aggregation + Tantivy search | Early (23 commits) | Requires HTTP transport |
| **mcp-lazy-proxy** | TypeScript | 0 | stdio only | 3.4-6.7x | Lazy stub loading | Pre-alpha (16 commits) | Drop-in |
| **Headroom** | Python/Rust | 1,947 | Library/Proxy/MCP | 60-95% | Output compression | Active (v0.22.2) | Via `headroom wrap claude` |
| **HyperTool MCP** | TypeScript | 153 | stdio | Proportional to filtering | Persona-based toolsets | Active (v0.0.45) | Partial (no hot-swap) |
| **Code Execution Mode** | Python | 332 | stdio | ~95% (30K to 200 tokens) | Single tool + code exec | Active (63 commits) | Drop-in |
| **IBM ContextForge** | Python | 3,760 | All transports | TOON compression (unquantified) | Gateway + virtual servers | Production (v1.0.1) | Via stdio wrapper |
| **mcp-funnel** | TypeScript | 153 | stdio | Claimed but unquantified | JSON config filtering | Early (288 commits) | Likely drop-in |
| **TBXark mcp-proxy** | Go | 692 | HTTP | None (aggregation only) | Multi-server aggregation | Stable (v0.43.2) | Via HTTP transport |

## Key Finding: Claude Code Built-in Tool Search

**The most important finding.** Anthropic shipped the Tool Search Tool (January 14, 2026) that addresses tool context bloat at the API level:

- Tools marked with `defer_loading: true` are NOT included in the system prompt prefix
- Claude sees only the Tool Search Tool (~500 tokens) plus 3-5 non-deferred "always-on" tools
- When Claude needs a tool, it searches using regex (`tool_search_tool_regex_20251119`) or BM25 (`tool_search_tool_bm25_20251119`) patterns
- **Triggers automatically** when tool definitions exceed ~10% of available context
- **Benchmarks:** 77K tokens reduced to 8.7K (85% reduction); Opus 4 accuracy improved from 49% to 74%
- Supports up to 10,000 tools; prompt caching is preserved
- [Documentation](https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool)

**Implication:** With ~60 tools, Claude Code may already be using this automatically. External proxies add value for: (a) reducing the catalog size for better selection accuracy, (b) security/filtering, and (c) output compression.

## Detailed Evaluations

### 1. mcp-filter — Best Quick Win

- **49 stars** | Python | pip/uvx installable
- **Proven 72% reduction** (50.1K to 13.7K tokens)
- Static allowlist with exact names or regex; drop-in stdio proxy
- **How it works:** wraps an upstream MCP server, intercepts `tools/list` responses, only passes through tools matching your allowlist
- **Claude Code integration:** add as a stdio MCP server in settings, point it at the upstream server
- **Limitations:** static per session, single upstream per instance, only 3 commits, no dynamic routing
- **Best for:** trimming GitHub from ~35 to ~8 tools you actually use
- [Repository](https://github.com/pro-vi/mcp-filter)

### 2. mcp-compressor — Most Actively Maintained

- **58 stars** | Rust/TS/Py | **472 commits, 87 releases** (v0.23.0) | Atlassian Labs
- **Two-phase approach:** compressed summaries first, full schema on demand via `get_tool_schema`
- 4 compression levels (low/medium/high/max); multi-language SDKs
- **How it works:** at `tools/list` time, returns shortened descriptions and simplified schemas. When the LLM selects a tool, it calls `get_tool_schema` to fetch the full parameter definition before execution
- **Claude Code integration:** drop-in stdio wrapper around any MCP server
- **Limitations:** extra round-trip per tool call; no quantitative benchmarks published; compression quality varies by tool
- **Best for:** keeping all tools accessible while reducing per-turn overhead
- [Repository](https://github.com/atlassian-labs/mcp-compressor)

### 3. MCProxy — Best Architecture

- **19 stars** | Rust | **Tantivy full-text search** + middleware stack
- Indexes all tools with BM25 scoring; exposes `maxToolsLimit` subset + `search_available_tools` meta-tool
- Per-server regex filtering + security middleware (blocks dangerous arguments)
- **How it works:** aggregates tools from multiple upstream servers, builds a Tantivy search index over tool names and descriptions. Exposes a configurable number of "top" tools plus a search tool. When the LLM needs something outside the top set, it searches
- **Claude Code integration:** HTTP-only (streamable), so requires Claude Code's HTTP MCP transport support
- **Limitations:** very early (23 commits), requires Rust toolchain to build, HTTP-only (no stdio)
- **Best for:** teams with many MCP servers who want intelligent tool discovery
- [Repository](https://github.com/igrigorik/MCProxy)

### 4. mcp-lazy-proxy (mcp-context-proxy) — Sound Concept, Zero Adoption

- **0 stars** | TypeScript | stdio only
- Stubs at ~54 tokens vs ~344 tokens full schema (6.5x reduction)
- Disk-cached schemas with 24h TTL; response compression included
- **How it works:** intercepts `tools/list` and returns minimal stubs (name + one-line description only, no parameter schemas). When a tool is called, loads the full schema for validation
- **Limitations:** zero community validation; built by an AI agent; no tests visible
- **Best for:** nothing — concept is valid but use mcp-compressor instead (same idea, better execution)
- [Repository](https://github.com/kira-autonoma/mcp-context-proxy)

## Expanded Search — Notable Additions

### 5. Headroom (1,947 stars) — Output Compression

- Compresses tool **outputs** (not schemas): 60-95% reduction with 97% accuracy
- Content-type routing: JSON/code/prose each get specialized compressors
- `headroom wrap claude` for Claude Code integration
- **Complementary** to schema reduction proxies — solves the other half of the token problem
- [Repository](https://github.com/chopratejas/headroom)

### 6. HyperTool MCP (153 stars) — Persona-Based Toolsets

- Curated tool groups (3-5 tools per "toolset") with named "personas" (web-dev, devops, etc.)
- Claude Code: partial support (`--equip-toolset` at launch, no hot-swap mid-session)
- **Interesting pattern:** pre-defined role-based tool bundles, similar to the "persona presets" idea
- [Repository](https://github.com/toolprint/hypertool-mcp)

### 7. Code Execution Mode (332 stars) — Radical Approach

- Replaces ALL tools with a single `run_python` tool; LLM discovers tools via Python code
- Claims 95% reduction (30K to 200 tokens) but adds Python/container overhead
- GPLv3 license
- **Too radical for most use cases** but interesting as an extreme point on the design spectrum
- [Repository](https://github.com/elusznik/mcp-server-code-execution-mode)

### 8. IBM ContextForge (3,760 stars) — Enterprise Gateway

- Full AI gateway with virtual server bundling, TOON compression, federation
- 300+ env vars, 55+ DB tables — massive overkill for individual use
- **Interesting for:** the TOON (Tool Output Optimization Notation) compression format concept
- [Repository](https://github.com/IBM/mcp-context-forge)

## Android ToolRouter Cross-Pollination

The current ToolRouter (`engine/ToolRouter.kt`) uses category-based routing with 7 categories, keyword stemming, overlap mapping, and scoring with boosts/penalties. Here's what's worth borrowing:

### Patterns Worth Borrowing

#### 1. Description-based search scoring (from MCProxy) — Highest impact
Currently `cherryPick()` scores only on tool name parts. Adding tool description text to scoring would catch many more relevant matches. Example: user asks "what's running?" — a tool with "running" in its description gets scored even if the name doesn't match.

**Effort:** Low. Add description words to the scoring loop in `cherryPick()`.

#### 2. Lazy schema loading (from mcp-compressor / Claude Tool Search)
Register MCP tools as lightweight stubs (name + description only). Load full parameter schemas only when selected for execution. Reduces memory footprint on constrained mobile devices.

**Effort:** Medium. Requires modifying `McpTool` to support two-phase loading.

#### 3. Lightweight inverted index (from MCProxy's Tantivy)
Replace `Category.stemmedKeywords` exact-match with a simple `Map<String, Set<Tool>>` inverted index over tool names + descriptions. TF-IDF or BM25 scoring handles synonyms and partial matches better than current stemming.

**Effort:** Medium. Replaces the category-based matching with a more flexible index.

#### 4. Persona presets (from HyperTool)
Named tool bundles (Operator, Admin, Developer) pre-filtering the catalog based on AAP user roles. Can auto-detect from user permissions returned by `/api/v2/me/`.

**Effort:** Low-medium. New data class + UI for role selection.

### Patterns NOT Worth Borrowing

- **Static allowlists** (mcp-filter) — too rigid for mobile, users need flexibility
- **Code execution intermediary** — inappropriate for mobile platform
- **Full gateway architecture** (ContextForge) — massive overkill for a single app
- **Output compression** (Headroom) — the app's ToolExecutor already does truncation and array capping

## Final Recommendations

### For Claude Code (this project)

| Priority | Action | Impact | Effort |
|----------|--------|--------|--------|
| 1 | **Check if Tool Search is already active** — with ~60 tools it may already be auto-enabled | Verify baseline | 5 min |
| 2 | **Apply mcp-filter to GitHub** — trim ~35 tools to ~8 you actually use | ~77% reduction on GitHub alone | 15 min |
| 3 | **Disable Slack** (already done) | ~3-4K saved per turn | Done |
| 4 | **Watch MCProxy** for stdio support | Best long-term solution | Track only |
| 5 | **Optionally add Headroom** if tool output sizes are a problem | 60-95% output reduction | 30 min |

### For Android ToolRouter (future improvements)

| Priority | Pattern | Source | Impact |
|----------|---------|--------|--------|
| 1 | Description-based scoring in `cherryPick()` | MCProxy | Better tool matching, low effort |
| 2 | Lazy MCP schema loading | mcp-compressor | Memory optimization for mobile |
| 3 | Inverted index over tool names + descriptions | MCProxy/Tantivy | More flexible than category keywords |
| 4 | Role-based persona presets | HyperTool | Nice-to-have UX improvement |

---

## Round 2: Deep Research — ToolRouter Improvements + On-Device ML (May 2026)

Seven parallel research streams across two rounds analyzed MCProxy source code, mcp-compressor internals, HyperTool personas, Koog ToolRegistry, Kai 9000 tool patterns, LiteRT-LM capabilities, and semantic search on mobile.

### Current ToolRouter Weaknesses

| Weakness | Location | Impact |
|----------|----------|--------|
| No-category-match returns empty tools | `ToolRouter.kt:197-200` | LLM gets zero tools for unrecognized queries |
| Stemmer too simplistic | `ToolRouter.kt:149-156` | Only handles plurals, misses -ing/-ed/-er |
| Tool descriptions ignored in scoring | `cherryPick():248-266` | Only scores on tool name parts |
| No synonym expansion | Throughout | "playbook" won't match "template" |
| Hardcoded categories | `ToolRouter.kt:15-94` | Adding categories requires code changes |
| Per-tool enable/disable not persisted | `ToolRouter.kt:171-178` | State lost on ViewModel recreation |

### On-Device ML Findings

**Gemma E2B/E4B for routing: REJECTED**
- No embedding API in LiteRT-LM (no `getEmbedding()` or hidden-layer access)
- Classification latency: 300ms (GPU) to 1.8s (CPU) — too slow for per-query pre-filter
- Memory: 676MB-1.7GB — competes with user's chat model
- Using the already-loaded chat LLM was considered but rejected (sequential bottleneck, single-conversation constraint)

**Model2Vec potion-base-8M: RECOMMENDED**
- 8 MB model, microsecond inference (lookup table, not neural network)
- 76 tool embeddings = 78 KB memory, cosine similarity sub-millisecond
- Android AAR: `io.gitlab.shubham0204:model2vec:v6`
- 91.9% of MiniLM quality — sufficient for short tool descriptions

**Kai 9000 Patterns**
- No tool routing — static allowlist of 8 simple tools for local Gemma models
- Small models fail on complex param schemas (enums, multiple params)
- `LIMITED_MODELS` blocklist bypasses tool system entirely
- Graceful fallback: retry without tools on parse failure

### LLM Tier Strategy

The ToolRouter only runs when an LLM is configured (no LLM = no assistant). Two capability tiers:

| Tier | Providers | Tool Routing Behavior |
|------|-----------|----------------------|
| **Full** | Frontier (OpenAI, Gemini, Claude), Custom (Ollama, Red Hat AI with capable models) | All categories, complex schemas, 30+ tools |
| **Simple** | Local on-device (Gemma E2B/E4B via LiteRT-LM) | Static allowlist, simple tools only, complexity filter |

### Revised Architecture

```
User Query (LLM always configured)
    │
    ▼
┌──────────────────────┐
│ Model Capability Tier│ ← Full or Simple
└────────┬─────────────┘
         ▼
┌──────────────────────┐
│ AAP Role Filter      │ ← Admin/Operator/Auditor
└────────┬─────────────┘
         │
    ├────┴────┐
    ▼         ▼
┌────────┐ ┌──────────┐
│Keyword │ │ Semantic  │
│Router  │ │(Model2Vec)│
└───┬────┘ └────┬─────┘
    └─────┬─────┘
          ▼
┌──────────────────────┐
│ RRF Fusion           │ ← keyword dominant, semantic fills gaps
└────────┬─────────────┘
         ▼
┌──────────────────────┐
│ Meta-search fallback │ ← search_available_tools if still empty
└────────┬─────────────┘
         ▼
   Tools sent to LLM
```

### Prioritized Implementation Plan

**Phase 1 — Quick Wins (3-4 days):** Synonym expansion, description-based scoring, stemmer expansion, AAP role filtering, meta-search fallback tool

**Phase 2 — Semantic Layer (3-4 days):** Model2Vec integration (8MB AAR), hybrid RRF fusion, precompute embeddings, index param names

**Phase 3 — Model Awareness (2-3 days, coordinates with #30):** Model capability tiers (Full/Simple), parameter complexity filter, graceful fallback on parse failure

**Phase 4 — Token Optimization (3-4 days):** Tool listing compression mapped to TokenSavingMode, AAP response compression

**Phase 5 — Architecture (3-5 days, future):** Middleware pipeline, config-driven categories, KMP extraction

### Researched and Rejected

| Pattern | Why not |
|---------|---------|
| Gemma E2B/E4B for routing | 300ms-1.8s latency, 676MB+ memory — Model2Vec is 100,000x faster at 8MB |
| EmbeddingGemma 300M | 200MB for 76 short descriptions — overkill |
| all-MiniLM-L6-v2 | 80-90MB + 50-200ms — overqualified |
| Lucene on Android | ~3-4MB dependency for 76 tools — simple inverted index suffices |
| Full lazy loading (wrapper tools) | Extra LLM round-trip hurts mobile latency |
| Replace keywords entirely | AAP terminology ("EDA", "WFJT") needs exact matching |
| Koog ToolRegistry adoption | Flat collection, no routing — we already exceed it |

### Tracking

- Issue #120 — ToolRouter improvements (all 5 phases)
- Issue #30 — Local LLM backend (Phase 3 coordinates with this)

---

## Sources

- [Anthropic Tool Search Tool docs](https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool)
- [Claude Code MCP Tool Search announcement](https://www.techbuddies.io/2026/01/18/how-claude-codes-new-mcp-tool-search-slashes-context-bloat-and-supercharges-ai-agents/)
- [Claude Code context reduction analysis](https://medium.com/@joe.njenga/claude-code-just-cut-mcp-context-bloat-by-46-9-51k-tokens-down-to-8-5k-with-new-tool-search-ddf9e905f734)
- [Speakeasy — Reducing MCP token usage by 100x](https://www.speakeasy.com/blog/how-we-reduced-token-usage-by-100x-dynamic-toolsets-v2)
- [StackOne — MCP Token Optimization](https://www.stackone.com/blog/mcp-token-optimization/)
- [mcp-filter](https://github.com/pro-vi/mcp-filter)
- [mcp-compressor](https://github.com/atlassian-labs/mcp-compressor)
- [MCProxy](https://github.com/igrigorik/MCProxy)
- [mcp-lazy-proxy](https://github.com/kira-autonoma/mcp-context-proxy)
- [Headroom](https://github.com/chopratejas/headroom)
- [HyperTool MCP](https://github.com/toolprint/hypertool-mcp)
- [Code Execution Mode](https://github.com/elusznik/mcp-server-code-execution-mode)
- [IBM ContextForge](https://github.com/IBM/mcp-context-forge)
- [Sentence-Embeddings-Android](https://github.com/shubham0204/Sentence-Embeddings-Android)
- [Model2Vec](https://github.com/MinishLab/model2vec)
- [potion-base-8M](https://huggingface.co/minishlab/potion-base-8M)
- [Aurelio Semantic Router](https://github.com/aurelio-labs/semantic-router)
- [RRF (Reciprocal Rank Fusion)](https://blog.serghei.pl/posts/reciprocal-rank-fusion-explained/)
- [LiteRT-LM](https://ai.google.dev/edge/litert-lm/overview)
- [EmbeddingGemma](https://developers.googleblog.com/en/introducing-embeddinggemma/)
- [FunctionGemma 270M](https://huggingface.co/google/functiongemma-270m-it)
- [PRISM on-device semantic selection](https://arxiv.org/abs/2510.15620)
- [Semantic Tool Selection (AWS)](https://dev.to/aws/reduce-agent-errors-and-token-costs-with-semantic-tool-selection-7mf)
- [MediaPipe Text Classifier](https://ai.google.dev/edge/mediapipe/solutions/customization/text_classifier)
- [Koog](https://github.com/JetBrains/koog)
- [Kotlin MCP SDK](https://github.com/modelcontextprotocol/kotlin-sdk)
