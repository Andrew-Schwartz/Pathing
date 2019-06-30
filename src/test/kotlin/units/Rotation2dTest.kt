package units

//import bezier.OLDunits.Radians.Companion.Radians
import bezier.OLDunits.*
import org.junit.jupiter.api.Test
import java.lang.Math.PI

object Rotation2dTest {
    @Test
    fun `compare degrees`() {
        var one = 330.degrees()
        val two = 30.degrees()

        assert(one isLeftTowards two) { "$one, $two" }

        one = 0.degrees()

        assert(one isLeftTowards two) { "$one, $two" }

        assert(two isRightTowards one) { "$one, $two" }
    }

    @Test
    fun `compare radians`() {
        var one = (1.95 * PI).radians()
        val two = .3.radians()

        assert(one isLeftTowards two) { "$one, $two" }

        one = 0.radians()

        assert(one isLeftTowards two) { "$one, $two" }

        assert(two isRightTowards one) { "$one, $two" }
    }

    @Test
    fun `from linear measurement`() {
        val radius = 1.feet()

        val length = (2 * PI).feet()

        val expected = 1.revolutions().radians()

        assert(Radians(length, radius) == expected) { "$radius, $length, $expected" }
    }

    @Test
    fun `to linear measurement`() {
        val theta = (2 * PI).radians()

        val radius = (1 / PI).feet()

        assert(theta.withRadius(radius) == 2.feet()) { "${theta.withRadius(radius)}" }
    }
}