from app.security import create_access_token, decode_access_token, hash_password, verify_password

def test_password_hash_verifies_only_correct_password():
    hashed = hash_password("correct-horse-battery")
    assert verify_password("correct-horse-battery", hashed)
    assert not verify_password("wrong-password", hashed)

def test_access_token_round_trip():
    token = create_access_token("user-123")
    assert decode_access_token(token) == "user-123"

def test_invalid_token_is_rejected():
    assert decode_access_token("not-a-token") is None
