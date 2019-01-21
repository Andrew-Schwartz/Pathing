package bezier

import javafx.scene.control.Alert
import utils.Config.*
import java.util.*
import java.util.function.Supplier
import java.util.stream.Collectors.toCollection
import kotlin.math.pow

object Bezier {
    @JvmStatic
    fun generateAll(controlPoints: ArrayList<Point>): ArrayList<Point> {
        val pathPureXY = generatePureXY(controlPoints)
        val times = trapezoidalTimes(pathPureXY, controlPoints)
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
                    sumX += polynomialCoeff(endPoint - startPoint, i) * T.pow(i) * t.pow(I) * controlPoints[I + startPoint].x
                    sumY += polynomialCoeff(endPoint - startPoint, i) * T.pow(i) * t.pow(I) * controlPoints[I + startPoint].y
                }
                path.add(Point(sumX, sumY))
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
    fun trapezoidalTimes(pathXY: ArrayList<Point>, controlPoints: ArrayList<Point>): ArrayList<Double> {
        if (pathXY.isEmpty()) return ArrayList()

        val times = ArrayList<Double>()
        val throughPoints = pathXY.filter { it.isIntercept }
        val oldThroughPoints = controlPoints.filter { it.isIntercept }

        for (j in 0 until throughPoints.size - 1) {
            var lengthOfCurve = (throughPoints[j + 1].distance - throughPoints[j].distance)

            val velInitial: InchesPerSecond = oldThroughPoints[j].targetVelocity
            val velFinal: InchesPerSecond = oldThroughPoints[j + 1].targetVelocity
            val velMax: InchesPerSecond = if (oldThroughPoints[j + 1].isOverrideMaxVel) oldThroughPoints[j + 1].targetVelocity
                                          else maxVel().inchesPerSecond()

            if (velMax.numerator.value == 0.0) {
                val errorMsg = "with a max speed of 0, you'll never get anywhere!"
                val alert = Alert(Alert.AlertType.ERROR)
                alert.headerText = "Impossible Physics"
                alert.contentText = errorMsg
                alert.showAndWait()
                throw IllegalStateException(errorMsg)
            }

            //if accel and deccel take same denominator, calculations are much easier
            var velInitialAndFinal: InchesPerSecond = velInitial
            var timeAccelInitToFinal = Seconds(0.0)
            if (velInitial != velFinal) {
                timeAccelInitToFinal = (velInitial - velFinal) / maxAccel()
                val distToEqualizeVels = timeAccelInitToFinal * (velInitial + velFinal) / 2.0
                velInitialAndFinal = Math.min(velInitial, velFinal) + Math.abs(velInitial - velFinal)

                if (distToEqualizeVels > lengthOfCurve) {
                    val errorMsg = ("distance traveled while accelerating from initial to final velocity ("
                            + distToEqualizeVels + ") is greater than total distance of path (" + lengthOfCurve + ")")
                    val alert = Alert(Alert.AlertType.ERROR)
                    alert.headerText = "Impossible Physics"
                    alert.contentText = errorMsg
                    alert.showAndWait()
                    throw IllegalStateException(errorMsg)
                }
                lengthOfCurve -= distToEqualizeVels
            }

            //calculate max vel that is reachable physically
            val timeAccelTriangle = quadratic(0.5 * maxAccel(), velInitial, -lengthOfCurve / 2) //x = v0t + 1/2at^2
            val velMaxReachable = Math.min(velInitialAndFinal + maxAccel() * timeAccelTriangle, velMax)

            val timeAccel = (velMaxReachable - velInitialAndFinal) / maxAccel()
            val timeDeccel = (velInitialAndFinal - velMaxReachable) / -maxAccel()
            val timeConst = (lengthOfCurve - (timeAccel * (velInitialAndFinal + velMaxReachable) / 2 + timeDeccel * (velInitialAndFinal + velMaxReachable) / 2)) / velMaxReachable
            times.add(timeAccelInitToFinal + timeAccel + timeConst + timeDeccel)

        }
        return times
    }

