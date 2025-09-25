from src.orders.model import OrderRequest, OrderResponse, OrderModel
from sqlalchemy import select, func
from src.orders.database import SessionLocal


def get_orders_service(order_number: int) -> list[OrderResponse]: 
    session = SessionLocal()
    orders = session.scalars(
        select(OrderModel).where(
            OrderModel.orderNumber == order_number
        )
    ).all()
    cast_orders = [OrderResponse.model_validate(order) for order in orders]
    return cast_orders


def create_order_service(order: OrderRequest) -> OrderResponse: 
    session = SessionLocal()
    order_number = (session.scalar(
        select(func.max(OrderModel.orderNumber))
    ) or 0) + 1
    order_db = OrderModel(
        orderNumber=order_number,
        **order.model_dump())
    session.add(order_db)
    session.commit()
    session.refresh(order_db)
    return OrderResponse.model_validate(order_db)
