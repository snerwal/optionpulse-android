from app.signal_engine import Candle, OptionSnapshot, evaluate, spread_percent

def test_liquid_call_breakout_is_eligible():
    candles = [Candle(1400, 1410, 1395, 1405, 1000) for _ in range(20)]
    candles.append(Candle(1438, 1450, 1437, 1445, 3000))
    option = OptionSnapshot(39.8, 40.0, 60_000, 120_000)
    result = evaluate(candles, 1400, "CALL", option)
    assert result["eligible"] is True
    assert result["score"] == 100

def test_spread_guard_rejects_illiquid_contract():
    assert spread_percent(OptionSnapshot(10, 12, 60_000, 120_000)) > 1
