#!/usr/bin/env bash
# check-deps.sh — Query official Maven repositories for latest dependency versions.
# Compares against versions declared in gradle/libs.versions.toml.
#
# Reports three levels per dependency:
#   PATCH — same X.Y, newer Z        (e.g., 2.11.0 → 2.11.1)
#   MINOR — same X, newer Y          (e.g., 2.11.0 → 2.12.0)
#   MAJOR — newer X (latest overall) (e.g., 2.11.0 → 3.0.0)
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
  [kotlinx-serialization-json]="central|org.jetbrains.kotlinx:kotlinx-serialization-json"
  [koin]="central|io.insert-koin:koin-core"
  [datastore-preferences]="google|androidx.datastore:datastore-preferences"
  [tink-android]="central|com.google.crypto.tink:tink-android"
  [coroutines]="central|org.jetbrains.kotlinx:kotlinx-coroutines-android"
  [core-ktx]="google|androidx.core:core-ktx"
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
  local lv="${v,,}"
  if [[ "$lv" =~ (alpha|beta|rc|dev|snapshot|eap|milestone|-b[0-9]) ]]; then
    return 1
  fi
  return 0
}

# Extract major version component
get_major() {
  local v="$1"
  # Compose BOM: YYYY.MM.DD — major is YYYY
  if [[ "$v" =~ ^[0-9]{4}\.[0-9]{2}\.[0-9]{2}$ ]]; then
    echo "${v%%.*}"
    return
  fi
  echo "${v%%.*}"
}

# Extract major.minor version components
get_major_minor() {
  local v="$1"
  # Compose BOM: YYYY.MM.DD — major.minor is YYYY.MM
  if [[ "$v" =~ ^[0-9]{4}\.[0-9]{2}\.[0-9]{2}$ ]]; then
    echo "${v%.*}"
    return
  fi
  echo "${v%.*}"
}

query_latest() {
  local url="$1" current="$2"
  local xml
  xml=$(curl -sfL --connect-timeout 5 --max-time 10 "$url" 2>/dev/null) || {
    echo "FETCH_ERROR|FETCH_ERROR|FETCH_ERROR|$url"
    return
  }

  local latest_stable="" latest_patch="" latest_minor=""
  local cur_major cur_major_minor
  cur_major=$(get_major "$current")
  cur_major_minor=$(get_major_minor "$current")

  # Extract all versions from <version> tags
  local versions
  versions=$(echo "$xml" | grep -oP '(?<=<version>)[^<]+' | sort -V)

  while IFS= read -r v; do
    is_stable "$v" || continue

    latest_stable="$v"

    local v_major v_major_minor
    v_major=$(get_major "$v")
    v_major_minor=$(get_major_minor "$v")

    # Same major.minor → patch candidate
    if [[ "$v_major_minor" == "$cur_major_minor" ]]; then
      latest_patch="$v"
    fi

    # Same major → minor candidate
    if [[ "$v_major" == "$cur_major" ]]; then
      latest_minor="$v"
    fi
  done <<< "$versions"

  echo "${latest_stable:-NONE}|${latest_minor:-NONE}|${latest_patch:-NONE}|$url"
}

# Ordered keys for consistent output
ORDERED_KEYS=(
  agp kotlin compose-bom activity-compose navigation-compose lifecycle
  retrofit okhttp kotlinx-serialization-json koin datastore-preferences
  tink-android coroutines core-ktx markdownRenderer
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

  # Parse: latest_stable|latest_minor|latest_patch|url
  latest_stable="${result%%|*}"
  rest="${result#*|}"
  latest_minor="${rest%%|*}"
  rest="${rest#*|}"
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
    "latest_patch": "$latest_patch",
    "latest_minor": "$latest_minor",
    "latest_stable": "$latest_stable",
    "metadata_url": "$source_url"
  }
JSONENTRY
  else
    # Build status string
    parts=()

    if [[ "$latest_patch" != "$current" && "$latest_patch" != "NONE" ]]; then
      parts+=("PATCH: $latest_patch")
    fi

    if [[ "$latest_minor" != "$current" && "$latest_minor" != "$latest_patch" && "$latest_minor" != "NONE" ]]; then
      parts+=("MINOR: $latest_minor")
    fi

    if [[ "$latest_stable" != "$current" && "$latest_stable" != "$latest_minor" && "$latest_stable" != "$latest_patch" && "$latest_stable" != "NONE" ]]; then
      parts+=("MAJOR: $latest_stable")
    fi

    if [[ ${#parts[@]} -eq 0 ]]; then
      status="  current"
    else
      status="  $(IFS=' | '; echo "${parts[*]}")"
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
