# Microserviços Cloud - PUC

Este projeto contém uma arquitetura de microserviços com service discovery usando Consul.

## 🚀 Início Rápido

### Opção 1: Script Automático (Recomendado)
```powershell
# Executar o script de inicialização
.\iniciar.ps1
```

### Opção 2: Manual
```bash
# Construir e iniciar serviços
docker-compose up -d --build

# Aguardar inicialização
sleep 15

# Verificar status
docker-compose ps
```

## 📁 Arquivos do Projeto

- `docker-compose.yml` - Configuração dos serviços
- `teste.http` - Arquivo de testes HTTP (VS Code REST Client)
- `iniciar.ps1` - Script PowerShell para iniciar tudo
- `exemplos-payloads.json` - Exemplos de payloads JSON para testes
- `README.md` - Este arquivo

## 🏗️ Serviços

| Serviço | Tecnologia | Porta | Descrição |
|---------|------------|-------|-----------|
| **Consul** | HashiCorp Consul | 8500 | Service Discovery |
| **API Gateway** | Java/Spring Boot | 8080 | Gateway principal |
| **ms-kotlin** | Kotlin/Spring Boot | Dinâmica | Microserviço Kotlin |
| **ms-python** | Python/FastAPI | Dinâmica | Microserviço Python |

## 🧪 Testes

### Usando VS Code REST Client
1. Instale a extensão "REST Client" no VS Code
2. Abra o arquivo `teste.http`
3. Clique em "Send Request" em cada endpoint

O arquivo inclui testes completos com:
- **GET**: Buscar/listar recursos
- **POST**: Criar novos recursos
- **PUT**: Atualizar recursos existentes
- **DELETE**: Remover recursos
- Headers apropriados (Content-Type: application/json)
- Payloads realistas baseados nos modelos dos serviços

### URLs Diretas
- **API Gateway**: http://localhost:8080
- **Consul UI**: http://localhost:8500
- **ms-kotlin via Gateway**: http://localhost:8080/ms-kotlin/api/mensagem?nome=teste
- **ms-python via Gateway**: http://localhost:8080/ms-python/api/mensagem?nome=teste

## 📋 Endpoints Disponíveis

### MS-Kotlin (Catálogo de Produtos)
- `GET /ms-kotlin/produto` - Listar todos os produtos
- `GET /ms-kotlin/produto/{id}` - Buscar produto por ID
- `GET /ms-kotlin/produto/codigo/{codigo}` - Buscar por código do produto
- `POST /ms-kotlin/produto` - Criar produto(s)
- `PUT /ms-kotlin/produto/{id}` - Atualizar produto
- `DELETE /ms-kotlin/produto/{id}` - Remover produto

**⚠️ Importante:** O ms-kotlin inicializa automaticamente 3 produtos com códigos 101, 202 e 303. Use códigos diferentes (401, 402, etc.) ao criar novos produtos para evitar conflito 409.

**🐛 Erro 409 (Conflict):** Se receber erro 409 ao criar produtos, significa que o código do produto já existe. Use códigos únicos não utilizados pelos dados iniciais.

### MS-Python (Pedidos e Pagamentos)
- `POST /ms-python/order/` - Criar pedido
- `GET /ms-python/order/{numero}` - Buscar pedidos
- `POST /ms-python/payment/` - Criar pagamento
- `GET /ms-python/payment/{id}` - Buscar pagamento por ID
- `GET /ms-python/payment/order/{numero}` - Pagamentos por pedido
- `PUT /ms-python/payment/{id}/status` - Atualizar status do pagamento
- `GET /ms-python/payment/` - Listar todos os pagamentos

### API Gateway
- `GET /loadbalancer/instances/{servico}` - Ver instâncias ativas

## 🛠️ Comandos Úteis

```bash
# Ver logs
docker-compose logs -f

# Parar serviços
docker-compose down

# Ver status
docker-compose ps

# Limpeza completa
docker-compose down --volumes --remove-orphans
docker system prune -f
```

## 🔧 Desenvolvimento

Para desenvolvimento local, cada microserviço pode ser executado individualmente:

### ms-kotlin
```bash
cd ms-kotlin
./mvnw spring-boot:run
```

### ms-python
```bash
cd ms-python
python main.py
```

### API Gateway
```bash
cd api-gateway
./mvnw spring-boot:run
```

## 📊 Monitoramento

- **Consul Dashboard**: http://localhost:8500
- **Health Checks**: http://localhost:8080/actuator/health
- **Load Balancer Info**: http://localhost:8080/loadbalancer/instances/{service-name}

## 🐛 Troubleshooting

### Serviços não sobem
```bash
# Verificar logs detalhados
docker-compose logs

# Limpeza e reinício
.\iniciar.ps1 -Clean
```

### Porta ocupada
```bash
# Verificar portas em uso
netstat -ano | findstr :8080
netstat -ano | findstr :8500
```

### Docker não responde
```bash
# Reiniciar Docker Desktop
# Ou no PowerShell como administrador:
Restart-Service docker
```

### Erro 409 (Conflict) no ms-kotlin
```bash
# Este erro ocorre quando tenta criar um produto com código já existente
# O ms-kotlin inicializa automaticamente produtos com códigos 101, 202, 303

# Solução: Use códigos diferentes
POST /ms-kotlin/produto
{
  "codigoProduto": 401,  // Use códigos a partir de 401
  "descricao": "Novo Produto",
  "preco": 99.99,
  "codGruEst": 300
}
```

### Produto não encontrado (404)
```bash
# Verifique se o ID do produto existe
GET /ms-kotlin/produto

# Use um ID válido da lista retornada
PUT /ms-kotlin/produto/{id}
```

### Erro no ms-python (TypeError: missing argument)
```bash
# Este erro ocorria ao criar pagamentos devido a campos opcionais
# Corrigido adicionando init=False nos campos opcionais do PaymentModel

# Para recriar o problema (não recomendado):
# 1. Remover init=False dos campos transactionId e updatedAt
# 2. Reiniciar ms-python
# 3. Tentar criar pagamento

# Solução aplicada no PaymentModel:
transactionId: Mapped[Optional[str]] = mapped_column(
    nullable=True, init=False, default=None
)
```

---

**Projeto acadêmico - PUC**