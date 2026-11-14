from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.db import get_db
from app.models import FuelType, QueueReport, Station, StatusReport
from app.schemas import FuelStatusOut, QueueEstimateOut
from app.services.consensus import calculate_status
from app.services.queue import QUEUE_MAX_AGE_MINUTES, estimate_queue

router = APIRouter(prefix="/stations", tags=["station status"])

@router.get("/{station_id}/status", response_model=list[FuelStatusOut])
def station_status(station_id: str, db: Session = Depends(get_db)):
    station = db.get(Station, station_id)
    if not station or not station.is_active:
        raise HTTPException(404, "Station not found")
    reports = list(db.scalars(select(StatusReport).where(StatusReport.station_id == station_id)).all())
    fuels = db.scalars(select(FuelType).where(FuelType.is_active.is_(True)).order_by(FuelType.id)).all()
    result = []
    for fuel in fuels:
        fuel_reports = [r for r in reports if r.fuel_type_id == fuel.id]
        result.append({"fuel_type_id": fuel.id, **calculate_status(
            fuel_reports, station.latitude, station.longitude)})
    return result

@router.get("/{station_id}/queue/{fuel_type_id}", response_model=QueueEstimateOut)
def queue_estimate(station_id: str, fuel_type_id: str, db: Session = Depends(get_db)):
    station = db.get(Station, station_id)
    if not station or not station.is_active:
        raise HTTPException(404, "Station not found")
    if not db.get(FuelType, fuel_type_id):
        raise HTTPException(404, "Fuel type not found")
    cutoff = datetime.now(timezone.utc) - timedelta(minutes=QUEUE_MAX_AGE_MINUTES)
    reports = list(db.scalars(select(QueueReport).where(
        QueueReport.station_id == station_id,
        QueueReport.fuel_type_id == fuel_type_id,
        QueueReport.created_at >= cutoff,
    )).all())
    estimate = estimate_queue(reports, station.latitude, station.longitude)
    return {"fuel_type_id": fuel_type_id, "estimated_cars": estimate["estimated_cars"],
            "report_count": estimate["report_count"], "last_updated": estimate["last_updated"]}
