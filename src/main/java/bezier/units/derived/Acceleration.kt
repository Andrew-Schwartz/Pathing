package bezier.units.derived

import bezier.units.*
import kotlin.math.pow

data class Acceleration<N : Length<N>, D : Time<D>>(
        private var numerator: N,
        private var denominator: D
) : SIUnit<Acceleration<N, D>>(numerator.value /(denominator.value.pow(2))) {
    init {
        numerator = numerator.createNew(this.value)
        denominator = denominator.one
    }

    override fun createNew(value: Double) = Acceleration(numerator.createNew(value), denominator.one)

    fun feetPerMinuteSquared() = FeetPerMinuteSquared(numerator.feet(), denominator.minutes())
    fun feetPerSecondSquared() = FeetPerSecondSquared(numerator.feet(), denominator.seconds())
    fun inchesPerSecondSquared() = InchesPerSecondSquared(numerator.inches(), denominator.seconds())

    operator fun times(time: D) = LinearVelocity(numerator * time.value, denominator.one)
}

fun Number.feetPerMinuteSquared() = FeetPerMinuteSquared(this.feet(), 1.minutes())
fun Number.feetPerSecondSquared() = FeetPerSecondSquared(this.feet(), 1.seconds())
fun Number.inchesPerSecondSquared() = InchesPerSecondSquared(this.inches(), 1.seconds())

typealias FeetPerMinuteSquared = Acceleration<Feet, Minutes>
typealias FeetPerSecondSquared = Acceleration<Feet, Seconds>
typealias InchesPerSecondSquared = Acceleration<Inches, Seconds>