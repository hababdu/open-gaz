from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from app.services.consensus import calculate_status

def report(status, minutes_ago=1, latitude=40.0, longitude=71.0):
    return SimpleNamespace(status=status, created_at=datetime.now(timezone.utc)-timedelta(minutes=minutes_ago),
                           latitude=latitude, longitude=longitude)

def test_no_reports_means_unknown():
    result = calculate_status([], 40.0, 71.0)
    assert result["status"] == "unknown"
    assert result["report_count"] == 0

def test_fresh_consistent_reports_produce_status():
    result = calculate_status([report("open"), report("open", 2), report("closed", 3)], 40.0, 71.0)
    assert result["status"] == "open"
    assert result["confidence"] > 0.55

def test_conflicting_reports_remain_unknown():
    result = calculate_status([report("open"), report("closed")], 40.0, 71.0)
    assert result["status"] == "unknown"

def test_stale_reports_are_ignored():
    result = calculate_status([report("open", 200)], 40.0, 71.0)
    assert result["status"] == "unknown"
