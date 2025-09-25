import os
import random
from typing import Any

_DEFAULT_FALLBACK = object()

import requests


class ServiceDiscoveryError(RuntimeError):
    """Raised when Consul discovery cannot return a healthy service instance."""


class ProductGatewayClient:
    """Resolve o API Gateway via Consul e faz chamadas ao catálogo de produtos."""

    def __init__(
        self,
        consul_addr: str | None = None,
        gateway_service: str | None = None,
        session: requests.Session | None = None,
        fallback_base_url: str | None | object = _DEFAULT_FALLBACK,
    ) -> None:
        self._consul_addr = consul_addr or os.getenv("CONSUL_HTTP_ADDR", "http://localhost:8500")
        self._gateway_service = gateway_service or os.getenv("GATEWAY_SERVICE_NAME", "api-gateway")
        self._session = session or requests.Session()
        if fallback_base_url is _DEFAULT_FALLBACK:
            env_fallback = os.getenv("GATEWAY_BASE_URL")
            fallback_base_url = env_fallback or "http://127.0.0.1:8080"
        self._fallback_base_url = fallback_base_url or None
        self._gateway_base_url: str | None = None

    def _discover_gateway(self) -> str:
        """Consulta o Consul por uma instância saudável do API Gateway."""
        try:
            response = self._session.get(
                f"{self._consul_addr}/v1/health/service/{self._gateway_service}",
                params={"passing": True},
                timeout=5,
            )
            response.raise_for_status()
            payload = response.json()
        except requests.RequestException as exc:
            if self._fallback_base_url:
                self._gateway_base_url = self._fallback_base_url
                return self._gateway_base_url
            raise ServiceDiscoveryError(
                f"Não foi possível consultar o Consul para {self._gateway_service}"
            ) from exc
        if not payload:
            if self._fallback_base_url:
                self._gateway_base_url = self._fallback_base_url
                return self._gateway_base_url
            raise ServiceDiscoveryError(
                f"Nenhuma instância saudável encontrada para {self._gateway_service}"
            )

        service_entry = random.choice(payload)
        service = service_entry.get("Service", {})
        address = service.get("Address") or service_entry.get("Node", {}).get("Address")
        port = service.get("Port")
        if not address or not port:
            if self._fallback_base_url:
                self._gateway_base_url = self._fallback_base_url
                return self._gateway_base_url
            raise ServiceDiscoveryError("Resposta do Consul incompleta para o gateway")

        self._gateway_base_url = f"http://{address}:{port}"
        return self._gateway_base_url

    def _get_gateway_base_url(self) -> str:
        if self._gateway_base_url:
            return self._gateway_base_url
        return self._discover_gateway()

    def get_product_by_code(self, product_code: int) -> dict[str, Any]:
        """Obtém o produto via rota do gateway que aponta para o ms-kotlin."""
        base_url = self._get_gateway_base_url()
        response = self._session.get(
            f"{base_url}/ms-kotlin/produto/codigo/{product_code}",
            timeout=5,
        )

        if response.status_code == 404:
            raise ValueError("Produto não encontrado")

        response.raise_for_status()
        return response.json()

    def clear_cache(self) -> None:
        """Permite limpar a URL cacheada do gateway (útil em testes)."""
        self._gateway_base_url = None
