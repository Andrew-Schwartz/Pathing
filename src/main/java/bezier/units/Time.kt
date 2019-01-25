package bezier.units

sealed class Time<T : Time<T>>(value: Double) : SIUnit<T>(value) {
    companion object {
        const val kMinToSec: Double = 60.0
        const val kSecToHundredMillis: Double = 10.0
    }

    abstract override fun createNew(value: Double): T

    abstract fun minutes(): Minutes
    abstract fun seconds(): Seconds
    abstract fun hundredMillis(): HundredMillis
}

fun Number.minutes() = Minutes(toDouble())
data class Minutes(override val value: Double) : Time<Minutes>(value) {
    override fun createNew(value: Double) = Minutes(value)

    override fun minutes() = this
    override fun seconds() = Seconds(value * kMinToSec)
    override fun hundredMillis() = HundredMillis(value * kMinToSec * kSecToHundredMillis)
}

fun Number.seconds() = Seconds(toDouble())
data class Seconds(override val value: Double) : Time<Seconds>(value) {
    override fun createNew(value: Double) = Seconds(value)

    override fun minutes() = Minutes(value / kMinToSec)
    override fun seconds() = this
    override fun hundredMillis() = HundredMillis(value * kSecToHundredMillis)
}

fun Number.hundredMillis() = HundredMillis(toDouble())
data class HundredMillis(override val value: Double) : Time<HundredMillis>(value) {
    override fun createNew(value: Double) = HundredMillis(value)

    override fun minutes() = Minutes(value / kSecToHundredMillis / kMinToSec)
    override fun seconds() = Seconds(value / kSecToHundredMillis)
    override fun hundredMillis() = this
}