package bezier

import bezier.units.*
import bezier.units.derived.*
import kotlin.math.pow

object Bezier {
    @JvmStatic
    fun generateSpline(controlPoints: ArrayList<Point>,
                       maxVel: InchesPerSecond,
                       maxAccel: InchesPerSecondSquared,
                       timeStep: Seconds,
                       width: Inches
    ): ArrayList<Point> {
        val pathXYCoords = generatePureXY(controlPoints)
        val times = trapezoidalTimes(pathXYCoords, controlPoints, maxVel, maxAccel)
        val path = generateWithVel(controlPoints, times, maxVel, maxAccel, timeStep)
        return motion(path, width)
    }

    @JvmStatic
    fun generatePureXY(controlPoints: ArrayList<Point>): ArrayList<Point> {
        val path = ArrayList<Point>()
        val throughPoints = controlPoints.filter { it.isIntercept }
        val precision = 299

        for (j in 0 until throughPoints.size - 1) {
            for (fakeT in 0..precision) {
                val t = fakeT.toDouble() / precision
                val T = 1.0 - t
                var sumX = 0.inches()
                var sumY = 0.inches()
                val startPoint = controlPoints.indexOf(throughPoints[j])
                val endPoint = controlPoints.indexOf(throughPoints[j + 1])
                for (i in 0..endPoint - startPoint) {
                    val I = endPoint - startPoint - i
                    sumX += controlPoints[I + startPoint].x * polynomialCoeff(endPoint - startPoint, i) * T.pow(i) * t.pow(I)
                    sumY += controlPoints[I + startPoint].y * polynomialCoeff(endPoint - startPoint, i) * T.pow(i) * t.pow(I)
                }
                val currentPoint = Point(sumX, sumY)
                if (t == 0.0) {
                    if (j != 0) continue // don't add duplicate points, since start and end of consecutive paths are the same
                    currentPoint.isIntercept = true // start and end of each segment is at same point as an intercept
                    currentPoint.distance = 0.inches()
                } else {
                    if (t == 1.0) currentPoint.isIntercept = true
                    currentPoint.distance = path.last().distance + path.last().distanceTo(currentPoint)
                    path.last().setHeadingTo(currentPoint)
                    path.last().setLeftAndRightPositions()
                }
                path += currentPoint
            }
        }
        if (path.isEmpty()) return ArrayList()

        path.last().heading = path[path.size - 2].heading
        path.last().setLeftAndRightPositions()
        path.last().isIntercept = true

        for (point in path.withIndex()) {
            if (point.index == 0) point.value.distance = 0.inches()
            else {
                point.value.leftPoint?.distance = path[point.index - 1].leftPoint?.distance!! + path[point.index - 1].leftPoint!!.distanceTo(point.value.leftPoint!!)
                point.value.rightPoint?.distance = path[point.index - 1].rightPoint?.distance!! + path[point.index - 1].rightPoint!!.distanceTo(point.value.rightPoint!!)
            }
        }

        return path
    }

