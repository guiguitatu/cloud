import json
import os
import sys
import tempfile
import threading
import time
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

from sqlalchemy import text

# Garante que o pacote "orders" fique disponível para os testes.
PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_PATH = PROJECT_ROOT / "src"
if str(SRC_PATH) not in sys.path:
    sys.path.insert(0, str(SRC_PATH))

from orders.product_client import ProductGatewayClient, ServiceDiscoveryError  # noqa: E402


PRODUCTS_FIXTURE = {
    101: {
        "id": 1,
        "codigoProduto": 101,
        "descricao": "Café especial em grãos 1kg",
        "preco": 74.9,
        "codGruEst": 100,
    },
    202: {
        "id": 2,
        "codigoProduto": 202,
        "descricao": "Caixa de barras de cereal sortidas",
        "preco": 39.5,
        "codGruEst": 200,
    },
}


class CatalogRequestHandler(BaseHTTPRequestHandler):
    def do_GET(self):  # noqa: N802 - assinatura definida pela stdlib
        if self.path.startswith("/ms-kotlin/produto/codigo/"):
            try:
                product_code = int(self.path.rsplit("/", 1)[-1])
            except ValueError:
                self.send_error(400, "Código inválido")
                return

            product = PRODUCTS_FIXTURE.get(product_code)
            if product is None:
                self.send_error(404, "Produto não encontrado")
                return

            body = json.dumps(product).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        self.send_error(404, "Rota não encontrada")

    def log_message(self, format, *args):  # noqa: A003 - método da stdlib
        # Evita poluição do output dos testes.
        return


def start_catalog_server():
    server = HTTPServer(("127.0.0.1", 0), CatalogRequestHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    time.sleep(0.05)
    return server, thread


class ProductGatewayClientIntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server, cls.thread = start_catalog_server()
        host, port = cls.server.server_address
        cls.base_url = f"http://{host}:{port}"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.thread.join(timeout=5)
        cls.server.server_close()

    def test_uses_fallback_when_consul_is_unavailable(self):
        client = ProductGatewayClient(
            consul_addr="http://127.0.0.1:59999",  # endereço inválido para simular falha
            fallback_base_url=self.base_url,
        )

        product = client.get_product_by_code(101)

        self.assertEqual(101, product["codigoProduto"])
        self.assertEqual("Café especial em grãos 1kg", product["descricao"])

    def test_raises_value_error_for_unknown_product(self):
        client = ProductGatewayClient(
            consul_addr="http://127.0.0.1:59999",
            fallback_base_url=self.base_url,
        )

        with self.assertRaises(ValueError):
            client.get_product_by_code(999)

    def test_raises_error_when_no_fallback_available(self):
        client = ProductGatewayClient(consul_addr="http://127.0.0.1:59999", fallback_base_url=None)

        with self.assertRaises(ServiceDiscoveryError):
            client.get_product_by_code(101)


class OrderServiceIntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server, cls.thread = start_catalog_server()
        host, port = cls.server.server_address
        base_url = f"http://{host}:{port}"

        cls._tmpdir = tempfile.TemporaryDirectory()
        cls._db_path = Path(cls._tmpdir.name) / "orders.db"

        os.environ["DATABASE_URL"] = f"sqlite:///{cls._db_path}"
        os.environ["SQLALCHEMY_ECHO"] = "0"
        os.environ["GATEWAY_BASE_URL"] = base_url
        os.environ["CONSUL_HTTP_ADDR"] = "http://127.0.0.1:59999"

        import importlib

        database_module = importlib.import_module("orders.database")
        model_module = importlib.import_module("orders.model")
        service_module = importlib.import_module("orders.service")

        cls.database_module = importlib.reload(database_module)
        cls.model_module = importlib.reload(model_module)
        cls.service_module = importlib.reload(service_module)

        cls.model_module.table_registry.metadata.create_all(bind=cls.database_module.engine)

    @classmethod
    def tearDownClass(cls):
        cls.database_module.engine.dispose()
        cls.server.shutdown()
        cls.thread.join(timeout=5)
        cls.server.server_close()
        cls._tmpdir.cleanup()

    def setUp(self):
        # Limpa o estado a cada teste.
        with self.database_module.SessionLocal() as session:
            session.execute(text("DELETE FROM orders"))
            session.commit()
        self.service_module.product_client.clear_cache()

    def test_creates_order_populating_details_from_catalog(self):
        order_request = self.model_module.OrderRequest(productCode=101, tableNumber=12, quantity=2)

        created = self.service_module.create_order_service(order_request)

        self.assertEqual(1, created.orderNumber)
        self.assertEqual(12, created.tableNumber)
        self.assertEqual(2, created.quantity)
        self.assertEqual(101, created.productCode)
        self.assertEqual("Café especial em grãos 1kg", created.description)
        self.assertEqual(100, created.codGruEst)

    def test_creates_orders_with_incremental_numbers(self):
        request_a = self.model_module.OrderRequest(productCode=101, tableNumber=1, quantity=1)
        request_b = self.model_module.OrderRequest(productCode=202, tableNumber=2, quantity=3)

        created_a = self.service_module.create_order_service(request_a)
        created_b = self.service_module.create_order_service(request_b)

        self.assertEqual(1, created_a.orderNumber)
        self.assertEqual(2, created_b.orderNumber)
        self.assertEqual("Caixa de barras de cereal sortidas", created_b.description)
        self.assertEqual(200, created_b.codGruEst)


if __name__ == "__main__":
    unittest.main()
