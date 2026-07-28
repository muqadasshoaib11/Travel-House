# Start Appium server (UiAutomator2 driver must already be installed: appium driver install uiautomator2)
# Usage: .\scripts\start-appium.ps1

param(
    [int]$Port = 4723
)

Write-Host "Starting Appium on port $Port..." -ForegroundColor Cyan
appium --port $Port
