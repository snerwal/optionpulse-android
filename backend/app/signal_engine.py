from dataclasses import dataclass
from .gann import resistance, support

@dataclass(frozen=True)
class Candle:
    open: float
    high: float
    low: float
    close: float
    volume: int

@dataclass(frozen=True)
class OptionSnapshot:
    bid: float
    ask: float
    volume: int
    oi: int

def spread_percent(option: OptionSnapshot) -> float:
    mid = (option.bid + option.ask) / 2
    return 100.0 if mid <= 0 else (option.ask - option.bid) / mid * 100

def evaluate(candles: list[Candle], pivot: float, direction: str, option: OptionSnapshot) -> dict:
    if len(candles) < 21: raise ValueError("At least 21 completed five-minute candles required")
    latest, history = candles[-1], candles[-21:-1]
    volume_average = sum(c.volume for c in history) / 20
    cumulative_volume = sum(c.volume for c in candles)
    vwap = sum(((c.high + c.low + c.close) / 3) * c.volume for c in candles) / cumulative_volume
    bullish = direction.upper() == "CALL"
    trigger = resistance(pivot, 90) if bullish else support(pivot, 90)
    crossed = latest.close > trigger if bullish else latest.close < trigger
    vwap_ok = latest.close > vwap if bullish else latest.close < vwap
    volume_ok = latest.volume > 2.5 * volume_average
    candle_ok = latest.close > latest.open if bullish else latest.close < latest.open
    liquidity_ok = option.oi > 100_000 and option.volume > 50_000 and spread_percent(option) <= 1.0
    checks = [crossed, vwap_ok, volume_ok, candle_ok, liquidity_ok]
    return {"eligible": all(checks), "score": sum(checks) * 20, "trigger": trigger, "vwap": vwap, "spread_pct": spread_percent(option)}
