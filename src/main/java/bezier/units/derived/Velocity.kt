package bezier.units.derived

import bezier.units.*

typealias FeetPerSecond = LinearVelocity<Feet, Seconds>
typealias InchesPerSecond = LinearVelocity<Inches, Seconds>
typealias InchesPerHundredMillis = LinearVelocity<Inches, HundredMillis>
typealias TicksPerHundredMillis = LinearVelocity<Ticks, HundredMillis>

fun Number.feetPerSecond() = FeetPerSecond(this.feet(), 1.seconds())
fun Number.inchesPerSecond() = InchesPerSecond(this.inches(), 1.seconds())
fun Number.ticksPerHundredMillis() = TicksPerHundredMillis(this.ticks(), 1.hundredMillis())

data class LinearVelocity<N : Length<N>, D : Time<D>>(
        var numerator: N,
        var denominator: D
) : SIUnit<LinearVelocity<N, D>>(numerator.value / denominator.value) {

    init {
        numerator = numerator.createNew(this.value)
        denominator = denominator.one
    }

    //    constructor(numerator: N) : this(numerator)
    override fun createNew(value: Double) = LinearVelocity(numerator.createNew(value), denominator.one)

    fun createNewTime(value: Double) = denominator.createNew(value)
    fun feetPerSecond() = FeetPerSecond(numerator.feet(), denominator.seconds())
    fun inchesPerSecond() = InchesPerSecond(numerator.inches(), denominator.seconds())
    fun inchesPerHundredMillis() = InchesPerHundredMillis(numerator.inches(), denominator.hundredMillis())
    fun ticksPerHundredMillis() = TicksPerHundredMillis(numerator.ticks(), denominator.hundredMillis())

    operator fun times(time: D) = numerator.createNew(this.value * time.value / denominator.value)
    operator fun div(time: D) = Acceleration(numerator / time.value, denominator.one)
    operator fun div(acceleration: Acceleration<N, D>) = denominator.createNew(this.value / acceleration.value)

}

typealias RadiansPerSecond<R> = AngularVelocity<R, Radians, Seconds>

data class AngularVelocity<R: Length<R>, N : Rotation2d<N>, D : Time<D>>(
        private val radius: R,
        private var numerator: N,
        private var denominator: D
) : SIUnit<AngularVelocity<R, N, D>>(numerator.value / denominator.value) {
    init {
        numerator = numerator.createNew(this.value)
        denominator = denominator.one
    }

//    constructor(radius: R, linearVelocity: LinearVelocity<R, D>) : this(radius, numerator , linearVelocity.denominator)

    override fun createNew(value: Double) = AngularVelocity(radius, numerator.createNew(this.value), denominator.one)
}