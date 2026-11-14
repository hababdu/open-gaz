from datetime import datetime, timedelta, timezone
from uuid import uuid4
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.config import settings
from app.db import get_db
from app.models import FuelType, QueueReport, Station, StatusReport
from app.schemas import QueueReportCreate, ReportAccepted, StatusReportCreate

router = APIRouter(tags=["reports"])

def validate_reporter_location(station: Station, lat: float, lon: float):
    from app.services.geo import distance_meters
    distance = distance_meters(station.latitude, station.longitude, lat, lon)
    if distance > settings.report_radius_meters:
        raise HTTPException(403, "Report location is too far from this station")

def ensure_station_and_fuel(db: Session, station_id: str, fuel_id: str) -> Station:
    station = db.get(Station, station_id)
    if not station or not station.is_active:
        raise HTTPException(404, "Station not found")
    if not db.get(FuelType, fuel_id):
        raise HTTPException(422, "Unsupported fuel type")
    return station

@router.post("/stations/{station_id}/reports/status", response_model=ReportAccepted, status_code=201)
def submit_status(station_id: str, payload: StatusReportCreate, db: Session = Depends(get_db)):
    station = ensure_station_and_fuel(db, station_id, payload.fuel_type_id)
    validate_reporter_location(station, payload.latitude, payload.longitude)
    since = datetime.now(timezone.utc) - timedelta(minutes=10)
    previous = db.scalar(select(StatusReport).where(
        StatusReport.station_id == station_id,
        StatusReport.fuel_type_id == payload.fuel_type_id,
        StatusReport.reporter_id == payload.reporter_id,
        StatusReport.created_at >= since,
    ).limit(1))
    if previous:
        raise HTTPException(429, "You have already reported this station and fuel recently")
    report = StatusReport(id=str(uuid4()), station_id=station_id, fuel_type_id=payload.fuel_type_id,
        reporter_id=payload.reporter_id, status=payload.status, latitude=payload.latitude,
        longitude=payload.longitude, note=payload.note)
    db.add(report)
    db.commit()
    db.refresh(report)
    return {"id": report.id, "created_at": report.created_at}

@router.post("/stations/{station_id}/reports/queue", response_model=ReportAccepted, status_code=201)
def submit_queue(station_id: str, payload: QueueReportCreate, db: Session = Depends(get_db)):
    station = ensure_station_and_fuel(db, station_id, payload.fuel_type_id)
    validate_reporter_location(station, payload.latitude, payload.longitude)
    since = datetime.now(timezone.utc) - timedelta(minutes=10)
    previous = db.scalar(select(QueueReport).where(
        QueueReport.station_id == station_id,
        QueueReport.fuel_type_id == payload.fuel_type_id,
        QueueReport.reporter_id == payload.reporter_id,
        QueueReport.created_at >= since,
    ).limit(1))
    if previous:
        raise HTTPException(429, "You have already reported this queue recently")
    report = QueueReport(id=str(uuid4()), station_id=station_id, fuel_type_id=payload.fuel_type_id,
        reporter_id=payload.reporter_id, cars_count=payload.cars_count,
        latitude=payload.latitude, longitude=payload.longitude)
    db.add(report)
    db.commit()
    db.refresh(report)
    return {"id": report.id, "created_at": report.created_at}
