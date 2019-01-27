package units

import bezier.units.feet
import bezier.units.inches
import bezier.units.ticks
import org.junit.jupiter.api.Test

object LengthTest {
    @Test
    fun `conversion test inches and ticks`() {
        val one = 1.inches()

        val two = 450.ticks()

        assert(one.ticks() == two) { "conversion failed: ${one.ticks()} and $two" }
    }

    @Test
    fun `conversion test inches and feet`() {
        val one = 2.feet()

        val two = 5.inches()

        assert((one + two.feet()).inches() == 29.inches())
    }
}