from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from app.services.overview import fuel_summaries
from app.services.queue import estimate_queue

NOW = datetime.now(timezone.utc)
STATION = SimpleNamespace(latitude=40.0, longitude=71.0)

def status(fuel, st, user="u1", ago=1):
    return SimpleNamespace(fuel_type_id=fuel, status=st, reporter_id=user, latitude=40.0, longitude=71.0,
                           created_at=NOW - timedelta(minutes=ago))

def queue(fuel, cars, user="u1", ago=1, lat=40.0):
    return SimpleNamespace(fuel_type_id=fuel, cars_count=cars, reporter_id=user, latitude=lat, longitude=71.0,
                           created_at=NOW - timedelta(minutes=ago))

def test_queue_uses_latest_per_user_and_median():
    reports = [queue("cng", 50, "a", ago=30), queue("cng", 10, "a", ago=2), queue("cng", 20, "b"), queue("cng", 30, "c")]
    result = estimate_queue(reports, 40.0, 71.0)
    assert result["report_count"] == 3
    assert result["estimated_cars"] == 20.0

def test_queue_ignores_old_and_far_reports():
    reports = [queue("cng", 10, "a", ago=100), queue("cng", 10, "b", lat=41.0)]
    result = estimate_queue(reports, 40.0, 71.0)
    assert result["estimated_cars"] is None
    assert result["report_count"] == 0

def test_overview_groups_by_fuel():
    out = fuel_summaries(STATION, [status("cng", "open"), status("lpg", "closed")], [queue("cng", 12)])
    by_fuel = {f["fuel_type_id"]: f for f in out}
    assert by_fuel["cng"]["status"] == "open"
    assert by_fuel["cng"]["queue_cars"] == 12.0
    assert by_fuel["lpg"]["status"] == "closed"
    assert by_fuel["lpg"]["queue_cars"] is None

def test_overview_empty_without_reports():
    assert fuel_summaries(STATION, [], []) == []

def test_overview_route_registered_before_station_id():
    from app.routers.stations import router
    paths = [r.path for r in router.routes if "GET" in getattr(r, "methods", set())]
    assert paths.index("/stations/overview") < paths.index("/stations/{station_id}")
