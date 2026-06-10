#!/usr/bin/env bash
# Sandbox environment setup for Claude Code sessions
# Run this at the start of any sandboxed session to fix common issues.
# When sandbox is not active, removes any leftover proxy settings.

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

# 1. Create .tmp dir for java.io.tmpdir (sandbox sets this to .tmp)
mkdir -p .tmp

# 2. Create local.properties for Android SDK if missing
if [ ! -f local.properties ]; then
    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    echo "Created local.properties with Android SDK path"
fi

# 3. Manage Gradle proxy settings based on sandbox state
GRADLE_PROPS="gradle.properties"
if [ -n "${HTTP_PROXY:-}" ]; then
    # Sandbox active — add proxy if not already present
    if ! grep -q "systemProp.http.proxyHost" "$GRADLE_PROPS" 2>/dev/null; then
        PROXY_HOST=$(echo "$HTTP_PROXY" | sed -E 's|https?://||' | cut -d: -f1)
        PROXY_PORT=$(echo "$HTTP_PROXY" | sed -E 's|https?://||' | cut -d: -f2)
        cat >> "$GRADLE_PROPS" <<EOF
systemProp.http.proxyHost=$PROXY_HOST
systemProp.http.proxyPort=$PROXY_PORT
systemProp.https.proxyHost=$PROXY_HOST
systemProp.https.proxyPort=$PROXY_PORT
systemProp.http.nonProxyHosts=localhost|127.0.0.1|*.local
EOF
        echo "Added Gradle proxy settings (host=$PROXY_HOST, port=$PROXY_PORT)"
    else
        echo "Gradle proxy settings already present"
    fi
else
    # No sandbox — remove proxy settings if present
    if grep -q "systemProp.http.proxyHost" "$GRADLE_PROPS" 2>/dev/null; then
        sed -i '/^systemProp\.http\.proxyHost=/d' "$GRADLE_PROPS"
        sed -i '/^systemProp\.http\.proxyPort=/d' "$GRADLE_PROPS"
        sed -i '/^systemProp\.https\.proxyHost=/d' "$GRADLE_PROPS"
        sed -i '/^systemProp\.https\.proxyPort=/d' "$GRADLE_PROPS"
        sed -i '/^systemProp\.http\.nonProxyHosts=/d' "$GRADLE_PROPS"
        # Remove trailing blank lines
        sed -i -e :a -e '/^\n*$/{$d;N;ba' -e '}' "$GRADLE_PROPS"
        echo "Removed stale Gradle proxy settings (sandbox not active)"
    fi
fi

# 4. Set up Robolectric offline mode jars
# Robolectric needs pre-instrumented android-all jars in a flat directory.
# In sandbox mode, it can't create ~/.robolectric-download-lock on the
# read-only home filesystem, so offline mode with local jars is required.
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

echo "Sandbox setup complete."
