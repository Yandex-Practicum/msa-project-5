$ErrorActionPreference = "Stop"

$taskRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $taskRoot

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
}

docker compose up airflow-init
docker compose up -d

Write-Host "Airflow: http://localhost:8080 (admin/admin)"
Write-Host "MailHog: http://localhost:8025"
