# Ansible Jane

A lightweight Android app for managing [Ansible Automation Platform (AAP)](https://www.redhat.com/en/technologies/management/ansible). Authenticate with your AAP instance, browse job templates, launch playbooks, monitor job status, and interact with Jane, an AI assistant that works with local models (as small as qwen2.5:7b via Ollama) or frontier providers (OpenAI-compatible, Gemini, OpenRouter) through tool-use and MCP.

> **Disclaimer:** This project is not affiliated with or endorsed by Red Hat or the Ansible project.
>
> This is a vibe-coded, spec-driven, AI-assisted developed application. It comes with **no warranties of any kind** - use at your own risk. This project exists purely for research and investigation purposes.

## Install

- **F-Droid** - add this repo to your F-Droid client: `https://leogallego.github.io/ansible-jane/fdroid/repo`
- **GitHub Releases** - download the latest APK from [Releases](https://github.com/leogallego/ansible-jane/releases/latest)
- **Obtainium** - point [Obtainium](https://github.com/ImranR98/Obtainium) at this repo for automatic updates

## Features

- **Multi-instance support** - connect to multiple AAP instances simultaneously and switch between them
- **Connect** to any AAP instance using a Personal Access Token (PAT)
- **Self-signed certificate** support for lab/dev environments
- **Auto-detect** AAP API version (2.4 controller vs 2.5+ gateway)
- **Browse** job templates and workflow templates with search and label filtering
- **Launch** jobs and workflows with optional extra variables (JSON)
- **Monitor** job status with live polling and stdout output
- **Monitor** workflow job status with sub-job tracking
- **Recent jobs** history
- **Infrastructure** - browse inventories and hosts
- **Jane AI assistant** - natural-language interaction with your AAP instance using tool-use and MCP
- **Bring your own model** - local models via Ollama (qwen2.5:7b and up) or cloud providers (OpenAI-compatible, Gemini, OpenRouter)
- **Schedules and EDA** - view schedules and EDA audit logs

## Screenshots

*Coming soon*

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (Material You) |
| Architecture | MVVM + Unidirectional Data Flow |
| Networking | Retrofit + Kotlin Serialization + Coroutines |
| DI | Koin |
| Security | Jetpack DataStore + Google Tink (Android Keystore-backed) |

## Requirements

- Android 12+ (API 31)
- AAP 2.5+ with Gateway (all API access goes through the gateway)
- Personal Access Token with appropriate permissions (write access required for launching jobs)

## Building

1. Clone the repository
2. Open in Android Studio (Ladybug or later recommended)
3. Sync Gradle
4. Run on a device or emulator

```bash
./gradlew assembleDebug
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

- **Add** - in Settings, tap **Add Instance** to connect to another AAP instance
- **Switch** - tap an inactive instance pill to switch; all data refreshes automatically
- **View details** - tap the active instance pill to see URL, alias, API version, and certificate trust status
- **Remove** - tap the **x** on any instance pill (with confirmation) to disconnect; if the active instance is removed, the next one is auto-promoted
- The **top bar** shows the active instance label (alias or hostname) as a subtitle

### Browsing Templates

The **Templates** tab has two segments accessible via the segmented buttons at the top:

- **Job Templates** - standard playbook-based templates
- **Workflow Templates** - multi-step workflow templates

Both segments support:

- **Search** - type in the search bar to filter by name
- **Label filtering** - tap a label chip to filter by label; tap again to clear
- **Pagination** - scroll to the bottom to load more templates automatically
- **Pull-to-refresh** - pull down on the list to reload data from the server (useful when templates are added or modified on the AAP side while the app is open)

### Launching Jobs

1. Tap the play button on a template card (only visible if your user has launch permission)
2. If the template accepts extra variables, an input dialog appears first - enter valid JSON
3. Confirm the launch in the confirmation dialog
4. The app navigates to the job status screen automatically

### Monitoring Jobs

- **Job status** - shows job name, status badge, template name, start/finish/elapsed times, and stdout output
- **Workflow job status** - same details plus a **Sub-Jobs** section listing each workflow node with its status; tap any sub-job to expand it and view its stdout output inline
- Status auto-updates every 5 seconds while the job is running; polling stops when the job completes

### Activity Tab

The **Activity** tab shows recent jobs (both regular and workflow) sorted by creation time. Tap any job to view its status.

### Infrastructure Tab

The **Infrastructure** tab shows inventories and hosts from the active AAP instance. Tap an inventory to see its hosts and details.

### Settings

Access settings via the gear icon in the top bar. Manage connected instances (add, switch, remove, view details) and log out.

## Security

- Per-instance credentials encrypted at rest using Google Tink AES-256-GCM with Android Keystore
- HTTPS-only enforced via network security config
- No hardcoded URLs or tokens
- Tokens never stored in plain text
- Per-instance 401 handling - token expiry on one instance does not affect others

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
