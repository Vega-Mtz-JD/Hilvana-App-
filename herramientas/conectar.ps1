# Conecta tu celular por depuración inalámbrica.
# Requisitos: celular y laptop en la misma red Wi-Fi, y en el celular
# "Opciones de desarrollador > Depuración inalámbrica" activada.
#
# Uso:  conectar               -> busca el celular solo
#       conectar 192.168.1.2:41234  -> usa la IP:puerto que muestra el celular

param([string]$Direccion)

# Aquí se guarda la última dirección que funcionó (fuera del repositorio).
$savedFile = Join-Path $env:LOCALAPPDATA 'Hilvana\ultimo-celular.txt'

function Get-PhysicalDevice {
    # Cualquier dispositivo real listo (Wi-Fi o USB), sin contar emuladores.
    adb devices | Select-String '^(?!emulator-)(\S+)\s+device$' |
        ForEach-Object { $_.Matches[0].Groups[1].Value } | Select-Object -First 1
}

function Try-Connect([string]$address) {
    if (-not $address) { return $false }
    # "adb connect" puede quedarse esperando minutos si el celular no responde: máximo 8 s.
    $log = Join-Path $env:TEMP 'hilvana-adb-connect.txt'
    $process = Start-Process adb -ArgumentList 'connect', $address -NoNewWindow -PassThru -RedirectStandardOutput $log
    if (-not $process.WaitForExit(8000)) {
        $process.Kill()
        return $false
    }
    $device = Get-PhysicalDevice
    if ($device) {
        New-Item -ItemType Directory -Force (Split-Path $savedFile) | Out-Null
        Set-Content -Path $savedFile -Value $address -Encoding ascii
        Write-Host "Conectado: $device" -ForegroundColor Green
        return $true
    }
    return $false
}

if (-not $Direccion) {
    $device = Get-PhysicalDevice
    if ($device) {
        Write-Host "Celular ya conectado: $device" -ForegroundColor Green
        exit 0
    }
}

# 1) Dirección indicada a mano
if ($Direccion) {
    if (Try-Connect $Direccion) { exit 0 }
    Write-Host "No pude conectar a $Direccion" -ForegroundColor Yellow
}

# 2) Última dirección que funcionó
if (Test-Path $savedFile) {
    $last = (Get-Content $savedFile -TotalCount 1).Trim()
    Write-Host "Probando la última dirección usada ($last)..."
    if (Try-Connect $last) { exit 0 }
}

# 3) Búsqueda automática en la red Wi-Fi
Write-Host "Buscando tu celular en la red Wi-Fi..."
for ($i = 0; $i -lt 8; $i++) {
    $service = adb mdns services | Select-String '_adb-tls-connect\._tcp\s+(\S+)' | Select-Object -First 1
    if ($service -and (Try-Connect $service.Matches[0].Groups[1].Value)) { exit 0 }
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "No encontré tu celular. Revisa:" -ForegroundColor Yellow
Write-Host "  1. Que el celular y la laptop estén en la MISMA red Wi-Fi."
Write-Host "  2. Que 'Depuración inalámbrica' esté ACTIVADA y la pantalla del celular encendida."
Write-Host "  3. Entra a 'Depuración inalámbrica' en el celular, mira la 'Dirección IP y puerto'"
Write-Host "     y ejecuta:   herramientas\conectar.cmd 192.168.X.X:PUERTO"
exit 1
