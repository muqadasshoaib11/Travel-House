# CI helper: ensure Appium is up, device is connected, run the selected TestNG suite.
# Usage:
#   .\scripts\ci-run.ps1
#   .\scripts\ci-run.ps1 -SuiteXml "src/test/resources/dual-route-booking.xml"
#   .\scripts\ci-run.ps1 -SuiteXml "src/test/resources/smoke.xml" -SkipAppiumStart
#
# Credentials (optional — otherwise config.properties is used):
#   $env:LOGIN_EMAIL / $env:LOGIN_PASSWORD / $env:DEVICE_UDID

param(
    [string]$SuiteXml = "src/test/resources/ci-health.xml",
    [int]$AppiumPort = 4723,
    [switch]$SkipAppiumStart,
    [switch]$SkipDeviceCheck
)

# Continue: native tools (adb/appium) write informational lines to stderr; Stop would abort CI.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "=== Travel House CI run ===" -ForegroundColor Cyan
Write-Host "Suite: $SuiteXml"
Write-Host "Root:  $root"

function Test-AppiumUp {
    try {
        Invoke-WebRequest -Uri "http://127.0.0.1:$AppiumPort/status" -UseBasicParsing -TimeoutSec 3 | Out-Null
        return $true
    } catch {
        return $false
    }
}

if (-not $SkipDeviceCheck) {
    $raw = cmd /c "adb devices"
    $devices = @($raw -split "`r?`n" | Where-Object { $_ -match "\tdevice$" })
    if ($devices.Count -eq 0) {
        Write-Host "[CI] FAIL: No Android device in 'device' state (adb devices)" -ForegroundColor Red
        cmd /c "adb devices -l"
        exit 1
    }
    Write-Host "[CI] Device OK: $($devices[0].ToString().Trim())" -ForegroundColor Green
    # Keep display awake — black screenshots / missed taps when Xiaomi sleeps
    Write-Host "[CI] Keeping screen on + restarting UiAutomator2..."
    cmd /c "adb shell svc power stayon usb"
    cmd /c "adb shell settings put system screen_off_timeout 1800000"
    cmd /c "adb shell input keyevent KEYCODE_WAKEUP"
    cmd /c "adb shell input keyevent KEYCODE_MENU"
    cmd /c "adb shell input swipe 540 1800 540 600"
    cmd /c "adb shell am force-stop io.appium.uiautomator2.server"
    cmd /c "adb shell am force-stop io.appium.uiautomator2.server.test"
}

$appiumProc = $null
if (-not $SkipAppiumStart) {
    if (Test-AppiumUp) {
        Write-Host "[CI] Appium already running on :$AppiumPort" -ForegroundColor Green
    } else {
        Write-Host "[CI] Starting Appium on :$AppiumPort ..." -ForegroundColor Yellow
        $appiumCmd = (Get-Command appium -ErrorAction SilentlyContinue)
        if (-not $appiumCmd) {
            Write-Host "[CI] FAIL: 'appium' not found on PATH" -ForegroundColor Red
            exit 1
        }
        $appiumProc = Start-Process -FilePath $appiumCmd.Source -ArgumentList @("--port", "$AppiumPort") -PassThru -WindowStyle Hidden
        $ready = $false
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Seconds 2
            if (Test-AppiumUp) { $ready = $true; break }
        }
        if (-not $ready) {
            Write-Host "[CI] FAIL: Appium did not become ready on :$AppiumPort" -ForegroundColor Red
            if ($appiumProc -and -not $appiumProc.HasExited) { Stop-Process -Id $appiumProc.Id -Force -ErrorAction SilentlyContinue }
            exit 1
        }
        Write-Host "[CI] Appium ready (pid=$($appiumProc.Id))" -ForegroundColor Green
    }
}

# Build Maven -D overrides from env (CI secrets)
$mvnArgs = @(
    "-q", "test",
    "-DsuiteXmlFile=$SuiteXml"
)
if ($env:LOGIN_EMAIL) { $mvnArgs += "-Dlogin.email=$($env:LOGIN_EMAIL)" }
if ($env:LOGIN_PASSWORD) { $mvnArgs += "-Dlogin.password=$($env:LOGIN_PASSWORD)" }
if ($env:DEVICE_UDID) { $mvnArgs += "-Ddevice.udid=$($env:DEVICE_UDID)" }
if ($env:APPIUM_SERVER_URL) { $mvnArgs += "-Dappium.server.url=$($env:APPIUM_SERVER_URL)" }

Write-Host "[CI] mvn $($mvnArgs -join ' ')" -ForegroundColor Cyan
& mvn @mvnArgs
$exitCode = $LASTEXITCODE

Write-Host "[CI] Maven exit code: $exitCode"
if (Test-Path "reports\TravelHouse_AppBehaviour_PassFail_Report.html") {
    Write-Host "[CI] Report: reports\TravelHouse_AppBehaviour_PassFail_Report.html"
} elseif (Test-Path "reports\TravelHouse_FlightBooking_Summary.html") {
    Write-Host "[CI] Report: reports\TravelHouse_FlightBooking_Summary.html"
}

# Do not kill externally managed Appium; only stop the process we started
if ($appiumProc -and -not $appiumProc.HasExited) {
    Write-Host "[CI] Leaving Appium running (started by CI helper, pid=$($appiumProc.Id))"
}

exit $exitCode

# Runner must be interactive (not Windows service) for USB device access. Last CI nudge: 2026-07-31 02:14:43
