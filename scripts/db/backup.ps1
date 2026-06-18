# =============================================================================
# scripts/db/backup.ps1
#
# Creates an encrypted pg_dump backup of the Brujula database (Windows/PowerShell).
# Functionally equivalent to scripts/db/backup.sh.
#
# Usage:
#   pwsh ./scripts/db/backup.ps1 <motivo>
#
# Arguments:
#   motivo  — A short slug identifying the backup reason (e.g. "before-migration").
#              Allowed characters: [a-zA-Z0-9_-]. Others are stripped.
#              Defaults to "manual" if omitted.
#
# Requires:
#   - .env file with DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, AGE_PUBLIC_KEY.
#   - age on PATH (https://github.com/FiloSottile/age)
#   - pg_dump (PostgreSQL 18.4+) on PATH
#
# Output:
#   backups\brujula-data-YYYY-MM-DD-<motivo>.sql.age
#
# Exit codes:
#   0  — success
#   1  — runtime failure (pg_dump or age returned non-zero)
#   2  — bad arguments / usage error
#   3  — preflight failure (missing tool or required env var)
# =============================================================================

[CmdletBinding()]
param(
    [string]$Motivo = "manual"
)

$ErrorActionPreference = 'Stop'

# ─── Locate repo root ──────────────────────────────────────────────────────────
# Allow REPO_ROOT to be overridden by the test harness via environment variable.
if (-not $env:REPO_ROOT) {
    try {
        $gitRoot = & git rev-parse --show-toplevel 2>$null
        if ($LASTEXITCODE -eq 0 -and $gitRoot) {
            $env:REPO_ROOT = $gitRoot.Trim()
        }
    } catch { }
    if (-not $env:REPO_ROOT) {
        # Fallback: two levels up from scripts/db/
        $env:REPO_ROOT = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
    }
}
$repoRoot = $env:REPO_ROOT

# ─── Load .env ────────────────────────────────────────────────────────────────
$envFilePath = if ($env:BRUJULA_ENV_FILE) { $env:BRUJULA_ENV_FILE } else { Join-Path $repoRoot ".env" }

if (-not (Test-Path $envFilePath)) {
    Write-Error "ERROR: .env file not found at '$envFilePath'.`n       Copy .env.example to .env and fill in the values."
    exit 3
}

# Parse .env line by line (PowerShell does not source bash files natively)
Get-Content $envFilePath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $parts = $line.Split('=', 2)
        if ($parts.Length -eq 2) {
            $varName  = $parts[0].Trim()
            $varValue = $parts[1].Trim()
            Set-Item -Path "Env:$varName" -Value $varValue
        }
    }
}

# ─── Preflight checks ─────────────────────────────────────────────────────────
$preflightError = $false

if (-not (Get-Command age -ErrorAction SilentlyContinue)) {
    Write-Error "ERROR: 'age' is not on PATH. Install from https://github.com/FiloSottile/age"
    $preflightError = $true
}

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    Write-Error "ERROR: 'pg_dump' is not on PATH. Install PostgreSQL client tools."
    $preflightError = $true
}

function Assert-EnvVar {
    param([string]$Name)
    $value = [System.Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrEmpty($value)) {
        Write-Error "ERROR: Required variable '$Name' is missing or empty in .env."
        $script:preflightError = $true
    }
}

Assert-EnvVar 'DB_HOST'
Assert-EnvVar 'DB_PORT'
Assert-EnvVar 'DB_NAME'
Assert-EnvVar 'DB_USER'
Assert-EnvVar 'DB_PASSWORD'
Assert-EnvVar 'AGE_PUBLIC_KEY'
# AGE_PRIVATE_KEY_PATH is intentionally NOT checked here: encryption uses only the public key.
# Validating the private key is restore.ps1's responsibility.

if ($preflightError) { exit 3 }

# ─── Motivo argument ──────────────────────────────────────────────────────────
$sanitizedMotivo = ($Motivo -replace '[^a-zA-Z0-9_-]', '')
if ([string]::IsNullOrEmpty($sanitizedMotivo)) { $sanitizedMotivo = "manual" }

# ─── Output path ──────────────────────────────────────────────────────────────
$ts         = (Get-Date -Format "yyyy-MM-dd")
$backupsDir = Join-Path $repoRoot "backups"
if (-not (Test-Path $backupsDir)) { New-Item -ItemType Directory -Path $backupsDir | Out-Null }
$outFile = Join-Path $backupsDir "brujula-data-$ts-$sanitizedMotivo.sql.age"

# ─── Temp file + cleanup guarantee ────────────────────────────────────────────
$tmpFile = [System.IO.Path]::GetTempFileName()

try {
    # ─── pg_dump ──────────────────────────────────────────────────────────────
    Write-Output "Running pg_dump for database '$env:DB_NAME' on $env:DB_HOST`:$env:DB_PORT ..."
    $env:PGPASSWORD = $env:DB_PASSWORD

    & pg_dump `
        -h $env:DB_HOST `
        -p $env:DB_PORT `
        -U $env:DB_USER `
        -d $env:DB_NAME `
        --data-only `
        --column-inserts `
        "--exclude-table=platform.spring_session" `
        "--exclude-table=platform.spring_session_attributes" `
        "--exclude-table=platform.idempotency_record" `
        -f $tmpFile

    if ($LASTEXITCODE -ne 0) {
        Write-Error "ERROR: pg_dump exited with code $LASTEXITCODE"
        exit 1
    }

    Write-Output "Encrypting backup to '$outFile' ..."

    # ─── Encrypt with age ─────────────────────────────────────────────────────
    & age -r $env:AGE_PUBLIC_KEY -o $outFile $tmpFile

    if ($LASTEXITCODE -ne 0) {
        Write-Error "ERROR: age exited with code $LASTEXITCODE"
        exit 1
    }

    Write-Output "Backup complete: $outFile"
} finally {
    # Guarantee plaintext deletion regardless of success or failure
    if (Test-Path $tmpFile) {
        Remove-Item $tmpFile -ErrorAction SilentlyContinue
    }
}
