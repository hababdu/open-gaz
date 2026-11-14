from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import select
from app.config import settings
from app.db import SessionLocal
from app.models import FuelType
from app.routers import auth, comments, fuels, health, reports, stations, status

DEFAULT_FUELS = [("cng", "Metan"), ("lpg", "Propan"), ("gasoline", "Benzin"),
                 ("diesel", "Dizel"), ("other", "Boshqa")]

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Database schema is managed by Alembic; only seed reference data here.
    with SessionLocal() as db:
        for fuel_id, name in DEFAULT_FUELS:
            if db.scalar(select(FuelType).where(FuelType.id == fuel_id)) is None:
                db.add(FuelType(id=fuel_id, name=name))
        db.commit()
    yield

app = FastAPI(title=settings.app_name, version="0.4.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=settings.allowed_origins,
    allow_credentials=False, allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "Authorization", "X-Admin-Key"])

app.include_router(health.router)
app.include_router(auth.router, prefix="/api/v1")
app.include_router(fuels.router, prefix="/api/v1")
app.include_router(stations.router, prefix="/api/v1")
app.include_router(comments.router, prefix="/api/v1")
app.include_router(reports.router, prefix="/api/v1")
app.include_router(status.router, prefix="/api/v1")
