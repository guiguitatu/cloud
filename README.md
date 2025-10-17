# Repositório de Referência Backend

Este é o **repositório de referência** para a atividade prática da Somativa N1 de **Backend: Cloud Computing**.  
Para a execução da atividade cada equipe deverá criar, `OBRIGATORIAMENTE`, um **fork** deste repositório com o nome da equipe/projeto fornecido pelo professor.

### `Exemplo: E01-AstraBar`

## Integração de microsserviços poliglotas com Service Discovery e Gateway

Esta atividade tem como objetivo a implementação PARCIAL do sistema Cloud Native que está sendo desenvolvido/refatorado ao longo da disciplina, através da criação de **dois microsserviços escritos em diferentes linguagens de programação**, integrados por meio de um **Service Discovery (Consul)** e um **API Gateway (Spring Cloud Gateway ou alternativa)**, e visa avaliar a capacidade da equipe/aluno em projetar e implementar microsserviços funcionais, integrados a um sistema distribuído com base nos conceitos de Arquitetura Cloud Native.

## Orientações gerais

- A equipe deverá desenvolver **pelo menos dois microsserviços**, cada um escrito em uma linguagem de programação distinta (ex: Java + Node.js, Python + C#, etc).
- Implementar uma **API REST funcional** em cada serviço, com ao menos um endpoint GET e um POST.
- Documentar as APIs utilizando Swagger/OpenAPI.
- Integrar os microsserviços ao **Service Discovery (Consul)** e **rotear chamadas HTTP via Gateway**.
- Registrar e desregistrar os serviços corretamente (manualmente ou via autoregistro).
- Utilizar o GitLab para versionamento colaborativo.
- Cada integrante da equipe deverá contribuir com commits autorais e mensagens descritivas no repositório.
- A integração deverá ser validada via requisições de teste com Postman, Insomnia ou equivalente (não será necessário frontend).
- O uso de mensageria não será exigido nesta avaliação.
- O foco principal está na separação dos microsserviços, integração via gateway e discovery, diversidade de tecnologias.
- O repositório deve conter um README.md com os dados da equipe, o contexo comercial, a stack tecnológica e instruções de execução.
---

## Como executar o ambiente

### Pré-requisitos

- [Docker](https://www.docker.com/) e Docker Compose para subir o Consul e o MySQL 8.
- JDK 17 ou superior (o projeto Kotlin usa Spring Boot 3.5).
- Python 3.10 ou superior e `pip`.

> Todas as instruções assumem que os comandos são executados a partir da raiz do repositório (`cloud/`).

### 1. Iniciar os serviços de infraestrutura (Consul e MySQL)

```bash
docker compose up -d consul mysql
```

O Consul ficará disponível em `http://localhost:8500/ui` e o MySQL exposto em `localhost:3306` com o banco `catalogo` e o usuário `catalogo/catalogo`. Mantenha ambos em execução enquanto iniciar os microsserviços. Para acompanhar os logs, utilize `docker compose logs -f consul mysql`.

### 2. Subir o microsserviço Kotlin (`ms-kotlin`)

Em outro terminal:

```bash
cd ms-kotlin
export MYSQL_HOST=localhost        # opcional, valores padrão já apontam para localhost
export MYSQL_PORT=3306             # idem
export MYSQL_DATABASE=catalogo
export MYSQL_USER=catalogo
export MYSQL_PASSWORD=catalogo
./mvnw spring-boot:run
# # Windows PowerShell (execute uma vez)
# setx MYSQL_HOST localhost
# setx MYSQL_PORT 3306
# setx MYSQL_DATABASE catalogo
# setx MYSQL_USER catalogo
# setx MYSQL_PASSWORD catalogo
# .\mvnw.cmd spring-boot:run
```

O Spring Boot utiliza porta dinâmica (definida para `0`), então verifique o log para saber a porta exposta ou consulte o Consul para descobrir o endereço registrado. Na primeira execução o serviço cria a tabela `produtos` automaticamente e popula três itens de exemplo.

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
