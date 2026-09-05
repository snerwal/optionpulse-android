from datetime import datetime
from redis.asyncio import Redis
from .config import settings

class AlertGuard:
    def __init__(self, redis: Redis): self.redis = redis

    async def reserve(self, symbol: str, direction: str, score: int) -> bool:
        if score < settings.minimum_score: return False
        day = datetime.now().strftime("%Y%m%d")
        count_key, cool_key = f"alerts:{day}", f"cooldown:{day}:{symbol}:{direction}"
        if int(await self.redis.get(count_key) or 0) >= settings.daily_alert_cap: return False
        if not await self.redis.set(cool_key, "1", ex=settings.cooldown_minutes * 60, nx=True): return False
        await self.redis.incr(count_key); await self.redis.expire(count_key, 172800)
        return True
