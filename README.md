# AAPdroid

A lightweight Android app for remotely controlling [Ansible Automation Platform (AAP)](https://www.redhat.com/en/technologies/management/ansible). Authenticate with your AAP instance, browse job templates, launch playbooks, and monitor job status from your phone.

> **Disclaimer:** This project is not affiliated with or endorsed by Red Hat or the Ansible project.

## Features

- **Connect** to any AAP instance using a Personal Access Token (PAT)
- **Self-signed certificate** support for lab/dev environments
- **Auto-detect** AAP API version (2.4 controller vs 2.5+ gateway)
- **Browse** job templates and workflow templates with search and label filtering
- **Launch** jobs and workflows with optional extra variables (JSON)
- **Monitor** job status with live polling and stdout output
- **Monitor** workflow job status with sub-job tracking
- **Recent jobs** history

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
2. Enter your Personal Access Token (PAT) — generate one from your AAP user settings under **Tokens**
3. For lab/dev environments with self-signed certificates, toggle **Allow self-signed certificates**
4. Tap **Login**

### Browsing Templates

The **Templates** tab has two segments accessible via the segmented buttons at the top:

- **Job Templates** — standard playbook-based templates
- **Workflow Templates** — multi-step workflow templates

Both segments support:

- **Search** — type in the search bar to filter by name
- **Label filtering** — tap a label chip to filter by label; tap again to clear
- **Pagination** — scroll to the bottom to load more templates automatically
- **Pull-to-refresh** — pull down on the list to reload data from the server (useful when templates are added or modified on the AAP side while the app is open)

### Launching Jobs

1. Tap the play button on a template card (only visible if your user has launch permission)
2. If the template accepts extra variables, an input dialog appears first — enter valid JSON
3. Confirm the launch in the confirmation dialog
4. The app navigates to the job status screen automatically

### Monitoring Jobs

- **Job status** — shows job name, status badge, template name, start/finish/elapsed times, and stdout output
- **Workflow job status** — same details plus a **Sub-Jobs** section listing each workflow node with its status; tap any sub-job to expand it and view its stdout output inline
- Status auto-updates every 5 seconds while the job is running; polling stops when the job completes

### Activity Tab

The **Activity** tab shows recent jobs (both regular and workflow) sorted by creation time. Tap any job to view its status.

### Settings

Access settings via the gear icon in the top bar. Shows the connected server URL and a logout button.

## Security

- Credentials are encrypted at rest using Google Tink with Android Keystore
- HTTPS-only enforced via network security config
- No hardcoded URLs or tokens
- Tokens never stored in plain text

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
