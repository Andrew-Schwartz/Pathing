package bezier

import bezier.units.*
import bezier.units.derived.*
import javafx.scene.control.Alert
import utils.Config.*
import java.util.*
import kotlin.math.pow

object Bezier {
    @JvmStatic
    fun generateAll(controlPoints: ArrayList<Point>): ArrayList<Point> {
        val pathJustXY = generatePureXY(controlPoints)
        val times = trapezoidalTimes(pathJustXY, controlPoints)
        val path = generateWithVel(controlPoints, times)
        motion(path)
        return path
    }

    @JvmStatic
    fun generatePureXY(controlPoints: ArrayList<Point>): ArrayList<Point> {
        val path = ArrayList<Point>()
        val throughPoints = controlPoints.filter { it.isIntercept }

        for (j in 0 until throughPoints.size - 1) {
            for (t in 0.0..1.0 step 1.0 / 299.0) {
                var sumX = Inches(0.0)
                var sumY = Inches(0.0)
                val T = 1.0 - t
                val startPoint = controlPoints.indexOf(throughPoints[j])
                val endPoint = controlPoints.indexOf(throughPoints[j + 1])
                for (i in 0..endPoint - startPoint) {
                    val I = endPoint - startPoint - i
                    sumX += controlPoints[I + startPoint].x * polynomialCoeff(endPoint - startPoint, i) * T.pow(i) * t.pow(I)
                    sumY += controlPoints[I + startPoint].y * polynomialCoeff(endPoint - startPoint, i) * T.pow(i) * t.pow(I)
                }
                path += Point(sumX, sumY)
                if (t == 0.0) path[path.size - 1].isIntercept = true //start and end of each segment is at same point as an intercept
            }
        }
        if (path.isEmpty()) return ArrayList()
        path[0].distance = Inches(0.0)
        path[path.size - 1].isIntercept = true
        for (i in path.indices) {
            val p = path[i]
            if (i != path.size - 1) {
                p.setHeadingTo(path[i + 1])
            }
            if (i != 0) {
                p.distance = path[i - 1].distance + p.distanceTo(path[i - 1])
            }
        }
        path[path.size - 1].heading = path[path.size - 2].heading
        return path
    }

    @JvmStatic
    fun trapezoidalTimes(pathXY: ArrayList<Point>, controlPoints: ArrayList<Point>): ArrayList<Seconds> {
        if (pathXY.isEmpty()) return ArrayList()

        val times = ArrayList<Seconds>()
        val throughPoints = pathXY.filter { it.isIntercept }
        val oldThroughPoints = controlPoints.filter { it.isIntercept }

        for (j in 0 until throughPoints.size - 1) {
            var lengthOfCurve = (throughPoints[j + 1].distance - throughPoints[j].distance)

            val velInitial: InchesPerSecond = oldThroughPoints[j].targetVelocity
            val velFinal: InchesPerSecond = oldThroughPoints[j + 1].targetVelocity
            val velMax: InchesPerSecond =
                    if (oldThroughPoints[j + 1].isOverrideMaxVel) oldThroughPoints[j + 1].targetVelocity
                    else maxVel().inchesPerSecond()

            if (velMax.value == 0.0) {
                val errorMsg = "with a max speed of 0, you'll never get anywhere!"
                val alert = Alert(Alert.AlertType.ERROR)
                alert.headerText = "Impossible Physics"
                alert.contentText = errorMsg
                alert.showAndWait()
                throw IllegalStateException(errorMsg) // TODO make a function for this and other errors
            }

            //if accel and deccel take same time, calculations are much easier
            var velInitialAndFinal: InchesPerSecond = velInitial
            var timeAccelInitToFinal = Seconds(0.0)
            if (velInitial != velFinal) {
                timeAccelInitToFinal = (velInitial - velFinal) / maxAccel().inchesPerSecondSquared()
                val distToEqualizeVels: Inches = (velInitial + velFinal) * timeAccelInitToFinal / 2.0
                velInitialAndFinal = max(velInitial, velFinal) + (velInitial - velFinal).abs()

                if (distToEqualizeVels > lengthOfCurve) {
                    val errorMsg = ("""distance traveled while accelerating from initial to final velocity (${distToEqualizeVels.value})
                                             |is greater than total distance of path (${lengthOfCurve.value})""".trimMargin())
                    val alert = Alert(Alert.AlertType.ERROR)
                    alert.headerText = "Impossible Physics"
                    alert.contentText = errorMsg
                    alert.showAndWait()
                    throw IllegalStateException(errorMsg)
                }
                lengthOfCurve -= distToEqualizeVels
            }

            //calculate max vel that is reachable physically
//            val timeAccelTriangle: Seconds = quadratic((maxAccel() * 0.5).inchesPerSecondSquared().value, velInitial.value, (-lengthOfCurve).value / 2).seconds() //x = v0t + 1/2at^2
            val timeAccelTriangle: Seconds = quadratic(maxAccel().inchesPerSecondSquared() * 0.5, velInitial, -lengthOfCurve / 2)
            val velMaxReachable: InchesPerSecond = min(velMax, velInitialAndFinal + (maxAccel() * timeAccelTriangle).inchesPerSecond())

            val timeAccel = (velMaxReachable - velInitialAndFinal) / maxAccel().inchesPerSecondSquared()
            val timeDeccel = (velInitialAndFinal - velMaxReachable) / -maxAccel().inchesPerSecondSquared()
            val timeConst = (lengthOfCurve - ((velInitialAndFinal + velMaxReachable) * timeAccel / 2 + (velInitialAndFinal + velMaxReachable) * timeDeccel / 2)) / velMaxReachable
            times += timeAccelInitToFinal + timeAccel + timeConst + timeDeccel
        }
        return times
    }