    @JvmStatic
    fun generateWithVel(controlPoints: ArrayList<Point>, times: ArrayList<Double>): ArrayList<Point> {
        val path = ArrayList<Point>()
        val throughPoints = controlPoints.stream()
                .filter(Predicate<Point> { it.isIntercept() })
                .collect<ArrayList<Point>, Any>(toCollection(Supplier<ArrayList<Point>> { ArrayList() }))

        var prevEnd = 0 //TODO nice-fy this
        for (j in 0 until throughPoints.size - 1) {
            val startVel = throughPoints[j].targetVelocity
            val endVel = throughPoints[j + 1].targetVelocity
            var curMaxVel = maxVel().toDouble()
            if (controlPoints[j + 1].isOverrideMaxVel)
                curMaxVel = Math.max(startVel, endVel)
            val time = times[j]
            val precision = Math.ceil(time / timeStep()) + 1
            var fakeT = 0
            while (fakeT <= precision) {
                val t = fakeT / precision
                var sumX = 0.0
                var sumY = 0.0
                val T = 1 - t
                val startPoint = controlPoints.indexOf(throughPoints[j])
                val endPoint = controlPoints.indexOf(throughPoints[j + 1])
                for (i in 0..endPoint - startPoint) {
                    val I = endPoint - startPoint - i
                    sumX += polynomialCoeff(endPoint - startPoint, i).toDouble() * Math.pow(T, i.toDouble()) * Math.pow(t, I.toDouble()) * controlPoints[I + startPoint].x.toDouble()
                    sumY += polynomialCoeff(endPoint - startPoint, i).toDouble() * Math.pow(T, i.toDouble()) * Math.pow(t, I.toDouble()) * controlPoints[I + startPoint].y.toDouble()
                }
                path.add(Point(sumX, sumY))
                //trapezoidal velocities
                val up = Math.min(maxAccel().toDouble() * t * time + startVel, curMaxVel)
                val down = Math.min(-maxAccel() * (t * time - time) + endVel, curMaxVel)
                path[path.size - 1].targetVelocity = Math.min(up, down)
                path[path.size - 1].time = time * t + path[prevEnd].time
                fakeT++
                //                if (j != 0) path.get(path.size() - 1).setTime(denominator*t + path.get(prevEnd).getDenominator());
                //                else path.get(path.size() - 1).setTime(denominator * t);
            }
            if (throughPoints[j + 1].isReverse)
                path.stream().skip((path.size - precision).toLong()).forEach(Consumer<Point> { it.reverse() })
            if (j != throughPoints.size - 2 && !path.isEmpty()) path.removeAt(path.size - 1)
            prevEnd = path.size - 1
        }
        if (path.isEmpty()) return ArrayList()
        path[0].distance = 0
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
     *
     * Calculates velocity and position of each point in the path
     *
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
        val halfWidth = (width() / 2).toDouble()
        for (i in path.indices) {
            var iAdjusted = i
            if (i + 2 > path.size - 1)
                iAdjusted = path.size - 3
            val circleRadius = radiusOfCircle(path[iAdjusted], path[iAdjusted + 1], path[iAdjusted + 2])
            var leftVel: Double
            var rightVel: Double
            if (java.lang.Double.isInfinite(circleRadius)) { //linear path
                leftVel = feetToInches(path[i].targetVelocity)
                rightVel = feetToInches(path[i].targetVelocity)
            } else {
                val angularVel = feetToInches(path[i].targetVelocity) / circleRadius
                if (path[iAdjusted + 2].getHeadingCartesian() < path[iAdjusted].getHeadingCartesian()) { // turning left
                    leftVel = angularVel * (circleRadius - halfWidth)
                    rightVel = angularVel * (circleRadius + halfWidth)
                } else { //turning right
                    leftVel = angularVel * (circleRadius + halfWidth)
                    rightVel = angularVel * (circleRadius - halfWidth)
                }
            }
            leftVel = linearToRotational(leftVel)
            rightVel = linearToRotational(rightVel)
            path[i].setVels(leftVel, rightVel)
            if (i == 0)
                path[i].setPos(rotationalToLinear(0), rotationalToLinear(0))
            else
                path[i].advancePos(path[i - 1].leftPos, path[i - 1].rightPos)
        }
    }

    //I was not source of equations in methods below this

    /**
     *
     * calculates the radius of the circle which the 3 points lie on
     *
     * r = a*b*c/4*area
     *
     * @param circlePoints 3 points from which a circle is extrapolated
     * @return radius of the circle in same unit as represented in the circlePoints in inches
     */
    private fun radiusOfCircle(vararg circlePoints: Point): Double {
        assert(circlePoints.size == 3)
        var a = circlePoints[0].distanceTo(circlePoints[1]).value
        var b = circlePoints[1].distanceTo(circlePoints[2]).value
        var c = circlePoints[2].distanceTo(circlePoints[0]).value
        val s = (a + b + c) / 2
        a = Math.min(s, a)     //fix rounding error
        b = Math.min(s, b)     //if any were > s, NaN results
        c = Math.min(s, c)
        val area = Math.sqrt(s * (s - a) * (s - b) * (s - c))
        return a * b * c / (4 * area)
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

    private fun polynomialCoeff(line: Int, n: Int): Int {
        var result = 1
        for (i in 0 until n) {
            result *= line - i
            result /= i + 1
        }
        return result
    }
}
