package bezier

import bezier.units.*
import bezier.units.derived.InchesPerSecond
import bezier.units.derived.inchesPerSecond
import utils.Config
import utils.Config.timeStep

data class Point
@JvmOverloads constructor(var x: Inches,
                          var y: Inches,
                          var isIntercept: Boolean = false,
                          var targetVelocity: InchesPerSecond = InchesPerSecond(Inches(0.0), Seconds(1.0)),
                          var isOverrideMaxVel: Boolean = false,
                          var isReverse: Boolean = false
) {
    @JvmOverloads
    constructor(x: Double,
                y: Double,
                isIntercept: Boolean = false,
                targetVelocity: InchesPerSecond = InchesPerSecond(Inches(
                        0.0), Seconds(1.0)),
                isOverrideMaxVel: Boolean = false,
                isReverse: Boolean = false
    ) : this(x.inches(), y.inches(), isIntercept, targetVelocity, isOverrideMaxVel, isReverse)
    var time = Seconds(0.0)
    var distance = Inches(0.0)

    var leftPos: Inches = 0.inches()
    var rightPos: Inches = 0.inches()

    var leftPoint: Point? = null
    var rightPoint: Point? = null

    var leftVel: InchesPerSecond = 0.inchesPerSecond()
        private set
    var rightVel: InchesPerSecond = 0.inchesPerSecond()
        private set

    lateinit var heading: Degrees

    fun distanceTo(p: Point): Inches {
        val a = p.x.value - x.value
        val b = p.y.value - y.value
        return Inches(Math.sqrt(a * a + b * b))
    }

    fun angleTo(p: Point): Degrees {
        val x = p.x.value - x.value
        val y = p.y.value - y.value
        return Radians(Math.atan2(x, y)).degrees()
    }

    fun setHeadingTo(p: Point) {
        heading = angleTo(p)
    }

    fun reverse() {
        targetVelocity = -targetVelocity
        setVels(-leftVel, -rightVel)
    }

    fun setVels(leftVel: InchesPerSecond, rightVel: InchesPerSecond) {
        this.leftVel = leftVel
        this.rightVel = rightVel
    }

    fun setPos(leftPos: Inches, rightPos: Inches) {
        this.leftPos = leftPos
        this.rightPos = rightPos
    }

    fun setLeftAndRightPositions() {
        val halfWidth = Config.width() / 2
        val offsetX = halfWidth * heading.radians().cos
        val offsetY = halfWidth * heading.radians().sin
        leftPoint = Point(x + offsetX, y - offsetY)
        rightPoint = Point(x - offsetX, y + offsetY)
    }

    /**
     * sets the target positions for this point based on this point's velocity
     *
     * @param prevLeftPos  left position of previous point
     * @param prevRightPos left position of previous point
     */
    fun advancePos(prevLeftPos: Inches, prevRightPos: Inches) {
        setPos(prevLeftPos + leftVel * timeStep(), prevRightPos + rightVel * timeStep())
    }

    /**
     * exists for java support
     */
    fun clone() = this.copy()
}
