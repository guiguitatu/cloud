from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker


engine = create_engine(
    'sqlite:///./orders.db', 
    echo=True, 
    future=True
)
SessionLocal = sessionmaker(
    bind=engine, 
    autoflush=False, 
    autocommit=False
)
