import secrets
from fastapi import APIRouter, Depends, Header, HTTPException, Query
from geoalchemy2.elements import WKTElement
from sqlalchemy import func, select
from sqlalchemy.orm import Session
from app.config import settings
from app.db import get_db
from app.models import Station
from app.schemas import NearbyStationOut, StationCreate, StationOut

router = APIRouter(prefix="/stations", tags=["stations"])

def require_admin(x_admin_key: str | None = Header(default=None)):
    if not settings.admin_api_key or not x_admin_key or not secrets.compare_digest(x_admin_key, settings.admin_api_key):
        raise HTTPException(403, "Valid admin API key required")

@router.post("", response_model=StationOut, status_code=201, dependencies=[Depends(require_admin)])
def create_station(payload: StationCreate, db: Session = Depends(get_db)):
    station = Station(**payload.model_dump(),
        location=WKTElement(f"POINT({payload.longitude} {payload.latitude})", srid=4326))
    db.add(station)
    db.commit()
    db.refresh(station)
    return station

@router.get("/nearby", response_model=list[NearbyStationOut])
def nearby_stations(latitude: float = Query(ge=-90, le=90),
                    longitude: float = Query(ge=-180, le=180),
                    radius_meters: int = Query(default=10000, ge=100, le=50000),
                    db: Session = Depends(get_db)):
    point = func.ST_SetSRID(func.ST_MakePoint(longitude, latitude), 4326).cast(Station.location.type)
    distance = func.ST_Distance(Station.location, point).label("distance_meters")
    query = select(Station, distance).where(
        Station.is_active.is_(True), func.ST_DWithin(Station.location, point, radius_meters)
    ).order_by(distance).limit(100)
    rows = db.execute(query).all()
    return [{**StationOut.model_validate(station).model_dump(), "distance_meters": round(meters, 1)}
            for station, meters in rows]

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
