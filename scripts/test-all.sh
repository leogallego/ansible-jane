#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}PASS${NC} $1"; }
fail() { echo -e "${RED}FAIL${NC} $1"; FAILURES=$((FAILURES + 1)); }
skip() { echo -e "${YELLOW}SKIP${NC} $1"; }
FAILURES=0

echo "=== Ansible Jane — Full Test Suite ==="
echo "Started: $(date)"
echo ""

# --- 1. Build ---
echo "--- Stage 1: Build ---"
if ./gradlew assembleDebug --quiet 2>&1; then
    pass "assembleDebug"
else
    fail "assembleDebug"
    echo "Build failed, aborting remaining tests."
    exit 1
fi
echo ""

# --- 2. Unit tests ---
echo "--- Stage 2: Unit Tests ---"
if ./gradlew testDebugUnitTest --quiet 2>&1; then
    pass "testDebugUnitTest"
else
    fail "testDebugUnitTest"
fi
echo ""

# --- 3. Screenshot tests ---
echo "--- Stage 3: Screenshot Tests ---"
if ./gradlew validateDebugScreenshotTest --quiet 2>&1; then
    pass "validateDebugScreenshotTest"
else
    fail "validateDebugScreenshotTest (may need golden images update)"
fi
echo ""

# --- 4. Lint ---
echo "--- Stage 4: Lint ---"
if ./gradlew lintDebug --quiet 2>&1; then
    pass "lintDebug"
else
    fail "lintDebug"
fi
echo ""

echo "=== Summary ==="
if [ $FAILURES -eq 0 ]; then
    echo -e "${GREEN}All stages passed.${NC}"
else
    echo -e "${RED}${FAILURES} stage(s) failed.${NC}"
fi
echo "Finished: $(date)"
exit $FAILURES
