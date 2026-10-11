#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

GW_CMD=${GW_CMD:-./gw25}
FAIL_FAST=0
declare -a TARGETS=()

declare -a DEFAULT_TARGETS=("26.1" "26.1.1" "26.1.2" "26.2")

usage() {
  cat <<'EOF'
usage: ./scripts/check-compat-26.sh [--gw <path>] [--fail-fast] [26.x ...]

runs compile checks across minecraft 26.x versions by resolving:
- latest fabric-api build matching each target patch
- latest cloth-config build matching each target minor line (26.1, 26.2)
- pinned mod menu version per minor line

notes:
- target "26" is treated as alias for latest known 26.x target (currently 26.2)

examples:
  ./scripts/check-compat-26.sh
  ./scripts/check-compat-26.sh 26.1 26.1.2
  ./scripts/check-compat-26.sh 26.2

examples:
  ./scripts/check-compat-26.sh
  ./scripts/check-compat-26.sh 26.1 26.1.2
  ./scripts/check-compat-26.sh --fail-fast 26
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --fail-fast)
      FAIL_FAST=1
      shift
      ;;
    --gw)
      if [[ $# -lt 2 ]]; then
        echo "missing value for --gw" >&2
        exit 2
      fi
      GW_CMD="$2"
      shift 2
      ;;
    *)
      TARGETS+=("$1")
      shift
      ;;
  esac
done

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 2
  fi
}

require_cmd curl
require_cmd grep
require_cmd sed
require_cmd sort

if ! command -v "$GW_CMD" >/dev/null 2>&1 && [[ ! -x "$GW_CMD" ]]; then
  echo "cannot execute gradle wrapper command: $GW_CMD" >&2
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
    printf '%s\n' "${DEFAULT_TARGETS[@]}" | sort -V | tail -n 1
    return 0
  fi
  echo "$target"
}

if [[ ${#TARGETS[@]} -eq 0 ]]; then
  TARGETS=("${DEFAULT_TARGETS[@]}")
fi

declare -a NORMALIZED_TARGETS=()
for target in "${TARGETS[@]}"; do
  normalized=$(normalize_target "$target")

  if [[ ! "$normalized" =~ ^26\.[0-9]+(\.[0-9]+)?$ ]]; then
    echo "unsupported target format: $target" >&2
    exit 2
  fi

  if ! contains_target "$normalized" "${NORMALIZED_TARGETS[@]}"; then
    NORMALIZED_TARGETS+=("$normalized")
  fi
done

FABRIC_API_METADATA_FILE=$(mktemp)
CLOTH_METADATA_FILE=$(mktemp)
trap 'rm -f "$FABRIC_API_METADATA_FILE" "$CLOTH_METADATA_FILE"' EXIT

curl -fsSL https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml -o "$FABRIC_API_METADATA_FILE"
curl -fsSL https://maven.shedaniel.me/me/shedaniel/cloth/cloth-config-fabric/maven-metadata.xml -o "$CLOTH_METADATA_FILE"

latest_fabric_api_for() {
  local game_version="$1"
  grep -oE "<version>[^<]+\\+$game_version</version>" "$FABRIC_API_METADATA_FILE" \
    | sed -e 's#<version>##' -e 's#</version>##' \
    | sort -V \
    | tail -n 1
}

minor_line_of() {
  echo "$1" | cut -d. -f1-2
}

latest_cloth_for() {
  local minor_line
  minor_line=$(minor_line_of "$1")
  grep -oE '<version>[^<]+</version>' "$CLOTH_METADATA_FILE" \
    | sed -e 's#<version>##' -e 's#</version>##' \
    | grep -E "^${minor_line//./\\.}\.[0-9]+$" \
    | sort -V \
    | tail -n 1
}

mod_menu_for() {
  case "$(minor_line_of "$1")" in
    26.1) echo "18.0.0-alpha.8" ;;
    26.2) echo "20.0.3" ;;
    *) echo "" ;;
  esac
}

LOG_DIR="$ROOT_DIR/compat-logs"
mkdir -p "$LOG_DIR"

failures=0
printf "%-10s %-24s %-12s %-14s %-8s\n" "mc" "fabric-api" "cloth" "modmenu" "status"
printf "%-10s %-24s %-12s %-14s %-8s\n" "----------" "------------------------" "------------" "--------------" "--------"

for game_version in "${NORMALIZED_TARGETS[@]}"; do
  fabric_api_version=$(latest_fabric_api_for "$game_version" || true)
  CLOTH_VERSION=$(latest_cloth_for "$game_version" || true)
  MOD_MENU_VERSION=$(mod_menu_for "$game_version")

  if [[ -z "$fabric_api_version" || -z "$CLOTH_VERSION" || -z "$MOD_MENU_VERSION" ]]; then
    printf "%-10s %-24s %-12s %-14s %-8s\n" "$game_version" "${fabric_api_version:-n/a}" "${CLOTH_VERSION:-n/a}" "${MOD_MENU_VERSION:-n/a}" "fail"
    echo "  unresolved dependency metadata for $game_version"
    failures=$((failures + 1))
    if [[ $FAIL_FAST -eq 1 ]]; then
      break
    fi
    continue
  fi

  log_file="$LOG_DIR/$game_version.log"
  if "$GW_CMD" --no-daemon clean compileJava \
    -Pminecraft_version="$game_version" \
    -Pfabric_api_version="$fabric_api_version" \
    -Pcloth_config_version="$CLOTH_VERSION" \
    -Pmod_menu_version="$MOD_MENU_VERSION" \
    >"$log_file" 2>&1; then
    printf "%-10s %-24s %-12s %-14s %-8s\n" "$game_version" "$fabric_api_version" "$CLOTH_VERSION" "$MOD_MENU_VERSION" "ok"
  else
    printf "%-10s %-24s %-12s %-14s %-8s\n" "$game_version" "$fabric_api_version" "$CLOTH_VERSION" "$MOD_MENU_VERSION" "fail"
    echo "  log: ${log_file#$ROOT_DIR/}"
    failures=$((failures + 1))
    if [[ $FAIL_FAST -eq 1 ]]; then
      break
    fi
  fi
done

if [[ $failures -gt 0 ]]; then
  echo
  echo "compatibility check failed for $failures target(s)"
  exit 1
fi

echo
echo "compatibility check passed for ${#NORMALIZED_TARGETS[@]} target(s)"
