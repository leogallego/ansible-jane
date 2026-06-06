# Reference Projects

Projects used as reference, inspiration, or integration targets for Ansible Jane. All GitHub URLs verified.

## Architecture References

Projects whose code or patterns directly influenced Ansible Jane's design.

| Project | Language | Stars | What We Used | License | Link |
|---------|----------|-------|--------------|---------|------|
| **Kai 9000** | Kotlin (KMP) | 838 | MCP client via OkHttp (~130 LOC), universal Tool interface, multi-provider LLM chain, KMP architecture | Apache 2.0 | [GitHub](https://github.com/SimonSchubert/Kai) |
| **PennywiseAI** | Kotlin | 462 | Validated dynamic system prompt injection for domain context; on-device inference via LiteRT-LM with streaming Flow | AGPL 3.0 | [GitHub](https://github.com/sarim2000/pennywiseai-tracker) |

## Framework Evaluations

| Project | Language | Stars | Status | License | Link |
|---------|----------|-------|--------|---------|------|
| **Koog** (JetBrains) | Kotlin | 4,220 | Evaluated as replacement for custom LLM/MCP/agent code (~1,260 LOC); deferred pending OkHttp upgrade and Kotlin 2.3. See `docs/specs/koog-evaluation.md` | Apache 2.0 | [GitHub](https://github.com/JetBrains/koog) |
| **ADK for Kotlin** (Google) | Kotlin | 26 | v0.1.0 (experimental). Multi-agent systems with MCP tool support, on-device Gemini Nano via ML Kit, cloud+on-device hybrid orchestration. Uses KSP for tool codegen. Very early but directly relevant to our agent architecture | Apache 2.0 | [GitHub](https://github.com/google/adk-kotlin) |

## MCP Servers (Integration Targets)

| Project | Language | Stars | Role | License | Link |
|---------|----------|-------|------|---------|------|
| **aap-mcp-server** | TypeScript | 27 | Official AAP MCP server — primary integration target for Controller, Gateway, and EDA resources | Apache 2.0 | [GitHub](https://github.com/ansible/aap-mcp-server) |
| **ansible-know-mcp** | Python | 0 | Ansible knowledge layer — module docs, conceptual guides, troubleshooting search | GPL 3.0 | [GitHub](https://github.com/leogallego/ansible-know-mcp) |

## AI Agent Skills (Sources)

Skills bundled in `skills/` for AI-assisted development. See `docs/skills-reference.md` for usage.

| Project | Stars | Contents | License | Link |
|---------|-------|----------|---------|------|
| **android/skills** | 5,293 | Edge-to-edge, Navigation 3 | Apache 2.0 | [GitHub](https://github.com/android/skills) |
| **chrisbanes/skills** | 671 | Compose animations, focus, modifiers, recomposition, side effects, state, testing | Apache 2.0 | [GitHub](https://github.com/chrisbanes/skills) |
| **javiercamarenatriguero/android-skills** | 3 | Compose patterns, coroutines, Koin DI, unit testing, Gradle config | Apache 2.0 | [GitHub](https://github.com/javiercamarenatriguero/android-skills) |
| **Meet-Miyani/compose-skill** | 221 | Comprehensive Compose/KMP reference: MVI, Nav 3, Ktor, Room, DataStore, Paging, Coil | MIT | [GitHub](https://github.com/Meet-Miyani/compose-skill) |
| **anhvt52/jetpack-compose-skills** | 89 | Compose best practices + M3 migration + accessibility + common LLM mistakes. Used: accessibility reference | MIT | [GitHub](https://github.com/anhvt52/jetpack-compose-skills) |
| **new-silvermoon/awesome-android-agent-skills** | 819 | 16 Android agent skills. Used: Retrofit networking, Gradle build performance | Apache 2.0 | [GitHub](https://github.com/new-silvermoon/awesome-android-agent-skills) |

## Projects to Investigate

Discovered from [privacytoolslist.com/ai](https://privacytoolslist.com/ai/) (2026-05-23). Worth studying for patterns, UX, or future integration.

### Mobile AI Apps

| Project | Language | Stars | What to Look At | License | Link |
|---------|----------|-------|-----------------|---------|------|
| **PocketPal AI** | TypeScript (React Native) | 7,027 | Model management UX, on-device inference patterns, model download/caching | MIT | [GitHub](https://github.com/a-ghorbani/pocketpal-ai) |
| **ChatterUI** | TypeScript (React Native) | 2,425 | Mobile chat frontend UX, multi-backend support | AGPL 3.0 | [GitHub](https://github.com/Vali-98/ChatterUI) |
| **Off-Grid Mobile AI** | TypeScript | 2,195 | Fully offline mobile AI: text, vision, image gen without internet | MIT | [GitHub](https://github.com/alichherawalla/off-grid-mobile-ai) |

### On-Device Inference

| Project | Language | Stars | What to Look At | License | Link |
|---------|----------|-------|-----------------|---------|------|
| **ExecuTorch** | Python/C++ | 4,651 | PyTorch on-device deployment for mobile/embedded — relevant if we run models directly on Android | Other | [GitHub](https://github.com/pytorch/executorch) |

### Agent Frameworks

| Project | Language | Stars | What to Look At | License | Link |
|---------|----------|-------|-----------------|---------|------|
| **MobileRun** (fka DroidRun) | Python | 8,395 | LLM-agnostic mobile agent — natural language device automation | MIT | [GitHub](https://github.com/droidrun/mobilerun) |

### Local Inference Servers

| Project | Language | Stars | What to Look At | License | Link |
|---------|----------|-------|-----------------|---------|------|
| **RamaLama** | Python | — | Local AI model runner from Red Hat/Fedora ecosystem, container-based | Apache 2.0 | [GitHub](https://github.com/containers/ramalama) |

## See Also

- `docs/mcp-proxy-research.md` — MCP proxy/router solutions relevant to ToolRouter optimization
