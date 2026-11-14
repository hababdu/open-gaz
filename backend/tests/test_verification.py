from app.services.verification import is_verified, should_hide

def test_admin_station_is_always_verified():
    assert is_verified("admin", 0, 0, 3)
    assert is_verified("admin", 0, 10, 3)

def test_community_station_needs_threshold_confirmations():
    assert not is_verified("community", 1, 0, 3)
    assert not is_verified("community", 2, 0, 3)
    assert is_verified("community", 3, 0, 3)

def test_conflicting_votes_do_not_verify():
    assert not is_verified("community", 3, 3, 3)
    assert not is_verified("community", 3, 4, 3)
    assert is_verified("community", 4, 3, 3)

def test_community_station_hidden_when_majority_says_missing():
    assert not should_hide("community", 0, 2, 3)
    assert should_hide("community", 0, 3, 3)
    assert not should_hide("community", 3, 3, 3)
    assert should_hide("community", 1, 4, 3)

def test_admin_station_never_hidden_by_votes():
    assert not should_hide("admin", 0, 50, 3)
