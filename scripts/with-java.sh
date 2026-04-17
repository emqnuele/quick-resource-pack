#!/usr/bin/env sh

set -eu

if [ "$#" -lt 2 ]; then
  echo "usage: $0 <21|25> <command> [args...]" >&2
  exit 2
fi

REQ="$1"
shift

case "$REQ" in
  21|25) ;;
  *)
    echo "unsupported java version: $REQ (use 21 or 25)" >&2
    exit 2
    ;;
esac

java_matches_version() {
  home="$1"
  [ -x "$home/bin/java" ] || return 1
  major=$("$home/bin/java" -version 2>&1 | sed -n '1s/.*version "\([0-9][0-9]*\).*/\1/p')
  [ "$major" = "$REQ" ]
}

find_java_home() {
  eval "version_home=\${JAVA${REQ}_HOME:-}"

  for home in \
    "$version_home" \
    "${JAVA_HOME:-}" \
    "/usr/lib/jvm/java-${REQ}-openjdk" \
    "/usr/lib/jvm/java-${REQ}-openjdk-amd64" \
    "/usr/lib/jvm/jre-${REQ}-openjdk"
  do
    [ -n "$home" ] || continue
    if java_matches_version "$home"; then
      echo "$home"
      return 0
    fi
  done

  if command -v "java${REQ}" >/dev/null 2>&1; then
    java_bin=$(command -v "java${REQ}")
    bin_dir=$(CDPATH= cd -- "$(dirname -- "$java_bin")" && pwd)
    home=$(CDPATH= cd -- "$bin_dir/.." && pwd)
    if java_matches_version "$home"; then
      echo "$home"
      return 0
    fi
  fi

  return 1
}

if ! JAVA_HOME_SELECTED=$(find_java_home); then
  echo "java ${REQ} not found. install with: sudo dnf install java-${REQ}-openjdk-devel" >&2
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_SELECTED"
export PATH="$JAVA_HOME/bin:$PATH"

exec "$@"