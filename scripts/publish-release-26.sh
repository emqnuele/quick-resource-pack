#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

MANIFEST=${MANIFEST:-release/26.x/modrinth-upload.csv}
NOTES=${NOTES:-MODRINTH_RELEASE_26.x.md}
PROJECT=${PROJECT:-quick-resource-pack}

exec ./scripts/publish_modrinth_releases.py \
  --project "$PROJECT" \
  --manifest "$MANIFEST" \
  --notes "$NOTES" \
  --no-default-deps \
  --dependency fabric-api:required \
  --dependency cloth-config:required \
  --dependency modmenu:optional \
  "$@"
