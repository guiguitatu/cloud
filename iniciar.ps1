# Script PowerShell para iniciar os microserviços
# Autor: Assistente IA
# Data: $(Get-Date -Format "yyyy-MM-dd")

param(
    [switch]$Clean,  # Remove containers e volumes antigos
    [switch]$NoBrowser,  # Não abre navegador automaticamente
    [switch]$Verbose  # Modo verbose
)

Write-Host "🚀 Iniciando Microserviços Cloud..." -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan

# Verificar se Docker está rodando
try {
    $dockerVersion = docker --version 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker não encontrado"
    }
    Write-Host "✅ Docker encontrado: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ ERRO: Docker não está instalado ou não está rodando!" -ForegroundColor Red
    Write-Host "   Instale o Docker Desktop e certifique-se de que está executando." -ForegroundColor Yellow
    exit 1
}

# Verificar se docker-compose existe
try {
    $composeVersion = docker-compose --version 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "docker-compose não encontrado"
    }
    Write-Host "✅ Docker Compose encontrado: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ ERRO: Docker Compose não encontrado!" -ForegroundColor Red
    Write-Host "   Instale o Docker Compose ou use 'docker compose' (versão mais nova)." -ForegroundColor Yellow
    exit 1
}

# Parar containers existentes
Write-Host "`n🛑 Parando containers existentes..." -ForegroundColor Yellow
docker-compose down 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Containers parados com sucesso" -ForegroundColor Green
} else {
    Write-Host "ℹ️  Nenhum container estava rodando" -ForegroundColor Blue
}

# Limpeza opcional
if ($Clean) {
    Write-Host "`n🧹 Realizando limpeza completa..." -ForegroundColor Yellow
    docker-compose down --volumes --remove-orphans 2>$null | Out-Null
    docker system prune -f 2>$null | Out-Null
    Write-Host "✅ Limpeza completa realizada" -ForegroundColor Green
}

# Subir os serviços
Write-Host "`n🏗️  Construindo e iniciando serviços..." -ForegroundColor Yellow
if ($Verbose) {
    docker-compose up -d --build
} else {
    docker-compose up -d --build 2>$null | Out-Null
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERRO: Falha ao iniciar os serviços!" -ForegroundColor Red
    Write-Host "   Verifique os logs com: docker-compose logs" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Serviços iniciados com sucesso!" -ForegroundColor Green

# Aguardar inicialização
Write-Host "`n⏳ Aguardando serviços ficarem prontos..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Verificar status dos containers
Write-Host "`n📊 Status dos containers:" -ForegroundColor Cyan
Write-Host "==========================" -ForegroundColor Cyan

$containers = docker-compose ps
if ($containers) {
    Write-Host $containers
} else {
    Write-Host "❌ Nenhum container encontrado!" -ForegroundColor Red
    exit 1
}

# Verificar saúde dos serviços
Write-Host "`n🏥 Verificando saúde dos serviços..." -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan

# Testar Consul
try {
    $consulResponse = Invoke-WebRequest -Uri "http://localhost:8500/v1/status/leader" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Consul: OK (porta 8500)" -ForegroundColor Green
} catch {
    Write-Host "❌ Consul: FALHA (porta 8500)" -ForegroundColor Red
}

# Testar API Gateway
try {
    $gatewayResponse = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 5 -ErrorAction Stop
    $healthStatus = ($gatewayResponse.Content | ConvertFrom-Json).status
    if ($healthStatus -eq "UP") {
        Write-Host "✅ API Gateway: OK (porta 8080)" -ForegroundColor Green
    } else {
        Write-Host "⚠️  API Gateway: $healthStatus (porta 8080)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ API Gateway: FALHA (porta 8080)" -ForegroundColor Red
}

# Testar ms-kotlin via gateway
try {
    $kotlinResponse = Invoke-WebRequest -Uri "http://localhost:8080/ms-kotlin/api/mensagem?nome=teste" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ ms-kotlin: OK (via gateway)" -ForegroundColor Green
} catch {
    Write-Host "❌ ms-kotlin: FALHA (via gateway)" -ForegroundColor Red
}

# Testar ms-python via gateway
try {
    $pythonResponse = Invoke-WebRequest -Uri "http://localhost:8080/ms-python/api/mensagem?nome=teste" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ ms-python: OK (via gateway)" -ForegroundColor Green
} catch {
    Write-Host "❌ ms-python: FALHA (via gateway)" -ForegroundColor Red
}

# Informações finais
Write-Host "`n🎉 Microserviços iniciados com sucesso!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📍 URLs importantes:" -ForegroundColor Cyan
Write-Host "   • API Gateway:     http://localhost:8080" -ForegroundColor White
Write-Host "   • Consul UI:       http://localhost:8500" -ForegroundColor White
Write-Host "   • Testes HTTP:     arquivo teste.http" -ForegroundColor White
Write-Host ""
Write-Host "🛠️  Comandos úteis:" -ForegroundColor Cyan
Write-Host "   • Ver logs:        docker-compose logs -f" -ForegroundColor White
Write-Host "   • Parar serviços:  docker-compose down" -ForegroundColor White
Write-Host "   • Status:          docker-compose ps" -ForegroundColor White
Write-Host ""

# Abrir navegador (opcional)
if (-not $NoBrowser) {
    Write-Host "🌐 Abrindo navegador..." -ForegroundColor Blue
    try {
        Start-Process "http://localhost:8500"  # Abre Consul UI
        Start-Sleep -Seconds 2
        Start-Process "http://localhost:8080"  # Abre API Gateway
    } catch {
        Write-Host "⚠️  Não foi possível abrir o navegador automaticamente" -ForegroundColor Yellow
    }
}

Write-Host "`n✨ Pronto! Seus microserviços estão rodando." -ForegroundColor Green
Write-Host "   Execute 'teste.http' no VS Code para testar as APIs." -ForegroundColor Cyan
