#!/usr/bin/env bash
# Sandbox environment setup for Claude Code sessions
#
# Usage:
#   ./scripts/sandbox-setup.sh          Auto-detect (add proxy if $HTTP_PROXY set, remove if not)
#   ./scripts/sandbox-setup.sh on       Force-enable proxy settings
#   ./scripts/sandbox-setup.sh off      Force-remove proxy settings

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

MODE="${1:-auto}"

# 1. Create .tmp dir for java.io.tmpdir (sandbox sets this to .tmp)
mkdir -p .tmp

# 2. Create local.properties for Android SDK if missing
if [ ! -f local.properties ]; then
    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    echo "Created local.properties with Android SDK path"
fi

# 3. Manage Gradle proxy settings
GRADLE_PROPS="gradle.properties"

should_enable_proxy() {
    case "$MODE" in
        on)   return 0 ;;
        off)  return 1 ;;
        auto) [ -n "${HTTP_PROXY:-}" ] ;;
        *)    echo "Usage: $0 [on|off|auto]"; exit 1 ;;
    esac
}

remove_proxy() {
    if grep -q "systemProp.http.proxyHost" "$GRADLE_PROPS" 2>/dev/null; then
        sed -i '/^systemProp\.http\.proxyHost=/d' "$GRADLE_PROPS"
        sed -i '/^systemProp\.http\.proxyPort=/d' "$GRADLE_PROPS"
        sed -i '/^systemProp\.https\.proxyHost=/d' "$GRADLE_PROPS"
        sed -i '/^systemProp\.https\.proxyPort=/d' "$GRADLE_PROPS"
        sed -i '/^systemProp\.http\.nonProxyHosts=/d' "$GRADLE_PROPS"
        # Remove trailing blank lines
        sed -i -e :a -e '/^\n*$/{$d;N;ba' -e '}' "$GRADLE_PROPS"
        echo "Removed Gradle proxy settings"
    else
        echo "Gradle proxy settings not present"
    fi
}

add_proxy() {
    local proxy_host proxy_port
    if [ -n "${HTTP_PROXY:-}" ]; then
        proxy_host=$(echo "$HTTP_PROXY" | sed -E 's|https?://||' | cut -d: -f1)
        proxy_port=$(echo "$HTTP_PROXY" | sed -E 's|https?://||' | cut -d: -f2)
    else
        proxy_host="localhost"
        proxy_port="3128"
    fi

    if ! grep -q "systemProp.http.proxyHost" "$GRADLE_PROPS" 2>/dev/null; then
        cat >> "$GRADLE_PROPS" <<EOF
systemProp.http.proxyHost=$proxy_host
systemProp.http.proxyPort=$proxy_port
systemProp.https.proxyHost=$proxy_host
systemProp.https.proxyPort=$proxy_port
systemProp.http.nonProxyHosts=localhost|127.0.0.1|*.local
EOF
        echo "Added Gradle proxy settings (host=$proxy_host, port=$proxy_port)"
    else
        echo "Gradle proxy settings already present"
    fi
}

if should_enable_proxy; then
    add_proxy
else
    remove_proxy
fi

# 4. Set up Robolectric offline mode jars
ROBO_DEPS="robolectric-deps"
if [ ! -d "$ROBO_DEPS" ]; then
    M2_ROBO="$HOME/.m2/repository/org/robolectric/android-all-instrumented"
    if [ -d "$M2_ROBO" ]; then
        mkdir -p "$ROBO_DEPS"
        for jar in "$M2_ROBO"/*/android-all-instrumented-*.jar; do
            [ -f "$jar" ] && ln -sf "$jar" "$ROBO_DEPS/$(basename "$jar")"
        done
        echo "Linked Robolectric SDK jars to $ROBO_DEPS"
    else
        echo "Warning: Robolectric jars not found in ~/.m2 — run tests once outside sandbox first"
    fi
else
    echo "Robolectric deps directory already exists"
fi

echo "Sandbox setup complete (mode=$MODE)."
