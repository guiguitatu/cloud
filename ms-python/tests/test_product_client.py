import unittest

from orders.product_client import ProductGatewayClient, ServiceDiscoveryError


class DummyResponse:
    def __init__(self, status_code: int = 200, json_data=None):
        self.status_code = status_code
        self._json_data = json_data

    def raise_for_status(self) -> None:
        if self.status_code >= 400:
            raise RuntimeError(f"HTTP {self.status_code}")

    def json(self):
        return self._json_data


class DummySession:
    def __init__(self):
        self.calls = []
        self.product_calls = []

    def get(self, url, params=None, timeout=5):
        self.calls.append({"url": url, "params": params, "timeout": timeout})
        if "health/service" in url:
            return DummyResponse(json_data=[])
        if "/ms-kotlin/produto/codigo/" in url:
            self.product_calls.append(url)
            return DummyResponse(json_data={"codigo": 11111})
        raise AssertionError(f"Unexpected URL: {url}")


class DummySessionNoFallback(DummySession):
    def get(self, url, params=None, timeout=5):
        self.calls.append({"url": url, "params": params, "timeout": timeout})
        if "health/service" in url:
            return DummyResponse(json_data=[])
        raise AssertionError("Product endpoint should not be called when no fallback is configured")


class ProductGatewayClientFallbackTests(unittest.TestCase):
    def test_uses_fallback_gateway_when_consul_returns_empty(self):
        session = DummySession()
        client = ProductGatewayClient(session=session, fallback_base_url="http://fallback:1234")

        product = client.get_product_by_code(11111)

        self.assertEqual({"codigo": 11111}, product)
        self.assertEqual(
            "http://fallback:1234/ms-kotlin/produto/codigo/11111",
            session.product_calls[-1],
        )
        # Apenas uma chamada ao Consul, demais requisições devem reutilizar o fallback cacheado.
        client.get_product_by_code(22222)
        self.assertEqual(1, sum("health/service" in call["url"] for call in session.calls))

    def test_raises_error_when_no_instances_and_no_fallback(self):
        session = DummySessionNoFallback()
        client = ProductGatewayClient(session=session)

        with self.assertRaises(ServiceDiscoveryError):
            client.get_product_by_code(12345)


if __name__ == "__main__":
    unittest.main()
