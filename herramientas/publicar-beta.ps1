# Publica una versión de prueba de Hilvana en GitHub Releases.
#
# Uso:   herramientas\publicar-beta.cmd 0.10.1 "Arreglé X. Ahora puedes Y."
#
# Qué hace:
#   1. Cambia la versión en app-android\app\build.gradle.kts.
#   2. Corre las pruebas y compila el APK firmado con la llave de ~/.hilvana.
#   3. Hace commit + etiqueta v<versión> y lo sube a GitHub.
#   4. Crea la versión en GitHub (título "beta") con el APK adjunto.
# Los testers con la app instalada ven el aviso "Hay una versión nueva" en Inicio.

param(
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$Notas
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$android = Join-Path $repo 'app-android'
$gradleFile = Join-Path $android 'app\build.gradle.kts'
$utf8 = New-Object System.Text.UTF8Encoding $false

function Fail($message) {
    Write-Host ""
    Write-Host "ERROR: $message" -ForegroundColor Red
    exit 1
}

# --- Revisiones antes de tocar nada ---
if ($Version -notmatch '^\d+\.\d+\.\d+$') { Fail "La versión debe ser como 0.10.1 (tres números)." }
if (-not (Test-Path "$env:USERPROFILE\.hilvana\keystore.properties")) {
    Fail "No encuentro la llave de firma en $env:USERPROFILE\.hilvana. Sin ella el APK no se puede actualizar encima."
}
Push-Location $repo
try {
    $branch = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($branch -ne 'main') { Fail "Estás en la rama '$branch'. Publica desde main." }
    if (git status --porcelain) { Fail "Hay cambios sin commit. Súbelos primero (o descártalos)." }
    if (git tag --list "v$Version") { Fail "La versión $Version ya existe. Usa un número mayor." }

    $gradleText = [IO.File]::ReadAllText($gradleFile)
    $current = [regex]::Match($gradleText, 'val hilvanaVersion = "([\d.]+)"').Groups[1].Value
    if (-not $current) { Fail "No encontré 'val hilvanaVersion' en build.gradle.kts." }
    if ([version]$Version -le [version]$current) { Fail "La versión nueva ($Version) debe ser mayor que la actual ($current)." }

    Write-Host "Hilvana $current -> $Version" -ForegroundColor Cyan

    # --- 1. Versión ---
    $gradleText = $gradleText.Replace("val hilvanaVersion = `"$current`"", "val hilvanaVersion = `"$Version`"")
    [IO.File]::WriteAllText($gradleFile, $gradleText, $utf8)

    # --- 2. Pruebas + APK firmado ---
    Write-Host "Probando y compilando (tarda un par de minutos)..." -ForegroundColor Cyan
    Push-Location $android
    try {
        & .\gradlew.bat testDebugUnitTest assembleRelease -q
        $buildOk = $LASTEXITCODE -eq 0
        & .\gradlew.bat --stop -q | Out-Null
    } finally {
        Pop-Location
    }
    if (-not $buildOk) {
        git checkout -- $gradleFile
        Fail "Fallaron las pruebas o la compilación. No se publicó nada."
    }
    $builtApk = Join-Path $android 'app\build\outputs\apk\release\app-release.apk'
    $apk = Join-Path $android "app\build\outputs\apk\release\Hilvana-$Version.apk"
    Copy-Item $builtApk $apk -Force

    # --- 3. Commit + etiqueta ---
    $msgFile = Join-Path $env:TEMP 'hilvana-release-msg.txt'
    [IO.File]::WriteAllText($msgFile, "Version $Version (beta)`n`n$Notas`n", $utf8)
    git add $gradleFile
    git commit -q -F $msgFile
    git tag "v$Version"
    git push -q origin main "v$Version"
    if ($LASTEXITCODE -ne 0) { Fail "No se pudo subir a GitHub (¿internet?). El commit y la etiqueta quedaron en tu laptop." }

    # --- 4. Versión en GitHub ---
    $body = @"
$Notas

---
**Cómo instalar o actualizar**
1. Abre este enlace desde el celular y toca **Hilvana-$Version.apk** (abajo, en *Assets*).
2. Abre el archivo descargado. Si Android lo pide, permite *instalar apps desconocidas* para tu navegador.
3. Si ya tienes Hilvana, se instala encima: tus notas se quedan.

Encuentras algo raro o tienes una idea? Escríbele a quien te pasó la app.
"@
    $notesFile = Join-Path $env:TEMP 'hilvana-release-notes.md'
    [IO.File]::WriteAllText($notesFile, $body, $utf8)
    gh release create "v$Version" $apk --latest --title "Hilvana $Version (beta)" --notes-file $notesFile
    if ($LASTEXITCODE -ne 0) { Fail "La etiqueta se subió pero no se creó la versión en GitHub. Reintenta con: gh release create v$Version `"$apk`" --latest" }

    Write-Host ""
    Write-Host "Listo: Hilvana $Version publicada." -ForegroundColor Green
    Write-Host "Enlace para tus testers: https://github.com/Vega-Mtz-JD/Hilvana-App-/releases/latest"
} finally {
    Pop-Location
}
