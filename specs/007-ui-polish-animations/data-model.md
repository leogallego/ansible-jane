# Data Model: UI Polish — Animations and Micro-Interactions

**Date**: 2026-04-03
**Branch**: `007-ui-polish-animations`

## Entities

### AppError (NEW)

Typed error representation replacing plain `String` error messages across all UiState sealed interfaces.

```
AppError (sealed class)
├── type: ErrorType (derived from sealed subclass)
├── title: String (user-facing, e.g., "Network Error")
├── message: String (user-facing description)
├── icon: ImageVector (Material icon for the error type)
├── detail: ErrorDetail? (optional, for expandable section)
│
├── Network
│   └── message: String (e.g., "Unable to reach the server. Check your connection.")
├── Auth
│   └── message: String (e.g., "Authentication failed. Please log in again.")
├── Server
│   └── message: String (e.g., "The server encountered an error.")
│   └── statusCode: Int
├── Ssl
│   └── message: String (e.g., "SSL certificate verification failed.")
└── Unknown
    └── message: String (fallback)

ErrorDetail (data class)
├── statusCode: Int? (HTTP status code, if available)
├── url: String? (requested URL, if available)
└── rawMessage: String? (original exception message)
```

### Factory Function

```
AppError.from(throwable: Throwable): AppError
```

Classification order (first match wins):
1. `SSLHandshakeException` / `SSLException` → `AppError.Ssl`
2. `UnknownHostException` / `ConnectException` / `SocketTimeoutException` → `AppError.Network`
3. `IOException` (generic) → `AppError.Network`
4. `HttpException` with 401/403 → `AppError.Auth`
5. `HttpException` with 5xx → `AppError.Server`
6. `HttpException` with other codes → `AppError.Server`
7. Everything else → `AppError.Unknown`

### UiState Changes

All `Error` variants across 8 sealed interfaces change from:
```
data class Error(val message: String)
```
to:
```
data class Error(val error: AppError)
```

Affected interfaces:
- `TemplatesUiState`
- `WorkflowTemplatesUiState`
- `RecentJobsUiState`
- `JobStatusUiState`
- `WorkflowJobStatusUiState`
- `SchedulesUiState`
- `EdaAuditUiState`
- `AuthUiState`

Also `LaunchState.LaunchError` and `WorkflowLaunchState.LaunchError` change from `message: String` to `error: AppError`.

## State Transitions

### Error Display State

```
Collapsed (default)
  └── User taps "Show details" → Expanded (shows statusCode, url, rawMessage)
       └── User taps "Hide details" → Collapsed
```

### Pulse Animation State

```
Static (status != RUNNING)
  └── Status changes to RUNNING → Pulsing (scale oscillates 0.96-1.04)
       └── Status changes away from RUNNING → Static
       └── Reduce motion enabled → Static (immediate)
```

### AapIcons (NEW)

Centralized icon registry replacing scattered inline icon references.

```
AapIcons (object)
├── Status
│   ├── New: ImageVector (HourglassEmpty)
│   ├── Pending: ImageVector (HourglassEmpty)
│   ├── Waiting: ImageVector (HourglassEmpty)
│   ├── Running: ImageVector (PlayCircle)
│   ├── Successful: ImageVector (CheckCircle)
│   ├── Failed: ImageVector (Error)
│   ├── Error: ImageVector (Error)
│   └── Canceled: ImageVector (Cancel)
├── Error
│   ├── Network: ImageVector (WifiOff)
│   ├── Auth: ImageVector (Lock)
│   ├── Server: ImageVector (DnsOutlined)
│   ├── Ssl: ImageVector (GppBad)
│   └── Unknown: ImageVector (ErrorOutline)
├── Navigation
│   ├── Settings: ImageVector
│   ├── Notifications: ImageVector
│   └── Back: ImageVector
└── Action
    ├── Launch: ImageVector (PlayArrow)
    ├── Retry: ImageVector (Refresh)
    └── ExpandMore/Less: ImageVector
```

### StatusColors (NEW)

Theme-level color set for job statuses, provided via CompositionLocal.

```
StatusColors (data class)
├── successful: Color (0xFF4CAF50)
├── failed: Color (0xFFF44336)
├── error: Color (0xFFD32F2F)
├── running: Color (0xFFFF9800)
├── pending: Color (0xFF9E9E9E)
├── waiting: Color (0xFF9E9E9E)
├── new: Color (0xFF9E9E9E)
└── canceled: Color (0xFF2196F3)

LocalStatusColors: CompositionLocal<StatusColors>
  └── default: StatusColors() (with the color values above)

AapRemoteTheme.statusColors: StatusColors
  └── accessor on companion object reading from LocalStatusColors.current
```

### Flow.asResult() Extension (NEW)

Utility extension for reducing ViewModel boilerplate.

```
Result (sealed interface) — uses kotlin.Result
  ├── Success(data: T)
  ├── Error(exception: Throwable)
  └── Loading

Flow<Result<T>>.asResult(): Flow that emits Loading → Success/Error
  └── combined with AppError.from() at ViewModel layer
```

## Relationships

```
Repository → throws raw Exception
     ↓
ViewModel → asResult() + AppError.from(e) → UiState.Error(AppError)
     ↓
ErrorMessage composable → renders typed display with AapIcons.Error.*
     ↓
StatusColors → via LocalStatusColors → JobStatusBadge, ErrorMessage
```
