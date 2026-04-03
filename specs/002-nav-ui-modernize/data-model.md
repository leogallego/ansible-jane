# Data Model: Navigation Foundation & UI Modernization

**Branch**: `002-nav-ui-modernize` | **Date**: 2026-04-02

## Overview

This feature is primarily a UI/navigation restructure. No new data entities are persisted or fetched from the API. The data model changes are limited to navigation state representation.

## New Entities

### Tab (Navigation)

Represents a top-level bottom navigation destination.

| Attribute | Type | Description |
|-----------|------|-------------|
| route | String | Navigation route identifier |
| label | String | Display label (e.g., "Templates") |
| icon | ImageVector | Material icon for the tab |
| selectedIcon | ImageVector | Filled icon variant when selected |
| segments | List<Segment> | Sub-sections within this tab |

**Identity**: Unique by `route`.

### Segment

Represents a sub-section within a tab, switchable via segmented buttons.

| Attribute | Type | Description |
|-----------|------|-------------|
| label | String | Display label (e.g., "Job Templates") |
| isDefault | Boolean | Whether this is the initially selected segment |
| isImplemented | Boolean | Whether this segment has real content (vs placeholder) |

**Identity**: Unique by `label` within a parent Tab.

## Existing Entities (Unchanged)

- **JobTemplate**: No changes. Continues to be fetched from `/api/v2/job_templates/`.
- **Job**: No changes. Continues to be fetched from `/api/v2/jobs/`.
- **User**: No changes. Used for auth validation.
- **Label**: No changes. Used for template filtering.

## State Transitions

### Tab Navigation State

```
Auth Screen → [login success] → Templates Tab (default)
Templates Tab ↔ Infrastructure Tab ↔ Activity Tab
Any Tab → [gear icon] → Settings Screen → [back] → Previous Tab
Any Tab → [detail screen] → Detail → [back] → Same Tab (state preserved)
```

### Segment State (per tab)

```
Default Segment (selected on tab entry)
  ↕ [tap segment button]
Other Segment
```

Segment selection is preserved when switching tabs and returning.

## No New API Contracts

This feature does not introduce any new API endpoints. All data continues to flow through the existing `AapApiService` interface.
