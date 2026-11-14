from app.services.consensus import calculate_status
from app.services.queue import estimate_queue


def fuel_summaries(station, status_reports, queue_reports) -> list[dict]:
    """Shahobchaning har yoqilg'i turi bo'yicha holati va navbati (faqat ma'lumoti borlari)."""
    fuels = sorted({r.fuel_type_id for r in status_reports} | {r.fuel_type_id for r in queue_reports})
    result = []
    for fuel in fuels:
        status = calculate_status([r for r in status_reports if r.fuel_type_id == fuel],
                                  station.latitude, station.longitude)
        queue = estimate_queue([r for r in queue_reports if r.fuel_type_id == fuel],
                               station.latitude, station.longitude)
        result.append({
            "fuel_type_id": fuel, "status": status["status"], "confidence": status["confidence"],
            "report_count": status["report_count"], "last_updated": status["last_updated"],
            "queue_cars": queue["estimated_cars"], "queue_report_count": queue["report_count"],
        })
    return result
