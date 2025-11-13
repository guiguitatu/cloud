## Como executar o ambiente

### 🚀 **Fluxo Simplificado (Recomendado)**

       ```bash
       # 1. Iniciar infraestrutura (opcional)
       docker compose up -d consul

       # 2. Iniciar microsserviços (em terminais separados)
       cd ms-kotlin && ./mvnw.cmd spring-boot:run
       cd ms-python && pip install -r requirements.txt && python main.py

       # 3. Iniciar gateway (usa portas conhecidas ou descobre automaticamente)
       cd api-gateway && ./mvnw.cmd spring-boot:run

       # 4. Acessar APIs via Gateway
       # http://localhost:8080/ms-kotlin/  → Catálogo de produtos e Gestão de clientes (Kotlin)
       # http://localhost:8080/ms-python/   → Gestão de pedidos e Pagamentos (Python)
       ```

**🎯 Vantagens:** Gateway descobre portas automaticamente, não precisa configurar nada!

---

### Pré-requisitos

- [Docker](https://www.docker.com/) e Docker Compose para subir o Consul.
- JDK 17 ou superior (o projeto Kotlin usa Spring Boot 3.5).
- Python 3.10 ou superior e `pip`.

> Todas as instruções assumem que os comandos são executados a partir da raiz do repositório (`cloud/`).

### 1. Iniciar o serviço de infraestrutura (Consul) - Opcional

```bash
docker compose up -d consul
```

O Consul ficará disponível em `http://localhost:8500/ui` para monitoramento. **Observação:** O gateway atual faz descoberta automática de portas e funciona **sem o Consul**, mas mantê-lo ativo permite monitoramento dos serviços registrados.

> **Dica:** Se preferir iniciar tudo de uma vez, use `docker compose up -d` para subir Consul, MySQL e outros serviços de infraestrutura.

### 2. Subir o microsserviço Kotlin (`ms-kotlin`)

Em outro terminal:

```bash
cd ms-kotlin
./mvnw spring-boot:run
# Windows PowerShell
# .\mvnw.cmd spring-boot:run
```

O Spring Boot utiliza **porta dinâmica** (definida para `0`), escolhendo automaticamente uma porta livre. **Não é necessário** verificar logs ou consultar o Consul - o gateway encontra automaticamente onde o serviço está rodando.

Por padrão o serviço utiliza um banco SQLite local em `catalogo.db`; caso deseje apontar para outro caminho, defina a variável de ambiente `SQLITE_DB_PATH` antes de executar o serviço. Na primeira execução o serviço cria automaticamente as tabelas `produtos` e `clientes`, e popula dados de exemplo:
- 3 produtos de exemplo no catálogo
- 2 clientes de exemplo na base de clientes

> **Dica (Windows):** Para utilizar outro caminho de banco, execute `setx SQLITE_DB_PATH "C:\\caminho\\catalogo.db"` antes de iniciar o serviço.

### 3. Subir o microsserviço Python (`ms-python`)

Em um novo terminal, instale as dependências e execute o serviço:

```bash
cd ms-python
pip install -r requirements.txt
python main.py
```

O serviço usa **porta dinâmica**, escolhendo automaticamente uma porta livre. **Não é necessário** verificar logs ou configurar ambiente virtual - o gateway encontra automaticamente onde o serviço está rodando.

Na primeira execução o serviço cria automaticamente as tabelas `orders` e `payments` no banco SQLite local (`orders.db`).

Utilize `CTRL+C` para finalizar o serviço.

### 4. Subir o API Gateway (`api-gateway`)

O gateway expõe um ponto de entrada único (`http://localhost:8080`) e **descobre automaticamente as portas dos microsserviços**. Não depende do Consul para roteamento básico - usa descoberta inteligente de portas!

#### 4.1 Executar localmente (recomendado)

```bash
cd api-gateway
./mvnw spring-boot:run
# Windows PowerShell
# .\mvnw.cmd spring-boot:run
```

O gateway iniciará na porta `8080` e automaticamente descobrirá onde estão os microsserviços, independente das portas que eles escolherem.

**Como funciona a descoberta automática:**
- Testa portas previamente conhecidas onde os serviços rodaram
- Faz health checks (`/actuator/health` para Kotlin, `/health` para Python)
- Encontra automaticamente os serviços e roteia as requisições

#### 4.2 Executar via Docker (opcional)

```bash
# Inicia apenas o gateway (Consul precisa estar rodando)
docker compose up -d api-gateway

# Para parar
docker compose stop api-gateway
```

> **Nota:** A versão Docker do gateway ainda depende do Consul para descoberta de serviços.

## Autenticação JWT

O sistema utiliza autenticação JWT (JSON Web Token) para proteger os endpoints. Todos os endpoints dos microsserviços (exceto `/auth/login`, `/auth/validate` e endpoints de health) requerem autenticação.

### Como usar o JWT

#### 1. Fazer login e obter o token

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "username": "admin",
  "role": "ADMIN",
  "expiresIn": 3600
}
```

#### 2. Usar o token nas requisições

Copie o token da resposta e use-o no header `Authorization` de todas as requisições:

```bash
curl -X GET "http://localhost:8080/ms-kotlin/produto" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 3. Validar um token

