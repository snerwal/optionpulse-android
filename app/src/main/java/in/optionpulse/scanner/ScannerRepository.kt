package in.optionpulse.scanner

interface ScannerRepository {
    suspend fun marketStatus(): MarketStatus
    suspend fun signals(): List<Signal>
}

class DemoScannerRepository : ScannerRepository {
    override suspend fun marketStatus() = MarketStatus()
    override suspend fun signals() = listOf(
        Signal("1", "VOLTAS", Direction.CALL, "BULLISH BREAKOUT", 1440.50, 85,
            "VOLTAS SEP 1440 CE", 38.50, 0.5, 184_500, 96_240, 1400.0, 1418.77, 1437.67, 1475.83, 1553.67,
            1418.70, 30.0, 16.2, "10:15:02 IST",
            listOf(Check("Gann 90° close", true, "Closed above ₹1,437.67"), Check("Volume surge", true, "3.1× SMA20"),
                Check("Anchored VWAP", true, "Spot above VWAP"), Check("Pattern", true, "Bullish Marubozu"),
                Check("Astro guard", true, "Moon–Saturn 18.4°"), Check("OI wall", true, "No wall within 0.5%"))),
        Signal("2", "BALKRISIND", Direction.PUT, "BEARISH BREAKDOWN", 2748.20, 82,
            "BALKRISIND SEP 2700 PE", 44.10, 0.8, 131_000, 72_800, 2800.0, 2773.60, 2747.33, 2695.17, 2592.34,
            2773.70, 34.0, 13.8, "11:05:12 IST",
            listOf(Check("Gann 90° close", true, "Closed below ₹2,747.33"), Check("Volume surge", true, "2.8× SMA20"),
                Check("Anchored VWAP", true, "Spot below VWAP"), Check("Pattern", true, "Bearish Engulfing"),
                Check("Astro guard", true, "No conjunction"), Check("OI wall", true, "Clear below spot")))
    )
}
