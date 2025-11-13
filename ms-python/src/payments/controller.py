from http import HTTPStatus
from fastapi import APIRouter, HTTPException
from src.payments.model import PaymentResponse, PaymentRequest, PaymentUpdateRequest
from src.payments.service import (
    create_payment_service,
    get_payment_by_id_service,
    get_payments_by_order_service,
    update_payment_status_service,
    list_all_payments_service
)


router = APIRouter(prefix='/payment', tags=['payment'])


@router.post(
    '/',
    status_code=HTTPStatus.CREATED,
    response_model=PaymentResponse,
    description='Criar um novo pagamento'
)
def create_payment(payment: PaymentRequest) -> PaymentResponse:
    return create_payment_service(payment)


@router.get(
    '/{payment_id}',
    status_code=HTTPStatus.OK,
    response_model=PaymentResponse,
    description='Buscar pagamento por ID'
)
def get_payment_by_id(payment_id: int) -> PaymentResponse:
    return get_payment_by_id_service(payment_id)


@router.get(
    '/order/{order_number}',
    status_code=HTTPStatus.OK,
    response_model=list[PaymentResponse],
    description='Buscar pagamentos por número do pedido'
)
def get_payments_by_order(order_number: int) -> list[PaymentResponse]:
    return get_payments_by_order_service(order_number)


@router.put(
    '/{payment_id}/status',
    status_code=HTTPStatus.OK,
    response_model=PaymentResponse,
    description='Atualizar status do pagamento'
)
def update_payment_status(payment_id: int, update: PaymentUpdateRequest) -> PaymentResponse:
    return update_payment_status_service(payment_id, update)


@router.get(
    '/',
    status_code=HTTPStatus.OK,
    response_model=list[PaymentResponse],
    description='Listar todos os pagamentos'
)
def list_all_payments() -> list[PaymentResponse]:
    return list_all_payments_service()

