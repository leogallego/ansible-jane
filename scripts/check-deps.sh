#!/usr/bin/env bash
# check-deps.sh — Query official Maven repositories for latest dependency versions.
# Compares against versions declared in gradle/libs.versions.toml.
#
# Usage: ./scripts/check-deps.sh [--json]
#
# Sources:
#   AndroidX/AGP → https://dl.google.com/dl/android/maven2/{group}/{artifact}/maven-metadata.xml
#   Maven Central → https://repo1.maven.org/maven2/{group}/{artifact}/maven-metadata.xml

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TOML="$PROJECT_ROOT/gradle/libs.versions.toml"

JSON_OUTPUT=false
[[ "${1:-}" == "--json" ]] && JSON_OUTPUT=true

GOOGLE_MAVEN="https://dl.google.com/dl/android/maven2"
MAVEN_CENTRAL="https://repo1.maven.org/maven2"

declare -A DEPS=(
  # key=toml_version_key  value=repo|group:artifact
  [agp]="google|com.android.tools.build:gradle"
  [kotlin]="central|org.jetbrains.kotlin:kotlin-stdlib"
  [compose-bom]="google|androidx.compose:compose-bom"
  [activity-compose]="google|androidx.activity:activity-compose"
  [navigation-compose]="google|androidx.navigation:navigation-compose"
  [lifecycle]="google|androidx.lifecycle:lifecycle-runtime-compose"
  [retrofit]="central|com.squareup.retrofit2:retrofit"
  [okhttp]="central|com.squareup.okhttp3:okhttp"
  [ktor]="central|io.ktor:ktor-client-core"
  [kotlinx-serialization-json]="central|org.jetbrains.kotlinx:kotlinx-serialization-json"
  [koin]="central|io.insert-koin:koin-core"
  [datastore-preferences]="google|androidx.datastore:datastore-preferences"
  [tink-android]="central|com.google.crypto.tink:tink-android"
  [coroutines]="central|org.jetbrains.kotlinx:kotlinx-coroutines-android"
  [collections-immutable]="central|org.jetbrains.kotlinx:kotlinx-collections-immutable"
  [core-ktx]="google|androidx.core:core-ktx"
  [work-runtime]="google|androidx.work:work-runtime-ktx"
  [koog]="central|ai.koog:prompt-executor-openai-client"
  [koog-google]="central|ai.koog:prompt-executor-google-client"
  [mcp-sdk]="central|io.modelcontextprotocol:kotlin-sdk-client"
  [markdownRenderer]="central|com.mikepenz:multiplatform-markdown-renderer"
)

get_current_version() {
  local key="$1"
  grep -E "^${key} = " "$TOML" | sed 's/.*= *"\(.*\)"/\1/' | head -1
}

metadata_url() {
  local repo="$1" group="$2" artifact="$3"
  local group_path="${group//.//}"
  local base
  if [[ "$repo" == "google" ]]; then
    base="$GOOGLE_MAVEN"
  else
    base="$MAVEN_CENTRAL"
  fi
  echo "${base}/${group_path}/${artifact}/maven-metadata.xml"
}

is_stable() {
  local v="$1"
  # Reject alpha, beta, RC, dev, SNAPSHOT, eap
  local lv="${v,,}"
  if [[ "$lv" =~ (alpha|beta|rc|dev|snapshot|eap|milestone|preview|-b[0-9]) ]]; then
    return 1
  fi
  return 0
}

same_major_minor() {
  local current="$1" candidate="$2"

  # Handle Compose BOM format (YYYY.MM.DD)
  if [[ "$current" =~ ^[0-9]{4}\.[0-9]{2}\.[0-9]{2}$ ]]; then
    local cur_ym="${current%.*}"
    local cand_ym="${candidate%.*}"
    [[ "$cur_ym" == "$cand_ym" ]]
    return $?
  fi

  # Standard semver: X.Y.Z — match X.Y
  local cur_xy="${current%.*}"
  local cand_xy="${candidate%.*}"
  [[ "$cur_xy" == "$cand_xy" ]]
}

query_latest() {
  local url="$1" current="$2"
  local xml
  xml=$(curl -sfL --connect-timeout 5 --max-time 10 "$url" 2>/dev/null) || {
    echo "FETCH_ERROR|FETCH_ERROR|$url"
    return
  }

  local latest="" latest_patch=""
  local current_is_stable=true
  is_stable "$current" || current_is_stable=false

  # Extract all versions from <version> tags
  local versions
  versions=$(echo "$xml" | grep -oP '(?<=<version>)[^<]+' | sort -V)

  # Find latest version (stable-only if current is stable, all if current is pre-release)
  while IFS= read -r v; do
    if $current_is_stable; then
      is_stable "$v" && latest="$v"
    else
      latest="$v"
    fi
  done <<< "$versions"

  # Find latest patch for current major.minor (same stability logic)
  while IFS= read -r v; do
    if same_major_minor "$current" "$v"; then
      if $current_is_stable; then
        is_stable "$v" && latest_patch="$v"
      else
        latest_patch="$v"
      fi
    fi
  done <<< "$versions"

  echo "${latest:-NONE}|${latest_patch:-NONE}|$url"
}

# Ordered keys for consistent output
ORDERED_KEYS=(
  agp kotlin compose-bom activity-compose navigation-compose lifecycle
  retrofit okhttp ktor kotlinx-serialization-json koin datastore-preferences
  tink-android coroutines collections-immutable core-ktx work-runtime
  koog koog-google mcp-sdk markdownRenderer
)

if $JSON_OUTPUT; then
  echo "["
  first=true
fi

for key in "${ORDERED_KEYS[@]}"; do
  spec="${DEPS[$key]}"
  repo="${spec%%|*}"
  coord="${spec#*|}"
  group="${coord%%:*}"
  artifact="${coord#*:}"

  current=$(get_current_version "$key")
  [[ -z "$current" ]] && current="NOT_FOUND"

  url=$(metadata_url "$repo" "$group" "$artifact")
  result=$(query_latest "$url" "$current")

  latest_stable="${result%%|*}"
  rest="${result#*|}"
  latest_patch="${rest%%|*}"
  source_url="${rest#*|}"

  if $JSON_OUTPUT; then
    $first || echo ","
    first=false
    cat <<JSONENTRY
  {
    "key": "$key",
    "group": "$group",
    "artifact": "$artifact",
    "current": "$current",
    "latest_stable": "$latest_stable",
    "latest_patch": "$latest_patch",
    "metadata_url": "$source_url"
  }
JSONENTRY
  else
    # Determine status
    status=""
    if [[ "$current" == "$latest_stable" ]]; then
      status="  current"
    elif [[ "$latest_patch" != "$current" && "$latest_patch" != "NONE" ]]; then
      status="  PATCH: $latest_patch"
    fi
    if [[ "$latest_stable" != "$current" && "$latest_stable" != "NONE" ]]; then
      if [[ -n "$status" && "$status" != "  current" ]]; then
        status="$status | LATEST: $latest_stable"
      else
        status="  LATEST: $latest_stable"
      fi
    fi

    printf "%-30s %-16s %s\n" "$key" "$current" "$status"
  fi
done

if $JSON_OUTPUT; then
  echo ""
  echo "]"
else
  echo ""
  echo "Sources (maven-metadata.xml):"
  for key in "${ORDERED_KEYS[@]}"; do
    spec="${DEPS[$key]}"
    repo="${spec%%|*}"
    coord="${spec#*|}"
    group="${coord%%:*}"
    artifact="${coord#*:}"
    echo "  $key: $(metadata_url "$repo" "$group" "$artifact")"
  done
fi
