# Change PC Guide

Move the Brujula database to a new machine — or recover from a factory reset or disk failure.
This runbook covers everything from tool installation through data validation.

---

## Overview

The migration has two phases: **prepare on the old machine** (backup + key export) and
**set up on the new machine** (install, clone, configure, migrate, restore, validate).

```
Old machine                       New machine
───────────────────────────────   ───────────────────────────────────────────────
1. Create encrypted backup        4. Install required tools
2. Export private key (secure)    5. Clone repository
                                  6. Copy private key (secure channel)
                                  7. Create empty database + configure .env
                                  8. Start app once → Flyway builds schema
                                  9. Run restore.sh
                                  10. Validate
```

---

## Phase 1 — Prepare on the Old Machine

### Step 1: Create a final encrypted backup

```bash
# Linux / macOS
./scripts/db/backup.sh before-pc-migration
```

```powershell
# Windows PowerShell
pwsh .\scripts\db\backup.ps1 before-pc-migration
```

Confirm the archive was created:

```bash
# Linux / macOS
ls -lh backups/brujula-data-*-before-pc-migration.sql.age
```

```powershell
# Windows PowerShell
Get-ChildItem backups\*before-pc-migration.sql.age
```

Commit the archive to the repository so it is available on the new machine:

```bash
git add backups/
git commit -m "chore(db): final backup before PC migration $(date +%F)"
git push
```

### Step 2: Export the private key (out-of-band)

The private key must be transferred **without going through the repository**. Use one of:

- A password manager (recommended — Bitwarden, 1Password, KeePass)
- An encrypted USB drive
- An end-to-end encrypted message (Signal, Keybase)
- Secure file transfer tool (rsync over SSH, sftp)

**Never** email the key, share it via the repository, or copy it to any unencrypted location.

The file is at (by default) `~/Desktop/brujula-key.txt` on Linux/macOS or
`C:\Users\<user>\Desktop\brujula-key.txt` on Windows.

---

## Phase 2 — Set Up on the New Machine

### Step 3: Install required tools

Install all of the following before proceeding.

#### Git

```bash
# Linux (Debian/Ubuntu)
sudo apt install git

# macOS
brew install git
```

```powershell
# Windows — download from https://git-scm.com/download/win
# or with winget:
winget install Git.Git
```

#### Java 25 (Temurin distribution recommended)

