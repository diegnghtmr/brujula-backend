# Restore Guide

Decrypt a `.sql.age` backup archive and load it into the Brujula database.

> **PREREQUISITE — READ BEFORE RESTORING**
>
> Flyway must have already built the database schema before you restore data.
> Start the application once (or run `./gradlew flywayMigrate`) against an empty database
> to let Flyway create all tables, indexes, and constraints. Only then run this restore script.
>
> **Do NOT restore into an empty (schema-less) database** — `psql` will fail on the first
> `INSERT` because the target tables do not exist.
>
> **Do NOT restore into a database that already contains live data** — the `INSERT` statements
> in the backup will conflict with existing rows and the restore will abort mid-way, leaving
> the database in a partially-restored state.

---

## Prerequisites

| Tool | Linux / macOS | Windows |
|------|---------------|---------|
| age | `brew install age` or package manager | Download from [github.com/FiloSottile/age/releases](https://github.com/FiloSottile/age/releases) and add to PATH |
| psql | Ships with PostgreSQL 18.4 client | Included in the PostgreSQL 18.4 installer |

You also need:

- `.env` configured with the database connection vars and `AGE_PRIVATE_KEY_PATH` pointing to your
  private key file (outside the repository directory).
- The `.sql.age` archive file to restore (in `backups/` or provided by another developer).

---

## Quick Path — Restore a Backup

```bash
# Linux / macOS
./scripts/db/restore.sh backups/brujula-data-YYYY-MM-DD-<motivo>.sql.age
```

```powershell
# Windows PowerShell
pwsh .\scripts\db\restore.ps1 backups\brujula-data-YYYY-MM-DD-<motivo>.sql.age
```

Replace the filename with the actual archive you want to restore.

**Full example:**

```bash
# Linux / macOS
./scripts/db/restore.sh backups/brujula-data-2026-06-17-before-migration.sql.age
```

```powershell
# Windows PowerShell
pwsh .\scripts\db\restore.ps1 backups\brujula-data-2026-06-17-before-migration.sql.age
```

---

## What Happens

1. The script loads `.env` from the project root (or the path in `BRUJULA_ENV_FILE`).
2. Preflight: checks that `age` and `psql` are on `PATH`, that all required vars are non-empty,
   and that `AGE_PRIVATE_KEY_PATH` points to an existing file. Exits with code `3` if anything
   is missing.
3. Validates the input file argument: exits with code `2` if no path is provided or the file is
   unreadable.
4. Creates a temporary plaintext SQL file (`mktemp`) with a `trap EXIT` deletion guarantee.
5. Decrypts the archive: `age -d -i $AGE_PRIVATE_KEY_PATH -o $TMP <archive>`.
6. Loads the decrypted SQL: `psql -v ON_ERROR_STOP=1 -f $TMP`.
7. The trap deletes the plaintext temp file — regardless of success or failure.

No plaintext SQL file is ever left on disk.

---

## Validation Steps After Restore

After the script completes, verify the restore succeeded:

### 1. Start the application

```bash
# Linux / macOS
JAVA_HOME=/path/to/java ./gradlew bootRun --args='--spring.profiles.active=local'
```

```powershell
# Windows PowerShell
$env:JAVA_HOME = "C:\path\to\java"
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Flyway will run in validate mode (`ddl-auto: validate`). If it passes, the schema is consistent.

### 2. Confirm your data is present

Log in to the application and spot-check the records that were present before the backup.

### 3. Confirm no plaintext file was left behind

```bash
# Linux / macOS
find . -name "*.sql" -not -path "*/db/migration/*" 2>/dev/null
```

```powershell
# Windows PowerShell
Get-ChildItem -Recurse -Filter "*.sql" | Where-Object { $_.FullName -notlike "*db\migration*" }
```

The command should produce no output. Any `.sql` file found outside the migrations directory is
a leftover that must be deleted and investigated.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `age: error: no identity matched any of the file's recipients` | Wrong private key | Use the key that corresponds to the `AGE_PUBLIC_KEY` used when the backup was created |
| `psql: error: ... relation "..." does not exist` | Schema not built | Run the app (or `./gradlew flywayMigrate`) against the target DB first, then re-run restore |
| `psql: error: ERROR: duplicate key value` | Database already contains data | Drop all data (`TRUNCATE ... CASCADE`) or restore into a fresh empty DB with schema |
| `ERROR: 'age' is not on PATH` | age not installed or not on PATH | Install age and confirm `which age` / `where age` resolves |
| `ERROR: Required variable 'AGE_PRIVATE_KEY_PATH' is missing` | `.env` not configured | Copy `.env.example` to `.env` and fill in `AGE_PRIVATE_KEY_PATH` |
| `ERROR: Key file not found at '...'` | Private key file missing | Place `brujula-key.txt` at the path set in `AGE_PRIVATE_KEY_PATH` |
| Script exits with code `1` (psql non-zero mid-import) | Partial failure — `ON_ERROR_STOP=1` halted on error | Check psql stderr output, fix the schema issue, and re-run from scratch |

### Restoring on a different machine

If you are restoring from a backup created on another machine:

1. Transfer the `.sql.age` file (commit it, or copy it manually — it is safe to share).
2. Transfer the **private key** out-of-band (encrypted message, USB drive, password manager).
   Never email the private key or share it via the repository.
3. Follow the full [change-pc-guide.md](change-pc-guide.md) instead of this guide.

---

## Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Success — data loaded |
| `1` | Runtime failure (`age` or `psql` exited non-zero) |
| `2` | Bad arguments — input file missing or unreadable |
| `3` | Preflight failure — missing tool or required `.env` variable |

---

## Checklist

- [ ] Flyway schema has been built on the target database (app started once, or `flywayMigrate` ran)
- [ ] Target database contains no live data that should be preserved (or intentionally accepted as overwrite risk)
- [ ] `.env` is configured with correct connection vars and `AGE_PRIVATE_KEY_PATH`
- [ ] Private key file exists at the path set in `AGE_PRIVATE_KEY_PATH`
- [ ] `age` and `psql` are installed and on `PATH`
- [ ] Restore script exited with code `0`
- [ ] Application starts and data is visible
- [ ] No plaintext `.sql` file left outside `src/main/resources/db/migration/`

---

## Next Step

If this restore is part of a machine migration, see [change-pc-guide.md](change-pc-guide.md).
