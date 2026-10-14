# Compila Hilvana, la instala en tu celular y la abre.
# Al terminar apaga Gradle para liberar memoria (usa -SinApagar si vas a
# probar varias veces seguidas: las siguientes compilaciones serán más rápidas).
param([switch]$SinApagar, [string]$Direccion)

$project = Join-Path $PSScriptRoot '..\app-android'
$apk = Join-Path $project 'app\build\outputs\apk\debug\app-debug.apk'

& (Join-Path $PSScriptRoot 'conectar.ps1') -Direccion $Direccion
if ($LASTEXITCODE -ne 0) { exit 1 }
$device = adb devices | Select-String '^(?!emulator-)(\S+)\s+device$' |
    ForEach-Object { $_.Matches[0].Groups[1].Value } | Select-Object -First 1

Push-Location $project
try {
    Write-Host "Compilando... (la primera vez puede tardar 1-2 minutos)"
    .\gradlew.bat assembleDebug --console=plain -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "La compilación falló. Revisa el error de arriba." -ForegroundColor Red
        exit 1
    }

    Write-Host "Instalando en $device..."
    adb -s $device install -r $apk | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Host "No se pudo instalar. Mira 'Problemas comunes' en docs/guia-desarrollo.md" -ForegroundColor Red
        exit 1
    }

    adb -s $device shell am start -n com.hilvana.app/.MainActivity | Out-Null
    Write-Host "Listo: Hilvana está abierta en tu celular." -ForegroundColor Green
}
finally {
    if (-not $SinApagar) { .\gradlew.bat --stop -q }
    Pop-Location
}
