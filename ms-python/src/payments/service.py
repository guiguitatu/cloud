from fastapi import HTTPException
from sqlalchemy import select
from datetime import datetime

from src.orders.database import SessionLocal
from src.payments.model import PaymentModel, PaymentRequest, PaymentResponse, PaymentUpdateRequest, PaymentStatus


def create_payment_service(payment: PaymentRequest) -> PaymentResponse:
    # Validar método de pagamento
    valid_methods = ["CREDIT_CARD", "DEBIT_CARD", "PIX", "CASH", "DIGITAL_WALLET"]
    if payment.paymentMethod.upper() not in valid_methods:
        raise HTTPException(
            status_code=400, 
            detail=f"Método de pagamento inválido. Use: {', '.join(valid_methods)}"
        )
    
    # Validar valor
    if payment.amount <= 0:
        raise HTTPException(status_code=400, detail="O valor do pagamento deve ser maior que zero")

    with SessionLocal() as session:
        # Verificar se já existe pagamento para este pedido
        existing = session.scalar(
            select(PaymentModel).where(
                PaymentModel.orderNumber == payment.orderNumber,
                PaymentModel.status.in_([PaymentStatus.PENDING, PaymentStatus.PROCESSING, PaymentStatus.COMPLETED])
            )
        )
        
        if existing:
            raise HTTPException(
                status_code=409, 
                detail=f"Já existe um pagamento ativo para o pedido {payment.orderNumber}"
            )

        payment_db = PaymentModel(
            orderNumber=payment.orderNumber,
            amount=payment.amount,
            paymentMethod=payment.paymentMethod.upper(),
            status=PaymentStatus.PENDING.value
        )
        session.add(payment_db)
        session.commit()
        session.refresh(payment_db)
        return PaymentResponse.model_validate(payment_db)


def get_payment_by_id_service(payment_id: int) -> PaymentResponse:
    with SessionLocal() as session:
        payment = session.get(PaymentModel, payment_id)
        if not payment:
            raise HTTPException(status_code=404, detail="Pagamento não encontrado")
        return PaymentResponse.model_validate(payment)


def get_payments_by_order_service(order_number: int) -> list[PaymentResponse]:
    with SessionLocal() as session:
        payments = session.scalars(
            select(PaymentModel).where(
                PaymentModel.orderNumber == order_number
            ).order_by(PaymentModel.createdAt.desc())
        ).all()
        return [PaymentResponse.model_validate(payment) for payment in payments]


def update_payment_status_service(payment_id: int, update: PaymentUpdateRequest) -> PaymentResponse:
    valid_statuses = [s.value for s in PaymentStatus]
    if update.status.upper() not in valid_statuses:
        raise HTTPException(
            status_code=400,
            detail=f"Status inválido. Use: {', '.join(valid_statuses)}"
        )

    with SessionLocal() as session:
        payment = session.get(PaymentModel, payment_id)
        if not payment:
            raise HTTPException(status_code=404, detail="Pagamento não encontrado")
        
        payment.status = update.status.upper()
        payment.transactionId = update.transactionId
        payment.updatedAt = datetime.now()
        
        session.commit()
        session.refresh(payment)
        return PaymentResponse.model_validate(payment)


def list_all_payments_service() -> list[PaymentResponse]:
    with SessionLocal() as session:
        payments = session.scalars(
            select(PaymentModel).order_by(PaymentModel.createdAt.desc())
        ).all()
        return [PaymentResponse.model_validate(payment) for payment in payments]

