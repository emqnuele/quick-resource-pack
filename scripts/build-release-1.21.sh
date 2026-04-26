#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

GW_CMD=${GW_CMD:-./gw21}
OUT_DIR="$ROOT_DIR/release/1.21.x"
BASE_VERSION=$(sed -n 's/^mod_version=//p' "$ROOT_DIR/gradle.properties" | head -n 1)
INCLUDE_SOURCES=0
DRY_RUN=0
declare -a REQUESTED_TARGETS=()

MATRIX=$(cat <<'EOF'
1.21.9|1.21.9+build.1|0.134.1+1.21.9|21.11.153|16.0.1
1.21.10|1.21.10+build.3|0.138.4+1.21.10|21.11.153|16.0.1
1.21.11|1.21.11+build.4|0.141.3+1.21.11|21.11.153|17.0.0
EOF
)

usage() {
  cat <<'EOF'
usage: ./scripts/build-release-1.21.sh [options] [1.21.x ...]

builds modrinth-ready jars for minecraft 1.21.x.

the output layout is:
- release/1.21.x/<mc-version>/quick-resource-pack-<mod-version>-mc<mc-version>.jar
- release/1.21.x/modrinth-upload.csv

options:
  --base-version <v>   base mod version (default: mod_version from gradle.properties)
  --out-dir <path>     output folder (default: release/1.21.x)
  --include-sources    copy -sources jars too
  --gw <path>          gradle command (default: ./gw21)
  --dry-run            print commands without running gradle
  -h, --help           show this help

examples:
  ./scripts/build-release-1.21.sh --base-version 1.1.0
  ./scripts/build-release-1.21.sh --base-version 1.1.0 1.21.8 1.21.11
  GW_CMD=./gw21 ./scripts/build-release-1.21.sh --include-sources
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

declare -a TARGET_ROWS=()
declare -a KNOWN_TARGETS=()

while IFS='|' read -r mc yarn fabric cloth modmenu; do
  [[ -z "$mc" ]] && continue
  KNOWN_TARGETS+=("$mc")

  if [[ ${#REQUESTED_TARGETS[@]} -eq 0 ]] || contains_target "$mc" "${REQUESTED_TARGETS[@]}"; then
    TARGET_ROWS+=("$mc|$yarn|$fabric|$cloth|$modmenu")
  fi
done <<< "$MATRIX"

if [[ ${#TARGET_ROWS[@]} -eq 0 ]]; then
  echo "no targets selected. known targets: ${KNOWN_TARGETS[*]}" >&2
  exit 2
fi

for target in "${REQUESTED_TARGETS[@]}"; do
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

printf "%-10s %-22s %-22s %-12s %-10s\n" "mc" "version_number" "fabric_api" "cloth" "modmenu"
printf "%-10s %-22s %-22s %-12s %-10s\n" "----------" "----------------------" "----------------------" "------------" "----------"

for row in "${TARGET_ROWS[@]}"; do
  IFS='|' read -r mc yarn fabric cloth modmenu <<< "$row"
  version_number="${BASE_VERSION}-mc${mc}"

  printf "%-10s %-22s %-22s %-12s %-10s\n" "$mc" "$version_number" "$fabric" "$cloth" "$modmenu"

  gradle_cmd=(
    "$GW_CMD" --no-daemon clean build
    "-Pmod_version=$version_number"
    "-Pminecraft_version=$mc"
    "-Pyarn_mappings=$yarn"
    "-Pfabric_version=$fabric"
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

  printf '%s,%s,%s,%s,%s,%s,%s\n' \
    "${mc}/quick-resource-pack-$version_number.jar" \
    "$version_number" \
    "$mc" \
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
