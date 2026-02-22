# ==============================================
#  HYPERSPARK FULL AUTOMATION v3.0 (NRP Edition)
# ==============================================
$NRP_INSTANCE = if ($env:NRP_TARGET) { $env:NRP_TARGET } else { "NRP1" }

Write-Host "STARTING DEMO HYPERSPARK - Instance: $NRP_INSTANCE" -ForegroundColor Cyan

# 1. STOP AND CLEANING LOGS
docker-compose down 2>$null
Start-Sleep -Seconds 2
Remove-Item -Path ".\data\logs\*.log" -Force -ErrorAction SilentlyContinue

# 2. CONTAINER START
Write-Host "Launching Containers for $NRP_INSTANCE..." -ForegroundColor Yellow
$env:NRP_TARGET=$NRP_INSTANCE
docker-compose up -d

Write-Host "...Waiting process end (Active Polling with Watchdog)..." -ForegroundColor Cyan

# 3. SMART POLLING
$nodes_count = 5
$completed = 0
$timeout_seconds = 700
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

while ($completed -lt $nodes_count) {
    Start-Sleep -Seconds 5
    $log_files = Get-ChildItem ".\data\logs\*.log" 2>$null
    if ($null -ne $log_files) {
        $completed = ($log_files | Select-String "Solution:" -Quiet).Count
    } else {
        $completed = 0
    }

    $elapsed = [math]::Round($stopwatch.Elapsed.TotalSeconds)
    Write-Host "`r   State: $completed / $nodes_count finished nodes... (Time: ${elapsed}s / ${timeout_seconds}s)" -NoNewline -ForegroundColor Gray

    if ($elapsed -ge $timeout_seconds) {
        Write-Host "`n[WATCHDOG ERROR] Timeout reached! Some nodes failed or crashed." -ForegroundColor Red
        break
    }
}

$stopwatch.Stop()

if ($completed -eq $nodes_count) {
    Write-Host "`nTask completed successfully for all nodes!" -ForegroundColor Green
} else {
    Write-Host "`nTask aborted due to timeout. Extracting data only from survived nodes..." -ForegroundColor Yellow
}

# 4. EXTRACTING CSV MASTER
Write-Host "Extracting data to CSV..." -ForegroundColor Yellow
python pipeline/extract_to_csv.py

# 5. UPLOAD DASHBOARD RUST
Write-Host "Updating topology graph..." -ForegroundColor Yellow
cd visualization-floria
cargo run
cd ..
Set-Location -Path $PSScriptRoot

Write-Host "RUN FINISHED! Data appended to data/experiments_results.csv" -ForegroundColor Green