package bezier.units

open class SIUnit<T : SIUnit<T>>(val value: Double) {
    operator fun plus(other: T): T {
        return value + other.value
    }
}