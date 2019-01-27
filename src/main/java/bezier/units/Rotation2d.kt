package bezier.units

import kotlin.math.PI

sealed class Rotation2d<T : Rotation2d<T>>(value: Double) : SIUnit<T>(value) {
    companion object {
        const val kRevolutionsToDegrees: Double = 360.0
        const val kRevolutionsToRadians: Double = 2.0 * PI
    }

    abstract override fun createNew(value: Double): T

    abstract fun degrees(): Degrees
    abstract fun radians(): Radians
    abstract fun revolutions(): Revolutions

    fun sin(): Double = Math.sin(radians().value)
    fun cos(): Double = Math.cos(radians().value)
}

data class Degrees(override val value: Double) : Rotation2d<Degrees>(value) {
    override fun createNew(value: Double) = Degrees(value)

    override fun degrees() = this
    override fun radians() = Radians(Math.toRadians(this.value))
    override fun revolutions() = Revolutions(value / kRevolutionsToDegrees)
}

data class Radians(override val value: Double) : Rotation2d<Radians>(value) {
    override fun createNew(value: Double) = Radians(value)

    override fun degrees() = Degrees(Math.toDegrees(this.value))
    override fun radians() = this
    override fun revolutions() = Revolutions(value / kRevolutionsToRadians)
}

data class Revolutions(override val value: Double) : Rotation2d<Revolutions>(value) {
    override fun createNew(value: Double) = Revolutions(value)

    override fun degrees() = Degrees(value / 360.0)
    override fun radians() = Radians(value * 2.0 * Math.PI)
    override fun revolutions() = this
}