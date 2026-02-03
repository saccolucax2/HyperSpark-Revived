# ==========================================
#  HYPERSPARK FULL AUTOMATION v2.0
# ==========================================
Write-Host "🔥 AVVIO DEMO HYPERSPARK (FULLY AUTOMATED)..." -ForegroundColor Cyan

# 1. STOP E PULIZIA
docker-compose down 2>$null
Start-Sleep -Seconds 2
Remove-Item -Path ".\edge_results\*.log" -Force -ErrorAction SilentlyContinue

# 2. AVVIO DOCKER
Write-Host "🚀 Avvio Container..." -ForegroundColor Yellow
docker-compose up -d

Write-Host "⏳ Attesa completamento calcoli (Polling attivo)..." -ForegroundColor Cyan

# 3. SMART POLLING (Ciclo di attesa automatico)
$nodes_count = 5
$completed = 0

while ($completed -lt $nodes_count) {
    Start-Sleep -Seconds 5 # Controlla ogni 5 secondi

    # Conta quanti file log contengono la parola "took" (che indica fine job Spark)
    # Nota: Usiamo Select-String per cercare dentro i file
    $completed = (Get-ChildItem ".\edge_results\*.log" | Select-String "took").Count

    # Barra di progresso testuale
    Write-Host "   Stato: $completed / $nodes_count nodi hanno finito..." -NoNewline -ForegroundColor Gray
    Write-Host "`r" -NoNewline # Ritorna a capo sulla stessa riga
}

Write-Host "`n✅ Tutti i nodi hanno completato il task!" -ForegroundColor Green

# 4. AVVIO IMMEDIATO DASHBOARD
Write-Host "🎨 Generazione Grafo..." -ForegroundColor Yellow
cd floria_dashboard
cargo run
cd ..

Write-Host "🏆 DEMO CONCLUSA. Grafico aggiornato." -ForegroundColor Green