#!/usr/bin/env bash
# =============================================================================
# scripts/db/backup.sh
#
# Creates an encrypted pg_dump backup of the Brujula database.
#
# Usage:
#   ./scripts/db/backup.sh <motivo>
#
# Arguments:
#   motivo  — A short slug identifying the backup reason (e.g. "before-migration").
#              Allowed characters: [a-zA-Z0-9_-]. Others are stripped.
#              Defaults to "manual" if omitted.
#
# Requires:
#   - .env file with DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD,
#     AGE_PUBLIC_KEY, AGE_PRIVATE_KEY_PATH
#   - age (https://github.com/FiloSottile/age) on PATH
#   - pg_dump (PostgreSQL 18.4+) on PATH
#
# Output:
#   backups/brujula-data-YYYY-MM-DD-<motivo>.sql.age
#
# Exit codes:
#   0  — success
#   1  — runtime failure (pg_dump or age returned non-zero)
#   2  — bad arguments / usage error
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

if ! command -v pg_dump > /dev/null 2>&1; then
  echo >&2 "ERROR: 'pg_dump' is not on PATH. Install PostgreSQL client tools."
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
check_var AGE_PUBLIC_KEY

if [ "$preflight_error" -ne 0 ]; then
  exit 3
fi

# ─── Motivo argument ──────────────────────────────────────────────────────────
RAW_MOTIVO="${1:-manual}"
# Sanitize: keep only [a-zA-Z0-9_-]
MOTIVO="$(printf '%s' "$RAW_MOTIVO" | tr -cd 'a-zA-Z0-9_-')"
if [ -z "$MOTIVO" ]; then
  MOTIVO="manual"
fi

# ─── Output path ──────────────────────────────────────────────────────────────
TS="$(date +%F)"
BACKUPS_DIR="$REPO_ROOT/backups"
mkdir -p "$BACKUPS_DIR"
OUT="$BACKUPS_DIR/brujula-data-$TS-$MOTIVO.sql.age"

# ─── Temp file + cleanup trap ─────────────────────────────────────────────────
TMP="$(mktemp)"
# Guarantee plaintext deletion on any exit path (success, failure, SIGINT, SIGTERM)
trap 'rm -f "$TMP"' EXIT INT TERM

# ─── pg_dump ──────────────────────────────────────────────────────────────────
echo "Running pg_dump for database '$DB_NAME' on $DB_HOST:$DB_PORT ..."
PGPASSWORD="$DB_PASSWORD" pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --data-only \
  --column-inserts \
  --exclude-table=platform.spring_session \
  --exclude-table=platform.spring_session_attributes \
  --exclude-table=platform.idempotency_record \
  > "$TMP"

echo "Encrypting backup to '$OUT' ..."

# ─── Encrypt with age ─────────────────────────────────────────────────────────
age -r "$AGE_PUBLIC_KEY" -o "$OUT" "$TMP"

echo "Backup complete: $OUT"
