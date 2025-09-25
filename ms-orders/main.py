import atexit, os, socket, time, requests
import uvicorn
import uuid
from fastapi import FastAPI
from fastapi.responses import PlainTextResponse
from src.orders.controller import router as orders_router
from src.orders.model import table_registry


CONSUL = os.getenv("CONSUL_HTTP_ADDR", "http://localhost:8500")
SERVICE_NAME = "ms-orders"
SERVICE_ID = f"{SERVICE_NAME}-{uuid.uuid4()}"


def get_outbound_ip() -> str:
    # descobre o IP “de saída”
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    finally:
        s.close()

def find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("", 0))
        return s.getsockname()[1]


app = FastAPI()

app.include_router(orders_router)


@app.get("/health")
def health(): return {"status": "UP"}

@app.get("/api/mensagem", response_class=PlainTextResponse)
def mensagem(nome: str = "desenvolvedor"):
    return f"Olá, {nome}! (ms-d-python)"


def register(addr: str, port: int):
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
    requests.put(f"{CONSUL}/v1/agent/service/register", json=payload, timeout=3)

def deregister():
    try:
        requests.put(f"{CONSUL}/v1/agent/service/deregister/{SERVICE_ID}", timeout=3)
    except: pass



if __name__ == "__main__":
    addr = os.getenv("SERVICE_ADDRESS") or get_outbound_ip()
    port = int(os.getenv("PORT", "0")) or find_free_port()
    # registra antes de subir (simples p/ dev)
    for _ in range(10):
        try:
            register(addr, port); break
        except Exception: time.sleep(1)
    atexit.register(deregister)
    from src.orders.database import engine
    table_registry.metadata.create_all(bind=engine)
    uvicorn.run(app, host="0.0.0.0", port=port)
