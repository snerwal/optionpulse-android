package in.optionpulse.scanner

import org.junit.Assert.*
import org.junit.Test

class StrikeEngineTest {
    @Test fun tiersAreAppliedAtBoundaries() {
        assertEquals(2.5, StrikeEngine.step(100.0), 0.0)
        assertEquals(5.0, StrikeEngine.step(100.01), 0.0)
        assertEquals(250.0, StrikeEngine.step(5000.01), 0.0)
    }
    @Test fun lowVixSelectsAtm() = assertEquals(1420.0, StrikeEngine.selectedStrike(1420.5, Direction.CALL, 13.8), 0.0)
    @Test fun highVixSelectsOtmOne() {
        assertEquals(1440.0, StrikeEngine.selectedStrike(1420.5, Direction.CALL, 16.0), 0.0)
        assertEquals(1400.0, StrikeEngine.selectedStrike(1420.5, Direction.PUT, 16.0), 0.0)
    }
}
