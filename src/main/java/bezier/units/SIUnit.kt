package bezier.units

import kotlin.math.abs

abstract class SIUnit<T : SIUnit<T>>(open val value: Double) : Comparable<SIUnit<T>> {
    abstract fun createNew(value: Double): T
    val one: T get() = createNew(1.0)

    operator fun plus(other: T): T = createNew(value + other.value)
    operator fun plus(other: Number): T = createNew(value + other.toDouble())
    fun abs(): T = createNew(abs(value))

    operator fun minus(other: T): T = createNew(value - other.value)
    operator fun minus(other: Number): T = createNew(value - other.toDouble())
    operator fun unaryMinus(): T = createNew(-value)

    operator fun times(other: Number): T = createNew(value * other.toDouble())

    operator fun div(other: Number): T = createNew(value / other.toDouble())
    operator fun div(other: T): Double = value / other.value

    infix fun aboutEquals(other: T) = this.value + 1e-6 > other.value && this.value - 1e-6 < other.value

    override fun compareTo(other: SIUnit<T>): Int = (value - other.value).toInt()
    operator fun compareTo(other: Number): Int = (value - other.toDouble()).toInt()
}

//operator fun <T: SIUnit<T>> Number.times(unit: T) = unit.createNew(unit.value * this.toDouble()) // TODO figure out why this doesn't work and make it work