$runs_per_instance = 2
$instances = @("NRP5")

Set-Location -Path $PSScriptRoot

foreach ($inst in $instances) {
    Write-Host "Starting tests for $inst" -ForegroundColor Magenta
    for ($i = 1; $i -le $runs_per_instance; $i++) {
        Write-Host "--- Esecution $i of $runs_per_instance ---" -ForegroundColor Cyan
        $env:NRP_TARGET=$inst
        & "$PSScriptRoot\RUN_DEMO.ps1"
        Write-Host "Cooling timeout (60 seconds)..." -ForegroundColor Yellow
        Start-Sleep -Seconds 60
    }
}

Write-Host "BENCHMARK COMPLETED! Generating final graphs..." -ForegroundColor Green
Set-Location -Path $PSScriptRoot
python pipeline/generate_plots.py