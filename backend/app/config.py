from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    app_name: str = "Open Gaz API"
    app_env: str = "development"
    database_url: str = "postgresql+psycopg://open_gaz:change-me@localhost:5432/open_gaz"
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
