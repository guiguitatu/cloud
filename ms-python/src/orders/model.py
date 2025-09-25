from sqlalchemy.orm import Mapped, mapped_column, registry
from pydantic import BaseModel
from typing import Optional


table_registry = registry()


@table_registry.mapped_as_dataclass
class OrderModel:
    __tablename__ = 'orders'
    id: Mapped[int] = mapped_column(
        primary_key=True, autoincrement=True, init=False
    )
    orderNumber: Mapped[int] = mapped_column(
        nullable=False
    )
    tableNumber: Mapped[int] = mapped_column(
        nullable=False
    )
    quantity: Mapped[int] = mapped_column(
        nullable=False
    )
    description: Mapped[str] = mapped_column(
        nullable=True
    )
    codGruEst: Mapped[int] = mapped_column(
        nullable=False
    )
    productCode: Mapped[int] = mapped_column(
        nullable=False
    )


class Order(BaseModel):
    orderNumber: int
    tableNumber: int
    quantity: int
    description: Optional[str] = None
    codGruEst: int
    productCode: int

    class Config:
        from_attributes = True

class OrderTable(BaseModel):
    tableNumber: int
    description: Optional[str] = None
    quantity: int

class OrderRequest(BaseModel):
    productCode: int
    tableNumber: int
    quantity: int

class OrderResponse(Order):
    id: int
