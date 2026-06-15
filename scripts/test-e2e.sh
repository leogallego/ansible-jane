#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

ADB=~/Android/Sdk/platform-tools/adb
PKG="io.github.leogallego.ansiblejane"
DEVICE="${1:-emulator-5554}"
SCREENSHOT_DIR="$PROJECT_DIR/tmp/e2e-screenshots"
LAYOUT_JSON="$PROJECT_DIR/tmp/layout_$$.json"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
DIM='\033[2m'
NC='\033[0m'
FAILURES=0
PASSES=0
SKIPS=0
TOTAL=0

pass() { PASSES=$((PASSES + 1)); TOTAL=$((TOTAL + 1)); echo -e "  ${GREEN}PASS${NC} $1"; }
fail() { FAILURES=$((FAILURES + 1)); TOTAL=$((TOTAL + 1)); echo -e "  ${RED}FAIL${NC} $1"; }
skip() { SKIPS=$((SKIPS + 1)); TOTAL=$((TOTAL + 1)); echo -e "  ${YELLOW}SKIP${NC} $1"; }
section() { echo -e "\n${BLUE}--- $1 ---${NC}"; }
trace() { echo -e "  ${DIM}> $1${NC}"; }

# --- Helpers ---

refresh_layout() {
    android layout --device "$DEVICE" -o "$LAYOUT_JSON" >/dev/null 2>&1 || true
}

has_resource_id() {
    refresh_layout
    grep -q "\"resource-id\":\"$1\"" "$LAYOUT_JSON" 2>/dev/null
}

has_text() {
    refresh_layout
    grep -q "\"text\":\"$1\"" "$LAYOUT_JSON" 2>/dev/null
}

has_text_containing() {
    refresh_layout
    grep -q "\"text\":\"[^\"]*$1" "$LAYOUT_JSON" 2>/dev/null
}

app_is_foreground() {
    $ADB -s "$DEVICE" shell "dumpsys activity activities" 2>/dev/null \
        | grep -qi "ResumedActivity.*$PKG\|mResumedActivity.*$PKG"
}

no_crash() {
    refresh_layout
    if grep -qi "has stopped\|keeps stopping\|isn't responding" "$LAYOUT_JSON" 2>/dev/null; then
        return 1
    fi
    if ! app_is_foreground; then
        return 1
    fi
    return 0
}

abort_on_crash() {
    local context="$1"
    if ! no_crash; then
        fail "$context — CRASH (app not in foreground)"
        screenshot "crash_${context// /_}"
        echo -e "  ${RED}Logcat (last 30 lines with errors):${NC}"
        $ADB -s "$DEVICE" logcat -d -t 30 2>&1 \
            | grep -iE "FATAL|AndroidRuntime|Exception|Error.*$PKG" \
            | tail -15 | sed 's/^/    /'
        echo ""
        echo -e "${RED}ABORTING — fix the crash before re-running${NC}"
        rm -f "$LAYOUT_JSON" 2>/dev/null || true
        echo ""
        echo "=== E2E Test Summary (ABORTED) ==="
        echo -e "  ${GREEN}Passed:${NC}  $PASSES"
        echo -e "  ${RED}Failed:${NC}  $FAILURES"
        echo -e "  ${YELLOW}Skipped:${NC} $SKIPS"
        echo -e "  Total:   $TOTAL"
        if [ -d "$SCREENSHOT_DIR" ]; then
            echo "Screenshots saved to: $SCREENSHOT_DIR/"
        fi
        echo "Finished: $(date)"
        exit 1
    fi
}

