# Backup Log

This directory stores encrypted database backup archives.
Files with `.sql.age` extension are age-encrypted ciphertext — safe to commit.
Plaintext `.sql` files are ignored by `.gitignore` and must never be committed.

## Frequency Policy (RF-07)

Create a backup:
- **Before every schema migration** (i.e., before running `./gradlew flywayMigrate` or starting
  the application when new Flyway migrations are present).
- **After each significant data-entry session** (e.g., after entering semester registrations,
  uploading curricula, or any bulk data operation).

Run the backup script:
```bash
# Linux / macOS
./scripts/db/backup.sh <motivo>

# Windows (PowerShell)
pwsh .\scripts\db\backup.ps1 <motivo>
```

## Excluded Tables Rationale

The following tables are intentionally excluded from every backup because they contain
transient, session-scoped, or idempotency-tracking data that should NOT be restored:

| Table | Reason |
|-------|--------|
| `platform.spring_session` | HTTP session tokens — meaningless after restore; users re-login |
| `platform.spring_session_attributes` | Session attribute data tied to the transient session rows |
| `platform.idempotency_record` | Deduplication fingerprints — restored data is already idempotent |

Restoring these tables would cause stale session conflicts and could break idempotency guarantees.

## Backup Log

| Date | Motivo | File | Notes |
|------|--------|------|-------|
| <!-- YYYY-MM-DD --> | <!-- reason slug --> | <!-- filename --> | <!-- optional notes --> |
