#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

GW_CMD=${GW_CMD:-./gw21}
FAIL_FAST=0
declare -a TARGETS=()

usage() {
  cat <<'EOF'
usage: ./scripts/check-compat-1.21.sh [--gw <path>] [--fail-fast] [1.21.x ...]

runs compile checks across minecraft 1.21.x versions by auto-resolving:
- latest yarn mappings for each target patch
- latest fabric-api build matching each target patch

examples:
  ./scripts/check-compat-1.21.sh
  ./scripts/check-compat-1.21.sh 1.21.4 1.21.11
  GW_CMD=./gw21 ./scripts/check-compat-1.21.sh --fail-fast
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
require_cmd jq
require_cmd grep
require_cmd sed
require_cmd sort

if ! command -v "$GW_CMD" >/dev/null 2>&1 && [[ ! -x "$GW_CMD" ]]; then
  echo "cannot execute gradle wrapper command: $GW_CMD" >&2
  exit 2
fi

discover_targets() {
  curl -fsSL https://meta.fabricmc.net/v2/versions/game \
    | jq -r '.[] | select(.stable == true and (.version | test("^1\\.21\\.[0-9]+$"))) | .version' \
    | sort -V
}

latest_yarn_for() {
  local game_version="$1"
  curl -fsSL "https://meta.fabricmc.net/v2/versions/yarn/$game_version" \
    | jq -r '.[].version' \
    | sort -V \
    | tail -n 1
}

FABRIC_API_METADATA_FILE=$(mktemp)
trap 'rm -f "$FABRIC_API_METADATA_FILE"' EXIT
curl -fsSL https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml -o "$FABRIC_API_METADATA_FILE"

latest_fabric_api_for() {
  local game_version="$1"
  grep -oE "<version>[^<]+\\+$game_version</version>" "$FABRIC_API_METADATA_FILE" \
    | sed -e 's#<version>##' -e 's#</version>##' \
    | sort -V \
    | tail -n 1
}

if [[ ${#TARGETS[@]} -eq 0 ]]; then
  mapfile -t TARGETS < <(discover_targets)
fi

if [[ ${#TARGETS[@]} -eq 0 ]]; then
  echo "no minecraft 1.21.x targets found" >&2
  exit 1
fi

LOG_DIR="$ROOT_DIR/compat-logs"
mkdir -p "$LOG_DIR"

failures=0
printf "%-10s %-20s %-24s %-8s\n" "mc" "yarn" "fabric-api" "status"
printf "%-10s %-20s %-24s %-8s\n" "----------" "--------------------" "------------------------" "--------"

for game_version in "${TARGETS[@]}"; do
  yarn_version=$(latest_yarn_for "$game_version" || true)
  fabric_api_version=$(latest_fabric_api_for "$game_version" || true)

  if [[ -z "$yarn_version" || -z "$fabric_api_version" ]]; then
    printf "%-10s %-20s %-24s %-8s\n" "$game_version" "n/a" "n/a" "fail"
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
    -Pyarn_mappings="$yarn_version" \
    -Pfabric_version="$fabric_api_version" \
    >"$log_file" 2>&1; then
    printf "%-10s %-20s %-24s %-8s\n" "$game_version" "$yarn_version" "$fabric_api_version" "ok"
  else
    printf "%-10s %-20s %-24s %-8s\n" "$game_version" "$yarn_version" "$fabric_api_version" "fail"
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
echo "compatibility check passed for ${#TARGETS[@]} target(s)"