Download from [adoptium.net](https://adoptium.net) and install Temurin 25 (LTS).

```bash
# Linux — with SDKMAN
sdk install java 25-tem

# macOS — with mise
mise use java@temurin-25
```

```powershell
# Windows — with winget
winget install EclipseAdoptium.Temurin.25.JDK
```

#### PostgreSQL 18.4

Download from [postgresql.org/download](https://www.postgresql.org/download/) and install the
full package (includes `psql` and `pg_dump`). Match the major version (18) to the production
server.

#### age encryption tool

```bash
# Linux
sudo apt install age
# or download binary: https://github.com/FiloSottile/age/releases

# macOS
brew install age
```

```powershell
# Windows — download age.exe from https://github.com/FiloSottile/age/releases
# Place it in a directory on your PATH (e.g. C:\tools\age\)
winget install FiloSottile.age
```

Verify installation:

```bash
age --version   # should print: age v1.x.x
pg_dump --version
psql --version
java -version
```

### Step 4: Clone the repository

```bash
# Linux / macOS
git clone <repository-url>
cd brujula-backend
```

```powershell
# Windows PowerShell
git clone <repository-url>
Set-Location brujula-backend
```

The `backups/` directory — including the archive you committed in Step 1 — will be present
after cloning.

### Step 5: Place the private key

Place the `brujula-key.txt` file you transferred in Step 2 **outside** the repository directory:

```bash
# Linux / macOS — place on Desktop
cp /path/to/transferred/brujula-key.txt ~/Desktop/brujula-key.txt
```

```powershell
# Windows PowerShell
Copy-Item "C:\path\to\transferred\brujula-key.txt" "$env:USERPROFILE\Desktop\brujula-key.txt"
```

> **Security reminder**: Once placed, the transferred copy in the temporary location should be
> securely deleted.

### Step 6: Generate a keypair if needed (new installation)

If this is a brand-new installation (not a migration from an existing machine) and you do not yet
have a keypair, generate one now:

```bash
# Linux / macOS
age-keygen -o ~/Desktop/brujula-key.txt
```

```powershell
# Windows PowerShell
age-keygen -o "$env:USERPROFILE\Desktop\brujula-key.txt"
```

See [backup-guide.md](backup-guide.md) for the full key-generation procedure.

### Step 7: Create an empty database

```bash
# Linux / macOS (run as PostgreSQL superuser)
createdb -U postgres planner_db
createuser -U postgres --pwprompt brujula
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE planner_db TO brujula;"
```

```powershell
# Windows PowerShell (run as PostgreSQL superuser)
createdb -U postgres planner_db
createuser -U postgres --pwprompt brujula
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE planner_db TO brujula;"
```

### Step 8: Configure `.env`

```bash
# Linux / macOS
cp .env.example .env
```

```powershell
# Windows PowerShell
Copy-Item .env.example .env
```

Edit `.env` and fill in:

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=planner_db
DB_USER=brujula
DB_PASSWORD=<password you set in Step 7>

# Paste the public key from ~/Desktop/brujula-key.txt (the "# public key: age1..." line):
AGE_PUBLIC_KEY=age1abc123...

# Path to the private key file you placed in Step 5:
AGE_PRIVATE_KEY_PATH=~/Desktop/brujula-key.txt          # Linux/macOS
# AGE_PRIVATE_KEY_PATH=C:\Users\<user>\Desktop\brujula-key.txt  # Windows
```

You also need to configure the Keycloak and other application settings in
`src/main/resources/application-local.yaml` (or via the other `.env` vars in `.env.example`).

### Step 9: Run the application once to build the schema

Flyway will automatically apply all migrations and build the complete schema on first startup.

```bash
# Linux / macOS
JAVA_HOME=/path/to/java ./gradlew bootRun --args='--spring.profiles.active=local'
```

```powershell
# Windows PowerShell
$env:JAVA_HOME = "C:\path\to\java"
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Wait for the log line `Started BrujulaApplication` (or equivalent). The schema is now built.
**Stop the application** (Ctrl+C / Ctrl+Break) before restoring data.

> If the application fails to start, check the Flyway migration log. All migrations V001–V025
> (and beyond) should apply or validate without error.

### Step 10: Restore the encrypted backup

```bash
# Linux / macOS
./scripts/db/restore.sh backups/brujula-data-YYYY-MM-DD-before-pc-migration.sql.age
```

```powershell
# Windows PowerShell
pwsh .\scripts\db\restore.ps1 backups\brujula-data-YYYY-MM-DD-before-pc-migration.sql.age
```

Use the archive filename from Step 1 (the one committed in the repository).

### Step 11: Validate

1. Start the application again:

   ```bash
   # Linux / macOS
   JAVA_HOME=/path/to/java ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

2. Log in and verify your data is present (student records, enrollments, compass entries, etc.).
3. Confirm no plaintext SQL file was left behind:

   ```bash
   # Linux / macOS
   find . -name "*.sql" -not -path "*/db/migration/*" 2>/dev/null
   ```

   ```powershell
   # Windows PowerShell
   Get-ChildItem -Recurse -Filter "*.sql" | Where-Object { $_.FullName -notlike "*db\migration*" }
   ```

   No output expected.

---

## Rollback

If the restore fails or data looks wrong:

1. Stop the application.
2. Drop and recreate the database:

   ```bash
   dropdb -U postgres planner_db && createdb -U postgres planner_db
   psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE planner_db TO brujula;"
   ```

3. Start the application again to let Flyway rebuild the schema.
4. Re-run the restore script with the correct archive.

---

## Checklist

**Old machine:**
- [ ] Final backup created with motivo `before-pc-migration`
- [ ] Archive committed and pushed to the repository
- [ ] Private key exported via a secure out-of-band channel

**New machine:**
- [ ] Git, Java 25, PostgreSQL 18.4, age installed and verified
- [ ] Repository cloned
- [ ] Private key placed outside the repository directory
- [ ] Empty database created and user granted privileges
- [ ] `.env` configured with DB vars, `AGE_PUBLIC_KEY`, `AGE_PRIVATE_KEY_PATH`
- [ ] Application started once — Flyway applied all migrations without errors
- [ ] Application stopped before restore
- [ ] Restore script ran and exited with code `0`
- [ ] Application started again — data is present and correct
- [ ] No plaintext `.sql` file found outside migration directory

---

## Next Step

To learn how the local-first strategy works in detail, see [local-first-strategy.md](local-first-strategy.md).
For day-to-day backup operations, see [backup-guide.md](backup-guide.md).
