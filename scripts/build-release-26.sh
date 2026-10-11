#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

GW_CMD=${GW_CMD:-./gw25}
OUT_DIR="$ROOT_DIR/release/26.x"
BASE_VERSION=$(sed -n 's/^mod_version=//p' "$ROOT_DIR/gradle.properties" | head -n 1 | tr -d '\r')
INCLUDE_SOURCES=0
INCLUDE_MAJOR_TAG=0
DRY_RUN=0
declare -a REQUESTED_TARGETS=()

MATRIX=$(cat <<'EOF'
26.1|0.145.1+26.1|26.1.154|18.0.0-alpha.8
26.1.1|0.145.4+26.1.1|26.1.154|18.0.0-alpha.8
26.1.2|0.146.1+26.1.2|26.1.154|18.0.0-alpha.8
EOF
)

usage() {
  cat <<'EOF'
usage: ./scripts/build-release-26.sh [options] [26.x ...]

builds modrinth-ready jars for minecraft 26.1.x.

the output layout is:
- release/26.x/<mc-version>/quick-resource-pack-<mod-version>-mc<mc-version>.jar
- release/26.x/modrinth-upload.csv

notes:
- target "26" is treated as alias of latest known 26.x target (currently 26.1.2)

options:
  --base-version <v>   base mod version (default: mod_version from gradle.properties)
  --out-dir <path>     output folder (default: release/26.x)
  --include-sources    copy -sources jars too
  --gw <path>          gradle command (default: ./gw25)
  --dry-run            print commands without running gradle
  -h, --help           show this help

examples:
  ./scripts/build-release-26.sh --base-version 1.2.0
  ./scripts/build-release-26.sh --base-version 1.2.0 26.1 26.1.2
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --base-version)
      BASE_VERSION=${2:-}
      shift 2
      ;;
    --out-dir)
      OUT_DIR=${2:-}
      shift 2
      ;;
    --include-sources)
      INCLUDE_SOURCES=1
      shift
      ;;
    --include-major-tag)
      INCLUDE_MAJOR_TAG=1
      shift
      ;;
    --gw)
      GW_CMD=${2:-}
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    *)
      REQUESTED_TARGETS+=("$1")
      shift
      ;;
  esac
done

if [[ -z "$BASE_VERSION" ]]; then
  echo "base version is empty; use --base-version or set mod_version in gradle.properties" >&2
  exit 2
fi

if [[ $INCLUDE_MAJOR_TAG -eq 1 ]]; then
  echo "--include-major-tag is unsupported: Modrinth rejects plain '26' in game_versions" >&2
  echo "use patch versions only (26.1, 26.1.1, 26.1.2)" >&2
  exit 2
fi

if ! command -v "$GW_CMD" >/dev/null 2>&1 && [[ ! -x "$GW_CMD" ]]; then
  echo "cannot execute gradle command: $GW_CMD" >&2
  exit 2
fi

contains_target() {
  local target="$1"
  shift
  local current
  for current in "$@"; do
    if [[ "$current" == "$target" ]]; then
      return 0
    fi
  done
  return 1
}

normalize_target() {
  local target="$1"
  if [[ "$target" == "26" ]]; then
    echo "26.1.2"
    return 0
  fi
  echo "$target"
}

declare -a TARGET_ROWS=()
declare -a KNOWN_TARGETS=()
declare -a NORMALIZED_REQUESTED_TARGETS=()

for requested in "${REQUESTED_TARGETS[@]}"; do
  normalized=$(normalize_target "$requested")
  if ! contains_target "$normalized" "${NORMALIZED_REQUESTED_TARGETS[@]}"; then
    NORMALIZED_REQUESTED_TARGETS+=("$normalized")
  fi
done

while IFS='|' read -r mc fabric cloth modmenu; do
  [[ -z "$mc" ]] && continue
  KNOWN_TARGETS+=("$mc")

  if [[ ${#NORMALIZED_REQUESTED_TARGETS[@]} -eq 0 ]] || contains_target "$mc" "${NORMALIZED_REQUESTED_TARGETS[@]}"; then
    TARGET_ROWS+=("$mc|$fabric|$cloth|$modmenu")
  fi
done <<< "$MATRIX"

if [[ ${#TARGET_ROWS[@]} -eq 0 ]]; then
  echo "no targets selected. known targets: ${KNOWN_TARGETS[*]}" >&2
  exit 2
fi

for target in "${NORMALIZED_REQUESTED_TARGETS[@]}"; do
  if ! contains_target "$target" "${KNOWN_TARGETS[@]}"; then
    echo "unsupported target: $target" >&2
    echo "known targets: ${KNOWN_TARGETS[*]}" >&2
    exit 2
  fi
done

mkdir -p "$OUT_DIR"
MANIFEST_PATH="$OUT_DIR/modrinth-upload.csv"

if [[ $DRY_RUN -eq 0 ]]; then
  printf 'file,version_number,game_versions,loaders,fabric_api,cloth_config,mod_menu\n' > "$MANIFEST_PATH"
fi

printf "%-10s %-22s %-22s %-12s %-14s\n" "mc" "version_number" "fabric_api" "cloth" "modmenu"
printf "%-10s %-22s %-22s %-12s %-14s\n" "----------" "----------------------" "----------------------" "------------" "--------------"

for row in "${TARGET_ROWS[@]}"; do
  IFS='|' read -r mc fabric cloth modmenu <<< "$row"
  version_number="${BASE_VERSION}-mc${mc}"

  printf "%-10s %-22s %-22s %-12s %-14s\n" "$mc" "$version_number" "$fabric" "$cloth" "$modmenu"

  gradle_cmd=(
    "$GW_CMD" --no-daemon clean build
    "-Pmod_version=$version_number"
    "-Pminecraft_version=$mc"
    "-Pfabric_api_version=$fabric"
    "-Pcloth_config_version=$cloth"
    "-Pmod_menu_version=$modmenu"
  )

  if [[ $DRY_RUN -eq 1 ]]; then
    echo "dry-run: ${gradle_cmd[*]}"
    continue
  fi

  "${gradle_cmd[@]}"

  release_version_dir="$OUT_DIR/$mc"
  mkdir -p "$release_version_dir"

  jar_path="$ROOT_DIR/build/libs/quick-resource-pack-$version_number.jar"
  if [[ ! -f "$jar_path" ]]; then
    echo "missing jar after build: $jar_path" >&2
    exit 1
  fi

  cp "$jar_path" "$release_version_dir/"

  if [[ $INCLUDE_SOURCES -eq 1 ]]; then
    sources_path="$ROOT_DIR/build/libs/quick-resource-pack-$version_number-sources.jar"
    if [[ -f "$sources_path" ]]; then
      cp "$sources_path" "$release_version_dir/"
    fi
  fi

  game_versions="$mc"

  printf '%s,%s,%s,%s,%s,%s,%s\n' \
    "${mc}/quick-resource-pack-$version_number.jar" \
    "$version_number" \
    "$game_versions" \
    "fabric" \
    "$fabric" \
    "$cloth" \
    "$modmenu" >> "$MANIFEST_PATH"
done

if [[ $DRY_RUN -eq 1 ]]; then
  echo
  echo "dry-run completed"
  exit 0
fi

echo
echo "release artifacts created in: ${OUT_DIR#$ROOT_DIR/}"
echo "manifest: ${MANIFEST_PATH#$ROOT_DIR/}"
