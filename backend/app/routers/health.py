from fastapi import APIRouter
from sqlalchemy import text
from app.db import engine

router = APIRouter(tags=["system"])

@router.get("/health")
def health():
    return {"status": "ok", "service": "open-gaz-api"}

@router.get("/ready")
def ready():
    with engine.connect() as connection:
        connection.execute(text("SELECT 1"))
    return {"status": "ready", "database": "connected"}
