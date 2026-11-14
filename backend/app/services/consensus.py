from datetime import datetime, timedelta, timezone
from math import exp
from app.config import settings
from app.services.geo import distance_meters

def calculate_status(reports, station_lat: float, station_lon: float) -> dict:
    """Freshness/proximity weighted consensus; raw report count is not proof."""
    now = datetime.now(timezone.utc)
    cutoff = now - timedelta(minutes=settings.report_max_age_minutes)
    weights = {"open": 0.0, "closed": 0.0, "no_fuel": 0.0}
    used = []
    for report in reports:
        created = report.created_at
        if created.tzinfo is None:
            created = created.replace(tzinfo=timezone.utc)
        if created < cutoff:
            continue
        distance = distance_meters(station_lat, station_lon, report.latitude, report.longitude)
        if distance > settings.report_radius_meters:
            continue
        age = max(0.0, (now - created).total_seconds() / 60)
        weight = exp(-age / 45) * max(0.15, 1 - distance / (settings.report_radius_meters * 1.25))
        weights[report.status] += weight
        used.append(report)
    total = sum(weights.values())
    if not used or total == 0:
        return {"status": "unknown", "confidence": 0.0, "report_count": 0, "last_updated": None}
    winner = max(weights, key=weights.get)
    confidence = weights[winner] / total
    status = winner if confidence >= settings.report_min_confidence else "unknown"
    newest = max(used, key=lambda item: item.created_at).created_at
    return {"status": status if status != "no_fuel" else "closed",
            "confidence": round(confidence, 3), "report_count": len(used), "last_updated": newest}
