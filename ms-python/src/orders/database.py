import os

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool


DATABASE_URL = os.getenv('DATABASE_URL', 'sqlite:///./orders.db')
echo_flag = os.getenv('SQLALCHEMY_ECHO', 'true').lower() in {'1', 'true', 'yes', 'on'}

engine_kwargs: dict[str, object] = {
    'echo': echo_flag,
    'future': True,
}

if DATABASE_URL.startswith('sqlite'):
    engine_kwargs['connect_args'] = {'check_same_thread': False}
    if DATABASE_URL.endswith(':memory:') or DATABASE_URL == 'sqlite://':
        engine_kwargs['poolclass'] = StaticPool

engine = create_engine(
    DATABASE_URL,
    **engine_kwargs,
)
SessionLocal = sessionmaker(
    bind=engine,
    autoflush=False,
    autocommit=False,
    future=True,
)
