#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

ADB=~/Android/Sdk/platform-tools/adb
PKG="io.github.leogallego.ansiblejane"
DEVICE="${1:-}" # optional device serial

ADB_CMD="$ADB"
if [ -n "$DEVICE" ]; then
    ADB_CMD="$ADB -s $DEVICE"
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
FAILURES=0
pass() { echo -e "${GREEN}PASS${NC} $1"; }
fail() { echo -e "${RED}FAIL${NC} $1"; FAILURES=$((FAILURES + 1)); }

echo "=== Ansible Jane — UI Smoke Test ==="
echo "Device: ${DEVICE:-auto}"
echo ""

# --- Install ---
echo "--- Installing debug APK ---"
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    echo "APK not found, building..."
    ./gradlew assembleDebug --quiet
fi
$ADB_CMD install -r "$APK"
echo ""

# --- Launch app ---
echo "--- Launching app ---"
$ADB_CMD shell am start -n "$PKG/.MainActivity"
sleep 3

# --- Screen navigation ---
SCREENS=(
    "Dashboard"
    "Templates"
    "Activity"
    "Infrastructure"
    "Chat"
    "Settings"
)

echo "--- Navigating screens ---"
for screen in "${SCREENS[@]}"; do
    echo "  Checking: $screen"
    # Capture layout and check for crash indicators
    LAYOUT=$($ADB_CMD shell uiautomator dump /dev/tty 2>/dev/null || true)
    if echo "$LAYOUT" | grep -qi "has stopped\|keeps stopping\|crash"; then
        fail "$screen — crash detected"
    else
        pass "$screen — rendered"
    fi

    # Navigate to next screen via bottom nav if applicable
    case "$screen" in
        Dashboard)
            $ADB_CMD shell input tap 270 2300 2>/dev/null || true  # Templates tab
            sleep 2
            ;;
        Templates)
            $ADB_CMD shell input tap 540 2300 2>/dev/null || true  # Activity tab
            sleep 2
            ;;
        Activity)
            $ADB_CMD shell input tap 810 2300 2>/dev/null || true  # Infrastructure tab
            sleep 2
            ;;
        Infrastructure)
            $ADB_CMD shell input tap 1080 2300 2>/dev/null || true  # Chat tab
            sleep 2
            ;;
        Chat)
            # Navigate to settings via menu or gear icon
            $ADB_CMD shell input keyevent KEYCODE_MENU 2>/dev/null || true
            sleep 1
            ;;
    esac
done
echo ""

# --- Layout inspection ---
echo "--- Layout tree dump ---"
android layout --device "${DEVICE:-emulator-5554}" -p 2>/dev/null && pass "Layout dump" || fail "Layout dump"
echo ""

echo "=== Summary ==="
if [ $FAILURES -eq 0 ]; then
    echo -e "${GREEN}All UI checks passed.${NC}"
else
    echo -e "${RED}${FAILURES} check(s) failed.${NC}"
fi
exit $FAILURES
