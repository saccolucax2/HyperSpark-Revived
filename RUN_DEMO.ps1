# ==========================================
#  HYPERSPARK FULL AUTOMATION v2.0
# ==========================================
Write-Host "STARTING DEMO HYPERSPARK..." -ForegroundColor Cyan

# 1. STOP  AND CLEANING
docker-compose down 2>$null
Start-Sleep -Seconds 2
Remove-Item -Path ".\data\logs\*.log" -Force -ErrorAction SilentlyContinue

# 2. STARTING DOCKER
Write-Host "Launching Containers..." -ForegroundColor Yellow
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

# 4. STARTING DASHBOARD
Write-Host "Generating graph..." -ForegroundColor Yellow
cd floria_dashboard
cargo run
cd ..

Write-Host "DEMO FINISHED. Updated graph." -ForegroundColor Green