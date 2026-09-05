package in.optionpulse.scanner

import kotlin.math.round

object StrikeEngine {
    fun step(spot: Double): Double = when {
        spot <= 100 -> 2.5
        spot <= 250 -> 5.0
        spot <= 500 -> 10.0
        spot <= 1_000 -> 20.0
        spot <= 2_500 -> 50.0
        spot <= 5_000 -> 100.0
        else -> 250.0
    }

    fun atm(spot: Double): Double { val s = step(spot); return round(spot / s) * s }

    fun selectedStrike(spot: Double, direction: Direction, indiaVix: Double): Double {
        val atm = atm(spot)
        if (indiaVix <= 15.0) return atm
        return if (direction == Direction.CALL) atm + step(spot) else atm - step(spot)
    }
}

object SignalGuard {
    fun isEligible(signal: Signal, settings: ScannerSettings): Boolean =
        signal.score >= settings.minimumScore &&
            signal.spreadPct <= settings.maximumSpreadPct &&
            signal.optionOi >= settings.optionOiMinimum &&
            signal.optionVolume >= settings.optionVolumeMinimum &&
            signal.checks.all { it.passed }
}
