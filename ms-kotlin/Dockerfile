FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Instalar curl para healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/ms-c-0.0.1-SNAPSHOT.jar app.jar

# Criar diretório para o banco de dados SQLite
RUN mkdir -p /app/data

# Expor porta (será atribuída dinamicamente)
EXPOSE 8080

# Variáveis de ambiente padrão
ENV SERVER_PORT=0
ENV SQLITE_DB_PATH=/app/data/catalogo.db

ENTRYPOINT ["java","-jar","/app/app.jar"]

