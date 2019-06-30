package units

import bezier.OLDunits.*
import org.junit.jupiter.api.Test

object TimeTest {
    @Test
    fun typesTest() {
        val one: Seconds = 30.seconds()

        val two: Minutes = 1.minutes()

        val three: HundredMillis = 10.hundredMillis()

        assert(one + two.seconds() + three.seconds() == 91.seconds()) { "Type Conversion Failed" }
    }


}