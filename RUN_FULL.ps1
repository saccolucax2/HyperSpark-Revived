$runs_per_instance = 5
$instances = @("NRP1", "NRP2", "NRP3", "NRP5", "NRP4")

Set-Location -Path $PSScriptRoot

$csv_path = "$PSScriptRoot\data\experiments_results.csv"
if (Test-Path $csv_path) {
    Write-Host "Deleting old dataset..." -ForegroundColor Red
    Remove-Item -Path $csv_path -Force
}

$archive_base_path = "$PSScriptRoot\data\logs\benchmark_NRP"
if (Test-Path $archive_base_path) {
    Write-Host "Cleaning old log archives..." -ForegroundColor Red
    Remove-Item -Path "$archive_base_path\*" -Recurse -Force
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

        Write-Host "Archiving logs for $inst (Run $i)..." -ForegroundColor Yellow
        $run_archive_path = "$archive_base_path\$inst\run_$i"
        New-Item -ItemType Directory -Force -Path $run_archive_path | Out-Null

        Move-Item -Path "$PSScriptRoot\data\logs\*.log" -Destination $run_archive_path -Force

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