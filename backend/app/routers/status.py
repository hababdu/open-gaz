from datetime import datetime, timedelta, timezone
from statistics import median
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.db import get_db
from app.models import FuelType, QueueReport, Station, StatusReport
from app.schemas import FuelStatusOut, QueueEstimateOut
from app.services.consensus import calculate_status
from app.services.geo import distance_meters

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
    cutoff = datetime.now(timezone.utc) - timedelta(minutes=45)
    reports = list(db.scalars(select(QueueReport).where(
        QueueReport.station_id == station_id,
        QueueReport.fuel_type_id == fuel_type_id,
        QueueReport.created_at >= cutoff,
    )).all())
    latest_by_user = {}
    for report in reports:
        if distance_meters(station.latitude, station.longitude, report.latitude, report.longitude) <= 1500:
            current = latest_by_user.get(report.reporter_id)
            if current is None or report.created_at > current.created_at:
                latest_by_user[report.reporter_id] = report
    valid = list(latest_by_user.values())
    estimate = round(float(median([r.cars_count for r in valid])), 1) if valid else None
    newest = max((r.created_at for r in valid), default=None)
    return {"fuel_type_id": fuel_type_id, "estimated_cars": estimate,
            "report_count": len(valid), "last_updated": newest}
