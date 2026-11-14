from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.db import get_db
from app.models import Station
from app.schemas import StationOut

router = APIRouter(prefix="/stations", tags=["stations"])

@router.get("", response_model=list[StationOut])
def list_stations(region: str | None = Query(default=None), db: Session = Depends(get_db)):
    query = select(Station).where(Station.is_active.is_(True))
    if region:
        if region not in {"fergana", "andijan", "namangan"}:
            raise HTTPException(422, "Unsupported region")
        query = query.where(Station.region == region)
    return list(db.scalars(query.order_by(Station.name)).all())

@router.get("/{station_id}", response_model=StationOut)
def get_station(station_id: str, db: Session = Depends(get_db)):
    station = db.get(Station, station_id)
    if not station or not station.is_active:
        raise HTTPException(404, "Station not found")
    return station
