from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.db import get_db
from app.models import FuelType

router = APIRouter(prefix="/fuel-types", tags=["fuel types"])

@router.get("")
def list_fuel_types(db: Session = Depends(get_db)):
    fuels = db.scalars(select(FuelType).where(FuelType.is_active.is_(True)).order_by(FuelType.id)).all()
    return [{"id": fuel.id, "name": fuel.name} for fuel in fuels]