    @JvmStatic
    fun trapezoidalTimes(pathXY: ArrayList<Point>,
                         controlPoints: ArrayList<Point>,
                         maxVel: InchesPerSecond,
                         maxAccel: InchesPerSecondSquared
    ): ArrayList<Seconds> {
        if (pathXY.isEmpty()) return ArrayList()

        val times = ArrayList<Seconds>()
        val throughPoints = pathXY.filter { it.isIntercept }
        val oldThroughPoints = controlPoints.filter { it.isIntercept }

        for (j in 0 until throughPoints.size - 1) {
            var lengthOfCurve = throughPoints[j + 1].distance - throughPoints[j].distance

            val velInitial: InchesPerSecond = oldThroughPoints[j].targetVelocity
            val velFinal: InchesPerSecond = oldThroughPoints[j + 1].targetVelocity
            val velMax: InchesPerSecond =
                    if (oldThroughPoints[j + 1].isOverrideMaxVel) oldThroughPoints[j + 1].targetVelocity
                    else maxVel.inchesPerSecond()

            if (velMax.value == 0.0)
                throw IllegalStateException("with a max speed of 0, you'll never get anywhere!")

            //if accel and deccel take same time, calculations are much easier
            var velInitialAndFinal: InchesPerSecond = velInitial
            var timeAccelInitToFinal = Seconds(0.0)
            if (velInitial != velFinal) {
                timeAccelInitToFinal = (velInitial - velFinal) / maxAccel.inchesPerSecondSquared()
                val distToEqualizeVels: Inches = (velInitial + velFinal) * timeAccelInitToFinal / 2.0
                velInitialAndFinal = max(velInitial, velFinal) + (velInitial - velFinal).abs()

                if (distToEqualizeVels > lengthOfCurve)
                    throw IllegalStateException("""distance traveled while accelerating from initial to final velocity (${distToEqualizeVels.value})
                                                  |is greater than total distance of path (${lengthOfCurve.value})""".trimMargin()
                    )
                lengthOfCurve -= distToEqualizeVels
            }

            //calculate max vel that is reachable physically
            val timeAccelTriangle: Seconds = quadratic(maxAccel.inchesPerSecondSquared() * 0.5, velInitial, -lengthOfCurve / 2)
            val maxVelReachable: InchesPerSecond = min(velMax, velInitialAndFinal + (maxAccel * timeAccelTriangle).inchesPerSecond())

            val timeAccel = (maxVelReachable - velInitialAndFinal) / maxAccel.inchesPerSecondSquared()
            val timeDeccel = (velInitialAndFinal - maxVelReachable) / -maxAccel.inchesPerSecondSquared()
            val timeConst = (lengthOfCurve - ((velInitialAndFinal + maxVelReachable) * timeAccel / 2 + (velInitialAndFinal + maxVelReachable) * timeDeccel / 2)) / maxVelReachable
            times += timeAccelInitToFinal + timeAccel + timeConst + timeDeccel
        }
        return times
    }

    @JvmStatic
    fun <L : Length<L>> trapezoidalTime(startPos: L,
                                        endPos: L,
                                        maxVel: LinearVelocity<L, Seconds>,
                                        maxAccel: Acceleration<L, Seconds>
    ): Seconds {
        val length = endPos - startPos
        val initialVel = maxVel.createNew(0.0)

        val timeAccelTriangle: Seconds = quadratic(maxAccel / 2, maxVel.createNew(0.0), -length / 2)
        val maxVelReachable: LinearVelocity<L, Seconds> = min(maxVel, initialVel + (maxAccel * timeAccelTriangle))

        val timeAccel: Seconds = (maxVelReachable - initialVel) / maxAccel
        val timeDeccel: Seconds = (initialVel - maxVelReachable) / -maxAccel
        val timeConst: Seconds = (length - ((initialVel + maxVelReachable) * timeAccel / 2 + (initialVel + maxVelReachable) * timeDeccel / 2)) / maxVelReachable

        return timeAccel + timeConst + timeDeccel
    }

