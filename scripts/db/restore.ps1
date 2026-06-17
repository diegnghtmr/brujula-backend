# =============================================================================
# scripts/db/restore.ps1
#
# Decrypts a .sql.age backup and loads it into the Brujula database (Windows/PowerShell).
# Functionally equivalent to scripts/db/restore.sh.
#
# Usage:
#   pwsh ./scripts/db/restore.ps1 <path-to-backup.sql.age>
#
# Arguments:
#   <path-to-backup.sql.age>  — path to the encrypted backup produced by backup.ps1 or backup.sh.
#
# Prerequisites:
#   - Flyway schema must already be built (run the app once or ./gradlew flywayMigrate)
#     before restoring data. Do NOT restore into an empty (schema-less) database.
#   - .env file with DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, AGE_PRIVATE_KEY_PATH.
#   - age on PATH (https://github.com/FiloSottile/age)
#   - psql (PostgreSQL 18.4+) on PATH
#
# Exit codes:
#   0  — success
#   1  — runtime failure (age or psql returned non-zero)
#   2  — bad arguments (missing or unreadable input file)
#   3  — preflight failure (missing tool or required env var)
# =============================================================================

[CmdletBinding()]
param(
    [string]$InputFile = ""
)

$ErrorActionPreference = 'Stop'

# ─── Locate repo root ──────────────────────────────────────────────────────────
if (-not $env:REPO_ROOT) {
    try {
        $gitRoot = & git rev-parse --show-toplevel 2>$null
        if ($LASTEXITCODE -eq 0 -and $gitRoot) {
            $env:REPO_ROOT = $gitRoot.Trim()
        }
    } catch { }
    if (-not $env:REPO_ROOT) {
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

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    Write-Error "ERROR: 'psql' is not on PATH. Install PostgreSQL client tools."
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
Assert-EnvVar 'AGE_PRIVATE_KEY_PATH'

if ($preflightError) { exit 3 }

# ─── Validate private key file ────────────────────────────────────────────────
$privateKeyPath = $env:AGE_PRIVATE_KEY_PATH
if (-not (Test-Path $privateKeyPath)) {
    Write-Error "ERROR: Private key file not found at '$privateKeyPath'.`n       Set AGE_PRIVATE_KEY_PATH in .env to the path of your age private key."
    exit 3
}

# ─── Validate input argument ──────────────────────────────────────────────────
if ([string]::IsNullOrEmpty($InputFile)) {
    Write-Error "ERROR: No .sql.age file specified.`n       Usage: pwsh ./scripts/db/restore.ps1 <path-to-backup.sql.age>"
    exit 2
}

if (-not (Test-Path $InputFile)) {
    Write-Error "ERROR: Backup file not found or not readable: '$InputFile'"
    exit 2
}

# ─── Temp file + cleanup guarantee ────────────────────────────────────────────
$tmpFile = [System.IO.Path]::GetTempFileName()

try {
    # ─── Decrypt with age ─────────────────────────────────────────────────────
    Write-Output "Decrypting '$InputFile' ..."
    & age -d -i $privateKeyPath -o $tmpFile $InputFile

    if ($LASTEXITCODE -ne 0) {
        Write-Error "ERROR: age decryption exited with code $LASTEXITCODE"
        exit 1
    }

    # ─── Restore via psql ─────────────────────────────────────────────────────
    Write-Output "Restoring data to database '$env:DB_NAME' on $env:DB_HOST`:$env:DB_PORT ..."
    $env:PGPASSWORD = $env:DB_PASSWORD

    & psql `
        -h $env:DB_HOST `
        -p $env:DB_PORT `
        -U $env:DB_USER `
        -d $env:DB_NAME `
        -v ON_ERROR_STOP=1 `
        -f $tmpFile

    if ($LASTEXITCODE -ne 0) {
        Write-Error "ERROR: psql exited with code $LASTEXITCODE"
        exit 1
    }

    Write-Output "Restore complete."
} finally {
    # Guarantee plaintext deletion regardless of success or failure
    if (Test-Path $tmpFile) {
        Remove-Item $tmpFile -ErrorAction SilentlyContinue
    }
}
