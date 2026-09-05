package in.optionpulse.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GannSquareOfNineTest {
    @Test fun voltasPivotProducesRequestedResistanceLevels() {
        val levels = GannSquareOfNine.levels(1400.0)
        assertEquals(1418.77, levels.resistance45, 0.01)
        assertEquals(1437.67, levels.resistance90, 0.01)
        assertEquals(1475.83, levels.resistance180, 0.01)
        assertEquals(1553.67, levels.resistance360, 0.01)
    }

    @Test fun lookupTableBenchmarksFollowFormula() {
        assertEquals(110.25, GannSquareOfNine.resistance(100.0, 90), 0.001)
        assertEquals(81.0, GannSquareOfNine.support(100.0, 180), 0.001)
        assertEquals(2550.25, GannSquareOfNine.resistance(2500.0, 90), 0.001)
        assertEquals(2304.0, GannSquareOfNine.support(2500.0, 360), 0.001)
    }

    @Test fun reverseAngleRecoversProjectionAngle() {
        val projected = GannSquareOfNine.resistance(1400.0, 180)
        assertEquals(180.0, GannSquareOfNine.reverseAngle(projected, 1400.0), 1e-8)
    }

    @Test fun supportClampsAtZeroAndInvalidPivotFails() {
        assertEquals(0.0, GannSquareOfNine.support(1.0, 360), 0.0)
        assertThrows(IllegalArgumentException::class.java) { GannSquareOfNine.resistance(0.0, 90) }
    }
}
