from datetime import datetime, timezone
from pydantic import BaseModel, Field

class Check(BaseModel):
    name: str
    passed: bool
    detail: str

class Signal(BaseModel):
    id: str
    symbol: str
    direction: str
    setup: str
    spot: float
    score: int = Field(ge=0, le=100)
    contract: str
    premium: float
    spreadPct: float
    optionOi: int
    optionVolume: int
    pivotPrice: float
    gann45: float
    gann90: float
    gann180: float
    gann360: float
    spotStop: float
    premiumStop: float
    vix: float
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    checks: list[Check]

class DeviceRegistration(BaseModel):
    token: str = Field(min_length=20)
