# AAPdroid

A lightweight Android app for remotely controlling [Ansible Automation Platform (AAP)](https://www.redhat.com/en/technologies/management/ansible). Authenticate with your AAP instance, browse job templates, launch playbooks, and monitor job status from your phone.

> **Disclaimer:** This project is not affiliated with or endorsed by Red Hat or the Ansible project.

## Features

- **Connect** to any AAP instance using a Personal Access Token (PAT)
- **Self-signed certificate** support for lab/dev environments
- **Auto-detect** AAP API version (2.4 controller vs 2.5+ gateway)
- **Browse** job templates with search and label filtering
- **Launch** jobs with optional extra variables (JSON)
- **Monitor** job status with live polling and stdout output
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
- AAP instance with API access
- Personal Access Token with appropriate permissions (write access required for launching jobs)

## Building

1. Clone the repository
2. Open in Android Studio (Ladybug or later recommended)
3. Sync Gradle
4. Run on a device or emulator

```bash
./gradlew assembleDebug
```

## Security

- Credentials are encrypted at rest using Google Tink with Android Keystore
- HTTPS-only enforced via network security config
- No hardcoded URLs or tokens
- Tokens never stored in plain text

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
