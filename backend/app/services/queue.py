from datetime import datetime, timedelta, timezone
from statistics import median
from app.services.geo import distance_meters

QUEUE_MAX_AGE_MINUTES = 45
QUEUE_RADIUS_METERS = 1500


def _aware(dt: datetime) -> datetime:
    return dt if dt.tzinfo else dt.replace(tzinfo=timezone.utc)


def estimate_queue(reports, station_lat: float, station_lon: float, now: datetime | None = None) -> dict:
    """Har haydovchining eng so'nggi navbat hisoboti olinadi, mediana hisoblanadi.
    Eski yoki shahobchadan uzoqdan berilgan hisobotlar hisobga olinmaydi."""
    now = now or datetime.now(timezone.utc)
    cutoff = now - timedelta(minutes=QUEUE_MAX_AGE_MINUTES)
    latest: dict[str, object] = {}
    for report in reports:
        created = _aware(report.created_at)
        if created < cutoff:
            continue
        if distance_meters(station_lat, station_lon, report.latitude, report.longitude) > QUEUE_RADIUS_METERS:
            continue
        current = latest.get(report.reporter_id)
        if current is None or created > _aware(current.created_at):
            latest[report.reporter_id] = report
    valid = list(latest.values())
    estimate = round(float(median([r.cars_count for r in valid])), 1) if valid else None
    newest = max((_aware(r.created_at) for r in valid), default=None)
    return {"estimated_cars": estimate, "report_count": len(valid), "last_updated": newest}
