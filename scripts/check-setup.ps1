# Verify device + Appium readiness before running tests
# Usage: .\scripts\check-setup.ps1

$failed = $false

function Ok($msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Bad($msg) { Write-Host "[!!] $msg" -ForegroundColor Red; $script:failed = $true }

Write-Host "=== Travel House Appium setup check ===" -ForegroundColor Cyan

try { Ok ("Java: " + (java -version 2>&1 | Select-Object -First 1)) } catch { Bad "Java not found" }
try { Ok ("Maven: " + ((mvn -v | Select-Object -First 1).ToString().Trim())) } catch { Bad "Maven not found" }
try { Ok ("Appium: " + (appium -v)) } catch { Bad "Appium not found (npm i -g appium)" }
try { Ok ("ADB: " + ((adb version | Select-Object -First 1).ToString().Trim())) } catch { Bad "ADB not found" }

$devices = @(adb devices 2>$null | Select-String "`tdevice$")
if ($devices.Count -eq 0) {
    Bad "No physical device connected (adb devices shows none in 'device' state)"
} elseif ($devices.Count -gt 1) {
    Write-Host "[..] Multiple devices found — set device.udid in config.properties" -ForegroundColor Yellow
    adb devices -l
} else {
    Ok ("Device: " + $devices[0].ToString().Trim())
}

try {
    $drivers = appium driver list --installed 2>&1 | Out-String
    if ($drivers -match "uiautomator2") { Ok "Appium driver uiautomator2 is installed" }
    else { Bad "Install driver: appium driver install uiautomator2" }
} catch {
    Bad "Could not list Appium drivers"
}

$config = Get-Content "src\test\resources\config.properties" -ErrorAction SilentlyContinue
$pkg = ($config | Select-String "^app.package=").ToString().Split("=",2)[1]
if ([string]::IsNullOrWhiteSpace($pkg) -or $pkg -eq "com.travelhouse") {
    Write-Host "[..] app.package still looks like a placeholder — run .\scripts\find-app.ps1 after installing Travel House" -ForegroundColor Yellow
} else {
    Ok "app.package=$pkg"
}

Write-Host ""
if ($failed) {
    Write-Host "Setup incomplete — fix the items above, then re-run." -ForegroundColor Red
    exit 1
}
Write-Host "Ready. Start Appium (.\scripts\start-appium.ps1), then: mvn clean test" -ForegroundColor Green
