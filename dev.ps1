# RentManager — arranca todo (MySQL + backend + frontend) con un solo comando: dev.cmd
# Uso: dev.cmd [-NoBrowser]
param([switch]$NoBrowser)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Log($message) {
  Write-Output ("[{0:HH:mm:ss}] {1}" -f (Get-Date), $message)
}

function Wait-Until($condition, $timeoutSeconds, $description) {
  $deadline = (Get-Date).AddSeconds($timeoutSeconds)
  $lastBeat = Get-Date
  while (-not (& $condition)) {
    if ((Get-Date) -gt $deadline) { throw "Timeout esperando: $description" }
    if (((Get-Date) - $lastBeat).TotalSeconds -ge 15) {
      Log "  ... esperando $description"
      $lastBeat = Get-Date
    }
    Start-Sleep -Seconds 2
  }
}

function Test-Http($url) {
  try { return (Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 5).StatusCode -eq 200 }
  catch { return $false }
}

function Test-Port($port) {
  return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Test-Docker {
  try { return [bool](docker info --format '{{.ServerVersion}}' 2>$null) } catch { return $false }
}

# 1. Docker / MySQL
Log 'Docker / MySQL'
if (-not (Test-Docker)) {
  Log '  Docker Desktop apagado, arrancando...'
  Start-Process "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe"
  Wait-Until { Test-Docker } 200 'Docker Desktop'
}
docker compose up -d | Out-Null
Wait-Until { (docker inspect --format '{{.State.Health.Status}}' rentmanager-mysql 2>$null) -eq 'healthy' } 150 'MySQL healthy'
Log '  MySQL healthy en localhost:3306'

# 2. Backend y frontend en paralelo
Log 'Backend y frontend (en paralelo)'
$jar = Join-Path $PSScriptRoot 'backend\target\backend-0.0.1-SNAPSHOT.jar'
if (Test-Port 8080) {
  Log '  backend ya estaba corriendo'
} else {
  if (-not (Test-Path $jar)) {
    Log '  generando jar (mvnw package)...'
    & (Join-Path $PSScriptRoot 'backend\mvnw.cmd') -f (Join-Path $PSScriptRoot 'backend\pom.xml') package -DskipTests -q
  }
  Start-Process java -ArgumentList '-jar', $jar -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $PSScriptRoot 'backend\target\app.log') `
    -RedirectStandardError (Join-Path $PSScriptRoot 'backend\target\app.err')
  Log '  backend lanzado (:8080, log: backend\target\app.log)'
}

if (Test-Port 4200) {
  Log '  frontend ya estaba corriendo'
} else {
  Start-Process node -ArgumentList 'node_modules/@angular/cli/bin/ng.js', 'serve', '--port', '4200' `
    -WindowStyle Hidden -WorkingDirectory (Join-Path $PSScriptRoot 'frontend') `
    -RedirectStandardOutput (Join-Path $env:TEMP 'rentmanager-ng.log') `
    -RedirectStandardError (Join-Path $env:TEMP 'rentmanager-ng.err')
  Log '  frontend lanzado (:4200, primera compilación puede tardar ~1 min)'
}

Wait-Until { Test-Http 'http://localhost:8080/api/health' } 180 'API /api/health'
Log '  API lista'
Wait-Until { Test-Http 'http://localhost:4200/' } 300 'SPA (ng serve)'
Log '  SPA lista'

Log 'Todo en marcha:'
Write-Output '    MySQL    localhost:3306 (rentmanager)'
Write-Output '    API      http://localhost:8080/api/health'
Write-Output '    Web      http://localhost:4200  (admin@rentmanager.local / admin1234)'
Write-Output '    Parar:   .\stop.cmd'

if (-not $NoBrowser) {
  Start-Process 'http://localhost:4200'
}
