import httpx
from .config import settings
from .models import Signal

async def send_telegram(signal: Signal) -> None:
    if not settings.telegram_bot_token or not settings.telegram_chat_id: return
    text = f"🚨 OPTIONPULSE ALERT\n{signal.symbol} {signal.setup}\n{signal.contract}\nSpot ₹{signal.spot:.2f} | Premium ₹{signal.premium:.2f}\nGann 90° ₹{signal.gann90:.2f} | T1 ₹{signal.gann180:.2f} | T2 ₹{signal.gann360:.2f}\nScore {signal.score}/100"
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.post(f"https://api.telegram.org/bot{settings.telegram_bot_token}/sendMessage", json={"chat_id": settings.telegram_chat_id, "text": text})
        response.raise_for_status()

def send_fcm(signal: Signal, tokens: list[str]) -> None:
    if not tokens: return
    import firebase_admin
    from firebase_admin import credentials, messaging
    if not firebase_admin._apps:
        credential = credentials.Certificate(settings.firebase_credentials_path) if settings.firebase_credentials_path else None
        firebase_admin.initialize_app(credential)
    body = f"{signal.contract} | Spot ₹{signal.spot:.2f} | Score {signal.score}/100"
    messaging.send_each_for_multicast(messaging.MulticastMessage(tokens=tokens, notification=messaging.Notification(title=f"{signal.symbol}: {signal.setup}", body=body), data={"signal_id": signal.id}))
