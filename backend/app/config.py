from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    app_name: str = "Open Gaz API"
    app_env: str = "development"
    database_url: str = "postgresql+psycopg://open_gaz:local-dev-password@localhost:5432/open_gaz"
    admin_api_key: str = ""
    jwt_secret_key: str = "development-only-change-this-secret-before-deploying"
    jwt_algorithm: str = "HS256"
    access_token_minutes: int = 60 * 24 * 7
    report_max_age_minutes: int = 90
    report_min_confidence: float = 0.55
    report_radius_meters: int = 1500
    cors_origins: str = "http://localhost:3000,http://localhost:5173"
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    @property
    def allowed_origins(self) -> list[str]:
        return [x.strip() for x in self.cors_origins.split(",") if x.strip()]

@lru_cache
def get_settings() -> Settings:
    return Settings()

settings = get_settings()
