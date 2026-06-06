#!/usr/bin/env bash
# Sandbox environment setup for Claude Code sessions
# Run this at the start of any sandboxed session to fix common issues.

set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

# 1. Create .tmp dir for java.io.tmpdir (sandbox sets this to .tmp)
mkdir -p .tmp

# 2. Create local.properties for Android SDK if missing
if [ ! -f local.properties ]; then
    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    echo "Created local.properties with Android SDK path"
fi

# 3. Add Gradle proxy settings if running in sandbox with HTTP proxy
if [ -n "${HTTP_PROXY:-}" ] && ! grep -q "systemProp.http.proxyHost" gradle.properties 2>/dev/null; then
    # Extract host and port from HTTP_PROXY (format: http://host:port)
    PROXY_HOST=$(echo "$HTTP_PROXY" | sed -E 's|https?://||' | cut -d: -f1)
    PROXY_PORT=$(echo "$HTTP_PROXY" | sed -E 's|https?://||' | cut -d: -f2)
    cat >> gradle.properties <<EOF
systemProp.http.proxyHost=$PROXY_HOST
systemProp.http.proxyPort=$PROXY_PORT
systemProp.https.proxyHost=$PROXY_HOST
systemProp.https.proxyPort=$PROXY_PORT
systemProp.http.nonProxyHosts=localhost|127.0.0.1|*.local
EOF
    echo "Added Gradle proxy settings (host=$PROXY_HOST, port=$PROXY_PORT)"
fi

echo "Sandbox setup complete."
