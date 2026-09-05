import asyncio
import json
import hmac
from datetime import datetime
from fastapi import FastAPI, Header, HTTPException
from redis.asyncio import Redis
from .config import settings
from .guards import AlertGuard
from .models import DeviceRegistration, Signal
from .notifications import send_fcm, send_telegram

app = FastAPI(title="OptionPulse Scanner API", version="1.0.0")
redis = Redis.from_url(settings.redis_url, decode_responses=True)

def require_token(authorization: str | None, expected: str) -> None:
    supplied = (authorization or "").removeprefix("Bearer ")
    if not expected or not hmac.compare_digest(supplied, expected):
        raise HTTPException(status_code=401, detail="Unauthorized")

@app.get("/health")
async def health():
    return {"ok": bool(await redis.ping()), "mode": "background-alerts"}

@app.get("/v1/status")
async def status():
    today = datetime.now().strftime("%Y%m%d")
    return {"connected": bool(await redis.get("feed:connected")), "universe": 210, "scanned": int(await redis.get("feed:scanned") or 0), "alertsToday": int(await redis.get(f"alerts:{today}") or 0), "vix": float(await redis.get("market:vix") or 0), "latencyMs": int(await redis.get("feed:latency_ms") or 0), "mode": "LIVE ALERTS / MANUAL ORDERS"}

@app.get("/v1/signals", response_model=list[Signal])
async def signals():
    return [Signal.model_validate_json(item) for item in await redis.lrange("signals:latest", 0, 49)]

@app.post("/v1/devices/register", status_code=204)
async def register_device(device: DeviceRegistration, authorization: str | None = Header(default=None)):
    require_token(authorization, settings.device_enrollment_token)
    await redis.sadd("devices:fcm", device.token)

@app.post("/internal/signals", status_code=202)
async def ingest_signal(signal: Signal, authorization: str | None = Header(default=None)):
    require_token(authorization, settings.internal_api_token)
    if not all(check.passed for check in signal.checks): return {"accepted": False, "reason": "validation"}
    if not await AlertGuard(redis).reserve(signal.symbol, signal.direction, signal.score): return {"accepted": False, "reason": "rate-limit"}
    await redis.lpush("signals:latest", signal.model_dump_json()); await redis.ltrim("signals:latest", 0, 49)
    tokens = list(await redis.smembers("devices:fcm"))
    await asyncio.gather(send_telegram(signal), asyncio.to_thread(send_fcm, signal, tokens))
    return {"accepted": True}
