package units

import bezier.OLDunits.derived.feetPerSecond
import bezier.OLDunits.derived.feetPerSecondSquared
import bezier.OLDunits.derived.inchesPerSecondSquared
import bezier.OLDunits.seconds
import org.junit.jupiter.api.Test

object AccelerationTest {
    @Test
    fun `conversion test`() {
        val one = 2.feetPerSecondSquared()

        val two = 4.inchesPerSecondSquared()

        println(one.inchesPerSecondSquared() + two)
        assert(one.inchesPerSecondSquared() + two == 28.inchesPerSecondSquared())
    }

    @Test
    fun `multiplication test`() {
        val one = 12.feetPerSecondSquared()

        val two = 3.seconds()

        println(one * two)
        assert(one * two == 36.feetPerSecond())
    }
}