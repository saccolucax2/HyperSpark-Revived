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

Write-Host "...Waiting process end (Active Polling)..." -ForegroundColor Cyan

# 3. SMART POLLING
$nodes_count = 5
$completed = 0

while ($completed -lt $nodes_count) {
    Start-Sleep -Seconds 5
    $completed = (Get-ChildItem ".\data\logs\*.log" | Select-String "Solution:").Count
    Write-Host "   State: $completed / $nodes_count finished nodes..." -NoNewline -ForegroundColor Gray
    Write-Host "`r" -NoNewline
}

Write-Host "`nTask completed for all nodes!" -ForegroundColor Green

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