from app.main import app
from app.routers.stations import station_out
from types import SimpleNamespace

def test_new_endpoints_are_registered():
    paths = app.openapi()["paths"]
    assert "post" in paths["/api/v1/stations/community"]
    assert "post" in paths["/api/v1/stations/{station_id}/votes"]
    assert {"get", "post"} <= set(paths["/api/v1/stations/{station_id}/comments"])

def station(source="community"):
    return SimpleNamespace(id="s1", name="N", address="", region="fergana", latitude=40.0, longitude=71.0,
                           is_active=True, source=source)

def test_station_out_reports_confirmation_progress():
    out = station_out(station(), {"exists": 2, "missing": 0, "comments": 4})
    assert out["confirmations_count"] == 2
    assert out["confirmations_needed"] == 3
    assert out["is_verified"] is False
    assert out["comments_count"] == 4

def test_station_out_verified_after_enough_confirmations():
    out = station_out(station(), {"exists": 3, "missing": 0, "comments": 0})
    assert out["is_verified"] is True

def test_admin_station_has_no_confirmation_requirement():
    out = station_out(station("admin"), {"exists": 0, "missing": 0, "comments": 0})
    assert out["is_verified"] is True
    assert out["confirmations_needed"] == 0
