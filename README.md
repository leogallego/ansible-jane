# Ansible Jane

A multiplatform app (Android and Desktop) for managing [Ansible Automation Platform (AAP)](https://www.redhat.com/en/technologies/management/ansible). Authenticate with your AAP instance, browse job templates, launch playbooks, monitor job status, and interact with Jane, an AI assistant that works with local models (as small as qwen3:8b via Ollama) or frontier providers (OpenAI-compatible, Gemini, OpenRouter) through tool-use and MCP.

> **Disclaimer:** This project is not affiliated with or endorsed by Red Hat or the Ansible project.
>
> This is a vibe-coded, spec-driven, AI-assisted developed application. It comes with **no warranties of any kind** - use at your own risk. This project exists purely for research and investigation purposes.

## Install

### Android

- **F-Droid** - add this repo to your F-Droid client: `https://leogallego.github.io/ansible-jane/fdroid/repo`
- **GitHub Releases** - download the latest APK from [Releases](https://github.com/leogallego/ansible-jane/releases/latest)
- **Obtainium** - point [Obtainium](https://github.com/ImranR98/Obtainium) to this repo for automatic updates

### Desktop

- **Build from source** - build a runnable JAR with `./gradlew :composeApp:desktopJar`
- **Native package** - build a platform-specific installer with `./gradlew :composeApp:packageDeb` (Linux .deb), `./gradlew :composeApp:packageRpm` (Linux .rpm), or `./gradlew :composeApp:packageDmg` (macOS)

## Features

### AAP Management

- **Multi-instance support** - connect to multiple AAP instances simultaneously and switch between them
- **Connect** to any AAP instance using a Personal Access Token (PAT)
- **Self-signed certificate** support for lab/dev environments
- **Instance discovery** - auto-detect platform type (AAP, AWX, Jewel), component versions, and API version
- **Dashboard** - home screen with job statistics and recent failures at a glance
- **Browse** job templates and workflow templates with search and label filtering
- **Launch** jobs and workflows with optional extra variables (JSON)
- **Monitor** job status with live polling and stdout output
- **Workflow visualization** - workflow job status with node cards, connectors, and inline sub-job stdout
- **Workflow approvals** - receive push notifications for pending approvals and approve/deny directly from the app
- **In-app notifications** - notification bell with pending approval count
- **Infrastructure** - browse inventories and hosts with search
- **Schedules and EDA** - view schedules and EDA audit logs with search
- **Recent jobs** history via the Activity tab

### AI Assistant (Jane)

- **Natural-language interaction** with your AAP instance using tool-use LLMs
- **61 local tools** - call AAP APIs directly via Ktor with zero latency (jobs, inventories, hosts, projects, credentials, EDA, schedules, approvals, platform config, and more)
- **MCP integration** - connect to [aap-mcp-server](https://github.com/ansible/aap-mcp-server) for additional tool coverage
- **Per-toolset MCP endpoints** - auto-detects 6 toolset-specific endpoints (jobs, inventory, monitoring, users, security, configuration) to reduce token cost
- **Category-based tool routing** - ToolRouter selects only relevant tools per query, keeping LLM context small
- **Token saving modes** - 3-tier control (Standard / Token Saver / Tools Only) for schema detail, tool count caps, and conversation compaction
- **Bring your own model** - local models via Ollama (qwen3:8b and up) or cloud providers (OpenAI-compatible, Gemini, OpenRouter)
- **Per-provider config** - save and switch between multiple LLM provider configurations
- **Credential backup** - password-protected export/import of all credentials and LLM configs

### Settings

- **Tabbed settings** - General, Instances, Agent, and Tools tabs
- **Theme** - system, light, or dark mode
- **Timezone and time format** - configurable display preferences
- **MCP server management** - enable/disable, add custom servers, per-server read-only toggle

## Screenshots

*Coming soon*

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin (Multiplatform) |
| UI | Compose Multiplatform + Material 3 (Material You) |
| Architecture | MVVM + Unidirectional Data Flow |
| AI Engine | [Koog](https://github.com/JetBrains/koog) (JetBrains AI Agent Framework) |
| Networking | Ktor + Kotlin Serialization + Coroutines |
| DI | Koin |
| Security | DataStore + cryptography-kotlin AES-256-GCM (Android Keystore on Android, PKCS12 keystore on Desktop) |

## Requirements

- **Android:** Android 12+ (API 31)
- **Desktop:** JDK 17+ (macOS, Linux, Windows)
- AAP 2.5+ with Gateway (all API access goes through the gateway)
- Personal Access Token with appropriate permissions (write access required for launching jobs)

## Building

1. Clone the repository
2. Open in Android Studio (Ladybug or later recommended) or IntelliJ IDEA
3. Sync Gradle

### Android

```bash
./gradlew assembleDebug
```

### Desktop

```bash
# Runnable JAR
./gradlew :composeApp:desktopJar

# Native package (choose one)
./gradlew :composeApp:packageDeb   # Linux .deb
./gradlew :composeApp:packageRpm   # Linux .rpm
./gradlew :composeApp:packageDmg   # macOS .dmg
```

## Usage

### Connecting to AAP

1. Enter your AAP instance URL (e.g., `https://aap.example.com`)
2. Optionally set an **Alias** (e.g., "Production", "Dev") for easy identification
3. Enter your Personal Access Token (PAT) - generate one from your AAP user settings under **Tokens**
4. For lab/dev environments with self-signed certificates, toggle **Allow self-signed certificates**
5. Tap **Connect**

### Managing Multiple Instances

You can connect to multiple AAP instances simultaneously:

- **Add** - in Settings > Instances, tap **Add Instance** to connect to another AAP instance
- **Switch** - tap an inactive instance card to switch; all data refreshes automatically
- **View details** - tap the active instance card to see URL, alias, platform type, component versions, and certificate trust status
- **Discover** - tap **Discover Instance Info** to detect platform type (AAP/AWX/Jewel), controller/gateway/EDA versions, and available components
- **Remove** - tap the logout icon on any instance card (with confirmation) to disconnect
- The **top bar** shows the active instance label (alias or hostname) as a subtitle

### Dashboard

The **Dashboard** tab is the home screen, showing:

- **Job statistics** - total jobs, success/failure counts
- **Recent failures** - quick access to failed jobs for troubleshooting

### Browsing Templates

The **Templates** tab has two segments accessible via the segmented buttons at the top:

- **Job Templates** - standard playbook-based templates
- **Workflow Templates** - multi-step workflow templates

Both segments support:

- **Search** - type in the search bar to filter by name
- **Label filtering** - tap a label chip to filter by label; tap again to clear
- **Pagination** - scroll to the bottom to load more templates automatically
- **Pull-to-refresh** - pull down on the list to reload data from the server

### Launching Jobs

1. Tap the play button on a template card (only visible if your user has launch permission)
2. If the template accepts extra variables, an input dialog appears first - enter valid JSON
3. Confirm the launch in the confirmation dialog
4. The app navigates to the job status screen automatically

### Monitoring Jobs

- **Job status** - shows job name, status badge, template name, start/finish/elapsed times, and stdout output
- **Workflow job status** - node card visualization with connectors showing the workflow graph; tap any node to expand and view its stdout output inline
- Status auto-updates every 5 seconds while the job is running; polling stops when the job completes

### Workflow Approvals

When a workflow job reaches an approval node:

- **Push notifications** - receive an Android notification with approve/deny actions
- **In-app bell** - notification bell icon in the top bar shows pending approval count
- **Approval detail** - tap to view the approval request with full context, then approve or deny

### Activity Tab

The **Activity** tab shows recent jobs (both regular and workflow) sorted by creation time. Tap any job to view its status.

### Infrastructure Tab

The **Infrastructure** tab shows inventories and hosts from the active AAP instance with search support. Tap an inventory to see its hosts and details.

### Jane AI Assistant

Access Jane via the chat icon. Ask questions in natural language about your AAP instance:

- "What job templates do I have?"
- "Show me failed jobs from the last hour"
- "Launch the deploy-webserver template"
- "What hosts are in the production inventory?"

Jane selects the right tools automatically based on your query. Configure your LLM provider in Settings > Agent.

### Settings

Access settings via the gear icon in the top bar:

- **General** - theme mode, timezone, time format
- **Instances** - add, switch, remove instances; view details and discover instance info
- **Agent** - configure LLM providers, switch active provider, fetch available models, clear chat history
- **Tools** - enable/disable MCP, manage MCP server connections, toggle read-only mode per server

## Security

- Per-instance credentials encrypted at rest using cryptography-kotlin AES-256-GCM (Android Keystore on Android, PKCS12 keystore at `~/.ansiblejane/` on Desktop)
- HTTPS-only enforced via network security config
- No hardcoded URLs or tokens
- Tokens never stored in plain text
- Per-instance 401 handling - token expiry on one instance does not affect others
- Password-protected credential backup and restore

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