    @JvmStatic
    fun generateWithVel(controlPoints: ArrayList<Point>, times: ArrayList<Seconds>): ArrayList<Point> {
        val path = ArrayList<Point>()
        val throughPoints = controlPoints.filter { it.isIntercept }

        var prevEnd = 0 // TODO make this less terrible
        for (j in 0 until throughPoints.size - 1) {
            val startVel: InchesPerSecond = throughPoints[j].targetVelocity
            val endVel: InchesPerSecond = throughPoints[j + 1].targetVelocity
//            val curMaxVel: LinearVelocity<Inches, Seconds> = if (controlPoints[j + 1].isOverrideMaxVel) startVel.maxVs(endVel) else maxVel().inchesPerSecond()
            val curMaxVel: InchesPerSecond =
                    if (controlPoints[j + 1].isOverrideMaxVel) max(startVel, endVel)
                    else maxVel().inchesPerSecond()

            val time: Seconds = times[j]
            val precision = Math.ceil(time / timeStep()) + 1
            var fakeT = 0 // TODO make this less terrible
            while (fakeT <= precision) { // essentially a for loop 0 to 1 with precision iters
                val t = fakeT / precision
                var sumX = Inches(0.0)
                var sumY = Inches(0.0)
                val T = 1 - t
                val startPoint = controlPoints.indexOf(throughPoints[j])
                val endPoint = controlPoints.indexOf(throughPoints[j + 1])
                for (i in 0..endPoint - startPoint) {
                    val I = endPoint - startPoint - i
                    sumX += controlPoints[I + startPoint].x * polynomialCoeff(endPoint - startPoint, i).toDouble() * Math.pow(T, i.toDouble()) * Math.pow(t, I.toDouble())
                    sumY += controlPoints[I + startPoint].y * polynomialCoeff(endPoint - startPoint, i).toDouble() * Math.pow(T, i.toDouble()) * Math.pow(t, I.toDouble())
                }
                path += Point(sumX, sumY)

                //trapezoidal velocities
//                val up = Math.min(maxAccel() * t * time + startVel, curMaxVel)
//                val down = Math.min(-maxAccel() * (t * time - time) + endVel, curMaxVel)
                val up: InchesPerSecond = min(curMaxVel, (maxAccel() * t * time).inchesPerSecond())
                val down: InchesPerSecond = min(curMaxVel, (-maxAccel() * (time * t - time)).inchesPerSecond())
                path[path.size - 1].targetVelocity = min(up, down)
                path[path.size - 1].time = time * t + path[prevEnd].time
                fakeT++
                //                if (j != 0) path.get(path.size() - 1).setTime(denominator*t + path.get(prevEnd).getDenominator());            //TODO where did this come from? should it be here?
                //                else path.get(path.size() - 1).setTime(denominator * t);          //TODO where did this come from? should it be here?
            }
            if (throughPoints[j + 1].isReverse)
                path.stream().skip((path.size - precision).toLong()).forEach { it.reverse() }
            if (j != throughPoints.size - 2 && !path.isEmpty()) path.removeAt(path.size - 1)
            prevEnd = path.size - 1
        }
        if (path.isEmpty()) return ArrayList()
        path[0].distance = Inches(0.0)
        for (i in path.indices) {
            val p = path[i]
            if (i != path.size - 1) {
                p.setHeadingTo(path[i + 1])
            }
            if (i != 0) {
                p.distance = path[i - 1].distance + p.distanceTo(path[i - 1])
            }
        }
        path[path.size - 1].heading = path[path.size - 2].heading
        return path
    }

