from sqlalchemy.orm import Mapped, mapped_column, registry
from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from enum import Enum


table_registry = registry()


class PaymentStatus(str, Enum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


@table_registry.mapped_as_dataclass
class PaymentModel:
    __tablename__ = 'payments'
    id: Mapped[int] = mapped_column(
        primary_key=True, autoincrement=True, init=False
    )
    orderNumber: Mapped[int] = mapped_column(
        nullable=False
    )
    amount: Mapped[float] = mapped_column(
        nullable=False
    )
    paymentMethod: Mapped[str] = mapped_column(
        nullable=False
    )
    status: Mapped[str] = mapped_column(
        nullable=False, default="PENDING"
    )
    transactionId: Mapped[Optional[str]] = mapped_column(
        nullable=True
    )
    createdAt: Mapped[datetime] = mapped_column(
        nullable=False, default_factory=datetime.now, init=False
    )
    updatedAt: Mapped[Optional[datetime]] = mapped_column(
        nullable=True
    )


class PaymentRequest(BaseModel):
    orderNumber: int
    amount: float
    paymentMethod: str

    class Config:
        from_attributes = True


class PaymentResponse(BaseModel):
    id: int
    orderNumber: int
    amount: float
    paymentMethod: str
    status: str
    transactionId: Optional[str] = None
    createdAt: datetime
    updatedAt: Optional[datetime] = None

    class Config:
        from_attributes = True


class PaymentUpdateRequest(BaseModel):
    status: str
    transactionId: Optional[str] = None

