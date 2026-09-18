# RentManager — apaga todo (frontend + backend + MySQL) con un solo comando: stop.cmd
$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot

function Stop-Port($port, $label) {
  $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
  if ($connections) {
    $connections | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
    Write-Host "    $label detenido (:$port)" -ForegroundColor Green
  } else {
    Write-Host "    $label no estaba corriendo (:$port)" -ForegroundColor DarkGray
  }
}

Write-Host "`n==> Deteniendo servicios" -ForegroundColor Cyan
Stop-Port 4200 'Frontend'
Stop-Port 8080 'Backend'

if (docker info --format '{{.ServerVersion}}' 2>$null) {
  docker compose stop | Out-Null
  Write-Host '    MySQL detenido (los datos se conservan)' -ForegroundColor Green
} else {
  Write-Host '    Docker no estaba corriendo' -ForegroundColor DarkGray
}

Write-Host "`nTodo detenido. Para volver a arrancar: .\dev.cmd`n" -ForegroundColor Gray
