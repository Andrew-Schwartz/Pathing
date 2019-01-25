package bezier.units.derived

import bezier.units.*
import kotlin.math.pow

data class Acceleration<N : Length<N>, D : Time<D>>(
        private var numerator: N,
        private var denominator: D
) : SIUnit<Acceleration<N, D>>(numerator.value / denominator.value.pow(2)) {
    init {
        numerator = numerator.createNew(this.value)
        denominator = denominator.one
    }

    override fun createNew(value: Double) = Acceleration(numerator.createNew(value * this.value), denominator.one)

    fun feetPerMinuteSquared() = FeetPerMinuteSquared(numerator.feet(), denominator.minutes() * denominator.minutes().value)
    fun feetPerSecondSquared() = FeetPerSecondSquared(numerator.feet(), denominator.seconds() * denominator.seconds().value)
    fun inchesPerSecondSquared() = InchesPerSecondSquared(numerator.inches(), denominator.seconds() * denominator.seconds().value)

    operator fun times(time: D) = LinearVelocity(numerator * time.value, denominator.one)
}

typealias FeetPerMinuteSquared = Acceleration<Feet, Minutes>
typealias FeetPerSecondSquared = Acceleration<Feet, Seconds>
typealias InchesPerSecondSquared = Acceleration<Inches, Seconds>