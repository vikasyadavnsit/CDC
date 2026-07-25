# Firebase App-Update Helper -- CDC project
#
# Commands:
#   .\scripts\firebase_update.ps1 init          -- write v1 baseline to RTDB (run once after first install)
#   .\scripts\firebase_update.ps1 check         -- read current update config from RTDB
#
#   # APK hosted on Firebase Storage (requires Firebase CLI):
#   .\scripts\firebase_update.ps1 release -ApkPath .\app\release\app-release.apk
#
#   # APK hosted on an external URL (GitHub Releases, CDN, own server, etc.):
#   .\scripts\firebase_update.ps1 release -ApkPath .\app\release\app-release.apk `
#                                         -ApkUrl https://github.com/user/repo/releases/download/v2/cdc-v2.apk
#
# When -ApkUrl is supplied the APK upload step is skipped and that URL is written
# directly to RTDB as the apkStoragePath.  Firebase Storage is not used.
#
# Prerequisites for 'release' without -ApkUrl:
#   Firebase CLI installed (npm install -g firebase-tools) and logged in (firebase login)
#
# RTDB path   : cdc/updates/

param(
    [Parameter(Position = 0)]
    [ValidateSet('init', 'check', 'release')]
    [string]$Command = 'check',

    [string]$ApkPath = '',

    # Optional: any HTTPS URL pointing to the APK (bypasses Firebase Storage upload)
    [string]$ApkUrl = ''
)

# ── Config ────────────────────────────────────────────────────────────────────
$DB_URL       = 'https://android-cdc-5357e-default-rtdb.asia-southeast1.firebasedatabase.app'
$RTDB_PATH    = '/cdc/updates.json'
$STORAGE_BUCKET = 'gs://android-cdc-5357e.appspot.com'
$STORAGE_FOLDER = 'updates'
$GRADLE_FILE  = "$PSScriptRoot\..\app\build.gradle"

# ── Helpers ───────────────────────────────────────────────────────────────────

function Get-VersionCode {
    $content = Get-Content $GRADLE_FILE -Raw
    if ($content -match 'versionCode\s+(\d+)') { return [int]$Matches[1] }
    throw 'Cannot parse versionCode from build.gradle'
}

function Get-VersionName {
    $content = Get-Content $GRADLE_FILE -Raw
    if ($content -match 'versionName\s+"([^"]+)"') { return $Matches[1] }
    throw 'Cannot parse versionName from build.gradle'
}

function Set-Version {
    param([int]$NewCode, [string]$NewName)
    $content = Get-Content $GRADLE_FILE -Raw
    $content = $content -replace 'versionCode\s+\d+', ('versionCode ' + $NewCode)
    $newNameLine = 'versionName "' + $NewName + '"'
    $content = $content -replace 'versionName\s+"[^"]+"', $newNameLine
    Set-Content $GRADLE_FILE $content -Encoding utf8
    Write-Host "build.gradle updated: versionCode=$NewCode  versionName=$NewName"
}

function Write-RtdbConfig {
    param([int]$VersionCode, [string]$ApkStoragePath)
    $body = '{"latestVersionCode":' + $VersionCode + ',"apkStoragePath":"' + $ApkStoragePath + '"}'
    $resp = Invoke-RestMethod -Method Put -Uri ($DB_URL + $RTDB_PATH) -Body $body -ContentType 'application/json'
    Write-Host ('RTDB set: ' + ($resp | ConvertTo-Json -Compress))
}

function Read-RtdbConfig {
    return Invoke-RestMethod -Method Get -Uri ($DB_URL + $RTDB_PATH)
}

# ── Commands ──────────────────────────────────────────────────────────────────

switch ($Command) {

    'init' {
        $vc = Get-VersionCode
        $vn = Get-VersionName
        Write-Host "Initialising baseline: versionCode=$vc  versionName=$vn"
        Write-RtdbConfig -VersionCode $vc -ApkStoragePath ''
        Write-Host ''
        Write-Host "Baseline written. All devices on v$vc see no pending update."
        Write-Host "To ship the next release:"
        Write-Host "  1. Build a release APK in Android Studio"
        Write-Host "  2. Run: .\scripts\firebase_update.ps1 release -ApkPath <path-to-apk>"
    }

    'check' {
        $cfg = Read-RtdbConfig
        if ($null -eq $cfg) {
            Write-Host "cdc/updates is empty -- run 'init' first."
        } else {
            $local = Get-VersionCode
            Write-Host ('Firebase RTDB  : versionCode=' + $cfg.latestVersionCode + '  apkStoragePath=' + $cfg.apkStoragePath)
            Write-Host "build.gradle   : versionCode=$local"
            if ($local -lt $cfg.latestVersionCode) {
                Write-Host 'WARNING: local build.gradle is behind Firebase -- build and install the release APK.'
            } elseif ($local -gt $cfg.latestVersionCode) {
                Write-Host 'NOTE: local build.gradle is ahead of Firebase -- run "init" to publish the baseline.'
            } else {
                Write-Host 'OK: local and Firebase are in sync.'
            }
        }
    }

    'release' {
        if (-not $ApkPath -or -not (Test-Path $ApkPath)) {
            Write-Error 'Provide a valid -ApkPath. Build a release APK in Android Studio first.'
            exit 1
        }

        $currentCode = Get-VersionCode
        $newCode     = $currentCode + 1
        $parts       = (Get-VersionName) -split '\.'
        $minor       = [int]$parts[1] + 1
        $newName     = $parts[0] + '.' + $minor + '.0'

        Write-Host "Releasing: v$currentCode --> v$newCode ($newName)"

        # 1. Bump version in build.gradle
        Set-Version -NewCode $newCode -NewName $newName

        # 2a. External URL supplied — skip Firebase Storage upload entirely
        if ($ApkUrl -ne '') {
            if (-not ($ApkUrl -match '^https?://')) {
                Write-Error '-ApkUrl must start with http:// or https://'
                Set-Version -NewCode $currentCode -NewName (Get-VersionName)
                exit 1
            }
            $apkSource = $ApkUrl
            Write-Host "Using external URL: $apkSource (skipping Firebase Storage upload)"
        } else {
            # 2b. Upload APK to Firebase Storage via CLI
            $storageDest = $STORAGE_BUCKET + '/' + $STORAGE_FOLDER + '/cdc-v' + $newCode + '.apk'
            Write-Host "Uploading $ApkPath --> $storageDest ..."
            firebase storage:cp $ApkPath $storageDest
            if ($LASTEXITCODE -ne 0) {
                Write-Error 'Firebase Storage upload failed. Reverting build.gradle.'
                Set-Version -NewCode $currentCode -NewName (Get-VersionName)
                exit 1
            }
            $apkSource = $STORAGE_FOLDER + '/cdc-v' + $newCode + '.apk'
        }

        # 3. Write to RTDB -- this triggers the update on all target devices
        Write-RtdbConfig -VersionCode $newCode -ApkStoragePath $apkSource

        Write-Host ''
        Write-Host "Release v$newCode published (source: $apkSource)"
        Write-Host "All devices on v$currentCode will now auto-update."
    }
}