    /**
     * Calculates velocity and position of each point in the path
     * turning left, equation is:
     *
     * Vr = w * (R + l/2)
     *
     * Vl = w * (R - l/2)
     *
     * @param path contains x,y,theta coordinates of each point, and the target velocity to travel at (as if going in a line)
     */
    @JvmStatic
    fun motion(path: ArrayList<Point>) {
        if (path.size == 0) return
        val halfWidth: Inches = (width() / 2)
        for (i in path.indices) {
            val iAdjusted = if (i + 2 > path.size - 1) path.size - 3 else i
            val circleRadius = radiusOfCircle(path[iAdjusted], path[iAdjusted + 1], path[iAdjusted + 2])

//            val leftWheelVel: RadiansPerSecond
//            val rightWheelVel: RadiansPerSecond
            var leftWheelVel: InchesPerSecond
            var rightWheelVel: InchesPerSecond


            if (circleRadius > 9_000_000) { //straight line path (some are not infinite)
//                leftWheelVel = RadiansPerSecond(path[i].targetVelocity, wheelRadius())
//                rightWheelVel = RadiansPerSecond(path[i].targetVelocity, wheelRadius())
                leftWheelVel = path[i].targetVelocity
                rightWheelVel = path[i].targetVelocity
            } else {
                val angularVel = RadiansPerSecond(path[i].targetVelocity, circleRadius)
//                val angularVel = path[i].targetVelocity

                if (path[iAdjusted].heading isLeftTowards path[iAdjusted + 2].heading) { // turning left
//                    leftWheelVel = angularVel * (circleRadius - halfWidth).value
//                    rightWheelVel = angularVel * (circleRadius + halfWidth).value
                    leftWheelVel = angularVel * (circleRadius - halfWidth)
                    rightWheelVel = angularVel * (circleRadius + halfWidth)
                } else { //turning right
//                    leftWheelVel = angularVel * (circleRadius + halfWidth).value
//                    rightWheelVel = angularVel * (circleRadius - halfWidth).value
                    leftWheelVel = angularVel * (circleRadius + halfWidth)
                    rightWheelVel = angularVel * (circleRadius - halfWidth)
                }
            }
//            leftWheelVel = linearToRotational(leftWheelVel)
//            rightWheelVel = linearToRotational(rightWheelVel)

            path[i].setVels(leftWheelVel, rightWheelVel)
//            path[i].distance = 0.inches()
            if (i == 0)
                path[i].setPos(0.inches(), 0.inches())
            else
                path[i].advancePos(path[i - 1].leftPos, path[i - 1].rightPos)
        }
    }

    //I was not source of equations in methods below this comment

    /**
     *
     * calculates the radius of the circle which the 3 points lie on
     *
     * r = a*b*c/4*area
     *
     * @param circlePoints 3 points from which a circle is extrapolated
     * @return radius of the circle in same unit as represented in the circlePoints in inches
     */
    private fun radiusOfCircle(vararg circlePoints: Point): Inches {
        assert(circlePoints.size == 3)
        var a = circlePoints[0].distanceTo(circlePoints[1])
        var b = circlePoints[1].distanceTo(circlePoints[2])
        var c = circlePoints[2].distanceTo(circlePoints[0])
        val s = (a + b + c) / 2
        a = min(s, a)     //fix rounding error
        b = min(s, b)     //if any were > s, NaN results
        c = min(s, c)
        val area = Math.sqrt(s.value * (s - a).value * (s - b).value * (s - c).value)
        return (a * b.value * c.value) / (4 * area)
    }

    /**
     * quadratic equation to solve 0=ax^2+bx+c
     * solves for positive values
     */
    private fun quadratic(a: Double, b: Double, c: Double): Double {
        return Math.max(
                (-b + Math.sqrt(b * b - 4.0 * a * c)) / (2 * a),
                (-b - Math.sqrt(b * b - 4.0 * a * c)) / (2 * a))
    }

    /**
     * type safe quadratic formula
     */
    private fun <L : Length<L>, T : Time<T>> quadratic(a: Acceleration<L, T>, b: LinearVelocity<L, T>, c: L): T {
        return b.createNewTime(quadratic(a.value, b.value, c.value))
    }

    /**
     * solves for number in Pascal's triangle on a line n indices over
     */
    private fun polynomialCoeff(line: Int, n: Int): Int {
        var result = 1
        for (i in 0 until n) {
            result *= line - i
            result /= i + 1
        }
        return result
    }
}
