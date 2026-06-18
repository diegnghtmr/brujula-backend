# Backup Guide

Create an encrypted backup of the Brujula database. The output is an age-encrypted
`.sql.age` archive in `backups/` — safe to commit and meaningless without the private key.

---

## Prerequisites

Before your first backup, complete these one-time setup steps.

### 1. Install required tools

| Tool | Linux / macOS | Windows |
|------|---------------|---------|
| age | `brew install age` or package manager | Download from [github.com/FiloSottile/age/releases](https://github.com/FiloSottile/age/releases) and add to PATH |
| pg_dump | Ships with PostgreSQL 18.4 client | Included in the PostgreSQL 18.4 installer |

### 2. Generate an age keypair (one-time, per machine)

Generate the keypair **outside** the repository directory. The private key must never be placed
inside the project folder — `.gitignore` blocks `brujula-key.txt` and `*.key`, but keeping it
outside is the primary safeguard.

```bash
# Linux / macOS — saves keypair to Desktop
age-keygen -o ~/Desktop/brujula-key.txt
```

```powershell
# Windows PowerShell — saves keypair to Desktop
age-keygen -o "$env:USERPROFILE\Desktop\brujula-key.txt"
```

The file contains both keys:

```
# created: 2026-06-17T10:00:00-05:00
# public key: age1abc123...
AGE-SECRET-KEY-1...
```

> **WARNING**: The private key (`AGE-SECRET-KEY-1...` line) is the only way to decrypt your
> backups. Store it somewhere safe and permanent — an encrypted password manager, a USB drive
> in a secure location, or cloud storage with strong authentication. If you lose it, your
> backups are permanently unrecoverable.

> **NEVER** place `brujula-key.txt` inside the repository directory or any subdirectory of it.

### 3. Configure `.env`

Copy `.env.example` to `.env` (if you have not already) and fill in the values:

```bash
# Linux / macOS
cp .env.example .env
```

```powershell
# Windows PowerShell
Copy-Item .env.example .env
```

Edit `.env` and set:

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=planner_db
DB_USER=brujula
DB_PASSWORD=<your-local-db-password>

# Paste the "# public key: age1..." value from brujula-key.txt:
AGE_PUBLIC_KEY=age1abc123...

# Full path to the private key file — OUTSIDE the repository:
AGE_PRIVATE_KEY_PATH=~/Desktop/brujula-key.txt          # Linux/macOS
# AGE_PRIVATE_KEY_PATH=C:\Users\<user>\Desktop\brujula-key.txt  # Windows
```

---

## Quick Path — Create a Backup

```bash
# Linux / macOS
./scripts/db/backup.sh <motivo>
```

```powershell
# Windows PowerShell
pwsh .\scripts\db\backup.ps1 <motivo>
```

Replace `<motivo>` with a short slug describing why you are backing up. Allowed characters:
`[a-zA-Z0-9_-]`. If omitted, defaults to `manual`.

**Examples:**

```bash
# Linux / macOS
./scripts/db/backup.sh before-migration
./scripts/db/backup.sh after-enrollment-session
./scripts/db/backup.sh manual
```

```powershell
# Windows PowerShell
pwsh .\scripts\db\backup.ps1 before-migration
pwsh .\scripts\db\backup.ps1 after-enrollment-session
pwsh .\scripts\db\backup.ps1 manual
```

---

## What Happens

1. The script loads `.env` from the project root (or the path in `BRUJULA_ENV_FILE`).
2. Preflight: checks that `age` and `pg_dump` are on `PATH` and that all required `.env` vars
   are non-empty. Exits with code `3` and a descriptive message if anything is missing.
3. Creates a temporary plaintext SQL file (`mktemp`) with a `trap EXIT` deletion guarantee.
4. Runs `pg_dump --data-only --column-inserts` excluding the three transient tables.
5. Encrypts the dump with `age -r $AGE_PUBLIC_KEY` and writes the archive to `backups/`.
6. The trap deletes the plaintext temp file — regardless of success or failure.

**Output filename:**

```
backups/brujula-data-YYYY-MM-DD-<motivo>.sql.age
```

---

## Verify the Backup

```bash
# Linux / macOS — confirm the file was created and is non-empty
ls -lh backups/*.sql.age
```

```powershell
# Windows PowerShell
Get-ChildItem backups\*.sql.age | Select-Object Name, Length
```

The file should be binary (age ciphertext). If you open it in a text editor it will look like
random bytes — that is correct.

To verify the archive is decryptable without restoring to the DB, run:

```bash
# Linux / macOS
age -d -i ~/Desktop/brujula-key.txt backups/<filename>.sql.age | head -20
```

```powershell
# Windows PowerShell
age -d -i "$env:USERPROFILE\Desktop\brujula-key.txt" backups\<filename>.sql.age | Select-Object -First 20
```

You should see SQL `INSERT` statements at the top.

---

## When to Back Up (RF-07 Policy)

| Situation | Frequency |
|-----------|-----------|
| Before every schema migration | Mandatory — run backup.sh before starting the app with new Flyway files |
| Before changing PC / formatting / cloud migration | Mandatory — see [change-pc-guide.md](change-pc-guide.md) |
| During heavy data-entry sessions | Daily |
| Light / no data-entry weeks | Weekly at minimum |
| Before any destructive operation | Always |

---

## What Gets Excluded and Why

| Table | Excluded because |
|-------|------------------|
| `platform.spring_session` | Session tokens are invalid after restore; users re-authenticate |
| `platform.spring_session_attributes` | Tied to transient session rows |
| `platform.idempotency_record` | Deduplication fingerprints; restored data is already idempotent |

See [local-first-strategy.md](local-first-strategy.md) for the full classification.

---

## Update the Backup Log

After each backup, add a row to `backups/README.md`:

```
| 2026-06-17 | before-migration | brujula-data-2026-06-17-before-migration.sql.age | V026 adds indexes |
```

Then commit the archive and the updated log together:

```bash
# Linux / macOS
git add backups/
git commit -m "chore(db): add encrypted backup before-migration 2026-06-17"
```

```powershell
# Windows PowerShell
git add backups/
git commit -m "chore(db): add encrypted backup before-migration 2026-06-17"
```

---

## Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Success — backup created |
| `1` | Runtime failure (`pg_dump` or `age` exited non-zero) |
| `2` | Bad arguments or usage error |
| `3` | Preflight failure — missing tool or required `.env` variable |

---

## Checklist

- [ ] `age` and `pg_dump` are installed and on `PATH`
- [ ] Keypair generated at `~/Desktop/brujula-key.txt` (or equivalent outside-repo path)
- [ ] `.env` contains `AGE_PUBLIC_KEY` and `AGE_PRIVATE_KEY_PATH`
- [ ] Backup archive appears in `backups/` and is non-empty
- [ ] No plaintext `.sql` file left in `backups/` or `/tmp`
- [ ] `backups/README.md` updated with a new log row
- [ ] Archive committed to the repository

---

## Next Step

To restore a backup, see [restore-guide.md](restore-guide.md).