```bash
curl -X POST "http://localhost:8080/auth/validate" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### Usuários de exemplo

O sistema vem com os seguintes usuários pré-configurados para testes:

| Username | Password  | Role    |
|----------|-----------|---------|
| admin    | admin123  | ADMIN   |
| user     | user123   | USER    |
| manager  | manager123| MANAGER |

**Nota:** Em produção, substitua esta autenticação simples por integração com um sistema de autenticação adequado (banco de dados, LDAP, etc.).

### Endpoints públicos (não requerem autenticação)

- `POST /auth/login` - Fazer login
- `POST /auth/validate` - Validar token
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Informações do sistema
- `GET /` - Página inicial

Todos os outros endpoints requerem o header `Authorization: Bearer <token>`.

---

## Exemplos de uso via API Gateway

Os exemplos abaixo assumem que todos os serviços estão rodando. O gateway automaticamente encontra os microsserviços independente das portas que eles escolherem.

**⚠️ IMPORTANTE:** Todos os exemplos abaixo requerem autenticação JWT. Adicione o header `Authorization: Bearer <seu-token>` em todas as requisições.

**URLs dos Swaggers (descobertas automaticamente):**
- `http://localhost:8080/ms-kotlin/` → Swagger com catálogo de produtos e gestão de clientes
- `http://localhost:8080/ms-python/` → Swagger com gestão de pedidos e pagamentos

### ms-kotlin — Catálogo de Produtos e Gestão de Clientes

#### Produtos

##### Inserir um produto

```bash
# Primeiro, faça login para obter o token
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Use o token para criar um produto
curl -X POST "http://localhost:8080/ms-kotlin/produto" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '[{
    "codigoProduto": 9001,
    "descricao": "Mouse sem fio",
    "preco": 199.9,
    "codGruEst": 300
  }]'
```

##### Consultar um produto

```bash
# Obter token (se ainda não tiver)
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-kotlin/produto/1" \
  -H "Authorization: Bearer $TOKEN"
```

##### Listar todos os produtos

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-kotlin/produto" \
  -H "Authorization: Bearer $TOKEN"
```

#### Clientes

##### Criar um cliente

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X POST "http://localhost:8080/ms-kotlin/cliente" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '[{
    "cpf": "11122233344",
    "nome": "Carlos Oliveira",
    "email": "carlos.oliveira@email.com",
    "telefone": "41988887777",
    "endereco": "Av. Brasil, 456",
    "cidade": "Curitiba",
    "estado": "PR",
    "cep": "80050000",
    "ativo": true
  }]'
```

##### Buscar cliente por ID

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-kotlin/cliente/1" \
  -H "Authorization: Bearer $TOKEN"
```

##### Buscar cliente por CPF

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-kotlin/cliente/cpf/12345678901" \
  -H "Authorization: Bearer $TOKEN"
```

##### Listar todos os clientes

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-kotlin/cliente" \
  -H "Authorization: Bearer $TOKEN"
```

##### Listar apenas clientes ativos

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-kotlin/cliente?ativos=true" \
  -H "Authorization: Bearer $TOKEN"
```

### ms-python — Gestão de Pedidos e Pagamentos

#### Pedidos

