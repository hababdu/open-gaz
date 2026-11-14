import pytest
from app.config import Settings

def test_postgres_urls_get_psycopg_driver():
    assert Settings(database_url="postgres://u:p@h/db?sslmode=require").database_url == \
        "postgresql+psycopg://u:p@h/db?sslmode=require"
    assert Settings(database_url="postgresql://u:p@h/db").database_url == "postgresql+psycopg://u:p@h/db"

def test_explicit_driver_is_kept():
    url = "postgresql+psycopg://u:p@h/db"
    assert Settings(database_url=url).database_url == url

def test_production_rejects_default_jwt_secret():
    with pytest.raises(ValueError):
        Settings(app_env="production")

def test_production_accepts_strong_secret():
    assert Settings(app_env="production", jwt_secret_key="x" * 40).app_env == "production"
