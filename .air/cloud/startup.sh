#!/usr/bin/env bash
# Air cloud environment startup script for the JetBrains Academy plugin.
# Ensures JDK 25 and, on warm-up, resolves Gradle dependencies (including the target IDEs)
# so they are cached in the snapshot. Does not compile the project or run tests.
set -euo pipefail

if [ "${AIR_STARTUP_MODE:-}" = warmup ]; then WARMUP=1; else WARMUP=; fi

log() { echo "[startup] $*"; }

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_DIR"

java_major() { "$1" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.specification.version = //p'; }

# --- JDK 25 (required by CONTRIBUTING.md and the Gradle toolchain) ---
if [ -z "${JAVA_HOME:-}" ] && command -v java > /dev/null && [ "$(java_major java)" = 25 ]; then
  # `java` from PATH is already JDK 25, use its home
  JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.home = //p')"
  export JAVA_HOME
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ] || [ "$(java_major "$JAVA_HOME/bin/java")" != 25 ]; then
  # Debian/Ubuntu JDK dir is suffixed with the dpkg architecture (amd64, arm64, ...).
  JDK25="/usr/lib/jvm/java-25-openjdk-$(dpkg --print-architecture)"
  if [ ! -x "$JDK25/bin/java" ]; then
    log "installing openjdk-25-jdk-headless"
    sudo apt-get update -qq
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-25-jdk-headless
  fi
  export JAVA_HOME="$JDK25"
  export PATH="$JAVA_HOME/bin:$PATH"

  # Persist JAVA_HOME for agent login/interactive shells.
  ENV_FILE="$HOME/.edu-plugin-env.sh"
  cat > "$ENV_FILE" <<EOF
export JAVA_HOME=$JDK25
export PATH="\$JAVA_HOME/bin:\$PATH"
EOF
  MARKER="# edu-plugin-env"
  PROFILE="$HOME/.profile"
  for f in "$HOME/.bash_profile" "$HOME/.bash_login" "$HOME/.profile"; do
    if [ -f "$f" ]; then PROFILE="$f"; break; fi
  done
  for f in "$PROFILE" "$HOME/.bashrc"; do
    touch "$f"
    grep -qF "$MARKER" "$f" || echo "[ -f \"$ENV_FILE\" ] && . \"$ENV_FILE\" $MARKER" >> "$f"
  done
fi
log "using JDK at $JAVA_HOME"

# Downloads all dependencies and IDEs into the Gradle cache.
# See `resolve-dependencies.init.gradle.kts` for details
resolve_dependencies() {
  chmod +x ./gradlew
  # Retry since remote repositories occasionally drop connections.
  # Already downloaded artifacts are kept in the Gradle cache between attempts
  local attempt
  for attempt in 1 2 3; do
    log "resolving Gradle dependencies (attempt $attempt)"
    if ./gradlew --init-script .air/cloud/resolve-dependencies.init.gradle.kts airResolveDependencies --console=plain; then
      log "Gradle dependencies resolved"
      return 0
    fi
  done
  log "failed to resolve Gradle dependencies"
  return 1
}

if [ -n "$WARMUP" ]; then resolve_dependencies; fi
log "done"
