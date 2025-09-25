from http import HTTPStatus
from fastapi import APIRouter
from src.orders.model import OrderResponse, OrderRequest
from src.orders.service import get_orders_service, create_order_service


router = APIRouter(prefix='/order', tags=['order'])


@router.get(
    '/{order_number}',
    status_code=HTTPStatus.OK,
    response_model=list[OrderResponse],
    description='List all orders'
)
def get_orders(order_number: int) -> list[OrderResponse]:
    return get_orders_service(order_number)


@router.post(
    '/',
    status_code=HTTPStatus.CREATED,
    description='Create an order'
)
def create_order(order: OrderRequest) -> OrderResponse:
    return create_order_service(order)
