from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    redis_url: str = "redis://redis:6379/0"
    telegram_bot_token: str = ""
    telegram_chat_id: str = ""
    firebase_credentials_path: str = ""
    upstox_access_token: str = ""
    internal_api_token: str = ""
    device_enrollment_token: str = ""
    minimum_score: int = 80
    cooldown_minutes: int = 15
    daily_alert_cap: int = 10
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()
