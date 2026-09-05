import pytest
from app.gann import resistance, reverse_angle, support

def test_voltas_levels():
    assert resistance(1400, 45) == pytest.approx(1418.77, abs=.01)
    assert resistance(1400, 90) == pytest.approx(1437.67, abs=.01)
    assert resistance(1400, 180) == pytest.approx(1475.83, abs=.01)
    assert resistance(1400, 360) == pytest.approx(1553.67, abs=.01)

def test_support_and_reverse():
    assert support(100, 180) == pytest.approx(81)
    projected = resistance(1400, 180)
    assert reverse_angle(projected, 1400) == pytest.approx(180)