##### Inserir um pedido

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X POST "http://localhost:8080/ms-python/order" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "productCode": 9001,
    "tableNumber": 12,
    "quantity": 3
  }'
```

##### Consultar pedidos por número

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-python/order/1001" \
  -H "Authorization: Bearer $TOKEN"
```

#### Pagamentos

##### Criar um pagamento

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X POST "http://localhost:8080/ms-python/payment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "orderNumber": 1001,
    "amount": 599.70,
    "paymentMethod": "PIX"
  }'
```

##### Buscar pagamento por ID

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-python/payment/1" \
  -H "Authorization: Bearer $TOKEN"
```

##### Buscar pagamentos de um pedido

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-python/payment/order/1001" \
  -H "Authorization: Bearer $TOKEN"
```

##### Atualizar status do pagamento

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X PUT "http://localhost:8080/ms-python/payment/1/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "status": "COMPLETED",
    "transactionId": "TXN-123456789"
  }'
```

##### Listar todos os pagamentos

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET "http://localhost:8080/ms-python/payment" \
  -H "Authorization: Bearer $TOKEN"
```

**Métodos de pagamento disponíveis:** `CREDIT_CARD`, `DEBIT_CARD`, `PIX`, `CASH`, `DIGITAL_WALLET`

**Status de pagamento disponíveis:** `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED`

### Exemplo prático: Script completo

Para facilitar o uso, você pode criar um script que obtém o token automaticamente:

**Windows PowerShell:**
```powershell
# Fazer login e obter token
$response = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'

$token = $response.token

# Usar o token em uma requisição
Invoke-RestMethod -Uri "http://localhost:8080/ms-kotlin/produto" `
  -Method Get `
  -Headers @{"Authorization"="Bearer $token"}
```

**Linux/Mac (Bash):**
```bash
#!/bin/bash
# Obter token
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Exportar token para usar em outras requisições
export TOKEN

# Exemplo de uso
curl -X GET "http://localhost:8080/ms-kotlin/produto" \
  -H "Authorization: Bearer $TOKEN"
```

**Nota sobre jq:** Se você não tiver `jq` instalado, pode extrair o token manualmente da resposta JSON ou instalar: `sudo apt install jq` (Linux) ou `brew install jq` (Mac).

## Problemas comuns

- **`connect ECONNREFUSED 127.0.0.1:8080` ao usar Postman/cURL:** certifique-se de que o gateway está ativo (passo 3). O gateway deve estar rodando para responder na porta `8080`.
- **Gateway retorna 503 Service Unavailable:** os microsserviços não estão rodando ou não são encontrados. Verifique se ms-kotlin e ms-python estão ativos nos terminais.
- **Erro 404 ao acessar endpoints:** o gateway está funcionando, mas o microsserviço pode não ter a rota solicitada. Verifique se o endpoint existe no microsserviço.
- **Consul mostra múltiplas instâncias mas gateway não encontra:** o gateway usa descoberta automática inteligente que funciona independentemente do Consul.
- **`Invalid URL path: ensure the path starts with '/v1/'` no `localhost:8500`:** esse endereço é a interface administrativa do Consul. Use `http://localhost:8080` para acessar os microsserviços via gateway.
- **Erro 401 Unauthorized ao acessar endpoints:** você não forneceu um token JWT válido ou o token expirou. Faça login novamente usando `POST /auth/login` e use o token retornado no header `Authorization: Bearer <token>`.
- **Token inválido ou expirado:** tokens JWT têm validade de 1 hora. Se o token expirar, faça login novamente para obter um novo token.

---
## Exemplo básico de README.md

## Nome do projeto

## Equipe

- Nome do Projeto: **[preencher com o nome definido pelo professor]**  
- Integrantes:
  - Nome 1 – @usuario1
  - Nome 2 – @usuario2
  - Nome 3 – @usuario3
  - Nome 4 – @usuario4
---

## Contexto Comercial

Descrever o **cenário de negócio** escolhido pela equipe (ex.: sistema de pedidos, reservas, pagamentos, catálogo de produtos, etc.).

---

## Stack Tecnológica

- **Linguagem de Programação:** [Java, Python, Node.js, Go, C# …]  
- **Ferramentas de Integração:** [Spring Cloud Gateway e Consul]  
