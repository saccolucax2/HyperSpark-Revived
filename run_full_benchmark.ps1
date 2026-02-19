$runs_per_instance = 3
$instances = @("NRP1", "NRP2", "NRP3", "NRP4", "NRP5")

Set-Location -Path $PSScriptRoot

foreach ($inst in $instances) {
    Write-Host "Starting tests for $inst" -ForegroundColor Magenta
    for ($i = 1; $i -le $runs_per_instance; $i++) {
        Write-Host "--- Esecution $i of $runs_per_instance ---" -ForegroundColor Cyan
        $env:NRP_TARGET=$inst
        & "$PSScriptRoot\RUN_DEMO.ps1"
    }
}

Write-Host "BENCHMARK COMPLETED! Generating final graphs..." -ForegroundColor Green
Set-Location -Path $PSScriptRoot
python pipeline/generate_plots.py