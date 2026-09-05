package in.optionpulse.scanner

enum class Direction { CALL, PUT }

data class Signal(
    val id: String,
    val symbol: String,
    val direction: Direction,
    val setup: String,
    val spot: Double,
    val score: Int,
    val contract: String,
    val premium: Double,
    val spreadPct: Double,
    val optionOi: Long,
    val optionVolume: Long,
    val pivotPrice: Double,
    val gann45: Double,
    val gann90: Double,
    val gann180: Double,
    val gann360: Double,
    val spotStop: Double,
    val premiumStop: Double,
    val vix: Double,
    val timestamp: String,
    val checks: List<Check>
)

data class Check(val name: String, val passed: Boolean, val detail: String)

data class MarketStatus(
    val connected: Boolean = true,
    val universe: Int = 210,
    val scanned: Int = 210,
    val alertsToday: Int = 4,
    val vix: Double = 13.8,
    val latencyMs: Int = 84,
    val mode: String = "PAPER / ALERTS ONLY"
)

data class ScannerSettings(
    val minimumScore: Int = 80,
    val cooldownMinutes: Int = 15,
    val dailyAlertCap: Int = 10,
    val optionOiMinimum: Long = 100_000,
    val optionVolumeMinimum: Long = 50_000,
    val maximumSpreadPct: Double = 1.0,
    val astroGuard: Boolean = true
)
