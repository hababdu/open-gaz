from app.services.geo import distance_meters

def test_same_point_is_zero_distance():
    assert distance_meters(40.0, 71.0, 40.0, 71.0) == 0

def test_distance_is_symmetric_and_positive():
    forward = distance_meters(40.0, 71.0, 40.1, 71.0)
    reverse = distance_meters(40.1, 71.0, 40.0, 71.0)
    assert forward > 10_000
    assert abs(forward - reverse) < 0.01
