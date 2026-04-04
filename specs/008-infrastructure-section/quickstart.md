# Quickstart: Infrastructure Section

**Feature**: 008-infrastructure-section | **Date**: 2026-04-03

## Prerequisites

- Android Studio with Kotlin plugin
- AAP 2.5+ instance with Gateway access
- Valid AAP credentials (for testing)

## Implementation Order

Build in this sequence to enable incremental testing:

### Phase 1: Data Layer (API + Models + Repositories)

1. **Add data models**: `Inventory.kt`, `Host.kt` in `model/`
   - Follow existing pattern: `@Serializable` data classes with `@SerialName` for snake_case
   - Include summary field nested classes
   - Inventory needs: created, modified, variables fields for bottom sheet

2. **Add API endpoints** to `AapApiService.kt`:
   - `getInventories()` — GET `inventories/`
   - `getInventory()` — GET `inventories/{id}/` (for detail if needed)
   - `getInventoryHosts()` — GET `inventories/{id}/hosts/`
   - `getHosts()` — GET `hosts/` (all hosts with search)

3. **Add repositories**: `InventoryRepository.kt`, `HostRepository.kt`
   - Follow `TemplateRepository` pattern: `Result<T>` returns, pagination wrappers
   - HostRepository handles both all-hosts and inventory-scoped hosts

4. **Register in Koin**: Add repositories to `DataModule`, ViewModels to `PresentationModule`

### Phase 2: Inventories (US1)

5. **InventoriesViewModel**: List inventories with pagination and refresh
6. **InventoriesScreen**: LazyColumn with inventory cards
7. **InventoryDetailSheet**: Bottom sheet with inventory fields, expandable to full screen with hosts list
8. **Wire navigation**: Mark Inventories segment as implemented, route in TabContent

### Phase 3: Hosts (US2)

9. **HostsViewModel**: List all hosts with search (debounced) and pagination
10. **HostsScreen**: LazyColumn with host cards showing description + inventory label
11. **HostDetailSheet**: Bottom sheet with host details, expandable to full screen
12. **Wire navigation**: Mark Hosts segment as implemented, route in TabContent

### Phase 4: Cleanup

13. **Remove Projects segment** from TabDefinitions (descoped to separate tab)
14. **Verify** no PlaceholderScreen remains for Infrastructure segments

## Key Patterns to Follow

- **ViewModel state**: Copy `EdaAuditViewModel` pattern for simple lists
- **Screen composable**: Copy `EdaAuditScreen` pattern (LazyColumn + PullToRefreshBox + skeleton)
- **Bottom sheet**: Copy `EdaAuditDetailSheet` pattern, add expand-to-full-screen
- **Tab wiring**: Set `isImplemented = true` in `TabDefinitions.kt`, add cases to `TabContent()` in `MainNavigation.kt`
- **Search with debounce**: Copy `TemplatesViewModel.search()` pattern (300ms delay)

## Testing

Test against a real AAP 2.5+ instance:
1. Verify inventory list loads and paginates
2. Tap inventory → verify bottom sheet shows name, type, org, total hosts, created, modified, variables
3. Expand inventory detail → verify hosts load with group badges
4. Switch to Hosts segment → verify all hosts load with descriptions and inventory labels
5. Search hosts → verify server-side filtering works
6. Tap host → verify bottom sheet shows details → expand to full screen
7. Test empty states, error states, pull-to-refresh on all screens
8. Verify Projects segment is removed from Infrastructure tab
