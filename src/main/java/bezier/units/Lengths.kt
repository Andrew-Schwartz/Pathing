package bezier.units

interface Length : SIUnit {
    override fun createNew(value: Double): Length
}

class Feet(value: Double) : Length, Measurement(value) {
    override fun createNew(value: Double): Feet = Feet(value)


}

