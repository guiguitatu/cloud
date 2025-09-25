from fastapi import HTTPException
from sqlalchemy import select, func

from src.orders.database import SessionLocal
from src.orders.model import OrderModel, OrderRequest, OrderResponse
from src.orders.product_client import ProductGatewayClient, ServiceDiscoveryError

product_client = ProductGatewayClient()


def get_orders_service(order_number: int) -> list[OrderResponse]:
    with SessionLocal() as session:
        orders = session.scalars(
            select(OrderModel).where(
                OrderModel.orderNumber == order_number
            )
        ).all()
        return [OrderResponse.model_validate(order) for order in orders]


def create_order_service(order: OrderRequest) -> OrderResponse:
    try:
        product = product_client.get_product_by_code(order.productCode)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ServiceDiscoveryError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:  # pragma: no cover - fallback genérico
        raise HTTPException(status_code=502, detail="Falha ao consultar catálogo de produtos") from exc

    try:
        cod_gru_est = int(product["codGruEst"])
    except (KeyError, TypeError, ValueError) as exc:
        raise HTTPException(status_code=502, detail="Resposta do catálogo de produtos inválida") from exc

    description = product.get("descricao")

    with SessionLocal() as session:
        order_number = (session.scalar(
            select(func.max(OrderModel.orderNumber))
        ) or 0) + 1

        order_db = OrderModel(
            orderNumber=order_number,
            description=description,
            codGruEst=cod_gru_est,
            **order.model_dump(),
        )
        session.add(order_db)
        session.commit()
        session.refresh(order_db)
        return OrderResponse.model_validate(order_db)
