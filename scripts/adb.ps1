# ADB helper script for CDC project
# Usage:
#   .\scripts\adb.ps1 connect       -- connect via WiFi (USB fallback)
#   .\scripts\adb.ps1 status        -- show connected devices
#   .\scripts\adb.ps1 disconnect    -- disconnect WiFi device
#   .\scripts\adb.ps1 reconnect     -- disconnect then reconnect
#   .\scripts\adb.ps1 logcat        -- stream logcat filtered to CDC package
#   .\scripts\adb.ps1 install       -- build & install debug APK
#   .\scripts\adb.ps1 firewall      -- add Windows firewall rule for ADB WiFi (auto-elevates if needed)

# --- Config ---
$DEVICE_IP   = "192.168.1.15"
$WIFI_PORT   = 5555
$DEVICE_ADDR = "${DEVICE_IP}:${WIFI_PORT}"
$APP_PACKAGE = "com.vikasyadavnsit.cdc"

# --- Helpers ---
function Get-UsbDevice {
    $devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" -and $_ -notmatch "^\d+\.\d+" }
    return $devices
}

function Get-WifiDevice {
    $devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "^$DEVICE_IP" }
    return $devices
}

function Connect-Wifi {
    Write-Host "Checking WiFi connection to $DEVICE_ADDR ..."

    $wifi = Get-WifiDevice
    if ($wifi) {
        Write-Host "Already connected: $DEVICE_ADDR" -ForegroundColor Green
        return
    }

    # Try direct connect first (device may already be in TCP mode)
    $result = adb connect $DEVICE_ADDR
    if ($result -match "connected") {
        Write-Host "Connected: $DEVICE_ADDR" -ForegroundColor Green
        return
    }

    # Fall back to USB -> tcpip -> connect
    Write-Host "Direct connect failed. Trying via USB..."
    $usb = Get-UsbDevice
    if (-not $usb) {
        Write-Host "ERROR: No USB device found. Plug in USB cable and allow debugging." -ForegroundColor Red
        return
    }

    Write-Host "USB device found. Switching to TCP mode on port $WIFI_PORT ..."
    adb tcpip $WIFI_PORT | Out-Null
    Start-Sleep -Seconds 2

    $result = adb connect $DEVICE_ADDR
    if ($result -match "connected") {
        Write-Host "Connected: $DEVICE_ADDR -- you can unplug USB." -ForegroundColor Green
    } else {
        Write-Host "ERROR: $result" -ForegroundColor Red
        Write-Host "Tip: Make sure both devices are on the same WiFi network." -ForegroundColor Yellow
    }
}

function Show-Status {
    Write-Host "`nConnected ADB devices:"
    adb devices
}

function Disconnect-Wifi {
    Write-Host "Disconnecting $DEVICE_ADDR ..."
    adb disconnect $DEVICE_ADDR
}

function Add-FirewallRule {
    $rule = Get-NetFirewallRule -DisplayName "ADBWiFi" -ErrorAction SilentlyContinue
    if ($rule) {
        Write-Host "Firewall rule already exists." -ForegroundColor Yellow
        return
    }

    $isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    if (-not $isAdmin) {
        Write-Host "Elevating to Administrator to add firewall rule..." -ForegroundColor Yellow
        Start-Process powershell -ArgumentList "-File `"$PSCommandPath`" firewall" -Verb RunAs -Wait
        return
    }

    New-NetFirewallRule -DisplayName "ADBWiFi" -Direction Outbound -Action Allow -Protocol TCP -RemotePort 5000-50000 | Out-Null
    Write-Host "Firewall rule added." -ForegroundColor Green
}

function Start-Logcat {
    Write-Host "Streaming logcat for $APP_PACKAGE (Ctrl+C to stop)..."
    $pid = adb shell pidof -s $APP_PACKAGE
    if ($pid) {
        adb logcat --pid=$pid
    } else {
        Write-Host "App not running or PID not found. Streaming all logcat with package filter..."
        adb logcat -s $APP_PACKAGE
    }
}

function Install-Apk {
    $apk = "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apk)) {
        Write-Host "APK not found at $apk. Building first..."
        .\gradlew.bat assembleDebug
    }
    Write-Host "Installing APK..."
    adb install -r $apk
}

# --- Main ---
$command = $args[0]

switch ($command) {
    "connect"    { Connect-Wifi }
    "status"     { Show-Status }
    "disconnect" { Disconnect-Wifi }
    "reconnect"  { Disconnect-Wifi; Start-Sleep -Seconds 1; Connect-Wifi }
    "logcat"     { Start-Logcat }
    "install"    { Install-Apk }
    "firewall"   { Add-FirewallRule }
    default {
        Write-Host "CDC ADB Helper" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Usage: .\scripts\adb.ps1 [command]"
        Write-Host ""
        Write-Host "Commands:"
        Write-Host "  connect      Connect via WiFi (falls back to USB if needed)"
        Write-Host "  status       Show connected devices"
        Write-Host "  disconnect   Disconnect WiFi device"
        Write-Host "  reconnect    Disconnect then reconnect"
        Write-Host "  logcat       Stream logcat filtered to CDC package"
        Write-Host "  install      Build and install debug APK"
        Write-Host "  firewall     Add Windows Firewall rule for ADB WiFi (auto-elevates if needed)"
    }
}