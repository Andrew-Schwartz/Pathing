package bezier.OLDunits

import kotlin.math.PI

fun atan2(y: Number, x: Number): Radians = Math.atan2(y.toDouble(), x.toDouble()).radians()

sealed class Rotation2d<T : Rotation2d<T>>(value: Double, ctor: (Double) -> T) : SIUnit<T>(value, ctor) {
    companion object {
        const val kRevolutionsToDegrees: Double = 360.0
        const val kRevolutionsToRadians: Double = 2.0 * PI
    }

    abstract fun degrees(): Degrees
    abstract fun radians(): Radians
    abstract fun revolutions(): Revolutions

    val sin: Double get() = Math.sin(this.radians().value)
    val cos: Double get() = Math.cos(this.radians().value)

    fun <T : Length<T>> withRadius(radius: T): T = radius * value

    fun angleTo(other: T): T {
        var angle = other.degrees().value - this.degrees().value
        angle += when {
            angle > 180 -> -360
            angle < -180 -> 360
            else -> 0
        }
        return ctor(angle)
    }

    infix fun isLeftTowards(other: T): Boolean {
        return angleTo(other) < 0
    }

    infix fun isRightTowards(other: T) = !(this isLeftTowards other)
}

fun Number.degrees() = Degrees(toDouble())
data class Degrees(override val value: Double) : Rotation2d<Degrees>(value, ::Degrees) {
    override fun radians() = Radians(Math.toRadians(this.value))
    override fun revolutions() = Revolutions(value / kRevolutionsToDegrees)
    override fun degrees() = this
}

fun Number.radians() = Radians(this.toDouble())
data class Radians(override val value: Double) : Rotation2d<Radians>(value, ::Radians) {
    companion object {
        @JvmStatic
        operator fun <T : Length<T>> invoke(length: T, radius: T) = Radians(length / radius)
    }
    override fun degrees() = Degrees(Math.toDegrees(this.value))
    override fun revolutions() = Revolutions(value / kRevolutionsToRadians)
    override fun radians() = this
}

fun Number.revolutions() = Revolutions(toDouble())
data class Revolutions(override val value: Double) : Rotation2d<Revolutions>(value, ::Revolutions) {
    override fun degrees() = Degrees(value * kRevolutionsToDegrees)
    override fun radians() = Radians(value * kRevolutionsToRadians)
    override fun revolutions() = this
}