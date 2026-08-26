$ErrorActionPreference = 'Stop'
$env:ANDROID_HOME = 'C:\Users\muham\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'

& "$PSScriptRoot\..\node_modules\.bin\appium.cmd" `
  --address 127.0.0.1 `
  --port 4723 `
  --base-path /
