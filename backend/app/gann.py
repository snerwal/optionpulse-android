from math import sqrt

def resistance(price: float, angle: int) -> float:
    if price <= 0 or angle < 0: raise ValueError("positive price and non-negative angle required")
    return (sqrt(price) + angle / 180.0) ** 2

def support(price: float, angle: int) -> float:
    if price <= 0 or angle < 0: raise ValueError("positive price and non-negative angle required")
    return max(0.0, sqrt(price) - angle / 180.0) ** 2

def reverse_angle(current: float, pivot: float) -> float:
    if current <= 0 or pivot <= 0: raise ValueError("prices must be positive")
    return abs(sqrt(current) - sqrt(pivot)) * 180.0
