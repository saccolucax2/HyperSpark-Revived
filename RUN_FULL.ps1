$runs_per_instance = 5
$instances = @("NRP1", "NRP2", "NRP3", "NRP5", "NRP4")

Set-Location -Path $PSScriptRoot

$csv_path = "$PSScriptRoot\data\experiments_results.csv"
if (Test-Path $csv_path) {
    Write-Host "Deleting old dataset..." -ForegroundColor Red
    Remove-Item -Path $csv_path -Force
}

Write-Host "Starting GOLDEN RUN BENCHMARK!" -ForegroundColor Green

foreach ($inst in $instances) {
    Write-Host "=========================================" -ForegroundColor Magenta
    Write-Host " Starting tests for $inst" -ForegroundColor Magenta
    Write-Host "=========================================" -ForegroundColor Magenta

    for ($i = 1; $i -le $runs_per_instance; $i++) {
        Write-Host "--- Execution $i of $runs_per_instance ---" -ForegroundColor Cyan

        Write-Host "Cleaning Docker environment..." -ForegroundColor DarkGray
        docker-compose down -v --remove-orphans | Out-Null

        $env:NRP_TARGET=$inst
        & "$PSScriptRoot\RUN_DEMO.ps1"

        Write-Host "Forcing Docker network teardown..." -ForegroundColor DarkGray
        docker-compose down -v | Out-Null

        try { wsl.exe -d docker-desktop -e sh -c "echo 3 > /proc/sys/vm/drop_caches" 2>$null } catch {}
        try { wsl.exe -d Ubuntu -e sh -c "echo 3 > /proc/sys/vm/drop_caches" 2>$null } catch {}

        Write-Host "Cooling timeout (60 seconds) to let OS recover..." -ForegroundColor Yellow
        Start-Sleep -Seconds 60
    }
}

Write-Host "BENCHMARK COMPLETED! Generating final graphs..." -ForegroundColor Green
Set-Location -Path $PSScriptRoot
python pipeline/generate_plots.py