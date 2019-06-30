package bezier.units

import utils.Config

/**
 * Base unit is inches
 */
class Length(inches: Double) : SIUnit<Length>(inches) {
    companion object {
        @JvmStatic
        val FIELD_HEIGHT_INCHES = inches(322.25) //ish
        const val baseFromInches = 1.0
        const val baseFromFeet = 12.0
        //        val baseFromPixels: Double = FIELD_HEIGHT_INCHES.baseValue / UIController.imageHeight().baseValue
        val baseFromTicks: Double get() = 1.0 / Config.ticksPerInch()

        fun inches(inches: Double): Length = Length(inches * baseFromInches)
        fun feet(feet: Double): Length = Length(feet * baseFromFeet)
        //        fun pixels(pixels: Double): Length = Length(pixels * baseFromPixels)
        fun ticks(ticks: Double): Length = Length(ticks * baseFromFeet)
    }

    val asInches: Double get() = value
    val asFeet: Double get() = value / baseFromFeet
    //    val asPixels: Double get() = inches / baseFromPixels
    val asTicks: Double get() = value / baseFromTicks
}
