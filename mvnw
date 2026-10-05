#!/usr/bin/env sh
set -e

MAVEN_VERSION=3.9.9
BASE_DIR="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MAVEN_BIN" ]; then
  mkdir -p "$WRAPPER_DIR"
  ZIP="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip" -o "$ZIP"
    else
      wget -q "https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip" -O "$ZIP"
    fi
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -oq "$ZIP" -d "$WRAPPER_DIR"
  else
    (cd "$WRAPPER_DIR" && jar xf "$ZIP")
  fi
fi

exec "$MAVEN_BIN" "$@"
