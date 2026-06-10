#!/usr/bin/env bash
# ci/run.sh — Backend CI gate (local/manual)
#
# Runs the full backend quality gate via ./gradlew check, which depends on:
#   - spotlessCheck  — code formatting (Palantir Java format)
#   - test           — all 1305+ unit + integration tests (needs Docker for Testcontainers)
#   - jacocoTestCoverageVerification — LINE >= 80%, BRANCH >= 70% on domain + service
#
# Prerequisites:
#   - Java (JAVA_HOME set or available on PATH)
#   - Docker (required by Testcontainers for integration tests)
#   - Gradle wrapper present at ./gradlew
#
# Usage:
#   bash ci/run.sh          # run from repo root
#   ./ci/run.sh             # same (chmod +x ci/run.sh)
#
# Exit codes:
#   0 — all gates passed
#   1 — one or more gates failed (the failing step is reported by Gradle)
#
# Note: steps run in strict order with set -euo pipefail; a failure at any step
# aborts the pipeline and propagates a non-zero exit code.

set -euo pipefail

# Export JAVA_HOME if not already set (adjust path to match local mise/sdkman/jabba install)
if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_HOME=/home/diegnghtmr/.local/share/mise/installs/java/temurin-25.0.3+9.0.LTS
  export JAVA_HOME
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "=== [CI] Backend quality gate ==="
echo "    JAVA_HOME : ${JAVA_HOME}"
echo "    Repo root : ${REPO_ROOT}"
echo ""

echo "--- [1/1] check (spotlessCheck + test + jacocoTestCoverageVerification) ---"
./gradlew check

echo ""
echo "=== [CI] All gates PASSED ==="
