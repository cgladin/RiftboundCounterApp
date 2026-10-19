param([switch]$Reinstall)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

Write-Host "Build de la release..."
& .\gradlew.bat :app:assembleRelease
if ($LASTEXITCODE -ne 0) { throw "Echec du build" }

$apk = "app\build\outputs\apk\release\RiftboundCounter-release.apk"
if (-not (Test-Path $apk)) {
    throw "APK signe introuvable ($apk). Verifiez la config de signature dans local.properties."
}

Write-Host "Installation sur le telephone..."
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { throw "Echec de l'installation (telephone connecte et debogage USB active ?)" }

Write-Host "Deploiement termine."
