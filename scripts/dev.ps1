param(
  [ValidateSet('up','down','logs','status','reset')][string]$Action = 'up',
  [switch]$DeleteData
)
$ErrorActionPreference = 'Stop'
$originalPassword = $env:CHECKERS_DATABASE_PASSWORD
Push-Location (Join-Path $PSScriptRoot '..')
try {
  if ($Action -ne 'up' -and -not $env:CHECKERS_DATABASE_PASSWORD) { $env:CHECKERS_DATABASE_PASSWORD = 'unused-for-stop' }
  switch ($Action) {
    'up' { docker compose up --build -d --wait; if ($LASTEXITCODE -eq 0) { Write-Host 'Abra http://localhost:8081' } }
    'down' { docker compose down }
    'logs' { docker compose logs -f }
    'status' { docker compose ps }
    'reset' { if (-not $DeleteData) { throw 'Para apagar o banco local, use: ./scripts/dev.ps1 reset -DeleteData' }; docker compose down -v }
  }
  if ($LASTEXITCODE -ne 0) { throw "Docker Compose falhou: $LASTEXITCODE" }
} finally {
  if ($null -eq $originalPassword) { Remove-Item Env:CHECKERS_DATABASE_PASSWORD -ErrorAction SilentlyContinue }
  else { $env:CHECKERS_DATABASE_PASSWORD = $originalPassword }
  Pop-Location
}