tap_resource_id() {
    local rid="$1"
    refresh_layout
    local center
    center=$(python3 -c "
import json, sys
with open('$LAYOUT_JSON') as f:
    data = json.load(f)
for item in data:
    if item.get('resource-id') == '$rid':
        print(item['center'])
        sys.exit(0)
sys.exit(1)
" 2>/dev/null) || { trace "tap_resource_id '$rid' — element not found"; return 1; }
    local cx cy
    cx=$(echo "$center" | sed 's/\[//;s/\]//;s/,.*//')
    cy=$(echo "$center" | sed 's/\[//;s/\]//;s/.*,//')
    trace "tap $rid at [$cx,$cy]"
    $ADB -s "$DEVICE" shell input tap "$cx" "$cy"
    sleep 1.5
}

tap_text() {
    local text="$1"
    refresh_layout
    local center
    center=$(python3 -c "
import json, sys
with open('$LAYOUT_JSON') as f:
    data = json.load(f)
for item in data:
    if item.get('text') == '$text':
        print(item['center'])
        sys.exit(0)
sys.exit(1)
" 2>/dev/null) || { trace "tap_text '$text' — not found"; return 1; }
    local cx cy
    cx=$(echo "$center" | sed 's/\[//;s/\]//;s/,.*//')
    cy=$(echo "$center" | sed 's/\[//;s/\]//;s/.*,//')
    trace "tap '$text' at [$cx,$cy]"
    $ADB -s "$DEVICE" shell input tap "$cx" "$cy"
    sleep 1.5
}

screenshot() {
    local name="$1"
    mkdir -p "$SCREENSHOT_DIR"
    $ADB -s "$DEVICE" shell screencap -p /sdcard/screenshot.png >/dev/null 2>&1
    $ADB -s "$DEVICE" pull /sdcard/screenshot.png "$SCREENSHOT_DIR/${name}.png" >/dev/null 2>&1
}

wait_for_app() {
    local retries=10
    while [ $retries -gt 0 ]; do
        if app_is_foreground; then
            return 0
        fi
        sleep 1
        retries=$((retries - 1))
    done
    return 1
}

scroll_down() {
    local amount="${1:-500}"
    $ADB -s "$DEVICE" shell input swipe 540 1400 540 $((1400 - amount)) 300
    sleep 1
}

scroll_up() {
    local amount="${1:-500}"
    $ADB -s "$DEVICE" shell input swipe 540 900 540 $((900 + amount)) 300
    sleep 1
}

has_any_text_matching() {
    local pattern="$1"
    refresh_layout
    python3 -c "
import json, re, sys
with open('$LAYOUT_JSON') as f:
    data = json.load(f)
for item in data:
    if 'text' in item and re.search(r'$pattern', item['text']):
        sys.exit(0)
sys.exit(1)
" 2>/dev/null
}

go_back() {
    if has_resource_id "button_back"; then
        tap_resource_id "button_back"
    else
        trace "back via KEYCODE_BACK"
        $ADB -s "$DEVICE" shell input keyevent KEYCODE_BACK
    fi
    sleep 1.5
}

# ============================================
echo "=== Ansible Jane — E2E Smoke Test ==="
echo "Device: $DEVICE"
echo "Started: $(date)"
echo ""

# --- Pre-flight ---
section "Pre-flight"

if ! $ADB -s "$DEVICE" get-state >/dev/null 2>&1; then
    echo -e "${RED}ERROR: Device $DEVICE not found${NC}"
    exit 1
fi
pass "Device connected"

# Always build from current source to avoid testing stale APKs
APK="app/build/outputs/apk/debug/app-debug.apk"
trace "cleaning shared + composeApp + app modules..."
trace "building assembleDebug..."
if ! ./gradlew :shared:clean :composeApp:clean :app:clean :app:assembleDebug --quiet 2>&1; then
    echo -e "${RED}ERROR: Build failed${NC}"
    exit 1
fi
pass "APK built from current source"

if [ ! -f "$APK" ]; then
    echo -e "${RED}ERROR: APK not found at $APK after build${NC}"
    exit 1
fi
trace "APK: $(ls -la "$APK" | awk '{print $6, $7, $8, $9}')"

trace "installing APK on device..."
$ADB -s "$DEVICE" install -r "$APK" >/dev/null 2>&1
pass "APK installed"

# Force-stop for clean state
trace "force-stopping $PKG for clean launch..."
$ADB -s "$DEVICE" shell "am force-stop $PKG" 2>/dev/null || true
sleep 1

# Clear logcat so crash detection only sees this run
$ADB -s "$DEVICE" logcat -c 2>/dev/null || true

# --- Launch app ---
section "App Launch"
trace "starting $PKG/.MainActivity..."
$ADB -s "$DEVICE" shell "am start -n $PKG/.MainActivity" >/dev/null 2>&1
sleep 3

if wait_for_app; then
    pass "App launched"
else
    fail "App launch timeout"
    exit 1
fi

if no_crash; then
    pass "No crash on launch"
else
    fail "Crash detected on launch"
    screenshot "crash_on_launch"
    exit 1
fi

screenshot "01_launch"

# --- Determine initial state ---
section "Initial State Detection"
refresh_layout

if has_resource_id "field_url"; then
    echo "  App is on Auth screen (no instances configured)"
    skip "Main screen tests — need login first"
    AUTH_MODE=true
else
    AUTH_MODE=false
    pass "App loaded to main screen"
fi

# ============================================
# AUTH SCREEN TESTS
# ============================================
if [ "$AUTH_MODE" = true ]; then
    section "Auth Screen"

    if has_resource_id "field_url"; then pass "URL field present"; else fail "URL field missing"; fi
    if has_resource_id "field_token"; then pass "Token field present"; else fail "Token field missing"; fi
    if has_resource_id "field_alias"; then pass "Alias field present"; else fail "Alias field missing"; fi
    if has_resource_id "switch_self_signed"; then pass "Self-signed cert switch present"; else fail "Self-signed cert switch missing"; fi
    if has_resource_id "button_connect"; then pass "Connect button present"; else fail "Connect button missing"; fi

    screenshot "02_auth_screen"
    echo ""
    echo "=== Auth Screen Summary ==="
    echo "Auth screen elements verified. To test full navigation,"
    echo "configure an AAP instance and re-run."
fi

# ============================================
# MAIN SCREEN TESTS (if logged in)
# ============================================
if [ "$AUTH_MODE" = false ]; then

    # --- Dashboard Tab ---
    section "Dashboard Tab"
    tap_resource_id "nav_dashboard" || true
    sleep 2
    abort_on_crash "Dashboard"
    pass "Dashboard — no crash"

    if has_resource_id "list_dashboard"; then pass "Dashboard list rendered"; else skip "Dashboard list (may be empty or loading)"; fi
    if has_resource_id "button_settings"; then pass "Settings button present"; else fail "Settings button missing"; fi
    if has_resource_id "button_notifications"; then pass "Notifications button present"; else fail "Notifications button missing"; fi
    screenshot "02_dashboard"

    # --- Templates Tab ---
    section "Templates Tab"
    tap_resource_id "nav_templates" || true
    sleep 2
    abort_on_crash "Templates"
    pass "Templates — no crash"

    if has_resource_id "segment_job_templates"; then pass "Job Templates segment visible"; else fail "Job Templates segment missing"; fi
    if has_resource_id "segment_workflows"; then pass "Workflows segment visible"; else fail "Workflows segment missing"; fi
    screenshot "03_templates_jobs"

    tap_resource_id "segment_workflows" && {
        sleep 2
        abort_on_crash "Workflows segment"
        pass "Workflows segment — no crash"
        screenshot "04_templates_workflows"
    } || skip "Workflows segment tap"

    tap_resource_id "segment_job_templates" || true
    sleep 1

    # --- Infrastructure Tab ---
    section "Infrastructure Tab"
    tap_resource_id "nav_infrastructure" || true
    sleep 2
    abort_on_crash "Infrastructure"
    pass "Infrastructure — no crash"

    if has_resource_id "segment_inventories"; then pass "Inventories segment visible"; else fail "Inventories segment missing"; fi
    if has_resource_id "segment_hosts"; then pass "Hosts segment visible"; else fail "Hosts segment missing"; fi
    screenshot "05_infrastructure_inventories"

    tap_resource_id "segment_hosts" && {
        sleep 2
        abort_on_crash "Hosts segment"
        pass "Hosts segment — no crash"
        screenshot "06_infrastructure_hosts"
    } || skip "Hosts segment tap"

    tap_resource_id "segment_inventories" || true
    sleep 1

    # --- Activity Tab ---
    section "Activity Tab"
    tap_resource_id "nav_activity" || true
    sleep 2
    abort_on_crash "Activity"
    pass "Activity — no crash"

    if has_resource_id "segment_jobs"; then pass "Jobs segment visible"; else fail "Jobs segment missing"; fi
    if has_resource_id "segment_schedules"; then pass "Schedules segment visible"; else fail "Schedules segment missing"; fi
    if has_resource_id "segment_eda"; then pass "EDA segment visible"; else fail "EDA segment missing"; fi
    screenshot "07_activity_jobs"

    tap_resource_id "segment_schedules" && {
        sleep 2
        abort_on_crash "Schedules segment"
        pass "Schedules segment — no crash"
        screenshot "08_activity_schedules"
    } || skip "Schedules segment tap"

    tap_resource_id "segment_eda" && {
        sleep 2
        abort_on_crash "EDA segment"
        pass "EDA segment — no crash"
        screenshot "09_activity_eda"
    } || skip "EDA segment tap"

    # --- Assistant Tab ---
    section "Assistant (Jane) Tab"
    tap_resource_id "nav_assistant" || true
    sleep 2
    abort_on_crash "Assistant (Jane)"
    pass "Assistant — no crash"

    if has_resource_id "field_assistant_input"; then pass "Chat input field present"; else fail "Chat input field missing"; fi
    if has_resource_id "button_send"; then pass "Send button present"; else fail "Send button missing"; fi
    if has_resource_id "chip_provider"; then pass "Provider switch chip visible"; else skip "Provider switch chip (may need config)"; fi
    if has_resource_id "button_clear_chat"; then pass "Clear chat button present"; else skip "Clear chat button (may not show when empty)"; fi
    if has_resource_id "text_session_tokens"; then pass "Session tokens indicator present"; else skip "Session tokens (shows after chat activity)"; fi
    screenshot "10_assistant"

    # --- Notifications Sheet ---
    section "Notifications"
    tap_resource_id "button_notifications" && {
        sleep 2
        if has_resource_id "sheet_notifications"; then pass "Notifications sheet opened"; else skip "Notifications sheet (may use different rendering)"; fi
        if has_resource_id "button_refresh_notifications"; then pass "Refresh notifications button present"; else skip "Refresh notifications button"; fi
        screenshot "11_notifications"
        $ADB -s "$DEVICE" shell input keyevent KEYCODE_BACK
        sleep 1
    } || skip "Notifications button tap"

    # --- Settings Screen ---
    section "Settings Screen"
    tap_resource_id "button_settings" || true
    sleep 2
    abort_on_crash "Settings"
    pass "Settings — no crash"
    screenshot "12_settings_general"

    if has_text "Backup & Restore"; then pass "Backup & Restore section visible"; else skip "Backup & Restore (may need scroll)"; fi

    # Instances tab
    section "Settings — Instances Tab"
    if tap_resource_id "tab_instances"; then
        sleep 2
        abort_on_crash "Instances tab"
        pass "Instances tab — no crash"
        if has_text "Add Instance"; then pass "Add Instance button visible"; else skip "Add Instance button (may need scroll)"; fi
        screenshot "13_settings_instances"
    else
        tap_text "Instances" || skip "Instances tab tap"
        sleep 2
    fi

    # Agent tab
    section "Settings — Agent Tab"
    if tap_resource_id "tab_agent"; then
        sleep 2
        abort_on_crash "Agent tab"
        pass "Agent tab — no crash"
        if has_text "LLM Provider"; then pass "LLM Provider section visible"; else fail "LLM Provider section missing"; fi
        screenshot "14_settings_agent"
    else
        tap_text "Agent" || skip "Agent tab tap"
        sleep 2
    fi

    # Tools tab
    section "Settings — Tools Tab"
    if tap_resource_id "tab_tools"; then
        sleep 2
        abort_on_crash "Tools tab"
        pass "Tools tab — no crash"
        if has_text "MCP Servers"; then pass "MCP Servers section visible"; else fail "MCP Servers section missing"; fi
        if has_resource_id "switch_mcp_enabled"; then pass "MCP enabled switch present"; else fail "MCP enabled switch missing"; fi
        screenshot "15_settings_tools"
    else
        tap_text "Tools" || skip "Tools tab tap"
        sleep 2
    fi

    # Navigate back from settings
    go_back
    sleep 1
    abort_on_crash "Back from settings"
    pass "Back from settings"

    # ============================================
    # FEATURE TESTS (require live AAP data)
    # ============================================

    # --- Dashboard Data Verification ---
    section "Dashboard Data Verification"
    tap_resource_id "nav_dashboard" || true
    sleep 2
    abort_on_crash "Dashboard (data)"

    if has_text "Jobs"; then pass "Dashboard Jobs section present"; else fail "Dashboard Jobs section missing"; fi
    if has_text "Resources"; then pass "Dashboard Resources section present"; else skip "Dashboard Resources section (may need scroll)"; fi

    scroll_down 600
    sleep 1
    abort_on_crash "Dashboard scroll"
    pass "Dashboard scroll — no crash"
    if has_text "Job Activity"; then pass "Dashboard Job Activity chart visible after scroll"; else skip "Dashboard Job Activity chart (may need more scroll)"; fi
    screenshot "20_dashboard_scrolled"
    scroll_up 600
    sleep 1

    # --- Template List Data ---
    section "Template List Data"
    tap_resource_id "nav_templates" || true
    sleep 2

    if has_resource_id "list_templates"; then
        pass "Template list rendered with data"
        if has_any_text_matching "Demo Job Template\|ping\|Hello"; then
            pass "Template items visible in list"
        else
            skip "No recognizable template names (may have different templates)"
        fi
    else
        skip "Template list (may be loading or empty)"
    fi
    screenshot "21_templates_data"

    scroll_down 400
    sleep 1
    abort_on_crash "Template list scroll"
    pass "Template list scroll — no crash"
    scroll_up 400
    sleep 1

    # --- Template Launch + Job Status ---
    section "Template Launch + Job Status"

    LAUNCH_FOUND=$(python3 -c "
import json, sys
with open('$LAYOUT_JSON') as f:
    data = json.load(f)
for item in data:
    rid = item.get('resource-id', '')
    if rid.startswith('button_launch_'):
        print(item['center'])
        sys.exit(0)
sys.exit(1)
" 2>/dev/null) || true

    if [ -n "$LAUNCH_FOUND" ]; then
        cx=$(echo "$LAUNCH_FOUND" | sed 's/\[//;s/\]//;s/,.*//')
        cy=$(echo "$LAUNCH_FOUND" | sed 's/\[//;s/\]//;s/.*,//')
        trace "tap launch button at [$cx,$cy]"
        $ADB -s "$DEVICE" shell input tap "$cx" "$cy"
        sleep 3

        abort_on_crash "Template launch"
        pass "Template launch tap — no crash"

        refresh_layout
        if grep -q '"resource-id":"scaffold_detail"\|"text":"Job Status"' "$LAYOUT_JSON" 2>/dev/null; then
            pass "Job status screen opened"
            if has_resource_id "text_job_name"; then pass "Job name displayed"; else skip "Job name (may still be loading)"; fi
            if has_resource_id "badge_job_status"; then pass "Job status badge displayed"; else skip "Job status badge (may still be loading)"; fi
            screenshot "22_job_status"
            sleep 3
            screenshot "23_job_status_updated"
            go_back
        else
            skip "Job status screen (launch may have shown dialog)"
            $ADB -s "$DEVICE" shell input keyevent KEYCODE_BACK
            sleep 1
        fi
    else
        skip "Template launch (no launch buttons found — may need templates configured)"
    fi

    # --- Workflow Templates ---
    section "Workflow Templates"
    tap_resource_id "nav_templates" || true
    sleep 1
    tap_resource_id "segment_workflows" || true
    sleep 2

    WORKFLOW_NAME=$(python3 -c "
import json, sys
with open('$LAYOUT_JSON') as f:
    data = json.load(f)
for item in data:
    t = item.get('text', '')
    if t and 'workflow' in t.lower() and t not in ('Workflows', 'Workflow Template'):
        print(t)
        sys.exit(0)
sys.exit(1)
" 2>/dev/null) || true

    if [ -n "$WORKFLOW_NAME" ]; then
        pass "Workflow templates visible"
        tap_text "$WORKFLOW_NAME" 2>/dev/null && {
            sleep 2
            abort_on_crash "Workflow detail screen"
            pass "Workflow detail screen — no crash"
            if has_resource_id "button_launch_workflow"; then pass "Workflow launch FAB present"; else skip "Workflow launch FAB (may not be in Success state)"; fi
            screenshot "24_workflow_detail"
            go_back
        } || skip "Workflow detail navigation"
    else
        skip "Workflow templates (none configured or loading)"
    fi

    # --- Infrastructure Data ---
    section "Infrastructure Data Verification"
    tap_resource_id "nav_infrastructure" || true
    sleep 2

    if has_resource_id "list_inventories"; then
        pass "Inventories list rendered"
        scroll_down 400
        abort_on_crash "Inventories scroll"
        pass "Inventories scroll — no crash"
        scroll_up 400
    else
        skip "Inventories list (may be loading or empty)"
    fi
    screenshot "25_inventories_data"

    tap_resource_id "segment_hosts" || true
    sleep 2
    if has_resource_id "list_hosts"; then
        pass "Hosts list rendered"
        scroll_down 400
        abort_on_crash "Hosts scroll"
        pass "Hosts scroll — no crash"
        scroll_up 400
    else
        skip "Hosts list (may be loading or empty)"
    fi
    screenshot "26_hosts_data"

    # --- Activity Data ---
    section "Activity Data Verification"
    tap_resource_id "nav_activity" || true
    sleep 2

    if has_resource_id "list_jobs"; then
        pass "Jobs list rendered"
        scroll_down 400
        abort_on_crash "Jobs list scroll"
        pass "Jobs list scroll — no crash"
        scroll_up 400
    else
        skip "Jobs list (may be loading or empty)"
    fi
    screenshot "27_jobs_data"

    tap_resource_id "segment_schedules" || true
    sleep 2
    if has_resource_id "list_schedules"; then pass "Schedules list rendered"; else skip "Schedules list (may be loading or empty)"; fi
    screenshot "28_schedules_data"

    tap_resource_id "segment_eda" || true
    sleep 2
    if has_resource_id "list_eda_audit"; then pass "EDA audit list rendered"; else skip "EDA audit list (may be loading or empty)"; fi
    screenshot "29_eda_data"

    # --- Chat Interaction ---
    section "Chat Interaction"
    tap_resource_id "nav_assistant" || true
    sleep 2
    abort_on_crash "Assistant (chat)"

    LLM_CONFIGURED=false
    if has_resource_id "chip_provider"; then
        if has_any_text_matching "OpenRouter\|Ollama\|OpenAI\|Gemini\|Custom"; then
            LLM_CONFIGURED=true
        fi
    fi

    if [ "$LLM_CONFIGURED" = true ]; then
        pass "LLM provider configured"

        tap_resource_id "field_assistant_input" || true
        sleep 0.5
        $ADB -s "$DEVICE" shell input text "ping"
        sleep 0.5

        tap_resource_id "button_send" || true
        sleep 5
        abort_on_crash "Chat send"
        pass "Chat send — no crash"

        refresh_layout
        if has_resource_id "list_chat_messages"; then pass "Chat message list active"; else skip "Chat message list (rendering may differ)"; fi

        sleep 10
        screenshot "30_chat_response"
        abort_on_crash "Chat response"
        pass "Chat response — no crash"

        if has_resource_id "text_session_tokens"; then pass "Token usage indicator visible after chat"; else skip "Token usage indicator (may not show for this provider)"; fi

        if has_resource_id "button_clear_chat"; then
            tap_resource_id "button_clear_chat" || true
            sleep 2
            if has_text "Clear"; then tap_text "Clear" || true; sleep 1; fi
            abort_on_crash "Clear chat"
            pass "Clear chat — no crash"
        else
            skip "Clear chat button (not visible)"
        fi
    else
        skip "Chat interaction — no LLM provider configured"
        skip "Chat send — no LLM provider configured"
        skip "Chat response — no LLM provider configured"
    fi

    # --- Settings Instance Management ---
    section "Settings — Instance Management"
    tap_resource_id "button_settings" || true
    sleep 2
    tap_resource_id "tab_instances" || tap_text "Instances" || true
    sleep 2

    INSTANCE_COUNT=$(python3 -c "
import json
with open('$LAYOUT_JSON') as f:
    data = json.load(f)
count = sum(1 for item in data if item.get('resource-id','').startswith('card_instance_'))
print(count)
" 2>/dev/null || echo "0")

    if [ "$INSTANCE_COUNT" -gt 0 ]; then pass "Instance cards rendered ($INSTANCE_COUNT found)"; else skip "Instance cards (may use text-only rendering)"; fi
    if has_text "Active"; then pass "Active instance indicator visible"; else skip "Active instance indicator"; fi
    if has_resource_id "button_add_instance"; then pass "Add Instance button present"; else fail "Add Instance button missing"; fi
    screenshot "31_instances_management"

    scroll_down 300
    if has_resource_id "button_logout_all"; then
        tap_resource_id "button_logout_all" || true
        sleep 1
        if has_text_containing "Remove all"; then
            pass "Logout confirmation dialog shown"
            tap_text "Cancel" || $ADB -s "$DEVICE" shell input keyevent KEYCODE_BACK
            sleep 1
        else
            skip "Logout confirmation dialog"
        fi
    else
        skip "Logout all button (may need scroll)"
    fi

    # --- Settings General Scroll ---
    section "Settings — General Tab Content"
    tap_resource_id "tab_general" || tap_text "General" || true
    sleep 2

    if has_text "Theme"; then pass "Theme section visible"; else skip "Theme section (may need scroll)"; fi
    scroll_down 500
    sleep 1
    if has_text "Backup & Restore"; then pass "Backup & Restore visible after scroll"; else skip "Backup & Restore (may need more scroll)"; fi
    if has_resource_id "button_export_backup"; then pass "Export backup button present"; else skip "Export backup button (may need scroll)"; fi
    if has_resource_id "button_import_backup"; then pass "Import backup button present"; else skip "Import backup button (may need scroll)"; fi
    screenshot "32_settings_general_scrolled"
    scroll_up 500

    # --- Settings Agent Tab Detail ---
    section "Settings — Agent Tab Detail"
    tap_resource_id "tab_agent" || tap_text "Agent" || true
    sleep 2
    if has_any_text_matching "OpenRouter\|Ollama\|OpenAI\|Gemini\|Custom"; then pass "Provider cards visible"; else skip "Provider cards (may need configuration)"; fi
    if has_resource_id "button_clear_history"; then pass "Clear history button present"; else skip "Clear history button (may need scroll)"; fi
    screenshot "33_settings_agent_detail"

    # --- Settings Tools Tab Detail ---
    section "Settings — Tools Tab Detail"
    tap_resource_id "tab_tools" || tap_text "Tools" || true
    sleep 2
    if has_text "Local Tools"; then pass "Local Tools section visible"; else skip "Local Tools (may need scroll)"; fi

    scroll_down 500
    sleep 1
    if has_any_text_matching "Jobs\|Inventory\|Monitoring\|Users\|Security\|Configuration\|EDA\|Platform"; then pass "Tool categories visible"; else skip "Tool categories (may need more scroll)"; fi

    if tap_text "Jobs"; then
        sleep 1
        abort_on_crash "Tool category expand"
        pass "Tool category expand — no crash"
        screenshot "34_tools_expanded"
        tap_text "Jobs" || true
        sleep 1
    else
        skip "Tool category expand"
    fi
    scroll_up 500

    go_back
    sleep 1

    # --- Cross-tab navigation stability ---
    section "Cross-tab Navigation Stability"
    for tab in nav_dashboard nav_templates nav_infrastructure nav_activity nav_assistant; do
        tap_resource_id "$tab" || true
        sleep 1
        abort_on_crash "Quick nav to $tab"
        pass "Quick nav to $tab"
    done

fi

# ============================================
# --- Cleanup ---
rm -f "$LAYOUT_JSON" 2>/dev/null || true
$ADB -s "$DEVICE" shell rm -f /sdcard/screenshot.png 2>/dev/null || true

echo ""
echo "=== E2E Test Summary ==="
echo -e "  ${GREEN}Passed:${NC}  $PASSES"
echo -e "  ${RED}Failed:${NC}  $FAILURES"
echo -e "  ${YELLOW}Skipped:${NC} $SKIPS"
echo -e "  Total:   $TOTAL"
echo ""
if [ -d "$SCREENSHOT_DIR" ]; then
    echo "Screenshots saved to: $SCREENSHOT_DIR/"
fi
echo "Finished: $(date)"

if [ $FAILURES -gt 0 ]; then
    echo -e "${RED}${FAILURES} test(s) FAILED${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed.${NC}"
    exit 0
fi
