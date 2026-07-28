# Find Travel House package / activity on a connected Android device
# Usage: .\scripts\find-app.ps1
# Optional: .\scripts\find-app.ps1 -Keyword "travel"

param(
    [string]$Keyword = "travel"
)

Write-Host "Checking adb devices..." -ForegroundColor Cyan
$devices = adb devices | Select-String "`tdevice$"
if (-not $devices) {
    Write-Host "No device in 'device' state. Connect the phone, enable USB debugging, and accept the RSA prompt." -ForegroundColor Red
    exit 1
}
adb devices -l

Write-Host "`nSearching installed packages for '$Keyword'..." -ForegroundColor Cyan
$packages = adb shell pm list packages | Select-String -Pattern $Keyword -CaseSensitive:$false
if (-not $packages) {
    Write-Host "No package matched '$Keyword'. Listing all packages (filter manually):" -ForegroundColor Yellow
    adb shell pm list packages
    exit 1
}

$packages | ForEach-Object { Write-Host $_.Line }

$first = ($packages | Select-Object -First 1).ToString().Replace("package:", "").Trim()
Write-Host "`nUsing first match: $first" -ForegroundColor Green

Write-Host "`nResolving launchable activity..." -ForegroundColor Cyan
$resolve = adb shell cmd package resolve-activity --brief $first
Write-Host $resolve

$activityLine = ($resolve | Select-Object -Last 1).ToString().Trim()
$activity = $null
if ($activityLine -match "/") {
    $activity = $activityLine.Split("/")[-1]
}

Write-Host "`n--- Suggested config.properties values ---" -ForegroundColor Green
Write-Host "app.package=$first"
if ($activity) {
    Write-Host "app.activity=$activity"
} else {
    Write-Host "# app.activity=  (could not resolve; Appium can often launch with package only)"
}

$udid = (adb devices | Select-String "`tdevice$" | Select-Object -First 1).ToString().Split("`t")[0].Trim()
Write-Host "device.udid=$udid"

Write-Host "`nCurrent foreground app (open Travel House on the phone first, then re-run if needed):" -ForegroundColor Cyan
adb shell dumpsys window | Select-String -Pattern "mCurrentFocus|mFocusedApp" | Select-Object -First 4
