import atexit, os, socket, time, requests
import uvicorn
import uuid
from fastapi import FastAPI
from fastapi.openapi.docs import get_swagger_ui_html
from fastapi.responses import PlainTextResponse, RedirectResponse
from src.orders.controller import router as orders_router
from src.orders.model import table_registry


CONSUL = os.getenv("CONSUL_HTTP_ADDR", "http://localhost:8500")
SERVICE_NAME = "ms-python"
SERVICE_ID = f"{SERVICE_NAME}-{uuid.uuid4()}"
API_ROOT = "/ms-python"


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


app = FastAPI(
    docs_url=None,
    docs_url=f"{API_ROOT}/",
    openapi_url=f"{API_ROOT}/openapi.json",
    redoc_url=None,
    title="ms-python",
)

app.include_router(orders_router, prefix=API_ROOT)


@app.get("/", include_in_schema=False)
def root() -> RedirectResponse:
    return RedirectResponse(url=f"{API_ROOT}/", status_code=302)


@app.get(f"{API_ROOT}", include_in_schema=False)
def swagger_redirect() -> RedirectResponse:
    return RedirectResponse(url=f"{API_ROOT}/", status_code=302)


@app.get(f"{API_ROOT}/", include_in_schema=False)
def swagger_ui():
    return get_swagger_ui_html(
        openapi_url=f"{API_ROOT}/openapi.json",
        title="ms-python - Swagger UI",
    )

@app.get(f"{API_ROOT}/health")
def health(): return {"status": "UP"}

@app.get(f"{API_ROOT}/api/mensagem", response_class=PlainTextResponse)
def mensagem(nome: str = "desenvolvedor"):
    return f"Olá, {nome}! (ms-python)"


def register(addr: str, port: int):
    payload = {
        "ID": SERVICE_ID,
        "Name": SERVICE_NAME,
        "Address": addr,
        "Port": port,
        "Check": {
            "HTTP": f"http://{addr}:{port}{API_ROOT}/health",
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
    port_env = os.getenv("PORT")
    port = int(port_env) if port_env else 8000
    if port == 0:
        port = find_free_port()
    for _ in range(10):
        try:
            register(addr, port); break
        except Exception: time.sleep(1)
    atexit.register(deregister)
    from src.orders.database import engine
    table_registry.metadata.create_all(bind=engine)
    uvicorn.run(app, host="0.0.0.0", port=port)
