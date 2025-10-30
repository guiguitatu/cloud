## Como executar o ambiente

### Pré-requisitos

- [Docker](https://www.docker.com/) e Docker Compose para subir o Consul.
- JDK 17 ou superior (o projeto Kotlin usa Spring Boot 3.5).
- Python 3.10 ou superior e `pip`.

> Todas as instruções assumem que os comandos são executados a partir da raiz do repositório (`cloud/`).

### 1. Iniciar o serviço de infraestrutura (Consul)

```bash
docker compose up -d consul
```

O Consul ficará disponível em `http://localhost:8500/ui`. Ele roda isolado em um contêiner Docker e expõe apenas a porta `8500`; nenhuma aplicação passará a responder em `http://localhost:8080` até que o API Gateway seja iniciado nos passos seguintes. Mantenha o Consul em execução enquanto iniciar os microsserviços. Para acompanhar os logs, utilize `docker compose logs -f consul`.

> **Quer subir o gateway ao mesmo tempo?** Chamar `docker compose up -d api-gateway` também iniciará automaticamente o serviço `consul` (graças ao `depends_on`) antes de liberar o gateway. Veja o passo 4.2 para mais detalhes e opções de logs.

### 2. Subir o microsserviço Kotlin (`ms-kotlin`)

Em outro terminal:

```bash
cd ms-kotlin
./mvnw spring-boot:run
# # Windows PowerShell (execute uma vez)
# .\mvnw.cmd spring-boot:run
```

O Spring Boot utiliza porta dinâmica (definida para `0`), então verifique o log para saber a porta exposta ou consulte o Consul para descobrir o endereço registrado. Por padrão o serviço utiliza um banco SQLite local em `catalogo.db`; caso deseje apontar para outro caminho, defina a variável de ambiente `SQLITE_DB_PATH` antes de executar o serviço. Na primeira execução o serviço cria a tabela `produtos` automaticamente e popula três itens de exemplo.

> **Dica (Windows):** Para utilizar outro caminho de banco, execute `setx SQLITE_DB_PATH "C:\\caminho\\catalogo.db"` antes de iniciar o serviço com `.\mvnw.cmd spring-boot:run`.

### 3. Subir o microsserviço Python (`ms-python`)

Em um novo terminal, crie e ative um ambiente virtual (opcional, mas recomendado), instale as dependências e execute o serviço:

```bash
cd ms-python
python -m venv .venv
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate  # Windows PowerShell
pip install -r requirements.txt
python main.py
```

Por padrão o serviço sobe na porta `8000` e se registra automaticamente no Consul. Utilize `CTRL+C` para finalizá-lo; o `atexit` garante o deregistro no Consul.

Após o registro, a documentação Swagger pode ser acessada via gateway em `http://localhost:8080/ms-python/` (ou diretamente, usando a porta impressa no log, em `http://localhost:<porta>/ms-python/`).

### 4. Subir o API Gateway (`api-gateway`)

O gateway expõe um ponto de entrada único (`http://localhost:8080`) e faz o balanceamento chamando os microsserviços registrados no Consul. **Esse passo precisa ficar ativo**; se o processo não estiver em execução, qualquer requisição ao `localhost:8080` retornará erro de conexão recusada.

#### 4.1 Executar localmente (sem Docker)

```bash
cd api-gateway
./mvnw spring-boot:run
# # Windows PowerShell (execute uma vez)
# .\mvnw.cmd spring-boot:run
```

Ao iniciar, o log deve indicar `Tomcat started on port(s): 8080` (ou mensagem equivalente do servidor embutido). Você também pode validar que o gateway está no ar executando, em outro terminal:

```bash
curl http://localhost:8080/actuator/health
```

Uma resposta `{"status":"UP"}` confirma que o processo está aceitando conexões.

#### 4.2 Executar o gateway via Docker Compose

Se preferir manter o gateway lado a lado com o Consul dentro do Docker, utilize o serviço `api-gateway` incluído no `docker-compose.yml`:

```bash
# inicia gateway e consul de uma vez
docker compose up -d api-gateway

# (opcional) derruba apenas o gateway mantendo o Consul rodando
docker compose stop api-gateway
```

Ao subir o gateway, o Compose garante que o contêiner `consul` esteja ativo antes da aplicação iniciar. Use `docker compose logs -f api-gateway` para acompanhar a inicialização. O contêiner já exporta a porta `8080` para o host; assim que o log indicar `Tomcat started on port(s): 8080`, o gateway estará acessível em `http://localhost:8080`.

> O contêiner define automaticamente `SPRING_CLOUD_CONSUL_HOST=consul` para localizar o serviço de registro e usa `SERVICE_ADDRESS=host.docker.internal` como endereço publicado no Consul. Caso seu ambiente Docker não ofereça essa entrada DNS (ex.: Docker Engine em Linux sem suporte), altere a variável `SERVICE_ADDRESS` no `docker-compose.yml` para o IP do host ou outro endereço acessível.

## Exemplos de uso via API Gateway

Os exemplos abaixo assumem que o gateway está disponível em `http://localhost:8080` e os microsserviços já estão registrados no Consul. O Consul continua acessível em `http://localhost:8500/ui` apenas para interface administrativa.

### ms-kotlin — catálogo de produtos

#### Inserir um produto

```bash
curl -X POST "http://localhost:8080/ms-kotlin/produto" \
  -H "Content-Type: application/json" \
  -d '[{
    "codigoProduto": 9001,
    "descricao": "Mouse sem fio",
    "preco": 199.9,
    "codGruEst": 42
  }]'
```

#### Consultar um produto

```bash
curl "http://localhost:8080/ms-kotlin/produto/1"
```

### ms-python — gestão de pedidos

#### Inserir um pedido

```bash
curl -X POST "http://localhost:8080/ms-python/order" \
  -H "Content-Type: application/json" \
  -d '{
    "productCode": 9001,
    "tableNumber": 12,
    "quantity": 3
  }'
```

#### Consultar pedidos por número

```bash
curl "http://localhost:8080/ms-python/order/1001"
```

## Problemas comuns

- **`connect ECONNREFUSED 127.0.0.1:8080` ao usar Postman/cURL:** certifique-se de que o passo 4 (API Gateway) está ativo em um terminal. Sem ele, o Consul não encaminha chamadas por conta própria.
- **"Docker não deveria expor o Consul em 8080?"**: o `docker compose up -d consul` inicia somente o Consul, que publica a interface HTTP na porta `8500`. A porta `8080` pertence ao API Gateway (Spring Cloud Gateway); execute o passo 4 para ter um serviço respondendo nesse endereço.
- **`Invalid URL path: ensure the path starts with '/v1/'` no `localhost:8500`:** esse endereço é apenas a interface administrativa do Consul. Utilize o gateway em `http://localhost:8080` para acessar os microsserviços.

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