    @JvmStatic
    fun generateWithVel(controlPoints: ArrayList<Point>,
                        times: ArrayList<Seconds>,
                        maxVel: InchesPerSecond,
                        maxAccel: InchesPerSecondSquared,
                        timeStep: Seconds
    ): ArrayList<Point> {
        val path = ArrayList<Point>()
        val throughPoints = controlPoints.filter { it.isIntercept }

        var prevEnd = 0 // TODO make this less terrible
        for (j in 0 until throughPoints.size - 1) {
            val startVel: InchesPerSecond = throughPoints[j].targetVelocity
            val endVel: InchesPerSecond = throughPoints[j + 1].targetVelocity
            val curMaxVel: InchesPerSecond =
                    if (controlPoints[j + 1].isOverrideMaxVel) max(startVel, endVel)
                    else maxVel.inchesPerSecond()

            val time: Seconds = times[j]
            val precision = Math.ceil(time / timeStep) + 1
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
                val up: InchesPerSecond = min(curMaxVel, (maxAccel * t * time).inchesPerSecond())
                val down: InchesPerSecond = min(curMaxVel, (-maxAccel * (time * t - time)).inchesPerSecond())
                path.last().targetVelocity = min(up, down)
                path.last().time = time * t + path[prevEnd].time
                fakeT++
                //                if (j != 0) path.get(path.size() - 1).setTime(denominator*t + path.get(prevEnd).getDenominator());            //TODO where did this come from? should it be here?
                //                else path.get(path.size() - 1).setTime(denominator * t);          //TODO where did this come from? should it be here?
//                if (j != 0)
//                    path.last().time =
            }
            if (throughPoints[j + 1].isReverse)

                path.stream().skip((path.size - precision).toLong()).forEach(Point::reverse)
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
        path.last().heading = path[path.size - 2].heading
        return path
    }

    /**
     * for linear motion of elevator
     */
    @JvmStatic
    fun <L : Length<L>> generateVelProfile(time: Seconds,
                                           timeStep: Seconds,
                                           maxVel: LinearVelocity<L, Seconds>,
                                           maxAccel: Acceleration<L, Seconds>
    ): Array<LinearVelocity<L, Seconds>> {
        val precision: Int = (Math.ceil(time / timeStep) + 1).toInt()
        return Array(precision) { i ->
            val t = i / precision.toDouble()
            val up: LinearVelocity<L, Seconds> = min(maxVel, (maxAccel * t * time))
            val down: LinearVelocity<L, Seconds> = min(maxVel, (-maxAccel * (time * t - time)))
            min(up, down)
        }
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
    fun motion(path: ArrayList<Point>, width: Inches): ArrayList<Point> {
        if (path.size == 0) return arrayListOf()
        val halfWidth: Inches = (width / 2)
        for (i in path.indices) {
            val iAdjusted = if (i + 2 > path.size - 1) path.size - 3 else i
            val circleRadius = radiusOfCircle(path[iAdjusted], path[iAdjusted + 1], path[iAdjusted + 2])

            var leftWheelVel: InchesPerSecond
            var rightWheelVel: InchesPerSecond

            //TODO more cases for when turning radius is less that robot radius
            if (circleRadius > 5_000) { //straight line path (some are not infinite, just very big)
                leftWheelVel = path[i].targetVelocity
                rightWheelVel = path[i].targetVelocity
            } /*else if (circleRadius < width) { //maybe should be halfWidth
                TODO("more math needed for turns that are inside frame perimeter")
            } */ else {
                val angularVel = RadiansPerSecond(path[i].targetVelocity, circleRadius)

                if (path[iAdjusted].heading isLeftTowards path[iAdjusted + 2].heading) { // turning left
                    leftWheelVel = angularVel * (circleRadius - halfWidth)
                    rightWheelVel = angularVel * (circleRadius + halfWidth)
                } else { //turning right
                    leftWheelVel = angularVel * (circleRadius + halfWidth)
                    rightWheelVel = angularVel * (circleRadius - halfWidth)
                }
            }

            path[i].setVels(leftWheelVel, rightWheelVel)
            if (i == 0)
                path[i].setPos(0.inches(), 0.inches())
            else
                path[i].advancePos(path[i - 1].leftPos, path[i - 1].rightPos)
        }
        return path
    }

    /**
     * velocity profile for linear motion (elevator)
     */
    fun <L : Length<L>> generateLinear(startPos: L, endPos: L): Array<LinearVelocity<L, Seconds>> {
        val maxVel = startPos.createNew(10.0) / 1.seconds()
        val maxAccel = startPos.createNew(10.0) / 1.seconds() / 1.seconds()

        val time = trapezoidalTime(startPos, endPos, maxVel, maxAccel)
        return generateVelProfile(time, 0.02.seconds(), maxVel, maxAccel)
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
