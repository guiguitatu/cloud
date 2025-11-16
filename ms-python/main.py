import atexit, os, socket, time, requests
import uvicorn
import uuid
from fastapi import FastAPI
from fastapi.openapi.docs import get_swagger_ui_html
from fastapi.responses import PlainTextResponse, RedirectResponse
from src.orders.controller import router as orders_router
from src.orders.model import table_registry
from src.payments.controller import router as payments_router
from src.payments.model import table_registry as payments_table_registry


CONSUL = os.getenv("CONSUL_HTTP_ADDR", "http://localhost:8500")
SERVICE_NAME = "ms-python"
SERVICE_ID = f"{SERVICE_NAME}-instance"  # ID consistente para permitir re-registro
API_ROOT = ""


def get_outbound_ip() -> str:
    # Para desenvolvimento local, sempre retorna localhost
    # Em produção, você pode implementar descoberta dinâmica de IP
    service_addr = os.getenv("SERVICE_ADDRESS")
    print(f"SERVICE_ADDRESS env var: {service_addr}")
    if service_addr and service_addr != "0.0.0.0":
        print(f"Using SERVICE_ADDRESS: {service_addr}")
        return service_addr

    # Tentar descobrir o IP da rede Docker
    try:
        # Para containers Docker, tentar descobrir o IP da interface de rede
        import socket
        # Criar um socket UDP e conectar a um host externo para descobrir o IP local
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))  # Google DNS
        ip = s.getsockname()[0]
        s.close()
        print(f"Discovered IP via external connection: {ip}")
        return ip
    except Exception as e:
        print(f"Failed to discover IP via external connection: {e}")
        # Fallback: tentar ler do hostname ou usar host.docker.internal para Docker Desktop
        try:
            import socket
            hostname = socket.gethostname()
            ip = socket.gethostbyname(hostname)
            print(f"Discovered IP via hostname: {ip}")
            if ip != "127.0.0.1":
                return ip
        except Exception as e:
            print(f"Failed to discover IP via hostname: {e}")
            pass
        # Último fallback
        print("Using fallback: host.docker.internal")
        return "host.docker.internal"

def find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("", 0))
        return s.getsockname()[1]


app = FastAPI(
    docs_url=None,
    openapi_url=f"{API_ROOT}/openapi.json",
    redoc_url=None,
    title="ms-python",
)

app.include_router(orders_router, prefix=API_ROOT)
app.include_router(payments_router, prefix=API_ROOT)


@app.get("/", include_in_schema=False)
def swagger_ui():
    return get_swagger_ui_html(
        openapi_url="/openapi.json",
        title="ms-python - Swagger UI",
    )

@app.get("/health")
def health(): return {"status": "UP"}

@app.get("/api/mensagem", response_class=PlainTextResponse)
def mensagem(nome: str = "desenvolvedor"):
    return f"Olá, {nome}! (ms-python)"


def register(addr: str, port: int):
    # Tentar deregistrar qualquer registro anterior primeiro
    deregister()

    payload = {
        "ID": SERVICE_ID,
        "Name": SERVICE_NAME,
        "Address": addr,
        "Port": port,
        "Check": {
            "HTTP": f"http://{addr}:{port}/health",
            "Interval": "10s",
            "Timeout": "2s"
        }
    }
    print(f"Registering service {SERVICE_NAME} at {addr}:{port} with Consul at {CONSUL}")
    try:
        response = requests.put(f"{CONSUL}/v1/agent/service/register", json=payload, timeout=3)
        print(f"Registration response: {response.status_code}")
        if response.status_code != 200:
            print(f"Registration failed: {response.text}")
    except Exception as e:
        print(f"Registration error: {e}")

def deregister():
    try:
        requests.put(f"{CONSUL}/v1/agent/service/deregister/{SERVICE_ID}", timeout=3)
    except: pass



if __name__ == "__main__":
    addr = os.getenv("SERVICE_ADDRESS") or get_outbound_ip()
    port_env = os.getenv("PORT", "0")
    try:
        port = int(port_env)
    except ValueError:
        port = 0
    if port == 0:
        port = find_free_port()
    for _ in range(10):
        try:
            register(addr, port); break
        except Exception: time.sleep(1)
    atexit.register(deregister)
    from src.orders.database import engine
    table_registry.metadata.create_all(bind=engine)
    payments_table_registry.metadata.create_all(bind=engine)
    uvicorn.run(app, host="0.0.0.0", port=port)
