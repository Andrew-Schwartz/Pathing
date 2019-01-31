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

    override fun createNew(value: Double) = LinearVelocity(numerator.createNew(value), denominator.one)

    fun createNewTime(value: Double) = denominator.createNew(value)
    fun feetPerSecond() = FeetPerSecond(numerator.feet(), denominator.seconds())
    fun inchesPerSecond() = InchesPerSecond(numerator.inches(), denominator.seconds())
    fun inchesPerHundredMillis() = InchesPerHundredMillis(numerator.inches(), denominator.hundredMillis())
    fun ticksPerHundredMillis() = TicksPerHundredMillis(numerator.ticks(), denominator.hundredMillis())

    operator fun times(time: D) = numerator.createNew(this.value * time.value / denominator.value)

    operator fun div(time: D) = Acceleration(numerator / time.value, denominator.one)
    operator fun div(acceleration: Acceleration<N, D>) = denominator.createNew(this.value / acceleration.value)
    operator fun div(radius: N): AngularVelocity<D> = AngularVelocity(this, radius)
}

typealias RadiansPerSecond = AngularVelocity<Seconds>

data class AngularVelocity<D : Time<D>>(
        var numerator: Radians,
        var denominator: D
) : SIUnit<AngularVelocity<D>>(numerator.value / denominator.value) {
    companion object {
        @JvmStatic
        operator fun <N : Length<N>, D : Time<D>> invoke(linVel: LinearVelocity<N, D>, radius: N) = AngularVelocity(Radians(linVel.numerator, radius), linVel.denominator)
    }

    init {
        numerator = numerator.createNew(this.value)
        denominator = denominator.one
    }

    override fun createNew(value: Double) = AngularVelocity(numerator.createNew(value), denominator)

    operator fun times(time: D) = numerator.createNew(this.value * time.value / denominator.value)
    operator fun <L : Length<L>> times(radius: L): LinearVelocity<L, D> = LinearVelocity(numerator.withRadius(radius), denominator)
}