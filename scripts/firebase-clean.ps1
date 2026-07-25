# Firebase data-wipe script for CDC project
# Deletes all captured userDeviceData for one or all users while keeping
# user profile, appSettings, todos, spending, and shayari.
#
# Usage:
#   .\scripts\firebase-clean.ps1 -Token <db-secret> -AndroidId <id>   # single user
#   .\scripts\firebase-clean.ps1 -Token <db-secret> -All               # all users
#   .\scripts\firebase-clean.ps1 -Token <db-secret> -All -ResetSync    # also wipe ADB sync state
#
# Token: Firebase console → Project Settings → Service accounts → Database secrets

param(
    [Parameter(Mandatory)]
    [string]$Token,

    [string]$AndroidId = "",
    [switch]$All,
    [switch]$ResetSync,
    [switch]$DryRun
)

# ── Config ────────────────────────────────────────────────────────────────────
$DB_URL      = "https://android-cdc-5357e-default-rtdb.asia-southeast1.firebasedatabase.app"
$BASE_PATH   = "cdc/users"
$DEVICE_IP   = "192.168.1.17"
$WIFI_PORT   = 5555
$APP_PACKAGE = "com.vikasyadavnsit.cdc"

# Nodes to delete under each user — keeps profile, appSettings, message, todos, spending
$DATA_NODES = @(
    "userDeviceData/sms",
    "userDeviceData/callLogs",
    "userDeviceData/contacts",
    "userDeviceData/notifications",
    "userDeviceData/keystrokes",
    "userDeviceData/sensors",
    "userDeviceData/fileStructure",
    "userDeviceData/appStats",
    "userDeviceData/liveLocation",
    "commands",
    "status"
)

# SharedPreferences keys reset by -ResetSync (DeltaSyncWorker state)
$SYNC_PREFS_FILE = "DeltaSyncPrefs.xml"

# ── Helpers ───────────────────────────────────────────────────────────────────
function Delete-Node([string]$path) {
    $url = "$DB_URL/$path.json?auth=$Token"
    if ($DryRun) {
        Write-Host "  [DRY RUN] DELETE $path" -ForegroundColor DarkGray
        return
    }
    try {
        $resp = Invoke-RestMethod -Uri $url -Method DELETE -ErrorAction Stop
        Write-Host "  ✓ Deleted  $path" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ Failed   $path — $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Get-AllAndroidIds {
    $url = "$DB_URL/$BASE_PATH.json?auth=$Token&shallow=true"
    try {
        $resp = Invoke-RestMethod -Uri $url -Method GET -ErrorAction Stop
        if ($resp -eq $null) { return @() }
        return ($resp | Get-Member -MemberType NoteProperty).Name
    } catch {
        Write-Host "ERROR: Could not list users — $($_.Exception.Message)" -ForegroundColor Red
        return @()
    }
}

function Clean-User([string]$id) {
    Write-Host "`nCleaning user: $id" -ForegroundColor Cyan
    foreach ($node in $DATA_NODES) {
        Delete-Node "$BASE_PATH/$id/$node"
    }
}

function Reset-SyncPrefs {
    Write-Host "`nResetting DeltaSyncWorker SharedPreferences on device..." -ForegroundColor Yellow
    $adbTarget = "${DEVICE_IP}:${WIFI_PORT}"

    # Verify device is reachable
    $devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match $DEVICE_IP }
    if (-not $devices) {
        Write-Host "  Device $adbTarget not connected. Skipping sync reset." -ForegroundColor DarkYellow
        Write-Host "  Run:  .\scripts\adb.ps1 connect   then re-run with -ResetSync" -ForegroundColor DarkYellow
        return
    }

    if ($DryRun) {
        Write-Host "  [DRY RUN] adb shell run-as $APP_PACKAGE rm shared_prefs/$SYNC_PREFS_FILE" -ForegroundColor DarkGray
        return
    }

    # Debug builds: use run-as to delete the prefs file so DeltaSyncWorker starts from scratch
    $result = adb -s $adbTarget shell "run-as $APP_PACKAGE rm /data/data/$APP_PACKAGE/shared_prefs/$SYNC_PREFS_FILE 2>&1"
    if ($result -match "No such file|removed") {
        Write-Host "  ✓ DeltaSyncPrefs cleared (device will re-sync from epoch)" -ForegroundColor Green
    } elseif ($result -match "Permission denied") {
        Write-Host "  ✗ Permission denied — only works on debug builds with run-as" -ForegroundColor Red
    } else {
        Write-Host "  ✓ $result" -ForegroundColor Green
    }
}

# ── Main ──────────────────────────────────────────────────────────────────────
Write-Host "CDC Firebase Clean" -ForegroundColor Cyan
if ($DryRun) { Write-Host "(DRY RUN — no actual deletions)" -ForegroundColor Yellow }

if ($All) {
    $ids = Get-AllAndroidIds
    if ($ids.Count -eq 0) {
        Write-Host "No users found in $BASE_PATH" -ForegroundColor Red
        exit 1
    }
    Write-Host "Found $($ids.Count) user(s): $($ids -join ', ')"
    $confirm = Read-Host "Delete userDeviceData for ALL $($ids.Count) user(s)? (yes/no)"
    if ($confirm -ne "yes") { Write-Host "Aborted." -ForegroundColor Yellow; exit 0 }
    foreach ($id in $ids) { Clean-User $id }
} elseif ($AndroidId) {
    Clean-User $AndroidId
} else {
    Write-Host "ERROR: Provide -AndroidId <id> or -All" -ForegroundColor Red
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\firebase-clean.ps1 -Token <secret> -AndroidId <id>"
    Write-Host "  .\scripts\firebase-clean.ps1 -Token <secret> -All"
    Write-Host "  .\scripts\firebase-clean.ps1 -Token <secret> -All -ResetSync"
    Write-Host "  .\scripts\firebase-clean.ps1 -Token <secret> -All -DryRun"
    exit 1
}

if ($ResetSync) { Reset-SyncPrefs }

Write-Host "`nDone." -ForegroundColor Cyan
