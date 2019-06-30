package bezier.OLDunits

import bezier.OLDunits.derived.LinearVelocity
import ui.UIController
import utils.Config

fun Number.feet() = Feet(toDouble())
fun Number.inches() = Inches(toDouble())
fun Number.pixels() = Pixels(this.toDouble())
fun Number.ticks() = Ticks(toDouble())

sealed class Length<T : Length<T>>(value: Double, ctor: (Double) -> T) : SIUnit<T>(value, ctor) {
    companion object {
        @JvmStatic
        val FIELD_HEIGHT_INCHES = Inches(322.25) //ish
        const val kFeetToInches: Double = 12.0
        val kInchesToTicks: Double get() = Config.ticksPerInch()
        val kPixelsToInches: Double get() = FIELD_HEIGHT_INCHES.value / UIController.imageHeight().value
    }

    abstract fun inches(): Inches
    abstract fun feet(): Feet
    abstract fun pixels(): Pixels
    abstract fun ticks(): Ticks

    operator fun <D : Time<D>> div(time: D): LinearVelocity<T, D> = LinearVelocity(ctor(value), time)
    operator fun <D : Time<D>> div(velocity: LinearVelocity<T, D>): D = velocity.createNewTime(value / velocity.value)
}

data class Feet(override val value: Double) : Length<Feet>(value, ::Feet) {
    override fun inches() = Inches(value * kFeetToInches)
    override fun feet() = this
    override fun pixels() = Pixels(value * kFeetToInches / kPixelsToInches)
    override fun ticks() = Ticks(value * kFeetToInches * kInchesToTicks)
}

data class Inches(override val value: Double) : Length<Inches>(value, ::Inches) {
    override fun inches() = this
    override fun feet() = Feet(value / kFeetToInches)
    override fun ticks() = Ticks(value * kInchesToTicks)
    override fun pixels() = Pixels(value / kPixelsToInches)
}

data class Pixels(override val value: Double) : Length<Pixels>(value, ::Pixels) {
    override fun inches() = Inches(value * kPixelsToInches)
    override fun feet() = Feet(value * kPixelsToInches / kFeetToInches)
    override fun pixels() = this
    override fun ticks() = Ticks(value * kPixelsToInches * kInchesToTicks)
}

data class Ticks(override val value: Double) : Length<Ticks>(value, ::Ticks) {
    override fun inches() = Inches(value / kInchesToTicks)
    override fun feet() = Feet(value / kInchesToTicks / kFeetToInches)
    override fun pixels() = Pixels(value * kFeetToInches / kPixelsToInches)
    override fun ticks() = this
}