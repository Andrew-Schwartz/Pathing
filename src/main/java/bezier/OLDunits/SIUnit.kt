package bezier.OLDunits

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

abstract class SIUnit<T : SIUnit<T>>(open val value: Double, val ctor: (Double) -> T) : Comparable<SIUnit<T>> {
    val one: T get() = ctor(1.0)

    operator fun plus(other: T): T = ctor(value + other.value)
    operator fun plus(other: Number): T = ctor(value + other.toDouble())
    operator fun minus(other: T): T = ctor(value - other.value)

    operator fun minus(other: Number): T = ctor(value - other.toDouble())
    operator fun unaryMinus(): T = ctor(-value)
    operator fun times(other: Number): T = ctor(value * other.toDouble())

    operator fun div(other: Number): T = ctor(value / other.toDouble())

    operator fun div(other: T): Double = value / other.value

    fun abs(): T = ctor(abs(value))
    fun sqrt(): T = ctor(sqrt(value))
    fun pow(n: Int): T = ctor(value.pow(n))
    fun round(places: Int) = ctor(Math.round(value * 10.0.pow(places)) / 10.0.pow(places))

    override fun compareTo(other: SIUnit<T>): Int = value.compareTo(other.value)
    operator fun compareTo(other: Number): Int = value.compareTo(other.toDouble())
}

operator fun <T : SIUnit<T>> Number.times(unit: T) = unit.ctor(unit.value * this.toDouble()) // TODO figure out why this doesn't work and make it work
