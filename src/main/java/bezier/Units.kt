package bezier

import ui.UIController

import utils.Config
import kotlin.math.pow

/**
 * This class is the supertype for all "Units"
 * The point is to add type safety to arithmetic done on values that represent real quantities
 */
sealed class Units {
    abstract operator fun unaryMinus(): Units
}

sealed class SingleUnit(open val value: Double) : Units() {
    abstract operator fun plus(n: Double): SingleUnit
}

const val FIELD_WIDTH_PIXELS = 384.0

val tickToInchFactor: Double
    get() = 2.0 * Math.PI * Config.wheelRadius() / Config.ticksPerRev()

operator fun Double.times(singleUnit: SingleUnit) = this * singleUnit.value

sealed class Length(override val value: Double) : SingleUnit(value) {
    abstract fun inches(): Inches
    abstract fun feet(): Feet
    abstract fun pixels(): Pixels
    abstract fun ticks(): Ticks

    abstract operator fun plus(other: Length): Length
    abstract operator fun minus(other: Length): Length
}

data class Inches(override val value: Double) : Length(value) {
    override fun feet() = Feet(value / 12.0)
    override fun pixels() = Pixels(value * FIELD_WIDTH_PIXELS / UIController.imageWidth())
    override fun ticks() = Ticks(value / tickToInchFactor)
    override fun inches() = this

    override fun unaryMinus() = Inches(-value)
    override fun plus(other: Length) = Inches(value + other.inches().value)
    override fun plus(d: Double) = Inches(value + d)
    override fun minus(other: Length) = Inches(value + other.inches().value)
}

data class Feet(override val value: Double) : Length(value) {
    override fun inches() = Inches(value * 12.0)
    override fun pixels() = inches().pixels()
    override fun ticks() = inches().ticks()
    override fun feet() = this

    override fun unaryMinus() = Feet(-value)
    override fun plus(other: Length) = Feet(value + other.feet().value)
    override fun plus(d: Double) = Feet(value + d)
    override fun minus(other: Length) = Feet(value + other.inches().value)
}

data class Pixels(override val value: Double) : Length(value) {
    override fun inches() = Inches(value * UIController.imageWidth() / FIELD_WIDTH_PIXELS)
    override fun feet() = inches().feet()
    override fun ticks() = inches().ticks()
    override fun pixels() = this

    override fun unaryMinus() = Pixels(-value)
    override fun plus(other: Length) = Pixels(value + other.pixels().value)
    override fun plus(d: Double) = Pixels(value + d)
    override fun minus(other: Length) = Pixels(value + other.inches().value)
}

data class Ticks(override val value: Double) : Length(value) {
    override fun inches() = Inches(value * tickToInchFactor)
    override fun feet() = inches().feet()
    override fun pixels() = inches().pixels()
    override fun ticks() = this

    override fun unaryMinus() = Ticks(-value)
    override fun plus(other: Length) = Ticks(value + other.ticks().value)
    override fun plus(d: Double) = Ticks(value + d)
    override fun minus(other: Length) = Ticks(value + other.inches().value)
}

sealed class Angle(override val value: Double) : SingleUnit(value) {
    abstract fun degrees(): Degrees
    abstract fun radians(): Radians
    abstract fun rotations(): Rotations

    abstract operator fun plus(other: Angle): Angle
}

data class Degrees(override val value: Double) : Angle(value) {
    //    override fun encoderTicks() = rotations().encoderTicks()
    override fun degrees() = this

    override fun radians() = Radians(Math.toRadians(this.value))
    override fun rotations() = Rotations(value * 360.0)

    override fun unaryMinus() = Degrees(-value)
    override fun plus(other: Angle) = Degrees(value + other.degrees().value)
    override fun plus(d: Double) = Degrees(value + d)
}

data class Radians(override val value: Double) : Angle(value) {
    //    override fun encoderTicks() = rotations().encoderTicks()
    override fun degrees() = Degrees(Math.toDegrees(this.value))

    override fun radians() = this
    override fun rotations() = Rotations(value / (2.0 * Math.PI))

    override fun unaryMinus() = Radians(-value)
    override fun plus(other: Angle) = Radians(value + other.radians().value)
    override fun plus(d: Double) = Radians(value + d)
}

