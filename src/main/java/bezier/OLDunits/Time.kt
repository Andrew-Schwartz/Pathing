package bezier.OLDunits

fun Number.minutes() = Minutes(toDouble())
fun Number.seconds() = Seconds(toDouble())
fun Number.hundredMillis() = HundredMillis(toDouble())

sealed class Time<T : Time<T>>(value: Double, ctor: (Double) -> T) : SIUnit<T>(value, ctor) {
    companion object {
        const val kMinToSec: Double = 60.0
        const val kSecToHundredMillis: Double = 10.0
    }

    abstract fun minutes(): Minutes
    abstract fun seconds(): Seconds
    abstract fun hundredMillis(): HundredMillis
}

data class Minutes(override val value: Double) : Time<Minutes>(value, ::Minutes) {
    override fun minutes() = this
    override fun seconds() = Seconds(value * kMinToSec)
    override fun hundredMillis() = HundredMillis(value * kMinToSec * kSecToHundredMillis)
}

data class Seconds(override val value: Double) : Time<Seconds>(value, ::Seconds) {
    override fun minutes() = Minutes(value / kMinToSec)
    override fun seconds() = this
    override fun hundredMillis() = HundredMillis(value * kSecToHundredMillis)
}

data class HundredMillis(override val value: Double) : Time<HundredMillis>(value, ::HundredMillis) {
    override fun minutes() = Minutes(value / kSecToHundredMillis / kMinToSec)
    override fun seconds() = Seconds(value / kSecToHundredMillis)
    override fun hundredMillis() = this
}