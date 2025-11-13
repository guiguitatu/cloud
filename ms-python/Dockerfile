FROM python:3.11-slim

WORKDIR /app

# Instalar dependências do sistema
RUN apt-get update && apt-get install -y \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copiar requirements e instalar dependências Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar código da aplicação
COPY . .

# Criar diretório para o banco de dados SQLite
RUN mkdir -p /app/data

# Variáveis de ambiente padrão
ENV DATABASE_URL=sqlite:///./data/orders.db
ENV PORT=0
ENV SERVICE_ADDRESS=0.0.0.0

# Expor porta (será atribuída dinamicamente)
EXPOSE 8000

# Comando para executar a aplicação
CMD ["python", "main.py"]

