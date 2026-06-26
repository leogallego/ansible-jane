#!/bin/bash
set -euo pipefail

APPDIR="$(dirname "$(readlink -f "$0")")/.."

if [ ! -d "$APPDIR/lib/runtime" ]; then
    echo "Error: runtime not found at $APPDIR/lib/runtime" >&2
    exit 1
fi

exec "$APPDIR/lib/runtime/bin/java" \
    -Djpackage.app-version=@VERSION@ \
    -Dcompose.application.resources.dir="$APPDIR/lib/app/resources" \
    -Dcompose.application.configure.swing.globals=true \
    -Dskiko.library.path="$APPDIR/lib/app" \
    -cp "$APPDIR/lib/app/*" \
    io.github.leogallego.ansiblejane.MainKt \
    "$@"