data class Rotations(override val value: Double) : Angle(value) {
    override fun degrees() = Degrees(value / 360.0)
    override fun radians() = Radians(value * 2.0 * Math.PI)
    override fun rotations() = this

    override fun unaryMinus() = Rotations(-value)
    override fun plus(other: Angle) = Rotations(value + other.rotations().value)
    override fun plus(d: Double) = Rotations(value + d)
}

sealed class Time(override val value: Double) : SingleUnit(value) {
    abstract fun minutes(): Minutes
    abstract fun seconds(): Seconds
    abstract fun oneHundredMillis(): OneHundredMillis

    abstract operator fun plus(other: Time): Time
    abstract operator fun minus(other: Time): Time
}

data class Minutes(override val value: Double) : Time(value) {
    override fun minutes() = this
    override fun seconds() = Seconds(value * 60.0)
    override fun oneHundredMillis() = OneHundredMillis(value * 60.0 * 10.0)

    override fun unaryMinus() = Minutes(-value)
    override fun plus(other: Time) = Minutes(value + other.minutes().value)
    override fun plus(d: Double) = Minutes(value + d)
    override fun minus(other: Time) = Minutes(value - other.minutes().value)
}

data class Seconds(override val value: Double) : Time(value) {
    override fun minutes() = Minutes(value / 60.0)
    override fun seconds() = this
    override fun oneHundredMillis() = OneHundredMillis(value * 10.0)

    override fun unaryMinus() = Seconds(-value)
    override fun plus(other: Time) = Seconds(value + other.seconds().value)
    override fun plus(d: Double) = Seconds(value + d)
    override fun minus(other: Time) = Seconds(value - other.seconds().value)

}

data class OneHundredMillis(override val value: Double) : Time(value) {
    override fun minutes() = Minutes(value / 10.0 / 60.0)
    override fun seconds() = Seconds(value / 10.0)
    override fun oneHundredMillis() = this

    override fun unaryMinus() = OneHundredMillis(-value)
    override fun plus(other: Time) = OneHundredMillis(value + other.oneHundredMillis().value)
    override fun plus(d: Double) = OneHundredMillis(value + d)
    override fun minus(other: Time) = OneHundredMillis(value - other.oneHundredMillis().value)

}

sealed class FractionalUnit(open val numerator: SingleUnit, open val denominator: SingleUnit) : Units() {
    open val value: Double
        get() = numerator.value / denominator.value
}

sealed class LinearVelocity(override val numerator: Length, override val denominator: Time) : FractionalUnit(numerator, denominator) {
    abstract fun feetPerSecond(): FeetPerSecond
    abstract fun inchesPerSecond(): InchesPerSecond
    abstract fun inchesPer100Millis(): InchesPer100Millis
    abstract fun ticksPer100Millis(): TicksPer100Millis

    abstract operator fun times(other: Time): Length
    abstract operator fun minus(other: LinearVelocity): LinearVelocity
//    abstract operator fun div(other: LinearAcceleration): Time
}

data class FeetPerSecond
@JvmOverloads constructor(override val numerator: Feet, override val denominator: Seconds = Seconds(1.0)) : LinearVelocity(numerator, denominator) {
    override fun feetPerSecond() = this
    override fun inchesPerSecond() = InchesPerSecond(numerator.inches(), denominator)
    override fun inchesPer100Millis() = InchesPer100Millis(numerator.inches(), denominator.oneHundredMillis())
    override fun ticksPer100Millis() = TicksPer100Millis(numerator.ticks(), denominator.oneHundredMillis())

    override fun unaryMinus() = FeetPerSecond(-numerator, denominator)
    override fun times(other: Time) = Feet(numerator.value / denominator.value * other.seconds())
    override fun minus(other: LinearVelocity) = FeetPerSecond(numerator - other.numerator, denominator - other.denominator)
//    override fun div(other: LinearAcceleration) = Seconds(value / other.value)
}

