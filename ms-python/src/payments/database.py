from src.orders.database import engine, SessionLocal

# Reutiliza o mesmo engine do banco de pedidos
__all__ = ['engine', 'SessionLocal']

