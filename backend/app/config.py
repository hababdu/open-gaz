from functools import lru_cache
from pydantic import field_validator, model_validator
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
    # Haydovchi qo'shgan shahobcha shuncha turli haydovchi tasdiqlagach "tasdiqlangan" bo'ladi.
    station_verify_threshold: int = 3
    # Shuncha haydovchi "yo'q" desa (va "bor"dan ko'p bo'lsa) community shahobcha yashiriladi.
    station_hide_threshold: int = 3
    # Shahobcha qo'shish va tasdiqlash uchun haydovchi shahobchadan shuncha metr ichida bo'lishi kerak.
    station_vote_radius_meters: int = 500
    max_new_stations_per_day: int = 5
    max_comments_per_10_min: int = 5
    cors_origins: str = "http://localhost:3000,http://localhost:5173"
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    @field_validator("database_url")
    @classmethod
    def _use_psycopg_driver(cls, value: str) -> str:
        """Neon/Render `postgres://` yoki `postgresql://` beradi; biz psycopg 3 drayveridan foydalanamiz."""
        for prefix in ("postgres://", "postgresql://"):
            if value.startswith(prefix):
                return "postgresql+psycopg://" + value[len(prefix):]
        return value

    @model_validator(mode="after")
    def _require_real_secrets_in_production(self):
        if self.app_env == "production":
            if self.jwt_secret_key.startswith("development-only") or len(self.jwt_secret_key) < 32:
                raise ValueError("JWT_SECRET_KEY must be a random string of at least 32 characters in production")
        return self

    @property
    def allowed_origins(self) -> list[str]:
        return [x.strip() for x in self.cors_origins.split(",") if x.strip()]

@lru_cache
def get_settings() -> Settings:
    return Settings()

settings = get_settings()
