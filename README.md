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

## Exemplos de uso via API Gateway

Os exemplos abaixo assumem que todos os serviços estão rodando. O gateway automaticamente encontra os microsserviços independente das portas que eles escolherem.

**URLs dos Swaggers (descobertas automaticamente):**
- `http://localhost:8080/ms-kotlin/` → Swagger com catálogo de produtos e gestão de clientes
- `http://localhost:8080/ms-python/` → Swagger com gestão de pedidos e pagamentos

### ms-kotlin — Catálogo de Produtos e Gestão de Clientes

#### Produtos

##### Inserir um produto

```bash
curl -X POST "http://localhost:8080/ms-kotlin/produto" \
  -H "Content-Type: application/json" \
  -d '[{
    "codigoProduto": 9001,
    "descricao": "Mouse sem fio",
    "preco": 199.9,
    "codGruEst": 300
  }]'
```

##### Consultar um produto

```bash
curl "http://localhost:8080/ms-kotlin/produto/1"
```

##### Listar todos os produtos

```bash
curl "http://localhost:8080/ms-kotlin/produto"
```

#### Clientes

##### Criar um cliente

```bash
curl -X POST "http://localhost:8080/ms-kotlin/cliente" \
  -H "Content-Type: application/json" \
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
curl "http://localhost:8080/ms-kotlin/cliente/1"
```

##### Buscar cliente por CPF

```bash
curl "http://localhost:8080/ms-kotlin/cliente/cpf/12345678901"
```

##### Listar todos os clientes

```bash
curl "http://localhost:8080/ms-kotlin/cliente"
```

##### Listar apenas clientes ativos

```bash
curl "http://localhost:8080/ms-kotlin/cliente?ativos=true"
```

### ms-python — Gestão de Pedidos e Pagamentos

#### Pedidos

##### Inserir um pedido

```bash
curl -X POST "http://localhost:8080/ms-python/order" \
  -H "Content-Type: application/json" \
  -d '{
    "productCode": 9001,
    "tableNumber": 12,
    "quantity": 3
  }'
```

##### Consultar pedidos por número

```bash
curl "http://localhost:8080/ms-python/order/1001"
```

#### Pagamentos

##### Criar um pagamento

```bash
curl -X POST "http://localhost:8080/ms-python/payment" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNumber": 1001,
    "amount": 599.70,
    "paymentMethod": "PIX"
  }'
```

##### Buscar pagamento por ID

```bash
curl "http://localhost:8080/ms-python/payment/1"
```

##### Buscar pagamentos de um pedido

```bash
curl "http://localhost:8080/ms-python/payment/order/1001"
```

##### Atualizar status do pagamento

```bash
curl -X PUT "http://localhost:8080/ms-python/payment/1/status" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "COMPLETED",
    "transactionId": "TXN-123456789"
  }'
```

##### Listar todos os pagamentos

```bash
curl "http://localhost:8080/ms-python/payment"
```

**Métodos de pagamento disponíveis:** `CREDIT_CARD`, `DEBIT_CARD`, `PIX`, `CASH`, `DIGITAL_WALLET`

**Status de pagamento disponíveis:** `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED`

## Problemas comuns

- **`connect ECONNREFUSED 127.0.0.1:8080` ao usar Postman/cURL:** certifique-se de que o gateway está ativo (passo 3). O gateway deve estar rodando para responder na porta `8080`.
- **Gateway retorna 503 Service Unavailable:** os microsserviços não estão rodando ou não são encontrados. Verifique se ms-kotlin e ms-python estão ativos nos terminais.
- **Erro 404 ao acessar endpoints:** o gateway está funcionando, mas o microsserviço pode não ter a rota solicitada. Verifique se o endpoint existe no microsserviço.
- **Consul mostra múltiplas instâncias mas gateway não encontra:** o gateway usa descoberta automática inteligente que funciona independentemente do Consul.
- **`Invalid URL path: ensure the path starts with '/v1/'` no `localhost:8500`:** esse endereço é a interface administrativa do Consul. Use `http://localhost:8080` para acessar os microsserviços via gateway.

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
