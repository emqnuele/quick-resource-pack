#!/usr/bin/env sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ -n "${JAVA_HOME:-}" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
elif [ -n "${JAVA21_HOME:-}" ]; then
  JAVA_CMD="$JAVA21_HOME/bin/java"
elif [ -x "/usr/lib/jvm/java-21-openjdk/bin/java" ]; then
  JAVA_CMD="/usr/lib/jvm/java-21-openjdk/bin/java"
else
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
