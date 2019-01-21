package bezier.units

interface SIUnit {
    fun createNew(value: Double): SIUnit
}