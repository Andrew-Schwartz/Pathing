package units

import bezier.units.derived.feetPerSecond
import bezier.units.derived.feetPerSecondSquared
import bezier.units.derived.inchesPerSecond
import bezier.units.feet
import bezier.units.seconds
import org.junit.jupiter.api.Test

object VelocityTest {
    @Test
    fun conversionTest() {
        val one = 3.inchesPerSecond()

        val two = 1.feetPerSecond()

        assert(one + two.inchesPerSecond() == 15.inchesPerSecond()) { "Velocity type conversion failed" }
    }

    @Test
    fun multiplicationTest() {
        val one = 4.feetPerSecond()

        val two = 3.seconds()

        println("Vel multiplication: ${one * two}")
        assert(one * two == 12.feet())
    }

    @Test
    fun `division by time`() {
        val one = 6.feetPerSecond()

        val two = 3.seconds()

        println("Vel division: ${one / two}")
        assert(one / two == 2.feetPerSecondSquared())
    }

    @Test
    fun `division by acceleration`() {
        val one = 6.feetPerSecond()

        val two = 3.feetPerSecondSquared()

        println("Vel division: ${one / two}")
        assert(one / two == 2.seconds())
    }
}