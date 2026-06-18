#!/usr/bin/env bash
# =============================================================================
# scripts/db/restore.sh
#
# Decrypts a .sql.age backup and loads it into the Brujula database via psql.
#
# Usage:
#   ./scripts/db/restore.sh <path-to-backup.sql.age>
#
# Arguments:
#   <path-to-backup.sql.age>  — absolute or relative path to the encrypted backup
#                                produced by backup.sh.
#
# Prerequisites:
#   - Flyway schema must already be built (run the app once or ./gradlew flywayMigrate)
#     before restoring data. Do NOT restore into an empty (schema-less) database.
#   - .env file with DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD,
#     AGE_PRIVATE_KEY_PATH.
#   - age (https://github.com/FiloSottile/age) on PATH
#   - psql (PostgreSQL 18.4+) on PATH
#
# Output:
#   Data from the backup is loaded into the configured database.
#   No plaintext SQL file is left on disk (temp file removed by trap).
#
# Exit codes:
#   0  — success
#   1  — runtime failure (age or psql returned non-zero)
#   2  — bad arguments (missing or unreadable input file)
#   3  — preflight failure (missing tool or required env var)
# =============================================================================

set -euo pipefail

# ─── Locate repo root ─────────────────────────────────────────────────────────
# Allow REPO_ROOT to be overridden by the test harness.
if [ -z "${REPO_ROOT:-}" ]; then
  if command -v git > /dev/null 2>&1 && git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    REPO_ROOT="$(git rev-parse --show-toplevel)"
  else
    # Fallback: two levels up from scripts/db/
    REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
  fi
fi

# ─── Load .env ────────────────────────────────────────────────────────────────
ENV_FILE="${BRUJULA_ENV_FILE:-$REPO_ROOT/.env}"
if [ ! -f "$ENV_FILE" ]; then
  echo >&2 "ERROR: .env file not found at '$ENV_FILE'."
  echo >&2 "       Copy .env.example to .env and fill in the values."
  exit 3
fi
# shellcheck disable=SC1090
set -a
. "$ENV_FILE"
set +a

# ─── Preflight checks ─────────────────────────────────────────────────────────
preflight_error=0

if ! command -v age > /dev/null 2>&1; then
  echo >&2 "ERROR: 'age' is not on PATH. Install from https://github.com/FiloSottile/age"
  preflight_error=1
fi

if ! command -v psql > /dev/null 2>&1; then
  echo >&2 "ERROR: 'psql' is not on PATH. Install PostgreSQL client tools."
  preflight_error=1
fi

check_var() {
  local var_name="$1"
  local var_value="${!var_name:-}"
  if [ -z "$var_value" ]; then
    echo >&2 "ERROR: Required variable '$var_name' is missing or empty in .env."
    preflight_error=1
  fi
}

check_var DB_HOST
check_var DB_PORT
check_var DB_NAME
check_var DB_USER
check_var DB_PASSWORD
check_var AGE_PRIVATE_KEY_PATH

if [ "$preflight_error" -ne 0 ]; then
  exit 3
fi

# ─── Validate private key file is readable ────────────────────────────────────
if [ ! -f "$AGE_PRIVATE_KEY_PATH" ]; then
  echo >&2 "ERROR: Private key file not found at '$AGE_PRIVATE_KEY_PATH'."
  echo >&2 "       Set AGE_PRIVATE_KEY_PATH in .env to the path of your age private key."
  exit 3
fi

# ─── Validate input argument ──────────────────────────────────────────────────
if [ $# -lt 1 ] || [ -z "${1:-}" ]; then
  echo >&2 "ERROR: No .sql.age file specified."
  echo >&2 "       Usage: $0 <path-to-backup.sql.age>"
  exit 2
fi

IN="$1"

if [ ! -f "$IN" ]; then
  echo >&2 "ERROR: Backup file not found or not readable: '$IN'"
  exit 2
fi

# ─── Temp file + cleanup trap ─────────────────────────────────────────────────
TMP="$(mktemp)"
# Guarantee plaintext deletion on any exit path (success, failure, SIGINT, SIGTERM)
trap 'rm -f "$TMP"' EXIT INT TERM

# ─── Decrypt with age ─────────────────────────────────────────────────────────
echo "Decrypting '$IN' ..."
age -d -i "$AGE_PRIVATE_KEY_PATH" -o "$TMP" "$IN"

# ─── Restore via psql ─────────────────────────────────────────────────────────
echo "Restoring data to database '$DB_NAME' on $DB_HOST:$DB_PORT ..."
PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  -v ON_ERROR_STOP=1 \
  -f "$TMP"

echo "Restore complete."