data class InchesPerSecond
@JvmOverloads constructor(override val numerator: Inches, override val denominator: Seconds = Seconds(1.0)) : LinearVelocity(numerator, denominator) {
    override fun feetPerSecond() = FeetPerSecond(numerator.feet(), denominator)
    override fun inchesPerSecond() = this
    override fun inchesPer100Millis() = InchesPer100Millis(numerator, denominator.oneHundredMillis())
    override fun ticksPer100Millis() = TicksPer100Millis(numerator.ticks(), denominator.oneHundredMillis())

    override fun unaryMinus() = InchesPerSecond(-numerator, denominator)
    override fun times(other: Time) = Inches(numerator.value / denominator.value * other.seconds())
    override fun minus(other: LinearVelocity) = InchesPerSecond(numerator - other.numerator, denominator - other.denominator)
    operator fun div(other: FeetPerSecondSquared) = Seconds(feetPerSecond().value / other.value)
}

data class InchesPer100Millis
@JvmOverloads constructor(override val numerator: Inches, override val denominator: OneHundredMillis = OneHundredMillis(1.0)) : LinearVelocity(numerator, denominator) {
    override fun feetPerSecond() = FeetPerSecond(numerator.feet(), denominator.seconds())

    override fun inchesPerSecond() = InchesPerSecond(numerator, denominator.seconds())
    override fun inchesPer100Millis() = this
    override fun ticksPer100Millis() = TicksPer100Millis(numerator.ticks(), denominator)
    override fun unaryMinus() = InchesPer100Millis(-numerator, denominator)

    override fun times(other: Time) = Inches(numerator.value / denominator.value * other.oneHundredMillis())
    override fun minus(other: LinearVelocity) = InchesPer100Millis(numerator - other.numerator, denominator - other.denominator)
//    override fun div(other: LinearAcceleration) = Seconds(value / other.value)
}

data class TicksPer100Millis
@JvmOverloads constructor(override val numerator: Ticks, override val denominator: OneHundredMillis = OneHundredMillis(1.0)) : LinearVelocity(numerator, denominator) {
    override fun feetPerSecond() = FeetPerSecond(numerator.feet(), denominator.seconds())
    override fun inchesPerSecond() = InchesPerSecond(numerator.inches(), denominator.seconds())
    override fun inchesPer100Millis() = InchesPer100Millis(numerator.inches(), denominator)
    override fun ticksPer100Millis() = this

    override fun times(other: Time) = Ticks(numerator.value / denominator.value * other.oneHundredMillis())
    override fun unaryMinus() = TicksPer100Millis(-numerator, denominator)
    override fun minus(other: LinearVelocity) = TicksPer100Millis(numerator - other.numerator, denominator - other.denominator)
}

sealed class AngularVelocity(override val numerator: Angle, override val denominator: Time) : FractionalUnit(numerator, denominator) {
    abstract fun radiansPerSecond(): RadiansPerSecond
    abstract fun rpm(): RPM
}

data class RadiansPerSecond(override val numerator: Radians, override val denominator: Seconds) : AngularVelocity(numerator, denominator) {
    override fun radiansPerSecond() = this
    override fun rpm() = RPM(numerator.rotations(), denominator.minutes())

    override fun unaryMinus() = RadiansPerSecond(-numerator, denominator)
}

data class RPM(override val numerator: Rotations, override val denominator: Minutes) : AngularVelocity(numerator, denominator) {
    override fun radiansPerSecond() = RadiansPerSecond(numerator.radians(), denominator.seconds())
    override fun rpm() = this

    override fun unaryMinus() = RPM(-numerator, denominator)
}

sealed class ExponentialUnits(override val value: Double, open val power: Double) : SingleUnit(value) {
    abstract fun secondsSquared(): SecondsSquared
}

data class SecondsSquared(override val value: Double, override val power: Double = 2.0) : ExponentialUnits(value, power) {
    override fun secondsSquared() = this

    override fun unaryMinus() = SecondsSquared(-value)
    override fun plus(n: Double) = SecondsSquared(value + n)
}

sealed class LinearAcceleration(override val numerator: Length, override val denominator: ExponentialUnits) : FractionalUnit(numerator, denominator) {
    override val value: Double
        get() = numerator.value / (denominator.value.pow(denominator.power))

    abstract fun feetPerSecondSquared(): FeetPerSecondSquared
}

data class FeetPerSecondSquared
@JvmOverloads constructor(override val numerator: Feet, override val denominator: SecondsSquared = SecondsSquared(1.0, 2.0)) : LinearAcceleration(numerator, denominator) {
    override fun feetPerSecondSquared() = this

    override fun unaryMinus() = FeetPerSecondSquared(-numerator, denominator)
}
