package bezier

import bezier.OLDunits.Seconds
import bezier.OLDunits.derived.InchesPerSecond
import bezier.OLDunits.derived.inchesPerSecond
import javafx.scene.control.Alert
import utils.Config

object GraphicalBezier {
    @JvmStatic
    fun generateSpline(controlPoints: ArrayList<Point>): ArrayList<Point> {
        val pathXYCoords = Bezier.generatePureXY(controlPoints)
        val times = trapezoidalTimes(pathXYCoords, controlPoints)
        val path = Bezier.generateWithVel(controlPoints, times, Config.maxVel().inchesPerSecond(), Config.maxAccel().inchesPerSecondSquared(), Config.timeStep())
        return Bezier.motion(path, Config.width())
    }

    @JvmStatic
    private fun trapezoidalTimes(xyCoords: ArrayList<Point>, controlPoints: ArrayList<Point>): ArrayList<Seconds> {
        return try {
            Bezier.trapezoidalTimes(xyCoords, Config.maxVel().inchesPerSecond(), Config.maxAccel().inchesPerSecondSquared())
        } catch (e: IllegalStateException) {
            val message = e.message!!
            val alert = Alert(Alert.AlertType.ERROR)
            alert.headerText = "Error"
            alert.headerText = "Impossible Physics"
            alert.contentText = message
            alert.showAndWait()

            return arrayListOf()
        }
    }

    @JvmStatic
    fun calcMaxVel(controlPoints: ArrayList<Point>, pointIndex: Int): InchesPerSecond {
        val oldInterceptPoints = controlPoints.filter { it.isIntercept }

        //edge cases
        val firstPoint = pointIndex == 0
        val lastPoint = pointIndex >= oldInterceptPoints.size - 1

        val interceptPoints = Bezier.generatePureXY(controlPoints).filter { it.isIntercept }

        return when {
            firstPoint && lastPoint -> 0.inchesPerSecond()
            lastPoint -> {
                val lengthBefore = interceptPoints[pointIndex].distance - interceptPoints[pointIndex - 1].distance
                // vf^2 = vi^2 + 2ad
                (oldInterceptPoints[pointIndex - 1].targetVelocity.pow(2) + 2 * Config.maxAccel().inchesPerSecondSquared().value * lengthBefore.value).sqrt()
            }
            firstPoint -> {
                val lengthAfter = interceptPoints[pointIndex + 1].distance - interceptPoints[pointIndex].distance
                // vi^2 = vf^2 - 2ad
                (oldInterceptPoints[pointIndex + 1].targetVelocity.pow(2) + 2 * Config.maxAccel().inchesPerSecondSquared().value * lengthAfter.value).sqrt()
            }
            else -> {
                val lengthBefore = interceptPoints[pointIndex].distance - interceptPoints[pointIndex - 1].distance
                val lengthAfter = interceptPoints[pointIndex + 1].distance - interceptPoints[pointIndex].distance

                minOf(
                        (oldInterceptPoints[pointIndex - 1].targetVelocity.pow(2) + 2 * Config.maxAccel().inchesPerSecondSquared().value * lengthBefore.value).sqrt(),
                        (oldInterceptPoints[pointIndex].targetVelocity.pow(2) + 2 * Config.maxAccel().inchesPerSecondSquared().value * lengthAfter.value).sqrt()
                )
            }
        }
    }
}