package bezier.units

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

abstract class SIUnit<T : SIUnit<T>>(open val value: Double) : Comparable<SIUnit<T>> {
    constructor(value: Number) : this(value.toDouble())

    abstract fun createNew(value: Double): T
    val one: T get() = createNew(1.0)

    operator fun plus(other: T): T = createNew(value + other.value)
    operator fun plus(other: Number): T = createNew(value + other.toDouble())
    operator fun minus(other: T): T = createNew(value - other.value)

    operator fun minus(other: Number): T = createNew(value - other.toDouble())
    operator fun unaryMinus(): T = createNew(-value)
    operator fun times(other: Number): T = createNew(value * other.toDouble())

    operator fun div(other: Number): T = createNew(value / other.toDouble())

    operator fun div(other: T): Double = value / other.value

    infix fun aboutEquals(other: T) = this.value + 1e-6 > other.value && this.value - 1e-6 < other.value

    fun abs(): T = createNew(abs(value))
    fun sqrt(): T = createNew(sqrt(value))
    fun pow(n: Int): T = createNew(value.pow(n))
    fun round(places: Int) = createNew(Math.round(value * 10.0.pow(places)) / 10.0.pow(places))

    override fun compareTo(other: SIUnit<T>): Int = value.compareTo(other.value)
    operator fun compareTo(other: Number): Int = value.compareTo(other.toDouble())
}

//operator fun <T: SIUnit<T>> Number.times(unit: T) = unit.createNew(unit.value * this.toDouble()) // TODO figure out why this doesn't work and make it work